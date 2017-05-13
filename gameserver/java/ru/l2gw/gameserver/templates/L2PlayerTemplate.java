package ru.l2gw.gameserver.templates;

import ru.l2gw.gameserver.model.base.ClassId;
import ru.l2gw.gameserver.model.base.Race;

public class L2PlayerTemplate extends L2CharTemplate
{
	/** The Class object of the L2Player */
	public final ClassId classId;

	public final Race race;
	public final String className;

	public final boolean isMale;

	public float[] baseHp;
	public float[] baseMp;
	public float[] baseCp;

	public final int safeFall;

	public L2PlayerTemplate(StatsSet set)
	{
		super(set);
		classId = ClassId.values()[set.getInteger("classId")];
		race = Race.values()[set.getInteger("raceId")];
		className = set.getString("className");

		isMale = set.getBool("isMale", true);

		baseHp = new float[86];
		baseMp = new float[86];
		baseCp = new float[86];

		safeFall = set.getInteger("safeFall");
	}
}