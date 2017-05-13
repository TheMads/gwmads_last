package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.instancemanager.SiegeManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.Fortress;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.entity.siege.SiegeClan;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 10.07.2010 12:48:01
 */
public class ConditionFlagpolePossess extends Condition
{
	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;

		L2Player player = (L2Player) env.character;
		if(player.getClanId() == 0)
			return false;

		Siege siege = SiegeManager.getSiege(player);
		if(siege == null || !(siege.getSiegeUnit() instanceof Fortress))
			return false;
		if(!siege.isInProgress())
			return false;
		if(siege.getAttackerClan(player.getClanId()) == null)
			return false;

		if(env.first)
			for(SiegeClan sc : siege.getDefenderClans().values())
			{
				L2Clan clan = sc.getClan();
				if(clan != null)
					for(L2Player pl : clan.getOnlineMembers(""))
						if(pl != null)
							pl.sendPacket(new SystemMessage(SystemMessage.S1_CLAN_IS_TRYING_TO_DISPLAY_A_FLAG).addString(player.getClan().getName()));
			}

		return true;
	}
}
