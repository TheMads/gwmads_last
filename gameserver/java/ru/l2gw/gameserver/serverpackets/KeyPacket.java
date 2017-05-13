package ru.l2gw.gameserver.serverpackets;

public class KeyPacket extends L2GameServerPacket
{
	private byte[] _key;

	public KeyPacket(byte[] key)
	{
		_key = key;
	}

	@Override
	public void writeImpl()
	{
		writeC(0x2e);
		if(_key == null)
		{
			writeC(0x00);
			return;
		}
		writeC(0x01);
		writeB(_key);
		writeD(0x01);
	}
}