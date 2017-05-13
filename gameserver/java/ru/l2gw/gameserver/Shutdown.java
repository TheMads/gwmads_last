package ru.l2gw.gameserver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.instancemanager.CoupleManager;
import ru.l2gw.gameserver.instancemanager.CursedWeaponsManager;
import ru.l2gw.gameserver.instancemanager.FieldCycleManager;
import ru.l2gw.gameserver.instancemanager.RaidBossSpawnManager;
import ru.l2gw.gameserver.loginservercon.LSConnection;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2World;
import ru.l2gw.gameserver.model.entity.SevenSigns;
import ru.l2gw.gameserver.model.entity.SevenSignsFestival.SevenSignsFestival;
import ru.l2gw.gameserver.model.entity.olympiad.Olympiad;
import ru.l2gw.gameserver.model.mail.MailController;
import ru.l2gw.gameserver.pservercon.PSConnection;
import ru.l2gw.gameserver.serverpackets.ExShowScreenMessage;
import ru.l2gw.gameserver.tables.DoorTable;
import ru.l2gw.util.Util;

@SuppressWarnings( { "nls", "unqualified-field-access", "boxing" })
public class Shutdown extends Thread
{
	private static final Log _log = LogFactory.getLog(Shutdown.class.getName());

	private static Shutdown _instance;
	private static Shutdown _counterInstance = null;

	private int secondsShut;
	private int shutdownMode;

	public static final int SIGTERM = 0;
	public static final int GM_SHUTDOWN = 1;
	public static final int GM_RESTART = 2;
	public static final int ABORT = 3;
	private static String[] _modeText = { "brought down", "brought down", "restarting", "aborting" };
	private static boolean old_ = Config.OLD_SHUTDOWN_MSG;

	public int getSeconds()
	{
		if(_counterInstance != null)
			return _counterInstance.secondsShut;
		return -1;
	}

	public int getMode()
	{
		if(_counterInstance != null)
			return _counterInstance.shutdownMode;
		return -1;
	}

	/**
	 * This function starts a shutdown countdown from Telnet (Copied from Function startShutdown())
	 *
	 * @param IP		    IP Which Issued shutdown command
	 * @param seconds	   seconds untill shutdown
	 * @param restart	   true if the server will restart after shutdown
	 */
	public void startTelnetShutdown(String IP, int seconds, boolean restart)
	{
		_log.warn("IP: " + IP + " issued shutdown command. " + _modeText[shutdownMode] + " in " + seconds + " seconds!");
		if(old_)
		{
			Announcements _an = Announcements.getInstance();
			_an.announceToAll("This server will be " + _modeText[shutdownMode] + " in " + seconds + " seconds!");
		}
		else
		{
			ExShowScreenMessage sm = new ExShowScreenMessage("This server will be " + _modeText[shutdownMode] + " in " + seconds + " seconds!", 10000, ExShowScreenMessage.ScreenMessageAlign.BOTTOM_RIGHT, false);
			for(L2Player player : L2ObjectsStorage.getAllPlayers())
				player.sendPacket(sm);
		}
		if(_counterInstance != null)
			_counterInstance._abort();
		_counterInstance = new Shutdown(seconds, restart);
		_counterInstance.start();
	}

	public void setAutoRestart(int seconds)
	{
		_log.warn("AutoRestart scheduled through " + Util.formatTime(seconds));
		if(_counterInstance != null)
			_counterInstance._abort();
		_counterInstance = new Shutdown(seconds, true);
		_counterInstance.start();
	}

	/**
	 * This function aborts a running countdown
	 *
	 * @param IP		    IP Which Issued shutdown command
	 */
	public void telnetAbort(String IP)
	{
		_log.warn("IP: " + IP + " issued shutdown ABORT. " + _modeText[shutdownMode] + " has been stopped!");
		if(old_)
		{
			Announcements _an = Announcements.getInstance();
			_an.announceToAll("This server aborts " + _modeText[shutdownMode] + " and continues normal operation!");
		}
		else
		{
			ExShowScreenMessage sm = new ExShowScreenMessage("This server aborts " + _modeText[shutdownMode] + " and continues normal operation!", 10000, ExShowScreenMessage.ScreenMessageAlign.BOTTOM_RIGHT, false);
			for(L2Player player : L2ObjectsStorage.getAllPlayers())
				player.sendPacket(sm);
		}

		if(_counterInstance != null)
			_counterInstance._abort();
	}

