package ru.l2gw.gameserver.model.entity.siege;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.entity.Castle;
import ru.l2gw.gameserver.model.entity.ClanHall;
import ru.l2gw.gameserver.model.entity.Fortress;
import ru.l2gw.gameserver.model.entity.Fortress.FortressType;
import ru.l2gw.gameserver.model.entity.siege.ClanHall.RolePlaySiege;
import ru.l2gw.gameserver.model.entity.siege.fortress.CombatFlag;
import ru.l2gw.gameserver.model.entity.siege.fortress.FortressSiegeDatabase;
import ru.l2gw.gameserver.model.entity.siege.reinforce.*;
import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ClanTable;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.tables.StaticObjectsTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.util.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class SiegeUnit
{
	private int id = 0;

	protected Siege _siege;
	protected int _contractCastle;
	protected int _taxAmount;
	protected int _supplyLevel;
	protected int _rewardLevel;
	protected long _lastSiegeDate;
	protected long _lastTax;
	protected long _holdTime;
	protected long _rebelTime;
	protected long _ctLoosPenalty;
	protected List<SiegeSpawn> _commanderSpawns;
	protected List<SiegeSpawn> _peaceSpawns;
	protected List<CombatFlag> _flagList;
	protected List<SiegeSpawn> _flagPoleSpawns;
	protected List<Integer> _commandCenterDoors;
	protected List<Integer> _castles;
	protected Map<Integer, L2Spawn> _supplyBoxes;
	protected L2Spawn _merchantSpawn;
	protected ScheduledFuture<HoldTask> _holdTask;
	protected FortressType _fortressType;
	protected List<Integer> _doorControllers;
	protected List<Integer> _mainControllers;
	protected List<Integer> _controlDoors;
	protected long _powerOnTime = 0;

	// ClanBase functions
	public static final int FUNC_TELEPORT = 1;
	public static final int FUNC_ITEM_CREATE = 2;
	public static final int FUNC_RESTORE_HP = 3;
	public static final int FUNC_RESTORE_MP = 4;
	public static final int FUNC_RESTORE_EXP = 5;
	public static final int FUNC_SUPPORT = 6;
	public static final int FUNC_CURTAIN = 7;
	public static final int FUNC_PLATFORM = 8;
	private String location = "";
	
	public boolean isCastle = false;
	public boolean isFort = false;
	public boolean isClanHall = false;

	//ClaHall
	private String name = "";
	private List<L2Skill> _skills;


	/** Clan objectId */
	protected int ownerId = 0;

	private L2Zone zone;
	private L2Zone residentZone;
	private L2Zone siegeZone;
	private L2Zone headquartersZone;
	private int price = 0;
	private int lease = 0;
	private String desc = "";
	private int grade;

	protected long _treasury;
	protected int _townId;
	protected int _collectedShops;
	protected int _collectedSeed;

	protected List<SiegeSpawn> _artefactSpawnList = null;
	protected List<SiegeSpawn> _controlTowerSpawnList = null;
	protected Map<Integer, L2Zone> _trapZones = null;
	protected Map<Integer, L2Spawn> _ambassadors = null;

	private GArray<L2DoorInstance> _doors = new GArray<L2DoorInstance>();
	protected Map<Integer, ClanBaseFunction> _functions = new FastMap<Integer, ClanBaseFunction>();

	protected Map<Integer, Reinforce> _reinforces;

	protected static Log _log = LogFactory.getLog("siegeunit");

	public SiegeUnit()
	{
		if(this instanceof Castle)
			isCastle = true;
		if(this instanceof ClanHall)
			isClanHall = true;
		if(this instanceof Fortress)
			isFort = true;
	}

	/**
	 * 
	 * @return Functions Zone
	 * like Castle inner walls
	 * like ClanHall inner Doors
	 * like fortress inner room
	 * if exits or full siege zone
	 */
	public L2Zone getRezidentZone()
	{
		return  residentZone != null ? residentZone : zone;
	}

	public L2Clan getOwner()
	{
		return ClanTable.getInstance().getClan(getOwnerId());
	}

	public abstract Siege getSiege();

	// This method sets the siege unit owner; null here means give it back to NPC
	public abstract void changeOwner(int clanId);

	public abstract void startAutoTask();

	public GArray<L2DoorInstance> getDoors()
	{
		return _doors;
	}

	public L2DoorInstance getDoor(int doorId)
	{
		if(doorId <= 0)
			return null;

		for(int i = 0; i < getDoors().size(); i++)
		{
			L2DoorInstance door = getDoors().get(i);
			if(door.getDoorId() == doorId)
				return door;
		}
		return null;
	}

	/** Return true if object is inside the zone */
	public boolean checkIfInZone(L2Object obj)
	{
		return checkIfInZone(obj.getX(), obj.getY());
	}

	/** Return true if object is inside the zone */
	public boolean checkIfInZone(int x, int y)
	{
		return getZone().isInsideZone(x, y);
	}

	/** Return true if object is inside the zone */
	public boolean checkIfInSiegeZone(int x, int y)
	{
		return getSiegeZone() != null && getSiegeZone().isInsideZone(x, y);
	}

	public boolean checkIfInresidenceZone(int x, int y)
	{
		return getRezidentZone().isInsideZone(x, y);
	}

	/** Respawn all doors on siege unit grounds */
	public void spawnDoor(boolean isDoorWeak)
	{
		for(int i = 0; i < getDoors().size(); i++)
		{
			L2DoorInstance door = getDoors().get(i);
			if(door.getCurrentHp() <= 0)
			{
				door.decayMe(); // Kill current if not killed already
				if(isDoorWeak)
					door.setCurrentHp(door.getMaxHp() / 2);
				else
					door.setCurrentHp(door.getMaxHp());
				door.spawnMe(door.getLoc());
				getDoors().set(i, door);
			}
			else if(door.isOpen())
				door.closeMe();

			if(!isDoorWeak && door.getCurrentHp() < door.getMaxHp())
				door.setCurrentHp(door.getMaxHp());
		}
	}

	public void reinforceDoors(boolean active)
	{
		for(DoorReinforce dr : getDoorReinforce())
			dr.setActive(active);
	}

	public void spawnDoor()
	{
		spawnDoor(false);
	}

	// This method is used to begin removing all siege unit upgrades
	public void removeUpgrade()
	{
		if(_reinforces != null)
			for(Reinforce rf : _reinforces.values())
			{
				rf.setLevel(0);
				rf.setActive(false);
				rf.store();
			}
	}

	/** Move non clan members off siegeUnit area and to nearest town. */
	public void banishForeigner()
	{
		// Get all players
		if(getZone() != null)
			for(L2Player player : getZone().getPlayers())
				if(player.getClanId() != getOwnerId())
					player.teleToClosestTown();
	}

	public final boolean haveRolePlaySiege()
	{
		return getSiege() instanceof RolePlaySiege;
	}

	public void closeDoor(L2Player player, int doorId)
	{
		openCloseDoor(player, doorId, false);
	}

	public void openDoor(L2Player player, int doorId)
	{
		openCloseDoor(player, doorId, true);
	}

	public void openCloseDoor(L2Player player, int doorId, boolean open)
	{
		if(!player.isGM())
			if(player.getClanId() != getOwnerId())
				return;

		L2DoorInstance door = getDoor(doorId);
		if(door != null)
			if(open)
				door.openMe();
			else
				door.closeMe();
	}

	public class ClanBaseFunction
	{
		private int _type;
		private int _lvl;
		protected int _fee;
		private long _rate;
		private ScheduledFuture<?> _rentTask;
		private Log _logCH = LogFactory.getLog("clanhall");
		private Calendar _nextPay = Calendar.getInstance();

		public ClanBaseFunction(int type, int lvl, int lease, long rate, long nextPay)
		{
			_type = type;
			_lvl = lvl;
			_fee = lease;
			_rate = rate;
			_nextPay.setTimeInMillis(nextPay);
			startAutoTaskForFunctions();
		}

		public int getType()
		{
			return _type;
		}

		public int getLvl()
		{
			return _lvl;
		}

		public int getLease()
		{
			return _fee;
		}

		public long getRate()
		{
			return _rate;
		}

		public long getNextPayTime()
		{
			return _nextPay.getTimeInMillis();
		}

		public void updateNextPayTime()
		{
			while(_nextPay.getTimeInMillis() < System.currentTimeMillis())
				_nextPay.add(Calendar.MILLISECOND, (int) getRate());

			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();

				statement = con.prepareStatement("UPDATE clanhall_functions SET endTime=? WHERE hall_id=? AND type=?");
				statement.setLong(1, _nextPay.getTimeInMillis());
				statement.setInt(2, getId());
				statement.setInt(3, getType());
				statement.executeUpdate();
			}
			catch(Exception e)
			{
				_logCH.warn(this + ": updateRentTime: " + e.getMessage());
				e.printStackTrace();
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}

		private void startAutoTaskForFunctions()
		{
			if(getOwnerId() != 0)
				try
				{
					L2Clan clan = ClanTable.getInstance().getClan(getOwnerId());
					if(clan == null)
					{
						_logCH.warn(this + ": clan == null for: " + this);
						return;
					}

					L2ItemInstance adena = clan.getWarehouse().getItemByItemId(57);

					if(getNextPayTime() > System.currentTimeMillis())
						_rentTask = ThreadPoolManager.getInstance().scheduleGeneral(new AutoTaskForFunctions(), getNextPayTime() - System.currentTimeMillis());
					else if(adena != null && adena.getCount() >= _fee) // if player didn't pay before add extra fee
					{
						clan.getWarehouse().destroyItemByItemId("CHFee", 57, _fee, null, null);
						updateNextPayTime();
						_rentTask = ThreadPoolManager.getInstance().scheduleGeneral(new AutoTaskForFunctions(), getNextPayTime() - System.currentTimeMillis());
						_logCH.warn(this + ": deducted " + _fee + " adena for function type: " + getType());
					}
					else
					{
						removeFunctions(getType());
						_logCH.warn(this + ": remove function type: " + getType() + ", because there are not enough adena.");
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
		}

		private class AutoTaskForFunctions implements Runnable
		{
			public void run()
			{
				startAutoTaskForFunctions();
			}
		}

		public void stopRentTask()
		{
			if(_rentTask != null)
			{
				_rentTask.cancel(false);
				_rentTask = null;
			}
		}

		@Override
		public String toString()
		{
			return "Function[type=" + _type + ";fee=" + _fee + ";nextPay=" + new Date(getNextPayTime()) + ";]";
		}
	}

	public void updateFunctions(int type, int lvl, int lease, long rate)
	{
		L2Clan clan = ClanTable.getInstance().getClan(getOwnerId());
		if(clan == null)
			return;

		long nextPay = System.currentTimeMillis() + rate;
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("REPLACE INTO clanhall_functions (hall_id, type, lvl, lease, rate, endTime) VALUES (?,?,?,?,?,?)");
			statement.setInt(1, getId());
			statement.setInt(2, type);
			statement.setInt(3, lvl);
			statement.setInt(4, lease);
			statement.setLong(5, rate);
			statement.setLong(6, nextPay);
			statement.execute();
			if(_functions.containsKey(type))
				_functions.remove(type).stopRentTask();
			_functions.put(type, new ClanBaseFunction(type, lvl, lease, rate, nextPay));
		}
		catch(Exception e)
		{
			_log.warn("Exception: updateFunctions(int type, int lvl, int lease, long rate, long nextPay, boolean addNew): " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void stopFunctions()
	{
		for(Integer funcType : _functions.keySet())
			removeFunctions(funcType);
	}

	public void removeFunctions(int functionType)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM clanhall_functions WHERE hall_id=? AND type=?");
			statement.setInt(1, getId());
			statement.setInt(2, functionType);
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warn("Exception: removeFunctions(int functionType): " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}

		if(_functions.containsKey(functionType))
			_functions.remove(functionType).stopRentTask();
	}

	public ClanBaseFunction getFunction(int type)
	{
		return _functions.get(type);
	}

	public void loadFunctions()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("Select * from clanhall_functions where hall_id = ?");
			statement.setInt(1, getId());
			rs = statement.executeQuery();

			while(rs.next())
				_functions.put(rs.getInt("type"), new ClanBaseFunction(rs.getInt("type"), rs.getInt("lvl"), rs.getInt("lease"), rs.getLong("rate"), rs.getLong("endTime")));

		}
		catch(Exception e)
		{
			_log.warn("Exception: ClanHall.loadFunctions(): " + getId() + ", " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}

	public String getLocation()
	{
		return location;
	}

	public Map<Integer, Reinforce> getReinforces()
	{
		return _reinforces;
	}

	public List<TrapReinforce> getTrapReinforce()
	{
		FastList<TrapReinforce> res = new FastList<TrapReinforce>();
		if(_reinforces != null)
		{
			for(Reinforce rf : _reinforces.values())
			{
				if(rf.getType().equalsIgnoreCase("TRAP"))
					res.add((TrapReinforce)rf);
			}
		}
		return res;
	}

	public List<DoorReinforce> getDoorReinforce()
	{
		FastList<DoorReinforce> res = new FastList<DoorReinforce>();
		if(_reinforces != null)
		{
			for(Reinforce rf : _reinforces.values())
			{
				if(rf.getType().equalsIgnoreCase("DOOR"))
					res.add((DoorReinforce)rf);
			}
		}
		return res;
	}

	public List<GuardReinforce> getGuardReinforce()
	{
		FastList<GuardReinforce> res = new FastList<GuardReinforce>();
		if(_reinforces != null)
		{
			for(Reinforce rf : _reinforces.values())
			{
				if(rf.getType().equalsIgnoreCase("GUARD"))
					res.add((GuardReinforce)rf);
			}
		}
		return res;
	}

	public GuardPowerReinforce getGuardPowerReinforce()
	{
		if(_reinforces != null)
			for(Reinforce rf : _reinforces.values())
				if(rf.getType().equalsIgnoreCase("GUARDPOWER"))
					return (GuardPowerReinforce)rf;

		return null;
	}

	public void setSiegeType(String type) 
 	{}

	public void addSkills(String str)
	{
		if(str == null || str.isEmpty())
			return;

		String[] skills = str.split(";");

		if(skills != null)
		{
			for(String skillInfo : skills)
			{
				String[] skill = skillInfo.split("-");
				if(skill != null && skill.length == 2)
				{
					int skillId = Integer.parseInt(skill[0]);
					int skillLvl = Integer.parseInt(skill[1]);
					if(skillId > 0 && skillLvl > 0)
					{
						L2Skill castleSkill = SkillTable.getInstance().getInfo(skillId, skillLvl);

						if(castleSkill != null)
							addSkill(castleSkill);
					}
				}
			}
		}
	}


	public void addSkill(L2Skill skill)
	{
		if(_skills == null)
			_skills = new FastList<L2Skill>();

		_skills.add(skill);
	}

	public List<L2Skill> getSkills()
	{
		return _skills;
	}

	public void giveSkills(L2Player player)
	{
		if(_skills == null || _skills.isEmpty())
			return;

		for(L2Skill skill : _skills)
			player.addSkill(skill);
	}

	public void removeSkills(L2Player player)
	{
		if(_skills == null || _skills.isEmpty())
			return;

		for(L2Skill skill : _skills)
			player.removeSkill(skill);
	}

	protected void parseDoorReinforce(Node g) throws Exception
	{
		Node idNode = g.getAttributes().getNamedItem("id");
		Node nameNode = g.getAttributes().getNamedItem("name");

		int id = (idNode != null) ? Integer.parseInt(idNode.getNodeValue()) : 0;
		String name = (nameNode != null) ? nameNode.getNodeValue() : "";

		if(id != 0)
		{
			DoorReinforce dr = new DoorReinforce(id, 0, getId());
			dr.setName(name);

			for(Node d = g.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if("door".equalsIgnoreCase(d.getNodeName()))
				{
					Node doorIdNode = d.getAttributes().getNamedItem("id");
					if(doorIdNode != null)
						dr.addGate(Integer.parseInt(doorIdNode.getNodeValue()));
				}
				else if("price".equalsIgnoreCase(d.getNodeName()))
				{
					Node levelNode = d.getAttributes().getNamedItem("level");
					Node costNode = d.getAttributes().getNamedItem("cost");
					Node multNode = d.getAttributes().getNamedItem("mul");
					if(levelNode != null && costNode != null)
					{
						dr.setPrice(Integer.parseInt(levelNode.getNodeValue()), Integer.parseInt(costNode.getNodeValue()));
						if(multNode != null)
							dr.setHpMult(Integer.parseInt(levelNode.getNodeValue()), Double.parseDouble(multNode.getNodeValue()));
					}
				}
			}
			addReinforce(dr);
		}
		else
			_log.warn("Siege parse error! Can't parse reinforces in castleId: " + getId());
	}

	protected void parseTrapReinforce(Node g) throws Exception
	{
		Node idNode = g.getAttributes().getNamedItem("id");
		Node nameNode = g.getAttributes().getNamedItem("name");
		Node eventIdNode = g.getAttributes().getNamedItem("eventId");

		int id = (idNode != null) ? Integer.parseInt(idNode.getNodeValue()) : 0;
		String name = (nameNode != null) ? nameNode.getNodeValue() : "";
		int eventId = (eventIdNode != null) ? Integer.parseInt(eventIdNode.getNodeValue()) : 0;

		if(id != 0)
		{
			TrapReinforce tr = new TrapReinforce(id, 0, getId());
			tr.setName(name);
			tr.setEventId(eventId);

			for(Node d = g.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if("zone".equalsIgnoreCase(d.getNodeName()))
				{
					Node doorIdNode = d.getAttributes().getNamedItem("id");
					Node levelNode = d.getAttributes().getNamedItem("level");

					if(doorIdNode != null && levelNode != null)
						tr.addZone(Integer.parseInt(levelNode.getNodeValue()), Integer.parseInt(doorIdNode.getNodeValue()));
				}
				else if("price".equalsIgnoreCase(d.getNodeName()))
				{
					Node levelNode = d.getAttributes().getNamedItem("level");
					Node costNode = d.getAttributes().getNamedItem("cost");
					if(levelNode != null && costNode != null)
						tr.setPrice(Integer.parseInt(levelNode.getNodeValue()), Integer.parseInt(costNode.getNodeValue()));
				}
			}
			addReinforce(tr);
		}
		else
			_log.warn("Siege parse error! Can't parse reinforces in castleId: " + getId());
	}

	protected void parseGuardReinforce(Node g) throws Exception
	{
		Node idNode = g.getAttributes().getNamedItem("id");
		Node nameNode = g.getAttributes().getNamedItem("name");

		int id = (idNode != null) ? Integer.parseInt(idNode.getNodeValue()) : 0;
		String name = (nameNode != null) ? nameNode.getNodeValue() : "";

		if(id != 0)
		{
			Reinforce tr = new GuardReinforce(id, 0, getId());
			tr.setName(name);

			for(Node d = g.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if("price".equalsIgnoreCase(d.getNodeName()))
				{
					Node levelNode = d.getAttributes().getNamedItem("level");
					Node costNode = d.getAttributes().getNamedItem("cost");
					if(levelNode != null && costNode != null)
						tr.setPrice(Integer.parseInt(levelNode.getNodeValue()), Integer.parseInt(costNode.getNodeValue()));
				}
			}
			addReinforce(tr);
		}
		else
			_log.warn("Siege parse error! Can't parse reinforces in castleId: " + getId());
	}

	protected void parseGuardPowerReinforce(Node g) throws Exception
	{
		Node idNode = g.getAttributes().getNamedItem("id");
		Node nameNode = g.getAttributes().getNamedItem("name");

		int id = (idNode != null) ? Integer.parseInt(idNode.getNodeValue()) : 0;
		String name = (nameNode != null) ? nameNode.getNodeValue() : "";

		if(id != 0)
		{
			GuardPowerReinforce gpr = new GuardPowerReinforce(id, 0, getId());
			gpr.setName(name);

			for(Node d = g.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if("price".equalsIgnoreCase(d.getNodeName()))
				{
					Node levelNode = d.getAttributes().getNamedItem("level");
					Node costNode = d.getAttributes().getNamedItem("cost");
					Node multNode = d.getAttributes().getNamedItem("mul");
					if(levelNode != null && costNode != null)
					{
						gpr.setPrice(Integer.parseInt(levelNode.getNodeValue()), Integer.parseInt(costNode.getNodeValue()));
						if(multNode != null)
							gpr.setHpMult(Integer.parseInt(levelNode.getNodeValue()), Double.parseDouble(multNode.getNodeValue()));
					}
				}
			}
			addReinforce(gpr);
		}
		else
			_log.warn("Siege parse error! Can't parse reinforces in castleId: " + getId());
	}

	protected void addReinforce(Reinforce rf)
	{
		if(_reinforces == null)
			_reinforces = new FastMap<Integer, Reinforce>();
		_reinforces.put(rf.getId(), rf);
	}

	public Reinforce getReinforceById(int rId)
	{
		if(_reinforces == null)
			return null;
		return _reinforces.get(rId);
	}

	public void loadReinforces()
	{
		if(getOwnerId() > 0)
		{
			Connection con = null;
			PreparedStatement statement = null;
			ResultSet rset = null;

			try
			{
				con = DatabaseFactory.getInstance().getConnection();

				statement = con.prepareStatement("SELECT * FROM siege_reinforce WHERE siegeUnitId=?");
				statement.setInt(1, getId());
				rset = statement.executeQuery();
				while(rset.next())
				{
					if(getReinforceById(rset.getInt("reinforceId")) != null)
						getReinforceById(rset.getInt("reinforceId")).setLevel(rset.getInt("level"));
				}
				DbUtils.closeQuietly(statement, rset);
			}
			catch(Exception e)
			{
				_log.warn("Exception: loadReinforces(): " + e.getMessage());
				e.printStackTrace();
			}
			finally
			{
				DbUtils.closeQuietly(con, statement, rset);
			}
		}
		else if(_reinforces != null)
		{
			for(Reinforce rf : _reinforces.values())
				rf.setLevel(rf.getMaxLevel());
		}
	}

	public void setLocation(String location)
	{
		this.location = location;
	}

	public void setSiegeZone(L2Zone siegeZone)
	{
		this.siegeZone = siegeZone;
	}

	public L2Zone getSiegeZone()
	{
		return siegeZone;
	}

	public void setHQZone(L2Zone headquartersZone)
	{
		this.headquartersZone = headquartersZone;
	}

	public L2Zone getHQZone()
	{
		return headquartersZone;
	}

	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}

	public int getOwnerId()
	{
		return ownerId;
	}

	public void setOwnerId(int ownerId)
	{
		this.ownerId = ownerId;
	}

	/**
	 * 
	 * @return funcZone Zone
	 */
	public L2Zone getZone()
	{
		return zone;
	}

	public void setZone(L2Zone zone)
	{
		this.zone = zone;
	}

	public void setLease(int lease)
	{
		this.lease = lease;
	}

	/**
	 * Only Ch
	 */
	public void setDesc(String desc)
	{
		this.desc = desc;
	}

	/**
	 * Only Ch
	 */
	public String getDesc()
	{
		return desc;
	}

	/**
	 * Only Ch
	 */
	public void setGrade(int grade)
	{
		this.grade = grade;
	}


	/**
	 * Only Ch
	 */
	public int getGrade()
	{
		return grade;
	}

	public final int getLease()
	{
		if(Config.CH_RENT_RATE)
			return lease * Config.RENT_VALUE;
		return lease;
	}

	/** Return if clanHall is paid or not */
	public boolean isPaid()
	{
		return true;
	}

	public void setPrice(int price)
	{
		this.price = price;
	}

	public int getPrice()
	{
		return price;
	}

	public long getLastTaxTime()
	{
		return _lastTax;
	}

	public void setLastTaxTime(long tax)
	{
		_lastTax = tax;
	}

	public void setResidentZone(L2Zone residentZone)
	{
		this.residentZone = residentZone;
	}

	public L2Spawn getSupplySpawn()
	{
		if(!_supplyBoxes.containsKey(_supplyLevel))
			return null;
		return _supplyBoxes.get(_supplyLevel);
	}

	public FortressType getFortressType()
	{
		return _fortressType;
	}

	public List<SiegeSpawn> getCommanderSpawns()
	{
		return _commanderSpawns;
	}

	public List<CombatFlag> getFlagList()
	{
		return _flagList;
	}

	public List<Integer> getCommandCenterDoors()
	{
		return _commandCenterDoors;
	}

	public List<SiegeSpawn> getPeaceNpcList()
	{
		return _peaceSpawns;
	}

	public L2Spawn getMerchantSpawn()
	{
		return _merchantSpawn;
	}

	public void requestAmbassadors()
	{
		for(Integer castleId : _castles)
			ResidenceManager.getInstance().getBuildingById(castleId).requestAmbassador(getId());
	}

	public void setCastleTax(int amount)
	{
		_taxAmount = amount;
	}

	public int getTaxAmount()
	{
		return _taxAmount;
	}

	public void setContractCastle(int castleId)
	{
		_contractCastle = castleId;
		((FortressSiegeDatabase) _siege.getDatabase()).saveContract();

		if(castleId > 0)
			_lastTax = _lastSiegeDate * 1000 + 60 * 60000;
		startAutoTask();
	}

	/**
	 * Only for load whith server
	 * @param castleId
	 */
	public void loadContractCastle(int castleId)
	{
		_contractCastle = castleId;
	}

	public int getContractCastleId()
	{
		return _contractCastle;
	}

	/**
	 * Only Fort
	 */
	public void setSupplyboxes(Map<Integer, Integer> list)
	{
		for(Integer level : list.keySet())
		{
			if(list.get(level) != null)
			{
				try
				{
					L2NpcTemplate template = NpcTable.getTemplate(list.get(level));
					L2Spawn spawn = new L2Spawn(template);
					spawn.setAmount(1);
					spawn.stopRespawn();
					_supplyBoxes.put(level, spawn);
				}
				catch(Exception e)
				{
					_log.warn(this + " cannot create spawn for supplybox id: " + list.get(level) + " level: " + level + " error: " + e, e);
				}
			}
		}
	}

	public void setSupplyLevel(int level)
	{
		_supplyLevel = level;
		((FortressSiegeDatabase) _siege.getDatabase()).saveSupplyLevel();
	}

	public int getSupplyLevel()
	{
		return _supplyLevel;
	}

	public void loadSupplyLevel(int level)
	{
		_supplyLevel = level;
	}

	public void loadRewardLevel(int level)
	{
		_rewardLevel = level;
	}

	public void setRewardLevel(int level)
	{
		_rewardLevel = level;
		((FortressSiegeDatabase) _siege.getDatabase()).saveRewardLevel();
	}

	public int getRewardLevel()
	{
		return _rewardLevel;
	}

	/**
	 * Only Castle
	 */
	public abstract Map<Integer, L2Zone> getTrapZones();

	public void requestAmbassador(int fortId)
	{
		if(getOwnerId() > 0 && _ambassadors.containsKey(fortId))
		{
			_ambassadors.get(fortId).startRespawn();
			_ambassadors.get(fortId).doSpawn(true);
			ThreadPoolManager.getInstance().scheduleGeneral(new DespawnAmbassador(fortId), 3600000);
		}
	}

	public abstract int getMinLeftForTax();

	public int getMinLeftForRebel()
	{
		if(_holdTask != null)
			return (int)_holdTask.getDelay(TimeUnit.MINUTES);
		return 0;
	}

	public List<Integer> getMainControllers()
	{
		return _mainControllers;
	}

	public List<Integer> getDoorControllers()
	{
		return _doorControllers;
	}

	public List<Integer> getControlDoors()
	{
		return _controlDoors;
	}

	public void powerOff()
	{
		if(getSiege().isInProgress())
		{
			_powerOnTime = System.currentTimeMillis() + 600000;
			getSiege().killedCommander();
		}
	}

	public long getPowerOnTime()
	{
		return _powerOnTime;
	}

	public void setPowerOnTime(long time)
	{
		_powerOnTime = time;
	}

	public int getSize()
	{
		return getMainControllers().size() > 0 ? 5 : 3;
	}

	/**
	 * Only Castle
	 */
	public boolean isParent(int fortId)
	{
		return _ambassadors.containsKey(fortId);
	}


	public List<SiegeSpawn> getControlTowerSpawns()
	{
		return _controlTowerSpawnList;
	}

	public List<SiegeSpawn> getArtifcatSpawns()
	{
		return _artefactSpawnList;
	}

	/**
	 * Only Castle
	 */
	public void parseCastle(Node sn) throws Exception
	{
		for(Node s = sn.getFirstChild(); s != null; s = s.getNextSibling())
		{
			if("reinforces".equalsIgnoreCase(s.getNodeName()))
			{
				for(Node g = s.getFirstChild(); g != null; g = g.getNextSibling())
				{
					if("gate".equalsIgnoreCase(g.getNodeName()))
						parseDoorReinforce(g);
					else if("trap".equalsIgnoreCase(g.getNodeName()))
						parseTrapReinforce(g);
				}
			}
			else if("towers".equalsIgnoreCase(s.getNodeName()))
			{
				_ctLoosPenalty = s.getAttributes().getNamedItem("loosePenalty") != null ? Long.parseLong(s.getAttributes().getNamedItem("loosePenalty").getNodeValue()) : 150000;
				for(Node t = s.getFirstChild(); t != null; t = t.getNextSibling())
				{
					if("control".equalsIgnoreCase(t.getNodeName()))
						_controlTowerSpawnList.add(parseControlTower(t));
				}
			}
			else if("artefacts".equalsIgnoreCase(s.getNodeName()))
			{
				for(Node t = s.getFirstChild(); t != null; t = t.getNextSibling())
				{
					if("artefact".equalsIgnoreCase(t.getNodeName()))
						_artefactSpawnList.add(parseArtefact(t));
				}
			}
			else if("skills".equalsIgnoreCase(s.getNodeName()))
			{
				for(Node t = s.getFirstChild(); t != null; t = t.getNextSibling())
					if("skill".equalsIgnoreCase(t.getNodeName()))
					{
						int skillId = t.getAttributes().getNamedItem("id") != null ? Integer.parseInt(t.getAttributes().getNamedItem("id").getNodeValue()) : 0;
						int skillLvl = t.getAttributes().getNamedItem("level") != null ? Integer.parseInt(t.getAttributes().getNamedItem("level").getNodeValue()) : 0;

						if(skillId > 0 && skillLvl > 0)
						{
							L2Skill castleSkill = SkillTable.getInstance().getInfo(skillId, skillLvl);

							if(castleSkill != null)
								addSkill(castleSkill);
							else
								_log.warn("Castle: " + this + " skill not found id: " + skillId + " level: " + skillLvl);
						}
					}
			}
			else if("ambassadors".equalsIgnoreCase(s.getNodeName()))
			{
				for(Node t = s.getFirstChild(); t != null; t = t.getNextSibling())
					if("ambassador".equalsIgnoreCase(t.getNodeName()))
					{
						int npcId = Integer.parseInt(t.getAttributes().getNamedItem("npc").getNodeValue());
						int fortId = Integer.parseInt(t.getAttributes().getNamedItem("fortress").getNodeValue());
						int x = Integer.parseInt(t.getAttributes().getNamedItem("x").getNodeValue());
						int y = Integer.parseInt(t.getAttributes().getNamedItem("y").getNodeValue());
						int z = Integer.parseInt(t.getAttributes().getNamedItem("z").getNodeValue());
						int h = Integer.parseInt(t.getAttributes().getNamedItem("heading").getNodeValue());
						L2Spawn spawn = new L2Spawn(NpcTable.getTemplate(npcId));
						spawn.setLoc(new Location(x, y, z, h));
						spawn.setAmount(1);
						spawn.setRespawnDelay(60);
						spawn.stopRespawn();
						_ambassadors.put(fortId, spawn);
					}
			}
		}
	}

	private SiegeSpawn parseArtefact(Node c) throws Exception
	{
		Node x = c.getAttributes().getNamedItem("x");
		Node y = c.getAttributes().getNamedItem("y");
		Node z = c.getAttributes().getNamedItem("z");
		Node head = c.getAttributes().getNamedItem("heading");
		Node npc = c.getAttributes().getNamedItem("npc");

		return new SiegeSpawn(getId(), Integer.parseInt(x.getNodeValue()), Integer.parseInt(y.getNodeValue()), Integer.parseInt(z.getNodeValue()), Integer.parseInt(head.getNodeValue()), Integer.parseInt(npc.getNodeValue()));
	}

	private SiegeSpawn parseControlTower(Node c) throws Exception
	{
		Node x = c.getAttributes().getNamedItem("x");
		Node y = c.getAttributes().getNamedItem("y");
		Node z = c.getAttributes().getNamedItem("z");
		Node npc = c.getAttributes().getNamedItem("npc");
		Node hp = c.getAttributes().getNamedItem("hp");
		Node controlTrap = c.getAttributes().getNamedItem("controlTrap");
		SiegeSpawn ss = new SiegeSpawn(getId(), Integer.parseInt(x.getNodeValue()), Integer.parseInt(y.getNodeValue()), Integer.parseInt(z.getNodeValue()), 0, Integer.parseInt(npc.getNodeValue()), Integer.parseInt(hp.getNodeValue()));
		if(controlTrap != null)
			ss.setControlId(Integer.parseInt(controlTrap.getNodeValue()));
		return ss;
	}

	public void loadTown(int townId)
	{
		_townId = townId;
	}

	protected class DespawnAmbassador implements Runnable
	{
		private int _fortId;

		public DespawnAmbassador(int fortId)
		{
			_fortId = fortId;
		}

		public void run()
		{
			_ambassadors.get(_fortId).stopRespawn();
			_ambassadors.get(_fortId).despawnAll();
		}
	}

	public int getContractedFortressId()
	{
		for(Integer fortId : _ambassadors.keySet())
			if(ResidenceManager.getInstance().getBuildingById(fortId).getContractCastleId() == getId() && System.currentTimeMillis() - ResidenceManager.getInstance().getBuildingById(fortId).getLastSiegeDate()  > 60 * 60000)
				return fortId;

		return 0;
	}

	public int getCollectedShops()
	{
		return _collectedShops;
	}

	public int getCollectedSeed()
	{
		return _collectedSeed;
	}

	public void setCollectedShops(int value)
	{
		_collectedShops = value;
	}

	public void setCollectedSeed(int value)
	{
		_collectedSeed = value;
	}

	public void startHoldTask()
	{
		if(getOwnerId() > 0)
		{
			if(_lastSiegeDate * 1000L + _holdTime < System.currentTimeMillis())
			{
				_log.info("Hold time for " + getName() + " has expired, change owner to NPC. " + new Date(_lastSiegeDate * 1000L + _holdTime));
				getOwner().setHasFortress(0);
				getOwner().broadcastToOnlineMembers(new SystemMessage(SystemMessage.THE_REBEL_ARMY_RECAPTURED_THE_FORTRESS));
				changeOwner(0);
			}
			else
			{
				_log.info("Start hold task for " + getName() + " to: " + new Date(_lastSiegeDate * 1000L + _holdTime));
				if(_holdTask != null)
					_holdTask.cancel(true);

				_holdTask = ThreadPoolManager.getInstance().scheduleGeneral(new HoldTask(), (_lastSiegeDate * 1000L + _holdTime) - System.currentTimeMillis());
			}
		}
	}

	public void stopHoldTask()
	{
		if(_holdTask != null)
			_holdTask.cancel(true);

		_holdTask = null;
	}

	private class HoldTask implements Runnable
	{
		public void run()
		{
			if(getOwnerId() > 0)
			{
				_log.info(SiegeUnit.this + " has expired hold time, change owner to NPC.");
				getOwner().setHasFortress(0);
				getOwner().broadcastToOnlineMembers(new SystemMessage(SystemMessage.THE_REBEL_ARMY_RECAPTURED_THE_FORTRESS));
				changeOwner(0);
			}
		}
	}

	/**
	 * Возвращает дату последней осады
	 *
	 * @return дата осады в unixtime
	 */
	public long getLastSiegeDate()
	{
		return _lastSiegeDate;
	}

	public void setLastSiegeDate(long time)
	{
		_lastSiegeDate = time;
	}

	public void setHoldTime(int hours)
	{
		_holdTime = hours * 60 * 60000;
	}

	public long getHoldTime()
	{
		return _holdTime;
	}

	public void setRebelTime(int hour)
	{
		_rebelTime = hour * 60;
	}

	public long getRebelTime()
	{
		return _rebelTime;
	}

	public void spawnFlagPoles()
	{
		for(SiegeSpawn sp : _flagPoleSpawns)
		{
			String line = "FlagPole;" + sp.getNpcId() + ";" + sp.getLoc().getX() + ";" + sp.getLoc().getY() + ";" + sp.getLoc().getZ() + ";3;none;0;0";
			getSiege().addFlagPole(StaticObjectsTable.parse(line));
		}
	}

	public long getControlTowerLosePenalty()
	{
		return _ctLoosPenalty;
	}

	public void parseFort(Node f) throws Exception
	{
		_fortressType = FortressType.valueOf(f.getAttributes().getNamedItem("type").getNodeValue().toUpperCase());

		for(Node s = f.getFirstChild(); s != null; s = s.getNextSibling())
		{
			if("castles".equalsIgnoreCase(s.getNodeName()))
			{
				for(Node c = s.getFirstChild(); c != null; c = c.getNextSibling())
				{
					if("castle".equalsIgnoreCase(c.getNodeName()))
						_castles.add(Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue()));
				}
			}
			else if("skills".equalsIgnoreCase(s.getNodeName()))
			{
				for(Node t = s.getFirstChild(); t != null; t = t.getNextSibling())
					if("skill".equalsIgnoreCase(t.getNodeName()))
					{
						int skillId = t.getAttributes().getNamedItem("id") != null ? Integer.parseInt(t.getAttributes().getNamedItem("id").getNodeValue()) : 0;
						int skillLvl = t.getAttributes().getNamedItem("level") != null ? Integer.parseInt(t.getAttributes().getNamedItem("level").getNodeValue()) : 0;
						if(skillId > 0 && skillLvl > 0)
						{
							L2Skill castleSkill = SkillTable.getInstance().getInfo(skillId, skillLvl);

							if(castleSkill != null)
								addSkill(castleSkill);
							else
								_log.warn("Castle: " + this + " skill not found id: " + skillId + " level: " + skillLvl);
						}
					}
			}
			else if("commanders".equalsIgnoreCase(s.getNodeName()))
			{
				for(Node c = s.getFirstChild(); c != null; c = c.getNextSibling())
				{
					if("commander".equalsIgnoreCase(c.getNodeName()))
					{
						int x = Integer.parseInt(c.getAttributes().getNamedItem("x").getNodeValue());
						int y = Integer.parseInt(c.getAttributes().getNamedItem("y").getNodeValue());
						int z = Integer.parseInt(c.getAttributes().getNamedItem("z").getNodeValue());
						int h = Integer.parseInt(c.getAttributes().getNamedItem("heading").getNodeValue());
						int peaceNpc = Integer.parseInt(c.getAttributes().getNamedItem("peaceNpc").getNodeValue());
						int npc = Integer.parseInt(c.getAttributes().getNamedItem("npc").getNodeValue());
						_commanderSpawns.add(new SiegeSpawn(getId(), x, y, z, h, npc));
						_peaceSpawns.add(new SiegeSpawn(getId(), x, y, z, h, peaceNpc));
					}
				}
			}
			else if("flags".equalsIgnoreCase(s.getNodeName()))
			{
				for(Node c = s.getFirstChild(); c != null; c = c.getNextSibling())
				{
					if("flag".equalsIgnoreCase(c.getNodeName()))
					{
						int x = Integer.parseInt(c.getAttributes().getNamedItem("x").getNodeValue());
						int y = Integer.parseInt(c.getAttributes().getNamedItem("y").getNodeValue());
						int z = Integer.parseInt(c.getAttributes().getNamedItem("z").getNodeValue());
						int itemId = Integer.parseInt(c.getAttributes().getNamedItem("item").getNodeValue());
						_flagList.add(new CombatFlag(getId(), x, y, z, 0, itemId));

					}
				}
			}
			else if("commandCenterDoors".equalsIgnoreCase(s.getNodeName()))
			{
				for(Node c = s.getFirstChild(); c != null; c = c.getNextSibling())
				{
					if("door".equalsIgnoreCase(c.getNodeName()))
					{
						int doorId = Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue());
						_commandCenterDoors.add(doorId);
					}
				}
			}
			else if("flagPole".equalsIgnoreCase(s.getNodeName()))
			{
				int x = Integer.parseInt(s.getAttributes().getNamedItem("x").getNodeValue());
				int y = Integer.parseInt(s.getAttributes().getNamedItem("y").getNodeValue());
				int z = Integer.parseInt(s.getAttributes().getNamedItem("z").getNodeValue());
				int h = Integer.parseInt(s.getAttributes().getNamedItem("heading").getNodeValue());
				int id = Integer.parseInt(s.getAttributes().getNamedItem("id").getNodeValue());
				_flagPoleSpawns.add(new SiegeSpawn(getId(), x, y, z, h, id));
			}
			else if("suspiciousMerchant".equalsIgnoreCase(s.getNodeName()))
			{
				int x = Integer.parseInt(s.getAttributes().getNamedItem("x").getNodeValue());
				int y = Integer.parseInt(s.getAttributes().getNamedItem("y").getNodeValue());
				int z = Integer.parseInt(s.getAttributes().getNamedItem("z").getNodeValue());
				int h = Integer.parseInt(s.getAttributes().getNamedItem("heading").getNodeValue());
				int id = Integer.parseInt(s.getAttributes().getNamedItem("npc").getNodeValue());
				_merchantSpawn = new L2Spawn(NpcTable.getTemplate(id));
				_merchantSpawn.setLoc(new Location(x, y, z, h));
				_merchantSpawn.setRespawnDelay(60);
				_merchantSpawn.setAmount(1);
			}
			else if("reinforces".equalsIgnoreCase(s.getNodeName()))
			{
				for(Node g = s.getFirstChild(); g != null; g = g.getNextSibling())
				{
					if("gate".equalsIgnoreCase(g.getNodeName()))
						parseDoorReinforce(g);
					else if("guard".equalsIgnoreCase(g.getNodeName()))
						parseGuardReinforce(g);
					else if("guardPower".equalsIgnoreCase(g.getNodeName()))
						parseGuardPowerReinforce(g);
				}
			}
			else if("powercontroller".equalsIgnoreCase(s.getNodeName()))
			{
				for(Node g = s.getFirstChild(); g != null; g = g.getNextSibling())
				{
					if("switch".equalsIgnoreCase(g.getNodeName()))
					{
						if("main".equalsIgnoreCase(g.getAttributes().getNamedItem("type").getNodeValue()))
						{
							for(Node c = g.getFirstChild(); c != null; c = c.getNextSibling())
							{
								if("controller".equalsIgnoreCase(c.getNodeName()))
								{
									int id = Integer.parseInt(c.getAttributes().getNamedItem("npc").getNodeValue());
									_mainControllers.add(id);
								}
							}
						}
						else if("door".equalsIgnoreCase(g.getAttributes().getNamedItem("type").getNodeValue()))
						{
							for(Node c = g.getFirstChild(); c != null; c = c.getNextSibling())
							{
								if("controller".equalsIgnoreCase(c.getNodeName()))
								{
									int id = Integer.parseInt(c.getAttributes().getNamedItem("npc").getNodeValue());
									_doorControllers.add(id);
								}
								else if("door".equalsIgnoreCase(c.getNodeName()))
								{
									int id = Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue());
									_controlDoors.add(id);
								}
							}
						}
					}
				}
			}
		}
	}
}