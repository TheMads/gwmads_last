package ru.l2gw.util;

import ru.l2gw.gameserver.model.L2Character;

import java.util.Comparator;

/**
 * @author: rage
 * @date: 16.01.12 14:50
 */
public class SortByHpComparator implements Comparator<L2Character>
{
	private static final SortByHpComparator instance = new SortByHpComparator();

	public static SortByHpComparator getInstance()
	{
		return instance;
	}

	@Override
	public int compare(L2Character c1, L2Character c2)
	{
		if(c1 == null || c2 == null)
			return 0;

		return (int) (c1.getCurrentHp() - c2.getCurrentHp());
	}
}
