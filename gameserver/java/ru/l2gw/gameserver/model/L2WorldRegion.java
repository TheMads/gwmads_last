package ru.l2gw.gameserver.model;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.ai.DefaultAI;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.tables.SpawnTable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * @author Diamond
 * @Date: 15/5/2007
 * @Time: 10:06:34
 */
public final class L2WorldRegion
{
	private int tileX, tileY, tileZ;
	private Boolean _active = false;
	private final Map<Integer, L2Object> _objects;
	private ScheduledFuture<?> _deactivateTask = null;

	public L2WorldRegion(int pTileX, int pTileY, int pTileZ)
	{
		tileX = pTileX;
		tileY = pTileY;
		tileZ = pTileZ;
		_objects = new ConcurrentHashMap<>();
	}

	private void switchAI(Boolean isOn)
	{
		if(_objects.size() == 0)
			return;

		for(L2Character cha : getCharactersList(-1))
			if(isOn)
			{
				if(cha.getAI() instanceof DefaultAI)
					cha.getAI().startAITask();
			}
			else if(cha.hasAI() && !cha.getAI().isGlobalAI() && cha.getAI() instanceof DefaultAI)
			{
				cha.setTarget(null);
				cha.stopMove();
				cha.stopAllEffects();
				cha.getAI().stopAITask();
			}
	}

	private void setActive(boolean value)
	{
		synchronized(_active)
		{
			if(_active == value)
				return;
			_active = value;

			if(value)
			{
				if(_deactivateTask != null)
				{
					_deactivateTask.cancel(true);
					_deactivateTask = null;
				}
				switchAI(value);
			}
			else if(_deactivateTask == null)
				_deactivateTask = ThreadPoolManager.getInstance().scheduleGeneral(new Deactivate(this), 300000);
		}
	}

	private void changeStatus(boolean status)
	{
		for(L2WorldRegion neighbor : getNeighbors())
			if(neighbor != null && (status || neighbor.areNeighborsEmpty()))
				neighbor.setActive(status);
	}

	public void addToPlayers(L2Object object, L2Character dropper)
	{
		if(_objects.size() == 0)
			return;

		L2Player player = null;
		if(object.isPlayer())
			player = (L2Player) object;

		// Если object - игрок, показать ему все видимые обьекты в регионе
		if(player != null)
			for(L2Object obj : getObjectsList(object.getReflection()))
			{
				if(obj == null)
					continue;
				// Если это фэйк обсервера - не показывать.
				if(obj.isPlayer() && obj.inObserverMode() && obj.getOlympiadGameId() < 0 && equals(((L2Player) obj).getObservRegion()))
					continue;

				player.addVisibleObject(obj, dropper);
			}

		// Показать обьект всем игрокам в регионе
		for(L2Object obj : getObjectsList(object.getReflection()))
		{
			if(obj != null && obj.isPlayer())
			{
				if(obj.inObserverMode() && obj.getOlympiadGameId() < 0 && equals(obj.getCurrentRegion()))
					continue;
				((L2Player) obj).addVisibleObject(object, dropper);
			}
		}
	}

	public void removeFromPlayers(L2Object object)
	{
		if(_objects.size() == 0)
			return;

		L2Player player = null;
		if(object.isPlayer())
			player = (L2Player) object;

		// Если object - игрок, убрать у него все видимые обьекты в регионе
		if(player != null)
			for(L2Object obj : getObjectsList(object.getReflection()))
				if(obj != null)
					player.removeVisibleObject(obj);

		// Убрать обьект у всех игроков в регионе
		for(L2Object obj : getObjectsList(object.getReflection()))
		{
			if(obj != null && obj.isPlayer())
			{
				if(obj.inObserverMode() && obj.getOlympiadGameId() < 0 && equals(obj.getCurrentRegion()))
					continue;
				((L2Player) obj).removeVisibleObject(object);
			}
		}
	}

	public void addObject(L2Object obj)
	{
		if(obj == null)
			return;

		_objects.put(obj.getObjectId(), obj);

		if(obj.isPlayer())
			changeStatus(true);
		else if(obj.isNpc())
		{
			if(obj.getAI() instanceof DefaultAI && (!areNeighborsEmpty() || obj.getAI().isGlobalAI()))
				obj.getAI().startAITask();
		}
	}

	public void removeObject(L2Object obj, Boolean move)
	{
		if(obj == null)
			return;

		_objects.remove(obj.getObjectId());

		if(obj.isPlayer())
			changeStatus(false);
		else if(obj.isNpc())
		{
			if(!move && obj.getAI() instanceof DefaultAI && !obj.getAI().isGlobalAI())
				obj.getAI().stopAITask();
		}
	}

