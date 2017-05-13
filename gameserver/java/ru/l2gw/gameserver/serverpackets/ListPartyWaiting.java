package ru.l2gw.gameserver.serverpackets;

import javolution.util.FastList;
import ru.l2gw.gameserver.model.L2Player;

/**
 * sample
 * af
 * 02 00 00 00   count
 *
 * 71 b3 70 4b  object id
 * 44 00 79 00 66 00 65 00 72 00 00 00   name
 * 14 00 00 00  level
 * 0f 00 00 00  class id
 * 00 00 00 00  sex ??
 * 00 00 00 00  clan id
 * 02 00 00 00  ??
 * 6f 5f 00 00  x
 * af a9 00 00  y
 * f7 f1 ff ff  z
 *
 *
 * c1 9c c0 4b object id
 * 43 00 6a 00 6a 00 6a 00 6a 00 6f 00 6e 00 00 00
 * 0b 00 00 00  level
 * 12 00 00 00  class id
 * 00 00 00 00  sex ??
 * b1 01 00 00  clan id
 * 00 00 00 00
 * 13 af 00 00
 * 38 b8 00 00
 * 4d f4 ff ff
 * *
 * format   d (dSdddddddd)
 */
public class ListPartyWaiting extends L2GameServerPacket
{
	private FastList<PartyWaitingPlayer> infos = new FastList<PartyWaitingPlayer>();

	public ListPartyWaiting(L2Player[] allPlayers)
	{
		int size = allPlayers.length < 40 ? allPlayers.length : 40;
		String _name;
		int obj_id, level, class_id, clan_id, x, y, z, col_name;

		for(int i = 0; i < size; i++)
		{
			_name = allPlayers[i].getName();
			obj_id = allPlayers[i].getObjectId();
			level = allPlayers[i].getLevel();
			class_id = allPlayers[i].getClassId().getId();
			clan_id = allPlayers[i].getClanId();
			x = allPlayers[i].getX();
			y = allPlayers[i].getY();
			z = allPlayers[i].getZ();
			col_name = 0; // 00 -white name   01-red name
			infos.add(new PartyWaitingPlayer(_name, obj_id, level, class_id, clan_id, x, y, z, col_name));
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x9C);
		writeD(infos.size());
		for(PartyWaitingPlayer _info : infos)
		{
			writeD(_info.obj_id);
			writeS(_info._name);
			writeD(_info.level);
			writeD(_info.class_id);
			writeD(_info.col_name); // 00 -white name   01-red name
			writeD(_info.clan_id);
			writeD(00); //  00 - no affil  01-party  02-party pending  03-
			writeD(_info.x);
			writeD(_info.y);
			writeD(_info.z);
		}
		infos.clear();
	}

	static class PartyWaitingPlayer
	{
		public String _name;
		public int obj_id, level, class_id, clan_id, x, y, z, col_name;

		public PartyWaitingPlayer(String __name, int _obj_id, int _level, int _class_id, int _clan_id, int _x, int _y, int _z, int _col_name)
		{
			_name = __name;
			obj_id = _obj_id;
			level = _level;
			class_id = _class_id;
			clan_id = _clan_id;
			x = _x;
			y = _y;
			z = _z;
			col_name = _col_name;
		}
	}
}