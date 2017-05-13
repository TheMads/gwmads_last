package ru.l2gw.gameserver;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.config.ConfigField;
import ru.l2gw.commons.config.ExProperties;
import ru.l2gw.commons.config.ServerConfig;
import ru.l2gw.commons.crontab.Crontab;
import ru.l2gw.commons.network.utils.AdvIP;
import ru.l2gw.gameserver.clientpackets.AbstractEnchantPacket;
import ru.l2gw.gameserver.model.base.Experience;
import ru.l2gw.gameserver.model.base.ItemData;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.templates.StatsSet;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

public class Config extends ServerConfig
{
	protected static Log _log = LogFactory.getLog(Config.class);

	public static ArrayList<AdvIP> GAMEIPS = new ArrayList<>();
	public static ArrayList<String> OBSCENE_LIST;
	public static ArrayList<String> INDECENT_LIST;
	public static ArrayList<String> SHOUT_LIST;
	public static ArrayList<String> PRIVATE_LIST;

	public static int SHOUT_OFF_ON_REGION = -1;
	

	/**
	 * Character name template
	 */
	public static String RusTemplate = "[\u0410-\u042f\u0430-\u044f]{2,16}";
	public static String EnTemplate = "[A-Za-z0-9]{2,16}";

	public static byte[] HEX_ID;

	/**
	 * Castle siege options *
	 */
	public static double SIEGE_DAWN_GATES_PDEF_MULT = 1.3;
	public static double SIEGE_DUSK_GATES_PDEF_MULT = 0.3;
	public static double SIEGE_DAWN_GATES_MDEF_MULT = 1.3;
	public static double SIEGE_DUSK_GATES_MDEF_MULT = 0.3;
	public static List<Integer> SIEGE_HOUR_LIST;

	/**
	 * Properties file for siege configuration
	 */
	public static final String ADV_IP_FILE = "./config/advipsystem.properties";

	/**
	 * Configuration files
	 */
	public static final String GM_PERSONAL_ACCESS_FILE = "./config/GMAccess.xml";
	public static final String CASTLE_DATA_FILE = "./data/castles.xml";
	public static final String FORTRESS_DATA_FILE = "./data/fortress.xml";
	public static final String ACTIONS_CONFIG = "./config/actions.xml";
	public static final String ITEM_AUCTION_FILE = "./data/auction.xml";
	public static final String SUPERPOINT_FILE = "./data/superpoint.xml";
	public static final String CATEGORY_FILE = "./data/categorydata.xml";
	public static final String NPCPOS_FILE = "./data/npcpos.xml";
	public static final String PRODUCTDATA_FILE = "./data/productdata.xml";
	public static final String VARIATIONDATA_FILE = "./data/variationdata.xml";
	public static final String OPTIONDATA_FILE = "./data/optiondata.xml";
	public static final String ENCHANTOPTION_FILE = "./data/enchantoption.xml";
	public static final String FIELDCYCLE_FILE = "./data/fieldcycle.xml";
	public static final String SETTINGS_FILE = "./data/settings.xml";
	public static final String HEXID_FILE = "./config/hexid.txt";
	public static final String OBSCENE_CONFIG_FILE = "./config/obscene.txt";
	public static final String INDECENT_CONFIG_FILE = "./config/indecent.txt";
	public static final String SHOUT_CONFIG_FILE = "./config/shout.txt";
	public static final String PRIVATE_CONFIG_FILE = "./config/private.txt";
	
	/**
	 * server.properties fields
	 */
	@ConfigField(config = "server", fieldName = "LoginHost", value = "127.0.0.1")
	public static String GAME_SERVER_LOGIN_HOST;
	@ConfigField(config = "server", fieldName = "LoginPort", value = "9013")
	public static int GAME_SERVER_LOGIN_PORT;
	@ConfigField(config = "server", value = "false")
	public static boolean DEBUG;
	@ConfigField(config = "server", value = "false")
	public static boolean DEBUG_INSTANCES;
	@ConfigField(config = "server", fieldName = "DebugMemdumpDir", value = "./dumpmem")
	public static String DEBUG_DUMPMEMDIR;
	@ConfigField(config = "server", value = "false")
	public static boolean ZONE_DEBUG;
	@ConfigField(config = "server", fieldName = "EnablePacketFloodProtector", value = "false")
	public static boolean PACKET_FLOOD_PROTECTOR;
	@ConfigField(config = "server", value = "30")
	public static int PACKET_MAX_SEND_PER_PASS;
	@ConfigField(config = "server", value = "true")
	public static boolean PING_ENABLED;
	@ConfigField(config = "server", value = "60000")
	public static int PING_INTERVAL;
	@ConfigField(config = "server", value = "1")
	public static int PING_MAX_LOST;
	@ConfigField(config = "server", fieldName = "TestServer", value = "false")
	public static boolean SERVER_LIST_TESTSERVER;
	@ConfigField(config = "server", fieldName = "AdvIPSystem", value = "false")
	public static boolean ADVIPSYSTEM;
	@ConfigField(config = "server", fieldName = "HideGMStatus", value = "false")
	public static boolean HIDE_GM_STATUS;
	@ConfigField(config = "server", fieldName = "ShowGMLogin", value = "true")
	public static boolean SHOW_GM_LOGIN;
	@ConfigField(config = "server", fieldName = "SaveGMEffects", value = "false")
	public static boolean SAVE_GM_EFFECTS;
	@ConfigField(config = "server", fieldName = "RequestServerID", value = "0")
	public static int REQUEST_ID;
	@ConfigField(config = "server", fieldName = "AcceptAlternateID", value = "true")
	public static boolean ACCEPT_ALTERNATE_ID;
	@ConfigField(config = "server", fieldName = "GameserverPort", value = "7777")
	public static int PORT_GAME;
	@ConfigField(config = "server", fieldName = "TemplateLang", value = "En")
	public static String Lang;
	@ConfigField(config = "server", value = "^[A-Za-z0-9]{3,16}$|^[\u0410-\u042f\u0430-\u044f0-9]{3,16}$")
	public static String CLAN_NAME_TEMPLATE;
	@ConfigField(config = "server", value = "[A-Za-z0-9\u0410-\u042f\u0430-\u044f \\p{Punct}]{1,16}")
	public static String CLAN_TITLE_TEMPLATE;
	@ConfigField(config = "server", value = "^[A-Za-z0-9]{3,16}$|^[\u0410-\u042f\u0430-\u044f0-9]{3,16}$")
	public static String ALLY_NAME_TEMPLATE;
	@ConfigField(config = "server", value = "[A-Za-z0-9]{5,16}")
	public static String APASSWD_TEMPLATE;
	@ConfigField(config = "server", value = "127.0.0.1")
	public static String GAMESERVER_HOSTNAME;
	@ConfigField(config = "server", value = "0")
	public static int GLOBAL_CHAT;
	@ConfigField(config = "server", value = "0")
	public static int GLOBAL_TRADE_CHAT;
	@ConfigField(config = "server", value = "1")
	public static int SHOUT_CHAT_MODE;
	@ConfigField(config = "server", fieldName = "ShoutChatRange", value = "20000")
	public static int SHOUT_RANGE;
	@ConfigField(config = "server", value = "1")
	public static int TRADE_CHAT_MODE;
	@ConfigField(config = "server", fieldName = "TradeChatRange", value = "20000")
	public static int TRADE_RANGE;
	@ConfigField(config = "server", fieldName = "MinLevelForPM", value = "1")
	public static int MIN_LEVEL_FOR_PM;
	@ConfigField(config = "server", value = "1")
	public static int MIN_LEVEL_FOR_SHOUT;
	@ConfigField(config = "server", value = "1")
	public static int MIN_LEVEL_FOR_TRADE;
	@ConfigField(config = "server", value = "false")
	public static boolean ALLOW_SPECIAL_COMMANDS;
	@ConfigField(config = "server", value = "false")
	public static boolean LOG_CHAT;
	@ConfigField(config = "server", value = "false")
	public static boolean LOG_KILLS;
	@ConfigField(config = "server", value = "1.")
	public static float RATE_XP;
	@ConfigField(config = "server", value = "1.")
	public static float RATE_SP;
	@ConfigField(config = "server", value = "1.")
	public static float RATE_QUESTS_DROP_ADENA;
	@ConfigField(config = "server", value = "1.")
	public static float RATE_QUESTS_DROP_REWARD;
	@ConfigField(config = "server", value = "1.")
	public static float RATE_QUESTS_DROP_CHANCE;
	@ConfigField(config = "server", fieldName = "RateQuestsExpSp", value = "1.")
	public static float RATE_QUESTS_EXPSP;
	@ConfigField(config = "server", value = "1.")
	public static float RATE_CLAN_REP_SCORE;
	@ConfigField(config = "server", value = "2")
	public static int RATE_CLAN_REP_SCORE_MAX_AFFECTED;
	@ConfigField(config = "server", value = "1.")
	public static float RATE_DROP_ADENA;
	@ConfigField(config = "server", value = "1.")
	public static float RATE_DROP_ITEMS;
	@ConfigField(config = "server", fieldName = "EquipQtDropLimitMin", value = "1")
	public static int RATE_EQUIP_LIMIT_MIN;
	@ConfigField(config = "server", fieldName = "EquipQtDropLimitMax", value = "1")
	public static int RATE_EQUIP_LIMIT_MAX;
	@ConfigField(config = "server", fieldName = "RateRaidBoss", value = "1.")
	public static float RATE_DROP_RAIDBOSS;
	@ConfigField(config = "server", value = "1.")
	public static float RATE_DROP_BOSS_JEWEL;
	@ConfigField(config = "server", value = "1.")
	public static float RATE_DROP_SPOIL;
	@ConfigField(config = "server", value = "1")
	public static int RATE_MANOR;
	@ConfigField(config = "server", value = "1.")
	public static float RATE_FISH_DROP_COUNT;
	@ConfigField(config = "server", value = "1.")
	public static float RATE_SIEGE_GUARDS_PRICE;
	@ConfigField(config = "server", value = "1.")
	public static float RATE_RAID_REGEN;
	@ConfigField(config = "server", fieldName = "RateRaidPDef", value = "1.")
	public static float RATE_RAID_PDEF;
	@ConfigField(config = "server", fieldName = "RateRaidMDef", value = "1.")
	public static float RATE_RAID_MDEF;
	@ConfigField(config = "server", fieldName = "RateBossPDef", value = "1.")
	public static float RATE_BOSS_PDEF;
	@ConfigField(config = "server", fieldName = "RateBossMDef", value = "1.")
	public static float RATE_BOSS_MDEF;
	@ConfigField(config = "server", fieldName = "RateHellboundPoints", value = "1.")
	public static float RATE_HB_POINTS;
	@ConfigField(config = "server", value = "8")
	public static int RAID_MAX_LEVEL_DIFF;
	@ConfigField(config = "server", value = "false")
	public static boolean RAID_FORCE_STATUS_UPDATE;
	@ConfigField(config = "server", fieldName = "AutoDestroyDroppedItemAfter", value = "1200")
	public static long AUTODESTROY_ITEM_AFTER;
	@ConfigField(config = "server", fieldName = "DeleteCharAfterDays", value = "7")
	public static int DELETE_DAYS;
	public static File DATAPACK_ROOT;
	@ConfigField(config = "server", fieldName = "BugUserPunishment", value = "2")
	public static int BUGUSER_PUNISH;
	@ConfigField(config = "server", fieldName = "IllegalActionPunishment", value = "1")
	public static int DEFAULT_PUNISH;
	@ConfigField(config = "server", fieldName = "AllowDiscardItem", value = "true")
	public static boolean ALLOW_DISCARDITEM;
	@ConfigField(config = "server", value = "true")
	public static boolean ALLOW_FREIGHT;
	@ConfigField(config = "server", value = "true")
	public static boolean ALLOW_WAREHOUSE;
	@ConfigField(config = "server", value = "true")
	public static boolean ALLOW_WATER;
	@ConfigField(config = "server", value = "false")
	public static boolean ALLOW_BOAT;
	@ConfigField(config = "server", value = "false")
	public static boolean ALLOW_CURSED_WEAPONS;
	@ConfigField(config = "server", value = "0")
	public static int CURSED_WEAPONS_MIN_PLAYERS_DROP;
	@ConfigField(config = "server", value = "false")
	public static boolean DROP_CURSED_WEAPONS_ON_KICK;
	@ConfigField(config = "server", value = "253")
	public static int MIN_PROTOCOL_REVISION;
	@ConfigField(config = "server", value = "260")
	public static int MAX_PROTOCOL_REVISION;
	@ConfigField(config = "server", fieldName = "MinNPCAnimation", value = "5")
	public static int MIN_NPC_ANIMATION;
	@ConfigField(config = "server", fieldName = "MaxNPCAnimation", value = "90")
	public static int MAX_NPC_ANIMATION;
	@ConfigField(config = "server", fieldName = "AllowCommunityBoard", value = "true")
	public static boolean COMMUNITYBOARD_ENABLED;
	@ConfigField(config = "server", fieldName = "BBSDefault", value = "_bbshome")
	public static String BBS_DEFAULT;
	@ConfigField(config = "server", value = "*")
	public static String INTERNAL_HOSTNAME;
	@ConfigField(config = "server", value = "*")
	public static String EXTERNAL_HOSTNAME;
	@ConfigField(config = "server", value = "false")
	public static boolean SERVER_SIDE_NPC_NAME;
	@ConfigField(config = "server", value = "false")
	public static boolean SERVER_SIDE_NPC_TITLE_WITH_LVL;
	@ConfigField(config = "server", value = "false")
	public static boolean SERVER_SIDE_NPC_TITLE;
	@ConfigField(config = "server", fieldName = "AutoDeleteInvalidQuestData", value = "false")
	public static boolean AUTODELETE_INVALID_QUEST_DATA;
	@ConfigField(config = "server", fieldName = "HardDbCleanUpOnStart", value = "false")
	public static boolean HARD_DB_CLEANUP_ON_START;
	@ConfigField(config = "server", value = "100")
	public static int MAXIMUM_ONLINE_USERS;
	@ConfigField(config = "server", value = "true")
	public static boolean ALLOW_SEND_STATUS;
	@ConfigField(config = "server", value = "true")
	public static boolean SEND_STATUS_REAL_STORE;
	@ConfigField(config = "server", value = "true")
	public static boolean DAY_STATUS_FORCE_CLIENT_UPDATE;
	@ConfigField(config = "server", value = "6")
	public static int DAY_STATUS_SUN_RISE_AT;
	@ConfigField(config = "server", value = "18")
	public static int DAY_STATUS_SUN_SET_AT;
	@ConfigField(config = "server", fieldName = "Driver", value = "com.mysql.jdbc.Driver")
	public static String DATABASE_DRIVER;
	@ConfigField(config = "server", fieldName = "URL", value = "jdbc:mysql://localhost/l2fdb")
	public static String DATABASE_URL;
	@ConfigField(config = "server", fieldName = "Login", value = "root")
	public static String DATABASE_LOGIN;
	@ConfigField(config = "server", fieldName = "Password", value = "")
	public static String DATABASE_PASSWORD;
	@ConfigField(config = "server", fieldName = "MaximumDbConnections", value = "10")
	public static int DATABASE_MAX_CONNECTIONS;
	@ConfigField(config = "server", fieldName = "MaxIdleConnectionTimeout", value = "600")
	public static int DATABASE_MAX_IDLE_TIMEOUT;
	@ConfigField(config = "server", fieldName = "IdleConnectionTestPeriod", value = "60")
	public static int DATABASE_IDLE_TEST_PERIOD;
	@ConfigField(config = "server", value = "false")
	public static boolean LAZY_ITEM_UPDATE;
	@ConfigField(config = "server", value = "60000")
	public static int LAZY_ITEM_UPDATE_TIME;
	@ConfigField(config = "server", value = "0")
	public static int USER_INFO_INTERVAL;
	@ConfigField(config = "server", value = "false")
	public static boolean BROADCAST_STATS_INTERVAL;
	@ConfigField(config = "server", value = "0")
	public static int BROADCAST_CHAR_INFO_INTERVAL;
	@ConfigField(config = "server", fieldName = "ServerListBrackets", value = "false")
	public static boolean SERVER_LIST_BRACKET;
	@ConfigField(config = "server", value = "false")
	public static boolean SERVER_LIST_CLOCK;
	@ConfigField(config = "server", fieldName = "ServerGMOnly", value = "false")
	public static boolean SERVER_GMONLY;
	@ConfigField(config = "server", fieldName = "ThreadPoolSizeMove", value = "25")
	public static int THREAD_P_MOVE;
	@ConfigField(config = "server", fieldName = "ThreadPoolSizeEffects", value = "10")
	public static int THREAD_P_EFFECTS;
	@ConfigField(config = "server", fieldName = "ThreadPoolSizeGeneral", value = "15")
	public static int THREAD_P_GENERAL;
	@ConfigField(config = "server", value = "4")
	public static int GENERAL_PACKET_THREAD_CORE_SIZE;
	@ConfigField(config = "server", value = "1")
	public static int THREADING_MODEL;
	@ConfigField(config = "server", value = "10")
	public static int NPC_AI_MAX_THREAD;
	@ConfigField(config = "server", value = "20")
	public static int PLAYER_AI_MAX_THREAD;
	@ConfigField(config = "server", fieldName = "MAT_BANCHAT", value = "false")
	public static boolean MAT_BANCHAT;
	@ConfigField(config = "server", fieldName = "INDECENT_BANCHAT", value = "false")
	public static boolean INDECENT_BLOCKCHAT;
	@ConfigField(config = "server", fieldName = "MAT_BAN_CHANNEL", value = "0")
	public static String BAN_CHANNEL;
	@ConfigField(config = "server", fieldName = "INDECENT_BAN_CHANNEL", value = "0")
	public static String INDECENT_BLOCK_CHANNEL;
	@ConfigField(config = "server", fieldName = "MAT_REPLACE", value = "false")
	public static boolean MAT_REPLACE;
	@ConfigField(config = "server", fieldName = "MAT_REPLACE_STRING", value = "[censored]")
	public static String MAT_REPLACE_STRING;
	@ConfigField(config = "server", fieldName = "MAT_ANNOUNCE", value = "true")
	public static boolean MAT_ANNOUNCE;
	@ConfigField(config = "server", fieldName = "MAT_ANNOUNCE_NICK", value = "true")
	public static boolean MAT_ANNOUNCE_NICK;
	@ConfigField(config = "server", fieldName = "SHOUT_FILTER", value = "false")
	public static boolean SHOUT_FILTER;
	@ConfigField(config = "server", fieldName = "Timer_to_UnBan", value = "30")
	public static int UNCHATBANTIME;
	@ConfigField(config = "server", value = "ru")
	public static String DEFAULT_LANG;
	@ConfigField(config = "server", value = "false")
	public static boolean SHOW_LANG_SELECT_MENU;
	@ConfigField(config = "server", fieldName = "AutoRestart", value = "0")
	public static int RESTART_TIME;
	@ConfigField(config = "server", fieldName = "AutoRestartAt", value = "5")
	public static int RESTART_AT_TIME;
	@ConfigField(config = "server", value = "3")
	public static int MAINTENANCE_DAY;
	@ConfigField(config = "server", value = "2")
	public static int MAINTENANCE_HOUR;
	@ConfigField(config = "server", fieldName = "checkLangFilesModify", value = "false")
	public static boolean CHECK_LANG_FILES_MODIFY;
	@ConfigField(config = "server", fieldName = "useFileCache", value = "true")
	public static boolean USE_FILE_CACHE;
	public static ArrayList<Integer> DISABLE_CREATION_ID_LIST = new ArrayList<Integer>();
	@ConfigField(config = "server", value = "50")
	public static int MOVE_PACKET_DELAY;
	@ConfigField(config = "server", value = "false")
	public static boolean ALLOW_WEDDING;
	@ConfigField(config = "server", value = "0")
	public static int WEDDING_GIVE_ITEM;
	@ConfigField(config = "server", fieldName = "WeddingGiveCount", value = "1")
	public static int WEDDING_GIVE_ITEM_COUNT;
	@ConfigField(config = "server", value = "500000")
	public static int WEDDING_PRICE;
	@ConfigField(config = "server", value = "true")
	public static boolean WEDDING_PUNISH_INFIDELITY;
	@ConfigField(config = "server", value = "500000")
	public static int WEDDING_TELEPORT_PRICE;
	@ConfigField(config = "server", value = "120")
	public static int WEDDING_TELEPORT_INTERVAL;
	@ConfigField(config = "server", fieldName = "WeddingAllowSameSex", value = "true")
	public static boolean WEDDING_SAMESEX;
	@ConfigField(config = "server", fieldName = "WeddingFormalWear", value = "true")
	public static boolean WEDDING_FORMALWEAR;
	@ConfigField(config = "server", value = "20")
	public static int WEDDING_DIVORCE_COSTS;
	@ConfigField(config = "server", fieldName = "ForceStatusUpdate", value = "false")
	public static boolean FORCE_STATUSUPDATE;
	@ConfigField(config = "server", fieldName = "StartWhisoutSpawn", value = "false")
	public static boolean DONTLOADSPAWN;
	@ConfigField(config = "server", fieldName = "StartWhisoutQuest", value = "false")
	public static boolean DONTLOADQUEST;
	@ConfigField(config = "server", fieldName = "PlayerVisibilityHorizontal", value = "4000")
	public static int PLAYER_VISIBILITY;
	@ConfigField(config = "server", fieldName = "PlayerVisibilityVertical", value = "1000")
	public static int PLAYER_VISIBILITY_Z;
	@ConfigField(config = "server", value = "12")
	public static int SHIFT_BY;
	@ConfigField(config = "server", value = "10")
	public static int SHIFT_BY_FOR_Z;
	@ConfigField(config = "server", fieldName = "VipSkills", value = "")
	public static int[] VipSkillsList;
	@ConfigField(config = "server", value = "900000")
	public static int CHAR_SAVE_INTERVAL;
	@ConfigField(config = "server", value = "5000")
	public static int TASK_SAVE_INTERVAL;
	@ConfigField(config = "server", value = "120000")
	public static long LOBBY_TIMEOUT;
	@ConfigField(config = "server", fieldName = "AntiFloodEnable", value = "false")
	public static boolean ANTIFLOOD_ENABLE;
	@ConfigField(config = "server", fieldName = "MaxUnhandledSocketsPerIP", value = "5")
	public static int MAX_UNHANDLED_SOCKETS_PER_IP;
	@ConfigField(config = "server", fieldName = "UnhandledSocketsMinTTL", value = "500")
	public static int UNHANDLED_SOCKET_MIN_TTL;
	public static int[] BAN_CHANNEL_LIST = new int[18];
	public static int[] INDECENT_CHANNEL_LIST = new int[18];
	public static int MAT_BAN_COUNT_CHANNELS;
	public static int INDECENT_BLOCK_COUNT_CHANNELS;
	// Secondary auth
	@ConfigField(config = "server", value = "false")
	public static boolean SECOND_AUTH_ENABLED;
	@ConfigField(config = "server", value = "5")
	public static int SECOND_AUTH_LOGIN_TRIES;
	@ConfigField(config = "server", value = "28800")
	public static int SECOND_AUTH_BAN_TIME;

