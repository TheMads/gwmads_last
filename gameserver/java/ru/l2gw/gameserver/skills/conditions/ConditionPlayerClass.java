package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 05.08.2010 15:16:49
 */
public class ConditionPlayerClass extends Condition
{
	private final GArray<Integer> _classes;
	public ConditionPlayerClass(String[] classes)
	{
		_classes = new GArray<Integer>(classes.length);
		for(String classId : classes)
			if(!classId.isEmpty())
				_classes.add(Integer.parseInt(classId));
	}

	@Override
	public boolean testImpl(Env env)
	{
		if(!env.target.isPlayer())
			return false;

		L2Player player = (L2Player) env.target;
		return _classes.contains(player.getClassId().getId());
	}
}
