package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.AllianceInfo;

/**
 * This class ...
 *
 * @version $Revision: 1479 $ $Date: 2005-11-09 02:47:42 +0300 (Ср, 09 ноя 2005) $
 */
public class RequestAllyInfo extends L2GameClientPacket
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
		sendPacket(new AllianceInfo(player));
	}
}
