package ru.l2gw.gameserver.model.entity;

import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.SevenSignsFestival.FestivalManager;
import ru.l2gw.gameserver.model.entity.SevenSignsFestival.SevenSignsFestival;
import ru.l2gw.gameserver.serverpackets.SSQInfo;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.SpawnTable;
import ru.l2gw.gameserver.templates.StatsSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;

/**
 * Seven Signs Engine
 * @author rage
 */
public class SevenSigns
{
	protected static Log _log = LogFactory.getLog("sevensigns");
	private static SevenSigns _instance;
	private ScheduledFuture<?> _periodChange;

	// Basic Seven Signs Constants \\
	public static final String SEVEN_SIGNS_HTML_PATH = "data/html/seven_signs/";
	public static final String SEVEN_SIGNS_STATUS = "data/sevensigns.status";

	public static final int CABAL_NULL = 0;
	public static final int CABAL_DUSK = 1;
	public static final int CABAL_DAWN = 2;

	public static final int SEAL_NULL = 0;
	public static final int SEAL_AVARICE = 1;
	public static final int SEAL_GNOSIS = 2;
	public static final int SEAL_STRIFE = 3;

	public static final int PERIOD_COMP_RECRUITING = 0;
	public static final int PERIOD_COMPETITION = 1;
	public static final int PERIOD_COMP_RESULTS = 2;
	public static final int PERIOD_SEAL_VALIDATION = 3;

	public static final int PERIOD_START_HOUR = 18;
	public static final int PERIOD_START_MINS = 0;
	public static final int PERIOD_START_DAY = Calendar.MONDAY;

	// The quest event and seal validation periods last for approximately one week
	// with a 15 minutes "interval" period sandwiched between them.
	public static final int PERIOD_MINOR_LENGTH = 900000;
	public static final int PERIOD_MAJOR_LENGTH = 604800000 - PERIOD_MINOR_LENGTH;

	public static final short ANCIENT_ADENA_ID = 5575;
	public static final short RECORD_SEVEN_SIGNS_ID = 5707;
	public static final short CERTIFICATE_OF_APPROVAL_ID = 6388;
	public static final int RECORD_SEVEN_SIGNS_COST = 500;
	public static final int ADENA_JOIN_DAWN_COST = 50000;

	// Seal Stone Related Constants \\
	public static final int SEAL_STONE_BLUE_ID = 6360;
	public static final int SEAL_STONE_GREEN_ID = 6361;
	public static final int SEAL_STONE_RED_ID = 6362;

	public static final int SEAL_STONE_BLUE_VALUE = 3;
	public static final int SEAL_STONE_GREEN_VALUE = 5;
	public static final int SEAL_STONE_RED_VALUE = 10;

	public static final int BLUE_CONTRIB_POINTS = 3;
	public static final int GREEN_CONTRIB_POINTS = 5;
	public static final int RED_CONTRIB_POINTS = 10;

	// There is a max on official, but not sure what!
	public static final long MAXIMUM_PLAYER_CONTRIB = Math.round(1000000 * Config.RATE_DROP_ITEMS);

	private final Calendar _calendar = Calendar.getInstance();

	protected int _activePeriod;
	protected int _currentCycle;
	protected long _dawnStoneScore;
	protected long _duskStoneScore;
	protected long _dawnFestivalScore;
	protected long _duskFestivalScore;
	protected int _compWinner;
	protected int _previousWinner;

	private static int sky = 256;

	private Map<Integer, StatsSet> _signsPlayerData;
	private Map<Integer, Integer> _signsSealOwners;
	private Map<Integer, Integer> _signsDuskSealTotals;
	private Map<Integer, Integer> _signsDawnSealTotals;

	public SevenSigns()
	{
		_signsPlayerData = new FastMap<Integer, StatsSet>();
		_signsSealOwners = new FastMap<Integer, Integer>();
		_signsDuskSealTotals = new FastMap<Integer, Integer>();
		_signsDawnSealTotals = new FastMap<Integer, Integer>();

		try
		{
			restoreSevenSignsData();
		}
		catch(Exception e)
		{
			_log.warn("SevenSigns: Failed to load configuration: " + e);
			e.printStackTrace();
		}

		_log.info("SevenSigns: Currently in the " + getCurrentPeriodName() + " period!");
		initializeSeals();

		if(isSealValidationPeriod())
		{
			if(getCabalWinner() == CABAL_NULL)
				_log.info("SevenSigns: The Competition last week ended with a tie.");
			else
				_log.info("SevenSigns: The " + getCabalName(getCabalWinner()) + " were victorious last week.");
		}
		else if(getCabalWinner() == CABAL_NULL)
			_log.info("SevenSigns: The Competition this week, if the trend continue, will end with a tie.");
		else
			_log.info("SevenSigns: The " + getCabalName(getCabalWinner()) + " are in the lead this week.");

		int numMins = 0;
		int numHours = 0;
		int numDays = 0;
		synchronized(this)
		{
			setCalendarForNextPeriodChange();
			long milliToChange = getMilliToPeriodChange();
			if(milliToChange < 10)
				milliToChange = 10;
			// Schedule a time for the next period change.
			_periodChange = ThreadPoolManager.getInstance().scheduleGeneral(new SevenSignsPeriodChange(), milliToChange);

			double numSecs = milliToChange / 1000 % 60;
			double countDown = (milliToChange / 1000 - numSecs) / 60;
			numMins = (int) Math.floor(countDown % 60);
			countDown = (countDown - numMins) / 60;
			numHours = (int) Math.floor(countDown % 24);
			numDays = (int) Math.floor((countDown - numHours) / 24);
		}

		_log.info("SevenSigns: Next period begins in " + numDays + " days, " + numHours + " hours and " + numMins + " mins.");

		if(Config.SS_ANNOUNCE_PERIOD > 0)
			ThreadPoolManager.getInstance().scheduleGeneral(new SevenSignsAnnounce(), Config.SS_ANNOUNCE_PERIOD * 1000 * 60);
	}

