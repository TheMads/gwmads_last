package ru.l2gw.gameserver.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.controllers.GameTimeController;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.instancemanager.CursedWeaponsManager;
import ru.l2gw.gameserver.instancemanager.FortressSiegeManager;
import ru.l2gw.gameserver.model.base.Race;
import ru.l2gw.gameserver.model.entity.vehicle.L2ClanAirship;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2ItemInstance.ItemLocation;
import ru.l2gw.gameserver.model.instances.L2PetInstance;
import ru.l2gw.gameserver.model.inventory.listeners.*;
import ru.l2gw.gameserver.serverpackets.InventoryUpdate;
import ru.l2gw.gameserver.serverpackets.PetInventoryUpdate;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.serverpackets.ExAdenaInvenCount;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.tables.PetDataTable;
import ru.l2gw.gameserver.templates.L2EtcItem.EtcItemType;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.gameserver.templates.L2Weapon.WeaponType;
import ru.l2gw.util.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Comparator;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static ru.l2gw.gameserver.templates.L2Armor.ArmorType.HEAVY;
import static ru.l2gw.gameserver.templates.L2Armor.ArmorType.MAGIC;
import static ru.l2gw.gameserver.templates.L2Item.SLOT_FORMAL_WEAR;
import static ru.l2gw.gameserver.templates.L2Weapon.WeaponType.*;

public abstract class Inventory
{
	protected static final Log _log = LogFactory.getLog(Inventory.class.getName());

	public static final byte PAPERDOLL_UNDER = 0;
	public static final byte PAPERDOLL_HEAD = 1;
	public static final byte PAPERDOLL_HAIR = 2;
	public static final byte PAPERDOLL_DHAIR = 3;
	public static final byte PAPERDOLL_NECK = 4;
	public static final byte PAPERDOLL_RHAND = 5;
	public static final byte PAPERDOLL_CHEST = 6;
	public static final byte PAPERDOLL_LHAND = 7;
	public static final byte PAPERDOLL_REAR = 8;
	public static final byte PAPERDOLL_LEAR = 9;
	public static final byte PAPERDOLL_GLOVES = 10;
	public static final byte PAPERDOLL_LEGS = 11;
	public static final byte PAPERDOLL_FEET = 12;
	public static final byte PAPERDOLL_LRHAND = 13;
	public static final byte PAPERDOLL_RFINGER = 14;
	public static final byte PAPERDOLL_LFINGER = 15;
	public static final byte PAPERDOLL_LBRACELET = 16;
	public static final byte PAPERDOLL_RBRACELET = 17;
	public static final byte PAPERDOLL_DECO1 = 18;
	public static final byte PAPERDOLL_DECO2 = 19;
	public static final byte PAPERDOLL_DECO3 = 20;
	public static final byte PAPERDOLL_DECO4 = 21;
	public static final byte PAPERDOLL_DECO5 = 22;
	public static final byte PAPERDOLL_DECO6 = 23;
	public static final byte PAPERDOLL_BACK = 24;
	public static final byte PAPERDOLL_BELT = 25;

	public static final int PAPERDOLL_MAX = 26;

	public static final int[] TALISMANS = {34984, 34985, 34986, 34987, 34988, 34989};
	public static final int[] TALISMANS_EFFECT = {13384, 13385, 13386, 13387, 13388, 13389};

	public static final int[] PAPERDOLL_ORDER = { Inventory.PAPERDOLL_UNDER, Inventory.PAPERDOLL_REAR, Inventory.PAPERDOLL_LEAR, Inventory.PAPERDOLL_NECK, Inventory.PAPERDOLL_RFINGER,
	        Inventory.PAPERDOLL_LFINGER, Inventory.PAPERDOLL_HEAD, Inventory.PAPERDOLL_RHAND, Inventory.PAPERDOLL_LHAND, Inventory.PAPERDOLL_GLOVES, Inventory.PAPERDOLL_CHEST,
	        Inventory.PAPERDOLL_LEGS, Inventory.PAPERDOLL_FEET, Inventory.PAPERDOLL_BACK, Inventory.PAPERDOLL_LRHAND, Inventory.PAPERDOLL_HAIR, Inventory.PAPERDOLL_DHAIR,
	        Inventory.PAPERDOLL_RBRACELET, Inventory.PAPERDOLL_LBRACELET, Inventory.PAPERDOLL_DECO1, Inventory.PAPERDOLL_DECO2, Inventory.PAPERDOLL_DECO3, Inventory.PAPERDOLL_DECO4,
	        Inventory.PAPERDOLL_DECO5, Inventory.PAPERDOLL_DECO6, Inventory.PAPERDOLL_BELT // Пояс
	};

	// Speed percentage mods
	public static final double MAX_ARMOR_WEIGHT = 12000;

	private final L2ItemInstance[] _paperdoll;

	private final CopyOnWriteArrayList<PaperdollListener> _paperdollListeners;
	private final CopyOnWriteArrayList<InventoryListener> inventoryListeners;

	// protected to be accessed from child classes only
	// Отдельно синхронизировать этот список не надо, ибо ConcurrentLinkedQueue уже синхронизирован
	protected final ConcurrentLinkedQueue<L2ItemInstance> _items;
	protected final ConcurrentLinkedQueue<L2ItemInstance> _refundItems;

	private int _totalWeight;
	private int _allowedTalismans;

	private boolean refreshingListeners;

	// used to quickly check for using of items of special type
	private int _wearedMask;

	/**
	 * Constructor of the inventory
	 */
	protected Inventory()
	{
		_paperdoll = new L2ItemInstance[PAPERDOLL_MAX];
		_items = new ConcurrentLinkedQueue<L2ItemInstance>();
		_refundItems = new ConcurrentLinkedQueue<L2ItemInstance>();
		_paperdollListeners = new CopyOnWriteArrayList<PaperdollListener>();
		addPaperdollListener(new BraceletListener(this));
		addPaperdollListener(new BowListener(this));
		addPaperdollListener(new FormalWearListener(this));
		addPaperdollListener(new ArmorSetListener(this));
		addPaperdollListener(new StatsListener(this));
		addPaperdollListener(new ItemSkillsListener(this));
		addPaperdollListener(new ItemAugmentationListener(this));
		inventoryListeners = new CopyOnWriteArrayList<>();
		addInventoryListener(new ScriptInventoryListener(this));
	}

	public abstract L2Character getOwner();

	protected abstract ItemLocation getBaseLocation();

	protected abstract ItemLocation getEquipLocation();

	/**
	 * Returns the ownerID of the inventory
	 *
	 * @return int
	 */
	public int getOwnerId()
	{
		return getOwner() == null ? 0 : getOwner().getObjectId();
	}

	/**
	 * Returns the quantity of items in the inventory
	 *
	 * @return int
	 */
	public int getSize()
	{
		return _items.size();
	}

	/**
	 * Returns the list of items in inventory
	 *
	 * @return L2ItemInstance : items in inventory
	 */
	public L2ItemInstance[] getItems()
	{
		return _items.toArray(new L2ItemInstance[_items.size()]);
	}

	public ConcurrentLinkedQueue<L2ItemInstance> getItemsList()
	{
		return _items;
	}

	public ConcurrentLinkedQueue<L2ItemInstance> getRefundItemsList()
	{
		return _refundItems;
	}

	/**
	 * Adds item to inventory for further adjustments.
	 *
	 * @param item : L2ItemInstance to be added from inventory
	 */
	protected void addItem(L2ItemInstance item)
	{
		_items.add(item);
	}

