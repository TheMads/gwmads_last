package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.instancemanager.PartyRoomManager;

public class RequestExitPartyMatchingWaitingRoom extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getPlayer();
		if(activeChar == null)
			return;

		PartyRoomManager.getInstance().removeFromWaitingList(activeChar);
	}
}