package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.Territory;
import ru.l2gw.gameserver.model.entity.siege.territory.TerritoryWar;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2TerritoryOutpostInstance;
import ru.l2gw.gameserver.model.instances.L2TerritoryWardInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.tables.SpawnTable;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author: rage
 * @date: 10.07.2010 15:24:22
 */
public class i_ward_possess extends i_effect
{
	public i_ward_possess(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(!cha.isPlayer() || !TerritoryWarManager.getWar().isInProgress())
			return;

		L2Player player = (L2Player) cha;
		L2Clan clan = player.getClan();
		L2ItemInstance wardItem = player.getActiveWeaponInstance();

		if(clan == null || clan.getHasCastle() == 0 || !player.isCombatFlagEquipped() || wardItem == null || !wardItem.isTerritoryWard())
			return;

		for(Env env : targets)
		{
			if(!(env.target instanceof L2TerritoryOutpostInstance))
				continue;

			L2TerritoryOutpostInstance outpost = (L2TerritoryOutpostInstance) env.target;
			if(outpost.getOwner() != clan)
				continue;

			int wardId = wardItem.getItemId() - 13479;
			L2TerritoryWardInstance ward = TerritoryWarManager.getWar().getWardByTerritoryId(wardId);
			TerritoryWarManager.getWar().removeSpawnedWard(ward);
			ward.deleteMe();
			player.destroyItem("WardPossess", wardItem.getObjectId(), 1, outpost, true);
			player.setCombatFlagEquipped(false);
			TerritoryWar.broadcastToPlayers(new SystemMessage(SystemMessage.CLAN_S1_HAS_SUCCEEDED_IN_CAPTURING_S2_S_TERRITORY_WARD).addString(clan.getName()).addHideoutName(wardId));

			Territory terr = TerritoryWarManager.getTerritoryByWardId(wardId);
			if(terr != null)
			{
				terr.removeWard(wardId);
				SpawnTable.getInstance().stopEventSpawn("territory_ward_" + wardId + "_" + terr.getId(), true);
				terr.updateWards();
				//terr.removeWardSkill(wardId);
			}

			terr = TerritoryWarManager.getTerritoryById(player.getTerritoryId());
			terr.addWardId(wardId);
			terr.updateWards();
			//terr.addWardSkill(wardId);
			SpawnTable.getInstance().startEventSpawn("territory_ward_" + wardId + "_" + terr.getId());
			_log.info("TerritoryWarManager: " + ResidenceManager.getInstance().getCastleById(wardId - 80).getName() + " Ward captured by " + ResidenceManager.getInstance().getCastleById(player.getTerritoryId() - 80).getName() + " territory by " + player);
		}
	}
}

