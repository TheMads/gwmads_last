package ru.l2gw.gameserver.templates;

import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.skills.funcs.FuncTemplate;

/**
 * This class is dedicated to the management of armors.
 */
public final class L2Armor extends L2Item
{
	public static final double EMPTY_RING = 5;
	public static final double EMPTY_EARRING = 9;
	public static final double EMPTY_NECKLACE = 13;
	public static final double EMPTY_HELMET = 12;
	public static final double EMPTY_BODY_FIGHTER = 31;
	public static final double EMPTY_LEGS_FIGHTER = 18;
	public static final double EMPTY_BODY_MYSTIC = 15;
	public static final double EMPTY_LEGS_MYSTIC = 8;
	public static final double EMPTY_GLOVES = 8;
	public static final double EMPTY_BOOTS = 7;

	public enum ArmorType
	{
		NONE(1, "None"),
		LIGHT(2, "Light"),
		HEAVY(3, "Heavy"),
		MAGIC(4, "Magic"),
		PET(5, "Pet"),
		SIGIL(6, "Sigil");

		final int _id;
		final String _name;

		ArmorType(int id, String name)
		{
			_id = id;
			_name = name;
		}

		public int mask()
		{
			return 1 << _id + 24;
		}

		@Override
		public String toString()
		{
			return _name;
		}
	}

	/**
	 * Constructor for Armor.<BR><BR>
	 * <U><I>Variables filled :</I></U><BR>
	 * <LI>_avoidModifier</LI>
	 * <LI>_pDef & _mDef</LI>
	 * <LI>_mpBonus & _hpBonus</LI>
	 * @param type : L2ArmorType designating the type of armor
	 * @param set : StatsSet designating the set of couples (key,value) caracterizing the armor
	 * @see L2Item constructor
	 */
	public L2Armor(ArmorType type, StatsSet set)
	{
		super(type, set);

		final int p_def = set.getInteger("p_def");
		final int m_def = set.getInteger("m_def");
		final int mp_bonus = set.getInteger("mp_bonus");
		if(p_def != 0)
		{
			attachFunction(new FuncTemplate(null, null, (p_def > 0 ? "Add" : "Sub"), Stats.POWER_DEFENCE, 0x10, (p_def > 0 ? p_def : -p_def)));
			if(p_def > 0)
				attachFunction(new FuncTemplate(null, null, "Enchant", Stats.POWER_DEFENCE, 0x0C, 0));
		}
		if(m_def != 0)
		{
			attachFunction(new FuncTemplate(null, null, (m_def > 0 ? "Add" : "Sub"), Stats.MAGIC_DEFENCE, 0x10, (m_def > 0 ? m_def : -m_def)));
			if(m_def > 0)
				attachFunction(new FuncTemplate(null, null, "Enchant", Stats.MAGIC_DEFENCE, 0x0C, 0));
		}
		if(mp_bonus != 0F)
			attachFunction(new FuncTemplate(null, null, (mp_bonus > 0 ? "Add" : "Sub"), Stats.MAX_MP, 0x10, (mp_bonus > 0 ? mp_bonus : -mp_bonus)));

		if(set.getFloat("avoid_modify", 0) != 0F)
			attachFunction(new FuncTemplate(null, null, (set.getFloat("avoid_modify") > 0 ? "Add" : "Sub"), Stats.EVASION_RATE, 0x10, (set.getFloat("avoid_modify", 0) > 0 ? set.getFloat("avoid_modify", 0) : -set.getFloat("avoid_modify", 0))));
	}

	/**
	 * Returns the type of the armor.
	 * @return L2ArmorType
	 */
	@Override
	public ArmorType getItemType()
	{
		return (ArmorType) super.type;
	}

	/**
	 * Returns the ID of the item after applying the mask.
	 * @return int : ID of the item
	 */
	@Override
	public final int getItemMask()
	{
		return getItemType().mask();
	}
}