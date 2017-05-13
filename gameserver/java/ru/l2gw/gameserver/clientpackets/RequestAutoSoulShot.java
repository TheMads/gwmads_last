package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.handler.IItemHandler;
import ru.l2gw.gameserver.handler.ItemHandler;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.ExAutoSoulShot;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestAutoSoulShot extends L2GameClientPacket
{
	private int _itemId;
	private boolean _type; // 1 = on : 0 = off;

	/**
	 * format:		chdd
	 */
	@Override
	public void readImpl()
	{
		_itemId = readD();
		_type = readD() == 1;
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		if(player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE || player.isDead())
			return;

		L2ItemInstance item = player.getInventory().getItemByItemId(_itemId);

		if(item == null)
			return;

		if(_type)
		{
			if(player.isTradeInProgress())
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_PICK_UP_OR_USE_ITEMS_WHILE_TRADING));
				return;
			}

			IItemHandler handler = ItemHandler.getInstance().getItemHandler(_itemId);
			if(handler == null)
			{
				System.out.println("Warning: no item handler for item: " + _itemId + " request auto soul shot by " + player);
				return;
			}
			if(handler.useItem(player, item))
			{
				player.addAutoSoulShot(_itemId);
				player.sendPacket(new ExAutoSoulShot(_itemId, true));
				player.sendPacket(new SystemMessage(SystemMessage.THE_USE_OF_S1_WILL_NOW_BE_AUTOMATED).addItemName(_itemId));
			}
			else if(_itemId >= 6645 && _itemId <= 6647)
				player.sendPacket(new SystemMessage(SystemMessage.SINCE_A_SERVITOR_OR_A_PET_DOES_NOT_EXIST_AUTOMATIC_USE_IS_NOT_APPLICABLE));

			return;
		}

		player.removeAutoSoulShot(_itemId);
		player.sendPacket(new ExAutoSoulShot(_itemId, false));
		player.sendPacket(new SystemMessage(SystemMessage.THE_AUTOMATIC_USE_OF_S1_WILL_NOW_BE_CANCELLED).addString(item.getItem().getName()));
	}
}