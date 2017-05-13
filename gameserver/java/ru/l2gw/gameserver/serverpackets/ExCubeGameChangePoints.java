package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 16.12.10 16:33
 */
public class ExCubeGameChangePoints extends L2GameServerPacket
{
	int _timeLeft;
	int _bluePoints;
	int _redPoints;

	/**
	 * Change Client Point Counter
	 *
	 * @param timeLeft Time Left before Minigame's End
	 * @param bluePoints Current Blue Team Points
	 * @param redPoints Current Red Team Points
	 */
	public ExCubeGameChangePoints(int timeLeft, int bluePoints, int redPoints)
	{
		_timeLeft = timeLeft;
		_bluePoints = bluePoints;
		_redPoints = redPoints;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x98);
		writeD(0x02);

		writeD(_timeLeft);
		writeD(_bluePoints);
		writeD(_redPoints);
	}
}
