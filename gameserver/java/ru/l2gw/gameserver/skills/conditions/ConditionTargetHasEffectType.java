package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.model.L2Effect;

public final class ConditionTargetHasEffectType extends Condition
{
	private final String _val;

	public ConditionTargetHasEffectType(String val)
	{
		_val = val;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(env.target == null)
			return false;
		for (L2Effect e : env.target.getAllEffects())
			if(e.getSkill().getAbnormalTypes().contains(_val))
				return true;
		return false;
	}
}
