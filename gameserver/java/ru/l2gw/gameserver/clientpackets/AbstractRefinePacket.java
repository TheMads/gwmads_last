package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.tables.VariationData;

/**
 * @author rage
 * @date 29.06.2010 15:35:12
 */
public abstract class AbstractRefinePacket extends L2GameClientPacket
{
	/*
	 * Checks player, source item, lifestone and gemstone validity for augmentation process
	 */
	protected static boolean isValid(L2Player player, L2ItemInstance item, L2ItemInstance refinerItem, L2ItemInstance gemStones, boolean message)
	{
		if(!isValid(player, item, refinerItem, message))
			return false;

		// GemStones must belong to owner
		if(gemStones.getOwnerId() != player.getObjectId())
		{
			if(message)
				player.sendPacket(Msg.THIS_IS_NOT_A_SUITABLE_ITEM);
			return false;
		}
		// .. and located in inventory
		if(gemStones.getLocation() != L2ItemInstance.ItemLocation.INVENTORY)
		{
			if(message)
				player.sendPacket(Msg.THIS_IS_NOT_A_SUITABLE_ITEM);
			return false;
		}

		return true;
	}

	/*
	 * Checks player, source item and lifestone validity for augmentation process
	 */
 	protected static boolean isValid(L2Player player, L2ItemInstance item, L2ItemInstance refinerItem, boolean message)
	{
		if(!isValid(player, item, message))
			return false;

		// Item must belong to owner
		if(refinerItem.getOwnerId() != player.getObjectId())
		{
			if(message)
				player.sendPacket(Msg.THIS_IS_NOT_A_SUITABLE_ITEM);
			return false;
		}
		// Lifestone must be located in inventory
		if(refinerItem.getLocation() != L2ItemInstance.ItemLocation.INVENTORY)
		{
			if(message)
				player.sendPacket(Msg.THIS_IS_NOT_A_SUITABLE_ITEM);
			return false;
		}
		// check for level of the lifestone
		if(player.getLevel() < VariationData.getMineralLevel(refinerItem.getItemId()))
		{
			if(message)
				player.sendPacket(Msg.THE_LEVEL_OF_THE_HARDENER_IS_TOO_HIGH_TO_BE_USED);
			return false;
		}

		return true;
	}

	/*
	 * Check both player and source item conditions for augmentation process
	 */
	protected static boolean isValid(L2Player player, L2ItemInstance item, boolean message)
	{
		if(!isValid(player, message))
			return false;

		if(item == null || item.getOwnerId() != player.getObjectId())
			return false;

		if(item.isAugmented())
		{
			if(message)
				player.sendPacket(Msg.ONCE_AN_ITEM_IS_AUGMENTED_IT_CANNOT_BE_AUGMENTED_AGAIN);
			return false;
		}

		if(!VariationData.isValidItem(item.getItemId()))
		{
			if(message)
				player.sendPacket(Msg.THIS_IS_NOT_A_SUITABLE_ITEM);
			return false;
		}

		return true;
	}

	/*
	 * Check if player's conditions valid for augmentation process
	 */
	protected static boolean isValid(L2Player player, boolean message)
	{
		if(player == null)
			return false;

		if(player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			if(message)
				player.sendPacket(Msg.YOU_CANNOT_AUGMENT_ITEMS_WHILE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP_IS_IN_OPERATION);
			return false;
		}
		if(player.isOutOfControl())
		{
			if(message)
				player.sendPacket(Msg.YOU_CANNOT_AUGMENT_ITEMS_WHILE_FROZEN);
			return false;
		}
		if(player.isDead())
		{
			if(message)
				player.sendPacket(Msg.YOU_CANNOT_AUGMENT_ITEMS_WHILE_DEAD);
			return false;
		}
		if(player.isTradeInProgress())
		{
			if(message)
				player.sendPacket(Msg.YOU_CANNOT_AUGMENT_ITEMS_WHILE_ENGAGED_IN_TRADE_ACTIVITIES);
			return false;
		}
		if(player.isParalyzed())
		{
			if(message)
				player.sendPacket(Msg.YOU_CANNOT_AUGMENT_ITEMS_WHILE_PARALYZED);
			return false;
		}
		if(player.isFishing())
		{
			if(message)
				player.sendPacket(Msg.YOU_CANNOT_AUGMENT_ITEMS_WHILE_FISHING);
			return false;
		}
		if(player.isSitting())
		{
			if(message)
				player.sendPacket(Msg.YOU_CANNOT_AUGMENT_ITEMS_WHILE_SITTING_DOWN);
			return false;
		}

		return !(player.getLastNpc() == null || !player.isInRange(player.getLastNpc(), player.getInteractDistance(player.getLastNpc()))) && !(player.isCursedWeaponEquipped() || player.getEnchantScroll() != null);
	}
}