	/**
	 * other.properties fields
	 */
	@ConfigField(config = "other", fieldName = "UseDeepBlueDropRules", value = "true")
	public static boolean DEEPBLUE_DROP_RULES;
	@ConfigField(config = "other", value = "false")
	public static boolean ALLOW_GUARDS;
	@ConfigField(config = "other", fieldName = "SwimmingSpeed", value = "50")
	public static int SWIMING_SPEED;
	@ConfigField(config = "other", fieldName = "MaximumSlotsForNoDwarf", value = "80")
	public static int INVENTORY_MAXIMUM_NO_DWARF;
	@ConfigField(config = "other", fieldName = "MaximumSlotsForDwarf", value = "100")
	public static int INVENTORY_MAXIMUM_DWARF;
	@ConfigField(config = "other", fieldName = "MaximumSlotsForGMPlayer", value = "250")
	public static int INVENTORY_MAXIMUM_GM;
	@ConfigField(config = "other", fieldName = "MaximumSlotsQuest", value = "100")
	public static int INVENTORY_MAXIMUM_QUEST;
	@ConfigField(config = "other", fieldName = "MultisellPageSize", value = "10")
	public static int MULTISELL_SIZE;
	@ConfigField(config = "other", fieldName = "BaseWarehouseSlotsForNoDwarf", value = "100")
	public static int WAREHOUSE_SLOTS_NO_DWARF;
	@ConfigField(config = "other", fieldName = "BaseWarehouseSlotsForDwarf", value = "120")
	public static int WAREHOUSE_SLOTS_DWARF;
	@ConfigField(config = "other", fieldName = "MaximumWarehouseSlotsForClan", value = "200")
	public static int WAREHOUSE_SLOTS_CLAN;
	@ConfigField(config = "other", value = "66")
	public static int ENCHANT_CHANCE_WEAPON;
	@ConfigField(config = "other", value = "40")
	public static int ENCHANT_CHANCE_WEAPON_MAGIC;
	@ConfigField(config = "other", value = "ENCHANT_CHANCE_WEAPON")
	public static int ENCHANT_CHANCE_ARMOR;
	@ConfigField(config = "other", value = "ENCHANT_CHANCE_ARMOR")
	public static int ENCHANT_CHANCE_ACCESSORY;
	@ConfigField(config = "other", value = "66")
	public static int ENCHANT_CHANCE_BLESS_WEAPON;
	@ConfigField(config = "other", value = "40")
	public static int ENCHANT_CHANCE_BLESS_WEAPON_MAGIC;
	@ConfigField(config = "other", value = "ENCHANT_CHANCE_BLESS_WEAPON")
	public static int ENCHANT_CHANCE_BLESS_ARMOR;
	@ConfigField(config = "other", value = "ENCHANT_CHANCE_BLESS_ARMOR")
	public static int ENCHANT_CHANCE_BLESS_ACCESSORY;
	@ConfigField(config = "other", value = "66")
	public static int ENCHANT_CHANCE_CRYSTAL_WEAPON;
	@ConfigField(config = "other", value = "40")
	public static int ENCHANT_CHANCE_CRYSTAL_WEAPON_MAGIC;
	@ConfigField(config = "other", value = "ENCHANT_CHANCE_CRYSTAL_WEAPON")
	public static int ENCHANT_CHANCE_CRYSTAL_ARMOR;
	@ConfigField(config = "other", value = "ENCHANT_CHANCE_CRYSTAL_ARMOR")
	public static int ENCHANT_CHANCE_CRYSTAL_ACCESSORY;
	@ConfigField(config = "other", value = "14")
	public static int ENCHANT_CHANCE_DECREASE_LEVEL;
	@ConfigField(config = "other", value = "false")
	public static boolean ENCHANT_CRYSTAL_DONT_BREAK;
	@ConfigField(config = "other", value = "3")
	public static int ENCHANT_SAFE_COMMON;
	@ConfigField(config = "other", fieldName = "EnchantSafeFullBody", value = "4")
	public static int ENCHANT_SAFE_FULLBODY;
	@ConfigField(config = "other", value = "0")
	public static int BLESSED_ENCHANT_SAFE_COMMON;
	@ConfigField(config = "other", value = "0")
	public static int BLESSED_ENCHANT_SAFE_FULL_BODY;
	@ConfigField(config = "other", value = "20")
	public static int ENCHANT_MAX_WEAPON;
	@ConfigField(config = "other", value = "20")
	public static int ENCHANT_MAX_ARMOR;
	@ConfigField(config = "other", value = "20")
	public static int ENCHANT_MAX_ACCESSORY;
	@ConfigField(config = "other", value = "false")
	public static boolean OVER_ENCHANT_ENABLED;
	@ConfigField(config = "other", value = "33")
	public static int OVER_ENCHANT_CHANCE;
	@ConfigField(config = "other", value = "5")
	public static int OVER_ENCHANT_VALUE;
	@ConfigField(config = "other", value = "40")
	public static int ENCHANT_ATTRIBUTE_CHANCE;
	@ConfigField(config = "other", value = "false")
	public static boolean REGEN_SIT_WAIT;
	@ConfigField(config = "other", value = "0")
	public static int STARTING_ADENA;
	@ConfigField(config = "other", value = "")
	public static int[] START_ITEMS;
	@ConfigField(config = "other", value = "false")
	public static boolean CUSTOM_INITIAL_EQUIPMENT;
	@ConfigField(config = "other", value = "true")
	public static boolean UNSTUCK_SKILL;
	@ConfigField(config = "other", fieldName = "MaxPvtStoreSlotsDwarf", value = "5")
	public static int MAX_PVTSTORE_SLOTS_DWARF;
	@ConfigField(config = "other", fieldName = "MaxPvtStoreSlotsOther", value = "4")
	public static int MAX_PVTSTORE_SLOTS_OTHER;
	@ConfigField(config = "other", fieldName = "MaxPvtManufactureSlots", value = "20")
	public static int MAX_PVTCRAFT_SLOTS;
	@ConfigField(config = "other", fieldName = "GMNameColour", value = "0xFFFFFF")
	public static int GM_NAME_COLOUR;
	@ConfigField(config = "other", fieldName = "GMHeroAura", value = "true")
	public static boolean GM_HERO_AURA;
	@ConfigField(config = "other", value = "0xFFFFFF")
	public static int NORMAL_NAME_COLOUR;
	@ConfigField(config = "other", value = "0xFFFFFF")
	public static int CLANLEADER_NAME_COLOUR;
	@ConfigField(config = "other", fieldName = "ShowHTMLWelcome", value = "false")
	public static boolean SHOW_HTML_WELCOME;
	@ConfigField(config = "other", value = "5")
	public static int LOG_PRIVATE_MESSAGE_COUNT;
	@ConfigField(config = "other", value = "15")
	public static int ITEM_LINK_SHOW_TIME;
	@ConfigField(config = "other", fieldName = "RespawnRestoreCP", value = "-1")
	public static double RESPAWN_RESTORE_CP;
	@ConfigField(config = "other", fieldName = "RespawnRestoreHP", value = "65")
	public static double RESPAWN_RESTORE_HP;
	@ConfigField(config = "other", fieldName = "RespawnRestoreMP", value = "-1")
	public static double RESPAWN_RESTORE_MP;

