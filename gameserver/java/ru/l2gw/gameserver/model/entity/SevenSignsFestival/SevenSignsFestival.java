package ru.l2gw.gameserver.model.entity.SevenSignsFestival;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.SevenSigns;
import ru.l2gw.gameserver.serverpackets.PledgeShowInfoUpdate;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ClanTable;
import ru.l2gw.gameserver.templates.StatsSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Seven Signs Festival of Darkness Engine
 * @author rage
 */
public class SevenSignsFestival
{
	private static Log _log = LogFactory.getLog("sevensigns");
	public static final int FAME_REWARD = 1080;
	public static final String SEVEN_SIGNS_FESTIVAL_STATUS = "data/festival.status";
	private static SevenSignsFestival _instance;
	public static final String GET_CLAN_NAME = "SELECT clan_name FROM clan_data WHERE clan_id = (SELECT clanid FROM characters WHERE char_name = ?)";
	public static final String ADD_FAME_POINTS = "UPDATE characters SET prPoints = prPoints + " + FAME_REWARD + " WHERE char_name = ?";
	// Key Constants
	public static final short FESTIVAL_OFFERING_ID = 5901;

	private static Map<Integer, Integer> _accumulatedBonuses; // The total bonus available (in Ancient Adena)
	private static Map<Integer, FestivalParty> _currentTopPartys;
	private static Map<Integer, FestivalParty> _topPartys;

	private static List<FestivalParty> _contributePartys;

	public SevenSignsFestival()
	{
		_accumulatedBonuses = new FastMap<Integer, Integer>();
		_currentTopPartys = new FastMap<Integer, FestivalParty>();
		_topPartys = new FastMap<Integer, FestivalParty>();

		_contributePartys = new FastList<FestivalParty>();
		restoreFestivalData();
		FestivalManager.getInstance();

		if(SevenSigns.getInstance().isCompetitionPeriod())
			FestivalManager.getInstance().startFestival();
	}

	public static SevenSignsFestival getInstance()
	{
		if(_instance == null)
			_instance = new SevenSignsFestival();
		return _instance;
	}

	/**
	 * Restores saved festival data
	 */
	private void restoreFestivalData()
	{
		try
		{
			Properties festivalStatus = new Properties();
			InputStream is = new FileInputStream(new File(Config.DATAPACK_ROOT, SEVEN_SIGNS_FESTIVAL_STATUS));
			festivalStatus.load(is);
			is.close();

			for(Integer level : FestivalManager.getInstance().getFestivalLevels())
			{
				_accumulatedBonuses.put(level, Integer.parseInt(festivalStatus.getProperty("accumulatedBonus" + level, "0")));
				String partyInfo = festivalStatus.getProperty("topParty" + level, "");
				if(!partyInfo.isEmpty())
					_topPartys.put(level, new FestivalParty(partyInfo));

			}

			for(Integer festId : FestivalManager.getInstance().getFestivals().keySet())
			{
				String partyInfo = festivalStatus.getProperty("currentTopParty" + festId, "");
				if(!partyInfo.isEmpty())
					_currentTopPartys.put(festId, new FestivalParty(partyInfo));
			}
		}
		catch(java.io.IOException e)
		{
			_log.warn("SevenSignsFestival: can't load data " + e);
		}
	}

