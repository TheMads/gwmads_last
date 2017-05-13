package ru.l2gw.gameserver.instancemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.Castle;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.tables.SkillTable;

public abstract class SiegeManager
{

	protected static Log _log = LogFactory.getLog(SiegeManager.class.getName());

	public static void addSiegeSkills(L2Player player)
	{
		player.addSkill(SkillTable.getInstance().getInfo(246, 1), false);
		player.addSkill(SkillTable.getInstance().getInfo(247, 1), false);
		player.addSkill(SkillTable.getInstance().getInfo(844, 1), false);
		player.addSkill(SkillTable.getInstance().getInfo(845, 1), false);
	}

	public static void removeSiegeSkills(L2Player player)
	{
		player.removeSkill(SkillTable.getInstance().getInfo(246, 1), false);
		player.removeSkill(SkillTable.getInstance().getInfo(247, 1), false);
		player.removeSkill(SkillTable.getInstance().getInfo(844, 1), false);
		player.removeSkill(SkillTable.getInstance().getInfo(845, 1), false);
	}

	public static boolean getCanRide()
	{
		for(Castle castle : ResidenceManager.getInstance().getCastleList())
			if(castle != null && (castle.getSiege().isInProgress() || castle.getSiege().getTimeRemaining() <= 7200))
				return false;
		return true;
	}

	public static SiegeUnit getSiegeUnitByObject(L2Object activeObject)
	{
		return getSiegeUnitByCoord(activeObject.getX(), activeObject.getY());
	}

	public static SiegeUnit getSiegeUnitByCoord(int x, int y)
	{
		for(SiegeUnit ch : ResidenceManager.getInstance().getClanHallList())
			if(ch.getSiegeZone() != null && ch.checkIfInZone(x, y))//TODO: убрать затычку с проверкой на нуль
				return ch;
		for(SiegeUnit fortress : ResidenceManager.getInstance().getFortressList())
			if(fortress.checkIfInZone(x, y))
				return fortress;
		for(SiegeUnit castle : ResidenceManager.getInstance().getCastleList())
			if(castle.checkIfInZone(x, y))
				return castle;
		return null;
	}

	public static Siege getSiege(L2Object activeObject)
	{
		return getSiege(activeObject.getX(), activeObject.getY());
	}

	public static Siege getSiege(int x, int y)
	{
		SiegeUnit unit = ResidenceManager.getInstance().getBuildingBySiegeZoneCoord(x, y);

		if(unit != null)
			return unit.getSiege();

		return null;
	}

	public static SiegeUnit getCastleDefenderSiegeUnit(int clanId)
	{
		for(SiegeUnit castle : ResidenceManager.getInstance().getCastleList())
			if(castle.getSiege().isInProgress() && castle.getSiege().checkIsDefender(clanId))
				return castle;

		return null;
	}
}