	/**
	 * spoil.properties fields
	 */
	@ConfigField(config = "spoil", fieldName = "BasePercentChanceOfSpoilSuccess", value = "78.")
	public static float BASE_SPOIL_RATE;
	@ConfigField(config = "spoil", fieldName = "MinimumPercentChanceOfSpoilSuccess", value = "1.")
	public static float MINIMUM_SPOIL_RATE;
	@ConfigField(config = "spoil", fieldName = "AltFormula", value = "false")
	public static boolean ALT_SPOIL_FORMULA;
	@ConfigField(config = "spoil", fieldName = "BasePercentChanceOfSowingSuccess", value = "100")
	public static int MANOR_SOWING_BASIC_SUCCESS;
	@ConfigField(config = "spoil", fieldName = "BasePercentChanceOfSowingAltSuccess", value = "10")
	public static int MANOR_SOWING_ALT_BASIC_SUCCESS;
	@ConfigField(config = "spoil", fieldName = "BasePercentChanceOfHarvestingSuccess", value = "90")
	public static int MANOR_HARVESTING_BASIC_SUCCESS;
	@ConfigField(config = "spoil", fieldName = "MinDiffPlayerMob", value = "5")
	public static int MANOR_DIFF_PLAYER_TARGET;
	@ConfigField(config = "spoil", fieldName = "DiffPlayerMobPenalty", value = "5")
	public static int MANOR_DIFF_PLAYER_TARGET_PENALTY;
	@ConfigField(config = "spoil", fieldName = "MinDiffSeedMob", value = "5")
	public static int MANOR_DIFF_SEED_TARGET;
	@ConfigField(config = "spoil", fieldName = "DiffSeedMobPenalty", value = "5")
	public static int MANOR_DIFF_SEED_TARGET_PENALTY;
	@ConfigField(config = "spoil", value = "true")
	public static boolean ALLOW_MANOR;
	@ConfigField(config = "spoil", fieldName = "AltManorRefreshTime", value = "20")
	public static int MANOR_REFRESH_TIME;
	@ConfigField(config = "spoil", fieldName = "AltManorRefreshMin", value = "00")
	public static int MANOR_REFRESH_MIN;
	@ConfigField(config = "spoil", fieldName = "AltManorApproveTime", value = "6")
	public static int MANOR_APPROVE_TIME;
	@ConfigField(config = "spoil", fieldName = "AltManorApproveMin", value = "00")
	public static int MANOR_APPROVE_MIN;
	@ConfigField(config = "spoil", fieldName = "AltManorMaintenancePeriod", value = "360000")
	public static int MANOR_MAINTENANCE_PERIOD;
	@ConfigField(config = "spoil", fieldName = "AltManorSaveAllActions", value = "false")
	public static boolean MANOR_SAVE_ALL_ACTIONS;

	/**
	 * telnet.properies
	 */
	@ConfigField(config = "telnet", fieldName = "EnableTelnet", value = "false")
	public static boolean TELNET_ENABLED;
	@ConfigField(config = "telnet", fieldName = "StatusHost", value = "localhost")
	public static String TELNET_HOST;
	@ConfigField(config = "telnet", fieldName = "StatusPort", value = "3344")
	public static int TELNET_PORT;
	@ConfigField(config = "telnet", fieldName = "StatusPW", value = "password")
	public static String TELNET_PASSWORD;

	/**
	 * altsettings.properties fields
	 */
	/**
	 * PTS Emulation
	 */
	@ConfigField(config = "altsettings", fieldName = "AllowMacrosReuseBug", value = "true")
	public static boolean ALLOW_MACROS_REUSE_BUG;
	@ConfigField(config = "altsettings", fieldName = "NpcDialogDelay", value = "3")
	public static int NPC_DIALOG_PLAYER_DELAY;
	
