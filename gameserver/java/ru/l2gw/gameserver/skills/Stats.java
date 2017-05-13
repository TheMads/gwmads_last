package ru.l2gw.gameserver.skills;

import java.util.NoSuchElementException;

public enum Stats
{
	MAX_HP("maxHp"),
	MAX_MP("maxMp"),
	MAX_CP("maxCp"),
	TRIGGER_BY_DMG("trigger_by_dmg"),
	TRIGGER_BY_ATTACK("trigger_by_attack"),
	TRIGGER_BY_SKILL("trigger_by_skill"),
	TRIGGER_BY_AVOID("trigger_by_evade"),
	HP_LIMIT("hpLimit"),
	CP_LIMIT("cpLimit"),
	MP_LIMIT("mpLimit"),
	REGENERATE_HP_RATE("regHp"),
	REGENERATE_CP_RATE("regCp"),
	REGENERATE_MP_RATE("regMp"),

	RUN_SPEED("runSpd"),

	POWER_DEFENCE("pDef"),
	MAGIC_DEFENCE("mDef"),
	POWER_ATTACK("pAtk"),
	POWER_ATTACK_SKILLS("pAtkSkills"),
	MAGIC_ATTACK("mAtk"),
	POWER_ATTACK_SPEED("pAtkSpd"),
	MAGIC_ATTACK_SPEED("mAtkSpd"),

	MAGIC_REUSE_RATE("mReuse"),
	PHYSIC_REUSE_RATE("pReuse"),
	MUSIC_REUSE_RATE("musicReuse"),
	ATK_REUSE("atkReuse"),

	CRITICAL_DAMAGE("cAtk"),
	CRITICAL_DAMAGE_STATIC("cAtkStatic"),
	EVASION_RATE("rEvas"),
	ACCURACY_COMBAT("accCombat"),
	CRITICAL_RATE("rCrit"),
	MCRITICAL_RATE("mCritRate"),

	PHYSICAL_DAMAGE("physDamage"),
	PHYSICAL_SKILL_DAMAGE("physSkillDamage"),
	MAGIC_DAMAGE("magicDamage"),

	REDUCE_CANCEL("reduce_cancel"),

	SHIELD_DEFENCE("sDef"),
	SHIELD_RATE("rShld"),
	SHIELD_ANGLE("shldAngle"),

	POWER_ATTACK_RANGE("pAtkRange"),
	MAGIC_ATTACK_RANGE("mAtkRange"),
	POLE_ATTACK_ANGLE("poleAngle"),
	POLE_TARGET_COUNT("poleTargetCount"),

	STAT_STR("STR"),
	STAT_CON("CON"),
	STAT_DEX("DEX"),
	STAT_INT("INT"),
	STAT_WIT("WIT"),
	STAT_MEN("MEN"),

	BREATH("breath"),
	FALL_DAMAGE("fallDamage"),
	FALL_SAFE("fallSafe"),
	EXP_LOST("expLost"),
	CLOAK("cloak"),

	BLEED_RECEPTIVE("bleedRcpt"),
	POISON_RECEPTIVE("poisonRcpt"),
	STUN_RECEPTIVE("stunRcpt"),
	ROOT_RECEPTIVE("rootRcpt"),
	FEAR_RECEPTIVE("fearRcpt"),
	SLEEP_RECEPTIVE("sleepRcpt"),
	PARALYZE_RECEPTIVE("paralyzeRcpt"),
	SILENCE_RECEPTIVE("silenceRcpt"),
	CANCEL_RECEPTIVE("cancelRcpt"),
	DEBUFF_RECEPTIVE("debuffRcpt"),
	DEATH_RECEPTIVE("deathRcpt"),
	SLOW_RECEPTIVE("slowRcpt"),
	MAGIC_RECEPTIVE("magicRcpt"),
	MAGIC_FAIL_RATE("magicFailRate"),

	BLEED_POWER("bleedPower"),
	POISON_POWER("poisonPower"),
	STUN_POWER("stunPower"),
	ROOT_POWER("rootPower"),
	SLOW_POWER("slowPower"),

