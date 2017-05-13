package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 19.08.2010 15:07:16
 */
public class ConditionCanFlyingTransform extends ConditionCanTransform
{
	@Override
	public boolean testImpl(Env env)
	{
		if(super.testImpl(env))
		{
			L2Player player = (L2Player) env.character;
			if(player.getPet() != null)
			{
				player.sendPacket(Msg.YOU_CANNOT_POLYMORPH_WHEN_YOU_HAVE_SUMMONED_A_SERVITOR_PET);
				return false;
			}
			if(player.getX() > -166168)
				return false;
		}
		return true;
	}
}
