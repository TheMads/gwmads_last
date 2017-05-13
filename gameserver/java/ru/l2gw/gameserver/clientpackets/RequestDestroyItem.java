package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

public class RequestDestroyItem extends L2GameClientPacket
{
	private int _objectId;
	private long _count;

	// format:		cdd
	@Override
	public void readImpl()
	{
		_objectId = readD();
		_count = readQ();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		long count = _count;

		L2ItemInstance itemToRemove = player.getInventory().getItemByObjectId(_objectId);

		if(itemToRemove == null)
			return;

		if(count < 1)
		{
			player.sendPacket(Msg.YOU_CANNOT_DESTROY_IT_BECAUSE_THE_NUMBER_IS_INCORRECT);
			return;
		}

		if(AdminTemplateManager.checkBoolean("noInventory", player))
		{
			player.sendActionFailed();
			return;
		}
	
		boolean canDiscard = AdminTemplateManager.checkBoolean("discardItem", player);

		if(itemToRemove.isHeroItem() && !canDiscard)
		{
			player.sendPacket(Msg.HERO_WEAPONS_CANNOT_BE_DESTROYED);
			return;
		}

		if((itemToRemove.getCustomFlags() & L2ItemInstance.FLAG_PET_INVENTORY) == L2ItemInstance.FLAG_PET_INVENTORY)
		{
			player.sendPacket(Msg.THERE_ARE_ITEMS_IN_YOUR_PET_INVENTORY_RENDERING_YOU_UNABLE_TO_SELL_TRADE_DROP_PET_SUMMONING_ITEM_PLEASE_EMPTY_YOUR_PET_INVENTORY);
			return;
		}

		if(!itemToRemove.canBeDestroyed(player) && !canDiscard)
		{
			//FIXME: может излишне было вносить в canBeDestroyed isActivePetControlItem два раза получаеться проверяем тупо из за месаги
			if(itemToRemove.isActivePetControlItem(player))
				player.sendPacket(Msg.THE_PET_HAS_BEEN_SUMMONED_AND_CANNOT_BE_DELETED);
			else
				player.sendPacket(Msg.THIS_ITEM_CANNOT_BE_DISCARDED);
			return;
		}

		if(player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			player.sendPacket(Msg.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
			return;
		}

		if(player.isFishing())
		{
			player.sendPacket(Msg.YOU_CANNOT_DO_THAT_WHILE_FISHING);
			return;
		}

		if(player.isActionsBlocked() || player.isStunned() || player.isSleeping() || player.isParalyzed())
		{
			player.sendActionFailed();
			return;
		}

		if(_count > itemToRemove.getCount())
			count = itemToRemove.getCount();

		if(itemToRemove.isEquipped())
			player.getInventory().unEquipItemAndSendChanges(itemToRemove);

		if(itemToRemove.getItem().isCrystallizable())
		{
			int level = player.getSkillLevel(L2Skill.SKILL_CRYSTALLIZE);
			if(!(level < 1 || itemToRemove.getItem().getCrystalType().ordinal() > level))
			{
				player.getInventory().destroyItem("Crystallize", itemToRemove.getObjectId(), 1, player, null);

				// add crystals
				int crystalAmount = itemToRemove.getItem().getCrystalCount();
				int crystalId = itemToRemove.getItem().getCrystalType().cry;

				player.sendPacket(Msg.THE_ITEM_HAS_BEEN_SUCCESSFULLY_CRYSTALLIZED);
				player.addItem("Crystallize", crystalId, crystalAmount, itemToRemove, true);

				player.updateStats();
				return;
			}
		}

		player.destroyItem("Destroy", _objectId, count, null, false);
		player.sendChanges();
		player.updateStats();
	}
}
