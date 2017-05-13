package ru.l2gw.gameserver.model;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.database.mysql;
import ru.l2gw.extensions.ccpGuard.ConfigProtect;
import ru.l2gw.extensions.listeners.PlayerActionListener;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.extensions.scripts.Events;
import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.extensions.scripts.Scripts.ScriptClassAndMethod;
import ru.l2gw.gameserver.controllers.GameTimeController;
import ru.l2gw.gameserver.controllers.RecipeController;
import ru.l2gw.gameserver.ai.CtrlEvent;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.ai.L2PlayableAI;
import ru.l2gw.gameserver.ai.L2PlayableAI.nextAction;
import ru.l2gw.gameserver.ai.L2PlayerAI;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.handler.IItemHandler;
import ru.l2gw.gameserver.handler.ItemHandler;
import ru.l2gw.gameserver.handler.ScriptHandler;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.instancemanager.*;
import ru.l2gw.gameserver.loginservercon.LSConnection;
import ru.l2gw.gameserver.loginservercon.gspackets.ChangeAccessLevel;
import ru.l2gw.gameserver.model.L2Clan.PledgeRank;
import ru.l2gw.gameserver.model.L2Clan.RankPrivs;
import ru.l2gw.gameserver.model.L2ObjectTasks.*;
import ru.l2gw.gameserver.model.L2Skill.AddedSkill;
import ru.l2gw.gameserver.model.L2Skill.TargetType;
import ru.l2gw.gameserver.model.L2Skill.AbnormalVisualEffect;
import ru.l2gw.gameserver.model.Warehouse.WarehouseType;
import ru.l2gw.gameserver.model.base.*;
import ru.l2gw.gameserver.model.entity.Hero;
import ru.l2gw.gameserver.model.entity.duel.Duel;
import ru.l2gw.gameserver.model.entity.olympiad.Olympiad;
import ru.l2gw.gameserver.model.entity.olympiad.OlympiadInstance;
import ru.l2gw.gameserver.model.entity.recipe.RecipeList;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.entity.siege.territory.TerritoryWar;
import ru.l2gw.gameserver.model.entity.vehicle.L2AirShipHelm;
import ru.l2gw.gameserver.model.entity.vehicle.L2ClanAirship;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.model.instances.*;
import ru.l2gw.gameserver.model.playerSubOrders.*;
import ru.l2gw.gameserver.model.quest.Quest;
import ru.l2gw.gameserver.model.quest.QuestState;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.model.zone.L2Zone.ZoneType;
import ru.l2gw.gameserver.network.GameClient;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Formulas;
import ru.l2gw.gameserver.skills.TimeStamp;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.skills.effects.EffectTemplate;
import ru.l2gw.gameserver.skills.effects.i_summon_friend;
import ru.l2gw.gameserver.skills.funcs.FuncAdd;
import ru.l2gw.gameserver.tables.*;
import ru.l2gw.gameserver.taskmanager.AutoSaveManager;
import ru.l2gw.gameserver.templates.*;
import ru.l2gw.gameserver.templates.L2Armor.ArmorType;
import ru.l2gw.gameserver.templates.L2Weapon.WeaponType;
import ru.l2gw.util.EffectsComparator;
import ru.l2gw.util.Location;
import ru.l2gw.util.Util;

import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import static ru.l2gw.gameserver.model.zone.L2Zone.ZoneType.siege;

/**
 * This class represents all player characters in the world.
 * There is always a client-thread connected to this (except if a player-store is activated upon logout).<BR><BR>
 */
public class L2Player extends L2Playable
{
	private static final org.apache.commons.logging.Log killLog = LogFactory.getLog("pc-kills");
	private Vitality _vitality = new Vitality(this);
	private HuntingBonus _huntingBonus = new HuntingBonus(this);
	private RecommendSystem _recommendSystem = new RecommendSystem(this);
	private FriendList _friendList = null;
	private ContactList _contactList = null;

	public HashMap<Short, L2SubClass> _classlist = new HashMap<Short, L2SubClass>(4);

	public static final short STORE_PRIVATE_NONE = 0;
	public static final short STORE_PRIVATE_SELL = 1;
	public static final short STORE_PRIVATE_BUY = 3;
	public static final short STORE_PRIVATE_MANUFACTURE = 5;
	public static final short STORE_OBSERVING_GAMES = 7;
	public static final short STORE_PRIVATE_SELL_PACKAGE = 8;

	private Map<Integer, Integer> _knownRelations;

	private double _cpUpdateIncCheck = .0;
	private double _cpUpdateDecCheck = .0;
	private double _cpUpdateInterval = .0;
	private double _mpUpdateIncCheck = .0;
	private double _mpUpdateDecCheck = .0;
	private double _mpUpdateInterval = .0;
	private GArray<String> bypasses = null, bypassesBbs = null;
	private GArray<String> links = null;

	private int _lastComapssZone;
	private GArray<AbnormalVisualEffect> _abnormalEffects = new GArray<AbnormalVisualEffect>();

	/**
	 * The table containing all minimum level needed for each Expertise (None, D, C, B, A, S, S80, S84)
	 */
	public static final short[] EXPERTISE_LEVELS = {
			SkillTreeTable.getExpertiseLevel(0), //NONE
			SkillTreeTable.getExpertiseLevel(1), //D
			SkillTreeTable.getExpertiseLevel(2), //C
			SkillTreeTable.getExpertiseLevel(3), //B
			SkillTreeTable.getExpertiseLevel(4), //A
			SkillTreeTable.getExpertiseLevel(5), //S
			SkillTreeTable.getExpertiseLevel(6), //S80
			SkillTreeTable.getExpertiseLevel(7), //S84
	};

	static final org.apache.commons.logging.Log _log = LogFactory.getLog(L2Player.class.getName());

	private GameClient _connection;

	//private L2Object _newTarget = null;

	/**
	 * The level of the L2Player
	 */
	private byte _level;

	/**
	 * The Identifier of the L2Player
	 */
	private int _charId = 0x00030b7a;

	/**
	 * The Experience of the L2Player
	 */
	private long _exp = 0;

	/**
	 * The number of SP of the L2Player
	 */
	private long _sp = 0;

	/**
	 * The Karma of the L2Player (if higher than 0, the name of the L2Player appears in red)
	 */
	private int _karma;

	/**
	 * The number of player killed during a PvP (the player killed was PvP Flagged)
	 */
	private int _pvpKills;

	/**
	 * The hexadecimal Color of players name (white is 0xFFFFFF)
	 */
	private int _nameColor;

	/**
	 * The hexadecimal Color of players title (white is 0xFFFFFF)
	 */
	private int _titlecolor;

	/**
	 * The PK counter of the L2Player (= Number of non PvP Flagged player killed)
	 */
	private int _pkKills;

	private boolean _isHourglassEffected;

	private int _curWeightPenalty = 0;

	private int _deleteTimer;
	private final PcInventory _inventory = new PcInventory(this);
	private PcWarehouse _warehouse = new PcWarehouse(this);
	private PcFreight _freight = new PcFreight(this);

	/**
	 * True if the L2Player is sitting
	 */
	boolean _sittingTask;
	/**
	 * Time counter when L2Player is sitting
	 */
	private int _waitTimeWhenSit;

	/**
	 * True if the L2Player is using the relax skill
	 */
	private boolean _relax;

	/**
	 * The face type Identifier of the L2Player
	 */
	private byte _face;

	/**
	 * The hair style Identifier of the L2Player
	 */
	private byte _hairStyle;

	/**
	 * The hair color Identifier of the L2Player
	 */
	private byte _hairColor;

	/**
	 * The table containing all Quests began by the L2Player
	 */
	private Map<String, QuestState> _quests = new ConcurrentHashMap<>();

	/**
	 * The list containing all shortCuts of this L2Player
	 */
	private ShortCuts _shortCuts = new ShortCuts(this);

	/**
	 * The list containing all macroses of this L2Player
	 */
	private MacroList _macroses = new MacroList(this);

	private L2TradeList _tradeList;
	private L2ManufactureList _createList;
	private ConcurrentLinkedQueue<TradeItem> _sellList;
	private ConcurrentLinkedQueue<TradeItem> _buyList;

	/**
	 * The Private Store type of the L2Player (STORE_PRIVATE_NONE=0, STORE_PRIVATE_SELL=1, sellmanage=2, STORE_PRIVATE_BUY=3, buymanage=4, STORE_PRIVATE_MANUFACTURE=5)
	 */
	private short _privatestore;
	private boolean _privateStoreManage = false;
	private ClassId _skillLearningClassId;

	// hennas
	private final L2HennaInstance[] _henna = new L2HennaInstance[3];
	private short _hennaSTR;
	private short _hennaINT;
	private short _hennaDEX;
	private short _hennaMEN;
	private short _hennaWIT;
	private short _hennaCON;

	/**
	 * The L2Summon of the L2Player
	 */
	private L2Summon _summon = null;
	private L2DecoyInstance _decoy = null;
	private L2TrapInstance _trap = null;

	// client radar
	public L2Radar radar;
	private final StatsChangeRecorder _statsChangeRecorder = new StatsChangeRecorder(this);

	// these values are only stored temporarily
	private boolean _partyMatchingAutomaticRegistration;
	private boolean _partyMatchingShowLevel;
	private boolean _partyMatchingShowClass;
	private String _partyMatchingMemo;

	private Point _lastPartyPositionSent = new Point(0, 0);
	private L2Party _party;
	// clan related attributes

	/**
	 * The Clan Identifier of the L2Player
	 */
	private int _clanId;

	private PledgeRank _pledgeRank = PledgeRank.VAGABOND;
	private int _pledgeType = 0;
	private int _powerGrade = 0;
	private int _lvlJoinedAcademy = 0;
	private int _apprentice = 0;

	/**
	 * The Clan Leader Flag of the L2Player (True : the L2Player is the leader of the clan)
	 */
	private boolean _clanLeader;

	private long _leaveClanTime;
	private long _deleteClanTime;

	private long onlineTime;
	private long onlineBeginTime;

	private long _NoChannel; // Nochannel mode
	private long _NoChannelBegin;

	//GM Stuff
	private int _accessLevel;

	private boolean _messageRefusal = false; // message refusal mode
	private boolean _tradeRefusal = false; // Trade refusal
	private boolean _exchangeRefusal = false; // Exchange refusal

	// this is needed to find the inviting player for Party response
	// there can only be one active party request at once
	private L2Player _currentTransactionRequester;
	public long _currentTransactionTimeout;
	private L2ItemInstance _arrowItem;

	private String _accountName;
	private String _lastHWID;

	private HashMap<Integer, String> _chars = new HashMap<Integer, String>(8);

	/**
	 * The table containing all RecipeList of the L2Player
	 */
	private Map<Integer, RecipeList> _recipebook = new TreeMap<Integer, RecipeList>();
	private Map<Integer, RecipeList> _commonrecipebook = new TreeMap<Integer, RecipeList>();

	/**
	 * Teleport Book List
	 */
	private TeleportBook _teleportBook;

	// Floating rates
	private FloatingRate _floatingRate;

	// stats watch
	int oldMaxHP;
	int oldMaxMP;
	int oldMaxCP;

	/**
	 * The current higher Expertise of the L2Player (None=0, D=1, C=2, B=3, A=4, S=5)
	 */
	public short expertiseIndex = 0;
	private short weaponPenalty = 0;
	private short armorPenalty = 0;

	private L2ItemInstance _enchantScroll = null;
	private L2ItemInstance _enchantSupport = null;
	private long _enchantStartTime = 0;

	private WarehouseType _usingWHType;
	private boolean _isOnline = false;
	private boolean _isDeleting = false;

	protected boolean _inventoryDisable = false;
	protected boolean _whDisable = false;

	protected FastList<L2CubicInstance> _cubics = new FastList<L2CubicInstance>(4);
	private int _agathionId = 0;

	/**
	 * The L2NpcInstance corresponding to the last Folk which one the player talked.
	 */
	private L2NpcInstance _lastFolkNpc = null;
	private int _npcDialogEndTime;
	/**
	 * Для закрытия чита с покупкой любых айтемов через мультиселы на большом расстоянии от продовца
	 */
	private L2NpcInstance _lastMultisellNpc = null;

	protected ConcurrentSkipListSet<Integer> _activeSoulShots = new ConcurrentSkipListSet<Integer>();

	/**
	 * 1 if	the player is invisible
	 */
	private boolean _invisible = false;

	/**
	 * Location before entering Observer Mode
	 */
	private L2WorldRegion _observRegion;
	private boolean _observerMode = false;

	public int _telemode = 0;
	public int _unstuck = 0;

	public boolean isResivedExp = false;
	private Map<Integer, Long> _botReports = new HashMap<>();

	/**
	 * Эта точка проверяется при нештатном выходе чара, и если не равна null чар возвращается в нее
	 * Используется например для возвращения при падении с виверны
	 * Поле heading используется для хранения денег возвращаемых при сбое
	 *
	 * @see ru.l2gw.gameserver.network.GameClient#onDisconnection()
	 */
	private Location stablePoint = null;

	/**
	 * new loto ticket *
	 */
	public int _loto[] = new int[5];
	/**
	 * new race ticket *
	 */
	public int _race[] = new int[2];

	private final Map<Integer, String> blockList = new HashMap<>(); // characters blocked with '/block <charname>' cmd
	HashMap<Integer, L2Skill> _skillChainDetail = new HashMap<Integer, L2Skill>();
	
	private boolean _blockAll = false; // /blockall cmd handling

	private boolean _isConnected = true;

	private boolean _hero = false;
	private int _team = 0;

	// time on login in game
	private long _lastAccess;
	// logoff time
	private long _logoutTime;

	// Birthday
	private long _birthday;
	/**
	 * True if the L2Player is in a boat
	 */
	private L2Vehicle _vehicle;

	protected int _baseClass;
	protected byte _baseLevel;
	protected long _baseExp;
	protected long _baseSp;
	protected short _activeClass = -1;

	public boolean _isSitting = false;

	private boolean _noble = false;
	private boolean _inOlympiadMode = false;
	private int _olympiadGameId = -1;
	private int _olympiadSide = -1;

	/**
	 * Duel
	 */
	private int _duelSide = 0;
	private Duel _duel = null;

	/**
	 * ally with ketra or varka related wars
	 */
	private int _varka = 0;
	private int _ketra = 0;
	private int _ram = 0;

	/**
	 * The Siege state
	 */
	private int _siegeState = 0;
	private int _siegeId = 0;
	private long _lastFameUpdate = 0;
	private int _territoryId = 0;

	private byte[] _keyBindings;

	@SuppressWarnings("unchecked")
	private ScheduledFuture<?> _waterTask;

	private int _cursedWeaponEquippedId = 0;

	private L2Fishing _fishCombat;
	private boolean _fishing = false;
	private Location _fishLoc = new Location(0, 0, 0);
	private L2ItemInstance _lure = null;
	@SuppressWarnings("unchecked")
	public ScheduledFuture<?> _taskforfish;

	private ScheduledFuture<?> _premiumExpire;

	private boolean _offline = false;

	/**
	 * Трансформация
	 */
	private int _transformationId;
	private int _transformationTemplate;
	private String _transformationName;

	private int pcBangPoints;

	/**
	 * PartyMatching
	 */

	private int _partyMatchingRegion;
	private int _partyMatchingLevels;

	private Integer partyRoom = 0;

	/**
	 * Коллекция для временного хранения скилов данной трансформации
	 */
	HashMap<Integer, L2Skill> _transformationSkills = new HashMap<Integer, L2Skill>();

	private int _famePoints = 0;
	private int _movieId;

	/**
	 * _userSession - испольюзуется для хранения временных переменных.
	 */
	private Map<String, String> _userSession;

	private boolean _notShowBuffAnim;

	public final ReentrantLock skillEnchantLock = new ReentrantLock();

	/**
	 * Constructor of L2Player (use L2Character constructor).<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Call the L2Character constructor to create an empty _skills slot and copy basic Calculator set to this L2Player </li>
	 * <li>Set the name of the L2Player</li><BR><BR>
	 * <p/>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method SET the level of the L2Player to 1</B></FONT><BR><BR>
	 *
	 * @param objectId	Identifier of the object to initialized
	 * @param template	The L2PlayerTemplate to apply to the L2Player
	 * @param accountName The name of the account including this L2Player
	 */
	protected L2Player(final int objectId, final L2PlayerTemplate template, final String accountName)
	{
		super(objectId, template);
		setOwner(this);
		super.initCharStatusUpdateValues();
		initPcStatusUpdateValues();

		_accountName = accountName;
		_level = 1;
		_nameColor = 0xFFFFFF;
		_titlecolor = 0xFFFF77;
		_baseClass = getClassId().getId();
		_baseLevel = _level;
		_baseExp = _exp;
		_baseSp = _sp;
	}

	/**
	 * Constructor of L2Player (use L2Character constructor).<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Call the L2Character constructor to create an empty _skills slot and copy basic Calculator set to this L2Player </li>
	 * <li>Create a L2Radar object</li>
	 * <li>Retrieve from the database all items of this L2Player and add them to _inventory </li>
	 * <p/>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SET the account name of the L2Player</B></FONT><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2PlayerTemplate to apply to the L2Player
	 */
	protected L2Player(final int objectId, final L2PlayerTemplate template)
	{
		this(objectId, template, null);

		super.initCharStatusUpdateValues();
		initPcStatusUpdateValues();

		//restore inventory
		getInventory().restore();

		// Create an AI
		setAI(new L2PlayerAI(this));

		// Create a L2Radar object
		radar = new L2Radar(this);

		// Retrieve from the database all macroses of this L2Player and add them to _macroses
		_macroses.restore();
	}

	/**
	 * Create a new L2Player and add it in the characters table of the database.<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Create a new L2Player with an account name </li>
	 * <li>Set the name, the Hair Style, the Hair Color and	the Face type of the L2Player</li>
	 * <li>Add the player in the characters table of the database</li><BR><BR>
	 *
	 * @param accountName The name of the L2Player
	 * @param name		The name of the L2Player
	 * @param hairStyle   The hair style Identifier of the L2Player
	 * @param hairColor   The hair color Identifier of the L2Player
	 * @param face		The face type Identifier of the L2Player
	 * @return The L2Player added to the database or null
	 */
	public static L2Player create(short classId, byte sex, String accountName, final String name, final byte hairStyle, final byte hairColor, final byte face)
	{
		L2PlayerTemplate template = CharTemplateTable.getInstance().getTemplate(classId, sex != 0);

		// Create a new L2Player with an account name
		L2Player player = new L2Player(IdFactory.getInstance().getNextId(), template, accountName);

		player.setName(name);
		player.setTitle("");
		player.setHairStyle(hairStyle);
		player.setHairColor(hairColor);
		player.setFace(face);

		// Add the player in the characters table of the database
		if(!player.createDb())
			return null;

		return player;
	}

	public String getAccountName()
	{
		if(isInOfflineMode())
			return _accountName;
		if(_connection == null)
			return "<not connected>";
		return _connection.getLoginName();
	}

	public String getIP()
	{
		if(_connection == null)
			return "<not connected>";
		return _connection.getIpAddr();
	}

	/**
	 * Возвращает список персонажей на аккаунте, за исключением текущего
	 *
	 * @return Список персонажей
	 */
	public HashMap<Integer, String> getAccountChars()
	{
		return _chars;
	}

	/**
	 * Retrieve a L2Player from the characters table of the database and add it in _allObjects of the L2world (call restore method).<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Retrieve the L2Player from the characters table of the database </li>
	 * <li>Add the L2Player object in _allObjects </li>
	 * <li>Set the x,y,z position of the L2Player and make it invisible</li>
	 * <li>Update the overloaded status of the L2Player</li><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 * @return The L2Player loaded from the database
	 */
	public static L2Player load(final int objectId, String HWID)
	{
		return restore(objectId, HWID);
	}

	private void initPcStatusUpdateValues()
	{
		_cpUpdateInterval = getMaxCp() / 352.0;
		_cpUpdateIncCheck = getMaxCp();
		_cpUpdateDecCheck = getMaxCp() - _cpUpdateInterval;
		_mpUpdateInterval = getMaxMp() / 352.0;
		_mpUpdateIncCheck = getMaxMp();
		_mpUpdateDecCheck = getMaxMp() - _mpUpdateInterval;
	}

	public static L2Player load(final int objectId)
	{
		return restore(objectId, "");
	}

	/**
	 * @return the L2PlayerTemplate link to the L2Player.<BR><BR>
	 */
	@Override
	public final L2PlayerTemplate getTemplate()
	{
		return (L2PlayerTemplate) _template;
	}

	@Override
	public L2PlayerTemplate getBaseTemplate()
	{
		return (L2PlayerTemplate) _baseTemplate;
	}

	public void changeSex()
	{
		boolean male = true;
		if(getSex() == 1)
			male = false;
		_template = CharTemplateTable.getInstance().getTemplate(getClassId(), !male);
	}

	/**
	 * @return the AI of the L2Player (create it if necessary).<BR><BR>
	 */
	@Override
	public L2PlayableAI getAI()
	{
		if(_ai == null)
			_ai = new L2PlayerAI(this);
		return (L2PlayableAI) _ai;
	}
	
	public L2PlayerAI getPlayerAI()
	{
		return (L2PlayerAI) _ai;
	}

	@Override
	public void doAttack(final L2Character target)
	{
		//нельзя атаковать чара с проклятым оружием, если уровент атакущего чара < 20
		//чар c проклятым оружием, не может атаковать чаров с лвл < 20
		if(target instanceof L2Playable && (isCursedWeaponEquipped() && target.getLevel() < 20 || target.isCursedWeaponEquipped() && getLevel() < 20))
		{
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null, null);
			return;
		}

		if(_duel != null && _duel.isPartyDuel() && getDuelState() == Duel.DUELSTATE_DEAD)
		{
			sendPacket(Msg.YOU_CANNOT_MOVE_IN_A_FROZEN_STATE_PLEASE_WAIT_A_MOMENT);
			return;
		}

		super.doAttack(target);
	}

	@Override
	public void doCast(final L2Skill skill, final L2Character target, final L2ItemInstance usedItem, boolean forceUse)
	{
		if(skill == null)
			return;

		if(_duel != null && _duel.isPartyDuel() && getDuelState() == Duel.DUELSTATE_DEAD)
		{
			sendPacket(Msg.YOU_CANNOT_MOVE_IN_A_FROZEN_STATE_PLEASE_WAIT_A_MOMENT);
			return;
		}

		super.doCast(skill, target, usedItem, forceUse);
		skill.setIsUseMacro(false);
		setUseMacro(false);
	}

	@Override
	public void updateEffectIcons()
	{
		if(_massUpdating)
			return;

		L2Effect[] effects = getAllEffectsArray();
		Arrays.sort(effects, EffectsComparator.getInstance());
		final AbnormalStatusUpdate mi = new AbnormalStatusUpdate();
		final PartySpelled ps = _party != null ? new PartySpelled(this, false) : null;
		final ExOlympiadSpelledInfo os = isOlympiadStart() ? new ExOlympiadSpelledInfo(getObjectId()) : null;

		for(final L2Effect effect : effects)
		{
			if(effect == null || !effect.isInUse())
				continue;

			if(effect.getSkill().getAbnormalTypes().contains("life_force"))
				sendPacket(new ShortBuffStatusUpdate(effect));
			else
			{
				L2Effect.addIcon(effect, mi);
				if(ps != null)
					L2Effect.addIcon(effect, ps);
			}

			if(os != null)
				L2Effect.addIcon(effect, os);
		}

		sendPacket(mi);
		if(ps != null)
			_party.broadcastToPartyMembers(this, ps);

		if(os != null)
			Olympiad.broadcastToSpectators(getOlympiadGameId(), os);
	}

	@Override
	public void sendChanges()
	{
		if(!_massUpdating)
			_statsChangeRecorder.sendChanges();
	}

	/**
	 * @return the Level of the L2Player.<BR><BR>
	 */
	@Override
	public final byte getLevel()
	{
		return _level;
	}

	public final void setLevel(final int lvl)
	{
		_level = (byte) lvl;
	}

	/**
	 * @return the Sex of the L2Player (Male=0, Female=1).<BR><BR>
	 */
	public byte getSex()
	{
		return getTemplate().isMale ? (byte) 0 : (byte) 1;
	}

	/**
	 * @return the Face type Identifier of the L2Player.<BR><BR>
	 */
	public byte getFace()
	{
		return _face;
	}

	/**
	 * Set the Face type of the L2Player.<BR><BR>
	 *
	 * @param face The Identifier of the Face type<BR><BR>
	 */
	public void setFace(final byte face)
	{
		_face = face;
	}

	/**
	 * @return the Hair Color Identifier of the L2Player.<BR><BR>
	 */
	public int getHairColor()
	{
		return _hairColor;
	}

	/**
	 * Set the Hair Color of the L2Player.<BR><BR>
	 *
	 * @param hairColor The Identifier of the Hair Color<BR><BR>
	 */
	public void setHairColor(final byte hairColor)
	{
		_hairColor = hairColor;
	}

	/**
	 * @return the Hair Style Identifier of the L2Player.<BR><BR>
	 */
	public int getHairStyle()
	{
		return _hairStyle;
	}

	/**
	 * Set the Hair Style of the L2Player.<BR><BR>
	 *
	 * @param hairStyle The Identifier of the Hair Style<BR><BR>
	 */
	public void setHairStyle(final byte hairStyle)
	{
		_hairStyle = hairStyle;
	}

	public boolean isInStoreMode()
	{
		return _privatestore != STORE_PRIVATE_NONE || _privateStoreManage;
	}

	/**
	 * @return the Max HP (base+modifier) of the L2Player and Launch a Regen Task (if necessary).<BR><BR>
	 */
	@Override
	public int getMaxHp()
	{
		int val = (int) calcStat(Stats.MAX_HP, getTemplate().baseHp[_level], null, null);

		if(val != oldMaxHP)
		{
			oldMaxHP = val;

			// Launch a regen task if the new Max HP is higher than the old one
			if(getCurrentHp() != val)
				setCurrentHp(getCurrentHp()); // trigger start of regeneration
		}

		return val;
	}

	/**
	 * @return the Max MP (base+modifier) of the L2Player and Launch a Regen Task (if necessary).<BR><BR>
	 */
	@Override
	public int getMaxMp()
	{

		int val = (int) calcStat(Stats.MAX_MP, getTemplate().baseMp[_level], null, null);

		if(val != oldMaxMP)
		{
			oldMaxMP = val;

			// Launch a regen task if the new Max MP is higher than the old one
			if(!isDead() && getCurrentMp() != val)
				setCurrentMp(getCurrentMp()); // trigger start of regeneration
		}

		return val;

	}

	@Override
	public int getMaxCp()
	{
		int val = (int) calcStat(Stats.MAX_CP, getTemplate().baseCp[_level], null, null);

		if(val != oldMaxCP)
		{
			oldMaxCP = val;

			// Launch a regen task if the new Max MP is higher than the old one
			if(!isDead() && getCurrentCp() != val)
				setCurrentCp(getCurrentCp()); // trigger start of regeneration
		}

		return val;

	}

	public void offline()
	{
		setNameColor(Config.SERVICES_OFFLINE_TRADE_NAME_COLOR);
		setOfflineMode(true);
		setPrivateStoreType(_privatestore);
		saveTradeList();

		try
		{
			if(getClanId() != 0)
				getClan().notifyClanMembers(this, false);
		}
		catch(Exception e)
		{
			_log.warn("offline()", e);
			e.printStackTrace();
		}

		if(getNetConnection() != null)
			getNetConnection().stopPingTask();

		if(getParty() != null)
			getParty().oustPartyMember(this);

		CursedWeaponsManager.getInstance().doLogout(this);

		if(Olympiad.isRegisteredInComp(this))
		{
			try
			{
				Olympiad.removeFromReg(this);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

		if(getPet() != null)
			getPet().unSummon();

		if(getOlympiadGameId() != -1 && !inObserverMode()) // handle removal from olympiad game
			Olympiad.removeDisconnectedCompetitor(this);

		if(Config.ALT_OLY_ENABLE_HWID_CHECK)
			Olympiad.removeHWID(getLastHWID());

		sendPacket(Msg.LeaveWorld);
		setConnected(false);
		//LSConnection.getInstance().removeAccount(getNetConnection());
		//LSConnection.getInstance().sendPacket(new PlayerLogout(getNetConnection().getLoginName()));
		broadcastUserInfo(true);
	}

	/**
	 * Сохраняет персонажа в бд и запускает необходимые процедуры.
	 *
	 * @param shutdown тру при шатдауне
	 * @param restart  тру при рестарте. Игнорируется шатдаун.
	 * @param kicked   Отобразить у клиента табличку с мессагой о закрытии коннекта
	 */
	public void logout(final boolean shutdown, final boolean restart, final boolean kicked)
	{
		if(isLogoutStarted())
			return;

		setLogoutStarted(true);
		prepareToLogout(kicked);

		//saveCharToDisk();
		// При рестарте просто обнуляем коннект
		if(restart)
		{
			if(_connection != null)
				_connection.setPlayer(null);
			_connection = null;
			setConnected(false);
		}
		else
			kick(kicked);
	}


	/**
	 * Logout without save.<BR><BR>
	 */
	private void kick(final boolean kicked)
	{
		if(_connection != null)
		{
			L2GameServerPacket sp;
			if(kicked)
				sp = Msg.ServerClose;
			else
				sp = Msg.LeaveWorld;

			if(_connection.getConnection() != null)
			{
				_connection.setPlayer(null);
				_connection.getConnection().close(sp);
			}
			_connection = null;
			setConnected(false);
		}
	}

	public void prepareToLogout(boolean kicked)
	{
		if(isFlying() && !checkLandingState())
			teleToClosestTown();

		if(isCastingNow())
			abortCast();

		if(kicked && Config.DROP_CURSED_WEAPONS_ON_KICK)
			if(isCursedWeaponEquipped())
			{
				_pvpFlag = 0;
				CursedWeaponsManager.getInstance().dropPlayer(this);
			}

		CursedWeaponsManager.getInstance().doLogout(this);

		if(isCombatFlagEquipped() && getActiveWeaponInstance() != null)
		{
			if(getActiveWeaponInstance().isTerritoryWard())
			{
				destroyItem("Relogin", getActiveWeaponInstance().getObjectId(), 1, null, false);
				_combatFlagEquippedId = false;
				TerritoryWarManager.respawnWard(getObjectId());
			}
			else
				FortressSiegeManager.getInstance().dropCombatFlag(this);
		}

		// Вызов всех хэндлеров, определенных в скриптах
		final Object[] script_args = new Object[]{this};
		for(final ScriptClassAndMethod handler : Scripts.onPlayerExit)
			callScripts(handler.scriptClass, handler.method, script_args);

		String wyvernPrice = getVar("wyvern_moneyback");
		if(wyvernPrice != null)
		{
			addAdena("wyvern_moneyback", Integer.valueOf(wyvernPrice), null, false);
			unsetVar("wyvern_moneyback");
		}

		if(isInParty())
			getParty().oustPartyMember(this);

		if(getMountEngine().isMounted())
			getMountEngine().dismount();

		nulledMountEngine();
		if(getVitality() != null)
			getVitality().stopUpdatetask();
		deleteMe();
	}

	/**
	 * @return a table containing all RecipeList of the L2Player.<BR><BR>
	 */
	public Collection<RecipeList> getDwarvenRecipeBook()
	{
		return _recipebook.values();
	}

	public Collection<RecipeList> getCommonRecipeBook()
	{
		return _commonrecipebook.values();
	}

	public boolean findRecipe(final RecipeList id)
	{
		return _recipebook.containsValue(id) || _commonrecipebook.containsValue(id);
	}

	public boolean findRecipe(final int id)
	{
		return _recipebook.containsKey(id) || _commonrecipebook.containsKey(id);
	}

	/**
	 * Add a new L2RecipList to the table _recipebook containing all RecipeList of the L2Player <BR><BR>
	 *
	 * @param recipe The RecipeList to add to the _recipebook
	 */
	public void registerRecipe(final RecipeList recipe, boolean saveDB)
	{
		if(recipe.isCommon() == 0)
			_recipebook.put(recipe.getId(), recipe);
		else
			_commonrecipebook.put(recipe.getId(), recipe);
		if(saveDB)
			mysql.set("REPLACE INTO character_recipebook (char_id, id) values(" + getObjectId() + "," + recipe.getId() + ")");
	}

	/**
	 * Remove a L2RecipList from the table _recipebook containing all RecipeList of the L2Player <BR><BR>
	 *
	 * @param RecipeID The Identifier of the RecipeList to remove from the _recipebook
	 */
	public void unregisterRecipe(final int RecipeID)
	{
		if(_recipebook.containsKey(RecipeID))
		{
			mysql.set("DELETE FROM `character_recipebook` WHERE `char_id`=" + getObjectId() + " AND `id`=" + RecipeID + " LIMIT 1");
			_recipebook.remove(RecipeID);
		}
		else if(_commonrecipebook.containsKey(RecipeID))
		{
			mysql.set("DELETE FROM `character_recipebook` WHERE `char_id`=" + getObjectId() + " AND `id`=" + RecipeID + " LIMIT 1");
			_commonrecipebook.remove(RecipeID);
		}
		else
			_log.warn("Attempted to remove unknown RecipeList" + RecipeID);
	}

	// ------------------- Quest Engine ----------------------

	/**
	 * @param quest The name of the quest
	 * @return the QuestState object corresponding to the quest name.
	 */
	public QuestState getQuestState(String quest)
	{
		return _quests != null ? _quests.get(quest) : null;
	}

	public QuestState getQuestState(int questId)
	{
		if(_quests != null)
			for(QuestState qs : _quests.values())
				if(qs.getQuest().getQuestIntId() == questId)
					return qs;

		return null;
	}

	/**
	 * Add a QuestState to the table _quest containing all quests began by the L2Player.<BR><BR>
	 *
	 * @param qs The QuestState to add to _quest
	 */
	public void setQuestState(QuestState qs)
	{
		_quests.put(qs.getQuest().getName(), qs);
	}

	/**
	 * Remove a QuestState from the table _quest containing all quests began by the L2Player.<BR><BR>
	 *
	 * @param quest The name of the quest
	 */
	public void delQuestState(String quest)
	{
		_quests.remove(quest);
	}

	/**
	 * @return a table containing all Quest in progress from the table _quests.<BR><BR>
	 */
	public GArray<QuestState> getAllActiveQuests()
	{
		GArray<QuestState> quests = new GArray<QuestState>();
		for(final QuestState qs : _quests.values())
		{
			if(qs == null || qs.getQuest().isCustom() || qs.isCompleted() || !qs.isStarted())
				continue;
			quests.add(qs);
		}
		return quests;
	}

	public Collection<QuestState> getAllQuestsStates()
	{
		return _quests.values();
	}

	/**
	 * @param npc L2NpcInstance attacked
	 * @return a table containing all QuestState to modify after a L2NpcInstance killing.<BR><BR>
	 */
	public ArrayList<QuestState> getQuestsForAttacks(L2NpcInstance npc)
	{
		ArrayList<QuestState> states = new ArrayList<QuestState>();
		Quest[] quests = npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_ATTACKED);
		if(quests != null)
			for(Quest quest : quests)
				if(getQuestState(quest.getName()) != null && !getQuestState(quest.getName()).isCompleted())
					states.add(getQuestState(quest.getName()));
		return states;
	}

	/**
	 * @param npc L2NpcInstance that was killed
	 * @return a table containing all QuestState to modify after a L2NpcInstance killing.
	 */
	public ArrayList<QuestState> getQuestsForKills(L2NpcInstance npc)
	{
		ArrayList<QuestState> states = new ArrayList<QuestState>();
		Quest[] quests = npc.getTemplate().getEventQuests(Quest.QuestEventType.ON_KILLED);
		if(quests != null)
			for(Quest quest : quests)
				states.add(getQuestState(quest.getName()));
		return states;
	}

	/**
	 * @param npcId The Identifier of the NPC
	 * @return a table containing all QuestState from the table _quests in which the L2Player must talk to the NPC.<BR><BR>
	 */
	public ArrayList<QuestState> getQuestsForTalk(int npcId)
	{
		ArrayList<QuestState> states = new ArrayList<QuestState>();
		Quest[] quests = NpcTable.getTemplate(npcId).getEventQuests(Quest.QuestEventType.QUEST_TALK);
		if(quests != null)
			for(Quest quest : quests)
				if(getQuestState(quest.getName()) != null && !getQuestState(quest.getName()).isCompleted())
					states.add(getQuestState(quest.getName()));
		return states;
	}

	public void processQuestEvent(String quest, String event)
	{
		if(event == null)
			event = "";

		QuestState qs = getQuestState(quest);
		if(qs == null)
		{
			Quest q = QuestManager.getQuest(quest);
			if(q == null)
				return;

			if(!q.isCustom())
			{
				if(!isQuestContinuationPossible())
					return;

				if(getQuestCount() >= Config.ALT_MAX_QUESTS)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile("data/html/fullquest.htm");
					sendPacket(html);
					sendActionFailed();
					return;
				}
			}

			qs = q.newQuestState(this);
		}
		else if(!qs.getQuest().isCustom() && !isQuestContinuationPossible())
			return;

		if(qs == null)
			return;
		qs.getQuest().notifyEvent(event, qs);
	}

	/**
	 * Проверка на переполнение инвентаря и перебор в весе для квестов и эвентов
	 *
	 * @return true если ве проверки прошли успешно
	 */
	public boolean isQuestContinuationPossible()
	{
		return isQuestContinuationPossible(true);
	}

	public boolean isQuestContinuationPossible(boolean msg)
	{
		if(getInventory().getQuestItemsCount() >= Config.INVENTORY_MAXIMUM_QUEST * 0.9 ||
				getInventory().getTotalWeight() >= getMaxLoad() * 0.8 ||
				getInventoryItemsCount() >= getInventoryLimit() * 0.9)
		{
			if(msg)
				sendPacket(Msg.YOU_CAN_PROCEED_ONLY_WHEN_THE_INVENTORY_WEIGHT_IS_BELOW_80_PERCENT_AND_THE_QUANTITY_IS_BELOW_90_PERCENT);
			return false;
		}
		return true;
	}

	public boolean isQuestComplete(int questId)
	{
		if(_quests != null)
			for(QuestState qs : _quests.values())
				if(qs.getQuest().getQuestIntId() == questId)
					return qs.isCompleted();
		return false;
	}

	public boolean isQuestStarted(int questId)
	{
		if(_quests != null)
			for(QuestState qs : _quests.values())
				if(qs.getQuest().getQuestIntId() == questId)
					return qs.isStarted();
		return false;
	}

	public int getQuestCount()
	{
		if(_quests == null)
			return 0;

		int c = 0;
		for(QuestState qs : _quests.values())
			if(!qs.getQuest().isCustom() && qs.isStarted())
				c++;

		return c;
	}

	// ----------------- End of Quest Engine -------------------

	/**
	 * @return a table containing all L2ShortCut of the L2Player.<BR><BR>
	 */
	public Collection<L2ShortCut> getAllShortCuts()
	{
		return _shortCuts.getAllShortCuts();
	}

	/**
	 * Return the L2ShortCut of the L2Player corresponding to the position (page-slot).<BR><BR>
	 *
	 * @param slot The slot in witch the shortCuts is equipped
	 * @param page The page of shortCuts containing the slot
	 * @return the L2ShortCut of the L2Player corresponding to the position (page-slot).
	 */
	public L2ShortCut getShortCut(int slot, int page)
	{
		return _shortCuts.getShortCut(slot, page);
	}

	/**
	 * Add a L2shortCut to the L2Player _shortCuts<BR><BR>
	 */
	public void registerShortCut(L2ShortCut shortcut)
	{
		_shortCuts.registerShortCut(shortcut);
	}

	/**
	 * Delete the L2ShortCut corresponding to the position (page-slot) from the L2Player _shortCuts.<BR><BR>
	 */
	public void deleteShortCut(int slot, int page)
	{
		_shortCuts.deleteShortCut(slot, page);
	}

	/**
	 * Add a L2Macro to the L2Player _macroses<BR><BR>
	 */
	public void registerMacro(L2Macro macro)
	{
		_macroses.registerMacro(macro);
	}

	/**
	 * Delete the L2Macro corresponding to the Identifier from the L2Player _macroses.<BR><BR>
	 */
	public void deleteMacro(int id)
	{
		_macroses.deleteMacro(id);
	}

	/**
	 * @return all L2Macro of the L2Player.<BR><BR>
	 */
	public MacroList getMacroses()
	{
		return _macroses;
	}

	/**
	 * Возвращает состояние осады L2Player.<BR>
	 * 1 = attacker, 2 = defender, 0 = не учавствует, 3 - Territory War
	 *
	 * @return состояние осады
	 */
	public int getSiegeState()
	{
		return _siegeState;
	}

	public int getSiegeId()
	{
		return _siegeId;
	}

	/**
	 * Устанавливает состояние осады L2Player.<BR>
	 * 1 = attacker, 2 = defender, 0 = не учавствует
	 */
	public void setSiegeState(int siegeState)
	{
		_siegeState = siegeState;
		if(_siegeState == 3)
		{
			if(getVarInt("tw_last") != _territoryId)
			{
				setVar("tw_badges", 0);
				setVar("tw_last", _territoryId);
				_badges = 0;
			}
			else
				_badges = getVarFloat("tw_badges");
		}
	}

	public void setSiegeId(int siegeId)
	{
		_siegeId = siegeId;
	}

	public boolean isSiegeUnitLordClanMember(final int UnitId)
	{
		L2Clan clan = getClan();
		return clan != null && (clan.getHasCastle() == UnitId || clan.getHasFortress() == UnitId || clan.getHasHideout() == UnitId);
	}

	/**
	 * @return the PK counter of the L2Player.<BR><BR>
	 */
	public int getPkKills()
	{
		return _pkKills;
	}

	/**
	 * Set the PK counter of the L2Player.<BR><BR>
	 */
	public void setPkKills(final int pkKills)
	{
		_pkKills = pkKills;
	}

	/**
	 * @return the _deleteTimer of the L2Player.<BR><BR>
	 */
	public int getDeleteTimer()
	{
		return _deleteTimer;
	}

	/**
	 * Set the _deleteTimer of the L2Player.<BR><BR>
	 */
	public void setDeleteTimer(final int deleteTimer)
	{
		_deleteTimer = deleteTimer;
	}

	/**
	 * @return the current weight of the L2Player.<BR><BR>
	 */
	public int getCurrentLoad()
	{
		return _inventory.getTotalWeight();
	}

	public long getLastAccess()
	{
		return _lastAccess;
	}

	public void setLastAccess(long value)
	{
		_lastAccess = value;
	}

	public long getLogoutTime()
	{
		return _logoutTime;
	}

	public void setLogoutTime(long value)
	{
		_logoutTime = value;
	}

	public boolean isHourglassEffected()
	{
		return _isHourglassEffected;
	}

	private void setHourlassEffected(boolean val)
	{
		_isHourglassEffected = val;
	}

	public void startHourglassEffect()
	{
		setHourlassEffected(true);
		if(_recommendSystem != null)
		{
			_recommendSystem.stopBonusTask(true);
			_recommendSystem.sendInfo();
		}
	}

	public void stopHourglassEffect()
	{
		setHourlassEffected(false);
		if(_recommendSystem != null)
		{
			_recommendSystem.startBonusTask();
			_recommendSystem.sendInfo();
		}
	}

	/**
	 * @return the Karma of the L2Player.<BR><BR>
	 */
	@Override
	public int getKarma()
	{
		return _karma;
	}

	/**
	 * Set the Karma of the L2Player and send a Server->Client packet StatusUpdate (broadcast).<BR><BR>
	 *
	 * @param karma new value of karma
	 */
	public void setKarma(int karma)
	{
		if(karma < 0)
			karma = 0;

		if(_karma == karma)
			return;

		_karma = karma;

		if(karma > 0)
			for(final L2Character object : getKnownCharacters(2000))
				if(object instanceof L2GuardInstance && object.getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
					object.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null, null);

		sendChanges();

		if(getPet() != null)
			getPet().broadcastPetInfo();
	}

	/**
	 * @return the max weight that the L2Player can load.
	 */
	public int getMaxLoad()
	{
		return Formulas.calcMaxLoad(this);
	}

	public int getWeaponPenalty()
	{
		return weaponPenalty;
	}

	public int getArmorPenalty()
	{
		return armorPenalty;
	}

	public int getWeightPenalty()
	{
		return _curWeightPenalty;
	}

	/**
	 * Update the overloaded status of the L2Player.<BR><BR>
	 */
	public void refreshOverloaded()
	{
		if(_massUpdating || getMaxLoad() <= 0)
			return;

		setOverloaded(getCurrentLoad() > getMaxLoad());

		int currLoad = (int) calcStat(Stats.CURR_LOAD, getCurrentLoad(), this, null);
		if(isPremiumEnabled())
			currLoad -= Config.PREMIUM_WEIGHT_LIMIT;

		final float weightproc = currLoad * 100 / (float) getMaxLoad();
		int newWeightPenalty;

		if(weightproc < 50)
			newWeightPenalty = 0;
		else if(weightproc < 66.6)
			newWeightPenalty = 1;
		else if(weightproc < 80)
			newWeightPenalty = 2;
		else if(weightproc < 100)
			newWeightPenalty = 3;
		else
			newWeightPenalty = 4;

		if(_curWeightPenalty == newWeightPenalty)
			return;

		_curWeightPenalty = newWeightPenalty;
		if(_curWeightPenalty > 0)
			super.addSkill(SkillTable.getInstance().getInfo(4270, _curWeightPenalty));
		else
			super.removeSkill(getKnownSkill(4270));

		sendChanges();
		sendPacket(new EtcStatusUpdate(this));
	}

	public void refreshExpertisePenalty()
	{
		if(_massUpdating)
			return;

		int newWeaponPenalty = 0;
		int newArmorPenalty = 0;
		int gradeModify = (int) calcStat(Stats.GRADE_MODIFY, 0, null, null);

		L2ItemInstance[] items = getInventory().getItems();
		for(L2ItemInstance item : items)
			if(item.isEquipped() && item.getItemType() != L2EtcItem.EtcItemType.ARROW && item.getItemType() != L2EtcItem.EtcItemType.BOLT)
			{
				int crystalType = item.getItem().getCrystalType().ordinal();
				if(item.getItem() instanceof L2Weapon)
				{
					if(crystalType > newWeaponPenalty)
						newWeaponPenalty = crystalType;
				}
				else if(crystalType > expertiseIndex)
					newArmorPenalty++;
			}

		newWeaponPenalty = Math.min(Math.max(newWeaponPenalty - expertiseIndex - gradeModify, 0), 4);
		newArmorPenalty = Math.min(Math.max(newArmorPenalty - gradeModify, 0), 4);

		if(weaponPenalty == newWeaponPenalty && armorPenalty == newArmorPenalty)
			return;

		if(weaponPenalty != newWeaponPenalty)
		{
			weaponPenalty = (short) newWeaponPenalty;
			if(weaponPenalty > 0)
				super.addSkill(SkillTable.getInstance().getInfo(6209, weaponPenalty));
			else
				super.removeSkill(getKnownSkill(6209));
		}

		if(armorPenalty != newArmorPenalty)
		{
			armorPenalty = (short) newArmorPenalty;
			if(armorPenalty > 0)
				super.addSkill(SkillTable.getInstance().getInfo(6213, armorPenalty));
			else
				super.removeSkill(getKnownSkill(6213));
		}

		sendPacket(new EtcStatusUpdate(this));
	}

	/**
	 * @return the the PvP Kills of the L2Player (Number of player killed during a PvP).<BR><BR>
	 */
	public int getPvpKills()
	{
		return _pvpKills;
	}

	/**
	 * Set the the PvP Kills of the L2Player (Number of player killed during a PvP).<BR><BR>
	 *
	 * @param pvpKills new value of pvp kills
	 */
	public void setPvpKills(int pvpKills)
	{
		_pvpKills = pvpKills;
	}

	/**
	 * @return the ClassId object of the L2Player contained in L2PlayerTemplate.<BR><BR>
	 */
	public ClassId getClassId()
	{
		return getTemplate().classId;
	}

	public void addClanPointsOnProfession(final short id)
	{
		L2Clan clan = getClan();
		if(getLvlJoinedAcademy() != 0 && _clanId != 0 && clan.getLevel() >= 5 && ClassId.values()[id].getLevel() == 2)
			clan.incReputation(100, true, "Academy");
		else if(getLvlJoinedAcademy() != 0 && _clanId != 0 && clan.getLevel() >= 5 && ClassId.values()[id].getLevel() > 2)
		{
			int earnedPoints;
			if(getLvlJoinedAcademy() <= 16)
				earnedPoints = 650;
			else if(getLvlJoinedAcademy() >= 39)
				earnedPoints = 190;
			else
				earnedPoints = 650 - (getLvlJoinedAcademy() - 16) * 20;

			clan.removeClanMember(getObjectId());
			SystemMessage sm = new SystemMessage(SystemMessage.CLAN_ACADEMY_MEMBER_S1_HAS_SUCCESSFULLY_COMPLETED_THE_2ND_CLASS_TRANSFER_AND_OBTAINED_S2_CLAN_REPUTATION_POINTS);
			sm.addString(getName());
			sm.addNumber(clan.incReputation(earnedPoints, true, "Academy"));
			clan.broadcastToOnlineMembers(sm);
			clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListDelete(getName()), this);

			setLvlJoinedAcademy(0);
			setClan(null);
			setTitle("");
			sendPacket(Msg.CONGRATULATIONS_YOU_WILL_NOW_GRADUATE_FROM_THE_CLAN_ACADEMY_AND_LEAVE_YOUR_CURRENT_CLAN_AS_A_GRADUATE_OF_THE_ACADEMY_YOU_CAN_IMMEDIATELY_JOIN_A_CLAN_AS_A_REGULAR_MEMBER_WITHOUT_BEING_SUBJECT_TO_ANY_PENALTIES);
			setLeaveClanTime(0);

			broadcastUserInfo(true);

			sendPacket(new PledgeShowMemberListDeleteAll());

			addItem("completeAcademy", 8181, 1, this, true);
			setVar("completeAcademy", "true");
		}
	}

	/**
	 * Set the template of the L2Player.<BR><BR>
	 *
	 * @param id The Identifier of the L2PlayerTemplate to set to the L2Player
	 */
	public void setClassId(final short id, boolean noban)
	{
		if(!noban && !(ClassId.values()[id].equalsOrChildOf(ClassId.values()[getActiveClass()]) || AdminTemplateManager.checkBoolean("changeClass", this)))
		{
			Thread.dumpStack();
			Util.handleIllegalPlayerAction(this, "L2Player[1535]", "tried to change class " + getActiveClass() + " to " + id, 2);
			return;
		}

		//Если новый ID не принадлежит имеющимся классам значит это новая профа
		if(!_classlist.containsKey(id))
		{
			final L2SubClass cclass = _classlist.get(getActiveClass());
			if(cclass == null) // не знаю как это возможно но тикет был
				return;

			_classlist.remove(getActiveClass());
			changeClassInDb(cclass.getClassId(), id);
			if(cclass.isBase())
			{
				setBaseClass(id);
				addClanPointsOnProfession(id);
				if(ClassId.values()[id].getLevel() == 2)
					addItem("ClassMaster", 8869, 15, null, true);
				else if(ClassId.values()[id].getLevel() == 3)
				{
					addItem("ClassMaster", 8870, 15, null, true);
					unsetVar("dd");
					unsetVar("dd1"); // удаляем отметки о выдаче дименшен даймондов
					unsetVar("dd2");
					unsetVar("dd3");
				}
			}

			cclass.setClassId(id);
			_classlist.put(id, cclass);
			setActiveClass(id);
			weaponPenalty = 0;
			armorPenalty = 0;
			refreshExpertisePenalty();
			storeCharSubClasses();

			// Социалка при получении профы
			broadcastPacket(new MagicSkillUse(this, this, 5103, 1, 1000, 0, false));
			//broadcastPacket(new SocialAction(getObjectId(), 16));
			sendPacket(new PlaySound("ItemSound.quest_fanfare_2"));
			broadcastUserInfo(true);
		}

		L2PlayerTemplate t = CharTemplateTable.getInstance().getTemplate(id, getSex() == 1);
		if(t == null)
		{
			_log.warn("Missing template for classId: " + id);
			// do not throw error - only print error
			return;
		}

		// Set the template of the L2Player
		setTemplate(t);

		// Update class icon in party and clan
		if(isInParty())
			getParty().broadcastToPartyMembers(new PartySmallWindowUpdate(this));
		if(getClanId() != 0)
			getClan().broadcastToOnlineMembers(new PledgeShowMemberListUpdate(this));
	}

	public void setClassId(final short id)
	{
		setClassId(id, false);
	}

	/**
	 * @return the Experience of the L2Player.<BR><BR>
	 */
	public long getExp()
	{
		return _exp;
	}

	public void setEnchantScroll(final L2ItemInstance scroll)
	{
		_enchantScroll = scroll;
	}

	public L2ItemInstance getEnchantScroll()
	{
		return _enchantScroll;
	}

	public void setEnchantSupportItem(L2ItemInstance item)
	{
		_enchantSupport = item;
	}

	public L2ItemInstance getEnchantSupportItem()
	{
		return _enchantSupport;
	}

	public void setEnchantStartTime(long time)
	{
		_enchantStartTime = time;
	}

	public long getEnchantStartTime()
	{
		return _enchantStartTime;
	}

	public void addExpAndSp(long addToExp, long addToSp, long bonusExp, long bonusSp, boolean useBonuses)
	{
		if(_exp > Experience.LEVEL[(isSubClassActive() ? Experience.getMaxSubLevel() : Experience.getMaxLevel()) + 1])
			_exp = Experience.LEVEL[(isSubClassActive() ? Experience.getMaxSubLevel() : Experience.getMaxLevel()) + 1];

		if(useBonuses)
		{
			addToExp *= _recommendSystem.getBonusMod();
			bonusExp *= _recommendSystem.getBonusMod();
			if(!isInZonePeace())
			{
				_recommendSystem.setActive(true);
				_huntingBonus.startAdventTask();
			}
		}

		if(addToExp > 0)
		{
			isResivedExp = true;//WTF?

			// Remove Karma when the player kills L2MonsterInstance
			if(!isCursedWeaponEquipped() && addToSp > 0 && _karma > 0)
				_karma -= addToSp / (Config.KARMA_SP_DIVIDER * Config.RATE_SP);

			if(_karma < 0)
				_karma = 0;

			SystemMessage sm;
			if(useBonuses)
			{
				sm = new SystemMessage(SystemMessage.EARNED_S1_B_S2_EXP_AND_S3_B_S4_SP);
				sm.addNumber(addToExp);
				sm.addNumber(bonusExp);
				sm.addNumber(addToSp);
				sm.addNumber(bonusSp);
			}
			else if(addToSp > 0)
			{
				sm = new SystemMessage(SystemMessage.YOU_EARNED_S1_EXP_AND_S2_SP);
				sm.addNumber(addToExp);
				sm.addNumber(addToSp);
			}
			else
			{
				sm = new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1_EXPERIENCE);
				sm.addNumber(addToExp);
			}
			sendPacket(sm);
		}
		else if(addToSp > 0)
			sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_ACQUIRED_S1_SP).addNumber(addToSp));

		if(_sp + addToSp > Integer.MAX_VALUE)
			_sp = Integer.MAX_VALUE;
		else
			_sp += addToSp;

		addExp(addToExp);
	}

	/**
	 * Добавляет чару опыт и/или сп с учетом личного бонуса
	 */
	@Override
	public void addExpAndSp(long addToExp, long addToSp)
	{
		addExpAndSp(addToExp, addToSp, 0, 0, false);
	}

	private void addExp(long addToExp)
	{
		long max_xp = Experience.LEVEL[(isSubClassActive() ? Experience.getMaxSubLevel() : Experience.getMaxLevel()) + 1];
		if(getVarB("NoExp"))
			max_xp = Experience.LEVEL[getLevel() + 1] - 1;

		_exp = _exp + addToExp > max_xp ? max_xp : _exp + addToExp;

		int oldLvl = _level;
		if(_exp >= Experience.LEVEL[_level + 1] && _level < (isSubClassActive() ? Experience.getMaxSubLevel() : Experience.getMaxLevel()))
			increaseLevelAction();

		while(_exp >= Experience.LEVEL[_level + 1] && _level < (isSubClassActive() ? Experience.getMaxSubLevel() : Experience.getMaxLevel()))
			increaseLevel();

		while(_exp < Experience.LEVEL[_level] && _level > 1)
			decreaseLevel();

		int addedLvls = _level - oldLvl;
		if(addedLvls > 0)
			getHuntingBonus().addPoints(500); //TODO: Исправить колличество.

		if(_exp < 0)
			_exp = 0;

		sendChanges();
	}

	public void setExp(long exp)
	{
		_exp = exp;
	}

	/**
	 * Give Expertise skill of this level.<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Get the Level of the L2Player </li>
	 * <li>Add the Expertise skill corresponding to its Expertise level</li>
	 * <li>Update the overloaded status of the L2Player</li><BR><BR>
	 * <p/>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T give other free skills (SP needed = 0)</B></FONT><BR><BR>
	 */
	private void rewardSkills()
	{
		// Calculate the current higher Expertise of the L2Player
		for(short i = 0; i < EXPERTISE_LEVELS.length; i++)
			if(_level >= EXPERTISE_LEVELS[i])
				expertiseIndex = i;

		// Add the Expertise skill corresponding to its Expertise level
		if(expertiseIndex > 0)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(239, expertiseIndex);
			addSkill(skill, false);
		}

		boolean update = false;
		if(Config.AUTO_LEARN_SKILLS)
		{
			Map<Short, L2SkillLearn> availableSkills = SkillTreeTable.getInstance().getMaxEnableLevelsForSkillsAtLevel(this, getClassId());
			for(L2SkillLearn s : availableSkills.values())
			{
				L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
				if(sk == null)
				{
					_log.info("Warning: no skill id: " + s.getId() + " lvl: " + s.getLevel() + " " + this);
					continue;
				}

				if(sk.getCanLearn(getClassId()) && (!_skills.containsKey(sk.getId()) || _skills.get(sk.getId()).getLevel() < sk.getLevel()))
				{
					if(sk.isForgotten() && !Config.AUTO_LEARN_FORGOTTEN)
						continue;
					addSkill(sk, true);
				}
			}
			update = true;
		}
		else
			// Скиллы дающиеся бесплатно не требуют изучения
			for(L2SkillLearn skill : SkillTreeTable.getInstance().getAvailableSkills(this, getClassId()))
				if(skill.getRepCost() == 0 && skill.getSpCost() == 0 && skill.getItemCount() == 0)
				{
					L2Skill sk = SkillTable.getInstance().getInfo(skill.getId(), skill.getLevel());
					if(sk.isForgotten())
						continue;
					addSkill(sk, true);
					if(getAllShortCuts().size() > 0 && sk.getLevel() > 1)
						for(L2ShortCut sc : getAllShortCuts())
							if(sc.id == sk.getId() && sc.type == L2ShortCut.TYPE_SKILL)
							{
								L2ShortCut newsc = new L2ShortCut(sc.slot, sc.page, sc.type, sc.id, sk.getLevel());
								sendPacket(new ShortCutRegister(newsc));
								registerShortCut(newsc);
							}
					update = true;
				}

		if(_transformationId != 0)
		{
			int transform = _transformationId;
			_transformationId = 0;
			setTransformation(transform);
			update = false;
		}

		// remove hi level skills from anyone who's not a GM
		if(!isGM())
		{
			// Собираем информацию классах персонажа
			FastList<ClassId> playerClasses = new FastList<ClassId>();
			playerClasses.add(getClassId());
			ClassId parentClass = getClassId().getParent(getSex());
			while(parentClass != null)
			{
				playerClasses.add(getClassId().getParent(getSex()));
				parentClass = parentClass.getParent(getSex());
			}
			// пробегаемся по классам
			for(ClassId cl : playerClasses)
			{
				// пробегаемся от максимального уровня персонажа до его уровня+9 и выбираем скиллы, которых у него не должно быть
				// или быть другого уровня
				for(int l = isSubClassActive() ? Config.ALT_MAX_SUB_LEVEL : Config.ALT_MAX_LEVEL; l > getLevel() + 9; l--)
				{
					Map<Short, L2SkillLearn> availableSkills = SkillTreeTable.getInstance().getSkillsAtCertainLevel(cl, l);
					for(L2SkillLearn s : availableSkills.values())

						for(int sl = SkillTable.getInstance().getMaxLevel(s.getId(), s.getLevel()); sl >= s.getLevel(); sl--)
						{
							L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), sl);
							if(sk != null && getAllSkills().contains(sk))
							{
								update = true;
								int lvlLower = sk.getLevel();
								lvlLower--;
								if(lvlLower > 0 && SkillTreeTable.getInstance().isSkillPossible(this, sk.getId(), lvlLower))
								{
									L2Skill skLower = SkillTable.getInstance().getInfo(sk.getId(), lvlLower);
									removeSkill(sk, true);
									addSkill(skLower, true);
									if(getAllShortCuts().size() > 0 && skLower.getLevel() > 1)
										for(L2ShortCut sc : getAllShortCuts())
											if(sc.id == sk.getId() && sc.type == L2ShortCut.TYPE_SKILL)
											{
												L2ShortCut newsc = new L2ShortCut(sc.slot, sc.page, sc.type, sc.id, skLower.getLevel());
												sendPacket(new ShortCutRegister(newsc));
												registerShortCut(newsc);
											}

								}
								else
								{
									removeSkill(sk, true);
									if(getAllShortCuts().size() > 0 && sk.getLevel() > 1)
										for(L2ShortCut sc : getAllShortCuts())
											if(sc.id == sk.getId() && sc.type == L2ShortCut.TYPE_SKILL)
												deleteShortCut(sc.slot, sc.page);

								}

							}
						}
				}
			}
		}

		if(update) {
			sendPacket(new SkillList(this));
			sendPacket(new ExAcquirableSkillListByClass(this));
		}

		// This function gets called on login, so not such a bad place to check weight
		// Update the overloaded status of the L2Player
		refreshOverloaded();
		refreshExpertisePenalty();
	}

	/**
	 * @return the Race object of the L2Player.<BR><BR>
	 */
	public Race getRace()
	{
		return getBaseTemplate().race;
	}

	/**
	 * @return the SP amount of the L2Player.<BR><BR>
	 */
	public int getSp()
	{
		return (int) _sp;
	}

	/**
	 * Set the SP amount of the L2Player.<BR><BR>
	 */
	public void setSp(int sp)
	{
		if(sp < 0)
			sp = 0;
		_sp = sp;
	}

	/**
	 * @return the Clan Identifier of the L2Player.<BR><BR>
	 */
	public int getClanId()
	{
		return _clanId;
	}

	/**
	 * @return the Clan Crest Identifier (= Clan Identifier) of the L2Player or 0.<BR><BR>
	 */
	@Override
	public int getClanCrestId()
	{
		if(_clanId != 0)
			return getClan().getCrestId();
		return 0;
	}

	@Override
	public int getClanCrestLargeId()
	{
		if(_clanId != 0)
			return getClan().getCrestLargeId();
		return 0;
	}

	public long getLeaveClanTime()
	{
		return _leaveClanTime;
	}

	public long getDeleteClanTime()
	{
		return _deleteClanTime;
	}

	public void setLeaveClanTime(final long time)
	{
		_leaveClanTime = time;
	}

	public void setDeleteClanTime(final long time)
	{
		_deleteClanTime = time;
	}

	public void setOnlineTime(final long time)
	{
		onlineTime = time;
		onlineBeginTime = System.currentTimeMillis();
	}

	public void setNoChannel(final long time)
	{
		_NoChannel = time;
		if(_NoChannel > 2145909600000L || _NoChannel < 0)
			_NoChannel = -1;

		if(_NoChannel > 0)
			_NoChannelBegin = System.currentTimeMillis();
		else
			_NoChannelBegin = 0;

		sendPacket(new EtcStatusUpdate(this));
	}

	public long getNoChannel()
	{
		return _NoChannel;
	}

	public long getNoChannelRemained()
	{
		if(_NoChannel == 0)
			return 0;
		else if(_NoChannel < 0)
			return -1;
		else
		{
			long remained = _NoChannel - System.currentTimeMillis() + _NoChannelBegin;
			if(remained < 0)
				return 0;

			return remained;
		}
	}

	public void setLeaveClanCurTime()
	{
		_leaveClanTime = System.currentTimeMillis();
	}

	public void setDeleteClanCurTime()
	{
		_deleteClanTime = System.currentTimeMillis();
	}

	public boolean canJoinClan()
	{
		if(_leaveClanTime == 0)
			return true;
		if(System.currentTimeMillis() - _leaveClanTime >= Config.HoursBeforeJoinAClan * 60 * 60 * 1000)
		{
			_leaveClanTime = 0;
			return true;
		}
		return false;
	}

	public boolean canCreateClan()
	{
		if(_deleteClanTime == 0)
			return true;
		if(System.currentTimeMillis() - _deleteClanTime >= Config.DaysBeforeCreateAClan * 24 * 60 * 60 * 1000)
		{
			_deleteClanTime = 0;
			return true;
		}
		return false;
	}

	/**
	 * @return the PcInventory Inventory of the L2Player contained in _inventory.<BR><BR>
	 */
	@Override
	public PcInventory getInventory()
	{
		return _inventory;
	}

	public int getInventoryItemsCount()
	{
		return _inventory.getSize() - _inventory.getQuestItemsCount();
	}

	public int getInventoryQuestItemsCount()
	{
		return _inventory.getQuestItemsCount();
	}

	/**
	 * Delete a item ShortCut of the L2Player _shortCuts.<BR><BR>
	 */
	public void removeItemFromShortCut(final int objectId)
	{
		_shortCuts.deleteShortCutByObjectId(objectId);
	}

	/**
	 * Delete a skill ShortCut of the L2Player _shortCuts.<BR><BR>
	 */
	public void removeSkillFromShortCut(final int skillId)
	{
		_shortCuts.deleteShortCutBySkillId(skillId);
	}

	/**
	 * @return True if the L2Player is sitting.<BR><BR>
	 */
	@Override
	public boolean isSitting()
	{
		return _isSitting;
	}

	public void setSitting(boolean val)
	{
		_isSitting = val;
	}

	public boolean getSittingTask()
	{
		return _sittingTask;
	}

	/**
	 * Sit down the L2Player, set the AI Intention to AI_INTENTION_REST and send a Server->Client ChangeWaitType packet (broadcast)<BR><BR>
	 */
	@Override
	public void sitDown()
	{
		if(isSitting() || _sittingTask || isAlikeDead())
			return;

		if(isActionsBlocked() || isStunned() || isSleeping() || isParalyzed() || isAttackingNow() || isCastingNow() || (isMoving && !isInBoat()))
		{
			getAI().setNextAction(nextAction.REST, null, null, false, false);
			return;
		}

		resetWaitSitTime();
		getAI().setIntention(CtrlIntention.AI_INTENTION_REST, null, null);
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_SITTING));
		_sittingTask = true;
		_isSitting = true;
		ThreadPoolManager.getInstance().scheduleGeneral(new EndSitDown(), 2500);
	}

	@Override
	public void standUp()
	{
		if(_isSitting && !_sittingTask && !isInStoreMode() && !isAlikeDead())
		{
			if(_relax)
			{
				setRelax(false);
				stopEffectsByName("c_rest");
			}

			L2Effect ef = getEffectBySkillId(296);
			if(ef != null)
				ef.exit();

			getAI().clearNextAction();
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STANDING));
			_sittingTask = true;
			_isSitting = true;
			ThreadPoolManager.getInstance().scheduleGeneral(new EndStandUp(), 2500);
		}
	}

	class EndSitDown implements Runnable
	{
		EndSitDown()
		{
		}

		public void run()
		{
			_sittingTask = false;
			getAI().clearNextAction();
		}
	}

	class EndStandUp implements Runnable
	{
		EndStandUp()
		{
		}

		public void run()
		{
			_sittingTask = false;
			_isSitting = false;
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null, null);
			getAI().setNextIntention();
		}
	}

	/**
	 * Set the value of the _relax value. Must be True if using skill Relax and False if not.
	 */
	public void setRelax(final boolean val)
	{
		_relax = val;
	}

	/**
	 * update time counter when L2Player is sitting.
	 */
	public void updateWaitSitTime()
	{
		if(_waitTimeWhenSit < 200)
			_waitTimeWhenSit += 2;
	}

	/**
	 * getting how long L2Player is sitting.
	 */
	public int getWaitSitTime()
	{
		return _waitTimeWhenSit;
	}

	/**
	 * reset time counter
	 */
	public void resetWaitSitTime()
	{
		_waitTimeWhenSit = 0;
	}

	/**
	 * @return the PcWarehouse object of the L2Player.<BR><BR>
	 */
	public Warehouse getWarehouse()
	{
		return _warehouse;
	}

	/**
	 * @return the PcFreight object of the L2Player.<BR><BR>
	 */
	public Warehouse getFreight()
	{
		return _freight;
	}

	/**
	 * @return the Identifier of the L2Player.<BR><BR>
	 */
	public int getCharId()
	{
		return _charId;
	}

	/**
	 * Set the Identifier of the L2Player.<BR><BR>
	 */
	public void setCharId(final int charId)
	{
		_charId = charId;
	}

	/**
	 * @return the Adena amount of the L2Player.<BR><BR>
	 */
	public long getAdena()
	{
		return _inventory.getAdena();
	}

	/**
	 * Забирает адену у игрока.<BR><BR>
	 * <p/>
	 * adena - сколько адены забрать
	 *
	 * @return L2ItemInstance - остаток адены
	 */
	public boolean reduceAdena(String proccess, long count, L2Object reference, boolean sendMessage)
	{
		if(count > getAdena())
		{
			if(sendMessage)
				sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			return false;
		}

		if(count > 0)
		{
			_inventory.reduceAdena(proccess, count, this, reference);
			if(sendMessage)
				sendPacket(new SystemMessage(SystemMessage.S1_ADENA_DISAPPEARED).addNumber(count));
		}
		return true;
	}

	/**
	 * Add adena to Inventory of the L2PcInstance and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 *
	 * @param process	 : String Identifier of process triggering this action
	 * @param count	   : int Quantity of adena to be added
	 * @param reference   : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addAdena(String process, long count, L2Object reference, boolean sendMessage)
	{
		if(sendMessage)
		{
			SystemMessage sm = new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1_ADENA);
			sm.addNumber(count);
			sendPacket(sm);
		}

		if(count > 0)
			_inventory.addAdena(process, count, this, reference);
	}

	/**
	 * @return the active connection with the client.<BR><BR>
	 */
	public GameClient getNetConnection()
	{
		return _connection;
	}

	/**
	 * Set the active connection with the client.<BR><BR>
	 */
	public void setNetConnection(final GameClient connection)
	{
		_connection = connection;
	}

	/**
	 * Close the active connection with the client.<BR><BR>
	 */
	public void closeNetConnection()
	{
		if(_connection != null)
			_connection.closeNow(false);
	}

	/**
	 * Manage actions when a player click on this L2Player.<BR><BR>
	 * <p/>
	 * <B><U> Actions on first click on the L2Player (Select it)</U> :</B><BR><BR>
	 * <li>Set the target of the player</li>
	 * <li>Send a Server->Client packet MyTargetSelected to the player (display the select window)</li><BR><BR>
	 * <p/>
	 * <B><U> Actions on second click on the L2Player (Follow it/Attack it/Intercat with it)</U> :</B><BR><BR>
	 * <li>Send a Server->Client packet MyTargetSelected to the player (display the select window)</li>
	 * <li>If this L2Player has a Private Store, notify the player AI with AI_INTENTION_INTERACT</li>
	 * <li>If this L2Player is autoAttackable, notify the player AI with AI_INTENTION_ATTACK</li><BR><BR>
	 * <li>If this L2Player is NOT autoAttackable, notify the player AI with AI_INTENTION_FOLLOW</li><BR><BR>
	 * <p/>
	 * <B><U> Example of use </U> :</B><BR><BR>
	 * <li> Client packet : Action, AttackRequest</li><BR><BR>
	 *
	 * @param player The player that start an action on this L2Player
	 */
	@Override
	public void onAction(final L2Player player, boolean dontMove)
	{
		if(!dontMove && Events.onAction(player, this))
			return;
		else if(dontMove && Events.onActionShift(player, this))
			return;

		// Check if the L2Player is confused
		if(player.isConfused() || player.isBlocked())
		{
			player.sendActionFailed();
			return;
		}

		if(getDuelState() == Duel.DUELSTATE_DEAD)
		{
			player.sendActionFailed();
			return;
		}

		if(Config.DEBUG)
			_log.info("player.getTarget()=" + player.getTarget() + "; this=" + this);

		// Check if the other player already target this L2Player
		if(player.getTarget() != this)
		{
			// Set the target of the player
			if(player.setTarget(this))
			{

				// The color to display in the select window is White
				player.sendPacket(new MyTargetSelected(getObjectId(), 0));
				sendRelation(player);
			}
		}
		else if(getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE && player != this) // Check if this L2Player has a Private Store
		{
			if(!isInRange(player, getInteractDistance(player)) && getAI().getIntention() != CtrlIntention.AI_INTENTION_INTERACT)
				if(!dontMove && player.isInRange(this, 3000))
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this, null);
				else
					player.sendActionFailed();
			else
				player.doInteract(this);
		}
		else if(isAttackable(player, false, false))
		{
			// Player with lvl < 21 can't attack a cursed weapon holder
			// And a cursed weapon holder	can't attack players with lvl < 21
			if(isCursedWeaponEquipped() && player.getLevel() < 20 || player.isCursedWeaponEquipped() && getLevel() < 20)
				player.sendActionFailed();
			else
				player.getAI().Attack(this, false, dontMove);
		}
		else if(player != this)
		{
			if(!dontMove && player.isInRange(this, 3000))
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, this, 80);
			player.sendActionFailed();
		}
		else
			player.sendActionFailed();
	}

	/**
	 * Send packet StatusUpdate with current HP,MP and CP to the L2Player and only current HP, MP and Level to all other L2Player of the Party.<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send the Server->Client packet StatusUpdate with current HP, MP and CP to this L2Player </li><BR>
	 * <li>Send the Server->Client packet PartySmallWindowUpdate with current HP, MP and Level to all other L2Player of the Party </li><BR><BR>
	 * <p/>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND current HP and MP to all L2Player of the _statusListener</B></FONT><BR><BR>
	 */
	@Override
	public void broadcastStatusUpdate()
	{
		// Send the Server->Client packet StatusUpdate with current HP and MP to all L2Player that must be informed of HP/MP updates of this L2Player
		if(Config.FORCE_STATUSUPDATE)
			super.broadcastStatusUpdate();
		else if(!needStatusUpdate()) //По идее еше должно срезать траффик. Будут глюки с отображением - убрать это условие.
			return;

		// Send the Server->Client packet StatusUpdate with current HP, MP and CP to this L2Player
		final StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.CUR_HP, (int) _currentHp);
		su.addAttribute(StatusUpdate.CUR_MP, (int) _currentMp);
		su.addAttribute(StatusUpdate.CUR_CP, (int) _currentCp);
		sendPacket(su);

		L2Party party = getParty();
		// Check if a party is in progress
		if(party != null && (needCpUpdate(352) || needHpUpdate(352) || needMpUpdate(352)))
			// Send the Server->Client packet PartySmallWindowUpdate with current HP, MP and Level to all other L2Player of the Party
			party.broadcastToPartyMembers(this, new PartySmallWindowUpdate(this));

		if(isInOlympiadMode() && isOlympiadStart())
			broadcastPacket(new ExOlympiadUserInfo(this, getOlympiadSide()));

		if(isInDuel() && (getDuelState() == Duel.DUELSTATE_DUELLING || getDuelState() == Duel.DUELSTATE_DEAD))
		{
			if(getDuelSide() == 1)
				getDuel().broadcastToTeam2(new ExDuelUpdateUserInfo(this));
			else if(getDuelSide() == 2)
				getDuel().broadcastToTeam1(new ExDuelUpdateUserInfo(this));
		}
	}

	public Future<?> _broadcastCharInfoTask = null;
	private BroadcastCharInfoTask _broadcastCharInfo = null;

	/**
	 * Отправляет UserInfo даному игроку и CharInfo всем окружающим.<BR><BR>
	 * <p/>
	 * <B><U> Концепт</U> :</B><BR><BR>
	 * Сервер шлет игроку UserInfo.
	 * Сервер вызывает метод {@link L2Player#broadcastPacketToOthers(ru.l2gw.gameserver.serverpackets.L2GameServerPacket)} для рассылки CharInfo<BR><BR>
	 * <p/>
	 * <B><U> Действия</U> :</B><BR><BR>
	 * <li>Отсылка игроку UserInfo(личные и общие данные)</li>
	 * <li>Отсылка другим игрокам CharInfo(Public data only)</li><BR><BR>
	 * <p/>
	 * <FONT COLOR=#FF0000><B> <U>Внимание</U> : НЕ ПОСЫЛАЙТЕ UserInfo другим игрокам либо CharInfo даному игроку.<BR>
	 * НЕ ВЫЗЫВАЕЙТЕ ЭТОТ МЕТОД КРОМЕ ОСОБЫХ ОБСТОЯТЕЛЬСТВ(смена сабкласса к примеру)!!! Траффик дико кушается у игроков и начинаются лаги.<br>
	 * Используйте метод {@link ru.l2gw.gameserver.model.L2Player#sendChanges()}</B></FONT><BR><BR>
	 *
	 * @see ru.l2gw.gameserver.serverpackets.UserInfo
	 * @see ru.l2gw.gameserver.serverpackets.CharInfo
	 */
	@Override
	public void broadcastUserInfo(boolean force)
	{
		sendUserInfo(force);

		if(isInvisible() || isHide())
			return;

		if(Config.BROADCAST_CHAR_INFO_INTERVAL == 0)
			force = true;

		if(force)
		{
			broadcastCharInfo();
			if(_broadcastCharInfoTask != null)
			{
				_broadcastCharInfoTask.cancel(true);
				_broadcastCharInfoTask = null;
			}
			return;
		}

		if(_broadcastCharInfoTask != null)
			return;

		if(_broadcastCharInfo == null)
			_broadcastCharInfo = new BroadcastCharInfoTask(this);

		_broadcastCharInfoTask = ThreadPoolManager.getInstance().scheduleAi(_broadcastCharInfo, Config.BROADCAST_CHAR_INFO_INTERVAL, true);
	}

	public void broadcastCharInfo()
	{
		if(isInvisible() || isHide())
			return;

		for(L2Player player : L2World.getAroundPlayers(this))
			if(player != null && _objectId != player.getObjectId())
			{
				player.sendPacket(new CharInfo(this));
				player.sendPacket(new ExBrExtraUserInfo(this));
				sendRelation(player);
			}
	}

	public void broadcastRelation()
	{
		if(isInvisible() || isHide())
			return;

		for(L2Player player : L2World.getAroundPlayers(this))
		{
			sendRelation(player);
			player.sendRelation(this);
		}
	}

	public boolean moveInVehicle = false;
	private Location vehicleFrom;
	private Location vehicleTo;
	private Location vehicleCurrent;
	private int vehicleId;
	private MoveInVehicleTask _moveInVehicleTask = null;

	public MoveInVehicleTask getMoveInVehicleTask()
	{
		if(_moveInVehicleTask == null)
			_moveInVehicleTask = new MoveInVehicleTask(this);

		return _moveInVehicleTask;
	}

	public Location getLocInVehicle()
	{
		return vehicleCurrent;
	}

	public Location getLocToVehicle()
	{
		return vehicleTo;
	}

	public void setLocInVehicle(Location loc)
	{
		try
		{
			if(vehicleCurrent == null)
				vehicleCurrent = loc;
			else
				vehicleCurrent.set(loc);
			Location vl = Util.convertVehicleCoordToWorld(_vehicle.getLoc(), loc, _vehicle.isAirShip());
			setXYZ(vl.getX(), vl.getY(), vl.getZ(), true);
		}
		catch(NullPointerException e)
		{
		}
	}

	@Override
	public boolean moveToLocation(int x_dest, int y_dest, int z_dest, int offset, boolean pathfinding)
	{
		if(!moveInVehicle && isInBoat())
			vehicleTo = Util.convertWorldCoordToVehicle(_vehicle.getLoc(), new Location(x_dest, y_dest, z_dest), _vehicle.isAirShip());

		return super.moveToLocation(x_dest, y_dest, z_dest, offset, pathfinding);
	}

	public boolean followInVehicle(L2Character target, int offset)
	{
		if(isMovementDisabled() || isSitting() || !isInAirShip() || !target.isInAirShip())
		{
			sendActionFailed();
			return false;
		}

		if(isFollow && vehicleTo != null && vehicleTo.distance(target.getPlayer().getLocInVehicle()) < 50)
			return true;

		if(isFollow && isInRange(target, offset))
		{
			isFollow = false;
			ThreadPoolManager.getInstance().executeAi(new L2ObjectTasks.NotifyAITask(this, CtrlEvent.EVT_ARRIVED_TARGET, null, null), isPlayable());
			return true;
		}

		vehicleFrom = vehicleCurrent.clone();
		vehicleTo = target.getPlayer().getLocInVehicle();
		moveInVehicle = true;
		vehicleId = _vehicle.getObjectId();
		return followToCharacter(target, offset);
	}

	public void moveInVehicle(Location destination, Location origin, L2Vehicle vehicle)
	{
		if(isMovementDisabled() || isSitting())
		{
			sendActionFailed();
			return;
		}

		vehicleCurrent = origin.clone();
		vehicleFrom = origin;
		vehicleTo = destination;
		moveInVehicle = true;
		vehicleId = vehicle.getObjectId();
		moveToLocation(Util.convertVehicleCoordToWorld(vehicle.getLoc(), destination, isAirShip()), 0, false);
	}

	public boolean isMoveInVehicle()
	{
		return moveInVehicle;
	}

	@Override
	public void broadcastMove()
	{
		if(moveInVehicle)
		{
			L2Vehicle vehicle = VehicleManager.getInstance().getVehicleByObjectId(vehicleId);
			if(vehicle == null)
				return;
			if(vehicle.isAirShip())
			{
				if(isFollow)
				{
					if(!isPawn)
					{
						isPawn = true;
						broadcastPacket(new ExMoveToTargetInAirShip(this, getFollowTarget().getObjectId(), _offset, vehicle.getObjectId()));
					}
				}
				else
					broadcastPacket(new ExMoveToLocationInAirShip(this, vehicleId, vehicleTo, vehicleFrom));
			}
			else
				broadcastPacket(new MoveToLocationInVehicle(this, vehicleId, vehicleTo, vehicleFrom));
			return;
		}

		if(!isFollow || _targetRecorder.size() > 0)
		{
			if(isMovePacketNeeded())
			{
				prevDestination = destination;
				if(destination != null)
					broadcastPacket(new CharMoveToLocation(this, destination));
			}
			else
				sendActionFailed();
		}
		else
		{
			L2Character target = getFollowTarget();
			if(target == null)
			{
				stopMove();
				return;
			}

			if(!isPawn && (!isInRange(getDestination(), 150) || target.isInRange(getDestination(), _offset + 16)))
			{
				isPawn = true;
				//_log.info(this + " broadcastMove: MoveToPawn");
				broadcastPacket(new MoveToPawn(this, target, _offset));
			}
			else if(isInRange(getDestination(), 150) && !target.isInRange(getDestination(), _offset + 16))
			{
				//_log.info(this + " broadcastMove: CharMoveToLocation 2");
				isPawn = false;
				if(isMovePacketNeeded())
				{
					prevDestination = destination;
					broadcastPacket(new CharMoveToLocation(this, destination));
				}
				else
					sendActionFailed();
			}
		}
		//if(!isMovePacketNeeded())
		//	sendActionFailed();
	}

	@Override
	public boolean isInRange(L2Object obj, int range)
	{
		if(obj == null)
			return false;

		if(getReflection() != obj.getReflection())
			return false;

		if(isInAirShip() && obj.isPlayer() && obj.getPlayer().isInAirShip())
		{
			L2Player target = obj.getPlayer();
			long dx = Math.abs(target.getLocInVehicle().getX() - getLocInVehicle().getX());
			if(dx > range)
				return false;
			long dy = Math.abs(target.getLocInVehicle().getY() - getLocInVehicle().getY());
			if(dy > range)
				return false;
			long dz = Math.abs(target.getLocInVehicle().getZ() - getLocInVehicle().getZ());
			return dz <= 1500 && dx * dx + dy * dy <= range * range;
		}

		return super.isInRange(obj, range);
	}

	@Override
	public double getDistance(L2Object obj)
	{
		if(obj == null)
			return 0;
		if(isInAirShip() && obj.isPlayer() && obj.getPlayer().isInAirShip())
		{
			L2Player target = obj.getPlayer();
			double dx = target.getLocInVehicle().getX() - getLocInVehicle().getX();
			double dy = target.getLocInVehicle().getY() - getLocInVehicle().getY();
			return Math.sqrt(dx * dx + dy * dy);
		}

		return super.getDistance(obj);
	}

	@Override
	public int getMoveTickInterval()
	{
		return (int) (16000 / getMoveSpeed());
	}

	@Override
	public void stopMove()
	{
		prevDestination = null;
		if(isMoving)
		{
			isMoving = false;
			setXYZ(getX(), getY(), isInBoat() || isFloating() || moveInVehicle ? getZ() : GeoEngine.getHeight(getX(), getY(), getZ(), getReflection()), false);

			if(_vehicle != null)
				broadcastPacket(_vehicle.isAirShip() ? new ExStopMoveInAirShip(this, _vehicle) : new StopMoveToLocationInVehicle(this, _vehicle));
			else
				broadcastPacket(new StopMove(this));
		}

		if(isFollow)
		{
			isFollow = false;
			isPawn = false;
		}
	}

	/**
	 * @param player of broadcast packet
	 *               info = this copy of instance
	 */
	public void sendRelation(L2Player player)
	{
		if(player == this)
			return;

		Integer relation = getRelation(player);
		Integer knowRelation = player.getKnownRelations().get(getObjectId());
		if(knowRelation == null || !knowRelation.equals(relation))
		{
			player.sendPacket(new RelationChanged(this, relation));
			player.getKnownRelations().put(getObjectId(), relation);
			if(getSiegeState() == 3 && player.getSiegeState() == 3)
				player.sendPacket(new ExDominionWarStart(this));
		}
	}

	/**
	 * @return the Alliance Identifier of the L2Player.<BR><BR>
	 */
	public int getAllyId()
	{
		return _clanId <= 0 ? 0 : getClan().getAllyId();
	}

	@Override
	public int getAllyCrestId()
	{
		return getAllyId() <= 0 ? 0 : getAlliance().getAllyCrestId();
	}

	@Override
	public void onForcedAttack(L2Player player, boolean dontMove)
	{
		if(!AdminTemplateManager.checkBoolean("peaceAttack", player) && (player.isInZonePeace() || isInZonePeace()))
		{
			player.sendPacket(Msg.YOU_CANNOT_ATTACK_THE_TARGET_IN_THE_PEACE_ZONE);
			player.sendActionFailed();
			return;
		}

		super.onForcedAttack(player, dontMove);
	}

	/**
	 * Send a Server->Client packet StatusUpdate to the L2Player.<BR><BR>
	 */
	@Override
	public void sendPacket(final L2GameServerPacket packet)
	{
		if(_isConnected)
			try
			{
				if(_connection != null)
					_connection.sendPacket(packet);
				//_connection.sendPacket(SystemMessage.sendString(packet.getType()));
			}
			catch(final Exception e)
			{
				_log.warn("", e);
				e.printStackTrace();
			}
	}

	/**
	 * Manage Interact Task with another L2Player.<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>If the private store is a STORE_PRIVATE_SELL, send a Server->Client PrivateBuyListSell packet to the L2Player</li>
	 * <li>If the private store is a STORE_PRIVATE_BUY, send a Server->Client PrivateBuyListBuy packet to the L2Player</li>
	 * <li>If the private store is a STORE_PRIVATE_MANUFACTURE, send a Server->Client RecipeShopSellList packet to the L2Player</li><BR><BR>
	 *
	 * @param target The L2Character targeted
	 */
	public void doInteract(final L2Object target)
	{
		if(target == null)
			return;
		if(target.isPlayer())
		{
			if(isInRange(target, getInteractDistance(target)))
			{
				final L2Player temp = (L2Player) target;

				if(temp.getPrivateStoreType() == STORE_PRIVATE_SELL || temp.getPrivateStoreType() == STORE_PRIVATE_SELL_PACKAGE)
				{
					sendPacket(new PrivateStoreListSell(this, temp));
					sendActionFailed();
				}
				else if(temp.getPrivateStoreType() == STORE_PRIVATE_BUY)
				{

					if(temp.getTradeList() == null)
					{
						_log.warn("PrivateStoreListBuy: " + temp + " has no trade list! seller " + this);
						sendActionFailed();
						temp.setPrivateStoreType(STORE_PRIVATE_NONE);
						return;
					}
					if(temp.getBuyList() == null)
					{
						_log.warn("PrivateStoreListBuy: " + temp + " has no buy list! seller " + this);
						sendActionFailed();
						temp.setPrivateStoreType(STORE_PRIVATE_NONE);
						return;
					}

					sendPacket(new PrivateStoreListBuy(this, temp));
					sendActionFailed();
				}
				else if(temp.getPrivateStoreType() == STORE_PRIVATE_MANUFACTURE)
				{
					sendPacket(new RecipeShopSellList(this, temp));
					sendActionFailed();
				}
				sendActionFailed();
			}
			else if(getAI().getIntention() != CtrlIntention.AI_INTENTION_INTERACT)
				getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this, null);
		}
		else
			target.onAction(this, false);
	}

	/**
	 * Manage AutoLoot Task.<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send a System Message to the L2Player : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li>
	 * <li>Add the Item to the L2Player inventory</li>
	 * <li>Send a Server->Client packet StatusUpdate to this L2Player with current weight</li><BR><BR>
	 * <p/>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress, distribute Items between party members</B></FONT><BR><BR>
	 *
	 * @param item	The L2ItemInstance dropped
	 * @param fromNpc L2NpcIntance that dropped
	 */
	public void doAutoLoot(L2ItemInstance item, L2NpcInstance fromNpc)
	{
		// Herbs
		if(item.isHerb())
		{
			altUseSkill(item.getItem().getFirstSkill(), this, item);
			item.decayMe();
			L2World.removeObject(item);
			return;
		}

		// Check if the L2Player is in a Party
		if(!isInParty() || item.isFortFlag())
		{
			if(!getInventory().validateWeight(item))
			{
				sendActionFailed();
				sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
				item.dropToTheGround(this, fromNpc);
				return;
			}

			if(!getInventory().validateCapacity(item))
			{
				sendActionFailed();
				sendPacket(Msg.YOUR_INVENTORY_IS_FULL);
				item.dropToTheGround(this, fromNpc);
				return;
			}

			// Send a System Message to the L2Player
			if(item.getItemId() == 57) {
				addAdena("Loot", item.getCount(), fromNpc, true);
				sendPacket(new ExAdenaInvenCount(this));
			}
			else
				addItem("Loot", item.getItemId(), item.getCount(), fromNpc, true);

			sendChanges();
		}
		else
			// Distribute Item between Party members
			getParty().distributeItem(this, item, fromNpc);

		broadcastPickUpMsg(item);
	}

	/**
	 * Manage Pickup Task.<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send a Server->Client packet StopMove to this L2Player </li>
	 * <li>Remove the L2ItemInstance from the world and send server->client GetItem packets </li>
	 * <li>Send a System Message to the L2Player : YOU_PICKED_UP_S1_ADENA or YOU_PICKED_UP_S1_S2</li>
	 * <li>Add the Item to the L2Player inventory</li>
	 * <li>Send a Server->Client packet StatusUpdate to this L2Player with current weight</li><BR><BR>
	 * <p/>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If a Party is in progress, distribute Items between party members</B></FONT><BR><BR>
	 *
	 * @param object The L2ItemInstance to pick up
	 */
	@Override
	public void doPickupItem(final L2Object object)
	{
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null, null);

		// Check if the L2Object to pick up is a L2ItemInstance
		if(!(object instanceof L2ItemInstance))
		{
			// dont try to pickup anything that is not an item :)
			_log.warn("trying to pickup wrong target." + getTarget());
			return;
		}

		final L2ItemInstance item = (L2ItemInstance) object;

		sendActionFailed();

		if(Config.DEBUG)
			_log.debug("pickup _geoPos: " + item.getX() + " " + item.getY() + " " + item.getZ());

		if(isTradeInProgress())
		{
			sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_PICK_UP_OR_USE_ITEMS_WHILE_TRADING));
			cancelActiveTrade();
			return;
		}

		synchronized(item)
		{
			// Check if me not owner of item and, if in party, not in owner party and nonowner pickup delay still active
			if(!item.isCanBePickuped(this) || isCombatFlagEquipped() && CursedWeaponsManager.getInstance().isCursed(item.getItemId()))
			{
				SystemMessage sm;
				if(item.getItemId() == 57)
				{
					sm = new SystemMessage(SystemMessage.FAILED_TO_PICK_UP_S1_ADENA);
					sendPacket(new ExAdenaInvenCount(this));
					sm.addNumber(item.getCount());
				}
				else
				{
					sm = new SystemMessage(SystemMessage.FAILED_TO_PICK_UP_S1);
					sm.addItemName(item.getItemId());
				}
				sendPacket(sm);
				sendActionFailed();
				return;
			}

			if(!item.isVisible())
			{
				sendActionFailed();
				return;
			}

			if(item.isFortFlag() && !FortressSiegeManager.getInstance().checkIfCanPickup(this))
			{
				sendPacket(new SystemMessage(SystemMessage.FAILED_TO_PICK_UP_S1).addItemName(item.getItemId()));
				return;
			}

			// Herbs
			if(item.isHerb())
			{
				altUseSkill(item.getItem().getFirstSkill(), this, item);
				if(getPet() != null && !getPet().isDead() && (item.getItemId() <= 8605 || item.getItemId() == 8614))
					getPet().altUseSkill(item.getItem().getFirstSkill(), getPet(), item);
				item.decayMe();
				L2World.removeObject(item);
				broadcastPacket(new GetItem(item, getObjectId()));
				return;
			}

			if(!isInParty() || item.isFortFlag())
			{
				if(!_inventory.validateWeight(item))
				{
					sendActionFailed();
					sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
					return;
				}

				if(!_inventory.validateCapacity(item))
				{
					sendActionFailed();
					sendPacket(Msg.YOUR_INVENTORY_IS_FULL);
					return;
				}

				item.pickupMe(this);
				addItem("Pickup", item, null, true);

				sendChanges();
			}
			else
			{
				// Нужно обязательно сначало удалить предмет с земли.
				item.pickupMe(this);
				getParty().distributeItem(this, item);
			}

			broadcastPickUpMsg(item);
		}
	}

	public long blockTargetTime;

	/**
	 * Set a target.<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Remove the L2Player from the _statusListener of the old target if it was a L2Character </li>
	 * <li>Add the L2Player to the _statusListener of the new target if it's a L2Character </li>
	 * <li>Target the new L2Object (add the target to the L2Player _target, _knownObject and L2Player to _KnownObject of the L2Object)</li><BR><BR>
	 *
	 * @param newTarget The L2Object to target
	 */
	@Override
	public boolean setTarget(L2Object newTarget)
	{
		if(isStatActive(Stats.BLOCK_TARGET) || blockTargetTime > System.currentTimeMillis() || newTarget instanceof L2Player && ((L2Player) newTarget).isHide() && newTarget != this)
		{
			sendActionFailed();
			return false;
		}

		if(getTarget() instanceof L2AirShipHelm)
		{
			L2AirShipHelm helm = (L2AirShipHelm) getTarget();
			if(helm.getClanAirship().getCaptainObjectId() == getObjectId())
			{
				if(newTarget != null)
					sendPacket(Msg.THIS_ACTION_IS_PROHIBITED_WHILE_CONTROLLING);
				return false;
			}
		}

		// Check if the new target is visible
		if(newTarget != null && !newTarget.isVisible() && newTarget != this)
			newTarget = null;

		// Get the current target
		final L2Object oldTarget = getTarget();
		if(oldTarget != null)
		{
			if(oldTarget.equals(newTarget))
				return false; // no target change

			// Remove the L2Player from the _statusListener of the old target if it was a L2Character
			if(oldTarget.isCharacter())
				((L2Character) oldTarget).removeStatusListener(this);
		}

		// Add the L2Player to the _statusListener of the new target if it's a L2Character
		if(newTarget != null && newTarget.isCharacter()) {
			((L2Character) newTarget).addStatusListener(this);
			//sendPacket(new ExAbnormalStatusUpdateFromTarget((L2Player) newTarget)); 
		}
			

		// Target the new L2Object (add the target to the L2Player _target, _knownObject and L2Player to _KnownObject of the L2Object)
		return super.setTarget(newTarget);
	}

	/**
	 * @return the active weapon instance (always equipped in the right hand).<BR><BR>
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		try
		{
			return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);
		}
		catch(Exception NPE)
		{
			//TODO: сделать нормальную обрабоку чтобы никогда не возвращало null например сделать пустой итем в базе
			return null;
		}

	}

	/**
	 * @return the active weapon item (always equipped in the right hand).<BR><BR>
	 */
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		final L2ItemInstance weapon = getActiveWeaponInstance();

		if(weapon != null)
			return (L2Weapon) weapon.getItem();

		return null;
	}

	/**
	 * @return the secondary weapon instance (always equipped in the left hand).<BR><BR>
	 */
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
	}

	/**
	 * @return the secondary weapon item (always equipped in the left hand) or the fists weapon.<BR><BR>
	 */
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		final L2ItemInstance weapon = getSecondaryWeaponInstance();

		if(weapon != null)
		{
			final L2Item item = weapon.getItem();

			if(item instanceof L2Weapon)
				return (L2Weapon) item;
		}
		return null;
	}

	public boolean isWearingArmor(final ArmorType armorType)
	{
		final L2ItemInstance lhand = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		final L2ItemInstance chest = getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);

		if(armorType == ArmorType.SIGIL && lhand != null && lhand.getItemType() == armorType)
			return true;

		if(chest == null)
			return armorType == ArmorType.NONE;

		if(chest.getItemType() != armorType)
			return false;

		if(chest.getBodyPart() == L2Item.SLOT_FULL_ARMOR)
			return true;

		final L2ItemInstance legs = getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS);

		return legs == null ? armorType == ArmorType.NONE : legs.getItemType() == armorType;
	}

	public void reduceHp(double damage, L2Character attacker, boolean directHp, boolean reflect)
	{
		if(damage < 1)
			return;

		if(isDead() || isInvul())
			return;

		if(isInDuel())
		{
			if(attacker instanceof L2Playable || attacker instanceof L2CubicInstance)
			{
				if(getDuelState() == Duel.DUELSTATE_DEAD || getDuelState() == Duel.DUELSTATE_WINNER)
					return;

				if(attacker.getDuel() != getDuel())
					setDuelState(Duel.DUELSTATE_INTERRUPTED);
			}
			else
				setDuelState(Duel.DUELSTATE_INTERRUPTED);
		}

		if(attacker instanceof L2Playable && isInZoneBattle() != attacker.isInZoneBattle())
		{
			L2Player pl = attacker.getPlayer();
			if(pl != null)
				pl.sendPacket(Msg.INVALID_TARGET);
			return;
		}

		double trans = Math.min(100, calcStat(Stats.TRANSFER_DAMAGE_PERCENT, 0, null, null));
		boolean summonDmg = false;
		if(trans > 1 && attacker != this && _summon != null && !_summon.isDead() && _summon.isInRange(this, 1000) && _summon.isSummon() && _summon.isInZonePeace() == isInZonePeace() && _summon.getEffectBySkillId(L2Skill.SKILL_RAID_CURSE) == null)
		{
			trans *= damage / 100;
			if(_summon.getCurrentHp() > trans)
			{
				damage -= trans;
				summonDmg = true;
			}
		}

		double tankDamage = 0;
		L2Player tank = null;
		if(isInParty() && attacker != this && isStatActive(Stats.TRANSFER_DAMAGE_TO_TANK))
		{
			for(L2Player player : getParty().getPartyMembers())
			{
				if(!player.isInRange(this, 1000) || player.isDead())
					continue;
				tankDamage = player.calcStat(Stats.TANK_ABSORBER_DAMAGE, 0, null, null);
				if(tankDamage > 0)
				{
					tank = player;
					break;
				}
			}

			if(tankDamage > 0 && tank != this)
			{
				tankDamage *= damage / 100;
				double hp = tank.getCurrentHp();
				if(attacker.isPlayable() && !directHp)
					hp += tank.getCurrentCp();
				if(hp > tankDamage)
					damage -= tankDamage;
				else
					tankDamage = 0;
			}
			else
				tankDamage = 0;
		}

		if(attacker != null && attacker != this)
			sendPacket(new SystemMessage(SystemMessage.S1_HAS_RECEIVED_DAMAGE_OF_S3_FROM_S2).addCharName(this).addCharName(attacker).addNumber((int) damage));

		for(L2CubicInstance cubic : _cubics)
			if(cubic != null)
				cubic.addAggro(attacker, Math.min((int) (damage / 2), 1));

		// Reduce the current HP of the L2Player
		super.reduceHp(damage, attacker, directHp, reflect);

		if(summonDmg && _summon != null && !_summon.isDead() && !_summon.isStatActive(Stats.BLOCK_HP))
			_summon.reduceHp(trans, attacker, directHp, false);

		if(tankDamage > 0 && tank != null)
			tank.reduceHp(tankDamage, attacker, directHp, false);

		if(getLevel() < 6 && getCurrentHp() < getMaxHp() / 4)
		{
			Quest q = QuestManager.getQuest(255);
			if(q != null)
			{
				QuestState qs = getQuestState(q.getName());
				if(qs != null && (qs.getInt("t") & 0x100) == 0x100)
					processQuestEvent(q.getName(), "TE" + 0x100);
			}
		}
	}

	public void decreaseHp(double damage, L2Character attacker, boolean directHp, boolean reflect)
	{
		if(damage < 1)
			return;

		if(isInDuel())
		{
			if(getDuelState() == Duel.DUELSTATE_DEAD || getDuelState() == Duel.DUELSTATE_WINNER)
				return;

			if(!(attacker instanceof L2Playable) && !(attacker instanceof L2CubicInstance) || attacker.getDuel() != getDuel())
				setDuelState(Duel.DUELSTATE_INTERRUPTED);
		}
/*
		if(damage > 50000)
		{
			_log.warn(attacker + " did damage: " + damage + " to " + this);
			Thread.dumpStack();
			_log.warn("attacker effects: ");
			for(L2Effect e : attacker.getAllEffects())
				_log.warn(e.toString());
		}
*/
		if(attacker != this)
			standUp();

		super.decreaseHp(damage, attacker, directHp, reflect);

		if(isOlympiadStart())
		{
			L2Player olyAttacker = null;
			if(attacker.isPlayer())
				olyAttacker = (L2Player) attacker;
			else if(attacker.isSummon())
				olyAttacker = attacker.getPlayer();

			if(olyAttacker != null && olyAttacker != this && olyAttacker.isOlympiadStart() && isOlympiadStart() && olyAttacker.getOlympiadGameId() == getOlympiadGameId())
				Olympiad.addReceivedDamage(getOlympiadGameId(), getObjectId(), (int) damage);
		}

		if(isInOlympiadMode() && getCurrentHp() < 0.5)
			finishOlympGame();
	}

	private void altDeathPenalty(final L2Character killer)
	{
		// Reduce the Experience of the L2Player in function of the calculated Death Penalty
		if(!Config.ALT_GAME_DELEVEL || isInZoneBattle() && killer instanceof L2Playable || _huntingBonus.isBlessingActive())
			return;
		if(killer instanceof L2Playable)
			deathPenalty(isAtWarWith(killer.getClanId()) > 0, killer);
		else
			deathPenalty(false, killer);
	}

	public boolean atMutualWarWith(L2Player player, final L2Clan myClan, final L2Clan otherClan)
	{
		return player.getClanId() != 0 && getClanId() != 0 && getPledgeType() != -1 && player.getPledgeType() != -1 && myClan.isAtWarWith(player.getClanId()) && otherClan.isAtWarWith(getClanId());
	}

	public final void doPurePk(final L2Player killer)
	{
		// Check if the attacker has a PK counter greater than 0
		final int pkCountMulti = Math.max(killer.getPkKills() / 2, 1);

		// Calculate the level difference Multiplier between attacker and killed L2Player
		//final int lvlDiffMulti = Math.max(killer.getLevel() / _level, 1);

		// Calculate the new Karma of the attacker : newKarma = baseKarma*pkCountMulti*lvlDiffMulti
		// Add karma to attacker and increase its PK counter
		killer.increaseKarma(Config.KARMA_MIN_KARMA * pkCountMulti); // * lvlDiffMulti);
		killer.setPkKills(killer.getPkKills() + 1);
	}

	public final void doKillInPeace(final L2Player killer)
	{ // Check if the L2Player killed haven't Karma
		if(_karma <= 0)
		{
			doPurePk(killer);
			if(killer.isPetSummoned())
				killer.getPet().broadcastUserInfo();
		}
		else
			// Dead player have Karma
			// Increase the PvP Kills counter (Number of player killed during a PvP)
			killer.setPvpKills(killer.getPvpKills() + 1);
	}

	public static ArrayList<L2ItemInstance> checkAddItemToDrop(final ArrayList<L2ItemInstance> array, final L2ItemInstance item, final double rate)
	{
		//		_log.info("checkAddItemToDrop("+array+", "+item+", "+rate+");");

		if(Rnd.get() >= rate)
			return array;

		if(Config.KARMA_LIST_NONDROPPABLE_ITEMS.contains(item.getItemId()))
			return array;

		// проверка на шабоу пухи квест итемы и тд выше,тут учитываем флаг
		// если итем не может быть выкинут но он пуха или армор не шадоу не ноудропПК неаугмент то упадёт
		// если итем не может быть выкинут и не пуха или арм то неупадёт
		// если итем может быть выкинут то полюбому упадёт
		if(!item.getItem().isDropable())
		{
			if(!(item.getItem().getType2() == L2Item.TYPE2_WEAPON) && !(item.getItem().getType2() == L2Item.TYPE2_SHIELD_ARMOR))
				return array;
		}

		array.add(item);

		return array;
	}

	/**
	 * Pk/PvP situation checking and managing after death<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B>
	 * <li>Manage Karma gain for attacker and Karma loss for the killed L2Player </li>
	 * <li>If the killed L2Player has Karma, manage Drop Item</li><BR>
	 *
	 * @param killer The L2Character who attacks
	 */
	protected void doPKPVPManage(L2Character killer)
	{
		if(killer == null || killer == _summon || (killer instanceof L2Summon && killer.getPlayer() == null))
			return;

		if(killer.getObjectId() == _objectId)
			return;

		if(isInZoneBattle() || killer.isInZoneBattle())
			return;

		if(killer instanceof L2Summon)
			killer = killer.getPlayer();

		// Processing Karma/PKCount/PvPCount for killer
		if(killer.isPlayer())
		{
			final L2Player pk = (L2Player) killer;

			if(isInDuel() && pk.isInDuel())
				return;

			if(Config.ALT_ANNOUNCE_PVP)
				try
				{
					for(L2Player player : L2ObjectsStorage.getAllPlayers())
						if(player.getVarB("pvpan"))
							player.sendMessage(new CustomMessage("common.altAnnouncePvP", player).addString(pk.getName()).addString(getName()).addString(TownManager.getInstance().getClosestTownName(this)));
				}
				catch(Exception e)
				{
				}

			final int repValue = getLevel() - pk.getLevel() >= 20 ? 2 : 1;
			L2Clan clan = getClan();
			L2Clan otherClan = pk.getClan();
			boolean war = atMutualWarWith(pk, clan, otherClan);

			boolean siegeWar = clan != null && otherClan != null && clan.getSiege() != null && clan.getSiege() == otherClan.getSiege() && (clan.isDefender() && otherClan.isAttacker() || clan.isAttacker() && otherClan.isDefender());
			boolean territoryWar = TerritoryWarManager.getWar().isInProgress() && getTerritoryId() > 0 && pk.getTerritoryId() > 0 && getTerritoryId() != pk.getTerritoryId();
			int fame = Math.min(Math.max(15 - (getLevel() - killer.getLevel()), 2), 20);
			if(getLevel() > 4 && _clanId > 0 && pk.getClanId() > 0)
				if(war || siegeWar)
				{
					if(siegeWar)
					{
						clan.incSiegeDeath();
						otherClan.incSiegeKills();
						if(getLevel() > 60 && isInSiege() && killer.isInSiege())
						{
							if(pk.isInParty())
							{
								for(L2Player member : pk.getParty().getPartyMembers())
									if(member.isInRange(pk, Config.ALT_PARTY_DISTRIBUTION_RANGE) && member.isInSiege() && member.getSiegeId() == pk.getSiegeId() && !getVarB("fp-" + member.getObjectId()))
									{
										member.addFame(fame);
										setVar("fp-" + member.getObjectId(), "true", Util.getCurrentTime() + Rnd.get(Config.FAME_DROP_PENALTY_MIN, Config.FAME_DROP_PENALTY_MAX));
									}
							}
							else if(getVarB("fp-" + pk.getObjectId()))
							{
								pk.addFame(fame);
								setVar("fp-" + pk.getObjectId(), "true", Util.getCurrentTime() + Rnd.get(Config.FAME_DROP_PENALTY_MIN, Config.FAME_DROP_PENALTY_MAX));
							}
						}
					}
					if(otherClan.getReputationScore() > 0 && clan.getLevel() >= 5 && clan.getReputationScore() > 0 && otherClan.getLevel() >= 5)
					{
						clan.incReputation(-repValue, true, "ClanWar");
						otherClan.incReputation(repValue, true, "ClanWar");
					}
				}

			if(territoryWar && getLevel() > 60 && isInSiege() && killer.isInSiege())
			{
				if(pk.isInParty())
				{
					for(L2Player member : pk.getParty().getPartyMembers())
						if(member.isInRange(pk, Config.ALT_PARTY_DISTRIBUTION_RANGE) && member.isInSiege() && member.getTerritoryId() == pk.getTerritoryId())
							member.addFame(fame);
				}
				else
					pk.addFame(fame);
			}

			if(isInSiege() || siegeWar || territoryWar)
				return;

			if(_pvpFlag > 0 || war)
			{
				if(_karma == 0)
					pk.setPvpKills(pk.getPvpKills() + 1);
			}
			else
				doKillInPeace(pk);

			// Send a Server->Client UserInfo packet to attacker with its PvP Kills Counter
			pk.sendUserInfo(false);
		}

		// KARMA LOSS FOR DEAD PLAYER
		// when a PKer gets killed by another player or a L2MonsterInstance, it loseq a certain amount of Karma based on their level.
		// this (with defaults) results in a level 1 losing about ~2 karma per death, and a lvl 70 loses about 11760 karma per death...
		// You lose karma as long as you were not in a pvp zone and you did not kill urself.
		// No item drop in PvP without Karma and for players with less than 5 PK kills

		int karma = _karma;
		double karmaLost = Config.KARMA_LOST_BASE;
		karmaLost *= getLevel(); // multiply by char lvl
		karmaLost *= (getLevel() / 100.0); // divide by 0.charLVL
		karmaLost = Math.round(karmaLost);
		if(karmaLost < 0)
			karmaLost = 1;
		decreaseKarma((int) karmaLost);

		if(_pkKills < Config.MIN_PK_TO_ITEMS_DROP || karma == 0 && Config.KARMA_NEEDED_TO_DROP)
			return;

		// No drop from GM's
		if(!Config.KARMA_DROP_GM && isGM())
			return;

		//Donate nonDrop WarLegend FlareDrakon
		if(isNonDrop())
			return;

		final int max_drop_count = Config.KARMA_DROP_ITEM_LIMIT;

		double dropRate = (double) _pkKills * Config.KARMA_DROPCHANCE_MULTIPLIER;

		if(dropRate < Config.KARMA_DROPCHANCE_MINIMUM)
			dropRate = Config.KARMA_DROPCHANCE_MINIMUM;

		dropRate /= 100.0;

		final double dropEquipRate = Config.KARMA_DROPCHANCE_EQUIPMENT / 100.0;
		final double dropWeaponRate = Config.KARMA_DROPCHANCE_EQUIPPED_WEAPON / 100.0;
		final double dropItemRate = Math.max(0, 1.0 - (dropEquipRate + dropWeaponRate));

		// _log.info("dropEquipRate=" + dropEquipRate + "; dropWeaponRate=" + dropWeaponRate + "; dropItemRate=" + dropItemRate);

		ArrayList<L2ItemInstance> dropped_items = new ArrayList<L2ItemInstance>();
		// Items to check = max_drop_count * 3; (Inventory + Weapon + Equipment)
		if(_inventory.getSize() > 0)
		{
			L2ItemInstance[] items = _inventory.getItems();
			for(int i = 0, cycles = 100; i < max_drop_count * 3 && cycles > 0 && dropped_items.size() < max_drop_count; cycles--)
			{
				final L2ItemInstance random_item = items[Rnd.get(items.length)];
				//не дропаем переточеные вещи
				if(random_item.getEnchantLevel() >= Config.KARMA_DROPCHANCE_MAXENCHANT)
					continue;
				//не дропаем инкрустированые, теневые, и геройские вещи
				//не дропаем инкрустированые, теневые, и геройские,донские и тд вещи
				if(random_item.isNoDropPK() || !random_item.canBeTraded(this))
					continue;

				if(dropped_items.contains(random_item))
					continue;

				i++;

				if(random_item.isEquipped())
				{
					if(random_item.getItem().getType2() == L2Item.TYPE2_WEAPON)
						dropped_items = checkAddItemToDrop(dropped_items, random_item, dropWeaponRate * dropRate);
					else
						dropped_items = checkAddItemToDrop(dropped_items, random_item, dropEquipRate * dropRate);
				}
				else
					dropped_items = checkAddItemToDrop(dropped_items, random_item, dropItemRate * dropRate);
			}
		}
		// Dropping items, if present
		if(dropped_items.isEmpty())
			return;
		for(L2ItemInstance item : dropped_items)
		{
			final long count = item.getCount();
			if(item.isEquipped())
				getInventory().unEquipItemAndSendChanges(item);

			item = _inventory.dropItem("DieDrop", item.getObjectId(), count, this, killer);

			if(killer.isPlayer() && Config.AUTO_LOOT && Config.AUTO_LOOT_PK)
				((L2Player) killer).getInventory().addItem(item);
			else if(killer.isSummon() && Config.AUTO_LOOT && Config.AUTO_LOOT_PK)
				killer.getPlayer().getInventory().addItem(item);
			else
			{
				Location pos = Location.coordsRandomize(this, Config.KARMA_RANDOM_DROP_LOCATION_LIMIT);
				for(int i = 0; i < 20 && !GeoEngine.canMoveWithCollision(getX(), getY(), getZ(), pos.getX(), pos.getY(), pos.getZ(), getReflection()); i++)
					pos = Location.coordsRandomize(this, Config.KARMA_RANDOM_DROP_LOCATION_LIMIT);
				if(!GeoEngine.canMoveWithCollision(getX(), getY(), getZ(), pos.getX(), pos.getY(), pos.getZ(), getReflection()))
				{
					pos.setX(killer.getX());
					pos.setY(killer.getY());
					pos.setZ(killer.getZ());
				}
				item.dropMe(this, pos);
			}

			sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_DROPPED_S1).addItemName(item.getItemId()));
		}
		refreshOverloaded();
	}

	/**
	 * Kill the L2Character, Apply Death Penalty, Manage gain/loss Karma and Item Drop.<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B>
	 * <li>Reduce the Experience of the L2Player in function of the calculated Death Penalty </li>
	 * <li>If necessary, unsummon the Pet of the killed L2Player </li>
	 * <li>Kill the L2Player </li><BR><BR>
	 *
	 * @param killer The L2Character who killed
	 */
	@Override
	public void doDie(L2Character killer)
	{
		//Check for active charm of luck for death penalty
		getDeathPenalty().checkCharmOfLuck();

		if(isTradeInProgress())
			cancelActiveTrade();

		if(getTradeList() != null)
			getTradeList().removeAll();
		setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);

		//Fix onDie trade bug
		setTradeList(null);
		setTransactionRequester(null);
		try
		{
			if(_stanceTask != null)
			{
				_stanceTask.cancel(true);
				_stanceTask = null;
			}
		}
		catch(NullPointerException e)
		{
		}

		L2Skill skill = null;
		if(isInSiege())
		{
			try
			{
				L2Zone zone = getZone(siege);
				int level = 0;
				L2Effect effect = getEffectBySkillId(L2Skill.SKILL_BATTLEFIELD_PENALTY);
				if(effect != null)
					level = effect.getLevel();

				if(zone != null && (zone.getEntityId() == getSiegeId() || TerritoryWarManager.getWar().isInProgress() && getTerritoryId() > 0) && Rnd.chance(Config.BATTLEFIELD_PENALTY_CHANCE))
				{
					level++;
					level = Math.min(level, 5);
				}
				if(level > 0)
					skill = SkillTable.getInstance().getInfo(L2Skill.SKILL_BATTLEFIELD_PENALTY, level);
			}
			catch(Exception e)
			{
			}
		}

		// Kill the L2Player
		super.doDie(killer);

		if(skill != null)
			skill.applyEffects(this, this, false);

		// Unsummon Cubics and agathion
		if(!_cubics.isEmpty())
		{
			for(L2CubicInstance cubic : _cubics)
				cubic.deleteMe();

			_cubics.clear();
		}

		setAgathion(0);

		if(Config.LOG_KILLS && killer != null)
		{
			String coords = " at (" + getX() + "," + getY() + "," + getZ() + ")";
			String killerCoords = " at (" + killer.getX() + "," + killer.getY() + "," + killer.getZ() + ")";
			if(killer instanceof L2NpcInstance)
				killLog.info(this + coords + " karma " + _karma + " killed by mob " + killer.getNpcId() + killerCoords);
			else if(killer instanceof L2Summon && killer.getPlayer() != null)
				killLog.info(this + coords + " karma " + _karma + " killed by summon of " + killer.getPlayer() + killerCoords);
			else
				killLog.info(this + coords + " karma " + _karma + " killed by " + killer + killerCoords);
		}

		if(isCombatFlagEquipped())
		{
			L2ItemInstance ward = getActiveWeaponInstance();
			if(ward != null && ward.isTerritoryWard())
			{
				destroyItem("Die", ward.getObjectId(), 1, null, false);
				_combatFlagEquippedId = false;
				TerritoryWar.broadcastToPlayers(new SystemMessage(SystemMessage.THE_CHARACTER_THAT_ACQUIRED_S1_WARD_HAS_BEEN_KILLED).addHideoutName(ward.getItemId() - 13479));
				TerritoryWarManager.respawnWard(getObjectId());
			}
			else
				FortressSiegeManager.getInstance().dropCombatFlag(this);
		}

		if(isCursedWeaponEquipped())
		{
			_pvpFlag = 0;

			CursedWeaponsManager.getInstance().dropPlayer(this);
			return;
		}
		else if(killer != null && killer.isPlayer() && killer.isCursedWeaponEquipped())
		{
			_pvpFlag = 0;

			//noinspection ConstantConditions
			CursedWeaponsManager.getInstance().increaseKills(((L2Player) killer).getCursedWeaponEquippedId());
			return;
		}

		doPKPVPManage(killer);

		if(killer != null && killer.getPlayer() != null && killer.getPlayer() != this)
		{
			L2Player killerPlayer = killer.getPlayer();
			L2Party party = killerPlayer.getParty();
			if(party != null)
				for(L2Player member : party.getPartyMembers())
					for(QuestState qs : member.getAllQuestsStates())
						if(killerPlayer == member)
							qs.getQuest().onPlayerKill(killerPlayer, this);
						else
							qs.getQuest().onPlayerKillParty(killerPlayer, this, qs);
			else
				for(QuestState qs : killerPlayer.getAllQuestsStates())
					qs.getQuest().onPlayerKill(killerPlayer, this);
		}

		// Set the PvP Flag of the L2Player
		_pvpFlag = 0;

		altDeathPenalty(killer);

		//And in the end of process notify death penalty that owner died :)
		getDeathPenalty().notifyDead(killer);

		setIncreasedForce(0);

		if(isInParty() && getParty().isInDimensionalRift())
			getParty().getDimensionalRift().checkDeath();

		if(getLevel() < 6)
		{
			Quest q = QuestManager.getQuest(255);
			if(q != null)
			{
				QuestState qs = getQuestState(q.getName());
				if(qs != null && (qs.getInt("t") & 0x200) == 0x200)
					processQuestEvent(q.getName(), "TE" + 0x200);
			}
		}
	}

	/**
	 * Restore the Experience of the L2Player and send it a Server->Client StatusUpdate packet.<BR><BR>
	 */
	public void restoreExp()
	{
		restoreExp(100.);
	}

	/**
	 * Restore the percentes of losted Experience of the L2Player.<BR><BR>
	 */
	public void restoreExp(double percent)
	{
		int lostexp = 0;

		String lostexps = getVar("lostexp");
		if(lostexps != null)
		{
			lostexp = Integer.parseInt(lostexps);
			unsetVar("lostexp");
		}

		if(lostexp != 0)
			addExp((long) (lostexp * percent / 100));
	}

	/**
	 * Reduce the Experience (and level if necessary) of the L2Player in function of the calculated Death Penalty.<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Calculate the Experience loss </li>
	 * <li>Set the value of _lostExp </li>
	 * <li>Set the new Experience value of the L2Player and Decrease its level if necessary </li>
	 * <li>Send a Server->Client StatusUpdate packet with its new Experience </li><BR><BR>
	 */
	public void deathPenalty(final boolean atwar, L2Character killer)
	{
		double deathPenaltyBonus = getDeathPenalty().getLevel() * Config.ALT_DEATH_PENALTY_C5_EXPERIENCE_PENALTY;
		if(deathPenaltyBonus < 2)
			deathPenaltyBonus = 1;
		else
			deathPenaltyBonus = deathPenaltyBonus / 2;

		//The death steal you some Exp
		double percentLost = 7.0;
		if(_level >= 76)
			percentLost = 2.0;
		else if(_level >= 40)
			percentLost = 4.0;

		if(Config.ALT_DEATH_PENALTY)
			percentLost = percentLost * Config.RATE_XP + _pkKills * Config.ALT_PK_DEATH_RATE;

		if(atwar)
			percentLost = percentLost / 4.0;

		// Calculate the Experience loss
		long lostexp = Math.round((Experience.LEVEL[_level + 1] - Experience.LEVEL[_level]) * percentLost / 100);
		lostexp *= deathPenaltyBonus;

		lostexp = (long) calcStat(Stats.EXP_LOST, lostexp, killer, null);

		// Потеря опыта на зарегистрированной осаде 1/4 от обычной смерти, с Charm of Courage нет потери
		// На чужой осаде - как при обычной смерти от *моба*
		if(isInSiege())
		{
			Siege siege = SiegeManager.getSiege(getX(), getY());
			if(siege != null && (siege.checkIsDefender(_clanId) || siege.checkIsAttacker(_clanId)) || getTerritoryId() > 0)
				lostexp = 0;
		}

		_log.debug(_name + "is dead, so exp to remove:" + lostexp);

		setVar("lostexp", String.valueOf(lostexp));

		if(lostexp > getExp())
			lostexp = getExp();
		addExpAndSp(-1 * lostexp, 0);
	}

	public void setPartyMatchingAutomaticRegistration(final boolean b)
	{
		_partyMatchingAutomaticRegistration = b;
	}

	public void setPartyMatchingShowLevel(final boolean b)
	{
		_partyMatchingShowLevel = b;
	}

	public void setPartyMatchingShowClass(final boolean b)
	{
		_partyMatchingShowClass = b;
	}

	public Point getLastPartyPositionSent()
	{
		return _lastPartyPositionSent;
	}

	@Override
	public void setXYZ(int x, int y, int z, boolean move)
	{
		if(getParty() != null && _lastPartyPositionSent != null && getDistance(_lastPartyPositionSent.x, _lastPartyPositionSent.y) > 500)
		{
			getParty().broadcastToPartyMembers(this, new PartyMemberPosition(this));
			_lastPartyPositionSent.move(x, y);
		}

		int currZ = getZ();

		super.setXYZ(x, y, z, move);

		if(move && !isFloating() && !isInBoat())
		{
			if(currZ - getZ() > 64 && _fallTask == null)
			{
				//_log.info(this + " start falling from: " + z);
				_fallZ = currZ;
				_fallTask = ThreadPoolManager.getInstance().scheduleAi(new FallTask(), 2000, true);
			}
		}
	}

	@Override
	public void setXYZInvisible(int x, int y, int z)
	{
		if(getParty() != null && getDistance(_lastPartyPositionSent.x, _lastPartyPositionSent.y) > 500)
		{
			getParty().broadcastToPartyMembers(this, new PartyMemberPosition(this));
			_lastPartyPositionSent.move(x, y);
		}
		super.setXYZInvisible(x, y, z);
	}

	/**
	 * @param memo
	 */
	public void setPartyMatchingMemo(final String memo)
	{
		_partyMatchingMemo = memo;
	}

	public boolean isPartyMatchingAutomaticRegistration()
	{
		return _partyMatchingAutomaticRegistration;
	}

	public String getPartyMatchingMemo()
	{
		return _partyMatchingMemo;
	}

	public boolean isPartyMatchingShowClass()
	{
		return _partyMatchingShowClass;
	}

	public boolean isPartyMatchingShowLevel()
	{
		return _partyMatchingShowLevel;
	}

	/**
	 * Set the L2Player requester of the transaction.<BR><BR>
	 */
	public void setTransactionRequester(final L2Player requestor)
	{
		_currentTransactionRequester = requestor;
		_currentTransactionTimeout = -1;
	}

	public void setTransactionRequester(L2Player requestor, long timeout)
	{
		_currentTransactionTimeout = timeout;
		_currentTransactionRequester = requestor;
	}

	public static enum TransactionType
	{
		NONE,
		PARTY,
		CLAN,
		ALLY,
		TRADE,
		FRIEND,
		CHANNEL,
		PARTY_ROOM,
		COUPLE_ACTION
	}

	private TransactionType _currentTransactionType = TransactionType.NONE;

	public void setTransactionType(TransactionType type)
	{
		_currentTransactionType = type;
	}

	public TransactionType getTransactionType()
	{
		return _currentTransactionType;
	}

	/**
	 * @return the L2Player requester of the transaction.<BR><BR>
	 */
	public L2Player getTransactionRequester()
	{
		return _currentTransactionRequester;
	}

	/**
	 * @return True if a transaction is in progress.<BR><BR>
	 */
	public boolean isTransactionInProgress()
	{
		return (_currentTransactionTimeout < 0 || _currentTransactionTimeout > System.currentTimeMillis()) && _currentTransactionRequester != null;
	}

	public boolean isTransactionRequestInProgress()
	{
		return _currentTransactionTimeout > System.currentTimeMillis();
	}

	/**
	 * @return True if a trade transaction is in progress.<BR><BR>
	 */
	public boolean isTradeInProgress()
	{
		return getTransactionType() == TransactionType.TRADE && getTradeList() != null;
	}

	public void cancelActiveTrade()
	{
		try
		{
			if(_currentTransactionRequester != null)
			{
				_currentTransactionRequester.sendPacket(new SendTradeDone(0));
				_currentTransactionRequester.sendPacket(new SystemMessage(SystemMessage.S1_CANCELED_THE_TRADE).addString(getName()));
				_currentTransactionRequester.setTradeList(null);
				_currentTransactionRequester.setTransactionRequester(null);
			}
		}
		catch(NullPointerException e)
		{
		}

		sendPacket(new SendTradeDone(0));
		setTradeList(null);
		setTransactionRequester(null);
	}

	public void cancelActiveEnchant()
	{
		if(getEnchantScroll() != null)
		{
			setEnchantScroll(null);
			setEnchantSupportItem(null);
			setEnchantStartTime(0);
		}
	}

	public void addVisibleObject(L2Object object, L2Character dropper)
	{
		if(isLogoutStarted() || object == null || object.getObjectId() == getObjectId() || !object.isVisible())
			return;

		if(object.isTrap() && !((L2TrapInstance) object).isDetected() && object.getPlayer() != this && !(getParty() != null && getParty().containsMember(object.getPlayer())))
			return;

		if(object.isPolymorphed() && object.getPolytype().equals("item"))
		{
			sendPacket(new SpawnItemPoly(object));
			showMoves(object);
			return;
		}

		if(object.isPolymorphed() && object.getPolytype().equals("npc"))
		{
			sendPacket(new NpcInfoPoly(object, this));
			showMoves(object);
			return;
		}

		if(object instanceof L2ItemInstance)
		{
			if(dropper != null)
				sendPacket(new DropItem((L2ItemInstance) object, dropper.getObjectId()));
			else
				sendPacket(new SpawnItem((L2ItemInstance) object));

			showMoves(object);
			return;
		}

		if(object instanceof L2DoorInstance)
		{
			sendPacket(new StaticObject((L2DoorInstance) object));
			return;
		}

		if(object instanceof L2StaticObjectInstance)
		{
			sendPacket(new StaticObject((L2StaticObjectInstance) object));
			return;
		}

		if(object instanceof L2ClanBaseManagerInstance)
			((L2ClanBaseManagerInstance) object).sendDecoInfo(this);

		if(object instanceof L2ControlTowerInstance && ((L2ControlTowerInstance) object).getControlEventId() > 0)
			sendPacket(new EventTrigger(((L2ControlTowerInstance) object).getControlEventId(), ((L2ControlTowerInstance) object).isTrapActive()));

		if(object instanceof L2FakeTowerInstance && ((L2FakeTowerInstance) object).getControlEventId() > 0)
			sendPacket(new EventTrigger(((L2FakeTowerInstance) object).getControlEventId(), ((L2FakeTowerInstance) object).isTrapActive()));

		if(object instanceof L2NpcInstance)
		{
			sendPacket(new NpcInfo((L2NpcInstance) object, this));
			// Хз почему, но клиент показывает данный обнормал только со второго раза.
			if((((L2NpcInstance) object).getAbnormalEffect() & L2Skill.AbnormalVisualEffect.change_texture.mask) == L2Skill.AbnormalVisualEffect.change_texture.mask)
				sendPacket(new NpcInfo((L2NpcInstance) object, this));
			showMoves(object);
			return;
		}

		if(object instanceof L2Summon)
		{
			L2Summon summon = (L2Summon) object;

			if(summon.getPlayer() == this)
			{
				sendPacket(new PetInfo(summon, summon.isShowSpawnAnimation() ? 2 : summon.isTeleported() ? 0 : 1));
				sendPacket(new PartySpelled(summon, true));

				if(summon.isPet())
					sendPacket(new PetItemList((L2PetInstance) summon));
			}
			else if(getParty() != null && getParty().containsMember(summon.getPlayer()))
			{
				sendPacket(new NpcInfo(summon, this, summon.isShowSpawnAnimation() || summon.isTeleported()));
				sendPacket(new PartySpelled(summon, true));
			}
			else
				sendPacket(new NpcInfo(summon, this, summon.isShowSpawnAnimation() || summon.isTeleported()));

			showMoves(object);
			return;
		}

		if(object.isPlayer())
		{
			final L2Player otherPlayer = (L2Player) object;

			if(otherPlayer.getKnownRelations().get(getObjectId()) == null)
				otherPlayer.getKnownRelations().put(getObjectId(), -1);

			if((otherPlayer.isInvisible() || otherPlayer.isHide()) && getObjectId() != otherPlayer.getObjectId())
				return;

			if(otherPlayer.getPrivateStoreType() != STORE_PRIVATE_NONE && getVarB("notraders"))
				return;

			if(getObjectId() != otherPlayer.getObjectId())
			{
				sendPacket(new CharInfo(otherPlayer));
				sendPacket(new ExBrExtraUserInfo(otherPlayer));

				if(otherPlayer.getMountEngine().isMounted())
				{
					sendPacket(new Ride(otherPlayer));
					sendPacket(new CharInfo(otherPlayer));
				}
			}

			if(otherPlayer.getPrivateStoreType() != STORE_PRIVATE_NONE)
				if(otherPlayer.getPrivateStoreType() == STORE_PRIVATE_BUY)
					sendPacket(new PrivateStoreMsgBuy(otherPlayer));
				else if(otherPlayer.getPrivateStoreType() == STORE_PRIVATE_SELL)
					sendPacket(new PrivateStoreMsgSell(otherPlayer));
				else if(otherPlayer.getPrivateStoreType() == STORE_PRIVATE_SELL_PACKAGE)
					sendPacket(new ExPrivateStoreSetWholeMsg(otherPlayer));
				else if(otherPlayer.getPrivateStoreType() == STORE_PRIVATE_MANUFACTURE)
					sendPacket(new RecipeShopMsg(otherPlayer));

			if(otherPlayer.isCastingNow() && otherPlayer.getAnimationEndTime() > 0)
			{
				L2Skill skill = otherPlayer.getCastingSkill();
				L2Character target = otherPlayer.getCastingTarget();
				if(skill != null && target != null && target.isCharacter())
					sendPacket(otherPlayer.isInAirShip() ? new ExMagicSkillUseInAirShip(otherPlayer, target, skill.getId(), skill.getLevel(), (int) (otherPlayer.getAnimationEndTime() - System.currentTimeMillis()), 0, skill.isBuff()) : new MagicSkillUse(otherPlayer, target, skill.getId(), skill.getLevel(), (int) (otherPlayer.getAnimationEndTime() - System.currentTimeMillis()), 0, skill.isBuff()));
			}

			if(otherPlayer.isInBoat())
				otherPlayer.getVehicle().sendOnBoardInfo(this, otherPlayer);

			otherPlayer.sendRelation(this);
			showMoves(object);
			return;
		}

		if(object.isVehicle())
		//if(object != getVehicle() || ((L2Vehicle) object).isTeleporting())
		{
			L2Vehicle vehicle = (L2Vehicle) object;
			if(vehicle.isAirShip())
				vehicle.sendInfo(this);

			if(vehicle.isMoving)
				vehicle.broadcastMove();
			else if(!vehicle.isAirShip())
				vehicle.sendInfo(this);
		}
	}

	public void removeVisibleObject(L2Object object)
	{
		if(isLogoutStarted() || object == null || object.getObjectId() == getObjectId())
			return;

		if(object instanceof L2Summon && object.getPlayer() == this)
			return;

		if(_vehicle != object)
			sendPacket(new DeleteObject(object));

		if(object.isNpc())
		{
			object.getAI().removeAttackDesire(this);
			if(getPet() != null)
				object.getAI().removeAttackDesire(getPet());
		}
//		{
//			if(((L2NpcInstance) object).getAI() instanceof DefaultAI && ((DefaultAI)((L2NpcInstance) object).getAI()).isDebug())
//				_log.info(object +" removeVisibaleObject: stopHate: " + this);
//		}
		if(object.isPlayer())
			getKnownRelations().remove(object.getObjectId());

		getAI().notifyEvent(CtrlEvent.EVT_FORGET_OBJECT, object);
	}

	public void showMoves(L2Object object)
	{
		if(object != null && object.isCharacter())
		{
			L2Character obj = (L2Character) object;
			if(obj.isMoving && obj.getDestination() != null)
				sendPacket(new CharMoveToLocation(obj, obj.getDestination()));
		}
	}

	private void increaseLevelAction()
	{
		if(_level >= Experience.getMaxLevel())
			return;

		sendPacket(Msg.YOU_HAVE_INCREASED_YOUR_LEVEL);
		broadcastPacket(new SocialAction(getObjectId(), SocialAction.SocialType.LEVEL_UP));
	}

	/**
	 * Manage the increase level task of a L2Player (Max MP, Max MP, Recommendation, Expertise).<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send a Server->Client System Message to the L2Player : YOU_INCREASED_YOUR_LEVEL </li>
	 * <li>Send a Server->Client packet StatusUpdate to the L2Player with new LEVEL, MAX_HP and MAX_MP </li>
	 * <li>Set the current HP and MP of the L2Player, Launch/Stop a HP/MP/CP Regeneration Task and send StatusUpdate packet to all other L2Player to inform (exclusive broadcast)</li>
	 * <li>Recalculate the party level</li>
	 * <li>Give Expertise skill of this level</li><BR><BR>
	 */
	private void increaseLevel()
	{
		if(_level >= Experience.getMaxLevel())
			return;

		_level++;

		//sendPacket(Msg.YOU_HAVE_INCREASED_YOUR_LEVEL);
		//broadcastPacket(new SocialAction(getObjectId(), 15));

		boolean restoreHpMp = true;

		GArray<L2Zone> zones = getZones();
		if(zones != null)
			for(L2Zone zone : zones)
				if(zone != null && !zone.isLevelUpRestoreHpMp())
				{
					restoreHpMp = false;
					break;
				}

		if(restoreHpMp && !isDead())
			setCurrentHpMp(getMaxHp(), getMaxMp());

		if(!isDead())
			setCurrentCp(getMaxCp());

		// Recalculate the party level
		if(isInParty())
			getParty().recalculatePartyData();

		if(_clanId > 0)
		{
			PledgeShowMemberListUpdate memberUpdate = new PledgeShowMemberListUpdate(this);
			for(L2Player clanMember : getClan().getOnlineMembers(null))
				clanMember.sendPacket(memberUpdate);
		}

		// Give Expertise skill of this level
		rewardSkills();

		Quest q = QuestManager.getQuest(255);
		if(q != null)
			processQuestEvent(q.getName(), "LU");


		if(isPetSummoned())
		{
			if(_summon instanceof L2PetInstance && ((L2PetInstance) _summon).getTemplate().sync_level)
				((L2PetInstance) _summon).increaseLevel();
			getPet().sendPetInfo();
		}

		Object[] script_args = new Object[] { this };
		for(Scripts.ScriptClassAndMethod handler : Scripts.onLevelUp)
			callScripts(handler.scriptClass, handler.method, script_args);
	}

	/**
	 * Manage the decrease level task of a L2Player (Max MP, Max MP, Recommendation, Expertise and beginner skills...).<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send a Server->Client packet StatusUpdate to the L2Player with new LEVEL, MAX_HP and MAX_MP </li>
	 * <li>Recalculate the party level</li>
	 * <li>Recalculate the number of Recommendation that the L2Player can give</li>
	 * <li>Give Expertise skill of this level</li><BR><BR>
	 */
	private void decreaseLevel()
	{
		if(_level == 1)
			return;

		_level--;

		// Recalculate the party level
		if(isInParty())
			getParty().recalculatePartyData();

		if(_clanId > 0)
		{
			PledgeShowMemberListUpdate memberUpdate = new PledgeShowMemberListUpdate(this);
			for(L2Player clanMember : getClan().getOnlineMembers(_name))
				if(!clanMember.equals(this))
					clanMember.sendPacket(memberUpdate);
		}

		// Give Expertise skill of this level
		rewardSkills();

		if(isPetSummoned())
		{
			if(_summon instanceof L2PetInstance && ((L2PetInstance) _summon).getTemplate().sync_level)
				((L2PetInstance) _summon).decreaseLevel();
			getPet().sendPetInfo();
		}
	}

	/**
	 * Stops timers that are related to current instance of L2Player<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Set the RegenActive flag to False </li>
	 * <li>Stop the HP/MP/CP Regeneration task </li>
	 * <li>Stop cubics</li>
	 */
	public void stopAllTimers()
	{
		for(L2CubicInstance cubic : _cubics)
			cubic.deleteMe();
		_cubics.clear();

		setAgathion(0);
		stopHpMpRegeneration();
		stopWaterTask();
		stopPremiumTask();
		stopHourlyTask();

		if(_fallTask != null)
		{
			_fallTask.cancel(true);
			_fallTask = null;
		}
		if(_nonAggroTask != null)
		{
			_nonAggroTask.cancel(true);
			_nonAggroTask = null;
		}
		if(_floatingRate != null)
			_floatingRate.stopExtraTask();
	}

	/**
	 * @return the L2Summon of the L2Player or null.
	 */
	@Override
	public L2Summon getPet()
	{
		return _summon;
	}

	public boolean isPetSummoned()
	{
		return _summon != null;
	}

	/**
	 * Set the L2Summon of the L2Player.<BR><BR>
	 *
	 * @param summon L2Summon to set
	 */
	public void setPet(L2Summon summon)
	{
		_summon = summon;
		if(summon == null)
		{
			for(int itemId = 6645; itemId <= 6647; itemId++)
				if(_activeSoulShots.contains(itemId))
				{
					removeAutoSoulShot(itemId);
					sendPacket(new ExAutoSoulShot(itemId, false));
				}

			L2Effect ef;
			if((ef = getEffectBySkillId(4140)) != null)
				ef.exit();
		}
		else if(isInDuel())
			_summon.setDuelState(getDuelState());
	}

	public Future<?> _userInfoTask = null;
	private SendUserInfo _sendTask = null;
	private boolean entering = true;

	public void sendUserInfo(boolean force)
	{
		if(isEntering() || isLogoutStarted())
			return;

		if(Config.USER_INFO_INTERVAL == 0)
			force = true;

		if(force)
		{
			sendPacket(new UserInfo(this));
			sendPacket(new ExBrExtraUserInfo(this));
			if(getSiegeState() == 3)
				sendPacket(new ExDominionWarStart(this));

			if(_userInfoTask != null)
			{
				_userInfoTask.cancel(true);
				_userInfoTask = null;
			}
			return;
		}

		if(_userInfoTask != null)
			return;

		if(_sendTask == null)
			_sendTask = new SendUserInfo(this);

		_userInfoTask = ThreadPoolManager.getInstance().scheduleAi(_sendTask, Config.USER_INFO_INTERVAL, true);
	}

	/**
	 * Manage the delete task of a L2Player (Leave Party, Unsummon pet, Save its inventory in the database, Remove it from the world...).<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>If the L2Player is in observer mode, set its position to its position before entering in observer mode </li>
	 * <li>Set the online Flag to True or False and update the characters table of the database with online status and lastAccess </li>
	 * <li>Stop the HP/MP/CP Regeneration task </li>
	 * <li>Cancel Crafting, Attak or Cast </li>
	 * <li>Remove the L2Player from the world </li>
	 * <li>Stop Party and Unsummon Pet </li>
	 * <li>Update database with items in its inventory and remove them from the world </li>
	 * <li>Remdrk.xfk ggw
	 * ove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI </li>
	 * <li>Close the connection with the client </li><BR><BR>
	 */
	public void deleteMe()
	{
		setReflection(0);

		AutoSaveManager.getInstance().removePlayer(this);
		//saveCharToDisk();
		// При логауте автоматом проигрывается дуэль.
		if(isInDuel())
			getDuel().onPlayerDefeat(this);

		_massUpdating = true;
		_isDeleting = true;

		if(jailTask != null)
			jailTask.cancel(true);

		if(isTradeInProgress())
			cancelActiveTrade();

		if(isCombatFlagEquipped())
		{
			if(getActiveWeaponInstance() != null && getActiveWeaponInstance().isTerritoryWard())
			{
				destroyItem("Relogin", getActiveWeaponInstance().getObjectId(), 1, null, false);
				TerritoryWarManager.respawnWard(getObjectId());
			}
			else
				FortressSiegeManager.getInstance().dropCombatFlag(this);
		}

		if(getTransactionRequester() != null)
		{
			getTransactionRequester().setTransactionRequester(null);
			setTransactionRequester(null);
		}

		if(Olympiad.isRegisteredInComp(this))
		{
			try
			{
				Olympiad.removeFromReg(this);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

		if(getVitality() != null)
			getVitality().stopUpdatetask();

		if(_huntingBonus != null)
			_huntingBonus.stopTasksOnLogout();

		if(_recommendSystem != null)
			_recommendSystem.stopBonusTask(true);

		// Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout)
		try
		{
			setOnlineStatus(false);
		}
		catch(Throwable t)
		{
			_log.warn("deletedMe()", t);
		}

		if(getOlympiadGameId() != -1 && !inObserverMode()) // handle removal from olympiad game
			Olympiad.removeDisconnectedCompetitor(this);

		super.deleteMe();

		// Stop the HP/MP/CP Regeneration task (scheduled tasks)
		try
		{
			stopAllTimers();
		}
		catch(Throwable t)
		{
			_log.warn("deletedMe()", t);
		}

		if(_forceBuff != null)
			_forceBuff.delete();

		if(getEffectPoint() != null)
			getEffectPoint().deleteMe();

		if(_party != null)
			for(L2Player member : _party.getPartyMembers())
				if(member.getForceBuff() != null && member.getForceBuff().getTarget() == this)
					member.getForceBuff().delete();

		// If a Party is in progress, leave it
		if(isInParty())
			try
			{
				getParty().oustPartyMember(this);
			}
			catch(Throwable t)
			{
				_log.warn("deletedMe()", t);
			}

		// If the L2Player has Pet, unsummon it
		if(getPet() != null)
			try
			{
				storeSummon();
				getPet().unSummon();
			}
			catch(Throwable t)
			{
				_log.warn("deletedMe()", t);
			}// returns pet to control item

		if(getLastTrap() != null)
			try
			{
				getLastTrap().doDie(null);
			}
			catch(Throwable t)
			{
				_log.warn("deletedMe()", t);
			}

		// Update database with items in its inventory and remove them from the world
		try
		{
			getInventory().deleteMe();
		}
		catch(Throwable t)
		{
			_log.warn("deletedMe()", t);
		}

		try
		{
			if(getClanId() != 0)
				getClan().notifyClanMembers(this, false);
		}
		catch(Exception e)
		{
			_log.warn("deletedMe()", e);
			e.printStackTrace();
		}

		if(CursedWeaponsManager.getInstance().getCursedWeapon(getCursedWeaponEquippedId()) != null)
			CursedWeaponsManager.getInstance().getCursedWeapon(getCursedWeaponEquippedId()).setPlayer(null);

		for(L2Player player : _snoopedPlayer)
			player.removeSnooper(this);

		for(L2Player player : _snoopListener)
			player.removeSnooped(this);

		if(getPartyRoom() > 0)
		{
			PartyRoom room = PartyRoomManager.getInstance().getRooms().get(getPartyRoom());
			if(room != null)
				if(room.getLeader() == null || room.getLeader().equals(this))
					PartyRoomManager.getInstance().removeRoom(room.getId());
				else
					room.removeMember(this, false);
		}

		setPartyRoom(0);

		//Send friendlists to friends that this player has logged off
		if(_friendList != null)
			_friendList.notifyFriends(false);

		stopAllEffects();
		if(_vehicle != null)
		{
			_vehicle.removePlayerFromBoard(this);
			_vehicle = null;
		}

		getAI().stopFollow();
		_ai = null;
		_summon = null;
		_warehouse = null;
		_freight = null;
		_lastPartyPositionSent = null;
		_arrowItem = null;
		_chars = null;
		_enchantScroll = null;
		_agathionId = 0;
		_lastFolkNpc = null;
		_lastMultisellNpc = null;
		_observRegion = null;
		_friendList = null;
		_contactList = null;
		setOwner(null);
	}

	/**
	 * Set the _tradeList object of the L2Player.<BR><BR>
	 */
	public void setTradeList(final L2TradeList x)
	{
		if(x == null && getPrivateStoreType() != STORE_PRIVATE_NONE)
			_log.info("setTrageList to null in private store: " + this);

		if(x == null)
			_currentTransactionType = TransactionType.NONE;

		_tradeList = x;
	}

	/**
	 * @return the _tradeList object of the L2Player.<BR><BR>
	 */
	public L2TradeList getTradeList()
	{
		return _tradeList;
	}

	/**
	 * Set the _sellList object of the L2Player.<BR><BR>
	 */
	public void setSellList(final ConcurrentLinkedQueue<TradeItem> x)
	{
		_sellList = x;
		saveTradeList();
	}

	/**
	 * @return the _sellList object of the L2Player.<BR><BR>
	 */
	public ConcurrentLinkedQueue<TradeItem> getSellList()
	{
		return _sellList != null ? _sellList : new ConcurrentLinkedQueue<TradeItem>();
	}

	/**
	 * @return the _createList object of the L2Player.<BR><BR>
	 */
	public L2ManufactureList getCreateList()
	{
		return _createList;
	}

	/**
	 * Set the _createList object of the L2Player.<BR><BR>
	 */
	public void setCreateList(final L2ManufactureList x)
	{
		_createList = x;
		saveTradeList();
	}

	/**
	 * Set the _buyList object of the L2Player.<BR><BR>
	 */
	public void setBuyList(final ConcurrentLinkedQueue<TradeItem> x)
	{
		_buyList = x;
		saveTradeList();
	}

	/**
	 * @return the _buyList object of the L2Player.<BR><BR>
	 */
	public ConcurrentLinkedQueue<TradeItem> getBuyList()
	{
		return _buyList != null ? _buyList : new ConcurrentLinkedQueue<TradeItem>();
	}

	/**
	 * Set the Private Store type of the L2Player.<BR><BR>
	 * <p/>
	 * <B><U> Values </U> :</B><BR><BR>
	 * <li>0 : STORE_PRIVATE_NONE</li>
	 * <li>1 : STORE_PRIVATE_SELL</li>
	 * <li>2 : sellmanage</li><BR>
	 * <li>3 : STORE_PRIVATE_BUY</li><BR>
	 * <li>4 : buymanage</li><BR>
	 * <li>5 : STORE_PRIVATE_MANUFACTURE</li><BR>
	 * <li>8 : STORE_PRIVATE_SELL_PACKAGE</li><BR>
	 */
	public void setPrivateStoreType(final short type)
	{
		_privatestore = type;
		if(type != STORE_PRIVATE_NONE)
			setVar("storemode", String.valueOf(type));
		else
			unsetVar("storemode");
	}

	public void setPrivateStoreManage(boolean manage)
	{
		_privateStoreManage = manage;
	}

	/**
	 * @return the Private Store type of the L2Player.<BR><BR>
	 *         <p/>
	 *         <B><U> Values </U> :</B><BR><BR>
	 *         <li>0 : STORE_PRIVATE_NONE</li>
	 *         <li>1 : STORE_PRIVATE_SELL</li>
	 *         <li>2 : sellmanage</li><BR>
	 *         <li>3 : STORE_PRIVATE_BUY</li><BR>
	 *         <li>4 : buymanage</li><BR>
	 *         <li>5 : STORE_PRIVATE_MANUFACTURE</li><BR>
	 *         <li>7 : STORE_OBSERVING_GAMES</li><BR>
	 *         <li>8 : STORE_PRIVATE_SELL_PACKAGE</li><BR>
	 */
	public short getPrivateStoreType()
	{
		if(inObserverMode() && getOlympiadGameId() < 0)
			return STORE_OBSERVING_GAMES;

		return _privatestore;
	}

	/**
	 * Set the _skillLearningClassId object of the L2Player.<BR><BR>
	 */
	public void setSkillLearningClassId(final ClassId classId)
	{
		_skillLearningClassId = classId;
	}

	/**
	 * @return the _skillLearningClassId object of the L2Player.<BR><BR>
	 */
	public ClassId getSkillLearningClassId()
	{
		return _skillLearningClassId;
	}

	/**
	 * Set the _clan object, _clanId, _clanLeader Flag and title of the L2Player.<BR><BR>
	 *
	 * @param clan the clat to set
	 */
	public void setClan(L2Clan clan)
	{
		if(clan == null)
		{
			_clanId = 0;
			_clanLeader = false;
			_pledgeType = 0;
			_pledgeRank = PledgeRank.VAGABOND;
			_powerGrade = 0;
			_lvlJoinedAcademy = 0;
			_apprentice = 0;
			return;
		}

		if(!clan.isMember(getObjectId()))
		{
			// char has been kicked from clan
			_log.debug("Char " + _name + " is kicked from clan: " + clan.getName());
			setClan(null);
			setTitle("");
			return;
		}

		_clanId = clan.getClanId();
		_clanLeader = getObjectId() == clan.getLeaderId();

		setTitle("");
	}

	/**
	 * @return the _clan object of the L2Player.<BR><BR>
	 */
	public L2Clan getClan()
	{
		return ClanTable.getInstance().getClan(_clanId);
	}

	/**
	 * @return True if the L2Player is the leader of its clan.
	 */
	public boolean isClanLeader()
	{
		return _clanLeader;
	}

	public void setClanLeader(boolean leader)
	{
		_clanLeader = leader;
	}

	public boolean isAllyLeader()
	{
		return getAllyId() > 0 && getAlliance().getLeader().getLeaderId() == getObjectId();
	}

	/**
	 * Reduce the number of arrows owned by the L2Player.<BR><BR>
	 */
	@Override
	public void reduceArrowCount()
	{
		L2ItemInstance arrows = getInventory().destroyItem("Consume", getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND), 1, this, null);
		if(arrows == null || arrows.getCount() == 0)
		{
			if(arrows != null)
				getInventory().unEquipItemAndSendChanges(arrows);
			_arrowItem = null;
		}
	}

	/**
	 * Equip arrows needed in left hand and send a Server->Client packet ItemList to the L2Player then return True.<BR><BR>
	 */
	protected boolean checkAndEquipArrows()
	{
		// Check if nothing is equipped in left hand
		if(_inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
		{
			L2Weapon weapon = getActiveWeaponItem();
			if(weapon == null)
				return false;
			// Get the L2ItemInstance of the arrows needed for this bow
			if(weapon.getItemType() == WeaponType.BOW)
				_arrowItem = _inventory.findArrowForBow(getActiveWeaponItem());
			else if(weapon.getItemType() == WeaponType.CROSSBOW)
				_arrowItem = _inventory.findArrowForCrossbow(getActiveWeaponItem());

			// Equip arrows needed in left hand
			if(_arrowItem != null)
				sendPacket(new InventoryUpdate(getInventory().equipItemAndRecord(_arrowItem)));
		}
		else
			// Get the L2ItemInstance of arrows equipped in left hand
			_arrowItem = _inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND);

		return _arrowItem != null;
	}

	/**
	 * @return True if the L2Player has a Party in progress.<BR><BR>
	 */
	public boolean isInParty()
	{
		return _party != null;
	}

	/**
	 * Set the _party object of the L2Player (without joining it).<BR><BR>
	 */
	public void setParty(final L2Party party)
	{
		_party = party;
	}

	/**
	 * Set the _party object of the L2Player AND join it.<BR><BR>
	 */
	public void joinParty(final L2Party party)
	{
		if(party != null)
		{
			_party = party;
			party.addPartyMember(this);
			party.broadcastToPartyMembers(this, new PartySpelled(this, true));
			for(L2Player member : party.getPartyMembers())
				sendPacket(new PartySpelled(member, true));
		}
	}

	/**
	 * Manage the Leave Party task of the L2Player.<BR><BR>
	 */
	public void leaveParty()
	{
		if(isInParty())
		{
			_party.removePartyMember(this);
			_party = null;
		}
	}

	/**
	 * @return the _party object of the L2Player.<BR><BR>
	 */
	public L2Party getParty()
	{
		return _party;
	}

	/**
	 * @return True if the L2Player is a GM.<BR><BR>
	 */
	public boolean isGM()
	{
		return AdminTemplateManager.checkBoolean("isGM", this);
	}

	/**
	 * @return True if the L2Player has NonDrop flag.<BR><BR>
	 */
	public boolean isNonDrop()
	{
		return AdminTemplateManager.checkBoolean("noDrop", this);
	}

	/**
	 * Нигде не используется, но может пригодиться для БД
	 */
	public void setAccessLevel(final int level)
	{
		_accessLevel = level;
	}

	/**
	 * Нигде не используется, но может пригодиться для БД
	 */
	@Override
	public int getAccessLevel()
	{
		return _accessLevel;
	}

	public void setAccountAccesslevel(final int level, final String comments, int banTime)
	{
		LSConnection.getInstance().sendPacket(new ChangeAccessLevel(getAccountName(), level, comments, banTime));
	}

	@Override
	public double getLevelMod()
	{
		return (89. + getLevel()) / 100.0;
	}

	/**
	 * Update Stats of the L2Player client side by sending Server->Client packet UserInfo/StatusUpdate to this L2Player and CharInfo/StatusUpdate to all L2Player in its _KnownPlayers (broadcast).<BR><BR>
	 */
	@Override
	public void updateStats()
	{
		refreshOverloaded();
		refreshExpertisePenalty();
		sendChanges();
	}

	/**
	 * Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2Player and all L2Player to inform (broadcast).<BR><BR>
	 */
	public void setKarmaFlag(final int flag)
	{
		// Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2Player and all L2Player to inform (broadcast)
		final StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.KARMA, _karma);
		su.addAttribute(StatusUpdate.PVP_FLAG, flag);
		broadcastPacket(su);
		if(isPetSummoned())
			getPet().broadcastPacket(su);
	}

	/**
	 * Send a Server->Client StatusUpdate packet with Karma to the L2Player and all L2Player to inform (broadcast).<BR><BR>
	 */
	public void updateKarma()
	{
		// Send a Server->Client StatusUpdate packet with Karma to the L2Player
		final StatusUpdate su = new StatusUpdate(getObjectId());
		su.addAttribute(StatusUpdate.KARMA, _karma);
		broadcastPacket(su);
		if(isPetSummoned())
			getPet().broadcastPacket(su);
	}

	/**
	 * Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout).<BR><BR>
	 */
	public void setOnlineStatus(final boolean isOnline)
	{
		if(_isOnline != isOnline)
			_isOnline = isOnline;

		// Update the characters table of the database with online status and lastAccess (called when login and logout)
		updateOnlineStatus();
	}

	/**
	 * Update the characters table of the database with online status and lastAccess of this L2Player (called when login and logout).<BR><BR>
	 */
	public void storeHWID(String HWID, org.apache.commons.logging.Log log)
	{
		if(!_lastHWID.equalsIgnoreCase(HWID))
		{
			log.info("HWID changed from " + _lastHWID + " to " + HWID + " account: " + _accountName);
			_lastHWID = HWID;
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("UPDATE characters SET LastHWID=? WHERE obj_id=?");
				statement.setString(1, HWID);
				statement.setInt(2, getObjectId());
				statement.execute();
			}
			catch(final Exception e)
			{
				_log.warn("could not store characters HWID:" + e);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}

	public void updateOnlineStatus()
	{
		if(isInOfflineMode())
			return;

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET online=?, lastAccess=? WHERE obj_id=?");
			statement.setInt(1, isOnline() ? 1 : 0);
			statement.setLong(2, System.currentTimeMillis() / 1000);
			statement.setInt(3, getObjectId());
			statement.execute();
		}
		catch(final Exception e)
		{
			_log.warn("could not set char online status:" + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	/**
	 * Decrease Karma of the L2Player and Send it StatusUpdate packet with Karma and PvP Flag (broadcast).<BR><BR>
	 */
	public void increaseKarma(final long add_karma)
	{
		long new_karma = _karma + add_karma;

		if(new_karma > Integer.MAX_VALUE)
			new_karma = Integer.MAX_VALUE;

		if(_karma == 0 && new_karma > 0)
		{
			_karma = (int) new_karma;
			for(final L2Character cha : getKnownCharacters(2000))
				if(cha instanceof L2GuardInstance && cha.getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
					cha.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE, null, null);
		}
		else
			_karma = (int) new_karma;

		// Send a Server->Client StatusUpdate packet with Karma to the L2Player and all L2Player to inform (broadcast)
		updateKarma();
	}

	/**
	 * Decrease Karma of the L2Player and Send it StatusUpdate packet with Karma and PvP Flag (broadcast).<BR><BR>
	 *
	 * @param i : The loss Karma value
	 */
	public void decreaseKarma(final int i)
	{
		_karma -= i;

		if(_karma <= 0)
		{
			_karma = 0;

			// Send a Server->Client StatusUpdate packet with Karma and PvP Flag to the L2Player and all L2Player to inform (broadcast)
			setKarmaFlag(0);

			return;
		}

		// Send a Server->Client StatusUpdate packet with Karma to the L2Player and all L2Player to inform (broadcast)
		updateKarma();

	}

	/**
	 * Create a new player in the characters table of the database.<BR><BR>
	 */
	protected boolean createDb()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO `characters` (account_name, obj_Id, char_name, face, hairStyle, hairColor, sex, karma, pvpkills, pkkills, clanid, deletetime, title, accesslevel, online, leaveclan, deleteclan, nochannel, pledge_type, pledge_rank, lvl_joined_academy, apprentice, birthday) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,UNIX_TIMESTAMP())");
			statement.setString(1, _accountName);
			statement.setInt(2, getObjectId());
			statement.setString(3, getName());
			statement.setInt(4, getFace());
			statement.setInt(5, getHairStyle());
			statement.setInt(6, getHairColor());
			statement.setInt(7, getSex());
			statement.setInt(8, getKarma());
			statement.setInt(9, getPvpKills());
			statement.setInt(10, getPkKills());
			statement.setInt(11, getClanId());
			statement.setInt(12, getDeleteTimer());
			statement.setString(13, getTitle());
			statement.setInt(14, _accessLevel);
			statement.setInt(15, isOnline() ? 1 : 0);
			statement.setLong(16, getLeaveClanTime() / 1000);
			statement.setLong(17, getDeleteClanTime() / 1000);
			statement.setLong(18, _NoChannel > 0 ? _NoChannel / 1000 : _NoChannel);
			statement.setInt(19, getPledgeType());
			statement.setInt(20, getPowerGrade());
			statement.setInt(21, getLvlJoinedAcademy());
			statement.setInt(22, getApprentice());
			statement.executeUpdate();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("INSERT INTO character_subclasses (char_obj_id, class_id, exp, sp, curHp, curMp, curCp, maxHp, maxMp, maxCp, level, active, isBase, death_penalty) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, getObjectId());
			statement.setInt(2, getTemplate().classId.getId());
			statement.setInt(3, 0);
			statement.setInt(4, 0);
			statement.setDouble(5, getMaxHp());
			statement.setDouble(6, getMaxMp());
			statement.setDouble(7, 0);
			statement.setDouble(8, getMaxHp());
			statement.setDouble(9, getMaxMp());
			statement.setDouble(10, getMaxCp());
			statement.setInt(11, 1);
			statement.setInt(12, 1);
			statement.setInt(13, 1);
			statement.setInt(14, 0);
			statement.executeUpdate();
		}
		catch(final Exception e)
		{
			_log.warn("could not insert char data:", e);
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		return true;
	}

	/**
	 * Retrieve a L2Player from the characters table of the database and add it in _allObjects of the L2World.<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Retrieve the L2Player from the characters table of the database </li>
	 * <li>Add the L2Player object in _allObjects </li>
	 * <li>Set the x,y,z position of the L2Player and make it invisible</li>
	 * <li>Update the overloaded status of the L2Player</li><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 * @return The L2Player loaded from the database
	 */
	private static L2Player restore(final int objectId, String HWID)
	{
		L2Player player = null;
		Connection con = null;
		Statement statement = null;
		Statement statement2 = null;
		ResultSet pl_rset = null;
		ResultSet ps_rset = null;
		try
		{
			// Retrieve the L2Player from the characters table of the database
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.createStatement();
			statement2 = con.createStatement();
			pl_rset = statement.executeQuery("SELECT * FROM `characters` WHERE `obj_Id`='" + objectId + "' LIMIT 1");
			ps_rset = statement2.executeQuery("SELECT `class_id` FROM `character_subclasses` WHERE `char_obj_id`='" + objectId + "' AND `isBase`='1' LIMIT 1");

			if(pl_rset.next() && ps_rset.next())
			{
				final short classId = ps_rset.getShort("class_id");
				final boolean female = pl_rset.getInt("sex") == 1;
				final L2PlayerTemplate template = CharTemplateTable.getInstance().getTemplate(classId, female);

				player = new L2Player(objectId, template);

				player.user_variables = loadVariables(objectId);

				player.setBaseClass(classId);
				player._accountName = pl_rset.getString("account_name");
				if(ConfigProtect.PROTECT_GS_STORE_HWID)
					player._lastHWID = pl_rset.getString("LastHWID");
				player.setName(pl_rset.getString("char_name"));
				player.setAccessLevel(pl_rset.getInt("accesslevel"));

				player.setFace(pl_rset.getByte("face"));
				player.setHairStyle(pl_rset.getByte("hairStyle"));
				player.setHairColor(pl_rset.getByte("hairColor"));
				player.setHeading(pl_rset.getInt("heading"));

				player.setKarma(pl_rset.getInt("karma"));
				player.setPvpKills(pl_rset.getInt("pvpkills"));
				player.setPkKills(pl_rset.getInt("pkkills"));
				player.setLeaveClanTime(pl_rset.getLong("leaveclan") * 1000);
				if(player.getLeaveClanTime() > 0 && player.canJoinClan())
					player.setLeaveClanTime(0);
				player.setDeleteClanTime(pl_rset.getLong("deleteclan") * 1000);
				if(player.getDeleteClanTime() > 0 && player.canCreateClan())
					player.setDeleteClanTime(0);

				player.setNoChannel(pl_rset.getLong("nochannel") * 1000);
				if(player.getNoChannel() > 0 && player.getNoChannelRemained() < 0)
					player.updateNoChannel(0);

				player.setOnlineTime(pl_rset.getLong("onlinetime") * 1000);

				player.setNoble(pl_rset.getBoolean("noble"));
				player.setVarka(pl_rset.getInt("varka"));
				player.setKetra(pl_rset.getInt("ketra"));
				player.setRam(pl_rset.getInt("ram"));

				final int clanId = pl_rset.getInt("clanid");
				if(clanId > 0)
				{
					if(Config.DEBUG)
						System.out.println("Char clan id is loaded as " + clanId);
					player.setClan(ClanTable.getInstance().getClan(clanId));
					player.setPledgeType(pl_rset.getInt("pledge_type"));
					player.setPowerGrade(pl_rset.getInt("pledge_rank"));
					player.setLvlJoinedAcademy(pl_rset.getInt("lvl_joined_academy"));
					player.setApprentice(pl_rset.getInt("apprentice"));
					player.updatePledgeClass();
					if(Config.DEBUG)
						System.out.println("Char clan is loaded");
				}

				player.setDeleteTimer(pl_rset.getInt("deletetime"));

				player.setTitle(pl_rset.getString("title"));

				if(player.getVar("namecolor") == null)
					if(player.isGM())
						player.setNameColor(Config.GM_NAME_COLOUR);
					else if(player.getClanId() != 0 && player.isClanLeader())
						player.setNameColor(Config.CLANLEADER_NAME_COLOUR);
					else
						player.setNameColor(Config.NORMAL_NAME_COLOUR);
				else
					player.setNameColor(Integer.decode("0x" + player.getVar("namecolor")));

				if(player.getVar("titlecolor") != null)
					player.setTitleColor(Integer.decode("0x" + player.getVar("titlecolor")));

				player.setLastAccess(pl_rset.getLong("lastAccess"));
				player.setLogoutTime(pl_rset.getLong("logoutTime"));
				player.getVitality().setStats(pl_rset.getInt("vitPoints"));
				player.getHuntingBonus().setStats(pl_rset.getInt("hunt_points"), pl_rset.getInt("hunt_time"));

				int rec_left_today = 0;
				if(player.getVar("recLeftToday") != null)
					rec_left_today = Integer.parseInt(player.getVar("recLeftToday"));
				player.getRecSystem().setStats(pl_rset.getInt("rec_have"), pl_rset.getInt("rec_left"), rec_left_today, pl_rset.getInt("rec_bonus_time"));

				player._birthday = pl_rset.getLong("birthday") * 1000;
				player.setKeyBindings(pl_rset.getBytes("key_bindings"));
				player.setPcBangPoints(pl_rset.getInt("pcBangPoints"));
				player.setFame(pl_rset.getInt("PRPoints"));

				player.restoreRecipeBook();
				player._teleportBook = TeleportBook.restore(player);

				player.setNotShowBuffAnim(player.getVarB("notShowBuffAnim"));

				if(Config.ALT_SAVE_PRIVATE_STORE || player.getVar("storemode") != null)
				{
					player.restoreTradeList();
					if(player.getVar("storemode") != null)
					{
						player.setPrivateStoreType(Short.parseShort(player.getVar("storemode")));
						player.setSitting(true);
						if(!Config.ALT_SAVE_PRIVATE_STORE && !(player.getVar("offline") != null && Integer.parseInt(player.getVar("storemode")) == 1))
						{
							player.unsetVar("storemode");
							player.unsetVar("selllist");
							player.unsetVar("sellstorename");
							player.unsetVar("buylist");
							player.unsetVar("buystorename");
							player.unsetVar("createlist");
							player.unsetVar("manufacturename");
						}
					}
				}

				if(Config.ALT_FLOATING_RATE_ENABLE)
				{
					player._floatingRate = new FloatingRate(player);
					if(player.getVar("fr_expsp") == null)
					{
						int p = Config.ALT_FLOATING_RATE_POINTS / 4;
						int m = Config.ALT_FLOATING_RATE_POINTS % 4;
						player._floatingRate.setPointExpSp(p + m);
						player.setVar("fr_expsp", p + m);
						player._floatingRate.setPointAdena(p);
						player._floatingRate.setPointDrop(p);
						player._floatingRate.setPointSpoil(p);
						player.setVar("fr_adena", p);
						player.setVar("fr_drop", p);
						player.setVar("fr_spoil", p);
					}
					else
					{
						UserVar uv = player.getUserVars().get("fr_extra");
						if(uv != null)
							player._floatingRate.setExtraPoints(Integer.parseInt(uv.value), uv.expire - System.currentTimeMillis());
						player._floatingRate.setPointExpSp(player.getVarInt("fr_expsp"));
						player._floatingRate.setPointAdena(player.getVarInt("fr_adena"));
						player._floatingRate.setPointDrop(player.getVarInt("fr_drop"));
						player._floatingRate.setPointSpoil(player.getVarInt("fr_spoil"));
						player._floatingRate.recalcRates();
					}
				}

				// ресторит обычных героев и донат героев,если периуд олимпа меняется донат герои пропадают по задумке
				if((Hero.getHeroes() != null && Hero.getHeroes().containsKey(player.getObjectId()) && Hero.getHeroes().get(player.getObjectId()).getInteger("active") == 1) || player.isDonateHero())
					player.setHero(true);

				restoreCharSubClasses(player);
				Quest.playerEnter(player);

				// 10 минут после входа в игру на персонажа не агрятся мобы
				player.setNonAggroTime(System.currentTimeMillis() + 600000);

				// для сервиса виверн - возврат денег если сервер упал во время полета
				String wm = player.getVar("wyvern_moneyback");
				if(wm != null && Integer.parseInt(wm) > 0)
					player.addAdena("wyvern_moneyback", Integer.parseInt(wm), null, false);
				player.unsetVar("wyvern_moneyback");

				// Add the L2Player object in _allObjects
				//L2World.addObject(player);

				// Set the x,y,z position of the L2Player and make it invisible
				player.setXYZInvisible(pl_rset.getInt("x"), pl_rset.getInt("y"), pl_rset.getInt("z"));

				if(player.getVar("jailed") != null)
				{
					long jailTime = player.getVarLong("jailed");

					if(jailTime < 0 || jailTime - System.currentTimeMillis() > 0)
					{
						if(ZoneManager.getInstance().isInsideZone(ZoneType.jail, pl_rset.getInt("x"), pl_rset.getInt("y"), pl_rset.getInt("z")) == null)
							player.setXYZInvisible(-114648, -249384, -2984);

						player.startJail();
					}
					else
					{
						player.setXYZInvisible(17836, 170178, -3507);
						player.unsetVar("jailed");
					}
				}

				PreparedStatement stmt = null;
				ResultSet chars = null;
				try
				{
					stmt = con.prepareStatement("SELECT obj_Id, char_name FROM characters WHERE account_name=? AND obj_Id<>?");
					stmt.setString(1, player._accountName);
					stmt.setInt(2, objectId);
					chars = stmt.executeQuery();
					while(chars.next())
					{
						final Integer charId = chars.getInt("obj_Id");
						final String charName = chars.getString("char_name");
						if(player._chars == null)
						{
							_log.warn("Player _chars is null on restore?? WTF!! " + player + " isDeleting? " + player.isDeleting());
							L2ObjectsStorage.remove(player.getStoredId());
							return null;
						}
						player._chars.put(charId, charName);
					}
				}
				catch(Exception e)
				{
					_log.error("L2Player: restore " + e);
					e.printStackTrace();
				}
				finally
				{
					DbUtils.closeQuietly(stmt, chars);
				}

				//Restore bot reports
				Statement stbot = null;
				PreparedStatement st;
				ResultSet rsbot = null;
				try
				{
					stbot = con.createStatement();
					rsbot = stbot.executeQuery("SELECT `bot_id`, `exp_time` FROM `character_botreports` WHERE `char_id`=" + objectId + " and `exp_time` > " + System.currentTimeMillis() / 1000);
					while(rsbot.next())
						player._botReports.put(rsbot.getInt("bot_id"), rsbot.getLong("exp_time") * 1000);

					st = con.prepareStatement("DELETE FROM `character_botreports` WHERE `char_id`=?");
					st.setInt(1, objectId);
					st.execute();
					st.close();
				}
				catch(Exception e)
				{
					_log.error("L2Player: restore" + e);
					e.printStackTrace();
				}
				finally
				{
					DbUtils.closeQuietly(stbot, rsbot);
				}

				if(!L2World.validCoords(player.getX(), player.getY()) || player.getX() == 0 && player.getY() == 0)
					player.teleToClosestTown();

				player.getInventory().validateItems();
				player.revalidatePenalties();
				player.restoreBlockList();
			}
		}
		catch(final Exception e)
		{
			_log.warn("restore: could not restore char data:", e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(statement2, ps_rset);
			DbUtils.closeQuietly(con, statement, pl_rset);
		}

		if(player != null)
		{
			//player.getInventory().notifyAllEquipped();
			AutoSaveManager.getInstance().addPlayer(player, HWID);
		}

		return player;
	}

	private Future<?> jailTask;

	public void startJail()
	{
		if(jailTask != null)
			jailTask.cancel(true);

		long time = getVarLong("jailed");

		if(time > System.currentTimeMillis())
		{
			jailTask = ThreadPoolManager.getInstance().scheduleGeneral(new JailTask(this, new Location(17836, 170178, -3507)), time - System.currentTimeMillis());
		}
	}

	public void stopJail()
	{
		if(jailTask != null)
			jailTask.cancel(false);

		jailTask = null;
	}

	public boolean isInJail()
	{
		long time = getVarLong("jailed");
		return time < 0 || time - System.currentTimeMillis() > 0;
	}

	/**
	 * Update L2Player stats in the characters table of the database.<BR><BR>
	 */
	public void store()
	{
		Connection con = null;
		PreparedStatement statement = null;
		Statement fs;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(//
					"UPDATE characters SET face=?,hairStyle=?,hairColor=?,heading=?,x=?,y=?,z=?" + //
							",karma=?,pvpkills=?,pkkills=?,rec_have=?,rec_left=?,rec_bonus_time=?,hunt_points=?,hunt_time=?,clanid=?,deletetime=?," + //
							"title=?,accesslevel=?,online=?,leaveclan=?,deleteclan=?,nochannel=?," + //
							"onlinetime=?,noble=?,ketra=?,varka=?,ram=?,pledge_type=?,pledge_rank=?,lvl_joined_academy=?,apprentice=?,key_bindings=?,pcBangPoints=?,char_name=?,logoutTime=?,vitPoints=?,prPoints=? WHERE obj_Id=?");
			statement.setInt(1, getFace());
			statement.setInt(2, getHairStyle());
			statement.setInt(3, getHairColor());
			statement.setInt(4, (short) getHeading());
			if(getStablePoint() == null) // если игрок находится в точке в которой его сохранять не стоит (например на виверне) то сохраняются последние координаты
			{
				statement.setInt(5, getX());
				statement.setInt(6, getY());
				statement.setInt(7, getZ());
			}
			else
			{
				statement.setInt(5, getStablePoint().getX());
				statement.setInt(6, getStablePoint().getY());
				statement.setInt(7, getStablePoint().getZ());
			}
			statement.setInt(8, getKarma());
			statement.setInt(9, getPvpKills());
			statement.setInt(10, getPkKills());
			statement.setInt(11, _recommendSystem.getRecommendsHave());
			statement.setInt(12, _recommendSystem.getRecommendsLeft());
			statement.setInt(13, _recommendSystem.getBonusTime());
			statement.setInt(14, _huntingBonus.getPoints());
			statement.setInt(15, _huntingBonus.getTime());
			statement.setInt(16, getClanId());
			statement.setInt(17, getDeleteTimer());
			statement.setString(18, getTitle());
			statement.setInt(19, _accessLevel);
			statement.setInt(20, isOnline() ? 1 : 0);
			statement.setLong(21, getLeaveClanTime() / 1000);
			statement.setLong(22, getDeleteClanTime() / 1000);
			statement.setLong(23, _NoChannel > 0 ? getNoChannelRemained() / 1000 : _NoChannel);
			statement.setLong(24, onlineBeginTime > 0 ? (onlineTime + System.currentTimeMillis() - onlineBeginTime) / 1000 : onlineTime / 1000);
			statement.setInt(25, isNoble() ? 1 : 0);
			statement.setInt(26, getKetra());
			statement.setInt(27, getVarka());
			statement.setInt(28, getRam());
			statement.setInt(29, getPledgeType());
			statement.setInt(30, getPowerGrade());
			statement.setInt(31, getLvlJoinedAcademy());
			statement.setInt(32, getApprentice());
			statement.setBytes(33, getKeyBindings());
			statement.setInt(34, getPcBangPoints());
			statement.setString(35, getName());
			statement.setLong(36, System.currentTimeMillis() / 1000);
			statement.setInt(37, getVitality() == null ? 20000 : getVitality().getPoints());
			statement.setInt(38, getFame());
			statement.setInt(39, getObjectId());

			statement.execute();
			DbUtils.closeQuietly(statement);
			statement = null;
			L2World.increaseUpdatePlayerBase();

			try
			{
				TextBuilder sb = TextBuilder.newInstance();
				fs = con.createStatement();
				for(Integer objId : _botReports.keySet())
				{
					fs.addBatch(sb.append("REPLACE INTO `character_botreports` SET `bot_id`=").append(objId).append(", `exp_time`=").append(_botReports.get(objId) / 1000).append(", `char_id`=").append(_objectId).toString());
					sb.clear();
				}
				TextBuilder.recycle(sb);
				fs.executeBatch();
				DbUtils.closeQuietly(statement);
			}
			catch(ConcurrentModificationException e)
			{
			}

			synchronized(this)
			{
				storeEffects();
				storeDisableSkills();
				storeCharSubClasses();
				storeBlockList();
			}
		}
		catch(final Exception e)
		{
			_log.warn("store: could not store char data: " + e + " heading: " + getHeading());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	/**
	 * @return True if the L2Player is on line.<BR><BR>
	 */
	public boolean isOnline()
	{
		return _isOnline;
	}

	/**
	 * Add a skill to the L2Player _skills and its Func objects to the calculator set of the L2Player and save update in the character_skills table of the database.<BR><BR>
	 * <p/>
	 * <B><U> Concept</U> :</B><BR><BR>
	 * All skills own by a L2Player are identified in <B>_skills</B><BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Replace oldSkill by newSkill or Add the newSkill </li>
	 * <li>If an old skill has been replaced, remove all its Func objects of L2Character calculator set</li>
	 * <li>Add Func objects of newSkill to the calculator set of the L2Character </li><BR><BR>
	 *
	 * @param newSkill The L2Skill to add to the L2Character
	 * @return The L2Skill replaced or null if just added a new L2Skill
	 */
	public L2Skill addSkill(final L2Skill newSkill, final boolean store)
	{
		// Add a skill to the L2Player _skills and its Func objects to the calculator set of the L2Player
		final L2Skill oldSkill = addSkill(newSkill);

		// Add or update a L2Player skill in the character_skills table of the database
		if(store)
        {
			storeSkill(newSkill, oldSkill);
            Object[] script_args = new Object[] { this, newSkill, oldSkill };
            for(ScriptClassAndMethod handler : Scripts.onPlayerSkillAdd)
                callScripts(handler.scriptClass, handler.method, script_args);
        }

		return oldSkill;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public L2Skill addSkill(L2Skill newSkill)
	{
		L2Skill oldSkill = super.addSkill(newSkill);

		if(!newSkill.isPassive() && newSkill.getMinReuseDelayOnSkillAdd() > 0)
		{
			int timePenalty = newSkill.getMinReuseDelayOnSkillAdd() * 1000;
			TimeStamp sts = getSkillReuseTimeStamp(newSkill.getId());
			if(sts != null)
			{
				long newEndTime = System.currentTimeMillis() + timePenalty;
				long curEndTime = sts.getEndTime();
				if(newEndTime > curEndTime)
					disableSkill(newSkill.getId(), timePenalty);
			}
			else
				disableSkill(newSkill.getId(), timePenalty);
		}

		return oldSkill;
	}

	/**
	 * Remove a skill from the L2Character and its Func objects from calculator set of the L2Character and save update in the character_skills table of the database.<BR><BR>
	 * <p/>
	 * <B><U> Concept</U> :</B><BR><BR>
	 * All skills own by a L2Character are identified in <B>_skills</B><BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Remove the skill from the L2Character _skills </li>
	 * <li>Remove all its Func objects from the L2Character calculator set</li><BR><BR>
	 * <p/>
	 * <B><U> Overriden in </U> :</B><BR><BR>
	 * <li> L2Player : Save update in the character_skills table of the database</li><BR><BR>
	 *
	 * @param skill The L2Skill to remove from the L2Character
	 * @return The L2Skill removed
	 */
	public L2Skill removeSkill(L2Skill skill, boolean fromDB)
	{
		return removeSkill(skill, fromDB, false);
	}

	public L2Skill removeSkill(L2Skill skill, boolean fromDB, boolean force)
	{
		// Remove a skill from the L2Character and its Func objects from calculator set of the L2Character
		L2Skill oldSkill = super.removeSkill(skill);

		if(!fromDB)
			return oldSkill;

		if(oldSkill == null)
			oldSkill = skill;

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			// Remove or update a L2Player skill from the character_skills table of the database
			con = DatabaseFactory.getInstance().getConnection();
			if(oldSkill != null || force)
			{
				statement = con.prepareStatement("DELETE FROM character_skills WHERE skill_id=? AND char_obj_id=? AND class_index=?");
				statement.setInt(1, oldSkill.getId());
				statement.setInt(2, getObjectId());
				statement.setInt(3, getActiveClass());
				statement.execute();
			}
		}
		catch(final Exception e)
		{
			_log.warn("Error could not delete Skill:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}

		return oldSkill;
	}

	public void restoreSummon()
	{
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		L2SummonInstance summon = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			stmt = con.prepareStatement("SELECT * FROM character_summons WHERE char_obj_id = ?");
			stmt.setInt(1, getObjectId());
			rset = stmt.executeQuery();
			if(rset.next())
			{
				int npcId = rset.getInt("npc_id");
				L2NpcTemplate template = NpcTable.getTemplate(npcId);
				if(template != null)
				{
					summon = new L2SummonInstance(IdFactory.getInstance().getNextId(), template, this, rset.getInt("max_life_time"), rset.getInt("item_id"), rset.getInt("item_count"), rset.getInt("item_delay"));

					summon.setTitle(getName());
					summon.setExpPenalty(rset.getFloat("penalty"));
					summon.setExp(Experience.LEVEL[summon.getLevel()]);
					int currMp = rset.getInt("curr_mp");
					int currHp = rset.getInt("curr_hp");
					summon.setLifeTime(rset.getInt("life_time"));
					summon.setHeading(getHeading());
					summon.setRunning();
					stmt.close();
					rset.close();
					summon.setCurrentHp(currHp);
					summon.setCurrentMp(currMp);

					setPet(summon);
					//L2World.addObject(summon);

					summon.spawnMe(GeoEngine.findPointToStay(getX(), getY(), getZ(), 40, 40, getReflection()));

					if(summon.getSkillLevel(4140) > 0)
						summon.altUseSkill(SkillTable.getInstance().getInfo(4140, summon.getSkillLevel(4140)), this, null);

					if(summon.getName().equalsIgnoreCase("Shadow"))
						summon.addStatFunc(new FuncAdd(Stats.ABSORB_DAMAGE_PERCENT, 0x40, this, 15));

					summon.setFollowStatus(true);
					summon.broadcastPetInfo();
					summon.setShowSpawnAnimation(false);
				}

				stmt.close();
				stmt = con.prepareStatement("DELETE FROM character_summons WHERE char_obj_id = ?");
				stmt.setInt(1, getObjectId());
				stmt.execute();
			}
		}
		catch(final Exception e)
		{
			_log.warn("Error could not store Skills:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, stmt, rset);
		}

		if(summon == null)
		{
			L2PetInstance pet = L2PetInstance.restore(this);
			if(pet != null)
			{
				pet.setRunning();
				setPet(pet);
				//L2World.addObject(pet);

				if(pet.getCurrentFed() < pet.getMaxMeal() * 0.36)
					sendPacket(Msg.YOUR_PET_IS_VERY_HUNGRY_PLEASE_BE_CAREFUL);

				pet.setTitle(getName());
				pet.spawnMe(GeoEngine.findPointToStay(getX(), getY(), getZ(), 40, 40, getReflection()));
				pet.broadcastPetInfo();
				pet.setShowSpawnAnimation(false);
				pet.startFeed();
				pet.setFollowStatus(true);
			}
		}
	}

	private void storeSummon()
	{
		if(!(_summon instanceof L2SummonInstance) || _summon.isDead())
			return;

		Connection con = null;
		PreparedStatement stmt = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			stmt = con.prepareStatement("REPLACE INTO character_summons VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			stmt.setInt(1, getObjectId());
			stmt.setInt(2, _summon.getNpcId());
			stmt.setInt(3, (int) _summon.getCurrentHp());
			stmt.setInt(4, (int) _summon.getCurrentMp());
			stmt.setInt(5, _summon.getCurrentFed());
			stmt.setInt(6, _summon.getMaxMeal());
			stmt.setInt(7, ((L2SummonInstance) _summon).getItemConsumeIdInTime());
			stmt.setInt(8, ((L2SummonInstance) _summon).getItemConsumeCountInTime());
			stmt.setInt(9, ((L2SummonInstance) _summon).getItemConsumeDelay());
			stmt.setDouble(10, _summon.getExpPenalty());
			stmt.execute();
			stmt.close();
		}
		catch(final Exception e)
		{
			_log.warn("Error could not store Skills:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, stmt);
		}
	}

	/**
	 * Add or update a L2Player skill in the character_skills table of the database.<BR><BR>
	 */
	private void storeSkill(final L2Skill newSkill, final L2Skill oldSkill)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			if(oldSkill != null && newSkill != null)
			{
				statement = con.prepareStatement("UPDATE character_skills SET skill_level=? WHERE skill_id=? AND char_obj_id=? AND class_index=?");
				statement.setInt(1, newSkill.getLevel());
				statement.setInt(2, oldSkill.getId());
				statement.setInt(3, getObjectId());
				statement.setInt(4, getActiveClass());
				statement.execute();
			}
			else if(newSkill != null)
			{
				statement = con.prepareStatement("REPLACE INTO character_skills (char_obj_id,skill_id,skill_level,skill_name,class_index) values(?,?,?,?,?)");
				statement.setInt(1, getObjectId());
				statement.setInt(2, newSkill.getId());
				statement.setInt(3, newSkill.getLevel());
				statement.setString(4, newSkill.getName());
				statement.setInt(5, getActiveClass());
				statement.execute();
			}
			else
				_log.warn("could not store new skill. its NULL");
		}
		catch(final Exception e)
		{
			_log.warn("Error could not store Skills:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	/**
	 * Retrieve from the database all skills of this L2Player and add them to _skills.<BR><BR>
	 */
	private void restoreSkills()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;

		try
		{
			// Retrieve all skills of this L2Player from the database
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("SELECT skill_id,skill_level FROM character_skills WHERE char_obj_id=? AND class_index=?");
			statement.setInt(1, getObjectId());
			statement.setInt(2, getActiveClass());

			rset = statement.executeQuery();

			// Go though the recordset of this SQL query
			while(rset.next())
			{
				final int id = rset.getInt("skill_id");
				final int level = rset.getInt("skill_level");
				boolean allow = false;

				if(id > 9000 && id < 10000)
					continue; // fake skills for base stats

				// Create a L2Skill object for each record
				final L2Skill skill = SkillTable.getInstance().getInfo(id, level);

				if(skill == null)
					continue;

				for(int i = 0; i < Config.VipSkillsList.length; i++)
					if(skill.getId() == Config.VipSkillsList[i])
						allow = true;

				// Remove skill if not possible
				if(Config.CHECK_SKILLS_POSSIBLE && !isGM() && !skill.isCommon() && !SkillTreeTable.getInstance().isSkillPossible(this, skill.getId(), skill._level) && !allow)
				{
					removeSkill(skill, true, true);
					removeSkillFromShortCut(skill.getId());
					GmListTable.broadcastMessageToGMs("has skill " + skill.getName());
					continue;
				}

				// Add the L2Skill object to the L2Character _skills and its Func objects to the calculator set of the L2Character
				super.addSkill(skill);
			}

			// Restore noble skills
			if(_noble)
				SkillTable.giveNobleSkills(this);

			// Restore Hero skills at main class only
			if(_hero && getBaseClass() == getActiveClass())
			{
				super.addSkill(SkillTable.getInstance().getInfo(395, 1));
				super.addSkill(SkillTable.getInstance().getInfo(396, 1));
				super.addSkill(SkillTable.getInstance().getInfo(1374, 1));
				super.addSkill(SkillTable.getInstance().getInfo(1375, 1));
				super.addSkill(SkillTable.getInstance().getInfo(1376, 1));
			}

			if(_clanId > 0)
			{
				L2Clan clan = getClan();
				// Restore clan leader siege skills
				if(clan.getLeaderId() == getObjectId() && clan.getLevel() > 3)
					SiegeManager.addSiegeSkills(this);

				// Restore clan skills
				clan.addAndShowSkillsToPlayer(this);
			}

			// Give dwarven craft skill
			if(_activeClass >= 53 && _activeClass <= 57 || _activeClass == 117 || _activeClass == 118)
				super.addSkill(SkillTable.getInstance().getInfo(1321, 1));
			super.addSkill(SkillTable.getInstance().getInfo(1322, 1));

			if(Config.UNSTUCK_SKILL && getSkillLevel(1050) < 0)
				addSkill(SkillTable.getInstance().getInfo(2099, 1), false);

            Object[] script_args = new Object[] { this };
            for(ScriptClassAndMethod handler : Scripts.onPlayerSkillsRestored)
                callScripts(handler.scriptClass, handler.method, script_args);
		}
		catch(final Exception e)
		{
			_log.warn("count not restore skills:" + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public void storeDisableSkills()
	{
		if(!_disabledSkillsLoaded)
			return;

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			int obj_id = getObjectId();
			int class_index = getActiveClass();
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_skills_save WHERE char_obj_id = ? AND class_index=?");
			statement.setInt(1, obj_id);
			statement.setInt(2, class_index);
			statement.execute();
			DbUtils.closeQuietly(statement);

			if(disabledSkills != null)
			{
				statement = con.prepareStatement("INSERT INTO character_skills_save (char_obj_id, skill_id, class_index, end_time, reuse_delay_org) VALUES (?,?,?,?,?)");
				for(TimeStamp timeStamp : disabledSkills.values())
				{
					statement.setInt(1, obj_id);
					statement.setInt(2, timeStamp.getSkillId());
					statement.setInt(3, class_index);
					statement.setLong(4, timeStamp.getEndTime());
					statement.setLong(5, timeStamp.getReuseTotal());
					statement.execute();
				}
				DbUtils.closeQuietly(statement);
			}
		}
		catch(final Exception e)
		{
			_log.warn("Could not store disable skills data: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con);
		}
	}

	public void storeEffects()
	{
		if(!_effectsLoaded)
			return;

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("DELETE FROM character_effects_save WHERE char_obj_id = ? AND class_index=?");
			statement.setInt(1, getObjectId());
			statement.setInt(2, getActiveClass());
			statement.execute();
			DbUtils.closeQuietly(statement);

			int order = 0;
			statement = con.prepareStatement("INSERT IGNORE INTO `character_effects_save` SET `char_obj_id`=?,`skill_id`=?,`skill_level`=?,`duration`=?,`order`=?,`class_index`=?");
			for(L2Effect effect : getAllEffects())
				if(effect != null && effect.isInUse() && effect.getSkill().isSaveable() && !effect.getSkill().isToggle())// && effect.getTimeLeft() > 0)
				{
					statement.setInt(1, getObjectId());
					statement.setInt(2, effect.getSkillId());
					statement.setInt(3, effect.getSkillLevel());
					statement.setLong(4, effect.getTimeLeft());
					statement.setInt(5, order);
					statement.setInt(6, getActiveClass());
					statement.execute();
					if((effect = effect.getNext()) != null)
					{
						statement.setInt(1, getObjectId());
						statement.setInt(2, effect.getSkillId());
						statement.setInt(3, effect.getSkillLevel());
						statement.setLong(4, effect.getTimeLeft());
						statement.setInt(5, order);
						statement.setInt(6, getActiveClass());
						statement.execute();
					}
					order++;
				}
		}
		catch(final Exception e)
		{
			_log.warn("Could not store active effects data: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}

		updateEffectIcons();
	}

	private boolean _effectsLoaded = false;

	public void restoreEffects()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("SELECT `skill_id`,`skill_level`,`duration` FROM `character_effects_save` WHERE `char_obj_id`=? AND `class_index`=? ORDER BY `order` ASC");
			statement.setInt(1, getObjectId());
			statement.setInt(2, getActiveClass());

			rset = statement.executeQuery();
			while(rset.next())
			{
				int skillId = rset.getInt("skill_id");
				int skillLvl = rset.getInt("skill_level");
				long duration = rset.getLong("duration");

				L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLvl);
				if(skill != null)
				{
					EffectTemplate et = skill.getTimedEffectTemplate();
					if(et == null)
						continue;
					Env env = new Env(this, this, skill);
					L2Effect effect = et.getEffect(env);
					effect.setAbnormalTime(duration);
					addEffect(effect);
				}
			}

			DbUtils.closeQuietly(statement, rset);

			statement = con.prepareStatement("DELETE FROM character_effects_save WHERE char_obj_id = ? AND class_index=?");
			statement.setInt(1, getObjectId());
			statement.setInt(2, getActiveClass());
			statement.executeUpdate();
		}
		catch(final Exception e)
		{
			_log.warn("Could not restore active effects data: " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
			_effectsLoaded = true;
		}

		updateEffectIcons();
		broadcastUserInfo(true);
	}

	private boolean _disabledSkillsLoaded = false;

	public void restoreDisableSkills()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("SELECT skill_id,end_time,reuse_delay_org FROM character_skills_save WHERE char_obj_id=? AND class_index=?");
			statement.setInt(1, getObjectId());
			statement.setInt(2, getActiveClass());

			rset = statement.executeQuery();
			while(rset.next())
			{
				int skillId = rset.getInt("skill_id");
				int skillLevel = Math.max(getSkillLevel(skillId), 1);
				long endTime = rset.getLong("end_time");
				long rDelayOrg = rset.getLong("reuse_delay_org");
				long curTime = System.currentTimeMillis();

				L2Skill skill = null;
				if(skillId > 0)
					skill = SkillTable.getInstance().getInfo(skillId, skillLevel);

				if((skill != null || skillId < 0) && endTime - curTime > 10)
				{
					disableSkill(skillId, rDelayOrg, endTime);
				}
			}
			DbUtils.closeQuietly(statement, rset);

			statement = con.prepareStatement("DELETE FROM character_skills_save WHERE char_obj_id = ? AND class_index=?");
			statement.setInt(1, getObjectId());
			statement.setInt(2, getActiveClass());
			statement.executeUpdate();
		}
		catch(Exception e)
		{
			_log.warn("Could not restore active skills data for " + getObjectId() + "/" + getActiveClass());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
			_disabledSkillsLoaded = true;
		}

		updateEffectIcons();
	}

	/**
	 * Retrieve from the database all Henna of this L2Player, add them to _henna and calculate stats of the L2Player.<BR><BR>
	 */
	private void restoreHenna()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("select slot, symbol_id from character_hennas where char_obj_id=? AND class_index=?");
			statement.setInt(1, getObjectId());
			statement.setInt(2, getActiveClass());
			rset = statement.executeQuery();

			for(int i = 0; i < 3; i++)
				_henna[i] = null;

			while(rset.next())
			{
				final int slot = rset.getInt("slot");
				if(slot < 1 || slot > 3)
					continue;

				final int symbol_id = rset.getInt("symbol_id");

				L2HennaInstance sym;

				if(symbol_id != 0)
				{
					final L2Henna tpl = HennaTable.getInstance().getTemplate(symbol_id);
					if(tpl != null)
					{
						sym = new L2HennaInstance(tpl);
						_henna[slot - 1] = sym;
					}
				}
			}
		}
		catch(final Exception e)
		{
			_log.warn("could not restore henna: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		// Calculate Henna modifiers of this L2Player
		recalcHennaStats();

	}

	/**
	 * @return the number of Henna empty slot of the L2Player.<BR><BR>
	 */
	public int getHennaEmptySlots()
	{
		int totalSlots = Math.min(getClassId().getLevel(), 3);
		for(int i = 0; i < 3; i++)
			if(_henna[i] != null)
				totalSlots--;

		if(totalSlots <= 0)
			return 0;

		return totalSlots;

	}

	/**
	 * Remove a Henna of the L2Player, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2Player.<BR><BR>
	 */
	public boolean removeHenna(int slot, L2NpcInstance npc)
	{
		if(slot < 1 || slot > 3)
			return false;

		slot--;

		if(_henna[slot] == null)
			return false;

		final L2HennaInstance henna = _henna[slot];
		final short dyeID = henna.getItemIdDye();

		// Added by Tempy - 10 Aug 05
		// Gives amount equal to half of the dyes needed for the henna back.
		_henna[slot] = null;

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_hennas where char_obj_id=? and slot=? and class_index=?");
			statement.setInt(1, getObjectId());
			statement.setInt(2, slot + 1);
			statement.setInt(3, getActiveClass());
			statement.execute();
		}
		catch(final Exception e)
		{
			_log.warn("could not remove char henna: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}

		// Calculate Henna modifiers of this L2Player
		recalcHennaStats();

		// Send Server->Client HennaInfo packet to this L2Player
		sendPacket(new HennaInfo(this));

		// Send Server->Client UserInfo packet to this L2Player
		sendUserInfo(false);

		// Add the recovered dyes to the player's inventory and notify them.
		addItem("removeHenna", dyeID, henna.getAmountDyeRequire() / 2, npc, true);

		return true;
	}

	/**
	 * Add a Henna to the L2Player, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2Player.<BR><BR>
	 *
	 * @param henna L2HennaInstance для добавления
	 */
	public boolean addHenna(final L2HennaInstance henna)
	{
		if(getHennaEmptySlots() == 0)
		{
			sendPacket(Msg.NO_SLOT_EXISTS_TO_DRAW_THE_SYMBOL);
			return false;
		}

		// int slot = 0;
		for(int i = 0; i < 3; i++)
			if(_henna[i] == null)
			{
				_henna[i] = henna;

				// Calculate Henna modifiers of this L2Player
				recalcHennaStats();

				Connection con = null;
				PreparedStatement statement = null;
				try
				{
					con = DatabaseFactory.getInstance().getConnection();
					statement = con.prepareStatement("INSERT INTO `character_hennas` (char_obj_id, symbol_id, slot, class_index) VALUES (?,?,?,?)");
					statement.setInt(1, getObjectId());
					statement.setInt(2, henna.getSymbolId());
					statement.setInt(3, i + 1);
					statement.setInt(4, getActiveClass());
					statement.execute();
				}
				catch(final Exception e)
				{
					_log.warn("could not save char henna: " + e);
				}
				finally
				{
					DbUtils.closeQuietly(con, statement);
				}

				// Send Server->Client HennaInfo packet to this L2Player
				final HennaInfo hi = new HennaInfo(this);
				sendPacket(hi);

				// Send Server->Client UserInfo packet to this L2Player
				sendUserInfo(true);

				return true;
			}

		return false;
	}

	/**
	 * Calculate Henna modifiers of this L2Player.<BR><BR>
	 */
	private void recalcHennaStats()
	{
		_hennaINT = 0;
		_hennaSTR = 0;
		_hennaCON = 0;
		_hennaMEN = 0;
		_hennaWIT = 0;
		_hennaDEX = 0;

		for(int i = 0; i < 3; i++)
		{
			if(_henna[i] == null)
				continue;
			_hennaINT += _henna[i].getStatINT();
			_hennaSTR += _henna[i].getStatSTR();
			_hennaMEN += _henna[i].getStatMEM();
			_hennaCON += _henna[i].getStatCON();
			_hennaWIT += _henna[i].getStatWIT();
			_hennaDEX += _henna[i].getStatDEX();
		}

		if(_hennaINT > 5)
			_hennaINT = 5;
		if(_hennaSTR > 5)
			_hennaSTR = 5;
		if(_hennaMEN > 5)
			_hennaMEN = 5;
		if(_hennaCON > 5)
			_hennaCON = 5;
		if(_hennaWIT > 5)
			_hennaWIT = 5;
		if(_hennaDEX > 5)
			_hennaDEX = 5;

	}

	/**
	 * @param slot id слота у перса
	 * @return the Henna of this L2Player corresponding to the selected slot.<BR><BR>
	 */
	public L2HennaInstance getHenna(final int slot)
	{
		if(slot < 1 || slot > 3)
			return null;
		return _henna[slot - 1];
	}

	/**
	 * @return the INT Henna modifier of this L2Player.<BR><BR>
	 */
	public int getHennaStatINT()
	{
		return _hennaINT;
	}

	/**
	 * @return the STR Henna modifier of this L2Player.<BR><BR>
	 */
	public int getHennaStatSTR()
	{
		return _hennaSTR;
	}

	/**
	 * @return the CON Henna modifier of this L2Player.<BR><BR>
	 */
	public int getHennaStatCON()
	{
		return _hennaCON;
	}

	/**
	 * @return the MEN Henna modifier of this L2Player.<BR><BR>
	 */
	public int getHennaStatMEN()
	{
		return _hennaMEN;
	}

	/**
	 * @return the WIT Henna modifier of this L2Player.<BR><BR>
	 */
	public int getHennaStatWIT()
	{
		return _hennaWIT;
	}

	/**
	 * @return the DEX Henna modifier of this L2Player.<BR><BR>
	 */
	public int getHennaStatDEX()
	{
		return _hennaDEX;
	}

	/**
	 * Reduce Item quantity of the L2Player Inventory.<BR><BR>
	 */
	@Override
	public boolean consumeItem(final int itemConsumeId, final int itemCount, boolean sendMessage)
	{
		return destroyItemByItemId("Consume", itemConsumeId, itemCount, null, sendMessage);
	}

	/**
	 * @return True if the L2Player is a Mage.<BR><BR>
	 */
	@Override
	public boolean isMageClass()
	{
		return _template.baseMAtk > 3;
	}

	/**
	 * Проверяет, можно ли приземлиться в этой зоне.
	 *
	 * @return можно ли приземлится
	 */
	public boolean checkLandingState()
	{
		if(isInZone(ZoneType.no_landing))
			return false;

		SiegeUnit su = ResidenceManager.getInstance().getBuildingByResidenceCoord(getX(), getY());
		return su == null || _clanId > 0 && isClanLeader() && (su.getSiege() == null || !su.getSiege().isInProgress() || su.getOwnerId() == getClanId());
	}

	public void sendDisarmMessage(L2ItemInstance wpn)
	{
		if(wpn.getEnchantLevel() > 0)
		{
			SystemMessage sm = new SystemMessage(SystemMessage.EQUIPMENT_OF__S1_S2_HAS_BEEN_REMOVED);
			sm.addNumber(wpn.getEnchantLevel());
			sm.addItemName(wpn.getItemId());
			sendPacket(sm);
		}
		else
		{
			SystemMessage sm = new SystemMessage(SystemMessage.S1__HAS_BEEN_DISARMED);
			sm.addItemName(wpn.getItemId());
			sendPacket(sm);
		}
	}

	/**
	 * Send a Server->Client packet UserInfo to this L2Player and CharInfo to all L2Player in its _KnownPlayers.<BR><BR>
	 * <p/>
	 * <B><U> Concept</U> :</B><BR><BR>
	 * Others L2Player in the detection area of the L2Player are identified in <B>_knownPlayers</B>.
	 * In order to inform other players of this L2Player state modifications, server just need to go through _knownPlayers to send Server->Client Packet<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Send a Server->Client packet UserInfo to this L2Player (Public and Private Data)</li>
	 * <li>Send a Server->Client packet CharInfo to all L2Player in _KnownPlayers of the L2Player (Public data only)</li><BR><BR>
	 * <p/>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : DON'T SEND UserInfo packet to other players instead of CharInfo packet.
	 * Indeed, UserInfo packet contains PRIVATE DATA as MaxHP, STR, DEX...</B></FONT><BR><BR>
	 */
	@Override
	public void updateAbnormalEffect()
	{
		sendChanges();
	}

	/**
	 * Disable the Inventory and create a new task to enable it after 1.5s.<BR><BR>
	 */
	public void tempInvetoryDisable()
	{
		_inventoryDisable = true;

		ThreadPoolManager.getInstance().scheduleGeneral(new InventoryEnable(), 1500);
	}

	/**
	 * @return True if the Inventory is disabled.<BR><BR>
	 */
	public boolean isInventoryDisabled()
	{
		return _inventoryDisable;
	}

	class InventoryEnable implements Runnable
	{
		public void run()
		{
			_inventoryDisable = false;
		}
	}

	/**
	 * Disable the warehouse
	 */
	public void tempWhDisable()
	{
		_whDisable = true;

	}

	/**
	 * Enable the warehouse>
	 */
	public void tempWhEnable()
	{
		_whDisable = false;

	}

	/**
	 * return True if the warehouse is disabled.<BR><BR>
	 */
	public boolean isWhDisabled()
	{
		return _whDisable;
	}

	/**
	 * Устанавливает тип используемого склада.
	 *
	 * @param type тип склада:<BR>
	 *             <ul>
	 *             <li>WarehouseType.PRIVATE
	 *             <li>WarehouseType.CLAN
	 *             <li>WarehouseType.CASTLE
	 *             <li>WarehouseType.FREIGHT
	 *             </ul>
	 */
	public void setUsingWarehouseType(final WarehouseType type)
	{
		_usingWHType = type;
	}

	/**
	 * Возвращает тип используемого склада.
	 *
	 * @return null или тип склада:<br>
	 *         <ul>
	 *         <li>WarehouseType.PRIVATE
	 *         <li>WarehouseType.CLAN
	 *         <li>WarehouseType.CASTLE
	 *         <li>WarehouseType.FREIGHT
	 *         </ul>
	 */
	public WarehouseType getUsingWarehouseType()
	{
		return _usingWHType;
	}
	
	public FastList<L2CubicInstance> getCubics()
	{
		return _cubics;
	}

	//public GArray<AbnormalVisualEffect> getAbnormalEffects()
	//{
	//	return _abnormalEffects;
	//}
    //
	/**
	 * Add a L2CubicInstance to the L2Player _cubics.<BR><BR>
	 */
	public synchronized void addCubic(int id, int level, boolean givenByOther)
	{
		int mastery = Math.max(getSkillLevel(L2Skill.SKILL_CUBIC_MASTERY), 0);

		CubicTemplate ct = CubicManager.getInstance().getCubicTemplate(id, level);
		if(ct == null)
			return;

		L2CubicInstance cubic = new L2CubicInstance(ct, this, givenByOther);

		for(int i = 0; i < _cubics.size(); i++)
		{
			L2CubicInstance cub = _cubics.get(i);
			if(cub != null && cub.getId() == id)
			{
				cub.deleteMe();
				_cubics.remove(cub);
				break;
			}
		}

		if(_cubics.size() > mastery)
		{
			L2CubicInstance cub = _cubics.removeFirst();
			if(cub != null)
				cub.deleteMe();
		}
		_cubics.add(cubic);
	}

	/**
	 * Remove a L2CubicInstance from the L2Player _cubics.<BR><BR>
	 */
	public void delCubic(int id)
	{
		for(L2CubicInstance cubic : _cubics)
			if(cubic.getId() == id)
			{
				_cubics.remove(cubic);
				break;
			}
	}

	@Override
	public String toString()
	{
		return "player '" + getName() + "'";
	}

	/**
	 * @return the modifier corresponding to the Enchant Effect of the Active Weapon (Min : 127).<BR><BR>
	 */
	public int getEnchantEffect()
	{
		final L2ItemInstance wpn = getActiveWeaponInstance();

		if(wpn == null)
			return 0;

		if(Config.MAXENCHANT_FOR_VISPLAYER > wpn.getEnchantLevel())
			return Math.min(127, wpn.getEnchantLevel());
		else
			return Math.min(127, Config.MAXENCHANT_FOR_VISPLAYER);
	}

	/**
	 * Устанавливаем NPC который показал нам MultiSell
	 */
	public void setLastMultisellNpc(final L2NpcInstance npc)
	{
		_lastMultisellNpc = npc;
	}

	/**
	 * @return Последний NPC который показывал Мультиселл для игрока
	 */
	public L2NpcInstance getLastMultisellNpc()
	{
		return _lastMultisellNpc;
	}

	/**
	 * Set the _lastFolkNpc of the L2Player corresponding to the last Folk witch one the player talked.<BR><BR>
	 */
	public void setLastNpc(final L2NpcInstance npc)
	{
		_lastFolkNpc = npc;
	}

	/**
	 * @return the _lastFolkNpc of the L2Player corresponding to the last Folk witch one the player talked.<BR><BR>
	 */
	public L2NpcInstance getLastNpc()
	{
		return _lastFolkNpc;
	}

	@Override
	public boolean unChargeShots(boolean spirit)
	{
		L2ItemInstance weapon = getActiveWeaponInstance();
		if(weapon == null)
			return false;

		if(spirit)
			weapon.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
		else
			weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);

		AutoShot();
		return true;
	}

	public boolean unChargeFishShot()
	{
		L2ItemInstance weapon = getActiveWeaponInstance();
		if(weapon == null)
			return false;
		weapon.setChargedFishshot(false);
		AutoShot();
		return true;
	}

	public void AutoShot()
	{
		synchronized(_activeSoulShots)
		{
			for(Integer e : _activeSoulShots)
			{
				if(e == null)
					continue;
				L2ItemInstance item = getInventory().getItemByItemId(e);
				if(item == null)
				{
					_activeSoulShots.remove(e);
					continue;
				}
				IItemHandler handler = ItemHandler.getInstance().getItemHandler(e);
				if(handler == null)
					continue;
				if(!handler.useItem(this, item))
				{
					_activeSoulShots.remove(e);
					sendPacket(new ExAutoSoulShot(item.getItemId(), false));
					sendPacket(new SystemMessage(SystemMessage.DUE_TO_INSUFFICIENT_S1_THE_AUTOMATIC_USE_FUNCTION_HAS_BEEN_CANCELLED).addItemName(item.getItemId()));
				}
			}
		}
	}

	public boolean getChargedFishShot()
	{
		L2ItemInstance weapon = getActiveWeaponInstance();
		return weapon != null && weapon.getChargedFishshot();
	}

	@Override
	public boolean getChargedSoulShot()
	{
		L2ItemInstance weapon = getActiveWeaponInstance();
		return weapon != null && weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT;
	}

	@Override
	public int getChargedSpiritShot()
	{
		L2ItemInstance weapon = getActiveWeaponInstance();
		if(weapon == null)
			return 0;
		return weapon.getChargedSpiritshot();
	}

	public void addAutoSoulShot(Integer itemId)
	{
		_activeSoulShots.add(itemId);
	}

	public void removeAutoSoulShot(Integer itemId)
	{
		_activeSoulShots.remove(itemId);
	}

	public ConcurrentSkipListSet<Integer> getAutoSoulShot()
	{
		return _activeSoulShots;
	}

	public void setInvisible(boolean vis)
	{
		_invisible = vis;
	}

	public boolean isInvisible()
	{
		return _invisible;
	}

	public int getNpcDialogEndTime()
	{
		return this._npcDialogEndTime;
	}

	public void setNpcDialogEndTime(final int val)
	{
		this._npcDialogEndTime = val;
	}
	
	public int getClanPrivileges()
	{
		if(_clanId <= 0 || _powerGrade < 1 || _powerGrade > 9)
			return 0;
		else if(isClanLeader())
			return L2Clan.CP_ALL;
		else
		{
			RankPrivs privs = getClan().getRankPrivs(_powerGrade);
			if(privs != null)
				return privs.getPrivs();
			return 0;
		}
	}

	public synchronized boolean enterObserverMode(int x, int y, int z)
	{
		_observRegion = L2World.getRegion(x, y, z);
		if(_observRegion == null)
			return false;

		// Если цель слишком близко - в одном регионе, будут конфликты.
		if(getCurrentRegion() == null || getCurrentRegion().equals(_observRegion))
			return false;

		setStablePoint(getLoc());
		setTarget(null);
		stopMove();
		block();
		setIsInvul(true);

		sitDown();
		_observerMode = true;
		broadcastUserInfo(true);

		// "Телепортируемся"
		sendPacket(new ObserverStart(x, y, z));

		for(L2WorldRegion oldNeighbor : getCurrentRegion().getNeighbors())
		{
			if(oldNeighbor != null)
				oldNeighbor.removeObjectsFromPlayer(this);
		}

		// Нужно при телепорте с более высокой точки на более низкую, иначе наносится вред от "падения"
		setLastClientPosition(null);
		setLastServerPosition(null);

		return true;
	}

	public synchronized void appearObserverMode()
	{
		if(!_observerMode)
			return;

		if(_observRegion != null)
		{
			_observRegion.addObject(this);
			// Добавляем фэйк в точку наблюдения и показываем чару все обьекты, что там находятся
			for(L2WorldRegion neighbor : _observRegion.getNeighbors())
			{
				if(neighbor != null)
					neighbor.showObjectsToPlayer(this);
			}
		}
		else
		{
			_observerMode = false;
			broadcastUserInfo(true);

			if(getCurrentRegion() != null)
			{
				for(L2WorldRegion region : getCurrentRegion().getNeighbors())
				{
					if(region != null)
						region.showObjectsToPlayer(this);
				}
			}

			standUp();
		}
	}

	public synchronized void leaveObserverMode()
	{
		sendPacket(new ObserverEnd(this));

		setTarget(null);
		unblock();
		setIsInvul(false);
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null, null);

		// Нужно при телепорте с более высокой точки на более низкую, иначе наносится вред от "падения"
		setLastClientPosition(null);
		setLastServerPosition(null);

		// Удаляем фэйк из точки наблюдения и удаляем у чара все обьекты, что там находятся
		if(_observRegion != null)
		{
			_observRegion.removeObject(this, false);
			for(L2WorldRegion neighbor : _observRegion.getNeighbors())
			{
				if(neighbor != null)
					neighbor.removeObjectsFromPlayer(this);
			}
		}

		_observRegion = null;

		//decayMe();
		//spawnMe();
	}

	public synchronized void enterOlympiadObserverMode(final int arenaId)
	{
		if(arenaId < 0 || arenaId >= Config.ALT_OLY_MAX_ARENAS)
			return;

		OlympiadInstance oi = Olympiad.getOlympiadInstances()[arenaId];
		if(oi == null)
			return;

		abortCast();
		abortAttack();

		if(getPet() != null)
			getPet().unSummon();

		if(_cubics.size() > 0)
		{
			for(L2CubicInstance cubic : _cubics)
				cubic.deleteMe();

			_cubics.clear();
		}

		if(isSitting())
			standUp();
		_olympiadGameId = arenaId;
		setStablePoint(getLoc());
		setTarget(null);
		setIsInvul(true);
		setInvisible(true);
		teleToLocation(oi.getStartLoc(), oi.getReflection());
		sendPacket(new ExOlympiadMode(3));
		_observerMode = true;
	}

	public void moveToArena(int arenaId)
	{
		if(!_observerMode || _olympiadGameId == -1)
			return;

		if(arenaId < 0 || arenaId >= Config.ALT_OLY_MAX_ARENAS)
			return;

		OlympiadInstance oi = Olympiad.getOlympiadInstances()[arenaId];
		if(oi == null)
			return;

		sendPacket(new ExOlympiadMatchEnd());
		teleToLocation(oi.getStartLoc(), oi.getReflection());
		_olympiadGameId = arenaId;
	}

	public void leaveOlympiadObserverMode()
	{
		setTarget(null);
		sendPacket(new ExOlympiadMode(0));
		setInvisible(false);
		setIsInvul(false);
		if(getAI() != null)
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

		OlympiadInstance oi = Olympiad.getOlympiadInstances()[_olympiadGameId];
		if(oi == null)
			_log.warn(this + " try to return from olympiad observ, but no stadium with id: " + _olympiadGameId);

		_olympiadGameId = -1;
		_observerMode = false;
		teleToLocation(getStablePoint(), 0);
		setStablePoint(null);
	}

	public void enterMovieMode()
	{
		setTarget(null);
		stopMove();
		setDisabled(true);
		if(!isGM())
			setIsInvul(true);
		setImobilised(true);
		sendPacket(new CameraMode(1));
	}

	public void leaveMovieMode()
	{
		setTarget(null);
		stopMove();
		setDisabled(false);
		if(!isGM())
			setIsInvul(false);
		setImobilised(false);
		sendPacket(new CameraMode(0));
	}

	/**
	 * yaw:North=90, south=270, east=0, west=180<BR>
	 * pitch > 0:looks up,pitch < 0:looks down<BR>
	 * time:faster that small value is.<BR>
	 */
	public void specialCamera(L2Object target, int dist, int yaw, int pitch, int time, int duration)
	{
		sendPacket(new SpecialCamera(target.getObjectId(), dist, yaw, pitch, time, duration));
	}

	public boolean canBeAHero()
	{
		return Hero.canBeAHero(getObjectId());
	}

	public void setOlympiadSide(final int i)
	{
		_olympiadSide = i;
	}

	public int getOlympiadSide()
	{
		return _olympiadSide;
	}

	public void setOlympiadGameId(final int id)
	{
		_olympiadGameId = id;
	}

	public int getOlympiadGameId()
	{
		return _olympiadGameId;
	}

	@Override
	public boolean inObserverMode()
	{
		return _observerMode;
	}

	public L2WorldRegion getObservRegion()
	{
		return _observRegion;
	}

	public int getTeleMode()
	{
		return _telemode;
	}

	public void setTeleMode(final int mode)
	{
		_telemode = mode;
	}

	public int getUnstuck()
	{
		return _unstuck;
	}

	public void setUnstuck(final int mode)
	{
		_unstuck = mode;
	}

	public void setLoto(final int i, final int val)
	{
		_loto[i] = val;
	}

	public int getLoto(final int i)
	{
		return _loto[i];
	}

	public void setRace(final int i, final int val)
	{
		_race[i] = val;
	}

	public int getRace(final int i)
	{
		return _race[i];
	}

	public boolean getMessageRefusal()
	{
		return _messageRefusal;
	}
	
	public HashMap<Integer, L2Skill> getSkillChainDetails()
	{
		return _skillChainDetail;
	}

	public void setMessageRefusal(final boolean mode)
	{
		_messageRefusal = mode;
		sendPacket(new EtcStatusUpdate(this));
	}

	public void setTradeRefusal(final boolean mode)
	{
		_tradeRefusal = mode;
	}

	public boolean getTradeRefusal()
	{
		return _tradeRefusal;
	}

	public void setExchangeRefusal(final boolean mode)
	{
		_exchangeRefusal = mode;
	}

	public boolean getExchangeRefusal()
	{
		return _exchangeRefusal;
	}

	public void addToBlockList(final String charName)
	{
		if(charName == null || charName.equalsIgnoreCase(getName()) || isInBlockList(charName))
		{
			// уже в списке
			sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_REGISTER_THE_USER_TO_YOUR_IGNORE_LIST));
			return;
		}

		L2Player block_target = L2ObjectsStorage.getPlayer(charName);

		if(block_target != null)
		{
			if(block_target.isGM())
			{
				sendPacket(new SystemMessage(SystemMessage.YOU_MAY_NOT_IMPOSE_A_BLOCK_ON_A_GM));
				return;
			}
			blockList.put(block_target.getObjectId(), block_target.getName());
			sendPacket(new SystemMessage(SystemMessage.S1_HAS_BEEN_ADDED_TO_YOUR_IGNORE_LIST).addString(block_target.getName()));
			block_target.sendPacket(new SystemMessage(SystemMessage.S1__HAS_PLACED_YOU_ON_HIS_HER_IGNORE_LIST).addString(getName()));
			return;
		}

		// чар не в игре
		int charId = Util.GetCharIDbyName(charName);

		if(charId == 0)
		{
			// чар не существует
			sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_REGISTER_THE_USER_TO_YOUR_IGNORE_LIST));
			return;
		}

		if(AdminTemplateManager.getAdminTemplate(charName) != null && AdminTemplateManager.getAdminTemplate(charName).checkBoolean("isGM", null))
		{
			sendPacket(new SystemMessage(SystemMessage.YOU_MAY_NOT_IMPOSE_A_BLOCK_ON_A_GM));
			return;
		}
		blockList.put(charId, charName);
		sendPacket(new SystemMessage(SystemMessage.S1_HAS_BEEN_ADDED_TO_YOUR_IGNORE_LIST).addString(charName));
	}

	public void removeFromBlockList(final String charName)
	{
		int charId = 0;
		for(int blockId : blockList.keySet())
			if(charName.equalsIgnoreCase(blockList.get(blockId)))
			{
				charId = blockId;
				break;
			}
		if(charId == 0)
		{
			sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_DELETE_THE_CHARACTER_FROM_IGNORE_LIST));
			return;
		}
		sendPacket(new SystemMessage(SystemMessage.S1_HAS_BEEN_REMOVED_FROM_YOUR_IGNORE_LIST).addString(blockList.remove(charId)));
		L2Player block_target = L2ObjectsStorage.getPlayer(charId);
		if(block_target != null)
			block_target.sendMessage(getName() + " has removed you from his/her Ignore List."); //В системных(619 == 620) мессагах ошибка ;)
	}

	public boolean isInBlockList(final L2Player player)
	{
		return isInBlockList(player.getObjectId());
	}

	public boolean isInBlockList(final int charId)
	{
		return blockList.containsKey(charId);
	}

	public boolean isInBlockList(final String charName)
	{
		for(int blockId : blockList.keySet())
			if(charName.equalsIgnoreCase(blockList.get(blockId)))
				return true;
		return false;
	}

	public boolean isBlockAll()
	{
		return _blockAll;
	}

	public void setBlockAll(final boolean state)
	{
		_blockAll = state;
	}

	public Collection<String> getBlockList()
	{
		return blockList.values();
	}

	public Map<Integer, String> getBlockListMap()
	{
		return blockList;
	}

	//TODO: pastle it to: static method of BD Engine

	private void restoreBlockList()
	{
		getBlockListMap().clear();

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT target_Id, target_Name FROM character_blocklist WHERE obj_Id = ?");
			statement.setInt(1, getObjectId());
			rs = statement.executeQuery();
			while(rs.next())
				getBlockListMap().put(rs.getInt("target_Id"), rs.getString("target_Name"));
		}
		catch(final SQLException e)
		{
			_log.warn("Can't restore player blocklist " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}

	//TODO: pastle it to: static method of BD Engine

	private void storeBlockList()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_blocklist WHERE obj_Id = ?");
			statement.setInt(1, getObjectId());
			statement.execute();
			statement.close();
			statement = con.prepareStatement("INSERT INTO character_blocklist values (?, ?, ?)");
			synchronized(getBlockListMap())
			{
				for(final int i : getBlockListMap().keySet())
				{
					statement.setInt(1, getObjectId());
					statement.setInt(2, i);
					statement.setString(3, getBlockListMap().get(i));
					statement.execute();
				}
			}
		}
		catch(final Exception e)
		{
			_log.warn("Can't store player blocklist " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void setConnected(boolean connected)
	{
		_isConnected = connected;
	}

	public boolean isConnected()
	{
		return _isConnected;
	}

	public void setHero(final boolean hero)
	{
		_hero = hero;
		updatePledgeClass();
	}

	@Override
	public boolean isHero()
	{
		return _hero;
	}

	public boolean isDonateHero()
	{
		return Integer.parseInt(getVar("donate@")) == 2;
	}

	public boolean isDonateNoble()
	{
		return Integer.parseInt(getVar("donate@")) > 0;
	}

	public void setDonateStatus(String status)
	{
		setVar("donate@", status);
	}

	public void setIsInOlympiadMode(final boolean b)
	{
		_inOlympiadMode = b;
	}

	public boolean isInOlympiadMode()
	{
		return _inOlympiadMode;
	}

	public void updateHeroHistory(String msg)
	{
		if(!isHero())
			return;
		Hero.updateHeroHistory(getObjectId(), msg);
	}

	public void finishOlympGame()
	{
		abortAttack();
		abortCast();
		Olympiad.finishGame(getOlympiadGameId());
	}

	boolean _olympiadStart;

	public void setIsOlympiadStart(boolean b)
	{
		_olympiadStart = b;
	}

	public boolean isOlympiadStart()
	{
		return _olympiadStart;
	}

	public void setNoble(final boolean noble)
	{
		_noble = noble;
		if(noble)
			Olympiad.checkNoble(this);
	}

	public boolean isNoble()
	{
		return _noble;
	}

	public int getSubLevel()
	{
		return isSubClassActive() ? _level : 0;
	}

	/* varka silenos and ketra orc quests related functions */

	public void setVarka(final int faction)
	{
		_varka = faction;
	}

	public int getVarka()
	{
		return _varka;
	}

	public void setKetra(final int faction)
	{
		_ketra = faction;
	}

	public int getKetra()
	{
		return _ketra;
	}

	public void setRam(final int faction)
	{
		_ram = faction;
	}

	public int getRam()
	{
		return _ram;
	}

	public void setPledgeType(final int typeId)
	{
		_pledgeType = typeId;
	}

	public int getPledgeType()
	{
		return _pledgeType;
	}

	public void setLvlJoinedAcademy(int lvl)
	{
		_lvlJoinedAcademy = lvl;
	}

	public int getLvlJoinedAcademy()
	{
		return _lvlJoinedAcademy;
	}

	public int getPledgeRank()
	{
		return _pledgeRank.ordinal();
	}

	public void updatePledgeClass()
	{
		L2Clan clan = getClan();
		byte CLAN_LEVEL = _clanId <= 0 ? -1 : clan.getLevel();
		boolean CLAN_LEADER = _clanId > 0 && clan.getLeaderId() == _objectId;
		boolean TW_LORD = CLAN_LEADER && clan.getHasCastle() > 0 && getVarB("territory_lord_" + (80 + clan.getHasCastle()));
		boolean IN_ACADEMY = _clanId > 0 && clan.isAcademy(_pledgeType);
		boolean IS_GUARD = _clanId > 0 && clan.isRoyalGuard(_pledgeType);
		boolean IS_KNIGHT = _clanId > 0 && clan.isOrderOfKnights(_pledgeType);
		boolean IS_GUARD_CAPTAIN = false;
		boolean IS_KNIGHT_BANNERET = false;
		if(_clanId > 0 && _pledgeType == 0)
		{
			int leaderOf = clan.getClanMember(_objectId).isSubLeader();
			if(clan.isRoyalGuard(leaderOf))
				IS_GUARD_CAPTAIN = true;
			else if(clan.isOrderOfKnights(leaderOf))
				IS_KNIGHT_BANNERET = true;
		}

		switch(CLAN_LEVEL)
		{
			case -1:
				_pledgeRank = PledgeRank.VASSAL;
				break;
			case 0:
			case 1:
			case 2:
			case 3:
				if(CLAN_LEADER)
					_pledgeRank = PledgeRank.HEIR;
				else
					_pledgeRank = PledgeRank.VASSAL;
				break;
			case 4:
				if(CLAN_LEADER)
					_pledgeRank = PledgeRank.KNIGHT;
				else
					_pledgeRank = PledgeRank.VASSAL;
				break;
			case 5:
				if(CLAN_LEADER)
					_pledgeRank = PledgeRank.WISEMAN;
				else
					_pledgeRank = PledgeRank.HEIR;
				break;
			case 6:
				if(CLAN_LEADER)
					_pledgeRank = PledgeRank.BARON;
				else if(IN_ACADEMY)
					_pledgeRank = PledgeRank.VASSAL;
				else if(IS_GUARD_CAPTAIN)
					_pledgeRank = PledgeRank.WISEMAN;
				else if(IS_GUARD)
					_pledgeRank = PledgeRank.HEIR;
				else
					_pledgeRank = PledgeRank.KNIGHT;
				break;
			case 7:
				if(CLAN_LEADER)
					_pledgeRank = PledgeRank.COUNT;
				else if(IN_ACADEMY)
					_pledgeRank = PledgeRank.VASSAL;
				else if(IS_GUARD_CAPTAIN)
					_pledgeRank = PledgeRank.VISCOUNT;
				else if(IS_GUARD)
					_pledgeRank = PledgeRank.KNIGHT;
				else if(IS_KNIGHT_BANNERET)
					_pledgeRank = PledgeRank.BARON;
				else if(IS_KNIGHT)
					_pledgeRank = PledgeRank.HEIR;
				else
					_pledgeRank = PledgeRank.WISEMAN;
				break;
			case 8:
				if(CLAN_LEADER)
					_pledgeRank = PledgeRank.MARQUIS;
				else if(IN_ACADEMY)
					_pledgeRank = PledgeRank.VASSAL;
				else if(IS_GUARD_CAPTAIN)
					_pledgeRank = PledgeRank.COUNT;
				else if(IS_GUARD)
					_pledgeRank = PledgeRank.WISEMAN;
				else if(IS_KNIGHT_BANNERET)
					_pledgeRank = PledgeRank.VISCOUNT;
				else if(IS_KNIGHT)
					_pledgeRank = PledgeRank.KNIGHT;
				else
					_pledgeRank = PledgeRank.BARON;
				break;
			case 9:
				if(CLAN_LEADER)
					_pledgeRank = PledgeRank.DUKE;
				else if(IN_ACADEMY)
					_pledgeRank = PledgeRank.VASSAL;
				else if(IS_GUARD_CAPTAIN)
					_pledgeRank = PledgeRank.MARQUIS;
				else if(IS_GUARD)
					_pledgeRank = L2Clan.PledgeRank.BARON;
				else if(IS_KNIGHT_BANNERET)
					_pledgeRank = PledgeRank.COUNT;
				else if(IS_KNIGHT)
					_pledgeRank = PledgeRank.WISEMAN;
				else
					_pledgeRank = PledgeRank.VISCOUNT;
				break;
			case 10:
				if(CLAN_LEADER)
					_pledgeRank = PledgeRank.GRAND_DUKE;
				else if(IN_ACADEMY)
					_pledgeRank = PledgeRank.VASSAL;
				else if(IS_GUARD_CAPTAIN)
					_pledgeRank = PledgeRank.DUKE;
				else if(IS_GUARD)
					_pledgeRank = PledgeRank.VISCOUNT;
				else if(IS_KNIGHT_BANNERET)
					_pledgeRank = PledgeRank.MARQUIS;
				else if(IS_KNIGHT)
					_pledgeRank = PledgeRank.BARON;
				else
					_pledgeRank = PledgeRank.COUNT;
				break;
			case 11:
				if(CLAN_LEADER)
					_pledgeRank = TW_LORD ? PledgeRank.EMPEROR : PledgeRank.DISTINGUISHED_KING;
				else if(IN_ACADEMY)
					_pledgeRank = PledgeRank.VASSAL;
				else if(IS_GUARD_CAPTAIN)
					_pledgeRank = PledgeRank.GRAND_DUKE;
				else if(IS_GUARD)
					_pledgeRank = PledgeRank.COUNT;
				else if(IS_KNIGHT_BANNERET)
					_pledgeRank = PledgeRank.DUKE;
				else if(IS_KNIGHT)
					_pledgeRank = PledgeRank.VISCOUNT;
				else
					_pledgeRank = PledgeRank.MARQUIS;
				break;
		}

		if(_hero && _pledgeRank.ordinal() < PledgeRank.MARQUIS.ordinal())
			_pledgeRank = PledgeRank.MARQUIS;
		else if(_noble && _pledgeRank.ordinal() < PledgeRank.BARON.ordinal())
			_pledgeRank = PledgeRank.BARON;
	}

	public void setPowerGrade(final int grade)
	{
		_powerGrade = grade;
	}

	public int getPowerGrade()
	{
		return _powerGrade;
	}

	public void setApprentice(final int apprentice)
	{
		_apprentice = apprentice;
	}

	public int getApprentice()
	{
		return _apprentice;
	}

	public int getSponsor()
	{
		if(_clanId <= 0)
			return 0;
		return getClan().getClanMember(getObjectId()).getSponsor();
	}

	@Override
	public void setTeam(final int team)
	{
		_team = team;

		broadcastUserInfo(true);
		if(getPet() != null)
			getPet().broadcastPetInfo();
	}

	@Override
	public int getTeam()
	{
		return _team;
	}

	public int getNameColor()
	{
		if(inObserverMode())
			return Color.black.getRGB();

		return _nameColor;
	}

	public void setNameColor(final int nameColor)
	{
		if(nameColor != Config.NORMAL_NAME_COLOUR && nameColor != Config.CLANLEADER_NAME_COLOUR && nameColor != Config.GM_NAME_COLOUR && nameColor != Config.SERVICES_OFFLINE_TRADE_NAME_COLOR)
			setVar("namecolor", Integer.toHexString(nameColor));
		else if(nameColor == Config.NORMAL_NAME_COLOUR)
			unsetVar("namecolor");
		_nameColor = nameColor;
	}

	public void setNameColor(final int red, final int green, final int blue)
	{
		_nameColor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16);
		if(_nameColor != Config.NORMAL_NAME_COLOUR && _nameColor != Config.CLANLEADER_NAME_COLOUR && _nameColor != Config.GM_NAME_COLOUR && _nameColor != Config.SERVICES_OFFLINE_TRADE_NAME_COLOR)
			setVar("namecolor", Integer.toHexString(_nameColor));
		else
			unsetVar("namecolor");
	}

	public final String toFullString()
	{
		final StringBuilder sb = new StringBuilder(160);

		sb.append("Player '").append(getName()).append("' [oid=").append(_objectId).append(", account='").append(getAccountName()).append(", ip=").append(_connection != null ? _connection.getIpAddr() : "0.0.0.0").append("]");
		return sb.toString();
	}

	protected Map<String, UserVar> user_variables;

	public Map<String, UserVar> getUserVars()
	{
		return user_variables;
	}

	public void setVar(String name, String value)
	{
		setVar(name, value, -1);
	}

	public void setVar(String name, int value)
	{
		setVar(name, String.valueOf(value), -1);
	}

	public void setVar(String name, String value, int expiredTime)
	{
		UserVar uv = new UserVar(name, value, expiredTime > 0 ? expiredTime * 1000L : expiredTime);
		user_variables.put(name, uv);

		if(name.equalsIgnoreCase("storemode") && (!Config.ALT_SAVE_PRIVATE_STORE && !isInOfflineMode()))
			return;
		saveUserVar(getObjectId(), uv);
	}

	public static void saveUserVar(int objectId, UserVar uv)
	{
		Connection con = null;
		PreparedStatement insertion = null;
		long startTime = System.currentTimeMillis();
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			insertion = con.prepareStatement("REPLACE INTO character_variables  (obj_id, type, name, value, expire_time) VALUES (?,\"user-var\",?,?,?)");
			insertion.setInt(1, objectId);
			insertion.setString(2, uv.name);
			insertion.setString(3, uv.value.length() > 255 ? uv.value.substring(0, 255) : uv.value);
			insertion.setInt(4, uv.expire > 0 ? (int) (uv.expire / 1000) : (int) uv.expire);
			insertion.executeUpdate();
		}
		catch(Exception e)
		{
			startTime = System.currentTimeMillis() - startTime;
			_log.warn("Interrupt time: " + startTime + " ms");
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, insertion);
		}
	}

	public void unsetVar(String name)
	{
		if(user_variables.remove(name) != null)
			unsetVar(getObjectId(), name);
	}

	public static void unsetVar(int objectId, String name)
	{
		if(name == null)
			return;

		Connection con = null;
		PreparedStatement insertion = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			insertion = con.prepareStatement("DELETE FROM character_variables WHERE obj_id = ? AND type = \"user-var\" AND name = ?");
			insertion.setInt(1, objectId);
			insertion.setString(2, name);
			insertion.executeUpdate();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, insertion);
		}
	}

	public String getVar(String name)
	{
		if(user_variables != null && user_variables.containsKey(name))
		{
			UserVar uv = user_variables.get(name);
			if(uv.expire <= 0 || uv.expire > System.currentTimeMillis())
				return uv.value;
			else
				unsetVar(name);
		}
		return null;
	}

	public long getVarExpireTime(String name)
	{
		if(user_variables.containsKey(name))
			return user_variables.get(name).expire;

		return 0;
	}

	public boolean getVarB(String name)
	{
		String var = getVar(name);
		return !(var == null || var.equals("0") || var.equalsIgnoreCase("false"));
	}

	public int getVarInt(String name)
	{
		int v = 0;
		try
		{
			String var = getVar(name);
			if(var != null)
				v = Integer.parseInt(var);
		}
		catch(Exception e)
		{
		}
		return v;
	}

	public long getVarLong(String name)
	{
		long v = 0;
		try
		{
			String var = getVar(name);
			if(var != null)
				v = Long.parseLong(var);
		}
		catch(Exception e)
		{
		}
		return v;
	}

	public float getVarFloat(String name)
	{
		float v = 0;
		try
		{
			String var = getVar(name);
			if(var != null)
				v = Float.parseFloat(var);
		}
		catch(Exception e)
		{
		}
		return v;
	}

	public static Map<String, UserVar> loadVariables(int objectId)
	{
		Map<String, UserVar> user_variables = new ConcurrentHashMap<>();
		//Удаляем переменные, которые закончились по времени
		mysql.set("DELETE FROM `character_variables` WHERE type = \"user-var\" and `obj_id` = " + objectId + " and `expire_time` < " + System.currentTimeMillis() / 1000 + " and `expire_time` != -1");
		Connection con = null;
		PreparedStatement offline = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			offline = con.prepareStatement("SELECT * FROM character_variables WHERE obj_id = ?");
			offline.setInt(1, objectId);
			rs = offline.executeQuery();
			while(rs.next())
				user_variables.put(rs.getString("name"), new UserVar(rs.getString("name"), rs.getString("value"), rs.getInt("expire_time") > 0 ? rs.getInt("expire_time") * 1000L : rs.getInt("expire_time")));

			// TODO Здесь обазятельно выставлять все стандартные параметры, иначе будут NPE
			if(!user_variables.containsKey("lang@"))
				user_variables.put("lang@", new UserVar("lang@", Config.DEFAULT_LANG, -1));
			if(!user_variables.containsKey("donate@"))
				user_variables.put("donate@", new UserVar("donate@", "0", -1));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, offline, rs);
		}
		return user_variables;
	}

	public int isAtWarWith(final Integer id)
	{
		if(getClanId() != 0)
		{
			L2Clan clan = getClan();
			if(clan != null && clan.isAtWarWith(id))
				return 1;
		}

		return 0;
	}

	public int isAtWar()
	{
		if(getClanId() != 0)
		{
			L2Clan clan = getClan();
			if(clan != null && clan.isAtWarOrUnderAttack() > 0)
				return 1;
		}
		return 0;
	}

	public void stopWaterTask()
	{
		if(_waterTask != null)
		{
			_waterTask.cancel(true);

			_waterTask = null;
			sendPacket(new SetupGauge(2, 0));
			sendChanges();
		}
	}

	public void startWaterTask()
	{
		if(isDead())
			stopWaterTask();
		else if(Config.ALLOW_WATER && _waterTask == null)
		{
			int timeinwater = (int) (calcStat(Stats.BREATH, 86, null, null) * 1000);
			sendPacket(new SetupGauge(2, timeinwater));
			if(_transformationId != 0 && !isCursedWeaponEquipped())
			{
				L2SkillLearn sl = null;
				try
				{
					sl = SkillTreeTable.getSkillLearn(getEffectByAbnormalType("transformation").getSkillId(), (short) 1, getClassId(), null);
				}
				catch(NullPointerException e)
				{
				}
				if(sl == null || !sl.isNormal())
					stopEffects("transformation");
			}
			_waterTask = ThreadPoolManager.getInstance().scheduleEffect(new WaterTask(this), timeinwater);
			sendChanges();
		}
	}

	public void setWaterTask(ScheduledFuture<?> waterTask)
	{
		_waterTask = waterTask;
	}

	private int _reviveRequested = 0;
	private long _reviverStoredId;
	private double _revivePower = 0;
	private boolean _revivePet = false;

	public void doRevive(double percent)
	{
		restoreExp(percent);
		doRevive();
	}

	@Override
	public void doRevive()
	{
		super.doRevive();
		unsetVar("lostexp");
		updateEffectIcons();
		AutoShot();
		_reviveRequested = 0;
		_revivePower = 0;
	}

	public void reviveRequest(L2Player reviver, double percent, boolean pet, boolean charm)
	{
		if(_reviveRequested == 1)
		{
			if(_revivePet == pet)
				reviver.sendPacket(Msg.BETTER_RESURRECTION_HAS_BEEN_ALREADY_PROPOSED);
			else if(pet)
				reviver.sendPacket(Msg.SINCE_THE_MASTER_WAS_IN_THE_PROCESS_OF_BEING_RESURRECTED_THE_ATTEMPT_TO_RESURRECT_THE_PET_HAS_BEEN_CANCELLED);
			else
				reviver.sendPacket(Msg.SINCE_THE_PET_WAS_IN_THE_PROCESS_OF_BEING_RESURRECTED_THE_ATTEMPT_TO_RESURRECT_ITS_MASTER_HAS_BEEN_CANCELLED);
			return;
		}
		if(pet && getPet() != null && getPet().isDead() || !pet && isDead())
		{
			_reviveRequested = 1;
			_revivePower = percent;
			_revivePet = pet;
			_reviverStoredId = reviver.getStoredId();
			ConfirmDlg pkt;
			int lostexp = 0;
			if(pet)
				lostexp = ((L2PetInstance) getPet()).getLostExp();
			else
			{
				String lostexps = getVar("lostexp");
				if(lostexps != null)
					lostexp = Integer.parseInt(lostexps);
			}
			if(charm)
				pkt = new ConfirmDlg(SystemMessage.RESURRECTION_IS_POSSIBLE_BECAUSE_OF_THE_COURAGE_CHARMS_EFFECT_WOULD_YOU_LIKE_TO_RESURRECT_NOW, 60000, 2);
			else
			{
				pkt = new ConfirmDlg(SystemMessage.S1_IS_MAKING_AN_ATTEMPT_AT_RESURRECTION_WITH_$S2_EXPERIENCE_POINTS_DO_YOU_WANT_TO_CONTINUE_WITH_THIS_RESURRECTION, 0, 2);
				pkt.addString(reviver.getName()).addNumber((int) (_revivePower * lostexp / 100));
			}
			sendPacket(pkt);
		}
	}

	public void reviveAnswer(int answer)
	{
		if(_reviveRequested != 1 || !isDead() && !_revivePet || _revivePet && getPet() != null && !getPet().isDead())
			return;
		if(answer == 1)
			if(!_revivePet)
			{
				if(_revivePower != 0)
					doRevive(_revivePower);
				else
					doRevive();

				ScriptHandler.getInstance().onResurrected(this, _reviverStoredId);
			}
			else if(getPet() != null)
				if(_revivePower != 0)
					((L2PetInstance) getPet()).doRevive(_revivePower);
				else
					getPet().doRevive();
		_reviveRequested = 0;
		_revivePower = 0;
		_reviverStoredId = 0;
	}

	/**
	 * Координаты точки призыва персонажа
	 */
	private Location _summonCharacterCoords;

	/**
	 * Флаг необходимости потребления Summoning Cystall-а при призыве персонажа
	 */
	private int _summonConsumeId = 0;
	private int _summonConsume = 0;

	/**
	 * Обработчик ответа клиента на призыв персонажа.
	 *
	 * @param answer Идентификатор запроса
	 */
	public void teleportAnswer(int answer)
	{
		if(answer == 1 && _summonCharacterCoords != null)
		{
			SystemMessage sm = i_summon_friend.checkSummonCond(this);
			if(sm != null)
			{
				_summonCharacterCoords = null;
				_summonConsume = 0;
				_summonConsumeId = 0;
				return;
			}

			if(_summonConsume > 0)
			{
				if(getItemCountByItemId(_summonConsumeId) < _summonConsume)
					sendPacket(new SystemMessage(SystemMessage.S1_IS_REQUIRED_FOR_SUMMONING).addItemName(_summonConsumeId));
				else if(destroyItemByItemId("Summon", _summonConsumeId, _summonConsume, this, true))
					teleToLocation(_summonCharacterCoords);
			}
			else
				teleToLocation(_summonCharacterCoords);
		}
		_summonCharacterCoords = null;
		_summonConsume = 0;
		_summonConsumeId = 0;
	}

	/**
	 * Отправляет запрос клиенту на призыв персонажа.
	 * <p/>
	 * SummonerName Имя призывающего персонажа
	 * coords	   Координаты точки призыва персонажа
	 */
	public void teleportRequest(L2Player player, int consumeCount, int consumeId)
	{
		if(_summonCharacterCoords == null)
		{
			_summonCharacterCoords = player.getLoc();
			_summonConsumeId = consumeId;
			_summonConsume = consumeCount;
			ConfirmDlg cd = new ConfirmDlg(SystemMessage.S1_WISHES_TO_SUMMON_YOU_FROM_S2_DO_YOU_ACCEPT, 15000, 1);
			cd.addString(player.getName()).addZoneName(_summonCharacterCoords.getX(), _summonCharacterCoords.getY(), _summonCharacterCoords.getZ());
			sendPacket(cd);
		}
	}

	public boolean isTeleportRequested()
	{
		return _summonCharacterCoords != null;
	}

	private String _scriptName = "";
	private Object[] _scriptArgs = new Object[0];
	private String _scriptName2 = "";

	public void scriptAnswer(final int answer)
	{
		if(answer == 1 && !_scriptName.equals(""))
			callScripts(_scriptName.split(":")[0], _scriptName.split(":")[1], _scriptArgs);
		else if(answer == 0 && !_scriptName2.equals(""))
			callScripts(_scriptName2.split(":")[0], _scriptName2.split(":")[1], _scriptArgs);
		_scriptName = _scriptName2 = "";
	}

	public void scriptRequest(String text, String scriptName, Object[] args)
	{
		scriptRequest(text, scriptName, args, 30000, "");
	}

	public void scriptRequest(String text, String scriptName, Object[] args, int dialogTime, String scriptName2)
	{
		if(_scriptName.equals(""))
		{
			_scriptName = scriptName;
			_scriptName2 = scriptName2;
			_scriptArgs = args;
			sendPacket(new ConfirmDlg(SystemMessage.S1_S2, dialogTime, 3).addString(text));
		}
	}

	public boolean isReviveRequested()
	{
		return _reviveRequested == 1;
	}

	public boolean isRevivingPet()
	{
		return _revivePet;
	}

	public void updateNoChannel(final long time)
	{
		Connection con = null;
		PreparedStatement statement = null;

		setNoChannel(time);

		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			final String stmt = "UPDATE characters SET nochannel = ? WHERE obj_Id=?";
			statement = con.prepareStatement(stmt);
			statement.setLong(1, _NoChannel > 0 ? _NoChannel / 1000 : _NoChannel);
			statement.setInt(2, getObjectId());
			statement.executeUpdate();
		}
		catch(final Exception e)
		{
			_log.warn("Could not activate nochannel:" + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	/**
	 * @return Returns the inBoat.
	 */
	@Override
	public boolean isInBoat()
	{
		return _vehicle != null;
	}

	@Override
	public boolean isInAirShip()
	{
		return _vehicle != null && _vehicle.isAirShip();
	}

	public boolean isAirshipCaptain()
	{
		return _vehicle instanceof L2ClanAirship && ((L2ClanAirship) _vehicle).getCaptainObjectId() == getObjectId();
	}

	/**
	 * @return
	 */
	public L2Vehicle getVehicle()
	{
		return _vehicle;
	}

	/**
	 * @param boat
	 */
	public void setVehicle(final L2Vehicle boat)
	{
		_vehicle = boat;
		if(boat != null)
			_recommendSystem.setActive(false);
		setStablePoint(boat == null ? null : boat.getKickPoint());
		int currentComapss = getCurrentCompassZone();
		if(currentComapss != getLastCompassZone())
		{
			setLastComapssZone(currentComapss);
			sendPacket(new ExSetCompassZoneCode(this));
		}
	}

	public HashMap<Short, L2SubClass> getSubClasses()
	{
		return _classlist;
	}

	public void setBaseClass(final int baseClass)
	{
		_baseClass = baseClass;
	}

	public int getBaseClass()
	{
		return _baseClass;
	}

	public int getBaseLevel()
	{
		return _baseLevel;
	}

	public long getBaseExp()
	{
		return _baseExp;
	}

	public long getBaseSp()
	{
		return _baseSp;
	}

	public void setActiveClass(final short activeClass)
	{
		_activeClass = activeClass;
	}

	public short getActiveClass()
	{
		return _activeClass;
	}

	/**
	 * Changing index of class in DB, used for changing class when finished professional quests
	 *
	 * @param oldclass
	 * @param newclass
	 */
	public void changeClassInDb(final short oldclass, final short newclass)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("UPDATE character_subclasses SET class_id=? WHERE char_obj_id=? AND class_id=?");
			statement.setInt(1, newclass);
			statement.setInt(2, getObjectId());
			statement.setInt(3, oldclass);
			statement.executeUpdate();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("UPDATE character_hennas SET class_index=? WHERE char_obj_id=? AND class_index=?");
			statement.setInt(1, newclass);
			statement.setInt(2, getObjectId());
			statement.setInt(3, oldclass);
			statement.executeUpdate();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("UPDATE character_shortcuts SET class_index=? WHERE char_obj_id=? AND class_index=?");
			statement.setInt(1, newclass);
			statement.setInt(2, getObjectId());
			statement.setInt(3, oldclass);
			statement.executeUpdate();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("UPDATE character_skills SET class_index=? WHERE char_obj_id=? AND class_index=?");
			statement.setInt(1, newclass);
			statement.setInt(2, getObjectId());
			statement.setInt(3, oldclass);
			statement.executeUpdate();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("UPDATE character_effects_save SET class_index=? WHERE char_obj_id=? AND class_index=?");
			statement.setInt(1, newclass);
			statement.setInt(2, getObjectId());
			statement.setInt(3, oldclass);
			statement.executeUpdate();
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("UPDATE character_skills_save SET class_index=? WHERE char_obj_id=? AND class_index=?");
			statement.setInt(1, newclass);
			statement.setInt(2, getObjectId());
			statement.setInt(3, oldclass);
			statement.executeUpdate();
			DbUtils.closeQuietly(statement);
		}
		catch(final SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}

        Object[] script_args = new Object[] { this, oldclass, newclass };
        for(ScriptClassAndMethod handler : Scripts.onPlayerClassChange)
            callScripts(handler.scriptClass, handler.method, script_args);
    }

	/**
	 * Сохраняет информацию о классах в БД
	 */
	public void storeCharSubClasses()
	{
		final L2SubClass currentSub = _classlist.get(_activeClass);
		if(currentSub != null)
		{
			currentSub.setCp(getCurrentCp());
			currentSub.setExp(getExp());
			currentSub.setHp(getCurrentHp());
			currentSub.setLevel(getLevel());
			currentSub.setMp(getCurrentMp());
			currentSub.setSp(getSp());
			currentSub.setActive(true);
			_classlist.put(_activeClass, currentSub);
		}

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE character_subclasses SET exp=?,sp=?,curHp=?,curMp=?,curCp=?,level=?,active=?,death_penalty=? WHERE char_obj_id=? AND class_id=?");
			for(final L2SubClass subClass : _classlist.values())
			{
				statement.setLong(1, subClass.getExp());
				statement.setInt(2, subClass.getSp());
				statement.setDouble(3, subClass.getHp());
				statement.setDouble(4, subClass.getMp());
				statement.setDouble(5, subClass.getCp());
				statement.setInt(6, subClass.getLevel());
				statement.setInt(7, subClass.isActive() ? 1 : 0);
				statement.setInt(8, subClass.getDeathPenalty().getLevelOnSaveDB());
				statement.setInt(9, getObjectId());
				statement.setInt(10, subClass.getClassId());
				statement.execute();
			}
			DbUtils.closeQuietly(statement);

			statement = con.prepareStatement("UPDATE character_subclasses SET maxHp=?,maxMp=?,maxCp=? WHERE char_obj_id=? AND active=1");
			statement.setDouble(1, getMaxHp());
			statement.setDouble(2, getMaxMp());
			statement.setDouble(3, getMaxCp());
			statement.setInt(4, getObjectId());
			statement.execute();
		}
		catch(final Exception e)
		{
			_log.warn("Could not store char sub data: " + getExp() + " " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	/**
	 * Restore list of character professions and set up active proof
	 * Used when character is loading
	 */
	public static void restoreCharSubClasses(final L2Player player)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT class_id,exp,sp,level,curHp,curCp,curMp,active,isBase,death_penalty,slot FROM character_subclasses WHERE char_obj_id=?");
			statement.setInt(1, player.getObjectId());
			rset = statement.executeQuery();
			while(rset.next())
			{
				final L2SubClass subClass = new L2SubClass();
				subClass.setBase(rset.getInt("isBase") != 0);
				subClass.setClassId(rset.getShort("class_id"));
				subClass.setLevel(rset.getByte("level"));
				subClass.setExp(rset.getLong("exp"));
				subClass.setSp(rset.getInt("sp"));
				subClass.setHp(rset.getDouble("curHp"));
				subClass.setMp(rset.getDouble("curMp"));
				subClass.setCp(rset.getDouble("curCp"));
				subClass.setActive(rset.getInt("active") != 0);
				subClass.setDeathPenalty(new DeathPenalty(player, rset.getByte("death_penalty")));
				subClass.setSlot(rset.getByte("slot"));
				subClass.setPlayer(player);

				player._classlist.put(subClass.getClassId(), subClass);
				if(subClass.isActive())
					player.setActiveSubClass(subClass.getClassId(), false);
			}
		}
		catch(final SQLException e)
		{
			_log.warn("Could not restore char sub-classes: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	/**
	 * Добавить класс, используется только для сабклассов
	 */
	public boolean addSubClass(final short classId)
	{
		if(_classlist.size() >= 4)
			return false;

		final ClassId newId = ClassId.values()[classId];

		if(newId.getRace() == null)
			return false;

		final L2SubClass newClass = new L2SubClass();

		newClass.setClassId(classId);
		newClass.setPlayer(this);

		byte slot = 1;
		boolean f = true;
		while(f)
		{
			f = false;
			for(L2SubClass sb : _classlist.values())
				if(sb.getSlot() == slot)
				{
					slot++;
					f = true;
				}
		}

		newClass.setSlot(slot);

		_classlist.put(classId, newClass);

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			// Store the basic info about this new sub-class.
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO character_subclasses (char_obj_id, class_id, exp, sp, curHp, curMp, curCp, maxHp, maxMp, maxCp, level, active, isBase, death_penalty, slot) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			statement.setInt(1, getObjectId());
			statement.setInt(2, newClass.getClassId());
			statement.setLong(3, Experience.LEVEL[40]);
			statement.setInt(4, 0);
			statement.setDouble(5, getCurrentHp());
			statement.setDouble(6, getCurrentMp());
			statement.setDouble(7, getCurrentCp());
			statement.setDouble(8, getCurrentHp());
			statement.setDouble(9, getCurrentMp());
			statement.setDouble(10, getCurrentCp());
			statement.setInt(11, 40);
			statement.setInt(12, 0);
			statement.setInt(13, 0);
			statement.setInt(14, 0);
			statement.setByte(15, newClass.getSlot());
			statement.execute();
		}
		catch(final Exception e)
		{
			_log.warn("Could not add character sub-class: " + e);
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}

		setActiveSubClass(classId, true);

		// Add all the necessary skills up to level 40 for this new class.
		boolean countUnlearnable = true;
		int unLearnable = 0;
		int numSkillsAdded = 0;
		GArray<L2SkillLearn> skills = SkillTreeTable.getInstance().getAvailableSkills(this, newId);
		while(skills.size() > unLearnable)
		{
			for(final L2SkillLearn s : skills)
			{
				final L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
				if(sk == null || !sk.getCanLearn(newId))
				{
					if(countUnlearnable)
						unLearnable++;
					continue;
				}
				addSkill(sk, true);
				numSkillsAdded++;
			}
			countUnlearnable = false;
			skills = SkillTreeTable.getInstance().getAvailableSkills(this, newId);
		}

		restoreSkills();
		rewardSkills();
		sendPacket(new SkillList(this));
		sendPacket(new ExAcquirableSkillListByClass(this));
		setCurrentHpMp(getMaxHp(), getMaxMp());
		setCurrentCp(getMaxCp());

		if(Config.DEBUG)
			_log.info(numSkillsAdded + " skills added for " + getName() + "'s sub-class.");

		return true;
	}

	/**
	 * Удаляет всю информацию о классе и добавляет новую, только для сабклассов
	 */
	public boolean modifySubClass(final short oldClassId, final short newClassId)
	{
		final L2SubClass originalClass = _classlist.get(oldClassId);
		if(originalClass.isBase())
			return false;

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			// Remove all basic info stored about this sub-class.
			statement = con.prepareStatement("DELETE FROM character_subclasses WHERE char_obj_id=? AND class_id=? AND isBase = 0");
			statement.setInt(1, getObjectId());
			statement.setInt(2, oldClassId);
			statement.execute();
			DbUtils.closeQuietly(statement);

			// Remove all skill info stored for this sub-class.
			statement = con.prepareStatement("DELETE FROM character_skills WHERE char_obj_id=? AND class_index=? ");
			statement.setInt(1, getObjectId());
			statement.setInt(2, oldClassId);
			statement.execute();
			DbUtils.closeQuietly(statement);

			// Remove all henna info stored for this sub-class.
			statement = con.prepareStatement("DELETE FROM character_hennas WHERE char_obj_id=? AND class_index=? ");
			statement.setInt(1, getObjectId());
			statement.setInt(2, oldClassId);
			statement.execute();
			DbUtils.closeQuietly(statement);

			// Remove all shortcuts info stored for this sub-class.
			statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=? AND class_index=? ");
			statement.setInt(1, getObjectId());
			statement.setInt(2, oldClassId);
			statement.execute();
		}
		catch(final Exception e)
		{
			_log.warn("Could not delete char sub-class: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		_classlist.remove(oldClassId);

		Object[] script_args = new Object[] { this, oldClassId, newClassId };
		for(ScriptClassAndMethod handler : Scripts.onPlayerClassChange)
			callScripts(handler.scriptClass, handler.method, script_args);

		return addSubClass(newClassId);
	}

	/**
	 * Устанавливает активный сабкласс
	 * <p/>
	 * <li>Retrieve from the database all skills of this L2Player and add them to _skills </li>
	 * <li>Retrieve from the database all macroses of this L2Player and add them to _macroses</li>
	 * <li>Retrieve from the database all shortCuts of this L2Player and add them to _shortCuts</li><BR><BR>
	 */
	public void setActiveSubClass(final Short subId, final boolean store)
	{
		storeEffects();
		storeDisableSkills();
		abortCast();

		for(L2Player player : getAroundPlayers(5000))
		{
			if(player != null && player.getForceBuff() != null && player.getForceBuff().getTarget() == this)
				player.abortCast();
		}

		if(QuestManager.getQuest(422) != null)
		{
			String qn = QuestManager.getQuest(422).getName();
			if(qn != null)
			{
				QuestState qs = getQuestState(qn);
				if(qs != null)
					qs.exitCurrentQuest(true);
			}
		}

		if(store && _classlist.containsKey(_activeClass))
		{
			final L2SubClass oldsub = _classlist.get(_activeClass);
			oldsub.setCp(getCurrentCp());
			oldsub.setExp(getExp());
			oldsub.setHp(getCurrentHp());
			oldsub.setLevel(getLevel());
			oldsub.setMp(getCurrentMp());
			oldsub.setSp(getSp());
			oldsub.setActive(false);
			_classlist.put(_activeClass, oldsub);
		}

		final L2SubClass sub = _classlist.get(subId);
		sub.setActive(true);
		_activeClass = sub.getClassId();
		_classlist.put(_activeClass, sub);
		_level = sub.getLevel();
		_exp = sub.getExp();
		_sp = sub.getSp();

		removeAllSkills();

		stopAllEffects();

		weaponPenalty = 0;
		armorPenalty = 0;
		setClassId(subId, false);

		if(getPet() != null && getPet().isSummon())
			getPet().unSummon();
		if(!_cubics.isEmpty())
		{
			for(final L2CubicInstance cubic : _cubics)
				cubic.deleteMe();

			_cubics.clear();
		}
		setAgathion(0);

		restoreSkills();
		rewardSkills();
		sendPacket(new SkillList(this));
		sendPacket(new ExAcquirableSkillListByClass(this));

		getInventory().refreshListeners();
		unEquipInappropriateItems();

		for(int i = 0; i < 3; i++)
			_henna[i] = null;

		restoreHenna();
		sendPacket(new HennaInfo(this));

		restoreEffects();
		if(isInWorld())
			restoreDisableSkills();

		_currentHp = sub.getHp();
		setCurrentHpMp(sub.getHp(), sub.getMp());
		setCurrentCp(sub.getCp());
		setIncreasedForce(0);
		broadcastUserInfo(true);
		updateStats();

		_shortCuts.restore();
		sendPacket(new ShortCutInit(this));
		for(int shotId : getAutoSoulShot())
			sendPacket(new ExAutoSoulShot(shotId, true));

		sendPacket(new SkillCoolTime(this));
		broadcastPacket(new SocialAction(getObjectId(), SocialAction.SocialType.LEVEL_UP));
		getDeathPenalty().restore();
		startHourlyTask();
	}

	public void startPremiumTask(long time)
	{
		stopPremiumTask();
		sendPacket(new ExBRPremiumState(getObjectId(), true));
		updateStats();
		sendUserInfo(true);

		Date d = new Date(time);
		time -= System.currentTimeMillis();
		if(time > 0)
		{
			_premiumExpire = ThreadPoolManager.getInstance().scheduleAi(new L2ObjectTasks.PremiumExpire(this), time, true);
			sendMessage(new CustomMessage("common.PremiumStart", this).addString(String.format("%1$te.%1$tm.%1$tY %1$tH:%1tM", d)));
		}
	}

	public void stopPremiumTask()
	{
		if(_premiumExpire != null)
		{
			_premiumExpire.cancel(true);
			_premiumExpire = null;
		}
	}

	public boolean isPremiumEnabled()
	{
		return Config.PREMIUM_ENABLED && getNetConnection() != null && (getNetConnection().getPremiumExpire() > System.currentTimeMillis() || Config.PREMIUM_MIN_CLAN_LEVEL >= 0 && isClanLeader() && getClan().getLevel() >= Config.PREMIUM_MIN_CLAN_LEVEL);
	}

	public boolean isAutoLoot()
	{
		if(Config.PREMIUM_ENABLED && Config.PREMIUM_AUTOLOOT_ONLY)
			return isPremiumEnabled() && getVarB("autoloot");
		else
			return getVarB("autoloot");
	}

	public int getInventoryLimit()
	{
		int limit;
		if(isGM())
			limit = Config.INVENTORY_MAXIMUM_GM;
		else if(getTemplate().race == Race.dwarf)
			limit = Config.INVENTORY_MAXIMUM_DWARF;
		else
			limit = Config.INVENTORY_MAXIMUM_NO_DWARF;

		if(isPremiumEnabled())
			limit += Config.PREMIUM_INVENTORY_LIMIT;

		return (int) calcStat(Stats.INVENTORY_LIMIT, limit, null, null);
	}

	public int getWarehouseLimit()
	{
		return isPremiumEnabled() ? (int) calcStat(Stats.STORAGE_LIMIT, 0, null, null) + Config.PREMIUM_WAREHOUSE_LIMIT : (int) calcStat(Stats.STORAGE_LIMIT, 0, null, null);
	}

	public int getFreightLimit()
	{
		// FIXME Не учитывается количество предметов, уже имеющееся на складе
		return getWarehouseLimit();
	}

	public int getTradeLimit()
	{
		return isPremiumEnabled() ? (int) calcStat(Stats.TRADE_LIMIT, 0, null, null) + Config.PREMIUM_PRIVATE_STORE_LIMIT : (int) calcStat(Stats.TRADE_LIMIT, 0, null, null);
	}

	public int getDwarvenRecipeLimit()
	{
		return (int) calcStat(Stats.DWARVEN_RECIPE_LIMIT, 50, null, null) + Config.ALT_ADD_RECIPES;
	}

	public int getCommonRecipeLimit()
	{
		return (int) calcStat(Stats.COMMON_RECIPE_LIMIT, 50, null, null) + Config.ALT_ADD_RECIPES;
	}

	@Override
	public int getNpcId()
	{
		return -2;
	}

	public L2Object getVisibleObject(int id)
	{
		if(getObjectId() == id)
			return this;

		if(getTargetId() == id)
			return getTarget();

		if(_party != null)
			for(L2Player p : _party.getPartyMembers())
				if(p != null && p.getObjectId() == id)
					return p;

		if(getPet() != null && getPet().getObjectId() == id)
			return getPet();

		return L2World.getAroundObjectById(this, id);
	}

	@Override
	public int getPAtk(final L2Character target)
	{
		double init = getMountEngine().isMounted() ? (int) getMountEngine().getPAtk() : isMageClass() ? 3 : 4;
		return (int) calcStat(Stats.POWER_ATTACK, init, target, null);
	}

	@Override
	public int getPAtkSpd()
	{
		L2Weapon weapon = getActiveWeaponItem();
		double init = Formulas.getPAtkSpdFromBase(getMountEngine().isMounted() ? getMountEngine().getPAtkSpd() : weapon == null ? _template.basePAtkSpd : weapon.attackSpeed, getDEX());
		int val = Math.max((int) (calcStat(Stats.POWER_ATTACK_SPEED, init, null, null) / getArmourExpertisePenalty()), 1);
		if(Config.LIM_PATK_SPD != 0 && val > Config.LIM_PATK_SPD)
			val = Config.LIM_PATK_SPD;
		return val;
	}

	@Override
	public int getMAtk(final L2Character target, final L2Skill skill)
	{
		if(skill != null && skill.getMatak() > 0)
			return skill.getMatak();

		double init = getMountEngine().isMounted() ? (int) getMountEngine().getMAtk() : 6;
		return (int) calcStat(Stats.MAGIC_ATTACK, init, target, skill);
	}

	@Override
	public int getMAtkSpd()
	{
		int val = super.getMAtkSpd();
		if(Config.LIM_MATK_SPD != 0 && val > Config.LIM_MATK_SPD)
			val = Config.LIM_MATK_SPD;
		return val;
	}

	@Override
	public int getCriticalHit(L2Character target, L2Skill skill)
	{
		int val = (int) calcStat(Stats.CRITICAL_RATE, _template.baseCritRate, target, skill);
		if(Config.LIM_CRIT != 0 && val > Config.LIM_CRIT)
			val = Config.LIM_CRIT;
		return val;
	}

	/**
	 * Возвращает шанс магического крита в тысячных
	 */
	@Override
	public double getCriticalMagic(L2Character target, L2Skill skill)
	{
		double val = super.getCriticalMagic(target, skill);
		// GF PTS Retail limit
		if(getLevel() >= 78 && target.getLevel() >= 78 && val > 32)
			val = 32;
		else if(Config.LIM_MCRIT != 0 && val > Config.LIM_MCRIT)
			val = Config.LIM_MCRIT;
		return val;
	}

	@Override
	public int getPDef(final L2Character target)
	{
		double init = 4; //empty cloak and underwear slots

		final L2ItemInstance chest = _inventory.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		if(chest == null)
			init += isMageClass() ? L2Armor.EMPTY_BODY_MYSTIC : L2Armor.EMPTY_BODY_FIGHTER;
		if(_inventory.getPaperdollItem(Inventory.PAPERDOLL_LEGS) == null && (chest == null || chest.getBodyPart() != L2Item.SLOT_FULL_ARMOR))
			init += isMageClass() ? L2Armor.EMPTY_LEGS_MYSTIC : L2Armor.EMPTY_LEGS_FIGHTER;

		if(_inventory.getPaperdollItem(Inventory.PAPERDOLL_HEAD) == null)
			init += L2Armor.EMPTY_HELMET;
		if(_inventory.getPaperdollItem(Inventory.PAPERDOLL_GLOVES) == null)
			init += L2Armor.EMPTY_GLOVES;
		if(_inventory.getPaperdollItem(Inventory.PAPERDOLL_FEET) == null)
			init += L2Armor.EMPTY_BOOTS;

		return (int) calcStat(Stats.POWER_DEFENCE, init, target, null);
	}

	@Override
	public int getMDef(final L2Character target, final L2Skill skill)
	{
		double init = 0;

		if(_inventory.getPaperdollItem(Inventory.PAPERDOLL_LEAR) == null)
			init += L2Armor.EMPTY_EARRING;
		if(_inventory.getPaperdollItem(Inventory.PAPERDOLL_REAR) == null)
			init += L2Armor.EMPTY_EARRING;
		if(_inventory.getPaperdollItem(Inventory.PAPERDOLL_NECK) == null)
			init += L2Armor.EMPTY_NECKLACE;
		if(_inventory.getPaperdollItem(Inventory.PAPERDOLL_LFINGER) == null)
			init += L2Armor.EMPTY_RING;
		if(_inventory.getPaperdollItem(Inventory.PAPERDOLL_RFINGER) == null)
			init += L2Armor.EMPTY_RING;

		return (int) calcStat(Stats.MAGIC_DEFENCE, init, null, skill);
	}
	
	public int getMEvasionRate(final L2Character target)
	{
		return 0;
	}
	public int getMAccuracy()
	{
		return 0;
	}
	public int getMagicCriticalRate(final L2Character target, final L2Skill skill)
	{
		return 0;
	}

	public boolean isSubClassActive()
	{
		return getBaseClass() != getActiveClass();
	}

	public int getTitleColor()
	{
		return _titlecolor;
	}

	public void setTitleColor(final int color)
	{
		_titlecolor = color;
	}

	public void setTitleColor(final int red, final int green, final int blue)
	{
		_titlecolor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16);
	}

	@Override
	public boolean isCursedWeaponEquipped()
	{
		return _cursedWeaponEquippedId != 0;
	}

	public void setCursedWeaponEquippedId(int value)
	{
		_cursedWeaponEquippedId = value;
	}

	public int getCursedWeaponEquippedId()
	{
		return _cursedWeaponEquippedId;
	}

	private FishData _fish;

	public void setFish(FishData fish)
	{
		_fish = fish;
	}

	public void stopLookingForFishTask()
	{
		if(_taskforfish != null)
		{
			_taskforfish.cancel(false);
			_taskforfish = null;
		}
	}

	public void startLookingForFishTask()
	{
		if(!isDead() && _taskforfish == null)
		{
			int checkDelay = 0;
			boolean isNoob = false;
			boolean isUpperGrade = false;

			if(_lure != null)
			{
				int lureid = _lure.getItemId();
				isNoob = _fish.getGroup() == 0;
				isUpperGrade = _fish.getGroup() == 2;
				if(lureid == 6519 || lureid == 6522 || lureid == 6525 || lureid == 8505 || lureid == 8508 || lureid == 8511) //low grade
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * 1.33));
				else if(lureid == 6520 || lureid == 6523 || lureid == 6526 || lureid >= 8505 && lureid <= 8513 || lureid >= 7610 && lureid <= 7613 || lureid >= 7807 && lureid <= 7809 || lureid >= 8484 && lureid <= 8486) //medium grade, beginner, prize-winning & quest special bait
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * 1.00));
				else if(lureid == 6521 || lureid == 6524 || lureid == 6527 || lureid == 8507 || lureid == 8510 || lureid == 8513) //high grade
					checkDelay = Math.round((float) (_fish.getGutsCheckTime() * 0.66));
			}
			_taskforfish = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new LookingForFishTask(this, _fish.getWaitTime(), _fish.getFishGuts(), _fish.getType(), isNoob, isUpperGrade), 10000, checkDelay);
		}
	}

	public void startFishCombat(boolean isNoob, boolean isUpperGrade)
	{
		_fishCombat = new L2Fishing(this, _fish, isNoob, isUpperGrade);
	}

	public void endFishing(boolean win)
	{
		ExFishingEnd efe = new ExFishingEnd(win, this);
		broadcastPacket(efe);
		_fishing = false;
		_fishLoc = new Location(0, 0, 0);
		broadcastUserInfo(true);
		if(_fishCombat == null)
			sendPacket(Msg.BAITS_HAVE_BEEN_LOST_BECAUSE_THE_FISH_GOT_AWAY);
		_fishCombat = null;
		_lure = null;
		//Ends fishing
		sendPacket(Msg.ENDS_FISHING);
		setImobilised(false);
		stopLookingForFishTask();
	}

	public L2Fishing getFishCombat()
	{
		return _fishCombat;
	}

	public void setFishLoc(Location loc)
	{
		_fishLoc = loc;
	}

	public Location getFishLoc()
	{
		return _fishLoc;
	}

	public void setLure(L2ItemInstance lure)
	{
		_lure = lure;
	}

	public L2ItemInstance getLure()
	{
		return _lure;
	}

	public boolean isFishing()
	{
		return _fishing;
	}

	public void setFishing(boolean fishing)
	{
		_fishing = fishing;
	}

	private boolean _maried = false;
	private int _partnerId = 0;
	private int _coupleId = 0;
	private boolean _engagerequest = false;
	private int _engageid = 0;
	private boolean _maryrequest = false;
	private boolean _maryaccepted = false;
	private boolean _IsWearingFormalWear = false;

	public boolean isWearingFormalWear()
	{
		return _IsWearingFormalWear;
	}

	public void setIsWearingFormalWear(boolean value)
	{
		_IsWearingFormalWear = value;
	}

	public boolean isMaried()
	{
		return _maried;
	}

	public void setMaried(boolean state)
	{
		_maried = state;
	}

	public boolean isEngageRequest()
	{
		return _engagerequest;
	}

	public void setEngageRequest(boolean state, int playerid)
	{
		_engagerequest = state;
		_engageid = playerid;
	}

	public void setMaryRequest(boolean state)
	{
		_maryrequest = state;
	}

	public boolean isMaryRequest()
	{
		return _maryrequest;
	}

	public void setMaryAccepted(boolean state)
	{
		_maryaccepted = state;
	}

	public boolean isMaryAccepted()
	{
		return _maryaccepted;
	}

	public int getEngageId()
	{
		return _engageid;
	}

	public int getPartnerId()
	{
		return _partnerId;
	}

	public void setPartnerId(int partnerid)
	{
		_partnerId = partnerid;
	}

	public int getCoupleId()
	{
		return _coupleId;
	}

	public void setCoupleId(int coupleId)
	{
		_coupleId = coupleId;
	}

	public void engageAnswer(int answer)
	{
		if(!_engagerequest || _engageid == 0)
			return;

		L2Player ptarget = L2ObjectsStorage.getPlayer(_engageid);
		setEngageRequest(false, 0);
		if(ptarget != null)
			if(answer == 1)
			{
				CoupleManager.getInstance().createCouple(ptarget, this);
				ptarget.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.L2Player.EngageAnswerYes", this));
			}
			else
				ptarget.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.L2Player.EngageAnswerNo", this));
	}

	private List<L2Player> _snoopListener = new FastList<L2Player>();
	private List<L2Player> _snoopedPlayer = new FastList<L2Player>();

	public void broadcastSnoop(int type, String name, int msgId, String... params)
	{
		if(_snoopListener.size() > 0)
		{
			Snoop sn = new Snoop(getObjectId(), getName(), type, name, msgId, params);
			for(L2Player pci : _snoopListener)
				if(pci != null)
					pci.sendPacket(sn);
		}
	}

	public void addSnooper(L2Player pci)
	{
		if(!_snoopListener.contains(pci))
			_snoopListener.add(pci);
	}

	public void removeSnooper(L2Player pci)
	{
		_snoopListener.remove(pci);
	}

	public void addSnooped(L2Player pci)
	{
		if(!_snoopedPlayer.contains(pci))
			_snoopedPlayer.add(pci);
	}

	public void removeSnooped(L2Player pci)
	{
		_snoopedPlayer.remove(pci);
	}

	public TimeStamp getSkillReuseTimeStamp(int skillId)
	{
		if(disabledSkills == null)
			return null;

		return disabledSkills.get(skillId);
	}

	public long getSkillDisableTime(Integer skillId)
	{
		if(disabledSkills == null)
			return 0;

		TimeStamp sts = disabledSkills.get(skillId);

		if(sts != null && sts.hasNotPassed())
			return sts.getEndTime() - System.currentTimeMillis();

		return 0;
	}

	public DeathPenalty getDeathPenalty()
	{
		if(_classlist.get(_activeClass) != null)
			return _classlist.get(_activeClass).getDeathPenalty();
		return null;
	}

	public void setDeathPeanalty(DeathPenalty dp)
	{
		if(_classlist.get(_activeClass) != null)
			_classlist.get(_activeClass).setDeathPenalty(dp);
	}

	//fast fix for dice spam
	public long lastDiceThrown = 0;

	private boolean _charmOfCourage = false;

	public boolean isCharmOfCourage()
	{
		return _charmOfCourage;
	}

	public void setCharmOfCourage(boolean val)
	{
		_charmOfCourage = val;
		sendPacket(new EtcStatusUpdate(this));
	}

	private void revalidatePenalties()
	{
		_curWeightPenalty = 0;
		weaponPenalty = 0;
		armorPenalty = 0;
		refreshOverloaded();
		refreshExpertisePenalty();
	}

	private int _increasedForce = 0;
	private int _consumedSouls = 0;
	public Runnable _lastChargeRunnable = null;
	@SuppressWarnings("unchecked")
	public Future<?> _resetSoulTask = null;

	@Override
	public int getIncreasedForce()
	{
		return _increasedForce;
	}

	@Override
	public int getConsumedSouls()
	{
		return _consumedSouls;
	}

	private ReentrantLock _changeSoulsLock = new ReentrantLock();

	@Override
	public void increaseSouls(int souls)
	{
		_changeSoulsLock.lock();
		try
		{
			int max = (int) calcStat(Stats.SOULS_LIMIT, 0, null, null);
			if(_consumedSouls + souls > max)
				souls = max - _consumedSouls;

			if(_resetSoulTask != null)
				_resetSoulTask.cancel(false);

			_resetSoulTask = ThreadPoolManager.getInstance().scheduleGeneral(new ResetSoulTask(this), 600000);

			if(souls > 0)
			{
				_consumedSouls += souls;
				SystemMessage sm = new SystemMessage(SystemMessage.YOUR_SOUL_HAS_INCREASED_BY_S1_SO_IT_IS_NOW_AT_S2);
				sm.addNumber(souls);
				sm.addNumber(_consumedSouls);
				sendPacket(sm);
				sendPacket(new EtcStatusUpdate(this));
			}
			else
				sendPacket(Msg.SOUL_CANNOT_BE_ABSORBED_ANY_MORE);
		}
		catch(Exception e)
		{
		}
		finally
		{
			_changeSoulsLock.unlock();
		}
	}

	@Override
	public void decreaseSouls(int souls)
	{
		_changeSoulsLock.lock();
		try
		{
			_consumedSouls = Math.max(0, _consumedSouls - souls);

			if(_consumedSouls == 0 && _resetSoulTask != null)
			{
				_resetSoulTask.cancel(false);
				_resetSoulTask = null;
			}
			else
			{
				if(_resetSoulTask != null)
					_resetSoulTask.cancel(false);

				_resetSoulTask = ThreadPoolManager.getInstance().scheduleGeneral(new ResetSoulTask(this), 600000);
			}

			sendPacket(new EtcStatusUpdate(this));
		}
		catch(Exception e)
		{
		}
		finally
		{
			_changeSoulsLock.unlock();
		}
	}

	@Override
	public void setIncreasedForce(int i)
	{
		if(i > 8)
			i = 8;

		if(i < 0)
			i = 0;

		if(i != 0 && i > _increasedForce)
			sendPacket(new SystemMessage(SystemMessage.YOUR_FORCE_HAS_INCREASED_TO_S1_LEVEL).addNumber(i));
		else if(i == 0)
			_lastChargeRunnable = null;

		_increasedForce = i;
		sendPacket(new EtcStatusUpdate(this));
	}

	public void falling(int height, int safeHeight)
	{
		if(isDead() || isFlying())
			return;

		int curHp = (int) getCurrentHp();
		int damage = (int) calcStat(Stats.FALL_DAMAGE, height - safeHeight, null, null);
		if(curHp - damage < 1)
			setCurrentHp(1);
		else
			setCurrentHp(curHp - damage);
		sendPacket(new SystemMessage(SystemMessage.YOU_RECEIVED_S1_DAMAGE_FROM_TAKING_A_HIGH_FALL).addNumber(damage));
	}

	/**
	 * Системные сообщения о текущем состоянии хп
	 */
	@Override
	public void checkHpMessages(double curHp, double newHp)
	{
		//сюда пасивные скиллы
		byte[] _hp = {30, 30};
		int[] skills = {290, 291};

		//сюда активные эффекты
		int[] _effects_skills_id = {176, 292, 292};
		byte[] _effects_hp = {30, 30, 60};

		double percent = getMaxHp() / 100;
		double _curHpPercent = curHp / percent;
		double _newHpPercent = newHp / percent;
		boolean needsUpdate = false;

		//check for passive skills
		for(int i = 0; i < skills.length; i++)
		{
			int level = getSkillLevel(skills[i]);
			if(level > 0)
				if(_curHpPercent > _hp[i] && _newHpPercent <= _hp[i])
				{
					sendPacket(new SystemMessage(SystemMessage.SINCE_HP_HAS_DECREASED_THE_EFFECT_OF_S1_CAN_BE_FELT).addSkillName(skills[i]));
					needsUpdate = true;
				}
				else if(_curHpPercent <= _hp[i] && _newHpPercent > _hp[i])
				{
					sendPacket(new SystemMessage(SystemMessage.SINCE_HP_HAS_INCREASED_THE_EFFECT_OF_S1_WILL_DISAPPEAR).addSkillName(skills[i]));
					needsUpdate = true;
				}
		}

		//check for active effects
		for(Integer i = 0; i < _effects_skills_id.length; i++)
			if(getEffectBySkillId(_effects_skills_id[i]) != null)
				if(_curHpPercent > _effects_hp[i] && _newHpPercent <= _effects_hp[i])
				{
					sendPacket(new SystemMessage(SystemMessage.SINCE_HP_HAS_DECREASED_THE_EFFECT_OF_S1_CAN_BE_FELT).addSkillName(_effects_skills_id[i]));
					needsUpdate = true;
				}
				else if(_curHpPercent <= _effects_hp[i] && _newHpPercent > _effects_hp[i])
				{
					sendPacket(new SystemMessage(SystemMessage.SINCE_HP_HAS_INCREASED_THE_EFFECT_OF_S1_WILL_DISAPPEAR).addSkillName(_effects_skills_id[i]));
					needsUpdate = true;
				}

		if(needsUpdate)
			sendChanges();
	}

	/**
	 * Системные сообщения для темных эльфов о вкл/выкл ShadowSence (skill id = 294)
	 */
	public void checkDayNightMessages()
	{
		int level = getSkillLevel(294);
		if(level > 0)
			if(GameTimeController.getInstance().isNowNight())
				sendPacket(new SystemMessage(SystemMessage.IT_IS_NOW_MIDNIGHT_AND_THE_EFFECT_OF_S1_CAN_BE_FELT).addSkillName(294));
			else
				sendPacket(new SystemMessage(SystemMessage.IT_IS_DAWN_AND_THE_EFFECT_OF_S1_WILL_NOW_DISAPPEAR).addSkillName(294));
		sendChanges();
	}

	@Override
	public void sendMessage(String message)
	{
		sendPacket(SystemMessage.sendString(message));
	}

	@Override
	public float getSwimSpeed()
	{
		return (float) calcStat(Stats.RUN_SPEED, Config.SWIMING_SPEED, null, null);
	}

	private Location _lastClientPosition;
	private Location _lastServerPosition;
	private long _lastClientValidate;
	public double clientSpeedMod;

	@Override
	public void setLastClientPosition(Location position)
	{
		if(position != null && _lastClientPosition != null)
		{
			long tm = System.currentTimeMillis() - _lastClientValidate;
			double d = position.distance3D(_lastClientPosition);
			if(tm > 0 && tm < 2000 && d > 0)
				clientSpeedMod = d / tm / getMoveSpeed() * 1000f;
		}
		_lastClientPosition = position;
		_lastClientValidate = System.currentTimeMillis();
	}

	public Location getLastClientPosition()
	{
		return _lastClientPosition;
	}

	@Override
	public void setLastServerPosition(Location position)
	{
		_lastServerPosition = position;
	}

	public Location getLastServerPosition()
	{
		return _lastServerPosition;
	}

	private int _useSeed = 0;

	public void setUseSeed(int id)
	{
		_useSeed = id;
	}

	public int getUseSeed()
	{
		return _useSeed;
	}

	public Integer getRelation(L2Player target)
	{
		Integer result = 0;

		if(getClanId() != 0)
		{
			result |= RelationChanged.RELATION_CLAN_MEMBER;
			if(getClanId() == target.getClanId())
				result |= RelationChanged.RELATION_CLAN_MATE;
			if(getAllyId() != 0)
				result |= RelationChanged.RELATION_ALLY_MEMBER;
			if(isClanLeader())
				result |= RelationChanged.RELATION_LEADER;
		}

		if(getParty() != null && target.getParty() == getParty())
		{
			result |= RelationChanged.RELATION_HAS_PARTY;
			if(getParty().isLeader(this))
				result |= RelationChanged.RELATION_PARTYLEADER;
			else
			{
				int c = getParty().getPartyMembers().indexOf(this) + 1;
				if(c > 0)
					c = 9 - c;
				result |= c;
			}
		}

		if(_siegeState != 0 && target.getSiegeState() != 0)
		{
			if(_siegeState == 3 && target.getSiegeState() == 3) // TW
				result |= RelationChanged.RELATION_TERRITORY_WAR;
			else if(_siegeId == target.getSiegeId()) // Siege
			{
				result |= RelationChanged.RELATION_INSIEGE;
				if(_siegeState == 1)
				{
					result |= RelationChanged.RELATION_ATTACKER;
					if(target.getSiegeState() == 1 && (ResidenceManager.getInstance().getBuildingById(target.getSiegeId()).getSiege().isTempAllyActive() || target.getClanId() == getClanId()))
						result |= RelationChanged.RELATION_ALLY;
					else
						result |= RelationChanged.RELATION_ENEMY;
				}
				else if(target.getSiegeState() == 2)
					result |= RelationChanged.RELATION_ALLY;
				else
					result |= RelationChanged.RELATION_ENEMY;
			}
		}

		L2Clan targetClan = null;
		if(target.getClanId() != 0)
		{
			targetClan = target.getClan();
			if(targetClan == null)
			{
				_log.warn("WTF!! clanId: " + target.getClanId() + " clan null!!! " + target);
				target.setClan(null);
			}
		}

		if(getClanId() != 0 && targetClan != null && targetClan.isAtWarWith(getClanId()) && target.getPledgeType() != L2Clan.SUBUNIT_ACADEMY)
		{
			result |= RelationChanged.RELATION_1SIDED_WAR;
			if(getClan().isAtWarWith(target.getClanId()) && getPledgeType() != L2Clan.SUBUNIT_ACADEMY)
				result |= RelationChanged.RELATION_MUTUAL_WAR;
		}

		if(isAttackable(target, false, false))
			result |= RelationChanged.RELATION_ATTACKABLE;

		return result;
	}

	/**
	 * The PvP Flag state of the L2Player (0=White, 1=Purple, 2=PurpleBlink)
	 */
	protected int _pvpFlag;
	@SuppressWarnings("unchecked")
	private Future<?> _pvPTask;
	private long _lastPvpAttack;

	public long getLastPvpAttack()
	{
		return _lastPvpAttack;
	}

	@Override
	public void startPvPFlag(L2Character target)
	{
		long startTime = System.currentTimeMillis();
		if(target != null && target.getPvpFlag() != 0)
			startTime -= Config.PVP_TIME / 2;
		if(_pvpFlag != 0 && _lastPvpAttack > startTime)
			return;
		_lastPvpAttack = startTime;

		updatePvPFlag(1);

		if(_pvPTask == null)
			_pvPTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new PvPFlagTask(this), 1000, 1000);
	}

	public void stopPvPFlag()
	{
		if(_pvPTask != null)
			_pvPTask.cancel(true);
		_pvPTask = null;
		updatePvPFlag(0);
	}

	public void updatePvPFlag(int value)
	{
		if(_pvpFlag == value)
			return;
		setPvpFlag(value);

		if(_karma < 1)
		{
			broadcastPacket(new StatusUpdate(getObjectId()).addAttribute(StatusUpdate.PVP_FLAG, value));
			if(getPet() != null)
				getPet().broadcastPetInfo();
		}
		for(L2Player player : L2World.getAroundPlayers(this))
			sendRelation(player);
	}

	/**
	 * Set the PvP Flag of the L2Player.<BR><BR>
	 *
	 * @param pvpFlag new value of pvpFlag to set
	 */
	public void setPvpFlag(int pvpFlag)
	{
		_pvpFlag = pvpFlag;
	}

	/**
	 * @return pvpFlag of this L2Player
	 */
	@Override
	public int getPvpFlag()
	{
		return _pvpFlag;
	}

	private long _lastPacket = 0;

	public long getLastPacket()
	{
		return _lastPacket;
	}

	public void setLastPacket()
	{
		_lastPacket = System.currentTimeMillis();
	}

	public byte[] getKeyBindings()
	{
		return _keyBindings;
	}

	public void setKeyBindings(byte[] keyBindings)
	{
		if(keyBindings == null)
			keyBindings = new byte[0];
		_keyBindings = keyBindings;
	}

	/**
	 * Устанавливает режим трансформаии<BR>
	 *
	 * @param transformationId идентификатор трансформации
	 *                         Известные режимы:<BR>
	 *                         <li>0 - стандартный вид чара
	 *                         <li>1 - Onyx Beast
	 *                         <li>2 - Death Blader
	 *                         <li>etc.
	 */
	public void setTransformation(int transformationId)
	{
		if(transformationId == _transformationId || _transformationId != 0 && transformationId != 0)
			return;

		_transformationId = transformationId;

		// Для каждой трансформации свой набор скилов
		if(_transformationId == 0) // Обычная форма
		{
			L2Effect transform = getEffectByAbnormalType("transformation");
			if(transform != null)
				for(AddedSkill as : transform.getSkill().getAddedSkills())
					if(as.getLevel() == -1)
						_transformationSkills.remove(as.getSkillId());

			// Удаляем скилы трансформации
			if(!_transformationSkills.isEmpty())
			{
				for(L2Skill s : _transformationSkills.values())
				{
					if(s.isPassive())
						removeSkill(s, false);

					_skills.remove(s.getId());
				}
				_transformationSkills.clear();
			}
		}
		else
		{
			// Добавляем скилы трансформации
			// Для все трансформаций кроме проклятых добавляем скилы:
			// - обратной трансформации (619)
			// - Decrease Bow/Crossbow Attack Speed (5491)
			if(!isCursedWeaponEquipped())
			{
				int transformSkillId = 0;
				final L2Effect effect = getEffectByAbnormalType("transformation");
				L2SkillLearn sl = null;
				if(effect != null)
				{
					transformSkillId = effect.getSkillId();
					for(AddedSkill s : effect.getSkill().getAddedSkills())
					{
						L2Skill skill = s.getSkill(this);
						if(skill != null)
							_transformationSkills.put(s.getSkillId(), skill);
					}

					sl = SkillTreeTable.getSkillLearn(transformSkillId, (short) 1, getClassId(), null);
				}

				if(sl != null && sl.isNormal())
					_transformationSkills.put(838, SkillTable.getInstance().getInfo(838, 1));
				else
				{
					if(_transformationId != 303 && _transformationId != 111 && _transformationId != 112 && _transformationId != 113 && _transformationId != 121 && _transformationId != 122 && _transformationId != 124)
						_transformationSkills.put(619, SkillTable.getInstance().getInfo(619, 1));
					_transformationSkills.put(5491, SkillTable.getInstance().getInfo(5491, 1));
				}
			}
			else
			{
				//добавляем скилы для трансформации с проклятым оружием
				_transformationSkills.put(transformationId == 301 ? 3329 : 3328, SkillTable.getInstance().getInfo(transformationId == 301 ? 3329 : 3328, 1));
				_transformationSkills.put(3330, SkillTable.getInstance().getInfo(3330, 1));
				_transformationSkills.put(3331, SkillTable.getInstance().getInfo(3331, 1));
				_transformationSkills.put(3630, SkillTable.getInstance().getInfo(3630, 1));
				_transformationSkills.put(3631, SkillTable.getInstance().getInfo(3631, 1));
			}

			for(L2Skill skill : _transformationSkills.values())
				if(skill.isPassive())
					addSkill(skill, false);

			_skills.putAll(_transformationSkills);

			for(L2Skill sk : _transformationSkills.values())
				if(getAllShortCuts().size() > 0 && sk.getLevel() > 1)
					for(L2ShortCut sc : getAllShortCuts())
						if(sc.id == sk.getId() && sc.type == L2ShortCut.TYPE_SKILL && sc.level != sk.getDisplayLevel())
						{
							L2ShortCut newsc = new L2ShortCut(sc.slot, sc.page, sc.type, sc.id, sk.getDisplayLevel());
							sendPacket(new ShortCutRegister(newsc));
							registerShortCut(newsc);
						}
		}

		if(_transformationId == 538 && getActiveWeaponItem() != null)
			getInventory().unEquipItemAndSendChanges(getActiveWeaponInstance());
		sendPacket(new ExBasicActionList(this));
		sendPacket(new SkillList(this));
		sendPacket(new ExAcquirableSkillListByClass(this));
		sendPacket(new ShortCutInit(this));
		for(int shotId : getAutoSoulShot())
			sendPacket(new ExAutoSoulShot(shotId, true));
		broadcastUserInfo(true);
	}

	/**
	 * Возвращает режим трансформации
	 *
	 * @return ID режима трансформации
	 */
	public int getTransformation()
	{
		return _transformationId;
	}

	/**
	 * Возвращает имя трансформации
	 *
	 * @return String
	 */
	public String getTransformationName()
	{
		return _transformationName;
	}

	public boolean isInFlyingTransform()
	{
		return _transformationId == 8 || _transformationId == 9 || _transformationId == 260;
	}

	/**
	 * Устанавливает имя трансформаии
	 *
	 * @param name имя трансформации
	 */
	public void setTransformationName(String name)
	{
		_transformationName = name;
	}

	/**
	 * Устанавливает шаблон трансформации, используется для определения коллизий
	 *
	 * @param template ID шаблона
	 */
	public void setTransformationTemplate(int template)
	{
		_transformationTemplate = template;
	}

	/**
	 * Возвращает шаблон трансформации, используется для определения коллизий
	 *
	 * @return NPC ID
	 */
	public int getTransformationTemplate()
	{
		return _transformationTemplate;
	}

	/**
	 * Возвращает коллекцию скиллов, с учетом текущей трансформации
	 */
	@Override
	public final Collection<L2Skill> getAllSkills()
	{
		// Трансформация неактивна
		if(_transformationId == 0)
			return _skills.values();

		// Трансформация активна
		HashMap<Integer, L2Skill> tempSkills = new HashMap<Integer, L2Skill>();
		for(L2Skill s : _skills.values())
			if(s != null && s.isPassive())
				tempSkills.put(s.getId(), s);
		tempSkills.putAll(_transformationSkills); // Добавляем к пассивкам скилы текущей трансформации
		return tempSkills.values();
	}

	/**
	 * Устанавливает агнишена
	 * <p/>
	 * template ID шаблона NPC агнишена<BR>
	 */
	public void setAgathion(int id)
	{
		_agathionId = id;
		broadcastUserInfo(true);
	}

	/**
	 * Возвращает агнишена
	 *
	 * @return L2AgathionInstance
	 */
	public int getAgathionId()
	{
		return _agathionId;
	}

	/**
	 * Возвращает количество PcBangPoint'ов даного игрока
	 *
	 * @return количество PcCafe Bang Points
	 */
	public int getPcBangPoints()
	{
		return pcBangPoints;
	}

	/**
	 * Устанавливает количество Pc Cafe Bang Points для даного игрока
	 *
	 * @param pcBangPoints новое количество PcCafeBangPoints
	 */
	public void setPcBangPoints(int pcBangPoints)
	{
		this.pcBangPoints = pcBangPoints;
	}

	private Location _groundSkillLoc;

	public void setGroundSkillLoc(Location location)
	{
		_groundSkillLoc = location;
	}

	public Location getGroundSkillLoc()
	{
		return _groundSkillLoc;
	}

	public boolean isDeleting()
	{
		return _isDeleting;
	}

	public void setOfflineMode(boolean val)
	{
		if(val)
		{
			if(getVar("offline") == null)
				setVar("offline", "1", (int) (System.currentTimeMillis() / 1000) + Config.SERVICES_OFFLINE_TRADE_DAYS_TO_KICK * 24 * 60 * 60);
		}
		else
			unsetVar("offline");
		_offline = val;
	}

	public boolean isInOfflineMode()
	{
		return _offline;
	}

	public void saveTradeList()
	{

		if(!Config.ALT_SAVE_PRIVATE_STORE && !isInOfflineMode())
			return;

		String val = "";

		if(_sellList == null || _sellList.isEmpty())
			unsetVar("selllist");
		else
		{
			for(TradeItem i : _sellList)
				val += i.getObjectId() + ";" + i.getCount() + ";" + i.getOwnersPrice() + ":";
			setVar("selllist", val);
			val = "";
			if(_tradeList != null && _tradeList.getSellStoreName() != null)
				setVar("sellstorename", _tradeList.getSellStoreName());
		}

		if(_buyList == null || _buyList.isEmpty())
			unsetVar("buylist");
		else
		{
			for(TradeItem i : _buyList)
				val += i.getItemId() + ";" + i.getCount() + ";" + i.getOwnersPrice() + ":";
			setVar("buylist", val);
			val = "";
			if(_tradeList != null && _tradeList.getBuyStoreName() != null)
				setVar("buystorename", _tradeList.getBuyStoreName());
		}

		if(_createList == null || _createList.getList().isEmpty())
			unsetVar("createlist");
		else
		{
			for(L2ManufactureItem i : _createList.getList())
				val += i.getRecipeId() + ";" + i.getCost() + ":";
			setVar("createlist", val);
			if(_createList.getStoreName() != null)
				setVar("manufacturename", _createList.getStoreName());
		}
	}

	public void restoreTradeList()
	{
		if(getVar("selllist") != null)
		{
			_sellList = new ConcurrentLinkedQueue<TradeItem>();
			String[] items = getVar("selllist").split(":");
			for(String item : items)
			{
				if(item.equals(""))
					continue;
				String[] values = item.split(";");
				if(values.length < 3)
					continue;

				int oId = Integer.parseInt(values[0]);
				long count = Long.parseLong(values[1]);
				long price = Long.parseLong(values[2]);

				L2ItemInstance itemToSell = getInventory().getItemByObjectId(oId);

				if(count < 1 || itemToSell == null)
					continue;

				if(count > itemToSell.getCount())
					count = itemToSell.getCount();

				TradeItem i = new TradeItem(itemToSell);
				i.setCount(count);
				i.setOwnersPrice(price);

				_sellList.add(i);
			}
			if(_tradeList == null)
				_tradeList = new L2TradeList();
			if(getVar("sellstorename") != null)
				_tradeList.setSellStoreName(getVar("sellstorename"));
		}
		if(getVar("buylist") != null)
		{
			_buyList = new ConcurrentLinkedQueue<TradeItem>();
			String[] items = getVar("buylist").split(":");
			for(String item : items)
			{
				if(item.equals(""))
					continue;
				String[] values = item.split(";");
				if(values.length < 3)
					continue;
				TradeItem i = new TradeItem();
				i.setItemId(Integer.parseInt(values[0]));
				i.setCount(Long.parseLong(values[1]));
				i.setOwnersPrice(Long.parseLong(values[2]));
				_buyList.add(i);
			}
			if(_tradeList == null)
				_tradeList = new L2TradeList();
			if(getVar("buystorename") != null)
				_tradeList.setBuyStoreName(getVar("buystorename"));
		}
		if(getVar("createlist") != null)
		{
			_createList = new L2ManufactureList();
			String[] items = getVar("createlist").split(":");
			for(String item : items)
			{
				if(item.equals(""))
					continue;
				String[] values = item.split(";");
				if(values.length < 2)
					continue;
				_createList.add(new L2ManufactureItem(Integer.parseInt(values[0]), Long.parseLong(values[1])));
			}
			if(getVar("manufacturename") != null)
				_createList.setStoreName(getVar("manufacturename"));
		}
	}

	public void restoreRecipeBook()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT id FROM character_recipebook WHERE char_id=?");
			statement.setInt(1, getObjectId());
			rset = statement.executeQuery();

			while(rset.next())
			{
				int id = rset.getInt("id");
				RecipeList recipe = RecipeController.getRecipeList(id);
				registerRecipe(recipe, false);
			}
		}
		catch(Exception e)
		{
			_log.warn("count not recipe skills:" + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public L2DecoyInstance getDecoy()
	{
		return _decoy;
	}

	public void setDecoy(L2DecoyInstance decoy)
	{
		_decoy = decoy;
		sendUserInfo(false);
	}

	@Override
	public float getColRadius()
	{
		if(getTransformation() != 0 && getTransformationTemplate() != 0)
		{
			if(NpcTable.getTemplate(getTransformationTemplate()) == null)
			{
				_log.warn("L2Player: can't find transform template: " + getTransformationTemplate());
				return getBaseTemplate().collisionRadius;
			}
			return NpcTable.getTemplate(getTransformationTemplate()).collisionRadius;
		}
		else if(getMountEngine().isMounted())
			return NpcTable.getTemplate(getMountEngine().getMountNpcId()).collisionRadius;
		else
			return getBaseTemplate().collisionRadius;
	}

	@Override
	public float getColHeight()
	{
		if(getTransformation() != 0 && getTransformationTemplate() != 0)
			return NpcTable.getTemplate(getTransformationTemplate()).collisionHeight;
		else if(getMountEngine().isMounted())
			return NpcTable.getTemplate(getMountEngine().getMountNpcId()).collisionHeight + getBaseTemplate().collisionHeight;
		else
			return getBaseTemplate().collisionHeight;
	}

	private boolean _combatFlagEquippedId = false;

	public boolean isCombatFlagEquipped()
	{
		return _combatFlagEquippedId;
	}

	public void setCombatFlagEquipped(boolean value)
	{
		_combatFlagEquippedId = value;
	}

	private long timeInPeaceZoneWithTW;
	private long lastTimeUpdate;

	public void updateTimeInPeaceZoneWithWard()
	{
		if(isInZonePeace())
		{
			if(lastTimeUpdate == 0)
			{
				lastTimeUpdate = System.currentTimeMillis();
				return;
			}

			timeInPeaceZoneWithTW += System.currentTimeMillis() - lastTimeUpdate;
			lastTimeUpdate = System.currentTimeMillis();
		}
		else
			lastTimeUpdate = 0;
	}

	public long getTimeInPeaceZoneWithTW()
	{
		return timeInPeaceZoneWithTW;
	}

	public void setTimeInPeaceZoneWithTW(long timeInPeaceZoneWithTW)
	{
		this.timeInPeaceZoneWithTW = timeInPeaceZoneWithTW;
	}

	public Vitality getVitality()
	{
		return _vitality;
	}

	public HuntingBonus getHuntingBonus()
	{
		return _huntingBonus;
	}

	public void setHuntingBonus(HuntingBonus val)
	{
		_huntingBonus = val;
	}

	public RecommendSystem getRecSystem()
	{
		return _recommendSystem;
	}

	public void setVitality(Vitality value)
	{
		_vitality = value;
	}

	public void saveCharToDisk()
	{
		try
		{
			getInventory().updateDatabase(true);
			store();
		}
		catch(Exception e)
		{
			_log.warn("Error saving player character: " + e);
			e.printStackTrace();
		}
	}

	@Override
	public String getVisibleName()
	{
		return isCursedWeaponEquipped() ? getTransformationName() : getName();
	}

	@Override
	public void sendDamageMessage(L2Character target, int damage, boolean miss, boolean pcrit, boolean block)
	{
		if(!miss && pcrit)
			sendPacket(new SystemMessage(SystemMessage.S1_HAD_A_CRITICAL_HIT).addCharName(this));


		if(miss)
		{
			sendPacket(new SystemMessage(SystemMessage.S1S_ATTACK_WENT_ASTRAY).addCharName(this));
			if(target != null && target.isPlayer())
				target.sendPacket(new SystemMessage(SystemMessage.S1_HAS_EVADED_S2S_ATTACK).addCharName(target).addCharName(this));
			return;
		}

		if(block)
		{
			sendPacket(Msg.THE_ATTACK_HAS_BEEN_BLOCKED);
			return;
		}

		SystemMessage sm = null;
		if(target.isPlayer())
		{
			double trans = Math.min(100, target.calcStat(Stats.TRANSFER_DAMAGE_PERCENT, 0, null, null));
			if(trans > 1)
			{
				L2Summon summon = target.getPet();
				if(summon != null && !summon.isDead() && summon.isInRange(this, 1000) && summon.isSummon() && summon.isInZonePeace() == isInZonePeace())
				{
					trans *= (double) damage / 100;
					if(summon.getCurrentHp() > trans)
					{
						damage -= trans;
						sm = new SystemMessage(SystemMessage.YOU_HAVE_GIVEN_S1_DAMAGE_TO_YOUR_TARGET_AND_S2_DAMAGE_TO_THE_SERVITOR);
						sm.addNumber(damage);
						sm.addNumber((int) trans);
					}
				}
			}
		}

		if(sm == null)
			sm = new SystemMessage(SystemMessage.S1_HAS_GIVEN_S2_DAMAGE_OF_S3).addCharName(this).addCharName(target).addNumber(damage);

		sendPacket(sm);
	}

	@Override
	public void sendSkillReuseMessage(int skillId, int level, L2ItemInstance usedItem)
	{
		TimeStamp timeStamp;
		if(usedItem != null && usedItem.getItem().getDelayShareGroup() > 0)
		{
			timeStamp = getSkillReuseTimeStamp(-usedItem.getItem().getDelayShareGroup());
			skillId = -1;
		}
		else
			timeStamp = getSkillReuseTimeStamp(skillId);
		SystemMessage sm;
		if(timeStamp != null && timeStamp.getEndTime() > System.currentTimeMillis())
		{
			int remainingTime = (int) (timeStamp.getEndTime() - System.currentTimeMillis()) / 1000;
			int hours = remainingTime / 3600;
			int minutes = (remainingTime % 3600) / 60;
			int seconds = (remainingTime % 60);

			if(seconds > 0)
			{
				if(hours > 0)
				{
					sm = new SystemMessage(SystemMessage.THERE_ARE_S2_HOURS_S3_MINUTES_AND_S4_SECONDS_REMAINING_IN_S1S_RE_USE_TIME);
					if(skillId > 0)
						sm.addSkillName(skillId, level);
					else
						sm.addItemName(usedItem.getItemId());
					sm.addNumber(hours);
					sm.addNumber(minutes);
				}
				else if(minutes > 0)
				{
					sm = new SystemMessage(SystemMessage.THERE_ARE_S2_MINUTES_S3_SECONDS_REMAINING_IN_S1S_RE_USE_TIME);
					if(skillId > 0)
						sm.addSkillName(skillId, level);
					else
						sm.addItemName(usedItem.getItemId());
					sm.addNumber(minutes);
				}
				else
				{
					sm = new SystemMessage(SystemMessage.THERE_ARE_S2_SECONDS_REMAINING_IN_S1S_RE_USE_TIME);
					if(skillId > 0)
						sm.addSkillName(skillId, level);
					else
						sm.addItemName(usedItem.getItemId());
				}

				sm.addNumber(seconds);
				sendPacket(sm);
			}
			else
				sendActionFailed();
		}
	}

	public Integer getPartyRoom()
	{
		return partyRoom;
	}

	public void setPartyRoom(Integer partyRoom)
	{
		this.partyRoom = partyRoom;
	}

	public int getPartyMatchingRegion()
	{
		return _partyMatchingRegion;
	}

	public void setPartyMatchingRegion(final int region)
	{
		_partyMatchingRegion = region;
	}

	public int getPartyMatchingLevels()
	{
		return _partyMatchingLevels;
	}

	public void setPartyMatchingLevels(final int levels)
	{
		_partyMatchingLevels = levels;
	}

	public final Map<Integer, Integer> getKnownRelations()
	{
		if(_knownRelations == null)
			_knownRelations = new ConcurrentHashMap<>();
		return _knownRelations;
	}

	/**
	 * Destroy item from inventory by using its <B>itemId</B> and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 *
	 * @param process	 : String Identifier of process triggering this action
	 * @param itemId	  : int Item identifier of the item to be destroyed
	 * @param count	   : int Quantity of items to be destroyed
	 * @param reference   : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItemByItemId(String process, int itemId, long count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = _inventory.getItemByItemId(itemId);

		if(item == null || item.getCount() < count || _inventory.destroyItemByItemId(process, itemId, count, this, reference) == null)
		{
			if(sendMessage)
			{
				if(itemId == 57)
					sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
				else
					sendPacket(Msg.INCORRECT_ITEM_COUNT);
			}

			return false;
		}

		// Sends message to client if requested
		if(sendMessage)
		{
			SystemMessage sm;
			if(itemId == 57)
			{
				sm = new SystemMessage(SystemMessage.S1_ADENA_DISAPPEARED);
				sendPacket(new ExAdenaInvenCount(this));
				sm.addNumber(count);
			}
			else
			{
				if(count > 1)
				{
					sm = new SystemMessage(SystemMessage.S2_S1_HAS_DISAPPEARED);
					sm.addItemName(itemId);
					sm.addNumber(count);
				}
				else
				{
					sm = new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED);
					sm.addItemName(itemId);
				}
			}
			sendPacket(sm);
		}

		return true;
	}

	/**
	 * Destroys item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 *
	 * @param process	 : String Identifier of process triggering this action
	 * @param objectId	: int Item Instance identifier of the item to be destroyed
	 * @param count	   : int Quantity of items to be destroyed
	 * @param reference   : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItem(String process, int objectId, long count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = _inventory.getItemByObjectId(objectId);

		if(item == null || item.getCount() < count || _inventory.destroyItem(process, objectId, count, this, reference) == null)
		{
			if(sendMessage)
				sendPacket(Msg.INCORRECT_ITEM_COUNT);

			return false;
		}

		// Sends message to client if requested
		if(sendMessage)
		{
			SystemMessage sm;
			if(count > 1)
			{
				sm = new SystemMessage(SystemMessage.S2_S1_HAS_DISAPPEARED);
				sm.addNumber(count);
			}
			else
				sm = new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED);

			sm.addItemName(item.getItemId());
			sendPacket(sm);
		}

		return true;
	}

	public long getItemCountByItemId(int itemId)
	{
		L2ItemInstance item = getInventory().getItemByItemId(itemId);
		if(item != null)
		{
			if(item.isStackable())
				return item.getCount();
			else
			{
				long total = 0;
				for(L2ItemInstance i : getInventory().getItemsList())
					if(i.getItemId() == itemId)
						total++;
				return total;
			}
		}
		return 0;
	}

	public void setFame(int pRP)
	{
		_famePoints = pRP;
	}

	public void addFame(int pRP)
	{

		if(getFame() + pRP > Config.ALT_MAX_FAME_POINTS)
			pRP = Config.ALT_MAX_FAME_POINTS - getFame();

		_famePoints = getFame() + pRP;
		if(pRP > 0)
			sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_ACQUIRED_S1_REPUTATION_SCORE).addNumber(pRP));
		sendUserInfo(true);
	}

	public int getFame()
	{
		return _famePoints;
	}

	/**
	 * Adds item to inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 *
	 * @param process	 : String Identifier of process triggering this action
	 * @param item		: L2ItemInstance to be added
	 * @param reference   : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addItem(String process, L2ItemInstance item, L2Object reference, boolean sendMessage)
	{
		if(item.getCount() > 0)
		{
			// Sends message to client if requested
			if(sendMessage)
			{
				SystemMessage sm;
				if(process.equalsIgnoreCase("Pickup") || process.equalsIgnoreCase("Loot") || process.equals("Party"))
				{
					if(item.getItemId() == 57)
					{
						sm = new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED_S1_ADENA);
						sendPacket(new ExAdenaInvenCount(this));
						sm.addNumber(item.getCount());
					}
					else if(item.getEnchantLevel() > 0)
					{
						sm = new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED__S1S2);
						sm.addNumber(item.getEnchantLevel());
						sm.addItemName(item.getItemId());
					}
					else if(item.getCount() == 1)
					{
						sm = new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED_S1);
						sm.addItemName(item.getItemId());
					}
					else
					{
						sm = new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED_S2_S1);
						sm.addItemName(item.getItemId());
						sm.addNumber(item.getCount());
					}
				}
				else
				{
					if(item.getItemId() == 57)
					{
						sm = new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1_ADENA);
						sendPacket(new ExAdenaInvenCount(this));
						sm.addNumber(item.getCount());
					}
					else if(item.getCount() == 1)
					{
						sm = new SystemMessage(SystemMessage.YOU_HAVE_EARNED__S1);
						sm.addItemName(item.getItemId());
					}
					else
					{
						sm = new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1_S);
						sm.addItemName(item.getItemId());
						sm.addNumber(item.getCount());
					}
				}
				sendPacket(sm);
			}

			// Add the item to inventory
			getInventory().addItem(process, item, this, reference);
		}
	}

	/**
	 * Adds item to Inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 *
	 * @param process	 : String Identifier of process triggering this action
	 * @param itemId	  : int Item Identifier of the item to be added
	 * @param count	   : int Quantity of items to be added
	 * @param reference   : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 */
	public void addItem(String process, int itemId, long count, L2Object reference, boolean sendMessage)
	{
		if(count > 0)
		{
			// Sends message to client if requested
			if(sendMessage)
			{
				SystemMessage sm;
				if(process.equalsIgnoreCase("Pickup") || process.equalsIgnoreCase("Loot") || process.equals("Party"))
				{
					if(itemId == 57)
					{
						sm = new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED_S1_ADENA);
						sendPacket(new ExAdenaInvenCount(this));
						sm.addNumber(count);
					}
					else if(count == 1)
					{
						sm = new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED_S1);
						sm.addItemName(itemId);
					}
					else
					{
						sm = new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED_S2_S1);
						sm.addItemName(itemId);
						sm.addNumber(count);
					}
				}
				else
				{
					if(itemId == 57)
					{
						sm = new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1_ADENA);
						sendPacket(new ExAdenaInvenCount(this));
						sm.addNumber(count);
					}
					else if(count == 1)
					{
						sm = new SystemMessage(SystemMessage.YOU_HAVE_EARNED__S1);
						sm.addItemName(itemId);
					}
					else
					{
						sm = new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1_S);
						sm.addItemName(itemId);
						sm.addNumber(count);
					}
				}
				sendPacket(sm);
			}

			// Add the item to inventory
			getInventory().addItem(process, itemId, count, this, reference);
		}
	}

	/**
	 * Transfers item to another ItemContainer and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 *
	 * @param process   : String Identifier of process triggering this action
	 * @param objectId  : int Item Identifier of the item to be transfered
	 * @param count	 : int Quantity of items to be transfered
	 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item or the updated item in inventory
	 */
	public L2ItemInstance transferItem(String process, int objectId, long count, Inventory target, L2Object reference)
	{
		L2ItemInstance oldItem = getInventory().getItemByObjectId(objectId);
		if(oldItem == null || !oldItem.canBeTraded(this))
			return null;

		return getInventory().transferItem(process, objectId, count, target, this, reference);
	}

	public String getLastHWID()
	{
		return _lastHWID;
	}

	private static org.apache.commons.logging.Log _botReport = LogFactory.getLog("botreport");

	public void botReport(L2Player bot)
	{
		if(bot == null)
			return;
		_botReports.put(bot.getObjectId(), System.currentTimeMillis() + 60 * 60 * 1000);
		_botReport.info(bot + " reported as bot by " + this);
	}

	public boolean isBotReported(int objectId)
	{
		if(_botReports.containsKey(objectId))
		{
			if(_botReports.get(objectId) > System.currentTimeMillis())
				return true;
			else
			{
				_botReports.remove(objectId);
				return false;
			}
		}
		return false;
	}

	public void updateFameTime()
	{
		_lastFameUpdate = System.currentTimeMillis();
	}

	public long getLastFameUpdate()
	{
		return _lastFameUpdate;
	}

	/**
	 * Returns true if cp update should be done, false if not
	 *
	 * @return boolean
	 */
	private boolean needCpUpdate(int barPixels)
	{
		double currentCp = getCurrentCp();

		if(currentCp <= 1.0 || getMaxCp() < barPixels)
			return true;

		if(currentCp <= _cpUpdateDecCheck || currentCp >= _cpUpdateIncCheck)
		{
			if(currentCp == getMaxCp())
			{
				_cpUpdateIncCheck = currentCp + 1;
				_cpUpdateDecCheck = currentCp - _cpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentCp / _cpUpdateInterval;
				int intMulti = (int) doubleMulti;

				_cpUpdateDecCheck = _cpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_cpUpdateIncCheck = _cpUpdateDecCheck + _cpUpdateInterval;
			}

			return true;
		}

		return false;
	}

	/**
	 * Returns true if mp update should be done, false if not
	 *
	 * @return boolean
	 */
	private boolean needMpUpdate(int barPixels)
	{
		double currentMp = getCurrentMp();

		if(currentMp <= 1.0 || getMaxMp() < barPixels)
			return true;

		if(currentMp <= _mpUpdateDecCheck || currentMp >= _mpUpdateIncCheck)
		{
			if(currentMp == getMaxMp())
			{
				_mpUpdateIncCheck = getMaxMp();
				_mpUpdateDecCheck = _mpUpdateIncCheck - _mpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentMp / _mpUpdateInterval;
				int intMulti = (int) doubleMulti;

				_mpUpdateDecCheck = _mpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_mpUpdateIncCheck = _mpUpdateDecCheck + _mpUpdateInterval;
			}

			return true;
		}

		return false;
	}

	@Override
	public boolean isInDuel()
	{
		return _duel != null;
	}

	@Override
	public Duel getDuel()
	{
		return _duel;
	}

	public void setDuelSide(int side)
	{
		_duelSide = side;
	}

	public int getDuelSide()
	{
		return _duelSide;
	}

	/**
	 * Sets up the duel state using a non 0 duelId.
	 *
	 * @param duel 0=not in a duel
	 */
	public void setDuel(Duel duel)
	{
		_duel = duel;
	}

	public boolean unEquipInappropriateItems()
	{
		boolean ret = false;
		GArray<L2ItemInstance> items = new GArray<L2ItemInstance>(5);
		for(L2ItemInstance item : getInventory().getItems())
			if(item.isEquipped() && !item.checkEquipCondition(this))
			{
				ret = true;
				items.addAll(getInventory().unEquipItemAndRecord(item));

				sendDisarmMessage(item);

				if(item.getItem() instanceof L2Weapon)
				{
					if(item.getChargedSpiritshot() != L2ItemInstance.CHARGED_NONE)
						sendPacket(new SystemMessage(SystemMessage.POWER_OF_MANA_DISABLED));
					if(item.getChargedSoulshot() != L2ItemInstance.CHARGED_NONE)
						sendPacket(new SystemMessage(SystemMessage.POWER_OF_THE_SPIRITS_DISABLED));
					item.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
					item.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
				}
			}

		if(items.size() > 0)
		{
			sendChanges();
			sendPacket(new InventoryUpdate(items));
		}

		return ret;
	}

	@Override
	public TargetType getTargetRelation(L2Character target, boolean offensive)
	{
		if(isDead())
		{
			if(getSessionVar("event_team_pvp") != null && getTeam() != target.getTeam())
			{
				return TargetType.invalid;
			}

			return TargetType.pc_body;
		}

		if(target instanceof L2NpcInstance)
		{
			if(target.getPlayer() != null)
				target = target.getPlayer();
			else
				return TargetType.enemy_only;
		}

		if(this == target)
			return TargetType.self;

		if(isCursedWeaponEquipped())
			return offensive ? TargetType.enemy_only : TargetType.invalid;

		if(target instanceof L2Playable)
		{
			if(target == getPet())
				return offensive ? TargetType.invalid : TargetType.target;

			L2Player player = target.getPlayer();
			if(player != null)
			{
				if(isInOlympiadMode() && getOlympiadGameId() == player.getOlympiadGameId())
				{
					if(isPartyMember(target))
						return TargetType.target;
					if(isOlympiadStart())
						return TargetType.enemy_only;
					if(getOlympiadSide() != player.getOlympiadSide())
						return TargetType.invalid;
				}

				if(getSiegeState() > 0 && player.getSiegeState() > 0) // Оба участники осады
				{
					if(getSiegeState() == 3 && player.getSiegeState() == 3) // Участники ТВ
					{
						if(getTerritoryId() == player.getTerritoryId() && offensive) // Зареганы за одну территорию и скилл плохой
							return TargetType.siege_ally;
					}
					else if(getSiegeId() > 0 && getSiegeId() == player.getSiegeId() && getSiegeState() == player.getSiegeState())
					{
						if((getSiegeState() == 2 || getSiegeState() == 1 && getClan().getSiege().isTempAllyActive()) && offensive)
							return TargetType.siege_ally;
					}
				}

				if(isInDuel() && target.getDuel() == getDuel())
				{
					if((getDuelSide() == player.getDuelSide() && offensive) || (player.getDuelState() == Duel.DUELSTATE_PREPARE && offensive) || player.getDuelState() == Duel.DUELSTATE_DEAD || getDuelState() == Duel.DUELSTATE_DEAD)
						return TargetType.invalid;
					if(getDuelState() == Duel.DUELSTATE_DUELLING && player.getDuelState() == Duel.DUELSTATE_DUELLING && getDuelSide() != player.getDuelSide())
						return TargetType.enemy_only;
				}

				if(getSessionVar("event_team_pvp") != null && player.getSessionVar("event_team_pvp") != null && getTeam() > 0 && player.getTeam() > 0)
				{
					if(getTeam() == player.getTeam() && offensive || getTeam() != player.getTeam() && !offensive)
						return TargetType.invalid;
					if(getTeam() != player.getTeam() && offensive)
						return TargetType.enemy_only;
					if(getTeam() == player.getTeam() && !offensive)
						return TargetType.target;
				}

				if(isPartyMember(target))
					return TargetType.target;

				if(isInZoneBattle() && target.isInZoneBattle())
				{
					if(isClanMember(target))
						return TargetType.at_war;
					return TargetType.enemy_only;
				}

				if(isClanMember(target) && getSiegeState() != 3)
					return TargetType.target;

				if(getKarma() > 0 || getPvpFlag() > 0 || isInSiege() && target.isInSiege())
					return TargetType.enemy_only;

				if(atMutualWarWith(player, getClan(), player.getClan()))
					return TargetType.at_war;
			}
		}

		return TargetType.target;
	}

	@Override
	public boolean isClanMember(L2Character target)
	{
		return target == getPet() || _clanId > 0 && target.getPlayer() != null && getClan().isMember(target.getPlayer().getObjectId()) && !target.getPlayer().isInvisible() && (!isInOlympiadMode() || getOlympiadSide() == target.getPlayer().getOlympiadSide());
	}

	@Override
	public boolean isPartyMember(L2Character target)
	{
		return target == getPet() || _party != null && _party.containsMember(target);
	}

	@Override
	public boolean isCommandChanelMember(L2Character target)
	{
		return isPartyMember(target) || _party != null && _party.getCommandChannel() != null && _party.getCommandChannel().containsMember(target);
	}

	@Override
	public boolean isFriend(L2Character target)
	{
		return isClanMember(target) || isPartyMember(target) || this == target || target.getTargetRelation(this, false) == TargetType.target;
	}

	//Mount Engnie
	private MountEngine mountEngine;

	public MountEngine getMountEngine()
	{
		if(mountEngine == null)
			mountEngine = new MountEngine(this);
		return mountEngine;
	}

	public void nulledMountEngine()
	{
		mountEngine = null;
	}

	private boolean logoutStarted = false;

	public boolean isLogoutStarted()
	{
		return logoutStarted;
	}

	public void setLogoutStarted(boolean logoutStarted)
	{
		this.logoutStarted = logoutStarted;
	}

	public void setStablePoint(Location stablePoint)
	{
		this.stablePoint = stablePoint;
	}

	public Location getStablePoint()
	{
		return stablePoint;
	}

	public void setEntering(boolean entering)
	{
		this.entering = entering;
	}

	public boolean isEntering()
	{
		return entering;
	}

	public GArray<String> getStoredBypasses(boolean bbs)
	{
		if(bbs)
		{
			if(bypassesBbs == null)
				bypassesBbs = new GArray<>();
			return bypassesBbs;
		}
		if(bypasses == null)
			bypasses = new GArray<>();
		return bypasses;
	}

	public GArray<String> getStoredLinks()
	{
		if(links == null)
			links = new GArray<>(3);

		return links;
	}

	public void setLastTrap(L2TrapInstance trap)
	{
		_trap = trap;
	}

	public L2TrapInstance getLastTrap()
	{
		return _trap;
	}

	public void setLastComapssZone(int compassZone)
	{
		_lastComapssZone = compassZone;
	}

	public int getLastCompassZone()
	{
		return _lastComapssZone;
	}

	public int getCurrentCompassZone()
	{
		if(isInSiege())
			return ExSetCompassZoneCode.ZONE_SIEGE;
		else if(isInZoneBattle())
			return ExSetCompassZoneCode.ZONE_PVP;
		else if(isInDangerArea() || isInZone(L2Zone.ZoneType.altered))
			return ExSetCompassZoneCode.ZONE_ALTERED;
		else if(isInZonePeace())
			return ExSetCompassZoneCode.ZONE_PEACE;
		else if(isInZoneSSQ())
			return ExSetCompassZoneCode.ZONE_SS;
		else
			return ExSetCompassZoneCode.ZONE_GENERAL_FIELD;
	}

	private boolean _isUseMacro = false;

	public boolean isUseMacro()
	{
		return _isUseMacro;
	}

	public void setUseMacro(boolean isUse)
	{
		_isUseMacro = isUse;
	}
	
	@Override
	public final float getSpeed(int baseSpeed)
	{
		float spd = baseSpeed;
		if(getMountEngine().isMounted())
			spd = getMountEngine().getMountSpeed();
		else if(isSwimming())
			spd = getSwimSpeed();

		spd = super.getSpeed((int) spd);
		return spd > Config.LIM_MOVE_SPEED && !isGM() ? Config.LIM_MOVE_SPEED : spd;
	}

	public L2Alliance getAlliance()
	{
		return getClanId() > 0 ? getClan().getAlliance() : null;
	}

	@Override
	public boolean isMovementDisabled()
	{
		return isEntering() || isLogoutStarted() || _movieId > 0 || super.isMovementDisabled();
	}

	@Override
	public L2Player getPlayer()
	{
		return this;
	}

	public long getBirthday()
	{
		return _birthday;
	}

	private long _lastMovePacket = 0;

	public long getLastMovePacket()
	{
		return _lastMovePacket;
	}

	public void setLastMovePacket()
	{
		_lastMovePacket = System.currentTimeMillis();
	}

	private int _fallZ;
	private ScheduledFuture<?> _fallTask = null;

	public boolean isFalling()
	{
		return _fallTask != null;
	}

	private class FallTask implements Runnable
	{
		public void run()
		{
			int safeHeight = (int) calcStat(Stats.FALL_SAFE, getTemplate().safeFall, null, null);
			int dz = _fallZ - getZ();

			if(!isFloating() && dz > safeHeight)
				falling(dz, safeHeight);

			_fallTask = null;
		}
	}

	@Override
	public void broadcastUserInfo()
	{
		broadcastUserInfo(false);
	}

	public FriendList getFriendList()
	{
		if(_friendList == null)
			_friendList = FriendList.restore(this);

		return _friendList;
	}

	public ContactList getContactList()
	{
		if(_contactList == null)
			_contactList = ContactList.restore(this);

		return _contactList;
	}

	public String getSessionVar(String key)
	{
		if(_userSession == null)
			return null;
		return _userSession.get(key);
	}

	public void setSessionVar(String key, String val)
	{
		if(_userSession == null)
			_userSession = new ConcurrentHashMap<>();

		if(val == null || val.isEmpty())
			_userSession.remove(key);
		else
			_userSession.put(key, val);
	}

	private int _buyListId;

	public void setBuyListId(int listId)
	{
		_buyListId = listId;
	}

	public int getBuyListId()
	{
		return _buyListId;
	}

	public TeleportBook getTeleportBook()
	{
		return _teleportBook;
	}

	@Override
	public int getTerritoryId()
	{
		return _territoryId;
	}

	public void setTerritoryId(int terrId)
	{
		_territoryId = terrId;
	}

	private float _badges = 0;

	public void addBadges(float badges)
	{
		_badges += badges;
		setVar("tw_badges", String.valueOf(_badges));
	}

	public void setBadges(int badges)
	{
		_badges = badges;
	}

	private L2EffectPointInstance _effectPoint = null;

	public void setEffectPoint(L2EffectPointInstance point)
	{
		_effectPoint = point;
	}

	@Override
	public L2EffectPointInstance getEffectPoint()
	{
		return _effectPoint;
	}

	private ScheduledFuture<?> _nonAggroTask;
	private PlayerActionListener _actionListener;

	public void startNonAggroTask()
	{
		if(_nonAggroTask != null)
			_nonAggroTask.cancel(true);

		setNonAggroTime(System.currentTimeMillis() + 600000);
		_nonAggroTask = ThreadPoolManager.getInstance().scheduleEffect(new Runnable()
		{
			public void run()
			{
				stopNonAggroTask();
			}
		}, 600000);
		addMethodInvokeListener(_actionListener = new PlayerActionListener());
	}

	public void stopNonAggroTask()
	{
		try
		{
			if(_nonAggroTask != null)
			{
				setNonAggroTime(0);
				sendPacket(Msg.YOU_ARE_NO_LONGER_PROTECTED_FROM_AGGRESSIVE_MONSTERS);
				removeMethodInvokeListener(_actionListener);
				restoreSummon();
				_actionListener = null;
				_nonAggroTask.cancel(true);
				_nonAggroTask = null;
			}
		}
		catch(NullPointerException e)
		{
		}
	}

	public void setMovieId(int id)
	{
		_movieId = id;
	}

	public int getMovieId()
	{
		return _movieId;
	}

	public void showQuestMovie(int id)
	{
		if(_movieId > 0) //already in movie
			return;
		sendActionFailed();
		setTarget(null);
		stopMove();
		_movieId = id;
		sendPacket(new ExStartScenePlayer(id));
	}

	public void onTeleported()
	{
		spawnMe();

		setLastClientPosition(getLoc());
		setLastServerPosition(getLoc());

		setIsTeleporting(false);

		if(_isPendingRevive)
			doRevive();

		if(getPet() != null)
		{
			getPet().setFollowStatus(getPet().getFollowStatus());
			getPet().teleportToOwner();
			getPet().setTeleported(false);
			if(getPet() instanceof L2PetBabyInstance)
				getPet().getAI().startAITask();
		}

		sendActionFailed();
	}

	private MultiSellListContainer _lastMultisell;

	public void setLastMultisell(MultiSellListContainer last)
	{
		_lastMultisell = last;
	}

	public MultiSellListContainer getLastMultisell()
	{
		return _lastMultisell;
	}

	public void setNotShowBuffAnim(boolean val)
	{
		_notShowBuffAnim = val;
	}

	public boolean isNotShowBuffAnim()
	{
		return _notShowBuffAnim;
	}

	@Override
	public void spawnMe()
	{
		if(_isDeleting)
			return;

		if(_vehicle != null)
		{
			_vehicle.sendInfo(this);
			sendUserInfo(true);
		}

		super.spawnMe();
	}

	public int getRideState()
	{
		if(getMountEngine().isMounted())
			return getMountEngine().getRideState();

		return L2Skill.RideState.ride_none.mask;
	}

	private Future<?> _hourlyTask;
	private int _hoursInGame = 0;

	public int getHoursInGame()
	{
		_hoursInGame++;
		return _hoursInGame;
	}

	public void startHourlyTask()
	{
		_hourlyTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new HourlyTask(this), 3600000L, 3600000L, true);
	}

	public void stopHourlyTask()
	{
		if(_hourlyTask != null)
		{
			_hourlyTask.cancel(false);
			_hourlyTask = null;
		}
	}

	public FloatingRate getFloatingRate()
	{
		return _floatingRate;
	}
}
