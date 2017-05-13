package ru.l2gw.gameserver.serverpackets;

/**
 * @author : Ragnarok
 * @date : 28.03.12  16:23
 */
public class ExCallToChangeClass extends L2GameServerPacket
{
	private int _classId;
	private boolean _showMsg;

	public ExCallToChangeClass(int classId, boolean showMsg)
	{
		_classId = classId;
		_showMsg = showMsg;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0xFE);
		writeD(_classId); // New Class Id
		writeD((_showMsg)? 0x00:0x01); // Show Message
	}
}