	/**
	 * Adds item to inventory
	 *
	 * @param process   : String Identifier of process triggering this action
	 * @param item	  : L2ItemInstance to be added
	 * @param actor	 : L2PcInstance Player requesting the item add
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance addItem(String process, L2ItemInstance item, L2Player actor, L2Object reference)
	{
		if((getOwner() == null && !(this instanceof ClanWarehouse)) || item == null)
			return null;

		if(item.isHerb() && !getOwner().getPlayer().isGM())
		{
			Util.handleIllegalPlayerAction(getOwner().getPlayer(), "tried to pickup herb into inventory", "Inventory[179]", 1);
			return null;
		}

		if(item.getCount() <= 0)
		{
			_log.warn("AddItem: count <= 0 owner:" + getOwner().getName());
			Thread.dumpStack();
			return null;
		}

		if(this instanceof PcInventory && !process.equalsIgnoreCase("Trade") && getOwner().isPlayer() && ((L2Player) getOwner()).isTradeInProgress())
			((L2Player) getOwner()).cancelActiveTrade();

		L2ItemInstance olditem = getItemByItemId(item.getItemId());

		if(olditem != null && olditem.isStackable())
		{
			long count = item.getCount();
			olditem.changeCount(process, count, actor, reference);
			olditem.setLastChange(L2ItemInstance.MODIFIED);

			// And destroys the item
			ItemTable.getInstance().destroyItem(process, item, actor, reference);
			item.updateDatabase();

			item = olditem;
			// Updates database
			if(item.getItemId() == 57 && count < 10000 * Config.RATE_DROP_ADENA)
			{
				// Small adena changes won't be saved to database all the time
				if(GameTimeController.getGameTicks() % 5 == 0)
					item.updateDatabase();
			}
			else
				item.updateDatabase();

			sendModifyItem(item);
		}
		// If item hasn't be found in inventory, create new one
		else
		{
			item.setOwnerId(process, getOwnerId(), actor, reference);
			item.setLocation(getBaseLocation());
			item.setLastChange((L2ItemInstance.ADDED));

			// Add item in inventory
			addItem(item);
			// Updates database
			item.updateDatabase();
			sendNewItem(item);

			if(item.getItemType() == EtcItemType.RUNE_SELECT || item.getItemType() == EtcItemType.RUNE)
				for(PaperdollListener listener : _paperdollListeners)
					listener.notifyEquipped(-1, item);

			for(InventoryListener listener : inventoryListeners)
				listener.itemAdded(item);
		}

		//CombatFlag
		if(item.isFortFlag() && getOwner().isPlayer())
			FortressSiegeManager.getInstance().activateCombatFlag((L2Player) getOwner(), item);

		if(item.isTerritoryWard() && getOwner().isPlayer())
		{
			InventoryUpdate iu = new InventoryUpdate(equipItemAndRecord(item));
			getOwner().getPlayer().sendPacket(new ExAdenaInvenCount(getOwner().getPlayer()));
			getOwner().getPlayer().sendChanges();
			getOwner().getPlayer().sendPacket(iu);
			getOwner().getPlayer().setCombatFlagEquipped(true);
			if(Config.ALT_TERRITORY_WARD_PEACE_ZONE_TIMEOUT > 0)
				ThreadPoolManager.getInstance().scheduleGeneral(new L2ObjectTasks.TerritoryWardPeaceTask(getOwner().getPlayer()), 1000);
		}

		if(CursedWeaponsManager.getInstance().isCursed(item.getItemId()) && getOwner().isPlayer())
			CursedWeaponsManager.getInstance().checkPlayer((L2Player) getOwner(), item);

		// Refresh weigth
		refreshWeight();
		return item;
	}

	/**
	 * Adds item to inventory
	 *
	 * @param process   : String Identifier of process triggering this action
	 * @param itemId	: int Item Identifier of the item to be added
	 * @param count	 : int Quantity of items to be added
	 * @param actor	 : L2PcInstance Player requesting the item add
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance addItem(String process, int itemId, long count, L2Player actor, L2Object reference)
	{
		if(getOwner() == null && !(this instanceof ClanWarehouse))
			return null;

		if(count <= 0)
		{
			_log.warn("AddItem: count <= 0 owner:" + getOwner().getName());
			Thread.dumpStack();
			return null;
		}

		if(this instanceof PcInventory && !process.equalsIgnoreCase("Trade") && getOwner().isPlayer() && ((L2Player) getOwner()).isTradeInProgress())
			((L2Player) getOwner()).cancelActiveTrade();

		L2ItemInstance item = getItemByItemId(itemId);

		if(item != null && item.isStackable())
		{
			item.changeCount(process, count, actor, reference);
			item.setLastChange(L2ItemInstance.MODIFIED);
			// Updates database
			if(itemId == 57 && count < 10000 * Config.RATE_DROP_ADENA && !(this instanceof Warehouse))
			{
				// Small adena changes won't be saved to database all the time
				if(GameTimeController.getGameTicks() % 5 == 0)
					item.updateDatabase();
			}
			else
				item.updateDatabase(this instanceof Warehouse);

			sendModifyItem(item);
		}
		// If item hasn't be found in inventory, create new one
		else
		{
			for(int i = 0; i < count; i++)
			{
				L2Item template = ItemTable.getInstance().getTemplate(itemId);
				if(template == null)
				{
					_log.error((actor != null ? "[" + actor.getName() + "] " : "") + "Invalid ItemId requested: " + itemId);
					return null;
				}

				item = ItemTable.getInstance().createItem(process, itemId, template.isStackable() ? count : 1, actor, reference);
				item.setOwnerId(getOwnerId());
				item.setLocation(getBaseLocation());
				item.setLastChange(L2ItemInstance.ADDED);

				// Add item in inventory
				addItem(item);
				// Updates database
				item.updateDatabase(this instanceof Warehouse);
				sendNewItem(item);

				if(item.getItemType() == EtcItemType.RUNE_SELECT || item.getItemType() == EtcItemType.RUNE)
					for(PaperdollListener listener : _paperdollListeners)
						listener.notifyEquipped(-1, item);

				for(InventoryListener listener : inventoryListeners)
					listener.itemAdded(item);

				// If stackable, end loop as entire count is included in 1 instance of item
				if(template.isStackable())
					break;
			}
		}

		//CombatFlag
		if(item.isFortFlag() && getOwner().isPlayer())
			FortressSiegeManager.getInstance().activateCombatFlag((L2Player) getOwner(), item);

		if(item.isTerritoryWard() && getOwner().isPlayer())
		{
			InventoryUpdate iu = new InventoryUpdate(equipItemAndRecord(item));
			getOwner().getPlayer().sendChanges();
			getOwner().getPlayer().sendPacket(iu);
			getOwner().getPlayer().setCombatFlagEquipped(true);
			if(Config.ALT_TERRITORY_WARD_PEACE_ZONE_TIMEOUT > 0)
				ThreadPoolManager.getInstance().scheduleGeneral(new L2ObjectTasks.TerritoryWardPeaceTask(getOwner().getPlayer()), 1000);
		}

		if(CursedWeaponsManager.getInstance().isCursed(item.getItemId()) && getOwner().isPlayer())
			CursedWeaponsManager.getInstance().checkPlayer((L2Player) getOwner(), item);

		refreshWeight();
		return item;
	}

	/**
	 * Returns the item in the paperdoll slot
	 *
	 * @param slot Слот в котором ищем предмет
	 * @return L2ItemInstance
	 */
	public L2ItemInstance getPaperdollItem(int slot)
	{
		if(slot == PAPERDOLL_LRHAND)
			slot = PAPERDOLL_RHAND;
		return _paperdoll[slot];
	}

	/**
	 * Returns the ID of the item in the paperdol slot
	 *
	 * @param slot : int designating the slot
	 * @return int designating the ID of the item
	 */
	public int getPaperdollItemId(int slot)
	{
		if(slot == PAPERDOLL_LRHAND)
			slot = PAPERDOLL_RHAND;

		if(slot == PAPERDOLL_RHAND && getOwner().isPlayer())
		{
			L2Player player = getOwner().getPlayer();
			if(player.getVehicle() instanceof L2ClanAirship)
			{
				L2ClanAirship cas = (L2ClanAirship) player.getVehicle();
				if(cas.getCaptainObjectId() == player.getObjectId())
					return 13556; // Затычка на отображение штурвала - Airship Helm
			}
		}

		L2ItemInstance item = _paperdoll[slot];
		if(item != null)
			return item.getItemId();
		else if(slot == PAPERDOLL_HAIR)
		{
			item = _paperdoll[PAPERDOLL_DHAIR];
			if(item != null)
				return item.getItemId();
		}
		return 0;
	}

	public int getVisualItemId(int slot)
	{
		L2ItemInstance item = getPaperdollItem(slot);
		if(item != null)
		{
			return item.getVisualId();
		}
		else if(slot == PAPERDOLL_HAIR)
		{
			item = _paperdoll[PAPERDOLL_DHAIR];
			if(item != null)
			{
				return item.getVisualId();
			}
		}

		return 0;
	}
	
	/**
	 * Returns the objectID associated to the item in the paperdoll slot
	 *
	 * @param slot : int pointing out the slot
	 * @return int designating the objectID
	 */
	public int getPaperdollObjectId(int slot)
	{
		if(slot == PAPERDOLL_LRHAND)
			slot = PAPERDOLL_RHAND;

		L2ItemInstance item = _paperdoll[slot];
		if(item != null)
			return item.getObjectId();
		else if(slot == PAPERDOLL_HAIR)
		{
			item = _paperdoll[PAPERDOLL_DHAIR];
			if(item != null)
				return item.getObjectId();
		}
		return 0;
	}

	/**
	 * Adds new inventory's paperdoll listener
	 *
	 * @param listener pointing out the listener
	 */
	public void addPaperdollListener(PaperdollListener listener)
	{
		synchronized(_paperdollListeners)
		{
			_paperdollListeners.add(listener);
		}
	}

	public void addInventoryListener(InventoryListener listener)
	{
		synchronized(inventoryListeners)
		{
			inventoryListeners.add(listener);
		}
	}

	/**
	 * Removes a paperdoll listener
	 *
	 * @param listener pointing out the listener to be deleted
	 */
	public void removePaperdollListener(PaperdollListener listener)
	{
		synchronized(_paperdollListeners)
		{
			_paperdollListeners.remove(listener);
		}
	}

	public void removeInventoryListener(InventoryListener listener)
	{
		synchronized(inventoryListeners)
		{
			inventoryListeners.remove(listener);
		}
	}

