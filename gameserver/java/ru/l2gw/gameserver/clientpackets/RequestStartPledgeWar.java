package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ClanTable;

public class RequestStartPledgeWar extends L2GameClientPacket
{
	//Format: cS
	String _pledgeName;
	L2Clan _clan;

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

		_clan = player.getClan();
		if(_clan == null)
		{
			player.sendActionFailed();
			return;
		}

		if(!((player.getClanPrivileges() & L2Clan.CP_CL_PLEDGE_WAR) == L2Clan.CP_CL_PLEDGE_WAR))
		{
			player.sendActionFailed();
			return;
		}

		if(_clan.getWarsCount() >= Config.AltClanWarMax)
		{
			player.sendPacket(new SystemMessage(SystemMessage.A_DECLARATION_OF_WAR_AGAINST_MORE_THAN_30_CLANS_CANT_BE_MADE_AT_THE_SAME_TIME));
			player.sendActionFailed();
			return;
		}

		if(_clan.getLevel() < Config.AltMinClanLvlForWar || _clan.getMembersCount() < Config.AltClanMembersForWar)
		{
			player.sendPacket(new SystemMessage(SystemMessage.A_CLAN_WAR_CAN_BE_DECLARED_ONLY_IF_THE_CLAN_IS_LEVEL_THREE_OR_ABOVE_AND_THE_NUMBER_OF_CLAN_MEMBERS_IS_FIFTEEN_OR_GREATER));
			player.sendActionFailed();
			return;
		}

		L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);
		if(clan == null)
		{
			player.sendPacket(new SystemMessage(SystemMessage.THE_DECLARATION_OF_WAR_CANT_BE_MADE_BECAUSE_THE_CLAN_DOES_NOT_EXIST_OR_ACT_FOR_A_LONG_PERIOD));
			player.sendActionFailed();
			return;
		}

		else if(_clan.equals(clan))
		{
			player.sendPacket(new SystemMessage(SystemMessage.FOOL_YOU_CANNOT_DECLARE_WAR_AGAINST_YOUR_OWN_CLAN));
			player.sendActionFailed();
			return;
		}

		else if(_clan.isAtWarWith(clan.getClanId()))
		{
			player.sendPacket(new SystemMessage(SystemMessage.THE_DECLARATION_OF_WAR_HAS_BEEN_ALREADY_MADE_TO_THE_CLAN));
			player.sendActionFailed();
			return;
		}

		else if(clan.getLevel() < Config.AltMinClanLvlForWar || clan.getMembersCount() < Config.AltClanMembersForWar)
		{
			player.sendPacket(new SystemMessage(SystemMessage.A_CLAN_WAR_CAN_BE_DECLARED_ONLY_IF_THE_CLAN_IS_LEVEL_THREE_OR_ABOVE_AND_THE_NUMBER_OF_CLAN_MEMBERS_IS_FIFTEEN_OR_GREATER));
			player.sendActionFailed();
			return;
		}

		_log.info("RequestStartPledgeWar: By player: " + player.getName() + " of clan: " + _clan.getName() + " to clan: " + _pledgeName);
		ClanTable.getInstance().startClanWar(player.getClan(), clan);
	}
}
