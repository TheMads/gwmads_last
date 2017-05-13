package ru.l2gw.gameserver.serverpackets;

import javolution.util.FastList;
import ru.l2gw.gameserver.model.L2Party;
import ru.l2gw.gameserver.model.L2Player;

/**
 * Format: ch d[Sdd]
 */
public class ExMPCCShowPartyMemberInfo extends L2GameServerPacket
{
	private FastList<PartyMemberInfo> members;

	public ExMPCCShowPartyMemberInfo(L2Player partyLeader)
	{
		if(!partyLeader.isInParty())
			return;

		L2Party _party = partyLeader.getParty();
		if(_party == null)
			return;

		if(!_party.isInCommandChannel())
			return;

		members = new FastList<PartyMemberInfo>();
		for(L2Player _member : _party.getPartyMembers())
			members.add(new PartyMemberInfo(_member.getName(), _member.getObjectId(), _member.getClassId().getId()));
	}

	@Override
	protected final void writeImpl()
	{
		if(members == null)
			return;

		writeC(EXTENDED_PACKET);
		writeH(0x4C);
		writeD(members.size()); // Количество членов в пати
		for(PartyMemberInfo _member : members)
		{
			writeS(_member._name); // Имя члена пати
			writeD(_member.object_id); // object Id члена пати
			writeD(_member.class_id); // id класса члена пати
		}
	}

	static class PartyMemberInfo
	{
		public String _name;
		public int object_id, class_id;

		public PartyMemberInfo(String __name, int _object_id, int _class_id)
		{
			_name = __name;
			object_id = _object_id;
			class_id = _class_id;
		}
	}
}