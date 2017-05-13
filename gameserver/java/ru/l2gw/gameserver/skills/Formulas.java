package ru.l2gw.gameserver.skills;

import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.extensions.scripts.Functions;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.L2Skill.BaseStats;
import ru.l2gw.gameserver.model.L2Skill.ResistType;
import ru.l2gw.gameserver.model.base.Experience;
import ru.l2gw.gameserver.model.base.Race;
import ru.l2gw.gameserver.model.entity.category.CategoryManager;
import ru.l2gw.gameserver.model.instances.*;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.conditions.ConditionPlayerState;
import ru.l2gw.gameserver.skills.conditions.ConditionPlayerState.CheckPlayerState;
import ru.l2gw.gameserver.skills.funcs.Func;
import ru.l2gw.gameserver.templates.L2Armor;
import ru.l2gw.gameserver.templates.L2Weapon;
import ru.l2gw.gameserver.templates.L2Weapon.WeaponType;

import java.util.TreeMap;

public class Formulas
{
	/**
	 * Regen Task period
	 */
	protected static final org.apache.commons.logging.Log _log = LogFactory.getLog(L2Character.class.getName());
	private static final int HP_REGENERATE_PERIOD = 3000; // 3 secs

	public static int MAX_STAT_VALUE = 100;

	public static final double CLOSE_RANGE_BONUS = 1.10;
	public static final double CLOSE_RANGE_NPC_BONUS = 1.10;
	public static final double BACK_BONUS = 1.20;
	public static final double SIDE_BONUS = 1.10;
	public static final double BLOW_CHANCE_CAP = 80;
	public static final double GRACIA_PHYS_SKILLS_MOD = 1.10113;
	public static final double LONG_RANGE_BONUS = 0.70;
	public static final double SOUL_MAX_BONUS = 0.05; // Макс. прибавка от душ (проценты)

	private static final double[] STRCompute = new double[]{1.036, 34.845}; //{1.016, 28.515}; for C1
	private static final double[] INTCompute = new double[]{1.020, 31.375}; //{1.020, 31.375}; for C1
	private static final double[] DEXCompute = new double[]{1.009, 19.360}; //{1.009, 19.360}; for C1
	private static final double[] WITCompute = new double[]{1.050, 20.000}; //{1.050, 20.000}; for C1
	private static final double[] CONCompute = new double[]{1.030, 27.632}; //{1.015, 12.488}; for C1
	private static final double[] MENCompute = new double[]{1.010, -0.060}; //{1.010, -0.060}; for C1

	public static final double[] WITbonus = new double[MAX_STAT_VALUE];
	public static final double[] MENbonus = new double[MAX_STAT_VALUE];
	public static final double[] INTbonus = new double[MAX_STAT_VALUE];
	public static final double[] STRbonus = new double[MAX_STAT_VALUE];
	public static final double[] DEXbonus = new double[MAX_STAT_VALUE];
	public static final double[] CONbonus = new double[MAX_STAT_VALUE];

	static
	{
		for(int i = 0; i < STRbonus.length; i++)
			STRbonus[i] = Math.floor(Math.pow(STRCompute[0], i - STRCompute[1]) * 100 + .5d) / 100;
		for(int i = 0; i < INTbonus.length; i++)
			INTbonus[i] = Math.floor(Math.pow(INTCompute[0], i - INTCompute[1]) * 100 + .5d) / 100;
		for(int i = 0; i < DEXbonus.length; i++)
			DEXbonus[i] = Math.floor(Math.pow(DEXCompute[0], i - DEXCompute[1]) * 100 + .5d) / 100;
		for(int i = 0; i < WITbonus.length; i++)
			WITbonus[i] = Math.floor(Math.pow(WITCompute[0], i - WITCompute[1]) * 100 + .5d) / 100;
		for(int i = 0; i < CONbonus.length; i++)
			CONbonus[i] = Math.floor(Math.pow(CONCompute[0], i - CONCompute[1]) * 100 + .5d) / 100;
		for(int i = 0; i < MENbonus.length; i++)
			MENbonus[i] = Math.floor(Math.pow(MENCompute[0], i - MENCompute[1]) * 100 + .5d) / 100;
	}

	private static final int[] _emptyElement = new int[]{-2, 0};

	private static class FuncMultRegenResting extends Func
	{
		static final FuncMultRegenResting[] _instance = new FuncMultRegenResting[Stats.NUM_STATS];

		static Func getInstance(Stats stat)
		{
			int pos = stat.ordinal();
			if(_instance[pos] == null)
				_instance[pos] = new FuncMultRegenResting(stat);
			return _instance[pos];
		}

		private FuncMultRegenResting(Stats stat)
		{
			super(stat, 0x20, null);
			setCondition(new ConditionPlayerState(CheckPlayerState.RESTING, true));
		}

		@Override
		public void calc(Env env)
		{
			if(!_cond.test(env))
				return;

			if(env.character.isPlayer() && env.character.getLevel() <= 40 && ((L2Player) env.character).getClassId().getLevel() < 3)
				env.value *= 6;
			else
				env.value *= 1.5;
		}
	}

	private static class FuncMultRegenStanding extends Func
	{
		static final FuncMultRegenStanding[] _instance = new FuncMultRegenStanding[Stats.NUM_STATS];

		static Func getInstance(Stats stat)
		{
			int pos = stat.ordinal();
			if(_instance[pos] == null)
				_instance[pos] = new FuncMultRegenStanding(stat);
			return _instance[pos];
		}

		private FuncMultRegenStanding(Stats stat)
		{
			super(stat, 0x20, null);
			setCondition(new ConditionPlayerState(CheckPlayerState.STANDING, true));
		}

		@Override
		public void calc(Env env)
		{
			if(!_cond.test(env))
				return;

			env.value *= 1.1;
		}
	}

	private static class FuncMultRegenRunning extends Func
	{
		static final FuncMultRegenRunning[] _instance = new FuncMultRegenRunning[Stats.NUM_STATS];

		static Func getInstance(Stats stat)
		{
			int pos = stat.ordinal();
			if(_instance[pos] == null)
				_instance[pos] = new FuncMultRegenRunning(stat);
			return _instance[pos];
		}

		private FuncMultRegenRunning(Stats stat)
		{
			super(stat, 0x20, null);
			setCondition(new ConditionPlayerState(CheckPlayerState.RUNNING, true));
		}

		@Override
		public void calc(Env env)
		{
			if(!_cond.test(env))
				return;
			env.value *= 0.7;
		}
	}

	private static class FuncPAtkMod extends Func
	{
		static final FuncPAtkMod _instance = new FuncPAtkMod();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncPAtkMod()
		{
			super(Stats.POWER_ATTACK, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			env.value *= STRbonus[env.character.getSTR()] * env.character.getLevelMod();
		}
	}

	private static class FuncMAtkMod extends Func
	{
		static final FuncMAtkMod _instance = new FuncMAtkMod();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncMAtkMod()
		{
			super(Stats.MAGIC_ATTACK, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			//{Wpn*(lvlbn^2)*[(1+INTbn)^2]+Msty}
			double ib = INTbonus[env.character.getINT()];
			double lvlb = env.character.getLevelMod();
			env.value *= lvlb * lvlb * ib * ib;
		}
	}

	private static class FuncPDefMod extends Func
	{
		static final FuncPDefMod _instance = new FuncPDefMod();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncPDefMod()
		{
			super(Stats.POWER_DEFENCE, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			env.value *= env.character.getLevelMod();
		}
	}

	private static class FuncMDefMod extends Func
	{
		static final FuncMDefMod _instance = new FuncMDefMod();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncMDefMod()
		{
			super(Stats.MAGIC_DEFENCE, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			env.value *= MENbonus[env.character.getMEN()] * env.character.getLevelMod();
		}
	}

	private static class FuncAttackRange extends Func
	{
		static final FuncAttackRange _instance = new FuncAttackRange();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncAttackRange()
		{
			super(Stats.POWER_ATTACK_RANGE, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Weapon weapon = env.character.getActiveWeaponItem();
			if(weapon != null)
				env.value = weapon.attackRange;
		}
	}

	private static class FuncAtkAccuracy extends Func
	{
		static final FuncAtkAccuracy _instance = new FuncAtkAccuracy();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncAtkAccuracy()
		{
			super(Stats.ACCURACY_COMBAT, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			//[Square(DEX)]*6 + lvl + weapon hitbonus;
			env.value += Math.sqrt(env.character.getDEX()) * 6 + env.character.getLevel();
			if(env.character.getLevel() > 69)
				env.value += env.character.getLevel() - 69;
			if(env.character.getLevel() > 77)
				env.value += 2;
			if(env.character.getLevel() > 80)
				env.value += 1;
			if(env.character.getLevel() > 85)
				env.value += 1;

			L2Weapon weapon = env.character.getActiveWeaponItem();
			if(weapon != null)
				env.value += weapon.hitModify;
			else
				env.value += env.character.getTemplate().physicalHitModify;
		}
	}

	private static class FuncAtkEvasion extends Func
	{
		static final FuncAtkEvasion _instance = new FuncAtkEvasion();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncAtkEvasion()
		{
			super(Stats.EVASION_RATE, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			env.value += Math.sqrt(env.character.getDEX()) * 6 + env.character.getLevel();
			if(env.character.getLevel() > 69)
				env.value += env.character.getLevel() - 69;
			if(env.character.getLevel() > 77)
				env.value += 2;
			if(env.character.getLevel() > 80)
				env.value += 1;
			if(env.character.getLevel() > 85)
				env.value += 1;

			L2Weapon weapon = env.character.getActiveWeaponItem();
			if(weapon != null)
				env.value += weapon.avoidModify;
			else
				env.value += env.character.getTemplate().physicalAvoidModify;
		}
	}

	private static class FuncAtkCritical extends Func
	{
		static final FuncAtkCritical _instance = new FuncAtkCritical();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncAtkCritical()
		{
			super(Stats.CRITICAL_RATE, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Weapon weapon = env.character.getActiveWeaponItem();
			if(weapon != null)
				env.value = weapon.critical * 10 * DEXbonus[env.character.getDEX()];
			else
				env.value = env.character.getTemplate().baseCritRate * 10 * DEXbonus[env.character.getDEX()];
		}
	}

	private static class FuncMAtkCriticalPercents extends Func
	{
		static final FuncMAtkCriticalPercents _instance = new FuncMAtkCriticalPercents();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncMAtkCriticalPercents()
		{
			super(Stats.MCRITICAL_RATE, 0x30, null);
		}

		@Override
		public void calc(Env env)
		{
			env.value *= WITbonus[env.character.getWIT()];
			// GF PTS Retail level diff modifier
			if(env.character.getLevel() >= 78 && env.target.getLevel() >= 78)
				env.value += Math.sqrt(env.character.getLevel()) + (env.character.getLevel() - env.target.getLevel()) / 25.;
		}
	}

	private static class FuncMoveSpeed extends Func
	{
		static final FuncMoveSpeed _instance = new FuncMoveSpeed();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncMoveSpeed()
		{
			super(Stats.RUN_SPEED, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			env.value *= DEXbonus[env.character.getDEX()];
		}
	}

	private static class FuncPAtkSpeed extends Func
	{
		static final FuncPAtkSpeed _instance = new FuncPAtkSpeed();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncPAtkSpeed()
		{
			super(Stats.POWER_ATTACK_SPEED, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			env.value *= DEXbonus[env.character.getDEX()];
		}
	}

