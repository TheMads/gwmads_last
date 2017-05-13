package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.serverpackets.ExBrLoadEventTopRankers;

/**
 * @author rage
 * @date 17.12.10 0:30
 */
public class RequestExBR_EventRankerList extends L2GameClientPacket
{
	private int _eventId;
	private int _day;
	@SuppressWarnings("unused")
	private int _ranking;

	@Override
	protected void readImpl()
	{
		_eventId = readD();
		_day = readD(); // 0 - current, 1 - previous
		_ranking = readD();
	}

	@Override
	protected void runImpl()
	{
		// TODO count, bestScore, myScore
		int count = 0;
		int bestScore = 0;
		int myScore = 0;
		getClient().sendPacket(new ExBrLoadEventTopRankers(_eventId, _day, count, bestScore, myScore));
	}
}