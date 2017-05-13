package ru.l2gw.gameserver.model.playerSubOrders;

import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.FriendListForPostBox;
import ru.l2gw.gameserver.serverpackets.L2FriendList;
import ru.l2gw.gameserver.serverpackets.L2FriendStatus;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.util.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

/**
 * @author rage
 * @date 20.05.2010 10:09:37
 */
public class FriendList
{
	private final FastMap<Integer, String> _friendList;
	private final L2Player _owner;
	private static Log _log = LogFactory.getLog(FriendList.class);

	private FriendList(L2Player owner)
	{
		_owner = owner;
		_friendList = new FastMap<Integer, String>();
	}

	public static FriendList restore(L2Player owner)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		FriendList fl = new FriendList(owner);

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT friend_id, friend_name FROM character_friends WHERE char_id=?");
			statement.setInt(1, owner.getObjectId());
			rset = statement.executeQuery();

			while(rset.next())
				fl._friendList.put(rset.getInt("friend_id"), rset.getString("friend_name"));
		}
		catch(Exception e)
		{
			_log.warn("Error in friendlist ", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		return fl;
	}

	public void sendFriendList()
	{
		_owner.sendPacket(new L2FriendList(_owner));
	}

	public void sendFriendListForPostBox()
	{
		GArray<StatsSet> friends = new GArray<StatsSet>(_friendList.size());

		for(int objectId : _friendList.keySet())
		{
			L2Player player = L2ObjectsStorage.getPlayer(objectId);
			StatsSet friend = new StatsSet();
			friend.set("objectId", objectId);
			friend.set("name", _friendList.get(objectId));
			if(player != null)
			{
				friend.set("online", 1);
				friend.set("classId", player.getActiveClass());
				friend.set("level", player.getLevel());
			}
			else
			{
				friend.set("online", 0);
				int[] res = Util.getCharLevelAndClassById(objectId);
				friend.set("classId", res[1]);
				friend.set("level", res[0]);
			}
			friends.add(friend);
		}

		_owner.sendPacket(new FriendListForPostBox(friends));
	}

	public void notifyFriends(boolean login)
	{
		for(int objectId : _friendList.keySet())
		{
			L2Player friend = L2ObjectsStorage.getPlayer(objectId);
			if(friend != null)
			{
				if(login)
					friend.sendPacket(new SystemMessage(SystemMessage.S1_FRIEND_HAS_LOGGED_IN).addString(_owner.getName()));
				friend.sendPacket(new L2FriendStatus(_owner, login));
			}
		}
	}

	public void addFriend(L2Player friend)
	{
		_friendList.put(friend.getObjectId(), friend.getName());
		addFriend(_owner, friend);
	}

	private static void addFriend(L2Player owner, L2Player friend)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO character_friends (char_id,friend_id,friend_name) VALUES(?,?,?)");
			statement.setInt(1, owner.getObjectId());
			statement.setInt(2, friend.getObjectId());
			statement.setString(3, friend.getName());
			statement.execute();
			DbUtils.closeQuietly(statement);
		}
		catch(Exception e)
		{
			_log.warn(owner.getFriendList() + " could not add friend objectid: " + friend.getObjectId());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public int removeFriend(String[] name)
	{
		if(name == null)
			return 0;

		int objectId = 0;
		for(Integer objId : _friendList.keySet())
			if(name[0].equalsIgnoreCase(_friendList.get(objId)))
			{
				objectId = objId;
				break;
			}

		if(objectId > 0)
		{
			name[0] = _friendList.remove(objectId);
			removeFriend(_owner.getObjectId(), objectId);
			return objectId;
		}

		return 0;
	}

	private static void removeFriend(int ownerId, int friendId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_friends WHERE (char_id=? AND friend_id=?) OR (char_id=? AND friend_id=?)");
			statement.setInt(1, ownerId);
			statement.setInt(2, friendId);
			statement.setInt(3, friendId);
			statement.setInt(4, ownerId);
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warn("FriendList: could not delete friend objectId: " + friendId + " ownerId: " + ownerId);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public Map<Integer, String> getList()
	{
		return _friendList;
	}

	@Override
	public String toString()
	{
		return "FreindList[owner=" + _owner.getName() + "]";
	}

}
