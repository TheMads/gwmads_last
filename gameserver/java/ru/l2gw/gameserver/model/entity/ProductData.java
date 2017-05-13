package ru.l2gw.gameserver.model.entity;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.templates.StatsSet;

/**
 * @author: rage
 * @date: 15.10.11 19:47
 */
public class ProductData
{
	public final int product_id;
	public final String name;
	public final int category;
	public final int price;
	public final int is_event_product;
	public final int is_best_product;
	public final int is_new_product;
	public final int buyable;
	public final int sale_start_date;
	public final int sale_end_date;
	public final int location_id;
	public final GArray<StatsSet> items;

	public ProductData(int id, String pName, int cat, int pr, int event, int best, int new_p, int buy, int sale_start, int sale_end, int loc, String itemids)
	{
		product_id = id;
		name = pName;
		category = cat;
		price = pr;
		is_event_product = event;
		is_best_product = best;
		is_new_product = new_p;
		buyable = buy;
		sale_start_date = sale_start;
		sale_end_date = sale_end;
		location_id = loc;
		String[] ids = itemids.split(";");
		items = new GArray<>(ids.length / 2);
		for(int i = 0; i < ids.length; i+=2)
		{
			StatsSet item = new StatsSet();
			item.set("item_id", ids[i]);
			item.set("item_count", ids[i + 1]);
			items.add(item);
		}
	}
}