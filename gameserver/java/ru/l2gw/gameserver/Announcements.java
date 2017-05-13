package ru.l2gw.gameserver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.clientpackets.Say2C;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.serverpackets.Say2;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class Announcements
{
	private static Log _log = LogFactory.getLog(Announcements.class.getName());

	private static final Announcements _instance = new Announcements();
	private ArrayList<String> _announcements = new ArrayList<String>();
	private static final int _type = Say2C.ANNOUNCEMENT;

	public Announcements()
	{
		loadAnnouncements();
	}

	public static Announcements getInstance()
	{
		return _instance;
	}

	public void loadAnnouncements()
	{
		_announcements.clear();
		File file = new File("./", "config/announcements.txt");
		if(file.exists())
			readFromDisk(file);
		else
			_log.error("config/announcements.txt doesn't exist");
	}

	public void showAnnouncements(L2Player player)
	{
		for(String _announcement : _announcements)
			player.sendPacket(new Say2(0, _type, player.getName(), _announcement));
	}

	public void listAnnouncements(L2Player player)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

		StringBuffer replyMSG = new StringBuffer("<html><body>");
		replyMSG.append("<table width=260><tr>");
		replyMSG.append("<td width=40><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\"></td>");
		replyMSG.append("<td width=180><center>Announcement Menu</center></td>");
		replyMSG.append("<td width=40><button value=\"Back\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\"></td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<br><br>");
		replyMSG.append("<center>Add or announce a new announcement:</center>");
		replyMSG.append("<center><multiedit var=\"new_announcement\" width=240 height=30></center><br>");
		replyMSG.append("<center><table><tr><td>");
		replyMSG.append("<button value=\"Add\" action=\"bypass -h admin_add_announcement $new_announcement\" width=60 height=15 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\"></td><td>");
		replyMSG.append("<button value=\"Announce\" action=\"bypass -h admin_announce_menu $new_announcement\" width=60 height=15 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\"></td><td>");
		replyMSG.append("<button value=\"Reload\" action=\"bypass -h admin_announce_announcements\" width=60 height=15 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
		replyMSG.append("</td></tr></table></center>");
		replyMSG.append("<br>");
		for(int i = 0; i < _announcements.size(); i++)
		{
			replyMSG.append("<table width=260><tr><td width=220>" + _announcements.get(i) + "</td><td width=40>");
			replyMSG.append("<button value=\"Delete\" action=\"bypass -h admin_del_announcement " + i + "\" width=60 height=15 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\"></td></tr></table>");
		}
		replyMSG.append("</body></html>");

		adminReply.setHtml(replyMSG.toString());
		player.sendPacket(adminReply);
	}

	public void addAnnouncement(String text)
	{
		_announcements.add(text);
		saveToDisk();
	}

	public void delAnnouncement(int line)
	{
		_announcements.remove(line);
		saveToDisk();
	}

	private void readFromDisk(File file)
	{
		LineNumberReader lnr = null;
		try
		{
			String line;
			lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

			while((line = lnr.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(line, "\n\r");
				if(st.hasMoreTokens())
				{
					String announcement = st.nextToken();
					_announcements.add(announcement);

				}
			}
		}
		catch(IOException e1)
		{
			_log.error("Error reading announcements", e1);
		}
		finally
		{
			try
			{
				if(lnr != null)
					lnr.close();
			}
			catch(Exception e2)
			{
				// nothing
			}
		}
	}

	private void saveToDisk()
	{
		File file = new File("config/announcements.txt");
		FileWriter save = null;

		try
		{
			save = new FileWriter(file);
			for(String _announcement : _announcements)
			{
				save.write(_announcement);
				save.write("\r\n");
			}
		}
		catch(IOException e)
		{
			_log.warn("saving the announcements file has failed: " + e);
		}
		finally
		{
			try
			{
				if(save != null)
					save.close();
			}
			catch(Exception e1)
			{
				// nothing
			}
		}
	}

	public void announceToAll(String text)
	{
		announceToAll(text, _type);
	}

	public void announceToAll(String text, int type)
	{
		Say2 cs = new Say2(0, type, "", text);
		for(L2Player player : L2ObjectsStorage.getAllPlayers())
			player.sendPacket(cs);
	}
	
	public void announceToLevelRanged(String text, int min_lvl, int max_lvl)
	{
		Say2 cs = new Say2(0, _type, "", text);
		for(L2Player player : L2ObjectsStorage.getAllPlayers())
			if(player.getLevel() >= min_lvl && player.getLevel() <= max_lvl)
				player.sendPacket(cs);
	}
	
	public void announceCustomToLevelRanged(String address, String[] replacements, int min_lvl, int max_lvl)
	{
		for(L2Player player : L2ObjectsStorage.getAllPlayers())
			if(player.getLevel() >= min_lvl && player.getLevel() <= max_lvl)
				announceToPlayerByCustomMessage(player, address, replacements);
	}


	/**
	 * Отправляет анонсом CustomMessage, приминимо к примеру в шатдауне.
	 * @param address адрес в {@link ru.l2gw.extensions.multilang.CustomMessage}
	 * @param replacements массив String-ов которые атоматически добавятся в сообщения
	 */
	public void announceByCustomMessage(String address, String[] replacements)
	{
		for(L2Player player : L2ObjectsStorage.getAllPlayers())
			announceToPlayerByCustomMessage(player, address, replacements);
	}

	public void announceToPlayerByCustomMessage(L2Player player, String address, String[] replacements)
	{
		CustomMessage cm = new CustomMessage(address, player);
		if(replacements != null)
			for(String s : replacements)
				cm.addString(s);
		player.sendPacket(new Say2(0, _type, "", cm.toString()));
	}

	public void announceToAll(SystemMessage sm)
	{
		for(L2Player player : L2ObjectsStorage.getAllPlayers())
			player.sendPacket(sm);
	}

	public void announceToAll(L2GameServerPacket gsp)
	{
		for(L2Player player : L2ObjectsStorage.getAllPlayers())
			player.sendPacket(gsp);
	}

	public void handleAnnounce(String command, int lengthToTrim)
	{
		handleAnnounce(command, lengthToTrim, _type);
	}

	// Method for handling announcements from admin
	public void handleAnnounce(String command, int lengthToTrim, int type)
	{
		try
		{
			// Announce string to everyone on server
			String text = command.substring(lengthToTrim);
			Announcements.getInstance().announceToAll(text, type);
		}

		// No body cares!
		catch(StringIndexOutOfBoundsException e)
		{
			// empty message.. ignore
		}
	}
}