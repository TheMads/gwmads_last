package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author: rage
 * @date: 17.10.11 16:49
 */
public class i_restoration extends i_effect
{
	private final int itemId;
	private final long count;

	public i_restoration(EffectTemplate template)
	{
		super(template);
		String[] items = template._options.split(";");
		if(items.length >= 2)
		{
			itemId = Integer.parseInt(items[0]);
			count = Long.parseLong(items[1]);
		}
		else
		{
			itemId = 0;
			count = 0;
		}
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(!cha.isPlayer() || itemId == 0)
			return;

		L2Player player = (L2Player) cha;
		player.addItem("SkillSummonItem", itemId, count, null, true);
	}
}
