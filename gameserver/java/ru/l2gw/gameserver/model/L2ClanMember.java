package ru.l2gw.gameserver.model;

import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2Clan.SubPledge;
import ru.l2gw.gameserver.model.playerSubOrders.UserVar;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;

public class L2ClanMember
{
	private int _objectId;
	private L2Clan _clan;
	private String _name;
	private String _title;
	private int _level;
	private int _classId;
	private int _sex;
	private L2Player _player;
	private int _pledgeType;
	private int _powerGrade;
	private int _apprentice;
	private Boolean _clanLeader;
	private Map<String, UserVar> _userVars;
	/** 
	 * Конструктор с указанием пола
	 */
	public L2ClanMember(L2Clan clan, String name, String title, int level, int classId, int objectId, int pledgeType, int powerGrade, int apprentice, Boolean clanLeader, int sex, Map<String, UserVar> userVars)
	{
		this(clan, name, title, level, classId, objectId, pledgeType, powerGrade, apprentice, clanLeader, userVars);
		_sex = sex;
	}

	public L2ClanMember(L2Clan clan, String name, String title, int level, int classId, int objectId, int pledgeType, int powerGrade, int apprentice, Boolean clanLeader, Map<String, UserVar> userVars)
	{
		_clan = clan;
		_name = name;
		_title = title;
		_level = level;
		_classId = classId;
		_objectId = objectId;
		_pledgeType = pledgeType;
		_powerGrade = powerGrade;
		_apprentice = apprentice;
		_clanLeader = clanLeader;
		_userVars = userVars;
	}

	public L2ClanMember(L2Player player)
	{
		_player = player;
		_userVars = player.getUserVars();
	}

	public void setPlayerInstance(L2Player player)
	{
		if(player == null && _player != null)
		{
			// this is here to keep the data when the player logs off
			_clan = _player.getClan();
			_name = _player.getName();
			_title = _player.getTitle();
			_level = _player.getLevel();
			_classId = _player.getClassId().getId();
			_objectId = _player.getObjectId();
			_pledgeType = _player.getPledgeType();
			_powerGrade = _player.getPowerGrade();
			_apprentice = _player.getApprentice();
			_clanLeader = _player.isClanLeader();
		}

		_player = player;
	}

	public L2Player getPlayer()
	{
		return _player;
	}

	public boolean isOnline()
	{
		return _player != null;
	}

	public L2Clan getClan()
	{
		if(_player != null)
			return _player.getClan();
		return _clan;
	}

	/**
	 * @return Returns the classId.
	 */
	public int getClassId()
	{
		if(_player != null)
			return _player.getClassId().getId();
		return _classId;
	}

	public int getSex()
	{
		if(_player != null)
			return _player.getSex();
		return _sex;
	}

	/**
	 * @return Returns the level.
	 */
	public int getLevel()
	{
		if(_player != null)
			return _player.getLevel();
		return _level;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName()
	{
		if(_player != null)
			return _player.getName();
		return _name;
	}

	/**
	 * @return Returns the objectId.
	 */
	public int getObjectId()
	{
		if(_player != null)
			return _player.getObjectId();
		return _objectId;
	}

	public String getTitle()
	{
		if(_player != null)
			return _player.getTitle();
		return _title;
	}

	public void setTitle(String title)
	{
		_title = title;
		if(_player != null)
		{
			_player.setTitle(title);
			_player.sendChanges();
		}
		else
		{
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("UPDATE characters SET title=? WHERE obj_Id=?");
				statement.setString(1, title);
				statement.setInt(2, _objectId);
				statement.execute();
			}
			catch(Exception e)
			{}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}

	public int getPledgeType()
	{
		if(_player != null)
			return _player.getPledgeType();
		return _pledgeType;
	}

	public void setPledgeType(int pledgeType)
	{
		_pledgeType = pledgeType;
		if(_player != null)
			_player.setPledgeType(pledgeType);
		else
			updatePledgeType();
	}

	public void setObjectId(int objId)
	{
		if(_player != null)
			_objectId = _player.getObjectId();
		else
			_objectId = objId;
	}

	public void updatePledgeType()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET pledge_type=? WHERE obj_Id=?");
			statement.setInt(1, _pledgeType);
			statement.setInt(2, _objectId);
			statement.execute();
		}
		catch(Exception e)
		{}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public int getPowerGrade()
	{
		if(_player != null)
			return _player.getPowerGrade();
		return _powerGrade;
	}