	FEAR_POWER("fearPower"),
	SLEEP_POWER("sleepPower"),
	PARALYZE_POWER("paralyzePower"),
	SILENCE_POWER("silencePower"),
	CANCEL_POWER("cancelPower"),
	DEBUFF_POWER("debuffPower"),
	BLOW_RATE("blowRate"),
	LETHAL_RATE("lethalRate"),

	FIRE_ATTRIBUTE("fireAttr"),
	WIND_ATTRIBUTE("windAttr"),
	WATER_ATTRIBUTE("waterAttr"),
	EARTH_ATTRIBUTE("earthAttr"),
	DARK_ATTRIBUTE("darkAttr"),
	HOLY_ATTRIBUTE("holyAttr"),

	CRIT_DAMAGE_RECEPTIVE("critDamRcpt"),
	CRIT_CHANCE_RECEPTIVE("critChanceRcpt"),

	ATTACK_ELEMENT_FIRE("attackFire"),
	ATTACK_ELEMENT_WATER("attackWater"),
	ATTACK_ELEMENT_WIND("attackWind"),
	ATTACK_ELEMENT_EARTH("attackEarth"),
	ATTACK_ELEMENT_HOLY("attackHoly"),
	ATTACK_ELEMENT_DARK("attackDark"),

	SWORD_WPN_RECEPTIVE("swordWpnRcpt"),
	ANCIENTSWORD_WPN_RECEPTIVE("ancientswordWpnRcpt"),
	DUAL_WPN_RECEPTIVE("dualWpnRcpt"),
	BLUNT_WPN_RECEPTIVE("bluntWpnRcpt"),
	DAGGER_WPN_RECEPTIVE("daggerWpnRcpt"),
	CROSSBOW_WPN_RECEPTIVE("crossBowWpnRcpt"),
	TWO_HANDED_WPN_RECEPTIVE("twoHandedWeaponRcpt"),
	BOW_WPN_RECEPTIVE("bowWpnRcpt"),
	POLE_WPN_RECEPTIVE("poleWpnRcpt"),
	FIST_WPN_RECEPTIVE("fistWpnRcpt"),
	// Reflect Damage
	REFLECT_DAMAGE_PERCENT("reflectDam"),
	REFLECT_MAGIC_DAMAGE_CHANCE("reflectMagicDamChance"),
	REFLECT_MAGIC_DAMAGE_PER("reflectMagicDam"),
	// Decrease damage
	DECREASE_DAMAGE_PER_MP("decreaseDamPerMp"),
	// Absorb HP
	ABSORB_DAMAGE_PERCENT("absorbDam"),
	//Absorb MP
	ABSORB_DAMAGEMP_PERCENT("absorbDamMp"),
	TRANSFER_DAMAGE_PERCENT("transferDam"),
	TRANSFER_DAMAGE_TO_TANK("transfer_damage_to_tank"),
	TANK_ABSORBER_DAMAGE("tank_absorber_damage"),
	REFLECT_PHYSIC_SKILL("reflectPhysicSkill"),
	REFLECT_MELEE_SKILL("reflectMeleeSkill"),
	REFLECT_MAGIC_SKILL("reflectMagicSkill"),
	// Transfer stats to summon
	SERVITOR_TRANSFER_PATK("servitor_trans_patk"),
	SERVITOR_TRANSFER_PDEF("servitor_trans_pdef"),
	SERVITOR_TRANSFER_MATK("servitor_trans_matk"),
	SERVITOR_TRANSFER_MDEF("servitor_trans_mdef"),
	SERVITOR_TRANSFER_MAX_HP("servitor_trans_max_hp"),
	SERVITOR_TRANSFER_MAX_MP("servitor_trans_max_mp"),
	SERVITOR_TRANSFER_P_ATK_SPD("servitor_trans_p_atk_spd"),
	SERVITOR_TRANSFER_M_ATK_SPD("servitor_trans_m_atk_spd"),
	SERVITOR_TRANSFER_C_ATK("servitor_trans_c_atk"),
	SERVITOR_TRANSFER_C_RATE("servitor_trans_c_rate"),
	//Counter Attack
	COUNTER_ATTACK("counterAttack"),
	// Dodge
	DODGE("dodge"),

