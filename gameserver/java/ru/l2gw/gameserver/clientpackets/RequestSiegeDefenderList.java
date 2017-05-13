package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.serverpackets.CastleSiegeDefenderList;

public class RequestSiegeDefenderList extends L2GameClientPacket
{
	private int _CastleId;

	@Override
	public void readImpl()
	{
		_CastleId = readD();
	}

	@Override
	public void runImpl()
	{
		SiegeUnit unit = ResidenceManager.getInstance().getBuildingById(_CastleId);

		if(unit != null)
			sendPacket(new CastleSiegeDefenderList(unit));
	}
}