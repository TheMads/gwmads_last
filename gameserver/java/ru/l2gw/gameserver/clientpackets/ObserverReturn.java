package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;

public class ObserverReturn extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null || !player.inObserverMode())
			return;

		if(player.getOlympiadGameId() == -1)
			player.leaveObserverMode();
		else
			player.leaveOlympiadObserverMode();
	}
}