	public void spawnSevenSignsNPC()
	{
		if(isSealValidationPeriod() || isCompResultsPeriod())
		{
			switch(getCabalWinner())
			{
				case CABAL_DAWN:
					SpawnTable.getInstance().stopEventSpawn("dusk", true);
					SpawnTable.getInstance().stopEventSpawn("ssq_comp", true);
					SpawnTable.getInstance().stopEventSpawn("ssq_tw", true);
					SpawnTable.getInstance().stopEventSpawn("ssq_no", true);
					SpawnTable.getInstance().startEventSpawn("dawn");
					SpawnTable.getInstance().startEventSpawn("ssq_da");
					break;
				case CABAL_DUSK:
					SpawnTable.getInstance().stopEventSpawn("dawn", true);
					SpawnTable.getInstance().stopEventSpawn("ssq_comp", true);
					SpawnTable.getInstance().stopEventSpawn("ssq_da", true);
					SpawnTable.getInstance().stopEventSpawn("ssq_no", true);
					SpawnTable.getInstance().startEventSpawn("dusk");
					SpawnTable.getInstance().startEventSpawn("ssq_tw");
					break;
				default:
					SpawnTable.getInstance().stopEventSpawn("ssq_comp", true);
					SpawnTable.getInstance().stopEventSpawn("ssq_da", true);
					SpawnTable.getInstance().stopEventSpawn("ssq_tw", true);
					SpawnTable.getInstance().stopEventSpawn("dusk", true);
					SpawnTable.getInstance().stopEventSpawn("dawn", true);
					SpawnTable.getInstance().startEventSpawn("ssq_no");
					break;
			}

			if(getSealOwner(SEAL_GNOSIS) == getCabalWinner() && getSealOwner(SEAL_GNOSIS) != CABAL_NULL)
				SpawnTable.getInstance().startEventSpawn("gnosis");
			else
				SpawnTable.getInstance().stopEventSpawn("gnosis", true);

			if(getSealOwner(SEAL_AVARICE) == getCabalWinner() && getSealOwner(SEAL_AVARICE) != CABAL_NULL)
			{
				SpawnTable.getInstance().startEventSpawn("avarice");

				switch(getCabalWinner())
				{
					case CABAL_DAWN:
						SpawnTable.getInstance().startEventSpawn("avarice_dawn");
						SpawnTable.getInstance().stopEventSpawn("avarice_dusk", true);
						break;
					case CABAL_DUSK:
						SpawnTable.getInstance().startEventSpawn("avarice_dusk");
						SpawnTable.getInstance().stopEventSpawn("avarice_dawn", true);
						break;
				}
			}
			else
			{
				SpawnTable.getInstance().stopEventSpawn("avarice", true);
				SpawnTable.getInstance().stopEventSpawn("avarice_dawn", true);
				SpawnTable.getInstance().stopEventSpawn("avarice_dusk", true);
			}
		}
		else
		{
			SpawnTable.getInstance().stopEventSpawn("avarice_dawn", true);
			SpawnTable.getInstance().stopEventSpawn("avarice_dusk", true);
			SpawnTable.getInstance().stopEventSpawn("ssq_tw", true);
			SpawnTable.getInstance().stopEventSpawn("ssq_no", true);
			SpawnTable.getInstance().stopEventSpawn("ssq_da", true);
			SpawnTable.getInstance().stopEventSpawn("dawn", true);
			SpawnTable.getInstance().stopEventSpawn("dusk", true);
			SpawnTable.getInstance().stopEventSpawn("gnosis", true);
			SpawnTable.getInstance().stopEventSpawn("avarice", true);
			SpawnTable.getInstance().startEventSpawn("ssq_comp");
		}
	}

	public static SevenSigns getInstance()
	{
		if(_instance == null)
			_instance = new SevenSigns();
		return _instance;
	}

	public static long calcContributionScore(long blueCount, long greenCount, long redCount)
	{
		long contrib = blueCount;
		contrib += greenCount;
		contrib += redCount * RED_CONTRIB_POINTS;

		return contrib;
	}

	public static long calcAncientAdenaReward(long blueCount, long greenCount, long redCount)
	{
		long reward = blueCount * SEAL_STONE_BLUE_VALUE;
		reward += greenCount * SEAL_STONE_GREEN_VALUE;
		reward += redCount * SEAL_STONE_RED_VALUE;

		return reward;
	}

	public static int getCabalNumber(String cabal)
	{
		if(cabal.equalsIgnoreCase("dawn"))
			return CABAL_DAWN;
		else if(cabal.equalsIgnoreCase("dusk"))
			return CABAL_DUSK;
		else
			return CABAL_NULL;
	}

	public static String getCabalShortName(int cabal)
	{
		switch(cabal)
		{
			case CABAL_DAWN:
				return "dawn";
			case CABAL_DUSK:
				return "dusk";
		}
		return "No Cabal";
	}

	public static String getCabalName(int cabal)
	{
		switch(cabal)
		{
			case CABAL_DAWN:
				return "Lords of Dawn";
			case CABAL_DUSK:
				return "Revolutionaries of Dusk";
		}
		return "No Cabal";
	}

	public static String getSealName(int seal, boolean shortName)
	{
		String sealName = !shortName ? "Seal of " : "";

		switch(seal)
		{
			case SEAL_AVARICE:
				sealName += "Avarice";
				break;
			case SEAL_GNOSIS:
				sealName += "Gnosis";
				break;
			case SEAL_STRIFE:
				sealName += "Strife";
				break;
		}
		return sealName;
	}

	public final int getCurrentCycle()
	{
		return _currentCycle;
	}

	public final int getCurrentPeriod()
	{
		return _activePeriod;
	}

	private int getDaysToPeriodChange()
	{
		int numDays = _calendar.get(Calendar.DAY_OF_WEEK) - PERIOD_START_DAY;

		if(numDays < 0)
			return 0 - numDays;

		return 7 - numDays;
	}

	public final long getMilliToPeriodChange()
	{
		return _calendar.getTimeInMillis() - System.currentTimeMillis();
	}

	protected void setCalendarForNextPeriodChange()
	{
		// Calculate the number of days until the next period
		// A period starts at 18:00 pm (local time), like on official servers.
		switch(getCurrentPeriod())
		{
			case PERIOD_SEAL_VALIDATION:
			case PERIOD_COMPETITION:
				int daysToChange = getDaysToPeriodChange();

				if(daysToChange == 7)
					if(_calendar.get(Calendar.HOUR_OF_DAY) < PERIOD_START_HOUR)
						daysToChange = 0;
					else if(_calendar.get(Calendar.HOUR_OF_DAY) == PERIOD_START_HOUR && _calendar.get(Calendar.MINUTE) < PERIOD_START_MINS)
						daysToChange = 0;

				// Otherwise...
				if(daysToChange > 0)
					_calendar.add(Calendar.DATE, daysToChange);
				_calendar.set(Calendar.HOUR_OF_DAY, PERIOD_START_HOUR);
				_calendar.set(Calendar.MINUTE, PERIOD_START_MINS);
				break;
			case PERIOD_COMP_RECRUITING:
			case PERIOD_COMP_RESULTS:
				_calendar.add(Calendar.MILLISECOND, PERIOD_MINOR_LENGTH);
				break;
		}
	}

	public final String getCurrentPeriodName()
	{
		String periodName = null;

		switch(_activePeriod)
		{
			case PERIOD_COMP_RECRUITING:
				periodName = "Quest Event Initialization";
				break;
			case PERIOD_COMPETITION:
				periodName = "Competition (Quest Event)";
				break;
			case PERIOD_COMP_RESULTS:
				periodName = "Quest Event Results";
				break;
			case PERIOD_SEAL_VALIDATION:
				periodName = "Seal Validation";
				break;
		}
		return periodName;
	}

	public final boolean isSealValidationPeriod()
	{
		return _activePeriod == PERIOD_SEAL_VALIDATION;
	}

	public final boolean isCompResultsPeriod()
	{
		return _activePeriod == PERIOD_COMP_RESULTS;
	}

	public final boolean isCompetitionPeriod()
	{
		return _activePeriod == PERIOD_COMPETITION;
	}

	public final long getCurrentScore(int cabal)
	{
		double totalStoneScore = _dawnStoneScore + _duskStoneScore;

		switch(cabal)
		{
			case CABAL_NULL:
				return 0;
			case CABAL_DAWN:
				return Math.round(totalStoneScore == 0 ? 0 : (_dawnStoneScore / (float) totalStoneScore) * 500) + _dawnFestivalScore;
			case CABAL_DUSK:
				return Math.round(totalStoneScore == 0 ? 0 : (_duskStoneScore / (float) totalStoneScore) * 500) + _duskFestivalScore;
		}
		return 0;
	}

