package ru.l2gw.gameserver.tables;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.loginservercon.LSConnection;
import ru.l2gw.gameserver.loginservercon.gspackets.SendFakePlayersCount;
import ru.l2gw.gameserver.model.FakePlayer;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.base.ClassId;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

public class FakePlayersTable
{
	private static final Log log = LogFactory.getLog(FakePlayersTable.class);
	private static long _nextLoginUpdate = 0;
	private static List<String> fakePlayerNames = new ArrayList<>();
	private static final Map<String, FakePlayer> fakePlayers = new ConcurrentHashMap<>();
	private static ScheduledFuture<?> fakePlayersDeleteTask;

	public static void loadFakeNames()
	{
		LineNumberReader lnr = null;
		try
		{
			File list = new File("config/fake_players.list");
			if(!list.exists())
				return;

			lnr = new LineNumberReader(new BufferedReader(new FileReader(list)));

			String line;
			while((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
					continue;

				fakePlayerNames.add(line.trim());
			}
		}
		catch(Exception e)
		{
			log.warn("FakePlayersTable: config/fake_players.list load error: " + e, e);
		}
	}

	public static int getFakePlayersCount()
	{
		if(Config.FAKE_PLAYERS_FACTOR > 0)
		{
			int count = (int)(L2ObjectsStorage.getAllPlayersCount() * Config.FAKE_PLAYERS_FACTOR * Config.FAKE_PLAYERS_FACTOR_HOUR[Calendar.getInstance().get(Calendar.HOUR_OF_DAY)]);
			if(_nextLoginUpdate < System.currentTimeMillis())
			{
				_nextLoginUpdate = System.currentTimeMillis() + 300000;
				LSConnection.getInstance().sendPacket(new SendFakePlayersCount(count));
			}
			return count;
		}
		return 0;
	}

	public static int getBotCount()
	{
		return fakePlayers.size();
	}

	public static FakePlayer createFakePlayer(ClassId classId, byte sex, byte hs, byte hc, byte f, int timeMin, int timeMax)
	{
		String name = null;
		while(fakePlayerNames.size() > 0 && name == null)
		{
			name = fakePlayerNames.remove(Rnd.get(fakePlayerNames.size()));
			if(CharNameTable.getInstance().doesCharNameExist(name))
				name = null;
		}

		if(name == null)
			return null;

		return FakePlayer.create((short) classId.getId(), sex, "fake_player", name, hs, hc, f, Rnd.get(timeMin, timeMax));
	}

	public static void addFakePlayer(FakePlayer player)
	{
		FakePlayer fakePlayer = fakePlayers.put(player.getName().toLowerCase(), player);
		if(fakePlayer != null)
			fakePlayer.deleteMe();

		if(fakePlayersDeleteTask == null)
			fakePlayersDeleteTask = ThreadPoolManager.getInstance().scheduleGeneral(new FakePlayerDeleteTask(), 60000);
	}

	public static boolean isFakePlayer(String name)
	{
		return fakePlayers.containsKey(name.toLowerCase());
	}

	private static class FakePlayerDeleteTask implements Runnable
	{
		@Override
		public void run()
		{
			for(FakePlayer fakePlayer : fakePlayers.values())
				if(fakePlayer.getDespawnTime() < System.currentTimeMillis())
				{
					fakePlayers.remove(fakePlayer.getName().toLowerCase());
					if(!fakePlayerNames.contains(fakePlayer.getName()))
						fakePlayerNames.add(fakePlayer.getName());

					fakePlayer.deleteMe();
				}

			if(fakePlayers.size() < 1)
				fakePlayersDeleteTask = null;
			else
				fakePlayersDeleteTask = ThreadPoolManager.getInstance().scheduleGeneral(this, 10000);
		}
	}
}
