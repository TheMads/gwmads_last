package ru.l2gw.gameserver.model.entity;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.serverpackets.SkillList;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.tables.SpawnTable;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * @author rage
 * @date 06.07.2010 11:32:23
 */
public class Territory
{
	private final int _territoryId;
	private final Fortress _fort;
	private final String _name;
	private final GArray<Integer> _wards = new GArray<Integer>(1);
	private L2Clan _owner;
	private final Castle _castle;
	private boolean _catapultKilled = false;
	private boolean _leadersKilled = false;
	private boolean _suppliesKilled = false;

	public Territory(int territoryId, int castleId, int fortId)
	{
		_territoryId = territoryId;
		_fort = (Fortress) ResidenceManager.getInstance().getBuildingById(fortId);
		_castle = ResidenceManager.getInstance().getCastleById(castleId);
		_owner = _castle.getOwner();
		_name = _castle.getName().toLowerCase() + "_dominion";
	}

	public int getId()
	{
		return _territoryId;
	}

	public int getCastleId()
	{
		return _castle.getId();
	}

	public Castle getCastle()
	{
		return _castle;
	}

	public Fortress getFort()
	{
		return _fort;
	}

	public String getName()
	{
		return _name;
	}

	public L2Clan getOwner()
	{
		return _owner;
	}

	public void setOwner(L2Clan owner)
	{
		_owner = owner;
	}

	public String getOwnerClanName()
	{
		if(_owner != null)
			return _owner.getName();

		return "";
	}

	public void addWardId(int wardId)
	{
		_wards.add(wardId);
	}

	public void removeWard(int wardId)
	{
		_wards.remove(new Integer(wardId));
	}

	public GArray<Integer> getWards()
	{
		return _wards;
	}

	public void spawnNpc()
	{
		SpawnTable.getInstance().startEventSpawn("territory_town_" + _territoryId);
	}

	public void despawnNpc()
	{
		SpawnTable.getInstance().stopEventSpawn("territory_town_" + _territoryId, true);
	}

	public void spawnWard()
	{
		for(int wardId : _wards)
			SpawnTable.getInstance().startEventSpawn("territory_ward_" + wardId + "_" + _territoryId);		
	}

	public void giveSkills(L2Player player)
	{
		if(_wards.contains(_territoryId))
			for(int wardId : _wards)
				player.addSkill(SkillTable.getInstance().getInfo(767 + wardId, 1));
	}

	public void removeSkills(L2Player player)
	{
		for(int wardId : _wards)
			player.removeSkill(SkillTable.getInstance().getInfo(767 + wardId, 1), false);
	}

	public boolean hasLord()
	{
		return _owner != null && _owner.getLeader().getVarB("territory_lord_" + _territoryId);
	}

	public void removeWardSkill(int wardId)
	{
		if(_owner != null)
		{
			L2Skill wardSkill = SkillTable.getInstance().getInfo(767 + wardId, 1);
			for(L2Player player : _owner.getOnlineMembers(""))
				if(player.getPledgeType() != L2Clan.SUBUNIT_ACADEMY)
				{
					player.removeSkill(wardSkill, false);
					player.sendPacket(new SkillList(player));
					player.sendPacket(Msg.THE_EFFECT_OF_TERRITORY_WARD_IS_DISAPPEARING);
				}
		}
	}

	public void addWardSkill(int wardId)
	{
		if(_owner != null)
		{
			L2Skill wardSkill = SkillTable.getInstance().getInfo(767 + wardId, 1);
			for(L2Player player : _owner.getOnlineMembers(""))
				if(player.getPledgeType() != L2Clan.SUBUNIT_ACADEMY)
				{
					player.addSkill(wardSkill, false);
					player.sendPacket(new SkillList(player));
				}
		}
	}

	public void updateWards()
	{
		Connection con = null;
		PreparedStatement stmt = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("UPDATE territories SET ward_ids = ? WHERE territory_id = ?");
			String wards = "";
			for(int wardId : _wards)
				wards += wardId + ";";
			stmt.setString(1, wards);
			stmt.setInt(2, _territoryId);
			stmt.execute();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, stmt);
		}
	}

	public void setCatapultState(boolean killed)
	{
		_catapultKilled = killed;
	}

	public void setLeadersState(boolean killed)
	{
		_leadersKilled = killed;
	}

	public void setSuppliesState(boolean killed)
	{
		_suppliesKilled = killed;
	}

	public boolean isCatapultKilled()
	{
		return _catapultKilled;
	}

	public boolean isLeadersKilled()
	{
		return _leadersKilled;
	}

	public boolean isSuppliesKilled()
	{
		return _suppliesKilled;
	}

	@Override
	public String toString()
	{
		return "Territory[id=" + _territoryId + ";owner=" + _owner + "]";
	}
}
