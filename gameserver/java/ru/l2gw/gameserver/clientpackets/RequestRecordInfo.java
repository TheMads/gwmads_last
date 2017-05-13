package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Summon;
import ru.l2gw.gameserver.model.L2World;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.instances.L2StaticObjectInstance;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.commons.arrays.GArray;

public class RequestRecordInfo extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		player.sendUserInfo(false);

		GArray<L2Object> objs = L2World.getAroundObjects(player);
		for(L2Object obj : objs)
		{
			if(obj instanceof L2NpcInstance)
				player.sendPacket(new NpcInfo((L2NpcInstance) obj, player));
			else if(obj instanceof L2Summon)
				((L2Summon) obj).broadcastPetInfo();
			else if(obj.isPlayer())
			{
				L2Player targetPlayer = (L2Player) obj;
				if(player.getObjectId() != targetPlayer.getObjectId() && !targetPlayer.isInvisible() && !targetPlayer.isHide())
				{
					player.sendPacket(new CharInfo(targetPlayer));
					if(targetPlayer.getMountEngine().isMounted())
						player.sendPacket(new Ride(targetPlayer));
					if(targetPlayer.isInBoat())
						player.sendPacket(new GetOnVehicle(targetPlayer, targetPlayer.getVehicle(), targetPlayer.getLocInVehicle()));
				}
			}
			else if(obj instanceof L2DoorInstance)
				player.sendPacket(new StaticObject((L2DoorInstance) obj));
			else if(obj instanceof L2Vehicle)
			{
				if(obj.getZ() > -1000)
					player.sendPacket(new ExAirShipInfo((L2Vehicle) obj));
				else
					player.sendPacket(new VehicleInfo((L2Vehicle) obj));
			}
			else if(obj instanceof L2ItemInstance)
				player.sendPacket(new SpawnItem((L2ItemInstance) obj));
			else if(obj instanceof L2StaticObjectInstance)
				player.sendPacket(new StaticObject((L2StaticObjectInstance) obj));
		}
	}
}
