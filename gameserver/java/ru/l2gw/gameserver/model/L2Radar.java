package ru.l2gw.gameserver.model;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.serverpackets.RadarControl;
import ru.l2gw.util.Location;

public final class L2Radar
{
	private L2Player player;
	private GArray<RadarMarker> markers;

	public L2Radar(L2Player player)
	{
		this.player = player;
		markers = new GArray<RadarMarker>();
	}

	// Add a marker to player's radar
	public void showRadar(int x, int y, int z, int type)
	{
		RadarMarker newMarker = new RadarMarker(x, y, z, type);
		markers.add(newMarker);
		player.sendPacket(new RadarControl(0, type, newMarker));
	}

	// Remove a marker from player's radar
	public void removeMarker(int x, int y, int z, int type)
	{
		for(int i = 0; i < markers.size(); i++)
		{
			RadarMarker rm = markers.get(i);
			if(rm.getX() == x && rm.getY() == y && rm.getZ() == z && rm.type == type)
			{
				player.sendPacket(new RadarControl(1, type, rm));
				markers.remove(i);
			}
		}
	}

	public void deleteAll(int type)
	{
		for(int i = 0; i < markers.size(); i++)
		{
			RadarMarker rm = markers.get(i);
			if(rm.type == type)
			{
				player.sendPacket(new RadarControl(1, type, rm));
				markers.remove(i);
			}
		}
	}

	@SuppressWarnings("serial")
	public class RadarMarker extends Location
	{
		// Simple class to model radar points.
		public int type;

		public RadarMarker(int x, int y, int z, int type)
		{
			super(x, y, z);
			this.type = type;
		}

		public RadarMarker(int x, int y, int z)
		{
			super(x, y, z);
			type = 1;
		}

		@Override
		public boolean equals(Object obj)
		{
			try
			{
				RadarMarker temp = (RadarMarker) obj;
				return temp.getX() == getX() && temp.getY() == getY() && temp.getZ() == getZ() && temp.type == type;
			}
			catch(Exception e)
			{
				return false;
			}
		}
	}
}