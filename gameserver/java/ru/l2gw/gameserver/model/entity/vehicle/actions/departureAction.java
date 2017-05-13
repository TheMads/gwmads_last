package ru.l2gw.gameserver.model.entity.vehicle.actions;

import org.w3c.dom.Node;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.ai.L2VehicleAI;
import ru.l2gw.gameserver.clientpackets.Say2C;
import ru.l2gw.gameserver.model.L2World;
import ru.l2gw.gameserver.model.entity.vehicle.L2ClanAirship;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.gameserver.model.entity.vehicle.RouteStation;
import ru.l2gw.gameserver.serverpackets.Say2;

/**
 * @author rage
 * @date 07.05.2010 10:34:05
 */
public class departureAction extends StationAction
{
	private int _moveSpeed;
	private int _rotateSpeed;
	private int _stringId;
	private int _messageId;
	private RouteStation _nextStation;
	private RouteStation _currentStation;
	private L2Vehicle _vehicle;
	private CheckCollisionTask _checkCollisionTask;

	@Override
	public void parseAction(Node an) throws Exception
	{
		_moveSpeed = Integer.parseInt(an.getAttributes().getNamedItem("moveSpeed").getNodeValue());
		_rotateSpeed = Integer.parseInt(an.getAttributes().getNamedItem("rotateSpeed").getNodeValue());
		Node attr = an.getAttributes().getNamedItem("stringId"); 
		_stringId = attr == null ? 0 : Integer.parseInt(attr.getNodeValue());
		attr = an.getAttributes().getNamedItem("messageId");
		_messageId = attr == null ? 0 : Integer.parseInt(attr.getNodeValue());

		super.parseAction(an);
	}

	public void doAction(L2Vehicle vehicle)
	{
		_vehicle = vehicle;
		vehicle.setMoveSpeed(_moveSpeed);
		vehicle.setRotationSpeed(_rotateSpeed);
		int nextStationIndex = ((L2VehicleAI) vehicle.getAI()).getCurrentStationIndex() + 1;
		if(nextStationIndex >= vehicle.getRouteStations().size())
			nextStationIndex = 0;

		_nextStation = vehicle.getRouteStations().get(nextStationIndex);
		_currentStation = vehicle.getRouteStations().get(((L2VehicleAI) vehicle.getAI()).getCurrentStationIndex());

		if(vehicle instanceof L2ClanAirship)
			_log.info("departure action: " + _nextStation.getPoint());

		vehicle.moveToLocation(_nextStation.getPoint(), 0, false);
		if(_nextStation.getStationId() > 0)
		{
			if(_checkCollisionTask == null)
				_checkCollisionTask = new CheckCollisionTask();

			ThreadPoolManager.getInstance().scheduleAi(_checkCollisionTask, 1000, false);
		}
	}

	private class CheckCollisionTask implements Runnable
	{
		public void run()
		{
			if(L2World.getAroundTransport(_nextStation.getPoint(), 800, _vehicle.getObjectId()).size() > 0)
			{
				if(!_currentStation.isDelayed())
				{
					_currentStation.setDelayd(true);
					if(_stringId > 0)
						_vehicle.broadcastPacketToPoints(new Say2(Say2C.SYSTEM_SHOUT, _stringId, _messageId));
					_vehicle.stopMove();
				}

				if(_stringId > 0)
					_vehicle.broadcastPacketToPoints(new Say2(Say2C.SYSTEM_SHOUT, _stringId, _messageId));

				if(!_vehicle.isInRange(_nextStation.getPoint(), 400))
					ThreadPoolManager.getInstance().scheduleAi(_checkCollisionTask, 30000, false);
				return;
			}
			else if(_currentStation.isDelayed())
			{
				_currentStation.setDelayd(false);
				_vehicle.moveToLocation(_nextStation.getPoint(), 0, false);
			}

			if(!_vehicle.isInRange(_nextStation.getPoint(), 400))
				ThreadPoolManager.getInstance().scheduleAi(_checkCollisionTask, 1000, false);
		}
	}
}
