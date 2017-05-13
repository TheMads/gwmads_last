package ru.l2gw.gameserver.serverpackets;

/**
 * @author: rage
 * @date: 24.04.13 21:22
 */
public class Ex2ndPasswordAck extends L2GameServerPacket
{
	int _response;

	public static int SUCCESS = 0x00;
	public static int WRONG_PATTERN = 0x01;
	public static int WRONG_PASSWORD = 0x02;

	public Ex2ndPasswordAck(int response)
	{
		_response = response;
	}

	@Override
	protected void writeImpl()
	{
		writeEx(0x10C);
		// writeEx(0x109); GOD
		writeC(0x00);
		writeD(_response);
		writeD(0x00);
	}
}