	private static class FuncMAtkSpeed extends Func
	{
		static final FuncMAtkSpeed _instance = new FuncMAtkSpeed();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncMAtkSpeed()
		{
			super(Stats.MAGIC_ATTACK_SPEED, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			env.value *= WITbonus[env.character.getWIT()];
		}
	}

	private static class FuncHennaSTR extends Func
	{
		static final FuncHennaSTR _instance = new FuncHennaSTR();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncHennaSTR()
		{
			super(Stats.STAT_STR, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Player pc = (L2Player) env.character;
			if(pc != null)
				env.value = Math.max(1, env.value + pc.getHennaStatSTR());
		}
	}

	private static class FuncHennaDEX extends Func
	{
		static final FuncHennaDEX _instance = new FuncHennaDEX();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncHennaDEX()
		{
			super(Stats.STAT_DEX, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Player pc = (L2Player) env.character;
			if(pc != null)
				env.value = Math.max(1, env.value + pc.getHennaStatDEX());
		}
	}

	private static class FuncHennaINT extends Func
	{
		static final FuncHennaINT _instance = new FuncHennaINT();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncHennaINT()
		{
			super(Stats.STAT_INT, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Player pc = (L2Player) env.character;
			if(pc != null)
				env.value = Math.max(1, env.value + pc.getHennaStatINT());
		}
	}

	private static class FuncHennaMEN extends Func
	{
		static final FuncHennaMEN _instance = new FuncHennaMEN();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncHennaMEN()
		{
			super(Stats.STAT_MEN, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Player pc = (L2Player) env.character;
			if(pc != null)
				env.value = Math.max(1, env.value + pc.getHennaStatMEN());
		}
	}

	private static class FuncHennaCON extends Func
	{
		static final FuncHennaCON _instance = new FuncHennaCON();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncHennaCON()
		{
			super(Stats.STAT_CON, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Player pc = (L2Player) env.character;
			if(pc != null)
				env.value = Math.max(1, env.value + pc.getHennaStatCON());
		}
	}

	private static class FuncHennaWIT extends Func
	{
		static final FuncHennaWIT _instance = new FuncHennaWIT();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncHennaWIT()
		{
			super(Stats.STAT_WIT, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Player pc = (L2Player) env.character;
			if(pc != null)
				env.value = Math.max(1, env.value + pc.getHennaStatWIT());
		}
	}

	private static class FuncMaxHp extends Func
	{
		static final FuncMaxHp _instance = new FuncMaxHp();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncMaxHp()
		{
			super(Stats.MAX_HP, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			env.value *= CONbonus[env.character.getCON()];
		}
	}

	private static class FuncMaxCp extends Func
	{
		static final FuncMaxCp _instance = new FuncMaxCp();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncMaxCp()
		{
			super(Stats.MAX_CP, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			env.value *= CONbonus[env.character.getCON()];
		}
	}

	private static class FuncMaxMp extends Func
	{
		static final FuncMaxMp _instance = new FuncMaxMp();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncMaxMp()
		{
			super(Stats.MAX_MP, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			env.value *= MENbonus[env.character.getMEN()];
		}
	}

	private static class FuncPhysicalDamageResists extends Func
	{
		private static final FuncPhysicalDamageResists _instance = new FuncPhysicalDamageResists();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncPhysicalDamageResists()
		{
			super(Stats.PHYSICAL_DAMAGE, 0x30, null);
		}

		@Override
		public void calc(Env env)
		{

			L2Weapon weapon = env.character.getActiveWeaponItem();
			if(weapon != null)
				switch(weapon.getItemType())
				{
					case BOW:
						env.value *= 0.01 * env.target.calcStat(Stats.BOW_WPN_RECEPTIVE, 100, null, null);
						break;
					case CROSSBOW:
						env.value *= 0.01 * env.target.calcStat(Stats.CROSSBOW_WPN_RECEPTIVE, 100, null, null);
						break;
					case BLUNT:
						env.value *= 0.01 * env.target.calcStat(Stats.BLUNT_WPN_RECEPTIVE, 100, null, null);
						break;
					case RAPIER:
					case DAGGER:
					case DUALDAGGER:
						env.value *= 0.01 * env.target.calcStat(Stats.DAGGER_WPN_RECEPTIVE, 100, null, null);
						break;
					case DUAL:
						env.value *= 0.01 * env.target.calcStat(Stats.DUAL_WPN_RECEPTIVE, 100, null, null);
						break;
					case BIGSWORD:
						env.value *= 0.01 * env.target.calcStat(Stats.TWO_HANDED_WPN_RECEPTIVE, 100, null, null);
						env.value *= 0.01 * env.target.calcStat(Stats.SWORD_WPN_RECEPTIVE, 100, null, null);
						break;
					case BIGBLUNT:
						env.value *= 0.01 * env.target.calcStat(Stats.TWO_HANDED_WPN_RECEPTIVE, 100, null, null);
						env.value *= 0.01 * env.target.calcStat(Stats.BLUNT_WPN_RECEPTIVE, 100, null, null);
						break;
					case SWORD:
						env.value *= 0.01 * env.target.calcStat(Stats.SWORD_WPN_RECEPTIVE, 100, null, null);
						break;
					case ANCIENTSWORD:
						env.value *= 0.01 * env.target.calcStat(Stats.ANCIENTSWORD_WPN_RECEPTIVE, 100, null, null);
						break;
					case POLE:
						env.value *= 0.01 * env.target.calcStat(Stats.POLE_WPN_RECEPTIVE, 100, null, null);
						env.value *= 0.01 * env.target.calcStat(Stats.TWO_HANDED_WPN_RECEPTIVE, 100, null, null);
						break;
					case DUALFIST:
						env.value *= 0.01 * env.target.calcStat(Stats.FIST_WPN_RECEPTIVE, 100, null, null);
						break;
				}

			env.value = calcDamageResists(env.skill, env.character, env.target, env.value);
		}
	}

	private static class FuncPhysicalSkillDamageResists extends Func
	{
		private static final FuncPhysicalSkillDamageResists _instance = new FuncPhysicalSkillDamageResists();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncPhysicalSkillDamageResists()
		{
			super(Stats.PHYSICAL_SKILL_DAMAGE, 0x30, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Weapon weapon = env.character.getActiveWeaponItem();
			if(weapon != null)
				switch(weapon.getItemType())
				{
					case BOW:
						env.value *= 0.01 * env.target.calcStat(Stats.BOW_WPN_RECEPTIVE, 100, null, null);
						break;
					case CROSSBOW:
						env.value *= 0.01 * env.target.calcStat(Stats.CROSSBOW_WPN_RECEPTIVE, 100, null, null);
						break;
					case BLUNT:
						env.value *= 0.01 * env.target.calcStat(Stats.BLUNT_WPN_RECEPTIVE, 100, null, null);
						break;
					case RAPIER:
					case DAGGER:
					case DUALDAGGER:
						env.value *= 0.01 * env.target.calcStat(Stats.DAGGER_WPN_RECEPTIVE, 100, null, null);
						break;
					case DUAL:
						env.value *= 0.01 * env.target.calcStat(Stats.DUAL_WPN_RECEPTIVE, 100, null, null);
						break;
					case BIGSWORD:
						env.value *= 0.01 * env.target.calcStat(Stats.TWO_HANDED_WPN_RECEPTIVE, 100, null, null);
						env.value *= 0.01 * env.target.calcStat(Stats.SWORD_WPN_RECEPTIVE, 100, null, null);
						break;
					case BIGBLUNT:
						env.value *= 0.01 * env.target.calcStat(Stats.TWO_HANDED_WPN_RECEPTIVE, 100, null, null);
						env.value *= 0.01 * env.target.calcStat(Stats.BLUNT_WPN_RECEPTIVE, 100, null, null);
						break;
					case SWORD:
						env.value *= 0.01 * env.target.calcStat(Stats.SWORD_WPN_RECEPTIVE, 100, null, null);
						break;
					case ANCIENTSWORD:
						env.value *= 0.01 * env.target.calcStat(Stats.ANCIENTSWORD_WPN_RECEPTIVE, 100, null, null);
						break;
					case POLE:
						env.value *= 0.01 * env.target.calcStat(Stats.POLE_WPN_RECEPTIVE, 100, null, null);
						env.value *= 0.01 * env.target.calcStat(Stats.TWO_HANDED_WPN_RECEPTIVE, 100, null, null);
						break;
					case DUALFIST:
						env.value *= 0.01 * env.target.calcStat(Stats.FIST_WPN_RECEPTIVE, 100, null, null);
						break;
				}

			env.value = calcDamageResists(env.skill, env.character, env.target, env.value);
		}
	}

	private static class FuncMagicDamageResists extends Func
	{
		private static final FuncMagicDamageResists _instance = new FuncMagicDamageResists();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncMagicDamageResists()
		{
			super(Stats.MAGIC_DAMAGE, 0x30, null);
		}

		@Override
		public void calc(Env env)
		{
			env.value = calcDamageResists(env.skill, env.character, env.target, env.value);
		}
	}
/*
	private static class FuncInventory extends Func
	{
		private static final FuncInventory _instance = new FuncInventory();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncInventory()
		{
			super(Stats.INVENTORY_LIMIT, 0x01, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Player _cha = (L2Player) env.character;
			if(_cha.isGM())
				env.value = Config.INVENTORY_MAXIMUM_GM;
			else if(_cha.getTemplate().race == Race.dwarf)
				env.value = Config.INVENTORY_MAXIMUM_DWARF;
			else
				env.value = Config.INVENTORY_MAXIMUM_NO_DWARF;
		}
	}
*/

	private static class FuncWarehouse extends Func
	{
		private static final FuncWarehouse _instance = new FuncWarehouse();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncWarehouse()
		{
			super(Stats.STORAGE_LIMIT, 0x01, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Player _cha = (L2Player) env.character;
			if(_cha.getTemplate().race == Race.dwarf)
				env.value = Config.WAREHOUSE_SLOTS_DWARF;
			else
				env.value = Config.WAREHOUSE_SLOTS_NO_DWARF;
		}
	}

	private static class FuncTradeLimit extends Func
	{
		private static final FuncTradeLimit _instance = new FuncTradeLimit();

		static Func getInstance()
		{
			return _instance;
		}

		private FuncTradeLimit()
		{
			super(Stats.TRADE_LIMIT, 0x01, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Player _cha = (L2Player) env.character;
			if(_cha.getRace() == Race.dwarf)
				env.value = Config.MAX_PVTSTORE_SLOTS_DWARF;
			else
				env.value = Config.MAX_PVTSTORE_SLOTS_OTHER;
		}
	}

	/**
	 * Return the period between 2 regenerations task (3s for L2Character, 5 min for L2DoorInstance).<BR><BR>
	 */
	public static int getRegeneratePeriod(L2Character cha)
	{
		if(cha instanceof L2DoorInstance)
			return HP_REGENERATE_PERIOD * 100; // 5 mins

		return HP_REGENERATE_PERIOD; // 3s
	}

