package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 * Created with IntelliJ IDEA.
 * User: Darvin
 * Date: 01.09.12
 * Time: 17:06
 */
public class RequestExEscapeScene extends L2GameClientPacket
{
	protected void readImpl()
	{
	}

	protected void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
		{
			return;
		}
		player.sendActionFailed();
		player.setMovieId(0);
	}

	public String getType()
	{
		return "[C] D0:93 RequestExEscapeScene";
	}
}
