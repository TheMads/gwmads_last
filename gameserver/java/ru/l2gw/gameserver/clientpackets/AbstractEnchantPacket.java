package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.commons.arrays.ArrayUtils;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.EnchantResult;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.gameserver.templates.L2Item.Grade;
import ru.l2gw.gameserver.templates.StatsSet;

import java.util.HashMap;
import java.util.Map;

/**
 * @author rage
 * @date 24.06.2010 10:42:05
 */
public abstract class AbstractEnchantPacket extends L2GameClientPacket
{
	public static final HashMap<Integer, EnchantScroll> scrolls = new HashMap<Integer, EnchantScroll>();
	public static final HashMap<Integer, EnchantItem> supports = new HashMap<Integer, EnchantItem>();
	public static final Map<Integer, StatsSet> customItems = new HashMap<>();

	public static class EnchantItem
	{
		protected final boolean _isWeapon;
		protected final Grade _grade;
		protected final int _maxEnchantLevel;
		protected final int _chanceAdd;
		protected final int[] _itemIds;

		public EnchantItem(boolean wep, Grade type, int level, int chance, int[] items)
		{
			_isWeapon = wep;
			_grade = type;
			_maxEnchantLevel = level;
			_chanceAdd = chance;
			_itemIds = items;
		}

		/*
		 * Return true if support item can be used for this item
		 */
		public final boolean isValid(L2ItemInstance enchantItem)
		{
			if(enchantItem == null)
				return false;

			int type2 = enchantItem.getItem().getType2();
			// checking scroll type and configured maximum enchant level
			switch(type2)
			{
				// weapon scrolls can enchant only weapons
				case L2Item.TYPE2_WEAPON:
					if(!_isWeapon || (Config.ENCHANT_MAX_WEAPON > 0 && enchantItem.getEnchantLevel() >= Config.ENCHANT_MAX_WEAPON && enchantItem.getItemId() != 13539 && !Config.OVER_ENCHANT_ENABLED))
						return false;
					break;
				// armor scrolls can enchant only accessory and armors
				case L2Item.TYPE2_SHIELD_ARMOR:
					if(_isWeapon || (Config.ENCHANT_MAX_ARMOR > 0 && enchantItem.getEnchantLevel() >= Config.ENCHANT_MAX_ARMOR && !Config.OVER_ENCHANT_ENABLED))
						return false;
					break;
				case L2Item.TYPE2_ACCESSORY:
					if(_isWeapon || (Config.ENCHANT_MAX_ACCESSORY > 0 && enchantItem.getEnchantLevel() >= Config.ENCHANT_MAX_ACCESSORY && !Config.OVER_ENCHANT_ENABLED))
						return false;
					break;
				default:
					return false;
			}

			// check for crystal types
			if(_grade.externalOrdinal != enchantItem.getCrystalType().externalOrdinal)
				return false;

			// check for maximum enchant level
			if(_maxEnchantLevel != 0 && enchantItem.getEnchantLevel() >= _maxEnchantLevel)
				return false;

			return _itemIds == null || ArrayUtils.contains(_itemIds, enchantItem.getItemId());
		}

		/*
		 * return chance increase
		 */
		public final int getChanceAdd()
		{
			return _chanceAdd;
		}
	}

	public static final class EnchantScroll extends EnchantItem
	{
		private final boolean _isBlessed;
		private final boolean _isCrystal;
		private final boolean _isSafe;

		public EnchantScroll(boolean wep, boolean bless, boolean crystal, boolean safe, Grade type, int level, int chance, int[] items)
		{
			super(wep, type, level, chance, items);

			_isBlessed = bless;
			_isCrystal = crystal;
			_isSafe = safe;
		}

		/*
		 * Return true for blessed scrolls
		 */
		public final boolean isBlessed()
		{
			return _isBlessed;
		}

		/*
		 * Return true for crystal scrolls
		 */
		public final boolean isCrystal()
		{
			return _isCrystal;
		}

		/*
		 * Return true for safe-enchant scrolls (enchant level will remain on failure)
		 */
		public final boolean isSafe()
		{
			return _isSafe;
		}

		public final boolean isValid(L2ItemInstance enchantItem, EnchantItem supportItem)
		{
			// blessed scrolls can't use support items
			return !(supportItem != null && (!supportItem.isValid(enchantItem) || isBlessed())) && isValid(enchantItem);

		}

