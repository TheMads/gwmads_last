package ru.l2gw.gameserver.model.instances;

import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.arrays.GCSArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.extensions.scripts.Events;
import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.controllers.TradeController;
import ru.l2gw.gameserver.ai.CtrlEvent;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.ai.L2CharacterAI;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.instancemanager.*;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.base.ClassId;
import ru.l2gw.gameserver.model.entity.Castle;
import ru.l2gw.gameserver.model.entity.ClanHall;
import ru.l2gw.gameserver.model.entity.SevenSigns;
import ru.l2gw.gameserver.model.entity.instance.Instance;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.model.npcmaker.DefaultMaker;
import ru.l2gw.gameserver.model.npcmaker.SpawnDefine;
import ru.l2gw.gameserver.model.quest.Quest;
import ru.l2gw.gameserver.model.quest.QuestState;
import ru.l2gw.gameserver.model.zone.L2Zone.ZoneType;
import ru.l2gw.gameserver.model.NpcTradeList;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.skills.funcs.FuncEnchantNpc;
import ru.l2gw.gameserver.tables.*;
import ru.l2gw.gameserver.taskmanager.DecayTaskManager;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.gameserver.templates.L2Weapon;
import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.util.*;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class L2NpcInstance extends L2Character
{
	public int i_ai0;
	public int i_ai1;
	public int i_ai2;
	public int i_ai3;
	public int i_ai4;
	public int i_ai5;
	public int i_ai6;
	public int i_ai7;
	public int i_ai8;
	public int i_ai9;
	public int i_quest0;
	public int i_quest1;
	public int i_quest2;
	public int i_quest3;
	public int i_quest4;
	public int i_quest5;
	public int i_quest6;
	public int i_quest7;
	public int i_quest8;
	public long c_ai0;
	public long c_ai1;
	public long c_ai2;
	public long param1;
	public long param2;
	public long param3;
	public long l_ai0;
	public long l_ai1;
	public long l_ai2;
	public long l_ai3;
	public long l_ai4;
	public long l_ai5;
	public int weight_point;
	public AtomicInteger av_quest0 = new AtomicInteger();

	private long lastFactionNotifyTime = 0;
	public int minFactionNotifyInterval = 1000;
	private int _damageReceived = 0;
	private int _personalAggroRange = -1;

	private int _currentLHandId;
	private int _currentRHandId;

	private float _growCollisionRadius;
	private float _growCollisionHeight;
	protected StatsSet _thisParams = null;

	/**
	 * Нужно для отображения анимайии спауна, используется в пакете NpcInfo *
	 */
	protected boolean _showSpawnAnimation = true;
	public boolean hasChatWindow = true;

	protected static int Cond_All_False = 0;
	protected static int Cond_Busy_Because_Of_Siege = 1;
	protected static int Cond_Clan = 2;
	protected static int Cond_Owner = 4;
	protected boolean _ignoreClanHelp;
	protected ConcurrentHashMap<Integer, AggroInfo> _aggroList;
	private boolean _randomWalk;
	private int _noAggroRange;
	private int _ssChance;
	private int _spiritCharged;
	private boolean _soulCharged;
	private int _fortressId = -1;
	private int _clanHallId = -1;
	private boolean _isRbMinion = false;
	private boolean _randomAnimation;
	private boolean _talkAnimation;
	protected final int _territoryId;
	protected boolean _showHp;
	private boolean _showTag;
	public boolean targetable;
	public int show_name_tag;
	private boolean _gatekeeper;
	private final int _baseAtkRange;
	private int _displayId = 0;
	private int _state = 0;
	protected GCSArray<Long> neighbors;
	private long lastNeighborsClean;

	protected final MinionList minionList;
	protected GArray<L2MinionData> minionData;
	protected L2NpcInstance _master;
	protected L2MinionData _minionData;
	protected ScheduledFuture<?> minionMaintainTask;

	protected static final int MONSTER_MAINTENANCE_INTERVAL = 1000;
	protected long spawnTime;
	protected boolean abilityItemDrop = true;
	protected double hpRegen;
	protected int weaponEnchant;
	private int team;

	public L2NpcInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template);

		if(template == null)
		{
			_log.warn("No template for Npc. Please check your datapack is setup correctly.");
			throw new IllegalArgumentException();
		}

		minionList = new MinionList(this);

		if(bossIndex > 0)
			_master = L2ObjectsStorage.getAsNpc(bossIndex);

		param1 = p1;
		param2 = p2;
		param3 = p3;

		setName(template.name);
		setTitle(template.title);

		String implementationName = template.ai_type;

		try
		{
			if(!implementationName.equalsIgnoreCase("npc"))
				_ai_constructor = Class.forName("ru.l2gw.gameserver.ai." + implementationName).getConstructors()[0];
		}
		catch(Exception e)
		{
			try
			{
				_ai_constructor = Scripts.getInstance().getClasses().get("ai." + implementationName).getRawClass().getConstructors()[0];
			}
			catch(Exception e1)
			{
				_log.warn("AI type " + template.ai_type + " not found! Npc id: " + getNpcId() + " use npc.");
				//e1.printStackTrace();
			}
		}

		initCharStatusUpdateValues();

		// Т.к. у нас не создаются инстансы при каждом спавне, то все ок.
		if(hasRandomAnimation() && !(this instanceof L2MonsterInstance)) // для монстров через AI
			startRandomAnimation();

		// инициализация параметров оружия
		_currentLHandId = getTemplate().lhand;
		_currentRHandId = getTemplate().rhand;
		// инициализация коллизий
		_growCollisionHeight = getAIParams() != null ? getAIParams().getFloat("growH", getTemplate().collisionHeight) : getTemplate().collisionHeight;
		_growCollisionRadius = getAIParams() != null ? getAIParams().getFloat("growR", getTemplate().collisionRadius) : getTemplate().collisionRadius;

		_ignoreClanHelp = getAIParams() != null && getAIParams().getBool("ignore_clan_help", false);
		_randomWalk = getAIParams() == null || getAIParams().getBool("random_walk", true) && getAIParams().getInteger("[MoveArounding]", 1) > 0;
		_noAggroRange = getAIParams() != null ? getAIParams().getInteger("no_aggro_range", 0) : 0;
		_ssChance = getAIParams() != null ? getAIParams().getInteger("ss_probability", 10) : 10;
		_clanHallId = getAIParams() != null ? getAIParams().getInteger("clan_hall_id", -1) : -1;
		_isRbMinion = getAIParams() != null && getAIParams().getBool("rb_minion", false);
		_randomAnimation = getAIParams() == null || getAIParams().getBool("random_animation", true);
		_talkAnimation = getAIParams() == null || getAIParams().getBool("talk_animation", true);
		_territoryId = getAIParams() != null ? getAIParams().getInteger("territory", 0) : 0;
		_showHp = getAIParams() != null && getAIParams().getBool("show_hp", false);
		_showTag = _territoryId > 0 && getAIParams() != null && getAIParams().getBool("show_tag", false);
		_isInvul = getAIParams() != null && getAIParams().getBool("is_invul", false);
		_baseAtkRange = _template.baseAtkRange;
		_flying = getTemplate().flying == 1;
		_gatekeeper = "Gatekeeper".equals(getTemplate().title);
		targetable = getTemplate().targetable == 1;
		show_name_tag = getTemplate().show_name_tag;
		hasChatWindow = getAIParams() == null || getAIParams().getBool("chat_window", true);
		hpRegen = getTemplate().baseHpReg;
	}

	public void callFriends(L2Character attacker, int damage)
	{
		if(isMonster())
		{
			L2NpcInstance master = this;
			if(isMinion())
			{
				master = getLeader();
				if(!master.isInCombat() && !master.isDead())
				//{
					master.getAI().notifyEvent(CtrlEvent.EVT_PARTY_ATTACKED, attacker, this, damage);
				master.callMinionsToAssist(attacker, this, damage);
				//}
			}
			else
				master.callMinionsToAssist(attacker, this, damage);
		}

		// call friend's
		if(getFactionId() != null && !getFactionId().isEmpty() && System.currentTimeMillis() - lastFactionNotifyTime > getMinFactionNotifyInterval())
		{
			lastFactionNotifyTime = System.currentTimeMillis();
			_damageReceived = 0;
			ThreadPoolManager.getInstance().scheduleAi(new L2ObjectTasks.NotifyFaction(this, attacker, _damageReceived > 0 ? _damageReceived : damage), 100, false);
		}
		else
			_damageReceived += damage;
	}

	public Location getMinionPosition()
	{
		return minionList.getMinionPosition();
	}

	protected int getMinFactionNotifyInterval()
	{
		return minFactionNotifyInterval;
	}

	public GArray<L2NpcInstance> getAroundFriends()
	{
		return getAroundFriends(getFactionRange());
	}

	public GArray<L2NpcInstance> getAroundFriends(int radius)
	{
		GArray<L2NpcInstance> friends = new GArray<L2NpcInstance>();
		for(L2NpcInstance npc : getKnownNpc(radius, 350))
			if(npc != null && getFactionId().equals(npc.getFactionId()) && !npc.isDead())
				friends.add(npc);
		return friends;
	}

	protected static final org.apache.commons.logging.Log _log = LogFactory.getLog(L2NpcInstance.class.getName());

	/**
	 * The delay after witch the attacked is stopped
	 */
	private long _attack_timeout;
	private Location _spawnedLoc = new Location(0, 0, 0);

	private Constructor<?> _ai_constructor;

	@Override
	public synchronized L2CharacterAI getAI()
	{
		if(_ai == null)
		{
			if(_ai_constructor != null)
				try
				{
					_ai = (L2CharacterAI) _ai_constructor.newInstance(this);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			if(_ai == null)
				_ai = new L2CharacterAI(this);

			NpcTable.setAIParams(this, _ai);
		}
		return _ai;
	}

	public void setAIConstructor(Constructor<?> constructor)
	{
		_ai_constructor = constructor;
	}

	public Constructor<?> getAIConstructor()
	{
		return _ai_constructor;
	}

	public void setAttackTimeout(long time)
	{
		_attack_timeout = time;
	}

	public long getAttackTimeout()
	{
		return _attack_timeout;
	}

	/**
	 * Return the position of the spawned point.<BR><BR>
	 */
	public Location getSpawnedLoc()
	{
		return _spawnedLoc;
	}

	public void setSpawnedLoc(Location loc)
	{
		_spawnedLoc = loc;
	}

	public int getRightHandItem()
	{
		return _currentRHandId;
	}

	public int getLeftHandItem()
	{
		return _currentLHandId;
	}

	public void setLHandId(int newWeaponId)
	{
		_currentLHandId = newWeaponId;
	}

	private int basePAtk, baseMAtk;
	public void setRHandId(int newWeaponId)
	{
		_currentRHandId = newWeaponId;
	}

	public void equipItem(int itemId)
	{
		L2Item item = ItemTable.getInstance().getTemplate(itemId);
		if(item != null)
		{
			switch(item.getBodyPart())
			{
				case L2Item.SLOT_R_HAND:
					_currentRHandId = itemId;
					break;
				case L2Item.SLOT_L_HAND:
					_currentLHandId = itemId;
					break;
				case L2Item.SLOT_LR_HAND:
					_currentRHandId = itemId;
					_currentLHandId = itemId;
					break;
			}
		}
		else
			_currentRHandId = itemId;
		updateAbnormalEffect();
	}

	/**
	 * @return Returns the zOffset.
	 */
	public float getCollisionHeight()
	{
		return getTemplate().collisionHeight;
	}

	public float getGrowCollisionHeight()
	{
		return _growCollisionHeight;
	}

	/**
	 * @return Returns the collisionRadius.
	 */
	public float getCollisionRadius()
	{
		return getTemplate().collisionRadius;
	}

	public float getGrowCollisionRadius()
	{
		return _growCollisionRadius;
	}

	/**
	 * Kill the L2NpcInstance (the corpse disappeared after 7 seconds), distribute rewards (EXP, SP, Drops...) and notify Quest Engine.<BR><BR>
	 */
	@Override
	public void doDie(L2Character killer)
	{
		if(_master != null && _minionData != null)
			_master.getMinionList().notifyDead(this);
		else
			minionList.notifyDead(this);

		if(!_isDecayed)
			DecayTaskManager.getInstance().addDecayTask(this);

		// установка параметров оружия и коллизий по умолчанию
		_currentLHandId = getTemplate().lhand;
		_currentRHandId = getTemplate().rhand;
		_growCollisionHeight = getTemplate().collisionHeight;
		_growCollisionRadius = getTemplate().collisionRadius;

		super.doDie(killer);
		notifyClanDead();
	}

	public void onClanAttacked(L2NpcInstance attacked_member, L2Character attacker, int damage)
	{
		if(!getAI().isGlobalAggro() || getTemplate().ignoreClanList != null && getTemplate().ignoreClanList.contains(Integer.toString(attacked_member.getNpcId())))
			return;

		getAI().notifyEvent(CtrlEvent.EVT_CLAN_ATTACKED, attacked_member, attacker, damage);
	}

	public class AggroInfo
	{
		public long hate;
		public long damage;
		public int level;
		public int objectId;
		private WeakReference<L2Character> attacker;

		public AggroInfo(L2Character attacker)
		{
			objectId = attacker.getObjectId();
			level = attacker.getLevel();
			this.attacker = new WeakReference<L2Character>(attacker);
		}

		public L2Character getAttacker()
		{
			return attacker.get();
		}

		public void setAttacker(L2Character attacker)
		{
			this.attacker = new WeakReference<L2Character>(attacker);
		}

		@Override
		public String toString()
		{
			return getAttacker() + " ai[hate=" + hate + ";dmge=" + damage + ";lvl=" + level + ";objId=" + objectId + "]";
		}
	}

	public ConcurrentHashMap<Integer, AggroInfo> getAggroList()
	{
		if(_aggroList == null)
			_aggroList = new ConcurrentHashMap<Integer, AggroInfo>();

		return _aggroList;
	}

	public long getHate(L2Character cha)
	{
		AggroInfo ai = getAggroList().get(cha.getObjectId()); 
		if(ai != null)
			return ai.hate;
		return 0;
	}

	public void addDamage(L2Character attacker, long damage)
	{
		addDamageHate(attacker, damage, damage);
	}

	public void addDamageHate(L2Character attacker, long damage, long aggro)
	{
		if(damage > 0 && aggro == 0)
			aggro = damage;

		if(attacker == null || attacker == this)
			return;

		if(((isRaid() || _isRbMinion) && attacker.getLevel() > getLevel() + Config.RAID_MAX_LEVEL_DIFF) || (isRaidMinion() && attacker.getLevel() > getLevel() + Config.RAID_MAX_LEVEL_DIFF))
			aggro = 0;

		AggroInfo ai = getAggroList().get(attacker.getObjectId());

		if(ai != null)
		{
			ai.damage += damage;
			ai.hate += aggro;
			ai.level = attacker.getLevel();
			if(ai.getAttacker() != attacker)
				ai.setAttacker(attacker);
			if(ai.hate < 0)
				ai.hate = 0;
		}
		else if(damage > 0 || aggro > 0)
		{
			ai = new AggroInfo(attacker);
			ai.damage = damage;
			ai.hate = aggro;
			getAggroList().put(attacker.getObjectId(), ai);
		}
	}

	public void removeAllHateInfoIF(int cond, int value)
	{
		ConcurrentHashMap<Integer, AggroInfo> aggroList = getAggroList();
		synchronized(aggroList)
		{
			if(cond == 0) // COND_ALL
			{
				aggroList.clear();
			}
			else if(cond == 1) // COND_IS_INVALID
			{
				for(AggroInfo ai : aggroList.values())
					if(ai != null && ai.getAttacker() == null)
						aggroList.remove(ai.objectId);
			}
			else if(cond == 2) // COND_HAS_HATE_LESS_THAN
			{
				for(AggroInfo ai : aggroList.values())
					if(ai != null && ai.hate < value)
						aggroList.remove(ai.objectId);
			}
			else if(cond == 3) // COND_IS_FAR_AWAY
			{
				for(AggroInfo ai : aggroList.values())
					if(ai != null)
					{
						L2Character attacker = ai.getAttacker();
						if(attacker == null || !isInRange(attacker, value))
							aggroList.remove(ai.objectId);
					}
			}
		}
	}

	public AggroInfo getRandomHateInfo()
	{
		ConcurrentHashMap<Integer, AggroInfo> aggroList = getAggroList();
		synchronized(aggroList)
		{
			if(aggroList.size() > 0)
			{
				int r = Rnd.get(aggroList.size());
				int c = 0;
				for(AggroInfo ai : aggroList.values())
				{
					if(c == r)
						return ai;
					c++;
				}
			}
		}

		return null;
	}

	public void stopHate(L2Character attacker)
	{
		AggroInfo ai = getAggroList().get(attacker.getObjectId());
		if(ai != null)
			ai.hate = 0;
		if(getAI().getAttackTarget() == attacker)
			getAI().setAttackTarget(null);
	}

	public void stopHate()
	{
		if(_aggroList != null)
			synchronized(_aggroList)
			{
				for(AggroInfo ai : _aggroList.values())
					if(ai != null)
						ai.hate = 0;
			}
	}

	public int getAggroListSize()
	{
		return getAggroList().size();
	}

	public void clearAggroList()
	{
		if(_aggroList != null)
			_aggroList.clear();
	}

	public L2Character getMostHated()
	{
		L2Character target = getAI().getAttackTarget();
		if(target != null && target.isNpc() && target.isVisible() && !target.isDead() && target.isInRange(this, 2000) && isConfused())
			return target;

		AggroInfo mosthated = null;

		for(AggroInfo ai : getAggroList().values())
		{
			if(ai == null)
				continue;

			if(ai.hate <= 0 && ai.damage == 0)
			{
				getAggroList().remove(ai.objectId);
				continue;
			}

			if(ai.hate <= 0)
				continue;

			L2Character cha = ai.getAttacker();

			if(cha == null || cha.isAlikeDead() || !cha.isVisible() || !knowsObject(cha))
			{
				ai.hate = 0;
				if(ai.damage == 0)
					getAggroList().remove(ai.objectId);
				continue;
			}

			if(mosthated == null)
				mosthated = ai;
			else if(mosthated.hate < ai.hate)
				mosthated = ai;
		}

		return mosthated != null && mosthated.hate > 0 ? mosthated.getAttacker() : null;
	}

	public void dropItem(L2Character killer, int itemId, long itemCount)
	{
		if(itemCount == 0 || killer == null)
			return;

		L2Player lastAttacker = killer.getPlayer();
		if(lastAttacker == null)
			return;

		L2ItemInstance item;

		for(long i = 1; i <= itemCount; i++)
		{
			item = ItemTable.getInstance().createItem("Loot", itemId, itemCount, lastAttacker, this);

			// Set the Item quantity dropped if L2ItemInstance is stackable
			if(item.isStackable())
				i = itemCount; // Set so loop won't happent again

			if(Config.DEBUG)
				_log.debug("Item id to drop: " + itemId + " amount: " + item.getCount());

			if(isRaid())
			{
				SystemMessage sm = new SystemMessage(SystemMessage.S1_DIED_AND_DROPPED_S3_S2);
				sm.addString(getName());
				sm.addItemName(itemId);
				sm.addNumber(item.getCount());
				broadcastPacket(sm);
			}

			// Check if the autoLoot mode is active
			if(isFlying() || !isRaid() && (Config.AUTO_LOOT || (lastAttacker.isAutoLoot() && (!lastAttacker.isInParty() || lastAttacker.getParty().isAutoLoot()))) && isInRange(lastAttacker, 1500))
				if(!Config.AUTO_LOOT_HERBS && item.isHerb())
					item.dropToTheGround(lastAttacker, this);
				else
					lastAttacker.doAutoLoot(item, this);
			else
				item.dropToTheGround(lastAttacker, this);
		}
	}

	public void dropItem(int itemId, long itemCount)
	{
		if(itemCount == 0)
			return;

		L2ItemInstance item;

		for(long i = 1; i <= itemCount; i++)
		{
			item = ItemTable.getInstance().createItem("Loot", itemId, itemCount, null, this);

			// Set the Item quantity dropped if L2ItemInstance is stackable
			if(item.isStackable())
				i = itemCount; // Set so loop won't happent again

			if(Config.DEBUG)
				_log.debug("Item id to drop: " + itemId + " amount: " + item.getCount());

			// Check if the autoLoot mode is active
			item.dropToTheGround(this, GeoEngine.findPointToStay(getX(), getY(), getZ(), 20, 50, getReflection()));
		}
	}

	public void dropItem(L2Player lastAttacker, L2ItemInstance item)
	{
		if(item.getCount() == 0)
			return;

		if(Config.DEBUG)
			_log.debug("Item id to drop: " + item + " amount: " + item.getCount());

		if(isRaid())
		{
			final SystemMessage sm = new SystemMessage(SystemMessage.S1_DIED_AND_DROPPED_S3_S2);
			sm.addString(getName());
			sm.addItemName(item.getItemId());
			sm.addNumber(item.getCount());
			broadcastPacket(sm);
		}

		// Check if the autoLoot mode is active
		if(isFlying() || !isRaid() && (Config.AUTO_LOOT || (lastAttacker.isAutoLoot() && (!lastAttacker.isInParty() || lastAttacker.getParty().isAutoLoot()))) && isInRange(lastAttacker, 1500))
			lastAttacker.doAutoLoot(item, this); // Give this or these Item(s) to the L2Player that has killed the L2NpcInstance
		else
			item.dropToTheGround(lastAttacker, this);
	}

	@Override
	public boolean isAttackable(L2Character attacker, boolean forceUse, boolean sendMessage)
	{
		return getTemplate().can_be_attacked == 1 || forceUse;
	}

	@Override
	public void onSpawn()
	{
		hpRegen = getTemplate().baseHpReg;
		spawnTime = System.currentTimeMillis();
		setDecayed(false);
		abilityItemDrop = true;
		_showSpawnAnimation = false;
		revalidateZones(true);
		if(spawnDefine != null)
			spawnDefine.getMaker().onNpcCreated(this);
		weaponEnchant = 0;
		removeStatsOwner(this);
		getAI().notifyEvent(CtrlEvent.EVT_SPAWN);
	}

	@Override
	public L2NpcTemplate getTemplate()
	{
		return (L2NpcTemplate) _template;
	}

	@Override
	public int getNpcId()
	{
		return getTemplate().npcId;
	}

	/**
	 * @return true if the L2NpcInstance is aggressive (ex : L2MonsterInstance in function of aggroRange).<BR><BR>
	 */
	public boolean isAggressive()
	{
		return !(_noAggroRange > 0 && getSpawnedLoc() != null && getDistance(getSpawnedLoc().getX(), getSpawnedLoc().getY(), getSpawnedLoc().getZ()) > _noAggroRange) && getAggroRange() > 0 && (isChampion() == 0 || Config.ALT_CHAMPION_AGGRO);
	}

	public int getAggroRange()
	{
		if(isStatActive(Stats.PASSIVE))
			return 0;

		if(_personalAggroRange >= 0)
			return _personalAggroRange;

		return getTemplate().aggroRange;
	}

	/**
	 * Устанавливает данному npc новый aggroRange.
	 * Если установленый aggroRange < 0, то будет братся аггрорейндж с темплейта.
	 *
	 * @param aggroRange новый agrroRange
	 */
	public void setAggroRange(int aggroRange)
	{
		_personalAggroRange = aggroRange;
	}

	public int getFactionRange()
	{
		return getTemplate().factionRange;
	}

	public final String getFactionId()
	{
		return getTemplate().factionId;
	}

	public long getExpReward()
	{
		return (long) calcStat(Stats.EXP_SP, getTemplate().revardExp * Config.RATE_XP, null, null);
	}

	public long getSpReward()
	{
		return (long) calcStat(Stats.EXP_SP, getTemplate().revardSp * Config.RATE_SP, null, null);
	}

	/**
	 * Remove PROPERLY the L2NpcInstance from the world.<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Remove the L2NpcInstance from the world and update its spawn object </li>
	 * <li>Remove L2Object object from _allObjects of L2World </li><BR><BR>
	 * <p/>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players</B></FONT><BR><BR>
	 */
	public void deleteMe()
	{
		//if(hasMinions())
		//	removeMinions();
		super.deleteMe();
		detachAI();
		stopAllEffects();
		if(!_isDecayed)
			DecayTaskManager.getInstance().cancelDecayTask(this);
	}

	private L2Spawn _spawn;

	public L2Spawn getSpawn()
	{
		return _spawn;
	}

	public void setSpawn(L2Spawn spawn)
	{
		_spawn = spawn;
	}

	private SpawnDefine spawnDefine;

	public SpawnDefine getSpawnDefine()
	{
		return spawnDefine;
	}

	public void setSpawnDefine(SpawnDefine sd)
	{
		spawnDefine = sd;
	}

	public DefaultMaker getMyMaker()
	{
		if(spawnDefine != null)
			return spawnDefine.getMaker();
		if(getLeader() != null)
			return getLeader().getMyMaker();
		return null;
	}

	@Override
	public void decayMe()
	{
		if(_master != null && _minionData != null)
			_master.getMinionList().removeSpawnedMinion(this);

		super.decayMe();
	}

	@Override
	public void spawnMe()
	{
		if(_master != null && _minionData != null)
			_master.notifyMinionSpawned(this);

		super.spawnMe();
	}

	@Override
	public synchronized void onDecay()
	{
		if(isDecayed())
			return;

		setDecayed(true);

		// Remove the L2NpcInstance from the world when the decay task is launched
		super.onDecay();

		// Decrease its spawn counter
		if(_spawn != null)
			_spawn.decreaseCount(this);
		else if(spawnDefine != null)
		{
			if(spawnDefine.getMaker().atomicDecrease(spawnDefine, 1))
				spawnDefine.getMaker().onNpcDeleted(this);
			else
				_log.warn(spawnDefine + " error decreasing npc count!");

			if(spawnDefine.getMaker().npc_count == 0)
				spawnDefine.getMaker().onAllNpcDeleted();
		}
		else if(_master != null && _minionData != null)
		{
			if(_master.getSpawnDefine() != null)
			{
				if(!_master.getSpawnDefine().getMaker().atomicDecrease(1))
					_log.warn(_master.getSpawnDefine() + " error decreasing minion npc count!");

				if(_master.getSpawnDefine().getMaker().npc_count == 0)
					_master.getSpawnDefine().getMaker().onAllNpcDeleted();
			}
			deleteMe();
		}
		else
			deleteMe();

		Quest[] quests = getTemplate().getEventQuests(Quest.QuestEventType.ON_DECAY);
		if(quests != null)
			for(Quest q : quests)
				if(q != null)
					q.notifyDecayd(this);
	}

	private boolean _isDecayed = false;

	/**
	 * Set the decayed state of this L2NpcInstance<BR><BR>
	 */
	public final void setDecayed(boolean mode)
	{
		_isDecayed = mode;
	}

	/**
	 * Return the decayed status of this L2NpcInstance<BR><BR>
	 */
	public final boolean isDecayed()
	{
		return _isDecayed;
	}

	public void endDecayTask()
	{
		if(!_isDecayed)
			DecayTaskManager.getInstance().cancelDecayTask(this);
		onDecay();
	}

	/**
	 * Return True if this L2NpcInstance is undead in function of the L2NpcTemplate.<BR><BR>
	 */
	@Override
	public boolean isUndead()
	{
		return getTemplate().isUndead();
	}

	/**
	 * Return the Level of this L2NpcInstance contained in the L2NpcTemplate.<BR><BR>
	 */
	@Override
	public byte getLevel()
	{
		return getTemplate().level;
	}

	/**
	 * Return null (regular NPCs don't have weapons instancies).<BR><BR>
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		// regular NPCs dont have weapons instancies
		return null;
	}

	/**
	 * Return the weapon item equipped in the right hand of the L2NpcInstance or null.<BR><BR>
	 */
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		if(_currentRHandId < 1)
			return null;

		// Get the weapon item equipped in the right hand of the L2NpcInstance
		L2Item item = ItemTable.getInstance().getTemplate(_currentRHandId);

		if(!(item instanceof L2Weapon))
			return null;

		return (L2Weapon) item;
	}

	/**
	 * Return null (regular NPCs don't have weapons instances).<BR><BR>
	 */
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		// regular NPCs dont have weapons instances
		return null;
	}

	/**
	 * Return the weapon item equipped in the left hand of the L2NpcInstance or null.<BR><BR>
	 */
	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		// Get the weapon identifier equipped in the right hand of the L2NpcInstance
		int weaponId = getTemplate().lhand;

		if(weaponId < 1)
			return null;

		// Get the weapon item equipped in the right hand of the L2NpcInstance
		L2Item item = ItemTable.getInstance().getTemplate(getTemplate().lhand);

		if(!(item instanceof L2Weapon))
			return null;

		return (L2Weapon) item;
	}

	/**
	 * Send a packet NpcInfo with state of abnormal effect to all visible L2Player<BR><BR>
	 */
	@Override
	public void updateAbnormalEffect()
	{
		if(isHide())
		{
			DeleteObject deleteObject = new DeleteObject(this);
			for(L2Player _cha : L2World.getAroundPlayers(this))
				_cha.sendPacket(deleteObject);
		}
		else
			for(L2Player _cha : L2World.getAroundPlayers(this))
				_cha.sendPacket(new NpcInfo(this, _cha));
	}

	// У NPC всегда 2
	public void onRandomAnimation()
	{
		broadcastPacket(new SocialAction(getObjectId(), 2));
	}

	public void startRandomAnimation()
	{
		int interval = 1000 * Rnd.get(Config.MIN_NPC_ANIMATION, Config.MAX_NPC_ANIMATION);
		ThreadPoolManager.getInstance().scheduleGeneral(new RandomAnimationTask(), interval);
	}

	public class RandomAnimationTask implements Runnable
	{
		private final int interval;

		public RandomAnimationTask()
		{
			interval = 1000 * Rnd.get(Config.MIN_NPC_ANIMATION, Config.MAX_NPC_ANIMATION);
		}

		public void run()
		{
			if(!isDead() && !isMoving)
				onRandomAnimation();

			ThreadPoolManager.getInstance().scheduleGeneral(this, interval);
		}
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return _randomAnimation && Config.MAX_NPC_ANIMATION > 0;
	}

	public boolean hasRandomWalk()
	{
		return _randomWalk;
	}

	/**
	 * @param type -1=anyNear,0=clanHall, 1=fort, 2=castle,
	 * @return for -1 never null
	 */
	public SiegeUnit getBuilding(int type)
	{
		SiegeUnit unit = null;
		int tempId = -1;
		if(type == -1)
		{
			if(_clanHallId > 0)
				unit = ResidenceManager.getInstance().getBuildingById(_clanHallId);
			else if(_fortressId > 0)
				unit = ResidenceManager.getInstance().getBuildingById(_fortressId);
			else
			{
				tempId = ResidenceManager.getInstance().findNearestClanHallIndex(getX(), getY(), 1500);
				if(tempId < 0)
					tempId = ResidenceManager.getInstance().findNearestFortressIndex(getX(), getY(), 10000);
				if(tempId > 0)
				{
					unit = ResidenceManager.getInstance().getBuildingById(tempId);
					if(unit.isClanHall)
						_clanHallId = unit.getId();
					if(unit.isFort)
						_fortressId = unit.getId();
				}
				else
					unit = TownManager.getInstance().getBuildingByObject(this).getCastle();
			}
		}
		else
		{
			if(type != 2)
			{
				if(_clanHallId > 0 && type == 0)
					unit = ResidenceManager.getInstance().getBuildingById(_clanHallId);
				else if(_fortressId > 0 && type == 1)
					unit = ResidenceManager.getInstance().getBuildingById(_fortressId);
				else
				{
					tempId = type == 1 ? ResidenceManager.getInstance().findNearestFortressIndex(getX(), getY(), 10000) : ResidenceManager.getInstance().findNearestClanHallIndex(getX(), getY(), 1500);
					if(tempId > 0)
						unit = ResidenceManager.getInstance().getBuildingById(tempId);
					if(unit != null)
					{
						if(unit.isClanHall)
							_clanHallId = unit.getId();
						if(unit.isFort)
							_fortressId = unit.getId();
					}
				}
			}
			else
				unit = TownManager.getInstance().getBuildingByObject(this).getCastleLikeUnit();
		}
		return unit;
	}

	public ClanHall getClanHall()
	{
		ClanHall hall = null;
		if(_clanHallId <= 0)
			_clanHallId = ResidenceManager.getInstance().findNearestClanHallIndex(getX(), getY(), 1500);
		if(_clanHallId > 0)
			hall = ResidenceManager.getInstance().getClanHallById(_clanHallId);

		return hall;
	}

	public Castle getCastle()
	{
		return TownManager.getInstance().getBuildingByObject(this).getCastle();
	}

	protected long _lastSocialAction;

	@Override
	public void onAction(L2Player player, boolean dontMove)
	{
		if(!targetable)
		{
			player.sendActionFailed();
			return;
		}

		if(this != player.getTarget())
		{
			if(Config.DEBUG)
				_log.debug("new target selected:" + getObjectId());
			if(player.setTarget(this))
			{
				player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
				if(isAttackable(player, false, false) || _showHp || isMonster())
				{
					StatusUpdate su = new StatusUpdate(getObjectId());
					su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
					su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
					player.sendPacket(su);
				}
				player.sendPacket(new ValidateLocation(this));
				player.sendActionFailed();
			}
			return;
		}

		player.getAI().stopFollow();

		if(!dontMove && Events.onAction(player, this))
			return;
		else if(dontMove && Events.onActionShift(player, this))
			return;

		if(isAttackable(player, false, false))
		{
			player.getAI().Attack(this, false, dontMove);
			return;
		}

		if(!isInRange(player, getInteractDistance(player)))
		{
			if(player.getAI().getIntention() != CtrlIntention.AI_INTENTION_INTERACT && !dontMove)
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this, null);
			else
				player.sendActionFailed();
			return;
		}

		if(getAI() != null && getAI().onTalk(player))
		{
			player.sendActionFailed();
			player.setHeading(player.calcHeading(getX(), getY()));
			if(!isMoving && !player.isCastingNow())
				player.sendPacket(new MoveToPawn(player, this, 100));
			return;
		}

		if(!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && player.getKarma() > 0 && !player.isGM() && !(this instanceof L2ClanBaseManagerInstance) ||
			_gatekeeper && (player.getEffectBySkillId(6201) != null || player.getEffectBySkillId(6202) != null || player.getEffectBySkillId(6203) != null))
		{
			player.sendActionFailed();
			return;
		}

		// С NPC нельзя разговаривать мертвым и сидя
		if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
			return;

		if(_talkAnimation && System.currentTimeMillis() - _lastSocialAction > 10000)
		{
			broadcastPacket(new SocialAction(getObjectId(), 2));
			_lastSocialAction = System.currentTimeMillis();
		}

		player.sendActionFailed();

		if(hasChatWindow)
		{
			if(_isBusy)
				showBusyWindow(player);
			else
			{
				Quest[] qlst = getTemplate().getEventQuests(Quest.QuestEventType.NPC_FIRST_TALK);
				if(qlst == null || qlst.length == 0 || player.getQuestState(qlst[0].getName()) == null || player.getQuestState(qlst[0].getName()).isCompleted() || !qlst[0].notifyFirstTalk(this, player))
				{
					player.setHeading(player.calcHeading(getX(), getY()));
					if(!isMoving && !player.isCastingNow())
						player.sendPacket(new MoveToPawn(player, this, 100));
					showChatWindow(player, 0);
					if (Config.NPC_DIALOG_PLAYER_DELAY > 0) {
						player.setNpcDialogEndTime((int)(System.currentTimeMillis() / 1000L) + Config.NPC_DIALOG_PLAYER_DELAY);
					}
				}
			}
		}
	}

	public void showQuestWindow(L2Player player, String questId)
	{
		if(!player.isQuestContinuationPossible())
			return;

		try
		{
			// Get the state of the selected quest
			QuestState qs = player.getQuestState(questId);
			if(qs != null)
			{
				if(qs.getQuest().notifyTalk(this, qs))
					return;
			}
			else
			{
				Quest q = QuestManager.getQuest(questId);
				if(q != null)
				{
					if(!q.isCustom() && player.getQuestCount() >= Config.ALT_MAX_QUESTS)
					{
						NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
						html.setFile("data/html/fullquest.htm");
						player.sendPacket(html);
						player.sendActionFailed();
						return;
					}

					// check for start point
					Quest[] qlst = getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);
					if(qlst != null && qlst.length > 0)
						for(Quest element : qlst)
							if(element == q)
							{
								if(q.notifyTalk(this, q.newQuestState(player)))
									return;
								break;
							}
				}
			}

			NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
			html.setFile("data/html/no-quest.htm");
			player.sendPacket(html);
		}
		catch(Exception e)
		{
			_log.warn("problem with npc text " + e);
			e.printStackTrace();
		}

		player.sendActionFailed();
	}

	public void showBuyWindow(L2Player player, int val)
	{
		if(AdminTemplateManager.checkBoolean("noShop", player))
			return;

		double taxRate = 0;

		if(!Config.SERVICES_OFFSHORE_NO_CASTLE_TAX && !isInZone(ZoneType.offshore))
			taxRate = getCastle().getTaxRate();

		player.tempInvetoryDisable();
		if(Config.DEBUG)
			_log.debug("Showing buylist");
		NpcTradeList list = TradeController.getInstance().getSellList(val);
		if(list != null && list.getNpcId() == getNpcId())
		{
			player.sendPacket(new ExBuyList(list, player, taxRate));
			player.sendPacket(new ExSellRefundList(player));
		}
		else
			_log.warn(player.getName() + " wrong sell list: " + list + " id: " + val + " for npc: " + this + " player loc: " + player.getLoc());
	}

	/**
	 * Open a quest or chat window on client with the text of the L2NpcInstance in function of the command.<BR><BR>
	 * <p/>
	 * <B><U> Example of use </U> :</B><BR><BR>
	 * <li> Client packet : RequestBypassToServer</li><BR><BR>
	 *
	 * @param command The command string received from client
	 */
	public void onBypassFeedback(L2Player player, String command)
	{
		if(!isInRange(player, getInteractDistance(player)))
		{
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			player.sendActionFailed();
		}
		else
			try
			{
				if(command.equalsIgnoreCase("TerritoryStatus"))
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile("data/html/merchant/territorystatus.htm");
					html.replace("%npcname%", getName());

					if(getBuilding(2).getId() > 0)
					{
						html.replace("%castlename%", getBuilding(2).getName());
						html.replace("%taxpercent%", String.valueOf(getCastle().getTaxPercent()));

						switch(getBuilding(2).getId())
						{
							case 1:
							case 2:
							case 3:
							case 4:
							case 5:
							case 6:
								html.replace("%kingdom%", "Aden");
								break;
							case 7:
							case 8:
							case 9:
								html.replace("%kingdom%", "Elmore");
								break;
						}

						if(getBuilding(2).getOwnerId() > 0)
						{
							L2Clan clan = ClanTable.getInstance().getClan(getBuilding(2).getOwnerId());
							if(clan != null)
							{
								html.replace("%clanname%", clan.getName());
								html.replace("%clanleadername%", clan.getLeaderName());
							}
							else
							{
								html.replace("%clanname%", "unexistant clan");
								html.replace("%clanleadername%", "None");
							}
						}
						else
						{
							html.replace("%clanname%", "NPC");
							html.replace("%clanleadername%", "None");
						}
					}
					else
					{
						html.replace("%castlename%", "Open");
						html.replace("%taxpercent%", "0");

						html.replace("%clanname%", "No");
						html.replace("%clanleadername%", getName());
					}

					player.sendPacket(html);
				}
				else if(command.equalsIgnoreCase("Exchange"))
				{
					if(player.getLevel() > 25)
					{
						NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
						html.setFile("data/html/merchant/exchange-level25.htm");
						html.replace("%npcname%", getName());
						player.sendPacket(html);
					}
					else
					{
						NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
						html.setFile("data/html/merchant/exchange.htm");
						html.replace("%npcname%", getName());
						player.sendPacket(html);
					}
				}
				else if(command.startsWith("Quest"))
				{
					String quest = command.substring(5).trim();
					if(quest.length() == 0)
						showQuestWindow(player);
					else
						showQuestWindow(player, quest);
				}
				else if(command.startsWith("Chat"))
					try
					{
						int val = Integer.parseInt(command.substring(5));
						showChatWindow(player, val);
					}
					catch(NumberFormatException nfe)
					{
						String filename = command.substring(5).trim();
						if(filename.length() == 0)
							showChatWindow(player, "data/html/npcdefault.htm");
						else
							showChatWindow(player, filename);
					}
				else if(command.startsWith("Loto"))
				{

					int val = 0;
					try
					{
						val = Integer.parseInt(command.substring(5));
					}
					catch(IndexOutOfBoundsException e)
					{
						e.printStackTrace();
					}
					catch(NumberFormatException e)
					{
						e.printStackTrace();
					}
					if(val == 0)
						// new loto ticket
						for(int i = 0; i < 5; i++)
							player.setLoto(i, 0);
					showLotoWindow(player, val);
				}
				else if(command.startsWith("AttributeCancel"))
					player.sendPacket(new ExShowBaseAttributeCancelWindow(player));
				else if(command.startsWith("CPRecovery"))
					makeCPRecovery(player);
				else if(command.startsWith("NpcLocationInfo"))
				{
					int val = Integer.parseInt(command.substring(16));
					L2NpcInstance npc = L2ObjectsStorage.getByNpcId(val);
					if(npc != null)
					{
						// Убираем флажок на карте и стрелку на компасе
						player.sendPacket(new RadarControl(2, 2, npc.getLoc()));
						player.sendPacket(new RadarControl(0, 1, npc.getLoc()));
						showChatWindow(player, "data/html/default/NPCloc/MoveToLoc.htm");
					}
				}
				else if(command.startsWith("Multisell") || command.startsWith("multisell"))
				{
					String listId = command.substring(9).trim();
					player.setLastMultisellNpc(this);
					L2Multisell.getInstance().SeparateAndSend(Integer.parseInt(listId), player, (!Config.SERVICES_OFFSHORE_NO_CASTLE_TAX && !isInZone(ZoneType.offshore)) ? getCastle().getTaxRate() : 0);
				}
				else if(command.equalsIgnoreCase("SkillList"))
					showSkillList(player);
				else if(command.equalsIgnoreCase("ClanSkillList"))
					showClanSkillList(player);
				else if(command.equalsIgnoreCase("FishingSkillList"))
					showFishingSkillList(player);
				else if(command.equalsIgnoreCase("CollectionSkillList"))
					showCollectionSkillList(player);
				else if(command.equalsIgnoreCase("SubclassSkillList"))
					showSubclassSkillList(player);
				else if(command.equalsIgnoreCase("TransformationSkillList"))
					showTransformationSkillList(player);
				else if(command.equalsIgnoreCase("LearnTransferSkill"))
				{
					if(player.getTransformation() != 0)
					{
						showChatWindow(player, "data/html/trainer/cant_teach_transform.htm");
						return;
					}

					ClassId classId = player.getClassId();
					if(classId == null)
						return;

					if(player.getLevel() < 76 || getTemplate().getTeachInfo() == null || !getTemplate().canTeach(classId) && !getTemplate().canTeach(classId.getParent(player.getSex())) || SkillTreeTable.getInstance().getAvailableTransferSkills(player) == null)
					{
						showChatWindow(player, "data/html/trainer/skill_link_no.htm");
						return;
					}

					showTransferSkillList(player);
				}
				else if(command.equalsIgnoreCase("ResetTransferSkill"))
				{
					if(player.getTransformation() != 0)
					{
						showChatWindow(player, "data/html/trainer/cant_teach_transform.htm");
						return;
					}

					ClassId classId = player.getClassId();
					if(classId == null)
						return;

					GArray<L2SkillLearn> list = SkillTreeTable.getInstance().getAvailableTransferSkills(player);
					if(player.getLevel() < 76 || getTemplate().getTeachInfo() == null || !getTemplate().canTeach(classId) && !getTemplate().canTeach(classId.getParent(player.getSex())) || list == null)
					{
						showChatWindow(player, "data/html/trainer/skill_link_reset_no.htm");
						return;
					}

					if(player.getAdena() < 10000000)
					{
						player.sendPacket(Msg.YOU_CANNOT_RESET_THE_SKILL_LINK_BECAUSE_THERE_IS_NOT_ENOUGH_ADENA);
						return;
					}

					GArray<L2Skill> learned = new GArray<L2Skill>(4);
					for(L2SkillLearn sl : list)
					{
						L2Skill skill = player.getKnownSkill((int) sl.getId());
						if(skill != null)
							learned.add(skill);
					}

					if(learned.size() == 0)
					{
						showChatWindow(player, "data/html/trainer/skill_link_reset_noskills.htm");
						return;
					}

					if(player.reduceAdena("ResetTransferSkill", 10000000, this, true))
					{
						for(L2Skill skill : learned)
						{
							player.removeSkill(skill, true);
							if(player.getAllShortCuts().size() > 0)
								for(L2ShortCut sc : player.getAllShortCuts())
									if(sc.id == skill.getId() && sc.type == L2ShortCut.TYPE_SKILL)
										player.deleteShortCut(sc.slot, sc.page);
						}
						
						int itemId = 0;
						int itemCount = 1;
						switch(player.getClassId())
						{
							case cardinal:
								itemId = L2Item.ITEM_ID_POMANDER_CARDINAL;
								break;
							case evaSaint:
								itemId = L2Item.ITEM_ID_POMANDER_EVAS_SAINT;
								break;
							case shillienSaint:
								itemId = L2Item.ITEM_ID_POMANDER_SHILIEN_SAINT;
								itemCount = 4;
								break;
						}

						if(itemId > 0)
						{
							L2ItemInstance pomander;
							while((pomander = player.getWarehouse().getItemByItemId(itemId)) != null)
								player.getWarehouse().destroyItem("ResetTransferSkill", pomander, player, this);

							while((pomander = player.getInventory().getItemByItemId(itemId)) != null)
								player.destroyItem("ResetTransferSkill", pomander.getObjectId(), pomander.getCount(), this, true);

							player.addItem("ResetTransferSkill", itemId, itemCount, this, true);
						}

						player.sendPacket(new SkillList(player));
					}
				}
				else if(command.equalsIgnoreCase("EnchantSkillManage"))
					showChatWindow(player, "data/html/enchant_skill_manage.htm");
				else if(command.startsWith("CancelCertification"))
				{
					if(player.isSubClassActive())
					{
						showChatWindow(player, "data/html/trainer/" + getNpcId() + "-nosub.htm");
						return;
					}
					else if(player.getAdena() < 10000000)
					{
						showChatWindow(player, "data/html/trainer/" + getNpcId() + "-nodel.htm");
						return;
					}

					if(player.reduceAdena("CancelCertification", 10000000, this, true))
					{
						player.unsetVar("cert-specific");
						for(int i = 1; i < 4; i++)
						{
							player.unsetVar("cert-" + i + "-" + 65);
							player.unsetVar("cert-" + i + "-" + 70);
							player.unsetVar("cert-" + i + "-" + 75);
							player.unsetVar("cert-" + i + "-" + 80);
						}

						SkillTreeTable.getInstance().deleteSubclassSkills(player);

						int items[] = {10280, 10281, 10282, 10283, 10284, 10285, 10286, 10287, 10289, 10288, 10290, 10292, 10291, 10294, 10293, 10612};

						for(Integer itemId : items)
						{
							L2ItemInstance item = player.getInventory().getItemByItemId(itemId);
							if(item != null)
								player.destroyItem("CancelCertification", item.getObjectId(), item.getCount(), this, true);
						}

						showChatWindow(player, "data/html/trainer/" + getNpcId() + "-del.htm");
					}
				}
				else if(command.startsWith("Augment"))
				{
					int cmdChoice = Integer.parseInt(command.substring(8, 9).trim());
					if(cmdChoice == 1)
					{
						player.sendPacket(new SystemMessage(SystemMessage.SELECT_THE_ITEM_TO_BE_AUGMENTED));
						player.sendPacket(Msg.ExShowVariationMakeWindow);
					}
					else if(cmdChoice == 2)
					{
						player.sendPacket(new SystemMessage(SystemMessage.SELECT_THE_ITEM_FROM_WHICH_YOU_WISH_TO_REMOVE_AUGMENTATION));
						player.setSessionVar("remove_aug", null);
						player.sendPacket(Msg.ExShowVariationCancelWindow);
					}
				}
				else if(command.startsWith("Link"))
					showChatWindow(player, "data/html/" + command.substring(5));
			}
			catch(StringIndexOutOfBoundsException sioobe)
			{
				_log.info("Incorrect htm bypass! npcId=" + getTemplate().npcId + " command=[" + command + "]");
			}
			catch(NumberFormatException nfe)
			{
				_log.info("Invalid bypass to Server command parameter! npcId=" + getTemplate().npcId + " command=[" + command + "]");
			}
	}

	public void showQuestWindow(L2Player player)
	{
		// collect awaiting quests and start points
		ArrayList<Quest> options = new ArrayList<Quest>();

		ArrayList<QuestState> awaits = player.getQuestsForTalk(getTemplate().npcId);
		Quest[] starts = getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);

		if(awaits != null)
			for(QuestState x : awaits)
				if(!options.contains(x.getQuest()))
					if(!x.getQuest().isCustom())
						options.add(x.getQuest());

		if(starts != null)
			for(Quest x : starts)
				if(!options.contains(x))
					if(!x.isCustom())
						options.add(x);

		// Display a QuestChooseWindow (if several quests are available) or QuestWindow
		if(options.size() > 1)
			showQuestChooseWindow(player, options);
		else if(options.size() == 1)
			showQuestWindow(player, options.get(0).getName());
		else
			showQuestWindow(player, "");
	}

	public void showQuestChooseWindow(L2Player player, ArrayList<Quest> quests)
	{
		HashMap<Integer, String> tpls = Util.parseTemplate(Files.read("data/html/quest_list.htm", player, false));
		StringBuffer sb = new StringBuffer();

		int t;
		for(Quest q : quests)
		{
			t = 1;
			QuestState qs = player.getQuestState(q.getName());
			if(qs != null)
			{
				if(qs.isStarted())
					t = 2;
				else if(qs.isCompleted())
					t = 3;
			}
			String descr = q.getDescr();
			if(!q.isCustom())
			{
				int fStringId = q.getQuestIntId();
				if(q.getQuestIntId() >= 10000)
					fStringId -= 5000;
				fStringId = fStringId * 100 + t;
				descr = Strings.getFString(fStringId);
			}
			sb.append(tpls.get(t).replace("%objectId%", String.valueOf(getObjectId())).replace("%questName%", q.getName()).replace("%questDescr%", descr));
		}

		NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
		html.setHtml(tpls.get(0).replace("<?QUESTLIST?>", sb.toString()));
		player.sendPacket(html);
	}

	public void showChatWindow(L2Player player, int val)
	{
		int npcId = getTemplate().npcId;
		int karma = player.getKarma();

		/* For use with Seven Signs implementation */
		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;
		int sealAvariceOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_AVARICE);
		int sealGnosisOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_GNOSIS);
		int playerCabal = SevenSigns.getInstance().getPlayerCabal(player);
		boolean isSealValidationPeriod = SevenSigns.getInstance().isSealValidationPeriod();
		int compWinner = SevenSigns.getInstance().getCabalWinner();

		switch(npcId)
		{
			case 31095: //
			case 31096: //
			case 31097: //
			case 31098: // Enter Necropolises
			case 31099: //
			case 31100: //
			case 31101: //
			case 31102: //
				if(isSealValidationPeriod)
				{
					if(sealAvariceOwner != compWinner)
					{
						player.sendMessage("Seal of avarice wasn't taken!");
						filename += "necro_no.htm";
					}
					else if(playerCabal != compWinner)
						switch(compWinner)
						{
							case SevenSigns.CABAL_DAWN:
								player.sendPacket(new SystemMessage(SystemMessage.CAN_BE_USED_ONLY_BY_THE_LORDS_OF_DAWN));
								filename += "necro_no.htm";
								break;
							case SevenSigns.CABAL_DUSK:
								player.sendPacket(new SystemMessage(SystemMessage.CAN_BE_USED_ONLY_BY_THE_REVOLUTIONARIES_OF_DUSK));
								filename += "necro_no.htm";
								break;
							case SevenSigns.CABAL_NULL:
								filename = getHtmlPath(npcId, val, karma); // do the default!
								break;
						}
					else
						filename = getHtmlPath(npcId, val, karma); // do the default!
				}
				else if(playerCabal == SevenSigns.CABAL_NULL)
					filename += "necro_no.htm";
				else
					filename = getHtmlPath(npcId, val, karma); // do the default!
				break;
			case 31114: //
			case 31115: //
			case 31116: // Enter Catacombs
			case 31117: //
			case 31118: //
			case 31119: //
				if(isSealValidationPeriod)
				{
					if(sealGnosisOwner != compWinner)
					{
						player.sendMessage("Seal of gnosis wasn't taken!");
						filename += "cata_no.htm";
					}
					else if(playerCabal != compWinner)
						switch(compWinner)
						{
							case SevenSigns.CABAL_DAWN:
								player.sendPacket(new SystemMessage(SystemMessage.CAN_BE_USED_ONLY_BY_THE_LORDS_OF_DAWN));
								filename += "cata_no.htm";
								break;
							case SevenSigns.CABAL_DUSK:
								player.sendPacket(new SystemMessage(SystemMessage.CAN_BE_USED_ONLY_BY_THE_REVOLUTIONARIES_OF_DUSK));
								filename += "cata_no.htm";
								break;
							case SevenSigns.CABAL_NULL:
								filename = getHtmlPath(npcId, val, karma); // do the default!
								break;
						}
					else
						filename = getHtmlPath(npcId, val, karma); // do the default!
				}
				else if(playerCabal == SevenSigns.CABAL_NULL)
					filename += "cata_no.htm";
				else
					filename = getHtmlPath(npcId, val, karma); // do the default!
				break;
			case 31111: // Gatekeeper Spirit (Disciples)
				if(playerCabal == sealAvariceOwner && playerCabal == compWinner)
					switch(sealAvariceOwner)
					{
						case SevenSigns.CABAL_DAWN:
							filename += "spirit_dawn.htm";
							break;
						case SevenSigns.CABAL_DUSK:
							filename += "spirit_dusk.htm";
							break;
						case SevenSigns.CABAL_NULL:
							filename += "spirit_null.htm";
							break;
					}
				else
					filename += "spirit_null.htm";
				break;
			case 31112: // Gatekeeper Spirit (Disciples)
				filename += "spirit_exit.htm";
				break;
			default:
				if(npcId >= 31093 && npcId <= 31094 || npcId >= 31172 && npcId <= 31201 || npcId >= 31239 && npcId <= 31254)
					return;
				// Get the text of the selected HTML file in function of the npcId and of the page number
				filename = getHtmlPath(npcId, val, karma);
				break;
		}

		player.sendPacket(new NpcHtmlMessage(player, this, filename, val));
	}

	public void showChatWindow(L2Player player, String filename)
	{
		player.sendPacket(new NpcHtmlMessage(player, this, filename, 0));
	}

	public String getHtmlPath(int npcId, int val, int karma)
	{
		String pom;
		if(val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;
		String temp = "data/html/default/" + pom + ".htm";
		File mainText = new File(temp);
		if(mainText.exists())
			return temp;

		temp = "data/html/trainer/" + pom + ".htm";
		mainText = new File(temp);
		if(mainText.exists())
			return temp;

		// If the file is not found, the standard message "I have nothing to say to you" is returned
		return "data/html/npcdefault.htm";
	}

	// 0 - first buy lottery ticket window
	// 1-20 - buttons
	// 21 - second buy lottery ticket window
	// 22 - selected ticket with 5 numbers
	// 23 - current lottery jackpot
	// 24 - Previous winning numbers/Prize claim
	// >24 - check lottery ticket by item object id
	public void showLotoWindow(L2Player player, int val)
	{
		int npcId = getTemplate().npcId;
		String filename;
		SystemMessage sm;
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

		if(val == 0) // 0 - first buy lottery ticket window
		{
			filename = (getHtmlPath(npcId, 1, player.getKarma()));
			html.setFile(filename);
		}
		else if(val >= 1 && val <= 21) // 1-20 - buttons, 21 - second buy lottery ticket window
		{
			if(!LotteryManager.getInstance().isStarted())
			{
				//tickets can't be sold
				player.sendPacket(new SystemMessage(SystemMessage.LOTTERY_TICKETS_ARE_NOT_CURRENTLY_BEING_SOLD));
				return;
			}
			if(!LotteryManager.getInstance().isSellableTickets())
			{
				//tickets can't be sold
				player.sendPacket(new SystemMessage(SystemMessage.TICKETS_FOR_THE_CURRENT_LOTTERY_ARE_NO_LONGER_AVAILABLE));
				return;
			}

			filename = (getHtmlPath(npcId, 5, player.getKarma()));
			html.setFile(filename);

			int count = 0;
			int found = 0;
			// counting buttons and unsetting button if found
			for(int i = 0; i < 5; i++)
			{
				if(player.getLoto(i) == val)
				{
					//unsetting button
					player.setLoto(i, 0);
					found = 1;
				}
				else if(player.getLoto(i) > 0)
				{
					count++;
				}
			}

			//if not rearched limit 5 and not unseted value
			if(count < 5 && found == 0 && val <= 20)
				for(int i = 0; i < 5; i++)
					if(player.getLoto(i) == 0)
					{
						player.setLoto(i, val);
						break;
					}

			//setting pusshed buttons
			count = 0;
			for(int i = 0; i < 5; i++)
				if(player.getLoto(i) > 0)
				{
					count++;
					String button = String.valueOf(player.getLoto(i));
					if(player.getLoto(i) < 10)
						button = "0" + button;
					String search = "fore=\"L2UI.lottoNum" + button + "\" back=\"L2UI.lottoNum" + button + "a_check\"";
					String replace = "fore=\"L2UI.lottoNum" + button + "a_check\" back=\"L2UI.lottoNum" + button + "\"";
					html.replace(search, replace);
				}

			if(count == 5)
			{
				String search = "0\">Return";
				String replace = "22\">The winner selected the numbers above.";
				html.replace(search, replace);
			}
		}
		else if(val == 22) //22 - selected ticket with 5 numbers
		{
			if(!LotteryManager.getInstance().isStarted())
			{
				//tickets can't be sold
				player.sendPacket(new SystemMessage(SystemMessage.LOTTERY_TICKETS_ARE_NOT_CURRENTLY_BEING_SOLD));
				return;
			}
			if(!LotteryManager.getInstance().isSellableTickets())
			{
				//tickets can't be sold
				player.sendPacket(new SystemMessage(SystemMessage.TICKETS_FOR_THE_CURRENT_LOTTERY_ARE_NO_LONGER_AVAILABLE));
				return;
			}

			int price = Config.ALT_LOTTERY_TICKET_PRICE;
			int lotonumber = LotteryManager.getInstance().getId();
			int enchant = 0;
			int type2 = 0;

			for(int i = 0; i < 5; i++)
			{
				if(player.getLoto(i) == 0)
					return;

				if(player.getLoto(i) < 17)
					enchant += Math.pow(2, player.getLoto(i) - 1);
				else
					type2 += Math.pow(2, player.getLoto(i) - 17);
			}
			if(player.reduceAdena("Lottery", price, this, true))
			{
				LotteryManager.getInstance().increasePrize(price);

				sm = new SystemMessage(SystemMessage.ACQUIRED__S1_S2);
				sm.addNumber(lotonumber);
				sm.addItemName(4442);
				player.sendPacket(sm);

				L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), 4442);
				item.setCount(1);
				item.setCustomType1(lotonumber);
				item.setEnchantLevel(enchant);
				item.setCustomType2(type2);
				player.getInventory().addItem("Lottery", item, player, this);

				InventoryUpdate iu = new InventoryUpdate();
				iu.addItem(item);
				L2ItemInstance adenaupdate = player.getInventory().getItemByItemId(57);
				iu.addModifiedItem(adenaupdate);
				player.sendPacket(iu);

				filename = (getHtmlPath(npcId, 3, player.getKarma()));
				html.setFile(filename);
			}
		}
		else if(val == 23) //23 - current lottery jackpot
		{
			filename = (getHtmlPath(npcId, 3, player.getKarma()));
			html.setFile(filename);
		}
		else if(val == 24) // 24 - Previous winning numbers/Prize claim
		{
			filename = (getHtmlPath(npcId, 4, player.getKarma()));
			html.setFile(filename);

			int lotonumber = LotteryManager.getInstance().getId();
			String message = "";
			for(L2ItemInstance item : player.getInventory().getItems())
			{
				if(item == null)
					continue;
				if(item.getItemId() == 4442 && item.getCustomType1() < lotonumber)
				{
					message = message + "<a action=\"bypass -h npc_%objectId%_Loto " + item.getObjectId() + "\">" + item.getCustomType1() + " Event Number ";
					int[] numbers = LotteryManager.getInstance().decodeNumbers(item.getEnchantLevel(), item.getCustomType2());
					for(int i = 0; i < 5; i++)
					{
						message += numbers[i] + " ";
					}
					int[] check = LotteryManager.getInstance().checkTicket(item);
					if(check[0] > 0)
					{
						switch(check[0])
						{
							case 1:
								message += "- 1st Prize";
								break;
							case 2:
								message += "- 2nd Prize";
								break;
							case 3:
								message += "- 3th Prize";
								break;
							case 4:
								message += "- 4th Prize";
								break;
						}
						message += " " + check[1] + "a.";
					}
					message += "</a><br>";
				}
			}
			if(message.equals(""))
			{
				message += "There is no winning lottery ticket...<br>";
			}
			html.replace("%result%", message);
		}
		else if(val > 24) // >24 - check lottery ticket by item object id
		{
			int lotonumber = LotteryManager.getInstance().getId();
			L2ItemInstance item = player.getInventory().getItemByObjectId(val);
			if(item == null || item.getItemId() != 4442 || item.getCustomType1() >= lotonumber)
			{
				return;
			}
			int[] check = LotteryManager.getInstance().checkTicket(item);

			player.destroyItem("Lottery", item.getObjectId(), 1, this, true);

			int adena = check[1];
			if(adena > 0)
				player.addAdena("Lottery", adena, this, true);

			return;
		}
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%race%", "" + LotteryManager.getInstance().getId());
		html.replace("%adena%", "" + LotteryManager.getInstance().getPrize());
		html.replace("%ticket_price%", "" + Config.ALT_LOTTERY_TICKET_PRICE);
		html.replace("%prize5%", "" + (Config.ALT_LOTTERY_5_NUMBER_RATE * 100));
		html.replace("%prize4%", "" + (Config.ALT_LOTTERY_4_NUMBER_RATE * 100));
		html.replace("%prize3%", "" + (Config.ALT_LOTTERY_3_NUMBER_RATE * 100));
		html.replace("%prize2%", "" + Config.ALT_LOTTERY_2_AND_1_NUMBER_PRIZE);
		html.replace("%enddate%", "" + DateFormat.getDateInstance().format(LotteryManager.getInstance().getEndDate()));
		player.sendPacket(html);

		player.sendActionFailed();
	}

	public void makeCPRecovery(L2Player player)
	{
		if(getNpcId() != 31225 && getNpcId() != 31226)
			return;
		if(player.reduceAdena("CPRecovery", 100, this, true))
		{
			player.setCurrentCp(getCurrentCp() + 5000);
			player.sendPacket(new SystemMessage(SystemMessage.S1_CPS_HAVE_BEEN_RESTORED).addString(player.getName()));
		}
	}

	private boolean _isBusy;
	private String _busyMessage = "";

	/**
	 * Return the busy status of this L2NpcInstance.<BR><BR>
	 */
	public final boolean isBusy()
	{
		return _isBusy;
	}

	/**
	 * Set the busy status of this L2NpcInstance.<BR><BR>
	 */
	public void setBusy(boolean isBusy)
	{
		_isBusy = isBusy;
	}

	/**
	 * Return the busy message of this L2NpcInstance.<BR><BR>
	 */
	public final String getBusyMessage()
	{
		return _busyMessage;
	}

	/**
	 * Set the busy message of this L2NpcInstance.<BR><BR>
	 */
	public void setBusyMessage(String message)
	{
		_busyMessage = message;
	}

	public void showBusyWindow(L2Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
		html.setFile("data/html/npcbusy.htm");
		html.replace("%npcname%", getName());
		html.replace("%playername%", player.getName());
		html.replace("%busymessage%", _busyMessage);
		player.sendPacket(html);
	}

	/**
	 * this displays SkillList to the player.
	 *
	 * @param player
	 */
	public void showSkillList(L2Player player)
	{
		if(Config.DEBUG)
			_log.debug("SkillList activated on: " + getObjectId());

		if(player.getTransformation() != 0)
		{
			showChatWindow(player, "data/html/trainer/cant_teach_transform.htm");
			return;
		}

		ClassId classId = player.getClassId();

		if(classId == null)
			return;

		if(getTemplate().getTeachInfo() == null || !getTemplate().canTeach(classId) && !getTemplate().canTeach(classId.getParent(player.getSex())))
		{
			showChatWindow(player, "data/html/trainer/wrong_class.htm");
			return;
		}

		AcquireSkillList asl = new AcquireSkillList(SkillTreeTable.SKILL_TYPE_NORMAL);
		int counts = 0;

		GArray<L2SkillLearn> skills = SkillTreeTable.getInstance().getAvailableSkills(player, classId);
		for(L2SkillLearn s : skills)
		{
			L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			if(sk == null || !sk.getCanLearn(player.getClassId()) || sk.isForgotten())
				continue;
			counts++;
			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), s.getSpCost(), 0);
		}

		if(counts == 0)
		{
			//NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			int minlevel = SkillTreeTable.getInstance().getMinLevelForNewSkill(player, classId);

			if(minlevel > 0)
			{
				SystemMessage sm = new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ANY_FURTHER_SKILLS_TO_LEARN__COME_BACK_WHEN_YOU_HAVE_REACHED_LEVEL_S1);
				sm.addNumber(minlevel);
				player.sendPacket(sm);
			}
			else
				player.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN));
			player.sendPacket(AcquireSkillDone.ACQUIRE_DONE);
		}
		else
			player.sendPacket(asl);
		player.sendActionFailed();
	}

	public void showFishingSkillList(L2Player player)
	{
		if(Config.DEBUG)
			_log.debug("SkillList activated on: " + getObjectId());

		if(player.getTransformation() != 0)
		{
			showChatWindow(player, "data/html/trainer/cant_teach_transform.htm");
			return;
		}

		AcquireSkillList asl = new AcquireSkillList(SkillTreeTable.SKILL_TYPE_FISHING);
		int counts = 0;

		GArray<L2SkillLearn> skills = SkillTreeTable.getInstance().getAvailableFishingSkills(player);
		for(L2SkillLearn s : skills)
		{
			L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			if(sk == null)
				continue;
			counts++;
			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), s.getSpCost(), 0);
		}

		if(counts == 0)
		{
			player.sendPacket(AcquireSkillDone.ACQUIRE_DONE);
			player.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN));
		}
		else
			player.sendPacket(asl);
		player.sendActionFailed();
	}

	public void showCollectionSkillList(L2Player player)
	{
		if(Config.DEBUG)
			_log.debug("SkillList activated on: " + getObjectId());

		if(player.getTransformation() != 0)
		{
			showChatWindow(player, "data/html/trainer/cant_teach_transform.htm");
			return;
		}

		AcquireSkillList asl = new AcquireSkillList(SkillTreeTable.SKILL_TYPE_COLLECTION);
		int counts = 0;

		GArray<L2SkillLearn> skills = SkillTreeTable.getInstance().getAvailableCollectionSkills(player);
		for(L2SkillLearn s : skills)
		{
			L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			if(sk == null)
				continue;
			counts++;
			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), s.getSpCost(), 0);
		}

		if(counts == 0)
		{
			player.sendPacket(AcquireSkillDone.ACQUIRE_DONE);
			player.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN));
		}
		else
			player.sendPacket(asl);
		player.sendActionFailed();
	}

	public void showSubclassSkillList(L2Player player)
	{
		if(Config.DEBUG)
			_log.debug("SkillList activated on: " + getObjectId());

		if(player.getTransformation() != 0)
		{
			showChatWindow(player, "data/html/trainer/cant_teach_transform.htm");
			return;
		}

		if(!Config.ALLOW_LEARN_TRANS_SKILLS_WO_QUEST)
		{
			QuestState q = player.getQuestState("_136_MoreThanMeetsTheEye");
			if(q == null || !q.getState().equalsIgnoreCase("COMPLETED") && !Config.ALLOW_LEARN_TRANS_SKILLS_WO_QUEST)
			{
				showChatWindow(player, "data/html/trainer/" + getNpcId() + "-noquest.htm");
				return;
			}
		}

		int items[] = {10280, 10281, 10282, 10283, 10284, 10285, 10286, 10287, 10289, 10288, 10290, 10292, 10291, 10294, 10293, 10612};
		boolean noItems = true;
		for(Integer itemId : items)
		{
			if(player.getInventory().getItemByItemId(itemId) != null && player.getInventory().getItemByItemId(itemId).getCount() > 0)
			{
				noItems = false;
				break;
			}
		}

		if(player.isSubClassActive() || noItems)
		{
			showChatWindow(player, "data/html/trainer/" + getNpcId() + "-noitems.htm");
			return;
		}

		AcquireSkillList asl = new AcquireSkillList(SkillTreeTable.SKILL_TYPE_SUBCLASS);
		int counts = 0;

		GArray<L2SkillLearn> skills = SkillTreeTable.getInstance().getAvailableSubclassSkills(player);
		for(L2SkillLearn s : skills)
		{
			L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			if(sk == null)
				continue;
			counts++;
			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), s.getSpCost(), 1);
		}

		if(counts == 0)
		{
			player.sendPacket(AcquireSkillDone.ACQUIRE_DONE);
			player.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN));
		}
		else
			player.sendPacket(asl);
		player.sendActionFailed();
	}

	public void showTransferSkillList(L2Player player)
	{
		AcquireSkillList asl = new AcquireSkillList(SkillTreeTable.SKILL_TYPE_TRANSFER);
		int counts = 0;

		GArray<L2SkillLearn> skills = SkillTreeTable.getInstance().getAvailableTransferSkills(player);
		if(skills == null)
			return;
		for(L2SkillLearn s : skills)
		{
			L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			if(sk == null || player.getSkillLevel(sk.getId()) > 0)
				continue;
			counts++;
			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), s.getSpCost(), 1);
		}

		if(counts == 0)
		{
			player.sendPacket(AcquireSkillDone.ACQUIRE_DONE);
			player.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN));
		}
		else
			player.sendPacket(asl);
		player.sendActionFailed();
	}

	public void showTransformationSkillList(L2Player player)
	{
		if(Config.DEBUG)
			_log.debug("SkillList activated on: " + getObjectId());

		if(player.getTransformation() != 0)
		{
			showChatWindow(player, "data/html/trainer/cant_teach_transform.htm");
			return;
		}

		if(!Config.ALLOW_LEARN_TRANS_SKILLS_WO_QUEST)
		{
			QuestState q = player.getQuestState("_136_MoreThanMeetsTheEye");
			if(q == null || !q.getState().equalsIgnoreCase("COMPLETED") && !Config.ALLOW_LEARN_TRANS_SKILLS_WO_QUEST)
			{
				showChatWindow(player, "data/html/trainer/" + getNpcId() + "-noquest.htm");
				return;
			}
		}

		AcquireSkillList asl = new AcquireSkillList(SkillTreeTable.SKILL_TYPE_TRANSFORM);
		int counts = 0;

		GArray<L2SkillLearn> skills = SkillTreeTable.getInstance().getAvailableTransformationSkills(player);
		for(L2SkillLearn s : skills)
		{
			L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			if(sk == null)
				continue;
			counts++;
			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), s.getSpCost(), 1);
		}

		if(counts == 0)
		{
			player.sendPacket(AcquireSkillDone.ACQUIRE_DONE);
			player.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN));
		}
		else
			player.sendPacket(asl);
		player.sendActionFailed();
	}

	public void showClanSkillList(L2Player player)
	{
		if(Config.DEBUG)
			_log.debug("SkillList activated on: " + getObjectId());

		if(player.getTransformation() != 0)
		{
			showChatWindow(player, "data/html/trainer/cant_teach_transform.htm");
			return;
		}

		if(player.getClanId() == 0 || !player.isClanLeader())
		{
			player.sendPacket(Msg.ONLY_THE_CLAN_LEADER_IS_ENABLED);
			player.sendActionFailed();
			return;
		}

		AcquireSkillList asl = new AcquireSkillList(SkillTreeTable.SKILL_TYPE_CLAN);
		int counts = 0;

		GArray<L2SkillLearn> skills = SkillTreeTable.getInstance().getAvailableClanSkills(player.getClan());
		for(L2SkillLearn s : skills)
		{
			L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			if(sk == null)
				continue;
			int cost = s.getRepCost();
			counts++;
			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), cost, 0);
		}

		if(counts == 0)
		{
			player.sendPacket(AcquireSkillDone.ACQUIRE_DONE);
			player.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN));
		}
		else
			player.sendPacket(asl);
		player.sendActionFailed();
	}

	public void showClanSubPledgeSkillList(L2Player player)
	{
		AcquireSkillList asl = new AcquireSkillList(SkillTreeTable.SKILL_TYPE_CLAN_SUB_PLEDGE);
		int counts = 0;

		GArray<L2SkillLearn> skills = SkillTreeTable.getInstance().getAvailableSubPledgeSkills(player);
		for(L2SkillLearn s : skills)
		{
			L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			if(sk == null)
				continue;
			int cost = s.getRepCost();
			counts++;
			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), cost, 1);
		}

		if(counts != 0)
			player.sendPacket(asl);
		else
		{
			player.sendPacket(AcquireSkillDone.ACQUIRE_DONE);
			player.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN));
		}

		player.sendActionFailed();
	}

	/**
	 * Возвращает режим NPC: свежезаспавненный или нормальное состояние
	 *
	 * @return true, если NPC свежезаспавненный
	 */
	public boolean isShowSpawnAnimation()
	{
		return _showSpawnAnimation;
	}

	@Override
	public boolean getChargedSoulShot()
	{
		return _soulCharged;
	}

	@Override
	public int getChargedSpiritShot()
	{
		return _spiritCharged;
	}

	public void chargeShots(boolean spirit)
	{
		if(getTemplate().shots == L2NpcTemplate.ShotsType.NONE)
			return;

		if(Rnd.chance(_ssChance))
		{
			if(spirit)
				switch(getTemplate().shots)
				{
					case SPIRIT:
					case SOUL_SPIRIT:
						_spiritCharged = 1;
						break;
					case BSPIRIT:
					case SOUL_BSPIRIT:
						_spiritCharged = 2;
						break;
					default:
						_spiritCharged = 0;
				}
			else
				switch(getTemplate().shots)
				{
					case SOUL:
					case SOUL_SPIRIT:
					case SOUL_BSPIRIT:
						_soulCharged = true;
						break;
					default:
						_soulCharged = false;
				}

			if(_soulCharged || _spiritCharged > 0)
				broadcastPacket(new MagicSkillUse(this, spirit ? 2061 : 2039, 1, 0, 0));
		}
		else
		{
			_spiritCharged = 0;
			_soulCharged = false;
		}
	}

	public void useSoulShot(int chance)
	{
		if(Rnd.chance(chance))
		{
			_soulCharged = true;
			broadcastPacket(new MagicSkillUse(this, 2039, 1, 0, 0));
		}
	}

	public void useSpiritShot(int chance)
	{
		if(Rnd.chance(chance))
		{
			_spiritCharged = 1;
			broadcastPacket(new MagicSkillUse(this, 2061, 1, 0, 0));
		}
	}

	@Override
	public boolean unChargeShots(boolean spirit)
	{
		_soulCharged = false;
		_spiritCharged = 0;
		return true;
	}

	@Override
	public float getColRadius()
	{
		return getCollisionRadius();
	}

	@Override
	public float getColHeight()
	{
		return getCollisionHeight();
	}

	@Override
	public float getGrowColRadius()
	{
		return getGrowCollisionRadius();
	}

	@Override
	public float getGrowColHeight()
	{
		return getGrowCollisionHeight();
	}

	public boolean canMoveToHome()
	{
		return false;
	}

	@Override
	public boolean isCatacombMob()
	{
		return getFactionId().equalsIgnoreCase("c_dungeon_clan");
	}

	public boolean isRaidMinion()
	{
		return isMinion() && getLeader() != null && getLeader().isRaid();
	}

	protected boolean isHaveRigths(L2Player player, int rigthsToCheck)
	{
		return player.isGM() || player.getClanId() != 0 && (player.getClanPrivileges() & rigthsToCheck) == rigthsToCheck;
	}

	protected int validateCondition(L2Player player)
	{
		SiegeUnit unit = getBuilding(-1);
		if(player.getClanId() != 0 && unit.getOwnerId() == player.getClanId())
		{
			if(unit.getSiegeZone() != null && unit.getSiege().isInProgress())
				return Cond_Busy_Because_Of_Siege;
			else
				return Cond_Owner;
		}
		return Cond_All_False;
	}

	public void setShowSpawnAnimation(boolean val)
	{
		_showSpawnAnimation = val;
	}

	@Override
	public L2Skill.TargetType getTargetRelation(L2Character target, boolean offensive)
	{
		if(isDead())
			return L2Skill.TargetType.npc_body;

		if(this == target)
			return L2Skill.TargetType.self;

		return L2Skill.TargetType.target;
	}

	@Override
	public int getTerritoryId()
	{
		return _territoryId;
	}

	@Override
	public int getClanCrestId()
	{
		if(_showTag && TerritoryWarManager.getTerritoryById(_territoryId).hasLord())
			return TerritoryWarManager.getTerritoryById(_territoryId).getOwner().getCrestId();
		return 0;
	}

	@Override
	public int getAllyCrestId()
	{
		if(_showTag && TerritoryWarManager.getTerritoryById(_territoryId).hasLord())
			return TerritoryWarManager.getTerritoryById(_territoryId).getOwner().getAllyCrestId();
		return 0;
	}

	@Override
	public int getClanId()
	{
		if(_showTag && TerritoryWarManager.getTerritoryById(_territoryId).hasLord())
			return TerritoryWarManager.getTerritoryById(_territoryId).getOwner().getClanId();
		return 0;
	}

	@Override
	public int getAllyId()
	{
		if(_showTag && TerritoryWarManager.getTerritoryById(_territoryId).hasLord())
			return TerritoryWarManager.getTerritoryById(_territoryId).getOwner().getAllyId();
		return 0;
	}

	public boolean isShowTag()
	{
		return _showTag;
	}

	public void setAIParams(StatsSet params)
	{
		if(getTemplate().getAIParams() != null)
			_thisParams = getTemplate().getAIParams().clone();

		if(_thisParams == null)
			_thisParams = params;
		else
			for(String key : params.getSet().keySet())
				_thisParams.set(key, params.getString(key));
	}

	public StatsSet getAIParams()
	{
		if(_thisParams != null)
			return _thisParams;
		return getTemplate().getAIParams();
	}

	public int getDisplayId()
	{
		if(_displayId > 0)
			return _displayId;
		return getTemplate().displayId;
	}

	public void setDisplayId(int displayId)
	{
		_displayId = displayId;
	}

	public void changeNpcState(int state)
	{
		_state = state;
		broadcastPacket(new ExChangeNpcState(getObjectId(), _state));
	}

	public void setNpcState(int state)
	{
		_state = state;
	}

	public int getNpcState()
	{
		return _state;
	}

	public int getShowNameTag()
	{
		return show_name_tag;
	}

	public void removeMinions()
	{
		if(minionMaintainTask != null)
			minionMaintainTask.cancel(true);

		minionList.maintainLonelyMinions();
	}

	public int getTotalSpawnedMinionsInstances()
	{
		return minionList.countSpawnedMinions();
	}

	public int getTotalSpawnedMinionsGroups()
	{
		return minionList.lazyCountSpawnedMinionsGroups();
	}

	public void notifyMinionSpawned(L2NpcInstance minion)
	{
		minionList.addSpawnedMinion(minion);
	}

	@Override
	public boolean hasMinions()
	{
		return minionList.hasMinions();
	}

	protected int getMaintenanceInterval()
	{
		return MONSTER_MAINTENANCE_INTERVAL;
	}

	public void spawnMinions()
	{
		if(getMinionsData() != null)
			minionList.maintainMinions();
	}

	public MinionList getMinionList()
	{
		return minionList;
	}

	public GArray<L2MinionData> getMinionsData()
	{
		if(minionData != null)
			return minionData;

		return getTemplate().getMinionData();
	}

	public void setMinionsData(GArray<L2MinionData> minions)
	{
		minionData = minions;
	}

	public void setMinionData(L2MinionData data)
	{
		_minionData= data;
	}

	public L2MinionData getMinionData()
	{
		return _minionData;
	}

	/**
	 * Return the master of this L2MinionInstance.<BR><BR>
	 */
	public L2NpcInstance getLeader()
	{
		return _master;
	}

	public void setLeader(L2NpcInstance leader)
	{
		_master = leader;
	}

	public boolean isMyBossAlive()
	{
		return _master != null && !_master.isDead() && _master.isVisible();
	}

	@Override
	public boolean isMovementDisabled()
	{
		return getTemplate().can_move == 0 || super.isMovementDisabled();
	}

	public void refreshID()
	{
		int oldObjectId = getObjectId();
		if(L2ObjectsStorage.isStored(_storedId))
		{
			_objectId = IdFactory.getInstance().getNextId();
			_storedId = L2ObjectsStorage.refreshId(this);
		}
		else
		{
			_objectId = IdFactory.getInstance().getNextId();
			_storedId = L2ObjectsStorage.put(this);
		}
		_moveTaskRunnable.updateStoreId(_storedId);
		IdFactory.getInstance().releaseId(oldObjectId);
	}

	public void createPrivates(String privateStr)
	{
		if(privateStr != null && !privateStr.isEmpty())
		{
			if(minionData == null)
				minionData = new GArray<>(1);
			else
				minionData.clear();

			for(String priv : privateStr.split(";"))
				if(priv != null && !priv.isEmpty())
				{
					String[] privateParams = priv.split(":");
					if(privateParams.length >= 4)
					{
						if(minionData == null)
							minionData = new GArray<>(1);

						minionData.add(new L2MinionData(Integer.parseInt(privateParams[0]), privateParams[1], 1, SpawnTable.getSecFromString(privateParams[3]), Integer.parseInt(privateParams[2])));
					}
				}
		}
	}

	public L2NpcInstance createOnePrivate(int npcId, String ai, int weight, int respawn, int x, int y, int z, int h, long p1, long p2, long p3)
	{
		return minionList.spawnSingleMinion(new L2MinionData(npcId, ai, 1, respawn, weight), new Location(x, y, z, h), p1, p2, p3);
	}

	public void respawnPrivate(L2NpcInstance minion, int weight, long respawn)
	{
		if(respawn > 0)
			minionList.respawnPrivate(minion, respawn);
	}

	public void lookNeighbor(int range)
	{
		lookNeighbor(range, false);
	}

	public void lookNeighbor(int range, boolean force)
	{
		if(neighbors == null)
			neighbors = new GCSArray<>();
		else if(force || lastNeighborsClean + 30000 < System.currentTimeMillis())
		{
			lastNeighborsClean = System.currentTimeMillis();
			neighbors.clear();
		}

		for(L2Character cha : L2World.getAroundCharacters(this))
			if(!cha.isPlayer() || !cha.isHide() && !((L2Player) cha).isInvisible())
				if(isInRange(cha, range) && !neighbors.contains(cha.getStoredId()))
				{
					getAI().notifyEvent(CtrlEvent.EVT_SEE_CREATURE, cha);
					neighbors.add(cha.getStoredId());
				}

		for(Long storedId : neighbors)
		{
			L2Character c0 = L2ObjectsStorage.getAsCharacter(storedId);
			if(c0 == null || !isInRange(c0, range))
			{
				neighbors.remove(storedId);
				getAI().notifyEvent(CtrlEvent.EVT_CREATURE_LOST, c0, L2ObjectsStorage.getStoredObjectId(storedId));
			}
		}
	}

	public void removeNeighbor(L2Object object)
	{
		if(neighbors != null)
			neighbors.remove(object.getStoredId());
	}

	public GCSArray<Long> getNeighbors()
	{
		return neighbors;
	}

	@Override
	public void addEffect(L2Effect newEffect, int effectTimeModifier)
	{
		super.addEffect(newEffect, effectTimeModifier);
		ThreadPoolManager.getInstance().executeAi(new L2ObjectTasks.NotifyAITask(this, CtrlEvent.EVT_ABNORMAL_STATUS_CHANGED, newEffect.getEffector(), newEffect, true), false);
	}

	@Override
	public void removeEffect(L2Effect effect)
	{
		if(effect == null)
			return;
		super.removeEffect(effect);
		ThreadPoolManager.getInstance().executeAi(new L2ObjectTasks.NotifyAITask(this, CtrlEvent.EVT_ABNORMAL_STATUS_CHANGED, effect.getEffector(), effect, false), false);
	}

	public void changeHeading(int heading)
	{
		if(getHeading() != heading)
		{
			setHeading(heading);
			updateAbnormalEffect();
			broadcastPacket(new ValidateLocation(this));
		}
	}

	public boolean inMyTerritory(L2Character cha)
	{
		if(getLeader() != null)
		{
			if(getLeader().spawnDefine != null)
				return getLeader().spawnDefine.getMaker().inTerritory(cha.getX(), cha.getY());
			else if(getLeader()._spawn != null && getLeader()._spawn.getLocation() > 0)
			{
				L2Territory territory = TerritoryTable.getInstance().getLocation(getLeader()._spawn.getLocation());
				if(territory != null)
					return territory.isInside(cha.getX(), cha.getY());
			}
		}
		else if(spawnDefine != null)
			return spawnDefine.getMaker().inTerritory(cha.getX(), cha.getY());
		else if(_spawn != null && _spawn.getLocation() > 0)
		{
			L2Territory territory = TerritoryTable.getInstance().getLocation(_spawn.getLocation());
			if(territory != null)
				return territory.isInside(cha.getX(), cha.getY());
		}

		return true;
	}

	public int getLifeTime()
	{
		if(spawnTime == 0)
			return 0;

		return (int) ((System.currentTimeMillis() - spawnTime) / 1000);
	}

	public void showPage(L2Player talker, String file)
	{
		if(file.contains("/"))
			file = "data/html/" + file;
		else
			file = "data/html/default/" + file;

		talker.sendPacket(new NpcHtmlMessage(talker, this, file, 0));
	}

	public void showPage(L2Player talker, String file, int questId)
	{
		Quest q = QuestManager.getQuest(questId);

		if(q == null)
			return;

		file = "data/scripts/quests/" + q.getName() + "/" + file;

		talker.sendPacket(new NpcHtmlMessage(talker, this, file, 0));
	}

	public void showQuestPage(L2Player talker, String file, int questId)
	{
		if(file.contains("/"))
			file = "data/html/" + file;
		else
			file = "data/html/default/" + file;
		NpcHtmlMessage html = new NpcHtmlMessage(talker, this, file, 0);
		html.setQuest(questId);
		talker.sendPacket(html);
	}

	public int getInstanceZoneId()
	{
		if(getReflection() > 0)
		{
			Instance inst = InstanceManager.getInstance().getInstanceByReflection(getReflection());
			if(inst != null)
				return inst.getTemplate().getId();
		}

		return 0;
	}

	public Instance getInstanceZone()
	{
		if(getReflection() > 0)
			return InstanceManager.getInstance().getInstanceByReflection(getReflection());

		return null;
	}

	public void showHtml(L2Player talker, String html)
	{
		talker.sendPacket(new NpcHtmlMessage(talker, this).setHtml(html));
	}

	public String getHtmlFile(L2Player talker, String file)
	{
		if(file.contains("/"))
			file = "data/html/" + file;
		else
			file = "data/html/default/" + file;

		return Files.read(file, talker, false);
	}

	public void setAbilityItemDrop(boolean drop)
	{
		abilityItemDrop = drop;
	}

	public boolean isShowHp()
	{
		return _showHp;
	}

	public void notifyAiEvent(L2Character cha, CtrlEvent event, Object eventId, Object arg1, Object arg2)
	{
		if(cha == null)
			return;

		ThreadPoolManager.getInstance().executeAi(new L2ObjectTasks.NotifyAITask(cha, event, eventId, arg1, arg2), false);
	}

	public void changeMasterName(String name)
	{
		setTitle(name);
		setDisplayId(getNpcId());
		updateAbnormalEffect();
	}

	public void changeNickName(String name)
	{
		setName(name);
		setDisplayId(getNpcId());
		updateAbnormalEffect();
	}

	public void changeFStrMasterName(int fStrId, String name)
	{
		setTitleFStringId(fStrId);
		setTitle(name);
		setDisplayId(getNpcId());
		updateAbnormalEffect();
	}

	public void changeFStrNickName(int fStrId, String name)
	{
		setNameFStringId(fStrId);
		setName(name);
		setDisplayId(getNpcId());
		updateAbnormalEffect();
	}

	public void teleportFStr(L2Player talker, int[][] position, String shopName, String unk1, String unk2, String unk3, int itemId, int fStringItemName, String p1, String p2, String p3, String p4, String p5)
	{
		if(talker == null || position == null || position.length < 1)
			return;

		HashMap<Integer, String> tpls;
		tpls = Util.parseTemplate(Files.read("data/html/teleport_request.htm", talker, false));
		String html = tpls.get(0);
		String list = "";

		if(talker.getLevel() < 41)
		{
			String tpl = tpls.get(1);
			for(int c = 0; c < position.length; c++)
			{
				list += tpl.replace("<?list_id?>", String.valueOf(position.hashCode())).replace("<?list_pos?>", String.valueOf(c)).replace("<?item_id?>", "0").replace("<?npc_index?>", String.valueOf(getObjectId())).replace("<?location?>", String.valueOf(position[c][0]));
			}
		}
		else
		{
			String tpl = tpls.get(2);
			int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
			int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
			boolean lowPrice = itemId == 57 && day != 1 && day != 7 && (hour <= 12 || hour >= 22);

			for(int c = 0; c < position.length; c++)
			{
				list += tpl.replace("<?list_id?>", String.valueOf(position.hashCode())).replace("<?list_pos?>", String.valueOf(c)).replace("<?item_id?>", String.valueOf(itemId)).replace("<?npc_index?>", String.valueOf(getObjectId())).replace("<?location?>", String.valueOf(position[c][0])).replace("<?count?>", String.valueOf(position[c][4] / (lowPrice ? 2 : 1)))
						.replace("<?item_name?>", String.valueOf(fStringItemName)).replace("<?p1?>", p1).replace("<?p2?>", p2).replace("<?p3?>", p3).replace("<?p4?>", p4).replace("<?p5?>", p5);
			}
		}

		html = html.replace("<?teleport_list?>", list);
		NpcHtmlMessage htm = new NpcHtmlMessage(talker, this);
		htm.setHtml(html);
		talker.sendPacket(htm);
	}

	@Override
	public double getBaseHpRegen()
	{
		return hpRegen;
	}

	public void setHpRegen(double regen)
	{
		hpRegen = regen;
	}

	public void notifyClanDead()
	{
		try
		{
			if(getFactionId() != null && !getFactionId().isEmpty())
			{
				for(L2NpcInstance npc1 : getAroundFriends())
					notifyAiEvent(npc1, CtrlEvent.EVT_CLAN_DIED, this, null, null);
			}
		}
		catch(Exception e)
		{
		}
	}

	public void teleportParty(L2Party party, int x, int y, int z, int range, int zRange)
	{
		if(party == null)
			return;

		for(L2Player member : party.getPartyMembers())
		{
			if(zRange > 0 && isInRange(member, range) && Math.abs(getZ() - member.getZ()) < zRange)
				member.teleToLocation(x, y, z);
			else if(isInRange(member, range))
				member.teleToLocation(x, y, z);
		}
	}

	public boolean isMyLord(L2Character talker)
	{
		if(talker == null)
			return false;

		L2Player player = talker.getPlayer();
		if(player == null)
			return false;

		L2Clan clan = player.getClan();
		if(clan == null)
			return false;

		if(clan.getLeaderId() != player.getObjectId())
			return false;

		//0=clanHall, 1=fort, 2=castle
		SiegeUnit su = getBuilding(0);
		if(su != null && su.getId() == clan.getHasHideout())
			return true;

		su = getBuilding(1);
		if(su != null && su.getId() == clan.getHasFortress())
			return true;

		su = getBuilding(2);
		return su != null && su.getId() == clan.getHasCastle();
	}

	public void changeWeaponEnchant(int enchant)
	{
		weaponEnchant = enchant;
		removeStatsOwner(this);
		addStatFunc(new FuncEnchantNpc(Stats.POWER_ATTACK, 0x0C, this, 0));
		addStatFunc(new FuncEnchantNpc(Stats.MAGIC_ATTACK, 0x0C, this, 0));
		updateAbnormalEffect();
	}

	public int getWeaponEnchant()
	{
		return weaponEnchant;
	}

	public void setWeaponEnchant(int enchant)
	{
		weaponEnchant = enchant;
	}

	public boolean isIgnoreClanHelp()
	{
		return  _ignoreClanHelp;
	}

	@Override
	public int getTeam()
	{
		return team;
	}

	@Override
	public void setTeam(int team)
	{
		this.team = team;
		updateAbnormalEffect();
	}

	@Override
	public String toString()
	{
		return "npc " + getTemplate().name + "(" + getNpcId() + ")";
	}
}