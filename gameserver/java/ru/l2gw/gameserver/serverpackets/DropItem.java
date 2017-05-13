package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.util.Location;

/**
 * 16
 * d6 6d c0 4b		player id who dropped it
 * ee cc 11 43 		object id
 * 39 00 00 00 		item id
 * 8f 14 00 00 		x
 * b7 f1 00 00 		y
 * 60 f2 ff ff 		z
 * 01 00 00 00 		show item-count 1=yes
 * 7a 00 00 00      count                                         .
 *
 * format  dddddddd    rev 377
 *         ddddddddd   rev 417
 */
public class DropItem extends L2GameServerPacket
{
	private Location _loc;
	private int _playerId, item_obj_id, item_id, stackable;
	long _count;

	/**
	 * Constructor of the DropItem server packet
	 * @param item : L2ItemInstance designating the item
	 * @param playerId : int designating the player ID who dropped the item
	 */
	public DropItem(L2ItemInstance item, int playerId)
	{
		_playerId = playerId;
		item_obj_id = item.getObjectId();
		item_id = item.getItemId();
		_loc = item.getLoc();
		stackable = item.isStackable() ? 1 : 0;
		_count = item.getCount();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x16);
		writeD(_playerId);
		writeD(item_obj_id);
		writeD(item_id);
		writeD(_loc.getX());
		writeD(_loc.getY());
		writeD(_loc.getZ());
		writeD(stackable);
		writeQ(_count);
		writeD(1); // unknown
	}
}
