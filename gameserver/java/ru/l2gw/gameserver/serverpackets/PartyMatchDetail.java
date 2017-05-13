package ru.l2gw.gameserver.serverpackets;

import javolution.util.FastList;
import ru.l2gw.gameserver.instancemanager.PartyRoomManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.PartyRoom;

import java.util.Collection;

/**
 *
 *
 * sample
 * b0
 * d8 a8 10 48  objectId
 * 00 00 00 00
 * 00 00 00 00
 * 00 00
 *
 * format   ddddS
 *
 */
public class PartyMatchDetail extends L2GameServerPacket
{
	private Collection<PartyRoom> _rooms;
	private int _fullSize;

	public PartyMatchDetail(L2Player player)
	{
		this(player.getPartyMatchingRegion(), player.getPartyMatchingLevels(), 1, player);
	}

	public PartyMatchDetail(int region, int lvlRst, int page, L2Player activeChar)
	{
		int first = (page - 1) * 64;
		int firstNot = page * 64;
		_rooms = new FastList<PartyRoom>();

		int i = 0;
		FastList<PartyRoom> temp = PartyRoomManager.getInstance().getRooms(region, lvlRst, activeChar);
		_fullSize = temp.size();
		for(PartyRoom room : temp)
		{
			if(i < first || i >= firstNot)
				continue;
			_rooms.add(room);
			i++;
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x9c);
		writeD(_fullSize); // unknown
		writeD(_rooms.size()); // room count

		for(PartyRoom room : _rooms)
		{
			writeD(room.getId()); //room id
			writeS(room.getTitle()); // room name
			writeD(room.getLocation()); //Location (смотерть список ниже)
			writeD(room.getMinLevel()); //min level
			writeD(room.getMaxLevel()); //max level
			writeD(room.getMaxMembers()); //max members count
			writeS(room.getLeader() == null ? "None" : room.getLeader().getName()); //leader name
			writeD(room.getMembersSize()); //members count
			for(L2Player player : room.getMembers())
			{
				if(player == null)
				{
					writeD(0x00);
					writeS("");
				}
				else
				{
					writeD(player.getClassId().getId());
					writeS(player.getName());
				}
			}
		}

		/*	Talking Island - 1
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
			Goddard - 15 */

	}
}
