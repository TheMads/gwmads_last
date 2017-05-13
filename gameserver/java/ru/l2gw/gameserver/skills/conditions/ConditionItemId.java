package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

public final class ConditionItemId extends Condition
{
	private final short _itemId;

	public ConditionItemId(short itemId)
	{
		_itemId = itemId;
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(env.item == null)
			return false;
		return env.item.getItemId() == _itemId;
	}
}