	/**
	 * Equips an item in the given slot of the paperdoll. <U><I>Remark :</I></U>
	 * The item <B>HAS TO BE</B> already in the inventory
	 *
	 * @param slot : int pointing out the slot of the paperdoll
	 * @param item : L2ItemInstance pointing out the item to add in slot
	 * @return L2ItemInstance designating the item placed in the slot before
	 */
	public L2ItemInstance setPaperdollItem(short slot, L2ItemInstance item)
	{
		L2ItemInstance old = _paperdoll[slot];
		if(old != item)
		{
			if(old != null)
			{
				_paperdoll[slot] = null;
				// Put old item from paperdoll slot to base location
				old.setLocation(getBaseLocation());
				old.setLastChange(L2ItemInstance.MODIFIED);

				int mask = 0;
				for(int i = 0; i < PAPERDOLL_MAX; i++)
				{
					L2ItemInstance pi = _paperdoll[i];
					if(pi != null)
						mask |= pi.getItem().getItemMask();
				}
				_wearedMask = mask;
				for(PaperdollListener listener : _paperdollListeners)
					listener.notifyUnequipped(slot, old);

				old.updateDatabase();
			}
			// Add new item in slot of paperdoll
			if(item != null)
			{
				_paperdoll[slot] = item;
				item.setLocation(getEquipLocation(), slot);
				item.setLastChange(L2ItemInstance.MODIFIED);

				_wearedMask |= item.getItem().getItemMask();
				for(PaperdollListener listener : _paperdollListeners)
					listener.notifyEquipped(slot, item);

				item.updateDatabase();
			}
		}

		return old;
	}

	/**
	 * Return the mask of weared item
	 *
	 * @return int
	 */
	public int getWearedMask()
	{
		return _wearedMask;
	}

	public void unEquipItem(L2ItemInstance item)
	{
		unEquipItemInBodySlot(item.getBodyPart(), item);
	}

	/**
	 * Unepquips item in slot and returns alterations
	 *
	 * @param item : int designating the slot
	 * @return L2ItemInstance[] : list of items altered
	 */
	public GArray<L2ItemInstance> unEquipItemAndRecord(L2ItemInstance item)
	{
		ChangeRecorder changeRecorder = new ChangeRecorder();
		addPaperdollListener(changeRecorder);

		try
		{
			unEquipItem(item);
		}
		finally
		{
			removePaperdollListener(changeRecorder);
		}

		return changeRecorder.getChangedItems();
	}

	public void unEquipItemAndSendChanges(L2ItemInstance item)
	{
		GArray<L2ItemInstance> items = unEquipItemAndRecord(item);
		getOwner().sendChanges();
		if(items.size() > 0)
			getOwner().sendPacket(getOwner().isPet() ? new PetInventoryUpdate(items) : new InventoryUpdate(items));
	}

	private void unEquipItemInBodySlot(int slot, L2ItemInstance item)
	{
		byte pdollSlot = -1;

		switch(slot)
		{
			case L2Item.SLOT_NECK:
				pdollSlot = PAPERDOLL_NECK;
				break;
			case L2Item.SLOT_L_EAR:
				pdollSlot = PAPERDOLL_LEAR;
				break;
			case L2Item.SLOT_R_EAR:
				pdollSlot = PAPERDOLL_REAR;
				break;
			case L2Item.SLOT_L_FINGER:
				pdollSlot = PAPERDOLL_LFINGER;
				break;
			case L2Item.SLOT_R_FINGER:
				pdollSlot = PAPERDOLL_RFINGER;
				break;
			case L2Item.SLOT_HAIR:
				pdollSlot = PAPERDOLL_HAIR;
				break;
			case L2Item.SLOT_DHAIR:
				pdollSlot = PAPERDOLL_DHAIR;
				break;
			case L2Item.SLOT_HAIRALL:
				setPaperdollItem(PAPERDOLL_HAIR, null);
				setPaperdollItem(PAPERDOLL_DHAIR, null); // This should be the same as in DHAIR
				pdollSlot = PAPERDOLL_HAIR;
				break;
			case L2Item.SLOT_HEAD:
				pdollSlot = PAPERDOLL_HEAD;
				break;
			case L2Item.SLOT_R_HAND:
				pdollSlot = PAPERDOLL_RHAND;
				break;
			case L2Item.SLOT_L_HAND:
				pdollSlot = PAPERDOLL_LHAND;
				break;
			case L2Item.SLOT_GLOVES:
				pdollSlot = PAPERDOLL_GLOVES;
				break;
			case L2Item.SLOT_LEGS:
				pdollSlot = PAPERDOLL_LEGS;
				break;
			case L2Item.SLOT_CHEST:
			case L2Item.SLOT_FULL_ARMOR:
			case L2Item.SLOT_FORMAL_WEAR:
				pdollSlot = PAPERDOLL_CHEST;
				break;
			case L2Item.SLOT_BACK:
				pdollSlot = PAPERDOLL_BACK;
				break;
			case L2Item.SLOT_FEET:
				pdollSlot = PAPERDOLL_FEET;
				break;
			case L2Item.SLOT_UNDERWEAR:
				pdollSlot = PAPERDOLL_UNDER;
				break;
			case L2Item.SLOT_BELT:
				pdollSlot = PAPERDOLL_BELT;
				break;
			case L2Item.SLOT_LR_HAND:
				setPaperdollItem(PAPERDOLL_LHAND, null);
				setPaperdollItem(PAPERDOLL_RHAND, null); // this should be the same as in LRHAND
				pdollSlot = PAPERDOLL_RHAND;
				break;
			case L2Item.SLOT_L_BRACELET:
				pdollSlot = PAPERDOLL_LBRACELET;
				break;
			case L2Item.SLOT_R_BRACELET:
				// При снятии браслета снемаем все талисманы
				if(getPaperdollItem(Inventory.PAPERDOLL_DECO1) != null)
				{
					L2ItemInstance talisman = getPaperdollItem(Inventory.PAPERDOLL_DECO1);
					unEquipItem(talisman);
				}
				if(getPaperdollItem(Inventory.PAPERDOLL_DECO2) != null)
				{
					L2ItemInstance talisman = getPaperdollItem(Inventory.PAPERDOLL_DECO2);
					unEquipItem(talisman);
				}
				if(getPaperdollItem(Inventory.PAPERDOLL_DECO3) != null)
				{
					L2ItemInstance talisman = getPaperdollItem(Inventory.PAPERDOLL_DECO3);
					unEquipItem(talisman);
				}
				if(getPaperdollItem(Inventory.PAPERDOLL_DECO4) != null)
				{
					L2ItemInstance talisman = getPaperdollItem(Inventory.PAPERDOLL_DECO4);
					unEquipItem(talisman);
				}
				if(getPaperdollItem(Inventory.PAPERDOLL_DECO5) != null)
				{
					L2ItemInstance talisman = getPaperdollItem(Inventory.PAPERDOLL_DECO5);
					unEquipItem(talisman);
				}
				if(getPaperdollItem(Inventory.PAPERDOLL_DECO6) != null)
				{
					L2ItemInstance talisman = getPaperdollItem(Inventory.PAPERDOLL_DECO6);
					unEquipItem(talisman);
				}
				pdollSlot = PAPERDOLL_RBRACELET;
				break;
			case L2Item.SLOT_DECO:
				if(item == null)
					return;
				if(getPaperdollObjectId(PAPERDOLL_DECO1) == item.getObjectId())
					pdollSlot = PAPERDOLL_DECO1;
				else if(getPaperdollObjectId(PAPERDOLL_DECO2) == item.getObjectId())
					pdollSlot = PAPERDOLL_DECO2;
				else if(getPaperdollObjectId(PAPERDOLL_DECO3) == item.getObjectId())
					pdollSlot = PAPERDOLL_DECO3;
				else if(getPaperdollObjectId(PAPERDOLL_DECO4) == item.getObjectId())
					pdollSlot = PAPERDOLL_DECO4;
				else if(getPaperdollObjectId(PAPERDOLL_DECO5) == item.getObjectId())
					pdollSlot = PAPERDOLL_DECO5;
				else if(getPaperdollObjectId(PAPERDOLL_DECO6) == item.getObjectId())
					pdollSlot = PAPERDOLL_DECO6;
				break;
			default:
				_log.warn("Requested invalid body slot!!! " + slot);
				Thread.dumpStack();
		}
		if(pdollSlot >= 0)
			setPaperdollItem(pdollSlot, null);
	}

