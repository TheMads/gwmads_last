package ru.l2gw.gameserver.serverpackets;

/**
 *	sample
 *	0000: 85 00 00 00 00 f0 1a 00 00
 */
public class SetupGauge extends L2GameServerPacket
{
	public static final int BLUE_DUAL = 0;
	public static final int BLUE = 1;
	public static final int BLUE_MINI = 2;
	public static final int GREEN = 3;
	public static final int RED = 4;

	private int _color;
	private int _timeTotal;
	private int _timeLeft;
	private int _objectId;
	
	public static enum Colors
	{
		BLUE_DUAL, BLUE, BLUE_MINI, GREEN, RED_MINI
	}

	public SetupGauge(int color, int time)
	{
		_color = color;// color  0-blue   1-red  2-cyan  3-green
		_timeTotal = time;
		_timeLeft = time;
	}

	public SetupGauge(int color, int time, int timeLeft)
	{
		_color = color;// color  0-blue   1-red  2-cyan  3-green
		_timeTotal = time;
		_timeLeft = timeLeft;
	}

	@Override
	public void runImpl()
	{
		_objectId = getClient().getPlayer().getObjectId();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x6B);
		writeD(_objectId);
		writeD(_color);
		writeD(_timeLeft);
		writeD(_timeTotal); //c2
	}
}