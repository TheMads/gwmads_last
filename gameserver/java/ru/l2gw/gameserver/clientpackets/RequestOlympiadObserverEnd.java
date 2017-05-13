package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 * format ch
 */
public class RequestOlympiadObserverEnd extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;
		if(player.inObserverMode() && player.getOlympiadGameId() != -1)
			player.leaveOlympiadObserverMode();
	}
}