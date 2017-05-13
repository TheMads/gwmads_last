package ru.l2gw.gameserver.model.gmaccess.limits;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.IAdminLimit;

/**
 * @author: rage
 * @date: 21.03.12 15:27
 */
public class LogicAndLimit implements IAdminLimit
{
	private final GArray<IAdminLimit> limits;

	public LogicAndLimit()
	{
		this(new GArray<IAdminLimit>(2));
	}

	public LogicAndLimit(GArray<IAdminLimit> limits)
	{
		this.limits = limits;
	}

	@Override
	public boolean checkLimit(L2Player player, L2Character target, Object arg1, Object arg2, Object arg3)
	{
		for(IAdminLimit limit : limits)
			if(!limit.checkLimit(player, target, arg1, arg2, arg3))
				return false;

		return true;
	}

	public void addLimit(IAdminLimit limit)
	{
		limits.add(limit);
	}
}
