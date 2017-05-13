package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 23.12.2009 15:07:02
 */
public class i_change_hair_style extends i_change_face
{
	public i_change_hair_style(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
			if(env.target.isPlayer())
			{
				env.target.getPlayer().setHairStyle(type);
				env.target.broadcastUserInfo(true);
			}
	}
}
