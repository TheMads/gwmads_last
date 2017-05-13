package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.instances.L2TerritoryOutpostInstance;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author: rage
 * @date: 10.07.2010 14:52:11
 */
public class i_deinstall_outpost extends i_effect
{
	public i_deinstall_outpost(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(!cha.isPlayer())
			return;

		L2Clan clan = cha.getPlayer().getClan();
		if(clan != null && clan.getLeaderId() == cha.getObjectId() && clan.getCamp() instanceof L2TerritoryOutpostInstance)
			clan.removeCamp();
	}
}

