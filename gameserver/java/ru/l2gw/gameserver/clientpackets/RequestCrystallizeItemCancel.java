package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 * @author ALF
 * @date 21.08.2012
 */
public class RequestCrystallizeItemCancel extends L2GameClientPacket
{

	@Override
	protected void readImpl() throws Exception
	{

	}

	@Override
	protected void runImpl() throws Exception
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
		{
			return;
		}

		player.sendActionFailed();
	}

}
