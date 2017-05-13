package ru.l2gw.gameserver.model.playerSubOrders;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.ZoneManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2ShortCut;
import ru.l2gw.gameserver.model.entity.olympiad.Olympiad;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.ExGetBookMarkInfo;
import ru.l2gw.gameserver.serverpackets.ShortCutInit;
import ru.l2gw.gameserver.serverpackets.ShortCutRegister;
import ru.l2gw.util.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static ru.l2gw.gameserver.model.zone.L2Zone.ZoneType.*;

/**
 * @author rage
 * @date 23.06.2010 11:11:14
 */
public class TeleportBook
{
	private final GArray<TeleportBookmark> _bookmarks = new GArray<TeleportBookmark>(3);
	private int _maxSlots = 0;
	private L2Player _owner;
	private final static int TELEPORT_FLAG_ID = 20033;
	public final static int TELEPORT_SCROLL_ID = 13016;

	public TeleportBook(L2Player player)
	{
		_owner = player;
		_maxSlots = player.getVar("tpb_slots") == null ? 0 : Integer.parseInt(player.getVar("tpb_slots"));
	}

	public int getMaxSlots()
	{
		return _maxSlots;
	}

	public void setMaxSlots(int slots)
	{
		_maxSlots = slots;
		_owner.setVar("tpb_slots", String.valueOf(slots));
	}

	public TeleportBookmark[] getBookmarks()
	{
		return _bookmarks.toArray(new TeleportBookmark[_bookmarks.size()]);
	}

	public void addBookmark(String name, String acronym, int icon)
	{
	 	if(!checkUseCondition(_owner))
			 return;

		if(_bookmarks.size() >= _maxSlots)
		{
			_owner.sendPacket(Msg.YOU_HAVE_NO_SPACE_TO_SAVE_THE_TELEPORT_LOCATION);
			return;
		}

		if(_owner.getItemCountByItemId(TELEPORT_FLAG_ID) < 1)
		{
			_owner.sendPacket(Msg.YOU_CANNOT_BOOKMARK_THIS_LOCATION_BECAUSE_YOU_DO_NOT_HAVE_A_MY_TELEPORT_FLAG);
			return;
		}

		int slot;
		for(slot = 1; slot < Config.ALT_TELEPORT_BOOK_SIZE + 1; slot++)
		{
			boolean used = false;
			for(TeleportBookmark tpb : _bookmarks)
				if(slot == tpb.getSlot())
				{
					used = true;
					break;
				}

			if(!used)
				break;
		}

		if(_owner.destroyItemByItemId("Consume", TELEPORT_FLAG_ID, 1, null, true))
		{
			TeleportBookmark tpb = new TeleportBookmark(slot, icon, name, acronym, _owner.getLoc());
			_bookmarks.add(tpb);
			storeBookmark(_owner, tpb);
			_owner.sendPacket(new ExGetBookMarkInfo(_owner));
		}
	}

	public void modifyBookmark(int slot, String name, String acronym, int icon)
	{
		TeleportBookmark tpb = null;
		for(TeleportBookmark tb : _bookmarks)
			if(tb.getSlot() == slot)
			{
				tpb = tb;
				break;
			}

		if(tpb != null)
		{
			tpb.setName(name);
			tpb.setAcronym(acronym);
			tpb.setIcon(icon);
			storeBookmark(_owner, tpb);
			_owner.sendPacket(new ExGetBookMarkInfo(_owner));

			for(L2ShortCut sc : _owner.getAllShortCuts())
				if(sc.type == L2ShortCut.TYPE_MYTELEPORT && sc.id == tpb.getSlot())
				{
					L2ShortCut newsc = new L2ShortCut(sc.slot, sc.page, sc.type, sc.id, -1);
					_owner.sendPacket(new ShortCutRegister(newsc));
					_owner.registerShortCut(newsc);
				}
		}
	}

	public void deleteBookmark(int slot)
	{
		TeleportBookmark tpb = null;
		for(TeleportBookmark tb : _bookmarks)
			if(tb.getSlot() == slot)
			{
				tpb = tb;
				break;
			}

		if(tpb != null)
		{
			_bookmarks.remove(tpb);
			deleteBookmark(_owner, tpb);
			_owner.sendPacket(new ExGetBookMarkInfo(_owner));

			boolean update = false;
			for(L2ShortCut sc : _owner.getAllShortCuts())
				if(sc.type == L2ShortCut.TYPE_MYTELEPORT && sc.id == tpb.getSlot())
				{
					_owner.deleteShortCut(sc.slot, sc.page);
					update = true;
				}
			if(update)
				_owner.sendPacket(new ShortCutInit(_owner));
		}
	}

