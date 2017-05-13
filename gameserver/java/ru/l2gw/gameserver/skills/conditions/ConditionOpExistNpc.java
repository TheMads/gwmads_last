package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 24.11.2010 18:30:27
 */
public class ConditionOpExistNpc extends Condition
{
	private final int _npcId, _radius, _count;

	public ConditionOpExistNpc(String cond)
	{
		String[] p = cond.split(";");
		_npcId = Integer.parseInt(p[0]);
		_radius = Integer.parseInt(p[1]);
		_count = p.length == 3 ? Integer.parseInt(p[2]) : 1;
	}

	@Override
	public boolean testImpl(Env env)
	{
		int c = 0;
		for(L2NpcInstance npc : env.character.getKnownNpc(_radius))
			if(npc.getNpcId() == _npcId)
				c++;

		return c >= _count;
	}
}