	@ConfigField(config = "altsettings", fieldName = "Delevel", value = "true")
	public static boolean ALT_GAME_DELEVEL;
	@ConfigField(config = "altsettings", value = "90")
	public static double SKILLS_HIGH_CHANCE_CAP;
	@ConfigField(config = "altsettings", value = "10")
	public static double SKILLS_LOW_CHANCE_CAP;
	@ConfigField(config = "altsettings", value = "75")
	public static double CANCEL_SKILLS_HIGH_CHANCE_CAP;
	@ConfigField(config = "altsettings", value = "5")
	public static double CANCEL_SKILLS_LOW_CHANCE_CAP;
	@ConfigField(config = "altsettings", value = "true")
	public static boolean SKILLS_SHOW_CHANCE;
	@ConfigField(config = "altsettings", value = "false")
	public static boolean SKILLS_USE_SIMPLE_LEVEL_MOD;
	@ConfigField(config = "altsettings", fieldName = "AltShowSkillReuseMessage", value = "true")
	public static boolean ALT_SHOW_REUSE_MSG;
	@ConfigField(config = "altsettings", value = "true")
	public static boolean AUTO_LOOT;
	@ConfigField(config = "altsettings", fieldName = "AutoLootPK", value = "true")
	public static boolean AUTO_LOOT_PK;
	@ConfigField(config = "altsettings", value = "true")
	public static boolean AUTO_LOOT_HERBS;
	@ConfigField(config = "altsettings", value = "false")
	public static boolean HERBS_DIVIDE;
	@ConfigField(config = "altsettings", fieldName = "AltKarmaPlayerCanShop", value = "false")
	public static boolean ALT_GAME_KARMA_PLAYER_CAN_SHOP;
	@ConfigField(config = "altsettings", fieldName = "DoubleSpawn", value = "false")
	public static boolean ALT_DOUBLE_SPAWN;
	@ConfigField(config = "altsettings", fieldName = "AlowDropAugmented", value = "false")
	public static boolean ALT_ALLOW_DROP_AUGMENTED;
	@ConfigField(config = "altsettings", fieldName = "AltUnregisterRecipe", value = "true")
	public static boolean ALT_GAME_UNREGISTER_RECIPE;
	@ConfigField(config = "altsettings", fieldName = "AltShowDroplist", value = "true")
	public static boolean ALT_GAME_SHOW_DROPLIST;
	@ConfigField(config = "altsettings", value = "true")
	public static boolean ALT_SHOW_QUEST_LOAD;
	@ConfigField(config = "altsettings", value = "40")
	public static int ALT_MAX_QUESTS;
	@ConfigField(config = "altsettings", fieldName = "AltGenerateDroplistOnDemand", value = "false")
	public static boolean ALT_GAME_GEN_DROPLIST_ON_DEMAND;
	@ConfigField(config = "altsettings", fieldName = "AllowShiftClick", value = "true")
	public static boolean ALLOW_NPC_SHIFTCLICK;
	@ConfigField(config = "altsettings", fieldName = "AltFullStatsPage", value = "false")
	public static boolean ALT_FULL_NPC_STATS_PAGE;
	@ConfigField(config = "altsettings", fieldName = "AltAllowSubClassWithoutQuest", value = "false")
	public static boolean ALT_GAME_SUBCLASS_WITHOUT_QUESTS;
	@ConfigField(config = "altsettings", fieldName = "AltLevelToGetSubclass", value = "75")
	public static int ALT_GAME_LEVEL_TO_GET_SUBCLASS;
	@ConfigField(config = "altsettings", fieldName = "AltSubAdd", value = "0")
	public static int ALT_GAME_SUB_ADD;
	@ConfigField(config = "altsettings", fieldName = "AltMaxLevel", value = "85")
	public static int ALT_MAX_LEVEL;
	@ConfigField(config = "altsettings", fieldName = "AltMaxSubLevel", value = "80")
	public static int ALT_MAX_SUB_LEVEL;
	@ConfigField(config = "altsettings", value = "86")
	public static int ALT_PET_MAX_LEVEL;
	@ConfigField(config = "altsettings", value = "true")
	public static boolean ALT_ALLOW_OTHERS_WITHDRAW_FROM_CLAN_WAREHOUSE;
	@ConfigField(config = "altsettings", value = "0")
	public static int ALT_ADD_RECIPES;
	@ConfigField(config = "altsettings", fieldName = "Alt100PercentRecipes", value = "false")
	public static boolean ALT_100_RECIPES;
	@ConfigField(config = "altsettings", value = "false")
	public static boolean ALT_TOMA_JUMP;
	@ConfigField(config = "altsettings", fieldName = "SSAnnouncePeriod", value = "0")
	public static int SS_ANNOUNCE_PERIOD;
	@ConfigField(config = "altsettings", value = "false")
	public static boolean AUTO_LEARN_SKILLS;
	@ConfigField(config = "altsettings", fieldName = "AutoLearnForgottens", value = "false")
	public static boolean AUTO_LEARN_FORGOTTEN;
	@ConfigField(config = "altsettings", value = "100000")
	public static int ALT_MAX_FAME_POINTS;
	@ConfigField(config = "altsettings", value = "18")
	public static int ALT_OLY_START_HOUR;
	@ConfigField(config = "altsettings", value = "00")
	public static int ALT_OLY_START_MIN;
	@ConfigField(config = "altsettings", fieldName = "AltOlyCPeriod", value = "21600000")
	public static long ALT_OLY_CPERIOD;
	@ConfigField(config = "altsettings", value = "360000")
	public static long ALT_OLY_BATTLE;
	@ConfigField(config = "altsettings", fieldName = "AltOlyBWait", value = "600000")
	public static long ALT_OLY_BWAIT;
	@ConfigField(config = "altsettings", fieldName = "AltOlyIWait", value = "300000")
	public static long ALT_OLY_IWAIT;
	@ConfigField(config = "altsettings", fieldName = "AltOlyWPeriod", value = "604800000")
	public static long ALT_OLY_WPERIOD;
	@ConfigField(config = "altsettings", fieldName = "AltOlyMinNobleNonClass", value = "10")
	public static byte ALT_OLY_MIN_NOBLE_NCB;
	@ConfigField(config = "altsettings", fieldName = "AltOlyMinNobleClass", value = "10")
	public static byte ALT_OLY_MIN_NOBLE_CB;
	@ConfigField(config = "altsettings", fieldName = "AltOlyMinTeams", value = "6")
	public static byte ALT_OLY_MIN_NOBLE_3x3;
	@ConfigField(config = "altsettings", fieldName = "AltOlyRewardOlympiadTokensNCB", value = "40")
	public static int ALT_OLY_REWARD_TOKENS_NCB;
	@ConfigField(config = "altsettings", fieldName = "AltOlyRewardOlympiadTokensCB", value = "30")
	public static int ALT_OLY_REWARD_TOKENS_CB;
	@ConfigField(config = "altsettings", fieldName = "AltOlyRewardOlympiadTokens3vs3", value = "50")
	public static int ALT_OLY_REWARD_TOKENS_3x3;
	@ConfigField(config = "altsettings", fieldName = "AltOlyHeroRewardPoints", value = "200")
	public static int ALT_OLY_HERO_POINTS_REWARD;
	@ConfigField(config = "altsettings", fieldName = "AltOlyHeroCRPReward", value = "1000")
	public static int ALT_OLY_HERO_CRP_REWARD;
	@ConfigField(config = "altsettings", value = "true")
	public static boolean ALT_OLY_ALLOW_CLIENT_RESTART;
	@ConfigField(config = "altsettings", value = "false")
	public static boolean ALT_SOCIAL_ACTION_REUSE;
	@ConfigField(config = "altsettings", value = "160")
	public static int ALT_OLY_MAX_ARENAS;
	@ConfigField(config = "altsettings", value = "10")
	public static int ALT_OLY_POINTS_DIFF;
	@ConfigField(config = "altsettings", fieldName = "AltOlyMaximumPoints", value = "10000")
	public static int ALT_OLY_MAX_POINTS;
	@ConfigField(config = "altsettings", value = "10")
	public static int ALT_OLY_START_POINTS;
	@ConfigField(config = "altsettings", value = "10")
	public static int ALT_OLY_WEEKLY_POINTS;
	@ConfigField(config = "altsettings", fieldName = "AltOlyClassBaseLimit", value = "30")
	public static int ALT_OLY_CB_LIMIT;
	@ConfigField(config = "altsettings", fieldName = "AltOlyNonClassBaseLimit", value = "60")
	public static int ALT_OLY_NCB_LIMIT;
	@ConfigField(config = "altsettings", value = "10")
	public static int ALT_OLY_TEAM_LIMIT;
	@ConfigField(config = "altsettings", fieldName = "AltOlyTotalMatchesLimit", value = "70")
	public static int ALT_OLY_MATCH_LIMIT;
	@ConfigField(config = "altsettings", fieldName = "AltOlyMinimumMatches", value = "15")
	public static int ALT_OLY_MIN_MATCHES;
	@ConfigField(config = "altsettings", value = "100")
	public static int ALT_OLY_RANK1_POINTS;
	@ConfigField(config = "altsettings", value = "75")
	public static int ALT_OLY_RANK2_POINTS;
	@ConfigField(config = "altsettings", value = "55")
	public static int ALT_OLY_RANK3_POINTS;
	@ConfigField(config = "altsettings", value = "40")
	public static int ALT_OLY_RANK4_POINTS;
	@ConfigField(config = "altsettings", value = "30")
	public static int ALT_OLY_RANK5_POINTS;
	@ConfigField(config = "altsettings", value = "false")
	public static boolean ALT_OLY_ENABLE_HWID_CHECK;
	@ConfigField(config = "altsettings", value = "false")
	public static boolean ALT_DISABLE_EGGS;
	@ConfigField(config = "altsettings", fieldName = "PushkinSignsOptions", value = "false")
	public static boolean ALT_SIMPLE_SIGNS;
	@ConfigField(config = "altsettings", fieldName = "AllowTattoo", value = "false")
	public static boolean ALT_ALLOW_TATTOO;
	@ConfigField(config = "altsettings", fieldName = "BuffLimit", value = "20")
	public static int ALT_BUFF_LIMIT;
	@ConfigField(config = "altsettings", fieldName = "SongDanceLimit", value = "12")
	public static int ALT_SONG_DANCE_LIMIT;
	@ConfigField(config = "altsettings", fieldName = "EnableAltDeathPenalty", value = "false")
	public static boolean ALT_DEATH_PENALTY;
	@ConfigField(config = "altsettings", fieldName = "EnableDeathPenaltyC5", value = "true")
	public static boolean ALLOW_DEATH_PENALTY_C5;
	@ConfigField(config = "altsettings", fieldName = "DeathPenaltyC5Chance", value = "10")
	public static int ALT_DEATH_PENALTY_C5_CHANCE;
	@ConfigField(config = "altsettings", fieldName = "DeathPenaltyC5RateExpPenalty", value = "1")
	public static int ALT_DEATH_PENALTY_C5_EXPERIENCE_PENALTY;
	@ConfigField(config = "altsettings", fieldName = "DeathPenaltyC5RateKarma", value = "1")
	public static int ALT_DEATH_PENALTY_C5_KARMA_PENALTY;
	@ConfigField(config = "altsettings", fieldName = "AltPKDeathRate", value = "0.")
	public static float ALT_PK_DEATH_RATE;
	@ConfigField(config = "altsettings", fieldName = "NonOwnerItemPickupDelay", value = "15")
	public static int NONOWNER_ITEM_PICKUP_DELAY;
	@ConfigField(config = "altsettings", fieldName = "LimitPatkSpd", value = "1500")
	public static int LIM_PATK_SPD;
	@ConfigField(config = "altsettings", fieldName = "LimitMAtkSpd", value = "2000")
	public static int LIM_MATK_SPD;
	@ConfigField(config = "altsettings", fieldName = "LimitCritical", value = "500")
	public static int LIM_CRIT;
	@ConfigField(config = "altsettings", fieldName = "LimitMCritical", value = "20")
	public static int LIM_MCRIT;
	@ConfigField(config = "altsettings", fieldName = "LimitMoveSpeed", value = "250")
	public static int LIM_MOVE_SPEED;
	@ConfigField(config = "altsettings", fieldName = "MaxRiftJumps", value = "4")
	public static int RIFT_MAX_JUMPS;
	@ConfigField(config = "altsettings", fieldName = "AutoJumpsDelay", value = "10")
	public static int RIFT_AUTO_JUMPS_TIME;
	@ConfigField(config = "altsettings", fieldName = "RecruitFC", value = "18")
	public static int RIFT_ENTER_COST_RECRUIT;
	@ConfigField(config = "altsettings", fieldName = "SoldierFC", value = "21")
	public static int RIFT_ENTER_COST_SOLDIER;
	@ConfigField(config = "altsettings", fieldName = "OfficerFC", value = "24")
	public static int RIFT_ENTER_COST_OFFICER;
	@ConfigField(config = "altsettings", fieldName = "CaptainFC", value = "27")
	public static int RIFT_ENTER_COST_CAPTAIN;
	@ConfigField(config = "altsettings", fieldName = "CommanderFC", value = "30")
	public static int RIFT_ENTER_COST_COMMANDER;
	@ConfigField(config = "altsettings", fieldName = "HeroFC", value = "33")
	public static int RIFT_ENTER_COST_HERO;
	@ConfigField(config = "altsettings", value = "10000")
	public static int RIFT_SPAWN_DELAY;
	@ConfigField(config = "altsettings", fieldName = "AllowLearnTransSkillsWOQuest", value = "false")
	public static boolean ALLOW_LEARN_TRANS_SKILLS_WO_QUEST;
	@ConfigField(config = "altsettings", value = "true")
	public static boolean PARTY_LEADER_ONLY_CAN_INVITE;
	@ConfigField(config = "altsettings", value = "true")
	public static boolean ALLOW_TALK_WHILE_SITTING;
	@ConfigField(config = "altsettings", fieldName = "AllowNobleTPToAll", value = "false")
	public static boolean ALLOW_NOBLE_TP_TO_ALL;
	@ConfigField(config = "altsettings", value = "0.0")
	public static double FAKE_PLAYERS_FACTOR;
	@ConfigField(config = "altsettings", fieldName = "FakePlayersFactorHour", value = "1.19;1.17;1.13;1.2;1;1;1;1;1;1.11;1.12;1.13;1.14;1.15;1.2;1.2;1.3;1.3;1.3;1.4;1.5;1.5;1.4;1.2")
	public static double[] FAKE_PLAYERS_FACTOR_HOUR;
	@ConfigField(config = "altsettings", value = "30")
	public static int FAKE_PLAYER_WALK_CHANCE;
	@ConfigField(config = "altsettings", value = "50")
	public static int FAKE_PLAYER_WALK_MIN;
	@ConfigField(config = "altsettings", value = "500")
	public static int FAKE_PLAYER_WALK_MAX;
	@ConfigField(config = "altsettings", value = "60000")
	public static int FAKE_PLAYER_WALK_TIME_MIN;
	@ConfigField(config = "altsettings", value = "600000")
	public static int FAKE_PLAYER_WALK_TIME_MAX;
	@ConfigField(config = "altsettings", fieldName = "BuffTimeModifier", value = "1.0")
	public static float BUFFTIME_MODIFIER;
	@ConfigField(config = "altsettings", fieldName = "SongDanceTimeModifier", value = "1.0")
	public static float SONGDANCETIME_MODIFIER;
	@ConfigField(config = "altsettings", fieldName = "MaxLoadModifier", value = "1.0")
	public static float MAXLOAD_MODIFIER;
	@ConfigField(config = "altsettings", fieldName = "AltOldShutdownMsg", value = "true")
	public static boolean OLD_SHUTDOWN_MSG;
	@ConfigField(config = "altsettings", value = "3")
	public static int ALT_MAX_ALLY_SIZE;
	@ConfigField(config = "altsettings", value = "1500")
	public static int ALT_PARTY_DISTRIBUTION_RANGE;
	@ConfigField(config = "altsettings", value = "5.0")
	public static float ALT_CHANCE_CRITICAL_MAGIC;
	@ConfigField(config = "altsettings", fieldName = "DaysBeforeCreateAClan", value = "1")
	public static int DaysBeforeCreateAClan;
	@ConfigField(config = "altsettings", fieldName = "HoursBeforeJoinAClan", value = "24")
	public static int HoursBeforeJoinAClan;
	@ConfigField(config = "altsettings", fieldName = "AltClanMembersForWar", value = "15")
	public static int AltClanMembersForWar;
	@ConfigField(config = "altsettings", fieldName = "AltMinClanLvlForWar", value = "3")
	public static int AltMinClanLvlForWar;
	@ConfigField(config = "altsettings", fieldName = "AltClanWarMax", value = "30")
	public static int AltClanWarMax;
	@ConfigField(config = "altsettings", fieldName = "DaysBeforeCreateNewAllyWhenDissolved", value = "1")
	public static int DaysBeforeCreateNewAllyWhenDissolved;
	@ConfigField(config = "altsettings", fieldName = "MinLevelToCreatePledge", value = "10")
	public static int MinLevelToCreatePledge;
	@ConfigField(config = "altsettings", fieldName = "MemberForLevel6", value = "30")
	public static int MemberForLevel6;
	@ConfigField(config = "altsettings", fieldName = "MemberForLevel7", value = "50")
	public static int MemberForLevel7;
	@ConfigField(config = "altsettings", fieldName = "MemberForLevel8", value = "80")
	public static int MemberForLevel8;
	@ConfigField(config = "altsettings", fieldName = "MemberForLevel9", value = "120")
	public static int MemberForLevel9;
	@ConfigField(config = "altsettings", fieldName = "MemberForLevel10", value = "140")
	public static int MemberForLevel10;
	@ConfigField(config = "altsettings", fieldName = "MemberForLevel11", value = "170")
	public static int MemberForLevel11;
	@ConfigField(config = "altsettings", value = "false")
	public static boolean ALT_SAVE_PRIVATE_STORE;
	@ConfigField(config = "altsettings", value = "true")
	public static boolean CHECK_SKILLS_POSSIBLE;
	@ConfigField(config = "altsettings", value = "true")
	public static boolean ALLOW_LOTTERY;
	@ConfigField(config = "altsettings", value = "50000")
	public static int ALT_LOTTERY_PRIZE;
	@ConfigField(config = "altsettings", value = "2000")
	public static int ALT_LOTTERY_TICKET_PRICE;
	@ConfigField(config = "altsettings", value = "0.6")
	public static float ALT_LOTTERY_5_NUMBER_RATE;
	@ConfigField(config = "altsettings", value = "0.2")
	public static float ALT_LOTTERY_4_NUMBER_RATE;
	@ConfigField(config = "altsettings", value = "0.2")
	public static float ALT_LOTTERY_3_NUMBER_RATE;
	@ConfigField(config = "altsettings", fieldName = "AltLottery2and1NumberPrize", value = "200")
	public static int ALT_LOTTERY_2_AND_1_NUMBER_PRIZE;
	@ConfigField(config = "altsettings", fieldName = "ChampionEnable", value = "false")
	public static boolean ALT_CHAMPION_ENABLE;
	@ConfigField(config = "altsettings", fieldName = "ChampionChance", value = "0")
	public static int ALT_CHAMPION_CHANCE;
	@ConfigField(config = "altsettings", fieldName = "Champion2Chance", value = "0")
	public static int ALT_CHAMPION2_CHANCE;
	@ConfigField(config = "altsettings", fieldName = "ChampionMinLevel", value = "20")
	public static int ALT_CHAMP_MIN_LVL;
	@ConfigField(config = "altsettings", fieldName = "ChampionMaxLevel", value = "60")
	public static int ALT_CHAMP_MAX_LVL;
	@ConfigField(config = "altsettings", fieldName = "ChampionHp", value = "7")
	public static float ALT_CHAMPION_HP;
	@ConfigField(config = "altsettings", fieldName = "ChampionHpRegen", value = "1.")
	public static float ALT_CHAMPION_HP_REGEN;
	@ConfigField(config = "altsettings", fieldName = "ChampionRewards", value = "8")
	public static int ALT_CHAMPION_REWARDS;
	@ConfigField(config = "altsettings", fieldName = "ChampionAdenasRewards", value = "1")
	public static int ALT_CHAMPION_ADENA_REWARDS;
	@ConfigField(config = "altsettings", fieldName = "ChampionAtk", value = "1.")
	public static float ALT_CHAMPION_ATK;
	@ConfigField(config = "altsettings", fieldName = "ChampionSpdAtk", value = "1.")
	public static float ALT_CHAMPION_SPD_ATK;
	@ConfigField(config = "altsettings", fieldName = "ChampionRewardItem", value = "0")
	public static int ALT_CHAMPION_REWARD;
	@ConfigField(config = "altsettings", fieldName = "Champion2Mult", value = "3")
	public static float ALT_CHAMPION2_MUL;
	@ConfigField(config = "altsettings", fieldName = "ChampionRewardItemID", value = "6393")
	public static int ALT_CHAMPION_REWARD_ID;
	@ConfigField(config = "altsettings", fieldName = "ChampionRewardItemQty", value = "1")
	public static int ALT_CHAMPION_REWARD_QTY;
	@ConfigField(config = "altsettings", fieldName = "ChampionShowAura", value = "false")
	public static boolean ALT_CHAMPION_SHOW_AURA;
	@ConfigField(config = "altsettings", fieldName = "ChampionAggro", value = "false")
	public static boolean ALT_CHAMPION_AGGRO;
	@ConfigField(config = "altsettings", fieldName = "ChampionSocial", value = "false")
	public static boolean ALT_CHAMPION_SOCIAL;
	@ConfigField(config = "altsettings", fieldName = "TeleportBookMaxSize", value = "9")
	public static int ALT_TELEPORT_BOOK_SIZE;
	@ConfigField(config = "altsettings", fieldName = "HerbLifeTime", value = "60")
	public static long ALT_HERB_LIFE_TIME;
	@ConfigField(config = "altsettings", fieldName = "BattleFieldPenaltyChance", value = "30")
	public static int BATTLEFIELD_PENALTY_CHANCE;
	@ConfigField(config = "altsettings", fieldName = "AltEnterWorldAnnounce", value = "")
	public static String ALT_ANNOUNCE_TEXT;
	@ConfigField(config = "altsettings", fieldName = "AltEnterWorldAnnounceMaxLevel", value = "0")
	public static int ALT_ANNOUNCE_MAX_LEVEL;
	@ConfigField(config = "altsettings", fieldName = "AltPvPAutoAttack", value = "false")
	public static boolean ALT_PVP_AUTO_ATTACK;
	@ConfigField(config = "altsettings", fieldName = "AltAnnouncePvP", value = "false")
	public static boolean ALT_ANNOUNCE_PVP;
	@ConfigField(config = "altsettings", value = "true")
	public static boolean PETITIONING_ALLOWED;
	@ConfigField(config = "altsettings", value = "5")
	public static int MAX_PETITIONS_PER_PLAYER;
	@ConfigField(config = "altsettings", value = "25")
	public static int MAX_PETITIONS_PENDING;
	@ConfigField(config = "altsettings", fieldName = "FloatingRateEnable", value = "false")
	public static boolean ALT_FLOATING_RATE_ENABLE;
	@ConfigField(config = "altsettings", fieldName = "FloatingRateMin", value = "5")
	public static int ALT_FLOATING_RATE_MIN;
	@ConfigField(config = "altsettings", fieldName = "FloatingRateMax", value = "15")
	public static int ALT_FLOATING_RATE_MAX;
	@ConfigField(config = "altsettings", fieldName = "FloatingRatePoints", value = "12")
	public static int ALT_FLOATING_RATE_POINTS;
	@ConfigField(config = "altsettings", fieldName = "FloatingRateNextChange", value = "86400")
	public static long ALT_FLOATING_RATE_NEXT_CHANGE;
	@ConfigField(config = "altsettings", fieldName = "FloatingRateItemId", value = "4037")
	public static int ALT_FLOATING_RATE_ITEM_ID;
	@ConfigField(config = "altsettings", fieldName = "FloatingRateItemCount", value = "1")
	public static int ALT_FLOATING_RATE_ITEM_COUNT;
	@ConfigField(config = "altsettings", fieldName = "FloatingRateShowConfigForLevel", value = "10")
	public static int ALT_FLOATING_RATE_SHOW_CONFIG;
	@ConfigField(config = "altsettings", fieldName = "FloatingRateExtraPointsLimit", value = "4")
	public static int ALT_FLOATING_RATE_EXTRA_LIMIT;
	@ConfigField(config = "altsettings", fieldName = "FloatingRateExtraPointsSellCount", value = "1")
	public static int ALT_FLOATING_RATE_EXTRA_SELL_COUNT;
	@ConfigField(config = "altsettings", fieldName = "FloatingRateExtraPointsItemId", value = "4037")
	public static int ALT_FLOATING_RATE_EXTRA_ITEM_ID;
	@ConfigField(config = "altsettings", fieldName = "FloatingRateExtraPointsItemCount", value = "1")
	public static int ALT_FLOATING_RATE_EXTRA_ITEM_COUNT;
	@ConfigField(config = "altsettings", fieldName = "FloatingRateExtraPointsDays", value = "30")
	public static int ALT_FLOATING_RATE_EXTRA_TIME;
	@ConfigField(config = "altsettings", fieldName = "FloatingRateExtraPointsItemCountMul", value = "2")
	public static double ALT_FLOATING_RATE_EXTRA_MUL;
	@ConfigField(config = "altsettings", fieldName = "PrivateStoreMinRadius", value = "0")
	public static int ALT_MIN_PRIVATE_STORE_RADIUS;
	@ConfigField(config = "altsettings", fieldName = "CharacterCreateLevel", value = "0")
	public static int CHARACTER_CREATE_LEVEL;
	@ConfigField(config = "altsettings", fieldName = "FirstCharacterCreateLevel", value = "0")
	public static int FIRST_CHARACTER_LEVEL;
	@ConfigField(config = "altsettings", fieldName = "FirstCharacterCreateWarehouseAdena", value = "0")
	public static long FIRST_CHARACTER_WH_ADENA;
	@ConfigField(config = "altsettings", value = "true")
	public static boolean ALT_DROP_HUNGRY_PET_CONTROL_ITEM;
	@ConfigField(config = "altsettings", value = "false")
	public static boolean DEBUG_OLYMP_MOVE;
	@ConfigField(config = "altsettings", value = "480")
	public static int FAME_DROP_PENALTY_MIN;
	@ConfigField(config = "altsettings", value = "0")
	public static long ALT_TERRITORY_WARD_PEACE_ZONE_TIMEOUT;
	@ConfigField(config = "altsettings", value = "900")
	public static int FAME_DROP_PENALTY_MAX;
	@ConfigField(config = "altsettings", value = "false")
	public static boolean ALT_ONE_SHOT_KILL_PROTECT;
	@ConfigField(config = "altsettings", value = "10000")
	public static int ALT_ONE_SHOT_KILL_PROTECT_DAMAGE;
	public static GArray<String> USE_SHOTS_EFFECT_LIST;

