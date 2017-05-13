package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 07.10.2010 17:17:06
 */
public class ExNotifyBirthDay extends L2GameServerPacket
{
	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x90);
	}
}
