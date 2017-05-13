package ru.l2gw.gameserver.serverpackets;

/**
 * @author: rage
 * @date: 11.07.2010 10:19:06
 */
public class ExDominionWarEnd extends L2GameServerPacket
{
	private int _unk = 0;

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xA5);
		writeD(_unk);
	}
}
