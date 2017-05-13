package ru.l2gw.gameserver.tables;

import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.handler.IItemHandler;
import ru.l2gw.gameserver.handler.ItemHandler;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.SkillsEngine;
import ru.l2gw.gameserver.templates.*;
import ru.l2gw.gameserver.templates.L2Armor.ArmorType;
import ru.l2gw.gameserver.templates.L2EtcItem.EtcItemType;
import ru.l2gw.gameserver.templates.L2Item.Grade;
import ru.l2gw.gameserver.templates.L2Weapon.WeaponType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings({"nls", "unqualified-field-access", "boxing"})
public class ItemTable
{
	private static Log _log = LogFactory.getLog(ItemTable.class.getName());
	private static Log _logItems = LogFactory.getLog("item");

	private static final HashMap<String, Integer> _crystalTypes = new HashMap<String, Integer>();
	private static final HashMap<String, WeaponType> _weaponTypes = new HashMap<String, WeaponType>();
	private static final HashMap<String, ArmorType> _armorTypes = new HashMap<String, ArmorType>();
	private static final HashMap<String, Integer> _slots = new HashMap<String, Integer>();
	private static final HashMap<String, Byte> _type2 = new HashMap<String, Byte>();

	static
	{
		_crystalTypes.put("s84", Grade.S84.ordinal());
		_crystalTypes.put("s80", Grade.S80.ordinal());
		_crystalTypes.put("s", Grade.S.ordinal());
		_crystalTypes.put("a", Grade.A.ordinal());
		_crystalTypes.put("b", Grade.B.ordinal());
		_crystalTypes.put("c", Grade.C.ordinal());
		_crystalTypes.put("d", Grade.D.ordinal());
		_crystalTypes.put("none", Grade.NONE.ordinal());

		_weaponTypes.put("blunt", WeaponType.BLUNT);
		_weaponTypes.put("bigblunt", WeaponType.BIGBLUNT);
		_weaponTypes.put("bow", WeaponType.BOW);
		_weaponTypes.put("crossbow", WeaponType.CROSSBOW);
		_weaponTypes.put("dagger", WeaponType.DAGGER);
		_weaponTypes.put("dual", WeaponType.DUAL);
		_weaponTypes.put("dualfist", WeaponType.DUALFIST);
		_weaponTypes.put("etc", WeaponType.ETC);
		_weaponTypes.put("fist", WeaponType.FIST);
		_weaponTypes.put("none", WeaponType.NONE); // these are shields !
		_weaponTypes.put("pole", WeaponType.POLE);
		_weaponTypes.put("sword", WeaponType.SWORD);
		_weaponTypes.put("bigsword", WeaponType.BIGSWORD); //Two-Handed Swords
		_weaponTypes.put("rapier", WeaponType.RAPIER); //Kamael Rapier
		_weaponTypes.put("ancientsword", WeaponType.ANCIENTSWORD); //Kamael Two-Handed Swords
		_weaponTypes.put("rod", WeaponType.ROD); //Fishing Rods
		_weaponTypes.put("dualdagger", WeaponType.DUALDAGGER); //Dual Daggers

		_armorTypes.put("none", ArmorType.NONE);
		_armorTypes.put("light", ArmorType.LIGHT);
		_armorTypes.put("heavy", ArmorType.HEAVY);
		_armorTypes.put("magic", ArmorType.MAGIC);
		_armorTypes.put("pet", ArmorType.PET);
		_armorTypes.put("sigil", ArmorType.SIGIL);

		_slots.put("chest", L2Item.SLOT_CHEST);
		_slots.put("lbracelet", L2Item.SLOT_L_BRACELET);
		_slots.put("rbracelet", L2Item.SLOT_R_BRACELET);
		_slots.put("belt", L2Item.SLOT_BELT);
		_slots.put("talisman", L2Item.SLOT_DECO);
		_slots.put("fullarmor", L2Item.SLOT_FULL_ARMOR);
		_slots.put("head", L2Item.SLOT_HEAD);
		_slots.put("hair", L2Item.SLOT_HAIR);
		_slots.put("face", L2Item.SLOT_DHAIR);
		_slots.put("dhair", L2Item.SLOT_HAIRALL);
		_slots.put("underwear", L2Item.SLOT_UNDERWEAR);
		_slots.put("back", L2Item.SLOT_BACK);
		_slots.put("neck", L2Item.SLOT_NECK);
		_slots.put("legs", L2Item.SLOT_LEGS);
		_slots.put("feet", L2Item.SLOT_FEET);
		_slots.put("gloves", L2Item.SLOT_GLOVES);
		_slots.put("chest,legs", L2Item.SLOT_CHEST | L2Item.SLOT_LEGS);
		_slots.put("rhand", L2Item.SLOT_R_HAND);
		_slots.put("lhand", L2Item.SLOT_L_HAND);
		_slots.put("lrhand", L2Item.SLOT_LR_HAND);
		_slots.put("rear,lear", L2Item.SLOT_R_EAR | L2Item.SLOT_L_EAR);
		_slots.put("rfinger,lfinger", L2Item.SLOT_R_FINGER | L2Item.SLOT_L_FINGER);
		_slots.put("none", L2Item.SLOT_NONE);
		_slots.put("wolf", L2Item.SLOT_WOLF); // for wolf
		_slots.put("gwolf", L2Item.SLOT_GWOLF); // for great wolf
		_slots.put("hatchling", L2Item.SLOT_HATCHLING); // for hatchling
		_slots.put("strider", L2Item.SLOT_STRIDER); // for strider
		_slots.put("formalwear", L2Item.SLOT_FORMAL_WEAR);
		_slots.put("baby", L2Item.SLOT_BABYPET); // for baby pet
		_slots.put("pendant", L2Item.SLOT_PENDANT); // magic armor for pet

		_type2.put("weapon", L2Item.TYPE2_WEAPON);
		_type2.put("armor", L2Item.TYPE2_SHIELD_ARMOR);
		_type2.put("accessary", L2Item.TYPE2_ACCESSORY);
		_type2.put("questitem", L2Item.TYPE2_QUEST);
		_type2.put("asset", L2Item.TYPE2_MONEY);
		_type2.put("etcitem", L2Item.TYPE2_OTHER);
	}

