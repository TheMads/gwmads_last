package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.PledgeShowMemberListAll;

public class RequestPledgeMemberList extends L2GameClientPacket
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
		L2Clan clan = player.getClan();

		if(clan != null)
			player.sendPacket(new PledgeShowMemberListAll(clan, player));
	}
}
