package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 23.12.2009 14:58:23
 */
public class i_change_face extends i_effect
{
	protected final byte type;
	public i_change_face(EffectTemplate template)
	{
		super(template);
		type = template._attrs.getByte("type", (byte) 0);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
			if(env.target.isPlayer())
			{
				env.target.getPlayer().setFace(type);
				env.target.broadcastUserInfo(true);
			}
	}
}
