package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.ExMPCCShowPartyMemberInfo;

/**
 * Format: ch d
 * Пример пакета:
 * D0 2E 00 4D 90 00 10
 */
public class RequestExMPCCShowPartyMembersInfo extends L2GameClientPacket
{
	private int _objectId;

	@Override
	public void readImpl()
	{
		_objectId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null || !player.isInParty() || !player.getParty().isInCommandChannel())
			return;

		L2Player partyLeader = L2ObjectsStorage.getPlayer(_objectId);
		if(partyLeader != null && partyLeader.getParty() != null)
			player.sendPacket(new ExMPCCShowPartyMemberInfo(partyLeader));
	}
}