	public final long getCurrentStoneScore(int cabal)
	{
		switch(cabal)
		{
			case CABAL_NULL:
				return 0;
			case CABAL_DAWN:
				return _dawnStoneScore;
			case CABAL_DUSK:
				return _duskStoneScore;
		}
		return 0;
	}

	public final int getCabalWinner()
	{
		if(getCurrentScore(CABAL_DUSK) == getCurrentScore(CABAL_DAWN))
			return CABAL_NULL;
		else if(getCurrentScore(CABAL_DUSK) > getCurrentScore(CABAL_DAWN))
			return CABAL_DUSK;

		return CABAL_DAWN;
	}

	public final int getSealOwner(int seal)
	{
		if(_signsSealOwners == null || !_signsSealOwners.containsKey(seal))
			return CABAL_NULL;
		return _signsSealOwners.get(seal);
	}

	public final int getSealProportion(int seal, int cabal)
	{
		if(cabal == CABAL_NULL)
			return 0;
		else if(cabal == CABAL_DUSK)
			return _signsDuskSealTotals.get(seal);
		else
			return _signsDawnSealTotals.get(seal);
	}

	public void updateFestivalScore()
	{
		_dawnFestivalScore = SevenSignsFestival.getInstance().getFestivalScore(CABAL_DAWN);
		_duskFestivalScore = SevenSignsFestival.getInstance().getFestivalScore(CABAL_DUSK);
		saveSevenSignsData(null);
	}

	public final int getTotalMembers(int cabal)
	{
		int cabalMembers = 0;
		if(cabal == CABAL_DUSK)
			for(Integer members : _signsDuskSealTotals.values())
				cabalMembers += members;
		else if(cabal == CABAL_DAWN)
			for(Integer members : _signsDawnSealTotals.values())
				cabalMembers += members;

		return cabalMembers;
	}

	public final StatsSet getPlayerStatsSet(L2Player player)
	{
		if(!hasRegisteredBefore(player))
			return null;

		return _signsPlayerData.get(player.getObjectId());
	}

	public long getPlayerStoneContrib(L2Player player)
	{
		if(!hasRegisteredBefore(player))
			return 0;

		long stoneCount = 0;

		StatsSet currPlayer = _signsPlayerData.get(player.getObjectId());

		if(getPlayerCabal(player) == CABAL_DAWN)
		{
			stoneCount += currPlayer.getLong("dawn_red_stones");
			stoneCount += currPlayer.getLong("dawn_green_stones");
			stoneCount += currPlayer.getLong("dawn_blue_stones");
		}
		else
		{
			stoneCount += currPlayer.getLong("dusk_red_stones");
			stoneCount += currPlayer.getLong("dusk_green_stones");
			stoneCount += currPlayer.getLong("dusk_blue_stones");
		}

		return stoneCount;
	}

	public void clearPlayerStoneContrib(L2Player player)
	{
		if(!hasRegisteredBefore(player))
			return;

		StatsSet currPlayer = _signsPlayerData.get(player.getObjectId());

		if(getPlayerCabal(player) == CABAL_DAWN)
		{
			currPlayer.set("dawn_red_stones", 0);
			currPlayer.set("dawn_green_stones", 0);
			currPlayer.set("dawn_blue_stones", 0);
		}
		else
		{
			currPlayer.set("dusk_red_stones", 0);
			currPlayer.set("dusk_green_stones", 0);
			currPlayer.set("dusk_blue_stones", 0);
		}
	}

	public long getPlayerContribScore(L2Player player)
	{
		if(!hasRegisteredBefore(player))
			return 0;

		StatsSet currPlayer = _signsPlayerData.get(player.getObjectId());
		if(getPlayerCabal(player) == CABAL_DAWN)
			return currPlayer.getInteger("dawn_contribution_score");
		return currPlayer.getInteger("dusk_contribution_score");
	}

	public long getPlayerAdenaCollect(L2Player player)
	{
		if(!hasRegisteredBefore(player))
			return 0;

		if(getPlayerCabal(player) == CABAL_DAWN)
			return _signsPlayerData.get(player.getObjectId()).getLong("dawn_ancient_adena_amount");
		return _signsPlayerData.get(player.getObjectId()).getLong("dusk_ancient_adena_amount");
	}

	public int getPlayerSeal(L2Player player)
	{
		if(!hasRegisteredBefore(player))
			return SEAL_NULL;

		return _signsPlayerData.get(player.getObjectId()).getInteger("seal");
	}

	public int getPlayerCabal(L2Player player)
	{
		if(!hasRegisteredBefore(player))
			return CABAL_NULL;

		return _signsPlayerData.get(player.getObjectId()).getInteger("cabal");
	}

	/**
	 * Restores all Seven Signs data and settings, usually called at server startup.
	 */
	protected void restoreSevenSignsData()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT char_obj_id, cabal, seal, dawn_red_stones, dawn_green_stones, dawn_blue_stones, " + "dawn_ancient_adena_amount, dawn_contribution_score, dusk_red_stones, dusk_green_stones, dusk_blue_stones, " + "dusk_ancient_adena_amount, dusk_contribution_score FROM seven_signs");
			rset = statement.executeQuery();

			while(rset.next())
			{
				int charObjId = rset.getInt("char_obj_id");

				StatsSet sevenDat = new StatsSet();
				sevenDat.set("char_obj_id", charObjId);
				sevenDat.set("cabal", getCabalNumber(rset.getString("cabal")));
				sevenDat.set("seal", rset.getInt("seal"));
				sevenDat.set("dawn_red_stones", rset.getInt("dawn_red_stones"));
				sevenDat.set("dawn_green_stones", rset.getInt("dawn_green_stones"));
				sevenDat.set("dawn_blue_stones", rset.getInt("dawn_blue_stones"));
				sevenDat.set("dawn_ancient_adena_amount", rset.getInt("dawn_ancient_adena_amount"));
				sevenDat.set("dawn_contribution_score", rset.getInt("dawn_contribution_score"));
				sevenDat.set("dusk_red_stones", rset.getInt("dusk_red_stones"));
				sevenDat.set("dusk_green_stones", rset.getInt("dusk_green_stones"));
				sevenDat.set("dusk_blue_stones", rset.getInt("dusk_blue_stones"));
				sevenDat.set("dusk_ancient_adena_amount", rset.getInt("dusk_ancient_adena_amount"));
				sevenDat.set("dusk_contribution_score", rset.getInt("dusk_contribution_score"));

				if(Config.DEBUG)
					_log.info("SevenSigns: Loaded data from DB for char ID " + charObjId + " (" + getCabalShortName(sevenDat.getInteger("cabal")) + ")");

				_signsPlayerData.put(charObjId, sevenDat);
			}
			DbUtils.closeQuietly(statement, rset);

			Properties sevenSignsStatus = new Properties();
			InputStream is = new FileInputStream(new File(Config.DATAPACK_ROOT, SEVEN_SIGNS_STATUS));
			sevenSignsStatus.load(is);
			is.close();