	public void teleportToBookmark(int slot)
	{
		TeleportBookmark tpb = null;
		for(TeleportBookmark tb : _bookmarks)
			if(tb.getSlot() == slot)
			{
				tpb = tb;
				break;
			}

		if(tpb != null && checkUseCondition(_owner))
		{
			Location loc = tpb.getLoc();
			GArray<L2Zone> zones = ZoneManager.getInstance().getZones(loc.getX(), loc.getY(), loc.getZ());
			if(zones != null)
				for(L2Zone zone : zones)
					if(zone.getTypes().contains(no_summon) || zone.getTypes().contains(instance)
							|| zone.getTypes().contains(no_restart) || zone.getTypes().contains(siege) && zone.isActive(_owner.getReflection())
							|| zone.getTypes().contains(ssq) || zone.getTypes().contains(no_escape))
					{
						_owner.sendPacket(Msg.YOU_CANNOT_USE_MY_TELEPORTS_TO_REACH_THIS_AREA);
						return;
					}

			if(_owner.getItemCountByItemId(TELEPORT_SCROLL_ID) < 1)
			{
				_owner.sendPacket(Msg.YOU_CANNOT_TELEPORT_BECAUSE_YOU_DO_NOT_HAVE_A_TELEPORT_ITEM);
				return;
			}

			if(_owner.destroyItemByItemId("Consume", TELEPORT_SCROLL_ID, 1, null, true))
				_owner.teleToLocation(loc);
		}
	}

	private static void deleteBookmark(L2Player player, TeleportBookmark tpb)
	{
		Connection con = null;
		PreparedStatement stmt = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("DELETE FROM `character_tpbookmark` WHERE `char_obj_id` = ? and `slot` = ?");
			stmt.setInt(1, player.getObjectId());
			stmt.setInt(2, tpb.getSlot());
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

	private static void storeBookmark(L2Player player, TeleportBookmark tpb)
	{
		Connection con = null;
		PreparedStatement stmt = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("REPLACE INTO `character_tpbookmark`(`char_obj_id`, `slot`, `name`, `acronym`, `icon`, `x`, `y`, `z`) VALUES(?,?,?,?,?,?,?,?)");
			stmt.setInt(1, player.getObjectId());
			stmt.setInt(2, tpb.getSlot());
			stmt.setString(3, tpb.getName());
			stmt.setString(4, tpb.getAcronym());
			stmt.setInt(5, tpb.getIcon());
			stmt.setInt(6, tpb.getLoc().getX());
			stmt.setInt(7, tpb.getLoc().getY());
			stmt.setInt(8, tpb.getLoc().getZ());
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

	public static boolean checkUseCondition(L2Player player)
	{
		if(player.isInZoneBattle() || player.isInCombat())
		{
			player.sendPacket(Msg.YOU_CANNOT_USE_MY_TELEPORTS_DURING_A_BATTLE);
			return false;
		}
		if(player.getSiegeId() > 0)
		{
			player.sendPacket(Msg.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_PARTICIPATING_A_LARGE_SCALE_BATTLE_SUCH_AS_A_CASTLE_SIEGE);
			return false;
		}
		if(player.isInDuel())
		{
			player.sendPacket(Msg.YOU_CANNOT_USE_MY_TELEPORTS_DURING_A_DUEL);
			return false;
		}
		if(player.isFlying())
		{
			player.sendPacket(Msg.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_FLYING);
			return false;
		}
		if(player.isInOlympiadMode() || Olympiad.isRegisteredInComp(player))
		{
			player.sendPacket(Msg.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_PARTICIPATING_IN_AN_OLYMPIAD_MATCH);
			return false;
		}
		if(player.isActionsBlocked() || player.isSleeping() || player.isStunned() || player.isParalyzed() || player.isImobilised())
		{
			player.sendPacket(Msg.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_YOU_ARE_IN_A_FLINT_OR_PARALYZED_STATE);
			return false;
		}
		if(player.isAlikeDead())
		{
			player.sendPacket(Msg.YOU_CANNOT_USE_MY_TELEPORTS_WHILE_YOU_ARE_DEAD);
			return false;
		}
		if(player.isInSiege() || player.isInZone(no_restart) || player.isInZone(no_escape) || player.isInZone(no_summon) || player.isInBoat() || player.isInZone(ssq) || player.inObserverMode() || player.getReflection() > 0)
		{
			player.sendPacket(Msg.YOU_CANNOT_USE_MY_TELEPORTS_IN_THIS_AREA);
			return false;
		}
		if(player.isSwimming())
		{
			player.sendPacket(Msg.YOU_CANNOT_USE_MY_TELEPORTS_UNDERWATER);
			return false;
		}
		if(player.isInZone(instance))
		{
			player.sendPacket(Msg.YOU_CANNOT_USE_MY_TELEPORTS_IN_AN_INSTANT_ZONE);
			return false;
		}

		return true;
	}

	public static TeleportBook restore(L2Player player)
	{
		TeleportBook tpbl = new TeleportBook(player);

		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("SELECT * FROM `character_tpbookmark` WHERE `char_obj_id` = ? ORDER BY `slot`");
			stmt.setInt(1, player.getObjectId());
			rs = stmt.executeQuery();

			while(rs.next())
				tpbl._bookmarks.add(new TeleportBookmark(rs.getInt("slot"), rs.getInt("icon"), rs.getString("name"), rs.getString("acronym"), new Location(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"))));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, stmt, rs);
		}

		return tpbl;
	}

}
