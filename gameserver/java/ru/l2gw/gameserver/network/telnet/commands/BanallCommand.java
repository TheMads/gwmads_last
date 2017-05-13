package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.commons.utils.StringUtil;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.extensions.ccpGuard.managers.HwidBan;
import ru.l2gw.gameserver.loginservercon.LSConnection;
import ru.l2gw.gameserver.loginservercon.gspackets.BanIP;
import ru.l2gw.gameserver.loginservercon.gspackets.ChangeAccessLevel;
import ru.l2gw.gameserver.loginservercon.gspackets.SendBanLastIp;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author: rage
 * @date: 03.03.12 23:49
 */
public class BanallCommand extends TelnetCommand
{
	public BanallCommand()
	{
		super("banall");
	}

	@Override
	public String getUsage()
	{
		return "banall <name> or <objectId>";
	}

	@Override
	public String handle(String[] args, String requestIp)
	{
		if(!checkArgs(1, args))
			return null;
		
		StringBuilder sb = new StringBuilder();

		try
		{
			Connection con;
			PreparedStatement statement;
			ResultSet rset;
			
			String comment = StringUtil.joinStrings(" ", args, 1);
			if(comment.isEmpty())
				comment = "no comment";

			comment += " [telnet]";

			L2Player player = L2ObjectsStorage.getPlayer(args[0]);
			if(player == null)
			{
				try
				{
					player = L2ObjectsStorage.getPlayer(Integer.parseInt(args[0]));
				}
				catch(Exception e)
				{
					// quite
				}
			}	

			String account = null, hwid = null, ip = null;

			if(player != null)
			{
				account = player.getAccountName();
				hwid = player.getLastHWID();
				ip = player.getNetConnection().getIpAddr();
				player.logout(true, false, true);
			}
			else
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("SELECT * FROM characters WHERE char_name = ? or obj_id = ?");
				statement.setString(1, args[0]);
				statement.setString(2, args[0]);
				rset = statement.executeQuery();
				if(rset.next())
				{
					account = rset.getString("account_name");
					hwid = rset.getString("LastHWID");
				}

				DbUtils.closeQuietly(con, statement, rset);
			}
			
			if(account != null)
			{
				sb.append("Banned: ").append(args[0]).append("\n");
				LSConnection.getInstance().sendPacket(new ChangeAccessLevel(account, -100, "Ban all from console.", -1));
				sb.append("Account: ").append(account).append("\n");

				if(ip != null)
				{
					LSConnection.getInstance().sendPacket(new BanIP(ip, "Ban all from console."));
					sb.append("IP: ").append(ip).append("\n");
				}
				else
				{
					LSConnection.getInstance().sendPacket(new SendBanLastIp(account, "console", "Ban all from console."));
					sb.append("IP: last\n");
				}

				if(hwid != null && !hwid.isEmpty())
				{
					HwidBan.addHwidBan(hwid, comment);
					sb.append("HWID: ").append(hwid).append("\n");

					con = DatabaseFactory.getInstance().getConnection();
					statement = con.prepareStatement("SELECT * FROM characters WHERE LastHWID = ? and account_name <> ?");
					statement.setString(1, hwid);
					statement.setString(2, account);
					rset = statement.executeQuery();
					while(rset.next())
					{
						String name = rset.getString("char_name");
						sb.append("Banned: ").append(name).append("\n");
						L2Player pl = L2ObjectsStorage.getPlayer(name);
						if(pl != null)
							pl.logout(true, false, true);
						account = rset.getString("account_name");
						LSConnection.getInstance().sendPacket(new ChangeAccessLevel(account, -100, "Ban all from console.", -1));
						sb.append("Account: ").append(account).append("\n");
					}

					DbUtils.closeQuietly(con, statement, rset);
				}
			}
			else
				sb.append("Player: ").append(args[0]).append(" not found.\n");
		}
		catch(Exception e)
		{
			sb.append("Error: ").append(e);
			e.printStackTrace();
		}

		return sb.toString();
	}
}
