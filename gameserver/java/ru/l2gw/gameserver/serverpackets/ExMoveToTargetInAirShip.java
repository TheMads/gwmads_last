package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.util.Location;

public class ExMoveToTargetInAirShip extends L2GameServerPacket
{
	private int _charId, _airShipId, _targetId, _dist;
	private Location _loc;

	public ExMoveToTargetInAirShip(L2Player player, int targetId, int offset, int airShipId)
	{
		_charId = player.getObjectId();
		_loc = player.getLocInVehicle();
		_airShipId = airShipId;
		_targetId = targetId;
		_dist = offset;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x72);

		writeD(_charId); // ID:%d
		writeD(_targetId); // TargetID:%d
		writeD(_dist); //Dist:%d		
		writeD(_loc.getX()); //OriginX:%d
		writeD(_loc.getY()); //OriginY:%d
		writeD(_loc.getZ()); //OriginZ:%d
		writeD(_airShipId); //AirShipID:%d
	}

}