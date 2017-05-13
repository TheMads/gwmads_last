package ru.l2gw.gameserver.model;

import javolution.util.FastList;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.arrays.GCSArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.extensions.listeners.PropertyCollection;
import ru.l2gw.extensions.listeners.engine.MethodInvocationResult;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.ai.CtrlEvent;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.ai.L2CharacterAI;
import ru.l2gw.gameserver.ai.L2PlayableAI;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.geodata.GeoMove;
import ru.l2gw.gameserver.handler.ScriptHandler;
import ru.l2gw.gameserver.instancemanager.QuestManager;
import ru.l2gw.gameserver.instancemanager.SiegeManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.instancemanager.ZoneManager;
import ru.l2gw.gameserver.model.L2ObjectTasks.*;
import ru.l2gw.gameserver.model.L2Skill.AbnormalVisualEffect;
import ru.l2gw.gameserver.model.L2Skill.TargetType;
import ru.l2gw.gameserver.model.entity.duel.Duel;
import ru.l2gw.gameserver.model.entity.olympiad.Olympiad;
import ru.l2gw.gameserver.model.entity.vehicle.L2ClanAirship;
import ru.l2gw.gameserver.model.instances.*;
import ru.l2gw.gameserver.model.quest.Quest;
import ru.l2gw.gameserver.model.quest.QuestState;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.model.zone.L2Zone.ZoneType;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.skills.*;
import ru.l2gw.gameserver.skills.funcs.Func;
import ru.l2gw.gameserver.tables.MapRegionTable;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.templates.*;
import ru.l2gw.util.Location;
import ru.l2gw.util.Util;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import static ru.l2gw.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;

/**
 * Mother class of all character objects of the world (PC, NPC...)<BR>
 * <BR>
 * L2Character :<BR>
 * <BR>
 * <li>L2CastleGuardInstance</li>
 * <li>L2DoorInstance</li>
 * <li>L2NpcInstance</li>
 * <li>L2PlayableInstance </li>
 * <BR>
 * <BR>
 * <p/>
 * <B><U> Concept of L2CharTemplate</U> :</B><BR>
 * <BR>
 * Each L2Character owns generic and static properties (ex : all Keltir have the
 * same number of HP...). All of those properties are stored in a different
 * template for each type of L2Character. Each template is loaded once in the
 * server cache memory (reduce memory use). When a new instance of L2Character
 * is spawned, server just create a link between the instance and the template.
 * This link is stored in <B>_template</B><BR>
 */
public abstract class L2Character extends L2Object
{
	public L2Character getFollowTarget()
	{
		L2Character target = null;
		try
		{
			if(followTarget == null)
				return null;

			target = followTarget.get();
			if(target == null)
				followTarget = null;
		}
		catch(NullPointerException npe)
		{
		}
		return target;
	}

	public void setFollowTarget(L2Character target)
	{
		followTarget = target == null ? null : new WeakReference<L2Character>(target);
	}

	class AltMagicUseTask implements Runnable
	{
		public final L2Skill _skill;
		public final L2Character _target;
		public final L2ItemInstance _usedItem;

		public AltMagicUseTask(L2Character target, L2Skill skill, L2ItemInstance usedItem)
		{
			_skill = skill;
			_target = target;
			_usedItem = usedItem;
		}

		public void run()
		{
			altOnMagicUseTimer(_target, _skill, _usedItem);
		}
	}

	/**
	 * Task of HP/MP/CP regeneration
	 */
	private class RegenTask implements Runnable
	{
		public void run()
		{
			try
			{
				if(isPlayer() && !(((L2Player) L2Character.this).isConnected() || ((L2Player) L2Character.this).isInOfflineMode()))
				{
					stopHpMpRegeneration();
					return;
				}

				if(isDead() || _currentHp >= getMaxHp() && _currentMp >= getMaxMp() && _currentCp >= getMaxCp())
				{
					stopHpMpRegeneration();
					return;
				}

				Duel duel = getDuel();
				if(duel != null && duel.isPartyDuel() && getDuelState() == Duel.DUELSTATE_DEAD)
				{
					stopHpMpRegeneration();
					return;
				}

				double addHp = 0;
				double addMp = 0;

				// Caculate the HP regen rate for this L2Character
				if(_currentHp < getMaxHp())
					addHp += Formulas.calcHpRegen(L2Character.this);

				// Caculate the HP regen rate for this L2Character
				if(_currentMp < getMaxMp())
					addMp += Formulas.calcMpRegen(L2Character.this);

				// Added regen bonus when character is sitting
				if(isPlayer() && Config.REGEN_SIT_WAIT)
				{
					L2Player pl = (L2Player) L2Character.this;
					if(pl.isSitting())
					{
						pl.updateWaitSitTime();
						if(pl.getWaitSitTime() > 5)
						{
							addHp += pl.getWaitSitTime();
							addMp += pl.getWaitSitTime();
						}
					}
				}
				else if(isRaid())
				{
					addHp *= Config.RATE_RAID_REGEN;
					addMp *= Config.RATE_RAID_REGEN;
				}

				if(_currentHp > calcStat(Stats.HP_LIMIT, getMaxHp(), null, null))
					addHp = 0;
				if(_currentMp > calcStat(Stats.MP_LIMIT, getMaxMp(), null, null))
					addMp = 0;
				// Modify the current HP and MP of the L2Character and broadcast
				// Server->Client packet StatusUpdate
				setCurrentHpMp(_currentHp + addHp, _currentMp + addMp);

				if(_currentCp < calcStat(Stats.CP_LIMIT, getMaxCp(), null, null))
					// Caculate the CP regen rate for this L2Character
					// Modify the current CP of the L2Character and broadcast
					// Server->Client packet StatusUpdate
					setCurrentCp(getCurrentCp() + Formulas.calcCpRegen(L2Character.this));
			}
			catch(Throwable e)
			{
				_log.warn("", e);
			}
		}
	}

	protected static final org.apache.commons.logging.Log _log = LogFactory.getLog(L2Character.class.getName());

	private int _customEffect;

	public static final double HEADINGS_IN_PI = 10430.378350470452724949566316381;
	public static final int INTERACTION_DISTANCE = 150;

	/**
	 * List of all QuestState instance that needs to be notified of this
	 * character's death
	 */
	private GArray<QuestState> _NotifyQuestOfDeathList;

	/**
	 * Array containing all clients that need to be notified about hp/mp updates
	 * of the L2Character
	 */
	private CopyOnWriteArraySet<L2Character> _statusListener;

	private Future<?> _skillTask;
	private Future<?> _skillLaunchedTask;
	private Future<?> _skillCoolTask;
	private Future<?> _altSkillTask;
	private Future<?> _regTask;
	public Future<?> _stanceTask;

	private long _stanceInited;

	private int _flagsRegenActive;
	private double _lastHpUpdate = -99999999;

	private static final int HP_REGEN_FLAG = 1;
	private static final int MP_REGEN_FLAG = 2;
	private static final int CP_REGEN_FLAG = 4;

	protected double _currentCp = 1;
	protected double _currentHp = 1;
	protected double _currentMp = 1;

	protected boolean _isAttackAborted;
	protected long _attackEndTime;
	protected long _attackReuseEndTime;

	/**
	 * HashMap(Integer, L2Skill) containing all skills of the L2Character
	 */
	protected Map<Integer, L2Skill> _skills = new ConcurrentHashMap<>();
	private Map<Integer, L2Skill> _skillsOnAttack;
	private Map<Integer, L2Skill> _skillsOnMagicAttack;
	private Map<Integer, L2Skill> _skillsOnUnderAttack;
	private Map<Integer, L2Skill> _skillsOnEvaded;
	private Map<Integer, L2Skill> _skillsOnShield;
	private Map<Integer, L2Skill> _skillsOnDamage;

	protected Map<Integer, Integer> _ignoredSkills;

	private L2Skill _castingSkill;
	private L2ItemInstance _castingItem;
	private WeakReference<L2Character> _castingTarget;

	private long _castEndTime;
	private long _castInterruptTime;

	private long _animationEndTime;

	/**
	 * Table containing all skillId that are disabled
	 */
	protected Map<Integer, TimeStamp> disabledSkills;

	protected ForceBuff _forceBuff;

	/**
	 * HashMap containing all active skills effects in progress of a
	 * L2Character. The Short key of this HashMap is the L2Skill Identifier that
	 * has created the effect.
	 */
	private EffectList _effects;

	/**
	 * Map 32 bits (0x00000000) containing all abnormal effect in progress
	 */
	private long _abnormalEffects;
	private GArray<AbnormalVisualEffect> _abnormalEffectsArray = new GArray<AbnormalVisualEffect>();

	protected boolean _flying;
	private boolean _riding = false;

	private boolean _fakeDeath;

	protected boolean _isInvul;

	protected boolean _isPendingRevive;

	private boolean _isTeleporting;

	protected boolean _overloaded;

	private boolean _killedAlready;

	private long _dropDisabled;

	private byte _isBlessedByNoblesse; // Восстанавливает все бафы после смерти
	private byte _isSalvation; // Восстанавливает все бафы после смерти и полностью CP, MP, HP
	private final Map<Integer, Byte> _skillMastery = new HashMap<>();
	private final GCSArray<Integer> _skillMasteryReuse = new GCSArray<>();

	private boolean _afraid;
	private boolean _imobilised;
	private boolean _confused;
	private boolean _blocked;
	private boolean _disabled;
	private boolean _isHide;

	private boolean _running;
	
	protected volatile EffectList _effectList;

	public ScheduledFuture<?> _blockTask;
	public ScheduledFuture<?> _moveTask;
	public final MoveNextTask _moveTaskRunnable;
	public boolean isMoving;
	public boolean isFollow;
	public boolean isPawn;
	public int _offset;

	protected ArrayList<Location> moveList = new ArrayList<Location>();
	protected Location destination = null;
	private Location desireDestination = null;
	protected Location prevDestination = null;

	/**
	 * при moveToLocation используется для хранения геокоординат в которые мы двигаемся для того что бы избежать повторного построения одного и того же пути
	 * при followToCharacter используется для хранения мировых координат в которых находилась последний раз преследуемая цель для отслеживания необходимости перестраивания пути
	 */
	private final Location movingDestTempPos = new Location(0, 0, 0);

	protected final ArrayList<ArrayList<Location>> _targetRecorder = new ArrayList<ArrayList<Location>>();

	protected long _startMoveTime;
	protected double _previousSpeed = -1;

	private WeakReference<L2Character> followTarget;

	private int _heading;

	private long targetStoredId;
	private GCSArray<Long> dieEventList;

	/**
	 * Table of Calculators containing all used calculator
	 */
	private final Calculator[] _calculators;

	/**
	 * The link on the L2CharTemplate object containing generic and static
	 * properties of this L2Character type (ex : Max HP, Speed...)
	 */
	protected L2CharTemplate _template;
	protected final L2CharTemplate _baseTemplate;
	protected L2CharacterAI _ai;

	protected String _name;
	private String _title;
	private final byte[] _insideZones = new byte[ZoneType.values().length];
	private final byte[] _abnormalVE = new byte[AbnormalVisualEffect.values().length];
	private Location _prevLoc = null;

	private double _hpUpdateIncCheck = .0;
	private double _hpUpdateDecCheck = .0;
	private double _hpUpdateInterval = .0;

	/**
	 * Constructor of L2Character.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Each L2Character owns generic and static properties (ex : all Keltir have
	 * the same number of HP...). All of those properties are stored in a
	 * different template for each type of L2Character. Each template is loaded
	 * once in the server cache memory (reduce memory use). When a new instance
	 * of L2Character is spawned, server just create a link between the instance
	 * and the template This link is stored in <B>_template</B><BR>
	 * <BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the _template of the L2Character </li>
	 * <li>Set _overloaded to false (the charcater can take more items)</li>
	 * <BR>
	 * <BR>
	 * <p/>
	 * <li>If L2Character is a L2NpcInstance, copy skills from template to
	 * object</li>
	 * <li>If L2Character is a L2NpcInstance, link _calculators to
	 * NPC_STD_CALCULATOR</li>
	 * <BR>
	 * <BR>
	 * <p/>
	 * <li>If L2Character is NOT a L2NpcInstance, create an empty _skills slot</li>
	 * <li>If L2Character is a L2Player or L2Summon, copy basic Calculator set
	 * to object</li>
	 * <BR>
	 * <BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 * @param template The L2CharTemplate to apply to the object
	 */
	public L2Character(int objectId, L2CharTemplate template)
	{
		super(objectId);

		if(this instanceof L2CubicInstance)
		{
			StatsSet set = new StatsSet();

			// Base stats
			set.set("baseSTR", 0);
			set.set("baseCON", 0);
			set.set("baseDEX", 0);
			set.set("baseINT", 0);
			set.set("baseWIT", 0);
			set.set("baseMEN", 0);
			set.set("baseHpMax", 0.);
			set.set("baseCpMax", 0.);
			set.set("baseMpMax", 0.);
			set.set("baseHpReg", 0.);
			set.set("baseCpReg", 0.);
			set.set("baseMpReg", 0.);
			set.set("basePAtk", 0);
			set.set("baseMAtk", 0);
			set.set("basePDef", 0);
			set.set("baseMDef", 0);
			set.set("basePAtkSpd", 0);
			set.set("baseMAtkSpd", 0);
			set.set("baseMReuseDelay", 1.f);
			set.set("baseShldDef", 0);
			set.set("baseAtkRange", 0);
			set.set("baseShldRate", 0);
			set.set("baseCritRate", 0);
			set.set("baseRunSpd", 0);
			set.set("baseWalkSpd", 0);

			// Geometry
			set.set("collision_radius", 0);
			set.set("collision_height", 0);
			template = new L2CharTemplate(set);
		}

		// Set its template to the new L2Character
		_template = template;
		_baseTemplate = template;

		_calculators = new Calculator[Stats.NUM_STATS];
		_effects = new EffectList(this);

		if(template != null && (this instanceof L2NpcInstance || this instanceof L2Summon))
			if(((L2NpcTemplate) template).getSkills().size() > 0)
				for(L2Skill skill : ((L2NpcTemplate) template).getSkills().values())
					addSkill(skill);

		Formulas.addFuncsToNewCharacter(this);

		_moveTaskRunnable = new MoveNextTask(this); //FIXME check hasAI???
	}

	/**
	 * Abort the attack of the L2Character and send Server->Client ActionFailed
	 * packet.<BR>
	 * <BR>
	 */
	public final void abortAttack()
	{
		if(isAttackingNow())
		{
			_attackEndTime = 0;
			_isAttackAborted = true;
			sendActionFailed();
		}
	}

	/**
	 * Abort the cast of the L2Character and send Server->Client
	 * MagicSkillCanceld/ActionFailed packet.<BR>
	 * <BR>
	 */
	public final void abortCast()
	{
		if(isCastingNow())
		{
			_castEndTime = 0;
			_castInterruptTime = 0;
			if(_castingSkill != null)
				_skillMastery.remove(_castingSkill.getId());
			_castingSkill = null;
			_castingItem = null;
			if(_skillTask != null)
				_skillTask.cancel(false); // cancels the skill hit scheduled task
			if(_skillLaunchedTask != null)
				_skillLaunchedTask.cancel(true); // cancels the skill hit scheduled task
			if(_skillCoolTask != null)
				_skillCoolTask.cancel(true);

			if(_forceBuff != null)
				_forceBuff.delete();

			if(getEffectPoint() != null && getEffectPoint().getSkill().isCastTimeEffect())
				getEffectPoint().deleteMe();

			broadcastPacket(new MagicSkillCanceled(_objectId)); // broadcast packet to stop animations client-side
			sendActionFailed(); // send an "action failed" packet to the caster
			getAI().setIntention(AI_INTENTION_ACTIVE, null, null);
		}
	}

	/**
	 * Add QuestState instance that is to be notified of character's death.<BR>
	 * <BR>
	 *
	 * @param qs The QuestState that subscribe to this event
	 */
	public void addNotifyQuestOfDeath(QuestState qs)
	{
		if(qs == null || _NotifyQuestOfDeathList != null && _NotifyQuestOfDeathList.contains(qs))
			return;
		if(_NotifyQuestOfDeathList == null)
			_NotifyQuestOfDeathList = new GArray<QuestState>();
		_NotifyQuestOfDeathList.add(qs);
	}

	/**
	 * Add a skill to the L2Character _skills and its Func objects to the
	 * calculator set of the L2Character.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills own by a L2Character are identified in <B>_skills</B><BR>
	 * <BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Replace oldSkill by newSkill or Add the newSkill </li>
	 * <li>If an old skill has been replaced, remove all its Func objects of
	 * L2Character calculator set</li>
	 * <li>Add Func objects of newSkill to the calculator set of the
	 * L2Character </li>
	 * <BR>
	 * <BR>
	 * <p/>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li> L2Player : Save update in the character_skills table of the database</li>
	 * <li> L2Player : Add skill first time use reuse delay penalties</li>
	 * <BR>
	 * <BR>
	 *
	 * @param newSkill The L2Skill to add to the L2Character
	 * @return The L2Skill replaced or null if just added a new L2Skill
	 */
	public L2Skill addSkill(L2Skill newSkill)
	{
		if(newSkill == null)
			return null;

		if(_skills == null)
			_skills = new HashMap<Integer, L2Skill>();

		L2Skill oldSkill = _skills.get(newSkill.getId());

		if(oldSkill != null && oldSkill.getLevel() == newSkill.getLevel())
			return null;

		// Replace oldSkill by newSkill or Add the newSkill
		_skills.put(newSkill.getId(), newSkill);

		// If an old skill has been replaced, remove all its Func objects
		if(oldSkill != null)
			removeStatsOwner(oldSkill);

		if(newSkill.isPassive())
			addChanceSkill(newSkill);
		// Add Func objects of newSkill to the calculator set of the L2Character
		addStatFuncs(newSkill.getStatFuncs(this));

		return oldSkill;
	}

	public void addChanceSkill(L2Skill newSkill)
	{
		if(newSkill.isOnAttack() || newSkill.isOnCrit())
		{
			if(_skillsOnAttack == null)
				_skillsOnAttack = new ConcurrentHashMap<>();
			_skillsOnAttack.put(newSkill.getId(), newSkill);
		}

		if(newSkill.isOnMagicAttack())
		{
			if(_skillsOnMagicAttack == null)
				_skillsOnMagicAttack = new ConcurrentHashMap<>();
			_skillsOnMagicAttack.put(newSkill.getId(), newSkill);
		}

		if(newSkill.isOnUnderAttack() || newSkill.isOnMagicAttacked())
		{
			if(_skillsOnUnderAttack == null)
				_skillsOnUnderAttack = new ConcurrentHashMap<>();
			_skillsOnUnderAttack.put(newSkill.getId(), newSkill);
		}
		if(newSkill.isOnEvaded())
		{
			if(_skillsOnEvaded == null)
				_skillsOnEvaded = new ConcurrentHashMap<>();
			_skillsOnEvaded.put(newSkill.getId(), newSkill);
		}
		if(newSkill.isOnShield())
		{
			if(_skillsOnShield == null)
				_skillsOnShield = new ConcurrentHashMap<>();
			_skillsOnShield.put(newSkill.getId(), newSkill);
		}
		if(newSkill.isOnDamageReceived())
		{
			if(_skillsOnDamage == null)
				_skillsOnDamage = new ConcurrentHashMap<>();
			_skillsOnDamage.put(newSkill.getId(), newSkill);
		}
	}

	/**
	 * Add a Func to the Calculator set of the L2Character.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A L2Character owns a table of Calculators called <B>_calculators</B>.
	 * Each Calculator (a calculator per state) own a table of Func object. A
	 * Func object is a mathematic function that permit to calculate the
	 * modifier of a state (ex : REGENERATE_HP_RATE...). To reduce cache memory
	 * use, L2NpcInstances who don't have skills share the same Calculator set
	 * called <B>NPC_STD_CALCULATOR</B>.<BR>
	 * <BR>
	 * <p/>
	 * That's why, if a L2NpcInstance is under a skill/spell effect that modify
	 * one of its state, a copy of the NPC_STD_CALCULATOR must be create in its
	 * _calculators before addind new Func object.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>If _calculators is linked to NPC_STD_CALCULATOR, create a copy of
	 * NPC_STD_CALCULATOR in _calculators</li>
	 * <li>Add the Func object to _calculators</li>
	 * <BR>
	 * <BR>
	 *
	 * @param f The Func object to add to the Calculator corresponding to the
	 *          state affected
	 */
	public final synchronized void addStatFunc(Func f)
	{
		if(f == null)
			return;

		// Select the Calculator of the affected state in the Calculator set
		int stat = f._stat.ordinal();

		if(_calculators[stat] == null)
			_calculators[stat] = new Calculator();

		_calculators[stat].addFunc(f);
	}

	/**
	 * Add a list of Funcs to the Calculator set of the L2Character.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A L2Character owns a table of Calculators called <B>_calculators</B>.
	 * Each Calculator (a calculator per state) own a table of Func object. A
	 * Func object is a mathematic function that permit to calculate the
	 * modifier of a state (ex : REGENERATE_HP_RATE...). <BR>
	 * <BR>
	 * <p/>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method is ONLY for
	 * L2Player</B></FONT><BR>
	 * <BR>
	 * <p/>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li> Equip an item from inventory</li>
	 * <li> Learn a new passive skill</li>
	 * <li> Use an active skill</li>
	 * <BR>
	 * <BR>
	 *
	 * @param funcs The list of Func objects to add to the Calculator
	 *              corresponding to the state affected
	 */
	public final synchronized void addStatFuncs(Func[] funcs)
	{
		for(Func f : funcs)
			addStatFunc(f);
	}

