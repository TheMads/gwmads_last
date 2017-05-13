package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 05.08.2010 22:49:03
 */
public class ConditionPlayerAcademyComplete extends Condition
{
	private final boolean _flag;
	public ConditionPlayerAcademyComplete(boolean flag)
	{
		_flag = flag;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;

		L2Player player = (L2Player) env.character;
		return player.getVarB("completeAcademy") == _flag;
	}
}
