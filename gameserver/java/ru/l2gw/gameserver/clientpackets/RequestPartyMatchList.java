package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.instancemanager.PartyRoomManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.PartyRoom;
import ru.l2gw.gameserver.serverpackets.ExPartyRoomMember;
import ru.l2gw.gameserver.serverpackets.PartyMatchList;

public class RequestPartyMatchList extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private int _lootDist;
	@SuppressWarnings("unused")
	private int _maxMembers;
	@SuppressWarnings("unused")
	private int _minLevel;
	@SuppressWarnings("unused")
	private int _maxLevel;
	@SuppressWarnings("unused")
	private int _roomId;
	@SuppressWarnings("unused")
	private String _roomTitle;

	@Override
	public void readImpl()
	{
		_roomId = readD();
		_maxMembers = readD();
		_minLevel = readD();
		_maxLevel = readD();
		_lootDist = readD();
		_roomTitle = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		PartyRoom room = null;
		if(player.getPartyRoom() == 0)
			room = PartyRoomManager.getInstance().addRoom(_minLevel, _maxLevel, _maxMembers, _lootDist, _roomTitle, player);
		else if(player.getPartyRoom() == _roomId)
			room = PartyRoomManager.getInstance().changeRoom(_roomId, _minLevel, _maxLevel, _maxMembers, _lootDist, _roomTitle);
		else
			return;

		// This packet is used to create a party room.
		player.sendPacket(new PartyMatchList(room));
		player.sendPacket(new ExPartyRoomMember(room, player));
		player.broadcastUserInfo(true);
	}
}