	/**
	 * Add basics Func objects to L2Player and L2Summon.<BR><BR>
	 * <p/>
	 * <B><U> Concept</U> :</B><BR><BR>
	 * A calculator is created to manage and dynamically calculate the effect of a character property (ex : MAX_HP, REGENERATE_HP_RATE...).
	 * In fact, each calculator is a table of Func object in which each Func represents a mathematic function : <BR><BR>
	 * <p/>
	 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<BR><BR>
	 *
	 * @param cha L2Player or L2Summon that must obtain basic Func objects
	 */
	public static void addFuncsToNewCharacter(L2Character cha)
	{
		if(cha.isPlayer())
		{
			cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_CP_RATE));
			cha.addStatFunc(FuncMultRegenStanding.getInstance(Stats.REGENERATE_CP_RATE));
			cha.addStatFunc(FuncMultRegenRunning.getInstance(Stats.REGENERATE_CP_RATE));
			cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_HP_RATE));
			cha.addStatFunc(FuncMultRegenStanding.getInstance(Stats.REGENERATE_HP_RATE));
			cha.addStatFunc(FuncMultRegenRunning.getInstance(Stats.REGENERATE_HP_RATE));
			cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_MP_RATE));
			cha.addStatFunc(FuncMultRegenStanding.getInstance(Stats.REGENERATE_MP_RATE));
			cha.addStatFunc(FuncMultRegenRunning.getInstance(Stats.REGENERATE_MP_RATE));

			cha.addStatFunc(FuncMaxCp.getInstance());
			cha.addStatFunc(FuncMaxHp.getInstance());
			cha.addStatFunc(FuncMaxMp.getInstance());

			cha.addStatFunc(FuncMoveSpeed.getInstance());

			cha.addStatFunc(FuncHennaSTR.getInstance());
			cha.addStatFunc(FuncHennaDEX.getInstance());
			cha.addStatFunc(FuncHennaINT.getInstance());
			cha.addStatFunc(FuncHennaMEN.getInstance());
			cha.addStatFunc(FuncHennaCON.getInstance());
			cha.addStatFunc(FuncHennaWIT.getInstance());

			//cha.addStatFunc(FuncInventory.getInstance());
			cha.addStatFunc(FuncWarehouse.getInstance());
			cha.addStatFunc(FuncTradeLimit.getInstance());

			cha.addStatFunc(FuncPAtkMod.getInstance());
			cha.addStatFunc(FuncMAtkMod.getInstance());
			cha.addStatFunc(FuncPDefMod.getInstance());
			cha.addStatFunc(FuncMDefMod.getInstance());

			//cha.addStatFunc(FuncPAtkSpeed.getInstance());
			cha.addStatFunc(FuncMAtkSpeed.getInstance());
		}
		else if(cha instanceof L2PetInstance)
		{
			cha.addStatFunc(FuncPAtkMod.getInstance());
			cha.addStatFunc(FuncMAtkMod.getInstance());
			cha.addStatFunc(FuncPDefMod.getInstance());
			cha.addStatFunc(FuncMDefMod.getInstance());
		}

		cha.addStatFunc(FuncAttackRange.getInstance());

		cha.addStatFunc(FuncAtkCritical.getInstance());
		cha.addStatFunc(FuncMAtkCriticalPercents.getInstance());
		cha.addStatFunc(FuncAtkAccuracy.getInstance());
		cha.addStatFunc(FuncAtkEvasion.getInstance());

		cha.addStatFunc(FuncPhysicalDamageResists.getInstance());
		cha.addStatFunc(FuncPhysicalSkillDamageResists.getInstance());
		cha.addStatFunc(FuncMagicDamageResists.getInstance());
	}

	/**
	 * Calculate the HP regen rate (base + modifiers).<BR><BR>
	 */
	public static double calcHpRegen(L2Character cha)
	{
		double init;
		if(cha.isPlayer())
			init = (cha.getLevel() <= 10 ? 1.95 + cha.getLevel() / 20. : 1.4 + cha.getLevel() / 10.) * cha.getLevelMod() * CONbonus[cha.getCON()];
		else if(cha instanceof L2PetInstance)
			init = ((L2PetInstance) cha).getTemplate().org_hp_regen * CONbonus[cha.getCON()];
		else
			init = cha.getBaseHpRegen() * CONbonus[cha.getCON()];

		if(Config.ALT_CHAMPION_ENABLE && cha.isChampion() > 0)
			init *= Config.ALT_CHAMPION_HP_REGEN * (cha.isChampion() == 2 ? Config.ALT_CHAMPION2_MUL : 1);

		if(cha instanceof L2Playable)
		{
			L2Player player = cha.getPlayer();
			if(player != null && player.isInClanBase() && player.getRestoreHpLevel() != -1)
				init *= 1. + player.getRestoreHpLevel() / 100.;
		}

		return cha.calcStat(Stats.REGENERATE_HP_RATE, init, null, null);
	}

	/**
	 * Calculate the MP regen rate (base + modifiers).<BR><BR>
	 */
	public static double calcMpRegen(L2Character cha)
	{
		double init;
		if(cha.isPlayer())
			init = (.87 + cha.getLevel() * .03) * cha.getLevelMod() * MENbonus[cha.getMEN()];
		else if(cha instanceof L2PetInstance)
			init = ((L2PetInstance) cha).getTemplate().org_mp_regen * MENbonus[cha.getMEN()];
		else
			init = cha.getTemplate().baseMpReg * MENbonus[cha.getMEN()];

		if(cha instanceof L2Playable)
		{
			L2Player player = cha.getPlayer();
			if(player != null && player.isInClanBase() && player.getRestoreMpLevel() != -1)
				init *= 1. + player.getRestoreMpLevel() / 100.;
		}

		return cha.calcStat(Stats.REGENERATE_MP_RATE, init, null, null);
	}

	/**
	 * Calculate the CP regen rate (base + modifiers).<BR><BR>
	 */
	public static double calcCpRegen(L2Character cha)
	{
		//double init = ((0.08 * cha.getLevel() + cha.getTemplate().baseCpReg) * (0.6 + CONbonus[cha.getCON()])) * (Config.CP_REGEN_MULTIPLIER / 100);
		double init = (1.5 + cha.getLevel() / 10) * cha.getLevelMod() * CONbonus[cha.getCON()];
		return cha.calcStat(Stats.REGENERATE_CP_RATE, init, null, null);
	}

	/**
	 * Рассчет домага только при физической атаке
	 *
	 * @param attacker
	 * @param target
	 * @param shld
	 * @param crit
	 * @param dual
	 * @param ss
	 * @return damage
	 */
	public static double calcPhysDam(L2Character attacker, L2Character target, boolean shld, boolean crit, boolean dual, boolean ss)
	{
		if(target instanceof L2DoorInstance && ((L2DoorInstance) target).isWall() && (!(attacker instanceof L2SummonInstance) || !((L2SummonInstance) attacker).isSiegeWeapon()))
			return 0;

		//if(attacker.getPlayer() != null && target instanceof L2DoorInstance && SiegeManager.getSiege(target) != null && SiegeManager.getSiege(target).isInProgress() && !SiegeManager.getSiege(target).checkIsAttacker(attacker.getPlayer().getClanId()))
		//	return 0;

		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		double critBonus = 0;

		if(shld)
			defence += target.getShldDef();

		if(defence == 0)
			defence = 1;

		if(ss)
			damage *= 2.;

		if(dual)
			damage /= 2;

		double bonus = 70;

		if(attacker.isBehindTarget(target))
			bonus *= BACK_BONUS;
		else if(attacker.isToSideOfTarget())
			bonus *= SIDE_BONUS;

		L2Weapon weapon = attacker.getActiveWeaponItem();
		if(weapon != null && (weapon.getItemType() == WeaponType.BOW || weapon.getItemType() == WeaponType.CROSSBOW))
			bonus *= (1 - LONG_RANGE_BONUS) * (attacker.getDistance3D(target) / attacker.getPhysicalAttackRange()) + LONG_RANGE_BONUS;
		else
			bonus *= attacker instanceof L2Playable ? CLOSE_RANGE_BONUS : CLOSE_RANGE_NPC_BONUS;
		if(crit)
		{
			double rcpt = 0.01 * target.calcStat(Stats.CRIT_DAMAGE_RECEPTIVE, 100, null, null);
			if(attacker.isBehindTarget())
				damage = attacker.calcStat(Stats.CRITICAL_DAMAGE_BACK, damage, target, null);
			else if(attacker.isToSideOfTarget())
				damage = attacker.calcStat(Stats.CRITICAL_DAMAGE_SIDE, damage, target, null);
			else
				damage = attacker.calcStat(Stats.CRITICAL_DAMAGE_FRONT, damage, target, null);

			critBonus = attacker.calcStat(Stats.CRITICAL_DAMAGE_STATIC, 0, target, null) * rcpt * bonus / defence;

			if(attacker instanceof L2SummonInstance && attacker.getPlayer() != null)
			{
				L2Player owner = attacker.getPlayer();
				double cAtk = owner.calcStat(Stats.SERVITOR_TRANSFER_C_ATK, 0, target, null) * 0.01;
				damage = attacker.calcStat(Stats.CRITICAL_DAMAGE, damage, target, null) * rcpt * (cAtk > 0 ? cAtk : 1);
			}
			else
				damage = attacker.calcStat(Stats.CRITICAL_DAMAGE, damage, target, null) * rcpt;

			bonus *= 2;
		}
		damage *= bonus / defence;

		damage += critBonus;
		damage *= 1 + (Rnd.get() * attacker.getRandomDamage() * 2 - attacker.getRandomDamage()) / 100;
		damage = attacker.calcStat(Stats.PHYSICAL_DAMAGE, damage, target, null);

		if(shld && Rnd.chance(5))
			damage = 1;

		return damage < 1 ? 1. : damage;
	}

