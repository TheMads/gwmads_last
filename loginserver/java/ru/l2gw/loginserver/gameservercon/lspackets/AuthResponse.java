package ru.l2gw.loginserver.gameservercon.lspackets;

import ru.l2gw.loginserver.Config;
import ru.l2gw.loginserver.GameServerTable;

public class AuthResponse extends ServerBasePacket
{
	public AuthResponse(int serverId)
	{
		writeC(0x02);
		writeC(serverId);
		writeS(GameServerTable.getInstance().getServerNameById(serverId));
		writeC(Config.SHOW_LICENCE ? 0 : 1);
	}
}