		public final int getChance(L2ItemInstance enchantItem, EnchantItem supportItem)
		{
			if(!isValid(enchantItem, supportItem))
				return -1;

			if(customItems.containsKey(enchantItem.getItemId()))
			{
				StatsSet statsSet = customItems.get(enchantItem.getItemId());
				if(enchantItem.getEnchantLevel() < statsSet.getInteger("safe"))
					return 100;

				return statsSet.getInteger("chance");
			}

			boolean fullBody = enchantItem.getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR;
			if(enchantItem.getEnchantLevel() < Config.ENCHANT_SAFE_COMMON || (fullBody && enchantItem.getEnchantLevel() < Config.ENCHANT_SAFE_FULLBODY))
				return 100;

			boolean isAccessory = enchantItem.getItem().getType2() == L2Item.TYPE2_ACCESSORY;
			int chance;

			if(_isCrystal)
			{
				if(_isWeapon)
				{
					if(enchantItem.getItem().isMagicWeapon())
						chance = Config.ENCHANT_CHANCE_CRYSTAL_WEAPON_MAGIC;
					else
						chance = Config.ENCHANT_CHANCE_CRYSTAL_WEAPON;
				}
				else if(isAccessory)
					chance = Config.ENCHANT_CHANCE_CRYSTAL_ACCESSORY;
				else
					chance = Config.ENCHANT_CHANCE_CRYSTAL_ARMOR;
			}
			else if(_isBlessed)
			{
				// blessed scrolls does not use support items
				if(supportItem != null)
					return -1;

				if(_isWeapon)
				{
					if(enchantItem.getItem().isMagicWeapon())
						chance = Config.ENCHANT_CHANCE_BLESS_WEAPON_MAGIC;
					else
						chance = Config.ENCHANT_CHANCE_BLESS_WEAPON;
				}
				else if(isAccessory)
					chance = Config.ENCHANT_CHANCE_BLESS_ACCESSORY;
				else
					chance = Config.ENCHANT_CHANCE_BLESS_ARMOR;
			}
			else
			{
				if(_isWeapon)
				{
					if(enchantItem.getItem().isMagicWeapon())
						chance = Config.ENCHANT_CHANCE_WEAPON_MAGIC;
					else
						chance = Config.ENCHANT_CHANCE_WEAPON;
				}
				else if(isAccessory)
					chance = Config.ENCHANT_CHANCE_ACCESSORY;
				else
					chance = Config.ENCHANT_CHANCE_ARMOR;
			}

			chance += _chanceAdd;

			if(supportItem != null)
				chance += supportItem.getChanceAdd();

			return chance;
		}
	}

