package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 16.12.10 17:01
 */
public class ExNotifyPremiumItem extends L2GameServerPacket
{
	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x86);
	}
}
