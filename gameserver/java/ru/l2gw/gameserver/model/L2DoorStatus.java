package ru.l2gw.gameserver.model;

import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.database.mysql;
import ru.l2gw.gameserver.tables.DoorTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class L2DoorStatus
{
	private final int _id;
	private ArrayList<Integer> _doors;
	private int _opendelay;
	private int _closedelay;
	private long _nexttimer;
	private boolean _isOpen;
	private String _openEvent;
	private String _closeEvent;

	public L2DoorStatus(int id)
	{
		_id = id;
	}

	public int getId()
	{
		return _id;
	}

	public void addDoorStatus(int id, ArrayList<Integer> doors, int opendelay, int closedelay, String onstart, String openEvent, String closeEvent)
	{
		_isOpen = false;
		_nexttimer = 0;
		_openEvent = openEvent;
		_closeEvent = closeEvent;
		if(onstart.equalsIgnoreCase("open"))
			_isOpen = true;
		else if(onstart.equalsIgnoreCase("laststate"))
		{
			Connection con = null;
			PreparedStatement statement = null;
			ResultSet rset = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("SELECT * FROM doors_status WHERE `id`=" + id);
				rset = statement.executeQuery();
				while(rset.next())
				{
					_isOpen = rset.getBoolean("isOpen");
					_nexttimer = rset.getLong("nexttimer");
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				DbUtils.closeQuietly(con, statement, rset);
			}
		}
		_opendelay = opendelay;
		_closedelay = closedelay;
		_doors = doors;
		if(_isOpen && _opendelay > 0 && System.currentTimeMillis() > _nexttimer)
		{
			_isOpen = false;
			_nexttimer = System.currentTimeMillis() + _closedelay * 1000;
		}
		else if(!_isOpen && _closedelay > 0 && System.currentTimeMillis() > _nexttimer)
		{
			_isOpen = true;
			_nexttimer = System.currentTimeMillis() + _opendelay * 1000;
		}
	}

	public void changeStatus()
	{
		if(_isOpen && _opendelay > 0)
		{
			_isOpen = false;
			_nexttimer = System.currentTimeMillis() + _closedelay * 1000;
		}
		else if(!_isOpen && _closedelay > 0)
		{
			_isOpen = true;
			_nexttimer = System.currentTimeMillis() + _opendelay * 1000;
		}
		mysql.set("REPLACE INTO `doors_status` (`id`, `nexttimer`, `isopen`) VALUES (" + _id + ", " + _nexttimer + ", " + _isOpen + ")");
	}

	public boolean isOpen()
	{
		return _isOpen;
	}

	public long getNextTimer()
	{
		return _nexttimer;
	}

	public ArrayList<Integer> getDoors()
	{
		return _doors;
	}

	public void notifyEvent(String event)
	{
		if(_openEvent != null && _openEvent.equalsIgnoreCase(event))
		{
			for(Integer doorId : _doors)
			{
				if(DoorTable.getInstance().getDoor(doorId) != null)
				{
					DoorTable.getInstance().getDoor(doorId).openMe();
					if(_closeEvent == null)
						DoorTable.getInstance().getDoor(doorId).onOpen();
				}
			}
			_isOpen = true;
		}
		else if(_closeEvent != null && _closeEvent.equalsIgnoreCase(event))
		{
			for(Integer doorId : _doors)
			{
				if(DoorTable.getInstance().getDoor(doorId) != null)
					DoorTable.getInstance().getDoor(doorId).closeMe();
			}
			_isOpen = false;
		}
	}
}