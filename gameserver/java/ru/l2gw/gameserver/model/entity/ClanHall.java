package ru.l2gw.gameserver.model.entity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.instancemanager.AuctionManager;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.entity.siege.ClanHall.ClanHallSiege;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.PledgeShowInfoUpdate;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ClanTable;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

public class ClanHall extends SiegeUnit
{
	protected static Log _log = LogFactory.getLog("clanhall");

	private ClanHallSiege _siege = null;
	protected String siegeType;
	private ScheduledFuture<AutoTask> autoTask;
	private long lastPrice = 0;
	protected Calendar paidUntil = Calendar.getInstance();
	private Castle _castle;

	public ClanHall()
	{
		super();
	}

	private class AutoTask implements Runnable
	{
		public void run()
		{
			if(getOwnerId() != 0 && getLease() > 0)
				try
				{
					_log.info(ClanHall.this + ": start auto task");
					L2Clan clan = ClanTable.getInstance().getClan(getOwnerId());
					if(clan == null)
					{
						_log.warn(ClanHall.this + ": clan owner == null");
						return;
					}

					L2ItemInstance adena = clan.getWarehouse().getItemByItemId(57);

					if(getPaidUntil() > System.currentTimeMillis())
					{
						_log.info(ClanHall.this + ": next pay: " + new Date(getPaidUntil()));
						autoTask = ThreadPoolManager.getInstance().scheduleGeneral(new AutoTask(), getPaidUntil() - System.currentTimeMillis());
					}
					else
					{
						if(adena != null && adena.getCount() >= getLease())
						{
							clan.getWarehouse().destroyItemByItemId("ClanHallLease", 57, getLease(), null, null);
							updateRentTime(true);
							long castleTax = 0;
							if(_castle != null && _castle.getOwnerId() > 0)
							{
								castleTax = (long)(getLease() * _castle.getTaxRate()); 
								_castle.addToTreasury(castleTax, false, false, "CLANHALL");
							}
							_log.info(ClanHall.this + ": get lease: " + getLease() + " " + _castle + " tax: " + castleTax +", next pay: " + new Date(getPaidUntil()));
							autoTask = ThreadPoolManager.getInstance().scheduleGeneral(new AutoTask(), getPaidUntil() - System.currentTimeMillis());
						}
						else
						{
							updateRentTime(false);
							if(System.currentTimeMillis() >= getPaidUntil() + 604800000)
							{
								_log.info(ClanHall.this + ": release, last pay: " + new Date(getPaidUntil()) + " lease: " + getLease() + " change owner to NPC");
								clan.broadcastToOnlineMembers(new SystemMessage(SystemMessage.THE_CLAN_HALL_FEE_IS_ONE_WEEK_OVERDUE_THEREFORE_THE_CLAN_HALL_OWNERSHIP_HAS_BEEN_REVOKED));
								changeOwner(0);
								ThreadPoolManager.getInstance().scheduleGeneral(new Runnable(){ public void run(){ AuctionManager.getInstance().addToAuction(ClanHall.this); } }, 2000);
							}
							else
							{
								SystemMessage sm = new SystemMessage(SystemMessage.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW);
								sm.addNumber(getLease());
								clan.broadcastToOnlineMembers(sm);
								long currTime = System.currentTimeMillis();
								long nextPay = getPaidUntil();

								while(nextPay < currTime)
									nextPay += 86400000;

								_log.info(ClanHall.this + ": no adena: " + getLease() + " block functions, next pay: " + new Date(nextPay));
								autoTask = ThreadPoolManager.getInstance().scheduleGeneral(new AutoTask(), nextPay - System.currentTimeMillis());
							}
						}
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
		}
	}

	public void startAutoTask()
	{
		if(autoTask != null)
			autoTask.cancel(false);
		autoTask= null;
		autoTask = ThreadPoolManager.getInstance().scheduleGeneral(new AutoTask(), 100);
		if(_castle == null)
			for(Castle castle : ResidenceManager.getInstance().getCastleList())
				if(castle.getName().equalsIgnoreCase(getLocation()))
				{
					_castle = castle;
					break;
				}
	}

	/** Return true if object is inside the zone */
	public boolean checkIfInZone(L2Object obj)
	{
		return checkIfInZone(obj.getX(), obj.getY());
	}

	/** Return true if object is inside the zone */
	public boolean checkIfInZone(int x, int y)
	{
		return getZone().isInsideZone(x, y);
	}

	@Override
	public void changeOwner(int clanId)
	{
		if(getZone() != null)
			getZone().setActive(false);
		else
			_log.warn(this + ": has no zone defined!");
		// Remove old owner
		if(getOwnerId() > 0 && (clanId == 0 || clanId != getOwnerId()))
		{
			L2Clan oldOwner = ClanTable.getInstance().getClan(getOwnerId()); // Try to find clan instance
			if(oldOwner != null)
			{
				oldOwner.setHasHideout(0); // Unset has hideout flag for old owner
				oldOwner.broadcastToOnlineMembers(new PledgeShowInfoUpdate(oldOwner));
			}
			_log.info(this + ": change owner to: " + clanId);
		}
		// Update in database
		updateOwnerInDB(clanId);

		banishForeigner();
		stopFunctions();

		if(clanId != 0)
		{
			getPaidUntilCalendar().setTimeInMillis(System.currentTimeMillis());
			updateRentTime(false);
			getZone().setActive(true);
			startAutoTask();
		}
		else
			reversValues();
	}

	private void updateOwnerInDB(int clanId)
	{
		if(clanId != 0)
			setOwnerId(clanId); // Update owner id property
		else
			setOwnerId(0); // Remove owner

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET hasHideout=0 WHERE hasHideout=?");
			statement.setInt(1, getId());
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("UPDATE clan_data SET hasHideout=? WHERE clan_id=?");
			statement.setInt(1, getId());
			statement.setInt(2, getOwnerId());
			statement.execute();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("DELETE FROM clanhall_functions WHERE hall_id=?");
			statement.setInt(1, getId());
			statement.execute();
			DbUtils.closeQuietly(statement);

			// Announce to clan memebers
			if(clanId != 0)
			{
				L2Clan clan = ClanTable.getInstance().getClan(clanId);
				if(clan == null)
					_log.warn(this + ": cannot set owner: " + clan);
				else
				{
					clan.setHasHideout(getId()); // Set has hideout flag for new owner
					clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con);
		}
	}

	public void updateRentTime(boolean setNext)
	{
		while(setNext && paidUntil.getTimeInMillis() < System.currentTimeMillis())
			paidUntil.add(Calendar.MILLISECOND, 604800000);

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE residence SET paidUntil=? WHERE id=?");
			statement.setLong(1, getPaidUntil());
			statement.setInt(2, getId());
			statement.executeUpdate();
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

	@Override
	public ClanHallSiege getSiege()
	{
		Constructor<?> ClanHallSConstructor = null;

		if(_siege == null && isSieaged())
		{
			try
			{
				ClanHallSConstructor = Class.forName("ru.l2gw.gameserver.model.entity.siege.ClanHall." + siegeType).getConstructors()[0];
			}
			catch(ClassNotFoundException e)
			{
				e.printStackTrace();
				_log.warn(this + ": cannot find siege");
			}

			if(ClanHallSConstructor != null)
			{
				try
				{
					setSiege((ClanHallSiege) ClanHallSConstructor.newInstance(this));
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		return _siege;
	}

	//убил хард код определяем по дп,тока надо саму систему осады кх доперенести:)
	public final boolean isSieaged()
	{
		return getPrice() == 0;
	}

	private void reversValues()
	{
		if(autoTask != null)
		{
			autoTask.cancel(false);
			autoTask = null;
		}
		getPaidUntilCalendar().setTimeInMillis(0);
		updateRentTime(false);
	}

	public void setSiege(ClanHallSiege s)
	{
		_siege = s;
	}

	@Override
	public void setSiegeType(String type)
	{
		siegeType = type;
	}

	@Override
	public String toString()
	{
		return "ClanHall[id=" + getId() + ";name=" + getName() + ";owner=" + getOwner() + "]";
	}

	@Override
	public int getMinLeftForTax()
	{
		return 0;
	}

	public Map<Integer, L2Zone> getTrapZones()
	{
		return _trapZones;
	}

	public long getLastPrice()
	{
		return lastPrice;
	}

	public void setLastPrice(long lastPrice)
	{
		this.lastPrice = lastPrice;
	}

	public final long getPaidUntil()
	{
		return paidUntil.getTimeInMillis();
	}

	public final Calendar getPaidUntilCalendar()
	{
		return paidUntil;
	}

	public boolean isPaid()
	{
		return getLease() == 0 || paidUntil.getTimeInMillis() > System.currentTimeMillis();
	}

	public void updateLastPrice()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE residence SET last_price = ? WHERE id = ?");
			statement.setLong(1, lastPrice);
			statement.setInt(2, getId());
			statement.execute();
		}
		catch(Exception e)
		{
			System.out.println("Exception: updateLastPrice(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

}
