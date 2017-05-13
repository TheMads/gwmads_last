package ru.l2gw.gameserver.serverpackets;

import javolution.util.FastList;
import ru.l2gw.gameserver.model.L2Player;

public class PartyMemberPosition extends L2GameServerPacket
{
	private FastList<PartyMemberpos> poses = new FastList<PartyMemberpos>();
	private int MemberCount;

	public PartyMemberPosition(L2Player actor)
	{
		MemberCount = actor.getParty().getMemberCount();
		for(L2Player pm : actor.getParty().getPartyMembers())
		{
			if(pm == null)
				continue;
			poses.add(new PartyMemberpos(pm.getObjectId(), pm.getX(), pm.getY(), pm.getZ()));
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xBA);
		writeD(MemberCount);
		for(PartyMemberpos _pos : poses)
		{
			writeD(_pos._id);
			writeD(_pos.x);
			writeD(_pos.y);
			writeD(_pos.z);
		}
	}

	static class PartyMemberpos
	{
		public int _id, x, y, z;

		public PartyMemberpos(int __id, int _x, int _y, int _z)
		{
			_id = __id;
			x = _x;
			y = _y;
			z = _z;
		}
	}
}