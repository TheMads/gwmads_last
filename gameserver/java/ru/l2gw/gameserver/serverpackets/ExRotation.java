package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 16.12.10 17:41
 */
public class ExRotation extends L2GameServerPacket
{
	private int _charObjId, _degree;

	public ExRotation(int charId, int degree)
	{
		_charObjId = charId;
		_degree = degree;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xC2);
		writeD(_charObjId);
		writeD(_degree);
	}
}
