package ru.l2gw.gameserver.clientpackets;

/**
 * Format: (ch)
 * just a trigger
 * @author  -Wooden-
 */
public class RequestExFishRanking extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		_log.info("Requested: " + getType());
	}
}