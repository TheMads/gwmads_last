package ru.l2gw.gameserver.tables;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.FishData;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class FishTable
{
	private static Log _log = LogFactory.getLog(SkillTreeTable.class.getName());
	private static FishTable _instance = new FishTable();

	private static List<FishData> _fishsNormal;
	private static List<FishData> _fishsEasy;
	private static List<FishData> _fishsHard;

	public static FishTable getInstance()
	{
		return _instance;
	}

	private FishTable()
	{
		//Create table that contains all fish datas
		int count = 0;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			try
			{
				_fishsEasy = new ArrayList<>();
				_fishsNormal = new ArrayList<>();
				_fishsHard = new ArrayList<>();
				FishData fish;
				statement = con.prepareStatement("SELECT id, level, name, hp, hpregen, fish_type, fish_group, fish_guts, guts_check_time, wait_time, combat_time FROM fish ORDER BY id");
				resultSet = statement.executeQuery();

				while(resultSet.next())
				{
					int id = resultSet.getInt("id");
					int lvl = resultSet.getInt("level");
					String name = resultSet.getString("name");
					int hp = resultSet.getInt("hp");
					int hpreg = resultSet.getInt("hpregen");
					int type = resultSet.getInt("fish_type");
					int group = resultSet.getInt("fish_group");
					int fish_guts = resultSet.getInt("fish_guts");
					int guts_check_time = resultSet.getInt("guts_check_time");
					int wait_time = resultSet.getInt("wait_time");
					int combat_time = resultSet.getInt("combat_time");
					fish = new FishData(id, lvl, name, hp, hpreg, type, group, fish_guts, guts_check_time, wait_time, combat_time);
					switch(fish.getGroup())
					{
						case 0:
							_fishsEasy.add(fish);
							break;
						case 1:
							_fishsNormal.add(fish);
							break;
						case 2:
							_fishsHard.add(fish);
					}
				}
				count = _fishsEasy.size() + _fishsNormal.size() + _fishsHard.size();
			}
			catch(Exception e)
			{
				_log.error("error while creating fishes table" + e);
			}
			finally
			{
				DbUtils.closeQuietly(statement, resultSet);
			}
			_log.info("FishTable: Loaded " + count + " Fishes.");
		}
		catch(Exception e)
		{
			_log.warn(e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, resultSet);
		}
	}

	/**
	 * @param Fish - lvl
	 * @param Fish - type
	 * @param Fish - group
	 * @return List of Fish that can be fished
	 */
	public List<FishData> getfish(int lvl, int type, int group)
	{
		List<FishData> result = new ArrayList<>();
		List<FishData> fishs = null;
		switch(group)
		{
			case 0:
				fishs = _fishsEasy;
				break;
			case 1:
				fishs = _fishsNormal;
				break;
			case 2:
				fishs = _fishsHard;
		}
		if(fishs == null)
		{
			// the fish list is empty
			_log.warn("Fish are not defined !");
			return null;
		}
		for(FishData f : fishs)
			if(f.getLevel() == lvl && f.getType() == type)
				result.add(new FishData(f.getId(), f.getLevel(), f.getName(), f.getHP(), f.getHpRegen(), f.getType(), f.getGroup(), f.getFishGuts(), f.getGutsCheckTime(), f.getWaitTime(), f.getCombatTime()));

		if(result.size() == 0)
			_log.warn("Cant Find Any Fish!? - Lvl: " + lvl + " Type: " + type + " Group: " + group);
		return result;
	}

	public int GetRandomFishType(int group, int lureId)
	{
		int check = Rnd.get(100);
		int type = 1;
		switch(group)
		{
			case 0: // fish for novices
				switch(lureId)
				{
					case 7807: //green lure, preferred by fast-moving (nimble) fish (type 5)
						if(check <= 54)
							type = 5;
						else if(check <= 77)
							type = 4;
						else
							type = 6;
						break;
					case 7808: //purple lure, preferred by fat fish (type 4)
						if(check <= 54)
							type = 4;
						else if(check <= 77)
							type = 6;
						else
							type = 5;
						break;
					case 7809: //yellow lure, preferred by ugly fish (type 6)
						if(check <= 54)
							type = 6;
						else if(check <= 77)
							type = 5;
						else
							type = 4;
						break;
					case 8486: //prize-winning fishing lure for beginners
						if(check <= 33)
							type = 4;
						else if(check <= 66)
							type = 5;
						else
							type = 6;
						break;
				}
				break;
			case 1: // normal fish
				switch(lureId)
				{
					case 7610:
					case 7611:
					case 7612:
					case 7613:
						type = 3;
						break;
					case 6519: // all theese lures (green) are prefered by fast-moving (nimble) fish (type 1)
					case 8505:
					case 6520:
					case 6521:
					case 8507:
						if(check <= 54)
							type = 1;
						else if(check <= 74)
							type = 0;
						else if(check <= 94)
							type = 2;
						else
							type = 3;
						break;
					case 6522: // all theese lures (purple) are prefered by fat fish (type 0)
					case 8508:
					case 6523:
					case 6524:
					case 8510:
						if(check <= 54)
							type = 0;
						else if(check <= 74)
							type = 1;
						else if(check <= 94)
							type = 2;
						else
							type = 3;
						break;
					case 6525: // all theese lures (yellow) are prefered by ugly fish (type 2)
					case 8511:
					case 6526:
					case 6527:
					case 8513:
						if(check <= 55)
							type = 2;
						else if(check <= 74)
							type = 1;
						else if(check <= 94)
							type = 0;
						else
							type = 3;
						break;
					case 8484: // prize-winning fishing lure
						if(check <= 33)
							type = 0;
						else if(check <= 66)
							type = 1;
						else
							type = 2;
						break;
				}
				break;
			case 2: // upper grade fish, luminous lure
				switch(lureId)
				{
					case 8506: //green lure, preferred by fast-moving (nimble) fish (type 8)
						if(check <= 54)
							type = 8;
						else if(check <= 77)
							type = 7;
						else
							type = 9;
						break;
					case 8509: // purple lure, preferred by fat fish (type 7)
						if(check <= 54)
							type = 7;
						else if(check <= 77)
							type = 9;
						else
							type = 8;
						break;
					case 8512: // yellow lure, preferred by ugly fish (type 9)
						if(check <= 54)
							type = 9;
						else if(check <= 77)
							type = 8;
						else
							type = 7;
						break;
					case 8485: // prize-winning fishing lure
						if(check <= 33)
							type = 7;
						else if(check <= 66)
							type = 8;
						else
							type = 9;
						break;
				}
		}
		return type;
	}

	public int GetRandomFishLvl(L2Player player)
	{
		int skilllvl = 0;

		// Проверка на Fisherman's Potion
		L2Effect effect = player.getEffectBySkillId(2274);
		if(effect != null)
			skilllvl = (int) effect.getSkill().getPower(player, null);
		else
			skilllvl = player.getSkillLevel(1315);

		if(skilllvl <= 0)
			return 1;

		int randomlvl;
		int check = Rnd.get(100);

		if(check < 50)
			randomlvl = skilllvl;
		else if(check <= 85)
		{
			randomlvl = skilllvl - 1;
			if(randomlvl <= 0)
				randomlvl = 1;
		}
		else
			randomlvl = skilllvl + 1;

		if(randomlvl > 27)
			randomlvl = 27;

		return randomlvl;
	}

	public int GetGroupForLure(int lureId)
	{
		switch(lureId)
		{
			case 7807: // green for beginners
			case 7808: // purple for beginners
			case 7809: // yellow for beginners
			case 8486: // prize-winning for beginners
				return 0;
			case 8506: // green luminous
			case 8509: // purple luminous
			case 8512: // yellow luminous
			case 8485: // prize-winning luminous
				return 2;
			default:
				return 1;
		}

		/**
		switch(lureId)
		{
			case 7807: // green for beginners
			case 7808: // purple for beginners
			case 7809: // yellow for beginners
			case 8486: // prize-winning for beginners
				return 0;
			case 8485: // Prize-Winning Night Fishing Lure
			case 8505: // Green Luminous Lure - Low Grade
			case 8506: // Green Luminous Lure
			case 8507: // Green Colored Lure - High Grade
			case 8508: // Purple Luminous Lure - Low Grade
			case 8509: // Purple Luminous Lure
			case 8510: // Purple Luminous Lure - High Grade
			case 8511: // Yellow Luminous Lure - Low Grade
			case 8512: // Yellow Luminous Lure
			case 8513: // Yellow Luminous Lure - High Grade
				return 2;
			default:
				return 1;
		}
		*/
	}

	public void reload()
	{
		_instance = new FishTable();
	}
}