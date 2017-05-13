package ru.l2gw.gameserver.loginservercon.gspackets;

import ru.l2gw.gameserver.network.GameClient;

public class PlayerAuthRequest extends GameServerBasePacket
{
	public PlayerAuthRequest(GameClient client)
	{
		writeC(0x05);
		writeS(client.getLoginName());
		writeD(client.getSessionId().playOkID1);
		writeD(client.getSessionId().playOkID2);
		writeD(client.getSessionId().loginOkID1);
		writeD(client.getSessionId().loginOkID2);
	}
}