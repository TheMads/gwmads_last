package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.serverpackets.ValidateLocation;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 26.07.2010 11:17:34
 */
public class i_teleport_npc extends i_effect
{
	public i_teleport_npc(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(env.target.isDead())
				continue;

			env.target.abortAttack();
			env.target.abortCast();
			env.target.sendActionFailed();
			env.target.stopMove();
			int x = cha.getX();
			int y = cha.getY();
			int z = cha.getZ();
			int h = cha.getHeading();
			int range = (int) (cha.getColRadius() + env.target.getColRadius());
			int hyp = (int) Math.sqrt(range * range / 2);
			if(h < 16384)
			{
				x += hyp;
				y += hyp;
			}
			else if(h > 16384 && h <= 32768)
			{
				x -= hyp;
				y += hyp;
			}
			else if(h < 32768 && h <= 49152)
			{
				x -= hyp;
				y -= hyp;
			}
			else if(h > 49152)
			{
				x += hyp;
				y -= hyp;
			}
			env.target.setXYZ(x, y, z, false);
			env.target.broadcastPacket(new ValidateLocation(env.target));
		}
	}
}