			_currentCycle = Integer.parseInt(sevenSignsStatus.getProperty("current_cycle", "1"));
			_activePeriod = Integer.parseInt(sevenSignsStatus.getProperty("active_period"));
			_previousWinner = Integer.parseInt(sevenSignsStatus.getProperty("previous_winner"));

			_dawnStoneScore = Long.parseLong(sevenSignsStatus.getProperty("dawn_stone_score"));
			_dawnFestivalScore = Long.parseLong(sevenSignsStatus.getProperty("dawn_festival_score"));
			_duskStoneScore = Long.parseLong(sevenSignsStatus.getProperty("dusk_stone_score"));
			_duskFestivalScore = Long.parseLong(sevenSignsStatus.getProperty("dusk_festival_score"));

			_signsSealOwners.put(SEAL_AVARICE, Integer.parseInt(sevenSignsStatus.getProperty("avarice_owner")));
			_signsSealOwners.put(SEAL_GNOSIS, Integer.parseInt(sevenSignsStatus.getProperty("gnosis_owner")));
			_signsSealOwners.put(SEAL_STRIFE, Integer.parseInt(sevenSignsStatus.getProperty("strife_owner")));

			_signsDawnSealTotals.put(SEAL_AVARICE, Integer.parseInt(sevenSignsStatus.getProperty("avarice_dawn_score")));
			_signsDawnSealTotals.put(SEAL_GNOSIS, Integer.parseInt(sevenSignsStatus.getProperty("gnosis_dawn_score")));
			_signsDawnSealTotals.put(SEAL_STRIFE, Integer.parseInt(sevenSignsStatus.getProperty("strife_dawn_score")));
			_signsDuskSealTotals.put(SEAL_AVARICE, Integer.parseInt(sevenSignsStatus.getProperty("avarice_dusk_score")));
			_signsDuskSealTotals.put(SEAL_GNOSIS, Integer.parseInt(sevenSignsStatus.getProperty("gnosis_dusk_score")));
			_signsDuskSealTotals.put(SEAL_STRIFE, Integer.parseInt(sevenSignsStatus.getProperty("strife_dusk_score")));