	/**
	 * Default constucter is only used internal to create the shutdown-hook instance
	 *
	 */
	public Shutdown()
	{
		secondsShut = -1;
		shutdownMode = SIGTERM;
	}

	/**
	 * This creates a countdown instance of Shutdown.
	 *
	 * @param seconds	how many seconds until shutdown
	 * @param restart	true is the server shall restart after shutdown
	 *
	 */
	public Shutdown(int seconds, boolean restart)
	{
		if(seconds < 0)
			seconds = 0;
		secondsShut = seconds;
		if(restart)
			shutdownMode = GM_RESTART;
		else
			shutdownMode = GM_SHUTDOWN;
	}

	/**
	 * get the shutdown-hook instance
	 * the shutdown-hook instance is created by the first call of this function,
	 * but it has to be registrered externaly.
	 *
	 * @return instance of Shutdown, to be used as shutdown hook
	 */
	public static Shutdown getInstance()
	{
		if(_instance == null)
			_instance = new Shutdown();
		return _instance;
	}

	/**
	 * this function is called, when a new thread starts
	 *
	 * if this thread is the thread of getInstance, then this is the shutdown hook
	 * and we save all data and disconnect all clients.
	 *
	 * after this thread ends, the server will completely exit
	 *
	 * if this is not the thread of getInstance, then this is a countdown thread.
	 * we start the countdown, and when we finished it, and it was not aborted,
	 * we tell the shutdown-hook why we call exit, and then call exit
	 *
	 * when the exit status of the server is 1, startServer.sh / startServer.bat
	 * will restart the server.
	 *
	 * Логгинг в этом методе не работает!!!
	 */
	@Override
	public void run()
	{
		if(this == _instance)
		{
			if(Config.PRODUCT_SHOP_ENABLED)
				PSConnection.getInstance().shutdown();

			LSConnection.getInstance().shutdown();
			System.out.println("Shutting down scripts.");
			// Вызвать выключение у скриптов
			Scripts.getInstance().shutdown();

			// ensure all services are stopped
			// stop all scheduled tasks
			saveData();
			try
			{
				ThreadPoolManager.getInstance().shutdown();
			}
			catch(Throwable t)
			{}
			// last byebye, save all data and quit this server
			// logging doesn't works here :(
			LSConnection.getInstance().shutdown();
			// saveData sends messages to exit players, so shutdown selector after it
			try
			{
				System.out.println("Shutting down selector.");
				GameServer.gameServer.getSelectorThread().shutdown();
				GameServer.gameServer.getSelectorThread().setDaemon(true);
			}
			catch(Throwable t)
			{}
			// commit data, last chance
			try
			{
				System.out.println("Shutting down database communication.");
				DatabaseFactory.getInstance().shutdown();
			}
			catch(Throwable t)
			{}
			// server will quit, when this function ends.
			if(_instance.shutdownMode == GM_RESTART)
				Runtime.getRuntime().halt(2);
			else
				Runtime.getRuntime().halt(0);
		}
		else
		{
			// gm shutdown: send warnings and then call exit to start shutdown sequence
			countdown();
			// last point where logging is operational :(
			System.out.println("GM shutdown countdown is over. " + _modeText[shutdownMode] + " NOW!");
			switch(shutdownMode)
			{
				case GM_SHUTDOWN:
					_instance.setMode(GM_SHUTDOWN);
					System.exit(0);
					break;
				case GM_RESTART:
					_instance.setMode(GM_RESTART);
					System.exit(2);
					break;
			}
		}
	}

