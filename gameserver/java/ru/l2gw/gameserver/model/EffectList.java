package ru.l2gw.gameserver.model;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.instances.L2SummonInstance;
import ru.l2gw.gameserver.serverpackets.ShortBuffStatusUpdate;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.skills.funcs.Func;

import java.util.concurrent.ConcurrentLinkedQueue;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * @author rage
 * @date 15.12.2010 16:55:54
 */
public class EffectList
{
	private static final ConcurrentLinkedQueue<L2Effect> emptyEffects = new ConcurrentLinkedQueue<L2Effect>();
	private static final L2Effect[] emptyEffectsArray = new L2Effect[0];
	private ConcurrentLinkedQueue<L2Effect> _effects;
	private final L2Character _owner;

	public EffectList(L2Character owner)
	{
		_owner = owner;
	}

	public L2Effect getEffectByAbnormalType(String est)
	{
		if(_effects != null)
			for(L2Effect e : _effects)
				if(e.getSkill().getAbnormalTypes().contains(est))
					return e;
		return null;
	}

	public L2Effect getEffectBySkillId(int skillId)
	{
		if(_effects == null)
			return null;
		for(L2Effect e : _effects)
			if(e.getSkillId() == skillId)
				return e;

		return null;
	}

	public ConcurrentLinkedQueue<L2Effect> getAllEffects()
	{
		if(_effects == null)
			return emptyEffects;
		return _effects;
	}

	/**
	 * Возвращает первые эффекты для всех скиллов. Нужно для отображения не
	 * более чем 1 иконки для каждого скилла.
	 */
	public L2Effect[] getAllEffectsArray()
	{
		if(_effects == null || _effects.size() < 1)
			return emptyEffectsArray;

		L2Effect[] list = new L2Effect[_effects.size()];
		_effects.toArray(list);
		return list;
	}

	/**
	 * Ограничение на количество бафов
	 */
	private void checkBuffSlots(L2Effect newEffect)
	{
		if(_effects == null || _effects.size() < 10)
			return;

		boolean isSongDance = false;

		if(newEffect.getSkill().isSongDance())
			isSongDance = true;

		if(newEffect.getSkill().isOffensive() || newEffect.getSkill().isTriggered() || newEffect.getSkill().isToggle() || newEffect.getSkill().getAbnormalTypes().contains("life_force") || newEffect.getSkillId() == 5041)
			return;

		int buffs = 0;
		int songDance = 0;
		for(L2Effect ef : _effects)
			if(ef != null && ef.isInUse())
				if(!ef.getSkill().isOffensive() && !ef.getSkill().isTriggered() && !ef.getSkill().isDebuff() && ef.getAbnormalTime() > 0 && !ef.getSkill().isToggle() && !ef.getSkill().getAbnormalTypes().contains("life_force"))
					if(!ef.getSkill().isSongDance())
						buffs++;
					else
						songDance++;

		if(songDance < _owner.getSongDanceLimit() && isSongDance)
			return;
		else if(buffs < _owner.getBuffLimit() && !isSongDance)
			return;

		int toRemove = 0;

		for(L2Effect ef : _effects)
			if(ef != null && ef.isInUse())
			{
				if(isSongDance)
				{
					if(ef.getSkill().isSongDance())
					{
						toRemove = ef.getSkillId();
						break;
					}
				}
				else if(!ef.getSkill().isDebuff() && !ef.getSkill().isToggle() && !ef.getSkill().getAbnormalTypes().contains("life_force"))
				{
					toRemove = ef.getSkillId();
					break;
				}
			}

		if(toRemove > 0)
			for(L2Effect ef : _effects)
				if(ef != null && ef.isInUse())
					if(ef.getSkillId() == toRemove)
					{
						ef.addToDebugStack("checkBuffSlots exit 8");
						ef.exit();
					}
	}

	public void addEffect(L2Effect newEffect)
	{
		addEffect(newEffect, 1);
	}

