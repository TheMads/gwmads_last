package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.Inventory;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;

public class ConditionChanceUsingNotKind extends ConditionChance
{
	private final int _mask;

	public ConditionChanceUsingNotKind(int mask)
	{
		_mask = mask;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;
		Inventory inv = ((L2Player) env.character).getInventory();
		return !((_mask & inv.getWearedMask()) != 0);
	}
}
