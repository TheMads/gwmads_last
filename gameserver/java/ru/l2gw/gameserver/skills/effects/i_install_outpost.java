package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2TerritoryOutpostInstance;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.util.Util;

/**
 * @author: rage
 * @date: 10.07.2010 14:33:59
 */
public class i_install_outpost extends i_effect
{
	public i_install_outpost(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(!cha.isPlayer())
			return;

		L2Player player = (L2Player) cha;
		L2Clan clan = player.getClan();
		if(clan == null || !TerritoryWarManager.getWar().isInProgress())
			return;

		L2TerritoryOutpostInstance outpost = new L2TerritoryOutpostInstance(clan, IdFactory.getInstance().getNextId(), NpcTable.getTemplate(36590));
		outpost.setCurrentHpMp(outpost.getMaxHp(), outpost.getMaxMp());
		outpost.setHeading(player.getHeading());
		outpost.spawnMe(Util.getPointInRadius(player.getLoc(), 80, (int) Util.convertHeadingToDegree(player.getHeading())));
		outpost.onSpawn();
		clan.setCamp(outpost);
	}
}

