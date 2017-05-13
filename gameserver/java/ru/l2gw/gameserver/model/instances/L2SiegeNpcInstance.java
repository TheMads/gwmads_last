package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

public class L2SiegeNpcInstance extends L2NpcInstance
{
	public L2SiegeNpcInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	/**
	 * If siege is in progress shows the Busy HTML<BR>
	 * else Shows the SiegeInfo window
	 * @param player
	 */
	@Override
	public void showChatWindow(L2Player player, int val)
	{
		if(!getBuilding(2).getSiege().isInProgress())
			getBuilding(2).getSiege().listRegisterClan(player);
		else
		{
			NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
			html.setHtml("<html><body><font color=\"LEVEL\">Oh! Our castle is being attacked and I can't do anything for you right now.</font></body></html>");
			player.sendPacket(html);
		}
	}
}