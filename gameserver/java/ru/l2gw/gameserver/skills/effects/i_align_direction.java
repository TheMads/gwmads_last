package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.serverpackets.CharMoveToLocation;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.util.Location;

class i_align_direction extends i_effect
{
	public i_align_direction(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(!env.success || (_template._activateRate > 0 && !Rnd.chance(_template._activateRate)))
				continue;
			int posX = env.target.getX();
			int posY = env.target.getY();
			int posZ = env.target.getZ();
			int signx = -1;
			int signy = -1;
			if(env.target.getX() > env.target.getX())
				signx = 1;
			if(env.target.getY() > env.target.getY())
				signy = 1;

			env.target.setHeading(cha, false);
			env.target.setRunning();
			env.target.broadcastPacket(new CharMoveToLocation(env.target, new Location(posX + signx * 20, posY + signy * 20, posZ)));
		}
	}
}