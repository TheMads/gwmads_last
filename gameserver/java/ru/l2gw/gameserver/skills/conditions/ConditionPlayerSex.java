package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.model.L2Player;

/**
 * @author rage
 * @date 25.11.2009 11:31:24
 */
public class ConditionPlayerSex extends Condition
{
	private byte _sex;

	public ConditionPlayerSex(byte sex)
	{
		_sex = sex;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.character.isPlayer() && ((L2Player) env.character).getSex() == _sex;
	}
}
