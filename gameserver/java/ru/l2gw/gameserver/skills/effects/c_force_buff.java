package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.util.Util;

public class c_force_buff extends t_effect
{
	public int _forces = 0;
	private int _range = -1;
	private final int _maxForce;

	public c_force_buff(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
		_forces = getSkill().getLevel();
		_range = getSkill().getCastRange();
		_maxForce = template._attrs.getInteger("maxForce", 3);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		startActionTask(3000);
	}

	@Override
	public boolean onActionTime()
	{
		if(!Util.checkIfInRange(_range, getEffector(), getEffected(), true) || !GeoEngine.canSeeTarget(getEffector(), getEffected()))
			getEffector().abortCast();
		return true;
	}

	public void increaseForce()
	{
		if(_forces < _maxForce)
		{
			_forces++;
			updateBuff();
		}
	}

	public void decreaseForce()
	{
		_forces--;

		if(_forces < 1)
			_effect.exit();
		else
			updateBuff();
	}

	public void updateBuff()
	{
		_effect.exit();
		L2Skill newSkill = SkillTable.getInstance().getInfo(getSkill().getId(), _forces);
		if(newSkill == null)
		{
			System.out.println(this + " has no force skill: " + getSkill().getId() + " lvl: " + _forces);
			return;
		}
		newSkill.applyEffects(getEffector(), getEffected(), false);
	}
}