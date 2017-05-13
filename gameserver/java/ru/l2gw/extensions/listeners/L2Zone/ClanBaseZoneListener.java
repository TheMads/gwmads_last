package ru.l2gw.extensions.listeners.L2Zone;

import ru.l2gw.extensions.listeners.L2ZoneEnterLeaveListener;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.AgitDecoInfo;

/**
 * @author rage
 * @date 05.08.2010 11:51:25
 */
public class ClanBaseZoneListener extends L2ZoneEnterLeaveListener
{
	@Override
	public void objectEntered(L2Zone zone, L2Character object)
	{
		if(object.isPlayer())
		{
			L2Player player = object.getPlayer();
			if(player.getClanId() != 0)
			{
				L2Clan clan = player.getClan();

				if(clan.getHasHideout() == zone.getEntityId() || clan.getHaseBase() == zone.getEntityId())
				{
					SiegeUnit siegeUnit = ResidenceManager.getInstance().getBuildingById(zone.getEntityId());
					// Send decoration packet
					if(siegeUnit.isClanHall)
						player.sendPacket(new AgitDecoInfo(siegeUnit));

					if(siegeUnit.getFunction(SiegeUnit.FUNC_RESTORE_HP) != null)
						player.setRestoreHpLevel(siegeUnit.getFunction(SiegeUnit.FUNC_RESTORE_HP).getLvl());
					if(siegeUnit.getFunction(SiegeUnit.FUNC_RESTORE_MP) != null)
						player.setRestoreMpLevel(siegeUnit.getFunction(SiegeUnit.FUNC_RESTORE_MP).getLvl());
				}
			}
		}
	}

	@Override
	public void objectLeaved(L2Zone zone, L2Character object)
	{
		if(object.isPlayer())
		{
			L2Player player = object.getPlayer();
			player.setRestoreHpLevel(-1);
			player.setRestoreMpLevel(-1);
		}
	}

	@Override
	public void sendZoneStatus(L2Zone zone, L2Player player)
	{
		if(player.getClanId() != 0 && player.isInClanBase())
		{
			L2Clan clan = player.getClan();

			if(clan != null && (clan.getHasHideout() == zone.getEntityId() || clan.getHaseBase() == zone.getEntityId()))
			{
				SiegeUnit siegeUnit = ResidenceManager.getInstance().getBuildingById(zone.getEntityId());

				if(siegeUnit.getFunction(SiegeUnit.FUNC_RESTORE_HP) != null)
					player.setRestoreHpLevel(siegeUnit.getFunction(SiegeUnit.FUNC_RESTORE_HP).getLvl());
				if(siegeUnit.getFunction(SiegeUnit.FUNC_RESTORE_MP) != null)
					player.setRestoreMpLevel(siegeUnit.getFunction(SiegeUnit.FUNC_RESTORE_MP).getLvl());
			}
			else
			{
				player.setRestoreHpLevel(-1);
				player.setRestoreMpLevel(-1);
			}

		}
	}
}
