package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 10.07.2010 14:59:53
 */
public class ConditionTargetNpcId extends Condition
{
	private GArray<Integer> _npcIds;

	public ConditionTargetNpcId(String[] ids)
	{
		_npcIds = new GArray<Integer>(ids.length);
		for(String npcId : ids)
			_npcIds.add(Integer.parseInt(npcId));
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.target instanceof L2NpcInstance && _npcIds.contains(env.target.getNpcId());
	}

}