	public void setPowerGrade(int powerGrade)
	{
		updatePowerGradeParty(getPowerGrade(), powerGrade);
		_powerGrade = powerGrade;
		if(_player != null)
			_player.setPowerGrade(powerGrade);
		else
			updatePowerGrade();
	}

	public void updatePowerGradeParty(int oldGrade, int newGrade)
	{
		if(oldGrade != 0)
			getClan().getRankPrivs(oldGrade).setParty(getClan().countMembersByRank(oldGrade));
		if(newGrade != 0)
			getClan().getRankPrivs(newGrade).setParty(getClan().countMembersByRank(newGrade));
	}

	public void updatePowerGrade()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET pledge_rank=? WHERE obj_Id=?");
			statement.setInt(1, _powerGrade);
			statement.setInt(2, _objectId);
			statement.execute();
		}
		catch(Exception e)
		{}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public int getApprentice()
	{
		if(_player != null)
			return _player.getApprentice();
		return _apprentice;
	}

	public void setApprentice(int apprentice)
	{
		_apprentice = apprentice;
		if(_player != null)
			_player.setApprentice(apprentice);
		else
			updateApprentice();
	}

	public void updateApprentice()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET apprentice=? WHERE obj_Id=?");
			statement.setInt(1, _apprentice);
			statement.setInt(2, _objectId);
			statement.execute();
		}
		catch(Exception e)
		{}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public String getApprenticeName()
	{
		if(getApprentice() != 0)
			if(getClan().getClanMember(getApprentice()) != null)
				return getClan().getClanMember(getApprentice()).getName();
		return "";
	}

	public Boolean hasApprentice()
	{
		return getApprentice() != 0;
	}

	public int getSponsor()
	{
		if(getPledgeType() != L2Clan.SUBUNIT_ACADEMY)
			return 0;
		int _id = getObjectId();
		L2ClanMember[] _members = getClan().getMembers();
		for(L2ClanMember element : _members)
			if(element.getApprentice() == _id)
				return element.getObjectId();
		return 0;
	}

	public String getSponsorName()
	{
		int _sponsorId = getSponsor();
		if(_sponsorId == 0)
			return "";
		else if(getClan().getClanMember(_sponsorId) != null)
			return getClan().getClanMember(_sponsorId).getName();
		return "";

	}

	public Boolean hasSponsor()
	{
		return getSponsor() != 0;
	}

	public String getRelatedName()
	{
		if(getPledgeType() == L2Clan.SUBUNIT_ACADEMY)
			return getSponsorName();
		return getApprenticeName();
	}

	public Boolean isClanLeader()
	{
		if(_player != null)
			return _player.isClanLeader();
		return _clanLeader;
	}

	public int isSubLeader()
	{
		SubPledge[] subPledge = getClan().getAllSubPledges();
		for(SubPledge element : subPledge)
			if(element.getLeaderId() == getObjectId())
				return element.getType();
		return 0;
	}

	public String getVar(String name)
	{
		if(_player != null)
			return _player.getVar(name);

		if(_userVars != null && _userVars.containsKey(name))
		{
			UserVar uv =  _userVars.get(name);
			if(uv.expire <= 0 || uv.expire > System.currentTimeMillis())
				return uv.value;
		}
		return null;
	}

	public boolean getVarB(String name)
	{
		if(_player != null)
			return _player.getVarB(name);

		String var = getVar(name);
		return !(var == null || var.equals("0") || var.equalsIgnoreCase("false"));
	}

	public void unsetVar(String name)
	{
		if(_player != null)
		{
			_player.unsetVar(name);
			return;
		}

		if(_userVars.remove(name) != null)
			L2Player.unsetVar(_objectId, name);
	}
}
