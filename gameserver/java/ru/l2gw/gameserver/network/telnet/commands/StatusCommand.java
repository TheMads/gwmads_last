package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.gameserver.*;
import ru.l2gw.gameserver.controllers.GameTimeController;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Summon;
import ru.l2gw.gameserver.model.L2World;
import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2MonsterInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.tables.FakePlayersTable;
import ru.l2gw.gameserver.tables.GmListTable;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author: rage
 * @date: 03.03.12 20:13
 */
public class StatusCommand extends TelnetCommand
{
	public StatusCommand()
	{
		super("status", "s");
	}
	
	@Override
	public String getUsage()
	{
		return "type: status";
	}
	
	@Override
	public String handle(String[] args, String ip)
	{
		int playerCount, objectCount, offlinePlayers, fakePlayers, botPlayers;
		int max = Config.MAXIMUM_ONLINE_USERS;

		playerCount = L2ObjectsStorage.getAllPlayersCount();
		offlinePlayers = L2ObjectsStorage.getAllOfflineCount();
		fakePlayers = FakePlayersTable.getFakePlayersCount();
		botPlayers = FakePlayersTable.getBotCount();
		objectCount = L2ObjectsStorage.getAllObjectsCount();

		playerCount -= offlinePlayers;
		playerCount -= botPlayers;

		int itemCount = 0;
		int itemVoidCount = 0;
		int monsterCount = 0;
		int minionCount = 0;
		int minionsGroupCount = 0;
		int npcCount = 0;
		int guardCount = 0;
		int charCount = 0;
		int doorCount = 0;
		int summonCount = 0;
		int AICount = 0;
		int extendedAICount = 0;
		int summonAICount = 0;

		for(L2Object obj : L2ObjectsStorage.getAllObjects())
		{
			if(obj.isCharacter())
			{
				charCount++;
				if(obj.hasAI())
				{
					AICount++;
					if(obj.isNpc())
						extendedAICount++;
					else if(obj instanceof L2Summon)
						summonAICount++;
				}
			}
			else if(obj instanceof L2ItemInstance)
				if(((L2ItemInstance) obj).getLocation() == L2ItemInstance.ItemLocation.VOID)
					itemVoidCount++;
				else
					itemCount++;

			if(obj.isMonster())
			{
				monsterCount++;
				minionCount += ((L2MonsterInstance) obj).getTotalSpawnedMinionsInstances();
				minionsGroupCount += ((L2MonsterInstance) obj).getTotalSpawnedMinionsGroups();
			}

			if(obj instanceof L2NpcInstance)
				npcCount++;
			else if(obj instanceof L2Summon)
				summonCount++;
			else if(obj instanceof L2DoorInstance)
				doorCount++;
		}

		int t = GameTimeController.getInstance().getGameTime();
		int h = t / 60;
		int m = t % 60;
		SimpleDateFormat format = new SimpleDateFormat("H:mm");
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, h);
		cal.set(Calendar.MINUTE, m);
		String gameTime = format.format(cal.getTime());

		t = GameServer.uptime();
		int s = t - h * 3600 - m * 60;
		String uptime =  h + "hrs " + m + "mins " + s + "secs";

		StringBuilder sb = new StringBuilder();
		
		sb.append("Server Status:\n");
		sb.append(" + Players (Real/Offline): ").append(playerCount).append("/").append(offlinePlayers).append(" of maximum ").append(max).append("\n");
		sb.append(" +.... Players (Bot/Fake): ").append(botPlayers).append("/").append(fakePlayers).append("\n");
		sb.append(" +............... Summons: ").append(summonCount).append("\n");
		sb.append(" +.............. Monsters: ").append(monsterCount).append("\n");
		sb.append(" +............... Minions: ").append(minionCount).append("\n");
		sb.append(" +........ Minion Groups: ").append(minionsGroupCount).append("\n");
		sb.append(" +.................. Npc: ").append(npcCount).append("\n");
		sb.append(" +................... GM: ").append(GmListTable.getAllGMs().size()).append("\n");
		sb.append(" +.............. Objects: ").append(objectCount).append("\n");
		sb.append(" +............... All AI: ").append(AICount).append("\n");
		sb.append(" +........... ExtendedAI: ").append(extendedAICount).append("\n");
		sb.append(" +........... L2SummonAI: ").append(summonAICount).append("\n");
		sb.append(" +..........Ground Items: ").append(itemVoidCount).append("\n");
		sb.append(" +...........Owned Items: ").append(itemCount).append("\n");
		sb.append(" +........ L2CastleGuard: ").append(guardCount).append("\n");
		sb.append(" +............... L2Door: ").append(doorCount).append("\n");
		sb.append(" +............... L2Char: ").append(charCount).append("\n");
		sb.append(" +. InGame Time / Uptime: ").append(gameTime).append(" / ").append(uptime);
		sb.append(" +.. Shutdown_sec / mode: ").append(Shutdown.getInstance().getSeconds()).append(" / ").append(Shutdown.getInstance().getMode()).append("\n");
		//sb.append(" +.. Log_chat / log_tell: " + log_chat + " / " + log_tell);
		sb.append(" +....... Active Regions: ").append(L2World.getActiveRegionsCount()).append("\n");
		sb.append(" +.............. Threads: ").append(Thread.activeCount()).append("\n");
		sb.append(" +............. RAM Used: ").append((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024).append("\n");

		return sb.toString();
	}
}