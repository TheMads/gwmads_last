package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 * @author rage
 * @date 16.12.10 16:38
 */
public class ExCubeGameRemovePlayer extends L2GameServerPacket
{
	private L2Player _player;
	private boolean _isRedTeam;
	
	/**
	 * Remove Player from Minigame Waiting List
	 * 
	 * @param player: Player to Remove
	 * @param isRedTeam: Is Player from Red Team?
	 */
	public ExCubeGameRemovePlayer(L2Player player, boolean isRedTeam)
	{
		_player = player;
		_isRedTeam = isRedTeam;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x97);
		writeD(0x02);
		
		writeD(0xffffffff);
		
		writeD(_isRedTeam ? 0x01 : 0x00);
		writeD(_player.getObjectId());
	}
}
