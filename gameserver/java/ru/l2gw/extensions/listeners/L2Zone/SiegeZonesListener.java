package ru.l2gw.extensions.listeners.L2Zone;

import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.entity.siege.reinforce.Reinforce;
import ru.l2gw.gameserver.model.entity.siege.reinforce.TrapReinforce;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.EventTrigger;

/**
 * Oсновной листнер сиедж зоны
 * @author FlareDrakon
 *
 */
public class SiegeZonesListener extends PlayerChangedZonesListener
{

	@Override
	public void objectEntered(L2Zone zone, L2Character object)
	{
		if(object.isPlayer() && zone.isActive())
		{
			L2Player player = object.getPlayer();
			sendZoneStatus(player, true, zone.getEntityId());
		}

		super.objectEntered(zone, object);
	}

	@Override
	public void objectLeaved(L2Zone zone, L2Character object)
	{
		if(object.isPlayer() && zone.isActive())
		{
			L2Player player = object.getPlayer();
			player.startPvPFlag(null);
			sendZoneStatus(player, true, zone.getEntityId());
		}

		super.objectLeaved(zone, object);
	}

	/**
	 * Обновляет статус ловушек у текущей осады.
	 * Если игрок входит(enter == true), то будет отослано состояние трэпов.
	 * Если выходит, то трэпы будут простро выключены
	 * Если осада не запущена, то трепы выключатся.
	 * @param player игрок
	 * @param enter вход или выход игрока
	 * <p>
	 * TODO: обработка
	 */
	public void sendZoneStatus(L2Player player, boolean enter, int castleId)
	{
		SiegeUnit castle = ResidenceManager.getInstance().getBuildingById(castleId);

		if(enter)
		{
			if(castle.getReinforces() != null)
				for(Reinforce rf : castle.getReinforces().values())
				{
					if(rf.getType().equalsIgnoreCase("TRAP") && rf.getLevel() > 0)
						player.sendPacket(new EventTrigger(((TrapReinforce)rf).getEventId(), rf.isActive()));
				}
		}
		else
		{
			if(castle.getReinforces() != null)
				for(Reinforce rf : castle.getReinforces().values())
				{
					if(rf.getType().equalsIgnoreCase("TRAP"))
						player.sendPacket(new EventTrigger(((TrapReinforce) rf).getEventId(), false));
				}
		}
	}

}