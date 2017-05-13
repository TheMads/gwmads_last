package ru.l2gw.gameserver.model.entity.olympiad;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GCSArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.Announcements;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.InstanceManager;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Party;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.Hero;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.instances.L2OlympiadManagerInstance;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.templates.StatsSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Future;

/**
 * @author rage
 *         all rights reserved :-)
 */
public class Olympiad
{
	protected static final Log _olyLog = LogFactory.getLog("olymp");
	protected static final Log _log = LogFactory.getLog(Olympiad.class.getName());

	protected static Map<Integer, StatsSet> _nobles;
	protected static GCSArray<Integer> _nonClassBasedRegisters = new GCSArray<Integer>();
	protected static Map<Integer, GCSArray<Integer>> _classBasedRegisters = new FastMap<Integer, GCSArray<Integer>>().shared();
	protected static Map<Integer, OlympiadTeam> _teamBaseRegistered = new FastMap<Integer, OlympiadTeam>().shared();
	protected static GCSArray<String> registeredHWID = new GCSArray<>();

	protected static Map<Integer, List<String>> _classLeadres;
	protected static List<L2OlympiadManagerInstance> _olympiadManagers;
	protected static OlympiadInstance[] _instances = new OlympiadInstance[Config.ALT_OLY_MAX_ARENAS];

	protected static long _olympiadEnd;
	protected static long _validationEnd;
	protected static long _calculateEnd;
	protected static byte _period;
	protected static long _nextWeeklyChange;
	protected static int _currentCycle;
	protected static boolean _isOlympiadEnd;
	protected static boolean _compStarted;
	protected static boolean _inCompPeriod;

	protected static final byte COMP_PERIOD = 0;
	protected static final byte VALID_PERIOD = 1;
	protected static final long VALIDATION_PERION_TIME = 24 * 60 * 60 * 1000; // 24 hours
	protected static final long CALCULATION_PERION_TIME = 12 * 60 * 60 * 1000; // 12 hours

	private static long _compEnd;
	private static Calendar _compStart;
	protected static Future<?> _scheduledCompStart = null;
	protected static Future<?> _scheduledCompEnd = null;
	protected static Future<?> _scheduledValdationTask = null;
	protected static Future<?> _scheduledOlympiadEnd = null;
	protected static Future<?> _scheduledManagerTask = null;
	protected static Future<?> _scheduledWeeklyTask = null;

	private static final String OLYMPIAD_DATA_FILE = "config/olympiad.properties";
	public static final String OLYMPIAD_HTML_FILE = "data/html/olympiad/";
	public static final int OLYMPIAD_TOKENS_ID = 13722;

