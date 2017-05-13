package ru.l2gw.gameserver.templates;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.skills.funcs.FuncTemplate;
import ru.l2gw.commons.math.Rnd;

import java.util.ArrayList;
import java.util.List;

public final class L2Weapon extends L2Item
{
	private final int _soulShotCount;
	private final int _spiritShotCount;
	public final int physical_damage;
	public final int randomDamage;
	public final int attackReuse;
	public final int mpConsume;
	public final int magic_damage;
	public final float hitModify;
	public final float avoidModify;

	public final int attackRange;
	public final int attackSpeed;
	public final int critical;

	private final boolean _isHaveSa;
	private final boolean _isMagicWeapon;

	public enum WeaponType
	{
		NONE(1, "Shield"),
		SWORD(2, "Sword"),
		BLUNT(3, "Blunt"),
		DAGGER(4, "Dagger"),
		BOW(5, "Bow"),
		POLE(6, "Pole"),
		ETC(7, "Etc"),
		FIST(8, "Fist"),
		DUAL(9, "Dual Sword"),
		DUALFIST(10, "Dual Fist"),
		BIGSWORD(11, "Big Sword"), // Two Handed Swords
		ROD(13, "Rod"),
		BIGBLUNT(14, "Big Blunt"),
		CROSSBOW(15, "Crossbow"),
		RAPIER(16, "Rapier"),
		ANCIENTSWORD(17, "Ancient Sword"), // Kamael 2h sword
		DUALDAGGER(18, "Dual Dagger");

		private final int _id;
		private final String _name;

		private WeaponType(int id, String name)
		{
			_id = id;
			_name = name;
		}

		public int mask()
		{
			return 1 << _id;
		}

		@Override
		public String toString()
		{
			return _name;
		}
	}

	/**
	 * Constructor for Weapon.<BR><BR>
	 * <U><I>Variables filled :</I></U><BR>
	 * <LI>_soulShotCount & _spiritShotCount</LI>
	 * <LI>_pDam & _mDam & _rndDam</LI>
	 * <LI>_critical</LI>
	 * <LI>_hitModifier</LI>
	 * <LI>_avoidModifier</LI>
	 * <LI>_shieldDes & _shieldDefRate</LI>
	 * <LI>_atkSpeed & _AtkReuse</LI>
	 * <LI>_mpConsume</LI>
	 *
	 * @param type : L2ArmorType designating the type of armor
	 * @param set  : StatsSet designating the set of couples (key,value) caracterizing the armor
	 * @see L2Item constructor
	 */
	public L2Weapon(WeaponType type, StatsSet set)
	{
		super(type, set);
		_soulShotCount = set.getInteger("soulshots");
		_spiritShotCount = set.getInteger("spiritshots");
		physical_damage = set.getInteger("p_dam");
		randomDamage = set.getInteger("rnd_dam");
		attackReuse = set.getInteger("reuse_delay", type == WeaponType.BOW ? 1500 : type == WeaponType.CROSSBOW ? 400 : 0);
		mpConsume = set.getInteger("mp_consume");
		magic_damage = set.getInteger("m_dam");
		hitModify = set.getFloat("hit_modify");
		avoidModify = set.getFloat("avoid_modify");
		attackRange = set.getInteger("attack_range");
		attackSpeed = set.getInteger("attack_speed");
		critical = set.getInteger("critical");
		_isHaveSa = set.getBool("is_have_sa");
		_isMagicWeapon = set.getBool("is_magic_weapon");

		if(physical_damage != 0)
		{
			attachFunction(new FuncTemplate(null, null, "Set", Stats.POWER_ATTACK, 0x08, physical_damage));
			if(physical_damage > 0)
				attachFunction(new FuncTemplate(null, null, "Enchant", Stats.POWER_ATTACK, 0x0C, 0));
		}
		if(magic_damage != 0)
		{
			attachFunction(new FuncTemplate(null, null, "Set", Stats.MAGIC_ATTACK, 0x08, magic_damage));
			if(magic_damage > 0)
				attachFunction(new FuncTemplate(null, null, "Enchant", Stats.MAGIC_ATTACK, 0x0C, 0));
		}
		if(avoidModify != 0F)
			attachFunction(new FuncTemplate(null, null, (avoidModify > 0 ? "Add" : "Sub"), Stats.EVASION_RATE, 0x10, (avoidModify > 0 ? avoidModify : -avoidModify)));

		final int shield_def = set.getInteger("shield_def");
		final int shield_def_rate = set.getInteger("shield_def_rate");
		if(shield_def != 0)
		{
			attachFunction(new FuncTemplate(null, null, "Set", Stats.SHIELD_DEFENCE, 0x08, shield_def));
			if(shield_def > 0)
				attachFunction(new FuncTemplate(null, null, "Enchant", Stats.SHIELD_DEFENCE, 0x0C, 0D));
		}
		if(shield_def_rate != 0)
			attachFunction(new FuncTemplate(null, null, "Set", Stats.SHIELD_RATE, 0x08, shield_def_rate));
	}

	/**
	 * Returns the type of Weapon
	 *
	 * @return L2WeaponType
	 */
	@Override
	public WeaponType getItemType()
	{
		return (WeaponType) super.type;
	}

	/**
	 * Returns the ID of the Etc item after applying the mask.
	 *
	 * @return int : ID of the Weapon
	 */
	@Override
	public int getItemMask()
	{
		return getItemType().mask();
	}

	/**
	 * Returns the quantity of SoulShot used.
	 *
	 * @return int
	 */
	public int getSoulShotCount()
	{
		return _soulShotCount;
	}

	/**
	 * Returns the quatity of SpiritShot used.
	 *
	 * @return int
	 */
	public int getSpiritShotCount()
	{
		return _spiritShotCount;
	}

	public void getEffect(boolean critNotCast, L2Character effector, L2Character effected, boolean offensive)
	{
		if(effector == null || effected == null)
			return;

		if(_skillOnAction == null)
			return;

		if(_skillOnCritNotCast != critNotCast || _offensive != offensive)
			return;

		if(!Rnd.chance(_chance))
			return;

		List<L2Character> targets = new ArrayList<>(1);
		targets.add(effected);
		_skillOnAction.useSkill(effector, targets);

	}

	@Override
	public boolean isHaveSa()
	{
		return _isHaveSa;
	}

	@Override
	public boolean isMagicWeapon()
	{
		return _isMagicWeapon;
	}
}