	/**
	 * services.properties fields
	 */
	@ConfigField(config = "services", fieldName = "AllowProofMarkSell", value = "false")
	public static boolean ALT_CLASSMASTER_INSTALLED;
	public static ArrayList<Integer> ALLOW_CLASS_MASTERS_LIST = new ArrayList<>();
	public static int[] CLASS_MASTERS_PRICE_LIST = new int[4];
	@ConfigField(config = "services", value = "57")
	public static int CLASS_MASTERS_PRICE_ITEM;
	@ConfigField(config = "services", fieldName = "NickChangeEnabled", value = "false")
	public static boolean SERVICES_CHANGE_NICK_ENABLED;
	@ConfigField(config = "services", fieldName = "NickChangePrice", value = "100")
	public static int SERVICES_CHANGE_NICK_PRICE;
	@ConfigField(config = "services", fieldName = "NickChangeItem", value = "4037")
	public static int SERVICES_CHANGE_NICK_ITEM;
	@ConfigField(config = "services", fieldName = "PetNameChangeEnabled", value = "false")
	public static boolean SERVICES_CHANGE_PET_NAME_ENABLED;
	@ConfigField(config = "services", fieldName = "PetNameChangePrice", value = "100")
	public static int SERVICES_CHANGE_PET_NAME_PRICE;
	@ConfigField(config = "services", fieldName = "PetNameChangeItem", value = "4037")
	public static int SERVICES_CHANGE_PET_NAME_ITEM;
	@ConfigField(config = "services", fieldName = "BabyPetExchangeEnabled", value = "false")
	public static boolean SERVICES_EXCHANGE_BABY_PET_ENABLED;
	@ConfigField(config = "services", fieldName = "BabyPetExchangePrice", value = "100")
	public static int SERVICES_EXCHANGE_BABY_PET_PRICE;
	@ConfigField(config = "services", fieldName = "BabyPetExchangeItem", value = "4037")
	public static int SERVICES_EXCHANGE_BABY_PET_ITEM;
	@ConfigField(config = "services", fieldName = "NickColorChangePrice", value = "100")
	public static int SERVICES_CHANGE_NICK_COLOR_PRICE;
	@ConfigField(config = "services", fieldName = "NickColorChangeItem", value = "4037")
	public static int SERVICES_CHANGE_NICK_COLOR_ITEM;
	@ConfigField(config = "services", fieldName = "NickColorChangeList", value = "00FF00")
	public static String[] SERVICES_CHANGE_NICK_COLOR_LIST;
	@ConfigField(config = "services", fieldName = "TitleColorChangePrice", value = "100")
	public static int SERVICES_CHANGE_TITLE_COLOR_PRICE;
	@ConfigField(config = "services", fieldName = "TitleColorChangeItem", value = "4037")
	public static int SERVICES_CHANGE_TITLE_COLOR_ITEM;
	@ConfigField(config = "services", fieldName = "TitleColorChangeList", value = "00FF00")
	public static String[] SERVICES_CHANGE_TITLE_COLOR_LIST;
	@ConfigField(config = "services", fieldName = "ColorsPerPage", value = "50")
	public static int SERVICES_CHANGE_NICK_COLOR_PER_PAGE;
	@ConfigField(config = "services", fieldName = "BashEnabled", value = "false")
	public static boolean SERVICES_BASH_ENABLED;
	@ConfigField(config = "services", fieldName = "BashSkipDownload", value = "false")
	public static boolean SERVICES_BASH_SKIP_DOWNLOAD;
	@ConfigField(config = "services", fieldName = "BashReloadTime", value = "24")
	public static int SERVICES_BASH_RELOAD_TIME;
	@ConfigField(config = "services", value = "false")
	public static boolean PREMIUM_ENABLED;
	public static GArray<int[]> PREMIUM_PRICES;
	@ConfigField(config = "services", fieldName = "PremiumSexChangePrice", value = "4037;20")
	public static int[] PREMIUM_SEX_CHANGE_PRICE;
	@ConfigField(config = "services", fieldName = "PremiumRateExpSp", value = "50")
	public static int PREMIUM_RATE_EXPSP;
	@ConfigField(config = "services", value = "1")
	public static float PREMIUM_RATE_DROP_COUNT;
	@ConfigField(config = "services", value = "0")
	public static int PREMIUM_INVENTORY_LIMIT;
	@ConfigField(config = "services", value = "0")
	public static int PREMIUM_WAREHOUSE_LIMIT;
	@ConfigField(config = "services", value = "0")
	public static int PREMIUM_WEIGHT_LIMIT;
	@ConfigField(config = "services", value = "0")
	public static int PREMIUM_PRIVATE_STORE_LIMIT;
	@ConfigField(config = "services", fieldName = "PremiumAutoLootOnly", value = "true")
	public static boolean PREMIUM_AUTOLOOT_ONLY;
	@ConfigField(config = "services", value = "true")
	public static boolean PREMIUM_FREE_COLOR_CHANGE;
	@ConfigField(config = "services", fieldName = "PremiumRimKamolokaOnly", value = "true")
	public static boolean PREMIUM_RIM_ONLY;
	@ConfigField(config = "services", fieldName = "PremiumKamalokaReset", value = "true")
	public static boolean PREMIUM_RESET_KAMA;
	@ConfigField(config = "services", value = "-1")
	public static int PREMIUM_MIN_CLAN_LEVEL;
	@ConfigField(config = "services", value = "300")
	public static int PREMIUM_MANAGER_ITEMS_LOAD_DELAY;
	@ConfigField(config = "services", fieldName = "CommunityBufferEnable", value = "false")
	public static boolean SERVICES_COMMUNITY_BUFFER;
	@ConfigField(config = "services", fieldName = "CommunityBufferAutoBuff", value = "false")
	public static boolean SERVICES_COMMUNITY_BUFFER_AUTOBUFF;
	@ConfigField(config = "services", fieldName = "NoblessSellEnabled", value = "false")
	public static boolean SERVICES_NOBLESS_SELL_ENABLED;
	@ConfigField(config = "services", fieldName = "NoblessSellPrice", value = "1000")
	public static int SERVICES_NOBLESS_SELL_PRICE;
	@ConfigField(config = "services", fieldName = "NoblessSellItem", value = "4037")
	public static int SERVICES_NOBLESS_SELL_ITEM;
	@ConfigField(config = "services", fieldName = "HowToGetCoL", value = "false")
	public static boolean SERVICES_HOW_TO_GET_COL;
	@ConfigField(config = "services", fieldName = "AllowOfflineTrade", value = "false")
	public static boolean SERVICES_OFFLINE_TRADE_ALLOW;
	@ConfigField(config = "services", fieldName = "OfflineTraderMinimumLvl", value = "1")
	public static int SERVICES_OFFLINE_TRADE_MINLVL;
	@ConfigField(config = "services", fieldName = "OfflineTradeNameColor", value = "0xB0FFFF")
	public static int SERVICES_OFFLINE_TRADE_NAME_COLOR;
	@ConfigField(config = "services", fieldName = "KickOfflineNotTrading", value = "true")
	public static boolean SERVICES_OFFLINE_TRADE_KICK_NOT_TRADING;
	@ConfigField(config = "services", fieldName = "OfflineTradePriceItem", value = "0")
	public static int SERVICES_OFFLINE_TRADE_PRICE_ITEM;
	@ConfigField(config = "services", fieldName = "OfflineTradePrice", value = "0")
	public static int SERVICES_OFFLINE_TRADE_PRICE;
	@ConfigField(config = "services", fieldName = "OfflineTradeDaysToKick", value = "14")
	public static int SERVICES_OFFLINE_TRADE_DAYS_TO_KICK;
	@ConfigField(config = "services", fieldName = "OfflineRestoreAfterRestart", value = "true")
	public static boolean SERVICES_OFFLINE_TRADE_RESTORE_AFTER_RESTART;
	@ConfigField(config = "services", fieldName = "RideHire", value = "false")
	public static boolean SERVECES_RIDEHIRE;
	@ConfigField(config = "services", fieldName = "NoTradeOnlyOffline", value = "false")
	public static boolean SERVICES_NO_TRADE_ONLY_OFFLINE;
	@ConfigField(config = "services", fieldName = "TradeTax", value = "0.0")
	public static float SERVICES_TRADE_TAX;
	@ConfigField(config = "services", fieldName = "OffshoreTradeTax", value = "0.0")
	public static float SERVICES_OFFSHORE_TRADE_TAX;
	@ConfigField(config = "services", fieldName = "TradeTaxOnlyOffline", value = "false")
	public static boolean SERVICES_TRADE_TAX_ONLY_OFFLINE;
	@ConfigField(config = "services", fieldName = "NoCastleTaxInOffshore", value = "false")
	public static boolean SERVICES_OFFSHORE_NO_CASTLE_TAX;
	@ConfigField(config = "services", fieldName = "GiranHarborZone", value = "false")
	public static boolean SERVICES_GIRAN_HARBOR_ENABLED;
	@ConfigField(config = "services", fieldName = "LockAccountIP", value = "false")
	public static boolean SERVICES_LOCK_ACCOUNT_IP;
	@ConfigField(config = "services", fieldName = "ChangePassword", value = "true")
	public static boolean SERVICES_CHANGE_PASSWORD;
	@ConfigField(config = "services", fieldName = "AutoHealActive", value = "false")
	public static boolean SERVICES_AUTO_HEAL_ACTIVE;
	@ConfigField(config = "services", fieldName = "AllowFreeExchangeWeapons", value = "0")
	public static long SERVICE_FREE_EXCHANGE_MAX_WEAPON_PRICE;
	@ConfigField(config = "services", fieldName = "AllowExchangeWeapons", value = "0")
	public static long SERVICE_EXCHANGE_MAX_WEAPON_PRICE;
	@ConfigField(config = "services", fieldName = "ItemForPayToExchange", value = "4037")
	public static int SERVICE_EXCHANGE_WEAPON_PAY;
	@ConfigField(config = "services", fieldName = "PayToExchangeCount", value = "4037")
	public static long SERVICE_EXCHANGE_WEAPON_PAY_COUNT;
	@ConfigField(config = "services", fieldName = "AllowExchangeRarWeapons", value = "0")
	public static long SERVICE_EXCHANGE_RAR_WEAPON_PRICE;
	@ConfigField(config = "services", fieldName = "ItemForPayToExchangeRar", value = "4037")
	public static int SERVICE_EXCHANGE_RAR_WEAPON_PAY;
	@ConfigField(config = "services", fieldName = "PayToRarExchangeCount", value = "0")
	public static long SERVICE_EXCHANGE_RAR_WEAPON_PAY_COUNT;
	@ConfigField(config = "services", fieldName = "AllowExchangeAllRarArmors", value = "false")
	public static boolean SERVICE_EXCHANGE_RAR_ARMOR;
	@ConfigField(config = "services", fieldName = "ItemForPayToExchangeRarArmor", value = "4037")
	public static int SERVICE_EXCHANGE_RAR_ARMOR_PAY;
	@ConfigField(config = "services", fieldName = "PayToRarArmorExchangeCount", value = "0")
	public static long SERVICE_EXCHANGE_RAR_ARMOR_PAY_COUNT;
	@ConfigField(config = "services", fieldName = "AllowExchangeAllArmors", value = "false")
	public static boolean SERVICE_EXCHANGE_ARMOR;
	@ConfigField(config = "services", fieldName = "ItemForPayToExchangeArmor", value = "4037")
	public static int SERVICE_EXCHANGE_ARMOR_PAY;
	@ConfigField(config = "services", fieldName = "PayToArmorExchangeCount", value = "0")
	public static long SERVICE_EXCHANGE_ARMOR_PAY_COUNT;
	@ConfigField(config = "services", fieldName = "AllowUpgradeWeapons", value = "0")
	public static long SERVICE_UPGRADE_MAX_WEAPON_PRICE;
	@ConfigField(config = "services", fieldName = "ItemForPayToUpdate", value = "5575")
	public static int SERVICE_UPGRADE_PAY;
	@ConfigField(config = "services", fieldName = "PayToUpdateCount", value = "-1")
	public static long SERVICE_UPGRADE_PAY_COUNT;
	@ConfigField(config = "services", fieldName = "PayToUpdateCountMod", value = "1.2")
	public static double SERVICE_UPGRADE_PAY_COUNT_MOD;
	@ConfigField(config = "services", fieldName = "ExchangeEnchantedArmor", value = "false")
	public static boolean SERVICE_EXCHANGE_ENCHANTED_ARMOR;
	@ConfigField(config = "services", fieldName = "ExchangeEnchantedArmorMinEnchant", value = "40")
	public static int SERVICE_EXCHANGE_ENCHANTED_ARMOR_MIN_ENCHANT;
	@ConfigField(config = "services", fieldName = "ExchangeEnchantedArmorPenalty", value = "20")
	public static int SERVICE_EXCHANGE_ENCHANTED_ARMOR_PENALTY;
	@ConfigField(config = "services", fieldName = "ExchangeEnchantedWeapon", value = "false")
	public static boolean SERVICE_EXCHANGE_ENCHANTED_WEAPON;
	@ConfigField(config = "services", fieldName = "ExchangeEnchantedWeaponMinEnchant", value = "40")
	public static int SERVICE_EXCHANGE_ENCHANTED_WEAPON_MIN_ENCHANT;
	@ConfigField(config = "services", fieldName = "ExchangeEnchantedWeaponPenalty", value = "20")
	public static int SERVICE_EXCHANGE_ENCHANTED_WEAPON_PENALTY;
	@ConfigField(config = "services", value = "false")
	public static boolean PRODUCT_SHOP_ENABLED;
	@ConfigField(config = "services", fieldName = "ProductServerIP", value = "127.0.0.1")
	public static String PRODUCT_SERVER_HOST;
	@ConfigField(config = "services", value = "10100")
	public static int PRODUCT_SERVER_PORT;
	@ConfigField(config = "services", value = "80423")
	public static int PRODUCT_SERVER_PROTOCOL;
	@ConfigField(config = "services", value = "8")
	public static int PRODUCT_LOCATION_ID;
	@ConfigField(config = "services", value = "false")
	public static boolean PRODUCT_SERVER_DEBUG;
	@ConfigField(config = "services", fieldName = "TransferManager", value = "false")
	public static boolean TM_ENABLED;
	@ConfigField(config = "services", fieldName = "TransferManagerPeriod", value = "900")
	public static int TM_PERIOD;
	@ConfigField(config = "services", value = "5")
	public static int GWS_SEX_CHANGE_COST;
	@ConfigField(config = "services", fieldName = "GwsRecommendationCost", value = "1")
	public static int GWS_REC_COST;
	@ConfigField(config = "services", fieldName = "GwsRecommendationAmount", value = "10")
	public static int GWS_REC_AMOUNT;
	@ConfigField(config = "services", value = "false")
	public static boolean MAIL_ANNOUNCE_ENABLED;