	public synchronized void addEffect(L2Effect newEffect, int effectTimeModifier)
	{
		if(newEffect == null)
			return;

		if(_effects == null)
			_effects = new ConcurrentLinkedQueue<L2Effect>();

		boolean massUpdate = false;
		try
		{
			if(_owner instanceof L2Playable)
			{
				massUpdate = ((L2Playable) _owner).isMassUpdating();
				((L2Playable) _owner).setMassUpdating(true);
			}
			// Хербы при вызванном саммоне делятся с саммоном пополам
			if(Config.HERBS_DIVIDE && (_owner.isPet() || _owner.isSummon() || _owner.getPet() != null && !_owner.getPet().isDead()) && newEffect.getSkill().isHerb())
			{
				newEffect.setAbnormalTime(newEffect.getAbnormalTime() / 2);
				if(!(_owner.isPet() || _owner.isSummon()))
					_owner.getPet().altUseSkill(newEffect.getSkill(), _owner.getPet(), null);
			}
			// Делим все позитивные эффекты с саммоном
			else if(!newEffect.getSkill().isOffensive() && !newEffect.getSkill().isToggle() && !newEffect.getSkill().getAbnormalTypes().contains("i_install_signet"))
			{
				if(_owner.isPlayer())
				{
					L2Summon summon = _owner.getPet();
					if(summon instanceof L2SummonInstance && !summon.isDead())
						summon.addEffect(newEffect.getSkill().getTimedEffectTemplate().getEffect(new Env(summon, summon, newEffect.getSkill())), effectTimeModifier);
				}
			}

			// затычка на баффы повышающие хп/мп
			double hp = _owner.getCurrentHp();
			double mp = _owner.getCurrentMp();
			double cp = _owner.getCurrentCp();

			if(newEffect.getSkill().isHerb())
			{
				newEffect.addToDebugStack("addEffect start isHerb");

				//		Arteas only! use effectTimeModifier to set time of effect permanently
				if(effectTimeModifier > 1)
					//			newEffect.setAbnormalTime(newEffect.getAbnormalTime() * effectTimeModifier);
					newEffect.setAbnormalTime(effectTimeModifier);

				if(!newEffect.isStackable())
				{
					// Удаляем такие же эффекты
					for(L2Effect ef : _effects)
						if(ef != null && ef.isInUse() && !ef.isStackable() && ef.getSkill().isHerb() && ef.getSkillId() == newEffect.getSkillId())
							// Если оставшаяся длительность старого эффекта больше чем длительность нового, то оставляем старый.
							if(newEffect.getTimeLeft() > ef.getTimeLeft())
							{
								ef.addToDebugStack("addEffect exit 1");
								ef.exit();
							}
							else
								return;
				}
				else
				{
					newEffect.addToDebugStack("addEffect start");
					// Проверяем, нужно ли накладывать эффект, при совпадении StackType.
					// Новый эффект накладывается только в том случае, если у него больше StackOrder и больше длительность.
					// Если условия подходят - удаляем старый, иначе выходим.
					for(L2Effect ef : _effects)
						if(ef != null && ef.getSkill().isHerb() && ef.isInUse() && ef.isStackable(newEffect))
						{
							// Если новый эффект слабее
							if(newEffect.getAbnormalLevel() < ef.getAbnormalLevel())
								return;

								// Если эффекты равны
							else if(newEffect.getAbnormalLevel() == ef.getAbnormalLevel())
							{
								// Передаем нижние эффекты новому эффекту
								newEffect.takeScheduledNext(ef);
								// Останавливаем старый
								ef.addToDebugStack("addEffect exit 2");
								ef.exit();
								//break;
							}
							// Если новый эффект сильнее
							else if(newEffect.getAbnormalLevel() > ef.getAbnormalLevel())
							{
								// Передаем нижние эффекты новому эффекту
								newEffect.takeScheduledNext(ef);
								// Останавливаем старый
								ef.addToDebugStack("addEffect exit 3");
								ef.exit();
								//break;
							}
						}
					// Поищем нехербовый эффект, который надо бы поместить ниже по стеку, или вобще не бафать херб, если есть более крутой нехербовый эффект
					for(L2Effect e : _effects)
						if(e != null && !e.getSkill().isHerb() && e.isStackable(newEffect) && e.getAbnormalLevel() <= newEffect.getAbnormalLevel())
						{
							_owner.removeStatsOwner(e);
							e.addToDebugStack("addEffect remove from list and set next for: " + newEffect);
							_effects.remove(e);
							newEffect.scheduleNext(e);
						}
						else if(e != null && !e.getSkill().isHerb() && e.isStackable(newEffect) && e.getAbnormalLevel() > newEffect.getAbnormalLevel())
							return;
				}
			}
			else
			{
				//		Arteas only! use effectTimeModifier to set time of effect permanently
				if(effectTimeModifier > 1)
					//			newEffect.setAbnormalTime(newEffect.getAbnormalTime() * effectTimeModifier);
					newEffect.setAbnormalTime(effectTimeModifier);

				if(!newEffect.isStackable())
				{
					// Удаляем такие же эффекты
					for(L2Effect ef : _effects)
						if(ef != null && ef.isInUse() && !ef.getSkill().isHerb() && !ef.isStackable() && ef.getSkillId() == newEffect.getSkillId())
							// Если оставшаяся длительность старого эффекта больше чем длительность нового, то оставляем старый.
							if(newEffect.getTimeLeft() > ef.getTimeLeft())
							{
								ef.addToDebugStack("addEffect exit 4");
								ef.exit();
						}
				}
				else
				{
					// Проверяем, нужно ли накладывать эффект, при совпадении StackType.
					// Новый эффект накладывается только в том случае, если у него больше StackOrder и больше длительность.
					// Если условия подходят - удаляем старый, иначе выходим.
					for(L2Effect ef : _effects)
						if(ef != null && ef.isInUse() && !ef.getSkill().isHerb() && ef.isStackable(newEffect))
						{
							// Если новый эффект слабее
							if(newEffect.getAbnormalLevel() < ef.getAbnormalLevel())
								return;

								// Если эффекты равны
							else if(newEffect.getAbnormalLevel() == ef.getAbnormalLevel())
							{
								ef.addToDebugStack("addEffect exit 5");
								// Останавливаем старый
								ef.exit();
								//break;
							}
							// Если новый эффект сильнее
							else if(newEffect.getAbnormalLevel() > ef.getAbnormalLevel())
							{
								ef.addToDebugStack("addEffect exit 6");
								ef.exit();
								//break;
							}
						}
						else if(ef != null && ef.isInUse() && ef.getSkill().isHerb() && ef.isStackable(newEffect))
						// Ищем зашедуленные эффекты с меньшим или таким же стак ордером
						{
							if(ef != null && ef.getAbnormalLevel() < newEffect.getAbnormalLevel())
							{
								ef.addToDebugStack("addEffect exit 7");
								ef.exit();
								if(ef.getNext() != null && ef.getNext().isInUse())
								{
									ef.addToDebugStack("addEffect exit 7 scheduled effect");
									ef.getNext().exit();
								}
								break;
							}

							if(ef.getNext() != null && ef.getNext().getAbnormalLevel() == newEffect.getAbnormalLevel())
							{
								// Если новый эффект короче то выйти
								if(newEffect.getTimeLeft() < ef.getNext().getTimeLeft())
									return;

								ef.getNext().addToDebugStack("addEffect exit 8");
								ef.getNext().exit();
								ef.scheduleNext(newEffect);
								ef.getNext().setInUse(true);
								return;
							}
							else if(ef.getNext() != null && ef.getNext().getAbnormalLevel() < newEffect.getAbnormalLevel())
							{
								ef.getNext().addToDebugStack("addEffect exit 9");
								ef.getNext().exit();
								ef.scheduleNext(newEffect);
								ef.getNext().setInUse(true);
								return;

							}
							else if(ef.getNext() != null && ef.getNext().getAbnormalLevel() > newEffect.getAbnormalLevel())
								return;
								// херб есть, а зашедуленных нет под ним бафов такого же стектайпа
							else if(ef.getNext() == null && ef.getAbnormalLevel() >= newEffect.getAbnormalLevel())
							{
								ef.scheduleNext(newEffect);
								ef.getNext().setInUse(true);
								return;
							}
						}
				}
			}

			// Проверяем на дупы, добавляемый эффект должен быть единственным с своим стектайпом
			for(L2Effect e : _effects)
				if(newEffect.isStackable() && e.isStackable(newEffect))
				{
//				_log.info("AddEffect WARNING: " + this + " has dup effects!");
//				_log.info("AddEffect WARNING: effect in list: " + e + " effect to add: " + newEffect);
//				_log.info("AddEffect WARNING: print debug stack:");
//				e.printDebugStack();
					_owner.removeStatsOwner(e);
					_effects.remove(e);
				}

			// Проверяем на лимиты бафов/дебафов
			checkBuffSlots(newEffect);

			newEffect.addToDebugStack("addEffect add to list");
			_effects.add(newEffect);

			if(newEffect.getEffector().isPlayer() && newEffect.getEffector().isInDuel())
				newEffect.getEffector().getDuel().onBuff((L2Player) newEffect.getEffector(), newEffect);

			// Применяем эффект к параметрам персонажа
			_owner.addStatFuncs(newEffect.getStatFuncs());

			// Запускаем эффект
			newEffect.setInUse(true);

			// затычка на баффы повышающие хп/мп
			for(Func f : newEffect.getStatFuncs())
				if(f._stat == Stats.MAX_HP)
					_owner.setCurrentHp(hp);
				else if(f._stat == Stats.MAX_MP)
					_owner.setCurrentMp(mp);
				else if(f._stat == Stats.MAX_CP)
					_owner.setCurrentCp(cp);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(_owner instanceof L2Playable)
			{
				((L2Playable) _owner).setMassUpdating(massUpdate);
				_owner.updateStats();
			}
		}
		// Обновляем иконки
		_owner.updateEffectIcons();
	}

