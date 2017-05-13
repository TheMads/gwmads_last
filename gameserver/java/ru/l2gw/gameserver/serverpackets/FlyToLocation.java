package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.util.Location;

public class FlyToLocation extends L2GameServerPacket
{
	private int _chaObjId;
	private final FlyType _type;
	private Location _loc;
	private Location _destLoc;
	private int _speed;

	public enum FlyType
	{
		THROW_UP,
		THROW_HORIZONTAL,
		CHARGE,
		DUMMY,
		PUSH_HORIZONTAL,
		JUMP_EFFECTED,
		NOT_USED,
		PUSH_DOWN_HORIZONTAL,
		WARP_BACK,
		WARP_FORWARD,
		NONE
	}

	public FlyToLocation(L2Character cha, Location destLoc, FlyType type)
	{
		_destLoc = destLoc;
		_type = type;
		_chaObjId = cha.getObjectId();
		_loc = cha.getLoc();
		_speed = 0;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xD4);
		writeD(_chaObjId);
		writeD(_destLoc.getX());
		writeD(_destLoc.getY());
		writeD(_destLoc.getZ());
		writeD(_loc.getX());
		writeD(_loc.getY());
		writeD(_loc.getZ());
		writeD(_type.ordinal());
		writeD(_speed);
		writeD(0x00); // Unknown (GOD)
	}
}