	public static void load()
	{
		_olyLog.warn("Olympiad system initialization...");
		_nobles = new FastMap<Integer, StatsSet>();

		Properties OlympiadProperties = new Properties();
		try
		{
			InputStream is = new FileInputStream(new File("./" + OLYMPIAD_DATA_FILE));
			OlympiadProperties.load(is);
			is.close();
		}
		catch(java.io.IOException e)
		{
			_log.warn("Olympiad: can't load data " + e);
			_olyLog.warn("Olympiad: can't load data " + e);
			return;
		}

		_currentCycle = Integer.parseInt(OlympiadProperties.getProperty("CurrentCycle", "1"));
		_period = Byte.parseByte(OlympiadProperties.getProperty("Period", "0"));
		_olympiadEnd = Long.parseLong(OlympiadProperties.getProperty("OlympiadEnd", "0"));
		_validationEnd = Long.parseLong(OlympiadProperties.getProperty("ValdationEnd", "0"));
		_nextWeeklyChange = Long.parseLong(OlympiadProperties.getProperty("NextWeeklyChange", "0"));

		try
		{
			Connection con = DatabaseFactory.getInstance().getConnection();
			final String OLYMPIAD_LOAD_NOBLES = "SELECT * from olymp_nobles";

			PreparedStatement statement = con.prepareStatement(OLYMPIAD_LOAD_NOBLES);
			ResultSet rset = statement.executeQuery();

			while(rset.next())
			{
				StatsSet statDat = new StatsSet();
				statDat.set("class_id", rset.getInt("class_id"));
				statDat.set("char_name", rset.getString("char_name"));
				statDat.set("points", rset.getInt("points"));
				statDat.set("wins", rset.getInt("wins"));
				statDat.set("loos", rset.getInt("loos"));
				statDat.set("prev_points", rset.getInt("prev_points"));
				statDat.set("cb_matches", rset.getInt("cb_matches"));
				statDat.set("ncb_matches", rset.getInt("ncb_matches"));
				statDat.set("team_matches", rset.getInt("team_matches"));

				_nobles.put(rset.getInt("char_id"), statDat);
			}

			rset.close();
			statement.close();
			con.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		switch(_period)
		{
			case COMP_PERIOD:
				if(_olympiadEnd == 0 || _olympiadEnd < Calendar.getInstance().getTimeInMillis())
					calcOlympEndTime();
				else
					_isOlympiadEnd = false;
				break;
			case VALID_PERIOD:
				if(_validationEnd > Calendar.getInstance().getTimeInMillis())
				{
					_isOlympiadEnd = true;
					_scheduledValdationTask = ThreadPoolManager.getInstance().scheduleGeneral(new validPeriodEndTask(), getTimeToValidationEnd());
					_calculateEnd = _validationEnd - VALIDATION_PERION_TIME + CALCULATION_PERION_TIME;
					_log.info("Calculate end time: " + new Date(_calculateEnd));
				}
				else
				{
					_currentCycle++;
					_period = 0;
					resetNobles();
					saveNobleData();
					calcOlympEndTime();
				}
				break;
			default:
				_log.warn("Olympiad System: Omg something went wrong in loading!! Period = " + _period);
				_olyLog.warn("Omg something went wrong in loading!! Period = " + _period);
				return;
		}

		_log.info("Olympiad System: Loading Olympiad System....");
		_olyLog.info("Loading Olympiad System....");
		if(_period == 0)
		{
			_log.info("Olympiad System: Currently in Olympiad Period");
			_olyLog.info("Currently in Olympiad Period");
		}
		else
		{
			_log.info("Olympiad System: Currently in Validation Period");
			_olyLog.info("Currently in Validation Period");
		}

		_log.info("Olympiad System: Period Ends....");
		_olyLog.info("Period Ends....");

		long milliToEnd;
		if(_period == 0)
			milliToEnd = getTimeToOlympiadEnd();
		else
			milliToEnd = getTimeToValidationEnd();

		double numSecs = (milliToEnd / 1000) % 60;
		double countDown = ((milliToEnd / 1000) - numSecs) / 60;
		int numMins = (int) Math.floor(countDown % 60);
		countDown = (countDown - numMins) / 60;
		int numHours = (int) Math.floor(countDown % 24);
		int numDays = (int) Math.floor((countDown - numHours) / 24);

		_log.info("Olympiad System: In " + numDays + " days, " + numHours
				+ " hours and " + numMins + " mins.");
		_olyLog.info("In " + numDays + " days, " + numHours + " hours and " + numMins + " mins.");

		if(_period == 0)
		{
			if(_nextWeeklyChange == 0)
			{
				_nextWeeklyChange = calcNextWeeklyChangeDate();
				saveProperties();
				_olyLog.info("Olympiad System: Calculate next weekly change date: " + new Date(_nextWeeklyChange));
			}

			_olyLog.info("Olympiad System: Next weekly change date: " + new Date(_nextWeeklyChange));
		}

		_log.info("Olympiad System: Loaded " + _nobles.size() + " Nobles");
		_olyLog.info("Loaded " + _nobles.size() + " Nobles");
		loadClassLeaders();
		_olyLog.info("Loaded " + _classLeadres.size() + " Classes in rank");

		_olympiadManagers = new FastList<>();
		for(L2NpcInstance cha : L2ObjectsStorage.getAllNpcs())
			if(cha.getNpcId() == 31688)
				_olympiadManagers.add((L2OlympiadManagerInstance) cha);

		if(_period == 0)
			startCompPeriod();
	}

	public static boolean isInCompPeriod()
	{
		return _inCompPeriod;
	}

	private static class validPeriodEndTask implements Runnable
	{
		public void run()
		{
			_period = 0;
			_currentCycle++;

			if(_scheduledOlympiadEnd != null)
			{
				_scheduledOlympiadEnd.cancel(true);
				_scheduledOlympiadEnd = null;
			}
			if(_scheduledWeeklyTask != null)
			{
				_scheduledWeeklyTask.cancel(true);
				_scheduledWeeklyTask = null;
			}
			calcOlympEndTime();
			startCompPeriod();
		}
	}

	private static void startCompPeriod()
	{
		if(_period == 1)
			return;

		_compStart = Calendar.getInstance();
		_compStart.set(Calendar.HOUR_OF_DAY, Config.ALT_OLY_START_HOUR);
		_compStart.set(Calendar.MINUTE, Config.ALT_OLY_START_MIN);
		_compEnd = _compStart.getTimeInMillis() + Config.ALT_OLY_CPERIOD;

		if(_scheduledOlympiadEnd == null)
			_scheduledOlympiadEnd = ThreadPoolManager.getInstance().scheduleGeneral(new olympEndTask(), getTimeToOlympiadEnd());
		updateCompStatus();
		if(_scheduledWeeklyTask == null)
			_scheduledWeeklyTask = ThreadPoolManager.getInstance().scheduleGeneral(new WeeklyTask(), getTimeToWeekChange());
	}

	public static OlympiadInstance[] getOlympiadInstances()
	{
		return _instances;
	}

	private static class olympEndTask implements Runnable
	{

		public void run()
		{
			_olyLog.info("End olympiad games, cycle: " + _currentCycle);

			SystemMessage sm = new SystemMessage(SystemMessage.OLYMPIAD_PERIOD_S1_HAS_ENDED);
			sm.addNumber(_currentCycle);

			Announcements.getInstance().announceToAll(sm);
			//Announcements.getInstance().announceToAll("Olympiad Validation Period has began");

			_isOlympiadEnd = true;

			if(_scheduledManagerTask != null)
				_scheduledManagerTask.cancel(true);
			if(_scheduledWeeklyTask != null)
				_scheduledWeeklyTask.cancel(true);

			try
			{
				_olyLog.info("Abort unfinihed games");
				for(OlympiadInstance oi : _instances)
					if(oi != null && oi.getOlympiadGame() != null)
					{
						_olyLog.info("Abort game: " + oi.getOlympiadGame());
						oi.getOlympiadGame().teleportPlayersBack();
						oi.stopInstance();
					}
			}
			catch(Exception e)
			{
			}

			Calendar validationEnd = Calendar.getInstance();

			_validationEnd = validationEnd.getTimeInMillis() + VALIDATION_PERION_TIME; // 24 Hoours
			_olyLog.info("Validation Period end time: " + new Date(_validationEnd));

			_calculateEnd = System.currentTimeMillis() + CALCULATION_PERION_TIME; // 12 hours
			_olyLog.info("Calculation Period end time: " + new Date(_calculateEnd));

			saveNobleData();
			_period = 1;
			// Calculate New hero and remove old one
			Hero.computeNewHeroes(_currentCycle);

			_olyLog.info("Load class leaders for previos period");
			loadClassLeaders();
			resetNobles();

			try
			{
				saveProperties();
			}
			catch(Exception e)
			{
				_olyLog.warn("Olympiad System: Failed to save Olympiad configuration: " + e);
			}

			_scheduledValdationTask = ThreadPoolManager.getInstance().scheduleGeneral(new validPeriodEndTask(), getTimeToValidationEnd());
		}
	}

	public static void manualOlympEnd()
	{
		if(_scheduledValdationTask != null)
			_scheduledValdationTask.cancel(true);
		if(_scheduledOlympiadEnd != null)
			_scheduledOlympiadEnd.cancel(true);

		new olympEndTask().run();
	}

	public static void manualCalculateEnd()
	{
		_calculateEnd = 0;
	}

	public static void manualValidationEnd()
	{
		if(_scheduledValdationTask != null)
			_scheduledValdationTask.cancel(true);
		if(_scheduledOlympiadEnd != null)
			_scheduledOlympiadEnd.cancel(true);

		new validPeriodEndTask().run();
	}

	public static boolean manualChangeNobleStat(int objId, int points, int wins, int loos)
	{
		if(_nobles.containsKey(objId))
		{
			StatsSet noble = _nobles.get(objId);
			noble.set("points", noble.getInteger("points") + points);
			noble.set("wins", noble.getInteger("wins") + wins);
			noble.set("loos", noble.getInteger("loos") + loos);
			_nobles.put(objId, noble);
			return true;
		}
		return false;
	}

	private static synchronized void updateCompStatus()
	{
		_compStarted = false;

		long milliToStart = getTimeToCompBegin();

		double numSecs = (milliToStart / 1000) % 60;
		double countDown = ((milliToStart / 1000) - numSecs) / 60;
		int numMins = (int) Math.floor(countDown % 60);
		countDown = (countDown - numMins) / 60;
		int numHours = (int) Math.floor(countDown % 24);
		int numDays = (int) Math.floor((countDown - numHours) / 24);

		_olyLog.info("Olympiad System: Competition Period Starts in "
				+ numDays + " days, " + numHours
				+ " hours and " + numMins + " mins.");

		_olyLog.info("Olympiad System: Event starts/started : " + _compStart.getTime());

		_scheduledCompStart = ThreadPoolManager.getInstance().scheduleGeneral(new startCompTask(), getTimeToCompBegin());
	}

	private static class startCompTask implements Runnable
	{
		public void run()
		{
			if(_isOlympiadEnd)
				return;

			_inCompPeriod = true;

			Announcements.getInstance().announceToAll(new SystemMessage(SystemMessage.THE_OLYMPIAD_GAME_HAS_STARTED));
			_log.info("Olympiad System: Olympiad Game Started");
			_olyLog.info("Olympiad Game Started");

			List<L2Player> list = new FastList<L2Player>(0);

			for(int i = 0; i < Config.ALT_OLY_MAX_ARENAS; i++)
			{
				_instances[i] = (OlympiadInstance) InstanceManager.getInstance().createNewInstance(Rnd.get(147, 150), list);
				_instances[i].setArenaId(i);
			}

			_scheduledManagerTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new CompetitionManager(), Config.ALT_OLY_IWAIT, Config.ALT_OLY_BWAIT);
			_scheduledCompEnd = ThreadPoolManager.getInstance().scheduleGeneral(new endCompTask(), getTimeToCompEnd());
		}
	}

