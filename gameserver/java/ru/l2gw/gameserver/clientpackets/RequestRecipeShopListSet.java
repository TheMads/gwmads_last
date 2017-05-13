package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2ManufactureItem;
import ru.l2gw.gameserver.model.L2ManufactureList;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.RecipeShopMsg;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestRecipeShopListSet extends L2GameClientPacket
{
	// format: cdb, b - array of (dd)
	private int _count;
	L2ManufactureList createList = new L2ManufactureList();

	@Override
	public void readImpl()
	{
		_count = readD();
		if(_count * 12 > _buf.remaining() || _count > Short.MAX_VALUE || _count < 0)
		{
			_count = 0;
			return;
		}
		for(int x = 0; x < _count; x++)
		{
			int id = readD();
			long cost = readQ();
			if(id < 1 || cost < 0)
			{
				_count = 0;
				return;
			}
			createList.add(new L2ManufactureItem(id, cost));
		}
		_count = createList.size();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(player.getMountEngine().isMounted())
		{
			cancelStore(player);
			return;
		}

		if(player.isInDuel())
		{
			cancelStore(player);
			return;
		}

		if(player.isActionBlocked(L2Zone.BLOCKED_ACTION_PRIVATE_WORKSHOP))
		{
			sendPacket(Msg.A_PRIVATE_WORKSHOP_MAY_NOT_BE_OPENED_IN_THIS_AREA);
			cancelStore(player);
			return;
		}

		if(player.getNoChannel() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_CURRENTLY_BANNED_FROM_ACTIVITIES_RELATED_TO_THE_PRIVATE_STORE_AND_PRIVATE_WORKSHOP));
			cancelStore(player);
			return;
		}

		if(_count == 0 || player.getCreateList() == null)
		{
			cancelStore(player);
			return;
		}

		if(_count > Config.MAX_PVTCRAFT_SLOTS)
		{
			sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EXCEEDED_THE_QUANTITY_THAT_CAN_BE_INPUTTED));
			cancelStore(player);
			return;
		}

		if(Config.ALT_MIN_PRIVATE_STORE_RADIUS > 0)
			for(L2Player cha : player.getAroundPlayers(Config.ALT_MIN_PRIVATE_STORE_RADIUS))
				if(cha.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
				{
					player.sendPacket(Msg.A_PRIVATE_WORKSHOP_MAY_NOT_BE_OPENED_IN_THIS_AREA);
					return;
				}

		createList.setStoreName(player.getCreateList().getStoreName());
		player.setCreateList(createList);

		player.setPrivateStoreType(L2Player.STORE_PRIVATE_MANUFACTURE);
		player.broadcastUserInfo(true);
		player.broadcastPacket(new RecipeShopMsg(player));
		player.sitDown();
	}

	private void cancelStore(L2Player player)
	{
		player.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
		player.broadcastUserInfo(true);
		player.getBuyList().clear();
		if(player.isInOfflineMode() && Config.SERVICES_OFFLINE_TRADE_KICK_NOT_TRADING)
		{
			player.setOfflineMode(false);
			player.logout(false, false, true);
			player.getNetConnection().disconnectOffline();
		}
	}
}