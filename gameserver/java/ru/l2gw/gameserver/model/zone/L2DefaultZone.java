package ru.l2gw.gameserver.model.zone;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.extensions.listeners.events.L2Zone.L2ZoneEnterLeaveEvent;
import ru.l2gw.gameserver.controllers.GameTimeController;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.serverpackets.EventTrigger;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.skills.funcs.FuncAdd;
import ru.l2gw.gameserver.skills.funcs.FuncMul;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.commons.arrays.GArray;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class L2DefaultZone extends L2Zone
{
	private Future<?> _damgeTask;
	private Map<ZoneEffect, Future<?>> _scheduledEffects = null;

	public L2DefaultZone()
	{
		super();
	}

	@Override
	public void onEnter(L2Character character)
	{
		if(!isActive(character.getReflection()))
			return;

		if(Config.ZONE_DEBUG)
			_log.info(this+" onEnter");

		for(ZoneType zt : getTypes())
		{
			if(Config.ZONE_DEBUG)
				_log.info("zs: onEnter " + character + " " + this + " " + Thread.currentThread() + " type: " + zt);

			character.setInsideZone(zt, true);
			if(zt == ZoneType.peace) //TODO: Проверить на оффе, при входе в какие зоны таймер рекомендаций останавлевается
				if(character.isPlayer())
				{
					character.getPlayer().getRecSystem().setActive(false);
					if(character.getPlayer().getHuntingBonus().isActive())
						character.getPlayer().getHuntingBonus().stopAdventTask(true);
				}

			if(zt == ZoneType.battle || zt == ZoneType.siege)
			{
				character.sendPacket(Msg.YOU_HAVE_ENTERED_A_COMBAT_ZONE);
				if(zt == ZoneType.siege && character.isPlayer())
					character.getPlayer().updateFameTime();
			}
			/*
			if(character.isPlayer() && zt == ZoneType.instance)
			{
				Instance inst = InstanceManager.getInstance().getInstanceByPlayer((L2Player) character);
				if(inst != null && inst.getTemplate().getId() == getEntityId() && inst.getReflection() == character.getReflection())
					inst.onPlayerEnter((L2Player) character);
			}
			*/
		}

		if(isAffected(character))
		{
			if(_enterMessage != 0)
				character.sendPacket(new SystemMessage(_enterMessage));

			if(_hpRegenBonus != 0)
				character.addStatFunc(new FuncAdd(Stats.REGENERATE_HP_RATE, 0x40, this, _hpRegenBonus));

			if(_mpRegenBonus != 0)
				character.addStatFunc(new FuncAdd(Stats.REGENERATE_MP_RATE, 0x40, this, _mpRegenBonus));

			if(_moveBonus != 0)
			{
				character.addStatFunc(new FuncAdd(Stats.RUN_SPEED, 0x40, this, _moveBonus));
				character.sendChanges();
			}

			if(_expLoss != 1)
				character.addStatFunc(new FuncMul(Stats.EXP_LOST, 0x30, this, _expLoss));

			if(_damgeTask == null && (_damageHp > 0 || _damageMp > 0))
				_damgeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new DamageTask(), _initDelay, 3300);

			// Add effect to char
			if(_zoneEffects != null)
			{
				if(Config.ZONE_DEBUG)
					_log.info(this + " Effects check");

				for(ZoneEffect ze : _zoneEffects)
				{
					if(ze.getEvent() == EventType.ONENTER)
						applyEffect(character, ze);
					else if(ze.getEvent() == EventType.SCHEDULE)
						startScheduleEffect(ze);
				}
			}
		}

		if(character.isPlayer() && _eventId > 0)
			character.sendPacket(new EventTrigger(_eventId, true));

		getListenerEngine().fireMethodInvoked(new L2ZoneEnterLeaveEvent(MethodCollection.L2ZoneObjectEnter, this, new L2Object[] { character }));
	}

	@Override
	public void onExit(L2Character character)
	{
		if(!isActive(character.getReflection()))
			return;

		for(ZoneType zt : getTypes())
		{
			if(Config.ZONE_DEBUG)
				_log.info("zs: onExit " + character + " " + this + " " + Thread.currentThread() + " type: " + zt);

			character.setInsideZone(zt, false);
			if(zt == ZoneType.battle || zt == ZoneType.siege)
				character.sendPacket(Msg.YOU_HAVE_LEFT_A_COMBAT_ZONE);
		}

		if(isAffected(character))
		{
			if(_exitMessage != 0)
				character.sendPacket(new SystemMessage(_exitMessage));

			if(_hpRegenBonus != 0 || _mpRegenBonus != 0 || _moveBonus != 0 || _expLoss != 1)
			{
				character.removeStatsOwner(this);
				if(_moveBonus != 0)
					character.sendChanges();
			}

			if(_characterList.isEmpty() && _damgeTask != null)
			{
				_damgeTask.cancel(true);
				_damgeTask = null;
			}

			// Remove effect on exit
			if(_zoneEffects != null)
			{
				if(Config.ZONE_DEBUG)
					_log.info(this + " Effects check");

				for(ZoneEffect ze : _zoneEffects)
				{
					if(ze.getEvent() == EventType.ONEXIT)
						applyEffect(character, ze);
				}
			}
		}

		getListenerEngine().fireMethodInvoked(new L2ZoneEnterLeaveEvent(MethodCollection.L2ZoneObjectLeave, this, new L2Object[] { character }));
	}

	private class DamageTask implements Runnable
	{
		public void run()
		{
			for(L2Character cha : getCharacters())
			{
				if(cha != null && !cha.isDead() && isAffected(cha))
				{
					if(_damageHp != 0)
					{
						cha.decreaseHp(_damageHp, null, true, true);
						if(_messageNo > 0)
							cha.sendPacket(new SystemMessage(_messageNo).addNumber(_damageHp));
					}
					if(_damageMp != 0)
					{
						cha.reduceCurrentMp(_damageMp, null);
						if(_messageNo > 0)
							cha.sendPacket(new SystemMessage(_messageNo).addNumber(_damageMp));
					}
				}
			}
		}
	}

	protected void startScheduleEffect(ZoneEffect ze)
	{
		if(_scheduledEffects == null)
			_scheduledEffects = new ConcurrentHashMap<>();

		if(_scheduledEffects.get(ze) == null)
			_scheduledEffects.put(ze, ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new EffectTask(ze), _initDelay, ze.getRate()));
	}

	protected class EffectTask implements Runnable
	{
		private ZoneEffect _zoneEffect = null;

		public EffectTask(ZoneEffect ze)
		{
			_zoneEffect = ze;
		}

		public void run()
		{
			GArray<L2Character> characters = getCharacters();
			if(characters.isEmpty())
			{
				try
				{
					_scheduledEffects.get(_zoneEffect).cancel(false);
					_scheduledEffects.remove(_zoneEffect);
				}
				catch(Exception e)
				{
				}
				return;
			}

			for(L2Character cha : characters)
				applyEffect(cha, _zoneEffect);
		}
	}

	protected void applyEffect(L2Character cha, ZoneEffect zoneEffect)
	{
		if(zoneEffect == null || cha == null || cha.isDead() || cha.getCurrentRegion() == null || !cha.getCurrentRegion().isActive() || !isActive(cha.getReflection()) || !isAffected(cha))
			return;

		if(Config.ZONE_DEBUG && cha.isPlayer())
			_log.info(this+" Apply effect to " + cha.getName());

		if(zoneEffect.getAction() == 2)
		{
			cha.teleToLocation(zoneEffect.getTeleportPoint());
			return;
		}

		for(int skillId : zoneEffect.getSkillIds())
		{

			if(Config.ZONE_DEBUG && cha.isPlayer())
				_log.info(this+" SkillID " + skillId);

			L2Skill skill = SkillTable.getInstance().getInfo(skillId, zoneEffect.getSkillLevel(skillId));

			if(skill != null)
			{
				if(zoneEffect.getAction() == 0)
				{ // Add effect

					if(Config.ZONE_DEBUG && cha.isPlayer())
						_log.info(this+" Add effect " + skillId);

					if(zoneEffect.getDayTime() > 0 && GameTimeController.getInstance().isNowNight() != (zoneEffect.getDayTime() == 1))
						continue;

					if(zoneEffect.getProbe() > 0)
					{
						if(Rnd.chance(zoneEffect.getProbe()))
							skill.applyEffects(cha, cha, false);
					}
					else
						skill.applyEffects(cha, cha, false);
				}
				else if(zoneEffect.getAction() == 1)
				{

					for(L2Effect effect : cha.getAllEffects())
					{
						if(effect.getSkillId() == skillId)
						{
							if(Config.ZONE_DEBUG && cha.isPlayer())
								_log.info(this+" Remove effect " + skillId);

							effect.exit();
						}
					}
				}
			}
		}
	}

}