	/**
	 * Calculated damage caused by ATTACK of attacker on target,
	 * called separatly for each weapon, if dual-weapon is used.
	 * <p/>
	 * param attacker player or NPC that makes ATTACK
	 * param target player or NPC, target of ATTACK
	 * param miss one of ATTACK_XXX constants
	 * param crit if the ATTACK have critical success
	 * param dual if dual weapon is used
	 * param ss if weapon item was changes by soulshot
	 * return damage points
	 */
	public static double calcPhysDam(L2Character attacker, Env env, L2Skill skill, boolean ss)
	{
		L2Character target = env.target;
		if(target instanceof L2DoorInstance && ((L2DoorInstance) target).isWall() && (!(attacker instanceof L2SummonInstance) || !((L2SummonInstance) attacker).isSiegeWeapon()))
			return 0;

		//if(attacker.getPlayer() != null && target instanceof L2DoorInstance && SiegeManager.getSiege(target) != null && SiegeManager.getSiege(target).isInProgress() && !SiegeManager.getSiege(target).checkIsAttacker(attacker.getPlayer().getClanId()))
		//	return 0;

		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		boolean shld = !skill.getShieldIgnore() && Formulas.calcShldUse(attacker, target);
		env.crit = skill.getCritRate() > 0 && calcCrit(attacker, target, 10 * skill.getCritRate() * STRbonus[attacker.getSTR()] * 0.01 * target.calcStat(Stats.CRIT_CHANCE_RECEPTIVE, 100, null, null));
		double power = attacker.calcStat(Stats.POWER_ATTACK_SKILLS, skill.getPower(attacker, target), attacker, skill);

		damage += power;

		if(shld)
			defence += target.getShldDef();

		if(defence == 0)
			defence = 1;

		if(ss)
			damage *= 2.;

		double bonus = 70;

		bonus *= GRACIA_PHYS_SKILLS_MOD; // Gracia Physical Skill Bonus

		damage *= bonus / defence;
		damage = attacker.calcStat(Stats.PHYSICAL_SKILL_DAMAGE, damage, target, skill);
		damage *= 1 + (Rnd.get() * attacker.getRandomDamage() * 2 - attacker.getRandomDamage()) / 100;

		if(env.crit)
			damage *= 2;

		if(shld && Rnd.chance(5))
			damage = 1;

		if(shld)
		{
			if(damage == 1)
				target.sendPacket(Msg.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
			else
				target.sendPacket(Msg.SHIELD_DEFENSE_HAS_SUCCEEDED);
			attacker.onShield(target);
		}

		if(target.isStunned() && calcStunBreak(env.crit))
			target.stopEffects("stun");

		if(calcCastBreak(attacker, target, damage))
			target.breakCast(false, true);

		return damage < 1 ? 1. : damage;
	}

	public static double calcSoulPhysDam(L2Character attacker, Env env, L2Skill skill, boolean ss, double critChance)
	{
		L2Character target = env.target;
		if(target instanceof L2DoorInstance && ((L2DoorInstance) target).isWall() && (!(attacker instanceof L2SummonInstance) || !((L2SummonInstance) attacker).isSiegeWeapon()))
			return 0;

		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		boolean shld = !skill.getShieldIgnore() && Formulas.calcShldUse(attacker, target);
		env.crit = Rnd.chance(critChance * STRbonus[attacker.getSTR()]);
		double power = attacker.calcStat(Stats.POWER_ATTACK_SKILLS, skill.getPower(attacker, target), attacker, skill);

		damage += power;

		if(skill.getMaxSoulsConsume() > 0 && attacker.getConsumedSouls() > 0)
			damage *= 1.0 + SOUL_MAX_BONUS * Math.min(attacker.getConsumedSouls(), skill.getMaxSoulsConsume());

		if(shld)
			defence += target.getShldDef();

		if(defence == 0)
			defence = 1;

		if(ss)
			damage *= 2.;

		double bonus = 70;

		damage *= bonus / defence;
		damage = attacker.calcStat(Stats.PHYSICAL_SKILL_DAMAGE, damage, target, skill);
		//double d = crit ? damage * 2 : damage;
		//_log.info(attacker + " soul attack dmg: " + d + " min: " + (d * (1.0 - attacker.getRandomDamage() / 100.)) + " max: " + (d * (1.0 + attacker.getRandomDamage() / 100.)) + " crit: " + crit + " rnd: " + (1.0 - attacker.getRandomDamage() / 100.) + "/" + (1.0 + attacker.getRandomDamage() / 100.));
		damage *= 1.0 + (Rnd.get() * attacker.getRandomDamage() * 2 - attacker.getRandomDamage()) / 100.;

		if(env.crit)
			damage *= 2;

		if(shld && Rnd.chance(5))
			damage = 1;

		if(shld)
		{
			if(damage == 1)
				target.sendPacket(Msg.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
			else
				target.sendPacket(Msg.SHIELD_DEFENSE_HAS_SUCCEEDED);
			attacker.onShield(target);
		}

		if(target.isStunned() && calcStunBreak(env.crit))
			target.stopEffects("stun");

		if(calcCastBreak(attacker, target, damage))
			target.breakCast(false, true);

		return damage < 1 ? 1. : damage;
	}

	public static double calcEnergyDam(L2Character attacker, Env env, L2Skill skill, boolean ss)
	{
		L2Character target = env.target;
		if(target instanceof L2DoorInstance && ((L2DoorInstance) target).isWall() && (!(attacker instanceof L2SummonInstance) || !((L2SummonInstance) attacker).isSiegeWeapon()))
			return 0;

		//if(attacker.getPlayer() != null && target instanceof L2DoorInstance && SiegeManager.getSiege(target) != null && SiegeManager.getSiege(target).isInProgress() && !SiegeManager.getSiege(target).checkIsAttacker(attacker.getClanId()))
		//	return 0;

		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		boolean shld = !skill.getShieldIgnore() && Formulas.calcShldUse(attacker, target);
		env.crit = skill.getCritRate() > 0 && calcCrit(attacker, target, 10 * skill.getCritRate() * STRbonus[attacker.getSTR()] * 0.01 * target.calcStat(Stats.CRIT_CHANCE_RECEPTIVE, 100, null, null));
		double power = attacker.calcStat(Stats.POWER_ATTACK_SKILLS, skill.getPower(attacker, target), attacker, skill);
		damage += power;
		if(shld)
			defence += target.getShldDef();

		if(defence == 0)
			defence = 1;

		if(ss)
			damage *= 2.04;
		damage *= 1 + 0.2 * (skill.getNumCharges() + attacker.getIncreasedForce() - 1);
		double bonus = 70;

		damage *= bonus / defence;
		damage *= 1 + (Rnd.get() * attacker.getRandomDamage() * 2 - attacker.getRandomDamage()) / 100;
		damage = attacker.calcStat(Stats.PHYSICAL_SKILL_DAMAGE, damage, target, skill);
		if(env.crit)
			damage *= 2;

		if(shld && Rnd.chance(5))
			damage = 1;

		if(shld)
		{
			if(damage == 1)
				target.sendPacket(Msg.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
			else
				target.sendPacket(Msg.SHIELD_DEFENSE_HAS_SUCCEEDED);
			attacker.onShield(target);
		}

		if(target.isStunned() && calcStunBreak(env.crit))
			target.stopEffects("stun");

		if(calcCastBreak(attacker, target, damage))
			target.breakCast(false, true);
		return damage < 1 ? 1. : damage;
	}

	public static boolean calcStunBreak(boolean crit)
	{
		return Rnd.chance(crit ? 75 : 10);
	}

	public static double calcCriticalHeightBonus(L2Character attacker, L2Character target)
	{
		int diffZ = attacker.getZ() - target.getZ();
		diffZ = Math.min(25, Math.max(-25, diffZ));
		return (diffZ * 4.0 / 5.0 + 10) / 100 + 1;
	}

	public static double calcCriticalRatePosBonus(L2Character attacker, L2Character target)
	{
		switch(getDirectionAt(attacker, target))
		{
			case 1:
				return attacker.calcStat(Stats.CRITICAL_RATE_SIDE_BONUS, 1.1, target, null);
			case 2:
				return attacker.calcStat(Stats.CRITICAL_RATE_BACK_BONUS, 1.3, target, null);
		}

		return attacker.calcStat(Stats.CRITICAL_RATE_FRONT_BONUS, 1, target, null);
	}

	public static double[] calcCriticalPosBonus(L2Character attacker, L2Character target)
	{
		double[] res = new double[3];
		switch(getDirectionAt(attacker, target))
		{
			case 0:
				res[0] = 0.;
				res[1] = (attacker.calcStat(Stats.BLOW_CRITICAL_DAMAGE_FRONT, 1, target, null) - 1) * 0.5 + 1;
				res[2] = 0; // must be a p_critical_damage_position diff
				break;
			case 1:
				res[0] = 0.05;
				res[1] = (attacker.calcStat(Stats.BLOW_CRITICAL_DAMAGE_SIDE, 1, target, null) - 1) * 0.5 + 1;
				res[2] = 0;
				break;
			case 2:
				res[0] = 0.2;
				res[1] = (attacker.calcStat(Stats.BLOW_CRITICAL_DAMAGE_BACK, 1, target, null) - 1) * 0.5 + 1;
				res[2] = 0;
				break;
		}

		return res;
	}

	public static int getDirectionAt(L2Character attacker, L2Character target)
	{
		int h = (int) (Math.atan2(target.getY() - attacker.getY(), target.getX() - attacker.getX()) * 65535.0 / (Math.PI * 2));
		h = Math.abs(target.getHeading() - h);

		if(h >= 8192 && h <= 24576 || h >= 40960 && h <= 57344)
			return 1;
		if(h > 24576 && h < 40960)
			return 0;

		return 2;
	}

	public static double calcBlowDamage(L2Character attacker, L2Character target, L2Skill skill, boolean ss, boolean useSoul, double critChance)
	{
		if(target instanceof L2DoorInstance && ((L2DoorInstance) target).isWall() && (!(attacker instanceof L2SummonInstance) || !((L2SummonInstance) attacker).isSiegeWeapon()))
			return 0;

		//if(attacker.getPlayer() != null && target instanceof L2DoorInstance && SiegeManager.getSiege(target) != null && SiegeManager.getSiege(target).isInProgress() && !SiegeManager.getSiege(target).checkIsAttacker(attacker.getClanId()))
		//	return 0;
		int shield = calcShieldUse(attacker, target, 3);
		double p_def = target.getPDef(attacker);

		if(shield == 2)
		{
			attacker.onShield(target);
			target.sendPacket(Msg.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
			return 1.0;
		}
		if(shield == 1)
		{
			attacker.onShield(target);
			target.sendPacket(Msg.SHIELD_DEFENSE_HAS_SUCCEEDED);
			p_def += target.getShldDef();
		}

		double ss_bonus = ss ? 2 : 1;
		double pos[] = calcCriticalPosBonus(attacker, target);
		double weapon_random = 1 + (Rnd.get() * attacker.getRandomDamage() * 2 - attacker.getRandomDamage()) / 100;
		double damage_bonus = (attacker.getPAtk(target) * ss_bonus + skill.getPower(attacker, target)) * weapon_random * pos[0];
		double soul_bonus = useSoul && skill.getMaxSoulsConsume() > 0 && attacker.getConsumedSouls() > 0 ? 1.3 + SOUL_MAX_BONUS * Math.min(attacker.getConsumedSouls(), skill.getMaxSoulsConsume()) : 1;

		double damage = attacker.getPAtk(target) * ss_bonus + skill.getPower(attacker, target);
		damage *= weapon_random;
		damage *= soul_bonus;
		damage = attacker.calcStat(Stats.CRITICAL_DAMAGE, damage, target, skill);
		damage = target.calcStat(Stats.CRIT_DAMAGE_RECEPTIVE, damage, attacker, skill);
		damage *= pos[1];
		damage += damage_bonus;
		damage *= 77;
		damage /= p_def;
		damage = attacker.calcStat(Stats.PHYSICAL_SKILL_DAMAGE, damage, target, skill);

		if(Rnd.chance(critChance))
			damage *= 2;

		//if(attacker.isPlayer())
		//	_log.info("calc blow damage: " + attacker + " -> " + target + " db: " + damage_bonus + " sb: " + soul_bonus + " wr: " + weapon_random + " p1: " + pos[0] + " p2: " + pos[1] + " dmg: " + damage);

		return damage < 1 ? 1. : damage;
	}

	public static double calcBackstabDamage(L2Character attacker, L2Character target, L2Skill skill, boolean ss)
	{
		if(target instanceof L2DoorInstance && ((L2DoorInstance) target).isWall() && (!(attacker instanceof L2SummonInstance) || !((L2SummonInstance) attacker).isSiegeWeapon()))
			return 0;

		//if(attacker.getPlayer() != null && target instanceof L2DoorInstance && SiegeManager.getSiege(target) != null && SiegeManager.getSiege(target).isInProgress() && !SiegeManager.getSiege(target).checkIsAttacker(attacker.getClanId()))
		//	return 0;

		double damage = attacker.getPAtk(target);
		double defence = target.getPDef(attacker);
		boolean shld = !skill.getShieldIgnore() && Formulas.calcShldUse(attacker, target);

		if(shld)
			defence += target.getShldDef();
		if(defence == 0)
			defence = 1;

		double power = attacker.calcStat(Stats.POWER_ATTACK_SKILLS, skill.getPower(attacker, target), attacker, skill);

		damage = attacker.calcStat(Stats.CRITICAL_DAMAGE, damage + power, target, skill);

		if(ss)
			damage *= 1.5;

		if(attacker.isBehindTarget())
			damage = BACK_BONUS * attacker.calcStat(Stats.BLOW_CRITICAL_DAMAGE_BACK, damage, target, skill); // Proximity Bonus
		else if(attacker.isToSideOfTarget())
			damage = SIDE_BONUS * attacker.calcStat(Stats.BLOW_CRITICAL_DAMAGE_SIDE, damage, target, skill); // Proximity Bonus
		else
			damage = attacker.calcStat(Stats.BLOW_CRITICAL_DAMAGE_FRONT, damage, target, skill);

		damage += 6.1 * attacker.calcStat(Stats.CRITICAL_DAMAGE_STATIC, 0, target, skill);

		double rcpt = 0.01 * target.calcStat(Stats.CRIT_DAMAGE_RECEPTIVE, 100, null, null);
		damage *= rcpt;
		double bonus = 70 * GRACIA_PHYS_SKILLS_MOD; // Gracia Physical Skill Bonus

		damage *= bonus / defence;
		damage *= 1 + (Rnd.get() * attacker.getRandomDamage() * 2 - attacker.getRandomDamage()) / 100;
		damage = attacker.calcStat(Stats.PHYSICAL_SKILL_DAMAGE, damage, target, skill);

		boolean skillCrit = Formulas.calcCrit(attacker, target, 100 * Formulas.STRbonus[attacker.getSTR()] * 0.01 * target.calcStat(Stats.CRIT_CHANCE_RECEPTIVE, 100, null, null));
		if(skillCrit)
			damage *= 2;

		// Excellent shield defence, 5% на прохождение
		if(shld)
		{
			if(Rnd.chance(5))
			{
				damage = 1;
				target.sendPacket(Msg.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
			}
			else
				target.sendPacket(Msg.SHIELD_DEFENSE_HAS_SUCCEEDED);
			attacker.onShield(target);
		}

		/*
		if(damage > 20000 && attacker.isPlayer() && target.isPlayer())
		{
			_log.info("calcBlowDamange: " + attacker + " --> " + target + " did damage: " + damage + " skill: " + skill + " skillCrit: " + skillCrit + " ss: " + ss);
			_log.info("calcBlowDamange: attacker weapon: " + attacker.getActiveWeaponInstance() + " attribute: " + (attacker.getActiveWeaponInstance() != null ? attacker.getActiveWeaponInstance().getAttackElement()[0] + " value: " + attacker.getActiveWeaponInstance().getAttackElement()[1] : "no weapon"));
			_log.info("calcBlowDamange: attacker p.atk: " + attacker.getPAtk(target) + " p.def: " + attacker.getPDef(target));
			_log.info("calcBlowDamange: target   p.atk: " + target.getPAtk(attacker) + " p.def: " + target.getPDef(attacker));
			for(L2Effect e : attacker.getAllEffects())
				_log.info("calcBlowDamange:: " + attacker + " " + e);
		}
        */
		return damage < 1 ? 1. : damage;
	}

	public static double calcMagicDam(L2Character attacker, L2Character target, L2Skill skill, boolean shield, int sps)
	{
		if(target instanceof L2DoorInstance && ((L2DoorInstance) target).isWall() && (!(attacker instanceof L2SummonInstance) || !((L2SummonInstance) attacker).isSiegeWeapon()))
			return 0;

		//if(attacker.getPlayer() != null && target instanceof L2DoorInstance && SiegeManager.getSiege(target) != null && SiegeManager.getSiege(target).isInProgress() && !SiegeManager.getSiege(target).checkIsAttacker(attacker.getPlayer().getClanId()))
		//	return 0;

		double mAtk = attacker.getMAtk(target, skill);
		if(skill.isForCubic() && skill.getMatak() > 0)
			mAtk = skill.getMatak();

		if(sps == 2)
			mAtk *= 4;
		else if(sps == 1)
			mAtk *= 2;

		double mdef = target.getMDef(null, skill);

		if(shield && skill.isShildHit())
			mdef += target.getShldDef() * 0.40; // from i_m_attack_range;
		if(mdef == 0)
			mdef = 1;

		double damage;

		// GF PTS Retail formula: damage = 91 * skillPower * sqrt(mAtck * soulBonus) / mDef * traitBonus * attrBonus * weaponRandom
		// where soulBonus = 0.05 * soulCount + 1.30
		double soulBonus = 1;
		if(skill.getMaxSoulsConsume() > 0 && attacker.getConsumedSouls() > 0)
			soulBonus = Math.min(attacker.getConsumedSouls(), skill.getMaxSoulsConsume()) * SOUL_MAX_BONUS + 1.30;

		damage = 91 * skill.getPower(attacker, target) * Math.sqrt(mAtk * soulBonus) / mdef;

		boolean crit = calcMCrit(attacker.getCriticalMagic(target, skill));

		if(crit)
		{
			damage *= target.isPlayable() ? 2.5 : 3.;  // Gracia Final patch notes: In PvP situations, magic critical damage has been decreased.
			attacker.sendPacket(Msg.MAGIC_CRITICAL_HIT);
		}

		damage *= 1 + (Rnd.get() * attacker.getRandomDamage() * 2 - attacker.getRandomDamage()) / 100;
		damage = attacker.calcStat(Stats.MAGIC_DAMAGE, damage, target, skill);

		if(!skill.getShieldIgnore() && !attacker.isRaid() && Rnd.chance((int) target.calcStat(Stats.REFLECT_MAGIC_DAMAGE_CHANCE, 0, attacker, skill)))
		{
			double reflectedDamage = damage * (target.calcStat(Stats.REFLECT_MAGIC_DAMAGE_PER, 0, attacker, skill) / 100);
			damage -= reflectedDamage;
			boolean blockHp = attacker.isStatActive(Stats.BLOCK_HP);
			target.sendPacket(Msg.SHIELD_DEFENSE_HAS_SUCCEEDED);
			target.sendDamageMessage(attacker, (int) reflectedDamage, false, false, blockHp);
			if(!blockHp)
				attacker.reduceHp(reflectedDamage, target, false, true);
			return damage < 1 ? -1 : damage;
		}

		if(shield && skill.isShildHit())
		{
			if(Rnd.chance(DEXbonus[target.getDEX()] * 2))
			{
				damage = 1;
				target.sendPacket(Msg.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
			}
			else
			{
				target.sendPacket(Msg.SHIELD_DEFENSE_HAS_SUCCEEDED);
			}
			attacker.onShield(target);
		}

		byte mLevel = skill.getMagicLevel() == 0 ? attacker.getLevel() : skill.getMagicLevel();
		//double levelDiff = target.getLevel() - mLevel;
		double levelDiff = 2 / Formulas.getC4LevelMod(mLevel, target.getLevel());
		double failChance = target.calcStat(Stats.MAGIC_RECEPTIVE, Math.max(Config.SKILLS_LOW_CHANCE_CAP, levelDiff), attacker, skill);
		failChance = attacker.calcStat(Stats.MAGIC_FAIL_RATE, failChance, attacker, skill);
		if(failChance > Config.SKILLS_HIGH_CHANCE_CAP)
			failChance = Config.SKILLS_HIGH_CHANCE_CAP;
		if(failChance < (Config.SKILLS_LOW_CHANCE_CAP))
			failChance = Config.SKILLS_LOW_CHANCE_CAP;
		if(Rnd.chance(failChance))
		{
			damage /= 2;
			attacker.sendPacket(new SystemMessage(SystemMessage.DAMAGE_IS_DECREASED_BECAUSE_C1_RESISTED_AGAINST_C2S_MAGIC).addCharName(target).addCharName(attacker));
			target.sendPacket(new SystemMessage(SystemMessage.S1_WEAKLY_RESISTED_S2S_MAGIC).addCharName(target).addCharName(attacker));
		}

		if(calcCastBreak(attacker, target, damage))
			target.breakCast(false, true);

		return damage;
	}

	public static double calcManaDam(L2Character attacker, L2Character target, L2Skill skill, int sps)
	{
		//Mana Burnt = (SQR(M.Atk)*Power*(Target Max MP/108))/M.Def - checked and calculated on official server
		double mAtk = attacker.getMAtk(target, skill);
		double mDef = target.getMDef(attacker, skill);
		double mp = target.getMaxMp();

		if(sps == 2)
			mAtk *= 4;
		else if(sps == 1)
			mAtk *= 2;

		double damage = (Math.sqrt(mAtk) * skill.getPower(attacker, target) * (mp / 108)) / mDef;

		byte mLevel = skill.getMagicLevel() == 0 ? attacker.getLevel() : skill.getMagicLevel();
		double levelDiff = target.getLevel() - mLevel;
		double failChance = target.calcStat(Stats.MAGIC_RECEPTIVE, Math.max(Config.SKILLS_LOW_CHANCE_CAP, levelDiff), attacker, skill);
		failChance = attacker.calcStat(Stats.MAGIC_FAIL_RATE, failChance, attacker, skill);
		if(failChance > Config.SKILLS_HIGH_CHANCE_CAP)
			failChance = Config.SKILLS_HIGH_CHANCE_CAP;
		if(failChance < (Config.SKILLS_LOW_CHANCE_CAP))
			failChance = Config.SKILLS_LOW_CHANCE_CAP;
		if(Rnd.chance(failChance))
		{
			double fullFail = levelDiff;
			if(fullFail > Config.SKILLS_HIGH_CHANCE_CAP)
				fullFail = Config.SKILLS_HIGH_CHANCE_CAP;
			if(fullFail < (Config.SKILLS_LOW_CHANCE_CAP))
				fullFail = Config.SKILLS_LOW_CHANCE_CAP;

			if(Rnd.chance(fullFail))
			{
				damage = (1 + Rnd.get(149)) / 100;
				attacker.sendPacket(new SystemMessage(SystemMessage.S1S_ATTACK_FAILED).addCharName(attacker));
			}
			else
			{
				damage /= 2;
				attacker.sendPacket(new SystemMessage(SystemMessage.DAMAGE_IS_DECREASED_BECAUSE_C1_RESISTED_AGAINST_C2S_MAGIC).addCharName(target).addCharName(attacker));
				target.sendPacket(new SystemMessage(SystemMessage.S1_WEAKLY_RESISTED_S2S_MAGIC).addCharName(target).addCharName(attacker));
			}
		}

		return damage;
	}

	/**
	 * Returns true in case of fatal blow success
	 */
	public static boolean calcBlowChance(L2Character cha, L2Character target, double effectBonus)
	{
		double chance;

		L2Weapon weapon = cha.getActiveWeaponItem();
		if(weapon != null)
			chance = weapon.critical;
		else
			chance = cha.getTemplate().baseCritRate;

		chance *= DEXbonus[cha.getDEX()];
		chance *= calcCriticalHeightBonus(cha, target);
		chance *= calcCriticalRatePosBonus(cha, target);
		chance *= (effectBonus + 100) / 100;
		chance = cha.calcStat(Stats.BLOW_RATE, chance, null, null);
		chance = Math.min(chance, BLOW_CHANCE_CAP); // Blow Chance Cap

		if(Config.SKILLS_SHOW_CHANCE && cha.isPlayer())
			cha.sendMessage(new CustomMessage("ru.l2gw.gameserver.skills.Formulas.Chance", cha).addString("Blow").addNumber((int) chance));

		return Rnd.chance(chance);
	}

	/**
	 * Returns true in case of critical hit
	 */
	public static boolean calcCrit(L2Character attacker, L2Character target, double rate)
	{
		if(attacker.isBehindTarget())
			rate *= attacker.calcStat(Stats.CRITICAL_RATE_BACK_BONUS, 1, null, null);
		else if(attacker.isToSideOfTarget())
			rate *= attacker.calcStat(Stats.CRITICAL_RATE_SIDE_BONUS, 1, null, null);
		else
			rate *= attacker.calcStat(Stats.CRITICAL_RATE_FRONT_BONUS, 1, null, null);
		return Rnd.get() * 1000 <= rate;
	}

	/**
	 * Returns true in case of magic critical hit
	 */
	public static boolean calcMCrit(double mRate)
	{
		// floating point random gives more accuracy calculation, because argument also floating point
		return Rnd.get() * 100 <= mRate;
	}

	/**
	 * Returns true in case when ATTACK is canceled due to hit
	 */
	public static boolean calcCastBreak(L2Character attacker, L2Character target, double damage)
	{
		if(!target.isCastingNow())
			return false;

		double interruptFactor = 100. * (int) target.calcStat(Stats.REDUCE_CANCEL, damage, null, null) / (double) target.getMaxHp();
		double levelFactor = target.getLevel() + 0.125 * target.getMEN() - attacker.getLevel();
		return Rnd.chance(levelFactor > interruptFactor ? 5 : Math.min((int) (2 * interruptFactor), 98));
	}

	/**
	 * Calculate delay (in milliseconds) before next ATTACK
	 */
	public static int calcPAtkSpd(double rate)
	{
		return (int) (500000 / rate); // в миллисекундах поэтому 500*1000
	}

	public static double calcCastSpeedFactor(L2Character attacker, L2Skill skill)
	{
		if(skill.isStaticHitTime() || skill.getMagicType() == 2)
			return 1;
		if(skill.isMagic())
			return attacker.getMAtkSpd() * (attacker.getChargedSpiritShot() > 0 ? 1.40 : 1.) / 333.;

		return attacker.getPAtkSpd() / 333.;
	}

	/**
	 * Calculate reuse delay (in milliseconds) for skills
	 */
	public static long calcSkillReuseDelay(L2Character actor, L2Skill skill)
	{
		long reuseDelay;
		
		if(skill.isUseMacro() && actor.isPlayer() && actor.getPlayer().isUseMacro() && skill.getReuseDelay() < 9000)
			reuseDelay = skill.getReuseDelay() - 3000;
		else
			reuseDelay = skill.getReuseDelay();

		if(!skill.isHandler() && !skill.altUse() && !skill.isTriggered() && actor.getSkillMastery(skill.getId()) == 1)
		{
			actor.removeSkillMastery(skill.getId());
			if(Rnd.chance(1))
				actor.sendPacket(Msg.A_SKILL_IS_READY_TO_BE_USED_AGAIN);
			else
			{
				actor.setSkillMasteryReuse(skill.getId());
				actor.sendPacket(Msg.A_SKILL_IS_READY_TO_BE_USED_AGAIN_BUT_ITS_RE_USE_COUNTER_TIME_HAS_INCREASED);
			}
			return 0;
		}

		int sm = 1;
		if(actor.isSkillMasterReuse(skill.getId()))
		{
			sm = 2;
			actor.removeSkillMasteryReuse(skill.getId());
		}

		if(skill.isStaticReuse())
			return reuseDelay * sm;

		if(skill.isSongDance())
			return (long) actor.calcStat(Stats.MUSIC_REUSE_RATE, reuseDelay, null, skill) * sm;
		if(skill.isMagic())
			return (long) actor.calcStat(Stats.MAGIC_REUSE_RATE, reuseDelay, null, skill) * sm;
		return (long) actor.calcStat(Stats.PHYSIC_REUSE_RATE, reuseDelay, null, skill) * sm;
	}

	/**
	 * Returns true if hit missed (target evaded)
	 */
	public static boolean calcHitMiss(L2Character attacker, L2Character target)
	{
		double chanceToHit = 100 - 10 * Math.pow(1.085, target.getEvasionRate(attacker) - attacker.getAccuracy());

		chanceToHit = Math.max(chanceToHit, 27.5);
		chanceToHit = Math.min(chanceToHit, 98);
		if(attacker.isBehindTarget(target))
			chanceToHit *= 1.2;
		else if(attacker.isToSideOfTarget(target))
			chanceToHit *= 1.1;
		return !Rnd.chance(chanceToHit);
	}

	/**
	 * Returns true if shield defence successfull
	 */
	public static int calcShieldUse(L2Character attacker, L2Character target, double chanceMod)
	{
		double shieldRate = target.calcStat(Stats.SHIELD_RATE, target.getTemplate().baseShldRate, attacker, null);
		if(shieldRate < 1)
			return 0;

		int angle = (int) target.calcStat(Stats.SHIELD_ANGLE, 60, null, null);

		if(!target.isInFront(attacker, angle))
			return 0;

		shieldRate *= DEXbonus[target.getDEX()] * chanceMod;

		if(Rnd.chance(shieldRate))
		{
			if(Rnd.chance(DEXbonus[target.getDEX()] * 2))
				return 2;

			return 1;
		}

		return 0;
	}

	public static boolean calcShldUse(L2Character attacker, L2Character target)
	{
		double shldRate = 0.;

		if(target.calcStat(Stats.SHIELD_RATE, target.getTemplate().baseShldRate, attacker, null) == 0)
			return false;

		int angle = (int) target.calcStat(Stats.SHIELD_ANGLE, 60, null, null);

		if(!target.isInFront(attacker, angle))
			return false;

		L2Weapon weapon = attacker.getActiveWeaponItem();
		if(weapon != null)
			if(weapon.getItemType() == WeaponType.BOW || weapon.getItemType() == WeaponType.CROSSBOW)
				shldRate = 30.;
			else if(weapon.getItemType() == WeaponType.DAGGER)
				shldRate = 12.;

		if(target.isPlayer())
		{
			L2ItemInstance shld = ((L2Player) target).getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
			if(shld != null && shld.getItemType() == WeaponType.NONE)
				shldRate = DEXbonus[target.getDEX()] * target.calcStat(Stats.SHIELD_RATE, shldRate, attacker, null);
			else
				return false;
		}
		else if(target instanceof L2NpcInstance)
			shldRate = target.calcStat(Stats.SHIELD_RATE, shldRate, attacker, null);

		return Rnd.chance((int) shldRate);
	}

	public static double calcSavevsDependence(BaseStats stat, L2Character cha)
	{
		try
		{
			switch(stat)
			{
				case INT:
					return INTbonus[cha.getINT()];
				case WIT:
					return WITbonus[cha.getWIT()];
				case MEN:
					return MENbonus[cha.getMEN()];
				case CON:
					return CONbonus[cha.getCON()];
				case DEX:
					return DEXbonus[cha.getDEX()];
				case STR:
					return STRbonus[cha.getSTR()];
			}
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			_log.warn("Failed calc savevs on char " + cha + " with save-stat " + stat);
			e.printStackTrace();
		}
		return 1.;
	}

	public static int getBaseStat(BaseStats stat, L2Character cha)
	{
		switch(stat)
		{
			case INT:
				return cha.getINT();
			case WIT:
				return cha.getWIT();
			case MEN:
				return cha.getMEN();
			case CON:
				return cha.getCON();
			case DEX:
				return cha.getDEX();
			case STR:
				return cha.getSTR();
		}
		return 0;
	}

	public static boolean calcSkillSuccess(Env env, ResistType resist)
	{
		if(env.value == -1)
			return true;

		if(env.target instanceof L2DoorInstance)
			return false;

		if(Config.SKILLS_USE_SIMPLE_LEVEL_MOD)
		{
			double mLevel = env.skill.getMagicLevel() == 0 ? env.character.getLevel() : env.skill.getMagicLevel();
			mLevel = (mLevel - env.target.getLevel() + 3) * env.skill.getLevelMod();
			env.value += mLevel >= 0 ? 0 : mLevel;

			if(Config.DEBUG)
				_log.info("LevelMod: " + mLevel + " value: " + env.value);

			if(env.skill.getBaseStat() != BaseStats.NONE)
				env.value += 30 - getBaseStat(env.skill.getBaseStat(), env.target);

			if(Config.DEBUG)
				_log.info("BaseStatMod: " + (30 - getBaseStat(env.skill.getBaseStat(), env.target)) + " value: " + env.value);
		}
		else
			env.value /= calcSavevsDependence(env.skill.getBaseStat(), env.target);

		//env.effectTime /= calcSavevsDependence(env.skill.getBaseStat(), env.target);

		switch(resist)
		{
			case BLEED:
				env.value *= 0.01 * env.character.calcStat(Stats.BLEED_POWER, 100, null, null);
				env.value *= 0.01 * env.target.calcStat(Stats.BLEED_RECEPTIVE, 100, null, null);
				//env.effectTime *= 0.01 * env.character.calcStat(Stats.BLEED_POWER, 100, null, null);
				//env.effectTime *= 0.01 * env.target.calcStat(Stats.BLEED_RECEPTIVE, 100, null, null);
				break;
			case CANCEL:
				env.value *= 0.01 * env.character.calcStat(Stats.CANCEL_POWER, 100, null, null);
				env.value *= 0.01 * env.target.calcStat(Stats.CANCEL_RECEPTIVE, 100, null, null);
				//env.effectTime *= 0.01 * env.character.calcStat(Stats.CANCEL_POWER, 100, null, null);
				//env.effectTime *= 0.01 * env.target.calcStat(Stats.CANCEL_RECEPTIVE, 100, null, null);
				break;
			case FEAR:
				env.value *= 0.01 * env.character.calcStat(Stats.FEAR_POWER, 100, null, null);
				env.value *= 0.01 * env.target.calcStat(Stats.FEAR_RECEPTIVE, 100, null, null);
				//env.effectTime *= 0.01 * env.character.calcStat(Stats.FEAR_POWER, 100, null, null);
				//env.effectTime *= 0.01 * env.target.calcStat(Stats.FEAR_RECEPTIVE, 100, null, null);
				break;
			case PARALYZE:
				env.value *= 0.01 * env.character.calcStat(Stats.PARALYZE_POWER, 100, null, null);
				env.value *= 0.01 * env.target.calcStat(Stats.PARALYZE_RECEPTIVE, 100, null, null);
				//env.effectTime *= 0.01 * env.character.calcStat(Stats.PARALYZE_POWER, 100, null, null);
				//env.effectTime *= 0.01 * env.target.calcStat(Stats.PARALYZE_RECEPTIVE, 100, null, null);
				break;
			case MUTE:
				env.value *= 0.01 * env.character.calcStat(Stats.SILENCE_POWER, 100, null, null);
				env.value *= 0.01 * env.target.calcStat(Stats.SILENCE_RECEPTIVE, 100, null, null);
				//env.effectTime *= 0.01 * env.character.calcStat(Stats.SILENCE_POWER, 100, null, null);
				//env.effectTime *= 0.01 * env.target.calcStat(Stats.SILENCE_RECEPTIVE, 100, null, null);
				break;
			case POISON:
				env.value *= 0.01 * env.character.calcStat(Stats.POISON_POWER, 100, null, null);
				env.value *= 0.01 * env.target.calcStat(Stats.POISON_RECEPTIVE, 100, null, null);
				//env.effectTime *= 0.01 * env.character.calcStat(Stats.POISON_POWER, 100, null, null);
				//env.effectTime *= 0.01 * env.target.calcStat(Stats.POISON_RECEPTIVE, 100, null, null);
				break;
			case ROOT:
				env.value *= 0.01 * env.character.calcStat(Stats.ROOT_POWER, 100, null, null);
				env.value *= 0.01 * env.target.calcStat(Stats.ROOT_RECEPTIVE, 100, null, null);
				//env.effectTime *= 0.01 * env.character.calcStat(Stats.ROOT_POWER, 100, null, null);
				//env.effectTime *= 0.01 * env.target.calcStat(Stats.ROOT_RECEPTIVE, 100, null, null);
				break;
			case SLEEP:
				env.value *= 0.01 * env.character.calcStat(Stats.SLEEP_POWER, 100, null, null);
				env.value *= 0.01 * env.target.calcStat(Stats.SLEEP_RECEPTIVE, 100, null, null);
				//env.effectTime *= 0.01 * env.character.calcStat(Stats.SLEEP_POWER, 100, null, null);
				//env.effectTime *= 0.01 * env.target.calcStat(Stats.SLEEP_RECEPTIVE, 100, null, null);
				break;
			case STUN:
				env.value *= 0.01 * env.character.calcStat(Stats.STUN_POWER, 100, null, null);
				env.value *= 0.01 * env.target.calcStat(Stats.STUN_RECEPTIVE, 100, null, null);
				//env.effectTime *= 0.01 * env.character.calcStat(Stats.STUN_POWER, 100, null, null);
				//env.effectTime *= 0.01 * env.target.calcStat(Stats.STUN_RECEPTIVE, 100, null, null);
				break;
			case SLOW:
				env.value *= 0.01 * env.character.calcStat(Stats.SLOW_POWER, 100, null, null);
				env.value *= 0.01 * env.target.calcStat(Stats.SLOW_RECEPTIVE, 100, null, null);
				//env.effectTime *= 0.01 * env.character.calcStat(Stats.SLOW_POWER, 100, null, null);
				//env.effectTime *= 0.01 * env.target.calcStat(Stats.SLOW_RECEPTIVE, 100, null, null);
				break;
			case DEATH:
				env.value *= 0.01 * env.target.calcStat(Stats.DEATH_RECEPTIVE, 100, null, null);
				break;
		}

		if(env.skill.isDebuff())
		{
			env.value *= 0.01 * env.character.calcStat(Stats.DEBUFF_POWER, 100, null, null);
			env.value *= 0.01 * env.target.calcStat(Stats.DEBUFF_RECEPTIVE, 100, null, null);
			//env.effectTime *= 0.01 * env.character.calcStat(Stats.DEBUFF_POWER, 100, null, null);
			//env.effectTime *= 0.01 * env.target.calcStat(Stats.DEBUFF_RECEPTIVE, 100, null, null);
		}

		if(Config.DEBUG)
			_log.info("Resists calc value: " + env.value);

		//env.effectTime = Math.min(Math.max(env.effectTime, 0.3), 1);

		if(env.value > 0) // No full resist
		{
			if(!Config.SKILLS_USE_SIMPLE_LEVEL_MOD)
			{
				byte mLevel = env.skill.getMagicLevel() == 0 ? env.character.getLevel() : env.skill.getMagicLevel();
				env.value *= getLevelMod(mLevel, env.target.getLevel());

				if(Config.DEBUG)
					_log.info("LevelMod: " + getLevelMod(mLevel, env.target.getLevel()) + " attackerLvl: " + mLevel + " targetLvl: " + env.target.getLevel());
			}

			if(env.skill.isMagic())
			{
				int mdef = env.target.getMDef(null, env.skill);
				if(mdef == 0)
					mdef = 1;

				double matak = env.character.getMAtkSps(env.target, env.skill);

				if(Config.SKILLS_USE_SIMPLE_LEVEL_MOD)
					env.value *= 11. * Math.pow(matak, 0.5) / mdef;
				else
					env.value *= Math.pow(matak, .35) * Math.pow(Math.log1p(matak), 2.) / mdef;

				if(Config.DEBUG)
					_log.info("mAtack calc: " + env.value);
			}

			env.value = env.character.calcStat(Stats.ACTIVATE_RATE, env.value, env.target, env.skill);

			env.value = Math.max(Math.min(env.value, Config.SKILLS_HIGH_CHANCE_CAP), env.target.isRaid() ? 1 : Config.SKILLS_LOW_CHANCE_CAP);
			if(Config.DEBUG)
				_log.info("activate rate calc: " + env.value);
		}

		if(Config.SKILLS_SHOW_CHANCE || env.character.isPlayer() && env.character.getPlayer().isGM())
		{
			if(env.character.isPlayer())
			{
				env.character.sendMessage(new CustomMessage("ru.l2gw.gameserver.skills.Formulas.Chance", env.character).addString(env.skill.getName()).addNumber((int) env.value));
				env.character.sendMessage("effectTimeMod: " + env.effectTime);
			}
			else if(env.character.isCubic() && env.character.getPlayer() != null)
			{
				env.character.getPlayer().sendMessage("Cubic: " + env.skill.getName() + " chance: " + (int) env.value + " mAtk: " + env.character.getMAtk(env.target, env.skill));
				env.character.getPlayer().sendMessage("effectTimeMod: " + env.effectTime);
			}
		}

		env.success = Rnd.chance((int) env.value);
		return env.success;
	}

	public static void calcSkillMastery(L2Skill skill, L2Character cha)
	{
		//Skill id 330 for fighters, 331 for mages
		//Actually only GM can have 2 skill masteries, so let's make them more lucky ^^
		if(cha.getSkillLevel(331) > 0 && cha.calcStat(Stats.SKILLMASTERY_RATE, cha.getINT(), null, null) >= Rnd.get(10000) || cha.getSkillLevel(330) > 0 && cha.calcStat(Stats.SKILLMASTERY_RATE, cha.getSTR(), null, null) >= Rnd.get(10000))
		{
			//byte mastery level, 0 = no skill mastery, 1 = no reuseTime, 2 = buff duration*2
			byte masteryLevel;
			if(skill.isBuff() && skill.getAbnormalTime() > 0 || skill.isSongDance()) //Hope i didn't forget skills to multiply their time
				masteryLevel = 2;
			else
				masteryLevel = 1;

			if(masteryLevel > 0)
				cha.setSkillMastery(skill.getId(), masteryLevel);
		}
	}

	public static double calcDamageResists(L2Skill skill, L2Character attacker, L2Character defender, double value)
	{
		int fire_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_FIRE, attacker.getTemplate().baseAttrAtk == 0 ? attacker.getTemplate().baseAttrAtkValue : 0, null, null);
		int water_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_WATER, attacker.getTemplate().baseAttrAtk == 1 ? attacker.getTemplate().baseAttrAtkValue : 0, null, null);
		int wind_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_WIND, attacker.getTemplate().baseAttrAtk == 2 ? attacker.getTemplate().baseAttrAtkValue : 0, null, null);
		int earth_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_EARTH, attacker.getTemplate().baseAttrAtk == 3 ? attacker.getTemplate().baseAttrAtkValue : 0, null, null);
		int holy_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_HOLY, attacker.getTemplate().baseAttrAtk == 4 ? attacker.getTemplate().baseAttrAtkValue : 0, null, null);
		int dark_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_DARK, attacker.getTemplate().baseAttrAtk == 5 ? attacker.getTemplate().baseAttrAtkValue : 0, null, null);

			/*
		if(attacker instanceof L2Playable)
		{
			if(attacker.isPlayer())
			{
				if(attacker.getPet() != null && attacker.getPet().isSummon() && !attacker.getPet().isDead() && attacker.getPlayer().getWeaponPenalty() == 0 && isSummonerClass(attacker.getPlayer()))
				{
					fire_attack *= 0.20;
					water_attack *= 0.20;
					wind_attack *= 0.20;
					earth_attack *= 0.20;
					holy_attack *= 0.20;
					dark_attack *= 0.20;
				}
			}
			*/
			if(attacker.isSummon())
			{
				L2Player owner = attacker.getPlayer();
				if(owner != null && owner.getWeaponPenalty() == 0 && CategoryManager.isInCategory(94, owner))
				{
					fire_attack = (int) owner.calcStat(Stats.ATTACK_ELEMENT_FIRE, 0, null, null);
					water_attack = (int) owner.calcStat(Stats.ATTACK_ELEMENT_WATER, 0, null, null);
					wind_attack = (int) owner.calcStat(Stats.ATTACK_ELEMENT_WIND, 0, null, null);
					earth_attack = (int) owner.calcStat(Stats.ATTACK_ELEMENT_EARTH, 0, null, null);
					holy_attack = (int) owner.calcStat(Stats.ATTACK_ELEMENT_HOLY, 0, null, null);
					dark_attack = (int) owner.calcStat(Stats.ATTACK_ELEMENT_DARK, 0, null, null);
				}
			}
		//}

		if(skill != null)
		{
			switch(skill.getElement())
			{
				case FIRE:
					return applyDefense(defender, Stats.FIRE_ATTRIBUTE, fire_attack + skill.getElementPower(), value);
				case WATER:
					return applyDefense(defender, Stats.WATER_ATTRIBUTE, water_attack + skill.getElementPower(), value);
				case WIND:
					return applyDefense(defender, Stats.WIND_ATTRIBUTE, wind_attack + skill.getElementPower(), value);
				case EARTH:
					return applyDefense(defender, Stats.EARTH_ATTRIBUTE, earth_attack + skill.getElementPower(), value);
				case HOLY:
					return applyDefense(defender, Stats.HOLY_ATTRIBUTE, holy_attack + skill.getElementPower(), value);
				case DARK:
					return applyDefense(defender, Stats.DARK_ATTRIBUTE, dark_attack + skill.getElementPower(), value);
			}
			return value;
		}

		if(fire_attack == 0 && water_attack == 0 && earth_attack == 0 && wind_attack == 0 && dark_attack == 0 && holy_attack == 0)
			return value;

		TreeMap<Integer, Stats> sort_attibutes = new TreeMap<Integer, Stats>();
		sort_attibutes.put(fire_attack, Stats.FIRE_ATTRIBUTE);
		sort_attibutes.put(water_attack, Stats.WATER_ATTRIBUTE);
		sort_attibutes.put(wind_attack, Stats.WIND_ATTRIBUTE);
		sort_attibutes.put(earth_attack, Stats.EARTH_ATTRIBUTE);
		sort_attibutes.put(holy_attack, Stats.HOLY_ATTRIBUTE);
		sort_attibutes.put(dark_attack, Stats.DARK_ATTRIBUTE);

		int attack = sort_attibutes.lastEntry().getKey();
		Stats defence_type = sort_attibutes.lastEntry().getValue();

		return applyDefense(defender, defence_type, attack, value);
	}

