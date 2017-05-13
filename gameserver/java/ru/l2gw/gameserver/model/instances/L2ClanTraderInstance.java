package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;

/**
 * @author rage
 * @date 07.11.2009 17:52:31
 */
public class L2ClanTraderInstance extends L2NpcInstance
{
	private static String _path = "data/html/clantrader/";
	private static int BLOOD_OATH = 9910;
	private static int BLOOD_ALLIANCE = 9911;
	private static int KNIGHTS_EPAULETTE = 9912;

	public L2ClanTraderInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(command.equalsIgnoreCase("Exchange 1"))
		{
			if(!player.isClanLeader())
			{
				showChatWindow(player, 2);
				return;
			}
			else if(player.getClan().getLevel() < 5)
			{
				showChatWindow(player, 4);
				return;
			}

			long count = player.getItemCountByItemId(BLOOD_ALLIANCE);
			if(count > 0)
			{
				if(player.destroyItemByItemId("ClanTrade", BLOOD_ALLIANCE, 1, this, true))
					player.getClan().incReputation(800, false, "ClanTrade");

				if(count > 1)
					showChatWindow(player, 5);
				else
					showChatWindow(player, 6);
			}
			else
				showChatWindow(player, 3);
		}
		if(command.equalsIgnoreCase("Exchange 2"))
		{
			if(!player.isClanLeader())
			{
				showChatWindow(player, 2);
				return;
			}
			else if(player.getClan().getLevel() < 5)
			{
				showChatWindow(player, 4);
				return;
			}

			long count = player.getItemCountByItemId(BLOOD_OATH);
			if(count >= 10)
			{
				if(player.destroyItemByItemId("ClanTrade", BLOOD_OATH, 10, this, true))
					player.getClan().incReputation(200, false, "ClanTrade");

				if(count > 10)
					showChatWindow(player, 5);
				else
					showChatWindow(player, 6);
			}
			else
				showChatWindow(player, 3);
		}
		if(command.equalsIgnoreCase("Exchange 3"))
		{
			if(!player.isClanLeader())
			{
				showChatWindow(player, 2);
				return;
			}
			else if(player.getClan().getLevel() < 5)
			{
				showChatWindow(player, 4);
				return;
			}

			long count = player.getItemCountByItemId(KNIGHTS_EPAULETTE);
			if(count >= 100)
			{
				if(player.destroyItemByItemId("ClanTrade", KNIGHTS_EPAULETTE, 100, this, true))
					player.getClan().incReputation(20, false, "ClanTrade");

				if(count > 100)
					showChatWindow(player, 5);
				else
					showChatWindow(player, 6);
			}
			else
				showChatWindow(player, 3);
		}
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String filename = _path + getNpcId();
		if(player.isClanLeader())
		{
			if(val > 0)
				filename += "-" + val + ".htm";
			else
				filename += ".htm";
		}	
		else
			filename += "-no.htm";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		if(val == 5)
		{
			L2Clan clan = player.getClan();
			html.replace("%pledge_name%", clan.getName());
			html.replace("%fame_value%", String.valueOf(clan.getReputationScore()));
		}
		player.sendPacket(html);
	}
}
