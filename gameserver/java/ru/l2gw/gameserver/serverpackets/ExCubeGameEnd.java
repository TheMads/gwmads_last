package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 16.12.10 16:37
 */
public class ExCubeGameEnd extends L2GameServerPacket
{
	private boolean _isRedTeamWin;

	/**
	 * Show Minigame Results
	 *
	 * @param isRedTeamWin: Is Red Team Winner?
	 */
	public ExCubeGameEnd(boolean isRedTeamWin)
	{
		_isRedTeamWin = isRedTeamWin;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x98);
		writeD(0x01);
		writeD(_isRedTeamWin ? 0x01 : 0x00);
	}
}