	/**
	 * Add the object to the list of L2Character that must be informed of HP/MP
	 * updates of this L2Character.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Each L2Character owns a list called <B>_statusListener</B> that contains
	 * all L2Player to inform of HP/MP updates. Players who must be informed are
	 * players that target this L2Character. When a RegenTask is in progress
	 * sever just need to go through this list to send Server->Client packet
	 * StatusUpdate.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li> Target a PC or NPC</li>
	 * <BR>
	 * <BR>
	 *
	 * @param object L2Character to add to the listener
	 */
	public void addStatusListener(L2Character object)
	{
		if(object == this)
			return;
		CopyOnWriteArraySet<L2Character> listeners;
		synchronized(this)
		{
			if(_statusListener == null)
				_statusListener = new CopyOnWriteArraySet<L2Character>();

			listeners = _statusListener;
		}
		listeners.add(object);
	}

	public void altOnMagicUseTimer(L2Character aimingTarget, L2Skill skill, L2ItemInstance usedItem)
	{
		_altSkillTask = null;
		if(isAlikeDead())
			return;

		int magicId = skill.getDisplayId();
		int level = Math.max(1, isNpc() ? skill.getLevel() : getSkillDisplayLevel(magicId));
		List<L2Character> targets = skill.getTargets(this, aimingTarget, true);
		int objId = _objectId;
		if(isCubic())
		{
			L2Player owner = getPlayer();
			if(owner == null)
				return;
			objId = owner.getObjectId();
		}
		broadcastPacket(new MagicSkillLaunched(objId, magicId, level, targets, skill.isBuff()));
		double mpConsume2 = skill.getMpConsume2();
		if(mpConsume2 > 0)
		{
			if(_currentMp < mpConsume2)
			{
				sendPacket(Msg.NOT_ENOUGH_MP);
				return;
			}

			reduceCurrentMp(Formulas.calcSkillMpConsume(this, skill, mpConsume2, false), null);
		}

		//if(!skill.checkCondition(this, aimingTarget, usedItem, false, false))
		//	return;

		if(skill.getSoulsConsume() > 0)
			decreaseSouls(skill.getSoulsConsume());
		//setConsumedSouls(getConsumedSouls() - skill.getSoulsConsume(), null);

		callSkill(skill, targets, usedItem);

		removeSkillMastery(skill.getId());
	}

	public void altUseSkill(L2Skill skill, L2Character target)
	{
		altUseSkill(skill, target, null);
	}

	public void altUseSkill(L2Skill skill, L2Character target, L2ItemInstance usedItem)
	{
		if(skill == null)
			return;
		int magicId = skill.getId();
		int level = Math.max(1, isNpc() ? skill.getLevel() : getSkillDisplayLevel(magicId));

		if(isSkillDisabled(magicId))
		{
			if(Config.ALT_SHOW_REUSE_MSG && isPlayer())
				sendSkillReuseMessage(magicId, (short) level, usedItem);
			return;
		}

		if(skill.getIncreaseLevel() > 0 && target.getEffectBySkillId(skill.getId()) != null)
		{
			L2Effect ef = target.getEffectBySkillId(skill.getId());
			if(ef != null && ef.getSkillLevel() <= skill.getIncreaseLevel())
				skill = SkillTable.getInstance().getInfo(skill.getId(), ef.getSkillLevel() == skill.getIncreaseLevel() ? skill.getIncreaseLevel() : ef.getSkillLevel() + 1);
		}

		if(target == null)
		{
			target = skill.getAimingTarget(this);
			if(target == null)
				return;
		}

		fireMethodInvoked(MethodCollection.onStartAltCast, new Object[]{skill, target, usedItem});

		// Не показывать сообщение для хербов и кубиков
		if(!skill.isForCubic() && !skill.isHerb())
		{
			if(skill.altUse() && usedItem != null)
				sendPacket(new SystemMessage(SystemMessage.USE_S1).addItemName(usedItem.getItemId()));
			else if(usedItem != null && usedItem.getItemType().equals(L2EtcItem.EtcItemType.PET_COLLAR))
			{
				sendPacket(Msg.SUMMON_A_PET);
				_log.info(this + " summon a pet: " + usedItem);
			}
			else
				sendPacket(new SystemMessage(SystemMessage.YOU_USE_S1).addSkillName(magicId));
		}

		int itemConsume[] = skill.getItemConsume();

		if(itemConsume[0] > 0)
			for(int i = 0; i < itemConsume.length; i++)
				if(!consumeItem(skill.getItemConsumeId()[i], itemConsume[i], !skill.altUse()))
				{
					if(skill.altUse())
						sendPacket(Msg.INCORRECT_ITEM_COUNT);
					sendChanges();
					return;
				}

		boolean groupDelay = usedItem != null && usedItem.getItem().getDelayShareGroup() > 0;
		long reuseDelay = groupDelay ? usedItem.getItem().getReuseDelay() : Formulas.calcSkillReuseDelay(this, skill);

		if(!skill.isToggle())
			broadcastPacket(isInAirShip() ? new ExMagicSkillUseInAirShip(this.isCubic() ? getPlayer() : this, target, skill.getDisplayId(), level, skill.getHitTime(), reuseDelay, skill.isBuff()) : new MagicSkillUse(this.isCubic() ? getPlayer() : this, target, skill.getDisplayId(), level, skill.getHitTime(), reuseDelay, skill.isBuff()));

		// Skill reuse check
		if(groupDelay)
		{
			disableSkill(-usedItem.getItem().getDelayShareGroup(), reuseDelay);
			sendPacket(new ExUseSharedGroupItem(usedItem.getItemId(), usedItem.getItem().getDelayShareGroup(), (int) (reuseDelay / 1000), (int) (reuseDelay / 1000)));
		}
		else
			disableSkill(skill.getId(), reuseDelay);

		if(reuseDelay > 10)
			disableSkill(groupDelay ? -usedItem.getItem().getDelayShareGroup() : skill.getId(), reuseDelay);

		_castingItem = usedItem;

		if(skill.isToggle())
			altOnMagicUseTimer(target, skill, usedItem);
		else
			_altSkillTask = ThreadPoolManager.getInstance().scheduleAi(new AltMagicUseTask(target, skill, usedItem), skill.getHitTime(), isPlayer());
	}

	/**
	 * Break an attack and send Server->Client ActionFailed packet and a System
	 * Message to the L2Character.<BR>
	 * <BR>
	 */
	public void breakAttack()
	{
		if(isAttackingNow())
		{
			abortAttack();

			if(isPlayer())
				sendPacket(Msg.ATTACK_FAILED);
		}
	}

	/**
	 * Break a cast and send Server->Client ActionFailed packet and a System
	 * Message to the L2Character.<BR>
	 * <BR>
	 *
	 * @param force
	 */
	public void breakCast(boolean force, boolean message)
	{
		if(isCastingNow() && (force || canAbortCast()))
		{
			abortCast();

			if(message && isPlayer())
				sendPacket(new SystemMessage(SystemMessage.S1S_CASTING_HAS_BEEN_INTERRUPTED).addCharName(this));
		}
	}

	/**
	 * Send a packet to the L2Character AND to all L2Player in the _KnownPlayers
	 * of the L2Character.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * L2Player in the detection area of the L2Character are identified in
	 * <B>_knownPlayers</B>. In order to inform other players of state
	 * modification on the L2Character, server just need to go through
	 * _knownPlayers to send Server->Client Packet<BR>
	 * <BR>
	 */
	public void broadcastPacket(L2GameServerPacket mov)
	{
		sendPacket(mov);
		broadcastPacketToOthers(mov);
	}

	/**
	 * Send a packet to all L2Player in the _KnownPlayers of the L2Character.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * L2Player in the detection area of the L2Character are identified in
	 * <B>_knownPlayers</B>. In order to inform other players of state
	 * modification on the L2Character, server just need to go through
	 * _knownPlayers to send Server->Client Packet<BR>
	 * <BR>
	 * <p/>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND
	 * Server->Client packet to this L2Character (to do this use function
	 * broadcastPacket)</B></FONT><BR>
	 * <BR>
	 */
	public final void broadcastPacketToOthers(L2GameServerPacket mov)
	{
		if(!isVisible())
			return;

		for(L2Player target : L2World.getAroundPlayers(this))
			if(_objectId != target.getObjectId() && (!target.isNotShowBuffAnim() || !(mov instanceof MagicSkillUse && ((MagicSkillUse) mov).isBuffPacket() || mov instanceof MagicSkillLaunched && ((MagicSkillLaunched) mov).isBuffPacket())))
				target.sendPacket(mov);
	}

	public final void broadcastPacket(L2GameServerPacket mov, int range)
	{
		if(!isVisible())
			return;

		for(L2Player target : L2World.getAroundPlayers(this, range, Config.PLAYER_VISIBILITY_Z))
			if(_objectId != target.getObjectId() && (!target.isNotShowBuffAnim() || !(mov instanceof MagicSkillUse && ((MagicSkillUse) mov).isBuffPacket() || mov instanceof MagicSkillLaunched && ((MagicSkillLaunched) mov).isBuffPacket())))
				target.sendPacket(mov);
	}

	/**
	 * Send the Server->Client packet StatusUpdate with current HP and MP to all
	 * other L2Player to inform.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Create the Server->Client packet StatusUpdate with current HP and MP
	 * </li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP
	 * to all L2Character called _statusListener that must be informed of HP/MP
	 * updates of this L2Character </li>
	 * <BR>
	 * <BR>
	 * <p/>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND CP
	 * information</B></FONT><BR>
	 * <BR>
	 * <p/>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li> L2Player : Send current HP,MP and CP to the L2Player and only
	 * current HP, MP and Level to all other L2Player of the Party</li>
	 * <BR>
	 * <BR>
	 */
	public void broadcastStatusUpdate()
	{
		CopyOnWriteArraySet<L2Character> list = _statusListener;

		if(list == null || list.isEmpty())
			return;

		if(!needHpUpdate(352))
		{
			if(Config.DEBUG)
				_log.info("" + getName() + ": saving statusUpdate.");
			return;
		}

		StatusUpdate su = new StatusUpdate(_objectId);
		su.addAttribute(StatusUpdate.CUR_HP, (int) _currentHp);
		su.addAttribute(StatusUpdate.CUR_MP, (int) _currentMp);
		su.addAttribute(StatusUpdate.CUR_CP, (int) _currentCp);

		for(L2Character temp : list)
			if(!Config.FORCE_STATUSUPDATE)
			{
				if(temp.getTarget() == this)
					temp.sendPacket(su);
			}
			else
				temp.sendPacket(su);
	}

	public int calcHeading(Location loc)
	{
		return calcHeading(loc.getX(), loc.getY());
	}

	public int calcHeading(int x_dest, int y_dest)
	{
		if(x_dest == getX() && y_dest == getY())
			return getHeading();
		return (int) (Math.atan2(getY() - y_dest, getX() - x_dest) * HEADINGS_IN_PI) + 32768;
	}

	/**
	 * Calculate the new value of the state with modifiers that will be applied
	 * on the targeted L2Character.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A L2Character owns a table of Calculators called <B>_calculators</B>.
	 * Each Calculator (a calculator per state) own a table of Func object. A
	 * Func object is a mathematic function that permit to calculate the
	 * modifier of a state (ex : REGENERATE_HP_RATE...) : <BR>
	 * <BR>
	 * <p/>
	 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<BR>
	 * <BR>
	 * <p/>
	 * When the calc method of a calculator is launched, each mathematic
	 * function is called according to its priority <B>_order</B>. Indeed, Func
	 * with lowest priority order is executed firsta and Funcs with the same
	 * order are executed in unspecified order. The result of the calculation is
	 * stored in the value property of an Env class instance.<BR>
	 * <BR>
	 *
	 * @param stat   The stat to calculate the new value with modifiers
	 * @param init   The initial value of the stat before applying modifiers
	 * @param object The L2Charcater whose properties will be used in the
	 *               calculation (ex : CON, INT...)
	 * @param skill  The L2Skill whose properties will be used in the calculation
	 *               (ex : Level...)
	 */
	public final double calcStat(Stats stat, double init, L2Character object, L2Skill skill)
	{
		int id = stat.ordinal();
		Calculator c = _calculators[id];
		// If no Func object found, no modifier is applied
		if(c == null || c.size() == 0)
			return init;
		// Create and init an Env object to pass parameters to the Calculator
		Env env = new Env(this, object, skill);
		env.value = init;
		// Launch the calculation
		c.calc(env);
		if(env.value < 1 && (stat == Stats.STAT_CON || stat == Stats.STAT_DEX || stat == Stats.STAT_INT || stat == Stats.STAT_MEN || stat == Stats.STAT_STR || stat == Stats.STAT_WIT))
			env.value = 1;
		else if(env.value < 0 && stat != Stats.FIRE_ATTRIBUTE && stat != Stats.WATER_ATTRIBUTE && stat != Stats.WIND_ATTRIBUTE && stat != Stats.EARTH_ATTRIBUTE && stat != Stats.HOLY_ATTRIBUTE && stat != Stats.DARK_ATTRIBUTE)
			env.value = 0;

		return env.value;
	}

	public final Calculator getCalculator(Stats stat)
	{
		return _calculators[stat.ordinal()];
	}

	/**
	 * Return the Attack Speed of the L2Character (delay (in milliseconds)
	 * before next attack).<BR>
	 * <BR>
	 */
	public int calculateAttackSpeed()
	{
		return Formulas.calcPAtkSpd(getPAtkSpd());
	}

	public void callSkill(L2Skill skill, List<L2Character> targets, L2ItemInstance usedItem)
	{
		callSkill(skill, targets, usedItem, false);
	}

	public void callSkill(L2Skill skill, List<L2Character> targets, L2ItemInstance usedItem, boolean counter)
	{
		try
		{
			boolean needsOverhitFlags = skill.isOverhit();

			for(L2Character target : targets)
				if(target != null && target.isMonster() && needsOverhitFlags)
					target.setOverhitEnabled(true);

			if(skill.isOffensive() && _skillsOnMagicAttack != null && _skillsOnMagicAttack.size() > 0)
				for(L2Skill chanceSkill : _skillsOnMagicAttack.values())
					chanceSkill.useChanceSkill(new Env(this, chanceSkill.getAimingTarget(this), skill));

			Calculator triggerBySkill = _calculators[Stats.TRIGGER_BY_SKILL.ordinal()];
			if(triggerBySkill != null && triggerBySkill.size() > 0)
			{
				Env env = new Env(this, skill.getAimingTarget(this), skill);
				for(Func func : triggerBySkill.getFunctions())
					func.calc(env);
			}

			skill.useSkill(this, targets, usedItem, counter);

			for(L2Character target : targets)
				if(target != null && target.isNpc())
					ThreadPoolManager.getInstance().executeAi(new NotifyAITask(target, CtrlEvent.EVT_SPELLED, skill, this), false);

			if(skill.isOffensive())
				startAttackStanceTask();
		}
		catch(Exception e)
		{
			_log.warn("", e);
		}
	}

	/**
	 * Return True if the cast of the L2Character can be aborted.<BR>
	 * <BR>
	 */
	public final boolean canAbortCast()
	{
		return _castInterruptTime > System.currentTimeMillis();
	}

	public boolean checkReflectDebuffSkill(L2Skill skill)
	{
		return skill.isDebuff() && (skill.isMagic() || skill.isPhysic()) && Rnd.chance((int) calcStat(skill.isMagic() ? Stats.REFLECT_MAGIC_SKILL : Stats.REFLECT_PHYSIC_SKILL, 0, null, skill));
	}

	public boolean checkReflectMeleeSkill(L2Skill skill)
	{
		return Rnd.chance((int) calcStat(Stats.REFLECT_MELEE_SKILL, 0, null, skill));
	}

	public void doCounterAttack(L2Skill skill, L2Character target)
	{
		if(!skill.isPhysic() || !skill.isOffensive() || skill.getCastRange() > 100)
			return;

		if(Rnd.chance((int) calcStat(Stats.COUNTER_ATTACK, 0, null, skill)))
		{
			target.sendPacket(new SystemMessage(SystemMessage.S1_IS_PERFORMING_A_COUNTER_ATTACK).addCharName(this));
			sendPacket(new SystemMessage(SystemMessage.YOU_COUNTER_ATTACK_S1_S_ATTACK).addCharName(target));
			List<L2Character> targets = new ArrayList<>();
			targets.add(target);

			callSkill(skill, targets, null, true);
			if(skill.hasEffect("i_death"))
			{
				target.sendPacket(new SystemMessage(SystemMessage.S1_IS_PERFORMING_A_COUNTER_ATTACK).addCharName(this));
				sendPacket(new SystemMessage(SystemMessage.YOU_COUNTER_ATTACK_S1_S_ATTACK).addCharName(target));
				callSkill(skill, targets, null, true);
			}
		}
	}

	public synchronized void detachAI()
	{
		if(_ai != null)
			_ai.stopAITask();
		_ai = null;
	}

	public final void disableDrop(int time)
	{
		_dropDisabled = System.currentTimeMillis() + time;
	}

	/**
	 * Disable this skill id for the duration of the delay in milliseconds.
	 *
	 * @param skillId
	 * @param delay   (seconds * 1000)
	 */
	public void disableSkill(int skillId, long delay)
	{
		disableSkill(skillId, delay, System.currentTimeMillis() + delay);
	}

	public void disableSkill(int skillId, long delay, long endTime)
	{
		if(disabledSkills == null)
			disabledSkills = new ConcurrentHashMap<>();

		disabledSkills.put(skillId, new TimeStamp(skillId, delay, endTime));
	}

	/**
	 * Enable a skill (remove it from disabledSkills of the L2Character).<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills disabled are identified by their skillId in <B>disabledSkills</B>
	 * of the L2Character <BR>
	 * <BR>
	 *
	 * @param skillId The identifier of the L2Skill to enable
	 */
	public void enableSkill(Integer skillId)
	{
		if(disabledSkills == null)
			return;

		disabledSkills.remove(skillId);
	}

	public void enableAllSkills()
	{
		if(disabledSkills == null)
			return;

		disabledSkills.clear();
	}

	public boolean isSkillDisabled(L2Skill skill)
	{
		return skill != null && isSkillDisabled(skill.getId());
	}

	public boolean isSkillDisabledEx(int skillIndex)
	{
		return isSkillDisabled(skillIndex >> 16);
	}

	public boolean isSkillDisabled(int skillId)
	{
		if(disabledSkills == null)
			return false;

		TimeStamp timeStamp = disabledSkills.get(skillId);
		if(timeStamp == null)
			return false;
		if(timeStamp.hasNotPassed())
			return true;

		disabledSkills.remove(skillId);
		return false;
	}

	public Collection<TimeStamp> getDisabledSkills()
	{
		if(disabledSkills == null)
			return Collections.emptyList();

		return disabledSkills.values();
	}

