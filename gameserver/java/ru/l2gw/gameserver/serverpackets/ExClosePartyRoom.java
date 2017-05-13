package ru.l2gw.gameserver.serverpackets;

public class ExClosePartyRoom extends L2GameServerPacket
{
	public ExClosePartyRoom()
	{}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x09);
	}
}
