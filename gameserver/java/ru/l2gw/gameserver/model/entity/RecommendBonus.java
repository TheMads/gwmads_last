package ru.l2gw.gameserver.model.entity;

import ru.l2gw.gameserver.model.L2Player;

public final class RecommendBonus
{
	private static final int[][] _recoBonus = {
		{ 25, 50, 50, 50, 50, 50, 50, 50, 50, 50 },
		{ 16, 33, 50, 50, 50, 50, 50, 50, 50, 50 },
		{ 12, 25, 37, 50, 50, 50, 50, 50, 50, 50 },
		{ 10, 20, 30, 40, 50, 50, 50, 50, 50, 50 },
		{  8, 16, 25, 33, 41, 50, 50, 50, 50, 50 },
		{  7, 14, 21, 28, 35, 42, 50, 50, 50, 50 },
		{  6, 12, 18, 25, 31, 37, 43, 50, 50, 50 },
		{  5, 11, 16, 22, 27, 33, 38, 44, 50, 50 },
		{  5, 10, 15, 20, 25, 30, 35, 40, 45, 50 }
	};

	public static int getRecommendBonus(L2Player activeChar)
	{
		if(activeChar != null && activeChar.isOnline())
		{
			if(activeChar.getRecSystem().getRecommendsHave() == 0)
				return 0;

			int _lvl = (int) Math.ceil(activeChar.getLevel() / 10);
			int _exp = (int) Math.ceil((Math.min(100, activeChar.getRecSystem().getRecommendsHave()) - 1) / 10);

			return _recoBonus[_lvl][_exp];
		}
		return 0;
	}

	public static double getRecommendMultiplier(L2Player activeChar)
	{
		double _multiplier = 1;

		int bonus = getRecommendBonus(activeChar);
		if(bonus > 0)
			_multiplier = (1 + (bonus / 100));

		if(_multiplier < 1)
			_multiplier = 1;

		return _multiplier;
	}
}