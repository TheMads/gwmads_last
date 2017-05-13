package ru.l2gw.gameserver.instancemanager;

import javolution.util.FastList;

import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.model.entity.Coliseum;

/**
 * This class need for load zones of Ungerground Coliseum
 *@author FlareDrakon l2f
 */
public class UnderGroundColliseumManager
{
	private static UnderGroundColliseumManager _instance;
	private static FastList<Coliseum> _coliseums;

	public static UnderGroundColliseumManager getInstance()
	{
		if(_instance == null)
			_instance = new UnderGroundColliseumManager();
		return _instance;
	}

	protected void addZone(L2Zone zone)
	{
		if(_coliseums == null)
			_coliseums = new FastList<Coliseum>();
		_coliseums.add(new Coliseum(zone));
	}

	public Coliseum getBuildingByCoord(int x, int y)
	{
		for(Coliseum coliseum : _coliseums)
			if(coliseum != null && coliseum.checkIfInZone(x, y))
				return coliseum;
		return null;
	}

	public Coliseum getBuildingById(int id)
	{
		for(Coliseum coliseum : _coliseums)
			if(coliseum.getId() == id)
				return coliseum;
		return null;
	}

	public void clearAllZones()
	{
		_coliseums.clear();
	}
}
