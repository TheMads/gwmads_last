package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.PcInventory;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2PetInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestGiveItemToPet extends L2GameClientPacket
{
	private int _objectId;
	private long _amount;

	@Override
	public void readImpl()
	{
		_objectId = readD();
		_amount = readQ();
	}

	@Override
	public void runImpl()
	{
		if(_amount < 1)
			return;
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2PetInstance pet = (L2PetInstance) player.getPet();
		if(!player.isPetSummoned() || pet.isDead())
		{
			sendPacket(new SystemMessage(SystemMessage.CANNOT_GIVE_ITEMS_TO_A_DEAD_PET));
			return;
		}

		if(player.isInStoreMode())
		{
			sendPacket(new SystemMessage(SystemMessage.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM));
			return;
		}

		AbstractEnchantPacket.checkAndCancelEnchant(player);

		PcInventory playerInventory = player.getInventory();

		L2ItemInstance playerItem = playerInventory.getItemByObjectId(_objectId);

		if(playerItem == null || !playerItem.canBeDropped(player))
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOUR_PET_CANNOT_CARRY_THIS_ITEM));
			player.sendActionFailed();
			return;
		}

		if(pet.getInventory().getTotalWeight() + playerItem.getItem().getWeight() * _amount >= pet.getMaxLoad())
		{
			player.sendPacket(new SystemMessage(SystemMessage.EXCEEDED_PET_INVENTORYS_WEIGHT_LIMIT));
			return;
		}
		synchronized(pet.getInventory())
		{
			if(player.transferItem("TransferToPet", _objectId, _amount, pet.getInventory(), pet) == null)
				_log.warn("Invalid item transfer request: " + pet.getName() + "(pet) --> " + player.getName());
		}

		player.sendChanges();
		//sendPacket(new PetItemList(pet));
		pet.broadcastPetInfo();
	}
}