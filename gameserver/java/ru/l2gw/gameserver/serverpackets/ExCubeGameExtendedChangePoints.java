package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 * @author rage
 * @date 16.12.10 16:37
 */
public class ExCubeGameExtendedChangePoints extends L2GameServerPacket
{
	private int _timeLeft;
	private int _bluePoints;
	private int _redPoints;
	private boolean _isRedTeam;
	private L2Player _player;
	private int _playerPoints;
	
	/**
	 * Update a Secret Point Counter (used by client when receive ExCubeGameEnd)
	 * 
	 * @param timeLeft Time Left before Minigame's End
	 * @param bluePoints Current Blue Team Points
	 * @param redPoints Current Blue Team points
	 * @param isRedTeam Is Player from Red Team?
	 * @param player Player Instance
	 * @param playerPoints Current Player Points
	 */
	public ExCubeGameExtendedChangePoints(int timeLeft, int bluePoints, int redPoints, boolean isRedTeam, L2Player player, int playerPoints)
	{
		_timeLeft = timeLeft;
		_bluePoints = bluePoints;
		_redPoints = redPoints;
		_isRedTeam = isRedTeam;
		_player = player;
		_playerPoints = playerPoints;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x98);
		writeD(0x00);
		
		writeD(_timeLeft);
		writeD(_bluePoints);
		writeD(_redPoints);
		
		writeD(_isRedTeam ? 0x01 : 0x00);
		writeD(_player.getObjectId());
		writeD(_playerPoints);
	}
}
