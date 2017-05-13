package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.serverpackets.ExShowFortressMapInfo;

/**
 * @author: rage
 * Date: 21.07.2009 19:18:34
 */
public class RequestFortressMapInfo extends L2GameClientPacket
{
	// format: chd
	private int _fortId;

	@Override
	public void readImpl()
	{
		if(_buf.hasRemaining())
			_fortId = readD();
		else
			_fortId = 0;
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null)
			return;
		if(_fortId > 0)
		{
			SiegeUnit fort = ResidenceManager.getInstance().getBuildingById(_fortId);
		
			if(fort != null && fort.isFort)
				player.sendPacket(new ExShowFortressMapInfo(fort));
		}
	}
}
