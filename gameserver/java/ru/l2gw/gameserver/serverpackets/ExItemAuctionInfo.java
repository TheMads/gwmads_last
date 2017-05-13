package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.entity.itemauction.AuctionItem;
import ru.l2gw.gameserver.model.entity.itemauction.ItemAuction;

/**
 * @author: rage
 * @date: 28.08.2010 19:46:46
 */
public class ExItemAuctionInfo extends L2GameServerPacket
{
	private final ItemAuction _ia;
	private final byte _update;

	public ExItemAuctionInfo(ItemAuction ia, boolean update)
	{
		_ia = ia;
		_update = (byte) (update ? 0x00 : 0x01);
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x69);

		AuctionItem item1;
		AuctionItem item2;

		if(_ia.isStarted())
		{
			item1 = _ia.getItem();
			item2 = _ia.getNextItem();
			writeC(_update);
			writeD(_ia.getAuctionId());
			writeQ(_ia.getCurrentBid());
			writeD(_ia.getTimeLeft());
			writeItem(item1);
			writeQ(item2.getStartBid());
			writeD(_ia.getNextDate());
			writeItem(item2);
		}
		else
		{
			item1 = _ia.getPrevItem();
			item2 = _ia.getItem();
			writeC(_update);
			writeD(_ia.getAuctionId());
			writeQ(_ia.getPrevBid());
			writeD(0x00);
			writeItem(item1);
			writeQ(item2.getStartBid());
			writeD(_ia.getStartDate());
			writeItem(item2);
		}
	}

	private void writeItem(AuctionItem item)
	{
		if(item == null)
		{
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeQ(0x00);
			writeH(0x00);
			writeH(0x00);
			writeH(0x00);
			writeD(0x00);
			writeH(0x00);
			writeH(0x00);
		}
		else
		{
			writeD(item.getItemId());
			writeD(item.getItemId());
			writeD(0x00);
			writeQ(item.getCount());
			writeH(item.getType2());
			writeH(item.getCustomType1());
			writeH(0x00);
			writeD(item.getBodyPart());
			writeH(item.getEnchantLevel());
			writeH(item.getCustomType2());
		}
		writeD(0x00); // Augmentation
		writeD(-1);
		writeD(-9999);
		writeH(-2); // Attack attribute
		writeH(0x00); // Attack value
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00); // Rev 152 enchant
		writeH(0x00);
		writeH(0x00);
	}
}
