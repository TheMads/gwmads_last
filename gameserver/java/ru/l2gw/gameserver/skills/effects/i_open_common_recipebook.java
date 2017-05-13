package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.controllers.RecipeController;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 13.07.2010 15:50:51
 */
public class i_open_common_recipebook extends i_effect
{
	public i_open_common_recipebook(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(!cha.isPlayer() || cha.isInDuel())
			return;

		RecipeController.requestBookOpen((L2Player) cha, 1);
	}
}
