package ru.l2gw.gameserver.model.gmaccess.limits;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.IAdminLimit;

/**
 * @author: rage
 * @date: 21.03.12 15:14
 */
public class ArgMinLimit implements IAdminLimit
{
	private final long min;
	private final int num;
	
	public ArgMinLimit(int num, String min)
	{
		this.num = num;
		this.min = Long.parseLong(min);
	}

	@Override
	public boolean checkLimit(L2Player player, L2Character target, Object arg1, Object arg2, Object arg3)
	{
		Object[] args = new Object[]{ null, arg1, arg2, arg3 };

		if(args[num] instanceof Number)
		{
			Number arg = (Number) args[num];
			return arg.longValue() >= min;
		}

		return true;
	}
}
