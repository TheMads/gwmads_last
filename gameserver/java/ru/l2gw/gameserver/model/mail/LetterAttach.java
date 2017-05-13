package ru.l2gw.gameserver.model.mail;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

/**
 * @author rage
 * @date 17.06.2010 15:52:11
 */
public class LetterAttach
{
	private final GArray<L2ItemInstance> _items;
	public int attach_id;

	public LetterAttach()
	{
		_items = new GArray<>();
	}

	public LetterAttach(L2Player player, int[] objectIds, long[] counts)
	{
		_items = new GArray<>();
		for(int i = 0; i < objectIds.length; i++)
		{
			L2ItemInstance item = player.getInventory().dropItem("MailSend", objectIds[i], counts[i], player, null);
			if(item != null)
			{
				item.setOwnerId(player.getObjectId());
				item.setLocation(L2ItemInstance.ItemLocation.MAILBOX, (short) 0);
				item.updateDatabase(true);
				_items.add(item);
			}
		}

	}

	public void addItem(L2ItemInstance item)
	{
		_items.add(item);
	}

	public GArray<L2ItemInstance> getItems()
	{
		return _items;
	}
}
