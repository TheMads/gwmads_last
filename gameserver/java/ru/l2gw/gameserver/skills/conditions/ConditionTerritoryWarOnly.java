package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.instancemanager.SiegeManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 10.07.2010 20:34:57
 */
public class ConditionTerritoryWarOnly extends Condition
{
	@Override
	public boolean testImpl(Env env)
	{
		L2Player player = env.character.getPlayer();
		return player != null && TerritoryWarManager.getWar().isInProgress() && player.getTerritoryId() > 0 && player.isInSiege();
	}
}