package ru.l2gw.gameserver.templates;

import ru.l2gw.extensions.listeners.items.ItemEquipListener;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.base.ItemEnchantTemplate;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2ItemInstance.ItemClass;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.conditions.Condition;
import ru.l2gw.gameserver.skills.funcs.FuncTemplate;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.templates.L2EtcItem.EtcItemType;

/**
 * This class contains all informations concerning the item (weapon, armor, etc).<BR>
 * Mother class of :
 * <LI>L2Armor</LI>
 * <LI>L2EtcItem</LI>
 * <LI>L2Weapon</LI>
 */
public abstract class L2Item
{
	/**
	 * Pc Cafe Bang Points item id. Используется на корейских серверах, но английский клиент в состоянии
	 * поддерживать даный функционал.
	 */
	public static final short ITEM_ID_PC_BANG_POINTS = -100;

	/**
	 * Item ID для клановой репутации
	 */
	public static final short ITEM_ID_CLAN_REPUTATION_SCORE = -200;
	public static final short ITEM_ID_FAME_POINTS = -300;
	public static final short ITEM_ID_PHOENIX_FEATHER = 13128;
	public static final short ITEM_ID_POMANDER_CARDINAL = 15307;
	public static final short ITEM_ID_POMANDER_EVAS_SAINT = 15308;
	public static final short ITEM_ID_POMANDER_SHILIEN_SAINT = 15309;

	public static final int TYPE1_WEAPON_RING_EARRING_NECKLACE = 0;
	public static final int TYPE1_SHIELD_ARMOR = 1;
	public static final int TYPE1_ITEM_QUESTITEM_ADENA = 4;

	public static final byte TYPE2_WEAPON = 0;
	public static final byte TYPE2_SHIELD_ARMOR = 1;
	public static final byte TYPE2_ACCESSORY = 2;
	public static final byte TYPE2_QUEST = 3;
	public static final byte TYPE2_MONEY = 4;
	public static final byte TYPE2_OTHER = 5;

	public static final int SLOT_NONE = 0x00000;
	public static final int SLOT_UNDERWEAR = 0x00001;

	public static final int SLOT_R_EAR = 0x00002;
	public static final int SLOT_L_EAR = 0x00004;

	public static final int SLOT_NECK = 0x00008;

	public static final int SLOT_R_FINGER = 0x00010;
	public static final int SLOT_L_FINGER = 0x00020;

	public static final int BELT = 0x00030;

	public static final int SLOT_HEAD = 0x00040;
	public static final int SLOT_R_HAND = 0x00080;
	public static final int SLOT_L_HAND = 0x00100;
	public static final int SLOT_GLOVES = 0x00200;
	public static final int SLOT_CHEST = 0x00400;
	public static final int SLOT_LEGS = 0x00800;
	public static final int SLOT_FEET = 0x01000;
	public static final int SLOT_BACK = 0x02000;
	public static final int SLOT_LR_HAND = 0x04000;
	public static final int SLOT_FULL_ARMOR = 0x08000;
	public static final int SLOT_HAIR = 0x10000;
	public static final int SLOT_FORMAL_WEAR = 0x20000;
	public static final int SLOT_DHAIR = 0x40000;
	public static final int SLOT_HAIRALL = 0x80000;
	public static final int SLOT_R_BRACELET = 0x100000;
	public static final int SLOT_L_BRACELET = 0x200000;
	public static final int SLOT_DECO = 0x400000;
	public static final int SLOT_BELT = 0x10000000;
	public static final int SLOT_WOLF = -100;
	public static final int SLOT_HATCHLING = -101;
	public static final int SLOT_STRIDER = -102;
	public static final int SLOT_BABYPET = -103;
	public static final int SLOT_GWOLF = -104;
	public static final int SLOT_PENDANT = -105;

	public static final int CRYSTAL_NONE = 0;
	public static final int CRYSTAL_D = 1458;
	public static final int CRYSTAL_C = 1459;
	public static final int CRYSTAL_B = 1460;
	public static final int CRYSTAL_A = 1461;
	public static final int CRYSTAL_S = 1462;
	
	public static final int KEEP_TYPE_PWH = 1;
	public static final int KEEP_TYPE_СWH = 2;
	public static final int KEEP_TYPE_MAIL = 4;
	public static final int KEEP_TYPE_FREIGHT = 8;

	public static enum Grade
	{
		NONE(CRYSTAL_NONE, 0),
		D(CRYSTAL_D, 1),
		C(CRYSTAL_C, 2),
		B(CRYSTAL_B, 3),
		A(CRYSTAL_A, 4),
		S(CRYSTAL_S, 5),
		S80(CRYSTAL_S, 5),
		S84(CRYSTAL_S, 5);

