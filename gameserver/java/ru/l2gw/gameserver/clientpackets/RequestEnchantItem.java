package ru.l2gw.gameserver.clientpackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.PcInventory;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.EnchantResult;
import ru.l2gw.gameserver.serverpackets.InventoryUpdate;
import ru.l2gw.gameserver.serverpackets.MagicSkillUse;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.templates.L2Item;

public class RequestEnchantItem extends AbstractEnchantPacket
{
	protected static Log _log = LogFactory.getLog(RequestEnchantItem.class.getName());
	// Format: cd
	private int _objectId;
	private int _supportObjectId;

	@Override
	public void readImpl()
	{
		_objectId = readD();
		_supportObjectId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(player.isOutOfControl() || player.isActionsDisabled())
		{
			player.cancelActiveEnchant();
			player.sendActionFailed();
			return;
		}

		PcInventory inventory = player.getInventory();
		L2ItemInstance itemToEnchant = inventory.getItemByObjectId(_objectId);
		L2ItemInstance scroll = player.getEnchantScroll();
		L2ItemInstance support = player.getEnchantSupportItem();

		if(itemToEnchant == null || scroll == null)
		{
			player.cancelActiveEnchant();
			return;
		}

		if(player.getEnchantStartTime() == 0 || player.getEnchantStartTime() + 2000 > System.currentTimeMillis())
		{
			player.cancelActiveEnchant();
			return;
		}

		if(player.isInStoreMode() || player.isTradeInProgress())
		{
			player.cancelActiveEnchant();
			player.sendPacket(Msg.YOU_CANNOT_PRACTICE_ENCHANTING_WHILE_OPERATING_A_PRIVATE_STORE_OR_PRIVATE_MANUFACTURING_WORKSHOP);
			player.sendPacket(new EnchantResult(2));
			return;
		}

		// template for scroll
		EnchantScroll scrollTemplate = getEnchantScroll(scroll);

		// scroll not found in list
		if(scrollTemplate == null)
		{
			player.cancelActiveEnchant();
			return;
		}

		// template for support item, if exist
		EnchantItem supportTemplate = null;
		if(support != null)
			supportTemplate = getSupportItem(support);

		// first validation check
		if(!scrollTemplate.isValid(itemToEnchant, supportTemplate) || !isEnchantable(itemToEnchant))
		{
			player.cancelActiveEnchant();
			player.sendPacket(Msg.INAPPROPRIATE_ENCHANT_CONDITIONS);
			player.sendPacket(new EnchantResult(2));
			return;
		}

		if((scroll = inventory.getItemByObjectId(scroll.getObjectId())) == null)
			return;

		// Запрет на заточку чужих вещей, баг может вылезти на серверных лагах
		if(itemToEnchant.getOwnerId() != player.getObjectId() || (support != null && support.getOwnerId() != player.getObjectId()))
		{
			player.cancelActiveEnchant();
			player.sendPacket(Msg.INAPPROPRIATE_ENCHANT_CONDITIONS);
			player.sendPacket(new EnchantResult(2));
			return;
		}

		L2ItemInstance removedScroll;
		synchronized(inventory)
		{
			removedScroll = inventory.destroyItem("Enchant", scroll.getObjectId(), 1, player, null);
		}

		//tries enchant without scrolls
		if(removedScroll == null)
		{
			player.cancelActiveEnchant();
			player.sendPacket(Msg.INAPPROPRIATE_ENCHANT_CONDITIONS);
			player.sendPacket(new EnchantResult(2));
			return;
		}

		if(support != null && !player.destroyItem("Enchant", support.getObjectId(), 1, null, false))
		{
			player.cancelActiveEnchant();
			player.sendPacket(Msg.INAPPROPRIATE_ENCHANT_CONDITIONS);
			player.sendPacket(new EnchantResult(2));
			return;
		}

		synchronized(itemToEnchant)
		{
			int chance = scrollTemplate.getChance(itemToEnchant, supportTemplate);

			if(itemToEnchant.getEnchantLevel() >= Config.ENCHANT_CHANCE_DECREASE_LEVEL && itemToEnchant.getItemId() != 13539)
				chance /= 2;

			boolean overEnchant = false;
			if(Config.OVER_ENCHANT_ENABLED)
			{
				int maxEnchant = 0;
				if(itemToEnchant.getItem().getType2() == L2Item.TYPE2_WEAPON)
					maxEnchant = Config.ENCHANT_MAX_WEAPON;
				else if(itemToEnchant.getItem().getType2() == L2Item.TYPE2_ACCESSORY)
					maxEnchant = Config.ENCHANT_MAX_ACCESSORY;
				else if(itemToEnchant.getItem().getType2() == L2Item.TYPE1_SHIELD_ARMOR)
					maxEnchant = Config.ENCHANT_MAX_ARMOR;

				if(maxEnchant > 0 && maxEnchant <= itemToEnchant.getEnchantLevel())
				{
					chance = Config.OVER_ENCHANT_CHANCE;
					overEnchant = true;
				}
			}

			if(chance < 0)
			{
				player.cancelActiveEnchant();
				player.sendPacket(Msg.INAPPROPRIATE_ENCHANT_CONDITIONS);
				player.sendPacket(new EnchantResult(2));
				return;
			}
			//System.out.println("Enchant chance: " + chance);
			if(Rnd.chance(chance))
			{
				//System.out.println("Enchant chance: ok");
				itemToEnchant.changeEnchantLevel("Enchant", itemToEnchant.getEnchantLevel() + (overEnchant ? Config.OVER_ENCHANT_VALUE : 1), player, scroll);
				itemToEnchant.updateDatabase();
				player.sendPacket(new EnchantResult(0));

				if(itemToEnchant.isEquipped())
					player.getInventory().refreshItemListeners(itemToEnchant);

				player.sendPacket(new InventoryUpdate().addModifiedItem(itemToEnchant));

				// announce the success
				if(itemToEnchant.getEnchantLevel() == (itemToEnchant.getItem().getType2() == L2Item.TYPE2_WEAPON ? 7 : 6) || itemToEnchant.getEnchantLevel() == 15)
				{
					player.broadcastPacket(new MagicSkillUse(player, player, 21006, 1, 2000, 0, false));
					player.broadcastPacket(new SystemMessage(SystemMessage.C1_HAS_SUCCESSFULY_ENCHANTED_A__S2_S3).addCharName(player).addNumber(itemToEnchant.getEnchantLevel()).addItemName(itemToEnchant.getItemId()));
				}
			}
			else
			{
				if(scrollTemplate.isSafe() && !overEnchant)
				{
					// safe enchant - remain old value
					// need retail message
					player.sendPacket(new EnchantResult(5));
				}
				else
				{
					if(!overEnchant && (scrollTemplate.isBlessed() || scrollTemplate.isCrystal() && Config.ENCHANT_CRYSTAL_DONT_BREAK))
					{
						int enchant = customItems.containsKey(itemToEnchant.getItemId()) ? customItems.get(itemToEnchant.getItemId()).getInteger("bless_safe") : itemToEnchant.getBodyPart() == L2Item.SLOT_FULL_ARMOR ? Config.BLESSED_ENCHANT_SAFE_FULL_BODY : Config.BLESSED_ENCHANT_SAFE_COMMON;
						itemToEnchant.changeEnchantLevel("Enchant", enchant, player, scroll);
						itemToEnchant.updateDatabase();
						if(itemToEnchant.isEquipped())
							player.getInventory().refreshItemListeners(itemToEnchant);
						player.sendPacket(Msg.FAILED_IN_BLESSED_ENCHANT_THE_ENCHANT_VALUE_OF_THE_ITEM_BECAME_0);
						player.sendPacket(new EnchantResult(3)); // t2.2
						player.sendPacket(new InventoryUpdate().addModifiedItem(itemToEnchant));
					}
					else
					{
						if(itemToEnchant.isEquipped())
						{
							player.getInventory().unEquipItemAndSendChanges(itemToEnchant);
							player.unEquipInappropriateItems();
						}

						L2ItemInstance destroyedItem = inventory.destroyItem("EnchantFail", itemToEnchant.getObjectId(), 1, player, null);
						if(destroyedItem == null)
						{
							_log.warn("failed to destroy " + itemToEnchant.getObjectId() + " after unsuccessful enchant attempt by char " + player.getName());
							player.sendActionFailed();
							return;
						}
						int crystalId = itemToEnchant.getCrystalType().cry;
						if(crystalId > 0)
						{
							int count = itemToEnchant.getCrystalCount() - (itemToEnchant.getItem().getCrystalCount() + 1) / 2;
							if(count < 1)
								count = 1;

							player.sendPacket(new EnchantResult(1, itemToEnchant.getCrystalType().cry, count));
							player.addItem("Enchant", itemToEnchant.getCrystalType().cry, count, null, true);
						}
						else
							player.sendPacket(new EnchantResult(4));

						player.refreshExpertisePenalty();
						player.refreshOverloaded();
					}
				}
			}
		}

		player.setEnchantScroll(null);
		player.setEnchantSupportItem(null);
		player.setEnchantStartTime(0);
		player.sendChanges();
	}
}