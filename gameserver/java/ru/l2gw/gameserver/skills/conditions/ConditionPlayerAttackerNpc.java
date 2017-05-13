package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.skills.Env;

/**
 * Created by IntelliJ IDEA.
 * User: rage
 * Date: 24.09.2008
 * Time: 15:06:25
 */
public class ConditionPlayerAttackerNpc extends Condition
{
	private final int _npcId;

	public ConditionPlayerAttackerNpc(int npcId)
	{
		_npcId = npcId;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.target instanceof L2NpcInstance && env.target.getNpcId() == _npcId;
	}
}
