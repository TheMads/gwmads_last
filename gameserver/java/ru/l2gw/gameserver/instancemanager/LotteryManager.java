package ru.l2gw.gameserver.instancemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.Announcements;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

public class LotteryManager
{

	public static final long SECOND = 1000;
	public static final long MINUTE = 60000;

	private static LotteryManager _instance;
	protected static final Log _log = LogFactory.getLog(LotteryManager.class.getName());

	private static final String INSERT_LOTTERY = "INSERT INTO lottery (id, idnr, enddate, prize, newprize) VALUES (?, ?, ?, ?, ?)";
	private static final String UPDATE_PRICE = "UPDATE lottery SET prize=?, newprize=? WHERE id = 1 AND idnr = ?";
	private static final String UPDATE_LOTTERY = "UPDATE lottery SET finished=1, prize=?, newprize=?, number1=?, number2=?, prize1=?, prize2=?, prize3=? WHERE id=1 AND idnr=?";
	private static final String SELECT_LAST_LOTTERY = "SELECT idnr, prize, newprize, enddate, finished FROM lottery WHERE id = 1 ORDER BY idnr DESC LIMIT 1";
	private static final String SELECT_LOTTERY_ITEM = "SELECT enchant_level, custom_type2 FROM items WHERE item_id = 4442 AND custom_type1 = ?";
	private static final String SELECT_LOTTERY_TICKET = "SELECT number1, number2, prize1, prize2, prize3 FROM lottery WHERE id = 1 and idnr = ?";

	protected int _number;
	protected int _prize;
	protected boolean _isSellingTickets;
	protected boolean _isStarted;
	protected long _enddate;

	private LotteryManager()
	{
		_number = 1;
		_prize = Config.ALT_LOTTERY_PRIZE;
		_isSellingTickets = false;
		_isStarted = false;
		_enddate = System.currentTimeMillis();

		if(Config.ALLOW_LOTTERY)
			(new startLottery()).run();
	}

	public static LotteryManager getInstance()
	{
		if(_instance == null)
			_instance = new LotteryManager();
		return _instance;
	}

	public int getId()
	{
		return _number;
	}

	public int getPrize()
	{
		return _prize;
	}

	public long getEndDate()
	{
		return _enddate;
	}

	public void increasePrize(int count)
	{
		_prize += count;
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(UPDATE_PRICE);
			statement.setInt(1, getPrize());
			statement.setInt(2, getPrize());
			statement.setInt(3, getId());
			statement.execute();
			statement.close();
		}
		catch(SQLException e)
		{
			_log.warn("LotteryManager: Could not increase current lottery prize: " + e);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public boolean isSellableTickets()
	{
		return _isSellingTickets;
	}

	public boolean isStarted()
	{
		return _isStarted;
	}

	private class startLottery implements Runnable
	{
		protected startLottery()
		{
		// Do nothing
		}

		public void run()
		{
			Connection con = null;
			PreparedStatement statement = null;
			ResultSet rset = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement(SELECT_LAST_LOTTERY);
				rset = statement.executeQuery();

				if(rset.next())
				{
					_number = rset.getInt("idnr");

					if(rset.getInt("finished") == 1)
					{
						_number++;
						_prize = rset.getInt("newprize");
					}
					else
					{
						_prize = rset.getInt("prize");
						_enddate = rset.getLong("enddate");

						if(_enddate <= System.currentTimeMillis() + 2 * MINUTE)
						{
							(new finishLottery()).run();
							DbUtils.closeQuietly(statement, rset);
							return;
						}

						if(_enddate > System.currentTimeMillis())
						{
							_isStarted = true;
							ThreadPoolManager.getInstance().scheduleGeneral(new finishLottery(), _enddate - System.currentTimeMillis());

							if(_enddate > System.currentTimeMillis() + 12 * MINUTE)
							{
								_isSellingTickets = true;
								ThreadPoolManager.getInstance().scheduleGeneral(new stopSellingTickets(), _enddate - System.currentTimeMillis() - 10 * MINUTE);
							}
							DbUtils.closeQuietly(statement, rset);
							return;
						}
					}
				}
				DbUtils.closeQuietly(statement, rset);
			}
			catch(SQLException e)
			{
				_log.warn("LotteryManager: Could not restore lottery data: " + e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement, rset);
			}

			if(Config.DEBUG)
				_log.info("LotteryManager: Starting ticket sell for lottery #" + getId() + ".");
			_isSellingTickets = true;
			_isStarted = true;

			Announcements.getInstance().announceToAll("LotteryManager tickets are now available for Lucky LotteryManager #" + getId() + ".");
			Calendar finishtime = Calendar.getInstance();
			finishtime.setTimeInMillis(_enddate);
			finishtime.set(Calendar.MINUTE, 0);
			finishtime.set(Calendar.SECOND, 0);

			if(finishtime.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
			{
				finishtime.set(Calendar.HOUR_OF_DAY, 19);
				_enddate = finishtime.getTimeInMillis();
				_enddate += 604800000;
			}
			else
			{
				finishtime.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
				finishtime.set(Calendar.HOUR_OF_DAY, 19);
				_enddate = finishtime.getTimeInMillis();
			}

			ThreadPoolManager.getInstance().scheduleGeneral(new stopSellingTickets(), _enddate - System.currentTimeMillis() - 10 * MINUTE);
			ThreadPoolManager.getInstance().scheduleGeneral(new finishLottery(), _enddate - System.currentTimeMillis());

			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement(INSERT_LOTTERY);
				statement.setInt(1, 1);
				statement.setInt(2, getId());
				statement.setLong(3, getEndDate());
				statement.setInt(4, getPrize());
				statement.setInt(5, getPrize());
				statement.execute();
				statement.close();
			}
			catch(SQLException e)
			{
				_log.warn("LotteryManager: Could not store new lottery data: " + e);
			}
			finally
			{
				DbUtils.closeQuietly(statement, rset);
			}
		}
	}

