package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 16.12.10 16:40
 */
public class ExCubeGameTeamList extends L2GameServerPacket
{
	// Players Lists
	private GArray<L2Player> _bluePlayers;
	private GArray<L2Player> _redPlayers;
	
	// Common Values
	int _roomNumber;
	
	/**
	 * 
	 * Show Minigame Waiting List to Player
	 * 
	 * @param redPlayers Red Players List
	 * @param bluePlayers Blue Players List
	 * @param roomNumber Arena/Room ID
	 */
	public ExCubeGameTeamList(GArray<L2Player> redPlayers, GArray<L2Player> bluePlayers, int roomNumber)
	{
		_redPlayers = redPlayers;
		_bluePlayers = bluePlayers;
		_roomNumber = roomNumber - 1;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x97);
		writeD(0x00);
		
		writeD(_roomNumber);
		writeD(0xffffffff);
		
		writeD(_bluePlayers.size());
		for (L2Player player : _bluePlayers)
		{
			writeD(player.getObjectId());
			writeS(player.getName());
		}
		writeD(_redPlayers.size());
		for (L2Player player : _redPlayers)
		{
			writeD(player.getObjectId());
			writeS(player.getName());
		}
	}
}
