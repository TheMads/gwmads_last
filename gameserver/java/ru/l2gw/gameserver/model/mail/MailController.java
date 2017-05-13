package ru.l2gw.gameserver.model.mail;

import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.arrays.GCSArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.ExNoticePostArrived;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ItemTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author rage
 * @date 17.06.2010 15:07:20
 */
public class MailController
{
	private static final Log _log = LogFactory.getLog("mail");
	private static ReentrantLock lock = new ReentrantLock();
	private static final ReentrantLock sendLock = new ReentrantLock();

	private final FastMap<Integer, GArray<Letter>> _receivedMail;
	private final FastMap<Integer, GArray<Letter>> _sendMail;
	private final FastMap<Integer, LetterAttach> _attachments;
	private final GCSArray<Letter> _allLetters;
	private static MailController _instance;
	private static final GArray<Letter> _emptyLetterList = new GArray<Letter>(0);
	private final LetterComparator<Letter> _letterComparator;
	private ScheduledFuture<CleanLetters> _cleanTask;

	public static MailController getInstance()
	{
		if(_instance == null)
			_instance = new MailController();
		return _instance;
	}

	private MailController()
	{
		_receivedMail = new FastMap<Integer, GArray<Letter>>().shared();
		_sendMail = new FastMap<Integer, GArray<Letter>>().shared();
		_attachments = new FastMap<Integer, LetterAttach>().shared();
		_allLetters = new GCSArray<Letter>();
		_letterComparator = new LetterComparator<Letter>();
		cleanup();
		restore();
		_cleanTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new CleanLetters(), 1000, 600000);
		_log.info("MailController: restored " + _allLetters.size() + " letters and " + _attachments.size() + " attachments.");
	}

	public void stopCleanTask()
	{
		if(_cleanTask != null)
			_cleanTask.cancel(true);
		_cleanTask = null;
	}

	public GArray<Letter> getReceivedMailList(int objectId)
	{
		GArray<Letter> list = _receivedMail.get(objectId);
		if(list == null)
			return _emptyLetterList;

		Letter[] array = new Letter[list.size()];
		list.toArray(array);
		Arrays.sort(array, _letterComparator);
		GArray<Letter> sorted = new GArray<Letter>(array.length);
		sorted.addAll(Arrays.asList(array));
		return sorted;
	}

	public GArray<Letter> getSendMailList(int objectId)
	{
		GArray<Letter> list = _sendMail.get(objectId);
		if(list == null)
			return _emptyLetterList;

		Letter[] array = new Letter[list.size()];
		list.toArray(array);
		Arrays.sort(array, _letterComparator);
		GArray<Letter> sorted = new GArray<Letter>(array.length);
		sorted.addAll(Arrays.asList(array));
		return sorted;
	}

	public int getReceivedCount(int objectId)
	{
		GArray<Letter> list = _receivedMail.get(objectId);
		if(list == null)
			return 0;

		return list.size();
	}

	public int getSendCount(int objectId)
	{
		GArray<Letter> list = _sendMail.get(objectId);
		if(list == null)
			return 0;

		return list.size();
	}

	public void sendMail(Letter letter, LetterAttach attach)
	{
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			sendLock.lock();
			con = DatabaseFactory.getInstance().getConnection();
			int attachId = 0;
			if(attach != null)
			{
				stmt = con.prepareStatement("INSERT INTO `character_mail_attach` (`item_obj_id`) VALUES (?)");
				stmt.setInt(1, attach.getItems().get(0).getObjectId());
				stmt.executeUpdate();

				DbUtils.closeQuietly(stmt);
				stmt = con.prepareStatement("SELECT LAST_INSERT_ID()");
				rs = stmt.executeQuery();

				if(rs.next())
					attachId = rs.getInt(1);

				attach.attach_id = attachId;

				if(attach.getItems().size() > 1)
				{
					DbUtils.closeQuietly(stmt);
					stmt = con.prepareStatement("INSERT INTO `character_mail_attach` (`attach_id`, `item_obj_id`) VALUES (?, ?)");
					for(int i = 1; i < attach.getItems().size(); i ++)
					{
						stmt.setInt(1, attachId);
						stmt.setInt(2, attach.getItems().get(i).getObjectId());
						stmt.execute();
					}
				}
				DbUtils.closeQuietly(stmt);
				_attachments.put(attachId, attach);
			}

			letter.attach_id = attachId;
			insertLetter(letter);
			logMail(letter, "send");

			if(letter.system == 0)
			{
				Letter sendLetter = letter.clone();
				sendLetter.unread = 2;
				insertLetter(sendLetter);
				addToCache(sendLetter);
			}

			addToCache(letter);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			sendLock.unlock();
			DbUtils.closeQuietly(con, stmt, rs);
		}
	}

	private void insertLetter(Letter letter) throws SQLException
	{
		Connection con;
		PreparedStatement stmt;
		ResultSet rs;
		con = DatabaseFactory.getInstance().getConnection();

		stmt = con.prepareStatement("INSERT INTO `character_mail` (`src_obj_id`, `dst_obj_id`, `expire`, `subject`, `message`, `price`, `system`, `unread`, `attach_id`, `returned`) VALUES (?,?,?,?,?,?,?,?,?,?)");
		stmt.setInt(1, letter.senderId);
		stmt.setInt(2, letter.receiverId);
		stmt.setInt(3, letter.expire);
		stmt.setString(4, letter.subject);
		stmt.setString(5, letter.message);
		stmt.setLong(6, letter.price);
		stmt.setInt(7, letter.system);
		stmt.setInt(8, letter.unread);
		stmt.setInt(9, letter.attach_id);
		stmt.setInt(10, letter.returned);
		stmt.executeUpdate();

		DbUtils.closeQuietly(stmt);
		stmt = con.prepareStatement("SELECT LAST_INSERT_ID()");
		rs = stmt.executeQuery();

		if(rs.next())
			letter.message_id = rs.getInt(1);

		DbUtils.closeQuietly(con, stmt, rs);
	}

	private void addToCache(Letter letter)
	{
		if(letter.unread == 2)
		{
			GArray<Letter> list = _sendMail.get(letter.senderId);
			if(list == null)
			{
				list = new GArray<Letter>(1);
				_sendMail.put(letter.senderId, list);
			}

			list.add(letter);
		}
		else
		{
			GArray<Letter> list = _receivedMail.get(letter.receiverId);
			if(list == null)
			{
				list = new GArray<Letter>(1);
				_receivedMail.put(letter.receiverId, list);
			}

			list.add(letter);
		}

		_allLetters.add(letter);
	}

	public Letter getReceivedLetter(int objectId, int messageId)
	{
		Letter letter = getLetterR(objectId, messageId);
		if(letter != null)
		{
			if(letter.unread == 1)
			{
				letter.unread = 0;
				markReadLetter(letter.message_id);
			}
			return letter;
		}

		return null;
	}

	public Letter getSendLetter(int objectId, int messageId)
	{
		return getLetterS(objectId, messageId);
	}

	private void markReadLetter(int messageId)
	{
		Connection con = null;
		PreparedStatement stmt = null;
		try
		{
			lock.lock();
			con = DatabaseFactory.getInstance().getConnection();

			stmt = con.prepareStatement("UPDATE `character_mail` SET `unread` = 0 WHERE `message_id` = ?");
			stmt.setInt(1, messageId);
			stmt.executeUpdate();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			lock.unlock();
			DbUtils.closeQuietly(con, stmt);
		}
	}

	public LetterAttach getAttach(int attachId)
	{
		return _attachments.get(attachId);
	}

	public void getReceiveAttach(L2Player player, int messageId)
	{
		Letter letter = getLetterR(player.getObjectId(), messageId);
		if(letter == null || letter.attach_id == 0)
			return;

		try
		{
			lock.lock();
			LetterAttach attach = getAttach(letter.attach_id);

			if(attach == null)
			{
				_log.warn("MailController: attach is null for: " + letter);
				return;
			}

			if(letter.returned == 0 && player.getAdena() < letter.price)
			{
				player.sendPacket(Msg.YOU_CANNOT_RECEIVE_BECAUSE_YOU_DON_T_HAVE_ENOUGH_ADENA);
				return;
			}

			if(!player.getInventory().validateCapacity(attach.getItems()))
			{
				player.sendPacket(Msg.YOU_COULD_NOT_RECEIVE_BECAUSE_YOUR_INVENTORY_IS_FULL);
				return;
			}

			for(L2ItemInstance item : attach.getItems())
				if(item.getLocation() != L2ItemInstance.ItemLocation.MAILBOX || (letter.senderId != 1 && !item.canBeTraded(player)))
				{
					_log.warn("MailController: " + item + " not tradable or wrong loc: " + item.getLocation() + " " + letter);
					return;
				}

			if(letter.returned == 0 && letter.price > 0 && !transferAdena(player, letter.senderId, letter.price, letter.senderName))
				return;

			for(L2ItemInstance item : attach.getItems())
				player.addItem("MailReceive", item, null, true);

			letter.attach_id = 0;
			storeLetter(letter);
			deleteAttach(attach);
			if(letter.message == null || letter.message.isEmpty())
				deleteLetter(letter);

			player.sendPacket(Msg.MAIL_SUCCESSFULLY_RECEIVED);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			lock.unlock();
		}
	}

	private Letter getLetterR(int objectId, int messageId)
	{
		for(Letter letter : _allLetters)
			if(letter.message_id == messageId && letter.receiverId == objectId)
				return letter;

		return null;
	}

	private Letter getLetterS(int objectId, int messageId)
	{
		for(Letter letter : _allLetters)
			if(letter.message_id == messageId && letter.senderId == objectId)
				return letter;

		return null;
	}

	private void storeLetter(Letter letter) throws SQLException
	{
		Connection con;
		PreparedStatement stmt;
		con = DatabaseFactory.getInstance().getConnection();
		stmt = con.prepareStatement("REPLACE INTO `character_mail` (`message_id`, `src_obj_id`, `dst_obj_id`, `expire`, `subject`, `message`, `price`, `system`, `unread`, `attach_id`, `returned`) VALUES (?,?,?,?,?,?,?,?,?,?,?)");
		stmt.setInt(1, letter.message_id);
		stmt.setInt(2, letter.senderId);
		stmt.setInt(3, letter.receiverId);
		stmt.setInt(4, letter.expire);
		stmt.setString(5, letter.subject);
		stmt.setString(6, letter.message);
		stmt.setLong(7, letter.price);
		stmt.setInt(8, letter.system);
		stmt.setInt(9, letter.unread);
		stmt.setInt(10, letter.attach_id);
		stmt.setInt(11, letter.returned);
		stmt.executeUpdate();
		DbUtils.closeQuietly(con, stmt);
	}

	private void deleteAttach(LetterAttach attach) throws SQLException
	{
		_attachments.remove(attach.attach_id);
		for(Letter letter : _allLetters)
			if(letter.attach_id == attach.attach_id)
			{
				letter.attach_id = 0;
				storeLetter(letter);
				if(letter.message == null || letter.message.isEmpty())
					deleteLetter(letter);
			}

		Connection con;
		PreparedStatement stmt;
		con = DatabaseFactory.getInstance().getConnection();
		stmt = con.prepareStatement("DELETE FROM `character_mail_attach` WHERE `attach_id` = ?");
		stmt.setInt(1, attach.attach_id);
		stmt.execute();
		DbUtils.closeQuietly(con, stmt);
	}

	private boolean transferAdena(L2Player player, int objectId, long count, String name)
	{
		if(!player.reduceAdena("MailPay", count, null, true))
			return false;

		L2Player sender = L2ObjectsStorage.getPlayer(objectId);

		if(sender != null)
			sender.addAdena("MailSell", count, player, true);
		else
		{
			L2ItemInstance adena = L2ItemInstance.restoreFromDb(objectId, 57, L2ItemInstance.ItemLocation.INVENTORY);
			if(adena != null)
			{
				adena.changeCount("MailSell", count, "player '" + name + "'", player);
				adena.updateDatabase(true);
			}
			else
			{
				adena = ItemTable.getInstance().createItem("MailSell", 57, count, "player '" + name + "'");
				adena.setOwnerId(objectId);
				adena.setLocation(L2ItemInstance.ItemLocation.INVENTORY);
				adena.updateDatabase(true);
			}
		}
		return true;
	}

	public void deleteReceivedLetters(L2Player player, int... mailIds)
	{
		Connection con = null;
		PreparedStatement stmt = null;
		try
		{
			lock.lock();
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("DELETE FROM `character_mail` WHERE message_id=?");
			GArray<Letter> letters = _receivedMail.get(player.getObjectId());
			if(letters == null)
				return;
			for(int messageId : mailIds)
			{
				Letter letter = getLetterR(player.getObjectId(), messageId);
				if(letter == null)
					continue;

				letters.remove(letter);
				_allLetters.remove(letter);

				stmt.setInt(1, messageId);
				stmt.executeUpdate();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, stmt);
			lock.unlock();
		}
	}

	public void deleteSendLetters(L2Player player, GArray<Integer> list)
	{
		Connection con = null;
		PreparedStatement stmnt = null;
		try
		{
			lock.lock();
			con = DatabaseFactory.getInstance().getConnection();
			stmnt = con.prepareStatement("DELETE FROM `character_mail` WHERE message_id=?");
			GArray<Letter> letters = _sendMail.get(player.getObjectId());
			if(letters == null)
				return;

			for(int i = 0; i < list.size(); i++)
			{
				Letter letter = getLetterS(player.getObjectId(), list.get(i));
				if(letter == null || letter.attach_id > 0)
				{
					list.remove(i);
					continue;
				}

				letters.remove(letter);
				_allLetters.remove(letter);

				stmnt.setInt(1, list.get(i));
				stmnt.executeUpdate();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, stmnt);
			lock.unlock();
		}
	}

	private void restore()
	{
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rs;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("SELECT\n" +
					"  cm.*,\n" +
					"  cs.char_name as sender,\n" +
					"  cr.char_name as receiver\n" +
					"FROM\n" +
					"  character_mail cm \n" +
					"    LEFT OUTER JOIN characters cs on (cm.src_obj_id = cs.obj_id)\n" +
					"    INNER JOIN characters cr on (cm.dst_obj_id = cr.obj_id)\n" +
					"ORDER BY cm.`expire`");

			rs = stmt.executeQuery();

			while(rs.next())
				addToCache(Letter.restore(rs));

			DbUtils.closeQuietly(stmt, rs);

			stmt = con.prepareStatement("SELECT * FROM `character_mail_attach` ORDER BY `attach_id`");
			rs = stmt.executeQuery();

			while(rs.next())
			{
				int attachId = rs.getInt("attach_id");
				LetterAttach attach = _attachments.get(attachId);
				if(attach == null)
				{
					attach = new LetterAttach();
					attach.attach_id = attachId;
					_attachments.put(attachId, attach);
				}
				L2ItemInstance item = L2ItemInstance.restoreFromDb(rs.getInt("item_obj_id"));
				if(item != null)
					attach.addItem(item);
			}
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

	public boolean cancelLetter(L2Player player, int messageId)
	{
		Letter letter = getLetterS(player.getObjectId(), messageId);
		if(letter == null)
			return false;

		LetterAttach attach;
		if(letter.attach_id == 0 || (attach = _attachments.get(letter.attach_id)) == null)
		{
			player.sendPacket(Msg.YOU_CANNOT_CANCEL_SENT_MAIL_SINCE_THE_RECIPIENT_RECEIVED_IT);
			return false;
		}

		if(!player.getInventory().validateCapacity(attach.getItems()))
		{
			player.sendPacket(Msg.YOU_COULD_NOT_CANCEL_RECEIPT_BECAUSE_YOUR_INVENTORY_IS_FULL);
			return false;
		}

		Connection con = null;
		PreparedStatement stmt = null;
		try
		{
			lock.lock();
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("DELETE FROM `character_mail` WHERE message_id=?");
			_allLetters.remove(letter);
			_sendMail.get(player.getObjectId()).remove(letter);
			GArray<Letter> list = _receivedMail.get(letter.receiverId);
			if(list != null)
				for(int i = 0; i < list.size(); i++)
				{
					Letter rec = list.get(i);
					if(rec.attach_id == letter.attach_id)
					{
						_allLetters.remove(rec);
						list.remove(rec);
						stmt.setInt(1, rec.message_id);
						stmt.executeUpdate();
						break;
					}
				}

			stmt.setInt(1, letter.message_id);
			stmt.executeUpdate();

			for(L2ItemInstance item : attach.getItems())
				player.addItem("MailCancel", item, null, false);

			deleteAttach(attach);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, stmt);
			lock.unlock();
		}

		return true;
	}

	public boolean returnLetter(int objectId, int messageId, boolean auto)
	{
		Connection con = null;
		PreparedStatement stmnt = null;
		try
		{
			lock.lock();
			Letter letter = getLetterR(objectId, messageId);
			if(letter == null)
				return false;

			if(letter.attach_id == 0 || _attachments.get(letter.attach_id) == null)
				return false;

			Letter retLetter = letter.clone();
			retLetter.expire = (int) (System.currentTimeMillis() / 1000) + 15 * 24 * 60 * 60;
			retLetter.unread = 1;
			retLetter.returned = 1;
			retLetter.system = 1;
			retLetter.senderId = 0;
			retLetter.senderName = "System";
			retLetter.receiverId = letter.senderId;
			retLetter.receiverName = letter.senderName;

			con = DatabaseFactory.getInstance().getConnection();
			stmnt = con.prepareStatement("DELETE FROM `character_mail` WHERE message_id=?");
			GArray<Letter> letters = _receivedMail.get(objectId);
			if(letters == null)
				return false;
			letters.remove(letter);
			_allLetters.remove(letter);
			stmnt.setInt(1, letter.message_id);
			stmnt.executeUpdate();

			letters = _sendMail.get(letter.senderId);
			if(letters == null)
				return false;

			for(int i = 0; i < letters.size(); i++)
			{
				Letter sendLetter = letters.get(i);
				if(sendLetter.attach_id == letter.attach_id)
				{
					letters.remove(i);
					_allLetters.remove(sendLetter);
					stmnt.setInt(1, sendLetter.message_id);
					stmnt.executeUpdate();
					break;
				}
			}
			insertLetter(retLetter);
			logMail(retLetter, "return");
			_allLetters.add(retLetter);
			letters = _receivedMail.get(letter.senderId);
			if(letters == null)
			{
				letters = new GArray<>();
				_receivedMail.put(letter.senderId, letters);
			}
			letters.add(retLetter);

			L2Player sender = L2ObjectsStorage.getPlayer(letter.senderId);
			if(sender != null)
			{
				if(auto)
					sender.sendPacket(Msg.THE_MAIL_WAS_RETURNED_DUE_TO_THE_EXCEEDED_WAITING_TIME);
				else
					sender.sendPacket(new SystemMessage(SystemMessage.S1_RETURNED_THE_MAIL).addString(letter.receiverName));
				sender.sendPacket(new ExNoticePostArrived(1));
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, stmnt);
			lock.unlock();
		}

		return true;
	}

	public int getUnreadCount(int objectId)
	{
		GArray<Letter> list = _receivedMail.get(objectId);
		if(list == null)
			return 0;
		int count = 0;
		for(Letter letter : list)
			if(letter.unread == 1)
				count++;

		return count;
	}

	private class LetterComparator<T> implements Comparator<T>
	{
		@Override
		public int compare(Object o1, Object o2)
		{
			if(o1 instanceof Letter && o2 instanceof Letter)
				return ((Letter)o2).expire - ((Letter)o1).expire;
			return 0;
		}
	}

	private void cleanup()
	{
		Connection con = null;
		PreparedStatement stmt;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("DELETE FROM `character_mail` WHERE returned = 0 and src_obj_id NOT IN (SELECT obj_id FROM characters)");
			stmt.execute();
			DbUtils.closeQuietly(stmt);

			stmt = con.prepareStatement("DELETE FROM `character_mail` WHERE dst_obj_id NOT IN (SELECT obj_id FROM characters)");
			stmt.execute();
			DbUtils.closeQuietly(stmt);

			stmt = con.prepareStatement("DELETE FROM `character_mail_attach` WHERE `attach_id` NOT IN (SELECT `attach_id` FROM `character_mail`)");
			stmt.execute();
			DbUtils.closeQuietly(stmt);

			stmt = con.prepareStatement("DELETE FROM `character_mail_attach` WHERE `item_obj_id` NOT IN (SELECT `object_id` FROM `items` WHERE loc = 'MAILBOX')");
			stmt.execute();
			DbUtils.closeQuietly(stmt);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con);
		}
	}

	private void deleteLetter(Letter letter) throws SQLException
	{
		Connection con = null;
		PreparedStatement stmt = null;
		try
		{
			lock.lock();
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("DELETE FROM `character_mail` WHERE `message_id` = ?");
			stmt.setInt(1, letter.message_id);
			stmt.executeUpdate();

			_allLetters.remove(letter);
			if(letter.unread == 2)
			{
				GArray<Letter> list = _sendMail.get(letter.senderId);
				if(list != null)
					list.remove(letter);
			}
			else
			{
				GArray<Letter> list = _receivedMail.get(letter.receiverId);
				if(list != null)
					list.remove(letter);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, stmt);
			lock.unlock();
		}
	}

	private void deleteLetterAndAttach(Letter letter)
	{
		try
		{
			lock.lock();
			if(letter.attach_id > 0)
			{
				LetterAttach attach = _attachments.get(letter.attach_id);
				if(attach != null)
				{
					for(L2ItemInstance item : attach.getItems())
					{
						item.setOwnerId("MailExpired", 0, null, null);
						item.updateDatabase(true);
					}
					deleteAttach(attach);
				}
			}

			deleteLetter(letter);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			lock.unlock();
		}
	}

	private class CleanLetters implements Runnable
	{
		public void run()
		{
			try
			{
				for(int i = 0; i < _allLetters.size(); i++)
				{
					Letter letter = _allLetters.get(i);
					if(letter.expire < System.currentTimeMillis() / 1000)
					{
						if(letter.attach_id > 0)
						{
							if(letter.returned == 1)
								continue;
							if(letter.system == 3)
								deleteLetterAndAttach(letter);
							else
								returnLetter(letter.receiverId, letter.message_id, true);
						}
						else
							deleteLetter(letter);
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private static void logMail(Letter letter, String action)
	{
		String id = String.format("%06d: ", letter.message_id);
		_log.info(id + action + " from: " + letter.senderName + ":" + letter.senderId + " to: " + letter.receiverName + ":" + letter.receiverId);
		_log.info(id + action + " subject: " + letter.subject);
		if(letter.price > 0)
			_log.info(id + action + " price: " + letter.price);
		for(String str : letter.message.split("\n"))
			_log.info(id + "msg: " + str);
		if(letter.attach_id > 0)
		{
			LetterAttach la = MailController.getInstance().getAttach(letter.attach_id);
			for(L2ItemInstance item : la.getItems())
				_log.info(id + "attach: " + item);
		}
	}
}
