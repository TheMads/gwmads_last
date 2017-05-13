package ru.l2gw.gameserver.model.gmaccess;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;

/**
 * @author: rage
 * @date: 09.03.12 13:08
 */
public interface IAdminLimit
{
	public boolean checkLimit(L2Player player, L2Character target, Object arg1, Object arg2, Object arg3);
}