	public L2ItemInstance getItemInBodySlot(int slot)
	{
		byte pdollSlot = -1;

		switch(slot)
		{
			case L2Item.SLOT_NECK:
				pdollSlot = PAPERDOLL_NECK;
				break;
			case L2Item.SLOT_L_EAR:
				pdollSlot = PAPERDOLL_LEAR;
				break;
			case L2Item.SLOT_R_EAR:
				pdollSlot = PAPERDOLL_REAR;
				break;
			case L2Item.SLOT_L_FINGER:
				pdollSlot = PAPERDOLL_LFINGER;
				break;
			case L2Item.SLOT_R_FINGER:
				pdollSlot = PAPERDOLL_RFINGER;
				break;
			case L2Item.SLOT_HAIR:
				pdollSlot = PAPERDOLL_HAIR;
				break;
			case L2Item.SLOT_DHAIR:
				pdollSlot = PAPERDOLL_DHAIR;
				break;
			case L2Item.SLOT_HAIRALL:
				pdollSlot = PAPERDOLL_DHAIR;
				break;
			case L2Item.SLOT_HEAD:
				pdollSlot = PAPERDOLL_HEAD;
				break;
			case L2Item.SLOT_R_HAND:
				pdollSlot = PAPERDOLL_RHAND;
				break;
			case L2Item.SLOT_L_HAND:
				pdollSlot = PAPERDOLL_LHAND;
				break;
			case L2Item.SLOT_GLOVES:
				pdollSlot = PAPERDOLL_GLOVES;
				break;
			case L2Item.SLOT_LEGS:
				pdollSlot = PAPERDOLL_LEGS;
				break;
			case L2Item.SLOT_CHEST:
			case L2Item.SLOT_FULL_ARMOR:
			case L2Item.SLOT_FORMAL_WEAR:
				pdollSlot = PAPERDOLL_CHEST;
				break;
			case L2Item.SLOT_BACK:
				pdollSlot = PAPERDOLL_BACK;
				break;
			case L2Item.SLOT_FEET:
				pdollSlot = PAPERDOLL_FEET;
				break;
			case L2Item.SLOT_UNDERWEAR:
				pdollSlot = PAPERDOLL_UNDER;
				break;
			case L2Item.SLOT_BELT:
				pdollSlot = PAPERDOLL_BELT;
				break;
			case L2Item.SLOT_LR_HAND:
				pdollSlot = PAPERDOLL_RHAND;
				break;
			case L2Item.SLOT_L_BRACELET:
				pdollSlot = PAPERDOLL_LBRACELET;
				break;
			case L2Item.SLOT_R_BRACELET:
				pdollSlot = PAPERDOLL_RBRACELET;
				break;
			case L2Item.SLOT_DECO:
				if(getPaperdollObjectId(PAPERDOLL_DECO1) != 0)
					pdollSlot = PAPERDOLL_DECO1;
				else if(getPaperdollObjectId(PAPERDOLL_DECO2) != 0)
					pdollSlot = PAPERDOLL_DECO2;
				else if(getPaperdollObjectId(PAPERDOLL_DECO3) != 0)
					pdollSlot = PAPERDOLL_DECO3;
				else if(getPaperdollObjectId(PAPERDOLL_DECO4) != 0)
					pdollSlot = PAPERDOLL_DECO4;
				else if(getPaperdollObjectId(PAPERDOLL_DECO5) != 0)
					pdollSlot = PAPERDOLL_DECO5;
				else if(getPaperdollObjectId(PAPERDOLL_DECO6) != 0)
					pdollSlot = PAPERDOLL_DECO6;
				break;
			default:
				_log.warn("Requested invalid body slot!!! " + slot);
				Thread.dumpStack();
		}
		if(pdollSlot >= 0)
			return getPaperdollItem(pdollSlot);

		return null;
	}

	public int getCurrentTalismanCount()
	{
		int count = 0;
		for(int i = PAPERDOLL_DECO1; i <= PAPERDOLL_DECO6; i++)
			if(_paperdoll[i] != null)
				count++;
		return count;
	}

	public GArray<L2ItemInstance> equipItemAndRecord(L2ItemInstance item)
	{
		ChangeRecorder changeRecorder = new ChangeRecorder();
		addPaperdollListener(changeRecorder);

		try
		{
			equipItem(item);
		}
		finally
		{
			removePaperdollListener(changeRecorder);
		}

		return changeRecorder.getChangedItems();
	}