	private class stopSellingTickets implements Runnable
	{
		protected stopSellingTickets()
		{
		// Do nothing
		}

		public void run()
		{
			if(Config.DEBUG)
				_log.info("LotteryManager: Stopping ticket sell for lottery #" + getId() + ".");
			_isSellingTickets = false;

			Announcements.getInstance().announceToAll(new SystemMessage(SystemMessage.LOTTERY_TICKET_SALES_HAVE_BEEN_TEMPORARILY_SUSPENDED));
		}
	}

	private class finishLottery implements Runnable
	{
		protected finishLottery()
		{
		// Do nothing
		}

		public void run()
		{
			if(Config.DEBUG)
				_log.info("LotteryManager: Ending lottery #" + getId() + ".");

			int[] luckynums = new int[5];
			int luckynum = 0;

			for(int i = 0; i < 5; i++)
			{
				boolean found = true;
				while(found)
				{
					luckynum = Rnd.get(20) + 1;
					found = false;

					for(int j = 0; j < i; j++)
						if(luckynums[j] == luckynum)
							found = true;
				}
				luckynums[i] = luckynum;
			}

			if(Config.DEBUG)
				_log.info("LotteryManager: The lucky numbers are " + luckynums[0] + ", " + luckynums[1] + ", " + luckynums[2] + ", " + luckynums[3] + ", " + luckynums[4] + ".");

			int enchant = 0;
			int type2 = 0;

			for(int i = 0; i < 5; i++)
			{
				if(luckynums[i] < 17)
					enchant += Math.pow(2, luckynums[i] - 1);
				else
					type2 += Math.pow(2, luckynums[i] - 17);
			}

			if(Config.DEBUG)
				_log.info("LotteryManager: Encoded lucky numbers are " + enchant + ", " + type2);

			int[] luckyCount = new int[4];
			Connection con = null;
			PreparedStatement statement = null;
			ResultSet rset = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement(SELECT_LOTTERY_ITEM);
				statement.setInt(1, getId());
				rset = statement.executeQuery();

				while(rset.next())
				{
					int curenchant = rset.getInt("enchant_level") & enchant;
					int curtype2 = rset.getInt("custom_type2") & type2;
					int count = 0;
					count = decodeTicketFormat(curenchant, curtype2);
					if(count == -1)
					{
						continue;
					}
					switch(count)
					{
						case 5:
							luckyCount[0]++;
							break;
						case 4:
							luckyCount[1]++;
							break;
						case 3:
							luckyCount[2]++;
							break;
						case 2:
						case 1:
							luckyCount[3]++;
							break;
					}
				}
				DbUtils.closeQuietly(statement, rset);
			}
			catch(SQLException e)
			{
				_log.warn("LotteryManager: Could restore lottery data: " + e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement, rset);
			}

			int[] prizeArray = new int[4];
			prizeArray[0] = 0;
			prizeArray[1] = 0;
			prizeArray[2] = 0;
			prizeArray[3] = luckyCount[3] * Config.ALT_LOTTERY_2_AND_1_NUMBER_PRIZE;

			if(luckyCount[0] > 0)
				prizeArray[0] = (int) ((getPrize() - prizeArray[3]) * Config.ALT_LOTTERY_5_NUMBER_RATE / luckyCount[0]);

			if(luckyCount[1] > 0)
				prizeArray[1] = (int) ((getPrize() - prizeArray[3]) * Config.ALT_LOTTERY_4_NUMBER_RATE / luckyCount[1]);

			if(luckyCount[2] > 0)
				prizeArray[2] = (int) ((getPrize() - prizeArray[3]) * Config.ALT_LOTTERY_3_NUMBER_RATE / luckyCount[2]);