	/**
	 * This functions starts a shutdown countdown
	 *
	 * @param player	GM who issued the shutdown command
	 * @param seconds		seconds until shutdown
	 * @param restart		true if the server will restart after shutdown
	 */
	public void startShutdown(L2Player player, int seconds, boolean restart)
	{
		_log.warn("GM: " + player.getName() + "(" + player.getObjectId() + ") issued shutdown command. " + _modeText[shutdownMode] + " in " + seconds + " seconds!");
		if(shutdownMode > 0)
			if(old_)
			{
				Announcements _an = Announcements.getInstance();
				_an.announceToAll("This server will be brought down in " + seconds + " seconds");
			}
			else
			{
				ExShowScreenMessage sm = new ExShowScreenMessage("This server will be brought down in " + seconds + " seconds", 3000, ExShowScreenMessage.ScreenMessageAlign.BOTTOM_RIGHT, false);
				for(L2Player player1 : L2ObjectsStorage.getAllPlayers())
					player1.sendPacket(sm);
			}

		if(_counterInstance != null)
			_counterInstance._abort();

		//the main instance should only run for shutdown hook, so we start a new instance
		_counterInstance = new Shutdown(seconds, restart);
		_counterInstance.start();
	}

	/**
	 * This function aborts a running countdown
	 *
	 * @param player	GM who issued the abort command
	 */
	public void abort(L2Player player)
	{
		_log.warn("GM: " + player.getName() + "(" + player.getObjectId() + ") issued shutdown ABORT. " + _modeText[shutdownMode] + " has been stopped!");
		if(old_)
		{
			Announcements _an = Announcements.getInstance();
			_an.announceToAll("This server aborts " + _modeText[shutdownMode] + " and continues normal operation!");
		}
		else
		{
			ExShowScreenMessage sm = new ExShowScreenMessage("This server aborts " + _modeText[shutdownMode] + " and continues normal operation!", 3000, ExShowScreenMessage.ScreenMessageAlign.BOTTOM_RIGHT, false);
			for(L2Player player1 : L2ObjectsStorage.getAllPlayers())
				player1.sendPacket(sm);
		}

		if(_counterInstance != null)
			_counterInstance._abort();
	}

	/**
	 * set the shutdown mode
	 * @param mode	what mode shall be set
	 */
	private void setMode(int mode)
	{
		shutdownMode = mode;
	}

	/**
	 * set shutdown mode to ABORT
	 */
	private void _abort()
	{
		shutdownMode = ABORT;
	}

	/**
	 * this counts the countdown and reports it to all players
	 * countdown is aborted if mode changes to ABORT
	 */
	private void countdown()
	{
		Announcements _an = Announcements.getInstance();

		try
		{
			while(secondsShut > 0)
			{

				switch(secondsShut)
				{
					case 1800:
						if(old_)
							_an.announceToAll("Attention Players! This server will be brought down in 30 minutes. Please avoid using Gatekeepers or performing other character actions until after the downtime and find a safe place to log out at this time. Thank you!");
						else
						{
							ExShowScreenMessage sm = new ExShowScreenMessage("Attention Players! This server will be brought down in 30 minutes.", 3000, ExShowScreenMessage.ScreenMessageAlign.BOTTOM_RIGHT, false);
							for(L2Player player : L2ObjectsStorage.getAllPlayers())
								player.sendPacket(sm);
						}
						break;
					case 600:
						if(old_)
							_an.announceToAll("Attention Players! This server will be brought down in 10 minutes. Please avoid using Gatekeepers or performing other character actions until after the downtime and find a safe place to log out at this time. Thank you!");
						else
						{
							ExShowScreenMessage sm = new ExShowScreenMessage("Attention Players! This server will be brought down in 10 minutes.", 3000, ExShowScreenMessage.ScreenMessageAlign.BOTTOM_RIGHT, false);
							for(L2Player player : L2ObjectsStorage.getAllPlayers())
								player.sendPacket(sm);
						}
						break;
					case 300:
						if(old_)
							_an.announceToAll("Attention Players! This server will be brought down in 5 minutes. Please avoid using Gatekeepers or performing other character actions until after the downtime and find a safe place to log out at this time. Thank you!");
						else
						{
							ExShowScreenMessage sm = new ExShowScreenMessage("Attention Players! This server will be brought down in 5 minutes.", 3000, ExShowScreenMessage.ScreenMessageAlign.BOTTOM_RIGHT, false);
							for(L2Player player : L2ObjectsStorage.getAllPlayers())
								player.sendPacket(sm);
						}
						break;
					case 60:
						if(old_)
							_an.announceToAll("Attention Players! This server will be brought down in 1 minute. Please avoid using Gatekeepers or performing other character actions until after the downtime and find a safe place to log out at this time. Thank you!");
						else
						{
							ExShowScreenMessage sm = new ExShowScreenMessage("Attention Players! This server will be brought down in 1 minute", 3000, ExShowScreenMessage.ScreenMessageAlign.TOP_CENTER, true);
							for(L2Player player : L2ObjectsStorage.getAllPlayers())
								player.sendPacket(sm);
						}
						if(!Config.DONTLOADSPAWN)
							L2World.deleteVisibleNpcSpawns();
						break;
					case 30:
						if(old_)
							_an.announceToAll("This server will be " + _modeText[shutdownMode] + " momentally!");
						else
						{
							ExShowScreenMessage sm = new ExShowScreenMessage("This server will be " + _modeText[shutdownMode] + " momentally!", 30000, ExShowScreenMessage.ScreenMessageAlign.TOP_CENTER, true);
							for(L2Player player : L2ObjectsStorage.getAllPlayers())
								player.sendPacket(sm);
						}
						break;
				}

				secondsShut--;

				int delay = 1000; //milliseconds
				Thread.sleep(delay);

				if(shutdownMode == ABORT)
					break;
			}
		}
		catch(InterruptedException e)
		{
			//this will never happen
		}
	}

