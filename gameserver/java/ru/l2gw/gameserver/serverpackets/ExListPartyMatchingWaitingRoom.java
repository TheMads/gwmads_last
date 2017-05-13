package ru.l2gw.gameserver.serverpackets;

import javolution.util.FastList;
import ru.l2gw.gameserver.instancemanager.PartyRoomManager;
import ru.l2gw.gameserver.model.L2Player;

import java.util.Vector;

public class ExListPartyMatchingWaitingRoom extends L2GameServerPacket
{
	private FastList<L2Player> _waiting;
	private int _fullSize;

	public ExListPartyMatchingWaitingRoom(L2Player searcher, int minLevel, int maxLevel, int page)
	{
		_waiting = new FastList<L2Player>();
		int first = (page - 1) * 64;
		int firstNot = page * 64;

		int i = 0;
		Vector<L2Player> temp = PartyRoomManager.getInstance().getWaitingList(minLevel, maxLevel);
		_fullSize = temp.size();
		for(L2Player pc : temp)
		{
			if(i < first || i >= firstNot)
				continue;
			_waiting.add(pc);
			i++;
		}
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x36);

		writeD(_fullSize);

		writeD(_waiting.size());
		for(L2Player p : _waiting)
		{
			writeS(p.getName());
			writeD(p.getClassId().getId());
			writeD(p.getLevel());
			writeD(PartyRoomManager.getInstance().getLocation(p));
			writeD(0x00);
		}
	}
}