	/**
	 * pvp.properties fields
	 */
	@ConfigField(config = "pvp", fieldName = "MinKarma", value = "240")
	public static int KARMA_MIN_KARMA;
	@ConfigField(config = "pvp", fieldName = "SPDivider", value = "7")
	public static int KARMA_SP_DIVIDER;
	@ConfigField(config = "pvp", fieldName = "BaseKarmaLost", value = "0")
	public static int KARMA_LOST_BASE;
	@ConfigField(config = "pvp", fieldName = "CanGMDropEquipment", value = "false")
	public static boolean KARMA_DROP_GM;
	@ConfigField(config = "pvp", value = "true")
	public static boolean KARMA_NEEDED_TO_DROP;
	public static ArrayList<Integer> KARMA_LIST_NONDROPPABLE_ITEMS;
	@ConfigField(config = "pvp", fieldName = "MaxItemsDroppable", value = "10")
	public static int KARMA_DROP_ITEM_LIMIT;
	@ConfigField(config = "pvp", fieldName = "MinPKToDropItems", value = "5")
	public static int MIN_PK_TO_ITEMS_DROP;
	@ConfigField(config = "pvp", fieldName = "MaxDropThrowDistance", value = "70")
	public static int KARMA_RANDOM_DROP_LOCATION_LIMIT;
	@ConfigField(config = "pvp", fieldName = "ChanceOfDropMinimum", value = "1")
	public static int KARMA_DROPCHANCE_MINIMUM;
	@ConfigField(config = "pvp", fieldName = "ChanceOfDropMultiplier", value = "1")
	public static int KARMA_DROPCHANCE_MULTIPLIER;
	@ConfigField(config = "pvp", fieldName = "ChanceOfDropEquipped", value = "20")
	public static int KARMA_DROPCHANCE_EQUIPMENT;
	@ConfigField(config = "pvp", fieldName = "NoDropEenchantAt", value = "45")
	public static int KARMA_DROPCHANCE_MAXENCHANT;
	@ConfigField(config = "pvp", fieldName = "ChanceOfDropEquippedWeapon", value = "25")
	public static int KARMA_DROPCHANCE_EQUIPPED_WEAPON;
	@ConfigField(config = "pvp", fieldName = "MaxEnchantForVisPlayer", value = "25")
	public static int MAXENCHANT_FOR_VISPLAYER;
	@ConfigField(config = "pvp", fieldName = "MaxEnchantOlyPlayer", value = "65535")
	public static int MAXENCHANT_OLYPLAYER;
	@ConfigField(config = "pvp", fieldName = "PvPTime", value = "120000")
	public static int PVP_TIME;
	@ConfigField(config = "pvp", fieldName = "PvPBlinkTime", value = "20000")
	public static int PVP_BLINK_TIME;

	/**
	 * ai.properties fields
	 */
	@ConfigField(config = "ai", value = "false")
	public static boolean RND_WALK;
	@ConfigField(config = "ai", value = "2")
	public static int RND_WALK_RATE;
	@ConfigField(config = "ai", value = "3")
	public static int RND_ANIMATION_RATE;
	@ConfigField(config = "ai", value = "1000")
	public static int AGGRO_CHECK_INTERVAL;
	@ConfigField(config = "ai", value = "300")
	public static int MAX_DRIFT_RANGE;
	@ConfigField(config = "ai", value = "40000")
	public static int MAX_PURSUE_RANGE;
	@ConfigField(config = "ai", value = "5000")
	public static int MAX_PURSUE_RANGE_RAID;
	@ConfigField(config = "ai", value = "false")
	public static boolean ALT_AI_KELTIRS;
	@ConfigField(config = "ai", value = "false")
	public static boolean DEBUG_AI;

	/**
	 * events.properties fields
	 */
	public static ExProperties eventsProperties;

	@ConfigField(config = "events", fieldName = "Min_lvl", value = "1")
	public static int EVENT_Min_lvl;
	@ConfigField(config = "events", fieldName = "Max_lvl", value = "85")
	public static int EVENT_Max_lvl;
	@ConfigField(config = "events", fieldName = "price", value = "5000")
	public static int EVENT_price;
	@ConfigField(config = "events", fieldName = "Buffer_Siege", value = "false")
	public static boolean EVENT_Buffer_Siege;
	@ConfigField(config = "events", fieldName = "EVENT_BUFFER_MOD", value = "1")
	public static int EVENT_BUFFER_MOD;
	@ConfigField(config = "events", fieldName = "CofferOfShadowsPriceRate", value = "1")
	public static int EVENT_CofferOfShadowsPriceRate;
	@ConfigField(config = "events", fieldName = "CofferOfShadowsRewardRate", value = "1.")
	public static float EVENT_CofferOfShadowsRewardRate;
	@ConfigField(config = "events", fieldName = "CM_SellSS", value = "false")
	public static boolean EVENT_ClassmastersSellsSS;
	@ConfigField(config = "events", fieldName = "CM_CoLShop", value = "false")
	public static boolean EVENT_ClassmastersCoLShop;
	@ConfigField(config = "events", fieldName = "TvT_Enabled", value = "false")
	public static boolean EVENT_TvT_Enabled;
	@ConfigField(config = "events", fieldName = "TvT_PrepareTime", value = "2")
	public static int EVENT_TvT_PrepareTime;
	@ConfigField(config = "events", fieldName = "TvT_FightTime", value = "10")
	public static int EVENT_TvT_FightTime;
	@ConfigField(config = "events", fieldName = "TvT_TeamRandomType", value = "1")
	public static int EVENT_TvT_TeamRandomType;
	@ConfigField(config = "events", fieldName = "TvT_CheckHWID", value = "true")
	public static boolean EVENT_TvT_CheckHWID;
	public static StatsSet[] EVENT_TvT_Config;
	@ConfigField(config = "events", fieldName = "LastHero_enabled", value = "false")
	public static boolean EVENT_LastHero_enabled;
	@ConfigField(config = "events", fieldName = "LastHero_bonus_id", value = "57")
	public static int EVENT_LastHeroBonusID;
	@ConfigField(config = "events", fieldName = "LastHero_bonus_count", value = "5000")
	public static int EVENT_LastHeroBonusCount;
	@ConfigField(config = "events", fieldName = "LastHero_time", value = "5")
	public static int EVENT_LastHeroTime;
	@ConfigField(config = "events", fieldName = "LastHero_MinParticipants", value = "2")
	public static int EVENT_LastHeroMinParticipants;
	@ConfigField(config = "events", fieldName = "LastHero_rate", value = "true")
	public static boolean EVENT_LastHeroRate;
	public static Crontab EVENT_LastHero_cron;
	@ConfigField(config = "events", fieldName = "LastHero_fighttime", value = "5")
	public static int EVENT_LastHero_FightTime;
	@ConfigField(config = "events", fieldName = "LastHero_heroMod", value = "false")
	public static boolean EVENT_LastHero_heroMod;
	@ConfigField(config = "events", fieldName = "LastHero_ruleMsg1", value = "Эвент длится 5 минут.")
	public static String EVENT_LastHero_ruleMsg1;
	@ConfigField(config = "events", fieldName = "LastHero_ruleMsg2", value = "Цель - убить как можно больше игроков.")
	public static String EVENT_LastHero_ruleMsg2;
	@ConfigField(config = "events", fieldName = "LastHero_ruleMsg3", value = "За каждого убитого вам дают 5000 помноженые на его уровень, аден.")
	public static String EVENT_LastHero_ruleMsg3;
	@ConfigField(config = "events", fieldName = "LastHero_ruleMsg4", value = "Старт через 2 минуты, по команде 'FIGHT!!!")
	public static String EVENT_LastHero_ruleMsg4;
	@ConfigField(config = "events", fieldName = "LastHero_msgStart", value = "Запущен эвент Last Hero.")
	public static String EVENT_LastHero_msgStart;
	@ConfigField(config = "events", fieldName = "LastHero_msgInv", value = "Вы хотите принять участие в эвенте?")
	public static String EVENT_LastHero_msgInv;
	@ConfigField(config = "events", fieldName = "LastHero_msgMiss", value = "Пропустившие момент регистрации, могут зарегистрироваться у Arena Manager на MDT.")
	public static String EVENT_LastHero_msgMiss;
	@ConfigField(config = "events", fieldName = "LastHero_msgStopEv", value = "Эвент отменен, слишком мало участников.")
	public static String EVENT_LastHero_msgStopEv;
	@ConfigField(config = "events", fieldName = "LastHero_msgEndEv", value = "Эвент 'Last Hero' завершен")
	public static String EVENT_LastHero_msgEndEv;
	@ConfigField(config = "events", fieldName = "LastHero_msgNoWIn", value = "Эвент закончился вничью")
	public static String EVENT_LastHero_msgNoWIn;
	@ConfigField(config = "events", fieldName = "LastHero_msgDie", value = "Вас убили. Ждите окончания эвента")
	public static String EVENT_LastHero_msgDie;
	@ConfigField(config = "events", fieldName = "LastHero_msgTP", value = "Телепортация всех игроков обратно...")
	public static String EVENT_LastHero_msgTP;
	@ConfigField(config = "events", fieldName = "LastHero_msgPrep", value = "Боевая готовность...!")
	public static String EVENT_LastHero_msgPrep;
	@ConfigField(config = "events", fieldName = "LastHero_msgFight", value = ">>> FIGHT!!! <<<")
	public static String EVENT_LastHero_msgFight;
	@ConfigField(config = "events", fieldName = "LastHero_sortBylvl", value = "false")
	public static boolean EVENT_LastHero_sortBylvl;
	@ConfigField(config = "events", fieldName = "LastHero_dispel", value = "false")
	public static boolean EVENT_LastHero_dispel;

	@ConfigField(config = "events", value="false")
	public static boolean CAPTURE_ENABLED;
	public static Crontab CAPTURE_CRON;
	@ConfigField(config = "events", value="0")
	public static int CAPTURE_TICKETS;
	@ConfigField(config = "events", value="5")
	public static int CAPTURE_TICKET_PER_PLAYER;
	@ConfigField(config = "events", value="0")
	public static int CAPTURE_EVENT_TIME;
	@ConfigField(config = "events", value="10")
	public static int CAPTURE_REGISTRATION_TIME;
	@ConfigField(config = "events", value="18")
	public static int CAPTURE_MIN_PARTICIPANTS;
	@ConfigField(config = "events", value="54")
	public static int CAPTURE_MAX_PARTICIPANTS;
	@ConfigField(config = "events", value="1")
	public static int CAPTURE_PLAYER_MIN_LEVEL;
	@ConfigField(config = "events", value="99")
	public static int CAPTURE_PLAYER_MAX_LEVEL;
	@ConfigField(config = "events", value="100")
	public static int CAPTURE_POINTS_KILL;
	@ConfigField(config = "events", value="100")
	public static int CAPTURE_POINTS_RESURRECT;
	@ConfigField(config = "events", value="250")
	public static int CAPTURE_POINTS_FLAG_CAPTURE;
	@ConfigField(config = "events", value="150")
	public static int CAPTURE_POINTS_FLAG_ATTACK;
	@ConfigField(config = "events", value="500")
	public static int CAPTURE_POINTS_WIN;
	@ConfigField(config = "events", value="20")
	public static int CAPTURE_RESURRECT_DELAY;
	@ConfigField(config = "events", value="false")
	public static boolean CAPTURE_HWID_CHECK;
	@ConfigField(config = "events", value="0.01")
	public static double CAPTURE_HEAL_RATE;
	@ConfigField(config = "events", value="14351")
	public static int CAPTURE_EXCHANGE_ITEM_ID;
	@ConfigField(config = "events", value="0.01")
	public static double CAPTURE_EXCHANGE_RATE;

	@ConfigField(config = "events", fieldName = "TFH_POLLEN_CHANCE", value = "5")
	public static int TFH_POLLEN_CHANCE;
	@ConfigField(config = "events", fieldName = "MEDAL_CHANCE", value = "10")
	public static int GLIT_MEDAL_CHANCE;
	@ConfigField(config = "events", fieldName = "GLITTMEDAL_CHANCE", value = "5")
	public static int GLIT_GLITTMEDAL_CHANCE;
	@ConfigField(config = "events", fieldName = "GLIT_MIN_MOB_LEVEL", value = "10")
	public static int GLIT_MIN_MOB_LEVEL;
	@ConfigField(config = "events", fieldName = "GLIT_MAX_LEVEL_DIFF", value = "3")
	public static int GLIT_MAX_LEVEL_DIFF;
	@ConfigField(config = "events", fieldName = "GLIT_EnableRate", value = "true")
	public static boolean GLIT_EnableRate;
	@ConfigField(config = "events", fieldName = "MOSStaffPrice", value = "1000")
	public static long EVENT_MOS_STAFF_PRICE;
	@ConfigField(config = "events", fieldName = "MOSScrollPrice", value = "77777")
	public static long EVENT_MOS_SCROLL_PRICE;
	@ConfigField(config = "events", fieldName = "MOSScroll24Price", value = "6000")
	public static long EVENT_MOS_SCROLL24_PRICE;
	@ConfigField(config = "events", fieldName = "MOSMobMinLevel", value = "20")
	public static int EVENT_MOS_MOB_MIN;
	@ConfigField(config = "events", fieldName = "MOSScrollDropChance", value = "1")
	public static float EVENT_MOS_SCROLL_DROP_CHANCE;
	public static ItemData[] EVENT_XMAS_GIFT_ITEMS;
	public static long EVENT_XMAS_DROP_TIME;
	public static long EVENT_XMAS_DROP_TIME_RAND;

	/**
	 * vitality.properties fields
	 */
	@ConfigField(config = "vitality", value = "false")
	public static boolean VIT_DEBUG;
	@ConfigField(config = "vitality", value = "1.")
	public static double VIT_RATE;
	@ConfigField(config = "vitality", value = "5000")
	public static int VIT_RECOVERY_TIME;
	@ConfigField(config = "vitality", value = "4.")
	public static double VIT_ONLINE_RECOVERY_RATE;
	@ConfigField(config = "vitality", value = "20000")
	public static int VIT_MAX_POINTS;
	@ConfigField(config = "vitality", fieldName = "VitPlayerMaxLevel", value = "1")
	public static byte VIT_MAX_PLAYER_LVL;
	@ConfigField(config = "vitality", value = "false")
	public static boolean VIT_CHECK_LUCKY_SKILL;
	public static int[] VIT_PER_LVL;
	public static double[] VIT_RATE_LVL;

	/**
	 * boss.properties fields
	 */
	@ConfigField(config = "boss", fieldName = "FixIntervalOfAntharas", value = "11520")
	public static int FWA_FIXINTERVALOFANTHARAS;
	@ConfigField(config = "boss", fieldName = "RandomIntervalOfAntharas", value = "8640")
	public static int FWA_RANDOMINTERVALOFANTHARAS;
	@ConfigField(config = "boss", fieldName = "AppTimeOfAntharas", value = "10")
	public static int FWA_APPTIMEOFANTHARAS;
	@ConfigField(config = "boss", fieldName = "ActivityTimeOfAntharas", value = "120")
	public static int FWA_ACTIVITYTIMEOFANTHARAS;
	@ConfigField(config = "boss", fieldName = "OldAntharas", value = "false")
	public static boolean FWA_OLDANTHARAS;
	@ConfigField(config = "boss", fieldName = "LimitOfWeak", value = "299")
	public static int FWA_LIMITOFWEAK;
	@ConfigField(config = "boss", fieldName = "LimitOfNormal", value = "399")
	public static int FWA_LIMITOFNORMAL;
	@ConfigField(config = "boss", fieldName = "IntervalOfBehemothOnWeak", value = "8")
	public static int FWA_INTERVALOFBEHEMOTHONWEAK;
	@ConfigField(config = "boss", fieldName = "IntervalOfBehemothOnNormal", value = "5")
	public static int FWA_INTERVALOFBEHEMOTHONNORMAL;
	@ConfigField(config = "boss", fieldName = "IntervalOfBehemothOnStrong", value = "3")
	public static int FWA_INTERVALOFBEHEMOTHONSTRONG;
	@ConfigField(config = "boss", fieldName = "IntervalOfBomberOnWeak", value = "6")
	public static int FWA_INTERVALOFBOMBERONWEAK;
	@ConfigField(config = "boss", fieldName = "IntervalOfBomberOnNormal", value = "4")
	public static int FWA_INTERVALOFBOMBERONNORMAL;
	@ConfigField(config = "boss", fieldName = "IntervalOfBomberOnStrong", value = "3")
	public static int FWA_INTERVALOFBOMBERONSTRONG;
	@ConfigField(config = "boss", fieldName = "MoveAtRandom", value = "true")
	public static boolean FWA_MOVEATRANDOM;
	@ConfigField(config = "boss", fieldName = "FixIntervalOfValakas", value = "11520")
	public static int FWV_FIXINTERVALOFVALAKAS;
	@ConfigField(config = "boss", fieldName = "RandomIntervalOfValakas", value = "8640")
	public static int FWV_RANDOMINTERVALOFVALAKAS;
	@ConfigField(config = "boss", fieldName = "AppTimeOfValakas", value = "10")
	public static int FWV_APPTIMEOFVALAKAS;
	@ConfigField(config = "boss", fieldName = "ActivityTimeOfValakas", value = "120")
	public static int FWV_ACTIVITYTIMEOFVALAKAS;
	@ConfigField(config = "boss", fieldName = "MoveAtRandom", value = "true")
	public static boolean FWV_MOVEATRANDOM;
	@ConfigField(config = "boss", fieldName = "FixIntervalOfBaium", value = "7200")
	public static int FWB_FIXINTERVALOFBAIUM;
	@ConfigField(config = "boss", fieldName = "RandomIntervalOfBaium", value = "480")
	public static int FWB_RANDOMINTERVALOFBAIUM;

