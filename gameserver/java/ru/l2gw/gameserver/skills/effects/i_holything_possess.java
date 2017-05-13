package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.instancemanager.SiegeManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.instances.L2ArtefactInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 10.07.2010 12:28:01
 */
public class i_holything_possess extends i_effect
{
	public i_holything_possess(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(!cha.isPlayer())
			return;
		for(Env env : targets)
		{
			if(!(env.target instanceof L2ArtefactInstance))
				continue;

			L2Player player = (L2Player) cha;
			Siege siege = SiegeManager.getSiege(player);
			if(siege != null && siege.isInProgress())
			{
				siege.broadcastToPlayer(new SystemMessage(SystemMessage.CLAN_S1_HAS_SUCCEEDED_IN_ENGRAVING_THE_RULER).addString(player.getClan().getName()), true);
				siege.Engrave(player.getClan(), env.target.getObjectId());
			}
		}
	}
}
