package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 16.12.10 16:02
 */
public class ExChangeNpcState extends L2GameServerPacket
{
	private int _objId;
	private int _state;

	public ExChangeNpcState(int objId, int state)
	{
		_objId = objId;
		_state = state;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xBF);
		writeD(_objId);
		writeD(_state);
	}
}
