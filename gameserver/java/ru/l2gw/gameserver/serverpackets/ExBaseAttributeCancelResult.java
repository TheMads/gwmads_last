package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 16.12.10 15:17
 */
public class ExBaseAttributeCancelResult extends L2GameServerPacket
{
	private int _objId;
	private byte _attribute;

	public ExBaseAttributeCancelResult(int objId, byte attribute)
	{
		_objId = objId;
		_attribute = attribute;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x76);
		writeD(1); // result
		writeD(_objId);
		writeD(_attribute);
	}
}