	private static class endCompTask implements Runnable
	{
		public void run()
		{

			if(_isOlympiadEnd)
				return;
			_scheduledManagerTask.cancel(true);
			_inCompPeriod = false;
			Announcements.getInstance().announceToAll(new SystemMessage(SystemMessage.THE_OLYMPIAD_GAME_HAS_ENDED));
			_log.info("Olympiad System: Olympiad Game Ended");
			_olyLog.info("Olympiad Game Ended");

			saveProperties();
			saveNobleData();
			startCompPeriod();

			try
			{
				for(OlympiadInstance oi : _instances)
				{
					OlympiadGame og = oi.getOlympiadGame();
					if(og != null && og.getGameState() == OlympiadGameState.INITIAL)
						og.abortGame();
				}
			}
			catch(NullPointerException e)
			{
				_olyLog.warn("EndCompTask exception: " + e);
			}

			for(int i = 0; i < _instances.length; i++)
				if(_instances[i].isFree())
				{
					_instances[i].stopInstance();
					_instances[i] = null;
				}
		}
	}

	private static void loadClassLeaders()
	{
		_classLeadres = new FastMap<Integer, List<String>>();
		Connection con = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt;
			final String sql = "SELECT IF(class_id = 133, 132, class_id) as class_id, char_name FROM olymp_nobles_prev WHERE wins + loos >= " + Config.ALT_OLY_MIN_MATCHES + " ORDER BY class_id, points DESC, wins DESC";

			stmt = con.prepareStatement(sql);
			ResultSet rset = stmt.executeQuery();

			while(rset.next())
			{
				int classId = rset.getInt("class_id") == 133 ? 132 : rset.getInt("class_id");
				if(_classLeadres.get(classId) == null)
					_classLeadres.put(classId, new FastList<String>());

				_classLeadres.get(classId).add(rset.getString("char_name"));
			}

			rset.close();
			stmt.close();
		}
		catch(SQLException e)
		{
			_olyLog.warn("Couldnt load class leaders " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private static class WeeklyTask implements Runnable
	{
		public void run()
		{
			addWeeklyPoints();
			_log.info("Olympiad System: Added weekly points to nobles");
			_olyLog.info("Added weekly points to nobles");
			saveNobleData();
			_nextWeeklyChange = calcNextWeeklyChangeDate();
			saveProperties();
			_olyLog.info("Next weekly change: " + new Date(_nextWeeklyChange));
			_scheduledWeeklyTask = ThreadPoolManager.getInstance().scheduleGeneral(this, _nextWeeklyChange - System.currentTimeMillis());
		}
	}

	private static long calcNextWeeklyChangeDate()
	{
		Calendar week = Calendar.getInstance();
		week.set(Calendar.DAY_OF_MONTH, (int)(Config.ALT_OLY_WPERIOD / 1000 / 60 / 60 / 24));
		week.set(Calendar.AM_PM, Calendar.AM);
		week.set(Calendar.HOUR, 6);
		week.set(Calendar.MINUTE, 30);
		week.set(Calendar.SECOND, 0);
		week.set(Calendar.MILLISECOND, 0);
		long w = week.getTimeInMillis();
		while(w < System.currentTimeMillis())
			w += Config.ALT_OLY_WPERIOD;

		return w;
	}

	protected synchronized static void addWeeklyPoints()
	{
		if(_period == 1)
			return;

		for(Integer char_id : _nobles.keySet())
		{
			_olyLog.info("Weekly points: processing " + _nobles.get(char_id).getString("char_name") + " points " + _nobles.get(char_id).getInteger("points") + " wins " + _nobles.get(char_id).getInteger("wins") + " loos " + _nobles.get(char_id).getInteger("loos"));
			StatsSet nobleStat = _nobles.remove(char_id);
			nobleStat.set("points", nobleStat.getInteger("points") + Config.ALT_OLY_WEEKLY_POINTS);
			nobleStat.set("cb_matches", 0);
			nobleStat.set("ncb_matches", 0);
			nobleStat.set("team_matches", 0);
			_nobles.put(char_id, nobleStat);
			_olyLog.info("Weekly points: processing " + _nobles.get(char_id).getString("char_name") + " points " + _nobles.get(char_id).getInteger("points"));
		}
	}

	public synchronized static void saveNobleData()
	{
		Connection con = null;

		_olyLog.info("Save nobles data start");
		if(_nobles == null || _nobles.size() == 0)
			return;
		_olyLog.info("nobles size: " + _nobles.size());

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt;
			final String sql = "REPLACE INTO olymp_nobles VALUES(?,?,?,?,?,?,?,?,?,?)";

			for(Integer nobleId : _nobles.keySet())
			{
				StatsSet nobleInfo = _nobles.get(nobleId);

				stmt = con.prepareStatement(sql);
				stmt.setInt(1, nobleId);
				stmt.setInt(2, nobleInfo.getInteger("class_id"));
				stmt.setString(3, nobleInfo.getString("char_name"));
				stmt.setInt(4, nobleInfo.getInteger("points"));
				stmt.setInt(5, nobleInfo.getInteger("prev_points"));
				stmt.setInt(6, nobleInfo.getInteger("wins"));
				stmt.setInt(7, nobleInfo.getInteger("loos"));
				stmt.setInt(8, nobleInfo.getInteger("cb_matches"));
				stmt.setInt(9, nobleInfo.getInteger("ncb_matches"));
				stmt.setInt(10, nobleInfo.getInteger("team_matches"));
				stmt.execute();
				stmt.close();
				_olyLog.info("class: " + nobleInfo.getInteger("class_id") + " " + nobleInfo.getString("char_name") + " points: " + nobleInfo.getInteger("points") + " wins/loos: " + nobleInfo.getInteger("wins") + "/" + nobleInfo.getInteger("loos"));
			}
		}
		catch(SQLException e)
		{
			_olyLog.warn("Couldnt save nobles info in db " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	protected static void resetNobles()
	{
		_log.info("Reset nobles to default");

		if(_nobles == null || _nobles.size() == 0)
			return;

		FastList<StatsSet> sorted = new FastList<StatsSet>();
		for(Integer objecrId : _nobles.keySet())
		{
			StatsSet noble = _nobles.get(objecrId);
			if(noble.getInteger("wins") + noble.getInteger("loos") >= Config.ALT_OLY_MIN_MATCHES && !Hero.canBeAHero(objecrId))
				sorted.add(noble);
		}

		for(Integer nobleId : _nobles.keySet())
		{
			StatsSet noble = _nobles.get(nobleId);
			noble.set("wins", 0);
			noble.set("loos", 0);
			noble.set("cb_matches", 0);
			noble.set("ncb_matches", 0);
			noble.set("team_matches", 0);
			noble.set("prev_points", noble.getInteger("points"));
			noble.set("points", Config.ALT_OLY_START_POINTS);
			if(Hero.canBeAHero(nobleId))
				noble.set("prev_points", noble.getInteger("prev_points") + Config.ALT_OLY_HERO_POINTS_REWARD);
		}

		_olyLog.info("Calculate rank for: " + sorted.size() + " nobleses.");
		Collections.sort(sorted, RateComparator.getInstance());
		double per = 100. / sorted.size();
		for(int i = 0; i < sorted.size(); i++)
		{
			StatsSet noble = sorted.get(i);
			double rank = (i + 1) * per;
			if(rank <= 1)
			{
				if(noble.getInteger("prev_points") < Config.ALT_OLY_RANK1_POINTS)
					noble.set("prev_points", Config.ALT_OLY_RANK1_POINTS);
				_olyLog.info((i + 1) + ": " + noble.getString("char_name") + " rank 1, points: " + noble.getInteger("prev_points"));
			}
			else if(rank <= 10)
			{
				if(noble.getInteger("prev_points") < Config.ALT_OLY_RANK2_POINTS)
					noble.set("prev_points", Config.ALT_OLY_RANK2_POINTS);
				_olyLog.info((i + 1) + ": " + noble.getString("char_name") + " rank 2, points: " + noble.getInteger("prev_points"));
			}
			else if(rank <= 25)
			{
				if(noble.getInteger("prev_points") < Config.ALT_OLY_RANK3_POINTS)
					noble.set("prev_points", Config.ALT_OLY_RANK3_POINTS);
				_olyLog.info((i + 1) + ": " + noble.getString("char_name") + " rank 3, points: " + noble.getInteger("prev_points"));
			}
			else if(rank <= 50)
			{
				if(noble.getInteger("prev_points") < Config.ALT_OLY_RANK4_POINTS)
					noble.set("prev_points", Config.ALT_OLY_RANK4_POINTS);
				_olyLog.info((i + 1) + ": " + noble.getString("char_name") + " rank 4, points: " + noble.getInteger("prev_points"));
			}
			else if(noble.getInteger("prev_points") < Config.ALT_OLY_RANK5_POINTS)
			{
				noble.set("prev_points", Config.ALT_OLY_RANK5_POINTS);
				_olyLog.info((i + 1) + ": " + noble.getString("char_name") + " rank 5, points: " + noble.getInteger("prev_points"));
			}
		}
	}

	public static void removeDisconnectedCompetitor(L2Player player)
	{
		if(player.getOlympiadGameId() < 0)
			return;

		if(Config.ALT_OLY_ENABLE_HWID_CHECK)
			removeHWID(player.getLastHWID());

		OlympiadInstance oi = _instances[player.getOlympiadGameId()];
		if(oi != null)
			oi.getOlympiadGame().handleDisconnect(player);
	}

	public static GCSArray<Integer> getNoblesInNonClassRegistered()
	{
		for(Integer objectId : _nonClassBasedRegisters)
		{
			L2Player player = L2ObjectsStorage.getPlayer(objectId);
			if(player == null)
				_nonClassBasedRegisters.remove(objectId);
		}
		return _nonClassBasedRegisters;
	}

	public static GCSArray<Integer> getNoblesInClassRegistered(int classId)
	{
		if(_classBasedRegisters.get(classId) == null)
			return null;

		String log = "";
		int c = 0;
		for(Integer objectId : _classBasedRegisters.get(classId))
		{
			L2Player player = L2ObjectsStorage.getPlayer(objectId);
			if(player == null)
				_classBasedRegisters.get(classId).remove(objectId);
			else
			{
				c++;
				log += player.getName() + " ";
			}
		}

		_olyLog.info("reg in class: " + classId + " " + c + " players: " + log);
		return _classBasedRegisters.get(classId);
	}

	public static boolean isMinRegistered()
	{
		int inClass;
		boolean inClassOk = false;
		for(Integer classId : _classBasedRegisters.keySet())
		{
			GCSArray<Integer> classed = getNoblesInClassRegistered(classId);
			inClass = classed != null ? classed.size() : 0;
			if(inClass >= Config.ALT_OLY_MIN_NOBLE_CB)
			{
				inClassOk = true;
				break;
			}
		}
		GCSArray<Integer> nonClassed = getNoblesInNonClassRegistered();
		_olyLog.warn("Check Regiseterd in: class " + inClassOk + " non class " + nonClassed.size() + " team: " + _teamBaseRegistered.size());
		return !inClassOk && nonClassed.size() < Config.ALT_OLY_MIN_NOBLE_NCB && _teamBaseRegistered.size() < Config.ALT_OLY_MIN_NOBLE_3x3;
	}

	public static void saveProperties()
	{
		try
		{
			Properties OlympiadProperties = new Properties();
			FileOutputStream fos = new FileOutputStream(new File(Config.DATAPACK_ROOT, OLYMPIAD_DATA_FILE));

			OlympiadProperties.setProperty("CurrentCycle", String.valueOf(_currentCycle));
			OlympiadProperties.setProperty("Period", String.valueOf(_period));
			OlympiadProperties.setProperty("OlympiadEnd", String.valueOf(_olympiadEnd));
			OlympiadProperties.setProperty("ValdationEnd", String.valueOf(_validationEnd));
			OlympiadProperties.setProperty("NextWeeklyChange", String.valueOf(_nextWeeklyChange));

			OlympiadProperties.store(fos, "Olympiad Properties");
			fos.close();
		}
		catch(java.io.IOException e)
		{
			_log.warn("Olympiad System: can't save data " + e);
			_olyLog.warn("can't save data " + e);
		}
	}

	public static void manualSelectHeroes()
	{
		Hero.computeNewHeroes(_currentCycle);
	}

	private static long getTimeToCompBegin()
	{
		if(_compStart.getTimeInMillis() < Calendar.getInstance().getTimeInMillis() &&
				_compEnd > Calendar.getInstance().getTimeInMillis())
			return 10L;

		if(_compStart.getTimeInMillis() > Calendar.getInstance().getTimeInMillis())
			return (_compStart.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());

		return calcCompBegin();
	}

	public static long getTimeToCompEnd()
	{
		return (_compEnd - Calendar.getInstance().getTimeInMillis());
	}

	private static long calcCompBegin()
	{
		_compStart = Calendar.getInstance();
		_compStart.set(Calendar.HOUR_OF_DAY, Config.ALT_OLY_START_HOUR);
		_compStart.set(Calendar.MINUTE, Config.ALT_OLY_START_MIN);
		_compStart.add(Calendar.HOUR_OF_DAY, 24);
		_compEnd = _compStart.getTimeInMillis() + Config.ALT_OLY_CPERIOD;

		_log.info("Olympiad System: New Schedule @ " + _compStart.getTime());
		_olyLog.info("New Schedule @ " + _compStart.getTime());

		return (_compStart.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());
	}

	private static void calcOlympEndTime()
	{
		SystemMessage sm = new SystemMessage(SystemMessage.OLYMPIAD_PERIOD_S1_HAS_STARTED);
		sm.addNumber(_currentCycle);

		Announcements.getInstance().announceToAll(sm);

		Calendar currentTime = Calendar.getInstance();
		currentTime.add(Calendar.MONTH, 1);
		currentTime.set(Calendar.DAY_OF_MONTH, 1);
		currentTime.set(Calendar.AM_PM, Calendar.AM);
		currentTime.set(Calendar.HOUR, 0);
		currentTime.set(Calendar.MINUTE, 1);
		currentTime.set(Calendar.SECOND, 0);
		_olympiadEnd = currentTime.getTimeInMillis();

		Calendar nextChange = Calendar.getInstance();
		nextChange.set(Calendar.AM_PM, Calendar.AM);
		nextChange.set(Calendar.HOUR, 6);
		nextChange.set(Calendar.MINUTE, 30);
		nextChange.set(Calendar.SECOND, 0);
		nextChange.set(Calendar.MILLISECOND, 0);
		_nextWeeklyChange = nextChange.getTimeInMillis() + Config.ALT_OLY_WPERIOD;
		_olyLog.info("Next Weekle change: " + nextChange.getTime());

		_isOlympiadEnd = false;
	}

	private static long getTimeToValidationEnd()
	{
		if(_validationEnd > Calendar.getInstance().getTimeInMillis())
			return _validationEnd - Calendar.getInstance().getTimeInMillis();
		return 10L;
	}

	private static long getTimeToWeekChange()
	{
		if(_nextWeeklyChange > Calendar.getInstance().getTimeInMillis())
			return _nextWeeklyChange - Calendar.getInstance().getTimeInMillis();
		return 10L;
	}

	private static long getTimeToOlympiadEnd()
	{
		return _olympiadEnd - Calendar.getInstance().getTimeInMillis();
	}

	public static void registerParty(L2Party party)
	{
		if(!_inCompPeriod)
		{
			party.broadcastToPartyMembers(Msg.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			return;
		}

		OlympiadTeam ot = new OlympiadTeam(0);
		ot.setLeader(party.getPartyLeader());
		for(L2Player player : party.getPartyMembers())
		{
			ot.addPlayer(player);
			if(Config.ALT_OLY_ENABLE_HWID_CHECK)
				registeredHWID.add(player.getLastHWID());
		}

		_teamBaseRegistered.put(party.getPartyLeaderOID(), ot);
		party.setOlympiadTeam(ot);
		party.broadcastToPartyMembers(Msg.YOU_ARE_CURRENTLY_REGISTERED_FOR_A_3_VS_3_CLASS_IRRELEVANT_TEAM_MATCH);
	}

	public static boolean registerNoble(L2Player noble, boolean classBased)
	{
		SystemMessage sm;

		if(!_inCompPeriod)
		{
			noble.sendPacket(Msg.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			return false;
		}

		if(!noble.isNoble())
		{
			sm = new SystemMessage(SystemMessage.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_ONLY_NOBLESSE_CHARACTERS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
			noble.sendPacket(sm.addCharName(noble));
			return false;
		}

		if(noble.isSubClassActive())
		{
			sm = new SystemMessage(SystemMessage.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD_BECAUSE_YOU_HAVE_CHANGED_TO_YOUR_SUB_CLASS).addCharName(noble);
			noble.sendPacket(sm);
			return false;
		}

		if(!noble.isQuestContinuationPossible(false))
		{
			noble.sendPacket(new SystemMessage(SystemMessage.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD_BECAUSE_YOUR_INVENTORY_SLOT_EXCEEDS_80).addCharName(noble));
			return false;
		}

		if(noble.isCursedWeaponEquipped())
		{
			noble.sendPacket(new SystemMessage(SystemMessage.C1_IS_THE_OWNER_OF_S2_AND_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD).addCharName(noble).addItemName(noble.getCursedWeaponEquippedId()));
			return false;
		}

		if(!_nobles.containsKey(noble.getObjectId()))
		{
			StatsSet statDat = new StatsSet();
			statDat.set("class_id", noble.getBaseClass());
			statDat.set("char_name", noble.getName());
			statDat.set("points", Config.ALT_OLY_START_POINTS);
			statDat.set("wins", 0);
			statDat.set("loos", 0);
			statDat.set("cb_matches", 0);
			statDat.set("ncb_matches", 0);
			statDat.set("team_matches", 0);
			_nobles.put(noble.getObjectId(), statDat);
		}
		else if(_nobles.get(noble.getObjectId()).getInteger("class_id") != noble.getBaseClass())
			_nobles.get(noble.getObjectId()).set("class_id", noble.getBaseClass());

		int gt = getRegisteredGameType(noble);
		if(gt >= 0)
		{
			if(gt == 0)
				sm = new SystemMessage(SystemMessage.C1_IS_ALREADY_REGISTERED_ON_THE_WAITING_LIST_FOR_THE_3_VS_3_CLASS_IRRELEVANT_TEAM_MATCH);
			else if(gt == 1)
				sm = new SystemMessage(SystemMessage.C1_IS_ALREADY_REGISTERED_ON_THE_WAITING_LIST_FOR_THE_NON_CLASS_LIMITED_INDIVIDUAL_MATCH_EVENT);
			else
				sm = new SystemMessage(SystemMessage.C1_IS_ALREADY_REGISTERED_ON_THE_CLASS_MATCH_WAITING_LIST);

			noble.sendPacket(sm.addCharName(noble));
			return false;
		}

		if(_classBasedRegisters.containsKey(noble.getBaseClass() == 133 ? 132 : noble.getBaseClass()))
		{
			GCSArray<Integer> classed = _classBasedRegisters.get(noble.getBaseClass() == 133 ? 132 : noble.getBaseClass());

			if(classed.contains(noble.getObjectId()))
			{
				sm = new SystemMessage(SystemMessage.C1_IS_ALREADY_REGISTERED_ON_THE_CLASS_MATCH_WAITING_LIST).addCharName(noble);
				noble.sendPacket(sm);
				return false;
			}
		}

		if(_nonClassBasedRegisters.contains(noble.getObjectId()))
		{
			sm = new SystemMessage(SystemMessage.C1_IS_ALREADY_REGISTERED_ON_THE_WAITING_LIST_FOR_THE_NON_CLASS_LIMITED_INDIVIDUAL_MATCH_EVENT).addCharName(noble);
			noble.sendPacket(sm);
			return false;
		}

		if(getNoblePoints(noble.getObjectId()) < 1)
			return false;

		if(Config.ALT_OLY_ENABLE_HWID_CHECK && registeredHWID.contains(noble.getLastHWID()))
		{
			noble.sendMessage(new CustomMessage("olympiad.hwid.check", noble).addCharName(noble));
			return false;
		}

		if(classBased)
		{
			if(_classBasedRegisters.containsKey(noble.getBaseClass() == 133 ? 132 : noble.getBaseClass()))
			{
				_classBasedRegisters.get(noble.getBaseClass() == 133 ? 132 : noble.getBaseClass()).add(noble.getObjectId());
				sm = new SystemMessage(SystemMessage.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_CLASSIFIED_GAMES);
				noble.sendPacket(sm);
				if(Config.ALT_OLY_ENABLE_HWID_CHECK)
					registeredHWID.add(noble.getLastHWID());
			}
			else
			{
				GCSArray<Integer> classed = new GCSArray<Integer>();
				classed.add(noble.getObjectId());

				_classBasedRegisters.put(noble.getBaseClass() == 133 ? 132 : noble.getBaseClass(), classed);

				sm = new SystemMessage(SystemMessage.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_CLASSIFIED_GAMES);
				noble.sendPacket(sm);
				if(Config.ALT_OLY_ENABLE_HWID_CHECK)
					registeredHWID.add(noble.getLastHWID());
			}
		}
		else
		{
			_nonClassBasedRegisters.add(noble.getObjectId());
			sm = new SystemMessage(SystemMessage.YOU_HAVE_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_NO_CLASS_GAMES);
			noble.sendPacket(sm);
			if(Config.ALT_OLY_ENABLE_HWID_CHECK)
				registeredHWID.add(noble.getLastHWID());
		}

		return true;
	}

	public static OlympiadGame getOlympiadGameByPlayer(L2Player player)
	{
		for(OlympiadInstance oi : _instances)
			if(oi != null)
			{
				OlympiadGame og = oi.getOlympiadGame();
				if(og != null && (og.getTeam(0).contains(player.getObjectId()) || og.getTeam(1).contains(player.getObjectId())))
					return og;
			}

		return null;
	}

	public static int getNoblePoints(int objId)
	{
		if(_nobles.size() == 0)
			return 0;

		StatsSet noble = _nobles.get(objId);
		if(noble == null)
			return 0;
		return noble.getInteger("points");
	}

	public static int getNoblePrevPoints(int objId)
	{
		Connection con = null;
		int points = 0;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt;
			final String sql = "select points from olymp_nobles_prev where char_id=?";

			stmt = con.prepareStatement(sql);
			stmt.setInt(1, objId);
			ResultSet rset = stmt.executeQuery();

			if(rset.next())
				points = rset.getInt("points");

			rset.close();
			stmt.close();
		}
		catch(SQLException e)
		{
			_olyLog.warn("Couldnt load player points " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

		return points;
	}

	public static int getNoblePrevFights(int objId)
	{
		Connection con = null;
		int fights = 0;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt;
			final String sql = "select wins + loos as fights from olymp_nobles_prev where char_id=?";

			stmt = con.prepareStatement(sql);
			stmt.setInt(1, objId);
			ResultSet rset = stmt.executeQuery();

			if(rset.next())
				fights = rset.getInt("fights");

			rset.close();
			stmt.close();
		}
		catch(SQLException e)
		{
			_olyLog.warn("Couldnt load player points " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

		return fights;
	}

	public static int[] getWaitingList()
	{
		if(!_inCompPeriod)
			return null;

		int[] array = new int[2];
		int classCount = 0;
		if(_classBasedRegisters.size() != 0)
			for(GCSArray<Integer> classed : _classBasedRegisters.values())
				classCount += classed.size();

		array[0] = classCount;
		array[1] = _nonClassBasedRegisters.size();

		return array;
	}

	public static void unRegisterTeam(OlympiadTeam ot)
	{
		if(_teamBaseRegistered.containsKey(ot.getLeaderObjectId()))
		{
			_teamBaseRegistered.remove(ot.getLeaderObjectId());
			if(Config.ALT_OLY_ENABLE_HWID_CHECK)
				for(OlympiadUserInfo oui : ot.getPlayersInfo())
					registeredHWID.remove(oui.getHWID());
		}
	}

	public static boolean unRegisterNoble(L2Player noble)
	{
		SystemMessage sm;

		if(!_inCompPeriod)
		{
			noble.sendPacket(Msg.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
			return false;
		}

		if(!noble.isNoble())
		{
			sm = new SystemMessage(SystemMessage.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_ONLY_NOBLESSE_CHARACTERS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
			noble.sendPacket(sm.addCharName(noble));
			return false;
		}

		if(!isRegisteredInComp(noble))
		{
			sm = new SystemMessage(SystemMessage.YOU_HAVE_NOT_BEEN_REGISTERED_IN_A_WAITING_LIST_OF_A_GAME);
			noble.sendPacket(sm);
			return false;
		}

		if(getRegisteredGameType(noble) == 0 && (noble.getParty() == null || !noble.getParty().isLeader(noble)))
			return false;

		if(_nonClassBasedRegisters.contains(noble.getObjectId()))
		{
			_nonClassBasedRegisters.remove(new Integer(noble.getObjectId()));
			if(Config.ALT_OLY_ENABLE_HWID_CHECK)
				registeredHWID.remove(noble.getLastHWID());
		}
		else
		{
			GCSArray<Integer> classed = _classBasedRegisters.get(noble.getBaseClass() == 133 ? 132 : noble.getBaseClass());
			classed.remove(new Integer(noble.getObjectId()));

			_classBasedRegisters.remove(noble.getClassId().getId() == 133 ? 132 : noble.getBaseClass());
			_classBasedRegisters.put(noble.getClassId().getId() == 133 ? 132 : noble.getBaseClass(), classed);
			if(Config.ALT_OLY_ENABLE_HWID_CHECK)
				registeredHWID.remove(noble.getLastHWID());
		}

		sm = new SystemMessage(SystemMessage.YOU_HAVE_BEEN_DELETED_FROM_THE_WAITING_LIST_OF_A_GAME);
		noble.sendPacket(sm);

		return true;
	}

	public static void removeFromReg(L2Player noble) throws Exception
	{
		if(_nonClassBasedRegisters == null || _classBasedRegisters == null)
			return;

		if(Config.ALT_OLY_ENABLE_HWID_CHECK)
			registeredHWID.remove(noble.getLastHWID());

		if(_nonClassBasedRegisters.contains(noble.getObjectId()))
			_nonClassBasedRegisters.remove(new Integer(noble.getObjectId()));
		else
		{
			GCSArray<Integer> classed = _classBasedRegisters.get(noble.getBaseClass() == 133 ? 132 : noble.getBaseClass());
			classed.remove(new Integer(noble.getObjectId()));

			_classBasedRegisters.remove(noble.getBaseClass() == 133 ? 132 : noble.getBaseClass());
			_classBasedRegisters.put(noble.getBaseClass() == 133 ? 132 : noble.getBaseClass(), classed);
		}
	}

	public static boolean isRegisteredInComp(L2Player player)
	{
		if(_nonClassBasedRegisters.contains(player.getObjectId()))
			return true;
		else if(_classBasedRegisters.containsKey(player.getBaseClass() == 133 ? 132 : player.getBaseClass()))
		{
			GCSArray<Integer> classed = _classBasedRegisters.get(player.getBaseClass() == 133 ? 132 : player.getBaseClass());
			if(classed.contains(player.getObjectId()))
				return true;
		}
		else if(_teamBaseRegistered.containsKey(player.getObjectId()))
			return true;
		else
			for(OlympiadTeam party : _teamBaseRegistered.values())
				if(party != null && party.contains(player.getObjectId()))
					return true;

		return false;
	}

	public static boolean isInTeamList(OlympiadTeam ot)
	{
		return _teamBaseRegistered.containsValue(ot);
	}

	public static int getRegisteredGameType(L2Player player)
	{
		if(player.getOlympiadGameId() >= 0)
		{
			OlympiadInstance oi = _instances[player.getOlympiadGameId()];
			if(oi != null && oi.getOlympiadGame() != null)
				return oi.getOlympiadGame().getGameType();
		}
		else if(_nonClassBasedRegisters.contains(player.getObjectId()))
			return 1;
		else if(_classBasedRegisters.containsKey(player.getBaseClass() == 133 ? 132 : player.getBaseClass()))
		{
			GCSArray<Integer> classed = _classBasedRegisters.get(player.getBaseClass() == 133 ? 132 : player.getBaseClass());
			if(classed.contains(player.getObjectId()))
				return 2;
		}
		else if(_teamBaseRegistered.containsKey(player.getObjectId()))
			return 0;
		else
			for(OlympiadTeam party : _teamBaseRegistered.values())
				if(party != null && party.contains(player.getObjectId()))
					return 0;

		return -1;
	}

	public static int getParticipantsCount()
	{
		int c = 0;
		for(StatsSet noble : _nobles.values())
			if(noble.getInteger("wins") + noble.getInteger("loos") > 0)
				c++;
		return c;
	}

	public static int getCurrentCycle()
	{
		return _currentCycle;
	}

	public static int getPreviousPoints(int objectId)
	{
		if(_nobles.size() == 0 || _nobles.get(objectId) == null)
			return 0;

		return _nobles.get(objectId).getInteger("prev_points");
	}

	public static long getOlympiadTokensCount(int objId)
	{
		if(getNoblePrevFights(objId) < Config.ALT_OLY_MIN_MATCHES)
			return 0;

		int points = getPreviousPoints(objId);

		_nobles.get(objId).set("prev_points", 0);

		return 1000L * points;
	}

	public static List<String> getClassLeaderBoard(int classId)
	{
		if(_classLeadres == null || _classLeadres.get(classId) == null)
			return new FastList<String>();
		return _classLeadres.get(classId);
	}

	public static StatsSet getNoblesData(L2Player player)
	{
		if(_nobles == null || _nobles.size() == 0)
			return null;
		return _nobles.get(player.getObjectId());
	}

	public static void finishGame(int gameId)
	{
		if(gameId < 0)
			return;

		OlympiadInstance oi = _instances[gameId];
		if(oi == null || oi.getOlympiadGame() == null)
			return;

		if(oi.getOlympiadGame().getGameType() == 0)
		{
			_olyLog.warn("OG(" + gameId + ") check team dead.");
			oi.getOlympiadGame().checkTeamDead();
		}
		else
		{
			_olyLog.warn("OG(" + gameId + ") finish game.");
			oi.getOlympiadGame().endGame();
		}
	}

	public static void addReceivedDamage(int gameId, int objectId, int damage)
	{
		if(gameId < 0)
			return;

		OlympiadInstance oi = _instances[gameId];
		if(oi == null || oi.getOlympiadGame() == null)
			return;

		oi.getOlympiadGame().addDamage(objectId, damage);
	}

	public static void broadcastPlayersState(int arenaId)
	{
		if(arenaId < 0)
			return;

		OlympiadInstance oi = _instances[arenaId];
		if(oi == null || oi.getOlympiadGame() == null)
			return;

		oi.getOlympiadGame().broadcastPlayersState();
	}

	public static void broadcastToSpectators(int arenaId, L2GameServerPacket sp)
	{
		if(arenaId < 0)
			return;

		OlympiadInstance oi = _instances[arenaId];
		if(oi != null)
			oi.broadcastPacket(sp);
	}

	public static void addPoints(int char_id, int points)
	{
		if(_nobles != null && _nobles.get(char_id) != null)
		{
			StatsSet noble = _nobles.get(char_id);
			noble.set("points", noble.getInteger("points") + points);
			_nobles.remove(char_id);
			_nobles.put(char_id, noble);
			updateNobleData(char_id);
		}
	}

	public static boolean isValidationPeriod()
	{
		return _period == VALID_PERIOD;
	}

	public static boolean isCalculatePeriod()
	{
		return isValidationPeriod() && _calculateEnd > System.currentTimeMillis();
	}

	public static void checkNoble(L2Player player)
	{
		if(!_nobles.containsKey(player.getObjectId()))
		{
			StatsSet noble = new StatsSet();
			noble.set("class_id", player.getBaseClass());
			noble.set("char_name", player.getName());
			noble.set("points", Config.ALT_OLY_START_POINTS);
			noble.set("wins", 0);
			noble.set("loos", 0);
			noble.set("prev_points", 0);
			noble.set("cb_matches", 0);
			noble.set("ncb_matches", 0);
			noble.set("team_matches", 0);
			_nobles.put(player.getObjectId(), noble);
		}
	}

	public static OlympiadInstance getFreeArena()
	{
		for(OlympiadInstance oi : _instances)
			if(oi.isFree())
				return oi;

		return null;
	}

	public static int getFreeArenaCount()
	{
		int c = 0;
		for(OlympiadInstance oi : _instances)
			if(oi.isFree())
				c++;

		return c;
	}

	public static void updateNobleData(int charId)
	{
		final String sql = "REPLACE INTO olymp_nobles(char_id,class_id,char_name,points,wins,loos,cb_matches,ncb_matches,team_matches) VALUES(?,?,?,?,?,?,?,?,?)";
		Connection con = null;
		PreparedStatement stmt = null;
		StatsSet noble = _nobles.get(charId);
		if(noble == null) return;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement(sql);
			stmt.setInt(1, charId);
			stmt.setInt(2, noble.getInteger("class_id"));
			stmt.setString(3, noble.getString("char_name"));
			stmt.setInt(4, noble.getInteger("points"));
			stmt.setInt(5, noble.getInteger("wins"));
			stmt.setInt(6, noble.getInteger("loos"));
			stmt.setInt(7, noble.getInteger("cb_matches"));
			stmt.setInt(8, noble.getInteger("ncb_matches"));
			stmt.setInt(9, noble.getInteger("team_matches"));
			stmt.execute();
		}
		catch(SQLException e)
		{
			_olyLog.warn("Can't update nobles " + noble.getString("char_name") + " : " + e + " " + e.getMessage() + " " + e.getSQLState());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, stmt);
		}
	}

	public static boolean checkHWID(String HWID)
	{
		return registeredHWID.contains(HWID);
	}

	public static void removeHWID(String HWID)
	{
		registeredHWID.remove(HWID);
	}
}