	/**
	 * clanhall.properties fields
	 */
	@ConfigField(config = "clanhall", fieldName = "ClanHallRentRate", value = "false")
	public static boolean CH_RENT_RATE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallTeleportFunctionFeeRation", value = "86400000")
	public static long CH_TELE_FEE_RATIO;
	@ConfigField(config = "clanhall", fieldName = "ClanHallTeleportFunctionFeeLvl1", value = "86400000")
	public static int CH_TELE1_FEE;
	@ConfigField(config = "clanhall", value = "1")
	public static int RENT_VALUE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallAuctionCencelPenalty", value = "7")
	public static int CH_AUCTION_CANCEL_PENALTY;
	@ConfigField(config = "clanhall", fieldName = "ClanHallTeleportFunctionFeeLvl2", value = "86400000")
	public static int CH_TELE2_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallTeleportFunctionFeeLvl3", value = "86400000")
	public static int CH_TELE3_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallSupportFunctionFeeRation", value = "86400000")
	public static long CH_SUPPORT_FEE_RATIO;
	@ConfigField(config = "clanhall", fieldName = "ClanHallSupportFeeLvl1", value = "86400000")
	public static int CH_SUPPORT1_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallSupportFeeLvl2", value = "86400000")
	public static int CH_SUPPORT2_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallSupportFeeLvl3", value = "86400000")
	public static int CH_SUPPORT3_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallSupportFeeLvl4", value = "86400000")
	public static int CH_SUPPORT4_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallSupportFeeLvl5", value = "86400000")
	public static int CH_SUPPORT5_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallSupportFeeLvl7", value = "86400000")
	public static int CH_SUPPORT7_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallSupportFeeLvl8", value = "86400000")
	public static int CH_SUPPORT8_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallMpRegenerationFunctionFeeRation", value = "86400000")
	public static long CH_MPREG_FEE_RATIO;
	@ConfigField(config = "clanhall", fieldName = "ClanHallMpRegenerationFeeLvl1", value = "86400000")
	public static int CH_MPREG1_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallMpRegenerationFeeLvl2", value = "86400000")
	public static int CH_MPREG2_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallMpRegenerationFeeLvl3", value = "86400000")
	public static int CH_MPREG3_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallMpRegenerationFeeLvl4", value = "86400000")
	public static int CH_MPREG4_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallMpRegenerationFeeLvl5", value = "86400000")
	public static int CH_MPREG5_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallMpRegenerationFeeLvl6", value = "86400000")
	public static int CH_MPREG6_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallHpRegenerationFunctionFeeRation", value = "86400000")
	public static long CH_HPREG_FEE_RATIO;
	@ConfigField(config = "clanhall", fieldName = "ClanHallHpRegenerationFeeLvl1", value = "86400000")
	public static int CH_HPREG1_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallHpRegenerationFeeLvl2", value = "86400000")
	public static int CH_HPREG2_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallHpRegenerationFeeLvl3", value = "86400000")
	public static int CH_HPREG3_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallHpRegenerationFeeLvl4", value = "86400000")
	public static int CH_HPREG4_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallHpRegenerationFeeLvl5", value = "86400000")
	public static int CH_HPREG5_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallHpRegenerationFeeLvl6", value = "86400000")
	public static int CH_HPREG6_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallHpRegenerationFeeLvl7", value = "86400000")
	public static int CH_HPREG7_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallHpRegenerationFeeLvl8", value = "86400000")
	public static int CH_HPREG8_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallHpRegenerationFeeLvl9", value = "86400000")
	public static int CH_HPREG9_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallHpRegenerationFeeLvl10", value = "86400000")
	public static int CH_HPREG10_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallHpRegenerationFeeLvl11", value = "86400000")
	public static int CH_HPREG11_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallHpRegenerationFeeLvl12", value = "86400000")
	public static int CH_HPREG12_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallExpRegenerationFunctionFeeRation", value = "86400000")
	public static long CH_EXPREG_FEE_RATIO;
	@ConfigField(config = "clanhall", fieldName = "ClanHallExpRegenerationFeeLvl1", value = "86400000")
	public static int CH_EXPREG1_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallExpRegenerationFeeLvl2", value = "86400000")
	public static int CH_EXPREG2_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallExpRegenerationFeeLvl3", value = "86400000")
	public static int CH_EXPREG3_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallExpRegenerationFeeLvl4", value = "86400000")
	public static int CH_EXPREG4_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallExpRegenerationFeeLvl5", value = "86400000")
	public static int CH_EXPREG5_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallExpRegenerationFeeLvl6", value = "86400000")
	public static int CH_EXPREG6_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallExpRegenerationFeeLvl7", value = "86400000")
	public static int CH_EXPREG7_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallExpRegenerationFeeLvl8", value = "86400000")
	public static int CH_EXPREG8_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallItemCreationFunctionFeeRation", value = "86400000")
	public static long CH_ITEM_FEE_RATIO;
	@ConfigField(config = "clanhall", fieldName = "ClanHallItemCreationFunctionFeeLvl1", value = "86400000")
	public static int CH_ITEM1_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallItemCreationFunctionFeeLvl2", value = "86400000")
	public static int CH_ITEM2_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallItemCreationFunctionFeeLvl3", value = "86400000")
	public static int CH_ITEM3_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallCurtainFunctionFeeRation", value = "86400000")
	public static long CH_CURTAIN_FEE_RATIO;
	@ConfigField(config = "clanhall", fieldName = "ClanHallCurtainFunctionFeeLvl1", value = "86400000")
	public static int CH_CURTAIN1_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallCurtainFunctionFeeLvl2", value = "86400000")
	public static int CH_CURTAIN2_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallFrontPlatformFunctionFeeRation", value = "86400000")
	public static long CH_PLATFORM_FEE_RATIO;
	@ConfigField(config = "clanhall", fieldName = "ClanHallFrontPlatformFunctionFeeLvl1", value = "86400000")
	public static int CH_PLATFORM1_FEE;
	@ConfigField(config = "clanhall", fieldName = "ClanHallFrontPlatformFunctionFeeLvl2", value = "86400000")
	public static int CH_PLATFORM2_FEE;

	/**
	 * Geodata config
	 */
	@ConfigField(config = "geodata", fieldName = "GeoFirstX", value = "11")
	public static int GEO_X_FIRST;
	@ConfigField(config = "geodata", fieldName = "GeoFirstY", value = "10")
	public static int GEO_Y_FIRST;
	@ConfigField(config = "geodata", fieldName = "GeoLastX", value = "26")
	public static int GEO_X_LAST;
	@ConfigField(config = "geodata", fieldName = "GeoLastY", value = "26")
	public static int GEO_Y_LAST;
	@ConfigField(config = "geodata", value = "true")
	public static boolean GEODATA_ENABLED;
	@ConfigField(config = "geodata", value = "false")
	public static boolean GEODATA_DEBUG;
	@ConfigField(config = "geodata", value = "false")
	public static boolean PATHFIND_DEBUG;
	@ConfigField(config = "geodata", fieldName = "GeoFilesPattern", value = "(\\d{2})_(\\d{2})_conv\\.dat")
	public static String GEOFILES_PATTERN;
	@ConfigField(config = "geodata", fieldName = "GeoFilesFormat", value = "1")
	public static int GEOFILES_FORMAT;
	@ConfigField(config = "geodata", value = "false")
	public static boolean ALLOW_DOORS;
	@ConfigField(config = "geodata", value = "true")
	public static boolean PATH_CLEAN;
	@ConfigField(config = "geodata", value = "64")
	public static int MAX_Z_DIFF;
	@ConfigField(config = "geodata", value = "64")
	public static int MIN_LAYER_HEIGHT;
	@ConfigField(config = "geodata", value = "20_21;22_16;22_25;23_18;25_14;25_15;25_19;24_23")
	public static String VERTICAL_SPLIT_REGIONS;

	public static void otherCustom(ExProperties properties)
	{
		ITEM_LINK_SHOW_TIME *= 60000;
		RESPAWN_RESTORE_CP /= 100;
		RESPAWN_RESTORE_HP /= 100;
		RESPAWN_RESTORE_MP /= 100;
		String customItems = properties.getProperty("EnchantCustomItems", null);
		if(StringUtils.isNotBlank(customItems))
		{
			for(String itemInfo : customItems.split(";"))
			{
				String[] info = itemInfo.split(",");
				if(info.length > 3)
				{
					try
					{
						int itemId = Integer.parseInt(info[0]);
						StatsSet statsSet = new StatsSet();
						statsSet.set("chance", info[1]);
						statsSet.set("safe", info[2]);
						statsSet.set("bless_safe", info[3]);
						AbstractEnchantPacket.customItems.put(itemId, statsSet);
					}
					catch (Exception e)
					{
						_log.error("other.properties: EnchantCustomItems parse error: " + e);
					}
				}
			}
		}
	}

	public static void vitalityCustom(ExProperties properties)
	{
		VIT_PER_LVL = new int[5];
		VIT_RATE_LVL = new double[5];
		if(VIT_DEBUG)
		{
			_log.info("VitDebug: true");
			_log.info("VitRate: " + VIT_RATE);
			_log.info("VitRecoveryTime: " + VIT_RECOVERY_TIME);
			_log.info("VitOnlineRecoveryRate: " + VIT_ONLINE_RECOVERY_RATE);
			_log.info("VitMaxPoints: " + VIT_MAX_POINTS);
		}

		for(int i = 0; i < 5; i++)
		{
			VIT_PER_LVL[i] = properties.getIntProperty("VitPerLvl" + i, 0);
			VIT_RATE_LVL[i] = properties.getDoubleProperty("VitRateLvl" + i, (i - i / 2));
		}
	}

	public static void bossCustom(ExProperties properties)
	{
		//antharas
		if(FWA_FIXINTERVALOFANTHARAS < 5 || FWA_FIXINTERVALOFANTHARAS > 20160)
			FWA_FIXINTERVALOFANTHARAS = 11520;
		FWA_FIXINTERVALOFANTHARAS = FWA_FIXINTERVALOFANTHARAS * 60000;
		if(FWA_RANDOMINTERVALOFANTHARAS < 5 || FWA_RANDOMINTERVALOFANTHARAS > 20160)
			FWA_RANDOMINTERVALOFANTHARAS = 8640;
		FWA_RANDOMINTERVALOFANTHARAS = FWA_RANDOMINTERVALOFANTHARAS * 60000;
		if(FWA_APPTIMEOFANTHARAS < 5 || FWA_APPTIMEOFANTHARAS > 60)
			FWA_APPTIMEOFANTHARAS = 10;
		FWA_APPTIMEOFANTHARAS = FWA_APPTIMEOFANTHARAS * 60000;
		if(FWA_ACTIVITYTIMEOFANTHARAS < 120 || FWA_ACTIVITYTIMEOFANTHARAS > 720)
			FWA_ACTIVITYTIMEOFANTHARAS = 120;
		FWA_ACTIVITYTIMEOFANTHARAS = FWA_ACTIVITYTIMEOFANTHARAS * 60000;
		if(FWA_LIMITOFWEAK >= FWA_LIMITOFNORMAL)
			FWA_LIMITOFNORMAL = FWA_LIMITOFWEAK + 1;
		if(FWA_INTERVALOFBEHEMOTHONWEAK < 1 || FWA_INTERVALOFBEHEMOTHONWEAK > 10)
			FWA_INTERVALOFBEHEMOTHONWEAK = 8;
		FWA_INTERVALOFBEHEMOTHONWEAK = FWA_INTERVALOFBEHEMOTHONWEAK * 60000;
		if(FWA_INTERVALOFBEHEMOTHONNORMAL < 1 || FWA_INTERVALOFBEHEMOTHONNORMAL > 10)
			FWA_INTERVALOFBEHEMOTHONNORMAL = 5;
		FWA_INTERVALOFBEHEMOTHONNORMAL = FWA_INTERVALOFBEHEMOTHONNORMAL * 60000;
		if(FWA_INTERVALOFBEHEMOTHONSTRONG < 1 || FWA_INTERVALOFBEHEMOTHONSTRONG > 10)
			FWA_INTERVALOFBEHEMOTHONSTRONG = 3;
		FWA_INTERVALOFBEHEMOTHONSTRONG = FWA_INTERVALOFBEHEMOTHONSTRONG * 60000;
		if(FWA_INTERVALOFBOMBERONWEAK < 1 || FWA_INTERVALOFBOMBERONWEAK > 10)
			FWA_INTERVALOFBOMBERONWEAK = 6;
		FWA_INTERVALOFBOMBERONWEAK = FWA_INTERVALOFBOMBERONWEAK * 60000;
		if(FWA_INTERVALOFBOMBERONNORMAL < 1 || FWA_INTERVALOFBOMBERONNORMAL > 10)
			FWA_INTERVALOFBOMBERONNORMAL = 4;
		FWA_INTERVALOFBOMBERONNORMAL = FWA_INTERVALOFBOMBERONNORMAL * 60000;
		if(FWA_INTERVALOFBOMBERONSTRONG < 1 || FWA_INTERVALOFBOMBERONSTRONG > 10)
			FWA_INTERVALOFBOMBERONSTRONG = 3;
		FWA_INTERVALOFBOMBERONSTRONG = FWA_INTERVALOFBOMBERONSTRONG * 60000;

		//valakas
		if(FWV_FIXINTERVALOFVALAKAS < 5 || FWV_FIXINTERVALOFVALAKAS > 20160) FWV_FIXINTERVALOFVALAKAS = 11520;
		FWV_FIXINTERVALOFVALAKAS = FWV_FIXINTERVALOFVALAKAS * 60000;
		if(FWV_RANDOMINTERVALOFVALAKAS < 5 || FWV_RANDOMINTERVALOFVALAKAS > 20160)
			FWV_RANDOMINTERVALOFVALAKAS = 8640;
		FWV_RANDOMINTERVALOFVALAKAS = FWV_RANDOMINTERVALOFVALAKAS * 60000;
		if(FWV_APPTIMEOFVALAKAS < 5 || FWV_APPTIMEOFVALAKAS > 60) FWV_APPTIMEOFVALAKAS = 10;
		FWV_APPTIMEOFVALAKAS = FWV_APPTIMEOFVALAKAS * 60000;
		if(FWV_ACTIVITYTIMEOFVALAKAS < 120 || FWV_ACTIVITYTIMEOFVALAKAS > 720) FWV_ACTIVITYTIMEOFVALAKAS = 120;
		FWV_ACTIVITYTIMEOFVALAKAS = FWV_ACTIVITYTIMEOFVALAKAS * 60000;

		//Baium
		if(FWB_FIXINTERVALOFBAIUM < 5 || FWB_FIXINTERVALOFBAIUM > 20160) FWB_FIXINTERVALOFBAIUM = 7200;
		FWB_FIXINTERVALOFBAIUM *= 60000;
		if(FWB_RANDOMINTERVALOFBAIUM < 5 || FWB_RANDOMINTERVALOFBAIUM > 20160)
			FWB_RANDOMINTERVALOFBAIUM = 480;
		FWB_RANDOMINTERVALOFBAIUM *= 60000;
	}

