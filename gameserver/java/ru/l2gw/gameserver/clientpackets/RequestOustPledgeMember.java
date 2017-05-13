package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.extensions.listeners.events.L2Zone.L2ZoneEnterLeaveEvent;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.PledgeShowMemberListDelete;
import ru.l2gw.gameserver.serverpackets.PledgeShowMemberListDeleteAll;
import ru.l2gw.gameserver.serverpackets.SkillList;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestOustPledgeMember extends L2GameClientPacket
{
	//Format: cS
	private String _target;

	@Override
	public void readImpl()
	{
		_target = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null || !((player.getClanPrivileges() & L2Clan.CP_CL_DISMISS) == L2Clan.CP_CL_DISMISS))
			return;

		L2Clan clan = player.getClan();
		L2ClanMember member = clan.getClanMember(_target);
		if(member == null)
		{
			_log.warn(player + " requested oust member " + _target + ", but there is no such member in the clan " + clan);
			return;
		}

		if(member.isOnline() && member.getPlayer().isInCombat())
		{
			player.sendPacket(new SystemMessage(SystemMessage.A_CLAN_MEMBER_MAY_NOT_BE_DISMISSED_DURING_COMBAT));
			return;
		}

		if(TerritoryWarManager.getWar().isInProgress() && clan.getTerritoryId() > 0)
		{
			player.sendPacket(Msg.THIS_CLAN_MEMBER_CANNOT_WITHDRAW_OR_BE_EXPELLED_WHILE_PARTICIPATING_IN_A_TERRITORY_WAR);
			return;
		}

		clan.removeClanMember(_target);
		clan.broadcastToOnlineMembers(new SystemMessage(SystemMessage.CLAN_MEMBER_S1_HAS_BEEN_EXPELLED).addString(_target));
		clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(_target));
		if(member.getPledgeType() != L2Clan.SUBUNIT_ACADEMY)
			clan.setExpelledMember();

		if(member.isOnline())
		{
			L2Player player1 = member.getPlayer();
			player1.setClan(null);
			if(!player1.isNoble())
				player1.setTitle("");
			player1.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_RECENTLY_BEEN_DISMISSED_FROM_A_CLAN_YOU_ARE_NOT_ALLOWED_TO_JOIN_ANOTHER_CLAN_FOR_24_HOURS));
			player1.setLeaveClanCurTime();
			player1.unEquipInappropriateItems();
			player1.broadcastUserInfo(true);
			player1.broadcastRelation();

			if(clan.getHasUnit(2) && ResidenceManager.getInstance().getBuildingById(clan.getHasCastle()) != null)
			{
				ResidenceManager.getInstance().getBuildingById(clan.getHasCastle()).removeSkills(player1);
				TerritoryWarManager.getTerritoryById(clan.getHasCastle() + 80).removeSkills(player1);
				player1.sendPacket(new SkillList(player1));
			}
			if(clan.getHasUnit(3) && ResidenceManager.getInstance().getBuildingById(clan.getHasFortress()) != null)
			{
				ResidenceManager.getInstance().getBuildingById(clan.getHasFortress()).removeSkills(player1);
				player1.sendPacket(new SkillList(player1));
			}

			SiegeUnit unit =  ResidenceManager.getInstance().getBuildingByObject(player1);
			if(unit != null && (unit.getId() == clan.getHasCastle() || unit.getId() == clan.getHasFortress() || unit.getId() == clan.getHasHideout()))
			{
				L2Zone z = unit.getZone();
				z.getListenerEngine().fireMethodInvoked(new L2ZoneEnterLeaveEvent(MethodCollection.L2ZoneChanged, z, new L2Object[] { player }));
			}

			// disable clan tab
			player1.sendPacket(new PledgeShowMemberListDeleteAll());
		}
		else if(Config.SERVICES_OFFLINE_TRADE_ALLOW)
		{
			L2Player clanMember = L2ObjectsStorage.getPlayer(_target);
			if(clanMember != null && clanMember.isInOfflineMode())
			{
				clanMember.setClan(null);
				if(!clanMember.isNoble())
					clanMember.setTitle("");

				clanMember.setLeaveClanCurTime();
				clanMember.unEquipInappropriateItems();
				clanMember.broadcastUserInfo();
			}
		}
	}
}
