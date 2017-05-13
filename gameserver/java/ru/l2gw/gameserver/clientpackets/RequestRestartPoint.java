package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.instances.L2TerritoryOutpostInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.Die;
import ru.l2gw.gameserver.serverpackets.Revive;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.util.Util;

/**
 * PointType:
 * 0 - To Village
 * 1 - ClanHall
 * 2 - Castle
 * 3 - Fortress
 * 4 - SiegeFlag
 * 5 - ResurectFixed
 */
public class RequestRestartPoint extends L2GameClientPacket
{
	protected int requestedPointType;
	protected boolean continuation;

	/**
	 * packet type id 0x7D
	 * format:    cd
	 */
	@Override
	public void readImpl()
	{
		requestedPointType = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null || player.isInOlympiadMode() || player.isActionBlocked(L2Zone.BLOCKED_SKILL_RESURRECT))
			return;

		if(player.isFakeDeath())
		{
			player.stopEffectsByName("c_fake_death");
			player.sendUserInfo(true);
			player.broadcastPacket(new Revive(player));
			return;
		}

		if(!player.isDead() && !player.isGM())
		{
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestRestartPoint.Cheating", player));
			Util.handleIllegalPlayerAction(player, "RequestRestartPoint[55]", "Tried to use revive cheat", 1);
			return;
		}

		if(player.getX() < -166168 && requestedPointType > 0 && requestedPointType < 5)
			requestedPointType = 0;

		if(player.isInJail())
		{
			player.setIsPendingRevive(true);
			player.teleToLocation(-114648, -249384, -2984);
			return;
		}

		SiegeUnit unit;
		try
		{
			switch(requestedPointType)
			{
				case 0: // to closest town
					player.setIsPendingRevive(true);
					player.teleToClosestTown();
					break;
				case 1: // to clan hall;
					player.setIsPendingRevive(true);
					player.teleToClanhall();
					if(player.getClan() != null && player.getClan().getHasHideout() > 0)
					{
						unit = ResidenceManager.getInstance().getBuildingById(player.getClan().getHasHideout());
						if(unit != null && unit.getFunction(SiegeUnit.FUNC_RESTORE_EXP) != null)
							player.restoreExp(unit.getFunction(SiegeUnit.FUNC_RESTORE_EXP).getLvl());
					}
					break;
				case 2: // to castle
					player.setIsPendingRevive(true);
					unit = null;
					if(player.getClan() != null && player.getClan().getHasCastle() > 0)
					{
						unit = ResidenceManager.getInstance().getBuildingById(player.getClan().getHasCastle());
						if(unit.getFunction(SiegeUnit.FUNC_RESTORE_EXP) != null)
							player.restoreExp(unit.getFunction(SiegeUnit.FUNC_RESTORE_EXP).getLvl());
					}
					else if(TerritoryWarManager.getWar().isInProgress() && player.getTerritoryId() > 0)
						unit = ResidenceManager.getInstance().getBuildingById(player.getTerritoryId() - 80);

					if(unit != null)
						player.teleToLocation(unit.getRezidentZone().getSpawn(player), 0);
					else
						player.teleToClosestTown();
					break;
				case 3: // to fortress
					player.setIsPendingRevive(true);
					player.teleToFortress();
					if(player.getClan() != null && player.getClan().getHasFortress() > 0)
					{
						unit = ResidenceManager.getInstance().getBuildingById(player.getClan().getHasFortress());
						if(unit.getFunction(SiegeUnit.FUNC_RESTORE_EXP) != null)
							player.restoreExp(unit.getFunction(SiegeUnit.FUNC_RESTORE_EXP).getLvl());
					}
					break;
				case 4: // to headquarters
					L2Clan clan = player.getClan();
					L2NpcInstance camp = null;
					if(clan != null && clan.getCamp() != null && clan.getCamp().isInZone(L2Zone.ZoneType.headquarters))
						camp = clan.getCamp();
					else if(TerritoryWarManager.getWar().isInProgress() && player.getTerritoryId() > 0)
						camp = ResidenceManager.getInstance().getCastleById(player.getTerritoryId() - 80).getOwner().getCamp();
					if(camp != null)
					{
						int min = 50;
						int max = 150;
						if(camp instanceof L2TerritoryOutpostInstance)
						{
							min = 100;
							max = 150;
						}
						player.setIsPendingRevive(true);
						player.teleToLocation(GeoEngine.findPointToStay(camp.getX(), camp.getY(), camp.getZ(), min, max, 0));
					}
					else
						sendPacket(new Die(player));
				break;
				case 5: // Use Feather
					if(AdminTemplateManager.checkBoolean("resurrectFixed", player))
						player.doRevive(100);
					else if(player.getItemCountByItemId(L2Item.ITEM_ID_PHOENIX_FEATHER) > 0 && player.destroyItemByItemId("Resurrect", L2Item.ITEM_ID_PHOENIX_FEATHER, 1, null, true))
						player.doRevive();
					else
						sendPacket(new Die(player));
					break;
			}
		}
		catch(Throwable e)
		{
			_log.warn("unknow ressive type not impliment?" + requestedPointType);
			e.printStackTrace();
			player.teleToClosestTown();
		}
	}
}