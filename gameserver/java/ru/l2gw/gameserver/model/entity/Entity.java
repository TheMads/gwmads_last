package ru.l2gw.gameserver.model.entity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;

public class Entity
{
	protected static Log _log = LogFactory.getLog(Entity.class.getName());

	protected L2Zone _zone;

	public void registerZone(L2Zone zone)
	{
		_zone = zone;
	}

	public L2Zone getZone()
	{
		return _zone;
	}

	public boolean checkIfInZone(L2Character cha)
	{
		if(_zone != null)
			return _zone.isInsideZone(cha);

		_log.error(getClass().getSimpleName() + " has no zone defined");
		return false;
	}

	public boolean checkIfInZone(int x, int y, int z)
	{
		if(_zone != null)
			return _zone.isInsideZone(x, y, z);

		_log.error(getClass().getSimpleName() + " has no zone defined");
		return false;
	}

	public double getDistanceToZone(int x, int y)
	{
		if(_zone != null)
			return _zone.getDistanceToZone(x, y);

		_log.error(getClass().getSimpleName() + " has no zone defined");
		return Double.MAX_VALUE;
	}

	public void broadcastToPlayers(L2GameServerPacket gsp)
	{
		if(_zone != null)
		{
			for(L2Character player : _zone.getCharacters())
			{
				if(player.isPlayer())
					player.sendPacket(gsp);
			}
		}
	}
}
