package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.SiegeDatabase;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.serverpackets.CastleSiegeDefenderList;
import ru.l2gw.gameserver.tables.ClanTable;

public class RequestConfirmSiegeWaitingList extends L2GameClientPacket
{
	// format: cddd
	private int _Approved;
	private int _CastleId;
	private int _ClanId;

	@Override
	public void readImpl()
	{
		_CastleId = readD();
		_ClanId = readD();
		_Approved = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		// Check if the player has a clan
		if(player.getClanId() == 0)
			return;

		SiegeUnit castle = ResidenceManager.getInstance().getBuildingById(_CastleId);
		if(castle == null || !castle.isCastle)
			return;

		// Check if leader of the clan who owns the castle?
		if(castle.getOwnerId() != player.getClanId() || !player.isClanLeader())
			return;

		L2Clan clan = ClanTable.getInstance().getClan(_ClanId);
		if(clan == null)
			return;

		if(!castle.getSiege().isRegistrationOver())
			if(_Approved == 1)
				if(castle.getSiege().checkIsDefenderWaiting(_ClanId))
					castle.getSiege().approveSiegeDefenderClan(_ClanId);
				else
					return;
			else if(castle.getSiege().checkIsDefenderWaiting(_ClanId) || castle.getSiege().checkIsDefender(_ClanId))
				SiegeDatabase.removeSiegeClan(_ClanId, castle.getSiege());

		//Update the defender list
		player.sendPacket(new CastleSiegeDefenderList(castle));

	}
}
