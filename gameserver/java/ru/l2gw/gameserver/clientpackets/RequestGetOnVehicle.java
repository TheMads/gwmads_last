package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.instancemanager.VehicleManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.gameserver.serverpackets.GetOnVehicle;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.util.Location;

public class RequestGetOnVehicle extends L2GameClientPacket
{
	private int _id;
	private Location _loc;

	/**
	 * format:      cdddd
	 */
	@Override
	public void readImpl()
	{
		_id = readD();
		_loc = new Location(readD(), readD(), readD());
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2Vehicle boat = VehicleManager.getInstance().getVehicleByObjectId(_id);
		if(boat == null)
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

		boat.addPlayerOnBoard(player);
		player.setVehicle(boat);
		player.setLocInVehicle(_loc);
		GetOnVehicle Gon = new GetOnVehicle(player, boat, _loc);
		player.broadcastPacket(Gon);
	}
}