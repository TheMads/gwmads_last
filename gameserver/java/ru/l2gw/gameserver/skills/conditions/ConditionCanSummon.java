package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.model.L2Player;

/**
 * @author rage
 * @date 23.11.2009 11:57:28
 */
public class ConditionCanSummon extends Condition
{
	@Override
	public boolean testImpl(Env env)
	{
		if(!env.first)
			return true;

		L2Player player = env.character.getPlayer();

		if(player == null)
			return false;


		if(player.isTradeInProgress() || player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_SUMMON_DURING_A_TRADE_OR_WHILE_USING_THE_PRIVATE_SHOPS));
			return false;
		}

		boolean ret = !env.character.getPlayer().isPetSummoned() && !env.character.getPlayer().getMountEngine().isMounted();

		if(!ret)
			env.character.sendPacket(new SystemMessage(SystemMessage.YOU_MAY_NOT_USE_MULTIPLE_PETS_OR_SERVITORS_AT_THE_SAME_TIME));

		return ret;
	}
}
