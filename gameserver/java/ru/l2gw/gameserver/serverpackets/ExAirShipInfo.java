package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.entity.vehicle.L2ClanAirship;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;

public class ExAirShipInfo extends L2GameServerPacket
{
	private final int _x, _y, _z, _heading, _objectId, _speed1, _speed2;
	private int _captainId, _helmId, _currentEp, _maxEp;

	public ExAirShipInfo(L2Vehicle vehicle)
	{
		_x = vehicle.getX();
		_y = vehicle.getY();
		_z = vehicle.getZ();
		_heading = vehicle.getHeading();
		_objectId = vehicle.getObjectId();
		_speed1 = (int) vehicle.getMoveSpeed();
		_speed2 = vehicle.getRotationSpeed();
		if(vehicle instanceof L2ClanAirship)
		{
			L2ClanAirship cas = (L2ClanAirship) vehicle;
			_captainId = cas.getCaptainObjectId();
			_helmId = cas.getHelmId();
			_currentEp = cas.getCurrentEp();
			_maxEp = 600;
		}
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x61);

		writeD(_objectId);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_heading);

		writeD(_captainId); // object id of player who control ship
		writeD(_speed1);
		writeD(_speed2);

		// clan airship related info
		writeD(_helmId); // owner object id?
		writeD(0x16e); // Controller X
		writeD(0x00); // Controller Y
		writeD(0x6b); // Controller Z
		writeD(0x15c); // Captain X
		writeD(0x00); // Captain Y
		writeD(0x69); // Captain Z
		writeD(_currentEp); // current fuel
		writeD(_maxEp); // max fuel
	}
}