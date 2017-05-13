package ru.l2gw.gameserver.model.gmaccess.limits;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.IAdminLimit;

/**
 * @author: rage
 * @date: 10.03.12 12:28
 */
public class AccessLimit implements IAdminLimit
{
	private final int accessLevel;

	public AccessLimit(String access)
	{
		accessLevel = Integer.parseInt(access);
	}

	public boolean checkLimit(L2Player player, L2Character target, Object arg1, Object arg2, Object arg3)
	{
		return player == null || player.getAccessLevel() >= accessLevel;
	}
}
