package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

public class ConditionPlayerIsPetSummoned extends Condition
{

	boolean summoned;
	
	public ConditionPlayerIsPetSummoned(boolean summoned)
	{
		this.summoned = summoned;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character.isPlayer() && env.character.getPlayer().isPetSummoned() == summoned;
	}

}
