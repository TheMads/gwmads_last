package ru.l2gw.gameserver.serverpackets;

public class PledgeShowMemberListDeleteAll extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(0x88);
	}
}