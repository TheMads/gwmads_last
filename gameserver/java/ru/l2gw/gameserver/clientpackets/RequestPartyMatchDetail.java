package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.instancemanager.PartyRoomManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.PartyRoom;

public class RequestPartyMatchDetail extends L2GameClientPacket
{
	private int _roomId;
	private int _mode;
	private int _level;
	@SuppressWarnings("unused")
	private int _unk;

	/**
	 * Format: ddd
	 */
	@Override
	public void readImpl()
	{
		_roomId = readD(); // room id, если 0 то autojoin
		_mode = readD(); // location
		_level = readD(); // 1 - all, 0 - my level (только при autojoin)
		_unk = readD(); //Unknown всегда 0 ??

		/*	Near Me - (-2)
		All - (-1)
		Talking Island - 1
		Gludio - 2
		Dark Elven Ter. - 3
		Elven Territory - 4
		Dion - 5
		Giran - 6
		Neutral Zone - 7
		Schuttgart - 9
		Oren - 10
		Hunters Village - 11
		Innadril - 12
		Aden - 13
		Rune - 14
		Goddard - 15
		Change - 100 ???? (возможно room id)*/
	}

	@Override
	protected void runImpl()
	{

		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(_roomId > 0)
			PartyRoomManager.getInstance().joinPartyRoom(player, _roomId);
		else
			for(PartyRoom room : PartyRoomManager.getInstance().getRooms(_mode, _level, player))
				if(room.getMembersSize() < room.getMaxMembers())
					PartyRoomManager.getInstance().joinPartyRoom(player, room.getId());
	}
}