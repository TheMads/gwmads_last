package ru.l2gw.gameserver.model.entity.siege;

import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2Clan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public abstract class SiegeDatabase
{
	protected Siege _siege;

	public SiegeDatabase(Siege siege)
	{
		_siege = siege;
	}

	public abstract void saveSiegeDate();

	public void saveLastSiegeDate()
	{
	}

	/**
	 * Return true if the clan is registered or owner of a castle<BR><BR>
	 *
	 * @param clan The L2Clan of the player
	 */
	public static boolean checkIsRegistered(int clanId, Siege siege)
	{
		int unitid = 0;
		if(siege != null)
		{
			unitid = siege.getSiegeUnit().getId();
		}

		if(clanId <= 0)
			return false;

		if(siege != null && siege.getSiegeUnit().getOwnerId() == clanId)
			return true;

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		boolean register = false;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT clan_id FROM siege_clans where clan_id=?" + (unitid == 0 ? "" : " and unit_id=?"));
			statement.setInt(1, clanId);
			if(unitid != 0)
				statement.setInt(2, unitid);
			rset = statement.executeQuery();
			if(rset.next())
				register = true;
		}
		catch(Exception e)
		{
			System.out.println("Exception: checkIsRegistered(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		return register;
	}

	public static void clearSiegeClan(Siege siege)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM siege_clans WHERE unit_id=?");
			statement.setInt(1, siege.getSiegeUnit().getId());
			statement.execute();
			DbUtils.closeQuietly(statement);
			if(siege.getSiegeUnit().getOwnerId() > 0 && siege.getSiegeUnit().isCastle)
			{
				statement = con.prepareStatement("DELETE FROM siege_clans WHERE clan_id=?");
				statement.setInt(1, siege.getSiegeUnit().getOwnerId());
				statement.execute();
			}

			siege.getAttackerClans().clear();
			siege.getDefenderClans().clear();
			siege.getDefenderWaitingClans().clear();
		}
		catch(Exception e)
		{
			System.out.println("Exception: clearSiegeClan(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void clearSiegeWaitingClan()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM siege_clans WHERE unit_id=? and type = 2");
			statement.setInt(1, _siege.getSiegeUnit().getId());
			statement.execute();

			_siege.getDefenderWaitingClans().clear();
		}
		catch(Exception e)
		{
			System.out.println("Exception: clearSiegeWaitingClan(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public static void removeSiegeClan(int clanId, Siege siege)
	{
		if(clanId <= 0)
			return;
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM siege_clans WHERE unit_id=? and clan_id=?");
			statement.setInt(1, siege.getSiegeUnit().getId());
			statement.setInt(2, clanId);
			statement.execute();
			siege.reloadRegistredMembers();
		}
		catch(Exception e)
		{
			System.out.println("Exception: removeSiegeClan(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public static void loadSiegeClan(Siege siege)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			siege.getAttackerClans().clear();
			siege.getDefenderClans().clear();
			siege.getDefenderWaitingClans().clear();
			// Add castle owner as defender
			if(siege.getSiegeUnit().getOwnerId() > 0)
				siege.addDefender(new SiegeClan(siege.getSiegeUnit().getOwnerId(), SiegeClanType.OWNER));
			con = DatabaseFactory.getInstance().getConnection();
/*
			statement = con.prepareStatement("DELETE FROM siege_clans WHERE unit_id=? and UNIX_TIMESTAMP() - 60 > (SELECT siegeDate FROM residence WHERE id=?)");
			statement.setInt(1, siege.getSiegeUnit().getId());
			statement.setInt(2, siege.getSiegeUnit().getId());
			statement.execute();
			statement.close();
*/
			statement = con.prepareStatement("SELECT clan_id, type FROM siege_clans WHERE unit_id = ?");
			statement.setInt(1, siege.getSiegeUnit().getId());

			rset = statement.executeQuery();
			int typeId;
			while(rset.next())
			{
				typeId = rset.getInt("type");
				if(typeId == 0)
					siege.addDefender(new SiegeClan(rset.getInt("clan_id"), SiegeClanType.DEFENDER));
				else if(typeId == 1)
					siege.addAttacker(new SiegeClan(rset.getInt("clan_id"), SiegeClanType.ATTACKER));
				else if(typeId == 2)
					siege.addDefenderWaiting(new SiegeClan(rset.getInt("clan_id"), SiegeClanType.DEFENDER_PENDING));
			}
		}
		catch(Exception e)
		{
			System.out.println("Exception: loadSiegeClan(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public void saveSiegeClan(L2Clan clan, int typeId, @SuppressWarnings("unused") boolean isUpdateRegistration)
	{
		if(clan == null || (clan.getHasUnit(2) && _siege.getSiegeUnit().isCastle))
			return;

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("REPLACE INTO siege_clans (clan_id,unit_id,type,castle_owner) values (?,?,?,0)");
			statement.setInt(1, clan.getClanId());
			statement.setInt(2, _siege.getSiegeUnit().getId());
			statement.setInt(3, typeId);
			statement.execute();

			if(typeId == 0 || typeId == -1)
				_siege.addDefender(new SiegeClan(clan.getClanId(), SiegeClanType.DEFENDER));
			else if(typeId == 1)
				_siege.addAttacker(new SiegeClan(clan.getClanId(), SiegeClanType.ATTACKER));
			else if(typeId == 2)
				_siege.addDefenderWaiting(new SiegeClan(clan.getClanId(), SiegeClanType.DEFENDER_PENDING));
		}
		catch(Exception e)
		{
			System.out.println("Exception: saveSiegeClan: " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	/**
	 * Only For ClanHall RolePlay Siege
	 */
	public void saveSettings(int clanId, int memberId, int npcValue)
	{
	}
}