package ru.l2gw.gameserver.skills.conditions;

import javolution.util.FastList;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.model.L2Player;

public class ConditionTargetClassId extends Condition
{

	private final FastList<Short> _classIds;

	public ConditionTargetClassId(FastList<Short> classId)
	{
		_classIds = classId;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return !env.target.isPlayer() || !_classIds.contains(((L2Player) env.target).getActiveClass());
	}
}
