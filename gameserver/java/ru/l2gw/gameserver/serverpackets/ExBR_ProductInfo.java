package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.entity.ProductData;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.gameserver.templates.StatsSet;

/**
 * @author: rage
 * @date: 15.10.11 22:00
 */
public class ExBR_ProductInfo extends L2GameServerPacket
{
	private ProductData productData;

	public ExBR_ProductInfo(ProductData prod)
	{
		productData = prod;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0xD8);
		writeD(productData.product_id);
		writeD(productData.price);
		writeD(productData.items.size());
		for(StatsSet st : productData.items)
		{
			L2Item item = ItemTable.getInstance().getTemplate(st.getInteger("item_id"));
			writeD(st.getInteger("item_id"));
			writeD(st.getInteger("item_count"));
			writeD(item.getWeight());
			writeD(item.isDropable() ? 0x01 : 0x00);
		}
	}
}