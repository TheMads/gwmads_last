package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 16.12.10 17:46
 */
public class ExSetPartyLooting extends L2GameServerPacket
{
	private int _result;
	private byte _mode;

	public ExSetPartyLooting(int result, byte mode)
	{
		_result = result;
		_mode = mode;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xC1);
		writeD(_result);
		writeD(_mode);
	}
}
