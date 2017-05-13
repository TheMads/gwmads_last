package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;

public class RequestBR_LectureMark extends L2GameClientPacket
{
	private int mark;
	@Override
	protected void readImpl() throws Exception
	{
		mark = readC();
	}

	@Override
	protected void runImpl() throws Exception
	{
		L2Player player = getClient().getPlayer();
		System.out.println("RequestBR_LectureMark [" + mark + "] from " + player);
	}
}
