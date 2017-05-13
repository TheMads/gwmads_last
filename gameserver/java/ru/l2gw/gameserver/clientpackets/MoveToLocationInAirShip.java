package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.instancemanager.VehicleManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.util.Location;

/**
 * format: ddddddd
 * X:%d Y:%d Z:%d OriginX:%d OriginY:%d OriginZ:%d
 */
public class MoveToLocationInAirShip extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private int _shipId;
	private Location _pos;
	private Location _originPos;

	@Override
	protected void readImpl()
	{
		_shipId = readD();
		_pos = new Location(readD(), readD(), readD());
		_originPos = new Location(readD(), readD(), readD());
	}

	@Override
	protected void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		if(player.isMovementDisabled())
		{
			player.sendActionFailed();
			return;
		}

		L2Vehicle boat = VehicleManager.getInstance().getVehicleByObjectId(_shipId);

		if(boat == null)
		{
			player.sendActionFailed();
			return;
		}
		player.moveInVehicle(_pos, _originPos, boat);
	}
}
