package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2PetInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ItemTable;

public class RequestPetUseItem extends L2GameClientPacket
{
	private int _objectId;

	/**
	 * packet type id 0x94
	 * format:		cd
	 *
	 * @param decrypt
	 */
	@Override
	public void readImpl()
	{
		_objectId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2PetInstance pet = (L2PetInstance) player.getPet();
		if(!player.isPetSummoned())
			return;

		L2ItemInstance item = pet.getInventory().getItemByObjectId(_objectId);

		if(item == null || item.getCount() <= 0)
			return;

		if(player.isAlikeDead() || pet.isDead())
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addItemName(item.getItemId()));
			return;
		}

		if(!item.getItem().isForNpc())
		{
			player.sendPacket(Msg.THIS_PET_CANNOT_USE_THIS_ITEM);
			return;
		}

		if(item.isEquipable())
		{
			if(!pet.tryEquipItem(item))
				player.sendPacket(Msg.THIS_PET_CANNOT_USE_THIS_ITEM);
			return;
		}

		if(!ItemTable.useHandler(pet, item))
			player.sendPacket(Msg.THIS_PET_CANNOT_USE_THIS_ITEM);
	}
}
