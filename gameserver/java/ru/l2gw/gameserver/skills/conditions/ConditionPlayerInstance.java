package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.instancemanager.InstanceManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.instance.Instance;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 06.10.2010 15:12:59
 */
public class ConditionPlayerInstance extends Condition
{
	private final GArray<Integer> _instanceId;

	public ConditionPlayerInstance(String value)
	{
		String[] ids = value.split(",");
		_instanceId = new GArray<Integer>(ids.length);
		for(String id : ids)
			if(id != null && !id.isEmpty())
				_instanceId.add(Integer.parseInt(id));
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(env.character.isPlayer())
		{
			Instance inst = InstanceManager.getInstance().getInstanceByPlayer((L2Player) env.character);
			return inst != null && _instanceId.contains(inst.getTemplate().getId()) && inst.getReflection() == env.character.getReflection() && (inst.getTemplate().getZone() == null || inst.getTemplate().getZone().isInsideZone(env.character));
		}
		return false;
	}
	
}
