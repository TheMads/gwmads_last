package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2ClanMember;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.ExReplyRegisterDominion;
import ru.l2gw.gameserver.serverpackets.PledgeShowInfoUpdate;

// ddd
public class RequestExJoinDominionWar extends L2GameClientPacket
{
	private int _terrId;
	private int _registrationType; // 0 - merc; 1 - clan
	private int _requestType; // 1 - регистрация; 0 - отмена регистрации

	@Override
	public void readImpl()
	{
		_terrId = readD();
		_registrationType = readD();
		_requestType = readD();
	}
	
	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		if(TerritoryWarManager.getWar().isRegistrationOver() || TerritoryWarManager.getWar().isInProgress())
		{
			player.sendPacket(Msg.IT_IS_NOT_A_TERRITORY_WAR_REGISTRATION_PERIOD_SO_A_REQUEST_CANNOT_BE_MADE_AT_THIS_TIME);
			return;
		}

		if(player.getLevel() < 40 || player.getClassId().getLevel() < 3)
		{
			player.sendPacket(Msg.ONLY_CHARACTERS_WHO_ARE_LEVEL_40_OR_ABOVE_WHO_HAVE_COMPLETED_THEIR_SECOND_CLASS_TRANSFER);
			return;
		}

		// Персональная регистрация
		if(_registrationType == 0)
		{
			int terrId = TerritoryWarManager.getMercRegisteredTerritoryId(player.getObjectId());
			if(_requestType == 1)
			{
				if(terrId != 0 || player.getClanId() > 0 && TerritoryWarManager.getClanRegisteredTerritoryId(player.getClanId()) > 0)
				{
					player.sendPacket(Msg.YOUVE_ALREADY_REQUESTED_A_TERRITORY_WAR_IN_ANOTHER_TERRITORY_ELSEWHERE);
					return;
				}
				terrId = _terrId;
				TerritoryWarManager.addMercRegistration(_terrId, player.getObjectId());
				player.setTerritoryId(_terrId);
			}
			else
			{
				TerritoryWarManager.removeMercRegistration(_terrId, player.getObjectId());
				player.setTerritoryId(0);
			}

			if(terrId == _terrId)
				player.sendPacket(new ExReplyRegisterDominion(_terrId, _registrationType, _requestType));
		}
		else
		{
			L2Clan clan = player.getClan();

			// Клановая регистрация
			if(clan == null || (player.getClanPrivileges() & L2Clan.CP_CS_MANAGE_SIEGE) != L2Clan.CP_CS_MANAGE_SIEGE)
			{
				player.sendPacket(Msg.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
				return;
			}

			if(clan.getHasCastle() > 0)
			{
				player.sendPacket(Msg.THE_CLAN_WHO_OWNS_THE_TERRITORY_CANNOT_PARTICIPATE_IN_THE_TERRITORY_WAR_AS_MERCENARIES);
				return;
			}

			int clanTerrId = TerritoryWarManager.getClanRegisteredTerritoryId(clan.getClanId());

			if(_requestType == 1)
			{
				if(clanTerrId != 0)
				{
					player.sendPacket(Msg.YOUVE_ALREADY_REQUESTED_A_TERRITORY_WAR_IN_ANOTHER_TERRITORY_ELSEWHERE);
					return;
				}
				clanTerrId = _terrId;
				clan.setTerritoryId(_terrId);
				TerritoryWarManager.addClanRegistration(_terrId, clan.getClanId());

				for(L2ClanMember member : clan.getMembers())
				{
					int memberTerr = TerritoryWarManager.getMercRegisteredTerritoryId(member.getObjectId());
					if(memberTerr > 0)
						TerritoryWarManager.removeMercRegistration(memberTerr, member.getObjectId());
					L2Player p = member.getPlayer();
					if(p != null)
						p.setTerritoryId(_terrId);
				}
			}
			else
			{
				clan.setTerritoryId(0);
				TerritoryWarManager.removeClanRegistration(_terrId, clan.getClanId());
			}

			if(clanTerrId == _terrId)
			{
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				player.sendPacket(new ExReplyRegisterDominion(_terrId, _registrationType, _requestType));
			}
		}
	}
}