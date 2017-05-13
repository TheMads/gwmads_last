package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.entity.vehicle.L2AirShipDock;
import ru.l2gw.gameserver.templates.StatsSet;

/**
 * @author rage
 * @date 08.09.2010 18:09:57
 */
public class ExAirShipTeleportList extends L2GameServerPacket
{
	private final L2AirShipDock _dock;

	public ExAirShipTeleportList(L2AirShipDock dock)
	{
		_dock = dock;
	}

	@Override
	protected void writeImpl()
	{
		if(_dock == null)
			return;

		writeC(EXTENDED_PACKET);
		writeH(0x9B);
		writeD(_dock.getClientDockId()); // Dock id
		writeD(_dock.getTeleports().size());

		for(StatsSet port : _dock.getTeleports())
		{
			writeD(port.getInteger("port_id", 0)); // AirportID
			writeD(port.getInteger("ep", 0)); // need fuel
			writeD(port.getInteger("x", 0)); // Airport x
			writeD(port.getInteger("y", 0)); // Airport y
			writeD(port.getInteger("z", 0)); // Airport z
		}
	}
}
