/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.PartyRoom;

/**
 *
 *
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
 *
 * 
 */
public class PartyMatchList extends L2GameServerPacket
{
	private int _id;
	private int _minLevel;
	private int _maxLevel;
	private int _lootDist;
	private int _maxMembers;
	private int _location;
	private String _title;

	public PartyMatchList()
	{}

	public PartyMatchList(PartyRoom room)
	{
		_id = room.getId();
		_minLevel = room.getMinLevel();
		_maxLevel = room.getMaxLevel();
		_lootDist = room.getLootDist();
		_maxMembers = room.getMaxMembers();
		_location = room.getLocation();
		_title = room.getTitle();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x9d);
		writeD(_id); // room id
		writeD(_maxMembers); //max members
		writeD(_minLevel); //min level
		writeD(_maxLevel); //max level
		writeD(_lootDist); //loot distribution 1-Random 2-Random includ. etc
		writeD(_location); //location
		writeS(_title); // room name
		writeH(0x00);
	}
}
