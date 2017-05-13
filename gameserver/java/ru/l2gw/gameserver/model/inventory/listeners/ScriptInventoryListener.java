package ru.l2gw.gameserver.model.inventory.listeners;

import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.gameserver.model.Inventory;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

/**
 * @author: rage
 * @date: 16.01.13 14:54
 */
public class ScriptInventoryListener implements InventoryListener
{
	private final Inventory inventory;

	public ScriptInventoryListener(Inventory inventory)
	{
		this.inventory = inventory;
	}

	@Override
	public void itemAdded(L2ItemInstance item)
	{
		if(!Scripts.onItemAdded.isEmpty())
		{
			Object[] script_args = new Object[] { inventory.getOwner(), item };
			for(Scripts.ScriptClassAndMethod handler : Scripts.onItemAdded)
				try
				{
					inventory.getOwner().callScripts(handler.scriptClass, handler.method, script_args);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
		}
	}

	@Override
	public void itemRemoved(L2ItemInstance item)
	{
		if(!Scripts.onItemRemoved.isEmpty())
		{
			Object[] script_args = new Object[] { inventory.getOwner(), item };
			for(Scripts.ScriptClassAndMethod handler : Scripts.onItemRemoved)
				try
				{
					inventory.getOwner().callScripts(handler.scriptClass, handler.method, script_args);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
		}
	}
}