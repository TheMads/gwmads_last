package ru.l2gw.loginserver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.network.utils.BannedIp;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class IpManager
{
	private static final Log _log = LogFactory.getLog(IpManager.class.getName());
	private static final IpManager _instance = new IpManager();

	public static IpManager getInstance()
	{
		return _instance;
	}

	public IpManager()
	{}

	public String banLastIp(String account, String admin, String comment)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		String ip = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM accounts WHERE login = ?");
			statement.setString(1, account);
			rset = statement.executeQuery();
			if(rset.next())
				ip = rset.getString("lastIP");

		}
		catch(Exception e)
		{
			_log.info("error while reading last ip");
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		BanIp(ip, admin, 0, comment);
		return ip;
	}

	public void BanIp(String ip, String admin, int time, String comments)
	{
		if(CheckIp(ip))
			return;
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			long expiretime = 0;
			if(time != 0)
				expiretime = System.currentTimeMillis() / 1000 + time;
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO banned_ips (ip,admin,expiretime,comments) values(?,?,?,?)");
			statement.setString(1, ip);
			statement.setString(2, admin);
			statement.setLong(3, expiretime);
			statement.setString(4, comments);
			statement.execute();
			_log.warn("Banning ip: " + ip + " for " + time + " seconds.");
		}
		catch(Exception e)
		{
			_log.info("error while reading banned_ips");
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void UnbanIp(String ip)
	{
		//		who`s care exist ban or not? ;)
		//		if(!CheckIp(ip))
		//			return;
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM banned_ips WHERE ip=?");
			statement.setString(1, ip);
			statement.execute();
			_log.warn("Removed ban for ip: " + ip);
		}
		catch(Exception e)
		{
			_log.info("error while reading banned_ips");
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public boolean CheckIp(String ip)
	{
		boolean result = false;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT expiretime FROM banned_ips WHERE ip=?");
			statement.setString(1, ip);
			rset = statement.executeQuery();
			if(rset.next())
			{
				long expiretime = rset.getLong("expiretime");
				if(expiretime != 0 && expiretime <= System.currentTimeMillis() / 1000)
					UnbanIp(ip);
				else
					result = true;
			}
		}
		catch(Exception e)
		{
			_log.info("error while reading banned_ips");
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return result;
	}

	public int getBannedCount()
	{
		int result = 0;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT count(*) AS num FROM banned_ips");
			rset = statement.executeQuery();
			if(rset.next())
				result = rset.getInt("num");
		}
		catch(Exception e)
		{
			_log.info("error while reading banned_ips");
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return result;
	}

	public GArray<BannedIp> getBanList()
	{
		GArray<BannedIp> result = new GArray<>();
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			BannedIp temp;
			statement = con.prepareStatement("SELECT ip,admin FROM banned_ips");
			rset = statement.executeQuery();
			while(rset.next())
			{
				temp = new BannedIp();
				temp.ip = rset.getString("ip");
				temp.admin = rset.getString("admin");
				result.add(temp);
			}
		}
		catch(Exception e)
		{
			_log.info("error while reading banned_ips");
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return result;
	}
}