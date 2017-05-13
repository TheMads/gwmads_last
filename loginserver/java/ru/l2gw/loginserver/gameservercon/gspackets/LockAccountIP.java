package ru.l2gw.loginserver.gameservercon.gspackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.loginserver.gameservercon.AttGS;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * @Author: SYS
 * @Date: 10/4/2007
 */
public class LockAccountIP extends ClientBasePacket
{
	private static final Log _log = LogFactory.getLog(LockAccountIP.class.getName());

	public LockAccountIP(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		String accname = readS();
		String IP = readS();

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE accounts SET AllowIPs = ? WHERE login = ?");
			statement.setString(1, IP);
			statement.setString(2, accname);
			statement.executeUpdate();
			DbUtils.closeQuietly(statement);
		}
		catch(Exception e)
		{
			_log.warn("Failed to lock/unlock account: " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con);
		}
	}
}