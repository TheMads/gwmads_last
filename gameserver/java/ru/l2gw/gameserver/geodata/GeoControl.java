package ru.l2gw.gameserver.geodata;

import ru.l2gw.gameserver.model.L2Territory;

import java.util.HashMap;

public interface GeoControl
{
	public abstract L2Territory getGeoPos();

	public abstract HashMap<Long, Byte> getGeoAround();

	public abstract void setGeoAround(HashMap<Long, Byte> value);

	public abstract int getReflection();
}
