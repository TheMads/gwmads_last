package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.instancemanager.SiegeManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.instances.L2SiegeHeadquarterInstance;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.skills.funcs.FuncFactory;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.util.Location;

/**
 * @author: rage
 * @date: 10.07.2010 13:51:54
 */
public class i_install_camp extends i_effect
{
	public i_install_camp(EffectTemplate template)
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
		if(clan == null)
			return;

		Siege siege = SiegeManager.getSiege(player);
		if(!TerritoryWarManager.getWar().isInProgress() && (siege == null || !siege.isInProgress()))
			return;

		L2SiegeHeadquarterInstance flag = new L2SiegeHeadquarterInstance(clan, IdFactory.getInstance().getNextId(), NpcTable.getTemplate(35062));
		try
		{
			flag.addStatFunc(FuncFactory.createFunc("Mul", Stats.MAX_HP, 0x50, 2500, flag));
			flag.addStatFunc(FuncFactory.createFunc("Mul", Stats.POWER_DEFENCE, 0x50, 4 * clan.getLevel(), flag));
			flag.addStatFunc(FuncFactory.createFunc("Mul", Stats.MAGIC_DEFENCE, 0x50, 4 * clan.getLevel(), flag));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		flag.setCurrentHpMp(flag.getMaxHp(), flag.getMaxMp());
		flag.setHeading(player.getHeading());
		flag.spawnMe(new Location(player.getX(), player.getY(), player.getZ()));
		flag.onSpawn();
		clan.setCamp(flag);
	}
}
