package ru.l2gw.extensions.ccpGuard.packets;

import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;

/**
 * @author: rage
 * @date: 12.10.11 21:58
 */
public class L2ExtHost extends L2GameServerPacket
{
	private byte[] host;
	private int port;

	public L2ExtHost(byte[] ip, int p)
	{
		host = ip;
		port = p;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xb0);
		writeC(0xd0);
		writeB(host);
		writeD(port);
	}
}
