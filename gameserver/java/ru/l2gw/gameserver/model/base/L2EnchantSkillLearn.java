package ru.l2gw.gameserver.model.base;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.tables.SkillTable;

public final class L2EnchantSkillLearn
{
	// these two build the primary key
	private final int _id;
	private final int _level;
	private int _maxEnchantLevel;

	// not needed, just for easier debug
	private final String _name;
	private final String _type;

	private final int _baseLvl;
	private final int _minSkillLevel;

	public L2EnchantSkillLearn(int id, int lvl, String name, String type, int minSkillLvl, int baseLvl)
	{
		_id = id;
		_level = lvl;
		_baseLvl = baseLvl;
		_minSkillLevel = minSkillLvl;
		_name = name.intern();
		_type = type.intern();
	}

	/**
	 * @return Returns the id.
	 */
	public int getId()
	{
		return _id;
	}

	/**
	 * @return Returns the level.
	 */
	public int getLevel()
	{
		return _level;
	}

	/**
	 * @return Returns the minLevel.
	 */
	public int getBaseLevel()
	{
		return _baseLvl;
	}

	/**
	 * @return Returns the minSkillLevel.
	 */
	public int getMinSkillLevel()
	{
		return _minSkillLevel;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName()
	{
		return _name;
	}

	public int getCostMult()
	{
		return _maxEnchantLevel == 15 ? 5 : 1;
	}

	/**
	 * @return Returns the spCost.
	 */
	public int[] getCost()
	{
		return SkillTable.getInstance().getInfo(_id, 1).isOffensive() ? _priceCombat[_level % 100] : _priceBuff[_level % 100];
	}

	/** Цена заточки неатакующих скиллов */
	private static final int[][] _priceBuff = { {}, //
			{ 51975, 352786 }, // 1
			{ 51975, 352786 }, // 2
			{ 51975, 352786 }, // 3
			{ 78435, 370279 }, // 4
			{ 78435, 370279 }, // 5
			{ 78435, 370279 }, // 6
			{ 105210, 388290 }, // 7
			{ 105210, 388290 }, // 8
			{ 105210, 388290 }, // 9
			{ 132300, 416514 }, // 10
			{ 132300, 416514 }, // 11
			{ 132300, 416514 }, // 12
			{ 159705, 435466 }, // 13
			{ 159705, 435466 }, // 14
			{ 159705, 435466 }, // 15
			{ 187425, 466445 }, // 16
			{ 187425, 466445 }, // 17
			{ 187425, 466445 }, // 18
			{ 215460, 487483 }, // 19
			{ 215460, 487483 }, // 20
			{ 215460, 487483 }, // 21
			{ 243810, 520215 }, // 22
			{ 243810, 520215 }, // 23
			{ 243810, 520215 }, // 24
			{ 272475, 542829 }, // 25
			{ 272475, 542829 }, // 26
			{ 272475, 542829 }, // 27
			{ 304500, 566426 }, // 28, цифра неточная
			{ 304500, 566426 }, // 29, цифра неточная
			{ 304500, 566426 }, // 30, цифра неточная
	};

	/** Цена заточки атакующих скиллов */
	private static final int[][] _priceCombat = { {}, //
			{ 93555, 635014 }, // 1
			{ 93555, 635014 }, // 2
			{ 93555, 635014 }, // 3
			{ 141183, 666502 }, // 4
			{ 141183, 666502 }, // 5
			{ 141183, 666502 }, // 6
			{ 189378, 699010 }, // 7
			{ 189378, 699010 }, // 8
			{ 189378, 699010 }, // 9
			{ 238140, 749725 }, // 10
			{ 238140, 749725 }, // 11
			{ 238140, 749725 }, // 12
			{ 287469, 896981 }, // 13
			{ 287469, 896981 }, // 14
			{ 287469, 896981 }, // 15
			{ 337365, 959540 }, // 16
			{ 337365, 959540 }, // 17
			{ 337365, 959540 }, // 18
			{ 387828, 1002821 }, // 19
			{ 387828, 1002821 }, // 20
			{ 387828, 1002821 }, // 21
			{ 438858, 1070155 }, // 22
			{ 438858, 1070155 }, // 23
			{ 438858, 1070155 }, // 24
			{ 496601, 1142010 }, // 25, цифра неточная
			{ 496601, 1142010 }, // 26, цифра неточная
			{ 496601, 1142010 }, // 27, цифра неточная
			{ 561939, 1218690 }, // 28, цифра неточная
			{ 561939, 1218690 }, // 29, цифра неточная
			{ 561939, 1218690 }, // 30, цифра неточная
	};

	/**
	 * Шанс успешной заточки
	 * @param ply
	 * @return
	 */
	public int getRate(L2Player ply)
	{
		return getEnchantChance(ply.getLevel(), _level % 100, _maxEnchantLevel == 15);
	}

	private static int getEnchantChance(int level, int enchantLevel, boolean thrid)
	{
		int dropChanceLevel = (thrid ? 10 : 25) - (3 * (85 - level));

		if(enchantLevel < dropChanceLevel)
			return 99 - (enchantLevel * 2);
		else
		{
			dropChanceLevel--;
			int chance = 99 - (2 * dropChanceLevel);
			int diff = enchantLevel - dropChanceLevel;
			int pd = 7;
			int d = 7;
			for(int c = 1; c <= diff; c++)
			{
				chance -= d;
				if(chance < 1)
					return 1;

				if(d > 2)
				{
					pd = d;
					d = 2;
				}

				if(c % 3 == 0)
				{
					if(c / 3 >= 3 || pd == 7)
						d = 12;
					else if(pd == 12)
						d = 26;
				}
			}
			return chance < 1 ? 1 : chance;
		}
	}

	public String getType()
	{
		return _type;
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + _id;
		result = PRIME * result + _level;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		if(!(obj instanceof L2EnchantSkillLearn))
			return false;
		L2EnchantSkillLearn other = (L2EnchantSkillLearn) obj;
		return getId() == other.getId() && getLevel() == other.getLevel();
	}

	public void setMaxEnchantLevel(int maxLevel)
	{
		_maxEnchantLevel = maxLevel;
	}

	public int getMaxEnchantLevel()
	{
		return _maxEnchantLevel;
	}
}