	/**
	 * @author rage
	 */
	public static double applyDefense(L2Character defender, Stats defence_type, int attack, double value)
	{
		int defenderValue = 0;
		if(defence_type == Stats.FIRE_ATTRIBUTE)
			defenderValue = (int) defender.calcStat(defence_type, defender.getTemplate().baseAttrDefFire, null, null);
		else if(defence_type == Stats.WATER_ATTRIBUTE)
			defenderValue = (int) defender.calcStat(defence_type, defender.getTemplate().baseAttrDefWater, null, null);
		else if(defence_type == Stats.WIND_ATTRIBUTE)
			defenderValue = (int) defender.calcStat(defence_type, defender.getTemplate().baseAttrDefWind, null, null);
		else if(defence_type == Stats.EARTH_ATTRIBUTE)
			defenderValue = (int) defender.calcStat(defence_type, defender.getTemplate().baseAttrDefEarth, null, null);
		else if(defence_type == Stats.HOLY_ATTRIBUTE)
			defenderValue = (int) defender.calcStat(defence_type, defender.getTemplate().baseAttrDefHoly, null, null);
		else if(defence_type == Stats.DARK_ATTRIBUTE)
			defenderValue = (int) defender.calcStat(defence_type, defender.getTemplate().baseAttrDefDark, null, null);

		if(defender.isSummon())
		{
			L2Player owner = defender.getPlayer();
			if(owner != null && CategoryManager.isInCategory(94, owner))
				defenderValue = calcArmorDefenceAttribute(owner, defence_type);
		}

		int attDiff = attack - defenderValue;

		/*
		if(diff <= 0)
			return 1.0;
		else if(diff < 50)
			return 1.0 + diff * 0.003948;
		else if(diff < 150)
			return 1.1974;
		else if(diff < 300)
			return 1.3973;
		else
			return 1.6963;
		*/

		if(attDiff < 1)
			return value;
		if(attDiff < 50)
			return value * (1 + (attDiff / 50 * 0.2));
		if(attDiff < 150)
			return value * 1.2;
		if(attDiff < 300)
			return value * 1.4;

		return value * 1.7;
	}

