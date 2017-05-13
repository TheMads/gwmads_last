package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.instancemanager.PartyRoomManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.PartyMatchDetail;

public class RequestPartyMatchConfig extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private int _page;
	@SuppressWarnings("unused")
	private int _region;
	@SuppressWarnings("unused")
	private int _allLevels;

	/**
	 * Format: ddd
	 */

	@Override
	public void readImpl()
	{
		_page = readD();
		_region = readD(); // 0 to 15, or -1
		_allLevels = readD(); // 1 -> all levels, 0 -> only levels matching my level
	}

	@Override
	protected void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;
		player.setPartyMatchingLevels(_allLevels);
		player.setPartyMatchingRegion(_region);

		PartyRoomManager.getInstance().addToWaitingList(player);

		player.sendPacket(new PartyMatchDetail(_region, _allLevels, _page, player));

	}
}
