package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.extensions.listeners.events.L2Zone.L2ZoneEnterLeaveEvent;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Player.TransactionType;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.*;

public class RequestAnswerJoinPledge extends L2GameClientPacket
{
	//Format: cd
	private int _response;

	@Override
	public void readImpl()
	{
		if(_buf.hasRemaining())
			_response = readD();
		else
			_response = 0;
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2Player requestor = player.getTransactionRequester();

		player.setTransactionRequester(null);

		if(requestor == null)
			return;

		requestor.setTransactionRequester(null);

		if(requestor.getClanId() == 0)
			return;

		if(player.getTransactionType() != TransactionType.CLAN || player.getTransactionType() != requestor.getTransactionType())
			return;

		if(_response == 1)
		{
			if(player.canJoinClan())
			{
				player.sendPacket(new JoinPledge(requestor.getClanId()));

				L2Clan clan = requestor.getClan();

				if(clan.getTerritoryId() > 0 && player.getTerritoryId() > 0)
				{
					TerritoryWarManager.removeMercRegistration(player.getTerritoryId(), player.getObjectId());
					player.setTerritoryId(clan.getTerritoryId());
				}

				clan.addClanMember(player);
				player.setClan(clan);
				clan.getClanMember(player.getName()).setPlayerInstance(player);

				if(clan.isAcademy(player.getPledgeType()))
					player.setLvlJoinedAcademy(player.getLevel());

				clan.getClanMember(player.getName()).setPowerGrade(clan.getAffiliationRank(player.getPledgeType()));

				player.store();

				SiegeUnit unit =  ResidenceManager.getInstance().getBuildingByObject(player);
				if(unit != null && (unit.getId() == clan.getHasCastle() || unit.getId() == clan.getHasFortress() || unit.getId() == clan.getHasHideout()))
				{
					L2Zone z = unit.getZone();
					z.getListenerEngine().fireMethodInvoked(new L2ZoneEnterLeaveEvent(MethodCollection.L2ZoneChanged, z, new L2Object[] { player }));
				}

				player.sendPacket(new SystemMessage(SystemMessage.ENTERED_THE_CLAN));
				clan.broadcastToOnlineMembers(new SystemMessage(SystemMessage.S1_HAS_JOINED_THE_CLAN).addString(player.getName()));
				clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListAdd(clan.getClanMember(player.getName())), player);
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));

				// this activates the clan tab on the new member
				player.sendPacket(new PledgeShowMemberListAll(clan, player));
				player.setLeaveClanTime(0);
				player.updatePledgeClass();
				clan.addAndShowSkillsToPlayer(player);
				player.broadcastUserInfo(true);
				player.broadcastRelation();
			}
			else
			{
				requestor.sendPacket(new SystemMessage(SystemMessage.AFTER_A_CLAN_MEMBER_IS_DISMISSED_FROM_A_CLAN_THE_CLAN_MUST_WAIT_AT_LEAST_A_DAY_BEFORE_ACCEPTING_A_NEW_MEMBER));
				player.sendPacket(new SystemMessage(SystemMessage.AFTER_LEAVING_OR_HAVING_BEEN_DISMISSED_FROM_A_CLAN_YOU_MUST_WAIT_AT_LEAST_A_DAY_BEFORE_JOINING_ANOTHER_CLAN));
				player.setPledgeType(0);
			}
		}
		else
		{
			requestor.sendPacket(new SystemMessage(SystemMessage.S1_REFUSED_TO_JOIN_THE_CLAN).addString(player.getName()));
			player.setPledgeType(0);
		}
	}
}
