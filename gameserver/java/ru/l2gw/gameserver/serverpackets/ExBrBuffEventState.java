package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 16.12.10 15:20
 */
public class ExBrBuffEventState extends L2GameServerPacket
{
	private int _type; // 1 - %, 2 - npcId
	private int _value; // depending on type: for type 1 - % value; for type 2 - 20573-20575
	private int _state; // 0-1
	private int _endtime; // only when type 2 as unix time in seconds from 1970

	public ExBrBuffEventState(int type, int value, int state, int endtime)
	{
		_type = type;
		_value = value;
		_state = state;
		_endtime = endtime;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xDB);
		writeD(_type);
		writeD(_value);
		writeD(_state);
		writeD(_endtime);
	}
}
