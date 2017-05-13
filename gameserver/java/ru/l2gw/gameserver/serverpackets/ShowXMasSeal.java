package ru.l2gw.gameserver.serverpackets;

public class ShowXMasSeal extends L2GameServerPacket
{
	private int _item;

	public ShowXMasSeal(int item)
	{
		_item = item;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xF8);
		writeD(_item);
	}
}