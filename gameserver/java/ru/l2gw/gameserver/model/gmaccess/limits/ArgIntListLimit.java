package ru.l2gw.gameserver.model.gmaccess.limits;

import ru.l2gw.commons.arrays.ArrayUtils;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.IAdminLimit;

/**
 * @author: rage
 * @date: 21.03.12 15:20
 */
public class ArgIntListLimit implements IAdminLimit
{
	private final int[] intList;
	private final int num;

	public ArgIntListLimit(int num, String list)
	{
		this.num = num;
		intList = ArrayUtils.toIntArray(list);
	}

	@Override
	public boolean checkLimit(L2Player player, L2Character target, Object arg1, Object arg2, Object arg3)
	{
		Object[] args = new Object[]{ null, arg1, arg2, arg3 };

		if(args[num] instanceof Number)
		{
			Number arg = (Number) args[num];
			return ArrayUtils.contains(intList, arg.intValue());
		}

		return true;
	}
}
