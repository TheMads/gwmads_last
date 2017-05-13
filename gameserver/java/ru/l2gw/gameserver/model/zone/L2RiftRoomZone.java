package ru.l2gw.gameserver.model.zone;

import ru.l2gw.gameserver.instancemanager.DimensionalRiftManager;
import ru.l2gw.util.Location;

import java.util.StringTokenizer;

/**
 * @author rage
 * @date 03.09.2009 11:13:58
 */
public class L2RiftRoomZone extends L2DefaultZone
{
	private boolean _isBossRoom = false;
	private byte _roomLevel;
	private byte _roomId;
	private Location _teleportLoc;

	@Override
	public void setAttribute(String name, String value)
	{
		if(name.equalsIgnoreCase("bossRoom"))
			_isBossRoom = "true".equalsIgnoreCase(value);
		else if(name.equalsIgnoreCase("roomLevel"))
			_roomLevel = Byte.parseByte(value);
		else if(name.equalsIgnoreCase("roomId"))
			_roomId = Byte.parseByte(value);
		else if(name.equalsIgnoreCase("teleportLoc"))
		{
			StringTokenizer st = new StringTokenizer(value, ",");
			if(st.countTokens() < 3)
				_log.warn(this + ": has wrong telportLoc arrribute: " + value);
			else
			{
				try
				{
				_teleportLoc = new Location(Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()));
				}
				catch(Exception e)
				{
					_log.warn(this + ": can't parse teleportLoc: " + value + " " + e);
				}
			}
		}
		else
			super.setAttribute(name, value);
	}

	public boolean isBossRoom()
	{
		return _isBossRoom;
	}

	public byte getRoomType()
	{
		return _roomLevel;
	}

	public byte getRoomId()
	{
		return _roomId;
	}

	public Location getTeleportLocation()
	{
		return _teleportLoc;
	}

	@Override
	public void register()
	{
		DimensionalRiftManager.getInstance().addZone(this);
	}
}
