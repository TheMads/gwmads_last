package ru.l2gw.gameserver.model.base;

import ru.l2gw.gameserver.Config;

@SuppressWarnings( { "nls", "unqualified-field-access", "boxing" })
public class Experience
{

	public final static long LEVEL[] = { -1L, // level 0 (unreachable)
			0L,
			68L,
			363L,
			1168L,
			2884L,
			6038L,
			11287L,
			19423L,
			31378L,
			48229L, //level 10
			71201L,
			101676L,
			141192L,
			191452L,
			254327L,
			331864L,
			426284L,
			539995L,
			675590L,
			835854L, //level 20
			1023775L,
			1242536L,
			1495531L,
			1786365L,
			2118860L,
			2497059L,
			2925229L,
			3407873L,
			3949727L,
			4555766L, //level 30
			5231213L,
			5981539L,
			6812472L,
			7729999L,
			8740372L,
			9850111L,
			11066012L,
			12395149L,
			13844879L,
			15422851L, //level 40
			17137002L,
			18995573L,
			21007103L,
			23180442L,
			25524751L,
			28049509L,
			30764519L,
			33679907L,
			36806133L,
			40153995L, //level 50
			45524865L,
			51262204L,
			57383682L,
			63907585L,
			70852742L,
			80700339L,
			91162131L,
			102265326L,
			114038008L,
			126509030L, //level 60
			146307211L,
			167243291L,
			189363788L,
			212716741L,
			237351413L,
			271973532L,
			308441375L,
			346825235L,
			387197529L,
			429632402L, //level 70
			474205751L,
			532692055L,
			606319094L,
			696376867L,
			804219972L,
			931275828L,
			1151275834L,
			1511275834L,
			2044287599L,
			3075966164L, //level 80
			4295351949L,
			5766985062L,
			7793077345L,
			10235368963L,
			13180481103L, //level 85
			17719800000L };

	/**
	 * Return PenaltyModifier (can use in all cases)
	 *
	 * @param count	- how many times <percents> will be substructed
	 * @param percents - percents to substruct
	 *
	 * @author Styx
	 */

	/*
	 *  This is for fine view only ;)
	 *
	 *	public final static double penaltyModifier(int count, int percents)
	 *	{
	 *		int allPercents = 100;
	 *		int allSubstructedPercents = count * percents;
	 *		int penaltyInPercents = allPercents - allSubstructedPercents;
	 *		double penalty = penaltyInPercents / 100.0;
	 *		return penalty;
	 *	}
	 */
	public static double penaltyModifier(long count, long percents)
	{
		double mod = (100 - count * percents) / 100.0;
		return mod < 0 ? 0 : mod;
	}

	/**
	 * Максимальный достижимый уровень
	 */
	public static int getMaxLevel()
	{
		return Config.ALT_MAX_LEVEL;
	}

	/**
	 * Максимальный уровень для саба
	 */
	public static int getMaxSubLevel()
	{
		return Config.ALT_MAX_SUB_LEVEL;
	}

	public static long getExpForLevel(int lvl)
	{
		if(lvl >= Experience.LEVEL.length)
			return 0;
		return Experience.LEVEL[lvl];
	}

	public static float getExpPercent(int level, long exp)
	{
		return (exp - getExpForLevel(level)) / ((getExpForLevel(level + 1) - getExpForLevel(level)) / 100.0F) * 0.01F;
	}
}