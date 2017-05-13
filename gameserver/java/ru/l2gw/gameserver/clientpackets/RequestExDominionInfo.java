package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.ExReplyDominionInfo;
import ru.l2gw.gameserver.serverpackets.ExShowOwnthingPos;

public class RequestExDominionInfo extends L2GameClientPacket
{
	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		player.sendPacket(new ExReplyDominionInfo());

		if(TerritoryWarManager.getWar().isInProgress())
			player.sendPacket(new ExShowOwnthingPos());
	}

	@Override
	public void readImpl()
	{}
}