	private L2Item[] _allTemplates;

	private final TIntObjectHashMap<L2EtcItem> _etcItems = new TIntObjectHashMap<>();
	private final TIntObjectHashMap<L2Armor> _armors = new TIntObjectHashMap<>();
	private final TIntObjectHashMap<L2Weapon> _weapons = new TIntObjectHashMap<>();

	private boolean _initialized = true;

	private static ItemTable _instance;

	/**
	 * Table of SQL request in order to obtain items from tables [etcitem], [armor], [weapon]
	 */
	private static final String[] SQL_ITEM_SELECTS = {
			"SELECT * FROM etcitem",
			"SELECT * FROM armor",
			"SELECT * FROM weapon"};

	/**
	 * Returns instance of ItemTable
	 *
	 * @return ItemTable
	 */
	public static ItemTable getInstance()
	{
		if(_instance == null)
			_instance = new ItemTable();
		return _instance;
	}

	/**
	 * Не работает, не использовать...
	 public static void reload()
	 {
	 _instance = null;
	 getInstance();
	 }
	 */

	/**
	 * Returns a new object Item
	 *
	 * @return
	 */
	public Item newItem()
	{
		return new Item();
	}

	/**
	 * Constructor.
	 */
	private ItemTable()
	{
		List<Item> itemData = new ArrayList<Item>();
		List<Item> armorData = new ArrayList<Item>();
		List<Item> weaponData = new ArrayList<Item>();

		Connection con = null;
		PreparedStatement st = null;
		PreparedStatement statement = null;
		ResultSet rs = null, rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			for(String selectQuery : SQL_ITEM_SELECTS)
			{
				statement = con.prepareStatement(selectQuery);
				rset = statement.executeQuery();

				// Add item in correct HashMap
				while(rset.next())
					if(selectQuery.endsWith("etcitem"))
					{
						Item newItem = readItem(rset);
						itemData.add(newItem);
					}
					else if(selectQuery.endsWith("armor"))
					{
						Item newItem = readArmor(rset);
						armorData.add(newItem);
					}
					else if(selectQuery.endsWith("weapon"))
					{
						Item newItem = readWeapon(rset);
						weaponData.add(newItem);
					}
				DbUtils.closeQuietly(statement, rset);
			}
		}
		catch(Exception e)
		{
			_log.warn("data error on item: " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(statement, rset);
			DbUtils.closeQuietly(con, st, rs);
		}

		for(Item item : itemData)
			_etcItems.put(item.id, new L2EtcItem((EtcItemType) item.type, item.set));
		_log.info("ItemTable: Loaded " + _etcItems.size() + " Items.");

		for(Item item : armorData)
			_armors.put(item.id, new L2Armor((ArmorType) item.type, item.set));
		_log.info("ItemTable: Loaded " + _armors.size() + " Armors.");

		for(Item item : weaponData)
			_weapons.put(item.id, new L2Weapon((WeaponType) item.type, item.set));
		_log.info("ItemTable: Loaded " + _weapons.size() + " Weapons.");

		itemData.clear();
		armorData.clear();
		weaponData.clear();

		buildFastLookupTable();
		new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					Thread.sleep(1000);
				}
				catch(InterruptedException e)
				{
				}
				loadItemsStats();
			}
		}).start();
	}

	private void loadItemsStats()
	{
		//Загружаем статты итемов из таблиц xml. (Дополнительно для указания: кондишонов, листенеров, дополнительных параметров и т.д.)
		SkillsEngine.getInstance().loadArmors();
		SkillsEngine.getInstance().loadWeapons();
		SkillsEngine.getInstance().loadItems();
	}

	/**
	 * Returns object Item from the record of the database
	 *
	 * @param rset : ResultSet designating a record of the [weapon] table of database
	 * @return Item : object created from the database record
	 * @throws SQLException
	 */
	private Item readWeapon(ResultSet rset) throws SQLException
	{
		Item item = new Item();
		item.set = new StatsSet();
		item.id = rset.getInt("item_id");
		item.type = _weaponTypes.get(rset.getString("weaponType"));
		if(item.type == null)
			System.out.println("Error in weapons table: unknown weapon type " + rset.getString("weaponType") + " for item " + item.id);
		item.name = rset.getString("name");
		item.set.set("class", "EQUIPMENT");

		item.set.set("item_id", item.id);
		item.set.set("name", item.name);
		item.set.set("type2", _type2.get(rset.getString("type2")));

		// lets see if this is a shield
		if(item.type == WeaponType.NONE)
			item.set.set("type1", L2Item.TYPE1_SHIELD_ARMOR);
		else
			item.set.set("type1", L2Item.TYPE1_WEAPON_RING_EARRING_NECKLACE);

		item.set.set("bodypart", _slots.get(rset.getString("bodypart")));
		item.set.set("crystal_type", _crystalTypes.get(rset.getString("crystal_type")));
		item.set.set("weight", rset.getInt("weight"));
		item.set.set("soulshots", rset.getInt("soulshots"));
		item.set.set("spiritshots", rset.getInt("spiritshots"));
		item.set.set("p_dam", rset.getInt("p_dam"));
		item.set.set("rnd_dam", rset.getInt("rnd_dam"));
		item.set.set("critical", rset.getInt("critical"));
		item.set.set("hit_modify", rset.getFloat("hit_modify"));
		item.set.set("avoid_modify", rset.getFloat("avoid_modify"));
		item.set.set("shield_def", rset.getInt("shield_def"));
		item.set.set("shield_def_rate", rset.getInt("shield_def_rate"));
		item.set.set("attack_speed", rset.getInt("attack_speed"));
		item.set.set("mp_consume", rset.getInt("mp_consume"));
		item.set.set("m_dam", rset.getInt("m_dam"));
		item.set.set("durability", rset.getInt("durability"));
		item.set.set("price", rset.getInt("price"));
		item.set.set("crystal_count", rset.getInt("crystal_count"));
		item.set.set("tradeable", rset.getBoolean("tradeable"));
		item.set.set("dropable", rset.getBoolean("dropable"));
		item.set.set("destroyable", rset.getBoolean("destroyable"));
		item.set.set("keep_type", rset.getInt("keep_type"));
		item.set.set("skills", rset.getString("skills"));
		item.set.set("enchant4_skills", rset.getString("enchant4_skills"));
		item.set.set("icon", rset.getString("icon"));
		item.set.set("period", rset.getInt("period"));
		item.set.set("for_npc", rset.getBoolean("for_npc"));
		item.set.set("enchanted", rset.getInt("enchanted"));
		item.set.set("enchantable", rset.getBoolean("enchantable"));
		item.set.set("elementable", rset.getBoolean("elementable"));
		item.set.set("is_magic_weapon", rset.getBoolean("is_magic_weapon"));
		item.set.set("attack_range", rset.getInt("attack_range"));
		item.set.set("reuse_delay", rset.getInt("reuse_delay"));
		item.set.set("olympiad_use", rset.getBoolean("olympiad_use"));
		item.set.set("is_have_sa", rset.getBoolean("is_have_sa"));
		item.set.set("is_masterwork", rset.getBoolean("is_masterwork"));
		item.set.set("is_pvp", rset.getBoolean("is_pvp"));
		item.set.set("is_common", rset.getBoolean("is_common"));
		item.set.set("additional_name", rset.getString("additional_name"));

		return item;
	}

	/**
	 * Returns object Item from the record of the database
	 *
	 * @param rset : ResultSet designating a record of the [armor] table of database
	 * @return Item : object created from the database record
	 * @throws SQLException
	 */
	private Item readArmor(ResultSet rset) throws SQLException
	{
		Item item = new Item();
		item.set = new StatsSet();
 
		item.type = _armorTypes.get(rset.getString("armor_type"));
		item.id = rset.getInt("item_id");
		item.name = rset.getString("name");

		item.set.set("class", "EQUIPMENT");
		item.set.set("item_id",	item.id);
		item.set.set("name", item.name);
		int bodypart = _slots.get(rset.getString("bodypart"));
		item.set.set("bodypart", bodypart);
		item.set.set("crystal_count", rset.getInt("crystal_count"));
		item.set.set("for_npc", rset.getBoolean("for_npc"));
		item.set.set("type2", _type2.get(rset.getString("type2")));

		if(bodypart == L2Item.SLOT_NECK || bodypart == L2Item.SLOT_HAIR || bodypart == L2Item.SLOT_DHAIR || (bodypart & L2Item.SLOT_L_EAR) != 0 || (bodypart & L2Item.SLOT_L_FINGER) != 0)
			item.set.set("type1", L2Item.TYPE1_WEAPON_RING_EARRING_NECKLACE);
		else
			item.set.set("type1", L2Item.TYPE1_SHIELD_ARMOR);

		item.set.set("weight", rset.getInt("weight"));
		item.set.set("crystal_type", _crystalTypes.get(rset.getString("crystal_type")));
		item.set.set("avoid_modify", rset.getInt("avoid_modify"));
		item.set.set("durability", rset.getInt("durability"));
		item.set.set("p_def", rset.getInt("p_def"));
		item.set.set("m_def", rset.getInt("m_def"));
		item.set.set("mp_bonus", rset.getInt("mp_bonus"));
		item.set.set("price", rset.getInt("price"));
		item.set.set("tradeable", rset.getBoolean("tradeable"));
		item.set.set("dropable", rset.getBoolean("dropable"));
		item.set.set("destroyable", rset.getBoolean("destroyable"));
		item.set.set("keep_type", rset.getInt("keep_type"));
		item.set.set("skills", rset.getString("skills"));
		item.set.set("enchant4_skills", rset.getString("enchant4_skills"));
		item.set.set("icon", rset.getString("icon"));
		item.set.set("period", rset.getInt("period"));
		item.set.set("enchanted", rset.getInt("enchanted"));
		item.set.set("enchantable", rset.getBoolean("enchantable"));
		item.set.set("elementable", rset.getBoolean("elementable"));
		item.set.set("olympiad_use", rset.getBoolean("olympiad_use"));
		item.set.set("is_masterwork", rset.getBoolean("is_masterwork"));
		item.set.set("is_pvp", rset.getBoolean("is_pvp"));
		item.set.set("is_sealed", rset.getBoolean("is_sealed"));
		item.set.set("is_common", rset.getBoolean("is_common"));
		item.set.set("additional_name", rset.getString("additional_name"));

		return item;
	}

	/**
	 * Returns object Item from the record of the database
	 *
	 * @param rset : ResultSet designating a record of the [etcitem] table of database
	 * @return Item : object created from the database record
	 * @throws SQLException
	 */
	private Item readItem(ResultSet rset) throws SQLException
	{
		Item item = new Item();
		item.set = new StatsSet();
		item.id = rset.getInt("item_id");

		item.set.set("item_id", item.id);
		item.set.set("type1", L2Item.TYPE1_ITEM_QUESTITEM_ADENA);
		item.set.set("type2", _type2.get(rset.getString("type2")));
		item.set.set("bodypart", 0);
		item.set.set("crystal_count", rset.getInt("crystal_count"));
		item.set.set("icon", rset.getString("icon"));
		item.set.set("class", rset.getString("class"));
		item.set.set("for_npc", rset.getBoolean("for_npc"));
		item.set.set("action", rset.getString("action"));
		String itemType = rset.getString("item_type");
		if(itemType.equals("none"))
			item.type = EtcItemType.OTHER; // only for default
		else if(itemType.equals("mticket"))
			item.type = EtcItemType.SCROLL; // dummy
		else if(itemType.equals("material"))
			item.type = EtcItemType.MATERIAL;
		else if(itemType.equals("pet_collar"))
			item.type = EtcItemType.PET_COLLAR;
		else if(itemType.equals("potion"))
			item.type = EtcItemType.POTION;
		else if(itemType.equals("recipe"))
			item.type = EtcItemType.RECIPE;
		else if(itemType.equals("scroll"))
			item.type = EtcItemType.SCROLL;
		else if(itemType.equals("seed"))
			item.type = EtcItemType.SEED;
		else if(itemType.equals("spellbook"))
			item.type = EtcItemType.SPELLBOOK; // Spellbook, Amulet, Blueprint
		else if(itemType.equals("shot"))
			item.type = EtcItemType.SHOT;
		else if(itemType.equals("herb"))
			item.type = EtcItemType.HERB;
		else if(itemType.equals("foundation"))
			item.type = EtcItemType.FOUNDATION;
		else if(itemType.equals("arrow"))
		{
			item.type = EtcItemType.ARROW;
			item.set.set("bodypart", L2Item.SLOT_L_HAND);
		}
		else if(itemType.equals("bolt"))
		{
			item.type = EtcItemType.BOLT;
			item.set.set("bodypart", L2Item.SLOT_L_HAND);
		}
		else if(itemType.equals("bait"))
		{
			item.type = EtcItemType.BAIT;
			item.set.set("bodypart", L2Item.SLOT_L_HAND);
		}
		else if(itemType.equals("rune_select"))
			item.type = EtcItemType.RUNE_SELECT;
		else if(itemType.equals("rune"))
			item.type = EtcItemType.RUNE;
		else
			item.type = EtcItemType.OTHER;

		String consume = rset.getString("consume_type");
		if(consume.equals("asset"))
		{
			item.type = EtcItemType.MONEY;
			item.set.set("stackable", true);
		}
		else if(consume.equals("stackable"))
			item.set.set("stackable", true);
		else
			item.set.set("stackable", false);

		int crystal = _crystalTypes.get(rset.getString("crystal_type"));
		item.set.set("crystal_type", crystal);

		int weight = rset.getInt("weight");
		item.set.set("weight", weight);
		item.name = rset.getString("name");
		item.set.set("name", item.name);
		item.set.set("price", rset.getInt("price"));
		item.set.set("skills", rset.getString("skills"));
		item.set.set("tradeable", rset.getBoolean("tradeable"));
		item.set.set("dropable", rset.getBoolean("dropable"));
		item.set.set("destroyable", rset.getBoolean("destroyable"));
		item.set.set("keep_type", rset.getInt("keep_type"));
		item.set.set("period", rset.getInt("period"));
		item.set.set("olympiad_use", rset.getBoolean("olympiad_use"));
		item.set.set("delay_share_group", rset.getInt("delay_share_group"));
		item.set.set("reuse_delay", rset.getInt("reuse_delay"));
		item.set.set("additional_name", rset.getString("additional_name"));

		return item;
	}

	/**
	 * Builds a variable in which all items are putting in in function of their ID.
	 */
	private void buildFastLookupTable()
	{
		int highestId = 0;

		for(L2Armor armor : _armors.valueCollection())
		{
			if(armor.getItemId() > highestId)
				highestId = armor.getItemId();
		}
		for(L2Weapon weapon : _weapons.valueCollection())
		{
			if(weapon.getItemId() > highestId)
				highestId = weapon.getItemId();
		}
		for(L2EtcItem item : _etcItems.valueCollection())
		{
			if(item.getItemId() > highestId)
				highestId = item.getItemId();
		}

		// Create a FastLookUp Table called _allTemplates of size : value of the highest item ID
		if(Config.DEBUG)
			_log.warn("highest item id used:" + highestId);
		_allTemplates = new L2Item[highestId + 1];

		for(L2Armor item : _armors.valueCollection())
		{
			assert _allTemplates[item.getItemId()] == null;
			_allTemplates[item.getItemId()] = item;
		}

		for(L2Weapon item : _weapons.valueCollection())
		{
			assert _allTemplates[item.getItemId()] == null;
			_allTemplates[item.getItemId()] = item;
		}

		for(L2EtcItem item : _etcItems.valueCollection())
		{
			assert _allTemplates[item.getItemId()] == null;
			_allTemplates[item.getItemId()] = item;
		}
	}

	/**
	 * Create the L2ItemInstance corresponding to the Item Identifier and add it to _allObjects of L2world.<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Create and Init the L2ItemInstance corresponding to the Item Identifier </li>
	 * <li>Add the L2ItemInstance object to _allObjects of L2world </li><BR><BR>
	 *
	 * @param itemId The Item Identifier of the L2ItemInstance that must be created
	 */
	public L2ItemInstance createItem(String process, int itemId, long count, Object actor, L2Object reference)
	{
		// Create and Init the L2ItemInstance corresponding to the Item Identifier
		L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), itemId);
		if(Config.DEBUG)
			_log.warn("ItemTable: Item created	oid:" + item.getObjectId() + " itemid:" + itemId);

		// Add the L2ItemInstance object to _allObjects of L2world
		//L2World.addObject(item);

		// Set Item parameters
		if(item.isStackable() && count > 1)
			item.setCount(count);

		List<Object> param = new ArrayList<Object>();
		param.add("CREATE:" + process);
		param.add(item);
		if(actor != null) 
			param.add(actor);
		if(reference != null)
			param.add(reference);
		_logItems.info(param);

		return item;
	}

	public L2ItemInstance createItem(String process, int itemId, long count, Object actor)
	{
		return createItem(process, itemId, count, actor, null);
	}

	/**
	 * Returns a dummy (fr = factice) item.<BR><BR>
	 * <U><I>Concept :</I></U><BR>
	 * Dummy item is created by setting the ID of the object in the world at null value
	 *
	 * @param itemId : int designating the item
	 * @return L2ItemInstance designating the dummy item created
	 */
	public L2ItemInstance createDummyItem(int itemId)
	{
		L2Item item = getTemplate(itemId);
		if(item == null)
			return null;
		L2ItemInstance temp = new L2ItemInstance(0, item);
		try
		{
			temp = new L2ItemInstance(0, itemId);
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			e.printStackTrace(); // this can happen if the item templates were not initialized
		}

		if(temp.getItem() == null)
			_log.warn("ItemTable: Item Template missing for Id: " + itemId);

		return temp;
	}

	public static boolean useHandler(L2Playable playable, final L2ItemInstance item)
	{
		if(playable.getPlayer().isInOlympiadMode() && !item.getItem().isInOlympiadUsable())
		{
			playable.sendPacket(Msg.THIS_ITEM_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return true;
		}

		// Вызов всех определенных скриптовых итемхэндлеров
		if(item.getItem().getEquipCondition() != null)
		{
			Env env = new Env(playable, playable, null);
			env.item = item;
			if(!item.getItem().getEquipCondition().test(env))
			{
				String condMsg = item.getItem().getEquipCondition().getMessage();
				int condMsgId = item.getItem().getEquipCondition().getMessageId();
				if(condMsgId != 0)
					playable.sendPacket(new SystemMessage(condMsgId).addItemName(item.getItemId()));
				else if(condMsg != null)
					playable.sendMessage(condMsg);

				return true;
			}
		}

		final ArrayList<Scripts.ScriptClassAndMethod> handlers = Scripts.itemHandlers.get(item.getItemId());
		if(handlers != null && handlers.size() > 0)
		{
			for(final Scripts.ScriptClassAndMethod handler : handlers)
				playable.callScripts(handler.scriptClass, handler.method, new Object[]{});
			return true;
		}

		IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getItemId());
		if(handler != null)
		{
			handler.useItem(playable, item);
			return true;
		}

		L2Skill[] skills = item.getItem().getAttachedSkills();

		if(skills != null && skills.length > 0)
		{

			boolean useSkill = true;

			if("action_capsule".equals(item.getItem().getAction()))
			{
				if(playable.isPlayer() && playable.getPlayer().getInventory().slotsLeft() < 10)
				{
					playable.sendPacket(Msg.YOUR_INVENTORY_IS_FULL);
					return true;
				}
				useSkill = playable.getInventory().destroyItem("Capsule", item.getObjectId(), 1, playable.getPlayer(), null) != null;
			}

			if(useSkill)
			{
				boolean consume = true;
				for(L2Skill skill : skills)
				{
					playable.fireMethodInvoked(MethodCollection.onSkillUse, new Object[]{skill, skill.getAimingTarget(playable), item, false});
					playable.getAI().Cast(skill, skill.getAimingTarget(playable), item, false, false);
					if(consume)
						consume = skill.getItemConsumeId()[0] == 0;
				}

				if(item.getItemType() == EtcItemType.POTION && consume)
					playable.getInventory().destroyItem("Consume", item.getObjectId(), 1, playable.getPlayer(), null);
			}
			return true;
		}

		return false;
	}

	/**
	 * Returns the item corresponding to the item ID
	 *
	 * @param id : int designating the item
	 */
	public L2Item getTemplate(int id)
	{
		if(id >= _allTemplates.length || id < 0)
		{
			_log.warn("ItemTable[604]: Not defined item_id=" + id + "; out of range");
			Thread.dumpStack();
			return null;
		}
		return _allTemplates[id];
	}

	public L2Item[] getAllTemplates()
	{
		return _allTemplates;
	}

	public Collection<L2Weapon> getAllWeapons()
	{
		return _weapons.valueCollection();
	}

	public Collection<L2Armor> getAllArmors()
	{
		return _armors.valueCollection();
	}

	/**
	 * Returns if ItemTable initialized
	 */
	public boolean isInitialized()
	{
		return _initialized;
	}

	/**
	 * Destroys the L2ItemInstance.<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Sets L2ItemInstance parameters to be unusable </li>
	 * <li>Removes the L2ItemInstance object to _allObjects of L2world </li>
	 * <li>Logs Item delettion according to log settings</li><BR><BR>
	 *
	 * @param process   : String Identifier of process triggering this action
	 * @param item	: int Item Identifier of the item to be created
	 * @param actor	 : L2PcInstance Player requesting the item destroy
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void destroyItem(String process, L2ItemInstance item, L2Player actor, L2Object reference)
	{
		synchronized(item)
		{
			long cnt = item.getCount();
			item.setCount(0);
			item.setOwnerId(0);
			item.setLocation(L2ItemInstance.ItemLocation.VOID);
			item.setLastChange(L2ItemInstance.REMOVED);

			L2World.removeObject(item);
			IdFactory.getInstance().releaseId(item.getObjectId());

			List<Object> param = new ArrayList<Object>();
			param.add("DELETE:" + process);
			param.add(item);
			if(actor != null) param.add(actor);
			if(reference != null) param.add(reference);
			param.add(cnt);
			_logItems.info(param);
			if(PetDataTable.isPetControlItem(item))
				PetDataTable.deletePet(item, actor);
		}
	}

	public class Item
	{
		public int id;
		public Enum<?> type;
		public String name;
		public StatsSet set;
		public L2Item item;
	}
}