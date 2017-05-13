package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.util.Location;

public class ExMoveToLocationInAirShip extends L2GameServerPacket
{
	private int _charObjId;
	private int _airShipId, _heading;
	private Location _destination, _origin;

	/**
	 * @param actor
	 * @param destination
	 * @param origin
	 */
	public ExMoveToLocationInAirShip(L2Player actor, int airShipId, Location destination, Location origin)
	{
		_charObjId = actor.getObjectId();
		_airShipId = airShipId;
		_destination = destination;
		_origin = origin;
		_heading = actor.getHeading();
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x6E);
		writeD(_charObjId);
		writeD(_airShipId);
		writeD(_destination.getX());
		writeD(_destination.getY());
		writeD(_destination.getZ());
		writeD(_heading);
		//writeD(_origin.getY());
		//writeD(_origin.getZ());
	}
}
