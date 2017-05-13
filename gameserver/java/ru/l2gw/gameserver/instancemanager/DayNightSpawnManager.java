package ru.l2gw.gameserver.instancemanager;

import java.util.HashSet;
import java.util.Set;

import ru.l2gw.gameserver.controllers.GameTimeController;
import ru.l2gw.gameserver.model.L2Spawn;
import ru.l2gw.gameserver.model.instances.L2RaidBossInstance;

public class DayNightSpawnManager
{
	private static DayNightSpawnManager _instance;
	private static HashSet<L2Spawn> _dayMobs = new HashSet<L2Spawn>();
	private static HashSet<L2Spawn> _nightMobs = new HashSet<L2Spawn>();
	private static HashSet<L2Spawn> _bosses = new HashSet<L2Spawn>();

	public static DayNightSpawnManager getInstance()
	{
		if(_instance == null)
			_instance = new DayNightSpawnManager();

		return _instance;
	}

	public void addDayMob(L2Spawn spawnDat)
	{
		_dayMobs.add(spawnDat);
	}

	public void addNightMob(L2Spawn spawnDat)
	{
		_nightMobs.add(spawnDat);
	}

	public void deleteMobs(Set<L2Spawn> mobsSpawnsList)
	{
		for(L2Spawn spawnDat : mobsSpawnsList)
			spawnDat.despawnAll();
	}

	public void spawnMobs(Set<L2Spawn> mobsSpawnsList)
	{
		for(L2Spawn spawnDat : mobsSpawnsList)
			spawnDat.init();
	}

	public void changeMode(int mode)
	{
		switch(mode)
		{
			case 1: // day spawns
				deleteMobs(_nightMobs);
				deleteMobs(_dayMobs);
				spawnMobs(_dayMobs);

				specialNightBoss(mode);
				break;
			case 2: // night spawns
				deleteMobs(_nightMobs);
				deleteMobs(_dayMobs);
				spawnMobs(_nightMobs);

				specialNightBoss(mode);
				break;
		}
	}

	public void notifyChangeMode()
	{
		if(GameTimeController.getInstance().isNowNight())
			changeMode(2);
		else
			changeMode(1);
	}

	public void cleanUp()
	{
		deleteMobs(_nightMobs);
		deleteMobs(_dayMobs);

		if(_bosses != null && !_bosses.isEmpty())
			deleteMobs(_bosses);

		_nightMobs.clear();
		_dayMobs.clear();
		_bosses.clear();
	}

	private void specialNightBoss(int mode)
	{
		try
		{
			for(L2Spawn spawnDat : _bosses)
				switch(mode)
				{
					case 1:
						spawnDat.despawnAll();
						break;
					case 2:
						if(!RaidBossSpawnManager.getInstance().isScheduled(spawnDat.getNpcId()))
						{
							L2RaidBossInstance boss = (L2RaidBossInstance) spawnDat.doSpawn(true);
							RaidBossSpawnManager.getInstance().notifySpawnNightBoss(boss);
						}
						break;
				}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public L2RaidBossInstance handleNightBoss(L2Spawn spawnDat)
	{
		L2RaidBossInstance raidboss = null;
		if(GameTimeController.getInstance().isNowNight())
			raidboss = (L2RaidBossInstance) spawnDat.doSpawn(true);
		_bosses.add(spawnDat);
		return raidboss;
	}
}