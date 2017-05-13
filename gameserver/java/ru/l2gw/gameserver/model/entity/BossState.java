package ru.l2gw.gameserver.model.entity;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import ru.l2gw.database.DatabaseFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;

public class BossState
{
	public static enum State
	{
		NOTSPAWN,
		ALIVE,
		DEAD
	}

	private int _bossId;
	private long _respawnDate;
	private State _state;

	private static final Log _log = LogFactory.getLog(BossState.class.getName());

	public int getBossId()
	{
		return _bossId;
	}

	public void setBossId(int newId)
	{
		_bossId = newId;
	}

	public State getState()
	{
		return _state;
	}

	public void setState(State newState)
	{
		_state = newState;
	}

	public long getRespawnDate()
	{
		return _respawnDate;
	}

	public void setRespawnDate(long interval)
	{
		_respawnDate = interval + System.currentTimeMillis();
	}

	public BossState(int bossId)
	{
		_bossId = bossId;
		load();
	}

	public BossState(int bossId, boolean doLoad)
	{
		_bossId = bossId;
		if(doLoad)
			load();
	}

	public void load()
	{

		Connection con = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			PreparedStatement statement = con.prepareStatement("SELECT * FROM boss_state WHERE bossId = ?");
			statement.setInt(1, _bossId);
			ResultSet rset = statement.executeQuery();

			while(rset.next())
			{
				_respawnDate = rset.getLong("respawnDate");

				if(_respawnDate - System.currentTimeMillis() <= 0)
					_state = State.NOTSPAWN;
				else
					_state = State.valueOf(rset.getString("state"));
			}
			rset.close();
			statement.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
			}
		}
	}

	public void save()
	{
		Connection con = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("REPLACE INTO boss_state(bossId,respawnDate,state) VALUES(?,?,?)");
			statement.setInt(1, _bossId);
			statement.setLong(2, _respawnDate);
			statement.setString(3, _state.toString());
			statement.execute();
			statement.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
			}
		}
	}

	public void update()
	{
		Connection con = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("UPDATE boss_state SET respawnDate = ?,state = ? WHERE bossId = ?");
			statement.setLong(1, _respawnDate);
			statement.setString(2, _state.toString());
			statement.setInt(3, _bossId);
			statement.execute();
			statement.close();
			SimpleDateFormat format = new SimpleDateFormat("[dd.MM.yyyy HH:mm]");
			_log.info("update BossState : ID-" + _bossId + ",RespawnDate-" + format.format(_respawnDate) + ",State-" + _state.toString());
		}
		catch(Exception e)
		{
			_log.warn("Exeption on update BossState : ID-" + _bossId + ",RespawnDate-" + _respawnDate + ",State-" + _state.toString());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
			}
		}
	}

	public void setNextRespawnDate(long newRespawnDate)
	{
		_respawnDate = newRespawnDate;
	}

	public long getInterval()
	{
		long interval = _respawnDate - System.currentTimeMillis();

		if(interval < 0)
			return 0;
		else
			return interval;
	}
}