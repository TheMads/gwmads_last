package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.duel.Duel;
import ru.l2gw.gameserver.network.GameClient;
import ru.l2gw.gameserver.serverpackets.CharMoveToLocation;
import ru.l2gw.util.Location;

// cdddddd(d)

public class MoveBackwardToLocation extends L2GameClientPacket
{
	private Location _targetLoc = new Location(0, 0, 0);
	private Location _originLoc = new Location(0, 0, 0);
	private int _moveMovement;
	private static int MAX_PATH_LENGTH = 121000000; // длина в квадрате 11000^2

	/**
	 * packet type id 0x0f
	 */
	@Override
	public void readImpl()
	{
		_targetLoc.setX(readD());
		_targetLoc.setY(readD());
		_targetLoc.setZ(readD());
		_originLoc.setX(readD());
		_originLoc.setY(readD());
		_originLoc.setZ(readD());
		GameClient client = getClient();
		L2Player player = client.getPlayer();
		if(player == null)
			return;

		player.fireMethodInvoked(MethodCollection.onMoveRequest, new Object[]{_targetLoc, _originLoc});

		if(_buf.hasRemaining())
			_moveMovement = readD();
		else
			_log.warn("Incompatible client found: L2Walker? " + client.getLoginName() + "/" + client.getIpAddr());
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		/*if(_targetloc.getX() == 0 || _targetloc.getY() == 0 || _targetloc.getX() == Integer.MAX_VALUE || _targetloc.getY() == Integer.MAX_VALUE)
		{
			System.out.println("MoveBackwardToLocation: zero coords");
			return;
		}*/

		player.moveInVehicle = false;

		if(System.currentTimeMillis() - player.getLastMovePacket() < Config.MOVE_PACKET_DELAY)
		{
			player.sendActionFailed();
			return;
		}

		player.setLastMovePacket();

		//проверяем координаты на размер геодаты по Z
		if(_originLoc.getZ() < -16384 || _originLoc.getZ() > 16384 || _targetLoc.getZ() < -16384 || _targetLoc.getZ() > 16384)
		{
			player.logout(false, false, true);
			return;
		}
		else
		{
			if(player.inObserverMode())
			{
				if(player.getOlympiadGameId() < 0)
				{
					player.sendPacket(Msg.OBSERVERS_CANNOT_PARTICIPATE);
					player.sendActionFailed();
				}
				else
					player.sendPacket(new CharMoveToLocation(player, _targetLoc));

				return;
			}

			Duel duel = player.getDuel();
			if(duel != null && duel.isPartyDuel() && player.getDuelState() == Duel.DUELSTATE_DEAD)
			{
				player.sendPacket(Msg.YOU_CANNOT_MOVE_IN_A_FROZEN_STATE_PLEASE_WAIT_A_MOMENT);
				return;
			}

			//проверяем длину пути, проверено опытным путем что максимальная длина пути не может быть больше 11000
			Location player_location = player.getLoc();

			if(!player.isInRange(_originLoc, 5000) && player.getLastServerPosition() != null)
			{
				_log.info("MoveBackwardToLocation: " + player + " cleintLoc: " + _originLoc + " serverLoc: " + player.getLoc() + " teleport to server loc!");
				player.sendActionFailed();
				if(player.getLastServerPosition() != null)
					player.teleToLocation(player.getLastServerPosition());
				return;
			}

			double dx = (player_location.getX() - _targetLoc.getX()), dy = (player_location.getY() - _targetLoc.getY()), dz = (player_location.getZ() - _targetLoc.getZ());

			//чтобы не извлекать корень - возвели в квадрат MAX_PATH_LENGTH
			//экономим ресурсы           					
			if((dx * dx + dy * dy + dz * dz) > MAX_PATH_LENGTH)
			{
				player.sendActionFailed();
				return;
			}
		}

		if(player.isOutOfControl())
		{
			player.sendActionFailed();
			return;
		}

		if(player.getTeleMode() > 0)
		{
			if(player.getTeleMode() == 1)
				player.setTeleMode(0);
			player.sendActionFailed();
			player.teleToLocation(_targetLoc);
			return;
		}

		if(_moveMovement == 0 && !Config.GEODATA_ENABLED)
		{
			player.sendActionFailed();
			return;
		}

		//if(GeoConfig.GEODATA_DEBUG && "Rage".equals(player.getName()))
		//	_log.info("ClientMove: " + _originLoc + " --> " + _targetLoc);
		if(player.isInFlyingTransform())
			_targetLoc.setZ(Math.min(6000, Math.max(-1000, _targetLoc.getZ())));

		player.moveToLocation(_targetLoc, 0, _moveMovement != 0);
	}
}