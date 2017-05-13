package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2MonsterInstance;
import ru.l2gw.gameserver.skills.Env;

public class ConditionTargetAggro extends Condition
{
	private final boolean _isAggro;

	public ConditionTargetAggro(boolean isAggro)
	{
		_isAggro = isAggro;
	}

	@Override
	public boolean testImpl(Env env)
	{
		L2Character target = env.target;
		if(target == null)
			return false;
		if(target.isMonster())
			return ((L2MonsterInstance) target).isAggressive() == _isAggro;
		if(target.isPlayer())
			return ((L2Player) target).getKarma() > 0;
		return false;
	}
}
