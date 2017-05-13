package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 01.08.2010 15:26:27
 */
public class ConditionOpHome extends Condition
{
	private final String _type;

	public ConditionOpHome(String type)
	{
		_type = type;
	}

	@Override
	public boolean testImpl(Env env)
	{
		L2Player player = env.character.getPlayer();
		if(player == null || player.getClanId() == 0)
			return false;

		return _type.equalsIgnoreCase("agit") && player.getClan().getHasHideout() > 0 || _type.equalsIgnoreCase("castle") && player.getClan().getHasCastle() > 0 || _type.equalsIgnoreCase("fortress") && player.getClan().getHasFortress() > 0;
	}

}
