package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.util.Location;

/**
 * Примеры пакетов:
 *
 * Ставит флажок на карте и показывает стрелку на компасе:
 * EB 00 00 00 00 01 00 00 00 40 2B FF FF 8C 3C 02 00 A0 F6 FF FF
 * Убирает флажок и стрелку
 * EB 02 00 00 00 02 00 00 00 40 2B FF FF 8C 3C 02 00 A0 F6 FF FF
 */
public class RadarControl extends L2GameServerPacket
{
	private int _showRadar;
	private int _type;
	private Location _loc;

	/**
	 * 0xEB RadarControl         ddddd
	 * @param _
	 */
	public RadarControl(int showRadar, int type, Location loc)
	{
		_showRadar = showRadar; // showRader?? 0 = showradar; 1 = delete radar;
		_type = type; // radar type??
		_loc = loc;
	}

	public RadarControl(int showRadar, int type, int x, int y, int z)
	{
		this(showRadar, type, new Location(x, y, z));
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xF1);
		writeD(_showRadar);
		writeD(_type); //maybe type
		writeD(_loc.getX()); //x
		writeD(_loc.getY()); //y
		writeD(_loc.getZ()); //z
	}
}