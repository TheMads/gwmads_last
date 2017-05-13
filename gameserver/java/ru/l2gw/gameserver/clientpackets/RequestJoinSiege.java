package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.Castle;
import ru.l2gw.gameserver.model.entity.ClanHall;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.serverpackets.CastleSiegeAttackerList;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

import java.util.Calendar;

public class RequestJoinSiege extends L2GameClientPacket
{
	// format: cddd
	private int _id;
	private boolean _isAttacker;
	private boolean _isJoining;

	@Override
	public void readImpl()
	{
		_id = readD();
		_isAttacker = readD() == 1;
		_isJoining = readD() == 1;
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null || player.getClanId() == 0)
			return;

		if((player.getClanPrivileges() & L2Clan.CP_CS_MANAGE_SIEGE) != L2Clan.CP_CS_MANAGE_SIEGE)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}

		SiegeUnit siegeUnit = ResidenceManager.getInstance().getBuildingById(_id);

		if(siegeUnit != null)
		{
			if(_isJoining)
			{
				if(siegeUnit.isClanHall)
				{
					if(player.getClan().getHasUnit(1) && !ResidenceManager.getInstance().getClanHallById(player.getClan().getHasHideout()).isSieaged())
					{
						player.sendPacket(new SystemMessage(SystemMessage.A_CLAN_THAT_OWNS_A_CLAN_HALL_MAY_NOT_PARTICIPATE_IN_A_CLAN_HALL_SIEGE));
						return;
					}

					for(ClanHall ch : ResidenceManager.getInstance().getClanHallList())
						if(ch.getSiege() != null && ch.getSiege().checkIsClanRegistered(player.getClanId()))
						{
							player.sendPacket(new SystemMessage(SystemMessage.YOUR_APPLICATION_HAS_BEEN_DENIED_BECAUSE_YOU_HAVE_ALREADY_SUBMITTED_A_REQUEST_FOR_ANOTHER_SIEGE_BATTLE));
							return;
						}
				}
				else if(siegeUnit.isCastle)
				{
					for(Castle cast : ResidenceManager.getInstance().getCastleList())
						if(cast.getSiege().getSiegeDate().get(Calendar.DAY_OF_MONTH) == siegeUnit.getSiege().getSiegeDate().get(Calendar.DAY_OF_MONTH) && cast.getSiege().getSiegeDate().get(Calendar.HOUR_OF_DAY) == siegeUnit.getSiege().getSiegeDate().get(Calendar.HOUR_OF_DAY) && cast.getId() != siegeUnit.getId())
						{
							if(cast.getSiege().checkIsDefender(player.getClanId()) || cast.getSiege().checkIsAttacker(player.getClanId()) || cast.getSiege().checkIsDefenderWaiting(player.getClanId()))
							{
								player.sendPacket(new SystemMessage(SystemMessage.YOUR_APPLICATION_HAS_BEEN_DENIED_BECAUSE_YOU_HAVE_ALREADY_SUBMITTED_A_REQUEST_FOR_ANOTHER_SIEGE_BATTLE));
								return;
							}
						}

					if(_isAttacker)
					{
						if(siegeUnit.getSiege().checkIsDefender(player.getClanId()))
						{
							player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_ALREADY_REGISTERED_TO_THE_DEFENDER_SIDE_AND_MUST_CANCEL_YOUR_REGISTRATION_BEFORE_SUBMITTING_YOUR_REQUEST));
							return;
						}

						if(player.getClan().getHasUnit(3) && ResidenceManager.getInstance().getBuildingById(player.getClan().getHasFortress()).getContractCastleId() == siegeUnit.getId())
						{
							player.sendPacket(new SystemMessage(SystemMessage.SIEGE_REGISTRATION_IS_NOT_POSSIBLE_DUE_TO_A_CONTRACT_WITH_A_HIGHER_CASTLE));
							return;
						}
					}
					else
					{
						if(siegeUnit.getSiege().checkIsAttacker(player.getClanId()))
						{
							player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_ALREADY_REGISTERED_TO_THE_ATTACKER_SIDE_AND_MUST_CANCEL_YOUR_REGISTRATION_BEFORE_SUBMITTING_YOUR_REQUEST));
							return;
						}

						if(player.getClan().getHasUnit(3) && ResidenceManager.getInstance().getBuildingById(player.getClan().getHasFortress()).getContractCastleId() == siegeUnit.getId())
						{
							player.sendPacket(new SystemMessage(SystemMessage.IT_IS_NOT_POSSIBLE_TO_REGISTER_FOR_THE_CASTLE_SIEGE_SIDE_OR_CASTLE_SIEGE_OF_A_HIGHER_CASTLE_IN_THE_CONTRACT));
							return;
						}
					}
				}

				if(_isAttacker || siegeUnit.isClanHall)
					siegeUnit.getSiege().registerAttacker(player, false);
				else
					siegeUnit.getSiege().registerDefender(player, false);
			}
			else
				siegeUnit.getSiege().removeSiegeClan(player.getClanId());

			siegeUnit.getSiege().listRegisterClan(player);

			if(siegeUnit.isClanHall)
				player.sendPacket(new CastleSiegeAttackerList(siegeUnit));
		}
	}
}