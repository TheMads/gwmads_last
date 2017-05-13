package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.PledgeReceiveWarList;

public class RequestPledgeWarList extends L2GameClientPacket
{
	// format: (ch)dd
	static int _type;
	private int _page;

	@Override
	public void readImpl()
	{
		_page = readD();
		_type = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		L2Clan clan = player.getClan();
		if(clan != null)
			player.sendPacket(new PledgeReceiveWarList(clan, _type, _page));
	}
}