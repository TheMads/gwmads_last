package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.PcInventory;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.ExShowBaseAttributeCancelWindow;
import ru.l2gw.gameserver.serverpackets.InventoryUpdate;
import ru.l2gw.gameserver.templates.L2Item;

/**
 * @author rage
 */
public class RequestExRemoveItemAttribute extends L2GameClientPacket
{
	// Format: chd
	private int _objectId;
	private int _attributeId;

	@Override
	public void readImpl()
	{
		_objectId = readD();
		_attributeId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(player.isOutOfControl() || player.isActionsDisabled())
		{
			player.sendActionFailed();
			return;
		}

		PcInventory inventory = player.getInventory();
		L2ItemInstance itemToUnnchant = inventory.getItemByObjectId(_objectId);
		long price;

		if(itemToUnnchant == null || itemToUnnchant.getAttributeElementValue(_attributeId) == 0 || player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE || player.isTradeInProgress() || (price = ExShowBaseAttributeCancelWindow.getAttributeRemovePrice(itemToUnnchant)) == 0)
		{
			player.sendPacket(Msg.YOU_FAILED_TO_REMOVE_THE_ELEMENTAL_POWER);
			player.sendActionFailed();
			return;
		}

		if(!player.reduceAdena("RemoveAttribute", price, player.getLastNpc(), true))
		{
			player.sendActionFailed();
			return;
		}

		itemToUnnchant.changeAttributeElement("RemoveAttribute", _attributeId, 0, player, player.getLastNpc());
		player.getInventory().refreshItemListeners(itemToUnnchant);

		player.sendPacket(new InventoryUpdate().addModifiedItem(itemToUnnchant));
		player.sendChanges();
		player.sendPacket(new ExShowBaseAttributeCancelWindow(player));
	}
}