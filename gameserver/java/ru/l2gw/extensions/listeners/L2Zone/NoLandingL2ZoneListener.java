package ru.l2gw.extensions.listeners.L2Zone;

import ru.l2gw.extensions.listeners.L2ZoneEnterLeaveListener;
import ru.l2gw.extensions.listeners.PropertyCollection;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

/**
 * Лисенер для Ноу ландинг зоны что не летали
 * @author Death
 * Rebuilding By FlareDrakon
 */
public class NoLandingL2ZoneListener extends L2ZoneEnterLeaveListener implements PropertyCollection
{
	@Override
	public void objectEntered(L2Zone zone, L2Character object)
	{
		L2Player player = object.getPlayer();

		if(player != null && player.isFlying())
		{
			player.addProperty(ZoneEnteredNoLandingFlying, System.currentTimeMillis());
			player.stopMove();
			player.sendPacket(new SystemMessage(SystemMessage.THIS_AREA_CANNOT_BE_ENTERED_WHILE_MOUNTED_ATOP_OF_A_WYVERN_YOU_WILL_BE_DISMOUNTED_FROM_YOUR_WYVERN_IF_YOU_DO_NOT_LEAVE));
			// Даём 5 секунд, что бы покинуть зону, запрещенную для полетов
			player.getMountEngine().addOtherDisMountTask(ThreadPoolManager.getInstance().scheduleAi(new Dismount(player), 5010, true));
		}
	}

	@Override
	public void objectLeaved(L2Zone zone, L2Character object)
	{
		L2Player player = object.getPlayer();

		if(player != null)
			player.addProperty(ZoneEnteredNoLandingFlying, 0L);
	}

	private static class Dismount implements Runnable
	{
		private L2Player player;

		public Dismount(L2Player player)
		{
			this.player = player;
		}

		public void run()
		{
			Long enterTime = (Long) player.getProperty(ZoneEnteredNoLandingFlying);

			if(enterTime == null || enterTime == 0)
				return;

			// Если он "влетел-вылетел-влетел"
			if(enterTime + 5000L > System.currentTimeMillis())
				return;
				
			if(player.isFlying())
			{
				player.getMountEngine().dismount();
				player.addProperty(ZoneEnteredNoLandingFlying, 0L);
			}
		}
	}

	@Override
	public void sendZoneStatus(L2Zone zone, L2Player object)
	{
	}
}
