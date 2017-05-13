package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.skills.Env;

import java.util.HashMap;

public final class ConditionTargetHasBuffId extends Condition
{
	private final boolean has;
	private final HashMap<Integer,Integer> skills;

	public ConditionTargetHasBuffId(HashMap<Integer,Integer> skills,boolean has)
	{
		this.has = has;
		this.skills = skills;
	}

	@Override
	public boolean testImpl(Env env)
	{
		boolean returned = false;
		for(Integer skill : skills.keySet())
		{
			boolean notNull = env.target != null && env.target.getEffectBySkillId(skill) != null;
			if(notNull && skills.get(skill) == -1)
				returned = true;
			else if(notNull)
			{
				L2Effect effect = env.target.getEffectBySkillId(skill);
				if(effect != null && effect.getSkillLevel() >= skills.get(skill))
					returned = true;
			}
			if(returned != has)
				return false;
		}
		return true;
	}
}
