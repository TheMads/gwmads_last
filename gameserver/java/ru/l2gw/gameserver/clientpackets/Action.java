package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.cache.Msg;

public class Action extends L2GameClientPacket
{
	// cddddc
	private int _objectId;
	@SuppressWarnings("unused")
	private int _originX;
	@SuppressWarnings("unused")
	private int _originY;
	@SuppressWarnings("unused")
	private int _originZ;
	private int _actionId;

	@Override
	public void readImpl()
	{
		_objectId = readD();
		_originX = readD();
		_originY = readD();
		_originZ = readD();
		_actionId = readC();// 0 for simple click  1 for shift click
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(System.currentTimeMillis() - player.getLastPacket() < 100)
		{
			player.sendActionFailed();
			return;
		}

		player.setLastPacket();

		if(player.isOutOfControl())
		{
			if(player.inObserverMode())
				player.sendPacket(Msg.OBSERVERS_CANNOT_PARTICIPATE);
			player.sendActionFailed();
			return;
		}

		L2Object obj = player.getVisibleObject(_objectId);

		if(obj == null)
		{
			L2Vehicle vehicle = player.getVehicle();
			if(vehicle != null && vehicle.getHelmId() == _objectId)
			{
				vehicle.getHelm().onAction(player, false);
				return;
			}

			// Для провалившихся предметов, чтобы можно было все равно поднять
			obj = L2ObjectsStorage.findObject(_objectId);
			if(obj == null || !(obj instanceof L2ItemInstance))
			{
				player.sendActionFailed();
				return;
			}
		}

		player.fireMethodInvoked(MethodCollection.onActionRequest, new Object[]{"target", obj});

		if(player.inObserverMode() && player.getOlympiadGameId() > 0)
		{
			if(obj.isPlayer() && obj.getOlympiadGameId() != player.getOlympiadGameId())
			{
				_log.warn("Player " + getClient() + " try to target player " + obj.getName() + " in Olympiad Observe mode");
				player.sendPacket(Msg.OBSERVERS_CANNOT_PARTICIPATE);
				player.sendActionFailed();
				return;
			}
			else if(obj.isSummon() && obj.getPlayer().getOlympiadGameId() != player.getOlympiadGameId())
			{
				_log.warn("Player " + getClient() + " try to target summon " + obj.getPlayer().getName() + " in Olympiad Observe mode");
				player.sendPacket(Msg.OBSERVERS_CANNOT_PARTICIPATE);
				player.sendActionFailed();
				return;
			}
			else if(obj.isNpc())
			{
				//_log.warn("Player "+getClient()+" try to target NPC "+obj.getName()+" in Olympiad Observe mode!");
				player.sendPacket(Msg.OBSERVERS_CANNOT_PARTICIPATE);
				player.sendActionFailed();
				return;
			}

			if(obj.isPet() && player.getDistance(obj) > Config.PLAYER_VISIBILITY)//Временная проверка,надо заменить на summon.isStayToFar() как булеан состояния самона
				return;
		}

		obj.onAction(player, _actionId == 1);
	}
}