	static
	{
		// itemId, (isWeapon, isBlessed, isCrystal, isSafe, grade, max enchant level, chance increase, allowed item IDs)
		// allowed items list must be sorted by ascending order
		scrolls.put(729, new EnchantScroll(true, false, false, false, Grade.A, 0, 0, null));
		scrolls.put(730, new EnchantScroll(false, false, false, false, Grade.A, 0, 0, null));
		scrolls.put(731, new EnchantScroll(true, false, true, false, Grade.A, 0, 0, null));
		scrolls.put(732, new EnchantScroll(false, false, true, false, Grade.A, 0, 0, null));
		scrolls.put(947, new EnchantScroll(true, false, false, false, Grade.B, 0, 0, null));
		scrolls.put(948, new EnchantScroll(false, false, false, false, Grade.B, 0, 0, null));
		scrolls.put(949, new EnchantScroll(true, false, true, false, Grade.B, 0, 0, null));
		scrolls.put(950, new EnchantScroll(false, false, true, false, Grade.B, 0, 0, null));
		scrolls.put(951, new EnchantScroll(true, false, false, false, Grade.C, 0, 0, null));
		scrolls.put(952, new EnchantScroll(false, false, false, false, Grade.C, 0, 0, null));
		scrolls.put(953, new EnchantScroll(true, false, true, false, Grade.C, 0, 0, null));
		scrolls.put(954, new EnchantScroll(false, false, true, false, Grade.C, 0, 0, null));
		scrolls.put(955, new EnchantScroll(true, false, false, false, Grade.D, 0, 0, null));
		scrolls.put(956, new EnchantScroll(false, false, false, false, Grade.D, 0, 0, null));
		scrolls.put(957, new EnchantScroll(true, false, true, false, Grade.D, 0, 0, null));
		scrolls.put(958, new EnchantScroll(false, false, true, false, Grade.D, 0, 0, null));
		scrolls.put(959, new EnchantScroll(true, false, false, false, Grade.S, 0, 0, null));
		scrolls.put(960, new EnchantScroll(false, false, false, false, Grade.S, 0, 0, null));
		scrolls.put(961, new EnchantScroll(true, false, true, false, Grade.S, 0, 0, null));
		scrolls.put(962, new EnchantScroll(false, false, true, false, Grade.S, 0, 0, null));
		scrolls.put(6569, new EnchantScroll(true, true, false, false, Grade.A, 0, 0, null));
		scrolls.put(6570, new EnchantScroll(false, true, false, false, Grade.A, 0, 0, null));
		scrolls.put(6571, new EnchantScroll(true, true, false, false, Grade.B, 0, 0, null));
		scrolls.put(6572, new EnchantScroll(false, true, false, false, Grade.B, 0, 0, null));
		scrolls.put(6573, new EnchantScroll(true, true, false, false, Grade.C, 0, 0, null));
		scrolls.put(6574, new EnchantScroll(false, true, false, false, Grade.C, 0, 0, null));
		scrolls.put(6575, new EnchantScroll(true, true, false, false, Grade.D, 0, 0, null));
		scrolls.put(6576, new EnchantScroll(false, true, false, false, Grade.D, 0, 0, null));
		scrolls.put(6577, new EnchantScroll(true, true, false, false, Grade.S, 0, 0, null));
		scrolls.put(6578, new EnchantScroll(false, true, false, false, Grade.S, 0, 0, null));
		scrolls.put(22006, new EnchantScroll(true, false, false, false, Grade.D, 0, 10, null));
		scrolls.put(22007, new EnchantScroll(true, false, false, false, Grade.C, 0, 10, null));
		scrolls.put(22008, new EnchantScroll(true, false, false, false, Grade.B, 0, 10, null));
		scrolls.put(22009, new EnchantScroll(true, false, false, false, Grade.A, 0, 10, null));
		scrolls.put(22010, new EnchantScroll(false, false, false, false, Grade.D, 0, 10, null));
		scrolls.put(22011, new EnchantScroll(false, false, false, false, Grade.C, 0, 10, null));
		scrolls.put(22012, new EnchantScroll(false, false, false, false, Grade.B, 0, 10, null));
		scrolls.put(22013, new EnchantScroll(false, false, false, false, Grade.A, 0, 10, null));
		scrolls.put(22014, new EnchantScroll(true, false, false, true, Grade.B, 16, 10, null));
		scrolls.put(22015, new EnchantScroll(true, false, false, true, Grade.A, 16, 10, null));
		scrolls.put(22016, new EnchantScroll(false, false, false, true, Grade.B, 16, 10, null));
		scrolls.put(22017, new EnchantScroll(false, false, false, true, Grade.B, 16, 10, null));
		scrolls.put(22018, new EnchantScroll(true, false, false, false, Grade.B, 0, 100, null));
		scrolls.put(22019, new EnchantScroll(true, false, false, false, Grade.A, 0, 100, null));
		scrolls.put(22020, new EnchantScroll(false, false, false, false, Grade.B, 0, 100, null));
		scrolls.put(22021, new EnchantScroll(false, false, false, false, Grade.A, 0, 100, null));
		scrolls.put(22221, new EnchantScroll(true, false, false, true, Grade.S, 16, 0, null));
		scrolls.put(22222, new EnchantScroll(false, false, false, true, Grade.S, 6, 0, null));
		scrolls.put(22223, new EnchantScroll(true, false, false, true, Grade.A, 16, 0, null));
		scrolls.put(22224, new EnchantScroll(false, false, false, true, Grade.A, 6, 0, null));
		scrolls.put(22225, new EnchantScroll(true, false, false, true, Grade.B, 16, 0, null));
		scrolls.put(22226, new EnchantScroll(false, false, false, true, Grade.B, 6, 0, null));
		scrolls.put(22227, new EnchantScroll(true, false, false, true, Grade.C, 16, 0, null));
		scrolls.put(22228, new EnchantScroll(false, false, false, true, Grade.C, 6, 0, null));
		scrolls.put(22229, new EnchantScroll(true, false, false, true, Grade.D, 16, 0, null));
		scrolls.put(22230, new EnchantScroll(false, false, false, true, Grade.D, 6, 0, null));

		// Olf's T-shirt Enchant Scroll
		scrolls.put(21581, new EnchantScroll(false, false, false, false, Grade.NONE, 0, 0, new int[]{21706, 21580}));
		// Blessed Olf's T-shirt Enchant Scroll
		scrolls.put(21582, new EnchantScroll(false, true, false, false, Grade.NONE, 0, 0, new int[]{21706, 21580}));

		// Event - Herdsman's Love Scroll
		scrolls.put(15381, new EnchantScroll(false, false, false, false, Grade.NONE, 0, 0, new int[]{15383, 15384, 15385, 15386, 15387, 15388, 15389, 15390, 15391, 15392}));

		// Master Yogi's Scroll Enchant Weapon (event)
		scrolls.put(13540, new EnchantScroll(true, false, false, false, Grade.NONE, 0, 0, new int[]{13539}));

		// itemId, (isWeapon, grade, max enchant level, chance increase)
		supports.put(12362, new EnchantItem(true, Grade.D, 9, 20, null));
		supports.put(12363, new EnchantItem(true, Grade.C, 9, 18, null));
		supports.put(12364, new EnchantItem(true, Grade.B, 9, 15, null));
		supports.put(12365, new EnchantItem(true, Grade.A, 9, 12, null));
		supports.put(12366, new EnchantItem(true, Grade.S, 9, 10, null));
		supports.put(12367, new EnchantItem(false, Grade.D, 9, 35, null));
		supports.put(12368, new EnchantItem(false, Grade.C, 9, 27, null));
		supports.put(12369, new EnchantItem(false, Grade.B, 9, 23, null));
		supports.put(12370, new EnchantItem(false, Grade.A, 9, 18, null));
		supports.put(12371, new EnchantItem(false, Grade.S, 9, 15, null));
		supports.put(14702, new EnchantItem(true, Grade.D, 9, 20, null));
		supports.put(14703, new EnchantItem(true, Grade.C, 9, 18, null));
		supports.put(14704, new EnchantItem(true, Grade.B, 9, 15, null));
		supports.put(14705, new EnchantItem(true, Grade.A, 9, 12, null));
		supports.put(14706, new EnchantItem(true, Grade.S, 9, 10, null));
		supports.put(14707, new EnchantItem(false, Grade.D, 9, 35, null));
		supports.put(14708, new EnchantItem(false, Grade.C, 9, 27, null));
		supports.put(14709, new EnchantItem(false, Grade.B, 9, 23, null));
		supports.put(14710, new EnchantItem(false, Grade.A, 9, 18, null));
		supports.put(14711, new EnchantItem(false, Grade.S, 9, 15, null));
	}

	/**
	 * Return enchant template for scroll
	 */
	public static EnchantScroll getEnchantScroll(L2ItemInstance scroll)
	{
		return scrolls.get(scroll.getItemId());
	}

	/**
	 * Return enchant template for support item
	 */
	protected static EnchantItem getSupportItem(L2ItemInstance item)
	{
		return supports.get(item.getItemId());
	}

	/**
	 * Return true if item can be enchanted
	 */
	public static boolean isEnchantable(L2ItemInstance item)
	{
		// only items in inventory and equipped can be enchanted
		return item.getItem().isEnchantable() && (item.getLocation() == L2ItemInstance.ItemLocation.INVENTORY || item.getLocation() == L2ItemInstance.ItemLocation.PAPERDOLL);
	}

	public static void checkAndCancelEnchant(L2Player player)
	{
		if(player.getEnchantScroll() != null)
		{
			player.setEnchantScroll(null);
			player.setEnchantSupportItem(null);
			player.setEnchantStartTime(0);
			player.sendPacket(Msg.YOU_HAVE_CANCELLED_THE_ENCHANTING_PROCESS);
			player.sendPacket(new EnchantResult(2));
		}
	}
}