	/**
	 * Equips item in slot of paperdoll.
	 *
	 * @param item : L2ItemInstance designating the item and slot used.
	 */
	public synchronized void equipItem(L2ItemInstance item)
	{
		if(getOwner().isPlayer() && getOwner().getName() != null)
		{
			L2Player owner = (L2Player) getOwner();

			if(owner.getRace().equals(Race.kamael) && ((item.getItemType().equals(HEAVY) || item.getItemType().equals(MAGIC) || item.getItemType().equals(NONE)) && !item.isFortFlag() && !item.isTerritoryWard() && !(item.getBodyPart() == SLOT_FORMAL_WEAR)))
			{
				owner.sendPacket(Msg.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
				return;
			}

			if(!owner.getRace().equals(Race.kamael) && (item.getItemType().equals(CROSSBOW) || item.getItemType().equals(RAPIER) || item.getItemType().equals(ANCIENTSWORD)))
			{
				owner.sendPacket(Msg.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
				return;
			}

			// нельзя одеть талисманы без браслета
			if(item.getItem().getBodyPart() == L2Item.SLOT_DECO && getAllowedTalismans() <= 0)
			{
				owner.sendPacket(Msg.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
				return;
			}
			// нельзя одеть два одинаковых талисмана
			for(int decoId = PAPERDOLL_DECO1; decoId <= PAPERDOLL_DECO6; decoId++)
				if(_paperdoll[decoId] != null && item.getItemId() == _paperdoll[decoId].getItemId())
				{
					owner.sendPacket(Msg.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
					return;
				}

			// нельзя одеть больше талисманов чем позволяет браслет
			for(int decoId = PAPERDOLL_DECO1; decoId <= PAPERDOLL_DECO6; decoId++)
			{
				int count = getAllowedTalismans() - getCurrentTalismanCount();
				if(count == 0 && getCurrentTalismanCount() != 0 && item.getItem().getBodyPart() == L2Item.SLOT_DECO)
				{
					owner.sendPacket(Msg.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
					return;
				}
			}
		}

		int targetSlot = item.getItem().getBodyPart();

		// Нельзя одевать оружие, если уже одето проклятое оружие
		if(CursedWeaponsManager.getInstance().isCursed(getPaperdollItemId(PAPERDOLL_LRHAND)) && (targetSlot == L2Item.SLOT_LR_HAND || targetSlot == L2Item.SLOT_L_HAND || targetSlot == L2Item.SLOT_R_HAND))
			return;

		double mp = 0; // при смене робы ману не сбрасываем
		//item.setOwner(getOwner()); // на всякий случай форсируем установку овнера для призрачного оружия
		switch(targetSlot)
		{
			case L2Item.SLOT_LR_HAND:
			{
				disarmAndDischarge(PAPERDOLL_LHAND);
				setPaperdollItem(PAPERDOLL_LHAND, null);

				disarmAndDischarge(PAPERDOLL_RHAND);
				setPaperdollItem(PAPERDOLL_RHAND, item);
				break;
			}

			case L2Item.SLOT_L_HAND:
			{
				L2ItemInstance slot = getPaperdollItem(PAPERDOLL_RHAND);

				L2Item oldItem = slot == null ? null : slot.getItem();
				L2Item newItem = item.getItem();

				if(oldItem != null && newItem.getItemType() == EtcItemType.ARROW && oldItem.getItemType() == WeaponType.BOW && oldItem.getCrystalType().cry != newItem.getCrystalType().cry)
					return;
				if(oldItem != null && newItem.getItemType() == EtcItemType.BOLT && oldItem.getItemType() == WeaponType.CROSSBOW && oldItem.getCrystalType().cry != newItem.getCrystalType().cry)
					return;

				if(newItem.getItemType() != EtcItemType.ARROW && newItem.getItemType() != EtcItemType.BOLT && newItem.getItemType() != EtcItemType.BAIT)
				{
					if(oldItem != null && oldItem.getBodyPart() == L2Item.SLOT_LR_HAND)
					{
						disarmAndDischarge(PAPERDOLL_RHAND);
						setPaperdollItem(PAPERDOLL_RHAND, null);
						disarmAndDischarge(PAPERDOLL_LHAND);
						setPaperdollItem(PAPERDOLL_LHAND, null);
					}
					else
					{
						disarmAndDischarge(PAPERDOLL_LHAND);
						setPaperdollItem(PAPERDOLL_LHAND, null);
					}
					setPaperdollItem(PAPERDOLL_LHAND, item);
				}
				else if(item.getLocation() == ItemLocation.PAPERDOLL || oldItem != null && (newItem.getItemType() == EtcItemType.ARROW && oldItem.getItemType() == WeaponType.BOW || newItem.getItemType() == EtcItemType.BOLT && oldItem.getItemType() == WeaponType.CROSSBOW || newItem.getItemType() == EtcItemType.BAIT && oldItem.getItemType() == WeaponType.ROD))
				{
					disarmAndDischarge(PAPERDOLL_LHAND);
					setPaperdollItem(PAPERDOLL_LHAND, item);
				}
				break;
			}

			case L2Item.SLOT_R_HAND:
			{
				disarmAndDischarge(PAPERDOLL_RHAND);
				setPaperdollItem(PAPERDOLL_RHAND, item);
				break;
			}
			case L2Item.SLOT_L_EAR:
			case L2Item.SLOT_R_EAR:
			case L2Item.SLOT_L_EAR | L2Item.SLOT_R_EAR:
			{
				if(_paperdoll[PAPERDOLL_LEAR] == null)
				{
					item.setBodyPart(L2Item.SLOT_L_EAR);
					setPaperdollItem(PAPERDOLL_LEAR, item);
				}
				else if(_paperdoll[PAPERDOLL_REAR] == null)
				{
					item.setBodyPart(L2Item.SLOT_R_EAR);
					setPaperdollItem(PAPERDOLL_REAR, item);
				}
				else
				{
					item.setBodyPart(L2Item.SLOT_L_EAR);
					setPaperdollItem(PAPERDOLL_LEAR, null);
					setPaperdollItem(PAPERDOLL_LEAR, item);
				}
				break;
			}
			case L2Item.SLOT_L_FINGER:
			case L2Item.SLOT_R_FINGER:
			case L2Item.SLOT_L_FINGER | L2Item.SLOT_R_FINGER:
			{
				if(_paperdoll[PAPERDOLL_LFINGER] == null)
				{
					item.setBodyPart(L2Item.SLOT_L_FINGER);
					setPaperdollItem(PAPERDOLL_LFINGER, item);
				}
				else if(_paperdoll[PAPERDOLL_RFINGER] == null)
				{
					item.setBodyPart(L2Item.SLOT_R_FINGER);
					setPaperdollItem(PAPERDOLL_RFINGER, item);
				}
				else
				{
					item.setBodyPart(L2Item.SLOT_L_FINGER);
					setPaperdollItem(PAPERDOLL_LFINGER, null);
					setPaperdollItem(PAPERDOLL_LFINGER, item);
				}
				break;
			}
			case L2Item.SLOT_NECK:
				setPaperdollItem(PAPERDOLL_NECK, item);
				break;
			case L2Item.SLOT_FULL_ARMOR:
				if(getOwner() != null)
					mp = getOwner().getCurrentMp();
				setPaperdollItem(PAPERDOLL_CHEST, null);
				setPaperdollItem(PAPERDOLL_LEGS, null);
				setPaperdollItem(PAPERDOLL_CHEST, item);
				if(mp > getOwner().getCurrentMp())
					getOwner().setCurrentMp(mp);
				break;
			case L2Item.SLOT_CHEST:
				if(getOwner() != null)
					mp = getOwner().getCurrentMp();
				setPaperdollItem(PAPERDOLL_CHEST, item);
				if(mp > getOwner().getCurrentMp())
					getOwner().setCurrentMp(mp);
				break;
			case L2Item.SLOT_LEGS:
			{
				// handle full armor
				L2ItemInstance chest = getPaperdollItem(PAPERDOLL_CHEST);
				if(chest != null && chest.getBodyPart() == L2Item.SLOT_FULL_ARMOR)
					setPaperdollItem(PAPERDOLL_CHEST, null);

				if(getPaperdollItemId(PAPERDOLL_CHEST) == 6408)
					setPaperdollItem(PAPERDOLL_CHEST, null);

				if(getOwner() != null)
					mp = getOwner().getCurrentMp();
				setPaperdollItem(PAPERDOLL_LEGS, null);
				setPaperdollItem(PAPERDOLL_LEGS, item);
				if(mp > getOwner().getCurrentMp())
					getOwner().setCurrentMp(mp);
				break;
			}
			case L2Item.SLOT_FEET:
				if(getPaperdollItemId(PAPERDOLL_CHEST) == 6408)
					setPaperdollItem(PAPERDOLL_CHEST, null);
				setPaperdollItem(PAPERDOLL_FEET, item);
				break;
			case L2Item.SLOT_GLOVES:
				if(getPaperdollItemId(PAPERDOLL_CHEST) == 6408)
					setPaperdollItem(PAPERDOLL_CHEST, null);
				setPaperdollItem(PAPERDOLL_GLOVES, item);
				break;
			case L2Item.SLOT_HEAD:
				if(getPaperdollItemId(PAPERDOLL_CHEST) == 6408)
					setPaperdollItem(PAPERDOLL_CHEST, null);
				setPaperdollItem(PAPERDOLL_HEAD, item);
				break;
			case L2Item.SLOT_HAIR:
				L2ItemInstance slot = getPaperdollItem(PAPERDOLL_DHAIR);
				if(slot != null && slot.getItem().getBodyPart() == L2Item.SLOT_HAIRALL)
				{
					setPaperdollItem(PAPERDOLL_HAIR, null);
					setPaperdollItem(PAPERDOLL_DHAIR, null);
				}
				setPaperdollItem(PAPERDOLL_HAIR, item);
				break;
			case L2Item.SLOT_DHAIR:
				L2ItemInstance slot2 = getPaperdollItem(PAPERDOLL_DHAIR);
				if(slot2 != null && slot2.getItem().getBodyPart() == L2Item.SLOT_HAIRALL)
				{
					setPaperdollItem(PAPERDOLL_HAIR, null);
					setPaperdollItem(PAPERDOLL_DHAIR, null);
				}
				setPaperdollItem(PAPERDOLL_DHAIR, item);
				break;
			case L2Item.SLOT_HAIRALL:
				setPaperdollItem(PAPERDOLL_HAIR, null);
				setPaperdollItem(PAPERDOLL_DHAIR, null);
				setPaperdollItem(PAPERDOLL_DHAIR, item);
				break;
			case L2Item.SLOT_R_BRACELET:
				if(getPaperdollItem(Inventory.PAPERDOLL_DECO1) != null)
				{
					int currentCount = getAllowedTalismans();
					int tCount = item.getItem().getFirstSkill().getId() - 3321;
					if(currentCount > tCount && getCurrentTalismanCount() > tCount)
					{
						int toUnEuip = getCurrentTalismanCount() - tCount;

						for(int decoslot = toUnEuip; decoslot > 0; decoslot--)
						{
							if(getPaperdollItem(Inventory.PAPERDOLL_DECO6) != null)
							{
								L2ItemInstance talisman = getPaperdollItem(Inventory.PAPERDOLL_DECO6);
								unEquipItem(talisman);
								continue;
							}
							if(getPaperdollItem(Inventory.PAPERDOLL_DECO5) != null)
							{
								L2ItemInstance talisman = getPaperdollItem(Inventory.PAPERDOLL_DECO5);
								unEquipItem(talisman);
								continue;
							}
							if(getPaperdollItem(Inventory.PAPERDOLL_DECO4) != null)
							{
								L2ItemInstance talisman = getPaperdollItem(Inventory.PAPERDOLL_DECO4);
								unEquipItem(talisman);
								continue;
							}
							if(getPaperdollItem(Inventory.PAPERDOLL_DECO3) != null)
							{
								L2ItemInstance talisman = getPaperdollItem(Inventory.PAPERDOLL_DECO3);
								unEquipItem(talisman);
								continue;
							}
							if(getPaperdollItem(Inventory.PAPERDOLL_DECO2) != null)
							{
								L2ItemInstance talisman = getPaperdollItem(Inventory.PAPERDOLL_DECO2);
								unEquipItem(talisman);
								continue;
							}
							//Не должно никогда случиться но пусть будет на случай если чьинить кривые руки забудут браслету дать скил на ячейки
							if(getPaperdollItem(Inventory.PAPERDOLL_DECO1) != null)
							{
								L2ItemInstance talisman = getPaperdollItem(Inventory.PAPERDOLL_DECO1);
								unEquipItem(talisman);
							}
						}
					}
				}
				setPaperdollItem(PAPERDOLL_RBRACELET, null);
				setPaperdollItem(PAPERDOLL_RBRACELET, item);
				break;
			case L2Item.SLOT_L_BRACELET:
				setPaperdollItem(PAPERDOLL_LBRACELET, null);
				setPaperdollItem(PAPERDOLL_LBRACELET, item);
				break;
			case L2Item.SLOT_UNDERWEAR:
				setPaperdollItem(PAPERDOLL_UNDER, item);
				break;
			case L2Item.SLOT_BACK:
				setPaperdollItem(PAPERDOLL_BACK, item);
				break;
			case L2Item.SLOT_BELT:
				setPaperdollItem(PAPERDOLL_BELT, item);
				break;
			case L2Item.SLOT_DECO:
				boolean talisman_equiped = false;
				if(_paperdoll[PAPERDOLL_DECO1] == null)
				{
					setPaperdollItem(PAPERDOLL_DECO1, item);
					talisman_equiped = true;
					break;
				}
				if(_paperdoll[PAPERDOLL_DECO2] == null)
				{
					setPaperdollItem(PAPERDOLL_DECO2, item);
					talisman_equiped = true;
					break;
				}
				if(_paperdoll[PAPERDOLL_DECO3] == null)
				{
					setPaperdollItem(PAPERDOLL_DECO3, item);
					talisman_equiped = true;
					break;
				}
				if(_paperdoll[PAPERDOLL_DECO4] == null)
				{
					setPaperdollItem(PAPERDOLL_DECO4, item);
					talisman_equiped = true;
					break;
				}
				if(_paperdoll[PAPERDOLL_DECO5] == null)
				{
					setPaperdollItem(PAPERDOLL_DECO5, item);
					talisman_equiped = true;
					break;
				}
				if(_paperdoll[PAPERDOLL_DECO6] == null)
				{
					setPaperdollItem(PAPERDOLL_DECO6, item);
					talisman_equiped = true;
				}
				if(!talisman_equiped && getAllowedTalismans() > 0)
					setPaperdollItem(PAPERDOLL_DECO1, item);
				break;

			case L2Item.SLOT_FORMAL_WEAR:
				// При одевании свадебного платья руки не трогаем
				setPaperdollItem(PAPERDOLL_LEGS, null);
				setPaperdollItem(PAPERDOLL_CHEST, null);
				setPaperdollItem(PAPERDOLL_HEAD, null);
				setPaperdollItem(PAPERDOLL_FEET, null);
				setPaperdollItem(PAPERDOLL_GLOVES, null);
				setPaperdollItem(PAPERDOLL_CHEST, item);
				break;
			default:
				_log.warn("unknown body slot:" + targetSlot + " for item id: " + item.getItemId());
		}
		//  Зачем тут это? автососки включаются из UseItem.
		//  Проверил, вроде все нормально. Если где-то не будут включаться надо смотреть.
		//		if(getOwner().isPlayer())
		//			((L2Player) getOwner()).AutoShot();
	}

	/**
	 * Returns the item from inventory by using its <B>itemId</B>
	 *
	 * @param itemId : int designating the ID of the item
	 * @return L2ItemInstance designating the item or null if not found in
	 *         inventory
	 */
	public L2ItemInstance getItemByItemId(int itemId)
	{
		for(L2ItemInstance temp : _items)
			if(temp.getItemId() == itemId)
				return temp;
		return null;
	}

	public GArray<L2ItemInstance> getAllItemsById(int itemId)
	{
		GArray<L2ItemInstance> ar = new GArray<L2ItemInstance>();
		for(L2ItemInstance i : _items)
			if(i.getItemId() == itemId)
				ar.add(i);
		return ar;
	}

	public int getPaperdollAugmentationId(int slot)
	{
		L2ItemInstance item = _paperdoll[slot];
		if(item != null && item.getAugmentation() != null)
			return item.getAugmentation().getAugmentationId();
		return 0;
	}

	public void disarmAndDischarge(int slot)
	{
		L2ItemInstance ii;
		ii = getPaperdollItem(slot);
		if(ii != null && getOwner().isPlayer())
		{
			SystemMessage sm;
			if(ii.getEnchantLevel() > 0)
			{
				sm = new SystemMessage(SystemMessage.EQUIPMENT_OF__S1_S2_HAS_BEEN_REMOVED);
				sm.addNumber(ii.getEnchantLevel());
				sm.addItemName(ii.getItemId());
				getOwner().sendPacket(sm);
			}
			else if(ii.getItemType() != EtcItemType.ARROW && ii.getItemType() != EtcItemType.BOLT && ii.getItemType() != EtcItemType.BAIT)
			{
				sm = new SystemMessage(SystemMessage.S1__HAS_BEEN_DISARMED).addItemName(ii.getItemId());
				getOwner().sendPacket(sm);
			}
			if(ii.getChargedSpiritshot() != L2ItemInstance.CHARGED_NONE)
				getOwner().sendPacket(new SystemMessage(SystemMessage.POWER_OF_MANA_DISABLED));
			if(ii.getChargedSoulshot() != L2ItemInstance.CHARGED_NONE)
				getOwner().sendPacket(new SystemMessage(SystemMessage.POWER_OF_THE_SPIRITS_DISABLED));
			ii.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
			ii.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
		}
	}

	public L2ItemInstance getItemByItemIdExclude(int itemId, Set<L2ItemInstance> excludeObjIds)
	{
		for(L2ItemInstance temp : _items)
			if(temp.getItemId() == itemId && !excludeObjIds.contains(temp))
				return temp;
		return null;
	}

	/**
	 * Returns item from inventory by using its <B>objectId</B>
	 *
	 * @param objectId : int designating the ID of the object
	 * @return L2ItemInstance designating the item or null if not found in
	 *         inventory
	 */
	public L2ItemInstance getItemByObjectId(Integer objectId)
	{
		for(L2ItemInstance temp : _items)
			if(temp.getObjectId() == objectId)
				return temp;
		return null;
	}

	public boolean resetItem(L2ItemInstance item)
	{
		if(item == null)
			return false;
		if(item.getLocation() == ItemLocation.MARKET)
		{
			synchronized(item)
			{
				if(!_items.contains(item))
					return false;

				removeItem(item);
				item.setLastChange(L2ItemInstance.REMOVED);
				sendRemoveItem(item);
				item.updateDatabase(true);

				for(InventoryListener listener : inventoryListeners)
					listener.itemRemoved(item);
			}
		}
		else
		{
			item.setLastChange((L2ItemInstance.ADDED));
			// Add item in inventory
			addItem(item);
			// Updates database
			item.updateDatabase(true);
			sendNewItem(item);

			for(InventoryListener listener : inventoryListeners)
				listener.itemAdded(item);
		}
		//sendModifyItem(item);
		refreshWeight();
		return true;
	}

	/**
	 * Destroy item from inventory by using its <B>objectID</B> and updates database
	 *
	 * @param process   : String Identifier of process triggering this action
	 * @param objectId  : int Item Instance identifier of the item to be destroyed
	 * @param count	 : int Quantity of items to be destroyed
	 * @param actor	 : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	public L2ItemInstance destroyItem(String process, int objectId, long count, L2Player actor, L2Object reference)
	{
		L2ItemInstance item = getItemByObjectId(objectId);
		if(item == null)
			return null;

		if(this instanceof PcInventory && !process.equalsIgnoreCase("Trade") && getOwner().isPlayer() && ((L2Player) getOwner()).isTradeInProgress())
			((L2Player) getOwner()).cancelActiveTrade();

		// Adjust item quantity
		if(item.getCount() > count)
		{
			synchronized(item)
			{
				item.changeCount(process, -count, actor, reference);
				item.setLastChange(L2ItemInstance.MODIFIED);
				sendModifyItem(item);
				item.updateDatabase(this instanceof Warehouse);
				refreshWeight();
			}
			return item;
		}
		// Directly drop entire item
		else
			return destroyItem(process, item, actor, reference);
	}


	/**
	 * Destroy item from inventory and updates database
	 *
	 * @param process   : String Identifier of process triggering this action
	 * @param item	  : L2ItemInstance to be destroyed
	 * @param actor	 : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	public L2ItemInstance destroyItem(String process, L2ItemInstance item, L2Player actor, L2Object reference)
	{
		if(this instanceof PcInventory && !process.equalsIgnoreCase("Trade") && getOwner().isPlayer() && ((L2Player) getOwner()).isTradeInProgress())
			((L2Player) getOwner()).cancelActiveTrade();

		synchronized(item)
		{
			// check if item is present in this container
			if(!_items.contains(item))
				return null;

			removeItem(item);
			ItemTable.getInstance().destroyItem(process, item, actor, reference);
			sendRemoveItem(item);
			item.updateDatabase();
			refreshWeight();
			if(item.getItemType() == EtcItemType.RUNE_SELECT || item.getItemType() == EtcItemType.RUNE)
				for(PaperdollListener listener : _paperdollListeners)
					listener.notifyUnequipped(-1, item);

			for(InventoryListener listener : inventoryListeners)
				listener.itemRemoved(item);
		}
		return item;
	}

	protected void sendModifyItem(L2ItemInstance item)
	{
		if(getOwner() == null)
			return;

		if(getOwner().isPet())
			getOwner().getPlayer().sendPacket(new PetInventoryUpdate().addModifiedItem(item));
		else
			getOwner().sendPacket(new InventoryUpdate().addModifiedItem(item));
	}

	protected void sendRemoveItem(L2ItemInstance item)
	{
		if(getOwner() == null)
			return;

		if(getOwner().isPet())
			getOwner().getPlayer().sendPacket(new PetInventoryUpdate().addRemovedItem(item));
		else
			getOwner().sendPacket(new InventoryUpdate().addRemovedItem(item));
	}

	protected void sendNewItem(L2ItemInstance item)
	{
		if(getOwner() == null)
			return;

		if(getOwner().isPet())
			getOwner().getPlayer().sendPacket(new PetInventoryUpdate().addNewItem(item));
		else
			getOwner().sendPacket(new InventoryUpdate().addNewItem(item));
	}

	/**
	 * Destroy item from inventory by using its <B>itemId</B> and updates database
	 *
	 * @param process   : String Identifier of process triggering this action
	 * @param itemId	: int Item identifier of the item to be destroyed
	 * @param count	 : int Quantity of items to be destroyed
	 * @param actor	 : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	public L2ItemInstance destroyItemByItemId(String process, int itemId, long count, L2Player actor, L2Object reference)
	{
		L2ItemInstance item = getItemByItemId(itemId);
		if(item == null) return null;

		if(this instanceof PcInventory && !process.equalsIgnoreCase("Trade") && getOwner().isPlayer() && ((L2Player) getOwner()).isTradeInProgress())
			((L2Player) getOwner()).cancelActiveTrade();

		synchronized(item)
		{
			// Adjust item quantity
			if(item.getCount() > count)
			{
				item.changeCount(process, -count, actor, reference);
				item.setLastChange(L2ItemInstance.MODIFIED);
				sendModifyItem(item);
			}
			// Directly drop entire item
			else return destroyItem(process, item, actor, reference);

			item.updateDatabase(this instanceof Warehouse);
			refreshWeight();
		}
		return item;
	}

	/**
	 * Destroy item from inventory and from database.
	 *
	 * param item	   : L2ItemInstance designating the item to remove from inventory
	 * param clearCount : boolean : if true, set the item quantity to 0
	 */
	private void removeItem(L2ItemInstance item)
	{
		if(getOwner() == null)
			return;

		if(getOwner().isPlayer())
		{
			L2Player player = (L2Player) getOwner();
			player.removeItemFromShortCut(item.getObjectId());
			if(item.isEquipped())
				unEquipItemAndSendChanges(item);
		}

		_items.remove(item);
	}

	/**
	 * Drop item from inventory by using its <B>objectID</B> and updates database
	 *
	 * @param process   : String Identifier of process triggering this action
	 * @param objectId  : int Item Instance identifier of the item to be dropped
	 * @param count	 : int Quantity of items to be dropped
	 * @param actor	 : L2PcInstance Player requesting the item drop
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	public L2ItemInstance dropItem(String process, int objectId, long count, L2Player actor, L2Object reference)
	{
		L2ItemInstance item = getItemByObjectId(objectId);
		if(item == null || count <= 0)
			return null;

		if(this instanceof PcInventory && !process.equalsIgnoreCase("Trade") && getOwner().isPlayer() && ((L2Player) getOwner()).isTradeInProgress())
			((L2Player) getOwner()).cancelActiveTrade();

		synchronized(item)
		{
			// Adjust item quantity and create new instance to drop
			if(item.getCount() > count)
			{
				item.changeCount(process, -count, actor, reference);
				item.setLastChange(L2ItemInstance.MODIFIED);
				item.updateDatabase();
				sendModifyItem(item);

				item = ItemTable.getInstance().createItem(process, item.getItemId(), count, actor, reference);
				item.updateDatabase();
				refreshWeight();
				return item;
			}
			// Directly drop entire item
			else
				return dropItem(process, item, actor, reference);
		}
	}


	/**
	 * Drop item from inventory and updates database
	 *
	 * @param process   : String Identifier of process triggering this action
	 * @param item	  : L2ItemInstance to be dropped
	 * @param actor	 : L2PcInstance Player requesting the item drop
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	public L2ItemInstance dropItem(String process, L2ItemInstance item, L2Player actor, L2Object reference)
	{
		if(item == null)
			return null;

		if(this instanceof PcInventory && !process.equalsIgnoreCase("Trade") && getOwner().isPlayer() && ((L2Player) getOwner()).isTradeInProgress())
			((L2Player) getOwner()).cancelActiveTrade();

		synchronized(item)
		{
			if(!_items.contains(item))
				return null;

			removeItem(item);
			item.setOwnerId(process, 0, actor, reference);
			item.setLocation(ItemLocation.VOID);
			item.setLastChange(L2ItemInstance.REMOVED);
			sendRemoveItem(item);

			item.updateDatabase();
			if(PetDataTable.isPetControlItem(item))
				PetDataTable.unSummonPet(item, getOwner());
			refreshWeight();

			for(InventoryListener listener : inventoryListeners)
				listener.itemRemoved(item);
		}
		return item;
	}

	/**
	 * Refresh the weight of equipment loaded
	 */
	private void refreshWeight()
	{
		if(getOwner() == null)
			return;

		int weight = 0;

		for(L2ItemInstance element : _items)
			weight += element.getItem().getWeight() * element.getCount();

		_totalWeight = weight;
		// notify char for overload checking
		if(getOwner().isPlayer())
			((L2Player) getOwner()).refreshOverloaded();
		else if(getOwner().isPet())
			((L2PetInstance) getOwner()).broadcastPetInfo();
	}

	/**
	 * Returns the totalWeight.
	 *
	 * @return int
	 */
	public int getTotalWeight()
	{
		return _totalWeight;
	}

	private static final int[][] arrows = {
	//
			{ 17 }, // NG
			{ 1341, 22067 }, // D
			{ 1342, 22068 }, // C
			{ 1343, 22069 }, // B
			{ 1344, 22070 }, // A
			{ 1345, 22071 }, // S
	};

	public L2ItemInstance findArrowForBow(L2Item bow)
	{
		int[] arrowsId = arrows[bow.getCrystalType().externalOrdinal];
		L2ItemInstance ret = null;
		for(int id : arrowsId)
			if((ret = getItemByItemId(id)) != null)
				return ret;
		return null;
	}

	private static final int[][] bolts = {
	//
			{ 9632 }, // NG
			{ 9633, 22144 }, // D
			{ 9634, 22145 }, // C
			{ 9635, 22146 }, // B
			{ 9636, 22147 }, // A
			{ 9637, 22148 }, // S
	};

	public L2ItemInstance findArrowForCrossbow(L2Item xbow)
	{
		int[] boltsId = bolts[xbow.getCrystalType().externalOrdinal];
		L2ItemInstance ret = null;
		for(int id : boltsId)
			if((ret = getItemByItemId(id)) != null)
				return ret;
		return null;
	}

	/**
	 * Delete item object from world
	 */
	public void deleteMe()
	{
		ConcurrentLinkedQueue<L2ItemInstance> items = new ConcurrentLinkedQueue<L2ItemInstance>(_items);
		_items.clear();
		try
		{
			updateDatabase(items, true);
		}
		catch(Throwable t)
		{
			_log.error("deletedMe()", t);
		}
		for(L2ItemInstance inst : items)
			try
			{
				inst.prepareRemove();
			}
			catch(Throwable t)
			{
				_log.error("deletedMe()", t);
			}
	}

	/**
	 * Update database with items in inventory
	 */
	public void updateDatabase()
	{
		updateDatabase(_items, false);
	}

	public void updateDatabase(boolean commit)
	{
		updateDatabase(_items, commit);
	}

	/**
	 * Update database with item
	 *
	 * @param items : ArrayList &lt;L2ItemInstance&gt; pointing out the list of items
	 */
	private void updateDatabase(ConcurrentLinkedQueue<L2ItemInstance> items, boolean commit)
	{
		if(getOwner() != null)
			for(L2ItemInstance inst : items)
				inst.updateDatabase(commit);
	}

	/**
	 * Функция для валдации вещей в инвентаре. Вызывается при загрузке персонажа.
	 */
	public void validateItems()
	{
		for(L2ItemInstance item : getItemsList())
		{
			// Hero Items
			if(item.isHeroItem() && !getOwner().isHero())
			{
				if(item.isEquipped())
					unEquipItem(item);
				// Удаляем все геройские предметы кроме Wings of Destiny Circlet
				if(item.getItemId() != 6842)
					destroyItem("HeroItem", item, getOwner().getPlayer(), null);
			}

			//if(!item.isEquipped())
			//	continue;

			//if(!item.getItem().checkEquipCondition(getOwner()))
			//	unEquipItem(item);
		}
	}

	public int getRuneCountByType(int runeType)
	{
		int c = 0;
		for(L2ItemInstance item : _items)
			if(item.getItemType() == EtcItemType.RUNE_SELECT && item.getItem().getDelayShareGroup() == runeType)
				c++;

		return c;
	}

	public int getRuneMaxLevelByType(int runeType)
	{
		int level = 0;
		for(L2ItemInstance item : _items)
			if(item.getItemType() == EtcItemType.RUNE && item.getItem().getDelayShareGroup() == runeType)
			{
				L2Skill[] skills = item.getItem().getAttachedSkills();
				if(skills != null && skills.length > 0 && skills[0].getLevel() > level)
					level = skills[0].getLevel();
			}

		return level;
	}

	/**
	 * Get back items in inventory from database
	 */
	public void restore()
	{
		final int OWNER = getOwner().getObjectId();

		Connection con = null;
		PreparedStatement statement = null;
		PreparedStatement delete = null;
		ResultSet rset = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM items WHERE owner_id=? AND (loc=? OR loc=?) ORDER BY loc_data,object_id DESC");
			statement.setInt(1, OWNER);
			statement.setString(2, getBaseLocation().name());
			statement.setString(3, getEquipLocation().name());
			rset = statement.executeQuery();

			L2ItemInstance item;
			while(rset.next())
			{
				item = L2ItemInstance.restoreFromDb(rset);

				if(item == null)
					continue;

				if(item.isTerritoryWard())
				{
					item.deleteMe();
					continue;
				}
				// If stackable item is found in inventory just add to current quantity
				if(item.isStackable() && getItemByItemId(item.getItemId()) != null)
					addItem("Restore", item, null, getOwner());
				else
					addItem(item);

				if(item.isEquipped())
					equipItem(item);
			}
			DbUtils.closeQuietly(statement, rset);

			// Delayed add
			statement = con.prepareStatement("SELECT * FROM items_delayed WHERE owner_id=? AND payment_status=0");
			delete = con.prepareStatement("UPDATE items_delayed SET payment_status=1 WHERE payment_id=?");
			statement.setInt(1, OWNER);
			rset = statement.executeQuery();

			while(rset.next())
			{
				final int ITEM_ID = rset.getShort("item_id");
				final int ITEM_COUNT = rset.getInt("count");
				final short ITEM_ENCHANT = rset.getShort("enchant_level");
				final int PAYMENT_ID = rset.getInt("payment_id");
				final int FLAGS = rset.getInt("flags");

				item = ItemTable.getInstance().createItem("delayed_add", ITEM_ID, ITEM_COUNT, getOwner().isPlayer() ? (L2Player) getOwner() : null, null);
				if(item == null)
					continue;
				if(!item.isStackable())
					item.setEnchantLevel(ITEM_ENCHANT);
				item.setOwnerId(OWNER);
				item.setLocation(ItemLocation.INVENTORY);
				item.setCustomFlags(FLAGS);

				if(item.isStackable() && getItemByItemId(item.getItemId()) != null)
					addItem("Restore", item, null, getOwner());
				else
					addItem(item);

				delete.setInt(1, PAYMENT_ID);
				delete.execute();
			}

			refreshWeight();
		}
		catch(Exception e)
		{
			_log.error("could not restore inventory for player " + getOwner().getName() + ":", e);
		}
		finally
		{
			DbUtils.closeQuietly(delete);
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	/**
	 * Refresh all listeners
	 * дергать осторожно, если какой-то предмет дает хп/мп то текущее значение будет сброшено
	 */
	public void refreshListeners()
	{
		setRefreshingListeners(true);
		for(int i = 0; i < _paperdoll.length; i++)
		{
			L2ItemInstance item = getPaperdollItem(i);
			if(item == null)
				continue;
			for(PaperdollListener listener : _paperdollListeners)
			{
				listener.notifyUnequipped(i, item);
				listener.notifyEquipped(i, item);
			}
		}

		for(L2ItemInstance item : _items)
			if(item.getItemType() == EtcItemType.RUNE_SELECT || item.getItemType() == EtcItemType.RUNE)
			{
				for(PaperdollListener listener : _paperdollListeners)
				{
					listener.notifyUnequipped(-1, item);
					listener.notifyEquipped(-1, item);
				}
			}

		getOwner().updateStats();
		setRefreshingListeners(false);
	}

	public void refreshItemListeners(L2ItemInstance refreshItem)
	{
		setRefreshingListeners(true);
		for(int i = 0; i < _paperdoll.length; i++)
		{
			L2ItemInstance item = getPaperdollItem(i);
			if(item == null || item.getObjectId() != refreshItem.getObjectId())
				continue;

			for(PaperdollListener listener : _paperdollListeners)
			{
				listener.notifyUnequipped(i, item);
				listener.notifyEquipped(i, item);
			}
			getOwner().updateStats();
			break;
		}
		setRefreshingListeners(false);
	}

	public boolean isRefreshingListeners()
	{
		return refreshingListeners;
	}

	public void setRefreshingListeners(boolean refreshingListeners)
	{
		this.refreshingListeners = refreshingListeners;
	}

	/**
	 * Возвращает все итемы на пейпердолле.<BR>
	 * Если есть повторяющися итемы, то они игнорируются.<BR>
	 * <B>Удаление итема из даного массива не приведет к удалению из paperdoll</B>
	 *
	 * @return Итемы одетые на персонажа
	 */
	public Vector<L2ItemInstance> getPaperdollItems()
	{
		Vector<L2ItemInstance> ret = new Vector<L2ItemInstance>();
		for(L2ItemInstance i : _paperdoll)
			if(i != null && !ret.contains(i))
				ret.add(i);
		return ret;
	}

	/**
	 * Вызывается из RequestSaveInventoryOrder
	 */
	public void sort(int[][] order)
	{
		L2ItemInstance _item;
		ItemLocation _itemloc;
		for(int[] element : order)
		{
			_item = getItemByObjectId(element[0]);
			if(_item == null)
				continue;
			_itemloc = _item.getLocation();
			if(_itemloc != ItemLocation.INVENTORY)
				continue;
			_item.setLocation(_itemloc, (short) element[1]);
		}
	}

	public long getCountOf(int itemId)
	{
		long result = 0;
		for(L2ItemInstance item : getItems())
			if(item != null && item.getItemId() == itemId)
				result += item.getCount();
		return result;
	}

	public void setAllowedTalismans(int allowedTalismans)
	{
		_allowedTalismans = allowedTalismans;
	}

	public int getAllowedTalismans()
	{
		return _allowedTalismans;
	}

	/**
	 * Transfers item to another inventory
	 *
	 * param process   : String Identifier of process triggering this action
	 * param itemId	: int Item Identifier of the item to be transfered
	 * param count	 : int Quantity of items to be transfered
	 * param actor	 : L2PcInstance Player requesting the item transfer
	 * param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance transferItem(String process, int objectId, long count, Inventory target, L2Player actor, L2Object reference)
	{
		if(target == null)
			return null;

		L2ItemInstance sourceitem = getItemByObjectId(objectId);

		if(sourceitem == null)
			return null;

		L2ItemInstance targetitem = sourceitem.isStackable() ? target.getItemByItemId(sourceitem.getItemId()) : null;

		synchronized(sourceitem)
		{
			// check if this item still present in this container
			if(getItemByObjectId(objectId) == null || !getItemByObjectId(objectId).equals(sourceitem))
				return null;

			// Check if requested quantity is available
			if(count > sourceitem.getCount())
				count = sourceitem.getCount();

			// If possible, move entire item object
			if(sourceitem.getCount() == count && targetitem == null)
			{
				removeItem(sourceitem);
				sendRemoveItem(sourceitem);
				target.addItem(process, sourceitem, actor, reference);
				targetitem = sourceitem;

				for(InventoryListener listener : inventoryListeners)
					listener.itemRemoved(sourceitem);
			}
			else
			{
				if(sourceitem.getCount() > count) // If possible, only update counts
				{
					sourceitem.changeCount(process, -count, actor, reference);
					sendModifyItem(sourceitem);
				}
				else // Otherwise destroy old item
				{
					removeItem(sourceitem);
					sendRemoveItem(sourceitem);
					ItemTable.getInstance().destroyItem(process, sourceitem, actor, reference);

					for(InventoryListener listener : inventoryListeners)
						listener.itemRemoved(sourceitem);
				}

				if(targetitem != null) // If possible, only update counts
				{
					if(reference.isPlayer())
						targetitem.changeCount(process, count, (L2Player) reference, actor);
					else
						targetitem.changeCount(process, count, actor, reference);

					target.sendModifyItem(targetitem);
					target.refreshWeight();
				}
				else // Otherwise add new item
				{
					if(reference.isPlayer())
						targetitem = target.addItem(process, sourceitem.getItemId(), count, (L2Player) reference, actor);
					else
						targetitem = target.addItem(process, sourceitem.getItemId(), count, actor, reference);
				}
			}

			// Updates database
			sourceitem.updateDatabase(true);
			if(targetitem != sourceitem && targetitem != null)
				targetitem.updateDatabase(true);

			if(sourceitem.isAugmented())
				sourceitem.getAugmentation().removeBonus(actor);

			if(this instanceof Warehouse)
				target.refreshWeight();
			else
				refreshWeight();
		}
		return targetitem;
	}

	private static class ItemOrderComparator implements Comparator<L2ItemInstance>
	{
		@Override
		public int compare(L2ItemInstance o1, L2ItemInstance o2)
		{
			if(o1 == null || o2 == null)
				return 0;
			return o1.getEquipSlot() - o2.getEquipSlot();
		}
	}

	public static ItemOrderComparator OrderComparator = new ItemOrderComparator();
	

	public static int getPaperdollIndex(int slot)
	{
		switch (slot)
		{
			case L2Item.SLOT_UNDERWEAR:
				return PAPERDOLL_UNDER;
			case L2Item.SLOT_R_EAR:
				return PAPERDOLL_REAR;
			case L2Item.SLOT_L_EAR:
				return PAPERDOLL_LEAR;
			case L2Item.SLOT_NECK:
				return PAPERDOLL_NECK;
			case L2Item.SLOT_R_FINGER:
				return PAPERDOLL_RFINGER;
			case L2Item.SLOT_L_FINGER:
				return PAPERDOLL_LFINGER;
			case L2Item.SLOT_HEAD:
				return PAPERDOLL_HEAD;
			case L2Item.SLOT_R_HAND:
				return PAPERDOLL_RHAND;
			case L2Item.SLOT_L_HAND:
				return PAPERDOLL_LHAND;
			case L2Item.SLOT_LR_HAND:
				return PAPERDOLL_LRHAND;
			case L2Item.SLOT_GLOVES:
				return PAPERDOLL_GLOVES;
			case L2Item.SLOT_CHEST:
			case L2Item.SLOT_FULL_ARMOR:
			case L2Item.SLOT_FORMAL_WEAR:
				return PAPERDOLL_CHEST;
			case L2Item.SLOT_LEGS:
				return PAPERDOLL_LEGS;
			case L2Item.SLOT_FEET:
				return PAPERDOLL_FEET;
			case L2Item.SLOT_BACK:
				return PAPERDOLL_BACK;
			case L2Item.SLOT_HAIR:
			case L2Item.SLOT_HAIRALL:
				return PAPERDOLL_HAIR;
			case L2Item.SLOT_DHAIR:
				return PAPERDOLL_DHAIR;
			case L2Item.SLOT_R_BRACELET:
				return PAPERDOLL_RBRACELET;
			case L2Item.SLOT_L_BRACELET:
				return PAPERDOLL_LBRACELET;
			case L2Item.SLOT_DECO:
				return PAPERDOLL_DECO1; // return first we deal with it later
			case L2Item.SLOT_BELT:
				return PAPERDOLL_BELT;
		}
		return -1;
	}

	public int getAllSize()
	{
		return getSize() - getQuestSize();
	}

	public int getQuestSize()
	{
		int size = 0;
		//for (L2ItemInstance item : getItems())
		//	if (item.getTemplate().isQuest())
		//		size++;
		return size;
	}
}