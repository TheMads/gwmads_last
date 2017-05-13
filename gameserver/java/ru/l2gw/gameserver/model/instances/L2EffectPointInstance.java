package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.serverpackets.MagicSkillLaunched;
import ru.l2gw.gameserver.skills.Formulas;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class L2EffectPointInstance extends L2NpcInstance
{
	private L2Player _owner;
	private L2Skill _skill;
	private L2Skill _castSkill;
	private ScheduledFuture<?> _actionTask = null;
	private ScheduledFuture<?> _despawnTask = null;

	public L2EffectPointInstance(int objectId, L2NpcTemplate template, L2Player owner)
	{
		super(objectId, template, 0L, 0L, 0L, 0L);
		_owner = owner;
	}

	@Override
	public L2Player getPlayer()
	{
		return _owner;
	}

	@Override
	public void onAction(L2Player player, boolean shift)
	{
		player.sendActionFailed();
	}

	public void setSkill(L2Skill skill, L2Skill castSkill)
	{
		_skill = skill;
		_castSkill = castSkill;
	}

	public L2Skill getSkill(){
		return _skill;
	}

	public void startActionTask(int startDelay, int delay, int lifeTime)
	{
		_actionTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new ActionTask(), startDelay * 1000L, delay * 1000L);
		_despawnTask = ThreadPoolManager.getInstance().scheduleEffect(new Runnable(){
			public void run()
			{
				_despawnTask = null;
				deleteMe();
			}
		}, lifeTime * 1000L);
	}

	@Override
	public void deleteMe()
	{
		super.deleteMe();
		if(_actionTask != null)
			_actionTask.cancel(true);
		if(_owner != null)
			_owner.setEffectPoint(null);
		if(_despawnTask != null)
			_despawnTask.cancel(true);
	}

	@Override
	public L2Skill.TargetType getTargetRelation(L2Character target, boolean offensive)
	{
		if(_owner == null)
			return L2Skill.TargetType.invalid;

		return _owner.getTargetRelation(target, offensive);
	}

	@Override
	public boolean isFriend(L2Character target)
	{
		return _owner != null && _owner.isFriend(target);
	}

	private class ActionTask implements Runnable
	{
		public void run()
		{
			if(_owner == null || _skill == null || _owner.isDead())
			{
				deleteMe();
				return;
			}

			List<L2Character> targets = _skill.getTargets(_owner, L2EffectPointInstance.this, false);

			if(targets.size() > 0)
			{
				targets.remove(L2EffectPointInstance.this);
				double mpConsume2 = Formulas.calcSkillMpConsume(_owner, _skill, _skill.getMpConsume2(), false);
				if(mpConsume2 > 0)
				{
					if(_owner.getCurrentMp() < mpConsume2)
					{
						_owner.sendPacket(Msg.SKILL_WAS_REMOVED_DUE_TO_LACK_OF_MP);
						_owner.abortCast();
						return;
					}
					_owner.reduceCurrentMp(mpConsume2, null);
				}

				broadcastPacket(new MagicSkillLaunched(getObjectId(), _skill.getDisplayId(), _skill.getDisplayLevel(), targets, _skill.isBuff()));
				broadcastPacket(new MagicSkillLaunched(getObjectId(), _castSkill.getDisplayId(), _castSkill.getDisplayLevel(), targets, _castSkill.isBuff()));

				_owner.callSkill(_skill, targets, null);
			}
		}
	}
}
