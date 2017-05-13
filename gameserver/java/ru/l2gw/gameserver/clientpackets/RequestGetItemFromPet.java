package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.PcInventory;
import ru.l2gw.gameserver.model.PetInventory;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2PetInstance;
import ru.l2gw.gameserver.serverpackets.PetItemList;

public class RequestGetItemFromPet extends L2GameClientPacket
{
	private int _objectId;
	private long _amount;
	@SuppressWarnings("unused")
	private int _unknown;

	@Override
	public void readImpl()
	{
		_objectId = readD();
		_amount = readQ();
		_unknown = readD();// = 0 for most trades
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
		if(!player.isPetSummoned())
		{
			player.sendActionFailed();
			return;
		}

		PetInventory petInventory = pet.getInventory();
		PcInventory playerInventory = player.getInventory();

		L2ItemInstance petItem = petInventory.getItemByObjectId(_objectId);

		if(petItem == null)
		{
			_log.warn("item requested from pet, but its not there.");
			return;
		}

		if(petItem.isEquipped())
		{
			player.sendActionFailed();
			return;
		}

		long finalLoad = petItem.getItem().getWeight() * _amount;

		if(!player.getInventory().validateWeight(finalLoad))
		{
			sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			return;
		}
		petInventory.transferItem("TransferFromPet", _objectId, _amount, playerInventory, player, pet);

		player.sendChanges();
		sendPacket(new PetItemList(pet));
		pet.broadcastPetInfo();
	}
}