	/**
	 * @param effect эффект для удаления
	 * @see ru.l2gw.gameserver.model.L2Effect#stopEffectTask()
	 */
	public void removeEffect(L2Effect effect)
	{
		if(effect == null || _effects == null)
			return;

		if(!_effects.contains(effect))
		{
			effect.addToDebugStack("removeEffect no in effect list?!");
			return;
		}

		_owner.removeStatsOwner(effect);

		effect.addToDebugStack("removeEffect");
		_effects.remove(effect);

		if(effect.getNext() != null && effect.getNext().isInUse())
		{
			L2Effect next = effect.getNext();

			for(L2Effect e : _effects)
				if(e != null && e.isStackable() && e.isStackable(next))
					next.printDebugStack();

			next.addToDebugStack("removeEffect add next to list");
			_effects.add(next);
			_owner.addStatFuncs(next.getStatFuncs());
			next.updateEffects();
		}

		if(effect.getSkill().getAbnormalTypes().contains("life_force"))
			_owner.sendPacket(new ShortBuffStatusUpdate());
		else
			_owner.updateEffectIcons();
	}

	public void stopAllEffects()
	{
		if(_effects != null)
			for(L2Effect e : _effects)
				if(e != null)
				{
					e.addToDebugStack("stopAllEffects exit 2");
					e.exit();

					if(e.getNext() != null && e.isInUse())
					{
						e.getNext().addToDebugStack("stopAllEffects exit 1");
						e.getNext().exit();
					}
				}
	}
	