	public static int[] calcAttackElement(L2Character attacker)
	{
		int fire_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_FIRE, 0, null, null);
		int water_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_WATER, 0, null, null);
		int wind_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_WIND, 0, null, null);
		int earth_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_EARTH, 0, null, null);
		int sacred_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_HOLY, 0, null, null);
		int unholy_attack = (int) attacker.calcStat(Stats.ATTACK_ELEMENT_DARK, 0, null, null);

		if(fire_attack == 0 && water_attack == 0 && earth_attack == 0 && wind_attack == 0 && unholy_attack == 0 && sacred_attack == 0)
			return _emptyElement;

		TreeMap<Integer, Stats> sort_attibutes = new TreeMap<Integer, Stats>();
		sort_attibutes.put(fire_attack, Stats.ATTACK_ELEMENT_FIRE);
		sort_attibutes.put(water_attack, Stats.ATTACK_ELEMENT_WATER);
		sort_attibutes.put(wind_attack, Stats.ATTACK_ELEMENT_WIND);
		sort_attibutes.put(earth_attack, Stats.ATTACK_ELEMENT_EARTH);
		sort_attibutes.put(sacred_attack, Stats.ATTACK_ELEMENT_HOLY);
		sort_attibutes.put(unholy_attack, Stats.ATTACK_ELEMENT_DARK);

