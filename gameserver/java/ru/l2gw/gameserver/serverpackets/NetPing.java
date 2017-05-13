package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 07.08.2009 12:14:33
 */
public class NetPing extends L2GameServerPacket
{
	private short _rnd;
	private short _seq;

	public NetPing(short rnd, short seq)
	{
		_rnd = rnd;
		_seq = seq;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xD9);
		writeH(_rnd);  // Random ?
		writeH(_seq);  // sequence
	}
}
