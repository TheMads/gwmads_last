package ru.l2gw.gameserver.model.base;

/**
 * @author: rage
 * @date: 30.12.11 12:29
 */
public class ItemData
{
	public int chance;
	public int item_id;
	public long count;
	public ItemData[] items;

	public static ItemData[] parseItem(String itemData)
	{
		String[] data = itemData.split(";");
		ItemData[] _items = new ItemData[data.length / 2];
		int j = 0;
		for(int i = 0; i < data.length; i += 2)
		{
			String[] items = data[i].split(",");
			ItemData id = new ItemData();
			id.chance = (int) (Double.parseDouble(data[i + 1]) * 1000000);
			id.items = new ItemData[items.length / 2];
			int k = 0;
			for(int n = 0; n < items.length; n += 2)
			{
				id.items[k] = new ItemData();
				id.items[k].item_id = Integer.parseInt(items[n]);
				id.items[k].count = Long.parseLong(items[n + 1]);
				k++;
			}
			_items[j] = id;
			j++;
		}

		return _items;
	}
}
