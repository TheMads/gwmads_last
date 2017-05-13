package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.skills.Env;

public class ConditionForceBuff extends Condition
{
	private int _battleForces;
	private int _spellForces;

	public ConditionForceBuff(int[] forces)
	{
		_battleForces = forces[0];
		_spellForces = forces[1];
	}

	public ConditionForceBuff(int battle, int spell)
	{
		_battleForces = battle;
		_spellForces = spell;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(_battleForces > 0 && env.first)
		{
			L2Effect effect = env.character.getEffectBySkillId(5104);
			if(effect == null || effect.getForce() < _battleForces)
				return false;
		}
		if(_spellForces > 0 && env.first)
		{
			L2Effect effect = env.character.getEffectBySkillId(5104);
			if(effect == null || effect.getForce() < _spellForces)
				return false;
		}
		return true;
	}
}