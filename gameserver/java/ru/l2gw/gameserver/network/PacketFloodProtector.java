package ru.l2gw.gameserver.network;

import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.Map;
import java.util.StringTokenizer;

public class PacketFloodProtector
{
	private static PacketFloodProtector _instance;
	private static Map<Integer, PacketData> _packetList;
	private static Log _log = LogFactory.getLog("network");

	public static enum ActionType
	{
		log,
		drop_log,
		kick_log,
		drop,
		none
	}

	public static PacketFloodProtector getInstance()
	{
		if(_instance == null)
			_instance = new PacketFloodProtector();
		return _instance;
	}

	public PacketFloodProtector()
	{
		load();
	}

	public void load()
	{
		if(_packetList == null) _packetList = new FastMap<Integer, PacketData>();

		LineNumberReader lnr = null;
		try
		{
			File dataFile = new File("./config/floodprotect.properties");
			lnr = new LineNumberReader(new BufferedReader(new FileReader(dataFile)));

			String line;
			_log.warn("PacketFloodProtector: initialize");

			while((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
					continue;

				PacketData pd = parseList(line);
				if(pd != null)
					_packetList.put(pd.getPacketId(), pd);
			}

			_log.info("PacketFloodProtector: Loaded " + _packetList.size() + " packets.");
		}
		catch(FileNotFoundException e)
		{
			_log.warn("PacketFloodProtector: config/floodprotect.properties is missing");
		}
		catch(IOException e)
		{
			_log.warn("PacketFloodProtector: error while creating packet flood table " + e);
		}
		finally
		{
			try
			{
				lnr.close();
			}
			catch(Exception e1)
			{
			}
		}
	}

	public void reload()
	{
		_packetList = null;
		load();
	}

	private PacketData parseList(String line)
	{
		StringTokenizer st = new StringTokenizer(line, ";");
		try
		{
			int packetId = Integer.decode(st.nextToken());
			int delay = Integer.parseInt(st.nextToken());
			ActionType action = ActionType.valueOf(st.nextToken());
			return new PacketData(packetId, delay, action);
		}
		catch(Exception e)
		{
			_log.warn("FP: parse error: '"+line+"' "+e);
		}
		return null;
	}

	public PacketData getDataByPacketId(int packetId)
	{
		if(_packetList == null || _packetList.size() == 0)
			return null;
		return _packetList.get(packetId);
	}

	public class PacketData
	{
		private int _packetId;
		private int _delay;
		private ActionType _action;

		public PacketData(int packetId, int delay, ActionType action)
		{
			_packetId = packetId;
			_delay = delay;
			_action = action;
		}

		public int getDelay()
		{
			return _delay;
		}

		public ActionType getAction()
		{
			return _action;
		}

		public int getPacketId()
		{
			return _packetId;
		}
	}
}
