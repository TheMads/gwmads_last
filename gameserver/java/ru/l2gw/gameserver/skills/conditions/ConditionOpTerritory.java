package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Territory;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.tables.SpawnTable;

import java.util.regex.Matcher;

/**
 * @author: rage
 * @date: 24.09.11 0:15
 */
public class ConditionOpTerritory extends Condition
{
	private static int count;
	private final L2Territory territory;

	public ConditionOpTerritory(String terr)
	{
		territory = new L2Territory("OpTerritory_" + (count++));
		Matcher m = SpawnTable.tp.matcher(terr);

		while(m.find())
			territory.add(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)));
	}

	@Override
	public boolean testImpl(Env env)
	{
		return territory.isInside(env.character.getX(), env.character.getY(), env.character.getZ());
	}
}
