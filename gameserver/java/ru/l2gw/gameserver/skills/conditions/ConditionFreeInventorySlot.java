package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 14.07.2010 12:23:04
 */
public class ConditionFreeInventorySlot extends Condition
{
	private final int _slots;

	public ConditionFreeInventorySlot(int slots)
	{
		_slots = slots;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;

		L2Player player = (L2Player) env.character;
		if(player.getInventoryLimit() - player.getInventoryItemsCount() < _slots)
		{
			player.sendPacket(Msg.YOUR_INVENTORY_IS_FULL);
			return false;
		}

		return true;
	}
}
