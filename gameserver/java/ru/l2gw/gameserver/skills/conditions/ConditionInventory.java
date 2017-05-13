package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

public abstract class ConditionInventory extends Condition implements ConditionListener
{
	protected final short _slot;

	public ConditionInventory(short slot)
	{
		_slot = slot;
	}

	@Override
	public abstract boolean testImpl(Env env);
}