package ru.l2gw.gameserver.model.instances;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.ai.CtrlEvent;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2ObjectTasks;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.serverpackets.MyTargetSelected;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class L2DecoyInstance extends L2NpcInstance
{
	protected static final Log log = LogFactory.getLog(L2DecoyInstance.class.getName());

	private L2Player owner;

	private int _lifeTime;
	private int _timeRemaining;

	private ScheduledFuture<?> _decoyLifeTask;
	private ScheduledFuture<?> _hateSpam;

	public L2DecoyInstance(int objectId, L2NpcTemplate template, L2Player owner, int lifeTime, int skillId, int skillLvl)
	{
		super(objectId, template, 0L, 0L, 0L, 0L);

		setOwner(owner);
		_lifeTime = lifeTime;
		_timeRemaining = _lifeTime;
		_decoyLifeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new DecoyLifetime(), 1000, 1000);
		_hateSpam = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new HateSpam(SkillTable.getInstance().getInfo(skillId, skillLvl)), 2000, 5000);
	}

	@Override
	public void doDie(L2Character killer)
	{
		super.doDie(killer);
		if(_hateSpam != null)
		{
			_hateSpam.cancel(true);
			_hateSpam = null;
		}
		_lifeTime = 0;
	}

	class DecoyLifetime implements Runnable
	{
		public void run()
		{
			try
			{
				double newTimeRemaining;
				decTimeRemaining(1000);
				newTimeRemaining = getTimeRemaining();
				if(newTimeRemaining < 0)
					unSummon();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	class HateSpam implements Runnable
	{
		private L2Skill _skill;

		HateSpam(L2Skill skill)
		{
			_skill = skill;
		}

		public void run()
		{
			try
			{
				setTarget(L2DecoyInstance.this);
				doCast(_skill, _skill.getAimingTarget(L2DecoyInstance.this), null, false);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public void unSummon()
	{
		if(_decoyLifeTask != null)
		{
			_decoyLifeTask.cancel(true);
			_decoyLifeTask = null;
		}
		if(_hateSpam != null)
		{
			_hateSpam.cancel(true);
			_hateSpam = null;
		}
		deleteMe();
	}

	public void decTimeRemaining(int value)
	{
		_timeRemaining -= value;
	}

	public int getTimeRemaining()
	{
		return _timeRemaining;
	}

	public int getLifeTime()
	{
		return _lifeTime;
	}

	@Override
	public L2Player getPlayer()
	{
		return owner;
	}

	@Override
	public boolean isAttackable(L2Character attacker, boolean forceUse, boolean sendMessage)
	{
		return attacker != getPlayer() && getPlayer().isAttackable(attacker, forceUse, sendMessage);
	}

	@Override
	public void deleteMe()
	{
		super.deleteMe();
		getPlayer().setDecoy(null);
	}

	@Override
	public boolean isInvul()
	{
		return true;
	}

	@Override
	public void onAction(L2Player player, boolean dontMove)
	{
		if(player.isConfused() || player.isBlocked())
		{
			player.sendActionFailed();
			return;
		}

		if(player.getTarget() != this)
		{
			if(player.setTarget(this))
				player.sendPacket(new MyTargetSelected(getObjectId(), 0));
		}
		else if(isAttackable(player, false, true))
			player.getAI().Attack(this, false, dontMove);
		else
			player.sendActionFailed();
	}

	@Override
	public void callSkill(L2Skill skill, List<L2Character> targets, L2ItemInstance usedItem)
	{
		List<L2Character> toRemove = new ArrayList<>();

		if(!skill.altUse())
		{
			int aggro = skill.getEffectPoint() != 0 ? skill.getEffectPoint() : Math.max(1, (int) skill.getPower(null, null));
			for(L2NpcInstance monster : getKnownNpc(1500, 300))
			{
				monster.getAI().notifyEvent(CtrlEvent.EVT_SEE_SPELL, skill, this);

				for(L2Character target : targets)
				{
					if(target == null)
						continue;

					if(!skill.isOffensive() && monster.getHate(target) > 0 && monster.hasAI())
					{
						ThreadPoolManager.getInstance().executeAi(new L2ObjectTasks.NotifyAITask(target, CtrlEvent.EVT_ATTACKED, this, 0, skill), isPlayer());
						//target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this, 0, skill);
						break;
					}
				}
			}

			for(L2Character target : targets)
			{
				if(target == null)
					continue;

				if((target.isInvul() || !target.isVisible()) && skill.isOffensive() && !(target instanceof L2ArtefactInstance || target instanceof L2DoorInstance))
					toRemove.add(target);

				if(skill.isOffensive() && target instanceof L2NpcInstance)
				{
					if(target.hasAI() && skill.getEffectPoint() > 0)
						ThreadPoolManager.getInstance().executeAi(new L2ObjectTasks.NotifyAITask(target, CtrlEvent.EVT_ATTACKED, this, 0, skill), isPlayer());
//						target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this, 0, skill);
				}
			}
		}

		for(L2Character cha : toRemove)
			targets.remove(cha);

		super.callSkill(skill, targets, usedItem);
	}

	@Override
	public float getColRadius()
	{
		if(getPlayer().getTransformation() != 0 && getPlayer().getTransformationTemplate() != 0)
			return NpcTable.getTemplate(getPlayer().getTransformationTemplate()).collisionRadius;
		return getPlayer().getBaseTemplate().collisionRadius;
	}

	@Override
	public float getColHeight()
	{
		if(getPlayer().getTransformation() != 0 && getPlayer().getTransformationTemplate() != 0)
			return NpcTable.getTemplate(getPlayer().getTransformationTemplate()).collisionHeight;
		return getPlayer().getBaseTemplate().collisionHeight;
	}

	public void setOwner(L2Player owner)
	{
		this.owner = owner;
	}
}
