package ru.l2gw.gameserver.model.instances;

import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.extensions.listeners.items.ItemEquipListener;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.extensions.scripts.Events;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.instancemanager.CursedWeaponsManager;
import ru.l2gw.gameserver.instancemanager.MercTicketManager;
import ru.l2gw.gameserver.instancemanager.QuestManager;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.base.EnchantOption;
import ru.l2gw.gameserver.model.base.ItemEnchantTemplate;
import ru.l2gw.gameserver.model.base.L2Augmentation;
import ru.l2gw.gameserver.model.quest.Quest;
import ru.l2gw.gameserver.model.quest.QuestState;
import ru.l2gw.gameserver.serverpackets.GetItem;
import ru.l2gw.gameserver.serverpackets.InventoryUpdate;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.skills.funcs.Func;
import ru.l2gw.gameserver.skills.funcs.FuncTemplate;
import ru.l2gw.gameserver.tables.GmListTable;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.tables.PetDataTable;
import ru.l2gw.gameserver.taskmanager.ItemsAutoDestroy;
import ru.l2gw.gameserver.templates.L2Armor;
import ru.l2gw.gameserver.templates.L2EtcItem;
import ru.l2gw.gameserver.templates.L2EtcItem.EtcItemType;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.gameserver.templates.L2Weapon;
import ru.l2gw.util.Location;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public final class L2ItemInstance extends L2Object
{
	private static final org.apache.commons.logging.Log _log = LogFactory.getLog(L2ItemInstance.class.getName());
	private static final org.apache.commons.logging.Log _logItems = LogFactory.getLog("item");

	/**
	 * Enumeration of locations for item
	 */
	public static enum ItemLocation
	{
		VOID,
		INVENTORY,
		PAPERDOLL,
		WAREHOUSE,
		CLANWH,
		PET,
		PET_EQUIP,
		FREIGHT,
		MARKET,
		MAILBOX
	}

	/**
	 * Item types to select
	 */
	public static enum ItemClass
	{
		/**
		 * List all deposited items
		 */
		ALL,
		/**
		 * Weapons, Armor, Jevels, Arrows, Baits
		 */
		EQUIPMENT,
		/**
		 * Soul/Spiritshot, Potions, Scrolls
		 */
		CONSUMABLE,
		/**
		 * Common craft matherials
		 */
		MATHERIALS,
		/**
		 * Special (item specific) craft matherials
		 */
		PIECES,
		/**
		 * Crafting recipies
		 */
		RECIPIES,
		/**
		 * Skill learn books
		 */
		SPELLBOOKS,
		/**
		 * Dyes, lifestones
		 */
		MISC,
		/**
		 * All other
		 */
		OTHER
	}

	/**
	 * ID of the owner
	 */
	private int _owner_id;

	/**
	 * Время жизни призрачных вещей *
	 */
	private ScheduledFuture<ConsumeManaTask> _consumeManaTask;
	private ScheduledFuture<ExpireTask> _expireTask;
	private int _mana;
	private long _expireTime;

	/**
	 * Quantity of the item
	 */
	private long _count;

	/**
	 * ID of the item
	 */
	private int _itemId;

	/**
	 * Object L2Item associated to the item
	 */
	private L2Item _itemTemplate;

	/**
	 * Location of the item
	 */
	private ItemLocation _loc;

	/**
	 * Slot where item is stored
	 */
	private short _loc_data;

	/**
	 * Level of enchantment of the item
	 */
	private int _enchantLevel;

	/**
	 * Price of the item for selling
	 */
	private long _price_sell;

	/**
	 * Price of the item for buying
	 */
	private long _price_buy;

	private long _count_sell;

	private L2Augmentation _augmentation = null;
	int _augmentationId;

	/**
	 * Custom item types (used loto, race tickets)
	 */
	private int _type1;
	private int _type2;

	/**
	 * Item drop time for autodestroy task
	 */
	private long _dropTime;

	/**
	 * Item drop time
	 */
	private long _dropTimeOwner;

	/**
	 * owner of the dropped item
	 */
	private WeakReference<L2Player> itemDropOwner;

	public static final byte CHARGED_NONE = 0;
	public static final byte CHARGED_SOULSHOT = 1;
	public static final byte CHARGED_SPIRITSHOT = 1;
	public static final byte CHARGED_BLESSED_SPIRITSHOT = 2;

	private byte _chargedSoulshot = CHARGED_NONE;
	private byte _chargedSpiritshot = CHARGED_NONE;

	private boolean _chargedFishtshot = false;

	public static final byte UNCHANGED = 0;
	public static final byte ADDED = 1;
	public static final byte REMOVED = 3;
	public static final byte MODIFIED = 2;
	private byte _lastChange = 2; //1 ??, 2 modified, 3 removed
	private boolean _existsInDb; // if a record exists in DB.
	private boolean _storedInDb; // if DB data is up-to-date.

	/** Elements Values **/
	private int _enchantAttributeFireValue = 0;
	private int _enchantAttributeWaterValue = 0;
	private int _enchantAttributeWindValue = 0;
	private int _enchantAttributeEarthValue = 0;
	private int _enchantAttributeHolyValue = 0;
	private int _enchantAttributeDarkValue = 0;

	/**
	 * Спецфлаги для конкретного инстанса
	 */
	private int _customFlags = 0;

	public static final int FLAG_NO_DROP = 1;
	public static final int FLAG_NO_TRADE = 2;
	public static final int FLAG_NO_TRANSFER = 4;
	public static final int FLAG_NO_CRYSTALLIZE = 8;
	public static final int FLAG_NO_ENCHANT = 16;
	public static final int FLAG_NO_DESTROY = 32;
	public static final int FLAG_PET_INVENTORY = 64;
	public static final int FLAG_NO_PKDROP = 512;//Donate Flag

	@SuppressWarnings("unchecked")
	private ScheduledFuture<?> _lazyUpdateInDb;

	private int _visualId;
	/**
	 * Task of delayed update item info in database
	 */
	public class LazyUpdateInDb implements Runnable
	{
		public void run()
		{
			updateInDb();
			_lazyUpdateInDb = null;
		}
	}

	// Для магазинов с ограниченным количеством предметов
	private long _maxCountToSell;
	private int _lastRechargeTime;
	private int _rechargeTime;

	private int _bodypart;

	private boolean _whflag = false;

	private int _deffFire = 0;
	private int _deffWind = 0;
	private int _deffWater = 0;
	private int _deffEarth = 0;
	private int _deffDark = 0;
	private int _deffHoly = 0;

	/**
	 * Constructor of the L2ItemInstance from the objectId and the itemId.
	 *
	 * @param objectId : int designating the ID of the object in the world
	 * @param itemId   : int designating the ID of the item
	 */
	public L2ItemInstance(int objectId, int itemId)
	{
		super(objectId);
		setItemId(itemId);
		if(getItemId() == 0 || _itemTemplate == null)
		{
			_log.warn("Not found template for item id: " + getItemId());
			throw new IllegalArgumentException();
		}
		_count = 1;
		_loc = ItemLocation.VOID;
		_type1 = 0;
		_type2 = 0;
		_dropTime = 0;
		_dropTimeOwner = 0;
		setItemDropOwner(null);

		if(_itemTemplate.getDefaultEcnhantVal() > 0)
			_enchantLevel = _itemTemplate.getDefaultEcnhantVal();

		_mana = _itemTemplate.getDurability();
		_expireTime = _itemTemplate.isTemporal() ? System.currentTimeMillis() + _itemTemplate.getPeriod() * 1000L : 0;

		_bodypart = _itemTemplate.getBodyPart();

		if(_itemTemplate.getAttachedFuncs() != null)
			for(FuncTemplate f : _itemTemplate.getAttachedFuncs())
				if(f._stat == Stats.FIRE_ATTRIBUTE)
					_deffFire = (int) f._value;
				else if(f._stat == Stats.WIND_ATTRIBUTE)
					_deffWind = (int) f._value;
				else if(f._stat == Stats.WATER_ATTRIBUTE)
					_deffWater = (int) f._value;
				else if(f._stat == Stats.EARTH_ATTRIBUTE)
					_deffEarth = (int) f._value;
				else if(f._stat == Stats.DARK_ATTRIBUTE)
					_deffDark = (int) f._value;
				else if(f._stat == Stats.HOLY_ATTRIBUTE)
					_deffHoly = (int) f._value;
	}

	/**
	 * Constructor of the L2ItemInstance from the objetId and the description of the item given by the L2Item.
	 *
	 * @param objectId : int designating the ID of the object in the world
	 * @param item	 : L2Item containing informations of the item
	 */
	public L2ItemInstance(int objectId, L2Item item)
	{
		super(objectId);
		_itemId = item.getItemId();
		_itemTemplate = item;
		if(getItemId() == 0)
			throw new IllegalArgumentException();
		_count = 1;
		_loc = ItemLocation.VOID;

		_dropTime = 0;
		_dropTimeOwner = 0;
		setItemDropOwner(null);

		if(_itemTemplate.getDefaultEcnhantVal() > 0)
			_enchantLevel = _itemTemplate.getDefaultEcnhantVal();

		_mana = _itemTemplate.getDurability();
		_expireTime = _itemTemplate.isTemporal() ? System.currentTimeMillis() + _itemTemplate.getPeriod() * 1000L : 0;

		_bodypart = _itemTemplate.getBodyPart();

		if(_itemTemplate.getAttachedFuncs() != null)
			for(FuncTemplate f : _itemTemplate.getAttachedFuncs())
				if(f._stat == Stats.FIRE_ATTRIBUTE)
					_deffFire = (int) f._value;
				else if(f._stat == Stats.WIND_ATTRIBUTE)
					_deffWind = (int) f._value;
				else if(f._stat == Stats.WATER_ATTRIBUTE)
					_deffWater = (int) f._value;
				else if(f._stat == Stats.EARTH_ATTRIBUTE)
					_deffEarth = (int) f._value;
				else if(f._stat == Stats.DARK_ATTRIBUTE)
					_deffDark = (int) f._value;
				else if(f._stat == Stats.HOLY_ATTRIBUTE)
					_deffHoly = (int) f._value;
	}

	public void consumeMana(int mp, L2Player player)
	{
		if(!isShadowItem())
			return;

		setMana(player, getMana() - mp);

		InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(this);
		player.sendPacket(iu);
		if(!taskStartCheck)
			checkMana();
	}

	/**
	 * удаляет все ссылки и останавливает все таймеры без сохранения
	 */
	public void prepareRemove()
	{
		setItemDropOwner(null);
		if(_consumeManaTask != null)
		{
			_consumeManaTask.cancel(false);
			_consumeManaTask = null;
		}
		if(_expireTask != null)
		{
			_expireTask.cancel(false);
			_expireTask = null;
		}
	}

	public int getBodyPart()
	{
		return _bodypart;
	}

	public void setBodyPart(int bodypart)
	{
		_bodypart = bodypart;
	}

	/**
	 * Sets the ownerID of the item
	 *
	 * @param owner_id : int designating the ID of the owner
	 */
	public void setOwnerId(int owner_id)
	{
		if(owner_id == _owner_id)
			return;
		_owner_id = owner_id;
		_storedInDb = false;
	}

	/**
	 * Sets the ownerID of the item
	 *
	 * @param process   : String Identifier of process triggering this action
	 * @param owner_id  : int designating the ID of the owner
	 * @param creator   : L2PcInstance Player requesting the item creation
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void setOwnerId(String process, int owner_id, L2Player creator, L2Object reference)
	{
		setOwnerId(owner_id);

		List<Object> param = new ArrayList<Object>();
		param.add("CHANGE:" + process);
		param.add(this);
		if(creator != null)
			param.add(creator);
		if(reference != null)
			param.add(reference);
		_logItems.info(param);
	}

	public L2Player getOwner()
	{
		return L2ObjectsStorage.getPlayer(_owner_id);
	}

	/**
	 * Returns the ownerID of the item
	 *
	 * @return int : ownerID of the item
	 */
	public int getOwnerId()
	{
		return _owner_id;
	}

	/**
	 * Sets the location of the item
	 *
	 * @param loc : ItemLocation (enumeration)
	 */
	public void setLocation(ItemLocation loc)
	{
		setLocation(loc, (short) 0);
	}

	/**
	 * Sets the location of the item.<BR><BR>
	 * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
	 *
	 * @param loc	  : ItemLocation (enumeration)
	 * @param loc_data : int designating the slot where the item is stored or the village for freights
	 */
	public void setLocation(ItemLocation loc, short loc_data)
	{
		if(loc == _loc && loc_data == _loc_data)
			return;
		_loc = loc;
		_loc_data = loc_data;
		_storedInDb = false;
	}

	public ItemLocation getLocation()
	{
		return _loc;
	}

	public void changeLocation(String process, ItemLocation loc, L2Player actor, L2Object reference)
	{
		ItemLocation oldLoc = _loc;
		setLocation(loc);

		if(oldLoc != loc && process != null)
		{
			List<Object> param = new ArrayList<Object>();
			param.add("CHANGE:" + process);
			param.add(this);
			if(actor != null)
				param.add(actor);
			if(reference != null)
				param.add(reference);
			param.add(oldLoc);
			param.add(_loc);
			_logItems.info(param);
		}
	}

	/**
	 * Возвращает количество предметов без приведения к int
	 * По возможности следует использовать именно его
	 *
	 * @return long
	 */
	public long getCount()
	{
		return _count;
	}

	/**
	 * Sets the quantity of the item.<BR><BR>
	 * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
	 *
	 * @param count : long
	 */
	public void setCount(long count)
	{
		if(count < 0)
			count = 0;
		if(!isStackable() && count > 1)
		{
			_count = 1;
			GmListTable.broadcastMessageToGMs(getPlayer() + " tried to stack unstackable item " + getItemId());
			return;
		}
		if(_count == count)
			return;
		_count = count;
		_storedInDb = false;
	}

	/**
	 * Returns if item is equipable
	 *
	 * @return boolean
	 */
	public boolean isEquipable()
	{
		return _itemTemplate.getItemType() == EtcItemType.BAIT || _itemTemplate.getItemType() == EtcItemType.ARROW || _itemTemplate.getItemType() == EtcItemType.BOLT || !(getBodyPart() == 0 || _itemTemplate instanceof L2EtcItem);
	}

	/**
	 * Returns if item is equipped
	 *
	 * @return boolean
	 */
	public boolean isEquipped()
	{
		return _loc == ItemLocation.PAPERDOLL || _loc == ItemLocation.PET_EQUIP;
	}

	/**
	 * Returns the slot where the item is stored
	 *
	 * @return int
	 */
	public short getEquipSlot()
	{
		return _loc_data;
	}

	/**
	 * Returns the characteristics of the item
	 *
	 * @return L2Item
	 */
	public L2Item getItem()
	{
		return _itemTemplate;
	}

	public int getCustomType1()
	{
		return _type1;
	}

	public int getCustomType2()
	{
		return _type2;
	}

	public void setCustomType1(int newtype)
	{
		_type1 = newtype;
	}

	public void setCustomType2(int newtype)
	{
		_type2 = newtype;
	}

	public void setDropTime(long time)
	{
		_dropTime = time;
	}

	public long getDropTime()
	{
		return _dropTime;
	}

	public void setDropTimeOwner(long time)
	{
		_dropTimeOwner = time;
	}

	public long getDropTimeOwner()
	{
		return _dropTimeOwner;
	}

	public void setItemDropOwner(L2Player owner)
	{
		itemDropOwner = owner == null ? null : new WeakReference<L2Player>(owner);
	}

	public L2Player getItemDropOwner()
	{
		if(itemDropOwner == null)
			return null;

		L2Player p = itemDropOwner.get();
		if(p == null)
			itemDropOwner = null;

		return p;
	}

	public boolean isCanBePickuped(L2Playable pickuper)
	{
		L2Player player = pickuper.getPlayer();
		return player != null && (_dropTimeOwner + Config.NONOWNER_ITEM_PICKUP_DELAY * 1000 < System.currentTimeMillis() || getItemDropOwner() == null || getItemDropOwner() == player || (player.getParty() != null && player.getParty().containsMember(getItemDropOwner())));
	}

	/**
	 * Returns the type of item
	 *
	 * @return Enum
	 */
	@SuppressWarnings("unchecked")
	public Enum<?> getItemType()
	{
		return _itemTemplate.getItemType();
	}

	/**
	 * Returns the ID of the item
	 *
	 * @return int
	 */
	public int getItemId()
	{
		return _itemId;
	}

	/**
	 * Returns the reference price of the item
	 *
	 * @return int
	 */
	public long getReferencePrice()
	{
		return _itemTemplate.getReferencePrice();
	}

	/**
	 * Returns the price of the item for selling
	 *
	 * @return int
	 */
	public long getPriceToSell()
	{
		return _price_sell;
	}

	/**
	 * Sets the price of the item for selling
	 * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
	 *
	 * @param price : int designating the price
	 */
	public void setPriceToSell(long price)
	{
		_price_sell = price;
		_storedInDb = false;
	}

	/**
	 * Returns the price of the item for buying
	 *
	 * @return int
	 */
	public long getPriceToBuy()
	{
		return _price_buy;
	}

	/**
	 * Sets the price of the item for buying
	 * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
	 *
	 * @param price : int
	 */
	public void setPriceToBuy(long price)
	{
		_price_buy = price;
		_storedInDb = false;
	}

	public void setCountToSell(long count)
	{
		_count_sell = count;
	}

	public long getCountToSell()
	{
		return _count_sell;
	}

	public void setMaxCountToSell(long count)
	{
		_maxCountToSell = count;
	}

	public long getMaxCountToSell()
	{
		return _maxCountToSell;
	}

	/**
	 * Устанавливает время последнего респауна предмета, используется в NPC магазинах с ограниченным количеством.
	 *
	 * @param lastRechargeTime : unixtime в минутах
	 */
	public void setLastRechargeTime(int lastRechargeTime)
	{
		_lastRechargeTime = lastRechargeTime;
	}

	/**
	 * Возвращает время последнего респауна предмета, используется в NPC магазинах с ограниченным количеством.
	 *
	 * @return unixtime в минутах
	 */
	public int getLastRechargeTime()
	{
		return _lastRechargeTime;
	}

	/**
	 * Устанавливает время респауна предмета, используется в NPC магазинах с ограниченным количеством.
	 *
	 * @param rechargeTime : unixtime в минутах
	 */
	public void setRechargeTime(int rechargeTime)
	{
		_rechargeTime = rechargeTime;
	}

	/**
	 * Возвращает время респауна предмета, используется в NPC магазинах с ограниченным количеством.
	 *
	 * @return unixtime в минутах
	 */
	public int getRechargeTime()
	{
		return _rechargeTime;
	}

	/**
	 * Возвращает ограничен ли этот предмет в количестве, используется в NPC магазинах с ограниченным количеством.
	 *
	 * @return true, если ограничен
	 */
	public boolean isCountLimited()
	{
		return _maxCountToSell > 0;
	}

	/**
	 * Returns the last change of the item
	 *
	 * @return int
	 */
	public int getLastChange()
	{
		return _lastChange;
	}

	/**
	 * Sets the last change of the item
	 *
	 * @param lastChange : int
	 */
	public void setLastChange(byte lastChange)
	{
		_lastChange = lastChange;
	}

	/**
	 * Returns if item is stackable
	 *
	 * @return boolean
	 */
	public boolean isStackable()
	{
		return _itemTemplate.isStackable();
	}

	@Override
	public void onAction(L2Player player, boolean dontMove)
	{
		if(!dontMove && Events.onAction(player, this))
			return;
		else if(dontMove && Events.onActionShift(player, this))
			return;

		// this causes the validate position handler to do the pickup if the location is reached.
		// mercenary tickets can only be picked up by the castle owner.

		int _castleId = MercTicketManager.getInstance().getTicketCastleId(getItemId());

		if(_castleId > 0)
		{
			if((player.getClanPrivileges() & L2Clan.CP_CS_MERCENARIES) == L2Clan.CP_CS_MERCENARIES)
			{
				if(player.isInParty())
					player.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.instances.L2ItemInstance.NoMercInParty", player));
				else if(!dontMove)
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_PICK_UP, this);
				else
					player.sendActionFailed();
			}
			else
				player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_THE_AUTHORITY_TO_CANCEL_MERCENARY_POSITIONING));

			player.setTarget(this);
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, this, null);
			// Send a Server->Client ActionFailed to the L2Player in order to avoid that the client wait another packet
			player.sendActionFailed();
		}
		else if(!dontMove)
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_PICK_UP, this, null);
		else
			player.sendActionFailed();
	}

	/**
	 * Returns the level of enchantment of the item
	 *
	 * @return int
	 */
	public int getEnchantLevel()
	{
		return _enchantLevel;
	}

	/**
	 * Sets the level of enchantment of the item
	 *
	 * @param enchantLevel level of enchant
	 */
	public void setEnchantLevel(int enchantLevel)
	{
		if(_enchantLevel == enchantLevel)
			return;
		_enchantLevel = enchantLevel;
		_storedInDb = false;
	}

	public void changeEnchantLevel(String process, int enchantLevel, L2Player actor, L2Object reference)
	{
		if(_enchantLevel == enchantLevel)
			return;

		int enchant = _enchantLevel;
		setEnchantLevel(enchantLevel);

		if(process != null)
		{
			List<Object> param = new ArrayList<Object>();
			param.add("CHANGE:" + process);
			param.add(this);
			if(actor != null)
				param.add(actor);
			if(reference != null)
				param.add(reference);
			param.add(enchant);
			_logItems.info(param);
		}
	}

	/**
	 * Returns false cause item can't be attacked
	 *
	 * @return boolean false
	 */
	@Override
	public boolean isAttackable(@SuppressWarnings("unused") L2Character attacker, boolean forceUse, boolean sendMessage)
	{
		return false;
	}

	/**
	 * Returns whether this item is augmented or not
	 *
	 * @return true if augmented
	 */
	public boolean isAugmented()
	{
		return _augmentation != null;
	}

	/**
	 * Returns the augmentation object for this item
	 *
	 * @return augmentation
	 */
	public L2Augmentation getAugmentation()
	{
		return _augmentation;
	}

	public int getAugmentationId()
	{
		return _augmentationId;
	}

	/**
	 * Sets a new augmentation
	 *
	 * @param augmentation
	 * @return return true if successfully
	 */
	public boolean setAugmentation(L2Augmentation augmentation)
	{
		// there shall be no previous augmentation..
		if(_augmentation != null)
			return false;
		_augmentation = augmentation;
		_augmentationId = augmentation.getAugmentationId();
		setLastChange(MODIFIED);
		return true;
	}

	/**
	 * Remove the augmentation
	 */
	public void removeAugmentation()
	{
		if(_augmentation == null)
			return;
		_augmentation.deleteAugmentationData();
		_augmentationId = 0;
		_augmentation = null;
		setLastChange(MODIFIED);
	}

	/**
	 * Returns the type of charge with SoulShot of the item.
	 *
	 * @return int (CHARGED_NONE, CHARGED_SOULSHOT)
	 */
	public byte getChargedSoulshot()
	{
		return _chargedSoulshot;
	}

	/**
	 * Returns the type of charge with SpiritShot of the item
	 *
	 * @return int (CHARGED_NONE, CHARGED_SPIRITSHOT, CHARGED_BLESSED_SPIRITSHOT)
	 */
	public byte getChargedSpiritshot()
	{
		return _chargedSpiritshot;
	}

	public boolean getChargedFishshot()
	{
		return _chargedFishtshot;
	}

	/**
	 * Sets the type of charge with SoulShot of the item
	 *
	 * @param type : int (CHARGED_NONE, CHARGED_SOULSHOT)
	 */
	public void setChargedSoulshot(byte type)
	{
		_chargedSoulshot = type;
	}

	/**
	 * Sets the type of charge with SpiritShot of the item
	 *
	 * @param type : int (CHARGED_NONE, CHARGED_SPIRITSHOT, CHARGED_BLESSED_SPIRITSHOT)
	 */
	public void setChargedSpiritshot(byte type)
	{
		_chargedSpiritshot = type;
	}

	public void setChargedFishshot(boolean type)
	{
		_chargedFishtshot = type;
	}

	protected GArray<FuncTemplate> _funcTemplates;

	public void attachFunction(FuncTemplate f)
	{
		if(_funcTemplates == null)
			_funcTemplates = new GArray<FuncTemplate>(1);

		_funcTemplates.add(f);
	}

	public void deattachFunction(FuncTemplate f)
	{
		if(_funcTemplates != null)
			_funcTemplates.remove(f);
	}

	/**
	 * This function basically returns a set of functions from
	 * L2Item/L2Armor/L2Weapon, but may add additional
	 * functions, if this particular item instance is enhanched
	 * for a particular player.
	 *
	 * @param cha : L2Character designating the player
	 * @return Func[]
	 */
	public Func[] getStatFuncs(L2Character cha)
	{
		ArrayList<Func> funcs = new ArrayList<Func>();
		if(_itemTemplate.getAttachedFuncs() != null)
			for(FuncTemplate t : _itemTemplate.getAttachedFuncs())
			{
				Env env = new Env();
				env.character = cha;
				env.item = this;
				Func f = t.getFunc(env, this);
				if(f != null)
					funcs.add(f);
			}

		ItemEnchantTemplate enchantTemplate = _itemTemplate.getEnchantOptions();
		if(enchantTemplate != null)
		{
			EnchantOption[] options = enchantTemplate.getEnchantOption(_enchantLevel);
			if(options != null)
			{
				for(EnchantOption option : options)
				{
					for(FuncTemplate t : option.getFunctions())
					{
						Env env = new Env();
						env.character = cha;
						env.item = this;
						Func f = t.getFunc(env, this);
						if(f != null)
							funcs.add(f);
					}
				}
			}
		}

		if(_funcTemplates != null)
			for(FuncTemplate t : _funcTemplates)
			{
				Env env = new Env();
				env.character = cha;
				env.item = this;
				Func f = t.getFunc(env, this);
				if(f != null)
					funcs.add(f);
			}
		if(funcs.size() == 0)
			return new Func[0];
		return funcs.toArray(new Func[funcs.size()]);
	}

	/**
	 * Updates database.<BR><BR>
	 * <U><I>Concept : </I></U><BR>
	 * <p/>
	 * <B>IF</B> the item exists in database :
	 * <UL>
	 * <LI><B>IF</B> the item has no owner, or has no location, or has a null quantity : remove item from database</LI>
	 * <LI><B>ELSE</B> : update item in database</LI>
	 * </UL>
	 * <p/>
	 * <B> Otherwise</B> :
	 * <UL>
	 * <LI><B>IF</B> the item hasn't a null quantity, and has a correct location, and has a correct owner : insert item in database</LI>
	 * </UL>
	 */
	public void updateDatabase()
	{
		updateDatabase(false);
	}

	public synchronized void updateDatabase(boolean commit)
	{
		if(_existsInDb)
		{
			if(_owner_id == 0 || _loc == ItemLocation.VOID || _count == 0)
				removeFromDb();
			else if(Config.LAZY_ITEM_UPDATE && isStackable())
			{
				if(commit)
				{
					// cancel lazy update task if need
					if(_lazyUpdateInDb != null)
					{
						_lazyUpdateInDb.cancel(true);
						_lazyUpdateInDb = null;
					}
					updateInDb();
					L2World.increaseUpdateItemCount();
				}
				else if(_lazyUpdateInDb == null)
				{
					_lazyUpdateInDb = ThreadPoolManager.getInstance().scheduleGeneral(new LazyUpdateInDb(), Config.LAZY_ITEM_UPDATE_TIME);
					L2World.increaseLazyUpdateItem();
				}
			}
			else
			{
				updateInDb();
				L2World.increaseUpdateItemCount();
			}
		}
		else
		{
			if(_count == 0)
				return;
			if(_loc == ItemLocation.VOID || _owner_id == 0)
				return;
			insertIntoDb();
		}
	}

	/**
	 * Returns a L2ItemInstance stored in database from its objectID
	 *
	 * @param objectId : int designating the objectID of the item
	 * @return L2ItemInstance
	 */
	public static L2ItemInstance restoreFromDb(int objectId)
	{
		L2ItemInstance inst = null;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM items WHERE object_id = ? LIMIT 1");
			statement.setInt(1, objectId);
			rset = statement.executeQuery();
			if(rset.next())
				inst = restoreFromDb(rset);

		}
		catch(Exception e)
		{
			_log.error("Could not restore item " + objectId + " from DB: " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return inst;
	}

	public static L2ItemInstance restoreFromDb(int ownerId, int itemId, ItemLocation itemLoc)
	{
		L2ItemInstance inst = null;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM items WHERE owner_id = ? and item_id = ? and loc = ? LIMIT 1");
			statement.setInt(1, ownerId);
			statement.setInt(2, itemId);
			statement.setString(3, itemLoc.name());
			rset = statement.executeQuery();
			if(rset.next())
				inst = restoreFromDb(rset);
		}
		catch(Exception e)
		{
			_log.error("Could not restore item " + itemId + " from DB: " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return inst;
	}

	public static L2ItemInstance restoreFromDb(ResultSet rset)
	{
		L2ItemInstance inst = null;
		Connection con = null;
		PreparedStatement statement2 = null;
		ResultSet rset2 = null;
		L2Item item;
		try
		{
			if(rset != null)
			{
				int object_id = rset.getInt("object_id");
				int owner_id = rset.getInt("owner_id");
				int item_id = rset.getInt("item_id");
				item = ItemTable.getInstance().getTemplate(item_id);

				if(item == null)
				{
					_log.warn("Item item_id=" + item_id + " not known, object_id=" + object_id);
					return null;
				}

				inst = new L2ItemInstance(object_id, item);
				inst._existsInDb = true;
				inst._storedInDb = true;
				inst._owner_id = owner_id;
				inst._count = rset.getLong("count");
				inst._enchantLevel = rset.getInt("enchant_level");
				inst._type1 = rset.getInt("custom_type1");
				inst._type2 = rset.getInt("custom_type2");
				inst._loc = ItemLocation.valueOf(rset.getString("loc"));
				inst._loc_data = rset.getShort("loc_data");
				inst._price_sell = rset.getLong("price_sell");
				inst._price_buy = rset.getLong("price_buy");
				inst._mana = rset.getInt("mana_left");
				inst._expireTime = rset.getLong("expire_time");
				inst._customFlags = rset.getInt("flags");

				inst.setAttributeElement(rset.getInt("fire_val"), rset.getInt("water_val"), rset.getInt("wind_val"), rset.getInt("earth_val"), rset.getInt("holy_val"), rset.getInt("dark_val"), false);
				inst.setOwnerId(owner_id);

				if(inst.isTemporalItem() && inst._expireTime <= System.currentTimeMillis())
				{
					inst.removeFromDb();
					return null;
				}

				//load augmentation
				if(item instanceof L2Weapon || item instanceof L2Armor)
				{
					con = DatabaseFactory.getInstance().getConnection();
					statement2 = con.prepareStatement("SELECT attributes, mineral FROM augmentations WHERE item_id=? LIMIT 1");
					statement2.setInt(1, object_id);
					rset2 = statement2.executeQuery();
					if(rset2.next())
						inst.setAugmentation(new L2Augmentation(inst, rset2.getInt("attributes"), rset2.getInt("mineral"), false));
				}
			}
			else
			{
				_log.warn("Restore items: rset is null!!");
				return null;
			}
		}
		catch(Exception e)
		{
			_log.error("Could not restore item from DB: " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement2, rset2);
		}
		return inst;
	}

	/**
	 * Update the database with values of the item
	 */
	void updateInDb()
	{
		if(_storedInDb)
			return;

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE items SET owner_id=?,count=?,loc=?,loc_data=?,enchant_level=?,fire_val=?,water_val=?,wind_val=?,earth_val=?,holy_val=?,dark_val=?,price_sell=?,price_buy=?,custom_type1=?,custom_type2=?,mana_left=?,expire_time=?,item_id=?,flags=? WHERE object_id = ?");
			statement.setInt(1, _owner_id);
			statement.setLong(2, _count);
			statement.setString(3, _loc.name());
			statement.setInt(4, _loc_data);
			statement.setInt(5, getEnchantLevel());
			statement.setInt(6, _enchantAttributeFireValue);
			statement.setInt(7, _enchantAttributeWaterValue);
			statement.setInt(8, _enchantAttributeWindValue);
			statement.setInt(9, _enchantAttributeEarthValue);
			statement.setInt(10, _enchantAttributeHolyValue);
			statement.setInt(11, _enchantAttributeDarkValue);
			statement.setLong(12, _price_sell);
			statement.setLong(13, _price_buy);
			statement.setInt(14, getCustomType1());
			statement.setInt(15, getCustomType2());
			statement.setInt(16, _mana);
			statement.setLong(17, _expireTime);
			statement.setInt(18, getItemId());
			statement.setInt(19, _customFlags);
			statement.setInt(20, getObjectId());
			statement.executeUpdate();

			_existsInDb = true;
			_storedInDb = true;
		}
		catch(Exception e)
		{
			_log.error("Could not update item " + getObjectId() + " itemID " + getItemId() + " count " + getCount() + " owner " + _owner_id + " in DB:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	/**
	 * Insert the item in database
	 */
	private void insertIntoDb()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO items (owner_id,item_id,count,loc,loc_data,enchant_level,fire_val,water_val,wind_val,earth_val,holy_val,dark_val,price_sell,price_buy,object_id,custom_type1,custom_type2,mana_left,expire_time,class) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, _owner_id);
			statement.setInt(2, getItemId());
			statement.setLong(3, _count);
			statement.setString(4, _loc.name());
			statement.setInt(5, _loc_data);
			statement.setInt(6, getEnchantLevel());
			statement.setInt(7, _enchantAttributeFireValue);
			statement.setInt(8, _enchantAttributeWaterValue);
			statement.setInt(9, _enchantAttributeWindValue);
			statement.setInt(10, _enchantAttributeEarthValue);
			statement.setInt(11, _enchantAttributeHolyValue);
			statement.setInt(12, _enchantAttributeDarkValue);
			statement.setLong(13, _price_sell);
			statement.setLong(14, _price_buy);
			statement.setInt(15, getObjectId());
			statement.setInt(16, _type1);
			statement.setInt(17, _type2);
			statement.setInt(18, _mana);
			statement.setLong(19, _expireTime);
			statement.setString(20, getItemClass().name());
			statement.executeUpdate();

			_existsInDb = true;
			_storedInDb = true;

			L2World.increaseInsertItemCount();
		}
		catch(Exception e)
		{
			_log.warn("Could not insert item " + getObjectId() + "; itemID=" + getItemId() + "; count=" + getCount() + "; owner=" + _owner_id + "; exception: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	/**
	 * Delete item from database
	 */
	private void removeFromDb()
	{
		// cancel lazy update task if need
		if(Config.LAZY_ITEM_UPDATE && _lazyUpdateInDb != null)
		{
			_lazyUpdateInDb.cancel(true);
			_lazyUpdateInDb = null;
		}

		if(!_whflag)
			removeAugmentation();

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM items WHERE object_id=" + _objectId + " limit 1");
			statement.executeUpdate();

			_existsInDb = false;
			_storedInDb = false;

			L2World.increaseDeleteItemCount();
		}
		catch(Exception e)
		{
			_log.error("Could not delete item " + getObjectId() + " in DB:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	/**
	 * Returns if item is available for manipulation
	 *
	 * @return boolean
	 */
	public boolean isAvailable(L2Player player, boolean allowAdena)
	{
		return getItemId() == 5575 || !isEquipped() && _itemTemplate.getType2() != L2Item.TYPE2_QUEST && (_itemTemplate.getType2() != L2Item.TYPE2_MONEY || _itemTemplate.getType1() != L2Item.TYPE1_SHIELD_ARMOR)
				&& !isActivePetControlItem(player) // Not Control item of currently summoned pet
				&& (_customFlags & FLAG_PET_INVENTORY) != FLAG_PET_INVENTORY
				&& player.getEnchantScroll() != this && (allowAdena || getItemId() != 57);
	}

	/**
	 * Return true if item is hero-item
	 *
	 * @return boolean
	 */
	public boolean isHeroItem()
	{
		return (getItemId() >= 6611 && getItemId() <= 6621) || (getItemId() >= 9388 && getItemId() <= 9390) || getItemId() == 6842;
	}

	/**
	 * Return true if item can be destroyed
	 *
	 * @return boolean
	 */
	public boolean canBeDestroyed(L2Player player)
	{
		if((_customFlags & FLAG_NO_DESTROY) == FLAG_NO_DESTROY)
			return false;

		//is hero item?
		if(isHeroItem())
			return false;

		if(isActivePetControlItem(player))
			return false;

		if(CursedWeaponsManager.getInstance().isCursed(getItemId()))
			return false;

		/** TODO: fill conditions ( by Styx to Styx ;) ) */

		return _itemTemplate.isDestroyable();
	}

	/**
	 * Return true if item can be dropped
	 *
	 * @return boolean
	 */
	public boolean canBeDropped(L2Player player)
	{
		if((_customFlags & FLAG_NO_DROP) == FLAG_NO_DROP)
			return false;

		//is hero item?
		if(isHeroItem())
			return false;

		if(isAugmented() && !Config.ALT_ALLOW_DROP_AUGMENTED)
			return false;

		//is quest item?
		if(_itemTemplate.getType2() == L2Item.TYPE2_QUEST)
			return false;

		if(isActivePetControlItem(player))
			return false;

		if(CursedWeaponsManager.getInstance().isCursed(getItemId()))
			return false;

		/** TODO: fill conditions ( by Styx to Styx ;) ) */

		return _itemTemplate.isDropable();
	}

	/**
	 * Return true if item can be trade
	 *
	 * @return boolean
	 */
	public boolean canBeTraded(L2Player player)
	{
		if((_customFlags & FLAG_NO_TRADE) == FLAG_NO_TRADE || (_customFlags & FLAG_PET_INVENTORY) == FLAG_PET_INVENTORY)
			return false;

		//is hero item?
		if(isHeroItem())
			return false;

		if(isActivePetControlItem(player))
			return false;

		if(isAugmented() && !Config.ALT_ALLOW_DROP_AUGMENTED)
			return false;

		if(CursedWeaponsManager.getInstance().isCursed(getItemId()))
			return false;

		/** TODO: fill conditions ( by Styx to Styx ;) ) */

		return _itemTemplate.isTradeable();
	}

	/**
	 * Return true if item can be sold
	 *
	 * @return boolean
	 */
	public boolean canBeSelled(L2Player player)
	{
		if((_customFlags & FLAG_NO_DESTROY) == FLAG_NO_DESTROY)
			return false;

		//is hero item?
		if(isHeroItem())
			return false;

		if(isActivePetControlItem(player))
			return false;

		if(isAugmented() && !Config.ALT_ALLOW_DROP_AUGMENTED)
			return false;

		if(CursedWeaponsManager.getInstance().isCursed(getItemId()))
			return false;

		/** TODO: fill conditions ( by Styx to Styx ;) ) */

		return _itemTemplate.isSellable();
	}

	/**
	 * Можно ли положить на клановый склад или передать фрейтом
	 *
	 * @param player
	 * @param privateWh - true Private/false Clan
	 * @return
	 */
	public boolean canBeStored(L2Player player, boolean privateWh)
	{
		if((_customFlags & FLAG_NO_TRANSFER) == FLAG_NO_TRANSFER || CursedWeaponsManager.getInstance().isCursed(getItemId()) || isActivePetControlItem(player) || (getCustomFlags() & L2ItemInstance.FLAG_PET_INVENTORY) == L2ItemInstance.FLAG_PET_INVENTORY)
			return false;

		return privateWh && _itemTemplate.isKeepType(L2Item.KEEP_TYPE_PWH) || !privateWh && (!isAugmented() || Config.ALT_ALLOW_DROP_AUGMENTED) && _itemTemplate.isKeepType(L2Item.KEEP_TYPE_СWH);
	}

	public boolean isFreightPossible(L2Player player)
	{
		if((_customFlags & FLAG_NO_TRANSFER) == FLAG_NO_TRANSFER || CursedWeaponsManager.getInstance().isCursed(getItemId()) ||
				isActivePetControlItem(player) || (getCustomFlags() & L2ItemInstance.FLAG_PET_INVENTORY) == L2ItemInstance.FLAG_PET_INVENTORY || isAugmented() || !_itemTemplate.isKeepType(L2Item.KEEP_TYPE_FREIGHT))
			return false;

		return true;
	}

	public boolean isActivePetControlItem(L2Player player)
	{
		L2ItemInstance item = player.getCastingItem();
		return (player.isPetSummoned() && getObjectId() == player.getPet().getControlItemObjId()) || (player.getMountEngine().isMounted() && PetDataTable.getControlItemId(player.getMountEngine().getMountNpcId()) == getItemId()) || (item != null && PetDataTable.isPetControlItem(item));
	}

	/**
	 * Return true if item can be crystallized
	 *
	 * @return boolean
	 */
	public boolean canBeCrystallized(L2Player player)
	{
		if((_customFlags & FLAG_NO_CRYSTALLIZE) == FLAG_NO_CRYSTALLIZE)
			return false;

		//is hero item?
		if(isHeroItem())
			return false;

		//crystallizable?
		if(!_itemTemplate.isCrystallizable())
			return false;

		//can player crystallize?
		int level = player.getSkillLevel(L2Skill.SKILL_CRYSTALLIZE);

		if(level < 1 || (_itemTemplate.getCrystalType().ordinal() > level && level < 5) || (_itemTemplate.getCrystalType() == L2Item.Grade.S80 && player.getLevel() < 80))
		{
			player.sendPacket(new SystemMessage(SystemMessage.CANNOT_CRYSTALLIZE_CRYSTALLIZATION_SKILL_LEVEL_TOO_LOW));
			player.sendActionFailed();
			return false;
		}

		if(isActivePetControlItem(player))
			return false;

		if(CursedWeaponsManager.getInstance().isCursed(getItemId()))
			return false;

		/** TODO: fill conditions ( by Styx to Styx ;) ) */

		return true;
	}

	/**
	 * Returns the item in String format
	 *
	 * @return String
	 */
	@Override
	public String toString()
	{
		return getObjectId() + ":" + getItemId() + ":" + (getEnchantLevel() > 0 ? "+" + getEnchantLevel() + " " : "") + _itemTemplate + "(" + getCount() + ")";
	}

	public boolean isNightLure()
	{
		return getItemId() >= 8505 && getItemId() <= 8513 || getItemId() == 8485;
	}

	public void notifyEquipped(boolean equipped)
	{
		if(!equipped)
		{
			if(_consumeManaTask != null)
			{
				_consumeManaTask.cancel(false);
				_consumeManaTask = null;
			}
			return;
		}

		L2Player owner = getOwner();
		if(owner == null)
			return;

		if(_consumeManaTask == null && isShadowItem()) // Если вещь уже надета метод может вызываться из refreshListeners
			_mana--;

		if(checkMana())
			_consumeManaTask = ThreadPoolManager.getInstance().scheduleEffect(new ConsumeManaTask(), 60000);
	}

	public void startLifeTask()
	{
		if(_consumeManaTask == null && isShadowItem())
			_consumeManaTask = ThreadPoolManager.getInstance().scheduleEffect(new ConsumeManaTask(), 60000);
		if(_expireTask == null && isTemporalItem())
			_expireTask = ThreadPoolManager.getInstance().scheduleGeneral(new ExpireTask(), _expireTime - System.currentTimeMillis());
	}

	public boolean isShadowItem()
	{
		return _itemTemplate.isShadowItem();
	}

	public boolean isStandartItem()
	{
		return _itemTemplate.isStandartItem();
	}

	public boolean isAltSeed()
	{
		return _itemTemplate.isAltSeed();
	}

	public int getMana()
	{
		return isShadowItem() ? _mana : -1;
	}

	public int getExpireTime()
	{
		return isTemporalItem() ? (int) ((_expireTime - System.currentTimeMillis()) / 1000) : -9999;
	}

	public void setMana(L2Player owner, final int lt)
	{
		_mana = lt;
		_storedInDb = false;
		owner.sendPacket(new InventoryUpdate().addModifiedItem(this));
	}

	boolean taskStartCheck = false;

	private class ConsumeManaTask implements Runnable
	{
		public void run()
		{
			if(!isEquipped() || !isShadowItem())
				return;

			L2Player owner = getOwner();
			if(owner == null || !owner.isOnline() || owner.isDeleting())
				return;

			setMana(owner, getMana() - 1);

			taskStartCheck = true;

			if(checkMana())
				_consumeManaTask = ThreadPoolManager.getInstance().scheduleEffect(this, 60000); //У шэдовов 1 цикл = 60 сек.
			else
				_consumeManaTask = null;

			taskStartCheck = false;
		}
	}

	private class ExpireTask implements Runnable
	{
		public void run()
		{
			L2Player owner = getOwner();
			if(owner != null)
			{
				if(isEquipped())
					owner.getInventory().unEquipItemAndSendChanges(L2ItemInstance.this);

				owner.getInventory().destroyItem("TimeOut", getObjectId(), getCount(), null, null);
				owner.sendPacket(Msg.THE_LIMITED_TIME_ITEM_HAS_BEEN_DELETED);
				owner.broadcastUserInfo(true);
			}
			else
			{
				removeFromDb();
				decayMe();
			}
		}
	}

	private boolean checkMana()
	{
		L2Player owner = getOwner();
		if(owner == null || !owner.isOnline() || owner.isDeleting())
			return false;

		SystemMessage sm = null;
		if(_mana == 10)
			sm = new SystemMessage(SystemMessage.S1S_REMAINING_MANA_IS_NOW_10);
		else if(_mana == 5)
			sm = new SystemMessage(SystemMessage.S1S_REMAINING_MANA_IS_NOW_5);
		else if(_mana == 1)
			sm = new SystemMessage(SystemMessage.S1S_REMAINING_MANA_IS_NOW_1);
		else if(_mana <= 0)
			sm = new SystemMessage(SystemMessage.S1S_REMAINING_MANA_IS_NOW_0);

		if(sm != null)
		{
			sm.addItemName(getItemId());
			owner.sendPacket(sm);
		}

		if(_mana <= 0)
		{
			owner.getInventory().unEquipItemAndSendChanges(L2ItemInstance.this);
			owner.getInventory().destroyItem("TimeOut", getObjectId(), getCount(), null, null);
			owner.broadcastUserInfo(true);
			return false;
		}

		return true;
	}

	public void dropToTheGround(L2Player lastAttacker, L2NpcInstance dropper)
	{
		if(dropper == null)
		{
			Location dropPos = Location.coordsRandomize(lastAttacker, 70);
			for(int i = 0; i < 20 && !GeoEngine.canMoveWithCollision(lastAttacker.getX(), lastAttacker.getY(), lastAttacker.getZ(), dropPos.getX(), dropPos.getY(), dropPos.getZ(), lastAttacker.getReflection()); i++)
				dropPos = Location.coordsRandomize(lastAttacker, 70);
			dropMe(lastAttacker, dropPos);
			ItemsAutoDestroy.getInstance().addItem(this);
			return;
		}

		// 20 попыток уронить дроп в точке смерти моба
		Location dropPos = Location.coordsRandomize(dropper, 70);
		for(int i = 0; i < 20 && !GeoEngine.canMoveWithCollision(dropper.getX(), dropper.getY(), dropper.getZ(), dropPos.getX(), dropPos.getY(), dropPos.getY(), dropper.getReflection()); i++)
			dropPos = Location.coordsRandomize(dropper, 70);

		// Если в точке смерти моба дропу негде упасть, то падает под ноги чару
		//if(!GeoEngine.canMoveWithCollision(dropper.getX(), dropper.getY(), dropper.getZ(), dropPos.getX(), dropPos.getY(), dropPos.getZ(), dropper.getReflection()))
		//{
		//	dropPos.setX(lastAttacker.getX());
		//	dropPos.setY(lastAttacker.getY());
		//	dropPos.setZ(lastAttacker.getZ());
		//}

		// Init the dropped L2ItemInstance and add it in the world as a visible object at the position where mob was last
		dropMe(dropper, dropPos);

		// Add drop to auto destroy item task
		if(isHerb())
			ItemsAutoDestroy.getInstance().addHerb(this);
		else if(Config.AUTODESTROY_ITEM_AFTER > 0 && !dropper.isPlayable())
			ItemsAutoDestroy.getInstance().addItem(this);

		// activate nonowner penalty
		if(dropper instanceof L2RaidBossInstance && ((L2RaidBossInstance) dropper).getLootOwner() != null)
		{
			setItemDropOwner(((L2RaidBossInstance) dropper).getLootOwner());
			setDropTimeOwner(System.currentTimeMillis() + 285000);
		}
		else
		{
			setItemDropOwner(lastAttacker);
			setDropTimeOwner(System.currentTimeMillis());
		}
	}

	/**
	 * Бросает вещь на землю туда, где ее можно поднять
	 *
	 * @param dropper
	 */
	public void dropToTheGround(L2Character dropper, Location dropPos)
	{
		if(GeoEngine.canMoveToCoord(dropper.getX(), dropper.getY(), dropper.getZ(), dropPos.getX(), dropPos.getY(), dropPos.getZ(), dropper.getReflection()))
			dropMe(dropper, dropPos);
		else
			dropMe(dropper, dropper.getLoc());

		// Add drop to auto destroy item task
		if(Config.AUTODESTROY_ITEM_AFTER > 0 && !dropper.isPlayable())
			ItemsAutoDestroy.getInstance().addItem(this);
	}

	public boolean isDestroyable()
	{
		return true;
	}

	public void setWhFlag(boolean whflag)
	{
		_whflag = whflag;
	}

	public void deleteMe()
	{
		removeFromDb();
		decayMe();
	}

	public ItemClass getItemClass()
	{
		return _itemTemplate.getItemClass();
	}

	public void setItemId(int id)
	{
		_itemId = id;
		_itemTemplate = ItemTable.getInstance().getTemplate(id);
	}

	/**
	 * Возвращает защиту от элемента: огонь.
	 *
	 * @return значение защиты
	 */
	public int getDefenceFire()
	{
		return (_itemTemplate.getType2() == L2Item.TYPE2_SHIELD_ARMOR ? _enchantAttributeFireValue : 0) + _deffFire;
	}

	/**
	 * Возвращает защиту от элемента: вода.
	 *
	 * @return значение защиты
	 */
	public int getDefenceWater()
	{
		return (_itemTemplate.getType2() == L2Item.TYPE2_SHIELD_ARMOR ? _enchantAttributeWaterValue : 0) + _deffWind;
	}

	/**
	 * Возвращает защиту от элемента: воздух.
	 *
	 * @return значение защиты
	 */
	public int getDefenceWind()
	{
		return (_itemTemplate.getType2() == L2Item.TYPE2_SHIELD_ARMOR ? _enchantAttributeWindValue : 0) + _deffWater;
	}

	/**
	 * Возвращает защиту от элемента: земля.
	 *
	 * @return значение защиты
	 */
	public int getDefenceEarth()
	{
		return (_itemTemplate.getType2() == L2Item.TYPE2_SHIELD_ARMOR ? _enchantAttributeEarthValue : 0) + _deffEarth;
	}

	/**
	 * Возвращает защиту от элемента: свет.
	 *
	 * @return значение защиты
	 */
	public int getDefenceHoly()
	{
		return (_itemTemplate.getType2() == L2Item.TYPE2_SHIELD_ARMOR ? _enchantAttributeHolyValue : 0) + _deffHoly;
	}

	/**
	 * Возвращает защиту от элемента: тьма.
	 *
	 * @return значение защиты
	 */
	public int getDefenceDark()
	{
		return (_itemTemplate.getType2() == L2Item.TYPE2_SHIELD_ARMOR ? _enchantAttributeDarkValue : 0) + _deffDark;
	}

	public int[] getAttackElement()
	{
		if(_itemTemplate.getType2() != L2Item.TYPE2_WEAPON)
			return new int[] { L2Item.ATTRIBUTE_NONE, 0 };

		int fire = _enchantAttributeFireValue + _deffFire;
		if(fire > 0)
			return new int[] { L2Item.ATTRIBUTE_FIRE, fire };
		int water = _enchantAttributeWaterValue + _deffWater;
		if(water > 0)
			return new int[] { L2Item.ATTRIBUTE_WATER, water };
		int wind = _enchantAttributeWindValue + _deffWind;
		if(wind > 0)
			return new int[] { L2Item.ATTRIBUTE_WIND, wind };
		int earth = _enchantAttributeEarthValue + _deffEarth;
		if(earth > 0)
			return new int[] { L2Item.ATTRIBUTE_EARTH, earth };
		int holy = _enchantAttributeHolyValue + _deffHoly;
		if(holy > 0)
			return new int[] { L2Item.ATTRIBUTE_HOLY, holy };
		int dark = _enchantAttributeDarkValue + _deffDark;
		if(dark > 0)
			return new int[] { L2Item.ATTRIBUTE_DARK, dark };

		return new int[] { L2Item.ATTRIBUTE_NONE, 0 };
	}

	/**
	 * Возвращает значение элемента атрибуции предмета
	 *
	 * @return сила элемента
	 */
	public int getAttributeElementValue(int element)
	{
		if(element == L2Item.ATTRIBUTE_FIRE)
			return _enchantAttributeFireValue;
		else if(element == L2Item.ATTRIBUTE_WATER)
			return _enchantAttributeWaterValue;
		else if(element == L2Item.ATTRIBUTE_WIND)
			return _enchantAttributeWindValue;
		else if(element == L2Item.ATTRIBUTE_EARTH)
			return _enchantAttributeEarthValue;
		else if(element == L2Item.ATTRIBUTE_HOLY)
			return _enchantAttributeHolyValue;
		else if(element == L2Item.ATTRIBUTE_DARK)
			return _enchantAttributeDarkValue;
		return 0;
	}

	private FuncTemplate _enchantFireFuncTemplate;
	private FuncTemplate _enchantWaterFuncTemplate;
	private FuncTemplate _enchantWindFuncTemplate;
	private FuncTemplate _enchantEarthFuncTemplate;
	private FuncTemplate _enchantHolyFuncTemplate;
	private FuncTemplate _enchantDarkFuncTemplate;

	public void changeAttributeElement(String process, int element, int value, L2Player creator, Object reference)
	{
		int fire_el = element == L2Item.ATTRIBUTE_FIRE ? value : _enchantAttributeFireValue;
		int water_el = element == L2Item.ATTRIBUTE_WATER ? value : _enchantAttributeWaterValue;
		int wind_el = element == L2Item.ATTRIBUTE_WIND ? value : _enchantAttributeWindValue;
		int earth_el = element == L2Item.ATTRIBUTE_EARTH ? value : _enchantAttributeEarthValue;
		int holy_el = element == L2Item.ATTRIBUTE_HOLY ? value : _enchantAttributeHolyValue;
		int dark_el = element == L2Item.ATTRIBUTE_DARK ? value : _enchantAttributeDarkValue;

		setAttributeElement(fire_el, water_el, wind_el, earth_el, holy_el, dark_el, true);

		if(process != null)
		{
			List<Object> param = new ArrayList<Object>();
			param.add("CHANGE:" + process);
			param.add(this);
			if(creator != null)
				param.add(creator);
			if(reference != null)
				param.add(reference);
			param.add(element);
			param.add(value);
			_logItems.info(param);
		}
	}

	/**
	 * Устанавливает элемент атрибуции предмета.<br>
	 * Element (0 - Fire, 1 - Water, 2 - Wind, 3 - Earth, 4 - Holy, 5 - Dark, -2 - None)
	 *
	 * @param element элемент
	 */
	public void setAttributeElement(int fire_el, int water_el, int wind_el, int earth_el, int holy_el, int dark_el, boolean updateDb)
	{
		boolean weapon = getItem().getType2() == L2Item.TYPE2_WEAPON;
		if(fire_el > 0)
		{
			if(_enchantFireFuncTemplate == null)
			{
				_enchantFireFuncTemplate = new FuncTemplate(null, null, "Add", weapon ? Stats.ATTACK_ELEMENT_FIRE : Stats.FIRE_ATTRIBUTE, 0x40, fire_el);
				attachFunction(_enchantFireFuncTemplate);
			}
			else
				_enchantFireFuncTemplate._value = fire_el;
			_enchantAttributeFireValue = fire_el;
		}
		else
		{
			if(_enchantFireFuncTemplate != null)
			{
				deattachFunction(_enchantFireFuncTemplate);
				_enchantFireFuncTemplate = null;
			}
			_enchantAttributeFireValue = 0;
		}
        if(water_el > 0)
		{
			if(_enchantWaterFuncTemplate == null)
			{
				_enchantWaterFuncTemplate = new FuncTemplate(null, null, "Add", weapon ? Stats.ATTACK_ELEMENT_WATER : Stats.WATER_ATTRIBUTE, 0x40, water_el);
				attachFunction(_enchantWaterFuncTemplate);
			}
			else
				_enchantWaterFuncTemplate._value = water_el;
			_enchantAttributeWaterValue = water_el;
		}
		else
		{
			if(_enchantWaterFuncTemplate != null)
			{
				deattachFunction(_enchantWaterFuncTemplate);
				_enchantWaterFuncTemplate = null;
			}
			_enchantAttributeWaterValue = 0;
		}
		if(wind_el > 0)
		{
			if(_enchantWindFuncTemplate == null)
			{
				_enchantWindFuncTemplate = new FuncTemplate(null, null, "Add", weapon ? Stats.ATTACK_ELEMENT_WIND : Stats.WIND_ATTRIBUTE, 0x40, wind_el);
				attachFunction(_enchantWindFuncTemplate);
			}
			else
				_enchantWindFuncTemplate._value = wind_el;
			_enchantAttributeWindValue = wind_el;
		}
		else
		{
			if(_enchantWindFuncTemplate != null)
			{
				deattachFunction(_enchantWindFuncTemplate);
				_enchantWindFuncTemplate = null;
			}
			_enchantAttributeWindValue = 0;
		}
		if(earth_el > 0)
		{
			if(_enchantEarthFuncTemplate == null)
			{
				_enchantEarthFuncTemplate = new FuncTemplate(null, null, "Add", weapon ? Stats.ATTACK_ELEMENT_EARTH : Stats.EARTH_ATTRIBUTE, 0x40, earth_el);
				attachFunction(_enchantEarthFuncTemplate);
			}
			else
				_enchantEarthFuncTemplate._value = earth_el;
			_enchantAttributeEarthValue = earth_el;
		}
		else
		{
			if(_enchantEarthFuncTemplate != null)
			{
				deattachFunction(_enchantEarthFuncTemplate);
				_enchantEarthFuncTemplate = null;
			}
			_enchantAttributeEarthValue = 0;
		}
		if(holy_el > 0)
		{
			if(_enchantHolyFuncTemplate == null)
			{
				_enchantHolyFuncTemplate = new FuncTemplate(null, null, "Add", weapon ? Stats.ATTACK_ELEMENT_HOLY : Stats.HOLY_ATTRIBUTE, 0x40, holy_el);
				attachFunction(_enchantHolyFuncTemplate);
			}
			else
				_enchantHolyFuncTemplate._value = holy_el;
			_enchantAttributeHolyValue = holy_el;
		}
		else
		{
			if(_enchantHolyFuncTemplate != null)
			{
				deattachFunction(_enchantHolyFuncTemplate);
				_enchantHolyFuncTemplate = null;
			}
			_enchantAttributeHolyValue = 0;
		}
		if(dark_el > 0)
		{
			if(_enchantDarkFuncTemplate == null)
			{
				_enchantDarkFuncTemplate = new FuncTemplate(null, null, "Add", weapon ? Stats.ATTACK_ELEMENT_DARK : Stats.DARK_ATTRIBUTE, 0x40, dark_el);
				attachFunction(_enchantDarkFuncTemplate);
			}
			else
				_enchantDarkFuncTemplate._value = dark_el;
			_enchantAttributeDarkValue = dark_el;
		}
		else
		{
			if(_enchantDarkFuncTemplate != null)
			{
				deattachFunction(_enchantDarkFuncTemplate);
				_enchantDarkFuncTemplate = null;
			}
			_enchantAttributeDarkValue = 0;
		}

		if(updateDb)
		{
			_storedInDb = false;
			updateDatabase(true);
		}
	}

	/**
	 * Проверяет, является ли данный инстанс предмета хербом
	 *
	 * @return true если предмет является хербом
	 */
	public boolean isHerb()
	{
		return getItem().getItemType() == EtcItemType.HERB;
		//return (getItemId() >= 8600 && getItemId() <= 8614) || (getItemId() >= 10655 && getItemId() <= 10657) || getItemId() == 13028 || (getItemId() >= 14824 && getItemId() <= 14827);
	}

	public boolean isFortFlag()
	{
		return _itemId == 9819;
	}

	public boolean isTerritoryWard()
	{
		return _itemId >= 13560 && _itemId <= 13568;
	}

	public L2Item.Grade getCrystalType()
	{
		return _itemTemplate.getCrystalType();
	}

	public final int getCrystalCount()
	{
		return _itemTemplate.getCrystalCount(_enchantLevel);
	}

	public void setCustomFlags(int i)
	{
		_customFlags = i;
	}

	public int getCustomFlags()
	{
		return _customFlags;
	}

	public boolean isBossJewel()
	{
		return getItem().isBossJewel();
	}

	public boolean isSealed()
	{
		return _itemTemplate.isSealed();
	}

	/**
	 * Sets the quantity of the item.<BR><BR>
	 * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
	 *
	 * @param process   : String Identifier of process triggering this action
	 * @param count	 : int
	 * @param creator   : L2PcInstance Player requesting the item creation
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void changeCount(String process, long count, Object creator, L2Object reference)
	{
		if(count == 0)
			return;
		if(count > 0 && _count > L2Item.MAX_COUNT - count)
			_count = L2Item.MAX_COUNT;
		else
			_count += count;
		if(_count < 0)
			_count = 0;

		_storedInDb = false;

		if(process != null)
		{
			List<Object> param = new ArrayList<Object>();
			param.add("CHANGE:" + process);
			param.add(this);
			if(creator != null)
				param.add(creator);
			if(reference != null)
				param.add(reference);
			param.add(count);
			_logItems.info(param);
		}
	}

	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof L2ItemInstance)
		{
			L2ItemInstance item = (L2ItemInstance) obj;
			return item.getObjectId() == getObjectId() && item.getItemId() == getItemId() && item.getOwnerId() == getOwnerId() && item.getCount() == getCount() && item.getLocation() == getLocation();
		}
		return false;
	}

	public int getEnchantHpBonus()
	{
		if(_enchantLevel < 4 || (_bodypart != L2Item.SLOT_HEAD && _bodypart != L2Item.SLOT_CHEST && _bodypart != L2Item.SLOT_LEGS && _bodypart != L2Item.SLOT_GLOVES && _bodypart != L2Item.SLOT_FEET && _bodypart != L2Item.SLOT_FULL_ARMOR && _bodypart != L2Item.SLOT_UNDERWEAR && _bodypart != L2Item.SLOT_L_HAND && _bodypart != L2Item.SLOT_BELT))
			return 0;

		int grade = getItem().getItemGrade().externalOrdinal;
		int enchant = Math.min(_enchantLevel - 4, L2Item.enchantHpBonus[grade].length - 1);

		if(_bodypart == L2Item.SLOT_FULL_ARMOR)
			return (int) (L2Item.enchantHpBonus[grade][enchant] * 1.5);

		return L2Item.enchantHpBonus[grade][enchant];
	}

	public boolean isNoDropPK()
	{
		return isAugmented() || isShadowItem() || isTemporalItem() || isHeroItem() || isPvP() || isResidentsCirclet() || (_customFlags & FLAG_NO_PKDROP) == FLAG_NO_PKDROP || getItem().getType2() == L2Item.TYPE2_QUEST || getItem().getType1() == L2Item.TYPE1_ITEM_QUESTITEM_ADENA || getItem().getBodyPart() == L2Item.SLOT_L_BRACELET || getItem().getBodyPart() == L2Item.SLOT_R_BRACELET;
	}

	public boolean isPvP()
	{
		return _itemTemplate.isPvP();
	}

	public boolean isTemporalItem()
	{
		return _itemTemplate.isTemporal();
	}

	public void notifyEquipped(L2Playable actor)
	{
		if(_itemTemplate._equipListeners == null)
			return;

		for(ItemEquipListener listener : _itemTemplate._equipListeners)
			listener.onEquip(this, actor);
	}

	public void notifyUnEquipped(L2Playable actor)
	{
		if(_itemTemplate._equipListeners == null)
			return;

		for(ItemEquipListener listener : _itemTemplate._equipListeners)
			listener.onUnEquip(this, actor);
	}

	public boolean checkEquipCondition(L2Character cha)
	{
		return _itemTemplate.checkEquipCondition(cha, this);
	}

	/**
	 * Init a dropped L2ItemInstance and add it in the world as a visible object.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Set the x,y,z position of the L2ItemInstance dropped and update its _worldregion </li>
	 * <li>Add the L2ItemInstance dropped to _visibleObjects of its L2WorldRegion</li>
	 * <li>Add the L2ItemInstance dropped in the world as a <B>visible</B> object</li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T ADD the object to _allObjects of L2World </B></FONT><BR><BR>
	 *
	 * <B><U> Assert </U> :</B><BR><BR>
	 * <li> this instanceof L2ItemInstance</li>
	 * <li> _worldRegion == null <I>(L2Object is invisible at the beginning)</I></li><BR><BR>
	 *
	 * <B><U> Example of use </U> :</B><BR><BR>
	 * <li> Drop item</li>
	 * <li> Call Pet</li><BR>
	 *
	 * @param dropper Char that dropped item
	 * @param loc drop coordinates
	 */
	public void dropMe(L2Character dropper, Location loc)
	{
        setXYZInvisible(loc.getX(), loc.getY(), getGeoZ(loc));

        if(dropper != null)
		{
            setReflection(dropper.getReflection());
        }

        // Set the x,y,z position of the L2ItemInstance dropped and update its _worldregion
        _hidden = false;

		L2World.addVisibleObject(this, dropper);
	}

	public final void pickupMe(L2Character target)
	{
		// Create a server->client GetItem packet to pick up the L2ItemInstance
		target.broadcastPacket(new GetItem(this, target.getObjectId()));

		// if this item is a mercenary ticket, remove the spawns!
		int itemId = getItemId();
		/*if(itemId >= 3960 && itemId <= 3972 // Gludio
					 || itemId >= 3973 && itemId <= 3985 // Dion
					 || itemId >= 3986 && itemId <= 3998 // Giran
					 || itemId >= 3999 && itemId <= 4011 // Oren
					 || itemId >= 4012 && itemId <= 4026 // Aden
					 || itemId >= 5205 && itemId <= 5215 // Innadril
					 || itemId >= 6779 && itemId <= 6833 // Goddard
					 || itemId >= 7973 && itemId <= 8029 // Rune
					 || itemId >= 7918 && itemId <= 7972 // Schuttgart
					 )*/
		if(itemId >= 3960 && itemId <= 4026 || itemId >= 5205 && itemId <= 5214 || itemId >= 6038 && itemId <= 6306 || itemId >= 6779 && itemId <= 6833 || itemId >= 7918 && itemId <= 8029)
			MercTicketManager.getInstance().removeTicket(this);

		if(target.isPlayer() && target.getLevel() < 6 && (itemId == 57 || itemId == 6353))
		{
			Quest q = QuestManager.getQuest(255);
			if(q != null)
			{
				QuestState qs = target.getPlayer().getQuestState(q.getName());
				if(qs != null)
				{
					if(itemId == 57 && (qs.getInt("t") & 0x200000) == 0x200000)
						((L2Player) target).processQuestEvent(q.getName(), "TE" + 0x200000);
					else if(itemId == 6353 && (qs.getInt("t") & 0x100000) == 0x100000)
						((L2Player) target).processQuestEvent(q.getName(), "TE" + 0x100000);
				}
			}
		}

		// Remove the L2ItemInstance from the world
		_hidden = true;
		L2World.removeVisibleObject(this);
	}

	/**
	 * BY FlareDrakon
	 * TODO: снести в дп
	 * Вот дурацкая структура мало того что приходиться писать костыли
	 * так их приходиться писать чтобы снести более жёсткие костяли,какой ужас:(
	 */
	public boolean isResidentsCirclet()
	{
		return (getItemId() >= 6834 && getItemId() <= 6840) || getItemId() == 8182 || getItemId() == 8183;
	}

	public List<L2Skill> getEnchantOptionSkills()
	{
		ItemEnchantTemplate enchantTemplate = _itemTemplate.getEnchantOptions();
		if(enchantTemplate == null)
			return Collections.emptyList();

		List<L2Skill> skills = new LinkedList<>();
		EnchantOption[] options = enchantTemplate.getEnchantOption(_enchantLevel);
		if(options != null)
			for(EnchantOption option : options)
				if(option != null && option.getSkill() != null)
					skills.add(option.getSkill());

		return skills;
	}

	public List<L2Skill> getAllEnchantOptionSkills()
	{
		ItemEnchantTemplate enchantTemplate = _itemTemplate.getEnchantOptions();
		if(enchantTemplate == null)
			return Collections.emptyList();

		List<L2Skill> skills = new LinkedList<>();
		EnchantOption[][] options = enchantTemplate.getOptions();
		if(options != null)
			for(EnchantOption[] enchantLevel : options)
				if(enchantLevel != null)
					for(EnchantOption option : enchantLevel)
						if(option != null && option.getSkill() != null && !skills.contains(option.getSkill()))
							skills.add(option.getSkill());

		return skills;
	}

	public int getEnchantOptionId(int slot)
	{
		ItemEnchantTemplate enchantTemplate = _itemTemplate.getEnchantOptions();
		if(enchantTemplate == null)
			return 0;

		EnchantOption[] options = enchantTemplate.getEnchantOption(_enchantLevel);
		if(options == null || options.length <= slot)
			return 0;

		return options[slot].getOptionId();
	}
	
	public int getVisualId()
	{
		return _visualId;
	}

	public void setVisualId(int val)
	{
		_visualId = val;
	}
}
