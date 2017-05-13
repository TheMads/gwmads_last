package ru.l2gw.gameserver.handler;

import ru.l2gw.gameserver.model.L2Playable;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

/**
 * Mother class of all itemHandlers.<BR><BR>
 * an IItemHandler implementation has to be stateless
 */
public interface IItemHandler
{
	/**
	 * Launch task associated to the item.
	 * @param player : L2PlayableInstance designating the player
	 * @param item : L2ItemInstance designating the item to use
	 */
	public boolean useItem(L2Playable playable, L2ItemInstance item);

	/**
	 * Returns the list of item IDs corresponding to the type of item.<BR><BR>
	 * <B><I>Use :</I></U><BR>
	 * This method is called at initialization to register all the item IDs automatically
	 * @return int[] designating all itemIds for a type of item.
	 */
	public int[] getItemIds();
}
