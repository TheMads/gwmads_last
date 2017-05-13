package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2TerritoryOutpostInstance;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 10.07.2010 15:11:56
 */
public class ConditionWardPossess extends Condition
{
	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer() || !TerritoryWarManager.getWar().isInProgress() || !(env.target instanceof L2TerritoryOutpostInstance))
			return false;

		L2Player player = (L2Player) env.character;
		L2Clan clan = player.getClan();

		if(clan == null || clan.getHasCastle() == 0 || !player.isCombatFlagEquipped() || player.getActiveWeaponInstance() == null || !player.getActiveWeaponInstance().isTerritoryWard())
			return false;

		L2TerritoryOutpostInstance outpost = (L2TerritoryOutpostInstance) env.target;

		return outpost.getOwner() == clan;
	}
}
