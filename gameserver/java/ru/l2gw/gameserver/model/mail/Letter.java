package ru.l2gw.gameserver.model.mail;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author rage
 * @date 17.06.2010 15:51:58
 */
public class Letter
{
	public int message_id, system, unread, attach_id, expire, senderId, receiverId, returned;
	public String senderName, receiverName, subject, message;
	public long price;

	public Letter clone()
	{
		Letter letter = new Letter();
		letter.message_id = message_id;
		letter.system = system;
		letter.unread = unread;
		letter.attach_id = attach_id;
		letter.expire = expire;
		letter.senderId = senderId;
		letter.receiverId = receiverId;
		letter.senderName = senderName;
		letter.receiverName = receiverName;
		letter.subject = subject;
		letter.message = message;
		letter.price = price;
		letter.returned = returned;
		return letter;
	}

	public static Letter restore(ResultSet rs) throws SQLException
	{
		Letter letter = new Letter();
		letter.message_id = rs.getInt("message_id");
		letter.senderId = rs.getInt("src_obj_id");
		letter.receiverId = rs.getInt("dst_obj_id");
		letter.senderName = letter.senderId == 0 ? "System" : rs.getString("sender");
		letter.receiverName = rs.getString("receiver");
		letter.expire = rs.getInt("expire");
		letter.subject = rs.getString("subject");
		letter.message = rs.getString("message");
		letter.price = rs.getLong("price");
		letter.system = rs.getInt("system");
		letter.unread = rs.getInt("unread");
		letter.returned = rs.getInt("returned");
		letter.attach_id = rs.getInt("attach_id");
		return letter;
	}

	@Override
	public String toString()
	{
		return "Letter[" + message_id + ";from=" + senderName + "(" + senderId + ");to=" + receiverName + "(" + receiverId + ");att=" + attach_id + "]";
	}
}
