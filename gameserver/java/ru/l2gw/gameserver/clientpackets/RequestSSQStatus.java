package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SSQStatus;

/**
 * Seven Signs Record Update Request
 *
 * packet type id 0xc8
 * format: cc
 */
public class RequestSSQStatus extends L2GameClientPacket
{
	private int _page;

	@Override
	public void readImpl()
	{
		//_page = readC();
	}

	@Override
	public void runImpl()
	{
		//L2Player player = getClient().getPlayer();
		//if(player == null)
		//	return;
		//sendPacket(new SSQStatus(player, _page));
	}
}
