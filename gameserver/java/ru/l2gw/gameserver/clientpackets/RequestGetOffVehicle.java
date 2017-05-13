package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.gameserver.serverpackets.GetOffVehicle;
import ru.l2gw.util.Location;
import ru.l2gw.util.Util;

/**
 * @author Maktakien
 *
 */
public class RequestGetOffVehicle extends L2GameClientPacket
{
	// Format: cdddd
	private int _id;
	Location loc;

	@Override
	public void readImpl()
	{
		_id = readD();
		loc = new Location(readD(), readD(), readD());
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2Vehicle vehicle = player.getVehicle();
		if(vehicle == null || vehicle.getObjectId() != _id)
		{
			player.sendActionFailed();
			return;
		}

		if(player.isInRange(loc, 150))
		{
			loc = Util.getPointInRadius(loc, 16, Util.convertHeadingToDegree(player.getHeading()));
			player.setXYZ(loc.getX(), loc.getY(), loc.getZ(), false);
		}

		player.stopMove();
		vehicle.removePlayerFromBoard(player);
		player.setVehicle(null);

		player.broadcastPacket(new GetOffVehicle(player, vehicle, loc));
	}
}