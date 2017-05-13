package ru.l2gw.gameserver.serverpackets;

/**
 * @author: rage
 * @date: 24.04.13 21:24
 */
public class Ex2ndPasswordCheck extends L2GameServerPacket
{
	public static final int PASSWORD_NEW = 0x00;
	public static final int PASSWORD_PROMPT = 0x01;
	public static final int PASSWORD_OK = 0x02;

	private final int _windowType;

	public Ex2ndPasswordCheck(int windowType)
	{
		_windowType = windowType;
	}

	@Override
	protected void writeImpl()
	{
		writeEx(0x10A);
		// writeEx(0x109); GOD
		writeD(_windowType);
		writeD(0x00);
	}
}
