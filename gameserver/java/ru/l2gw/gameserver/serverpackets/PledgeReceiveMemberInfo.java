package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2ClanMember;

public class PledgeReceiveMemberInfo extends L2GameServerPacket
{
	private L2ClanMember _member;

	public PledgeReceiveMemberInfo(L2ClanMember member)
	{
		_member = member;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x3F);

		writeD(_member.getPledgeType());
		writeS(_member.getName());
		writeS(_member.getTitle());
		writeD(_member.getPowerGrade());

		if(_member.getPledgeType() != 0 && _member.getClan() != null && _member.getClan().getSubPledge(_member.getPledgeType()) != null)
			writeS(_member.getClan().getSubPledge(_member.getPledgeType()).getName());
		else
			writeS(_member.getClan().getName());

		writeS(_member.getRelatedName()); // apprentice/sponsor name if any
	}
}