	public GArray<L2Object> getObjectsList(int reflection)
	{
		GArray<L2Object> result = new GArray<L2Object>();
		try
		{

			if(_objects.size() == 0)
				return result;

			for(L2Object obj : _objects.values())
				if(obj != null && (reflection == -1 || obj.getReflection() == reflection))
					result.add(obj);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		return result;
	}

	public GArray<L2Character> getCharactersList(int reflection)
	{
		GArray<L2Character> result = new GArray<L2Character>();

		try
		{
			for(L2Object obj : _objects.values())
				if(obj != null && obj.isCharacter() && (reflection == -1 || obj.getReflection() == reflection))
					result.add((L2Character) obj);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		return result;
	}

	public GArray<L2NpcInstance> getNpcsList(int reflection)
	{
		GArray<L2NpcInstance> result = new GArray<L2NpcInstance>();

		try
		{
			for(L2Object obj : _objects.values())
				if(obj != null && obj.isNpc() && (reflection == -1 || obj.getReflection() == reflection))
					result.add((L2NpcInstance) obj);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		return result;
	}

	public GArray<L2Player> getPlayersList(int reflection)
	{
		GArray<L2Player> result = new GArray<L2Player>();

		try
		{
			for(L2Object obj : _objects.values())
				if(obj instanceof  L2Player && (reflection == -1 || obj.getReflection() == reflection))
					result.add((L2Player) obj);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		return result;
	}

	public void deleteVisibleNpcSpawns()
	{
		try
		{
			if(_objects.size() > 0)
			{
				GArray<L2NpcInstance> toRemove = new GArray<L2NpcInstance>();
				for(L2Object obj : _objects.values())
					if(obj != null && obj.isNpc())
						toRemove.add((L2NpcInstance) obj);

				for(L2NpcInstance npc : toRemove)
				{
					L2Spawn spawn = npc.getSpawn();
					if(spawn != null)
					{
						npc.deleteMe();
						spawn.stopRespawn();
						SpawnTable.getInstance().deleteSpawn(spawn, false);
					}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Показывает игроку все видимые обьекты в регионе
	 */
	public void showObjectsToPlayer(L2Player player)
	{
		if(player != null && _objects.size() > 0)
			for(L2Object obj : getObjectsList(player.getReflection()))
				if(obj != null)
					player.addVisibleObject(obj, null);
	}

	/**
	 * Убирает у игрока все видимые обьекты в регионе
	 */
	public void removeObjectsFromPlayer(L2Player player)
	{
		if(player != null && _objects.size() > 0)
			for(L2Object obj : getObjectsList(player.getReflection()))
				if(obj != null)
					player.removeVisibleObject(obj);
	}

	/**
	 * Убирает обьект у всех игроков в регионе
	 */
	public void removePlayerFromOtherPlayers(L2Object object)
	{
		if(object != null && _objects.size() > 0)
			for(L2Object obj : getObjectsList(object.getReflection()))
				if(obj != null && obj.isPlayer())
					((L2Player) obj).removeVisibleObject(object);
	}

	public boolean areNeighborsEmpty()
	{
		if(!isEmpty())
			return false;
		for(L2WorldRegion neighbor : getNeighbors())
			if(neighbor != null && !neighbor.isEmpty())
				return false;
		return true;
	}

	public GArray<L2WorldRegion> getNeighbors()
	{
		return L2World.getNeighbors(tileX, tileY, tileZ, 1, 1);
	}

	public GArray<L2WorldRegion> getNeighbors(int deep, int deepV)
	{
		return L2World.getNeighbors(tileX, tileY, tileZ, deep, deepV);
	}


	public int getObjectsSize()
	{
		return _objects.size();
	}

	public boolean isEmpty()
	{
		return getPlayersList(-1).isEmpty();
	}

	public boolean isActive()
	{
		return _active;
	}

	public String getName()
	{
		return "(" + tileX + ", " + tileY + ", " + tileZ + ")";
	}

	@Override
	public String toString()
	{
		return "L2WorldRegion[" + tileX + "][" + tileY + "][" + tileZ + "]";
	}

	private static class Deactivate implements Runnable
	{
		private L2WorldRegion _region;

		public Deactivate(L2WorldRegion region)
		{
			_region = region;
		}

		public void run()
		{
			_region.switchAI(false);
			_region._deactivateTask = null;
		}
	}
}
