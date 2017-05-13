package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.util.Location;

public class PlaySound extends L2GameServerPacket
{
	private int _un1,_unk3, _unk4, _unk5;
	private String _soundFile;
	private Location _loc = new Location(0, 0, 0);

	public PlaySound(String soundFile)
	{
		_un1 = 0;
		_soundFile = soundFile;
		_unk3 = 0;
		_unk4 = 0;
		_unk5 = 0;
	}

	public PlaySound(int unknown1, String soundFile, int unknown3, int unknown4, Location loc)
	{
		_un1 = unknown1;
		_soundFile = soundFile;
		_unk3 = unknown3;
		_unk4 = unknown4;
		_loc = loc;
	}

	public PlaySound(int unknown1, String soundFile, int unknown3, int unknown4, Location loc, int unk5)
	{
		_un1 = unknown1;
		_soundFile = soundFile;
		_unk3 = unknown3;
		_unk4 = unknown4;
		_loc = loc;
		_unk5 = unk5;
	}

	public PlaySound(L2Vehicle vehicle, String soubndFile)
	{
		_un1 = 0;
		_soundFile = soubndFile;
		_unk3 = 1;
		_unk4 = vehicle.getObjectId();
		_loc = vehicle.getLoc();
		_unk5 = 0;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x9E);
		writeD(_un1); //unknown 0 for quest and ship, c4 toturial = 2
		writeS(_soundFile);
		writeD(_unk3); //unknown 0 for quest; 1 for ship;
		writeD(_unk4); //0 for quest; objectId of ship
		writeD(_loc.getX()); //x
		writeD(_loc.getY()); //y
		writeD(_loc.getZ()); //z
		writeD(_unk5); // unknown
	}
}