package ru.l2gw.gameserver.model.entity.siege.territory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.Territory;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

/**
 * @author rage
 * @date 07.07.2010 10:32:04
 */
public class TerritoryWarTasks
{
	private static final Log _log = LogFactory.getLog("tw");
	/*
2402	1	a,The Territory War request period has ended.
2403	1	a,Territory War begins in 10 minutes!
2404	1	a,Territory War begins in 5 minutes!
2405	1	a,Territory War begins in 1 minute!
2406	1	a,$s1's territory war has begun.
2407	1	a,$s1's territory war has ended.
2794	1	a,The territory war channel and functions will now be deactivated.
2798	1	a,The territory war will end in $s1-hour(s).
2799	1	a,The territory war will end in $s1-minute(s).
2900	1	a,$s1-second(s) to the end of territory war!
2903	1	a,Territory war has begun.
2904	1	a,Territory war has ended.
2914	1	a,The territory war will begin in 20 minutes! Territory related functions (ie: battlefield channel, Disguise Scrolls, Transformations, etc...) can now be used.
	 */

	public static class TerritoryWarStartTask implements Runnable
	{
		private final TerritoryWar _tw;
		private long _delay;

		public TerritoryWarStartTask(TerritoryWar tw, long delay)
		{
			_tw = tw;
			_delay = delay;
		}

		public void run()
		{
			if(_tw.isInProgress())
				return;
			_log.info("TerritoryWarStartTask: run delay: " + _delay);
			if(_delay == 2 * 60 * 60) // 2 Hours
			{
				_delay = 100 * 60;
				TerritoryWar.broadcastToPlayers(Msg.THE_TERRITORY_WAR_REQUEST_PERIOD_HAS_ENDED);
				_tw.setStartTask(ThreadPoolManager.getInstance().scheduleGeneral(this, _delay * 1000));
			}
			else if(_delay == 100 * 60)
			{
				_delay = 10 * 60;
				TerritoryWar.broadcastToPlayers(Msg.THE_TERRITORY_WAR_WILL_BEGIN_IN_20_MINUTES_TERRITORY_RELATED_FUNCTIONS_CAN_NOW_BE_USED);
				_tw.setFunctionsActive(true);
				_tw.setStartTask(ThreadPoolManager.getInstance().scheduleGeneral(this, _delay * 1000));
			}
			else if(_delay == 10 * 60)
			{
				_delay = 5 * 60;
				TerritoryWar.broadcastToPlayers(Msg.TERRITORY_WAR_BEGINS_IN_10_MINUTES);
				_tw.setFunctionsActive(true);
				_tw.setStartTask(ThreadPoolManager.getInstance().scheduleGeneral(this, _delay * 1000));
			}
			else if(_delay == 5 * 60)
			{
				_delay = 4 * 60;
				TerritoryWar.broadcastToPlayers(Msg.TERRITORY_WAR_BEGINS_IN_5_MINUTES);
				_tw.setFunctionsActive(true);
				_tw.setStartTask(ThreadPoolManager.getInstance().scheduleGeneral(this, _delay * 1000));
			}
			else if(_delay == 4 * 60)
			{
				_delay = 0;
				TerritoryWar.broadcastToPlayers(Msg.TERRITORY_WAR_BEGINS_IN_1_MINUTES);
				_tw.setFunctionsActive(true);
				_tw.setStartTask(ThreadPoolManager.getInstance().scheduleGeneral(this, _tw.getWarDate().getTimeInMillis() - System.currentTimeMillis()));
			}
			else
			{
				_tw.setFunctionsActive(true);
				_tw.startWar();
				_tw.setStartTask(null);
			}
		}
	}

	public static class TerritoryWarEndTask implements Runnable
	{
		private final TerritoryWar _tw;
		private long _delay;

		public TerritoryWarEndTask(TerritoryWar tw, long delay)
		{
			_tw = tw;
			_delay = delay;
		}