	/**
	 * Возвращает первые эффекты для всех скиллов. Нужно для отображения не более чем 1 иконки для каждого скилла.
	 */
	public L2Effect[] getAllFirstEffects() {
		if (_effects == null)
			return L2Effect.EMPTY_L2EFFECT_ARRAY;
		
		TIntObjectHashMap<L2Effect> map = new TIntObjectHashMap<L2Effect>();
	
		for (L2Effect e : _effects)
			map.put(e.getSkill().getId(), e);
		
		return map.values(new L2Effect[map.size()]);
	}

	public void stopEffects()
	{
		if(_effects != null)
			for(L2Effect e : _effects)
				if(e != null && e.getSkill().getBuffProtectLevel() < 2)
				{
					e.addToDebugStack("stopAllEffects exit 2");
					e.exit();

					if(e.getNext() != null && e.isInUse() && e.getNext().getSkill().getBuffProtectLevel() < 2)
					{
						e.getNext().addToDebugStack("stopAllEffects exit 1");
						e.getNext().exit();
					}
				}
	}

	public void stopEffect(int skillId)
	{
		if(_effects != null)
			for(L2Effect e : _effects)
				if(e != null && e.getSkillId() == skillId)
				{
					e.addToDebugStack("stopEffect exit 1");
					e.exit();
				}
	}

	public void stopEffects(String... abnormalTypes)
	{
		if(_effects != null)
			for(L2Effect e : _effects)
				for(String at : abnormalTypes)
					if(at != null && !at.isEmpty() && e.getSkill().getAbnormalTypes().contains(at.toLowerCase()))
					{
						e.addToDebugStack("stopEffects exit 1");
						e.exit();
					}
	}

	public void stopEffectsByName(String effectName)
	{
		if(_effects != null)
			for(L2Effect e : _effects)
				if(e.containsEffect(effectName))
				{
					e.addToDebugStack("stopEffects exit 1");
					e.exit();
				}
	}
}
