package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.instancemanager.PartyRoomManager;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.PartyRoom;

/**
 * format (ch) d
 * @author -Wooden-
 *
 */
public class RequestOustFromPartyRoom extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private int _id;

	@Override
	public void readImpl()
	{
		_id = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		L2Player member = L2ObjectsStorage.getPlayer(_id);
		if(player == null || member == null)
			return;

		PartyRoom room = PartyRoomManager.getInstance().getRoom(member.getPartyRoom());
		if(room != null)
			room.removeMember(member, true);
	}
}