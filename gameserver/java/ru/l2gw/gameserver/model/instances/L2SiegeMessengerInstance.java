package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

public class L2SiegeMessengerInstance extends L2NpcInstance
{
	@SuppressWarnings("unused")
	//private static final Log _log = LogFactory.getLog(L2SiegeMessengerInstance.class.getName());
	public L2SiegeMessengerInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		if(getBuilding(0) != null)
		{
			if(!getBuilding(0).getSiege().isInProgress())
				getBuilding(0).getSiege().listRegisterClan(player);
		}
		else
			player.sendPacket(new NpcHtmlMessage(player, this, "data/html/chsiege/busy.htm", val));
	}
}
