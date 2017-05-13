package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 23.11.2009 18:15:42
 */
public class i_summon_cubic extends i_effect
{
	private final int cubicId;
	private final int cubicLevel;

	public i_summon_cubic(EffectTemplate template)
	{
		super(template);
		cubicId = template._attrs.getInteger("cubicId", 0);
		cubicLevel = template._attrs.getInteger("cubicLevel", 0);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
			if(env.target.isPlayer() && cubicId > 0 && cubicLevel > 0)
			{
				L2Player target = env.target.getPlayer();
				target.addCubic(cubicId, cubicLevel, env.character != env.target);
				target.broadcastUserInfo(true);
			}
	}
}
