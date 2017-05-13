package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.ExListPartyMatchingWaitingRoom;

/** 
 * Format: dddd
 */
public class RequestListPartyMatchingWaitingRoom extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private int _minLevel, _maxLevel, _page, _unk;

	@Override
	protected void readImpl()
	{
		_page = readD();
		_minLevel = readD();
		_maxLevel = readD();
		_unk = readD(); // всегда 1?
	}

	@Override
	protected void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		player.sendPacket(new ExListPartyMatchingWaitingRoom(player, _minLevel, _maxLevel, _page));
	}
}