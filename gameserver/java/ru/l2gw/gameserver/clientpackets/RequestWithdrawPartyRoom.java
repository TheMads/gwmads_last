package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.instancemanager.PartyRoomManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.PartyRoom;

/**
 * Format (ch) dd
 * @author -Wooden-
 *
 */
public class RequestWithdrawPartyRoom extends L2GameClientPacket
{
	private int _roomId;
	@SuppressWarnings("unused")
	private int _data2;

	@Override
	public void readImpl()
	{
		_roomId = readD(); //room id
		_data2 = readD(); //unknown
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		PartyRoom room = PartyRoomManager.getInstance().getRoom(_roomId);
		if(room == null)
			return;

		if(room.getLeader() == null || room.getLeader().equals(player))
			PartyRoomManager.getInstance().removeRoom(_roomId);
		else
			PartyRoomManager.getInstance().getRoom(_roomId).removeMember(player, false);
	}
}