	public void doAttack(L2Character target)
	{
		if(isAttackingNow())
			return;

		if(target == null)
			return;

		// Get the active weapon item corresponding to the active weapon
		// instance (always equipped in the right hand)
		L2Weapon weaponItem = getActiveWeaponItem();

		if(isAlikeDead() || target.isAlikeDead() || !isInRange(target, 2000) || (target instanceof L2Playable && ((L2Playable) target).getDuelState() == Duel.DUELSTATE_DEAD))
			return;

		fireMethodInvoked(MethodCollection.onStartAttack, new Object[]{this, target});

		// Get the Attack Speed of the L2Character (delay (in milliseconds)
		// before next attack)
		// лимит в 0.2 секунды означает скорость атаки 2500
		int sAtk = Math.max(calculateAttackSpeed(), 200) - 50;

		// Get the Attack Reuse Delay of the L2Weapon
		int reuse = (int) (weaponItem != null ? weaponItem.attackReuse * 331.5 / getPAtkSpd() : 0);
		if(reuse > 0)
			reuse += sAtk;

		_attackEndTime = sAtk + System.currentTimeMillis();
		_attackReuseEndTime = reuse + System.currentTimeMillis();

		// Create a Server->Client packet Attack
		Attack attack = new Attack(this, target, getChargedSoulShot(), weaponItem != null ? weaponItem.getCrystalType().externalOrdinal : 0);

		_isAttackAborted = false;

		setHeading(target, true);

		// Select the type of attack to start
		if(weaponItem == null)
			doAttackHitSimple(attack, target, weaponItem, 1., true, sAtk);
		else
			switch(weaponItem.getItemType())
			{
				case BOW:
				case CROSSBOW:
					doAttackHitByBow(attack, target, sAtk, reuse, weaponItem);
					break;
				case POLE:
					if(calcStat(Stats.POLE_ATTACK_ANGLE, 0, null, null) == -1)
						doAttackHitSimple(attack, target, weaponItem, 1., true, sAtk);
					else
						doAttackHitByPole(attack, weaponItem, sAtk);
					break;
				case DUAL:
				case DUALFIST:
				case DUALDAGGER:
					doAttackHitByDual(attack, target, weaponItem, sAtk);
					break;
				default:
					doAttackHitSimple(attack, target, weaponItem, 1., true, sAtk);
			}

		// If the Server->Client packet Attack contains at least 1 hit, send the
		// Server->Client packet Attack
		// to the L2Character AND to all L2Player in the _KnownPlayers of the
		// L2Character
		if(attack.hasHits())
			broadcastPacket(attack);

		// Для кайтинга с луком
		if(reuse > 0)
			ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(this, CtrlEvent.EVT_READY_TO_ACT, null, null), sAtk + 10, isPlayer());
		ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(this, CtrlEvent.EVT_READY_TO_ACT, null, null), Math.max(reuse, sAtk) + 10, isPlayer());
	}

	/**
	 * Launch a Bow attack.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Calculate if hit is missed or not </li>
	 * <li>Consumme arrows </li>
	 * <li>If hit isn't missed, calculate if shield defense is efficient </li>
	 * <li>If hit isn't missed, calculate if hit is critical </li>
	 * <li>If hit isn't missed, calculate physical damages </li>
	 * <li>If the L2Character is a L2Player, Send a Server->Client packet
	 * SetupGauge </li>
	 * <li>Create a new hit task with Medium priority</li>
	 * <li>Calculate and set the disable delay of the bow in function of the
	 * Attack Speed</li>
	 * <li>Add this hit to the Server-Client packet Attack </li>
	 * <BR>
	 * <BR>
	 *
	 * @param attack Server->Client packet Attack in which the hit will be added
	 * @param target The L2Character targeted
	 * @param sAtk   The Attack Speed of the attacker
	 * @param weapon L2Weapon item used by this
	 * @return True if the hit isn't missed
	 */
	protected boolean doAttackHitByBow(Attack attack, L2Character target, int sAtk, int reuse, L2Weapon weapon)
	{
		int damage1 = 0;
		boolean shld1 = false;
		boolean crit1 = false;

		// Calculate if hit is missed or not
		boolean miss1 = Formulas.calcHitMiss(this, target);

		reduceArrowCount();

		isMoving = false;

		if(!miss1)
		{
			shld1 = Formulas.calcShldUse(this, target);
			crit1 = Formulas.calcCrit(this, target, getCriticalHit(target, null) * 0.01 * target.calcStat(Stats.CRIT_CHANCE_RECEPTIVE, 100, null, null));
			damage1 = (int) Formulas.calcPhysDam(this, target, shld1, crit1, false, attack._soulshot);
			if(crit1 && weapon != null)
				weapon.getEffect(true, this, target, true);
		}

		if(isPlayer())
		{
			sendPacket(Msg.GETTING_READY_TO_SHOOT_ARROWS);
			sendPacket(new SetupGauge(SetupGauge.RED, reuse));
		}

		// Create a new hit task with Medium priority
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(this, target, damage1, crit1, miss1, attack._soulshot, shld1, true), sAtk, isPlayer());

		attack.addHit(target, damage1, miss1, crit1, shld1);

		return !miss1;
	}

	/**
	 * Launch a Dual attack.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Calculate if hits are missed or not </li>
	 * <li>If hits aren't missed, calculate if shield defense is efficient
	 * </li>
	 * <li>If hits aren't missed, calculate if hit is critical </li>
	 * <li>If hits aren't missed, calculate physical damages </li>
	 * <li>Create 2 new hit tasks with Medium priority</li>
	 * <li>Add those hits to the Server-Client packet Attack </li>
	 * <BR>
	 * <BR>
	 *
	 * @param attack Server->Client packet Attack in which the hit will be added
	 * @param target The L2Character targeted
	 * @param weapon L2Weapon item used by this
	 * @return True if hit 1 or hit 2 isn't missed
	 */
	protected boolean doAttackHitByDual(Attack attack, L2Character target, L2Weapon weapon, int sAtk)
	{
		int damage1 = 0;
		int damage2 = 0;
		boolean shld1 = false;
		boolean shld2 = false;
		boolean crit1 = false;
		boolean crit2 = false;

		boolean miss1 = Formulas.calcHitMiss(this, target);
		boolean miss2 = Formulas.calcHitMiss(this, target);

		if(!miss1)
		{
			shld1 = Formulas.calcShldUse(this, target);
			crit1 = Formulas.calcCrit(this, target, getCriticalHit(target, null) * 0.01 * target.calcStat(Stats.CRIT_CHANCE_RECEPTIVE, 100, null, null));
			damage1 = (int) Formulas.calcPhysDam(this, target, shld1, crit1, true, attack._soulshot);
			if(crit1 && weapon != null)
				weapon.getEffect(true, this, target, true);
		}

		if(!miss2)
		{
			shld2 = Formulas.calcShldUse(this, target);
			crit2 = Formulas.calcCrit(this, target, getCriticalHit(target, null) * 0.01 * target.calcStat(Stats.CRIT_CHANCE_RECEPTIVE, 100, null, null));
			damage2 = (int) Formulas.calcPhysDam(this, target, shld2, crit2, true, attack._soulshot);
			if(crit2 && weapon != null)
				weapon.getEffect(true, this, target, true);
		}

		// Create a new hit task with Medium priority for hit 1 and for hit 2
		// with a higher delay
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(this, target, damage1, crit1, miss1, attack._soulshot, shld1, true), sAtk / 2, isPlayer());
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(this, target, damage2, crit2, miss2, attack._soulshot, shld2, false), sAtk, isPlayer());

		attack.addHit(target, damage1, miss1, crit1, shld1);
		attack.addHit(target, damage2, miss2, crit2, shld2);
		return !miss1 || !miss2;
	}

	/**
	 * Launch a Pole attack.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get all visible objects in a spheric area near the L2Character to
	 * obtain possible targets </li>
	 * <li>If possible target is the L2Character targeted, launch a simple
	 * attack against it </li>
	 * <li>If possible target isn't the L2Character targeted but is attakable,
	 * launch a simple attack against it </li>
	 * <BR>
	 * <BR>
	 *
	 * @param attack Server->Client packet Attack in which the hit will be added
	 * @param weapon L2Weapon item used by this
	 * @return True if one hit isn't missed
	 */
	protected boolean doAttackHitByPole(Attack attack, L2Weapon weapon, int sAtk)
	{
		boolean hitted = false;
		int angle = (int) calcStat(Stats.POLE_ATTACK_ANGLE, 60, null, null);
		int range = (int) calcStat(Stats.POWER_ATTACK_RANGE, getTemplate().baseAtkRange, null, null);
		double mult = 1.;

		int attackcount = 0;
		// Используем Math.round т.к. обычный кастинг обрезает к меньшему
		// double d = 2.95. int i = (int)d, выйдет что i = 2
		// если 1% угла или 1 дистанции не играет огромной роли, то для
		// количества целей это критично
		int attackcountmax = (int) Math.round(calcStat(Stats.POLE_TARGET_COUNT, 3, null, null));

		if(isRaid())
			attackcountmax += 12;
		else if(this instanceof L2NpcInstance && getLevel() > 0)
			attackcountmax += getLevel() / 7.5;

		L2Character targ = getAI().getAttackTarget();
		if(targ != null && !targ.isDead() && isInFront(targ, angle))
			if(doAttackHitSimple(attack, targ, weapon, mult, true, sAtk))
			{
				mult *= 0.85;
				attackcount++;
				hitted = true;
			}

		for(L2Character target : getKnownCharacters(range, 400))
			if(attackcount < attackcountmax)
			{
				if(target != null && !target.isDead() && target.isAttackable(this, false, false) && (!isNpc() || !target.isNpc()))
				{
					if(target == getAI().getAttackTarget() || !isInFront(target, angle))
						continue;

					hitted |= doAttackHitSimple(attack, target, weapon, mult, attackcount == 0, sAtk);
					mult *= 0.85;
					attackcount++;
				}
			}
			else
				break;

		// Return true if one hit isn't missed
		return hitted;
	}

	/**
	 * Launch a simple attack.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Calculate if hit is missed or not </li>
	 * <li>If hit isn't missed, calculate if shield defense is efficient </li>
	 * <li>If hit isn't missed, calculate if hit is critical </li>
	 * <li>If hit isn't missed, calculate physical damages </li>
	 * <li>Create a new hit task with Medium priority</li>
	 * <li>Add this hit to the Server-Client packet Attack </li>
	 * <BR>
	 * <BR>
	 *
	 * @param attack	 Server->Client packet Attack in which the hit will be added
	 * @param target	 The L2Character targeted
	 * @param weapon	 L2Weapon item used by <b>this</b>
	 * @param multiplier damage multiplier for polearmers
	 * @return True if the hit isn't missed
	 */
	protected boolean doAttackHitSimple(Attack attack, L2Character target, L2Weapon weapon, double multiplier, boolean unchargeSS, int sAtk)
	{
		int damage1 = 0;
		boolean shld1 = false;
		boolean crit1 = false;

		// Calculate if hit is missed or not
		boolean miss1 = Formulas.calcHitMiss(this, target);

		// Check if hit isn't missed
		if(!miss1)
		{
			// Calculate if shield defense is efficient
			shld1 = Formulas.calcShldUse(this, target);

			// Calculate if hit is critical
			crit1 = Formulas.calcCrit(this, target, getCriticalHit(target, null) * 0.01 * target.calcStat(Stats.CRIT_CHANCE_RECEPTIVE, 100, null, null));

			// Calculate physical damages
			damage1 = (int) Formulas.calcPhysDam(this, target, shld1, crit1, false, attack._soulshot);
			damage1 *= multiplier;

			if(crit1 && weapon != null)
				weapon.getEffect(true, this, target, true);
		}

		// Create a new hit task with Medium priority
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(this, target, damage1, crit1, miss1, attack._soulshot, shld1, unchargeSS), sAtk / 2, isPlayer());

		// Add this hit to the Server-Client packet Attack
		attack.addHit(target, damage1, miss1, crit1, shld1);

		// Return true if hit isn't missed
		return !miss1;
	}

	public long getAnimationEndTime()
	{
		return _animationEndTime;
	}

	public void doCast(L2Skill skill, L2Character target, boolean forceUse)
	{
		doCast(skill, target, null, forceUse);
	}

	public void doCast(L2Skill skill, L2Character target, L2ItemInstance usedItem, boolean forceUse)
	{	
		if(skill == null)
		{
			sendActionFailed();
			return;
		}

		if(skill.isToggle() && getEffectBySkill(skill) != null)
		{
			stopEffect(skill.getId());
			return;
		}

		int magicId = skill.getId();

		if(magicId == L2Skill.BLINDING_BLOW)
			stopEffect(magicId);

		if(target == null)
		{
			target = skill.getAimingTarget(this);
			if(target == null)
				return;
		}

		fireMethodInvoked(MethodCollection.onStartCast, new Object[]{skill, target, usedItem, forceUse});

		int level = isNpc() ? skill.getLevel() : getSkillDisplayLevel(magicId);
		if(level < 1)
			level = 1;

		if(skill.getIncreaseLevel() > 0 && target.getEffectBySkillId(skill.getId()) != null)
		{
			L2Effect ef = target.getEffectBySkillId(skill.getId());
			if(ef != null && ef.getSkillLevel() <= skill.getIncreaseLevel())
				skill = SkillTable.getInstance().getInfo(skill.getId(), ef.getSkillLevel() == skill.getIncreaseLevel() ? skill.getIncreaseLevel() : ef.getSkillLevel() + 1);
		}

		if(isPlayer())
		{
			if(usedItem != null && usedItem.getItemType().equals(L2EtcItem.EtcItemType.PET_COLLAR))
				sendPacket(Msg.SUMMON_A_PET);
			else
				sendPacket(new SystemMessage(SystemMessage.YOU_USE_S1).addSkillName(magicId));
		}

		int itemConsume[] = skill.getItemConsume();

		if(itemConsume[0] > 0)
			for(int i = 0; i < itemConsume.length; i++)
				if(!consumeItem(skill.getItemConsumeId()[i], itemConsume[i], !skill.isPotion()))
				{
					sendPacket(Msg.INCORRECT_ITEM_COUNT);
					sendChanges();
					return;
				}

		double speed = Formulas.calcCastSpeedFactor(this, skill);
		int hitTime = (int) (skill.getHitTime() / speed);
		int skillInterruptTime = Math.max(hitTime - (int) Math.min(skill.getHitCancelTime() / speed, 500), 0);
		int coolTime = skill.isCastTimeEffect() ? skill.getCoolTime() : (int) (skill.getCoolTime() / speed);
		
		//if(coolTime<100) {
		//	coolTime=100;
		//}

		_animationEndTime = System.currentTimeMillis() + hitTime;

		
		if(skill.isMagic() && !skill.isStaticHitTime() && getChargedSpiritShot() > 0)
		{
			hitTime *= 0.70;
			skillInterruptTime *= 0.70;
		}
        

		_castEndTime = System.currentTimeMillis() + hitTime + coolTime + 20;
		if(skill.isMagic())
			_castInterruptTime = System.currentTimeMillis() + skillInterruptTime;

		Formulas.calcSkillMastery(skill, this); // Calculate skill mastery for
		// current cast
		boolean groupDelay = usedItem != null && usedItem.getItem().getDelayShareGroup() > 0;
		long reuseDelay = groupDelay ? usedItem.getItem().getReuseDelay() : Formulas.calcSkillReuseDelay(this, skill);

		//if(reuseDelay < 500)
		//	reuseDelay = 500;

		if(isPlayer() && getPlayer().getGroundSkillLoc() != null)
		{
			setHeading(calcHeading(getPlayer().getGroundSkillLoc().getX(), getPlayer().getGroundSkillLoc().getY()));
			broadcastPacket(new ValidateLocation(this));
		}
		else
			setHeading(target, true);

		if(!skill.isToggle())
		{
			if(skill.isCastTimeEffect())
			{
				_castInterruptTime += coolTime;
				broadcastPacket(isInAirShip() ? new ExMagicSkillUseInAirShip(this, target, skill.getDisplayId(), level, hitTime + coolTime, reuseDelay, skill.isBuff()) : new MagicSkillUse(this, target, skill.getDisplayId(), level, hitTime + coolTime, reuseDelay, skill.isBuff()));
			}
			else
				broadcastPacket(isInAirShip() ? new ExMagicSkillUseInAirShip(this, target, skill.getDisplayId(), level, hitTime, reuseDelay, skill.isBuff()) : new MagicSkillUse(this, target, skill.getDisplayId(), level, hitTime, reuseDelay, skill.isBuff()));
		}

		if(skill.getSkillTargetType() == L2Skill.TargetType.holything)
			target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, this, 1, skill);

		// Skill reuse check
		if(groupDelay)
		{
			disableSkill(groupDelay ? -usedItem.getItem().getDelayShareGroup() : skill.getId(), reuseDelay);
			sendPacket(new ExUseSharedGroupItem(usedItem.getItemId(), usedItem.getItem().getDelayShareGroup(), (int) (reuseDelay / 1000), (int) (reuseDelay / 1000)));
		}
		else
			disableSkill(skill.getId(), reuseDelay);

		if(reuseDelay > 10)
			disableSkill(groupDelay ? -usedItem.getItem().getDelayShareGroup() : skill.getId(), reuseDelay);

		double mpConsume1 = skill.getMpConsume1();

		if(mpConsume1 > 0)
			reduceCurrentMp(Formulas.calcSkillMpConsume(this, skill, skill.getMpConsume1(), true), null);

		if(skill.getMpUsage() > 0 && isPlayer())
		{
			L2ItemInstance item = getPlayer().getInventory().getEquippedItemBySkill(skill);
			if(item != null)
				item.consumeMana(skill.getMpUsage(), getPlayer());
		}

		_castingItem = usedItem;
		// launch the magic in skillTime milliseconds
		if(hitTime > 50 && skill.getCastType() != L2Skill.CastType.instant)
		{
			// Send a Server->Client packet SetupGauge with the color of the
			// gauge and the casting time
			if(isPlayer() && hitTime > 200)
				sendPacket(new SetupGauge(SetupGauge.BLUE, hitTime));

			// Create a task MagicUseTask with Medium priority to launch the
			// MagicSkill at the end of the casting time
			_skillTask = ThreadPoolManager.getInstance().scheduleAi(new MagicUseTask(this, target, skill, usedItem, forceUse), hitTime, isPlayer());
			if(!skill.isCastTimeEffect())
				_skillLaunchedTask = ThreadPoolManager.getInstance().scheduleAi(new MagicLaunchedTask(this, forceUse), (int) (hitTime * 0.7), isPlayer());
		}
		else
		{
			setCastingSkill(skill);
			setCastingTarget(target);
			onMagicUseTimer(target, skill, usedItem, forceUse);
		}
	}

	public void startForceBuff(L2Character target, L2Skill skill)
	{
		if(_forceBuff == null)
			_forceBuff = new ForceBuff(this, target, skill);
	}

	public ForceBuff getForceBuff()
	{
		return _forceBuff;
	}

	public void setForceBuff(ForceBuff value)
	{
		_forceBuff = value;
	}

	public void doDie(L2Character killer)
	{
		// Set target to null and cancel Attack or Cast
		setTarget(null);

		// Stop movement
		stopMove();

		// Stop HP/MP/CP Regeneration task
		stopHpMpRegeneration();

		_currentHp = 0;

		if(isPlayer() && killer instanceof L2Playable)
			_currentCp = 0;

		if(getEffectPoint() != null)
			getEffectPoint().deleteMe();

		// Send the Server->Client packet StatusUpdate with current HP and MP to
		// all other L2Player to inform
		broadcastStatusUpdate();

		boolean charm = isPlayer() && ((L2Player) this).isCharmOfCourage() && isInSiege() &&
				(SiegeManager.getSiege(this) != null && (SiegeManager.getSiege(this).checkIsAttacker(getClanId()) || SiegeManager.getSiege(this).checkIsDefender(getClanId())) ||
						TerritoryWarManager.getWar().isInProgress() && getTerritoryId() > 0);

		if(charm)
			broadcastPacket(new MagicSkillUse(this, this, 5662, 1, 60000, 60000, false));
		// Notify L2Character AI
		getAI().notifyEvent(CtrlEvent.EVT_DEAD, killer, null);

		fireMethodInvoked(MethodCollection.doDie, new Object[]{killer});

		if(killer != null)
			killer.fireMethodInvoked(MethodCollection.onKill, new Object[]{this});

		if(charm)
		{
			if(!isActionBlocked(L2Zone.BLOCKED_SKILL_RESURRECT))
				((L2Player) this).reviveRequest((L2Player) this, 100, false, true);
			((L2Player) this).setCharmOfCourage(false);
			if(!isBlessedByNoblesse() && !isSalvation())
				stopEffects();
			else
			{
				for(L2Effect e : getAllEffects())
					if(e.getSkill().getAbnormalTypes().contains("blessofnoble") || e.getSkillId() == 1325 || e.getSkillId() == 2168)
					{
						e.addToDebugStack("doDie exit 1");
						e.exit();
					}
			}
		}
		// Stop all active skills effects in progress on the L2Character
		else if(isBlessedByNoblesse() || isSalvation())
		{
			if(isSalvation() && getPlayer() != null && !isActionBlocked(L2Zone.BLOCKED_SKILL_RESURRECT) && !isSummon())
				getPlayer().reviveRequest(getPlayer(), 100, this instanceof L2PetInstance, false);

			for(L2Effect e : getAllEffects())
				// Noblesse Blessing Buff/debuff effects are retained after
				// death. However, Noblesse Blessing and Lucky Charm are lost as
				// normal.
				if(e.getSkill().getAbnormalTypes().contains("blessofnoble") || e.getSkillId() == 1325 || e.getSkillId() == 2168)
				{
					e.addToDebugStack("doDie exit 2");
					e.exit();
				}
		}
		else
		{
			for(L2Effect e : getAllEffects())
				if(!e.containsEffect("t_flying_transform") && e.getSkill().getBuffProtectLevel() < 2)
				{
					if(e.getNext() != null && e.getNext().getSkill().getBuffProtectLevel() < 2)
						e.getNext().exit();
					e.exit();
				}

			if(isPlayer() && ((L2Player) this).isCharmOfCourage())
				((L2Player) this).setCharmOfCourage(false);
		}

		ScriptHandler.getInstance().onDie(this, killer);

		// Notify Quest of character's death
		L2NpcInstance npc = null;
		if(killer instanceof L2NpcInstance)
			npc = (L2NpcInstance) killer;

		if(_NotifyQuestOfDeathList != null)
		{
			for(QuestState qs : getNotifyQuestOfDeath())
				qs.getQuest().notifyDeath(npc, this, qs);
			_NotifyQuestOfDeathList.clear();
		}

		if(dieEventList != null)
		{
			for(Long storedId : dieEventList)
			{
				npc = L2ObjectsStorage.getAsNpc(storedId);
				if(npc != null && knowsObject(npc))
					npc.notifyAiEvent(npc, CtrlEvent.EVT_DIE_SET, this, null, null);
			}
			dieEventList.clear();
		}
	}

	/**
	 * Sets HP, MP and CP and revives the L2Character.
	 */
	public void doRevive()
	{
		if(!isTeleporting())
		{
			setIsPendingRevive(false);

			if(isSalvation())
			{
				for(L2Effect e : getAllEffects())
					// Stop Salvation effect
					if(e.getSkill().getAbnormalTypes().contains("resurrection_special"))
					{
						e.exit();
						break;
					}
				setCurrentCp(getMaxCp());
				setCurrentHp(getMaxHp());
				setCurrentMp(getMaxMp());
			}
			else
			{
				if(isPlayer() && Config.RESPAWN_RESTORE_CP >= 0)
					setCurrentCp(getMaxCp() * Config.RESPAWN_RESTORE_CP);

				setCurrentHp(getMaxHp() * Config.RESPAWN_RESTORE_HP);

				if(Config.RESPAWN_RESTORE_MP >= 0)
					setCurrentMp(getMaxMp() * Config.RESPAWN_RESTORE_MP);
			}

			// Start broadcast status
			if(isPlayer())
				broadcastUserInfo();
			broadcastPacket(new Revive(this));
		}
		else
			setIsPendingRevive(true);

		if(isPet())
		{
			L2PetInstance pet = (L2PetInstance) this;
			pet.stopDecay();
			pet.startFeed();
			setRunning();
		}
	}

	/**
	 * Return a map of 32 bits (0x00000000) containing all abnormal effect in
	 * progress for this L2Character.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * In Server->Client packet, each effect is represented by 1 bit of the map
	 * (ex : BLEEDING = 0x0001 (bit 1), SLEEP = 0x0080 (bit 8)...). The map is
	 * calculated by applying a BINARY OR operation on each effect.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li> Server Packet : CharInfo, NpcInfo, NpcInfoPoly, UserInfo...</li>
	 * <BR>
	 * <BR>
	 */
	public int getAbnormalEffect()
	{
		int ae = (int) _abnormalEffects;
		if(getCustomEffect() > 0)
			ae |= getCustomEffect();
		return ae;
	}
    
	public int getAbnormalEffect2()
	{
		return (int) (_abnormalEffects >> 32);
	}
	
	public GArray<AbnormalVisualEffect> getAbnormalEffects()
	{
		return _abnormalEffectsArray;
	}

	public long getAllAbnormalEffects()
	{
		return _abnormalEffects;
	}

	/**
	 * Return the Accuracy (base+modifier) of the L2Character in function of the
	 * Weapon Expertise Penalty.<BR>
	 * <BR>
	 */
	public int getAccuracy()
	{
		return (int) (calcStat(Stats.ACCURACY_COMBAT, 0, null, null) / getWeaponExpertisePenalty());
	}

	/**
	 * Возвращает тип атакующего элемента и его силу.
	 *
	 * @return массив, в котором:
	 *         <li>[0]: тип элемента,
	 *         <li>[1]: его сила
	 */
	public int[] getAttackElement()
	{
		return Formulas.calcAttackElement(this);
	}

	/**
	 * Возвращает защиту от элемента: огонь.
	 *
	 * @return значение защиты
	 */
	public int getDefenceFire()
	{
		return (int) calcStat(Stats.FIRE_ATTRIBUTE, 0, null, null);
	}

	/**
	 * Возвращает защиту от элемента: вода.
	 *
	 * @return значение защиты
	 */
	public int getDefenceWater()
	{
		return (int) calcStat(Stats.WATER_ATTRIBUTE, 0, null, null);
	}

	/**
	 * Возвращает защиту от элемента: воздух.
	 *
	 * @return значение защиты
	 */
	public int getDefenceWind()
	{
		return (int) calcStat(Stats.WIND_ATTRIBUTE, 0, null, null);
	}

	/**
	 * Возвращает защиту от элемента: земля.
	 *
	 * @return значение защиты
	 */
	public int getDefenceEarth()
	{
		return (int) calcStat(Stats.EARTH_ATTRIBUTE, 0, null, null);
	}

	/**
	 * Возвращает защиту от элемента: свет.
	 *
	 * @return значение защиты
	 */
	public int getDefenceHoly()
	{
		return (int) calcStat(Stats.HOLY_ATTRIBUTE, 0, null, null);
	}

	/**
	 * Возвращает защиту от элемента: тьма.
	 *
	 * @return значение защиты
	 */
	public int getDefenceDark()
	{
		return (int) calcStat(Stats.DARK_ATTRIBUTE, 0, null, null);
	}

	/**
	 * Return the L2CharacterAI of the L2Character and if its null create a new
	 * one.<BR>
	 * <BR>
	 */
	@Override
	public L2CharacterAI getAI()
	{
		if(_ai == null)
			_ai = new L2CharacterAI(this);
		return _ai;
	}

	/**
	 * Возвращает коллекцию скиллов для быстрого перебора
	 */
	public Collection<L2Skill> getAllSkills()
	{
		return _skills.values();
	}

	/**
	 * Return the Armour Expertise Penalty of the L2Character.<BR>
	 * <BR>
	 */
	public float getArmourExpertisePenalty()
	{
		return 1.f;
	}

	public GArray<L2Player> getAroundPlayers(int radius, int height)
	{
		if(!isVisible())
			return new GArray<L2Player>();

		return L2World.getAroundPlayers(this, radius, height);
	}

	public GArray<L2Player> getAroundPlayers(int radius)
	{
		return getAroundPlayers(radius, Config.PLAYER_VISIBILITY_Z);
	}

	public GArray<L2Player> getAroundLivePlayers(int radius)
	{
		GArray<L2Player> livePlayers = new GArray<L2Player>();

		if(!isVisible())
			return livePlayers;


		for(L2Player player : L2World.getAroundPlayers(this, radius, Config.PLAYER_VISIBILITY_Z))
			if(!player.isDead() && player.isVisible() && !player.isInvisible())
				livePlayers.add(player);

		return livePlayers;
	}

	/**
	 * Return the Attack Speed multiplier (base+modifier) of the L2Character to
	 * get proper animations.<BR>
	 * <BR>
	 */
	public final float getAttackSpeedMultiplier()
	{
		return (float) (1.1 * getPAtkSpd() / getTemplate().basePAtkSpd);
	}

	public int getBuffLimit()
	{
		return (int) calcStat(Stats.BUFF_LIMIT, Config.ALT_BUFF_LIMIT, null, null);
	}

	public int getSongDanceLimit()
	{
		return (int) calcStat(Stats.SONGDANCE_LIMIT, Config.ALT_SONG_DANCE_LIMIT, null, null);
	}

	public L2Skill getCastingSkill()
	{
		return _castingSkill;
	}

	public L2ItemInstance getCastingItem()
	{
		return _castingItem;
	}

	/**
	 * Return the COM of the L2Character (base+modifier).<BR>
	 * <BR>
	 */
	public byte getCON()
	{
		return (byte) calcStat(Stats.STAT_CON, _template.baseCON, null, null);
	}

	/**
	 * Return the Critical Hit rate (base+modifier) of the L2Character.<BR>
	 * <BR>
	 */
	public int getCriticalHit(L2Character target, L2Skill skill)
	{
		return (int) calcStat(Stats.CRITICAL_RATE, _template.baseCritRate, target, skill);
	}

	/**
	 * Возвращает шанс магического крита в тысячных
	 */
	public double getCriticalMagic(L2Character target, L2Skill skill)
	{
		return calcStat(Stats.MCRITICAL_RATE, Config.ALT_CHANCE_CRITICAL_MAGIC, target, skill);
	}

	/**
	 * Return the current CP of the L2Character.<BR>
	 * <BR>
	 */
	public final double getCurrentCp()
	{
		return _currentCp;
	}

	/**
	 * Return the current HP of the L2Character.<BR>
	 * <BR>
	 */
	public double getCurrentHp()
	{
		return _currentHp;
	}

	/**
	 * Return the current MP of the L2Character.<BR>
	 * <BR>
	 */
	public final double getCurrentMp()
	{
		return _currentMp;
	}

	public Location getDestination()
	{
		return destination;
	}

	/**
	 * Return the DEX of the L2Character (base+modifier).<BR>
	 * <BR>
	 */
	public byte getDEX()
	{
		return (byte) calcStat(Stats.STAT_DEX, _template.baseDEX, null, null);
	}

	/**
	 * Return the Attack Evasion rate (base+modifier) of the L2Character.<BR>
	 * <BR>
	 */
	public int getEvasionRate(L2Character target)
	{
		return (int) (calcStat(Stats.EVASION_RATE, 0, target, null) / getArmourExpertisePenalty());
	}

	/**
	 * Return the orientation of the L2Character.<BR>
	 * <BR>
	 */
	@Override
	public final int getHeading()
	{
		return _heading;
	}

	/**
	 * Return heading to L2Character<BR>
	 * If <b>boolean toChar</b> is true heading calcs this->target, else
	 * target->this.<BR>
	 * <BR>
	 */
	public int getHeadingTo(L2Object target, boolean toChar)
	{
		if(target == null || target == this)
			return -1;

		int dx = target.getX() - getX();
		int dy = target.getY() - getY();
		int heading = (int) (Math.atan2(-dy, -dx) * 32768. / Math.PI + 32768);

		heading = toChar ? target.getHeading() - heading : _heading - heading;

		if(heading < 0)
			heading += 65536;
		return heading;
	}

	/**
	 * Возвращает угол относительно target по часовой стрелке.
	 *
	 * @param target
	 * @return угол
	 */
	public double getDirection(L2Object target)
	{
		return Math.abs(Util.convertHeadingToDegree(getHeadingTo(target, false)) - 360);
	}

	/**
	 * Return the INT of the L2Character (base+modifier).<BR>
	 * <BR>
	 */
	public byte getINT()
	{
		return (byte) calcStat(Stats.STAT_INT, _template.baseINT, null, null);
	}

	public GArray<L2Character> getKnownCharacters(int radius)
	{
		if(!isVisible())
			return new GArray<L2Character>(0);

		return L2World.getAroundCharacters(this, radius, Config.PLAYER_VISIBILITY_Z);
	}

	public GArray<L2Character> getKnownCharacters(int radius, int height)
	{
		if(!isVisible())
			return new GArray<L2Character>(0);

		return L2World.getAroundCharacters(this, radius, height);
	}

	public GArray<L2NpcInstance> getKnownNpc(int range)
	{
		if(!isVisible())
			return new GArray<L2NpcInstance>(0);

		return L2World.getAroundNpc(this, range, Config.PLAYER_VISIBILITY_Z);
	}

	public GArray<L2NpcInstance> getKnownNpc(int range, int height)
	{
		if(!isVisible())
			return new GArray<L2NpcInstance>(0);

		return L2World.getAroundNpc(this, range, height);
	}


	/**
	 * Return Skill if the skill is known by the L2Character.<BR>
	 * <BR>
	 *
	 * @param skillId The identifier of the L2Skill to check the knowledge
	 */
	public final L2Skill getKnownSkill(int skillId)
	{
		if(_skills == null)
			return null;

		return _skills.get(skillId);
	}

	/**
	 * Return the Magical Attack range (base+modifier) of the L2Character.<BR>
	 * <BR>
	 */
	public final int getMagicalAttackRange(L2Skill skill)
	{
		if(skill != null)
			return (int) calcStat(Stats.MAGIC_ATTACK_RANGE, skill.getCastRange(), null, skill);
		return getTemplate().baseAtkRange;
	}

	/**
	 * Return the MAtk (base+modifier) of the L2Character for a skill used in
	 * function of abnormal effects in progress.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li> Calculate Magic damage </li>
	 * <BR>
	 * <BR>
	 *
	 * @param target The L2Character targeted by the skill
	 * @param skill  The L2Skill used against the target
	 */
	public int getMAtk(L2Character target, L2Skill skill)
	{
		if(skill != null && skill.getMatak() > 0)
			return skill.getMatak();
		return (int) calcStat(Stats.MAGIC_ATTACK, _template.baseMAtk, target, skill);
	}

	/**
	 * Return the MAtk Speed (base+modifier) of the L2Character in function of
	 * the Armour Expertise Penalty.<BR>
	 * <BR>
	 */
	public int getMAtkSpd()
	{
		return (int) (calcStat(Stats.MAGIC_ATTACK_SPEED, _template.baseMAtkSpd, null, null) / getArmourExpertisePenalty());
	}

	public double getMAtkSps(L2Character target, L2Skill skill)
	{
		double matk = calcStat(Stats.MAGIC_ATTACK, _template.baseMAtk, target, skill);
		switch(getChargedSpiritShot())
		{
			case 1:
				return matk * 1.41;
			case 2:
				return matk * 2;
			default:
				return matk;
		}
	}

	/**
	 * Return the Max CP (base+modifier) of the L2Character.<BR>
	 * <BR>
	 */
	public int getMaxCp()
	{
		return (int) calcStat(Stats.MAX_CP, _template.baseCpMax, null, null);
	}

	/**
	 * Return the Max HP (base+modifier) of the L2Character.<BR>
	 * <BR>
	 */
	public int getMaxHp()
	{
		return (int) calcStat(Stats.MAX_HP, _template.baseHpMax, null, null);
	}

	/**
	 * Return the Max MP (base+modifier) of the L2Character.<BR>
	 * <BR>
	 */
	public int getMaxMp()
	{
		return (int) calcStat(Stats.MAX_MP, _template.baseMpMax, null, null);
	}

	/**
	 * Return the MDef (base+modifier) of the L2Character against a skill in
	 * function of abnormal effects in progress.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li> Calculate Magic damage </li>
	 * <BR>
	 * <BR>
	 *
	 * @param target The L2Character targeted by the skill
	 * @param skill  The L2Skill used against the target
	 */
	public int getMDef(L2Character target, L2Skill skill)
	{
		if(isBoss() || (isMinion() && ((L2NpcInstance) this).getLeader() != null && ((L2NpcInstance) this).getLeader().isBoss()))
			return Math.max((int) calcStat(Stats.MAGIC_DEFENCE, _template.baseMDef * Config.RATE_BOSS_MDEF, null, skill), 1);
		else if(isRaid() || (isMinion() && ((L2NpcInstance) this).getLeader() != null && ((L2NpcInstance) this).getLeader().isRaid()))
			return Math.max((int) calcStat(Stats.MAGIC_DEFENCE, _template.baseMDef * Config.RATE_RAID_MDEF, null, skill), 1);
		else
			return Math.max((int) calcStat(Stats.MAGIC_DEFENCE, _template.baseMDef, null, skill), 1);
	}

	public double getBaseHpRegen()
	{
		return getTemplate().baseHpReg;
	}

	/**
	 * Return the MEN of the L2Character (base+modifier).<BR>
	 * <BR>
	 */
	public byte getMEN()
	{
		return (byte) calcStat(Stats.STAT_MEN, _template.baseMEN, null, null);
	}

	public int getInteractDistance(L2Object target)
	{
		return L2Character.INTERACTION_DISTANCE + getMinDistance(target);
	}

	public int getMinDistance(L2Object obj)
	{
		int distance = (int) getTemplate().collisionRadius;

		if(obj.isCharacter())
			distance += ((L2Character) obj).getTemplate().collisionRadius;

		return distance;
	}

	public float getMovementSpeedMultiplier()
	{
		if(isRunning())
			return getRunSpeed() / _template.baseRunSpd;

		return getWalkSpeed() / _template.baseWalkSpd;
	}

	/**
	 * Return the RunSpeed (base+modifier) or WalkSpeed (base+modifier) of the
	 * L2Character in function of the movement type.<BR>
	 * <BR>
	 */
	@Override
	public float getMoveSpeed()
	{
		if(isRunning())
			return getRunSpeed();

		return getWalkSpeed();
	}

	public String getName()
	{
		return _name;
	}

	public String getVisibleName()
	{
		return _name;
	}

	/**
	 * Return a list of L2Character that attacked.<BR>
	 * <BR>
	 */
	public final GArray<QuestState> getNotifyQuestOfDeath()
	{
		if(_NotifyQuestOfDeathList == null)
			_NotifyQuestOfDeathList = new GArray<QuestState>();
		return _NotifyQuestOfDeathList;
	}

	/**
	 * Return the PAtk (base+modifier) of the L2Character.<BR>
	 * <BR>
	 */
	public int getPAtk(L2Character target)
	{
		return (int) calcStat(Stats.POWER_ATTACK, _template.basePAtk, target, null);
	}

	/**
	 * Return the PAtk Speed (base+modifier) of the L2Character in function of
	 * the Armour Expertise Penalty.<BR>
	 * <BR>
	 */
	public int getPAtkSpd()
	{
		return Math.max((int) (calcStat(Stats.POWER_ATTACK_SPEED, _template.basePAtkSpd, null, null) / getArmourExpertisePenalty()), 1);
	}

	/**
	 * Return the PDef (base+modifier) of the L2Character.<BR>
	 * <BR>
	 */
	public int getPDef(L2Character target)
	{
		if(isBoss() || (isMinion() && ((L2NpcInstance) this).getLeader() != null && ((L2NpcInstance) this).getLeader().isBoss()))
			return Math.max((int) calcStat(Stats.POWER_DEFENCE, _template.basePDef * Config.RATE_BOSS_PDEF, target, null), 1);
		else if(isRaid() || (isMinion() && ((L2NpcInstance) this).getLeader() != null && ((L2NpcInstance) this).getLeader().isRaid()))
			return Math.max((int) calcStat(Stats.POWER_DEFENCE, _template.basePDef * Config.RATE_RAID_PDEF, target, null), 1);
		else
			return Math.max((int) calcStat(Stats.POWER_DEFENCE, _template.basePDef, target, null), 1);
	}

	/**
	 * Return the Physical Attack range (base+modifier) of the L2Character.<BR>
	 * <BR>
	 */
	public final int getPhysicalAttackRange()
	{
		return (int) calcStat(Stats.POWER_ATTACK_RANGE, getTemplate().baseAtkRange, null, null);
	}

	/**
	 * Return a Random Damage in function of the weapon.<BR>
	 * <BR>
	 */
	public int getRandomDamage()
	{
		L2Weapon weaponItem = getActiveWeaponItem();

		if(weaponItem == null)
			return getTemplate().baseRandDam;

		return weaponItem.randomDamage;
	}

	/**
	 * Return the Skill/Spell reuse modifier.<BR>
	 * <BR>
	 */
	public double getReuseModifier(L2Character target)
	{
		return calcStat(Stats.ATK_REUSE, 1, target, null);
	}

	/**
	 * @return the RunSpeed of the L2Character.
	 */
	public float getRunSpeed()
	{
		return getSpeed(_template.baseRunSpd);
	}

	/**
	 * @return the ShieldDef rate (base+modifier) of the L2Character.
	 */
	public final int getShldDef()
	{
		if(isPlayer())
			return (int) calcStat(Stats.SHIELD_DEFENCE, 0, null, null);
		return (int) calcStat(Stats.SHIELD_DEFENCE, _template.baseShldDef, null, null);
	}

	public final int getSkillDisplayLevel(int skillId)
	{
		if(_skills == null)
			return -1;

		L2Skill skill = _skills.get(skillId);

		if(skill == null)
			return -1;

		return skill.getDisplayLevel();
	}

	/**
	 * Return the level of a skill owned by the L2Character.<BR>
	 * <BR>
	 *
	 * @param skillId The identifier of the L2Skill whose level must be returned
	 * @return The level of the L2Skill identified by skillId
	 */
	public int getSkillLevel(int skillId)
	{
		if(_skills == null)
			return -1;

		L2Skill skill = _skills.get(skillId);

		if(skill == null)
			return -1;

		return skill.getLevel();
	}

	public byte getSkillMastery(Integer skillId)
	{
		Byte val = _skillMastery.get(skillId);
		return val == null ? 0 : val;
	}

	public void removeSkillMastery(Integer skillId)
	{
		_skillMastery.remove(skillId);
	}

	public void setSkillMasteryReuse(Integer skillId)
	{
		_skillMasteryReuse.add(skillId);
	}

	public void removeSkillMasteryReuse(Integer skillId)
	{
		_skillMasteryReuse.remove(skillId);
	}

	public boolean isSkillMasterReuse(Integer skillId)
	{
		return _skillMasteryReuse.contains(skillId);
	}

	/**
	 * Return the Speed (base+modifier) of the L2Character in function of the
	 * Armour Expertise and Territories Penalty.<BR>
	 * <BR>
	 */
	public float getSpeed(int baseSpeed)
	{
		return (float) (calcStat(Stats.RUN_SPEED, baseSpeed, null, null) / getArmourExpertisePenalty());
	}

	/**
	 * Return the list of L2Character that must be informed of HP/MP updates of
	 * this L2Character.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Each L2Character owns a list called <B>_statusListener</B> that contains
	 * all L2Player to inform of HP/MP updates. Players who must be informed are
	 * players that target this L2Character. When a RegenTask is in progress
	 * sever just need to go through this list to send Server->Client packet
	 * StatusUpdate.<BR>
	 * <BR>
	 *
	 * @return The list of L2Character to inform or null if empty
	 */
	private CopyOnWriteArraySet<L2Character> getStatusListener()
	{
		return _statusListener;
	}

	/**
	 * Return the STR of the L2Character (base+modifier).<BR>
	 * <BR>
	 */
	public byte getSTR()
	{
		return (byte) calcStat(Stats.STAT_STR, _template.baseSTR, null, null);
	}

	public float getSwimSpeed()
	{
		return (float) calcStat(Stats.RUN_SPEED, _template.baseRunSpd, null, null);
	}

	/**
	 * Return the L2Object targeted or null.<BR>
	 * <BR>
	 */
	public final L2Object getTarget()
	{
		return L2ObjectsStorage.get(targetStoredId);
	}

	public final L2Player getTargetPlayer()
	{
		return L2ObjectsStorage.getAsPlayer(targetStoredId);
	}

	/**
	 * Return the identifier of the L2Object targeted or -1.<BR>
	 * <BR>
	 */
	public final int getTargetId()
	{
		L2Object target = getTarget();
		if(target != null)
			return target.getObjectId();
		return -1;
	}

	/**
	 * Return the template of the L2Character.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Each L2Character owns generic and static properties (ex : all Keltir have
	 * the same number of HP...). All of those properties are stored in a
	 * different template for each type of L2Character. Each template is loaded
	 * once in the server cache memory (reduce memory use). When a new instance
	 * of L2Character is spawned, server just create a link between the instance
	 * and the template This link is stored in <B>_template</B><BR>
	 * <BR>
	 */
	public L2CharTemplate getTemplate()
	{
		return _template;
	}

	public L2CharTemplate getBaseTemplate()
	{
		return _baseTemplate;
	}

	/**
	 * Return the Title of the L2Character.<BR>
	 * <BR>
	 */
	public String getTitle()
	{
		if(_title != null && _title.length() > 16)
			return _title.substring(0, 16);
		return _title;
	}

	/**
	 * Return the WalkSpeed of the L2Character.<BR>
	 * <BR>
	 */
	public final float getWalkSpeed()
	{
		if(isSwimming())
			return getSwimSpeed();
		return getSpeed(_template.baseWalkSpd);
		// return (getSpeed(_template.baseWalkSpd) * 70) / 100;
	}

	/**
	 * Return the Weapon Expertise Penalty of the L2Character.<BR>
	 * <BR>
	 */
	public float getWeaponExpertisePenalty()
	{
		return 1.f;
	}

	/**
	 * Return the WIT of the L2Character (base+modifier).<BR>
	 * <BR>
	 */
	public byte getWIT()
	{
		return (byte) calcStat(Stats.STAT_WIT, _template.baseWIT, null, null);
	}

	/**
	 * Return True if the L2Character has a L2CharacterAI.<BR>
	 * <BR>
	 */
	@Override
	public boolean hasAI()
	{
		return _ai != null;
	}

	/**
	 * Return True if the L2Character is dead or use fake death.<BR>
	 * <BR>
	 */
	public final boolean isAlikeDead()
	{
		return _fakeDeath || _currentHp < 1.;
	}

	/**
	 * Return True if the L2Character has aborted its attack.<BR>
	 * <BR>
	 */
	public boolean isAttackAborted()
	{
		return _isAttackAborted;
	}

	/**
	 * Return True if the L2Character is attacking.<BR>
	 * <BR>
	 */
	public final boolean isAttackingNow()
	{
		// +10 тут для того чтобы корректно работали еффекты скилов
		// например в i_remove_target функция isAttackingNow() возвращяет фелс без +10
		// и при этом после снятия таргета с цели, цель продолжает бить. 		
		return _attackEndTime + 10 > System.currentTimeMillis();
	}

	/**
	 * Return True if the L2Character is behind the target and can't be seen.<BR>
	 * <BR>
	 */
	public boolean isBehindTarget()
	{
		L2Object target = getTarget();
		if(target != null && target.isCharacter())
		{
			int head = getHeadingTo(target, true);
			return head != -1 && (head <= 10430 || head >= 55105);
		}

		return false;
	}

	public boolean isToSideOfTarget()
	{
		L2Object target = getTarget();
		if(target != null && target.isCharacter())
		{
			int head = getHeadingTo(target, true);
			return head != -1 && (head <= 22337 || head >= 43197) && !isBehindTarget();
		}

		return false;
	}

	public boolean isToSideOfTarget(L2Object target)
	{
		if(target != null && target.isCharacter())
		{
			int head = getHeadingTo(target, true);
			return head != -1 && (head <= 22337 || head >= 43197);
		}

		return false;
	}

	/**
	 * Return True if the L2Character is behind the target and can't be seen.<BR>
	 * <BR>
	 */
	public boolean isBehindTarget(L2Object target)
	{
		if(target != null && target.isCharacter())
		{
			int head = getHeadingTo(target, true);
			return head != -1 && (head <= 10430 || head >= 55105);
		}
		return false;
	}

	public final boolean isBlessedByNoblesse()
	{
		return _isBlessedByNoblesse > 0;
	}

	public final boolean isSalvation()
	{
		return _isSalvation > 0;
	}

	/**
	 * Return True if the L2Character is dead.<BR>
	 * <BR>
	 */
	public final boolean isDead()
	{
		return _currentHp < 0.5;
	}

	public final boolean isDropDisabled()
	{
		return _dropDisabled > System.currentTimeMillis();
	}

	/**
	 * Return True if the L2Character is flying.<BR>
	 * <BR>
	 */
	@Override
	public boolean isFlying()
	{
		return _flying;
	}

	/**
	 * Return True if the L2Character is in combat.<BR>
	 * <BR>
	 */
	public final boolean isInCombat()
	{
		return _stanceTask != null;
	}

	/**
	 * Return True if the target is front L2Character and can be seen.<BR>
	 * <BR>
	 * degrees = 0..180, front->sides->back
	 */
	public boolean isInFront(L2Object target, int degrees)
	{
		int head = getHeadingTo(target, false);
		return head <= 32768 * degrees / 180 || head >= 65536 - 32768 * degrees / 180;
	}


	/*
	Return true of Character stands in front of the target (180 degrees sector).
	 */

	public boolean isInFrontOfTarget()
	{
		if(getTarget() != null && getTarget().isCharacter())
		{
			int head = getHeadingTo(getTarget(), true);
			return head != -1 && head >= 16384 && head <= 49152;
		}
		return false;
	}

	/**
	 * Return True if the L2Player is invulnerable.<BR>
	 * <BR>
	 */
	public boolean isInvul()
	{
		return _isInvul;
	}

	/**
	 * Check if player is mage
	 */
	public boolean isMageClass()
	{
		return getTemplate().baseMAtk > 3;

	}

	/**
	 * Return True if the L2Character is riding.<BR>
	 * <BR>
	 */
	public final boolean isRiding()
	{
		return _riding;
	}

	/**
	 * Return True if the L2Character is running.<BR>
	 * <BR>
	 */
	public final boolean isRunning()
	{
		return _running;
	}

	public boolean isSwimming()
	{
		return false;
	}

	public final boolean isTeleporting()
	{
		return _isTeleporting;
	}

	public boolean knowsObject(L2Object obj)
	{
		return isInRange(obj, Config.PLAYER_VISIBILITY);
	}

	public Location applyOffset(Location pnt, int offset)
	{
		Location point = pnt.clone();
		if(offset <= 0)
			return point;

		long dx = point.getX() - getX();
		long dy = point.getY() - getY();
		long dz = point.getZ() - getZ();

		double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

		if(distance <= offset)
		{
			point.set(getX(), getY(), getZ());
			return point;
		}

		if(distance >= 1)
		{
			double cut = offset / distance;
			point.set(point.getX() - (int) Math.round(dx * cut), point.getY() - (int) Math.round(dy * cut), point.getZ() - (int) Math.round(dz * cut));

			if(!isFlying() && !isInBoat() && !isSwimming() && !isVehicle())
				point.correctGeoZ();
		}

		return point;
	}

	public Location addOffset(Location pnt, int offset)
	{
		Location point = pnt.clone();

		long dx = point.getX() - getX();
		long dy = point.getY() - getY();
		long dz = point.getZ() - getZ();

		double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

		if(distance >= 1)
		{
			double cut = offset / distance;
			point.set(point.getX() + (int) Math.round(dx * cut), point.getY() + (int) Math.round(dy * cut), point.getZ() + (int) Math.round(dz * cut));

			if(!isFlying() && !isInBoat() && !isSwimming() && !isVehicle())
				point.correctGeoZ();
		}

		return point;
	}

	public void applyOffset(ArrayList<Location> points, int offset)
	{
		offset = offset >> 4;
		if(offset <= 0)
			return;

		long dx = points.get(points.size() - 1).getX() - points.get(0).getX();
		long dy = points.get(points.size() - 1).getY() - points.get(0).getY();
		long dz = points.get(points.size() - 1).getZ() - points.get(0).getZ();

		double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if(distance <= offset)
		{
			Location point = points.get(0);
			points.clear();
			points.add(point);
			return;
		}

		if(distance >= 1)
		{
			double cut = offset / distance;
			int num = (int) Math.round(points.size() * cut);
			for(int i = 1; i <= num && points.size() > 0; i++)
				points.remove(points.size() - 1);
		}
	}

	public boolean setSimplePath(Location dest)
	{
		ArrayList<Location> moveList = GeoMove.constructMoveList(getLoc(), dest);
		if(moveList.isEmpty())
			return false;
		_targetRecorder.clear();
		_targetRecorder.add(moveList);
		return true;
	}

	public boolean buildPathToPawn(L2Character target, int offset)
	{
		int ref = getReflection();
		offset = Math.max(offset, 0);

		boolean pf = !(this instanceof L2Player && (getAI().getIntention() == CtrlIntention.AI_INTENTION_ATTACK || getAI().getIntention() == CtrlIntention.AI_INTENTION_CAST));

		desireDestination = target.isMoving ? isPawn ? addOffset(target.getLoc(), (int) getDistance(target) + 40) : target.getLoc() : applyOffset(target.getLoc(), offset);

		if(isInAirShip())
		{
			desireDestination = applyOffset(desireDestination, offset);
			return setSimplePath(desireDestination.clone());
		}

		if(isFloating())
		{
			//if(.isPlayable()
			//	_log.info(this + " biuldPathTo: get point in water: " + desireDestination);

			L2Zone water = getZone(ZoneType.water);

			if(water != null)
			{
				int zMin = water.getMinZ(getX(), getY(), getZ()) >> 4;
				int zMax = water.getMaxZ(getX(), getY(), getZ()) >> 4;
				if(desireDestination.getZ() >> 4 > zMax - 1)
					desireDestination.setZ((zMax - 1) << 4);
				else if(desireDestination.getZ() >> 4 < zMin + 1)
					desireDestination.setZ((zMin + 1) << 4);

				//if(.isPlayable()
				//	_log.info(this + " biuldPathTo: limit Z: " + desireDestination.getZ());
			}

			ArrayList<Location> ml = GeoEngine.moveListInWater(getX(), getY(), getZ(), desireDestination.getX(), desireDestination.getY(), desireDestination.getZ(), getReflection());

			//if(isPlayable)
			//	_log.info(this + " biuldPathTo: can see false return dest: " + desireDestination);

			if(ml.isEmpty() || ml.size() < 2)
				return false;
			_targetRecorder.clear();
			_targetRecorder.add(ml);

			return true;
		}

		Location d = desireDestination.clone().world2geo();
		moveList = GeoEngine.MoveList(getX(), getY(), getZ(), desireDestination.getX(), desireDestination.getY(), ref, false);
/*
		if(isPlayable)
		{
			_log.info(this + " buildPathToPawn: " + getAI().getIntention() + " offset: " + offset + " pf: " + pf + " ms: " + moveList.size());
			_log.info(this + " buildPathToPawn: " + desireDestination + " --> " + getLoc() + " dist: " + String.format("%.2f/%.2f", getDistance(desireDestination.getX(), desireDestination.getY()), getDistance(target)));
		}
*/
		if(moveList != null && moveList.isEmpty())
			return false;

		if(moveList != null && !moveList.isEmpty() && moveList.get(moveList.size() - 1).getX() == d.getX() && moveList.get(moveList.size() - 1).getY() == d.getY())
		{
			if(moveList.size() < 2) // уже стоим на нужной клетке
				return false;

			_targetRecorder.clear();
			_targetRecorder.add(moveList);
			return true;
		}

		if(pf && !isFloating())
		{
			ArrayList<ArrayList<Location>> targets = GeoMove.findMovePath(getX(), getY(), getZ(), target.getLoc().clone(), this, Config.PATHFIND_DEBUG, ref);
			if(!targets.isEmpty())
			{
				moveList = targets.remove(targets.size() - 1);
				applyOffset(moveList, offset);
				if(!moveList.isEmpty())
					targets.add(moveList);
				if(!targets.isEmpty())
				{
					_targetRecorder.clear();
					_targetRecorder.addAll(targets);
					return true;
				}
			}
		}

		if(moveList != null && moveList.size() > 1) // null - до конца пути дойти нельзя
		{
			_targetRecorder.clear();
			_targetRecorder.add(moveList);
			return true;
		}

		return false;
	}

	public boolean buildPathTo(int dest_x, int dest_y, int dest_z, int offset, boolean pathFind)
	{
		int ref = getReflection();

		desireDestination = new Location(dest_x, dest_y, dest_z);

		if(isMoveInVehicle() || isInBoat() || (isVehicle() && !(this instanceof L2ClanAirship)) || (isNpc() && isFloating()))
		{
			desireDestination = applyOffset(desireDestination, offset);
			return setSimplePath(desireDestination.clone());
		}

		if(offset > 0)
			desireDestination = applyOffset(desireDestination, offset);

		if(isFloating())
		{
			//if(isPlayable)
			//	_log.info(this + " biuldPathTo: get point in water: " + desireDestination);

			L2Zone water = getZone(ZoneType.water);

			if(water != null)
			{
				int zMin = water.getMinZ(getX(), getY(), getZ()) >> 4;
				int zMax = water.getMaxZ(getX(), getY(), getZ()) >> 4;
				if(desireDestination.getZ() >> 4 > zMax - 1)
					desireDestination.setZ((zMax - 1) << 4);
				else if(desireDestination.getZ() >> 4 < zMin + 1)
					desireDestination.setZ((zMin + 1) << 4);

				//if(isPlayable)
				//	_log.info(this + " biuldPathTo: limit Z: " + desireDestination.getZ());
			}

			ArrayList<Location> ml = GeoEngine.moveListInWater(getX(), getY(), getZ(), desireDestination.getX(), desireDestination.getY(), desireDestination.getZ(), getReflection());

			//if(isPlayable)
			//	_log.info(this + " biuldPathTo: can see false return dest: " + desireDestination);

			if(ml.isEmpty() || ml.size() < 2)
				return false;
			_targetRecorder.clear();
			_targetRecorder.add(ml);

			return true;
		}

		Location d = desireDestination.clone().world2geo();

		moveList = GeoEngine.MoveList(getX(), getY(), getZ(), desireDestination.getX(), desireDestination.getY(), ref, false); // onlyFullPath = false - идем до куда можем

		//if(isPlayable)
		//	_log.info(this + "buildPath: size: " + moveList.size() + " dest: " + d + " last: " + (moveList.size() > 0 ? moveList.get(moveList.size() - 1) : "no point"));

		if(moveList != null && moveList.isEmpty())
			return false;

		if(moveList != null && !moveList.isEmpty() && moveList.get(moveList.size() - 1).getX() == d.getX() && moveList.get(moveList.size() - 1).getY() == d.getY()) // null - нет геодаты, empty - уже стоим на нужной клетке
		{
			//applyOffset(moveList, offset);
			if(moveList.size() < 2) // уже стоим на нужной клетке
				return false;

			_targetRecorder.clear();
			_targetRecorder.add(moveList);

			return true;
		}


		if(offset > 0)
			desireDestination = new Location(dest_x, dest_y, dest_z);

		if(pathFind && !isFloating() && isInRange(desireDestination, 2000))
		{
			ArrayList<ArrayList<Location>> targets = GeoMove.findMovePath(getX(), getY(), getZ(), desireDestination.clone(), this, Config.PATHFIND_DEBUG, ref);
			if(!targets.isEmpty())
			{
				moveList = targets.remove(targets.size() - 1);
				applyOffset(moveList, offset);
				if(!moveList.isEmpty())
					targets.add(moveList);
				if(!targets.isEmpty())
				{
					_targetRecorder.clear();
					_targetRecorder.addAll(targets);
					return true;
				}
			}
		}

		if(moveList != null && !moveList.isEmpty() && moveList.size() > 1) // null - до конца пути дойти нельзя
		{
			_targetRecorder.clear();
			_targetRecorder.add(moveList);
			return true;
		}

		return false;
	}

	public boolean followToCharacter(L2Character target, int offset)
	{
		//if(isPlayable)
		//	_log.info(this + " follow: " + target + " offset: " + offset + " isFollow: " + isFollow);
		offset = Math.max(offset, 10);

		getAI().clearNextAction();

		if(isMovementDisabled() || target == null || (isInBoat() && !isInAirShip()))
		{
			stopMove();
			return false;
		}

		if(Math.abs(getZ() - target.getZ()) > 1000 && !isFlying())
		{
			stopMove();
			sendPacket(Msg.CANNOT_SEE_TARGET);
			return false;
		}

		if(isFollow && getFollowTarget() == target && target.isInRange(movingDestTempPos, 100))
			return true;

		try
		{
			if(_moveTask != null)
			{
				_moveTaskRunnable.abort = true;
				_moveTask.cancel(true);
				if(moveList != null && !moveList.isEmpty())
					_moveTaskRunnable.run();
				_moveTask = null;
				isMoving = false;
			}
		}
		catch(NullPointerException e)
		{
		}

		isFollow = true;
		setFollowTarget(target);

		if(isInRangeZ(target, offset + 12))
		{
			_offset = offset;
			if(GeoEngine.canSeeTarget(this, target))
			{
				isFollow = false;
				ThreadPoolManager.getInstance().executeAi(new NotifyAITask(this, CtrlEvent.EVT_ARRIVED_TARGET, null, null), isPlayable());
				return true;
			}
			return false;
		}

		synchronized(_targetRecorder)
		{
			if(buildPathToPawn(target, offset))
			{
				//if(isPlayable)
				//	_log.info(this + " follow: --> " + target.getLoc());
				movingDestTempPos.set(target.getX(), target.getY(), target.getZ());
			}
			else
			{
				//if(isPlayable)
				//	_log.info(this + " follow: no path");
				isFollow = false;
				return false;
			}

			_offset = offset;
			moveNext(true);
			return true;
		}
	}

	public boolean moveToLocation(Location loc, int offset, boolean pathfinding)
	{
		//if(isPlayable)
		//	_log.info(this + " moveToLocation: " + loc + " " + offset + " " + pathfinding);
		return moveToLocation(loc.getX(), loc.getY(), loc.getZ(), offset, pathfinding);
	}

	public boolean moveToLocation(int x_dest, int y_dest, int z_dest, int offset, boolean pathfinding)
	{

		synchronized(_targetRecorder)
		{
			offset = Math.max(offset, 0);
			_offset = offset;

			Location dst_geoloc = new Location(x_dest, y_dest, z_dest).world2geo();
			if(isMoving && !isFollow && movingDestTempPos.equals(dst_geoloc))
			{
				sendActionFailed();
				return true;
			}

			Location currLoc = getLoc().clone().world2geo();
			if(currLoc.equals(dst_geoloc))
			{
				sendActionFailed();
				return true;
			}

			getAI().clearNextAction();

			if(isPlayable())
				((L2PlayableAI) getAI()).stopFollow();

			if(isMovementDisabled())
			{
				getAI().setNextAction(L2PlayableAI.nextAction.MOVE, new Location(x_dest, y_dest, z_dest), offset, pathfinding, false);
				sendActionFailed();
				return false;
			}

			isFollow = false;

			if(_moveTask != null)
			{
				_moveTaskRunnable.abort = true;
				_moveTask.cancel(true);
				//if(isPlayable)
				//	_log.info(this + " abort move task.");
				if(moveList != null && !moveList.isEmpty())
					_moveTaskRunnable.run();
				_moveTask = null;
				isMoving = false;
			}

			if(isPlayer())
				getAI().changeIntention(AI_INTENTION_ACTIVE, null, null);

			if(z_dest > 15000)
				z_dest = 14900;
			else if(z_dest < -16384)
				z_dest = -16300;

			if(buildPathTo(x_dest, y_dest, z_dest, offset, pathfinding))
				movingDestTempPos.set(dst_geoloc);
			else
			{
				isMoving = false;
				isPawn = false;
				sendActionFailed();
				return false;
			}
		}

		moveNext(true);

		return true;
	}

	/**
	 * должно вызыватся только из synchronized(_targetRecorder)
	 *
	 * @param firstMove
	 */
	public void moveNext(boolean firstMove)
	{
		_previousSpeed = getMoveSpeed();
		if(_previousSpeed <= 0)
		{
			stopMove();
			return;
		}

		if(!firstMove)
		{
			Location dest = destination;
			if(dest != null)
				setXYZ(dest.getX(), dest.getY(), dest.getZ(), false);
			//setLoc(dest, true);
		}

		double distance;

		synchronized(_targetRecorder)
		{
			if(_targetRecorder.isEmpty())
			{
				isMoving = false;
				if(isFollow)
				{
					isFollow = false;
					//if(isPlayer())
					//	_log.info(this + " arrived target notify AI, dest: " + getDistance(getFollowTarget()));
					ThreadPoolManager.getInstance().executeAi(new NotifyAITask(this, CtrlEvent.EVT_ARRIVED_TARGET, null, null), isPlayable());
				}
				else
					ThreadPoolManager.getInstance().executeAi(new NotifyAITask(this, CtrlEvent.EVT_ARRIVED, null, null), isPlayable());

				//validateLocation(isPlayer()() ? 2 : 1);
				return;
			}

			moveList = _targetRecorder.remove(0);
			Location begin = moveList.get(0).clone().geo2world();
			Location end = moveList.get(moveList.size() - 1).clone();
			Location dLoc = desireDestination.clone().world2geo();
			destination = dLoc.getX() == end.getX() && dLoc.getY() == end.getY() ? desireDestination.setZ(end.getZ()) : end.geo2world();
			movingDestTempPos.set(destination.clone().world2geo());

			distance = isFloating() ? begin.distance3D(destination) : begin.distance(destination);

			isMoving = true;
		}

		broadcastMove();
		setHeading(calcHeading(destination));
		_startMoveTime = System.currentTimeMillis();

		if(isPlayer() && isInBoat())
			_moveTask = ThreadPoolManager.getInstance().scheduleMove(getPlayer().getMoveInVehicleTask().setTarget(getPlayer().getLocToVehicle()), 500);
		else
			_moveTask = ThreadPoolManager.getInstance().scheduleMove(_moveTaskRunnable.setDist(distance), getMoveTickInterval());
	}

	public int getMoveTickInterval()
	{
		return (int) (32000 / getMoveSpeed());
	}

	public Location getDesireLoc()
	{
		return desireDestination;
	}

	public void broadcastMove()
	{
		if(isMovePacketNeeded())
		{
			prevDestination = destination;
			if(destination != null)
				broadcastPacket(new CharMoveToLocation(this, destination));
		}
	}

	public boolean isMoveInVehicle()
	{
		return false;
	}

	public void stopMove()
	{
		prevDestination = null;
		if(isMoving)
		{
			isMoving = false;

			setXYZ(getX(), getY(), isFlying() ? getZ() : GeoEngine.getHeight(getX(), getY(), getZ(), getReflection()), false);

			broadcastPacket(new StopMove(this));
		}

		if(isFollow)
		{
			isFollow = false;
			isPawn = false;
		}
	}

	/**
	 * Returns true if status update should be done, false if not
	 *
	 * @return boolean
	 */
	protected boolean needStatusUpdate()
	{
		if(Config.FORCE_STATUSUPDATE)
			return true;

		if(!isNpc())
			return true;

		double _intervalHpUpdate = getMaxHp() / 352;

		if(_lastHpUpdate == -99999999)
		{
			_lastHpUpdate = -9999999;
			return true;
		}

		if(getCurrentHp() <= 0 || getMaxHp() < 352)
			return true;

		if(_lastHpUpdate + _intervalHpUpdate < getCurrentHp() && getCurrentHp() > _lastHpUpdate)
		{
			_lastHpUpdate = getCurrentHp();
			return true;
		}

		if(_lastHpUpdate - _intervalHpUpdate > getCurrentHp() && getCurrentHp() < _lastHpUpdate)
		{
			_lastHpUpdate = getCurrentHp();
			return true;
		}
		return false;
	}

	protected void initCharStatusUpdateValues()
	{
		_hpUpdateInterval = getMaxHp() / 352.0; // MAX_HP div MAX_HP_BAR_PX
		_hpUpdateIncCheck = getMaxHp();
		_hpUpdateDecCheck = getMaxHp() - _hpUpdateInterval;
	}

	/**
	 * Remove the L2Character from the world when the decay task is launched.<BR>
	 * <BR>
	 */
	public void onDecay()
	{
		decayMe();
		fireMethodInvoked(MethodCollection.onDecay, null);
	}

	@Override
	public void deleteMe()
	{
		setTarget(null);
		stopMove();
		super.deleteMe();
	}

	/**
	 * Manage Forced attack (ctrl + select target).<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>If L2Character or target is in a town area, send a system message
	 * TARGET_IN_PEACEZONE a Server->Client packet ActionFailed </li>
	 * <li>If target is confused, send a Server->Client packet ActionFailed
	 * </li>
	 * <li>If L2Character is a L2ArtefactInstance, send a Server->Client packet
	 * ActionFailed </li>
	 * <li>Send a Server->Client packet MyTargetSelected to start attack and
	 * Notify AI with AI_INTENTION_ATTACK </li>
	 * <BR>
	 * <BR>
	 *
	 * @param player The L2Player to attack
	 */
	@Override
	public void onForcedAttack(L2Player player, boolean dontMove)
	{
		if(player.isConfused() || player.isBlocked())
		{
			player.sendActionFailed();
			return;
		}

		//player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
		player.getAI().Attack(this, true, dontMove);
	}

	public void onShield(L2Character target)
	{
		if(!target.isDead() && target._skillsOnShield != null && target._skillsOnShield.size() > 0)
			for(L2Skill skill : target._skillsOnShield.values())
				skill.useChanceSkill(new Env(target, this, null));
	}

	protected void onHitTimer(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, boolean shld, boolean unchargeSS)
	{
		if(isAlikeDead())
		{
			sendActionFailed();
			return;
		}

		if(target.isDead() || !isInRange(target, 1500))
		{
			sendActionFailed();
			return;
		}

		// If attack isn't aborted, send a message system (critical hit,
		// missed...) to attacker/target if they are L2Player
		if(!isAttackAborted())
		{
			target.fireMethodInvoked(MethodCollection.onAttacked, new Object[]{this, target, damage, crit, miss, soulshot, shld, unchargeSS});

			boolean block = false;
			if(target.calcStat(Stats.BLOCK_HP, 0, null, null) > 0)
			{
				damage = 0;
				block = true;
			}

			if(target.isStunned() && Formulas.calcStunBreak(crit))
				target.stopEffects("stun");

			if(target.isPlayer())
			{
				L2Player enemy = (L2Player) target;

				if(shld && damage > 1)
					enemy.sendPacket(Msg.SHIELD_DEFENSE_HAS_SUCCEEDED);
				else if(shld && damage == 1)
					enemy.sendPacket(Msg.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
				if(shld)
					onShield(target);
			}

			sendDamageMessage(target, damage, miss, crit, block);

			if(checkPvP(target, null))
				startPvPFlag(target);

			// Reduce HP of the target and calculate reflection damage to reduce
			// HP of attacker if necessary
			if(!miss)
			{
				if(damage > 0)
				{
					if(target.getForceBuff() != null)
						target.abortCast();

					L2Weapon weapon = getActiveWeaponItem();

					if(weapon == null || (weapon.getItemType() != L2Weapon.WeaponType.BOW && weapon.getItemType() != L2Weapon.WeaponType.CROSSBOW))
					{
						if(!target.isStatActive(Stats.BLOCK_HP))
						{
							double reflect = Math.min(100, target.calcStat(Stats.REFLECT_DAMAGE_PERCENT, 0, null, null));
							if(reflect > 0 && target.getCurrentHp() > damage)
							{
								double rdmg = damage * reflect / 100.;

								if(rdmg > target.getMaxHp())
									rdmg = target.getMaxHp();

								decreaseHp(rdmg, target, target.isNpc(), true);
							}
						}

						if(damage > 0 && !(target instanceof L2DoorInstance) && getCurrentHp() > 0.5)
						{
							// Absorb HP from the damage inflicted
							double absorbPercent = Math.min(100, calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, null, null));

							if(absorbPercent > 0)
							{
								int maxCanAbsorb = (int) (getMaxHp() - getCurrentHp());
								int absorbDamage = (int) (absorbPercent / 100. * damage);

								if(absorbDamage > maxCanAbsorb)
									absorbDamage = maxCanAbsorb; // Can't absord more than max hp

								if(absorbDamage > 0)
								{
									int maxHp = (int) calcStat(Stats.HP_LIMIT, getMaxHp(), null, null);
									if(_currentHp + absorbDamage > maxHp)
										absorbDamage = (int) Math.max(0, maxHp - _currentHp);

									setCurrentHp(_currentHp + absorbDamage);
								}
							}

							absorbPercent = Math.min(100, calcStat(Stats.ABSORB_DAMAGEMP_PERCENT, 0, target, null));
							if(absorbPercent > 0 && (target.getCurrentHp() >= damage || (target.isNpc() && ((L2NpcInstance) target).getTemplate().undying == 1)))
							{
								int maxMp = (int) calcStat(Stats.MP_LIMIT, getMaxMp(), null, null);
								absorbPercent = damage * absorbPercent / 100.;

								if(_currentMp + absorbPercent > maxMp)
									absorbPercent = (int) Math.max(0, maxMp - _currentMp);

								setCurrentMp(_currentMp + absorbPercent);
							}
						}
					}

					target.reduceHp(damage, this, false, false);
				}

				// Скиллы, кастуемые при физ атаке
				if(!target.isDead())
				{
					if(_skillsOnAttack != null && _skillsOnAttack.size() > 0)
						for(L2Skill skill : _skillsOnAttack.values())
							if(crit || !skill.isOnCrit())
							{
								Env env = new Env(this, target, null);
								env.value = damage;
								skill.useChanceSkill(env);
							}

					Calculator triggerByAttack = _calculators[Stats.TRIGGER_BY_ATTACK.ordinal()];
					if(triggerByAttack != null && triggerByAttack.size() > 0)
					{
						Env env = new Env(this, target, null);
						env.value = damage;
						env.success = crit;
						for(Func func : triggerByAttack.getFunctions())
							func.calc(env);
					}

					if(target._skillsOnUnderAttack != null && target._skillsOnUnderAttack.size() > 0)
						for(L2Skill chance : target._skillsOnUnderAttack.values())
							if(!chance.isOnMagicAttacked())
							{
								Env env = new Env(target, this, null);
								env.value = damage;
								chance.useChanceSkill(env);
							}
				}

				if(damage > 0)
				{
					if(soulshot && unchargeSS)
						unChargeShots(false);

					// Manage attack or cast break of the target (calculating rate,
					// sending message...)
					if(Formulas.calcCastBreak(this, target, damage))
						target.breakCast(false, true);
				}
			}
			else
			{
				//Скилы кастуемые при промахе(увороте)
				if(!target.isDead())
				{
					if(target._skillsOnEvaded != null && target._skillsOnEvaded.size() > 0)
						for(L2Skill skill : target._skillsOnEvaded.values())
						{
							Env env = new Env(target, this, null);
							env.value = damage;
							skill.useChanceSkill(env);
						}

					Calculator triggerByAvoid = _calculators[Stats.TRIGGER_BY_AVOID.ordinal()];
					if(triggerByAvoid != null && triggerByAvoid.size() > 0)
					{
						Env env = new Env(target, this, null);
						env.value = damage;
						for(Func func : triggerByAvoid.getFunctions())
							func.calc(env);
					}
				}
			}
		}

		startAttackStanceTask();
	}

	public void onMagicUseTimer(L2Character aimingTarget, L2Skill skill, L2ItemInstance usedItem, boolean forceUse)
	{
		if(skill == null)
			return;
		if(!skill.isCastTimeEffect())
			_castInterruptTime = 0;

		if(_forceBuff != null)
		{
			_forceBuff.delete();
			return;
		}

		if(skill.isMuted(this))
		{
			if(isNpc())
				abortCast();
			else
			{
				breakCast(true, false);
				if(isPet() || isSummon())
				{
					L2Player player = getPlayer();
					L2Summon thisSummon = (L2Summon) this;
					if(player == null || thisSummon.getDistance(player) < Config.PLAYER_VISIBILITY)
						getAI().setIntention(AI_INTENTION_ACTIVE);
				}
			}
			return;
		}

		if(!skill.checkCondition(this, aimingTarget, usedItem, forceUse, false))
		{
			if(isPet() || isSummon())
			{
				L2Player player = getPlayer();
				L2Summon thisSummon = (L2Summon) this;
				if(player == null || thisSummon.getDistance(player) < Config.PLAYER_VISIBILITY)
					getAI().setIntention(AI_INTENTION_ACTIVE);
			}
			return;
		}

		List<L2Character> targets = skill.getTargets(this, aimingTarget, forceUse);

		double mpConsume2 = skill.getMpConsume2();
		int hpConsume = skill.getHpConsume();

		if(hpConsume > 0)
		{
			synchronized(this)
			{
				decreaseHp(hpConsume >= _currentHp ? _currentHp - 2 : hpConsume, this, true, true);
			}
		}

		decreaseSouls(skill.getSoulsConsume());
		//setConsumedSouls(getConsumedSouls() - skill.getSoulsConsume(), null);

		if(mpConsume2 > 0)
		{
			mpConsume2 = Formulas.calcSkillMpConsume(this, skill, mpConsume2, false);
			if(_currentMp < mpConsume2)
			{
				sendPacket(Msg.NOT_ENOUGH_MP);
				return;
			}
			reduceCurrentMp(mpConsume2, null);
		}

		callSkill(skill, targets, usedItem);
		setPrevLoc(null);

		if(skill.getMaxSoulsConsume() > 0 && getConsumedSouls() > 0)
			decreaseSouls(skill.getMaxSoulsConsume());
		//setConsumedSouls(getConsumedSouls() - skill.getMaxSoulsConsume(), null);

		removeSkillMastery(skill.getId());

		if(aimingTarget != this && aimingTarget._skillsOnUnderAttack != null && aimingTarget._skillsOnUnderAttack.size() > 0 && skill.isOffensive())
			for(L2Skill chance : aimingTarget._skillsOnUnderAttack.values())
				if(chance.isOnMagicAttacked())
					chance.useChanceSkill(new Env(aimingTarget, this, skill));

		int skillCoolTime = skill.isCastTimeEffect() ? skill.getCoolTime() : (int) (skill.getCoolTime() / Formulas.calcCastSpeedFactor(this, skill));
		if(skillCoolTime > 0)
			_skillCoolTask = ThreadPoolManager.getInstance().scheduleAi(new Runnable()
			{
				public void run()
				{
					onCastEndTime();
				}
			}, skillCoolTime, isPlayer());
		else
			onCastEndTime();
	}

	public void onCastEndTime()
	{
		final L2Skill skill = _castingSkill;
		_castEndTime = 0;
		_castingSkill = null;
		_castingItem = null;
		_skillTask = null;
		_skillCoolTask = null;
		if(_forceBuff != null)
			_forceBuff.delete();
		ThreadPoolManager.getInstance().executeAi(new NotifyAITask(this, CtrlEvent.EVT_FINISH_CASTING, skill, null), isPlayer());
	}

	/**
	 * Reduce the current HP of the L2Character and launch the doDie Task if
	 * necessary.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li> L2NpcInstance : Update the attacker AggroInfo of the L2NpcInstance
	 * _aggroList</li>
	 * <BR>
	 * <BR>
	 *
	 * @param damage   The HP decrease value
	 * @param attacker The L2Character who attacks
	 * @param directHp If True : hit on HP instead of CP first
	 * @param reflect
	 */
	public void reduceHp(double damage, L2Character attacker, boolean directHp, boolean reflect)
	{
		if(attacker instanceof L2Playable || attacker instanceof L2CubicInstance)
		{
			L2Playable pAttacker;
			if(attacker instanceof L2Playable)
			{
				pAttacker = (L2Playable) attacker;

				if(attacker.getPlayer() != null)
				{
					L2Player player = attacker.getPlayer();
					for(L2CubicInstance cubic : player.getCubics())
						if(cubic != null)
							cubic.addAggro(this, (int) damage);

					if(player.isInDuel() && player.getDuel() != getDuel())
						player.setDuelState(Duel.DUELSTATE_INTERRUPTED);
				}

				// Flag the attacker if it's a L2Player outside a PvP area
				if(!isDead() && pAttacker.checkPvP(this, null))
					pAttacker.startPvPFlag(this);
			}
		}

		decreaseHp(damage, attacker, directHp, reflect);
	}

	public void decreaseHp(double damage, L2Character attacker, boolean directHp, boolean reflect)
	{
		if(damage < 1)
			return;

		if(fireMethodInvoked(MethodCollection.ReduceCurrentHp, new Object[]{damage, attacker, directHp}) == MethodInvocationResult.BLOCK)
			return;

		if(isDead() || isInvul())
			return;

		// 5182 = Blessing of protection, работает если разница уровней больше 10 и не в зоне осады
		L2Player playerAttacker;
		if(attacker != null && (playerAttacker = attacker.getPlayer()) != null && Math.abs(playerAttacker.getLevel() - getLevel()) >= 10)
		{
			// ПК не может нанести урон чару с блессингом
			if(playerAttacker.getKarma() > 0 && getEffectBySkillId(5182) != null && !isInSiege())
				return;
			// чар с блессингом не может нанести урон ПК
			if(getKarma() > 0 && playerAttacker.getEffectBySkillId(5182) != null && !attacker.isInSiege())
				return;
		}

		if(isSleeping() && attacker != this)
			stopEffects("sleep");

		if(attacker != this)
			stopEffects("meditation");

		if(attacker != this)
			startAttackStanceTask();

		if(isOverhit() && attacker.getPlayer() != null)
			setOverhitValues(attacker.getPlayer(), damage);
		else
			setOverhitEnabled(false);

		if(!reflect && attacker != null && attacker != this)
		{
			if(_skillsOnDamage != null && _skillsOnDamage.size() > 0)
				for(L2Skill chance : _skillsOnDamage.values())
				{
					Env env = new Env(this, attacker, null);
					env.value = damage;
					chance.useChanceSkill(env);
				}

			Calculator triggerByDmg = _calculators[Stats.TRIGGER_BY_DMG.ordinal()];
			if(triggerByDmg != null && triggerByDmg.size() > 0)
			{
				Env env = new Env(this, attacker, null);
				env.value = damage;
				for(Func func : triggerByDmg.getFunctions())
					func.calc(env);
			}
		}

		double temp = calcStat(Stats.DECREASE_DAMAGE_PER_MP, 0, attacker, null) * damage * 0.01;
		if(temp > 0 && getCurrentMp() >= temp)
		{
			reduceCurrentMp(temp, attacker);
			return;
		}

		double hpDamage;

		if(attacker instanceof L2Playable || attacker instanceof L2CubicInstance)
		{
			if(!directHp)
			{
				double d = damage;
				damage = _currentCp - damage;
				hpDamage = damage;

				if(Config.ALT_ONE_SHOT_KILL_PROTECT && isPlayer() && _currentCp > getMaxCp() * 0.3 && Math.abs(d) >= Config.ALT_ONE_SHOT_KILL_PROTECT_DAMAGE && hpDamage < 0)
				{
					hpDamage = 0;
				}

				if(hpDamage < 0)
					hpDamage *= -1;

				if(damage < 0)
					damage = 0;

				setCurrentCp(damage);
			}
			else
				hpDamage = damage;

			if(_currentCp == 0 || directHp)
			{
				hpDamage = _currentHp - hpDamage;

				if(hpDamage < 0)
					hpDamage = 0;

				if((isNpc() || this instanceof L2Summon) && hasAI())
					getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, attacker, (_currentHp - hpDamage));

				if(hpDamage == 0 && isPlayer() && isInDuel())
				{
					stopHpMpRegeneration();
					getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
					sendPacket(Msg.ActionFail);
					stopMove();
					setTarget(null);
					abortAttack();
					abortCast();
					// let the DuelManager know of his defeat
					getDuel().onPlayerDefeat((L2Player) this);
					hpDamage = 1;
				}

				setCurrentHp(hpDamage);
			}
		}
		else
		{
			damage = _currentHp - damage;

			if(damage < 0)
				damage = 0;

			if((isNpc() || this instanceof L2Summon) && hasAI())
				getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, attacker, damage);

			setCurrentHp(damage);
		}

		if(isDead())
		{
			if(isPlayer() && ((L2Player) this).isInOlympiadMode())
			{
				_currentHp = 0;
				stopMove();
				stopHpMpRegeneration();
				if(Olympiad.getRegisteredGameType((L2Player) this) == 0)
					broadcastPacket(new Die(this));
				return;
			}
			// killing is only possible one time
			synchronized(this)
			{
				if(_killedAlready)
					return;

				_killedAlready = true;
			}

			stopMove();
			doDie(attacker != null && attacker.isCubic() ? attacker.getPlayer() : attacker);
			_currentHp = 0;
		}
		else
			setOverhitEnabled(false);
	}

	/**
	 * Reduce the current MP of the L2Character.<BR><BR>
	 *
	 * @param i		The MP decrease value
	 * @param attacker L2Character
	 */
	public void reduceCurrentMp(double i, L2Character attacker)
	{
		if(attacker != null && attacker != this)
		{
			if(isSleeping())
				stopEffects("sleep");
			stopEffects("meditation");
		}

		// 5182 = Blessing of protection, работает если разница уровней больше 10 и не в зоне осады
		if(attacker != null && attacker.isPlayer() && Math.abs(attacker.getLevel() - getLevel()) >= 10)
		{
			// ПК не может нанести урон чару с блессингом
			if(attacker.getKarma() > 0 && getEffectBySkillId(5182) != null && !isInSiege())
				return;
			// чар с блессингом не может нанести урон ПК
			if(getKarma() > 0 && attacker.getEffectBySkillId(5182) != null && !attacker.isInSiege())
				return;
		}

		i = _currentMp - i;

		if(i < 0)
			i = 0;

		setCurrentMp(i);
	}

	public void removeAllSkills()
	{
		for(L2Skill s : getAllSkills())
			removeSkill(s);
	}

	public L2Effect getEffectByAbnormalType(String est)
	{
		return _effects.getEffectByAbnormalType(est);
	}

	public L2Effect getEffectBySkill(L2Skill skill)
	{
		return getEffectBySkillId(skill.getId());
	}

	public L2Effect getEffectBySkillId(int skillId)
	{
		return _effects.getEffectBySkillId(skillId);
	}

	public ConcurrentLinkedQueue<L2Effect> getAllEffects()
	{
		return _effects.getAllEffects();
	}

	public int getAbnormalLevelBySkill(L2Skill skill)
	{
		L2Effect effect = getEffectBySkill(skill);
		if(effect == null)
			return 0;

		return effect.getAbnormalLevel();
	}

	public int getAbnormalLevelByType(int skillId)
	{
		L2Skill skill = SkillTable.getInstance().getInfo(skillId, 1);
		if(skill == null)
			return -1;

		for(String abnormal : skill.getAbnormalTypes())
		{
			L2Effect effect = getEffectByAbnormalType(abnormal);
			if(effect != null && effect.getSkill().getAbnormalTypes().contains(abnormal))
				return effect.getAbnormalLevel();
		}

		return -1;
	}

	/**
	 * Возвращает первые эффекты для всех скиллов. Нужно для отображения не
	 * более чем 1 иконки для каждого скилла.
	 */
	public L2Effect[] getAllEffectsArray()
	{
		return _effects.getAllEffectsArray();
	}

	public void addEffect(L2Effect newEffect)
	{
		addEffect(newEffect, 1);
	}

	public void addEffect(L2Effect newEffect, int effectTimeModifier)
	{
		_effects.addEffect(newEffect, effectTimeModifier);
	}

	/**
	 * @param effect эффект для удаления
	 * @see ru.l2gw.gameserver.model.L2Effect
	 */
	public void removeEffect(L2Effect effect)
	{
		if(effect == null)
			return;

		_effects.removeEffect(effect);
	}

	public void stopAllEffects()
	{
		_effects.stopAllEffects();
	}

	public void stopEffects()
	{
		_effects.stopEffects();
	}

	public void stopEffect(int skillId)
	{
		_effects.stopEffect(skillId);
	}

	public void stopEffects(String... abnormalTypes)
	{
		_effects.stopEffects(abnormalTypes);
	}

	public void dispelByAbnormal(int skillIndex)
	{
		dispelByAbnormal(SkillTable.getInstance().getInfo(skillIndex));
	}

	public void dispelByAbnormal(L2Skill skill)
	{
		if(skill == null)
			return;

		for(String abnormal : skill.getAbnormalTypes())
			_effects.stopEffects(abnormal);
	}

	public void stopEffectsByName(String effectName)
	{
		_effects.stopEffectsByName(effectName);
	}

	/**
	 * Remove a skill from the L2Character and its Func objects from calculator
	 * set of the L2Character.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills own by a L2Character are identified in <B>_skills</B><BR>
	 * <BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove the skill from the L2Character _skills </li>
	 * <li>Remove all its Func objects from the L2Character calculator set</li>
	 * <BR>
	 * <BR>
	 * <p/>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li> L2Player : Save update in the character_skills table of the database</li>
	 * <BR>
	 * <BR>
	 *
	 * @param skill The L2Skill to remove from the L2Character
	 * @return The L2Skill removed
	 */
	public L2Skill removeSkill(L2Skill skill)
	{
		if(skill == null)
			return null;

		return removeSkillById(skill.getId());
	}

	public L2Skill removeSkillById(Integer id)
	{
		// Remove the skill from the L2Character _skills
		L2Skill oldSkill = _skills.remove(id);
		removeChanceSkill(id);

		// Remove all its Func objects from the L2Character calculator set
		if(oldSkill != null)
			removeStatsOwner(oldSkill);

		return oldSkill;
	}

	public void removeChanceSkill(int id)
	{
		if(_skillsOnAttack != null)
			_skillsOnAttack.remove(id);
		if(_skillsOnMagicAttack != null)
			_skillsOnMagicAttack.remove(id);
		if(_skillsOnUnderAttack != null)
			_skillsOnUnderAttack.remove(id);
		if(_skillsOnEvaded != null)
			_skillsOnEvaded.remove(id);
		if(_skillsOnShield != null)
			_skillsOnShield.remove(id);
		if(_skillsOnDamage != null)
			_skillsOnDamage.remove(id);
	}

	/**
	 * Remove all Func objects with the selected owner from the Calculator set
	 * of the L2Character.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A L2Character owns a table of Calculators called <B>_calculators</B>.
	 * Each Calculator (a calculator per state) own a table of Func object. A
	 * Func object is a mathematic function that permit to calculate the
	 * modifier of a state (ex : REGENERATE_HP_RATE...). To reduce cache memory
	 * use, L2NpcInstances who don't have skills share the same Calculator set
	 * called <B>NPC_STD_CALCULATOR</B>.<BR>
	 * <BR>
	 * <p/>
	 * That's why, if a L2NpcInstance is under a skill/spell effect that modify
	 * one of its state, a copy of the NPC_STD_CALCULATOR must be create in its
	 * _calculators before addind new Func object.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove all Func objects of the selected owner from _calculators</li>
	 * <BR>
	 * <BR>
	 * <li>If L2Character is a L2NpcInstance and _calculators is equal to
	 * NPC_STD_CALCULATOR, free cache memory and just create a link on
	 * NPC_STD_CALCULATOR in _calculators</li>
	 * <BR>
	 * <BR>
	 * <p/>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li> Unequip an item from inventory</li>
	 * <li> Stop an active skill</li>
	 * <BR>
	 * <BR>
	 *
	 * @param owner The Object(Skill, Item...) that has created the effect
	 */
	public final void removeStatsOwner(Object owner)
	{
		synchronized(_calculators)
		{
			// Go through the Calculator set
			for(int i = 0; i < _calculators.length; i++)
				if(_calculators[i] != null)
				{
					// Delete all Func objects of the selected owner
					_calculators[i].removeOwner(owner);

					if(_calculators[i].size() == 0)
						_calculators[i] = null;
				}
		}
	}

	/**
	 * Remove the object from the list of L2Character that must be informed of
	 * HP/MP updates of this L2Character.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Each L2Character owns a list called <B>_statusListener</B> that contains
	 * all L2Player to inform of HP/MP updates. Players who must be informed are
	 * players that target this L2Character. When a RegenTask is in progress
	 * sever just need to go through this list to send Server->Client packet
	 * StatusUpdate.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li> Untarget a PC or NPC</li>
	 * <BR>
	 * <BR>
	 *
	 * @param object L2Character to add to the listener
	 */
	public void removeStatusListener(L2Character object)
	{
		synchronized(this)
		{
			if(getStatusListener() == null)
				return;
			getStatusListener().remove(object);
			if(getStatusListener() != null && getStatusListener().isEmpty())
				setStatusListener(null);
		}
	}

	public void sendActionFailed()
	{
		sendPacket(Msg.ActionFail);
	}

	public L2CharacterAI setAI(L2CharacterAI new_ai)
	{
		if(new_ai == null)
			return _ai = null;
		if(_ai != null)
			_ai.stopAITask();
		_ai = new_ai;
		return _ai;
	}

	/**
	 * Set the current CP of the L2Character.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the Max CP of the L2Character </li>
	 * <li>Set the RegenActive flag to True/False </li>
	 * <li>Launch/Stop the HP/MP/CP Regeneration task with Medium priority</li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP
	 * to all other L2Player to inform</li>
	 * <BR>
	 * <BR>
	 */
	public final void setCurrentCp(double newCp)
	{
		// Get the Max CP of the L2Character
		int maxCp = getMaxCp();

		if(newCp < 0)
			newCp = 0;

		synchronized(this)
		{
			if(newCp >= maxCp)
			{
				// Set the RegenActive flag to false
				_currentCp = maxCp;
				_flagsRegenActive &= ~CP_REGEN_FLAG;

				// Stop the HP/MP/CP Regeneration task
				if(_flagsRegenActive == 0)
					stopHpMpRegeneration();
			}
			else
			{
				// Set the RegenActive flag to true
				_currentCp = newCp;
				_flagsRegenActive |= CP_REGEN_FLAG;

				// Start the HP/MP/CP Regeneration task with Medium priority
				startHpMpRegeneration();
			}
		}

		// Send the Server->Client packet StatusUpdate with current HP and MP to
		// all other L2Player to inform
		broadcastStatusUpdate();
	}

	/**
	 * Set the current HP of the L2Character.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the Max HP of the L2Character </li>
	 * <li>Set the RegenActive flag to True/False </li>
	 * <li>Launch/Stop the HP/MP/CP Regeneration task with Medium priority</li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP
	 * to all other L2Player to inform</li>
	 * <BR>
	 * <BR>
	 */
	public void setCurrentHp(double newHp)
	{
		// Get the Max HP of the L2Character
		int maxHp = getMaxHp();
		double hpStart = _currentHp;

		if(isNpc() && ((L2NpcInstance) this).getTemplate().undying == 1 && newHp < 1)
			newHp = 1;

		if(newHp > maxHp)
			newHp = maxHp;

		firePropertyChanged(PropertyCollection.HitPoints, hpStart, _currentHp);

		synchronized(this)
		{
			if(newHp >= maxHp)
			{
				// Set the RegenActive flag to false
				_currentHp = maxHp;
				_flagsRegenActive &= ~HP_REGEN_FLAG;
				_killedAlready = false;

				// Stop the HP/MP/CP Regeneration task
				if(_flagsRegenActive == 0)
					stopHpMpRegeneration();
			}
			else
			{
				// Set the RegenActive flag to true
				_currentHp = newHp;
				_flagsRegenActive |= HP_REGEN_FLAG;

				if(!isDead())
					_killedAlready = false;

				// Start the HP/MP/CP Regeneration task with Medium priority
				startHpMpRegeneration();
			}
		}

		checkHpMessages(hpStart, newHp);

		// Send the Server->Client packet StatusUpdate with current HP and MP to
		// all other L2Player to inform
		broadcastStatusUpdate();
	}

	/**
	 * Set the current HP and MP of the L2Character, Launch/Stop a HP/MP/CP
	 * Regeneration Task and send StatusUpdate packet to all other L2Player to
	 * inform (exclusive broadcast).<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the Max HP and MP of the L2Character </li>
	 * <li>Set the RegenActive flag to True/False </li>
	 * <li>Launch/Stop the HP/MP/CP Regeneration task with Medium priority</li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP
	 * to all other L2Player to inform</li>
	 * <BR>
	 * <BR>
	 * <p/>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND
	 * Server->Client StatusUpdate packet to the L2Character</B></FONT><BR>
	 * <BR>
	 *
	 * @param newHp The new HP value of the L2Player
	 * @param newMp The new MP value of the L2Player
	 */
	public void setCurrentHpMp(double newHp, double newMp)
	{
		// Get the Max HP and MP of the L2Character
		int maxHp = getMaxHp();
		int maxMp = getMaxMp();
		double curHp = _currentHp;

		synchronized(this)
		{
			if(newHp >= maxHp)
			{
				// Set the RegenActive flag to false
				_killedAlready = false;
				_currentHp = maxHp;
				_flagsRegenActive &= ~HP_REGEN_FLAG;
			}
			else
			{
				_currentHp = newHp;

				if(!isDead())
				{
					// Set the RegenActive flag to true
					_killedAlready = false;
					_flagsRegenActive |= HP_REGEN_FLAG;

					// Start the HP/MP/CP Regeneration task with Medium priority
					startHpMpRegeneration();
				}
			}

			if(newMp >= maxMp)
			{
				// Set the RegenActive flag to false
				_currentMp = maxMp;
				_flagsRegenActive &= ~MP_REGEN_FLAG;
			}
			else
			{
				_currentMp = newMp;

				if(!isDead())
				{
					// Set the RegenActive flag to true
					_flagsRegenActive |= MP_REGEN_FLAG;

					// Start the HP/MP/CP Regeneration task with Medium priority
					startHpMpRegeneration();
				}
			}

			// Stop the HP/MP/CP Regeneration task
			if(_flagsRegenActive == 0)
				stopHpMpRegeneration();
		}

		firePropertyChanged(PropertyCollection.HitPoints, curHp, _currentHp);
		checkHpMessages(curHp, newHp);

		// Send the Server->Client packet StatusUpdate with current HP and MP to
		// all other L2Player to inform
		broadcastStatusUpdate();

		if(isPlayer() && getLevel() < 6 && getCurrentHp() > getMaxHp() / 2 && isSitting())
		{
			Quest q = QuestManager.getQuest(255);
			if(q != null)
			{
				QuestState qs = getPlayer().getQuestState(q.getName());
				if(qs != null)
				{
					if((qs.getInt("t") & 0x800000) == 0x800000 && (qs.getInt("t") & 0x100) != 0x100)
						((L2Player) this).processQuestEvent(q.getName(), "TE" + 0x800000);
				}
			}
		}
	}

	public final void setCurrentMp(double newMp)
	{
		// Get the Max MP of the L2Character
		int maxMp = getMaxMp();

		if(newMp < 0)
			newMp = 0;

		synchronized(this)
		{
			if(newMp >= maxMp)
			{
				// Set the RegenActive flag to false
				_currentMp = maxMp;
				_flagsRegenActive &= ~MP_REGEN_FLAG;

				// Stop the HP/MP/CP Regeneration task
				if(_flagsRegenActive == 0)
					stopHpMpRegeneration();
			}
			else
			{
				// Set the RegenActive flag to true
				_currentMp = newMp;
				_flagsRegenActive |= MP_REGEN_FLAG;

				// Start the HP/MP/CP Regeneration task with Medium priority
				startHpMpRegeneration();
			}
		}

		// Send the Server->Client packet StatusUpdate with current HP and MP to
		// all other L2Player to inform
		broadcastStatusUpdate();
	}

	/**
	 * Set the current MP of the L2Character.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the Max HP of the L2Character </li>
	 * <li>Set the RegenActive flag to True/False </li>
	 * <li>Launch/Stop the HP/MP/CP Regeneration task with Medium priority</li>
	 * <li>Send the Server->Client packet StatusUpdate with current HP and MP
	 * to all other L2Player to inform</li>
	 * <BR>
	 * <BR>
	 */
	public final void setCurrentMp(Double newMp)
	{
		setCurrentMp((double) newMp);
	}

	/**
	 * Set the L2Character flying mode to True.<BR>
	 * <BR>
	 */
	public final void setFlying(boolean mode)
	{
		_flying = mode;
	}

	/**
	 * Set the orientation of the L2Character.<BR>
	 * <BR>
	 */
	public final void setHeading(int heading)
	{
		_heading = heading;
	}

	/**
	 * Set the orientation of the L2Character to the target.<BR>
	 * <BR>
	 */
	public final void setHeading(L2Character target, boolean toChar)
	{
		if(target == null || target == this)
			return;

		_heading = (int) (Math.atan2(getY() - target.getY(), getX() - target.getX()) * 32768. / Math.PI) + (toChar ? 32768 : 0);
		if(_heading < 0)
			_heading += 65536;
	}

	public final void setIsBlessedByNoblesse(boolean value)
	{
		if(value)
			_isBlessedByNoblesse++;
		else
			_isBlessedByNoblesse--;
	}

	public final void setIsSalvation(boolean value)
	{
		if(value)
			_isSalvation++;
		else
			_isSalvation--;
	}

	/**
	 * Set the invulnerability Flag of the L2Character.<BR>
	 */
	public void setIsInvul(boolean b)
	{
		_isInvul = b;
	}

	public final void setIsPendingRevive(boolean value)
	{
		_isPendingRevive = value;
	}

	public final void setIsTeleporting(boolean value)
	{
		_isTeleporting = value;
	}

	public final void setName(String name)
	{
		_name = name;
	}

	public L2Character getCastingTarget()
	{
		if(_castingTarget == null)
			return null;

		L2Character c = _castingTarget.get();
		if(c == null)
			_castingTarget = null;

		return c;
	}

	public void setCastingSkill(L2Skill skill)
	{
		_castingSkill = skill;
	}

	public void setCastingTarget(L2Character target)
	{
		_castingTarget = target == null ? null : new WeakReference<L2Character>(target);
	}

	/**
	 * Set the L2Character riding mode to True.<BR>
	 * <BR>
	 */
	public final void setRiding(boolean mode)
	{
		_riding = mode;
	}

	/**
	 * Set the L2Character movement type to run and send Server->Client packet
	 * ChangeMoveType to all others L2Player.<BR>
	 * <BR>
	 */
	public final void setRunning()
	{
		if(!_running)
		{
			_running = true;
			broadcastPacket(new ChangeMoveType(this));
			if(isPlayer())
				((L2Player) this).sendUserInfo(true);
		}
	}

	public void setSkillMastery(Integer skill, byte mastery)
	{
		_skillMastery.put(skill, mastery);
	}

	private void setStatusListener(CopyOnWriteArraySet<L2Character> value)
	{
		_statusListener = value;
	}

	public boolean setTarget(L2Object object)
	{
		return setTarget(object, false);
	}

	public boolean setTarget(L2Object object, boolean manual)
	{
		if(object != null && !object.isVisible() && object != this)
			object = null;
		// If object==null, Cancel Attak or Cast
		if(object == null)
		{
			if(getTargetId() > 0)
				broadcastPacket(new TargetUnselected(this, manual));
			if(isAttackingNow() && getAI().getAttackTarget() == getTarget())
			{
				abortAttack();
				getAI().setIntention(AI_INTENTION_ACTIVE, null, null);
			}
			if(isCastingNow() && canAbortCast() && getAI().getAttackTarget() == getTarget())
			{
				abortCast();
				getAI().setIntention(AI_INTENTION_ACTIVE, null, null);
			}
			targetStoredId = 0;
		}
		else if(object != getTarget())
		{
			targetStoredId = object.getStoredId();
			broadcastPacketToOthers(new TargetSelected(getObjectId(), object.getObjectId(), getLoc()));
		}
		return true;
	}

	/**
	 * Set the template of the L2Character.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Each L2Character owns generic and static properties (ex : all Keltir have
	 * the same number of HP...). All of those properties are stored in a
	 * different template for each type of L2Character. Each template is loaded
	 * once in the server cache memory (reduce memory use). When a new instance
	 * of L2Character is spawned, server just create a link between the instance
	 * and the template This link is stored in <B>_template</B><BR>
	 * <BR>
	 * <p/>
	 * <B><U> Assert </U> :</B><BR>
	 * <BR>
	 * <li> isCharacter</li>
	 * <BR>
	 * <BR
	 */
	protected void setTemplate(L2CharTemplate template)
	{
		_template = template;
	}

	/**
	 * Set the Title of the L2Character.<BR>
	 * <BR>
	 *
	 * @param title The text to set as title
	 */
	public void setTitle(String title)
	{
		_title = title;
	}

	/**
	 * Set the L2Character movement type to walk and send Server->Client packet
	 * ChangeMoveType to all others L2Player.<BR>
	 * <BR>
	 */
	public void setWalking()
	{
		if(_running)
		{
			_running = false;
			broadcastPacket(new ChangeMoveType(this));
			if(isPlayer())
				((L2Player) this).sendUserInfo(true);
		}
	}

	/**
	 * Active abnormal effects flags in the binary mask and send Server->Client
	 * UserInfo/CharInfo packet.<BR>
	 * <BR>
	 */
	public void startAbnormalEffect(AbnormalVisualEffect ve)
	{
		//_abnormalVE[ve.ordinal()]++;
		//_abnormalEffects |= ve.mask;
		_abnormalEffectsArray.clear();
		updateAbnormalEffect();
		_abnormalVE[ve.ordinal()]++;
		_abnormalEffects |= ve.mask;
		_abnormalEffectsArray.add(ve);
		updateAbnormalEffect();
	}

	@Override
	public void startAttackStanceTask()
	{
		if(System.currentTimeMillis() < _stanceInited + 10000)
			return;

		_stanceInited = System.currentTimeMillis();

		// Бесконечной рекурсии не будет, потому что выше проверка на
		// _stanceInited
		if(this instanceof L2Summon && getPlayer() != null)
			getPlayer().startAttackStanceTask();
		else if(isPlayer() && getPet() != null)
			getPet().startAttackStanceTask();

		if(_stanceTask != null)
			try
			{
				_stanceTask.cancel(false);
			}
			catch(NullPointerException e)
			{
			}
		else
			broadcastPacket(new AutoAttackStart(getObjectId()));

		_stanceTask = ThreadPoolManager.getInstance().scheduleAi(new CancelAttackStance(this), 15000, isPlayer());
	}

	/**
	 * Start the HP/MP/CP Regeneration task.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Calculate the regen task period </li>
	 * <li>Launch the HP/MP/CP Regeneration task with Medium priority </li>
	 * <BR>
	 * <BR>
	 */
	protected synchronized void startHpMpRegeneration()
	{
		if(_regTask == null && !isDead())
		{
			if(Config.DEBUG)
				_log.debug("HP/MP/CP regen started");

			// Get the Regeneration periode
			int period = Formulas.getRegeneratePeriod(this);

			// Create the HP/MP/CP Regeneration task
			_regTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new RegenTask(), period, period);
		}
	}

	/**
	 * Modify the abnormal effect map according to the mask.<BR>
	 * <BR>
	 */
	public void stopAbnormalEffect(AbnormalVisualEffect ve)
	{
		
		_abnormalVE[ve.ordinal()]--;
		if(_abnormalVE[ve.ordinal()] <= 0)
		{
			_abnormalEffectsArray.remove(ve);
			_abnormalEffects &= ~ve.mask;
			updateAbnormalEffect();
		}
	}

	/**
	 * Stop the HP/MP/CP Regeneration task.<BR>
	 * <BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the RegenActive flag to False </li>
	 * <li>Stop the HP/MP/CP Regeneration task </li>
	 * <BR>
	 * <BR>
	 */
	public synchronized void stopHpMpRegeneration()
	{
		if(_regTask != null)
		{
			// Stop the HP/MP/CP Regeneration task
			_regTask.cancel(true);
			_regTask = null;

			// Set the RegenActive flag to false
			_flagsRegenActive = 0;

			if(Config.DEBUG)
				_log.debug("HP/MP/CP regen stop");
		}
	}

	public void block()
	{
		if(_blockTask != null)
		{
			try
			{
				_blockTask.cancel(true);
				_blockTask = null;
			}
			catch(NullPointerException e)
			{
				// quite
			}
		}
		_blocked = true;
	}

	public void block(long msec)
	{
		block();
		_blockTask = ThreadPoolManager.getInstance().scheduleGeneral(new UnblockTask(this), msec);
	}

	public void unblock()
	{
		if(_blockTask != null)
		{
			try
			{
				_blockTask.cancel(true);
				_blockTask = null;
			}
			catch(NullPointerException e)
			{
				// quite
			}
		}
		_blocked = false;
	}

	public void startConfused()
	{
		if(!_confused)
		{
			_confused = true;
			startAttackStanceTask();
			updateAbnormalEffect();
		}
	}

	public void stopConfused()
	{
		if(_confused)
		{
			_confused = false;
			updateAbnormalEffect();

			breakAttack();
			breakCast(true, false);
			stopMove();
			getAI().setAttackTarget(null);
		}
	}

	public void startFakeDeath()
	{
		if(!_fakeDeath)
		{
			_fakeDeath = true;
			getAI().notifyEvent(CtrlEvent.EVT_FAKE_DEATH, null, null);
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_START_FAKEDEATH));
			updateAbnormalEffect();
		}
	}

	public void stopFakeDeath()
	{
		if(_fakeDeath)
		{
			_fakeDeath = false;
			broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STOP_FAKEDEATH));
			updateAbnormalEffect();
		}
	}

	public void startFear()
	{
		if(!_afraid)
		{
			_afraid = true;
			breakAttack();
			breakCast(true, true);
			sendActionFailed();
			stopMove();
			startAttackStanceTask();
			updateAbnormalEffect();
		}
	}

	public void stopFear()
	{
		if(_afraid)
		{
			_afraid = false;
			updateAbnormalEffect();
		}
	}

	public void setImobilised(boolean imobilised)
	{
		if(_imobilised != imobilised)
		{
			_imobilised = imobilised;
			if(imobilised)
				stopMove();
			updateAbnormalEffect();
		}
	}

	public void setDisabled(boolean disabled)
	{
		_disabled = disabled;
	}

	/**
	 * Set the overloaded status of the L2Character is overloaded (if True, the
	 * L2Player can't take more item).<BR>
	 * <BR>
	 */
	public void setOverloaded(boolean overloaded)
	{
		_overloaded = overloaded;
	}

	public boolean isConfused()
	{
		return _confused;
	}

	public boolean isFakeDeath()
	{
		return _fakeDeath;
	}

	public boolean isAfraid()
	{
		return _afraid;
	}

	public boolean isBlocked()
	{
		return _blocked;
	}

	public boolean isMuted()
	{
		return isStatActive(Stats.BLOCK_SPELL);
	}

	public boolean isPMuted()
	{
		return isStatActive(Stats.BLOCK_PHYS_SKILLS);
	}

	public boolean isRooted()
	{
		return (_abnormalEffects & AbnormalVisualEffect.root.mask) == AbnormalVisualEffect.root.mask || (_abnormalEffects & AbnormalVisualEffect.av2_root.mask) == AbnormalVisualEffect.av2_root.mask;
	}

	public boolean isSleeping()
	{
		return (_abnormalEffects & AbnormalVisualEffect.sleep.mask) == AbnormalVisualEffect.sleep.mask;
	}

	public boolean isStunned()
	{
		return (_abnormalEffects & AbnormalVisualEffect.stun.mask) == AbnormalVisualEffect.stun.mask || (_abnormalEffects & AbnormalVisualEffect.danceStun.mask) == AbnormalVisualEffect.danceStun.mask || (_abnormalEffects & AbnormalVisualEffect.av2_stun.mask) == AbnormalVisualEffect.av2_stun.mask;
	}

	public boolean isActionsBlocked()
	{
		return calcStat(Stats.BLOCK_ACT, 0, null, null) > 0;
	}

	public boolean isParalyzed()
	{
		return (_abnormalEffects & AbnormalVisualEffect.paralyze.mask) == AbnormalVisualEffect.paralyze.mask || (_abnormalEffects & AbnormalVisualEffect.stone.mask) == AbnormalVisualEffect.stone.mask;
	}

	public boolean isImobilised()
	{
		return _imobilised || getMoveSpeed() < 1;
	}

	public boolean isDisabled()
	{
		return _disabled;
	}
	
	public EffectList getEffectList()
	{
		if (_effectList == null)
			synchronized (this)
			{
				if (_effectList == null)
					_effectList = new EffectList(this);
			}

		return _effectList;
	}

	/**
	 * Return True if the L2Character is casting.<BR>
	 * <BR>
	 */
	public final boolean isCastingNow()
	{
		return _castEndTime > System.currentTimeMillis();
	}

	/**
	 * Return True if the L2Character can't move (stun, root, sleep, overload,
	 * paralyzed).<BR>
	 * <BR>
	 */
	public boolean isMovementDisabled()
	{
		return isStatActive(Stats.BLOCK_MOVE) || isActionsBlocked() || isSitting() || isStunned() || isRooted() || isSleeping() || isParalyzed() || isImobilised() || isAlikeDead() || _overloaded || isDisabled() || isAttackingNow() || isCastingNow();
	}

	/**
	 * Return True if the L2Character can't use its skills (ex : stun,
	 * sleep...).<BR>
	 * <BR>
	 */
	public boolean isActionsDisabled()
	{
		return isActionsBlocked() || isStunned() || isSleeping() || isParalyzed() || isAlikeDead() || isDisabled() || isAttackingNow() || isCastingNow();
	}

	public boolean isPotionsDisabled()
	{
		return isActionsBlocked() || isStunned() || isSleeping() || isParalyzed() || isAlikeDead() || isAfraid();
	}

	/**
	 * Return True if the L2Character can't attack (stun, sleep, reuse,onBoat,target is on boat).<BR>
	 * <BR>
	 */
	public boolean isAttackingDisabled()
	{
		if(_attackReuseEndTime > System.currentTimeMillis())
			return true;
		if(isInBoat() && !isInAirShip())
			return true;
		L2Object target = getTarget();
		return target != null && (target.isPlayer() || target.isSummon() || target.isPet()) && target.getPlayer() != null && target.getPlayer().isInBoat() && !target.getPlayer().isInAirShip() || isStatActive(Stats.BLOCK_PHYS_ATTACK) || (isPlayer() && ((L2Player) this).isCombatFlagEquipped());
	}

	/**
	 * Return True if the L2Character can't be controlled by the player
	 * (confused, affraid).<BR>
	 * <BR>
	 */
	public boolean isOutOfControl()
	{
		return calcStat(Stats.BLOCK_CONTROL, 0, null, null) > 0 || isConfused() || isAfraid() || isBlocked();
	}

	public void teleToLocation(Location loc)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), getReflection(), isFloating());
	}

	public void teleToLocation(Location loc, int reflection)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), reflection, isFloating());
	}

	public void teleToLocation(int x, int y, int z)
	{
		teleToLocation(x, y, z, getReflection(), isFloating());
	}

	public void teleToLocation(int x, int y, int z, int reflection, boolean flying)
	{
		if(!flying && !isVehicle())
		{
			if(Config.GEODATA_DEBUG && "Rage".equals(getName()))
				_log.info("teleToLocation: getHeight for _geoPos: " + x + ", " + y + ", " + z);
			z = GeoEngine.getHeight(x, y, z, getReflection()) + 5;
		}

		setTarget(null);
		stopMove();

		if(isPlayer())
		{
			L2Player player = (L2Player) this;

			if(player.getVehicle() != null && !player.getVehicle().isTeleporting())
				player.getVehicle().oustPlayer(player, new Location(x, y, z));

			for(L2Player pl : L2World.getAroundPlayers(this))
				if(pl.getFollowTarget() == this)
					pl.setFollowTarget(null);

			decayMe();

			setXYZInvisible(x, y, z);

			if(player.isLogoutStarted())
				return;

			if(reflection != getReflection())
				setReflection(reflection);

			setIsTeleporting(true);

			player.sendPacket(new TeleportToLocation(player, x, y, z));
			player.sendPacket(new ExTeleportToLocationActivate(player, x, y, z));
			
			if(player.getVehicle() != null)
				player.sendPacket(new DeleteObject(player.getVehicle()));
			// Нужно при телепорте с более высокой точки на более низкую, иначе наносится вред от "падения"
			player.setLastClientPosition(null);
			player.setLastServerPosition(null);

			if(player.getParty() != null)
			{
				Point position = player.getLastPartyPositionSent();
				if(player.getDistance(position.x, position.y) < 500)
				{
					player.getParty().broadcastToPartyMembers(player, new PartyMemberPosition(player));
					position.move(player.getX(), player.getY());
				}
			}

			if(player.getPet() != null)
			{
				player.getPet().setTeleported(true);
				if(player.getPet() instanceof L2PetBabyInstance)
					player.getPet().getAI().stopAITask();
				player.getPet().decayMe();
			}
		}
		else
		{
			decayMe();
			setXYZInvisible(x, y, z);
			if(reflection != getReflection())
				setReflection(reflection);
			spawnMe();
		}
	}

	public void onTeleported()
	{
	}

	public void sendMessage(CustomMessage message)
	{
		sendMessage(message.toString());
	}

	private long _nonAggroTime;

	public long getNonAggroTime()
	{
		return _nonAggroTime;
	}

	public void setNonAggroTime(long time)
	{
		_nonAggroTime = time;
	}

	public int getCustomEffect()
	{
		return _customEffect;
	}

	public void setCustomEffect(int effect)
	{
		_customEffect = effect;
	}

	@Override
	public String toString()
	{
		return "Mob (" + getNpcId() + ")";
	}

	// ---------------------------- Not Implemented -------------------------------

	@SuppressWarnings("unused")
	public void addExpAndSp(long addToExp, long addToSp)
	{
	}

	@SuppressWarnings("unused")
	public void broadcastUserInfo()
	{
	}

	@SuppressWarnings("unused")
	public void checkHpMessages(double currentHp, double newHp)
	{
	}

	@SuppressWarnings("unused")
	public boolean checkPvP(L2Character target, L2Skill skill)
	{
		return false;
	}

	@SuppressWarnings("unused")
	public boolean consumeItem(int itemConsumeId, int itemCount, boolean sendMessage)
	{
		return true;
	}

	@SuppressWarnings("unused")
	public void doPickupItem(L2Object object)
	{
	}

	@SuppressWarnings("unused")
	public boolean isFearImmune()
	{
		return false;
	}

	@SuppressWarnings("unused")
	public boolean isLethalImmune()
	{
		return false;
	}

	@SuppressWarnings("unused")
	public boolean getChargedSoulShot()
	{
		return false;
	}

	@SuppressWarnings("unused")
	public int getChargedSpiritShot()
	{
		return 0;
	}

	@SuppressWarnings("unused")
	public int getIncreasedForce()
	{
		return 0;
	}

	@SuppressWarnings("unused")
	public int getConsumedSouls()
	{
		return 0;
	}

	@SuppressWarnings("unused")
	public int getKarma()
	{
		return 0;
	}

	@SuppressWarnings("unused")
	public double getLevelMod()
	{
		return 1;
	}

	@SuppressWarnings("unused")
	public int getNpcId()
	{
		return 0;
	}

	@SuppressWarnings("unused")
	public L2Summon getPet()
	{
		return null;
	}

	@SuppressWarnings("unused")
	public int getPvpFlag()
	{
		return 0;
	}

	@SuppressWarnings("unused")
	public int getTeam()
	{
		return 0;
	}

	@SuppressWarnings("unused")
	public void setTeam(int team)
	{
	}

	@SuppressWarnings("unused")
	public boolean isOverhit()
	{
		return false;
	}

	@SuppressWarnings("unused")
	public boolean isSitting()
	{
		return false;
	}

	@SuppressWarnings("unused")
	public boolean isUndead()
	{
		return false;
	}

	@SuppressWarnings("unused")
	public void setOverhitEnabled(boolean b)
	{
	}

	@SuppressWarnings("unused")
	public void reduceArrowCount()
	{
	}

	@SuppressWarnings("unused")
	public void sendChanges()
	{
	}

	@SuppressWarnings("unused")
	public void sendMessage(String message)
	{
	}

	/*@SuppressWarnings("unused")
	@Deprecated
	public void sendMessage(String message, String message_ru)
	{}*/

	@SuppressWarnings("unused")
	public void sendPacket(L2GameServerPacket mov)
	{
	}

	@SuppressWarnings("unused")
	public void setIncreasedForce(int i)
	{
	}

	@SuppressWarnings("unused")
	public void increaseSouls(int souls)
	{
	}

	@SuppressWarnings("unused")
	public void decreaseSouls(int souls)
	{
	}

	@SuppressWarnings("unused")
	public void setConsumedSouls(int i, L2NpcInstance monster)
	{
	}

	@SuppressWarnings("unused")
	public void setOverhitValues(L2Character attacker, double i)
	{
	}

	public void sitDown()
	{
	}

	@SuppressWarnings("unused")
	public void standUp()
	{
	}

	@SuppressWarnings("unused")
	public void startPvPFlag(L2Character target)
	{
	}

	@SuppressWarnings("unused")
	public boolean unChargeShots(boolean spirit)
	{
		return false;
	}

	@SuppressWarnings("unused")
	public void updateEffectIcons()
	{
	}

	@SuppressWarnings("unused")
	public void updateStats()
	{
	}

	@SuppressWarnings("unused")
	public void callMinionsToAssist(L2Character attacker, L2Character victim, int damage)
	{
	}

	@SuppressWarnings("unused")
	public boolean hasMinions()
	{
		return false;
	}

	@SuppressWarnings("unused")
	public boolean isCursedWeaponEquipped()
	{
		return false;
	}

	@SuppressWarnings("unused")
	public boolean isHero()
	{
		return false;
	}

	@SuppressWarnings("unused")
	public boolean isInBoat()
	{
		return false;
	}

	@SuppressWarnings("unused")
	public boolean isInAirShip()
	{
		return false;
	}

	@SuppressWarnings("unused")
	public int getAccessLevel()
	{
		return 0;
	}

	@SuppressWarnings("unused")
	public void setFollowStatus(boolean b)
	{
	}

	@SuppressWarnings("unused")
	public void setLastClientPosition(Location charPosition)
	{
	}

	@SuppressWarnings("unused")
	public void setLastServerPosition(Location charPosition)
	{
	}

	@SuppressWarnings("unused")
	public boolean hasRandomAnimation()
	{
		return true;
	}

	@SuppressWarnings("unused")
	public boolean hasRandomWalk()
	{
		return true;
	}

	public int getClanCrestId()
	{
		return 0;
	}

	public int getClanCrestLargeId()
	{
		return 0;
	}

	public int getAllyCrestId()
	{
		return 0;
	}

	@Override
	public float getColRadius()
	{
		return getTemplate().collisionRadius;
	}

	@Override
	public float getColHeight()
	{
		return getTemplate().collisionHeight;
	}

	public float getGrowColRadius()
	{
		return getTemplate().collisionRadius;
	}

	public float getGrowColHeight()
	{
		return getTemplate().collisionHeight;
	}

	public boolean canAttackCharacter(L2Character _target)
	{
		return _target.getPlayer() != null;
	}

	// --------------------------------- Abstract --------------------------------------

	public abstract byte getLevel();

	public abstract void updateAbnormalEffect();

	public abstract L2ItemInstance getActiveWeaponInstance();

	public abstract L2Weapon getActiveWeaponItem();

	public abstract L2ItemInstance getSecondaryWeaponInstance();

	public abstract L2Weapon getSecondaryWeaponItem();

	// ----------------------------- End Of Abstract -----------------------------------

	public void sendDamageMessage(L2Character target, int damage, boolean miss, boolean pcrit, boolean block)
	{
		if(miss && target != null && target.isPlayer())
			target.sendPacket(new SystemMessage(SystemMessage.S1_HAS_EVADED_S2S_ATTACK).addCharName(target).addCharName(this));
	}

	public boolean isMovePacketNeeded()
	{
		return !getLoc().equals(getDestination()) && getDestination() != null && !getDestination().equals(prevDestination);
	}

	public void sendSkillReuseMessage(int skillId, int level, L2ItemInstance item)
	{
	}

	public boolean isCatacombMob()
	{
		return false;
	}

	public int getDanceSongCount()
	{
		int count = 0;
		for(L2Effect e : getAllEffects())
		{
			if((e.getSkill().isSongDance()) && e.isInUse())
				count++;
		}
		return count;
	}

	public void setInsideZone(ZoneType zt, boolean inside)
	{
		synchronized(_insideZones)
		{
			if(inside)
			{
				if(Config.DEBUG && zt == L2Zone.ZoneType.fishing)
					_log.warn("ZoneSystem: " + this + " enter in zone. Zone type " + zt + "(" + _insideZones[zt.ordinal()] + ") coords: [" + getX() + "," + getY() + "," + getZ() + "]");
				_insideZones[zt.ordinal()]++;
			}
			else
			{
				if(_insideZones[zt.ordinal()] < 1)
					_log.warn("ZoneSystem: " + this + " onExit from not entered zone. Zone type " + zt + " " + getLoc() + " " + Thread.currentThread());
				else
					_insideZones[zt.ordinal()]--;
			}
		}
	}

	public boolean isInZone(ZoneType zt)
	{
		return _insideZones[zt.ordinal()] > 0;
	}

	public boolean isInClanBase()
	{
		return isInZone(ZoneType.residence);
	}

	public boolean isInZonePeace()
	{
		return !(isInBoat() && isInAirShip()) && (isInZone(ZoneType.peace) || isInBoat());
	}

	public boolean isInZoneBattle()
	{
		return isInZone(ZoneType.battle);
	}

	public boolean isInZoneOlympiad()
	{
		return isInZone(ZoneType.olympiad_stadia);
	}

	public boolean isInDangerArea()
	{
		return isInZone(ZoneType.danger);
	}

	public boolean isInSiege()
	{
		return isInZone(ZoneType.siege);
	}

	public boolean isInZoneSSQ()
	{
		return isInZone(ZoneType.ssq);
	}

	public void clearInZones()
	{
		for(int i = 0; i < _insideZones.length; i++)
			_insideZones[i] = 0;
	}

	/*
	 * For debug
	 */
	public void sendInsideZones(L2Player player)
	{
		for(int i = 0; i < _insideZones.length; i++)
			if(_insideZones[i] > 0)
				player.sendMessage(ZoneType.values()[i] + " " + _insideZones[i]);
	}

	public byte[] getInsideZones()
	{
		return _insideZones;
	}

	public boolean isActionBlocked(String action)
	{
		if(_zones == null)
			return false;

		for(L2Zone zone : _zones)
			if(zone.isActive(getReflection()) && zone.isActionBlocked(action) && zone.isInsideZone(this))
				return true;

		return false;
	}

	private int _zoneCheckCounter = 0;
	private final ReentrantLock _zoneLock = new ReentrantLock();
	private GArray<L2Zone> _zones = null;

	@Override
	public void revalidateZones(boolean force)
	{
		if(force || _zoneCheckCounter > 4)
		{
			_zoneCheckCounter = 0;
			GArray<L2Zone> currentZone = ZoneManager.getInstance().getZones(getX(), getY(), getZ());
			GArray<L2Zone> newZones = null;
			GArray<L2Zone> oldZones = null;

			_zoneLock.lock();
			try
			{
				if(_zones == null)
					newZones = currentZone;
				else
				{
					if(currentZone != null)
						for(L2Zone zone : currentZone)
							if(!_zones.contains(zone))
							{
								if(newZones == null)
									newZones = new GArray<L2Zone>();
								newZones.add(zone);
							}

					if(_zones.size() > 0)
						for(L2Zone zone : _zones)
							if(currentZone == null || !currentZone.contains(zone))
							{
								if(oldZones == null)
									oldZones = new GArray<L2Zone>();
								oldZones.add(zone);
							}
				}

				if(currentZone != null && currentZone.size() > 0)
					_zones = currentZone;
				else
					_zones = null;
			}
			finally
			{
				_zoneLock.unlock();
			}

			if(oldZones != null)
				for(L2Zone zone : oldZones)
					if(zone != null)
						zone.doExit(this);

			if(newZones != null)
				for(L2Zone zone : newZones)
					if(zone != null)
						zone.doEnter(this);
		}
		else
			_zoneCheckCounter++;
	}

	public void clearZones()
	{
		_zoneLock.lock();
		try
		{
			if(_zones != null)
				for(L2Zone zone : _zones)
					if(zone != null)
						zone.doExit(this);
			_zones = null;
		}
		finally
		{
			_zoneLock.unlock();
		}
	}

	public L2Zone getZone(ZoneType type)
	{
		if(_zones == null)
			return null;

		for(L2Zone zone : _zones)
			if(zone.isActive(getReflection()) && zone.getTypes().contains(type))
				return zone;

		return null;
	}

	public GArray<L2Zone> getZones()
	{
		return _zones;
	}

	public boolean isFloating()
	{
		return isSwimming() || isFlying();
	}

	/**
	 * Returns true if hp update should be done, false if not
	 *
	 * @return boolean
	 */
	protected boolean needHpUpdate(int barPixels)
	{
		double currentHp = getCurrentHp();

		if(currentHp <= 1.0 || getMaxHp() < barPixels)
			return true;

		if(currentHp <= _hpUpdateDecCheck || currentHp >= _hpUpdateIncCheck)
		{
			if(currentHp == getMaxHp())
			{
				_hpUpdateIncCheck = currentHp + 1;
				_hpUpdateDecCheck = currentHp - _hpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentHp / _hpUpdateInterval;
				int intMulti = (int) doubleMulti;

				_hpUpdateDecCheck = _hpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_hpUpdateIncCheck = _hpUpdateDecCheck + _hpUpdateInterval;
			}

			return true;
		}

		return false;
	}

	public Duel getDuel()
	{
		return null;
	}

	public boolean isInDuel()
	{
		return false;
	}

	public int getDuelState()
	{
		return Duel.DUELSTATE_NODUEL;
	}

	public boolean isFriend(L2Character target)
	{
		return false;
	}

	public boolean isPartyMember(L2Character target)
	{
		return false;
	}

	public boolean isCommandChanelMember(L2Character target)
	{
		return false;
	}

	public boolean isClanMember(L2Character target)
	{
		return false;
	}

	public TargetType getTargetRelation(L2Character target, boolean offensive)
	{
		if(this == target)
			return TargetType.self;

		return TargetType.none;
	}

	public boolean isStatActive(Stats stat)
	{
		return calcStat(stat, 0, null, null) > 0;
	}

	public void teleToClosestTown()
	{
		teleToLocation(MapRegionTable.getTeleToClosestTown(this), 0);
	}

	public void teleToCastle()
	{
		teleToLocation(MapRegionTable.getTeleToCastle(this), 0);
	}

	public void teleToFortress()
	{
		teleToLocation(MapRegionTable.getTeleToFortress(this), 0);
	}

	public void teleToClanhall()
	{
		teleToLocation(MapRegionTable.getTeleToClanHall(this), 0);
	}

	public void broadcastUserInfo(boolean force)
	{
	}

	public int getClanId()
	{
		return 0;
	}

	public int getAllyId()
	{
		return 0;
	}

	public boolean isShowTag()
	{
		return false;
	}

	public int getTerritoryId()
	{
		return 0;
	}

	public int isChampion()
	{
		return 0;
	}

	public L2EffectPointInstance getEffectPoint()
	{
		return null;
	}

	public Location getPrevLoc()
	{
		return _prevLoc;
	}

	public void setPrevLoc(Location loc)
	{
		_prevLoc = loc;
	}

	public void setHide(boolean hide)
	{
		_isHide = hide;
	}

	public boolean isHide()
	{
		return _isHide;
	}

	private int _nameAddStringId = -1;
	private int _titleAddStringId = -1;

	public void setNameFStringId(int val)
	{
		_nameAddStringId = val;
	}

	public int getNameFStringId()
	{
		return _nameAddStringId;
	}

	public void setTitleFStringId(int val)
	{
		_titleAddStringId = val;
	}

	public int getTitleFStringId()
	{
		return _titleAddStringId;
	}

	public short getActiveClass()
	{
		return -1;
	}

	public void addIgnoredSkill(int skillId)
	{
		if(_ignoredSkills == null)
			_ignoredSkills = new ConcurrentHashMap<>();

		synchronized(_ignoredSkills)
		{
			if(_ignoredSkills.containsKey(skillId))
				_ignoredSkills.put(skillId, _ignoredSkills.get(skillId) + 1);
			else
				_ignoredSkills.put(skillId, 1);
		}
	}

	public void removeIgnoredSkill(int skillId)
	{
		if(_ignoredSkills == null)
			return;

		synchronized(_ignoredSkills)
		{
			if(_ignoredSkills.containsKey(skillId))
			{
				int c = _ignoredSkills.get(skillId) - 1;
				if(c < 1)
					_ignoredSkills.remove(skillId);
				else
					_ignoredSkills.put(skillId, c);
			}
		}
	}

	public boolean isSkillIgnored(int skillId)
	{
		return _ignoredSkills != null && _ignoredSkills.containsKey(skillId);
	}

	public void setDieEvent(L2NpcInstance npc)
	{
		if(npc == null)
			return;

		if(dieEventList == null)
			dieEventList = new GCSArray<>();

		if(!dieEventList.contains(npc.getStoredId()))
			dieEventList.add(npc.getStoredId());
	}
}
