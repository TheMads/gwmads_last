package ru.l2gw.gameserver.model;

import java.util.ArrayList;

public class L2AutoAnnounce
{
	private final int _id;
	private ArrayList<String> _msg;
	private int _repeat;
	private long _nextSend;
	private boolean _isOnScreen;

	public L2AutoAnnounce(int id)
	{
		_id = id;
	}

	public int getId()
	{
		return _id;
	}
	
	public void setScreenAnnounce(boolean arg)
	{
	    _isOnScreen = arg;
	}
	
	public boolean isScreenAnnounce()
	{
	    return _isOnScreen;
	}

	public void setAnnounce(int delay, int repeat, ArrayList<String> msg)
	{
		_nextSend = System.currentTimeMillis() + delay * 1000;
		_repeat = repeat;
		_msg = msg;
	}

	public void updateRepeat()
	{
		_nextSend = System.currentTimeMillis() + _repeat * 1000;
	}

	public boolean canAnnounce()
	{
		return System.currentTimeMillis() > _nextSend;
	}

	public ArrayList<String> getMessage()
	{
		return _msg;
	}
}