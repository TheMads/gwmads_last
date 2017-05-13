package ru.l2gw.gameserver.model.playerSubOrders;

import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.ExConfirmAddingPostFriend;
import ru.l2gw.gameserver.serverpackets.ExReceiveShowPostFriend;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.util.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author rage
 * @date 03.02.11 12:43
 */
public class ContactList
{
	private final FastMap<Integer, String> _contactList;
	private final L2Player _owner;
	private static Log _log = LogFactory.getLog(ContactList.class);

	private ContactList(L2Player owner)
	{
		_contactList = new FastMap<Integer, String>().shared();
		_owner = owner;
	}

	public void addContact(String name)
	{
		if(name == null)
			return;

		if(_contactList.size() >= 100)
		{
			_owner.sendPacket(new ExConfirmAddingPostFriend(name, -3));
			return;
		}

		L2Player player = L2ObjectsStorage.getPlayer(name);
		int targetId;
		if(player == null)
			targetId = Util.getCharIdByNameAndName(new String[]{name});
		else
			targetId = player.getObjectId();

		if(targetId == 0)
		{
			_owner.sendPacket(new ExConfirmAddingPostFriend(name, -2));
			return;
		}

		if(_contactList.containsKey(targetId))
		{
			_owner.sendPacket(new ExConfirmAddingPostFriend(name, -4));
			return;
		}

		_contactList.put(targetId, name);
		_owner.sendPacket(new SystemMessage(SystemMessage.S1_WAS_SUCCESSFULLY_ADDED_TO_YOUR_CONTACT_LIST).addString(name));
		_owner.sendPacket(new ExConfirmAddingPostFriend(name, 1));
		saveContact(_owner, name, targetId);
	}

	public void sendContactList()
	{
		_owner.sendPacket(new ExReceiveShowPostFriend(_contactList.values()));
	}

	public static ContactList restore(L2Player owner)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		ContactList cl = new ContactList(owner);

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT contact_id, contact_name FROM character_contactlist WHERE char_id=?");
			statement.setInt(1, owner.getObjectId());
			rset = statement.executeQuery();

			while(rset.next())
				cl._contactList.put(rset.getInt("contact_id"), rset.getString("contact_name"));
		}
		catch(Exception e)
		{
			_log.warn("Error in contact list ", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		return cl;
	}

	private static void saveContact(L2Player owner, String contact_name, int contact_id)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO character_contactlist (char_id, contact_id, contact_name) VALUES(?,?,?)");
			statement.setInt(1, owner.getObjectId());
			statement.setInt(2, contact_id);
			statement.setString(3, contact_name);
			statement.execute();
			DbUtils.closeQuietly(statement);
		}
		catch(Exception e)
		{
			_log.warn(owner + " could not add contact: " + contact_name + " " + contact_id);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void removeContact(String name)
	{
		if(name == null)
			return;

		int objectId = 0;
		for(Integer objId : _contactList.keySet())
			if(name.equalsIgnoreCase(_contactList.get(objId)))
			{
				objectId = objId;
				break;
			}

		if(objectId > 0)
		{
			_contactList.remove(objectId);
			removeFriend(_owner.getObjectId(), objectId);
			_owner.sendPacket(new SystemMessage(SystemMessage.S1_WAS_SUCCESSFULLY_DELETED_FROM_YOUR_CONTACT_LIST).addString(name));
		}
		else
			_owner.sendPacket(new ExConfirmAddingPostFriend(name, -5));
	}

	private static void removeFriend(int ownerId, int contactId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_contactlist WHERE char_id = ? AND contact_id=?");
			statement.setInt(1, ownerId);
			statement.setInt(2, contactId);
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warn("ContactList: could not delete contact objectId: " + contactId + " ownerId: " + ownerId);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
}
