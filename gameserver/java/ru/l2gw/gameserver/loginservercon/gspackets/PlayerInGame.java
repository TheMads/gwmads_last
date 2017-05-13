package ru.l2gw.gameserver.loginservercon.gspackets;

import javolution.util.FastList;

public class PlayerInGame extends GameServerBasePacket
{
	public PlayerInGame(String player)
	{
		writeC(0x02);
		writeH(1);
		writeS(player);
	}

	public PlayerInGame(FastList<String> players)
	{
		writeC(0x02);
		writeH(players.size());
		for(String pc : players)
			writeS(pc);
	}
}