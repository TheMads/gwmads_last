package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author rage
 * @date 23.06.2010 16:12:49
 */
public class i_myteleport extends i_effect
{
	public i_myteleport(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(!cha.isPlayer())
			return;

		L2Player player = cha.getPlayer();
		String slot = player.getSessionVar("tele_slot");
		if(slot != null)
		{
			player.getTeleportBook().teleportToBookmark(Integer.parseInt(slot));
			player.setSessionVar("tele_slot", null);
		}
	}
}