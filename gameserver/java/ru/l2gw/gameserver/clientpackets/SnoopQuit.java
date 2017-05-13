package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;

public class SnoopQuit extends L2GameClientPacket
{
	private int _snoopID;

	/**
	 * format: cd
	 */
	@Override
	public void readImpl()
	{
		_snoopID = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = L2ObjectsStorage.getPlayer(_snoopID);
		if(player == null)
			return;
		L2Player player1 = getClient().getPlayer();
		if(player1 == null)
			return;
		player.removeSnooper(player);
		player.removeSnooped(player);
	}
}