	/**
	 * this sends a last byebye, disconnects all players and saves data
	 *
	 */
	private void saveData()
	{
		switch(shutdownMode)
		{
			case SIGTERM:
				System.err.println("SIGTERM received. Shutting down NOW!");
				break;
			case GM_SHUTDOWN:
				System.err.println("GM shutdown received. Shutting down NOW!");
				break;
			case GM_RESTART:
				System.err.println("GM restart received. Restarting NOW!");
				break;
		}

		disconnectAllCharacters();

		RaidBossSpawnManager.getInstance().saveData();
		System.out.println("RaidBossSpawnManager: data saved.");

		// Seven Signs data is now saved along with Festival data.
		if(!SevenSigns.getInstance().isSealValidationPeriod())
		{
			SevenSignsFestival.getInstance().saveFestivalData();
			System.out.println("Seven Signs Festival data saved.");
		}

		// Save Seven Signs data before closing. :)
		SevenSigns.getInstance().saveSevenSignsData(null);
		System.out.println("Seven Signs data saved.");

		try
		{
			Olympiad.saveProperties();
			Olympiad.saveNobleData();
			System.out.println("Olympiad System: Data saved!");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		DoorTable.stopAutoOpenTask();
		
		if(Config.ALLOW_WEDDING)
		{
			CoupleManager.getInstance().store();
			System.out.println("Couples: Data saved!");
		}

		CursedWeaponsManager.getInstance().saveData();
		System.out.println("CursedWeaponsManager: Data saved!");

		FieldCycleManager.shutdown();

		MailController.getInstance().stopCleanTask();
		System.out.println("All Data saved. All players disconnected, shutting down.");
		try
		{
			int delay = 5000;
			Thread.sleep(delay);
		}
		catch(InterruptedException e)
		{
			//never happens :p
		}
	}

	/**
	 * this disconnects all clients from the server
	 *
	 */
	private void disconnectAllCharacters()
	{
		for(L2Player player : L2ObjectsStorage.getAllPlayers())
			//Logout Character
			try
			{
				if(player.isInOfflineMode())
				{
					player.logout(true, false, false);
					if(player.getNetConnection() != null)
						player.getNetConnection().disconnectOffline();
				}
				else
				{
					player.sendPacket(Msg.ExRestartClient);
					player.logout(true, false, true);
				}
			}
			catch(Throwable t)
			{
				System.out.println("Error while disconnect char: " + player.getName());
				t.printStackTrace();
			}
		try
		{
			Thread.sleep(15000);
		}
		catch(Throwable t)
		{
			System.out.println(t);
		}
	}
}