		int element = 0;
		switch(sort_attibutes.lastEntry().getValue())
		{
			case ATTACK_ELEMENT_FIRE:
				element = 0;
				break;
			case ATTACK_ELEMENT_WATER:
				element = 1;
				break;
			case ATTACK_ELEMENT_WIND:
				element = 2;
				break;
			case ATTACK_ELEMENT_EARTH:
				element = 3;
				break;
			case ATTACK_ELEMENT_HOLY:
				element = 4;
				break;
			case ATTACK_ELEMENT_DARK:
				element = 5;
				break;
		}

		return new int[]{element, sort_attibutes.lastEntry().getKey()};
	}

	public static boolean calcDodge(L2Character cha, L2Character target, L2Skill skill)
	{
		if(cha != target && Rnd.chance(target.calcStat(Stats.DODGE, 0, cha, skill)))
		{
			cha.sendPacket(new SystemMessage(SystemMessage.S1_DODGES_THE_ATTACK).addCharName(target));
			target.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_AVOIDED_C1S_ATTACK).addCharName(cha));
			return true;
		}
		else
			return false;
	}

	public static double calcLethal(L2Character activeChar, L2Character target, double baseLethal)
	{
		double levelMod = getLevelMod(activeChar.getLevel(), target.getLevel()) > 1 ? 1 : getLevelMod(activeChar.getLevel(), target.getLevel());
		double chance = baseLethal * activeChar.calcStat(Stats.LETHAL_RATE, 1, null, null) * levelMod * 0.01 * target.calcStat(Stats.DEATH_RECEPTIVE, 100, null, null);
		if(Config.SKILLS_SHOW_CHANCE && chance > 10 && target.isPlayer())
		{
			_log.info("CALC LETHAL WARNING: Character " + activeChar.getName() + " has lethal chance over 10 against target: " + target.getName() + " and Chance is " + (chance));
			_log.info("CALC LETHAL WARNING: baseLethal: " + baseLethal + " lethalrate: " + activeChar.calcStat(Stats.LETHAL_RATE, 1, null, null) + " levelMod: " + levelMod + " rcpt: " + target.calcStat(Stats.DEATH_RECEPTIVE, 100, null, null));
			for(L2Effect e : activeChar.getAllEffects())
				_log.info("CALC LETHAL WARNING: " + activeChar + " " + e);
		}
		if(Config.SKILLS_SHOW_CHANCE && activeChar.isPlayer())
			activeChar.sendMessage(new CustomMessage("ru.l2gw.gameserver.skills.Formulas.Chance", activeChar).addString("Lethal").addNumber((int) chance));
		return chance;
	}

