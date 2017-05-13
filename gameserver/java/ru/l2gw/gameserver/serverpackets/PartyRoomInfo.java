package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 * sample
 * b0
 * d8 a8 10 48  objectId
 * 00 00 00 00
 * 00 00 00 00
 * 00 00
 *
 * format   ddddS
 */
public class PartyRoomInfo extends L2GameServerPacket
{
	private int player_obj_id, view_level, view_class;
	private String MatchingMemo;

	public PartyRoomInfo(L2Player player)
	{
		player_obj_id = player.getObjectId();
		view_level = player.isPartyMatchingShowLevel() ? 1 : 0;
		view_class = player.isPartyMatchingShowClass() ? 1 : 0;
		MatchingMemo = "  " + player.getPartyMatchingMemo();
		// seems to be bugged.. first 2 chars get stripped away
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x9D);
		writeD(player_obj_id);
		writeD(view_level);
		writeD(view_class);
		writeD(0); //c2
		writeS(MatchingMemo);
	}
}