package ru.l2gw.gameserver.clientpackets;

import gnu.trove.map.hash.TIntObjectHashMap;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.PcInventory;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.ExAttributeEnchantResult;
import ru.l2gw.gameserver.serverpackets.InventoryUpdate;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.templates.L2Item;

public class RequestExEnchantItemAttribute extends L2GameClientPacket
{
	// Format: chd
	private int _objectId;

	public final static int[] WEAPON_VALUES =
	{
		0,   // Level 1
		25,  // Level 2
		75,  // Level 3
		150, // Level 4
		175, // Level 5
		225, // Level 6
		300, // Level 7
		325, // Level 8
		375, // Level 9
		450, // Level 10
		475, // Level 11
		525, // Level 12
		600, // Level 13
		Integer.MAX_VALUE  // TODO: Higher stones
	};

	public final static int[] ARMOR_VALUES =
	{
		0,  // Level 1
		12, // Level 2
		30, // Level 3
		60, // Level 4
		72, // Level 5
		90, // Level 6
		120, // Level 7
		132, // Level 8
		150, // Level 9
		180, // Level 10
		192, // Level 11
		210, // Level 12
		240, // Level 13
		Integer.MAX_VALUE  // TODO: Higher stones
	};

	public final static TIntObjectHashMap<StoneInfo> _stoneLevels = new TIntObjectHashMap<>();
	static
	{
		// Stones
		_stoneLevels.put(9546, new StoneInfo(3, 0));
		_stoneLevels.put(9547, new StoneInfo(3, 1));
		_stoneLevels.put(9549, new StoneInfo(3, 2));
		_stoneLevels.put(9548, new StoneInfo(3, 3));
		_stoneLevels.put(9551, new StoneInfo(3, 4));
		_stoneLevels.put(9550, new StoneInfo(3, 5));
		// Crystals
		_stoneLevels.put(9552, new StoneInfo(6, 0));
		_stoneLevels.put(9553, new StoneInfo(6, 1));
		_stoneLevels.put(9555, new StoneInfo(6, 2));
		_stoneLevels.put(9554, new StoneInfo(6, 3));
		_stoneLevels.put(9557, new StoneInfo(6, 4));
		_stoneLevels.put(9556, new StoneInfo(6, 5));
		// Jewels
		_stoneLevels.put(9558, new StoneInfo(9, 0));
		_stoneLevels.put(9559, new StoneInfo(9, 1));
		_stoneLevels.put(9561, new StoneInfo(9, 2));
		_stoneLevels.put(9560, new StoneInfo(9, 3));
		_stoneLevels.put(9563, new StoneInfo(9, 4));
		_stoneLevels.put(9562, new StoneInfo(9, 5));
		// Energies
		_stoneLevels.put(9564, new StoneInfo(12, 0));
		_stoneLevels.put(9565, new StoneInfo(12, 1));
		_stoneLevels.put(9567, new StoneInfo(12, 2));
		_stoneLevels.put(9566, new StoneInfo(12, 3));
		_stoneLevels.put(9569, new StoneInfo(12, 4));
		_stoneLevels.put(9568, new StoneInfo(12, 5));
		// Roughores
		_stoneLevels.put(10521, new StoneInfo(3, 0));
		_stoneLevels.put(10522, new StoneInfo(3, 1));
		_stoneLevels.put(10524, new StoneInfo(3, 2));
		_stoneLevels.put(10523, new StoneInfo(3, 3));
		_stoneLevels.put(10526, new StoneInfo(3, 4));
		_stoneLevels.put(10525, new StoneInfo(3, 5));
	}

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

		if(player.isOutOfControl() || player.isActionsDisabled())
		{
			player.sendActionFailed();
			return;
		}

		if(_objectId == -1)
		{
			player.setEnchantScroll(null);
			player.sendPacket(new SystemMessage(SystemMessage.ELEMENTAL_POWER_ENCHANCER_USAGE_HAS_BEEN_CANCELLED));
			player.sendPacket(new ExAttributeEnchantResult(2));
			return;
		}

		final PcInventory inventory = player.getInventory();
		L2ItemInstance itemToEnchant = inventory.getItemByObjectId(_objectId);
		L2ItemInstance stone = player.getEnchantScroll();
		player.setEnchantScroll(null);

		if(itemToEnchant == null || stone == null || player.getEnchantStartTime() == 0 || player.getEnchantStartTime() + 3000 > System.currentTimeMillis())
		{
			player.setEnchantStartTime(0);
			player.sendActionFailed();
			return;
		}
		player.setEnchantStartTime(0);

		if(!canBeEnchanted(itemToEnchant))
		{
			player.setEnchantScroll(null);
			player.sendPacket(new ExAttributeEnchantResult(2));
			player.sendPacket(Msg.ELEMENTAL_POWER_ENCHANCER_USAGE_REQUIREMENT_IS_NOT_SUFFICIENT);
			player.sendActionFailed();
			return;
		}

