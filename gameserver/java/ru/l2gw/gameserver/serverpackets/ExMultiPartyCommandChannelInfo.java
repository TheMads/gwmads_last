package ru.l2gw.gameserver.serverpackets;

import javolution.util.FastList;
import ru.l2gw.gameserver.model.L2CommandChannel;
import ru.l2gw.gameserver.model.L2Party;

/**
 * Update a Command Channel
 *
 * Format:
 * ch sddd[sdd]
 * Пример пакета с оффа (828 протокол):
 * fe 31 00
 * 62 00 75 00 73 00 74 00 65 00 72 00 00 00 - имя лидера СС
 * 00 00 00 00 - ? Looting type
 * 19 00 00 00 - общее число человек в СС
 * 04 00 00 00 - общее число партий в СС
 * [
 * 62 00 75 00 73 00 74 00 65 00 72 00 00 00  - лидер партии 1
 * 36 46 70 4c - ObjId пати лидера 1
 * 08 00 00 00 - количество мемберов в пати 1
 * 4e 00 31 00 67 00 68 00 74 00 48 00 75
 * ]
 */
public class ExMultiPartyCommandChannelInfo extends L2GameServerPacket
{

	private String ChannelLeaderName;
	private int MemberCount;
	private int lootingRights;
	private FastList<ChannelPartyInfo> parties;

	/**
	 * @param L2CommandChannel CommandChannel
	 */
	public ExMultiPartyCommandChannelInfo(L2CommandChannel channel)
	{
		if(channel == null)
			return;
		ChannelLeaderName = channel.getChannelLeader().getName();
		MemberCount = channel.getMemberCount();
		lootingRights = channel.getLootingRights() ? 1 : 0;

		parties = new FastList<ChannelPartyInfo>();
		for(L2Party party : channel.getParties())
			parties.add(new ChannelPartyInfo(party.getPartyLeader().getName(), party.getPartyLeaderOID(), party.getMemberCount()));
	}

	@Override
	protected void writeImpl()
	{
		if(parties == null)
			return;

		writeC(EXTENDED_PACKET);
		writeH(0x31);
		writeS(ChannelLeaderName); // имя лидера CC
		writeD(lootingRights); // Channelloot 0 or 1
		writeD(MemberCount); // общее число человек в СС
		writeD(parties.size()); // общее число партий в СС

		for(ChannelPartyInfo party : parties)
		{
			writeS(party.Leader_name); // имя лидера партии
			writeD(party.Leader_obj_id); // ObjId пати лидера
			writeD(party.MemberCount); // количество мемберов в пати
		}
	}

	static class ChannelPartyInfo
	{
		public String Leader_name;
		public int Leader_obj_id, MemberCount;

		public ChannelPartyInfo(String _Leader_name, int _Leader_obj_id, int _MemberCount)
		{
			Leader_name = _Leader_name;
			Leader_obj_id = _Leader_obj_id;
			MemberCount = _MemberCount;
		}
	}
}