package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author: rage
 * @date: 13.07.2010 15:23:54
 */
public class i_sp extends i_effect
{
	public i_sp(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(!cha.isPlayer())
				return;

		L2Player player = (L2Player) cha;
		player.addExpAndSp(0, (int) calc());
	}
}
