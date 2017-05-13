package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.Fortress;
import ru.l2gw.gameserver.serverpackets.ExShowFortressSiegeInfo;

/**
 * @author: rage
 * @date: 21.07.2009 19:24:52
 */
public class RequestFortressSiegeInfo extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		for(Fortress fort : ResidenceManager.getInstance().getFortressList())
			if(fort.getSiege().isInProgress())
				player.sendPacket(new ExShowFortressSiegeInfo(fort));
	}
}
