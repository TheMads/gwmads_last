package ru.l2gw.gameserver.serverpackets;

public class GMHide extends L2GameServerPacket
{
	private boolean _hide;

	public GMHide(boolean hide)
	{
		_hide = hide;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x93);
		writeD(_hide ? 0x01 : 0x00);
	}
}