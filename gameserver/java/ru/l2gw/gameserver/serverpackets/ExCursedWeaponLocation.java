package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.util.Location;

import java.util.List;

/**
 * Format: (ch) d[ddddd]
 */
public class ExCursedWeaponLocation extends L2GameServerPacket
{
	private List<CursedWeaponInfo> _cursedWeaponInfo;

	public ExCursedWeaponLocation(List<CursedWeaponInfo> cursedWeaponInfo)
	{
		_cursedWeaponInfo = cursedWeaponInfo;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x48);

		if(!_cursedWeaponInfo.isEmpty())
		{
			writeD(_cursedWeaponInfo.size());
			for(CursedWeaponInfo w : _cursedWeaponInfo)
			{
				writeD(w._id);
				writeD(w._status);

				writeD(w._pos.getX());
				writeD(w._pos.getY());
				writeD(w._pos.getZ());
			}
		}
		else
		{
			writeD(0);
			writeD(0);
		}
	}

	public static class CursedWeaponInfo
	{
		public Location _pos;
		public int _id;
		public int _status;

		public CursedWeaponInfo(Location p, int ID, int status)
		{
			_pos = p;
			_id = ID;
			_status = status;
		}
	}
}