		if(player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			player.sendPacket(new ExAttributeEnchantResult(2));
			player.sendPacket(Msg.YOU_CANNOT_ADD_ELEMENTAL_POWER_WHILE_OPERATING_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			player.sendActionFailed();
			return;
		}

		if((stone = inventory.getItemByObjectId(stone.getObjectId())) == null)
		{
			player.sendPacket(new ExAttributeEnchantResult(2));
			player.sendActionFailed();
			return;
		}

		int itemType = itemToEnchant.getItem().getType2();
		int maxValue = getMaxValue(stone, itemType != L2Item.TYPE2_WEAPON);
		int stoneElement = getAttributeElement(stone, itemType != L2Item.TYPE2_WEAPON);

		if(itemType == L2Item.TYPE2_SHIELD_ARMOR)
		{
			if(itemToEnchant.getAttributeElementValue(getAttributeElement(stone, false)) != 0)
			{
				player.sendPacket(new SystemMessage(SystemMessage.ANOTHER_ELEMENTAL_POWER_HAS_ALREADY_BEEN_ADDED_THIS_ELEMENTAL_POWER_CANNOT_BE_ADDED));
				player.sendPacket(new ExAttributeEnchantResult(2));
				player.sendActionFailed();
				return;
			}
		}
		else if(itemType == L2Item.TYPE2_WEAPON)
		{
			if(itemToEnchant.getAttackElement()[0] != L2Item.ATTRIBUTE_NONE && itemToEnchant.getAttackElement()[0] != stoneElement)
			{
				player.sendPacket(new SystemMessage(SystemMessage.ANOTHER_ELEMENTAL_POWER_HAS_ALREADY_BEEN_ADDED_THIS_ELEMENTAL_POWER_CANNOT_BE_ADDED));
				player.sendPacket(new ExAttributeEnchantResult(2));
				player.sendActionFailed();
				return;
			}
		}
		if(itemToEnchant.getAttributeElementValue(stoneElement) >= maxValue)
		{
			player.sendPacket(new ExAttributeEnchantResult(2));
			player.sendPacket(Msg.ELEMENTAL_POWER_ENCHANCER_USAGE_REQUIREMENT_IS_NOT_SUFFICIENT);
			player.sendActionFailed();
			return;
		}

		// Запрет на заточку чужих вещей, баг может вылезти на серверных лагах
		if(itemToEnchant.getOwnerId() != player.getObjectId() || stoneElement < 0)
		{
			player.setEnchantScroll(null);
			player.sendPacket(new ExAttributeEnchantResult(2));
			player.sendPacket(Msg.YOU_CANNOT_ADD_ELEMENTAL_POWER_WHILE_OPERATING_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			player.sendActionFailed();
			return;
		}

		int value = itemType == L2Item.TYPE2_WEAPON ? 5 : 6;//Like l2wh резиста поднимаеться на 6

		if(itemType == L2Item.TYPE2_WEAPON && itemToEnchant.getAttributeElementValue(stoneElement) == 0)
			value = 20;//Like goha first attribute give 20

		L2ItemInstance removedStone;
		synchronized(inventory)
		{
			removedStone = inventory.destroyItem("EnchantAttribute", stone.getObjectId(), 1, player, itemToEnchant);
		}

		if(removedStone == null)
		{
			player.sendPacket(new ExAttributeEnchantResult(2));
			player.sendActionFailed();
			return;
		}
			
		if(Rnd.chance(Config.ENCHANT_ATTRIBUTE_CHANCE))
		{
			if(itemToEnchant.getAttributeElementValue(stoneElement) == 0)
			{
				SystemMessage sm = new SystemMessage(SystemMessage.S2_ELEMENTAL_POWER_HAS_BEEN_ADDED_SUCCESSFULLY_TO_S1);
				sm.addItemName(itemToEnchant.getItemId());
				sm.addItemName(stone.getItemId());
				player.sendPacket(sm);
			}
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessage.S3_ELEMENTAL_POWER_HAS_BEEN_ADDED_SUCCESSFULLY_TO__S1S2);
				sm.addNumber(itemToEnchant.getAttributeElementValue(stoneElement));
				sm.addItemName(itemToEnchant.getItemId());
				sm.addItemName(stone.getItemId());
				player.sendPacket(sm);
			}

			itemToEnchant.changeAttributeElement("EnchantAttribute", stoneElement, itemToEnchant.getAttributeElementValue(stoneElement) + value, player, removedStone);
			player.getInventory().refreshItemListeners(itemToEnchant);
			player.sendPacket(new InventoryUpdate().addModifiedItem(itemToEnchant));
		}
		else
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_ADD_ELEMENTAL_POWER));

		player.setEnchantScroll(null);
		player.sendChanges();
		player.sendPacket(new ExAttributeEnchantResult(itemToEnchant.getAttributeElementValue(stoneElement)));
	}

	public static int getMaxValue(L2ItemInstance stone, boolean armor)
	{
		if(!_stoneLevels.containsKey(stone.getItemId()))
			return 0;

		return armor ? ARMOR_VALUES[_stoneLevels.get(stone.getItemId()).level] : WEAPON_VALUES[_stoneLevels.get(stone.getItemId()).level];
	}

	public static boolean canBeEnchanted(L2ItemInstance item)
	{
		return item.getItem().isElementable() && (item.getLocation() == L2ItemInstance.ItemLocation.INVENTORY || item.getLocation() == L2ItemInstance.ItemLocation.PAPERDOLL);
	}

	public static int getAttributeElement(L2ItemInstance stone, boolean armor)
	{
		StoneInfo stoneInfo = _stoneLevels.get(stone.getItemId());
		if(stoneInfo == null)
			return -2;

		return armor ? stoneInfo.element % 2 == 0 ? stoneInfo.element + 1 : stoneInfo.element - 1 : stoneInfo.element;
	}

	public static class StoneInfo
	{
		public final int level, element;

		public StoneInfo(int level, int element)
		{
			this.level = level;
			this.element = element;
		}
	}
}