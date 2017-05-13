package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.playerSubOrders.TeleportBookmark;

/**
 * @author rage
 * @date 23.06.2010 11:04:12
 */
public class ExGetBookMarkInfo extends L2GameServerPacket
{
	private final int bookmarksCapacity;
	private final TeleportBookmark[] bookmarks;

	public ExGetBookMarkInfo(L2Player player)
	{
		bookmarksCapacity = player.getTeleportBook().getMaxSlots();
		bookmarks = player.getTeleportBook().getBookmarks();
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x85);

		writeD(0x00); // должно быть 0
		writeD(bookmarksCapacity);
		writeD(bookmarks.length);
		for(TeleportBookmark bookmark : bookmarks)
		{
			writeD(bookmark.getSlot());
			writeD(bookmark.getLoc().getX());
			writeD(bookmark.getLoc().getY());
			writeD(bookmark.getLoc().getZ());
			writeS(bookmark.getName());
			writeD(bookmark.getIcon());
			writeS(bookmark.getAcronym());
		}
	}
}
