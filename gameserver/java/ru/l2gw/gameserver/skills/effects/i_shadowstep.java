package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.serverpackets.FlyToLocation;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.util.Location;

/**
 * @author rage
 * @date 14.04.2010 12:51:41
 */
public class i_shadowstep extends i_effect
{
	public i_shadowstep(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		Location destiny;
		if(cha.getDistance3D(cha) < getSkill().getCastRange() - 100)
		{
			cha.sendPacket(new SystemMessage(SystemMessage.THERE_IS_NOT_ENOUGH_SPACE_TO_MOVE_THE_SKILL_CANNOT_BE_USED));
			return;
		}
		destiny = cha.applyOffset(cha.getLoc(), 20);

		destiny = GeoEngine.moveCheck(cha.getX(), cha.getY(), cha.getZ(), destiny.getX(), destiny.getY(), cha.getReflection());

		cha.setXYZ(destiny.getX(), destiny.getY(), destiny.getZ(), false);
		cha.broadcastPacket(new FlyToLocation(cha, destiny, getSkill().getFlyType()));
	}
}
