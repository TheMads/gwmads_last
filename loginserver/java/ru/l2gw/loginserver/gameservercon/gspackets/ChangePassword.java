package ru.l2gw.loginserver.gameservercon.gspackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.Base64;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.loginserver.gameservercon.AttGS;
import ru.l2gw.loginserver.gameservercon.lspackets.ChangePasswordResponse;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @Author: Death
 * @Date: 8/2/2007
 * @Time: 15:33:15
 */
public class ChangePassword extends ClientBasePacket
{
	private static final Log log = LogFactory.getLog(ChangePassword.class.getName());

	public ChangePassword(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		String accname = readS();
		String oldPass = readS();
		String newPass = readS();

		String dbPassword = null;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			try
			{
				statement = con.prepareStatement("SELECT password FROM accounts WHERE login = ?");
				statement.setString(1, accname);
				rs = statement.executeQuery();
				if(rs.next())
					dbPassword = rs.getString("password");
			}
			catch(Exception e)
			{
				log.warn("Can't recive old password for account " + accname + ", exciption :" + e);
			}
			finally
			{
				DbUtils.closeQuietly(statement, rs);
			}

			//Encode old password and compare it to sended one, send packet to determine changed or not.
			try
			{
				MessageDigest md = MessageDigest.getInstance("SHA");
				byte[] op = oldPass.getBytes("UTF-8");
				op = md.digest(op);
				String oldP = Base64.encodeBytes(op);

				if(!oldP.equals(dbPassword))
				{
					ChangePasswordResponse cp1;
					cp1 = new ChangePasswordResponse(accname, false);
					sendPacket(cp1);
				}
				else
				{
					byte[] np = newPass.getBytes("UTF-8");
					np = md.digest(np);

					statement = con.prepareStatement("UPDATE accounts SET password = ? WHERE login = ?");
					statement.setString(1, Base64.encodeBytes(np));
					statement.setString(2, accname);
					statement.executeUpdate();

					ChangePasswordResponse cp1;
					cp1 = new ChangePasswordResponse(accname, true);
					sendPacket(cp1);
				}
			}
			catch(Exception e1)
			{
				e1.printStackTrace();
			}
			finally
			{
				DbUtils.closeQuietly(statement);
			}
		}
		catch(Exception e)
		{
			log.warn(e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con);
		}
	}
}