		/** ID соответствующего грейду кристалла */
		public final int cry;
		/** ID грейда, без учета уровня S */
		public final int externalOrdinal;

		private Grade(int crystal, int ext)
		{
			cry = crystal;
			externalOrdinal = ext;
		}
	}

	public static final byte ATTRIBUTE_NONE = -2;
	public static final byte ATTRIBUTE_FIRE = 0;
	public static final byte ATTRIBUTE_WATER = 1;
	public static final byte ATTRIBUTE_WIND = 2;
	public static final byte ATTRIBUTE_EARTH = 3;
	public static final byte ATTRIBUTE_HOLY = 4;
	public static final byte ATTRIBUTE_DARK = 5;

	public static final long MAX_COUNT = 999999999999L;

	public static int[][] enchantHpBonus = {
	//  +4   +5   +6   +7   +8   +9  +10 +11 +12
			{ 4, 8, 6, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16 }, // None grade
			{ 9, 26, 52, 86, 130, 181, 242, 311, 389 }, // D grade
			{ 12, 36, 71, 118, 178, 249, 332, 426, 533 }, // C grade
			{ 14, 42, 84, 139, 209, 293, 390, 502, 627 }, // B grade
			{ 16, 47, 94, 157, 235, 329, 439, 564, 705 }, // A grade
			{ 17, 52, 104, 173, 259, 363, 484, 623, 778 } // S/S80 grade
	};

	private static final int[] crystalEnchantBonusArmor =
	{
		0, 11, 6, 11, 19, 25, 25, 25
	};
	private static final int[] crystalEnchantBonusWeapon =
	{
		0, 90, 45, 67, 144, 250, 250, 250
	};

	private final short _itemId;
	private final ItemClass _class;
	protected final String _name;
	private final String _additionalName;
	protected final String _icon;
	private final int _type1; // needed for item list (inventory)
	private final int _type2; // different lists for armor, weapon, etc
	private final int _weight;
	private final boolean _stackable;
	private final Grade _crystalType; // default to none-grade
	private final int _durability;
	private final int _period;
	private final int _bodyPart;
	private final int _referencePrice;
	private final short _crystalCount;
	private final int _keepType;
	private final boolean _dropable;
	private final boolean _tradeable;
	private final boolean _destroyable;
	private final boolean _forNpc;
	private final boolean _olympiadUse;
	private final boolean _enchantable;
	private final boolean _elementable;
	private final boolean _isMasterwork;
	private final boolean _isSealed;
	private final boolean _isPvP;
	private final boolean _isCommon;

	private final int _reuseDelay;
	private final int _delayShareGroup;
	private final int _enchanted;

	private final String _action;

	protected L2Skill _skillOnAction;
	protected boolean _offensive;
	protected byte _chance;
	protected boolean _skillOnCritNotCast;

	public ItemEquipListener[] _equipListeners;
	private L2Skill[] _skills;
	private L2Skill[] _enchant4Skills;
	private Condition equipCondition;

	private ItemEnchantTemplate enchantTemplate;

	@SuppressWarnings("unchecked")
	public final Enum<?> type;

	protected FuncTemplate[] _funcTemplates;

