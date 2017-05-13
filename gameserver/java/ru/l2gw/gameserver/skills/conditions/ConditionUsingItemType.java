package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.Inventory;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;

public final class ConditionUsingItemType extends Condition
{
	private final int _mask;

	public ConditionUsingItemType(int mask)
	{
		_mask = mask;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;
		Inventory inv = ((L2Player) env.character).getInventory();
		return (_mask & inv.getWearedMask()) != 0;
	}
}
