package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 18.07.2010 16:34:33
 */
public class ConditionOpClanGate extends Condition
{
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;

		L2Player caster = (L2Player) env.character;

		if(caster.getClanId() == 0 || !caster.isClanLeader() || !caster.getClan().getHasUnit(2) || caster.isInZoneBattle() ||
				caster.isInZone(L2Zone.ZoneType.no_summon) || caster.getMountEngine().isMounted() || caster.getReflection() > 0 || caster.getX() < -166168)
			return false;

		if(caster.isInOlympiadMode())
		{
			caster.sendPacket(Msg.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return false;
		}

		return true;
	}
}