			if(Config.DEBUG)
			{
				_log.info("LotteryManager: " + luckyCount[0] + " players with all FIVE numbers each win " + prizeArray[0] + ".");
				_log.info("LotteryManager: " + luckyCount[1] + " players with FOUR numbers each win " + prizeArray[1] + ".");
				_log.info("LotteryManager: " + luckyCount[2] + " players with THREE numbers each win " + prizeArray[2] + ".");
				_log.info("LotteryManager: " + luckyCount[3] + " players with ONE or TWO numbers each win " + prizeArray[3] + ".");
			}

			int newprize = getPrize() - (prizeArray[0] + prizeArray[1] + prizeArray[2] + prizeArray[3]);
			if(Config.DEBUG)
				_log.info("LotteryManager: Jackpot for next lottery is " + newprize + ".");

			SystemMessage sm;
			if(luckyCount[0] > 0)
			{
				// There are winners.
				sm = new SystemMessage(SystemMessage.THE_PRIZE_AMOUNT_FOR_THE_WINNER_OF_LOTTERY__S1__IS_S2_ADENA_WE_HAVE_S3_FIRST_PRIZE_WINNERS);
				sm.addNumber(getId());
				sm.addNumber(getPrize());
				sm.addNumber(luckyCount[0]);
				Announcements.getInstance().announceToAll(sm);
			}
			else
			{
				// There are no winners.
				sm = new SystemMessage(SystemMessage.THE_PRIZE_AMOUNT_FOR_LUCKY_LOTTERY__S1__IS_S2_ADENA_THERE_WAS_NO_FIRST_PRIZE_WINNER_IN_THIS_DRAWING_THEREFORE_THE_JACKPOT_WILL_BE_ADDED_TO_THE_NEXT_DRAWING);
				sm.addNumber(getId());
				sm.addNumber(getPrize());
				Announcements.getInstance().announceToAll(sm);
			}

			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement(UPDATE_LOTTERY);
				statement.setInt(1, getPrize());
				statement.setInt(2, newprize);
				statement.setInt(3, enchant);
				statement.setInt(4, type2);
				statement.setInt(5, prizeArray[0]);
				statement.setInt(6, prizeArray[1]);
				statement.setInt(7, prizeArray[2]);
				statement.setInt(8, getId());
				statement.execute();
				statement.close();
			}
			catch(SQLException e)
			{
				_log.warn("LotteryManager: Could not store finished lottery data: " + e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}

			ThreadPoolManager.getInstance().scheduleGeneral(new startLottery(), MINUTE);
			_number++;
			_isStarted = false;
		}
	}

	public int[] decodeNumbers(int enchant, int type2)
	{
		int res[] = new int[5];
		int id = 0;
		int nr = 1;
		while(enchant > 0)
		{
			int val = enchant / 2;
			if(val != (double) enchant / 2)
			{
				res[id++] = nr;
			}
			enchant /= 2;
			nr++;
		}
		nr = 17;
		while(type2 > 0)
		{
			int val = type2 / 2;
			if(val != (double) type2 / 2)
			{
				res[id++] = nr;
			}
			type2 /= 2;
			nr++;
		}
		return res;
	}

	private int decodeTicketFormat(int enchant, int type2)
	{
		if(enchant == 0 && type2 == 0){ return -1; }
		int count = 0;
		for(int i = 1; i <= 16; i++)
		{
			int val = enchant / 2;
			if(val != (double) enchant / 2)
			{
				count++;
			}
			int val2 = type2 / 2;
			if(val2 != (double) type2 / 2)
			{
				count++;
			}
			enchant = val;
			type2 = val2;
		}
		return count;
	}

	public int[] checkTicket(L2ItemInstance item)
	{
		return checkTicket(item.getCustomType1(), item.getEnchantLevel(), item.getCustomType2());
	}

	public int[] checkTicket(int id, int enchant, int type2)
	{
		int res[] = { 0, 0 };
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(SELECT_LOTTERY_TICKET);
			statement.setInt(1, id);
			rset = statement.executeQuery();

			if(rset.next())
			{
				int curenchant = rset.getInt("number1") & enchant;
				int curtype2 = rset.getInt("number2") & type2;
				int count = 0;
				count = decodeTicketFormat(curenchant, curtype2);
				if(count == -1)
				{
					DbUtils.closeQuietly(statement, rset);
					return res;
				}
				switch(count)
				{
					case 0:
						break;
					case 5:
						res[0] = 1;
						res[1] = rset.getInt("prize1");
						break;
					case 4:
						res[0] = 2;
						res[1] = rset.getInt("prize2");
						break;
					case 3:
						res[0] = 3;
						res[1] = rset.getInt("prize3");
						break;
					default:
						res[0] = 4;
						res[1] = 200;
				}

				if(Config.DEBUG)
					_log.warn("Count: " + count + ", id: " + id + ", enchant: " + enchant + ", type2: " + type2);
			}

			DbUtils.closeQuietly(statement, rset);
		}
		catch(SQLException e)
		{
			_log.warn("LotteryManager: Could not check lottery ticket #" + id + ": " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		return res;
	}

}
