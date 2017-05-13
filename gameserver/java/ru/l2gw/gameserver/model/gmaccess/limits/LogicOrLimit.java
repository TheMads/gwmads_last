package ru.l2gw.gameserver.model.gmaccess.limits;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.IAdminLimit;

/**
 * @author: rage
 * @date: 21.03.12 15:29
 */
public class LogicOrLimit implements IAdminLimit
{
	private final GArray<IAdminLimit> limits;

	public LogicOrLimit()
	{
		this(new GArray<IAdminLimit>(2));
	}

	public LogicOrLimit(GArray<IAdminLimit> limits)
	{
		this.limits = limits;
	}

	@Override
	public boolean checkLimit(L2Player player, L2Character target, Object arg1, Object arg2, Object arg3)
	{
		for(IAdminLimit limit : limits)
			if(limit.checkLimit(player, target, arg1, arg2, arg3))
				return true;

		return false;
	}

	public void addLimit(IAdminLimit limit)
	{
		limits.add(limit);
	}
}
