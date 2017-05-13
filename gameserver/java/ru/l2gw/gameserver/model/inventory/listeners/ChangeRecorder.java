package ru.l2gw.gameserver.model.inventory.listeners;

import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.commons.arrays.GArray;

/**
 * Recorder of alterations in inventory
 */
public final class ChangeRecorder implements PaperdollListener
{
	private final GArray<L2ItemInstance> _changed;

	/**
	 * Constructor of the ChangeRecorder
	 * @param inventory inventory to watch
	 */
	public ChangeRecorder()
	{
		_changed = new GArray<L2ItemInstance>(5);
	}

	/**
	 * Add alteration in inventory when item equipped
	 */
	@SuppressWarnings("unused")
	public void notifyEquipped(int slot, L2ItemInstance item)
	{
		if(!_changed.contains(item))
			_changed.add(item);
	}

	/**
	 * Add alteration in inventory when item unequipped
	 */
	@SuppressWarnings("unused")
	public void notifyUnequipped(int slot, L2ItemInstance item)
	{
		if(!_changed.contains(item))
			_changed.add(item);
	}

	/**
	 * Returns alterations in inventory
	 * @return L2ItemInstance[] : array of alterated items
	 */
	public GArray<L2ItemInstance> getChangedItems()
	{
		return _changed;
	}
}