	HEAL_EFFECTIVNESS("hpEff"),
	HEAL_EFFECTIVNESS_STATIC("hpEffStatic"),
	HEAL_POWER("healPower"),
	HEAL_POWER_STATIC("healPowerStatic"),
	MANAHEAL_EFFECTIVNESS("mpEff"),
	MANAHEAL_EFFECTIVNESS_STATIC("mpEffStatic"),
	CP_EFFECTIVNESS("cpEff"),
	MP_MAGIC_SKILL_CONSUME("mpConsume"),
	MP_PHYSICAL_SKILL_CONSUME("mpConsumePhysical"),
	MP_DANCE_SKILL_CONSUME("mpDanceConsume"),
	MP_USE_BOW("cheapShot"),
	MP_USE_BOW_CHANCE("cheapShotChance"),
	SS_USE_BOW("miser"),
	ACTIVATE_RATE("activateRate"),
	SKILLMASTERY_RATE("skillmastery"),

	MAX_LOAD("maxLoad"),
	CURR_LOAD("currLoad"),
	INVENTORY_LIMIT("inventoryLimit"),
	STORAGE_LIMIT("storageLimit"),
	TRADE_LIMIT("tradeLimit"),
	COMMON_RECIPE_LIMIT("CommonRecipeLimit"),
	DWARVEN_RECIPE_LIMIT("DwarvenRecipeLimit"),
	BUFF_LIMIT("buffLimit"),
	SONGDANCE_LIMIT("SongDanceLimit"),
	SOULS_LIMIT("soulsLimit"),
	SOULS_CONSUME_EXP("soulsExp"),

	BLOCK_HP("block_hp"),
	BLOCK_MP("block_mp"),
	BLOCK_ACT("block_act"),
	BLOCK_MOVE("block_move"),
	BLOCK_BUFF("block_buff"),
	BLOCK_BUFF_SLOT("block_buff_slot"),
	BLOCK_DEBUFF("block_debuff"),
	BLOCK_CONTROL("block_control"),
	BLOCK_HEAL("block_heal"),
	BLOCK_SPELL("block_spell"),
	BLOCK_PHYS_SKILLS("block_physical_skills"),
	BLOCK_PHYS_ATTACK("block_physical_attack"),
	BLOCK_WEAPON("block_weapon_equip"),
	BLOCK_TARGET("block_target"),
	CRITICAL_DAMAGE_FRONT("critical_damage_front"),
	CRITICAL_DAMAGE_SIDE("critical_damage_side"),
	CRITICAL_DAMAGE_BACK("critical_damage_back"),
	BLOW_CRITICAL_DAMAGE_FRONT("blow_critical_damage_front"),
	BLOW_CRITICAL_DAMAGE_SIDE("blow_critical_damage_side"),
	BLOW_CRITICAL_DAMAGE_BACK("blow_critical_damage_back"),
	CRITICAL_RATE_FRONT_BONUS("critical_rate_front_bonus"),
	CRITICAL_RATE_SIDE_BONUS("critical_rate_side_bonus"),
	CRITICAL_RATE_BACK_BONUS("critical_rate_back_bonus"),
	RECOVERY_VP("recovery_vp"),
	CHANGE_VP("change_vp"),
	AVOID_AGGRO("avoid_aggro"),
	CLAN_GATE("clan_gate"),
	PASSIVE("passive"),

	EXP_MODIFY("exp_modify"),
	SP_MODIFY("sp_modify"),
	GRADE_MODIFY("grade_modify"),
	EXP_SP("ExpSpMultiplier");

	public static final int NUM_STATS = values().length;

	private String _value;

	public String getValue()
	{
		return _value;
	}

	private Stats(String s)
	{
		_value = s;
	}

	public static Stats valueOfXml(String name)
	{
		for(Stats s : values())
			if(s.getValue().equals(name))
				return s;

		throw new NoSuchElementException("Unknown name '" + name + "' for enum BaseStats");
	}

	@Override
	public String toString()
	{
		return _value;
	}
}