package ru.l2gw.gameserver.serverpackets;

public class ExAskCoupleAction extends L2GameServerPacket
{
	private int _charObjId;
	private int _actionId;
	
	public ExAskCoupleAction(int charObjId, int social)
	{
		_charObjId = charObjId;
		_actionId = social;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xBC);
		writeD(_actionId);
		writeD(_charObjId);
	}
}