		public void run()
		{
			if(_delay == 60 * 60)
			{
				_delay = 30 * 60;
				TerritoryWar.broadcastToPlayers(new SystemMessage(SystemMessage.THE_TERRITORY_WAR_WILL_END_IN_S1_HOURS).addNumber(1));
				_tw.setEndTask(ThreadPoolManager.getInstance().scheduleGeneral(this, _delay * 1000));
			}
			else if(_delay == 30 * 60)
			{
				_delay = 20 * 60;
				TerritoryWar.broadcastToPlayers(new SystemMessage(SystemMessage.THE_TERRITORY_WAR_WILL_END_IN_S1_MINUTES).addNumber(30));
				_tw.setEndTask(ThreadPoolManager.getInstance().scheduleGeneral(this, _delay * 1000));
			}
			else if(_delay == 20 * 60)
			{
				_delay = 5 * 60;
				TerritoryWar.broadcastToPlayers(new SystemMessage(SystemMessage.THE_TERRITORY_WAR_WILL_END_IN_S1_MINUTES).addNumber(10));
				_tw.setEndTask(ThreadPoolManager.getInstance().scheduleGeneral(this, _delay * 1000));
			}
			else if(_delay == 5 * 60)
			{
				_delay = 4 * 60;
				TerritoryWar.broadcastToPlayers(new SystemMessage(SystemMessage.THE_TERRITORY_WAR_WILL_END_IN_S1_MINUTES).addNumber(5));
				_tw.setEndTask(ThreadPoolManager.getInstance().scheduleGeneral(this, _delay * 1000));
			}
			else if(_delay == 4 * 60)
			{
				_delay = 30;
				TerritoryWar.broadcastToPlayers(new SystemMessage(SystemMessage.THE_TERRITORY_WAR_WILL_END_IN_S1_MINUTES).addNumber(1));
				_tw.setEndTask(ThreadPoolManager.getInstance().scheduleGeneral(this, _delay * 1000));
			}
			else if(_delay == 30)
			{
				_delay = 20;
				TerritoryWar.broadcastToPlayers(new SystemMessage(SystemMessage.S1_SECONDS_TO_THE_END_OF_TERRITORY_WAR).addNumber(30));
				_tw.setEndTask(ThreadPoolManager.getInstance().scheduleGeneral(this, _delay * 1000));
			}
			else if(_delay == 20)
			{
				_delay -= 10;
				TerritoryWar.broadcastToPlayers(new SystemMessage(SystemMessage.S1_SECONDS_TO_THE_END_OF_TERRITORY_WAR).addNumber(_delay));
				_tw.setEndTask(ThreadPoolManager.getInstance().scheduleGeneral(this, 1000));
			}
			else if(_delay <= 10)
			{
				_delay--;
				if(_delay == 0)
				{
					_tw.endWar();
					_tw.setEndTask(null);
					return;
				}
				TerritoryWar.broadcastToPlayers(new SystemMessage(SystemMessage.S1_SECONDS_TO_THE_END_OF_TERRITORY_WAR).addNumber(_delay));
				_tw.setEndTask(ThreadPoolManager.getInstance().scheduleGeneral(this, 1000));
			}
		}
	}

	public static class WarFameTask implements Runnable
	{
		public void run()
		{
			if(!TerritoryWarManager.getWar().isInProgress())
				return;

			long currTime = System.currentTimeMillis();
			for(Territory terr : TerritoryWarManager.getTerritories())
				if(terr.getOwner() != null)
				{
					for(L2Player player : terr.getCastle().getSiegeZone().getPlayers())
						if(player.getSiegeState() == 3 && player.getLastFameUpdate() + 5 * 60000 < currTime)
						{
							player.addFame(terr.getCastle().getSiege().getFamePoints());
							player.addBadges(0.5f);
							player.updateFameTime();
						}

					for(L2Player player : terr.getFort().getSiegeZone().getPlayers())
						if(player.getSiegeState() == 3 && player.getLastFameUpdate() + 5 * 60000 < currTime)
						{
							player.addFame(terr.getFort().getSiege().getFamePoints());
							player.addBadges(0.5f);
							player.updateFameTime();
						}
				}
		}
	}

	public static class DisableWarFunctions implements Runnable
	{
		public void run()
		{
			TerritoryWarManager.getWar().deactivateFunctions();
		}
	}
}
