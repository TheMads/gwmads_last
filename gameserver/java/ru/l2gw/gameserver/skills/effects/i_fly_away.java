package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.serverpackets.FlyToLocation;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.util.Location;
import ru.l2gw.util.Util;

/**
 * @author: rage
 * @date: 18.10.2009 16:33:53
 */
public class i_fly_away extends i_effect
{
	private final int _flyRadius;

	public i_fly_away(EffectTemplate template)
	{
		super(template);
		_flyRadius = getSkill().getFlyRadius() > 0 ? getSkill().getFlyRadius() : 150;
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(env.target.isDead())
				continue;

			int dmg = (int) calc();
			if(dmg > 0)
			{
				if(dmg >= env.target.getCurrentHp())
					dmg = (int) env.target.getCurrentHp() - 1;
				env.target.reduceHp(dmg, cha, false, false);
			}
			Location dest = Util.getPointInRadius(env.target.getLoc(), _flyRadius, (int) (Util.calculateAngleFrom(cha, env.target) + 180));
			dest = GeoEngine.moveCheck(env.target.getX(), env.target.getY(), env.target.getZ(), dest.getX(), dest.getY(), env.target.getReflection());
			env.target.broadcastPacket(new FlyToLocation(env.target, dest, FlyToLocation.FlyType.THROW_UP));
			env.target.setXYZ(dest.getX(), dest.getY(), dest.getZ(), false);
		}
	}
}
