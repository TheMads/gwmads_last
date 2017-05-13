package ru.l2gw.gameserver.model.instances;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.RaidBossSpawnManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Spawn;
import ru.l2gw.gameserver.serverpackets.RadarControl;
import ru.l2gw.gameserver.tables.TerritoryTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.util.Location;

import java.io.File;

/**
 * This class ...
 *
 * @version $Revision: $ $Date: $
 * @author LBaldi
 */
public class L2AdventurerInstance extends L2NpcInstance
{
	private static Log _log = LogFactory.getLog(L2AdventurerInstance.class.getName());

	public L2AdventurerInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(command.startsWith("npcfind_byid"))
			try
			{
				int bossId = Integer.parseInt(command.substring(12).trim());
				switch(RaidBossSpawnManager.getInstance().getRaidBossStatusId(bossId))
				{
					case ALIVE:
					case DEAD:
						L2Spawn spawn = RaidBossSpawnManager.getInstance().getSpawnTable().get(bossId);
						// Убираем флажок на карте и стрелку на компасе
						if(spawn == null)
						{
							_log.warn("L2Adventurer: no spawn for raid boss id: " + bossId);
							return;
						}
						player.sendPacket(new RadarControl(2, 2, spawn.getLoc()));
						// Ставим флажок на карте и стрелку на компасе
						Location loc;
						if(spawn.getLocation() != 0)
						{
							int p[] = TerritoryTable.getInstance().getRandomPoint(spawn.getLocation(), false);
							loc = new Location(p[0], p[1], p[2]);
						}
						else
							loc = spawn.getLoc();

						player.sendPacket(new RadarControl(0, 1, loc));
						break;
					case UNDEFINED:
						player.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.instances.L2AdventurerInstance.BossNotInGame", player).addNumber(bossId));
						break;
				}
			}
			catch(NumberFormatException e)
			{
				_log.warn("L2AdventurerInstance: Invalid Bypass to Server command parameter.");
			}
		else if(command.startsWith("raidInfo"))
		{
			int bossLevel = Integer.parseInt(command.substring(9).trim());

			String filename = "data/html/adventurer_guildsman/raid_info/info.htm";
			if(bossLevel != 0)
				filename = "data/html/adventurer_guildsman/raid_info/level" + bossLevel + ".htm";

			showChatWindow(player, filename);
		}
		else if(command.equalsIgnoreCase("questlist"))
			player.sendPacket(Msg.ExShowQuestInfo);
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	public String getHtmlPath(int npcId, int val, int karma)
	{
		String pom;
		if(val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		String temp = "data/html/adventurer_guildsman/" + pom + ".htm";

		File mainText = new File(temp);

		// Return the pathfile of the HTML file
		if(mainText.exists())
			return temp;

		// if the file is not found, the standard message "I have nothing to say to you" is returned
		return "data/html/npcdefault.htm";
	}
}
