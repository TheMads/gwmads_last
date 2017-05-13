package ru.l2gw.extensions.ccpGuard.managers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.extensions.ccpGuard.ProtectInfo;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.network.GameClient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class HwidManager
{

	private static HwidManager _instance;
	private static final Log _log = LogFactory.getLog("hwid");

	private static GArray<HwidInfo> _listHWID;

	public static HwidManager getInstance()
	{
		if(_instance == null)
			_instance = new HwidManager();
		return _instance;
	}

	public HwidManager()
	{
		_listHWID = new GArray<HwidInfo>();
		load();
		_log.info("Manager: Loaded: " + _listHWID.size() + " HWIDs");
	}

	private void load()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM hwid_info");
			rset = statement.executeQuery();
			while(rset.next())
			{
				final HwidInfo hInfo = new HwidInfo(rset.getString("HWID"));
				hInfo.setCount(rset.getInt("WindowsCount"));
				hInfo.setLogin(rset.getString("Account"));
				hInfo.setPlayerID(rset.getInt("PlayerID"));
				hInfo.setLockType(HwidInfo.LockType.valueOf(rset.getString("LockType")));
				_listHWID.add(hInfo);
			}
		}
		catch(final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public static void reload()
	{
		_instance = new HwidManager();
	}

	public static boolean checkLockedHWID(final ProtectInfo pi)
	{
		if(_listHWID.size() == 0)
			return false;
		boolean result = false;
		for(HwidInfo hw : _listHWID)
		{
			switch(hw.getLockType())
			{
				case NONE:
					break;
				case PLAYER_LOCK:
					if(pi.getPlayerId()!=0 && hw.getPlayerID() == pi.getPlayerId())
					{
						if(hw.getHWID().equals(pi.getHWID()))
							return false;
						else
							result = true;
					}
					break;
				case ACCOUNT_LOCK:
					if(hw.getLogin().equals(pi.getLoginName()))
					{
						if(hw.getHWID().equals(pi.getHWID()))
							return false;
						else
							result = true;
					}
					break;
				default:
					break;
			}
		}
		return result;
	}

	public static int getAllowedWindowsCount(final ProtectInfo pi)
	{
		if(_listHWID.size() == 0)
			return -1;
		for(HwidInfo hw : _listHWID)
			if(hw.getHWID().equals(pi.getHWID()))
			{
				if(hw.getHWID().equals(""))
					return -1;
				else
					return hw.getCount();
			}
		return -1;
	}

	public static int getCountHwidInfo()
	{
		return _listHWID.size();
	}

	public static void updateHwidInfo(final L2Player player, final HwidInfo.LockType lockType)
	{
		updateHwidInfo(player, 1, lockType);
	}

	public static void updateHwidInfo(final L2Player player, final int windowscount)
	{
		updateHwidInfo(player, windowscount, HwidInfo.LockType.NONE);
	}

	public static void updateHwidInfo(final L2Player player, final int windowsCount, final HwidInfo.LockType lockType)
	{
		final GameClient client = player.getNetConnection();
		HwidInfo hw = null;
		for(int i = 0; i < _listHWID.size(); i++)
			if(_listHWID.get(i).getHWID().equals(client._prot_info.getHWID()))
			{
				hw = _listHWID.get(i);
				break;
			}

		if(hw == null)
		{
			hw = new HwidInfo(client._prot_info.getHWID());
			_listHWID.add(hw);
		}

		hw.setCount(windowsCount);
		hw.setLogin(client.getLoginName());
		hw.setPlayerID(player.getObjectId());
		hw.setLockType(lockType);

		Connection con = null;
		PreparedStatement statement = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("REPLACE INTO hwid_info (HWID, WindowsCount, Account, PlayerID, LockType) values (?,?,?,?,?)");
			statement.setString(1, client._prot_info.getHWID());
			statement.setInt(2, windowsCount);
			statement.setString(3, client.getLoginName());
			statement.setInt(4, player.getObjectId());
			statement.setString(5, lockType.toString());
			statement.execute();
			DbUtils.closeQuietly(statement);
		}
		catch(final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
}
