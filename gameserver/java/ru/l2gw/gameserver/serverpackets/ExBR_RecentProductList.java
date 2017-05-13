package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.entity.ProductData;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author: rage
 * @date: 15.10.11 21:12
 */
public class ExBR_RecentProductList extends L2GameServerPacket
{
	private GArray<ProductData> products;

	public ExBR_RecentProductList(GArray<ProductData> prod)
	{
		products = prod;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0xDD);
		// dx[dhddddcccccdd]
		writeD(products.size());
		for(ProductData pd : products)
		{
			writeD(pd.product_id);
			writeH(pd.category);
			writeD(pd.price);
			writeD(0x00); // show tab ?
			writeD(pd.sale_start_date);
			writeD(pd.sale_end_date);
			writeC(0x7F); // ? day week (127 = not daily goods)
			writeC(0x00); // start hour
			writeC(0x00); // start min
			writeC(23); // end hour
			writeC(59); // end min
			writeD(0x00); // stock
			writeD(0x00); // max stock
		}
	}
}

