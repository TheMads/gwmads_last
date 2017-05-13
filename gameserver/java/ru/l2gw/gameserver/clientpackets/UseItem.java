package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.CursedWeaponsManager;
import ru.l2gw.gameserver.instancemanager.ExtractableItems;
import ru.l2gw.gameserver.model.L2Manor;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.InventoryUpdate;
import ru.l2gw.gameserver.serverpackets.ShowCalc;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2EtcItem;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.commons.arrays.GArray;

import java.nio.BufferUnderflowException;

public class UseItem extends L2GameClientPacket
{
	private int _objectId;
	@SuppressWarnings("unused")
	private int _unknown;

	/**
	 * packet type id 0x19
	 * format:		cdd
	 */
	@Override
	public void readImpl()
	{
		try
		{
			_objectId = readD();
			_unknown = readD();
		}
		catch(BufferUnderflowException e)
		{
			_log.warn(e.getMessage());
			_log.info("Attention! Possible cheater found! Login:" + getClient().getLoginName());
		}
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(player.inObserverMode())
		{
			player.sendPacket(Msg.OBSERVERS_CANNOT_PARTICIPATE);
			player.sendActionFailed();
			return;
		}

		if(player.isOutOfControl())
		{
			player.sendActionFailed();
			return;
		}

		if(player.getUnstuck() != 0)
		{
			player.sendActionFailed();
			return;
		}

		if(player.isInStoreMode())
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_MAY_NOT_USE_ITEMS_IN_A_PRIVATE_STORE_OR_PRIVATE_WORK_SHOP));
			player.sendActionFailed();
			return;
		}

		Integer lastUsedObjectId = (Integer) player.getProperty("ui.objectId");
		if(lastUsedObjectId != null && lastUsedObjectId == _objectId && System.currentTimeMillis() - (Long) player.getProperty("ui.time") < 100)
		{
			player.addProperty("ui.time", System.currentTimeMillis());
			return;
		}

		player.addProperty("ui.time", System.currentTimeMillis());
		player.addProperty("ui.objectId", _objectId);

		synchronized(player.getInventory())
		{
			L2ItemInstance item = player.getInventory().getItemByObjectId(_objectId);

			if(item == null)
				return;

			int itemId = item.getItemId();
			if(itemId == 57)
				return;

			if(player.isFishing() && (itemId < 6535 || itemId > 6540))
			{
				// You cannot do anything else while fishing
				player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_DO_ANYTHING_ELSE_WHILE_FISHING));
				return;
			}

			if(player.isDead())// || PetDataTable.isFeedItem(item.getItemId()) && !player.getMountEngine().isMounted())
			{
				player.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addItemName(itemId));
				return;
			}

			if(player.isTradeInProgress())
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_PICK_UP_OR_USE_ITEMS_WHILE_TRADING));
				return;
			}

			if(item.isEquipable())
			{
				if(((item.getBodyPart() & L2Item.SLOT_R_HAND) == L2Item.SLOT_R_HAND || (item.getBodyPart() & L2Item.SLOT_LR_HAND) == L2Item.SLOT_LR_HAND) &&
						player.isStatActive(Stats.BLOCK_WEAPON))
				{
					player.sendPacket(Msg.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
					return;
				}

				if(player.isCastingNow())
				{
					player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_USE_EQUIPMENT_WHEN_USING_OTHER_SKILLS_OR_MAGIC));
					return;
				}

				// Нельзя снимать/одевать любое снаряжение при этих условиях
				if(player.isActionsBlocked() || player.isStunned() || player.isSleeping() || player.isParalyzed() || player.isAlikeDead())
				{
					player.sendActionFailed();
					return;
				}

				int bodyPart = item.getBodyPart();

				// Нельзя снимать/одевать оружие, сидя на пете
				if(player.getMountEngine().isMounted() && (bodyPart == L2Item.SLOT_LR_HAND || bodyPart == L2Item.SLOT_L_HAND || bodyPart == L2Item.SLOT_R_HAND))
					return;

				// Нельзя снимать/одевать проклятое оружие
				if(CursedWeaponsManager.getInstance().isCursed(itemId))
					return;

				if(player.isCombatFlagEquipped() && (bodyPart == L2Item.SLOT_LR_HAND || bodyPart == L2Item.SLOT_L_HAND || bodyPart == L2Item.SLOT_R_HAND))
					return;

				//На олимпиаде запрещаем использовать переточеные вещи. Актуально для пвп серверов.
				if(player.isInOlympiadMode() && (item.getEnchantLevel() > Config.MAXENCHANT_OLYPLAYER))
					return;

				L2ItemInstance weapon;

				// Equip or unEquip
				boolean isEquipped = item.isEquipped();
				if(isEquipped)
				{
					if(item.getItem().getItemType() == L2EtcItem.EtcItemType.ARROW || item.getItem().getItemType() == L2EtcItem.EtcItemType.BOLT)
						return;

					weapon = player.getActiveWeaponInstance();
					player.getInventory().unEquipItemAndSendChanges(item);
				}
				else
				{
					if(!item.checkEquipCondition(player))
						return;
					GArray<L2ItemInstance> items = player.getInventory().equipItemAndRecord(item);
					weapon = player.getActiveWeaponInstance();
					player.sendChanges();
					player.sendPacket(new InventoryUpdate(items));
				}

				if(isEquipped != item.isEquipped())
				{
					SystemMessage sm;
					if(isEquipped)
					{
						player.sendDisarmMessage(item);

						if(weapon != null && item == weapon)
						{
							if(item.getChargedSpiritshot() != L2ItemInstance.CHARGED_NONE)
								player.sendPacket(new SystemMessage(SystemMessage.POWER_OF_MANA_DISABLED));
							if(item.getChargedSoulshot() != L2ItemInstance.CHARGED_NONE)
								player.sendPacket(new SystemMessage(SystemMessage.POWER_OF_THE_SPIRITS_DISABLED));
							item.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
							item.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);

						}
					}
					else
					{
						if(item.getEnchantLevel() > 0)
						{
							sm = new SystemMessage(SystemMessage.EQUIPPED__S1_S2);
							sm.addNumber(item.getEnchantLevel());
							sm.addItemName(itemId);
						}
						else
							sm = new SystemMessage(SystemMessage.YOU_HAVE_EQUIPPED_YOUR_S1).addItemName(itemId);
						player.sendPacket(sm);

						if(weapon != null && item == weapon)
							player.AutoShot();
					}
				}

				player.unEquipInappropriateItems();
				player.updateStats();
				return;
			}

			if(itemId == 4393)
			{
				player.sendPacket(new ShowCalc(itemId));
				return;
			}
			if(ExtractableItems.useHandler(player, item))
				return;

			if(L2Manor.useHandler(player, item))
				return;

			ItemTable.useHandler(player, item);
		}
	}
}