package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.extensions.listeners.events.L2Zone.L2ZoneEnterLeaveEvent;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2ClanMember;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.PledgeShowMemberListDelete;
import ru.l2gw.gameserver.serverpackets.PledgeShowMemberListDeleteAll;
import ru.l2gw.gameserver.serverpackets.SkillList;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestWithdrawalPledge extends L2GameClientPacket
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

		//is the guy in a clan  ?
		if(player.getClanId() == 0)
		{
			player.sendActionFailed();
			return;
		}

		if(player.isInCombat())
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONE_CANNOT_LEAVE_ONES_CLAN_DURING_COMBAT));
			return;
		}

		L2Clan clan = player.getClan();
		if(clan == null)
			return;

		L2ClanMember member = clan.getClanMember(player.getObjectId());
		if(member == null)
		{
			player.sendActionFailed();
			return;
		}

		if(TerritoryWarManager.getWar().isInProgress() && player.getTerritoryId() > 0)
		{
			player.sendPacket(Msg.THIS_CLAN_MEMBER_CANNOT_WITHDRAW_OR_BE_EXPELLED_WHILE_PARTICIPATING_IN_A_TERRITORY_WAR);
			return;
		}

		// this also updated the database
		clan.removeClanMember(player.getObjectId());

		//player withdrawed.
		clan.broadcastToOnlineMembers(new SystemMessage(SystemMessage.S1_HAS_WITHDRAWN_FROM_THE_CLAN).addString(player.getName()));

		// Remove the Player From the Member list
		clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(player.getName()));

		player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_RECENTLY_BEEN_DISMISSED_FROM_A_CLAN_YOU_ARE_NOT_ALLOWED_TO_JOIN_ANOTHER_CLAN_FOR_24_HOURS));

		player.setClan(null);

		if(!player.isNoble())
			player.setTitle("");

		player.setLeaveClanCurTime();
		player.unEquipInappropriateItems();

		SiegeUnit unit = null;
		if(clan.getHasUnit(2))
		{
			unit = ResidenceManager.getInstance().getBuildingById(clan.getHasCastle());
			if(unit != null)
			{
				unit.removeSkills(player);
				TerritoryWarManager.getTerritoryById(clan.getHasCastle() + 80).removeSkills(player);
			}
			player.sendPacket(new SkillList(player));
		}
		if(clan.getHasUnit(3))
		{
			unit = ResidenceManager.getInstance().getBuildingById(clan.getHasFortress());
			if(unit != null)
				unit.removeSkills(player);
			player.sendPacket(new SkillList(player));
		}

		unit =  ResidenceManager.getInstance().getBuildingByObject(player);
		if(unit != null && (unit.getId() == clan.getHasCastle() || unit.getId() == clan.getHasFortress() || unit.getId() == clan.getHasHideout()))
		{
			L2Zone z = unit.getZone();
			z.getListenerEngine().fireMethodInvoked(new L2ZoneEnterLeaveEvent(MethodCollection.L2ZoneChanged, z, new L2Object[] { player }));
		}

		player.broadcastUserInfo(true);
		player.broadcastRelation();

		// disable clan tab
		player.sendPacket(new PledgeShowMemberListDeleteAll());
	}
}