	public static int calcArmorDefenceAttribute(L2Character cha, Stats stat)
	{
		Calculator c = cha.getCalculator(stat);
		if(c == null || c.size() < 1)
			return 0;

		Env env = new Env(cha, null, null);
		env.value = 0;

		for(Func f : c.getFunctions())
			if(f != null && f._funcOwner instanceof L2ItemInstance && ((L2ItemInstance) f._funcOwner).getItem() instanceof L2Armor)
				f.calc(env);

		return (int) env.value;
	}

	public static int calcMaxLoad(L2Character cha)
	{
		return (int) cha.calcStat(Stats.MAX_LOAD, CONbonus[Math.max(cha.getCON(), 1)] * 69000 * Config.MAXLOAD_MODIFIER, cha, null);
	}

	public static double getLevelMod(int l1, int l2)
	{
		if(l1 < 0 || l2 < 0)
			return 1;
		if(l1 > 86)
			l1 = 86;
		if(l2 > 86)
			l2 = 86;
		double levelMod = (double) Experience.LEVEL[l1] / (double) Experience.LEVEL[l2];
		if(l1 >= 70 && l2 >= 70)
		{
			if(levelMod < 0.75)
				levelMod = 0.75;
			else if(levelMod > 1.25)
				levelMod = 1.25;
			return levelMod;
		}
		return levelMod;
	}

	public static double getC4LevelMod(int l1, int l2)
	{
		if(l2 == 0)
			return 1;
		return (double) l1 / (double) l2;
	}

	public static double calcSkillMpConsume(L2Character caster, L2Skill skill, double mpConsume, boolean first)
	{
		if(skill.isSongDance())
		{
			mpConsume += caster.getDanceSongCount() * 30;
			return caster.calcStat(Stats.MP_DANCE_SKILL_CONSUME, mpConsume, null, skill);
		}
		else if(skill.isPhysic())
			return caster.calcStat(Stats.MP_PHYSICAL_SKILL_CONSUME, mpConsume, null, skill);
		else if(!first && skill.isMagic())
			return caster.calcStat(Stats.MP_MAGIC_SKILL_CONSUME, mpConsume, null, skill);

		return mpConsume;
	}

	public static double calcCancelChance(L2Character attacker, L2Character target, double baseChance, int cancelLevel, L2Effect effect)
	{
		double chance = (2 * (cancelLevel - effect.getSkill().getMagicLevel()) + baseChance + effect.getSkill().getAbnormalTime() / 1200000) * 0.01 * attacker.calcStat(Stats.CANCEL_POWER, 100, null, null) * 0.01 * target.calcStat(Stats.CANCEL_RECEPTIVE, 100, null, null);
		if(Config.SKILLS_SHOW_CHANCE && attacker.isPlayer() && attacker.getPlayer().isGM())
		{
			Functions.sendSysMessage(attacker.getPlayer(), "===============================");
			Functions.sendSysMessage(attacker.getPlayer(), "buff: " + effect.getSkill().getName() + " " + effect.getSkill().getId() + "-" + effect.getSkill().getLevel());
			Functions.sendSysMessage(attacker.getPlayer(), "dml: " + (cancelLevel - effect.getSkill().getMagicLevel()));
			Functions.sendSysMessage(attacker.getPlayer(), "base chance: " + baseChance);
			Functions.sendSysMessage(attacker.getPlayer(), "abnormal mod: " + (effect.getSkill().getAbnormalTime() / 1200000));
			Functions.sendSysMessage(attacker.getPlayer(), "resist: " + (0.01 * attacker.calcStat(Stats.CANCEL_POWER, 100, null, null) * 0.01 * target.calcStat(Stats.CANCEL_RECEPTIVE, 100, null, null)));
			Functions.sendSysMessage(attacker.getPlayer(), "=: " + chance + " cap=" + Math.max(Math.min(Config.CANCEL_SKILLS_HIGH_CHANCE_CAP, chance), Config.CANCEL_SKILLS_LOW_CHANCE_CAP));
		}
		return Math.max(Math.min(Config.CANCEL_SKILLS_HIGH_CHANCE_CAP, chance), Config.CANCEL_SKILLS_LOW_CHANCE_CAP);
	}

	public static double getLevelMod(int level)
	{
		return (89. + level) / 100.0;
	}

	public static float getPAtkFromBase(int basePAtk, int baseSTR, int level)
	{
		return (float) (basePAtk * STRbonus[baseSTR] * getLevelMod(level));
	}

	public static float getMAtkFromBase(int baseMAtk, int baseINT, int level)
	{
		return (float) (baseMAtk * Math.pow(INTbonus[baseINT], 2) * Math.pow(getLevelMod(level), 2));
	}

	public static float getPDefFromBase(int basePDef, int level)
	{
		return (float) (basePDef * getLevelMod(level));
	}

	public static float getMDefFromBase(int baseMDef, int baseMEN, int level)
	{
		return (float) (baseMDef * MENbonus[baseMEN] * getLevelMod(level));
	}

	public static float getPAtkSpdFromBase(int basePAtkSpd, int baseDEX)
	{
		return (float) (basePAtkSpd * DEXbonus[baseDEX]);
	}

	public static float getMAtkSpdFromBase(int baseMAtkSpd, int baseWIT)
	{
		return (float) (baseMAtkSpd * WITbonus[baseWIT]);
	}

	public static float getMaxHpFromBase(float baseHp, int baseCON)
	{
		return (float) (baseHp * CONbonus[baseCON]);
	}

	public static float getMaxMpFromBase(float baseMp, int baseMEN)
	{
		return (float) (baseMp * MENbonus[baseMEN]);
	}
}
