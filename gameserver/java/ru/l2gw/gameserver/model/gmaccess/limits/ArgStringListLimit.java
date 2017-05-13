package ru.l2gw.gameserver.model.gmaccess.limits;

import ru.l2gw.commons.arrays.ArrayUtils;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.IAdminLimit;

/**
 * @author: rage
 * @date: 21.03.12 15:23
 */
public class ArgStringListLimit implements IAdminLimit
{
	private final String[] list;
	private final int num;

	public ArgStringListLimit(int num, String list)
	{
		this.num = num;
		this.list = ArrayUtils.toStringArray(list);
	}

	@Override
	public boolean checkLimit(L2Player player, L2Character target, Object arg1, Object arg2, Object arg3)
	{
		Object[] args = new Object[]{ null, arg1, arg2, arg3 };

		return args[num] == null || ArrayUtils.contains(list, args[num].toString());
	}
}

