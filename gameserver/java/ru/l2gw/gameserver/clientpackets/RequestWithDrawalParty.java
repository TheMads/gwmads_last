package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 *
 * This class ...
 *
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestWithDrawalParty extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;
		if(player.isInParty())
		{
			if(player.getParty().isInDimensionalRift())
				player.getParty().getDimensionalRift().oustMember(player.getName());

			player.getParty().oustPartyMember(player);
		}
	}
}
