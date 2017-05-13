package ru.l2gw.gameserver.templates;

public class L2CharTemplate
{
	private StatsSet _set;

	// BaseStats
	public final byte baseSTR;
	public final byte baseCON;
	public final byte baseDEX;
	public final byte baseINT;
	public final byte baseWIT;
	public final byte baseMEN;
	public float baseHpMax;
	public final float baseCpMax;
	public final float baseMpMax;

	/** HP Regen base */
	public float baseHpReg;

	/** MP Regen base */
	public float baseMpReg;

	/** CP Regen base */
	public final float baseCpReg;

	public int basePAtk;
	public int baseMAtk;
	public int basePDef;
	public final int baseMDef;
	public final int basePAtkSpd;
	public final int baseMAtkSpd;
	public final float baseMReuseRate;
	public final int baseShldDef;
	public int baseAtkRange;
	public final int baseShldRate;
	public final int baseCritRate;
	public final int baseRunSpd;
	public final int baseWalkSpd;
	public final int baseRandDam;

	public final int baseAttrAtk;
	public final int baseAttrAtkValue;
	public final int baseAttrDefFire;
	public final int baseAttrDefWater;
	public final int baseAttrDefWind;
	public final int baseAttrDefEarth;
	public final int baseAttrDefHoly;
	public final int baseAttrDefDark;

	public float physicalHitModify;
	public float physicalAvoidModify;
	public float collisionRadius;
	public float collisionHeight;

	public final int corpse_time;

	public L2CharTemplate(StatsSet set)
	{
		_set = set;

		// Base stats
		baseSTR = set.getByte("baseSTR");
		baseCON = set.getByte("baseCON");
		baseDEX = set.getByte("baseDEX");
		baseINT = set.getByte("baseINT");
		baseWIT = set.getByte("baseWIT");
		baseMEN = set.getByte("baseMEN");
		baseHpMax = set.getFloat("baseHpMax", 1);
		baseCpMax = set.getFloat("baseCpMax", 1);
		baseMpMax = set.getFloat("baseMpMax", 1);
		baseHpReg = set.getFloat("baseHpReg");
		baseCpReg = set.getFloat("baseCpReg");
		baseMpReg = set.getFloat("baseMpReg");
		basePAtk = set.getInteger("basePAtk");
		baseMAtk = set.getInteger("baseMAtk");
		basePDef = set.getInteger("basePDef");
		baseMDef = set.getInteger("baseMDef");
		basePAtkSpd = set.getInteger("basePAtkSpd");
		baseMAtkSpd = set.getInteger("baseMAtkSpd");
		baseMReuseRate = set.getFloat("baseMReuseDelay", 1.f);
		baseShldDef = set.getInteger("baseShldDef");
		baseAtkRange = set.getInteger("baseAtkRange");
		baseShldRate = set.getInteger("baseShldRate");
		baseCritRate = set.getInteger("baseCritRate");
		baseRunSpd = set.getInteger("baseRunSpd");
		baseWalkSpd = set.getInteger("baseWalkSpd");
		baseRandDam = set.getInteger("base_rand_dam", 10);
		physicalHitModify = set.getFloat("physical_hit_modify", 0);
		physicalAvoidModify = set.getFloat("physical_avoid_modify", 0);

		baseAttrAtk = set.getInteger("base_attr_attack", -2);
		baseAttrAtkValue = set.getInteger("base_attr_attack_value", 0);
		baseAttrDefFire = set.getInteger("base_attr_def_fire", 0);
		baseAttrDefWater = set.getInteger("base_attr_def_water", 0);
		baseAttrDefWind = set.getInteger("base_attr_def_wind", 0);
		baseAttrDefEarth = set.getInteger("base_attr_def_earth", 0);
		baseAttrDefHoly = set.getInteger("base_attr_def_holy", 0);
		baseAttrDefDark = set.getInteger("base_attr_def_dark", 0);

		// Geometry
		collisionRadius = set.getFloat("collision_radius", 5);
		collisionHeight = set.getFloat("collision_height", 5);
		if(set.getSet().containsKey("corpse_time"))
			corpse_time = set.getInteger("corpse_time", 7) * 1000;
		else
			corpse_time = 7000;
	}

	public int getNpcId()
	{
		return 0;
	}

	public StatsSet getSet()
	{
		return _set;
	}

	public void setSet(StatsSet set)
	{
		_set = set;
	}
}