package ru.l2gw.gameserver.model;

import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.tables.SkillTable;

import java.util.concurrent.ScheduledFuture;

public class ForceBuff
{
	private L2Character _caster;
	private L2Character _target;
	private L2Skill _force;
	private ScheduledFuture<?> _task;

	public L2Character getTarget()
	{
		return _target;
	}

	public ForceBuff(L2Character caster, L2Character target, L2Skill skill)
	{
		_caster = caster;
		_target = target;
		_force = SkillTable.getInstance().getInfo(skill.getForceId(), 1);
		
		L2Effect eff = _target.getEffectBySkillId(_force.getId());
		if(eff != null && eff.getForce() >= SkillTable.getInstance().getMaxLevel(_force.getId(), _force.getLevel()))
			return;

		_task = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				L2Effect eff = _target.getEffectBySkillId(_force.getId());
				if(eff == null)
					_force.applyEffects(_caster, _target, false);
				else
					eff.increaseForce();
			}
		}, 300);
	}

	public void delete()
	{
		if(_task != null)
		{
			_task.cancel(false);
			_task = null;
		}

		L2Effect eff = _target.getEffectBySkillId(_force.getId());
		if(eff != null)
			eff.decreaseForce();

		_caster.setForceBuff(null);
	}
}