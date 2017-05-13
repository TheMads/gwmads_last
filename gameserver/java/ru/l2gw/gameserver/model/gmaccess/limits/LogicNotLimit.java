package ru.l2gw.gameserver.model.gmaccess.limits;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.IAdminLimit;

/**
 * @author: rage
 * @date: 21.03.12 15:30
 */
public class LogicNotLimit implements IAdminLimit
{
	private final IAdminLimit limit;

	public LogicNotLimit(IAdminLimit limit)
	{
		this.limit = limit;
	}

	@Override
	public boolean checkLimit(L2Player player, L2Character target, Object arg1, Object arg2, Object arg3)
	{
		return !limit.checkLimit(player, target, arg1, arg2, arg3);
	}
}
