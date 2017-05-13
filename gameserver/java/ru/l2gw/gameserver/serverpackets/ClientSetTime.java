package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.controllers.GameTimeController;

public class ClientSetTime extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(0xF2);
		writeD(GameTimeController.getInstance().getGameTime() % (24 * 60)); // time in client minutes
		writeD(6); //constant to match the server time( this determines the speed of the client clock)
	}
}