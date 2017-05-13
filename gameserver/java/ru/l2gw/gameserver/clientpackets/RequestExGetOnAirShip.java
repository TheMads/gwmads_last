package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.instancemanager.VehicleManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.gameserver.serverpackets.ExGetOnAirShip;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.util.Location;

public class RequestExGetOnAirShip extends L2GameClientPacket
{
	private Location _loc;
	private int _shipId;

	@Override
	protected void readImpl()
	{
		_loc = new Location(readD(), readD(), readD());
		_shipId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2Vehicle airship = VehicleManager.getInstance().getVehicleByObjectId(_shipId);
		if(airship == null)
			return;

		if(player.isPetSummoned())
			player.sendPacket(new SystemMessage(SystemMessage.BECAUSE_PET_OR_SERVITOR_MAY_BE_DROWNED_WHILE_THE_BOAT_MOVES_PLEASE_RELEASE_THE_SUMMON_BEFORE_DEPARTURE));

		player.stopMove();

		if(player.getTransformation() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_BOARD_A_SHIP_WHILE_YOU_ARE_POLYMORPHED));
			player.sendActionFailed();
			return;
		}

		airship.addPlayerOnBoard(player);
		player.setVehicle(airship);
		player.setLocInVehicle(_loc);
		player.broadcastPacket(new ExGetOnAirShip(player, airship, _loc));
	}
}
