package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2World;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestCrystallizeItem extends L2GameClientPacket
{
	//Format: cdd
	//0, d, c, b, a, s ... 0 is for none
	public static short[] _crystalId = { 0, 1458, 1459, 1460, 1461, 1462, 1462 };

	private int _objectId;

	@Override
	public void readImpl()
	{
		_objectId = readD();
		readQ();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		if(player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			player.sendPacket(new SystemMessage(SystemMessage.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM));
			player.sendActionFailed();
			return;
		}

		L2ItemInstance item = player.getInventory().getItemByObjectId(_objectId);

		if(item == null)
		{
			player.sendActionFailed();
			return;
		}

		if((item.getCustomFlags() & L2ItemInstance.FLAG_PET_INVENTORY) == L2ItemInstance.FLAG_PET_INVENTORY)
		{
			player.sendPacket(Msg.THERE_ARE_ITEMS_IN_YOUR_PET_INVENTORY_RENDERING_YOU_UNABLE_TO_SELL_TRADE_DROP_PET_SUMMONING_ITEM_PLEASE_EMPTY_YOUR_PET_INVENTORY);
			return;
		}

		if(!item.canBeCrystallized(player))
		{
			player.sendActionFailed();
			return;
		}

		// unequip if needed
		if(item.isEquipped())
			player.getInventory().unEquipItemAndSendChanges(item);

		player.getInventory().destroyItem("Crystallize", item.getObjectId(), 1, player, null);

		// add crystals
		int crystalAmount = item.getItem().getCrystalCount();
		int crystalId = item.getItem().getCrystalType().cry;

		player.addItem("Crystallize", crystalId, crystalAmount, null, true);
		player.sendPacket(new SystemMessage(SystemMessage.THE_ITEM_HAS_BEEN_SUCCESSFULLY_CRYSTALLIZED));

		player.updateStats();

		L2World.removeObject(item);
	}
}
