package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2ClanMember;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ClanTable;

public class RequestStopPledgeWar extends L2GameClientPacket
{
	String _pledgeName;

	@Override
	public void readImpl()
	{
		_pledgeName = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2Clan playerClan = player.getClan();
		if(playerClan == null)
			return;

		if(!((player.getClanPrivileges() & L2Clan.CP_CL_PLEDGE_WAR) == L2Clan.CP_CL_PLEDGE_WAR))
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			player.sendActionFailed();
			return;
		}

		L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);

		if(clan == null)
		{
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestStopPledgeWar.NoSuchClan", player));
			player.sendActionFailed();
			return;
		}

		if(!playerClan.isAtWarWith(clan.getClanId()))
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_NOT_DECLARED_A_CLAN_WAR_TO_S1_CLAN));
			player.sendActionFailed();
			return;
		}

		for(L2ClanMember mbr : playerClan.getMembers())
			if(mbr.isOnline() && mbr.getPlayer().isInCombat())
			{
				player.sendPacket(new SystemMessage(SystemMessage.A_CEASE_FIRE_DURING_A_CLAN_WAR_CAN_NOT_BE_CALLED_WHILE_MEMBERS_OF_YOUR_CLAN_ARE_ENGAGED_IN_BATTLE));
				player.sendActionFailed();
				return;
			}

		_log.info("RequestStopPledgeWar: By player: " + player.getName() + " of clan: " + playerClan.getName() + " to clan: " + _pledgeName);

		ClanTable.getInstance().stopClanWar(playerClan, clan);
	}
}