	/**
	 * Constructor of the L2Item that fill class variables.<BR><BR>
	 * <U><I>Variables filled :</I></U><BR>
	 * <LI>type</LI>
	 * <LI>_itemId</LI>
	 * <LI>_name</LI>
	 * <LI>_type1 & _type2</LI>
	 * <LI>_weight</LI>
	 * <LI>_crystallizable</LI>
	 * <LI>_stackable</LI>
	 * <LI>_materialType & _crystalType & _crystlaCount</LI>
	 * <LI>_durability</LI>
	 * <LI>_bodypart</LI>
	 * <LI>_referencePrice</LI>
	 * @param type : Enum designating the type of the item
	 * @param set : StatsSet corresponding to a set of couples (key,value) for description of the item
	 */
	@SuppressWarnings("unchecked")
	protected L2Item(final Enum<?> type, final StatsSet set)
	{
		this.type = type;
		_itemId = set.getShort("item_id");
		_class = ItemClass.valueOf(set.getString("class"));
		_name = set.getString("name");
		_icon = set.getString("icon");
		_type1 = set.getInteger("type1"); // needed for item list (inventory)
		_type2 = set.getInteger("type2"); // different lists for armor, weapon, etc
		_weight = set.getInteger("weight");
		_stackable = set.getBool("stackable", false);
		_crystalType = Grade.values()[set.getInteger("crystal_type", Grade.NONE.ordinal())]; // default to none-grade
		_durability = set.getInteger("durability", -1);
		_bodyPart = set.getInteger("bodypart");
		_referencePrice = set.getInteger("price");
		_period = set.getInteger("period", 0);
		_crystalCount = set.getShort("crystal_count", (short) 0);
		_dropable = set.getBool("dropable", true);
		_destroyable = set.getBool("destroyable", true);
		_tradeable = set.getBool("tradeable", true);
		_keepType = set.getInteger("keep_type", 0);
		_forNpc = set.getBool("for_npc", false);
		_olympiadUse = set.getBool("olympiad_use", true);
		_reuseDelay = set.getInteger("reuse_delay", 0);
		_delayShareGroup = set.getInteger("delay_share_group", -1);
		_enchantable = set.getBool("enchantable", false);
		_elementable = set.getBool("elementable", false);
		_isMasterwork = set.getBool("is_masterwork", false);
		_isPvP = set.getBool("is_pvp", false);
		_isSealed = set.getBool("is_sealed", false);
		_isCommon = set.getBool("is_common", false);
		_enchanted = set.getInteger("enchanted", 0);
		_additionalName = set.getString("additional_name", "");
		_action = set.getString("action", "action_none");

		String[] skills = set.getString("skills").split(";");
		if(skills != null)
		{
			for(String skillInfo : skills)
			{
				String[] skill = skillInfo.split("-");
				if(skill != null && skill.length == 2)
				{
					int skillId = Integer.parseInt(skill[0]);
					int skillLvl = Integer.parseInt(skill[1]);
					if(skillId > 0 && skillLvl > 0)
					{
						L2Skill itemSkill = SkillTable.getInstance().getInfo(skillId, skillLvl);

						if(itemSkill != null)
							attachSkill(itemSkill);
					}
				}
			}
		}

		skills = set.getString("enchant4_skills", "").split(";");
		if(skills != null)
		{
			for(String skillInfo : skills)
			{
				String[] skill = skillInfo.split("-");
				if(skill != null && skill.length == 2)
				{
					int skillId = Integer.parseInt(skill[0]);
					int skillLvl = Integer.parseInt(skill[1]);
					if(skillId > 0 && skillLvl > 0)
					{
						L2Skill itemSkill = SkillTable.getInstance().getInfo(skillId, skillLvl);

						if(itemSkill != null)
							if(_enchant4Skills == null)
								_enchant4Skills = new L2Skill[] { itemSkill };
							else
							{
								int len = _enchant4Skills.length;
								L2Skill[] tmp = new L2Skill[len + 1];
								System.arraycopy(_enchant4Skills, 0, tmp, 0, len);
								tmp[len] = itemSkill;
								_enchant4Skills = tmp;
							}
					}
				}
			}
		}
	}

	/**
	 * Returns the itemType.
	 * @return Enum
	 */
	@SuppressWarnings("unchecked")
	public Enum<?> getItemType()
	{
		return type;
	}

	public String getIcon()
	{
		return _icon;
	}

	/**
	 * Returns the durability of th item
	 * @return int
	 */
	public final int getDurability()
	{
		return _durability;
	}

	/**
	 * Returns the ID of the item
	 * @return int
	 */
	public final short getItemId()
	{
		return _itemId;
	}

	public abstract int getItemMask();

	/**
	 * Returns the type 2 of the item
	 * @return int
	 */
	public final int getType2()
	{
		return _type2;
	}

	/**
	 * Returns the weight of the item
	 * @return int
	 */
	public final int getWeight()
	{
		return _weight;
	}

	/**
	 * Returns if the item is crystallizable
	 * @return boolean
	 */
	public final boolean isCrystallizable()
	{
		return isDestroyable() && !isStackable() && getCrystalType() != Grade.NONE && getCrystalCount() > 0;
	}

	/**
	 * Return the type of crystal if item is crystallizable
	 * @return int
	 */
	public final Grade getCrystalType()
	{
		return _crystalType;
	}

	/**
	 * Returns the grade of the item.<BR><BR>
	 * <U><I>Concept :</I></U><BR>
	 * In fact, this fucntion returns the type of crystal of the item.
	 * @return int
	 */
	public final Grade getItemGrade()
	{
		return getCrystalType();
	}

	/**
	 * Returns the quantity of crystals for crystallization
	 * @return int
	 */
	public final int getCrystalCount()
	{
		return _crystalCount;
	}

