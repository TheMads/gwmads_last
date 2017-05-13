package ru.l2gw.gameserver.model.instances;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.entity.duel.Duel;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;
import ru.l2gw.gameserver.templates.CubicTemplate;
import ru.l2gw.gameserver.templates.L2Weapon;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public class L2CubicInstance extends L2Character
{
	protected static final Log _log = LogFactory.getLog(L2CubicInstance.class.getName());

	public static final int ACTION_TIME = 3000;
	/**
	 * Оффсет для корректного сохранения кубиков в базе
	 */
	public static final short SKILL_CUBIC_HEAL = 4051;
	public static final short SKILL_CUBIC_CURE = 5579;

	private L2Player _owner;
	private CubicTemplate _template;

	private boolean _givenByOther;

	@SuppressWarnings("unchecked")
	private Future<?> _disappearTask;
	private Future<?> _actionThread;
	private Map<L2Character, Integer> _aggroInfo;
	private long _startTime;
	private long _reuseTime;

	public L2CubicInstance(CubicTemplate ct, L2Player owner, boolean givenByOther)
	{
		super(IdFactory.getInstance().getNextId(), null);

		_owner = owner;
		_template = ct;
		_givenByOther = givenByOther;
		_aggroInfo = new FastMap<L2Character, Integer>().shared();

		_actionThread = ThreadPoolManager.getInstance().scheduleAi(new ActionThread(_template.getSkills()), ACTION_TIME, false);
		_disappearTask = ThreadPoolManager.getInstance().scheduleGeneral(new Disappear(), _template.getLiveTime()); // disappear in 20 mins

		_startTime = System.currentTimeMillis();
	}

	public int getId()
	{
		return _template.getId();
	}

	@Override
	public byte getLevel()
	{
		return (byte)_template.getLevel();
	}

	public long lifeLeft()
	{
		return _template.getLiveTime() - (System.currentTimeMillis() - _startTime);
	}

	public int getAttackCount()
	{
		return _template.getAttackCount();
	}

	public int getActivateRate()
	{
		return _template.getActivateRate();
	}

	public void deleteMe()
	{
		if(_disappearTask != null)
			_disappearTask.cancel(true);

		if(_actionThread != null)
			_actionThread.cancel(true);

		_owner = null;
		_aggroInfo = null;
		IdFactory.getInstance().releaseId(getObjectId());
	}

	public int getReuseDelay()
	{
		return _template.getReuseDelay();
	}

	public void addAggro(L2Character cha, int dmg)
	{
		if(cha.getPlayer() == _owner || _aggroInfo == null)
			return;
		Integer damage = _aggroInfo.remove(cha);
		damage = damage == null ? dmg : damage + dmg;
		if(Config.DEBUG)
			_log.info("L2CubicInstance: addAggro: " + cha + " --> " + dmg);
		_aggroInfo.put(cha, damage);
	}

	public void clearAggro()
	{
		_aggroInfo.clear();
	}

	public void disableActions()
	{
		if(Config.DEBUG)
			_log.info("L2CubicInctance: disableActions: " + getReuseDelay());
		_reuseTime = System.currentTimeMillis() + getReuseDelay();
	}

	/*
		 * base-class for cubic actions (one skill = one action)
		 */
	private abstract class Action
	{
		protected L2Skill _skill;

		public Action(L2Skill skill)
		{
			_skill = skill;
		}

		/*
		 * main skill action
		 */
		protected abstract boolean doAction(L2Character target);

		/*
		 * _target finder
		 * "_target" must be syncronized
		 */
		protected abstract L2Character findTarget();
	}

	private class Attack extends Action
	{
		private L2Character _lastTarget;
		private int _attackCount;

		public Attack(L2Skill skill)
		{
			super(skill);
		}

		@Override
		protected boolean doAction(L2Character target)
		{
			if(Config.DEBUG)
				_log.info("doAction: " + _skill + " --> " + target);

			if(target == null || target.isDead() || !_owner.isInRangeZ(target, 1500) || _attackCount > getAttackCount())
			{
				if(Config.DEBUG)
					_log.info("doAction: " + _skill + " stopAction " + target);
				_aggroInfo.clear();
				_lastTarget = null;
				_attackCount = 0;
				return false;
			}

			if(_lastTarget != target)
			{
				_lastTarget = target;
				_attackCount = 0;
			}
			else
				_attackCount++;

			if(Rnd.chance(getActivateRate()) && _skill.checkCondition(_owner, target, null, false, true))
			{
				if(Config.DEBUG)
					_log.info("doAction: " + _skill + " altUseSkill");
				altUseSkill(_skill, target, null);
				//_owner.broadcastPacket(new MagicSkillUse(_owner, target, _skill.getId(), _skill.getLevel(), 0, 0));
				disableActions();
				return true;
			}

			return false;
		}

		@Override
		protected L2Character findTarget()
		{
			int dmg = 0;
			L2Character tmptarget = null;

			try
			{
				for(L2Character cha : _aggroInfo.keySet())
				{
					Integer charDmg = _aggroInfo.get(cha);
					if(cha != null && charDmg != null && charDmg > dmg && GeoEngine.canSeeTarget(_owner, cha))
					{
						dmg = _aggroInfo.get(cha);
						tmptarget = cha;
					}
				}
			}
			catch(NullPointerException e)
			{
			}

			return tmptarget;
		}
	}

	private class Cleanse extends Action
	{

		public Cleanse(L2Skill skill)
		{
			super(skill);
		}

		@Override
		protected boolean doAction(L2Character target)
		{
			if(target != null && _skill != null && Rnd.chance(getActivateRate())) // снять дебафы, если умеем и если есть с кого
			{
				altUseSkill(_skill, target, null);
				//_owner.broadcastPacket(new MagicSkillUse(_owner, target, _skill.getId(), _skill.getLevel(), 0, 0));
				disableActions();
				return true;
			}

			return false;
		}

		@Override
		protected L2Character findTarget()
		{
			L2Character temptarget = null;
			if(_skill != null)
			{// может надо раздебафить?
				for(L2Effect e : _owner.getAllEffects())
					if(e.getSkill().isDebuff())
						temptarget = _owner;

				if(_owner.getPet() != null && !_owner.getPet().isDead())
					for(L2Effect e : _owner.getPet().getAllEffects())
						if(e.getSkill().isDebuff())
							temptarget = _owner.getPet();
			}
			return GeoEngine.canSeeTarget(_owner, temptarget) ? temptarget : null;
		}
	}

	private class Heal extends Action
	{
		public Heal(L2Skill skill)
		{
			super(skill);
		}

		@Override
		protected boolean doAction(L2Character target)
		{
			if(target != null && target.getCurrentHp() + _skill.getPower(null, null) <= target.getMaxHp())
			{
				double hpp = target.getCurrentHp() / target.getMaxHp();
				if(Rnd.chance(hpp > 0.6 ? 13 : hpp > 0.3 ? 33 : 53))
				{
					altUseSkill(_skill, target, null);
					//_owner.broadcastPacket(new MagicSkillUse(_owner, target, _skill.getId(), _skill.getLevel(), 0, 0));
					disableActions();
					return true;
				}
			}
			return false;
		}

		@Override
		protected L2Character findTarget()
		{
			L2Character target = null;
			if(_skill != null)
			{// может надо подлечить?
				target = _owner;
				if(_owner.getParty() != null)
				{
					for(L2Playable member : _owner.getParty().getPartyMembersWithPets())
						if(member != null && !member.isDead() && _owner.isInRange(member, _skill.getCastRange()) && GeoEngine.canSeeTarget(_owner, member) && member.getCurrentHp() / member.getMaxHp() < target.getCurrentHp() / target.getMaxHp() && _owner.isInRangeZ(member, _skill.getCastRange()))
							target = member;
				}
				else if(_owner.getPet() != null && !_owner.getPet().isDead() && _owner.isInRange(_owner.getPet(), _skill.getCastRange()) && GeoEngine.canSeeTarget(_owner, _owner.getPet()) && _owner.getPet().getCurrentHp() / _owner.getPet().getMaxHp() < _owner.getCurrentHp() / _owner.getMaxHp())
					target = _owner.getPet();
			}
			return target;
		}
	}

	private class ActionThread implements Runnable
	{
		private List<Action> _attackActions = new FastList<Action>();
		private List<Action> _healActions = new FastList<Action>();
		private List<Action> _cureActions = new FastList<Action>();

		public ActionThread(List<L2Skill> skills)
		{
			for(L2Skill skill : skills)
			{
				if(skill.getId() == SKILL_CUBIC_HEAL)
					addAction(new Heal(skill));
				else if(skill.getId() == SKILL_CUBIC_CURE)
					addAction(new Cleanse(skill));
				else
					addAction(new Attack(skill));
			}
		}

		public void run()
		{
			// нужно ли вообще дергаться?
			boolean action = false;

			if(_owner == null || _owner.isDead())
			{
				if(_owner != null)
				{
					_owner.delCubic(getId());
					_owner.broadcastUserInfo();
				}
				deleteMe();
				return;
			}

			try
			{
				if(!isActionDisabled())
				{
					if(Config.DEBUG)
						_log.info("ActionThread: try some actions");
					if(_healActions.size() > 0)
					{
						Action heal = _healActions.get(Rnd.get(_healActions.size()));
						action = heal.doAction(heal.findTarget());
					}
					if(!action && _cureActions.size() > 0)
					{
						Action cure = _cureActions.get(Rnd.get(_cureActions.size()));
						action = cure.doAction(cure.findTarget());
					}
					if(!action && _attackActions.size() > 0)
					{
						Action attack = _attackActions.get(Rnd.get(_attackActions.size()));
						action = attack.doAction(attack.findTarget());
					}
				}
			}
			catch(final Exception e)
			{
				_log.warn("L2CubicInstance: " + e);
				e.printStackTrace();
			}
			finally
			{
				if(action)
					_actionThread = ThreadPoolManager.getInstance().scheduleAi(this, getReuseDelay(), false);
				else
					_actionThread = ThreadPoolManager.getInstance().scheduleAi(this, ACTION_TIME, false);
			}
		}

		public void addAction(Action action)
		{
			if(action instanceof Heal)
				_healActions.add(action);
			else if(action instanceof Cleanse)
				_cureActions.add(action);
			else
				_attackActions.add(action);
		}
	}

	private class Disappear implements Runnable
	{
		public void run()
		{
			if(_owner != null)
			{
				_owner.delCubic(getId());
				_owner.broadcastUserInfo();
			}
			deleteMe();
		}
	}

	public boolean givenByOther()
	{
		return _givenByOther;
	}

	@Override
	public String getName()
	{
		return _owner.getName();
	}

	public boolean isActionDisabled()
	{
		return _reuseTime > System.currentTimeMillis();
	}

	@Override
	public L2Player getPlayer()
	{
		return _owner;
	}

	@Override
	public void sendDamageMessage(L2Character target, int damage, boolean miss, boolean pcrit, boolean block)
	{
		if(_owner != null)
			_owner.sendDamageMessage(target, damage, miss, pcrit, block);
	}

	@Override
	public int getMAtk(L2Character target, L2Skill skill)
	{
		return _template.getPower();
	}

	@Override
	public double getMAtkSps(L2Character target, L2Skill skill)
	{
		return _template.getPower();
	}

	@Override
	public void decreaseHp(double i, L2Character attacker, boolean directHp, boolean reflect)
	{
		if(i < 1)
			return;

		_owner.decreaseHp(i, attacker, directHp, reflect);
	}

	@Override
	public double getCurrentHp()
	{
		return _owner.getCurrentHp();
	}

	@Override
	public void setCurrentHp(double newHp)
	{
		_owner.setCurrentHp(newHp);
	}

	@Override
	public Duel getDuel()
	{
		return _owner.getDuel();
	}

	@Override
	public void broadcastPacket(L2GameServerPacket mov)
	{
		_owner.sendPacket(mov);
		_owner.broadcastPacketToOthers(mov);
	}


	@Override
	public void updateAbnormalEffect()
	{ }

	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}

	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}

	@Override
	public boolean isAttackable(L2Character attacker, boolean forceUse, boolean sendMessage)
	{
		return false;
	}

	@Override
	public void startAttackStanceTask()
	{
		_owner.startAttackStanceTask();
	}

	@Override
	public int getRandomDamage()
	{
		return 0;
	}

	@Override
	public boolean isInBoat()
	{
		return _owner != null && _owner.isInBoat();
	}

	@Override
	public boolean isInAirShip()
	{
		return _owner != null && _owner.isInAirShip();
	}

}