package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.instancemanager.VehicleManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.util.Location;

public class RequestMoveToLocationInVehicle extends L2GameClientPacket
{
	private Location _pos = new Location(0, 0, 0);
	private Location _originPos = new Location(0, 0, 0);
	private int _boatId;

	/**
	 * format: cddddddd
	 */
	@Override
	public void readImpl()
	{
		_boatId = readD(); //objectId of boat
		_pos.setX(readD());
		_pos.setY(readD());
		_pos.setZ(readD());
		_originPos.setX(readD());
		_originPos.setY(readD());
		_originPos.setZ(readD());
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(player.isMovementDisabled())
		{
			player.sendActionFailed();
			return;
		}

		L2Vehicle boat = VehicleManager.getInstance().getVehicleByObjectId(_boatId);

		if(boat == null)
		{
			player.sendActionFailed();
			return;
		}
		player.moveInVehicle(_pos, _originPos, boat);
	}
}