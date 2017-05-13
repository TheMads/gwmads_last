package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Alliance;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ClanTable;

public class RequestDismissAlly extends L2GameClientPacket
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
		if(clan == null)
		{
			player.sendActionFailed();
			return;
		}

		L2Alliance alliance = clan.getAlliance();
		if(alliance == null)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_CURRENTLY_ALLIED_WITH_ANY_CLANS));
			return;
		}

		if(!player.isAllyLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessage.FEATURE_AVAILABLE_TO_ALLIANCE_LEADERS_ONLY));
			return;
		}

		if(alliance.getMembersCount() > 1)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_DISSOLVE_THE_ALLIANCE));
			return;
		}

		ClanTable.getInstance().dissolveAlly(player);
	}
}