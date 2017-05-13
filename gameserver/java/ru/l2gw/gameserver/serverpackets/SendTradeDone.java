package ru.l2gw.gameserver.serverpackets;

public class SendTradeDone extends L2GameServerPacket
{
	private int _num;

	public SendTradeDone(int num)
	{
		_num = num;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x1c);
		writeD(_num);
	}
}