	/**
	 * Returns the quantity of crystals for crystallization on specific enchant level
	 * @return int
	 */
	public final int getCrystalCount(int enchantLevel)
	{
		if (enchantLevel > 3)
			switch (_type2)
			{
				case TYPE2_SHIELD_ARMOR:
				case TYPE2_ACCESSORY:
					return _crystalCount + crystalEnchantBonusArmor[getCrystalType().externalOrdinal] * (3 * enchantLevel - 6);
				case TYPE2_WEAPON:
					return _crystalCount + crystalEnchantBonusWeapon[getCrystalType().externalOrdinal] * (2 * enchantLevel - 3);
				default:
					return _crystalCount;
			}
		else if (enchantLevel > 0)
			switch (_type2)
			{
				case TYPE2_SHIELD_ARMOR:
				case TYPE2_ACCESSORY:
					return _crystalCount + crystalEnchantBonusArmor[getCrystalType().externalOrdinal] * enchantLevel;
				case TYPE2_WEAPON:
					return _crystalCount + crystalEnchantBonusWeapon[getCrystalType().externalOrdinal] * enchantLevel;
				default:
					return _crystalCount;
			}
		else
			return _crystalCount;
	}

	/**
	 * Returns the name of the item
	 * @return String
	 */
	public final String getName()
	{
		return _name;
	}

	/**
	 * Return the part of the body used with the item.
	 * @return int
	 */
	public final int getBodyPart()
	{
		return _bodyPart;
	}

	/**
	 * Returns the type 1 of the item
	 * @return int
	 */
	public final int getType1()
	{
		return _type1;
	}

	/**
	 * Returns if the item is stackable
	 * @return boolean
	 */
	public final boolean isStackable()
	{
		return _stackable;
	}

	public final int getDefaultEcnhantVal()
	{
		return _enchanted;
	}

	/**
	 * Returns the price of reference of the item
	 * @return int
	 */
	public final int getReferencePrice()
	{
		return _referencePrice;
	}

	/**
	 * Returns if the item can be sold
	 * @return boolean
	 */
	public final boolean isSellable()
	{
		return getReferencePrice() > 0 && isDestroyable();
	}

	public boolean isTradeable()
	{
		return _tradeable;
	}

	public boolean isDestroyable()
	{
		return _destroyable;
	}

	public boolean isDropable()
	{
		return _dropable;
	}

	public boolean isKeepType(int type)
	{
		return (_keepType & type) == type;
	}

	public boolean isEnchantable()
	{
		return _enchantable;
	}

	public boolean isElementable()
	{
		return _elementable;
	}

	public boolean isMagicWeapon()
	{
		return false;
	}

	public boolean isInOlympiadUsable()
	{
		return _olympiadUse;
	}

	public int getReuseDelay()
	{
		return _reuseDelay;
	}

	public int getDelayShareGroup()
	{
		return _delayShareGroup;
	}

	public void attachSkillOnAction(L2Skill skill, byte chance, boolean onCritNotCast)
	{
		_skillOnAction = skill;
		_offensive = skill.isOffensive();
		_chance = chance;
		_skillOnCritNotCast = onCritNotCast;
	}

