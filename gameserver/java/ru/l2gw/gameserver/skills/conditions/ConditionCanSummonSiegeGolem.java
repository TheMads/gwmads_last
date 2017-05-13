package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.instancemanager.SiegeManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 23.11.2009 17:33:43
 */
public class ConditionCanSummonSiegeGolem extends Condition
{
	@Override
	public boolean testImpl(Env env)
	{
		if(!env.first)
			return true;

		if(!env.character.isPlayer())
			return false;

		L2Player player = env.character.getPlayer();

		Siege siege = SiegeManager.getSiege(player);
		if(siege == null)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION));
			return false;
		}
		else if(!siege.isInProgress() || !siege.checkIsClanRegistered(player.getClanId()))
			return false;

		return true;
	}
}