			if(getCabalWinner() == CABAL_DUSK)
				sky = 257;
			else if(getCabalWinner() == CABAL_DAWN)
				sky = 258;
			else
				sky = 256;
		}
		catch(SQLException e)
		{
			_log.warn("Unable to load Seven Signs Data: " + e);
		}
		catch(java.io.IOException e)
		{
			_log.warn("SevenSigns: can't load data " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		// Festival data is loaded now after the Seven Signs engine data.
	}

	/**
	 * Saves all Seven Signs data, both to the database and properties file (if updateSettings = True).
	 * Often called to preserve data integrity and synchronization with DB, in case of errors.
	 * <BR>
	 * If player != null, just that player's data is updated in the database, otherwise all player's data is
	 * sequentially updated.
	 *
	 * @param player
	 * @throws Exception
	 */
	public synchronized void saveSevenSignsData(L2Player player)
	{
		Connection con = null;
		PreparedStatement statement = null;

		if(Config.DEBUG)
			System.out.println("SevenSigns: Saving data to disk.");
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			for(StatsSet sevenDat : _signsPlayerData.values())
			{
				if(player != null && sevenDat.getInteger("char_obj_id") != player.getObjectId())
					continue;

				statement = con.prepareStatement("UPDATE seven_signs SET cabal=?, seal=?, dawn_red_stones=?, dawn_green_stones=?, dawn_blue_stones=?, dawn_ancient_adena_amount=?, dawn_contribution_score=?, " + "dusk_red_stones=?, dusk_green_stones=?, dusk_blue_stones=?, " + "dusk_ancient_adena_amount=?, dusk_contribution_score=? WHERE char_obj_id=?");
				statement.setString(1, getCabalShortName(sevenDat.getInteger("cabal")));
				statement.setInt(2, sevenDat.getInteger("seal"));
				statement.setInt(3, sevenDat.getInteger("dawn_red_stones"));
				statement.setInt(4, sevenDat.getInteger("dawn_green_stones"));
				statement.setInt(5, sevenDat.getInteger("dawn_blue_stones"));
				statement.setInt(6, sevenDat.getInteger("dawn_ancient_adena_amount"));
				statement.setInt(7, sevenDat.getInteger("dawn_contribution_score"));
				statement.setInt(8, sevenDat.getInteger("dusk_red_stones"));
				statement.setInt(9, sevenDat.getInteger("dusk_green_stones"));
				statement.setInt(10, sevenDat.getInteger("dusk_blue_stones"));
				statement.setInt(11, sevenDat.getInteger("dusk_ancient_adena_amount"));
				statement.setInt(12, sevenDat.getInteger("dusk_contribution_score"));
				statement.setInt(13, sevenDat.getInteger("char_obj_id"));
				statement.execute();
				DbUtils.closeQuietly(statement);
				statement = null;
				if(Config.DEBUG)
					_log.info("SevenSigns: Updated data in DB for char ID " + sevenDat.getInteger("char_obj_id") + " (" + getCabalShortName(sevenDat.getInteger("cabal")) + ")");
			}

			Properties sevenSignsStatus = new Properties();

			FileOutputStream fos = new FileOutputStream(new File(Config.DATAPACK_ROOT, SEVEN_SIGNS_STATUS));

			sevenSignsStatus.setProperty("current_cycle", String.valueOf(_currentCycle));

			sevenSignsStatus.setProperty("active_period", String.valueOf(_activePeriod));
			sevenSignsStatus.setProperty("previous_winner", String.valueOf(_previousWinner));

			sevenSignsStatus.setProperty("dawn_stone_score", String.valueOf(_dawnStoneScore));
			sevenSignsStatus.setProperty("dawn_festival_score", String.valueOf(_dawnFestivalScore));
			sevenSignsStatus.setProperty("dusk_stone_score", String.valueOf(_duskStoneScore));
			sevenSignsStatus.setProperty("dusk_festival_score", String.valueOf(_duskFestivalScore));

			sevenSignsStatus.setProperty("avarice_owner", String.valueOf(_signsSealOwners.get(SEAL_AVARICE)));
			sevenSignsStatus.setProperty("gnosis_owner", String.valueOf(_signsSealOwners.get(SEAL_GNOSIS)));
			sevenSignsStatus.setProperty("strife_owner", String.valueOf(_signsSealOwners.get(SEAL_STRIFE)));

			sevenSignsStatus.setProperty("avarice_dawn_score", String.valueOf(_signsDawnSealTotals.get(SEAL_AVARICE)));
			sevenSignsStatus.setProperty("gnosis_dawn_score", String.valueOf(_signsDawnSealTotals.get(SEAL_GNOSIS)));
			sevenSignsStatus.setProperty("strife_dawn_score", String.valueOf(_signsDawnSealTotals.get(SEAL_STRIFE)));
			sevenSignsStatus.setProperty("avarice_dusk_score", String.valueOf(_signsDuskSealTotals.get(SEAL_AVARICE)));
			sevenSignsStatus.setProperty("gnosis_dusk_score", String.valueOf(_signsDuskSealTotals.get(SEAL_GNOSIS)));
			sevenSignsStatus.setProperty("strife_dusk_score", String.valueOf(_signsDuskSealTotals.get(SEAL_STRIFE)));

			sevenSignsStatus.store(fos, "Seven Signs Status Data");

			if(Config.DEBUG)
				_log.info("SevenSigns: Updated data in status file.");
		}
		catch(SQLException e)
		{
			_log.warn("Unable to save Seven Signs data: " + e);
		}
		catch(java.io.IOException e)
		{
			_log.warn("SevenSigns: can't save data " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	/**
	 * Used to reset the cabal details of all players, and update the database.<BR>
	 * Primarily used when beginning a new cycle, and should otherwise never be called.
	 */
	protected void resetPlayerData()
	{
		if(Config.DEBUG)
			_log.info("SevenSigns: Resetting player data for new event period.");

		for(StatsSet sevenDat : _signsPlayerData.values())
		{
			int charObjId = sevenDat.getInteger("char_obj_id");
			// Reset seal stones and contribution score for winning cabal
			if(sevenDat.getInteger("cabal") == getCabalWinner())
				switch(getCabalWinner())
				{
					case CABAL_DAWN:
						sevenDat.set("dawn_red_stones", 0);
						sevenDat.set("dawn_green_stones", 0);
						sevenDat.set("dawn_blue_stones", 0);
						sevenDat.set("dawn_contribution_score", 0);
						break;
					case CABAL_DUSK:
						sevenDat.set("dusk_red_stones", 0);
						sevenDat.set("dusk_green_stones", 0);
						sevenDat.set("dusk_blue_stones", 0);
						sevenDat.set("dusk_contribution_score", 0);
						break;
				}
			else if(sevenDat.getInteger("cabal") == CABAL_DAWN || sevenDat.getInteger("cabal") == CABAL_NULL)
			{
				sevenDat.set("dusk_red_stones", 0);
				sevenDat.set("dusk_green_stones", 0);
				sevenDat.set("dusk_blue_stones", 0);
				sevenDat.set("dusk_contribution_score", 0);
			}
			else if(sevenDat.getInteger("cabal") == CABAL_DUSK || sevenDat.getInteger("cabal") == CABAL_NULL)
			{
				sevenDat.set("dawn_red_stones", 0);
				sevenDat.set("dawn_green_stones", 0);
				sevenDat.set("dawn_blue_stones", 0);
				sevenDat.set("dawn_contribution_score", 0);
			}

			// Reset the player's cabal and seal information
			sevenDat.set("cabal", CABAL_NULL);
			sevenDat.set("seal", SEAL_NULL);
			_signsPlayerData.put(charObjId, sevenDat);
		}
		// A database update should soon follow this!
	}

	/**
	 * Tests whether the specified player has joined a cabal in the past.
	 *
	 * @param player
	 * @return boolean hasRegistered
	 */
	private boolean hasRegisteredBefore(L2Player player)
	{
		return _signsPlayerData.containsKey(player.getObjectId());
	}

	/**
	 * Used to specify cabal-related details for the specified player. This method
	 * checks to see if the player has registered before and will update the database
	 * if necessary.
	 * <BR>
	 * Returns the cabal ID the player has joined.
	 *
	 * @param player
	 * @param chosenCabal
	 * @param chosenSeal
	 * @return int cabal
	 */
	public int setPlayerInfo(L2Player player, int chosenCabal, int chosenSeal)
	{
		final int charObjId = player.getObjectId();

		Connection con = null;
		PreparedStatement statement = null;
		StatsSet currPlayer = null;

		if(hasRegisteredBefore(player))
		{
			// If the seal validation period has passed,
			// cabal information was removed and so "re-register" player
			currPlayer = _signsPlayerData.get(charObjId);
			currPlayer.set("cabal", chosenCabal);
			currPlayer.set("seal", chosenSeal);

			_signsPlayerData.put(charObjId, currPlayer);
		}
		else
		{
			currPlayer = new StatsSet();
			currPlayer.set("char_obj_id", charObjId);
			currPlayer.set("cabal", chosenCabal);
			currPlayer.set("seal", chosenSeal);
			currPlayer.set("dawn_red_stones", 0);
			currPlayer.set("dawn_green_stones", 0);
			currPlayer.set("dawn_blue_stones", 0);
			currPlayer.set("dawn_ancient_adena_amount", 0);
			currPlayer.set("dawn_contribution_score", 0);
			currPlayer.set("dusk_red_stones", 0);
			currPlayer.set("dusk_green_stones", 0);
			currPlayer.set("dusk_blue_stones", 0);
			currPlayer.set("dusk_ancient_adena_amount", 0);
			currPlayer.set("dusk_contribution_score", 0);

			_signsPlayerData.put(charObjId, currPlayer);

			// Update data in database, as we have a new player signing up.
			try
			{
				con = DatabaseFactory.getInstance().getConnection();

				statement = con.prepareStatement("INSERT INTO seven_signs (char_obj_id, cabal, seal) VALUES (?,?,?)");
				statement.setInt(1, charObjId);
				statement.setString(2, getCabalShortName(chosenCabal));
				statement.setInt(3, chosenSeal);
				statement.execute();

				if(Config.DEBUG)
					_log.info("SevenSigns: Inserted data in DB for char ID " + currPlayer.getInteger("char_obj_id") + " (" + getCabalShortName(currPlayer.getInteger("cabal")) + ")");

			}
			catch(SQLException e)
			{
				_log.warn("SevenSigns: Failed to save data: " + e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
		long contribScore = 0;

		switch(chosenCabal)
		{
			case CABAL_DAWN:
				contribScore = calcContributionScore(currPlayer.getInteger("dawn_blue_stones"), currPlayer.getInteger("dawn_green_stones"), currPlayer.getInteger("dawn_red_stones"));
				_dawnStoneScore += contribScore;
				break;
			case CABAL_DUSK:
				contribScore = calcContributionScore(currPlayer.getInteger("dusk_blue_stones"), currPlayer.getInteger("dusk_green_stones"), currPlayer.getInteger("dusk_red_stones"));
				_duskStoneScore += contribScore;
				break;
		}

		// Increasing Seal total score for the player chosen Seal.
		if(currPlayer.getInteger("cabal") == CABAL_DAWN)
			_signsDawnSealTotals.put(chosenSeal, _signsDawnSealTotals.get(chosenSeal) + 1);
		else
			_signsDuskSealTotals.put(chosenSeal, _signsDuskSealTotals.get(chosenSeal) + 1);

		saveSevenSignsData(player);

		if(Config.DEBUG)
			_log.info("SevenSigns: " + player.getName() + " has joined the " + getCabalName(chosenCabal) + " for the " + getSealName(chosenSeal, false) + "!");

		return chosenCabal;
	}

	/**
	 * Returns the amount of ancient adena the specified player can claim, if any.<BR>
	 * If removeReward = True, all the ancient adena owed to them is removed, then
	 * DB is updated.
	 *
	 * @param player
	 * @param removeReward
	 * @return int rewardAmount
	 */
	public int getAncientAdenaReward(L2Player player, boolean removeReward)
	{
		int charObjId = player.getObjectId();
		StatsSet currPlayer = _signsPlayerData.get(charObjId);

		int rewardAmount = 0;
		if(currPlayer.getInteger("cabal") == CABAL_DAWN)
		{
			rewardAmount = currPlayer.getInteger("dawn_ancient_adena_amount");
			currPlayer.set("dawn_ancient_adena_amount", 0);
		}
		else
		{
			rewardAmount = currPlayer.getInteger("dusk_ancient_adena_amount");
			currPlayer.set("dusk_ancient_adena_amount", 0);
		}

		if(removeReward)
		{
			_signsPlayerData.put(charObjId, currPlayer);
			saveSevenSignsData(player);
		}

		return rewardAmount;
	}

	/**
	 * Used to add the specified player's seal stone contribution points
	 * to the current total for their cabal. Returns the point score the
	 * contribution was worth.
	 * <p/>
	 * Each stone count <B>must be</B> broken down and specified by the stone's color.
	 *
	 * @param player
	 * @param blueCount
	 * @param greenCount
	 * @param redCount
	 * @return int contribScore
	 */
	public long addPlayerStoneContrib(L2Player player, long blueCount, long greenCount, long redCount)
	{
		int charObjId = player.getObjectId();
		StatsSet currPlayer = _signsPlayerData.get(charObjId);

		long contribScore = calcContributionScore(blueCount, greenCount, redCount);
		long totalAncientAdena = 0;
		long totalContribScore = 0;

		if(currPlayer.getInteger("cabal") == CABAL_DAWN)
		{
			totalAncientAdena = currPlayer.getInteger("dawn_ancient_adena_amount") + calcAncientAdenaReward(blueCount, greenCount, redCount);
			totalContribScore = currPlayer.getInteger("dawn_contribution_score") + contribScore;

			if(totalContribScore > MAXIMUM_PLAYER_CONTRIB)
				return -1;

			currPlayer.set("dawn_red_stones", currPlayer.getInteger("dawn_red_stones") + redCount);
			currPlayer.set("dawn_green_stones", currPlayer.getInteger("dawn_green_stones") + greenCount);
			currPlayer.set("dawn_blue_stones", currPlayer.getInteger("dawn_blue_stones") + blueCount);
			currPlayer.set("dawn_ancient_adena_amount", totalAncientAdena);
			currPlayer.set("dawn_contribution_score", totalContribScore);
			_signsPlayerData.put(charObjId, currPlayer);
			_dawnStoneScore += contribScore;
		}
		else
		{
			totalAncientAdena = currPlayer.getInteger("dusk_ancient_adena_amount") + calcAncientAdenaReward(blueCount, greenCount, redCount);
			totalContribScore = currPlayer.getInteger("dusk_contribution_score") + contribScore;

			if(totalContribScore > MAXIMUM_PLAYER_CONTRIB)
				return -1;

			currPlayer.set("dusk_red_stones", currPlayer.getInteger("dusk_red_stones") + redCount);
			currPlayer.set("dusk_green_stones", currPlayer.getInteger("dusk_green_stones") + greenCount);
			currPlayer.set("dusk_blue_stones", currPlayer.getInteger("dusk_blue_stones") + blueCount);
			currPlayer.set("dusk_ancient_adena_amount", totalAncientAdena);
			currPlayer.set("dusk_contribution_score", totalContribScore);
			_signsPlayerData.put(charObjId, currPlayer);
			_duskStoneScore += contribScore;
		}

		saveSevenSignsData(player);

		if(Config.DEBUG)
			_log.info("SevenSigns: " + player.getName() + " contributed " + contribScore + " seal stone points to their cabal.");

		return contribScore;
	}

	/**
	 * Send info on the current Seven Signs period to the specified player.
	 *
	 * @param player
	 */
	public void sendCurrentPeriodMsg(L2Player player)
	{
		SystemMessage sm = null;

		switch(_activePeriod)
		{
			case PERIOD_COMP_RECRUITING:
				sm = new SystemMessage(SystemMessage.SEVEN_SIGNS_PREPARATIONS_HAVE_BEGUN_FOR_THE_NEXT_QUEST_EVENT);
				break;
			case PERIOD_COMPETITION:
				sm = new SystemMessage(SystemMessage.SEVEN_SIGNS_THE_QUEST_EVENT_PERIOD_HAS_BEGUN_SPEAK_WITH_A_PRIEST_OF_DAWN_OR_DUSK_PRIESTESS_IF_YOU_WISH_TO_PARTICIPATE_IN_THE_EVENT);
				break;
			case PERIOD_COMP_RESULTS:
				sm = new SystemMessage(SystemMessage.SEVEN_SIGNS_QUEST_EVENT_HAS_ENDED_RESULTS_ARE_BEING_TALLIED);
				break;
			case PERIOD_SEAL_VALIDATION:
				sm = new SystemMessage(SystemMessage.SEVEN_SIGNS_THIS_IS_THE_SEAL_VALIDATION_PERIOD_A_NEW_QUEST_EVENT_PERIOD_BEGINS_NEXT_MONDAY);
				break;
		}

		if(sm != null)
			player.sendPacket(sm);
	}

	/**
	 * Sends the built-in system message specified by sysMsgId to all online players.
	 *
	 * @param sysMsgId
	 */
	public void sendMessageToAll(int sysMsgId)
	{
		SystemMessage sm = new SystemMessage(sysMsgId);

		for(L2Player player : L2ObjectsStorage.getAllPlayers())
			player.sendPacket(sm);
	}

	/**
	 * Used to initialize the seals for each cabal. (Used at startup or at beginning of a new cycle).
	 * This method should	be called after <B>resetSeals()</B> and <B>calcNewSealOwners()</B> on a new cycle.
	 */
	protected void initializeSeals()
	{
		for(Integer currSeal : _signsSealOwners.keySet())
		{
			int sealOwner = _signsSealOwners.get(currSeal);

			if(sealOwner != CABAL_NULL)
				if(isSealValidationPeriod())
					_log.info("SevenSigns: The " + getCabalName(sealOwner) + " have won the " + getSealName(currSeal, false) + ".");
				else
					_log.info("SevenSigns: The " + getSealName(currSeal, false) + " is currently owned by " + getCabalName(sealOwner) + ".");
			else
				_log.info("SevenSigns: The " + getSealName(currSeal, false) + " remains unclaimed.");
		}
	}

	/**
	 * Only really used at the beginning of a new cycle, this method resets all seal-related data.
	 */
	protected void resetSeals()
	{
		_signsDawnSealTotals.put(SEAL_AVARICE, 0);
		_signsDawnSealTotals.put(SEAL_GNOSIS, 0);
		_signsDawnSealTotals.put(SEAL_STRIFE, 0);
		_signsDuskSealTotals.put(SEAL_AVARICE, 0);
		_signsDuskSealTotals.put(SEAL_GNOSIS, 0);
		_signsDuskSealTotals.put(SEAL_STRIFE, 0);
	}

	/**
	 * Calculates the ownership of the three Seals of the Seven Signs,
	 * based on various criterion.
	 * <BR><BR>
	 * Should only ever called at the beginning of a new cycle.
	 */
	protected void calcNewSealOwners()
	{
		_log.info("SevenSigns: (Avarice) Dawn = " + _signsDawnSealTotals.get(SEAL_AVARICE) + ", Dusk = " + _signsDuskSealTotals.get(SEAL_AVARICE));
		_log.info("SevenSigns: (Gnosis) Dawn = " + _signsDawnSealTotals.get(SEAL_GNOSIS) + ", Dusk = " + _signsDuskSealTotals.get(SEAL_GNOSIS));
		_log.info("SevenSigns: (Strife) Dawn = " + _signsDawnSealTotals.get(SEAL_STRIFE) + ", Dusk = " + _signsDuskSealTotals.get(SEAL_STRIFE));

		int totalDawnMembers = getTotalMembers(CABAL_DAWN);
		int totalDuskMembers = getTotalMembers(CABAL_DUSK);
		int cabalWinner = getCabalWinner();

		_log.info("SevenSigns: total dawn members: " + totalDawnMembers);
		_log.info("SevenSigns: total dusk members: " + totalDuskMembers);

		for(Integer currSeal : _signsDawnSealTotals.keySet())
		{
			int prevSealOwner = _signsSealOwners.get(currSeal);
			int newSealOwner = CABAL_NULL;
			int dawnProportion = getSealProportion(currSeal, CABAL_DAWN);
			int duskProportion = getSealProportion(currSeal, CABAL_DUSK);

			_log.info("SevenSigns: calcualte new owner for: " + getSealName(currSeal, true) + " previos owner: " + getCabalName(prevSealOwner));
			_log.info("SevenSigns: dawnProportion: " + dawnProportion);
			_log.info("SevenSigns: duskProportion: " + duskProportion);

			/*
			 * - If a Seal was already closed or owned by the opponent and the new winner wants
			 *	 to assume ownership of the Seal, 35% or more of the members of the Cabal must
			 *	 have chosen the Seal. If they chose less than 35%, they cannot own the Seal.
			 *
			 * - If the Seal was owned by the winner in the previous Seven Signs, they can retain
			 *	 that seal if 10% or more members have chosen it. If they want to possess a new Seal,
			 *	 at least 35% of the members of the Cabal must have chosen the new Seal.
			 */
			switch(prevSealOwner)
			{
				case CABAL_NULL: // No previous owner, Dawn and Dusk must have more 35% registered.
					switch(cabalWinner)
					{
						case CABAL_NULL:
							newSealOwner = prevSealOwner;
							break;
						case CABAL_DAWN:
							if(dawnProportion >= 0.35 * totalDawnMembers && dawnProportion > 0)
								newSealOwner = CABAL_DAWN;
							else
								newSealOwner = prevSealOwner;
							break;
						case CABAL_DUSK:
							if(duskProportion >= 0.35 * totalDuskMembers && duskProportion > 0)
								newSealOwner = CABAL_DUSK;
							else
								newSealOwner = prevSealOwner;
							break;
					}
					break;
				case CABAL_DAWN: // Previous owner Dawn, Dusk mast have more 35%, Dawn must have more 10%
					switch(cabalWinner)
					{
						case CABAL_NULL:
							newSealOwner = CABAL_NULL;
							break;
						case CABAL_DAWN:
							if(dawnProportion >= 0.10 * totalDawnMembers && dawnProportion > 0)
								newSealOwner = prevSealOwner;
							else
								newSealOwner = CABAL_NULL;
							break;
						case CABAL_DUSK:
							if(duskProportion >= 0.35 * totalDuskMembers && duskProportion > 0)
								newSealOwner = CABAL_DUSK;
							else
								newSealOwner = CABAL_NULL;
							break;
					}
					break;
				case CABAL_DUSK: // Previous owner Dusk, Dawn must have 35%, Dusk must have more 10%
					switch(cabalWinner)
					{
						case CABAL_NULL:
							newSealOwner = CABAL_NULL;
							break;
						case CABAL_DAWN:
							if(dawnProportion >= 0.35 * totalDawnMembers && dawnProportion > 0)
								newSealOwner = CABAL_DAWN;
							else
								newSealOwner = CABAL_NULL;
							break;
						case CABAL_DUSK:
							if(duskProportion >= 0.10 * totalDuskMembers && duskProportion > 0)
								newSealOwner = prevSealOwner;
							else
								newSealOwner = CABAL_NULL;
							break;
					}
					break;
			}

			_log.info("SevenSigns: new Seal of " + getSealName(currSeal, true) + " owner is " + getCabalName(newSealOwner));
			_signsSealOwners.put(currSeal, newSealOwner);

			// Alert all online players to new seal status.
			switch(currSeal)
			{
				case SEAL_AVARICE:
					if(newSealOwner == CABAL_DAWN)
						sendMessageToAll(SystemMessage.SEVEN_SIGNS_THE_LORDS_OF_DAWN_HAVE_OBTAINED_THE_SEAL_OF_AVARICE);
					else if(newSealOwner == CABAL_DUSK)
						sendMessageToAll(SystemMessage.SEVEN_SIGNS_THE_REVOLUTIONARIES_OF_DUSK_HAVE_OBTAINED_THE_SEAL_OF_AVARICE);
					break;
				case SEAL_GNOSIS:
					if(newSealOwner == CABAL_DAWN)
						sendMessageToAll(SystemMessage.SEVEN_SIGNS_THE_LORDS_OF_DAWN_HAVE_OBTAINED_THE_SEAL_OF_GNOSIS);
					else if(newSealOwner == CABAL_DUSK)
						sendMessageToAll(SystemMessage.SEVEN_SIGNS_THE_REVOLUTIONARIES_OF_DUSK_HAVE_OBTAINED_THE_SEAL_OF_GNOSIS);
					break;
				case SEAL_STRIFE:
					if(newSealOwner == CABAL_DAWN)
						sendMessageToAll(SystemMessage.SEVEN_SIGNS_THE_LORDS_OF_DAWN_HAVE_OBTAINED_THE_SEAL_OF_STRIFE);
					else if(newSealOwner == CABAL_DUSK)
						sendMessageToAll(SystemMessage.SEVEN_SIGNS_THE_REVOLUTIONARIES_OF_DUSK_HAVE_OBTAINED_THE_SEAL_OF_STRIFE);
					break;
			}
		}
	}

	public class SevenSignsAnnounce implements Runnable
	{
		public void run()
		{
			for(L2Player player : L2ObjectsStorage.getAllPlayers())
				if(player != null)
					sendCurrentPeriodMsg(player);
			ThreadPoolManager.getInstance().scheduleGeneral(new SevenSignsAnnounce(), Config.SS_ANNOUNCE_PERIOD * 1000 * 60);
		}
	}

	/**
	 * The primary controller of period change of the Seven Signs system.
	 * This runs all related tasks depending on the period that is about to begin.
	 */
	public class SevenSignsPeriodChange implements Runnable
	{
		public void run()
		{
			_log.info("SevenSignsPeriodChange: old=" + _activePeriod);
			int periodEnded = _activePeriod;
			_activePeriod++;
			int compWinner;
			sky = 256;
			switch(periodEnded)
			{
				case PERIOD_COMP_RECRUITING: // Initialization
					FestivalManager.getInstance().startFestival();
					sendMessageToAll(SystemMessage.SEVEN_SIGNS_THE_QUEST_EVENT_PERIOD_HAS_BEGUN_VISIT_A_PRIEST_OF_DAWN_OR_DUSK_TO_PARTICIPATE_IN_THE_EVENT);
					break;
				case PERIOD_COMPETITION: // Results Calculation
					sendMessageToAll(SystemMessage.SEVEN_SIGNS_THE_QUEST_EVENT_PERIOD_HAS_ENDED_THE_NEXT_QUEST_EVENT_WILL_START_IN_ONE_WEEK);
					// Schedule a stop of the festival engine.
					FestivalManager.getInstance().stopFestival();
					compWinner = getCabalWinner();

					if(compWinner == CABAL_NULL)
						sendMessageToAll(SystemMessage.THE_COMPETITION_HAS_ENDED_IN_A_TIE_THEREFORE_NOBODY_HAS_BEEN_AWARDED_THE_SEAL);
					else if(compWinner == CABAL_DUSK)
					{
						sky = 257;
						sendMessageToAll(SystemMessage.SEVEN_SIGNS_THE_REVOLUTIONARIES_OF_DUSK_HAVE_WON);
					}
					else
					{
						sky = 258;
						sendMessageToAll(SystemMessage.SEVEN_SIGNS_THE_LORDS_OF_DAWN_HAVE_WON);
					}
					calcNewSealOwners();
					_previousWinner = compWinner;
					break;
				case PERIOD_COMP_RESULTS: // Seal Validation
					// Perform initial Seal Validation set up.
					initializeSeals();
					// Send message that Seal Validation has begun.
					sendMessageToAll(SystemMessage.SEVEN_SIGNS_THE_SEAL_VALIDATION_PERIOD_HAS_BEGUN);
					// reward highest ranking members from cycle
					SevenSignsFestival.getInstance().rewardHighestRanked();
					_log.info("SevenSigns: The " + getCabalName(_previousWinner) + " have won the competition with " + getCurrentScore(_previousWinner) + " points!");
					break;
				case PERIOD_SEAL_VALIDATION: // Reset for New Cycle
					// Ensure a cycle restart when this period ends.
					_activePeriod = PERIOD_COMP_RECRUITING;
					// Send message that Seal Validation has ended.
					sendMessageToAll(SystemMessage.SEVEN_SIGNS_THE_SEAL_VALIDATION_PERIOD_HAS_ENDED);
					// Reset all data
					resetPlayerData();
					resetSeals();
					_dawnStoneScore = 0;
					_duskStoneScore = 0;
					_currentCycle++;
					// Reset all Festival-related data and remove any unused blood offerings.
					SevenSignsFestival.getInstance().resetFestivalData();
					updateFestivalScore();
					break;
			}
			// Make sure all Seven Signs data is saved for future use.
			saveSevenSignsData(null);
			_log.info("SevenSignsPeriodChange: new=" + _activePeriod);
			try
			{
				SSQInfo ss = new SSQInfo();

				for(L2Player player : L2ObjectsStorage.getAllPlayers())
					player.sendPacket(ss);

				_log.info("SevenSigns: Change Catacomb spawn...");
				_log.info("SevenSigns: Spawning NPCs...");
				spawnSevenSignsNPC();
				_log.info("SevenSigns: The " + getCurrentPeriodName() + " period has begun!");
				_log.info("SevenSigns: Calculating next period change time...");
				setCalendarForNextPeriodChange();
				_log.info("SevenSignsPeriodChange: SecondsToNextChange=" + getMilliToPeriodChange() / 1000);
				_periodChange = ThreadPoolManager.getInstance().scheduleGeneral(new SevenSignsPeriodChange(), getMilliToPeriodChange());
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public int getPriestCabal(int id)
	{
		switch(id)
		{
			case 31078:
			case 31079:
			case 31080:
			case 31081:
			case 31082: // Dawn Priests
			case 31083:
			case 31084:
			case 31168:
			case 31997:
			case 31692:
			case 31694:
				return CABAL_DAWN;
			case 31085:
			case 31086:
			case 31087:
			case 31088: // Dusk Priest
			case 31089:
			case 31090:
			case 31091:
			case 31169:
			case 31998:
			case 31693:
			case 31695:
				return CABAL_DUSK;
		}
		return CABAL_NULL;
	}

	public void changePeriod()
	{
		_periodChange = ThreadPoolManager.getInstance().scheduleGeneral(new SevenSignsPeriodChange(), 10);
	}

	public void changePeriod(int period)
	{
		changePeriod(period, 1);
	}

	public void changePeriod(int period, int seconds)
	{
		_activePeriod = period - 1;
		if(_activePeriod < 0)
			_activePeriod += 4;
		_periodChange = ThreadPoolManager.getInstance().scheduleGeneral(new SevenSignsPeriodChange(), seconds * 1000);
	}

	public void setTimeToNextPeriodChange(int time)
	{
		_calendar.setTimeInMillis(System.currentTimeMillis() + time * 60 * 1000);
		if(_periodChange != null)
			_periodChange.cancel(false);
		_periodChange = ThreadPoolManager.getInstance().scheduleGeneral(new SevenSignsPeriodChange(), getMilliToPeriodChange());
	}

	public int getSky()
	{
		return sky;
	}

	public static void setSky(int _sky)
	{
		sky = _sky;
	}

	/**
	 * returns true if the given date is in Seal Validation or in Quest Event Results period
	 *
	 * @param date
	 */
	public boolean isDateInSealValidPeriod(Calendar date)
	{
		long nextPeriodChange = getMilliToPeriodChange();
		long nextQuestStart = 0;
		long nextValidStart = 0;
		long tillDate = date.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
		while((2 * PERIOD_MAJOR_LENGTH + 2 * PERIOD_MINOR_LENGTH) < tillDate)
			tillDate -= (2 * PERIOD_MAJOR_LENGTH + 2 * PERIOD_MINOR_LENGTH);
		while(tillDate < 0)
			tillDate += (2 * PERIOD_MAJOR_LENGTH + 2 * PERIOD_MINOR_LENGTH);

		switch(getCurrentPeriod())
		{
			case PERIOD_COMP_RECRUITING:
				nextValidStart = nextPeriodChange + PERIOD_MAJOR_LENGTH;
				nextQuestStart = nextValidStart + PERIOD_MAJOR_LENGTH + PERIOD_MINOR_LENGTH;
				break;
			case PERIOD_COMPETITION:
				nextValidStart = nextPeriodChange;
				nextQuestStart = nextPeriodChange + PERIOD_MAJOR_LENGTH + PERIOD_MINOR_LENGTH;
				break;
			case PERIOD_COMP_RESULTS:
				nextQuestStart = nextPeriodChange + PERIOD_MAJOR_LENGTH;
				nextValidStart = nextQuestStart + PERIOD_MAJOR_LENGTH + PERIOD_MINOR_LENGTH;
				break;
			case PERIOD_SEAL_VALIDATION:
				nextQuestStart = nextPeriodChange;
				nextValidStart = nextPeriodChange + PERIOD_MAJOR_LENGTH + PERIOD_MINOR_LENGTH;
				break;
		}

		return !((nextQuestStart < tillDate && tillDate < nextValidStart) ||
				(nextValidStart < nextQuestStart && (tillDate < nextValidStart || nextQuestStart < tillDate)));
	}

	public static int getStoneIdByType(int stoneType)
	{
		switch(stoneType)
		{
			case 1:
				return SEAL_STONE_BLUE_ID;
			case 2:
				return SEAL_STONE_GREEN_ID;
			case 3:
				return SEAL_STONE_RED_ID;
			default:
				return 0;
		}
	}
}
