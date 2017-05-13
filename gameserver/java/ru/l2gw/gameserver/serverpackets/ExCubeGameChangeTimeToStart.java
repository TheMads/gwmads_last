package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 16.12.10 16:35
 */
public class ExCubeGameChangeTimeToStart extends L2GameServerPacket
{
	private int _seconds;

	/**
	 * Update Minigame Waiting List Time to Start
	 *
	 * @param seconds
	 */
	public ExCubeGameChangeTimeToStart(int seconds)
	{
		_seconds = seconds;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x97);
		writeD(0x03);

		writeD(_seconds);
	}
}
