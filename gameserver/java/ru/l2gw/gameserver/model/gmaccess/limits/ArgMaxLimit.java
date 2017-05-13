package ru.l2gw.gameserver.model.gmaccess.limits;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.IAdminLimit;

/**
 * @author: rage
 * @date: 21.03.12 15:19
 */
public class ArgMaxLimit implements IAdminLimit
{
	private final long max;
	private final int num;

	public ArgMaxLimit(int num, String max)
	{
		this.num = num;
		this.max = Long.parseLong(max);
	}

	@Override
	public boolean checkLimit(L2Player player, L2Character target, Object arg1, Object arg2, Object arg3)
	{
		Object[] args = new Object[]{ null, arg1, arg2, arg3 };

		if(args[num] instanceof Number)
		{
			Number arg = (Number) args[num];
			return arg.longValue() <= max;
		}

		return true;
	}
}
