package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;

public class VehicleStarted extends L2GameServerPacket
{
	private final int vehicleObjId;
	private int _state;

	public VehicleStarted(L2Vehicle vehicle, int state)
	{
		vehicleObjId = vehicle.getObjectId();
		_state = state;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xC0);
		writeD(vehicleObjId);
		writeD(_state);
	}
}
