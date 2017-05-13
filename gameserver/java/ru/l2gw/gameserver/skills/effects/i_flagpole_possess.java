package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.instancemanager.SiegeManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.instances.L2StaticObjectInstance;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 10.07.2010 12:46:15
 */
public class i_flagpole_possess extends i_effect
{
	public i_flagpole_possess(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(!(env.target instanceof L2StaticObjectInstance) || ((L2StaticObjectInstance) env.target).getType() != 3 || !cha.isPlayer())
				continue;

			L2Player player = (L2Player) cha;
			Siege siege = SiegeManager.getSiege(player);
			if(siege != null && siege.isInProgress())
				siege.Engrave(player.getClan(), env.target.getObjectId());
		}
	}
}
