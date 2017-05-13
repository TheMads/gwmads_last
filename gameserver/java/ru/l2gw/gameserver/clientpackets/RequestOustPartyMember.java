package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 * This class ...
 *
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestOustPartyMember extends L2GameClientPacket
{
	//Format: cS
	private String _name;

	@Override
	public void readImpl()
	{
		_name = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(player.isInParty() && player.getParty().isLeader(player))
		{
			if(player.getParty().isInDimensionalRift())
				player.getParty().getDimensionalRift().oustMember(_name);
			player.getParty().oustPartyMember(_name);
		}
	}
}