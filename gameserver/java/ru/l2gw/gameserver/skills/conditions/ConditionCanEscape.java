package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 01.08.2010 15:57:00
 */
public class ConditionCanEscape extends Condition
{
	@Override
	public boolean testImpl(Env env)
	{
		L2Player player = env.character.getPlayer();
		return player != null && !(player.isInZone(L2Zone.ZoneType.no_escape) || player.isInDuel() || player.isInOlympiadMode());
	}
}
