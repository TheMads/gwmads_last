package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 09.02.11 18:27
 */
public class ConditionPlayerTransform extends Condition
{
	private final GArray<Integer> _transformId;

	public ConditionPlayerTransform(String value)
	{
		String[] ids = value.split(",");
		_transformId = new GArray<Integer>(ids.length);
		for(String id : ids)
			if(id != null && !id.isEmpty())
				_transformId.add(Integer.parseInt(id));
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(env.character.isPlayer())
		{
			L2Player player = (L2Player) env.character;
			return _transformId.contains(player.getTransformation());
		}
		return false;
	}
}