package ru.l2gw.gameserver.model.entity.vehicle;

import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.templates.L2CharTemplate;
import ru.l2gw.util.Location;
import ru.l2gw.util.Util;

/**
 * @author: rage
 * @date: 24.06.2010 20:43:41
 */
public class L2AirShip extends L2Vehicle
{
	public L2AirShip(int objectId, L2CharTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void sendInfo(L2Player player)
	{
		player.sendPacket(new ExAirShipInfo(this));
	}

	@Override
	public void sendOnBoardInfo(L2Player player, L2Player onBoardPlayer)
	{
		if(player != null && _onBoard != null && onBoardPlayer != null && _onBoard.contains(onBoardPlayer.getObjectId()))
		{
			sendInfo(player);
			if(player.getVehicle() != this)
				player.sendPacket(new ExGetOnAirShip(onBoardPlayer, this, onBoardPlayer.getLocInVehicle()));
		}
	}

	@Override
	public void oustPlayer(L2Player player, Location loc)
	{
		if(_onBoard == null || _onBoard.indexOf(player.getObjectId()) < 0)
			return;

		removePlayerFromBoard(player);
		player.setVehicle(null);

		if(getHelm() != null && player.getTarget() == getHelm() && getHelm().getClanAirship().getCaptainObjectId() == player.getObjectId())
		{
			getHelm().getClanAirship().setCaptainObjectId(0);
			player.setTarget(null);
			getHelm().getClanAirship().broadcastUserInfo();
		}
		player.setXYZInvisible(loc.getX(), loc.getY(), loc.getZ());
		player.sendUserInfo(true);
		player.sendActionFailed();
		player.sendActionFailed();
		player.broadcastPacket(new ExGetOffAirShip(player, this, loc));
		player.setIsTeleporting(true);
		player.decayMe();
	}

	@Override
	public void setKickPoint(Location loc)
	{
		_kickPoint = loc;
		if(_onBoard != null)
			for(int objectId : _onBoard)
			{
				L2Player player = L2ObjectsStorage.getPlayer(objectId);
				if(player != null && player.getVehicle() == this)
					player.setStablePoint(loc);
			}
	}

	@Override
	public void stopMove()
	{
		prevDestination = null;
		if(isMoving)
		{
			isMoving = false;
			setXYZ(getX(), getY(), getZ(), false);
			broadcastPacket(new ExStopMoveAirShip(this));
		}
	}

	@Override
	public void broadcastMove()
	{
		//broadcastPacket(new ExAirShipInfo(this));
		broadcastPacket(new ExMoveToLocationAirShip(this));
	}

	@Override
	public void teleToLocation(Location loc)
	{
		broadcastPacket(new ExStopMoveAirShip(this));
		setIsTeleporting(true);
		if(_onBoard != null)
			for(int objectId : _onBoard)
			{
				L2Player player = L2ObjectsStorage.getPlayer(objectId);
				if(player != null && player.getVehicle() == this)
				{
					Location vl = Util.convertVehicleCoordToWorld(loc, player.getLocInVehicle(), true);
					player.teleToLocation(vl.getX(), vl.getY(), vl.getZ(), getReflection(), true);
				}
			}
		super.teleToLocation(loc);
		setIsTeleporting(false);
	}

/*
	@Override
	public void broadcastPacket(L2GameServerPacket pk)
	{
		for(L2Player player : getAroundPlayers(3000, 3000))
			if(getDistance(player) < 3000)
				player.sendPacket(pk);

		for(L2Player player : getAroundPlayers(4000, 3000))
			if(getDistance(player) > 3100)
				player.sendPacket(_dop);
	}
*/

	@Override
	public boolean isFlying()
	{
		return true;
	}
}
