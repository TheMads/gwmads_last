package ru.l2gw.gameserver.serverpackets;

public class ExRedSky extends L2GameServerPacket
{
	private int _duration;

	/**
	 * 0xfe:0x40 ExRedSky         d
	 */
	public ExRedSky(int duration/*, int type, int v3, int v4, int v5*/)
	{
		_duration = duration;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x42); // sub id
		writeD(_duration);
	}
}