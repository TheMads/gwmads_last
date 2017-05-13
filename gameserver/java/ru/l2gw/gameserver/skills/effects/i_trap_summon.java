package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2TrapInstance;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.util.Location;

/**
 * @author rage
 * @date 20.11.2009 14:47:56
 */
public class i_trap_summon extends i_effect
{
	public i_trap_summon(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(cha.isPlayer() && ((L2Player) cha).getLastTrap() != null)
			((L2Player) cha).getLastTrap().doDie(null);

		L2TrapInstance trap = L2TrapInstance.createTrap(cha, getSkill().getNpcId());

		if(cha.isPlayer())
			cha.getPlayer().setLastTrap(trap);

		if(getSkill().getTrapLifeTime() > 0)
			trap.setLifeTime(getSkill().getTrapLifeTime());

		Location loc = cha.getLoc();
		trap.setXYZ(loc.getX(), loc.getY(), loc.getZ(), false);
		trap.spawnMe();

		cha.startAttackStanceTask();
	}
}
