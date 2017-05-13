package ru.l2gw.gameserver.controllers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.extensions.listeners.PropertyCollection;
import ru.l2gw.extensions.listeners.engine.DefaultListenerEngine;
import ru.l2gw.extensions.listeners.engine.ListenerEngine;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.DayNightSpawnManager;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@SuppressWarnings( { "nls", "unqualified-field-access", "boxing" })
public class GameTimeController
{
	private static final Log _log = LogFactory.getLog(GameTimeController.class.getName());

	public static final int TICKS_PER_SECOND = 5;
	public static final int MILLIS_IN_TICK = 1000 / TICKS_PER_SECOND;

	private static GameTimeController _instance = new GameTimeController();

	private static int _gameTicks;
	private static long _gameStartTime;
	private static boolean _isNight;

	private TimerThread _timer;

	public long _startMoveTime;

	private final ListenerEngine<GameTimeController> listenerEngine = new DefaultListenerEngine<GameTimeController>(this);

	/**
	 * one ingame day is 240 real minutes
	 */
	public static GameTimeController getInstance()
	{
		return _instance;
	}

	private GameTimeController()
	{
		int diff = ((Calendar.getInstance().get(Calendar.HOUR_OF_DAY) % 4) * 60 * 60 + Calendar.getInstance().get(Calendar.MINUTE) * 60 + Calendar.getInstance().get(Calendar.SECOND)) * 1000;
		_gameStartTime = System.currentTimeMillis() - diff; // offset so that the server starts a day begin
		_gameTicks = diff / MILLIS_IN_TICK; // offset so that the server starts a day begin

		_timer = new TimerThread();
		_timer.start();

		diff = ((10 - (Calendar.getInstance().get(Calendar.MINUTE) % 10)) * 60 - Calendar.getInstance().get(Calendar.SECOND) + 5) * 1000;

		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new TimerWatcher(), 0, 1000);
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new BroadcastSunState(), diff, 600000);
	}

	public boolean isNowNight()
	{
		return _isNight;
	}

	public int getGameTime()
	{
		return _gameTicks / (TICKS_PER_SECOND * 10);
	}

	public static int getGameTicks()
	{
		return _gameTicks;
	}
/*
	private void moveObjects()
	{
		for(L2WorldRegion region : L2World.getActiveRegions())
		{
			if(region != null && region.isActive() && region.getObjectsSize() > 0)
				for(L2Character cha : region.getCharactersList(-1))
					if(cha != null)
					{
						if(!cha.isMoving || !cha.hasAI() || cha.getAI().isGlobalAI())
							continue;
						cha.updatePosition(_gameTicks);
					}
		}

		Set<L2Character> _custom_objects = L2World.getCustomMoveObjects();

		synchronized (_custom_objects)
		{
			for(L2Character cha : _custom_objects)
			{
				if(cha == null || !cha.isMoving || !cha.hasAI() && !cha.isVehicle())
					continue;
				if(!cha.isVehicle() && !cha.getAI().isGlobalAI())
					continue;
				cha.updatePosition(_gameTicks);
			}
		}
	}
*/
	public void stopTimer()
	{
		_timer._stop = true;
	}

	class TimerThread extends Thread
	{
		protected boolean _stop;
		protected Exception _error;

		public TimerThread()
		{
			super("TimerThread");
			setDaemon(true);
			setPriority(MAX_PRIORITY);
			_stop = false;
			_error = null;
		}

		@Override
		public void run()
		{
			try
			{
				while(!_stop)
				{
					long currentTime = System.currentTimeMillis();
					_startMoveTime = currentTime;
					_gameTicks = (int) ((currentTime - _gameStartTime) / MILLIS_IN_TICK);

					//moveObjects();

					currentTime = System.currentTimeMillis();

					// Пересчитываем _gameTicks еще раз, для большей точности, т.к. moveObjects() мог занять много времени
					_gameTicks = (int) ((currentTime - _gameStartTime) / MILLIS_IN_TICK);

					// Интервалы между вызовами минимум Config.MOVE_DELAY мсек
					//long sleepTime = Config.MOVE_DELAY - (currentTime - _startMoveTime);
					long sleepTime = MILLIS_IN_TICK - (currentTime - _startMoveTime);
					if(sleepTime > 0)
						sleep(sleepTime);
				}
			}
			catch(Exception e)
			{
				_error = e;
			}

			_log.info("TimerThread was canceled");
		}
	}

	class TimerWatcher implements Runnable
	{
		public void run()
		{
			if(!_timer.isAlive())
			{
				String time = (new SimpleDateFormat("HH:mm:ss")).format(new Date());
				_log.warn(time + " TimerThread stop with following error. restart it.");
				if(_timer._error != null)
					_timer._error.printStackTrace();

				_timer = new TimerThread();
				_timer.start();
			}
		}
	}

	public class BroadcastSunState implements Runnable
	{
		public void run()
		{
			int h = getGameTime() / 60 % 24; // Time in hour
			boolean tempIsNight;
			if(Config.DAY_STATUS_SUN_RISE_AT > Config.DAY_STATUS_SUN_SET_AT)
				tempIsNight = h < Config.DAY_STATUS_SUN_RISE_AT && h >= Config.DAY_STATUS_SUN_SET_AT;
			else
				tempIsNight = h < Config.DAY_STATUS_SUN_RISE_AT || h >= Config.DAY_STATUS_SUN_SET_AT;

			if(tempIsNight != isNowNight())
			{
				_isNight = tempIsNight;

				DayNightSpawnManager.getInstance().notifyChangeMode();

				getListenerEngine().firePropertyChanged(PropertyCollection.GameTimeControllerDayNightChange, getInstance(), !_isNight, _isNight);

				for(L2Player player : L2ObjectsStorage.getAllPlayers())
					player.checkDayNightMessages();
			}

			L2GameServerPacket packet = null;
			if(h == Config.DAY_STATUS_SUN_RISE_AT + 1)
				packet = Msg.SunRise;
			else if(h == Config.DAY_STATUS_SUN_SET_AT + 1)
				packet = Msg.SunSet; // Set client day/night state to night

			if(packet != null)
				for(L2Player player : L2ObjectsStorage.getAllPlayers())
					player.sendPacket(packet);
		}
	}

	public ListenerEngine<GameTimeController> getListenerEngine()
	{
		return listenerEngine;
	}
}