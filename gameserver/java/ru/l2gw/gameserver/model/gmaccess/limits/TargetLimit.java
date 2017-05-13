package ru.l2gw.gameserver.model.gmaccess.limits;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.IAdminLimit;

/**
 * @author: rage
 * @date: 10.03.12 12:11
 */
public class TargetLimit implements IAdminLimit
{
	private final String type;

	public TargetLimit(String type)
	{
		this.type = type;
	}

	public boolean checkLimit(L2Player player, L2Character target, Object arg1, Object arg2, Object arg3)
	{
		if("self".equals(type))
			return target == null || target == player;

		return false;
	}
}
