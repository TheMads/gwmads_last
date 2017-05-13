package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.PledgeInfo;
import ru.l2gw.gameserver.tables.ClanTable;

/**
 * This class ...
 *
 * @version $Revision: 1.5.4.3 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestPledgeInfo extends L2GameClientPacket
{
	private int _clanId;

	/**
	 * packet type id 0x65
	 * format:		cd
	 * @param rawPacket
	 */
	@Override
	public void readImpl()
	{
		_clanId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2Clan clan = ClanTable.getInstance().getClan(_clanId);
		if(clan == null)
			return; // we have no clan data ?!? should not happen

		player.sendPacket(new PledgeInfo(clan));
	}
}
