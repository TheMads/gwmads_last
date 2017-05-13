package ru.l2gw.gameserver.model.entity.vehicle.actions;

import org.w3c.dom.Node;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;

/**
 * @author rage
 * @date 07.09.2010 16:27:22
 */
public class movieAction extends StationAction
{
	private int _movieId;

	@Override
	public void parseAction(Node an) throws Exception
	{
		_movieId = Integer.parseInt(an.getAttributes().getNamedItem("id").getNodeValue());
		super.parseAction(an);
	}

	public void doAction(L2Vehicle vehicle)
	{
		if(_movieId > 0 && vehicle.getOnBoardPlayer() != null)
			for(int objectId : vehicle.getOnBoardPlayer())
			{
				L2Player player = L2ObjectsStorage.getPlayer(objectId);
				if(player != null && player.getVehicle() == vehicle)
					player.showQuestMovie(_movieId);
			}
	}
}
