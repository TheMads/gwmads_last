package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.util.Location;

/**
 * @author: rage
 * @date: 01.08.2010 16:03:08
 */
public class i_teleport extends i_effect
{
	private final Location _loc;
	public i_teleport(EffectTemplate template)
	{
		super(template);
		_loc = Location.parseLoc(template._attrs.getString("loc"));
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(!env.target.isPlayer())
				continue;

			L2Player pcTarget = env.target.getPlayer();

			if(pcTarget.isCombatFlagEquipped() || pcTarget.isInOlympiadMode() || pcTarget.isInDuel() || pcTarget.isInZone(L2Zone.ZoneType.no_escape))
				continue;

			env.target.abortAttack();
			env.target.abortCast();
			env.target.sendActionFailed();
			//int reflection = env.target.isInZone(L2Zone.ZoneType.instance) ? 0 : env.target.getReflection();

			pcTarget.teleToLocation(_loc, 0);
		}
	}
}
