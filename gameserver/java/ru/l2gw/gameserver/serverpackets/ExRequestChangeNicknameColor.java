package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 21.06.2010 10:32:22
 */
public class ExRequestChangeNicknameColor extends L2GameServerPacket
{
	private int _itemObjectId;

	public ExRequestChangeNicknameColor(int itemObjectId)
	{
		_itemObjectId = itemObjectId;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x83);
		writeD(_itemObjectId);
	}
}