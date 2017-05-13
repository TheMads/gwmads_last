package ru.l2gw.extensions.listeners.L2Zone;

import ru.l2gw.extensions.listeners.L2ZoneEnterLeaveListener;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.EtcStatusUpdate;
import ru.l2gw.gameserver.serverpackets.ExSetCompassZoneCode;

import static ru.l2gw.gameserver.model.zone.L2Zone.ZoneType.danger;
import static ru.l2gw.gameserver.model.zone.L2Zone.ZoneType.no_radar;

public class PlayerChangedZonesListener extends L2ZoneEnterLeaveListener
{

	@Override
	public void objectEntered(L2Zone zone, L2Character object)
	{
		if(object.isPlayer())
		{
			if(zone.getTypes().contains(danger) && object.getInsideZones()[danger.ordinal()] == 1)
				object.sendPacket(new EtcStatusUpdate(object.getPlayer()));

			int currentComapss = object.getPlayer().getCurrentCompassZone();
			if(currentComapss != object.getPlayer().getLastCompassZone() || zone.getTypes().contains(no_radar))
			{
				object.getPlayer().setLastComapssZone(currentComapss);
				object.sendPacket(new ExSetCompassZoneCode(object.getPlayer()));
			}

			object.getPlayer().broadcastRelation();
		}
	}

	@Override
	public void objectLeaved(L2Zone zone, L2Character object)
	{
		if(object.isPlayer())
		{
			if(zone.getTypes().contains(danger) && object.getInsideZones()[danger.ordinal()] == 0)
				object.sendPacket(new EtcStatusUpdate(object.getPlayer()));

			int currentComapss = object.getPlayer().getCurrentCompassZone();
			if(currentComapss != object.getPlayer().getLastCompassZone() || zone.getTypes().contains(no_radar))
			{
				object.getPlayer().setLastComapssZone(currentComapss);
				object.sendPacket(new ExSetCompassZoneCode(object.getPlayer()));
			}

			object.getPlayer().broadcastRelation();
		}
	}

	@Override
	public void sendZoneStatus(L2Zone zone, L2Player player)
	{
		if(zone.getTypes().contains(danger))
			player.sendPacket(new EtcStatusUpdate(player));

		int currentComapss = player.getCurrentCompassZone();
		if(currentComapss != player.getLastCompassZone() || zone.getTypes().contains(no_radar))
		{
			player.getPlayer().setLastComapssZone(currentComapss);
			player.sendPacket(new ExSetCompassZoneCode(player));
		}

		player.broadcastRelation();
	}
}