	/**
	 * Stores current festival data, basic settings to the properties file
	 * and past high score data to the database.
	 */
	public synchronized void saveFestivalData()
	{
		try
		{
			Properties festivalStatus = new Properties();

			FileOutputStream fos = new FileOutputStream(new File(Config.DATAPACK_ROOT, SEVEN_SIGNS_FESTIVAL_STATUS));

			for(Integer level : FestivalManager.getInstance().getFestivalLevels())
			{
				if(_accumulatedBonuses.containsKey(level))
					festivalStatus.setProperty("accumulatedBonus" + level, String.valueOf(_accumulatedBonuses.get(level)));
				else
					festivalStatus.setProperty("accumulatedBonus" + level, "0");

				if(_topPartys.containsKey(level))
					festivalStatus.setProperty("topParty" + level, _topPartys.get(level).toString());
				else
					festivalStatus.setProperty("topParty" + level, "");
			}

			for(Integer festId : FestivalManager.getInstance().getFestivals().keySet())
			{
				if(_currentTopPartys.containsKey(festId))
					festivalStatus.setProperty("currentTopParty" + festId, _currentTopPartys.get(festId).toString());
				else
					festivalStatus.setProperty("currentTopParty" + festId, "");
			}

			festivalStatus.store(fos, "Seven Signs Festival Data");
		}
		catch(java.io.IOException e)
		{
			_log.warn("SevenSignsFestival: can't save status data " + e);
		}
	}

	/**
	 * If a clan member is a member of the highest-ranked party in the Festival of Darkness, 200 points are added per member
	 * Also, 1080 Fame is granted to player.
	 */
	public void rewardHighestRanked()
	{
		for(FestivalParty fp : _currentTopPartys.values())
			if(fp.getCabal() == getWiningCabalForLevel(fp.getFestivalLevel()))
				for(StatsSet member : fp.getMembers())
				{
					addReputationPointsForPartyMemberClan(member.getString("name"));
					addFamePointsForPartyMember(member.getString("name"));
				}
	}

