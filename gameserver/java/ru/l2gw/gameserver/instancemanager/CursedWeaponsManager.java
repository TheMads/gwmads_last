package ru.l2gw.gameserver.instancemanager;

import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.CursedWeapon;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.util.Location;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

public class CursedWeaponsManager
{
	private static final Log _log = LogFactory.getLog(CursedWeaponsManager.class.getName());

	private static CursedWeaponsManager _instance;

	public static CursedWeaponsManager getInstance()
	{
		if(_instance == null)
			_instance = new CursedWeaponsManager();
		return _instance;
	}

	Map<Integer, CursedWeapon> _cursedWeapons;
	@SuppressWarnings("unchecked")
	private ScheduledFuture<?> _removeTask;

	private static final int CURSEDWEAPONS_MAINTENANCE_INTERVAL = 5 * 60 * 1000; // 5 min in millisec

	public CursedWeaponsManager()
	{
		_cursedWeapons = new FastMap<Integer, CursedWeapon>();

		_log.info("CursedWeaponsManager: Initializing");

		load();
		restore();
		checkConditions();

		cancelTask();
		_removeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RemoveTask(), CURSEDWEAPONS_MAINTENANCE_INTERVAL, CURSEDWEAPONS_MAINTENANCE_INTERVAL);

		_log.info("CursedWeaponsManager: Loaded " + _cursedWeapons.size() + " cursed weapon(s).");
	}

	public final void reload()
	{
		_instance = new CursedWeaponsManager();
	}

	private void load()
	{
		if(Config.DEBUG)
			System.out.print("CursedWeaponsManager: Parsing ... ");
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			File file = new File(Config.DATAPACK_ROOT + "/data/cursed_weapons.xml");
			if(!file.exists())
			{
				if(Config.DEBUG)
					System.out.println("CursedWeaponsManager: NO FILE");
				return;
			}

			Document doc = factory.newDocumentBuilder().parse(file);

			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				if("list".equalsIgnoreCase(n.getNodeName()))
					for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						if("item".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							Integer skillId = Integer.parseInt(attrs.getNamedItem("skillId").getNodeValue());
							String name = "Unknown cursed weapon";
							if(attrs.getNamedItem("name") != null)
								name = attrs.getNamedItem("name").getNodeValue();
							else if(ItemTable.getInstance().getTemplate(id) != null)
								name = ItemTable.getInstance().getTemplate(id).getName();

							if(id == 0)
								continue;

							CursedWeapon cw = new CursedWeapon(id, skillId, name);
							for(Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
								if("dropRate".equalsIgnoreCase(cd.getNodeName()))
									cw.setDropRate(Integer.parseInt(cd.getAttributes().getNamedItem("val").getNodeValue()));
								else if("duration".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									cw.setDurationMin(Integer.parseInt(attrs.getNamedItem("min").getNodeValue()));
									cw.setDurationMax(Integer.parseInt(attrs.getNamedItem("max").getNodeValue()));
								}
								else if("durationLost".equalsIgnoreCase(cd.getNodeName()))
									cw.setDurationLost(Integer.parseInt(cd.getAttributes().getNamedItem("val").getNodeValue()));
								else if("disapearChance".equalsIgnoreCase(cd.getNodeName()))
									cw.setDisapearChance(Integer.parseInt(cd.getAttributes().getNamedItem("val").getNodeValue()));
								else if("stageKills".equalsIgnoreCase(cd.getNodeName()))
									cw.setStageKills(Integer.parseInt(cd.getAttributes().getNamedItem("val").getNodeValue()));
								else if("transformationId".equalsIgnoreCase(cd.getNodeName()))
									cw.setTransformationId(Integer.parseInt(cd.getAttributes().getNamedItem("val").getNodeValue()));
								else if("transformationTemplateId".equalsIgnoreCase(cd.getNodeName()))
									cw.setTransformationTemplateId(Integer.parseInt(cd.getAttributes().getNamedItem("val").getNodeValue()));
								else if("transformationName".equalsIgnoreCase(cd.getNodeName()))
									cw.setTransformationName(cd.getAttributes().getNamedItem("val").getNodeValue());

							// Store cursed weapon
							_cursedWeapons.put(id, cw);
						}

			if(Config.DEBUG)
				System.out.println("CursedWeaponsManager: OK");
		}
		catch(Exception e)
		{
			_log.error("CursedWeaponsManager: Error parsing cursed_weapons file. " + e);

			if(Config.DEBUG)
				System.out.println("CursedWeaponsManager: ERROR");
		}
	}

	private void restore()
	{
		if(Config.DEBUG)
			System.out.print("CursedWeaponsManager: restoring ... ");

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("SELECT * FROM cursed_weapons");
			rset = statement.executeQuery();

			while(rset.next())
			{
				int itemId = rset.getInt("item_id");
				CursedWeapon cw = _cursedWeapons.get(itemId);
				if(cw != null)
				{
					cw.setPlayerId(rset.getInt("player_id"));
					cw.setPlayerKarma(rset.getInt("player_karma"));
					cw.setPlayerPkKills(rset.getInt("player_pkkills"));
					cw.setNbKills(rset.getInt("nb_kills"));
					cw.setLoc(new Location(rset.getInt("x"), rset.getInt("y"), rset.getInt("z")));
					cw.setEndTime(rset.getLong("end_time") * 1000);

					if(!cw.reActivate())
						endOfLife(cw);
				}
				else
				{
					removeFromDb(itemId);
					_log.warn("CursedWeaponsManager: Unknown cursed weapon " + itemId + ", deleted");
				}
			}
			if(Config.DEBUG)
				System.out.println("CursedWeaponsManager: OK");
		}
		catch(Exception e)
		{
			_log.warn("CursedWeaponsManager: Could not restore cursed_weapons data: " + e);
			e.printStackTrace();

			if(Config.DEBUG)
				System.out.println("CursedWeaponsManager: ERROR");
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	private void checkConditions()
	{
		if(Config.DEBUG)
			System.out.print("CursedWeaponsManager: Checking conditions ... ");

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("SELECT owner_id FROM items WHERE item_id=?");

			for(CursedWeapon cw : _cursedWeapons.values())
			{
				// Do an item check to be sure that the cursed weapon and/or skill isn't hold by someone
				int itemId = cw.getItemId();
				boolean foundedInItems = false;

				statement.setInt(1, itemId);
				rset = statement.executeQuery();

				while(rset.next())
				{
					// A player has the cursed weapon in his inventory ...
					int playerId = rset.getInt("owner_id");

					if(!foundedInItems)
					{
						if(playerId != cw.getPlayerId() || cw.getPlayerId() == 0)
						{
							emptyPlayerCursedWeapon(playerId, itemId, cw);
							_log.info("CursedWeaponsManager[254]: Player " + playerId + " owns the cursed weapon " + itemId + " but he shouldn't.");
						}
						else
							foundedInItems = true;
					}
					else
					{
						emptyPlayerCursedWeapon(playerId, itemId, cw);
						_log.info("CursedWeaponsManager[262]: Player " + playerId + " owns the cursed weapon " + itemId + " but he shouldn't.");
					}
				}

				if(!foundedInItems && cw.getPlayerId() != 0)
				{
					removeFromDb(cw.getItemId());

					_log.info("CursedWeaponsManager: Unownered weapon, removing from table...");
				}
			}
		}
		catch(Exception e)
		{
			_log.warn("CursedWeaponsManager: Could not check cursed_weapons data: " + e);

			if(Config.DEBUG)
				System.out.println("CursedWeaponsManager: ERROR");
			return;
		}
		finally
		{
			DbUtils.closeQuietly(statement);
		}

		if(Config.DEBUG)
			System.out.println("CursedWeaponsManager: DONE");
	}

	private void emptyPlayerCursedWeapon(int playerId, int itemId, CursedWeapon cw)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			// Delete the item
			statement = con.prepareStatement("DELETE FROM items WHERE owner_id=? AND item_id=?");
			statement.setInt(1, playerId);
			statement.setInt(2, itemId);
			statement.executeUpdate();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("UPDATE characters SET karma=?, pkkills=? WHERE obj_id=?");
			statement.setInt(1, cw.getPlayerKarma());
			statement.setInt(2, cw.getPlayerPkKills());
			statement.setInt(3, playerId);
			if(statement.executeUpdate() != 1)
				_log.warn("Error while updating karma & pkkills for userId " + cw.getPlayerId());
			// clean up the cursedweapons table.
			removeFromDb(itemId);
		}
		catch(SQLException e)
		{}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void removeFromDb(int itemId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("DELETE FROM cursed_weapons WHERE item_id = ?");
			statement.setInt(1, itemId);
			statement.executeUpdate();

			if(getCursedWeapon(itemId) != null)
				getCursedWeapon(itemId).initWeapon();
		}
		catch(SQLException e)
		{
			_log.error("CursedWeaponsManager: Failed to remove data: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	private void cancelTask()
	{
		if(_removeTask != null)
		{
			_removeTask.cancel(true);
			_removeTask = null;
		}
	}

	private class RemoveTask implements Runnable
	{
		public void run()
		{
			for(CursedWeapon cw : _cursedWeapons.values())
				if(cw.isActive() && cw.getTimeLeft() <= 0)
					endOfLife(cw);
		}
	}

	public void endOfLife(int item_id)
	{
		CursedWeapon cw = _cursedWeapons.get(item_id);
		if(_cursedWeapons != null)
		{
			cw.setEndTime(System.currentTimeMillis());
			endOfLife(cw);
		}
	}

	public void endOfLife(CursedWeapon cw)
	{
		if(cw.isActivated())
		{
			if(cw.getPlayer() != null && cw.getPlayer().isOnline())
			{
				L2Player player = cw.getPlayer();

				// Remove from player
				_log.info("CursedWeaponsManager: " + cw.getName() + " being removed online from " + player + ".");

				player.abortAttack();

				player.setKarma(cw.getPlayerKarma());
				player.setPkKills(cw.getPlayerPkKills());
				player.setCursedWeaponEquippedId(0);
				player.setTransformation(0);
				player.setTransformationName(null);
				player.removeSkill(SkillTable.getInstance().getInfo(cw.getSkillId(), player.getSkillLevel(cw.getSkillId())), false);
				player.removeSkill(SkillTable.getInstance().getInfo(3630, 1), false);
				player.removeSkill(SkillTable.getInstance().getInfo(3631, 1), false);

				// Remove
				player.getInventory().unEquipItemAndSendChanges(player.getActiveWeaponInstance());
				player.store();

				// Destroy
				if(player.getInventory().destroyItemByItemId("EndOfLife", cw.getItemId(), 1, player, null) == null)
					_log.info("CursedWeaponsManager[395]: Error! Cursed weapon not found!!!");

				player.broadcastUserInfo();
			}
			else
			{
				// Remove from Db
				_log.info("CursedWeaponsManager: " + cw.getName() + " being removed offline.");

				Connection con = null;
				PreparedStatement statement = null;
				try
				{
					con = DatabaseFactory.getInstance().getConnection();

					// Delete the item
					statement = con.prepareStatement("DELETE FROM items WHERE owner_id=? AND item_id=?");
					statement.setInt(1, cw.getPlayerId());
					statement.setInt(2, cw.getItemId());
					statement.execute();
					DbUtils.closeQuietly(statement);

					// Restore the karma
					statement = con.prepareStatement("UPDATE characters SET karma=?, pkkills=? WHERE obj_Id=?");
					statement.setInt(1, cw.getPlayerKarma());
					statement.setInt(2, cw.getPlayerPkKills());
					statement.setInt(3, cw.getPlayerId());
					statement.execute();
				}
				catch(SQLException e)
				{
					_log.warn("CursedWeaponsManager: Could not delete : " + e);
				}
				finally
				{
					DbUtils.closeQuietly(con, statement);
				}
			}
		}
		else
		// either this cursed weapon is in the inventory of someone who has another cursed weapon equipped,
		// OR this cursed weapon is on the ground.
		{
			if(cw.getPlayer() != null && cw.getPlayer().getInventory().getItemByItemId(cw.getItemId()) != null)
			{
				L2Player player = cw.getPlayer();
				if(cw.getPlayer().getInventory().destroyItemByItemId("EndOfLife", cw.getItemId(), 1, cw.getPlayer(), null) == null)
					_log.info("CursedWeaponsManager[453]: Error! Cursed weapon not found!!!");

				player.broadcastUserInfo(true);
			}
			// is dropped on the ground
			else if(cw.getItem() != null)
			{
				cw.getItem().deleteMe();
				_log.info("CursedWeaponsManager: " + cw.getName() + " item has been removed from World.");
			}
		}

		cw.initWeapon();
		removeFromDb(cw.getItemId());

		announce(new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED_CW).addString(cw.getName()));
	}

	public void saveData(CursedWeapon cw)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			// Delete previous datas
			statement = con.prepareStatement("DELETE FROM cursed_weapons WHERE item_id = ?");
			statement.setInt(1, cw.getItemId());
			statement.executeUpdate();
			DbUtils.closeQuietly(statement);
			statement = null;
			if(cw.isActive())
			{
				statement = con.prepareStatement("REPLACE INTO cursed_weapons (item_id, player_id, player_karma, player_pkkills, nb_kills, x, y, z, end_time) VALUES (?,?,?,?,?,?,?,?,?)");
				statement.setInt(1, cw.getItemId());
				statement.setInt(2, cw.getPlayerId());
				statement.setInt(3, cw.getPlayerKarma());
				statement.setInt(4, cw.getPlayerPkKills());
				statement.setInt(5, cw.getNbKills());
				statement.setInt(6, cw.getLoc().getX());
				statement.setInt(7, cw.getLoc().getY());
				statement.setInt(8, cw.getLoc().getZ());
				statement.setLong(9, cw.getEndTime() / 1000);
				statement.executeUpdate();
			}
		}
		catch(SQLException e)
		{
			_log.error("CursedWeapon: Failed to save data: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void saveData()
	{
		if(Config.DEBUG)
			System.out.println("CursedWeaponsManager: saving data to disk.");
		for(CursedWeapon cw : _cursedWeapons.values())
			saveData(cw);
	}

	/**
	 * вызывается при логине игрока
	 */
	public void checkPlayer(L2Player player)
	{
		for(CursedWeapon cw : _cursedWeapons.values())
			if(player.getInventory().getItemByItemId(cw.getItemId()) != null)
				checkPlayer(player, player.getInventory().getItemByItemId(cw.getItemId()));
	}

	/**
	 * вызывается, когда проклятое оружие оказывается в инвентаре игрока
	 */
	public void checkPlayer(L2Player player, L2ItemInstance item)
	{
		if(player == null || item == null)
			return;

		CursedWeapon cw = _cursedWeapons.get(item.getItemId());
		if(cw == null)
			return;

		if(player.getObjectId() == cw.getPlayerId() || cw.getPlayerId() == 0 || cw.isDropped())
		{
			activate(player, item);
			showUsageTime(player, cw);
		}
		else
		{
			// wtf? how you get it?
			_log.warn("CursedWeaponsManager: " + player + " tried to obtain " + item + " in wrong way");
			player.getInventory().destroyItem("Unknown", item.getObjectId(), item.getCount(), player, null);
		}
	}

	public void activate(L2Player player, L2ItemInstance item)
	{
		CursedWeapon cw = _cursedWeapons.get(item.getItemId());
		if(cw != null)
		{
			if(player.isCursedWeaponEquipped()) // cannot own 2 cursed swords
			{
				if(player.getCursedWeaponEquippedId() != item.getItemId())
				{
					CursedWeapon cw2 = _cursedWeapons.get(player.getCursedWeaponEquippedId());
					cw2.increaseLevel();
				}

				cw.setPlayer(player);

				// erase the newly obtained cursed weapon
				endOfLife(cw);
			}
			else if(cw.getTimeLeft() > 0)
			{
				cw.activate(player, item);
				saveData(cw);

				SystemMessage sm = new SystemMessage(SystemMessage.THE_OWNER_OF_S2_HAS_APPEARED_IN_THE_S1_REGION);
				sm.addZoneName(cw.getPlayer().getX(), cw.getPlayer().getY(), cw.getPlayer().getZ()); // Region Name
				sm.addString(cw.getName());
				announce(sm);
			}
			else
				endOfLife(cw);
		}
	}

	public void doLogout(L2Player player)
	{
		for(CursedWeapon cw : _cursedWeapons.values())
			if(player.getInventory().getItemByItemId(cw.getItemId()) != null)
			{
				cw.setPlayer(null);
				cw.setItem(null);
			}
	}

	/**
	 * drop from L2NpcInstance killed by L2Player
	 */
	public void dropAttackable(L2NpcInstance attackable, L2Player killer)
	{
		if(killer.isCursedWeaponEquipped() || _cursedWeapons.size() == 0 || attackable.isRaid())
			return;
		if(!Config.ALLOW_CURSED_WEAPONS)
			return;
		if(Config.CURSED_WEAPONS_MIN_PLAYERS_DROP > 0 && (L2ObjectsStorage.getAllPlayersCount() - L2ObjectsStorage.getAllOfflineCount()) < Config.CURSED_WEAPONS_MIN_PLAYERS_DROP)
			return;

		synchronized(_cursedWeapons)
		{
			int num = 0;
			short count = 0;
			byte breakFlag = 0;

			while(breakFlag == 0)
			{
				num = _cursedWeapons.keySet().toArray(new Integer[_cursedWeapons.size()])[Rnd.get(_cursedWeapons.size())];
				count++;

				if(_cursedWeapons.get(num) != null && !_cursedWeapons.get(num).isActive())
					breakFlag = 1;
				else if(count >= getCursedWeapons().size())
					breakFlag = 2;
			}

			if(breakFlag == 1)
				_cursedWeapons.get(num).dropIt(attackable, killer);
		}
	}

	// drop from killed L2Player (loosing CursedWeapon)
	public void dropPlayer(L2Character cha)
	{
		if(!cha.isPlayer())
			return;

		CursedWeapon cw = _cursedWeapons.get(((L2Player) cha).getCursedWeaponEquippedId());
		if(cw == null)
			return;

		if(cw.dropIt(null, null))
		{
			saveData(cw);

			SystemMessage sm = new SystemMessage(SystemMessage.S2_WAS_DROPPED_IN_THE_S1_REGION);
			sm.addZoneName(cw.getPlayer().getX(), cw.getPlayer().getY(), cw.getPlayer().getZ()); // Region Name
			sm.addString(cw.getName());
			announce(sm);
		}
		else
			endOfLife(cw);
	}

	public void increaseKills(int itemId)
	{
		CursedWeapon cw = _cursedWeapons.get(itemId);
		if(cw != null)
		{
			cw.increaseKills();
			saveData(cw);
		}
	}

	public void increaseLevel(int itemId)
	{
		CursedWeapon cw = _cursedWeapons.get(itemId);
		if(cw != null)
		{
			cw.increaseLevel();
			saveData(cw);
		}
	}

	public int getLevel(int itemId)
	{
		CursedWeapon cw = _cursedWeapons.get(itemId);
		return cw != null ? cw.getLevel() : 0;
	}

	public void announce(SystemMessage sm)
	{
		for(L2Player player : L2ObjectsStorage.getAllPlayers())
			player.sendPacket(sm);
	}

	public void showUsageTime(L2Player player, short itemId)
	{
		CursedWeapon cw = _cursedWeapons.get(itemId);
		if(cw != null)
			showUsageTime(player, cw);
	}

	public void showUsageTime(L2Player player, CursedWeapon cw)
	{
		SystemMessage sm = new SystemMessage(SystemMessage.S2_MINUTE_OF_USAGE_TIME_ARE_LEFT_FOR_S1);
		sm.addString(cw.getName());
		sm.addNumber((new Long(cw.getTimeLeft() / 60000)).intValue());
		player.sendPacket(sm);
	}

	public boolean isCursed(int itemId)
	{
		return _cursedWeapons.containsKey(itemId);
	}

	public Collection<CursedWeapon> getCursedWeapons()
	{
		return _cursedWeapons.values();
	}

	public Set<Integer> getCursedWeaponsIds()
	{
		return _cursedWeapons.keySet();
	}

	public CursedWeapon getCursedWeapon(int itemId)
	{
		return _cursedWeapons.get(itemId);
	}
}
