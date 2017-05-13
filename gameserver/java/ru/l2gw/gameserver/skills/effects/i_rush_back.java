package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.serverpackets.FlyToLocation;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.util.Location;
import ru.l2gw.util.Util;

/**
 * @author rage
 * @date 14.04.2010 16:34:55
 */
public class i_rush_back extends i_effect
{
	public i_rush_back(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		Location destiny;
		L2Character target = cha.getCastingTarget();
		
		if(target != null && target != cha)
			destiny = Util.getPointInRadius(cha.getLoc(), getSkill().getFlyRadius(), (int) (Util.calculateAngleFrom(cha, target) + 180));
		else
			destiny = Util.getPointInRadius(cha.getLoc(), getSkill().getFlyRadius(), (int) Util.convertHeadingToDegree(cha.getHeading()) + 180);

		destiny = GeoEngine.moveCheck(cha.getX(), cha.getY(), cha.getZ(), destiny.getX(), destiny.getY(), cha.getReflection());

		cha.setXYZ(destiny.getX(), destiny.getY(), destiny.getZ(), false);
		cha.broadcastPacket(new FlyToLocation(cha, destiny, getSkill().getFlyType()));
	}
}
