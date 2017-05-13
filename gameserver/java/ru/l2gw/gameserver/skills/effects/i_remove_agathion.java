package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 25.11.2009 10:07:58
 */
public class i_remove_agathion extends i_effect
{
	public i_remove_agathion(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(cha.isPlayer())
		{
			L2Player player = (L2Player) cha;

			if(player.getAgathionId() != 0)
				player.setAgathion(0);
		}
	}
}
