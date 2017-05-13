package ru.l2gw.gameserver.instancemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.Announcements;
import ru.l2gw.gameserver.model.L2AutoAnnounce;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.ExShowScreenMessage;
import ru.l2gw.gameserver.serverpackets.ExShowScreenMessage.ScreenMessageAlign;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AutoAnnounce implements Runnable
{
	private static Log _log = LogFactory.getLog(AutoAnnounce.class.getName());
	private static AutoAnnounce _instance;

	static Map<Integer, L2AutoAnnounce> _lists;

	public static AutoAnnounce getInstance()
	{
		if(_instance == null)
			_instance = new AutoAnnounce();
		return _instance;
	}

	public static void reload()
	{
		_instance = new AutoAnnounce();
	}

	public AutoAnnounce()
	{
		_lists = new HashMap<Integer, L2AutoAnnounce>();
		_log.info("AutoAnnounce: Initializing");
		load();
		_log.info("AutoAnnounce: Loaded " + (_lists.size() - 1) + " announce.");
	}

	private void load()
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			File file = new File("./config/autoannounce.xml");
			if(!file.exists())
			{
				if(Config.DEBUG)
					System.out.println("AutoAnnounce: NO FILE");
				return;
			}

			Document doc = factory.newDocumentBuilder().parse(file);
			int counterAnnounce = 0;
			if(counterAnnounce == 0)
			{
				ArrayList<String> msg = new ArrayList<String>();
				L2AutoAnnounce aa = new L2AutoAnnounce(counterAnnounce);
				int revision = 634 - 634;
				msg.add("" + revision);
				String name = "Own1";
				String name2 = "Own2";
				msg.add(name + name2);
				aa.setAnnounce(0, 0, msg);
				_lists.put(counterAnnounce, aa);
				counterAnnounce++;
			}
			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				if("list".equalsIgnoreCase(n.getNodeName()))
					for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						if("announce".equalsIgnoreCase(d.getNodeName()))
						{
							ArrayList<String> msg = new ArrayList<String>();
							NamedNodeMap attrs = d.getAttributes();
							int delay = Integer.parseInt(attrs.getNamedItem("delay").getNodeValue());
							int repeat = Integer.parseInt(attrs.getNamedItem("repeat").getNodeValue());
							
							boolean isScreenMessage;
							
							try
							{
							    isScreenMessage = Boolean.parseBoolean(attrs.getNamedItem("isScreenMessage").getNodeValue());
							} catch (Exception e)
							{
							
							    isScreenMessage = false;
							
							}
							L2AutoAnnounce aa = new L2AutoAnnounce(counterAnnounce);
							aa.setScreenAnnounce(isScreenMessage);
							for(Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
								if("message".equalsIgnoreCase(cd.getNodeName()))
								{
									msg.add(String.valueOf(cd.getAttributes().getNamedItem("text").getNodeValue()));
								}
							aa.setAnnounce(delay, repeat, msg);
							_lists.put(counterAnnounce, aa);
							counterAnnounce++;
						}
			if(Config.DEBUG)
				System.out.println("AutoAnnounce: OK");
		}
		catch(Exception e)
		{
			_log.error("AutoAnnounce: Error parsing autoannounce.xml file. " + e);
		}
	}

	public void run()
	{
		if(_lists.size() <= 1)
			return;
		for(int i = 1; i < _lists.size(); i++)
		{
		L2AutoAnnounce item = _lists.get(i);
			if(item.canAnnounce())
			{
				ArrayList<String> msg = item.getMessage();
				for(String text : msg)
					if(!item.isScreenAnnounce())
						Announcements.getInstance().announceToAll(text);
					else
					{
						int _time = 3000 + text.length() * 100; // 3 секунды + 100мс на символ
						boolean _font_big = text.length() < 64;

						ExShowScreenMessage sm = new ExShowScreenMessage(text, _time, ScreenMessageAlign.TOP_CENTER, _font_big);

						for(L2Player player : L2ObjectsStorage.getAllPlayers())
							player.sendPacket(sm);

					}

				_lists.get(i).updateRepeat();
			}
		}
	}

	public static String getRevision()
	{
		if(_lists.size() == 0)
			return "";
		return _lists.get(0).getMessage().get(0);
	}

	public static String getOwnerName()
	{
		if(_lists.size() == 0)
			return "";
		return _lists.get(0).getMessage().get(1);
	}
}