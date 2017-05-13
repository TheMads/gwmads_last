package ru.l2gw.gameserver.model;

import javolution.util.FastList;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.commons.math.Rnd;

public class L2ExtractableItems
{
	private final FastList<StatsSet> _items = FastList.newInstance();
	private final int _itemId;

	public L2ExtractableItems(int itemId)
	{
		_itemId = itemId;
	}

	public void addProduct(int itemId, long min, long max, double chance)
	{
		StatsSet item = new StatsSet();
		item.set("item_id", itemId);
		item.set("min", min);
		item.set("max", max);
		item.set("chance", chance);
		_items.add(item);
	}

	public boolean extractItem(L2ItemInstance item, L2Player player)
	{
		if(player.destroyItem("Consume", item.getObjectId(), 1, null, true))
		{
			for(StatsSet ii : _items)
			{
				if(Rnd.chance(ii.getDouble("chance")))
				{
					long count = ii.getLong("min") == ii.getLong("max") ? ii.getLong("min") : Rnd.get(ii.getLong("min"), ii.getLong("min"));
					player.addItem("ExtractableItems", ii.getInteger("item_id"), count, null, true);
				}
			}
		}
		else
		{
			return false;
		}

		return true;
	}
}