	public static void serverCustom(ExProperties properties) throws Exception
	{
		AUTODESTROY_ITEM_AFTER *= 1000L;
		DATAPACK_ROOT = new File(properties.getProperty("DatapackRoot", ".")).getCanonicalFile();
		if(MIN_PROTOCOL_REVISION > MAX_PROTOCOL_REVISION)
			throw new Error("MinProtocolRevision is bigger than MaxProtocolRevision in server configuration file.");

		MAT_BAN_COUNT_CHANNELS = 1;
		for(String id : BAN_CHANNEL.split(","))
		{
			BAN_CHANNEL_LIST[MAT_BAN_COUNT_CHANNELS] = Integer.parseInt(id);
			MAT_BAN_COUNT_CHANNELS++;
		}
		INDECENT_BLOCK_COUNT_CHANNELS = 1;
		for(String id : INDECENT_BLOCK_CHANNEL.split(","))
		{
			INDECENT_CHANNEL_LIST[INDECENT_BLOCK_COUNT_CHANNELS] = Integer.parseInt(id);
			INDECENT_BLOCK_COUNT_CHANNELS++;
		}

		if(RESTART_AT_TIME > 24)
			RESTART_AT_TIME = 24;

		int[] list = properties.getIntArrayProperty("DisableCreateItems", "");
		if(list.length > 0)
			for(int id : list)
				DISABLE_CREATION_ID_LIST.add(id);

		if(PLAYER_VISIBILITY > 7000 || PLAYER_VISIBILITY < 1500)
			PLAYER_VISIBILITY = 4500;
		if(PLAYER_VISIBILITY_Z > 3000 || PLAYER_VISIBILITY_Z < 500)
			PLAYER_VISIBILITY_Z = 2000;
	}

	public static void clanhallCustom(ExProperties properties)
	{
		CH_AUCTION_CANCEL_PENALTY *= 24 * 60 * 60000;
	}

	public static void altsettingsCustom(ExProperties properties)
	{
		String[] list = properties.getArrayProperty("UseShotsEffectList", "i_p_attack;i_p_soul_attack;i_m_attack;i_mp_burn;i_hp_drain;i_fatal_blow;i_soul_blow;i_backstab;i_death_link;i_p_attack_hp_link;i_energy_attack;i_seven_arrows");
		USE_SHOTS_EFFECT_LIST = new GArray<>();
		for(String effect : list)
			if(!effect.isEmpty())
				USE_SHOTS_EFFECT_LIST.add(effect);

		ALT_HERB_LIFE_TIME *= 1000;
		ALT_MAX_LEVEL = Math.min(ALT_MAX_LEVEL, Experience.LEVEL.length - 1);
		ALT_MAX_SUB_LEVEL = Math.min(ALT_MAX_SUB_LEVEL, Experience.LEVEL.length - 1);
		RIFT_AUTO_JUMPS_TIME *= 60000;
		ALT_FLOATING_RATE_EXTRA_TIME *= 24 * 3600;
	}

	public static void servicesCustom(ExProperties properties)
	{
		String allowClassMasters = properties.getProperty("AllowClassMasters", "0");
		if(allowClassMasters.length() != 0 && !allowClassMasters.equals("0"))
			for(String id : allowClassMasters.split(","))
				ALLOW_CLASS_MASTERS_LIST.add(Integer.parseInt(id));

		String classMastersPrice = properties.getProperty("ClassMastersPrice", "0,0,0");
		if(classMastersPrice.length() >= 5)
		{
			int level = 1;
			for(String id : classMastersPrice.split(","))
			{
				CLASS_MASTERS_PRICE_LIST[level] = Integer.parseInt(id);
				level++;
			}
		}

		PREMIUM_PRICES = new GArray<>(1);

		String[] prices = properties.getArrayProperty("PremiumPrice", "4037;5;30;4037;3;15;4037;1;1");
		for(int i = 0; i < prices.length; i += 3)
			PREMIUM_PRICES.add(new int[]{Integer.parseInt(prices[i]), Integer.parseInt(prices[i + 1]), Integer.parseInt(prices[i + 2])});

		prices = properties.getArrayProperty("PremiumSexChangePrice", "4037;20");

		PREMIUM_SEX_CHANGE_PRICE = new int[prices.length];
		for(int i = 0; i < prices.length; i++)
			PREMIUM_SEX_CHANGE_PRICE[i] = Integer.parseInt(prices[i]);

		TM_PERIOD *= 1000;
	}

	public static void pvpCustom(ExProperties properties)
	{
		String[] list = properties.getArrayProperty("ListOfNonDroppableItems", "57,1147,425,1146,461,10,2368,7,6,2370,2369,3500,3501,3502,4422,4423,4424,2375,6648,6649,6650,6842,6834,6835,6836,6837,6838,6839,6840,5575,7694,6841,8181");
		KARMA_LIST_NONDROPPABLE_ITEMS = new ArrayList<Integer>();
		for(String id : list)
			KARMA_LIST_NONDROPPABLE_ITEMS.add(Integer.parseInt(id));
	}

	public static void eventsCustom(ExProperties properties)
	{
		int tvtCount = properties.getIntProperty("TvT_EventCount", 1);
		EVENT_TvT_Config = new StatsSet[tvtCount];

		for(int i = 0; i < tvtCount; i++)
		{
			StatsSet tvt = new StatsSet();
			String prefix = "TvT" + (i + 1);
			tvt.set("Crontab", properties.getProperty(prefix + "_Crontab", "30 */3 * * *"));
			tvt.set("MinLevel", properties.getIntProperty(prefix + "_MinLevel", 1));
			tvt.set("MaxLevel", properties.getIntProperty(prefix + "_MaxLevel", 85));
			tvt.set("MinParticipants", properties.getIntProperty(prefix + "_MinParticipants", 10));
			tvt.set("RewardItemId", properties.getIntProperty(prefix + "_RewardItemId", 57));
			tvt.set("RewardItemCount", properties.getLongProperty(prefix + "_RewardItemCount", 50000));
			tvt.set("RewardTopPlayerItemId", properties.getIntProperty(prefix + "_RewardTopPlayerItemId", 57));
			tvt.set("RewardTopPlayerCount", properties.getLongProperty(prefix + "_RewardTopPlayerCount", 500000));
			tvt.set("StartAnnounce", properties.getProperty(prefix + "_StartAnnounce", "Регистрация на TvT начата, уровни с 1 по 85."));
			tvt.set("RegistrationAnnounce", properties.getProperty(prefix + "_RegistrationAnnounce", "Окончание TvT регистрации через MIN минут (уровни 1-85)."));
			tvt.set("RegistrationEndAnnounce", properties.getProperty(prefix + "_RegistrationEndAnnounce", "Регистрация окончена уровни 1-85."));
			tvt.set("Dispel", properties.getBooleanProperty(prefix + "_Dispel", false));
			tvt.set("RuleAnnounce1", properties.getProperty(prefix + "_RuleAnnounce1", "Эвент длится 10 минут."));
			tvt.set("RuleAnnounce2", properties.getProperty(prefix + "_RuleAnnounce2", "Побеждает комманда больше всех убившая противников."));
			tvt.set("RuleAnnounce3", properties.getProperty(prefix + "_RuleAnnounce3", "Каждый игрок победившей комманды получает 50000 аден."));
			tvt.set("RuleAnnounce4", properties.getProperty(prefix + "_RuleAnnounce4", "Игрок убивший больше всех противников получит 500000 аден."));
			tvt.set("RuleAnnounce5", properties.getProperty(prefix + "_RuleAnnounce5", "Старт эвента через 2 минуты."));
			tvt.set("NoParticipantsAnnounce", properties.getProperty(prefix + "_NoParticipantsAnnounce", "TvT отменен, уровни с 1 по 85."));
			tvt.set("WinnerTeamAnnounce", properties.getProperty(prefix + "_WinnerTeamAnnounce", "COLOR комманда победила убив KILLS противников, уровни с 1 по 85."));
			tvt.set("NoWinnerTeamAnnounce", properties.getProperty(prefix + "_NoWinnerTeamAnnounce", "TvT закончилось в ничью, уровни с 1 по 85."));
			tvt.set("TopPlayerAnnounce", properties.getProperty(prefix + "_TopPlayerAnnounce", "NAME стал чемпионом TvT, убив KILLS игроков, COLOR комманда."));
			EVENT_TvT_Config[i] = tvt;
		}
		EVENT_LastHero_cron = new Crontab(properties.getProperty("LastHero_cron", "0 */3 * * *"));
		CAPTURE_CRON = new Crontab(properties.getProperty("CaptureCron", "10 */3 * * *"));

		EVENT_XMAS_GIFT_ITEMS = ItemData.parseItem(properties.getProperty("XmasGiftItems", "959,1;10;960,2;90"));
		EVENT_XMAS_DROP_TIME = properties.getIntProperty("XmasGiftDropTime", 3600) * 1000L;
		EVENT_XMAS_DROP_TIME_RAND = properties.getIntProperty("XmasGiftDropTimeRand", 1800) * 1000L;
	}

	private static void loadHexConfig(Properties _settings) throws Exception
	{
		HEX_ID = new BigInteger(_settings.getProperty("HexID"), 16).toByteArray();
	}

	public static void load()
	{
		_log.info("Loading gameserver config.");
		loadConfig(Config.class, "server");
		loadConfig(Config.class, "geodata");
		loadConfig(Config.class, "telnet");
		loadConfig(Config.class, "clanhall");
		loadConfig(Config.class, "other");
		loadConfig(Config.class, "spoil");
		loadConfig(Config.class, "altsettings");
		loadConfig(Config.class, "services");
		loadConfig(Config.class, "pvp");
		loadConfig(Config.class, "ai");
		loadConfig(Config.class, "events");
		loadConfig(Config.class, "vitality");
		loadConfig(Config.class, "boss");

		// hexid
		try
		{
			Properties Settings = new Properties();
			InputStream is = new FileInputStream(HEXID_FILE);
			Settings.load(is);
			is.close();
			loadHexConfig(Settings);
		}
		catch(Exception e)
		{
		}

		abuseLoad();
		loadGMAccess();
		if(ADVIPSYSTEM)
			ipsLoad();
	}

	public static boolean reload(String what)
	{
		_log.info(what + " need reload");
		if(what.equalsIgnoreCase("hexid"))
		{
			// hexid
			try
			{
				Properties Settings = new Properties();
				InputStream is = new FileInputStream(HEXID_FILE);
				Settings.load(is);
				is.close();
				loadHexConfig(Settings);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				throw new Error("Failed to Load " + HEXID_FILE + " File.");
			}
		}
		else if(what.equalsIgnoreCase("abuse"))
			abuseLoad();
		else if(what.equalsIgnoreCase("gmaccess"))
		{
			loadGMAccess();
		}
		else
		{
			loadConfig(Config.class, what);
		}

		return true;
	}

	private Config()
	{
	}

	public static void abuseLoad()
	{
		OBSCENE_LIST = new ArrayList<>();
		LineNumberReader lnr = null;
		try
		{
			int i = 0;
			String line;

			lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(OBSCENE_CONFIG_FILE), "UTF-8"));

			while((line = lnr.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(line, "\n\r");
				if(st.hasMoreTokens())
				{
					String Mat = st.nextToken();
					OBSCENE_LIST.add(Mat);

					i++;
				}
			}

			_log.info("Obscene: Loaded " + i + " abuse words.");

		}
		catch(IOException e1)
		{
			_log.warn("Error reading abuse: " + e1);
		}
		finally
		{
			try
			{
				if(lnr != null)
					lnr.close();
			}
			catch(Exception e2)
			{
				// nothing
			}
		}

		INDECENT_LIST = new ArrayList<>();
		lnr = null;
		try
		{
			int i = 0;
			String line;

			lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(INDECENT_CONFIG_FILE), "UTF-8"));

			while((line = lnr.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(line, "\n\r");
				if(st.hasMoreTokens())
				{
					String Mat = st.nextToken();
					INDECENT_LIST.add(Mat);
					i++;
				}
			}

			_log.info("Indecent: Loaded " + i + " abuse words.");

		}
		catch(IOException e1)
		{
			_log.warn("Error reading abuse: " + e1);
		}
		finally
		{
			try
			{
				if(lnr != null)
					lnr.close();
			}
			catch(Exception e2)
			{
				// nothing
			}
		}

		SHOUT_LIST = new ArrayList<>();
		lnr = null;
		try
		{
			int i = 0;
			String line;

			lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(SHOUT_CONFIG_FILE), "UTF-8"));

			while((line = lnr.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(line, "\n\r");
				if(st.hasMoreTokens())
				{
					String Mat = st.nextToken();
					SHOUT_LIST.add(Mat);
					i++;
				}
			}

			_log.info("Shout: Loaded " + i + " abuse words.");

		}
		catch(IOException e1)
		{
			_log.warn("Error reading abuse: " + e1);
		}
		finally
		{
			try
			{
				if(lnr != null)
					lnr.close();
			}
			catch(Exception e2)
			{
				// nothing
			}
		}

		PRIVATE_LIST = new ArrayList<>();
		lnr = null;
		try
		{
			String line;

			lnr = new LineNumberReader(new InputStreamReader(new FileInputStream(PRIVATE_CONFIG_FILE), "UTF-8"));

			while((line = lnr.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(line, "\n\r");
				if(st.hasMoreTokens())
				{
					String Mat = st.nextToken();
					_log.info("Private: Loaded " + Mat + " abuse words.");
					PRIVATE_LIST.add(Mat);
				}
			}

			_log.info("Private: Loaded " + PRIVATE_LIST.size() + " abuse words.");
		}
		catch(IOException e1)
		{
			_log.warn("Error reading private abuse: " + e1);
		}
		finally
		{
			try
			{
				if(lnr != null)
					lnr.close();
			}
			catch(Exception e2)
			{
				// nothing
			}
		}
	}

	private static void ipsLoad()
	{
		try
		{
			Properties ipsSettings = new Properties();
			InputStream is = new FileInputStream(new File(ADV_IP_FILE));
			ipsSettings.load(is);
			is.close();

			String NetMask;
			String ip;
			for(int i = 0; i < ipsSettings.size() / 2; i++)
			{
				NetMask = ipsSettings.getProperty("NetMask" + (i + 1));
				ip = ipsSettings.getProperty("IPAdress" + (i + 1));
				for(String mask : NetMask.split(","))
				{
					AdvIP advip = new AdvIP();
					advip.ipadress = ip;
					advip.ipmask = mask.split("/")[0];
					advip.bitmask = mask.split("/")[1];
					GAMEIPS.add(advip);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new Error("Failed to Load " + ADV_IP_FILE + " File.");
		}
	}

	public static void loadGMAccess()
	{
		AdminTemplateManager.reload();
	}
}