	private void addReputationPointsForPartyMemberClan(String partyMemberName)
	{
		L2Player player = L2ObjectsStorage.getPlayer(partyMemberName);
		if(player != null)
		{
			if(player.getClanId() != 0)
			{
				L2Clan clan = player.getClan();
				clan.incReputation(200, true, "SevenSignsFestival");
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				SystemMessage sm = new SystemMessage(SystemMessage.CLAN_MEMBER_S1_WAS_AN_ACTIVE_MEMBER_OF_THE_HIGHEST_RANKED_PARTY_IN_THE_FESTIVAL_OF_DARKNESS_S2_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_REPUTATION_SCORE);
				sm.addString(partyMemberName);
				sm.addNumber(200);
				clan.broadcastToOnlineMembers(sm);
			}
		}
		else
		{
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement(GET_CLAN_NAME);
				statement.setString(1, partyMemberName);
				ResultSet rset = statement.executeQuery();
				if(rset.next())
				{
					String clanName = rset.getString("clan_name");
					if(clanName != null)
					{
						L2Clan clan = ClanTable.getInstance().getClanByName(clanName);
						if(clan != null)
						{
							clan.incReputation(200, true, "SevenSignsFestival");
							clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
							SystemMessage sm = new SystemMessage(SystemMessage.CLAN_MEMBER_S1_WAS_AN_ACTIVE_MEMBER_OF_THE_HIGHEST_RANKED_PARTY_IN_THE_FESTIVAL_OF_DARKNESS_S2_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_REPUTATION_SCORE);
							sm.addString(partyMemberName);
							sm.addNumber(200);
							clan.broadcastToOnlineMembers(sm);
						}
					}
				}
				DbUtils.closeQuietly(rset);
				DbUtils.closeQuietly(statement);
			}
			catch(Exception e)
			{
				_log.warn("could not get clan name of " + partyMemberName + ": " + e);
			}
			finally
			{
				DbUtils.closeQuietly(con);
			}
		}
	}

	private void addFamePointsForPartyMember(String partyMemberName)
	{
		L2Player player = L2ObjectsStorage.getPlayer(partyMemberName);
		if(player != null)
			player.addFame(1080);
		else
		{
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement(ADD_FAME_POINTS);
				statement.setString(1, partyMemberName);
				statement.execute();
			}
			catch(Exception e)
			{
				_log.warn("SQL Error while updating prPoints for offline char " + partyMemberName + ": " + e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}

	/**
	 * Used to reset all festival data at the beginning of a new quest event period.
	 */
	public void resetFestivalData()
	{
		_accumulatedBonuses.clear();
		_currentTopPartys.clear();
		saveFestivalData();
		_log.info("SevenSignsFestival: Reinitialized engine for next competition period.");
	}

	public int getAccumulatedBonus(int level)
	{
		if(_accumulatedBonuses.containsKey(level))
			return _accumulatedBonuses.get(level);
		return 0;
	}

	public void addAccumulatedBonus(int level, int stoneType, int stoneAmount)
	{
		int eachStoneBonus = 0;
		switch(stoneType)
		{
			case SevenSigns.SEAL_STONE_BLUE_ID:
				eachStoneBonus = SevenSigns.SEAL_STONE_BLUE_VALUE;
				break;
			case SevenSigns.SEAL_STONE_GREEN_ID:
				eachStoneBonus = SevenSigns.SEAL_STONE_GREEN_VALUE;
				break;
			case SevenSigns.SEAL_STONE_RED_ID:
				eachStoneBonus = SevenSigns.SEAL_STONE_RED_VALUE;
				break;
		}
		int bonus = stoneAmount * eachStoneBonus;
		if(_accumulatedBonuses.containsKey(level))
			bonus += _accumulatedBonuses.get(level);

		_accumulatedBonuses.put(level, bonus);
	}

	/**
	 * Calculate and return the proportion of the accumulated bonus for the festival
	 * where the player was in the winning party, if the winning party's cabal won the event.
	 * The accumulated bonus is then updated, with the player's share deducted.
	 * @param level
	 * @return playerBonus (the share of the bonus for the party)
	 */
	public int getAccumulatedBonusByLevel(int level)
	{
		int freeBonus = 0;
		int freeCount = 0;
		int totalBonus = 0;
		int winCabal = SevenSigns.getInstance().getCabalWinner();

		for(Integer lvl : FestivalManager.getInstance().getFestivalLevels())
		{
			FestivalParty fp = _currentTopPartys.get(FestivalManager.getInstance().getFestivalIdByCabalLevel(winCabal, lvl));

			if(fp == null)
				continue;

			if(fp.getFestivalLevel() == level)
			{
				if(!fp.isAborted())
					totalBonus = _accumulatedBonuses.get(level);
			}
			else if(fp.isAborted())
			{
				freeBonus += _accumulatedBonuses.get(lvl);
				freeCount++;
			}
		}

		if(totalBonus > 0)
			return totalBonus + (int)(freeCount > 0 ? (float) freeBonus / (FestivalManager.getInstance().getFestivalLevels().size() - freeCount) : 0);

		return 0;
	}

	public void addContributeParty(FestivalParty fp)
	{
		_contributePartys.add(fp);
	}

	public synchronized List<FestivalParty> getContrubutePartys()
	{
		return _contributePartys;
	}

	public FestivalParty getCurrentTopParty(int festivalId)
	{
		return _currentTopPartys.get(festivalId);
	}

	public void setCurrentTopParty(int festivalId, FestivalParty fp)
	{
		_currentTopPartys.put(festivalId, fp);
	}

	public FestivalParty getTopParty(int level)
	{
		return _topPartys.get(level);
	}

	public void setTopParty(int level, FestivalParty fp)
	{
		_topPartys.put(level, fp);	
	}

	public int getWiningCabalForLevel(int level)
	{
		int cabal = SevenSigns.CABAL_NULL;
		long score = 0;
		for(FestivalParty fp : _currentTopPartys.values())
			if(fp.getFestivalLevel() == level && fp.getScore() > score)
			{
				score = fp.getScore();
				cabal = fp.getCabal();
			}

		return cabal;
	}

	public int getFestivalScore(int cabal)
	{
		int score = 0;
		for(Integer level : FestivalManager.getInstance().getFestivalLevels())
			if(getWiningCabalForLevel(level) == cabal)
				score += FestivalManager.getInstance().getFestivalRewardPoints(level);

		return score;
	}
}