	/**
	 * Add the FuncTemplate f to the list of functions used with the item
	 * @param f : FuncTemplate to add
	 */
	public void attachFunction(FuncTemplate f, boolean replace)
	{
		if(_funcTemplates == null)
			_funcTemplates = new FuncTemplate[] { f };
		else
		{
			if(replace)
			{
				for(int i = 0; i < _funcTemplates.length; i++)
					if(_funcTemplates[i]._stat == f._stat && f._funcName.equalsIgnoreCase(_funcTemplates[i]._funcName))
					{
						if(_funcTemplates[i]._order == f._order && _funcTemplates[i]._value == f._value)
							return;
						else
						{
							_funcTemplates[i] = f;
							return;
						}
					}
			}

			int len = _funcTemplates.length;
			FuncTemplate[] tmp = new FuncTemplate[len + 1];
			System.arraycopy(_funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			_funcTemplates = tmp;
		}
	}

	public void attachFunction(FuncTemplate f)
	{
		attachFunction(f, false);
	}

	public FuncTemplate[] getAttachedFuncs()
	{
		return _funcTemplates;
	}

	/**
	 * Add the L2Skill skill to the list of skills generated by the item
	 * @param skill : L2Skill
	 */
	public void attachSkill(L2Skill skill)
	{
		if(_skills == null)
			_skills = new L2Skill[] { skill };
		else
		{
			int len = _skills.length;
			L2Skill[] tmp = new L2Skill[len + 1];
			System.arraycopy(_skills, 0, tmp, 0, len);
			tmp[len] = skill;
			_skills = tmp;
		}
	}

	public L2Skill[] getAttachedSkills()
	{
		return _skills;
	}

	public L2Skill getFirstSkill()
	{
		if(_skills != null && _skills.length > 0)
			return _skills[0];
		return null;
	}

	/**
	 * Returns the name of the item
	 * @return String
	 */
	@Override
	public String toString()
	{
		return _name;
	}

	public boolean isAltSeed()
	{
		return _name.contains("Alternative");
	}

	public ItemClass getItemClass()
	{
		return _class;
	}

	/**
	 * Является ли вещь аденой или камнем печати
	 */
	public boolean isAdena()
	{
		return _itemId == 57 || _itemId == 6360 || _itemId == 6361 || _itemId == 6362;
	}

	public boolean isEquipment()
	{
		return _bodyPart != 0 && type != EtcItemType.ARROW && type != EtcItemType.BOLT && type != EtcItemType.BAIT;
	}

	public L2Skill[] getEnchant4Skills()
	{
		return _enchant4Skills;
	}

	public void attachCondition(Condition equipCondition)
	{
		this.equipCondition = equipCondition;
	}

	public boolean checkEquipCondition(L2Character cha, L2ItemInstance item)
	{
		if(cha.getPlayer() != null && cha.getPlayer().isInOlympiadMode() && !isInOlympiadUsable())
		{
			cha.sendPacket(Msg.THIS_ITEM_CANT_BE_EQUIPPED_FOR_THE_OLYMPIAD_EVENT);
			return false;
		}

		if(getEquipCondition() == null)
			return true;

		Env env = new Env(cha, cha, null);
		env.item = item;

		if(!getEquipCondition().test(env))
		{
			String condMsg = getEquipCondition().getMessage();
			int condMsgId = getEquipCondition().getMessageId();
			if(condMsgId != 0)
				cha.sendPacket(new SystemMessage(condMsgId).addItemName(getItemId()));
			else if(condMsg != null)
				cha.sendMessage(condMsg);

			return false;
		}

		return true;
	}

	/**
	 * Определяет призрачный предмет или нет
	 * @return true, если предмет призрачный
	 */
	public boolean isShadowItem()
	{
		return _durability > 0;
	}

	public final boolean isTemporal()
	{
		return _period > 0;
	}

	public final int getPeriod()
	{
		return _period;
	}

	/**
	 * Определяет запечатаный предмет или нет
	 *
	 * @return true, если предмет простой
	 */
	public boolean isSealed()
	{
		return _isSealed;
	}

	/**
	 * Определяет стандартный (простой) предмет или нет
	 * @return true, если предмет простой
	 */

	public boolean isStandartItem()
	{
		return _isCommon;
	}

	/**
	 * Определяет Masterwork предмет или нет
	 *
	 * @return true, если предмет простой
	 */
	public boolean isMasterwork()
	{
		return _isMasterwork;
	}

	/**
	 * Определяет PvP предмет или нет
	 *
	 * @return true, если предмет простой
	 */
	public boolean isPvP()
	{
		return _isPvP;
	}

	/**
	 * Returns the additional name
	 *
	 * @return String
	 */
	public String getAdditionalName()
	{
		return _additionalName;
	}

	public boolean isBossJewel()
	{
		return _itemId >= 6656 && _itemId <= 6662 || _itemId == 8191 || _itemId == 10170 || _itemId == 21712 || _itemId == 16025 || _itemId == 16026 || _itemId == 22714 || _itemId == 22173 || _itemId == 10314 || _itemId == 22175;
	}

	public boolean isForNpc()
	{
		return _forNpc;
	}

	public boolean isHaveSa()
	{
		return false;
	}

	public String getAction()
	{
		return _action;
	}

	public Condition getEquipCondition()
	{
		return equipCondition;
	}

	public void attachEquipListener(ItemEquipListener listener)
	{
		if(_equipListeners == null)
			_equipListeners = new ItemEquipListener[]{ listener };
		else
		{
			int len = _equipListeners.length;
			ItemEquipListener[] tmp = new ItemEquipListener[len + 1];
			System.arraycopy(_equipListeners, 0, tmp, 0, len);
			tmp[len] = listener;
			_equipListeners = tmp;
		}
	}

	public void addEnchantOptions(ItemEnchantTemplate enchantTemplate)
	{
		this.enchantTemplate = enchantTemplate;
	}

	public ItemEnchantTemplate getEnchantOptions()
	{
		return enchantTemplate;
	}
}
