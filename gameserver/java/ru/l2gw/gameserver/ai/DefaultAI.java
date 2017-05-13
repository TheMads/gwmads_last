package ru.l2gw.gameserver.ai;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.extensions.scripts.Functions;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.clientpackets.Say2C;
import ru.l2gw.gameserver.controllers.GameTimeController;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.instancemanager.superpoint.Superpoint;
import ru.l2gw.gameserver.instancemanager.superpoint.SuperpointManager;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.entity.SevenSigns;
import ru.l2gw.gameserver.model.instances.L2ChestInstance;
import ru.l2gw.gameserver.model.instances.L2FestivalMonsterInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance.AggroInfo;
import ru.l2gw.gameserver.model.instances.L2PetBabyInstance;
import ru.l2gw.gameserver.model.npcmaker.DefaultMaker;
import ru.l2gw.gameserver.model.quest.Quest;
import ru.l2gw.gameserver.serverpackets.SocialAction;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.tables.TerritoryTable;
import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.util.Location;
import ru.l2gw.util.Util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

import static ru.l2gw.gameserver.ai.CtrlIntention.*;

public class DefaultAI extends L2CharacterAI implements Runnable
{
	protected boolean _isMovingBack = false;
	protected boolean _inMyTerritory = true;
	private boolean _isRbMinion = false;
	protected static final long DEFAULT_DESIRE = 10000;
	protected Map<Integer, Timer> delayedTimers;
	private int moveSuperPointFails = 0;
	private int _noDesireCount;

	private static final TaskComparator task_comparator = new TaskComparator();

	public static enum TaskType
	{
		MOVE,
		ATTACK,
		CAST,
		BUFF,
		FLEE,
		MOVE_SUPERPOINT,
		EFFECT_ACTION,
		FOLLOW,
		FOLLOW2
	}

	public static class Task
	{
		public TaskType type;
		public L2Skill skill;
		public long targetId;
		public Location loc;
		public boolean usePF = true;
		public long weight;
		public int p1, p2;
		public Superpoint superpoint;

		public L2Character getTarget()
		{
			return L2ObjectsStorage.getAsCharacter(targetId);
		}

		@Override
		public String toString()
		{
			return "Task[" + type + ";target=" + getTarget() + ";desire=" + weight + (skill != null ? ";skill=" + skill : "") + (loc != null ? ";loc=" + loc : "") + (superpoint != null ? ";superpoint=" + superpoint : "") + "]";
		}
	}

	private static class TaskComparator implements Comparator<Task>
	{
		@Override
		public int compare(Task o1, Task o2)
		{
			if(o1 == null || o2 == null)
				return 0;

			if(o1.type == TaskType.ATTACK && o2.type == TaskType.ATTACK && o1.targetId == o2.targetId)
			{
				if(o2.weight < o2.weight)
					o2.weight = o1.weight;
				return 0;
			}

			long r = o2.weight - o1.weight;
			return r > 0 ? 1 : r < 0 ? -1 : 0;
		}
	}

	public class Teleport implements Runnable
	{
		Location _destination;

		public Teleport(Location destination)
		{
			_destination = destination;
		}

		public void run()
		{
			if(debug)
				_log.info(_thisActor + " teleport to: " + _destination);

			HashMap<Integer, Long> oldAggro = new HashMap<>();
			for(AggroInfo ai : _thisActor.getAggroList().values())
				if(ai.hate > 0)
					oldAggro.put(ai.objectId, ai.hate);

			_thisActor.teleToLocation(_destination);

			for(Integer objectId : oldAggro.keySet())
			{
				AggroInfo ai = _thisActor.getAggroList().get(objectId);
				if(ai != null)
					ai.hate = oldAggro.get(objectId);
			}
		}
	}

	public class RunningTask implements Runnable
	{
		public void run()
		{
			_thisActor.setRunning();
			_runningTask = null;
		}
	}

	protected static Log _log = LogFactory.getLog(DefaultAI.class.getName());

	protected int MAX_ATTACK_TIMEOUT = 40000 / GameTimeController.MILLIS_IN_TICK; // int millis->ticks
	protected int TELEPORT_TIMEOUT = 10000 / GameTimeController.MILLIS_IN_TICK; // int millis->ticks
	protected int MAX_PATHFIND_FAILS = 5;
	protected int AI_TASK_DELAY = 1000;
	protected int MAX_PURSUE_RANGE;

	/**
	 * Current L2NpcInstance
	 */
	protected L2NpcInstance _thisActor;

	/**
	 * The L2NpcInstance AI task executed every 1s (call onEvtThink method)
	 */
	protected Future<?> _aiTask;

	protected ScheduledFuture<?> _runningTask;

	/**
	 * The flag used to indicate that a thinking action is in progress
	 */
	private boolean _thinking = false;
	protected boolean _useUD = false;

	/**
	 * The L2NpcInstance aggro counter
	 */
	protected int _globalAggro;
	protected boolean _aggroOnSight;
	protected long _randomAnimationEnd;
	protected boolean _isMobile = true;
	protected int _pathfind_fails;
	protected int _see_spell_z;
	protected long _lastChampionTalk = 0;
	public long check_territory_time = 120;
	public boolean check_territory_chstate = true;
	public int check_territory_return;
	protected L2Territory my_territory;
	protected DefaultMaker my_maker;

	/**
	 * Список заданий
	 */
	protected ConcurrentSkipListSet<Task> _task_list = new ConcurrentSkipListSet<Task>(task_comparator);

	/**
	 * Показывает, есть ли задания
	 */
	protected boolean _def_think = false;

	protected L2Skill[] _heal_skills;
	protected L2Skill[] _buff_skills;
	protected L2Skill[] _selfbuff_skills;
	protected L2Skill[] _mdam_skills;
	protected L2Skill[] _pdam_skills;
	protected L2Skill[] _cancel_skills;
	protected L2Skill[] _debuff_skills;
	protected L2Skill[] _selfexplosion_skills;
	protected L2Skill[] _manaburn_skills;
	protected L2Skill[] _aggression_skills;

	protected L2Skill _ud = null;
	protected int _useUDChance;

	/**
	 * Сложение массивов скиллов
	 */
	public static L2Skill[] msum(L2Skill[] array1, L2Skill[] array2)
	{
		L2Skill[] result = new L2Skill[array1.length + array2.length];

		System.arraycopy(array1, 0, result, 0, array1.length);

		System.arraycopy(array2, 0, result, array1.length, array2.length);

		return result;
	}

	public DefaultAI(L2Character actor)
	{
		super(actor);

		_thisActor = (L2NpcInstance) _actor;
		_thisActor.setAttackTimeout(Integer.MAX_VALUE);
		_globalAggro = getInt("global_aggro", -10); // 10 seconds timeout of ATTACK after respawn
		_aggroOnSight = getBool("aggro_on_sight", false);
		_isRbMinion = getBool("rb_minion", false);
		_see_spell_z = getInt("see_spell_z", Config.PLAYER_VISIBILITY_Z);

		// Primary skills
		_heal_skills = _thisActor.getTemplate().getSkillsByType("HEALMAGIC");
		_selfbuff_skills = _thisActor.getTemplate().getSkillsByType("SELFBUFF");
		_buff_skills = _thisActor.getTemplate().getSkillsByType("BUFF");
		_mdam_skills = _thisActor.getTemplate().getSkillsByType("DDMAGIC");
		_pdam_skills = _thisActor.getTemplate().getSkillsByType("DDPHYSICAL");
		_debuff_skills = _thisActor.getTemplate().getSkillsByType("DEBUFF");
		// Secondary skills
		_cancel_skills = _thisActor.getTemplate().getSkillsByType("CANCEL");
		_selfexplosion_skills = _thisActor.getTemplate().getSkillsByType("SELFEXPLOSION");
		_manaburn_skills = _thisActor.getTemplate().getSkillsByType("MANABURN");
		_aggression_skills = _thisActor.getTemplate().getSkillsByType("AGGRESSION");

		_debuff_skills = msum(_debuff_skills, _aggression_skills);

		_ud = SkillTable.getInstance().getInfo(5044, 3);

		MAX_PURSUE_RANGE = getInt("MaxPursueRange", _thisActor.isRaid() || _thisActor.isRaidMinion() ? Config.MAX_PURSUE_RANGE_RAID : Config.MAX_PURSUE_RANGE);
		_thisActor.minFactionNotifyInterval = getInt("FactionNotifyInterval", _thisActor.minFactionNotifyInterval);

		check_territory_time *= 1000;

		if(getBool("immobile", false))
			_thisActor.setImobilised(true);

		if(_thisActor.getSpawnDefine() != null)
			my_maker = _thisActor.getSpawnDefine().getMaker();

		if(check_territory_time > 0)
		{
			L2Spawn spawn = _thisActor.getSpawn();
			if(my_maker == null && (spawn == null || spawn.getLocation() <= 0 || (my_territory = TerritoryTable.getInstance().getLocation(spawn.getLocation())) == null))
				check_territory_time = 0;
		}
	}

	public void run()
	{
		try
		{
			onEvtThink();
		}
		catch(Exception e)
		{
			_log.info(_thisActor + " AI error: " + e);
			e.printStackTrace();
		}
	}

	@Override
	public void startAITask()
	{
		if(_aiTask == null)
		{
			_aiTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, Rnd.get(AI_TASK_DELAY), AI_TASK_DELAY);
			setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			if(check_territory_time > 0)
			{
				blockTimer(-1000);
				addTimer(-1000, check_territory_time);
			}
			if(delayedTimers != null)
			{
				for(Timer timer : delayedTimers.values())
				{
					if(debug)
						_log.info(_thisActor + " start delayed timer: " + timer._timerId + " " + timer._arg1 + " " + timer._arg2 + " " + timer.delay);
					addTimer(timer);
				}
				delayedTimers.clear();
			}
		}
	}

	@Override
	public void addTimer(int timerId, Object arg1, Object arg2, long delay)
	{
		if(_aiTask == null)
		{
			if(delayedTimers == null)
				delayedTimers = new ConcurrentHashMap<>();
			if(debug)
				_log.info(_thisActor + " addTimer delayed: " + timerId + " " + arg1 + " " + arg2 + " " + delay);
			Timer timer = new Timer(timerId, arg1, arg2, delay);
			delayedTimers.put(timerId, timer);
		}
		else
			super.addTimer(timerId, arg1, arg2, delay);
	}

	@Override
	public void stopAITask()
	{
		try
		{
			if(_aiTask != null)
			{
				blockTimer(-1000);
				setIntention(CtrlIntention.AI_INTENTION_IDLE);
				_aiTask.cancel(false);
				_aiTask = null;
			}
		}
		catch(NullPointerException e)
		{
		}
	}

	/**
	 * Определяет, может ли этот тип АИ видеть персонажей в режиме Silent Move.
	 *
	 * @param target L2Playable цель
	 * @return true если цель не видна в режиме Silent Move
	 */
	protected boolean isSilent(L2Character target)
	{
		return Rnd.chance((int) target.calcStat(Stats.AVOID_AGGRO, 0, null, null)) && !getBool("isSilentMoveVisible", false);
	}

	@Override
	public boolean checkAggression(L2Character target)
	{
		if(_intention != AI_INTENTION_ACTIVE || _globalAggro < 0)
			return false;
		if(_thisActor.getHate(target) == 0 && (!_thisActor.isAggressive() || !_thisActor.isInRange(target, _thisActor.getAggroRange()) || !_inMyTerritory))
			return false;
		if(Math.abs(target.getZ() - _actor.getZ()) > 400)
			return false;
		if(isSilent(target))
			return false;
		if(_thisActor.getFactionId().equalsIgnoreCase("@varka_silenos_clan") && target.getPlayer().getVarka() > 0)
			return false;
		if(_thisActor.getFactionId().equalsIgnoreCase("@ketra_orc_clan") && target.getPlayer().getKetra() > 0)
			return false;
		if(!GeoEngine.canSeeTarget(_actor, target))
			return false;
		if(target.isInZonePeace())
			return false;
		if(target.isPlayer())
			if(((L2Player) target).isGM() && ((L2Player) target).isInvisible())
				return false;
		if(!_thisActor.canAttackCharacter(target))
			return false;
		if(target instanceof L2PetBabyInstance && _thisActor.getHate(target) == 0)
			return false;

		if((target.isSummon() || target.isPet()) && target.getPlayer() != null)
			_thisActor.addDamageHate(target.getPlayer(), 0, 1);

		_thisActor.addDamageHate(target, 0, 2);

		startRunningTask(2000);
		setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
		return true;
	}

	protected boolean randomAnimation()
	{
		if(!_actor.isMoving)
			// Анимации мобов
			if(_actor.hasRandomAnimation() && Rnd.chance(Config.RND_ANIMATION_RATE))
			{
				_randomAnimationEnd = System.currentTimeMillis();
				_thisActor.onRandomAnimation();
				return true;
			}
		return false;
	}

	protected boolean randomWalk()
	{
		return !_actor.isMoving && _actor.hasRandomWalk() && maybeMoveToHome();
	}

	private int moveToHomeFails = 0;

	protected boolean thinkActive()
	{
		if(_thisActor.isDead())
			return true;

		if(debug)
			_log.info("thinkActive: " + _thisActor);

		_noDesireCount++;
		if(_noDesireCount >= 5)
		{
			_noDesireCount = 0;
			onEvtNoDesire();
		}

		// Update every 1s the _globalAggro counter to come close to 0
		if(_globalAggro < 0)
			_globalAggro++;
		else if(_globalAggro > 0)
			_globalAggro--;

		if(_randomAnimationEnd > System.currentTimeMillis() || _globalAggro < 0)
			return true;

		L2Character hated = _thisActor.getMostHated();
		if(debug)
			_log.info("thinkActive: " + _thisActor + " hated: " + hated + " globalAggro: " + _globalAggro);

		if(hated != null && _thisActor.canAttackCharacter(hated))
		{
			startRunningTask(2000);
			_thisActor.setAttackTimeout(MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks());
			setIntention(AI_INTENTION_ATTACK, hated);
			return true;
		}

		if(_task_list.size() > 0)
		{
			doTask();
			return true;
		}

		if(!randomWalk() && !randomAnimation() && _thisActor.canMoveToHome())
		{
			if(!_isMovingBack && !_thisActor.isInRange(_thisActor.getSpawnedLoc(), Config.MAX_DRIFT_RANGE + 100)) // Двигаемся на базу
			{
				if(debug)
					_log.info(_thisActor + " start move to home");
				_isMovingBack = true;
				if(moveToHomeFails > 4)
				{
					if(debug)
						_log.info(_thisActor + " move to home fails, teleport.");
					moveToHomeFails = 0;
					teleportHome();
					return false;
				}
				returnHome();
				return true;
			}
			else if(_isMovingBack && !_thisActor.isMoving) //Пришли на базу
			{
				if(!_thisActor.isInRange(_thisActor.getSpawnedLoc(), 16))
					moveToHomeFails++;
				else
					moveToHomeFails = 0;
				if(debug)
					_log.info(_thisActor + " stop move to home");
				_isMovingBack = false;
				_thisActor.setHeading(_thisActor.getSpawnedLoc().getHeading());
			}
		}

		if(checkBossPosition())
			return false;

		// If this is a festival monster or chest, then it remains in the same location
		if(_thisActor instanceof L2FestivalMonsterInstance || _thisActor instanceof L2ChestInstance)
			return false;

		if(!_thisActor.isInRange(_thisActor.getSpawnedLoc(), MAX_PURSUE_RANGE) && _thisActor.isMonster())
		{
			clearTasks();
			_thisActor.teleToLocation(_thisActor.getSpawnedLoc());
			return true;
		}

		if(Config.ALT_CHAMPION_ENABLE && _thisActor.isChampion() > 0 && _lastChampionTalk < System.currentTimeMillis() && Rnd.chance(30))
		{
			_lastChampionTalk = System.currentTimeMillis() + Rnd.get(30, 120) * 1000;
			Functions.npcSayCustom(_thisActor, Say2C.ALL, "AltChampionTalk" + Rnd.get(1, 3), null);
		}

		return _aggroOnSight && findTargetForAttack() || randomAnimation() || randomWalk();
	}

	public boolean checkBossPosition()
	{
		if(_thisActor.isMinion())
		{
			L2NpcInstance leader = _thisActor.getLeader();
			if(leader == null)
				return true;
			double distance = _thisActor.getDistance3D(leader);
			Location pos = leader.getMinionPosition();
			if(distance > 1000 || Math.abs(leader.getZ() - _thisActor.getZ()) > 350)
			{
				if(debug)
					_log.info("thinkActive: " + _thisActor + " teleport minion");
				_thisActor.teleToLocation(pos);
			}
			else if(distance > 200)
			{
				if(debug)
					_log.info("thinkActive: " + _thisActor + " minion try to move");
				if(leader.isRunning())
					_thisActor.setRunning();
				else
					_thisActor.setWalking();
				_thisActor.moveToLocation(pos, 0, true);
			}
			return true;
		}

		return false;
	}

	@Override
	protected void onIntentionActive()
	{
		_thisActor.setAttackTimeout(Integer.MAX_VALUE);
		super.onIntentionActive();
	}

	protected boolean checkTarget(L2Character target)
	{
		if(target == null || target == _thisActor || target.isAlikeDead() || !_thisActor.isInRange(target, 4000))
			return true;

		if(!_thisActor.getAggroList().containsKey(target.getObjectId()))
			return true;

		if(_thisActor.getAttackTimeout() < GameTimeController.getGameTicks())
		{
			if(_thisActor.isRunning() && _thisActor.getAggroListSize() == 1)
			{
				_thisActor.setWalking();
				_thisActor.setAttackTimeout(MAX_ATTACK_TIMEOUT / 4 + GameTimeController.getGameTicks());
				return false;
			}
			if(debug)
				_log.info(_thisActor + " attack timeout stop hate target: " + target);
			removeAttackDesire(target);
			//_thisActor.stopHate(target);
			return true;
		}

		return false;
	}

	protected void thinkAttack()
	{
		if(debug)
			_log.info("thinkAttack: " + _thisActor);

		_noDesireCount = 0;

		L2Character target = getAttackTarget();

		if(!_thisActor.isInRange(_thisActor.getSpawnedLoc(), MAX_PURSUE_RANGE) && _thisActor.getSpawn() != null)
		{
			if(debug)
				_log.info("thinkAttack: try teleport to home " + _thisActor + " range: " + MAX_PURSUE_RANGE);
			clearTasks();
			//_thisActor.clearAggroList();
			//setIntention(AI_INTENTION_ACTIVE);
			//onEvtThink();
			teleportHome();
			return;
		}
		else if((target == null || target.isDead()) && _thisActor.getMostHated() == null)
		{
			if(debug)
				_log.info("thinkAttack: no attack target " + _thisActor + " --> " + target);
			setIntention(AI_INTENTION_ACTIVE);
			onEvtThink();
		}
		else if(_thisActor.getDistance(target) > MAX_PURSUE_RANGE)
			if(_thisActor.canMoveToHome())
			{
				if(debug)
					_log.info("thinkAttack: attack target too far " + _thisActor + " --> " + target);
				setIntention(AI_INTENTION_ACTIVE);
				onEvtThink();
				return;
			}

		if(debug)
			_log.info("thinkAttack: " + _thisActor + " --> " + target);

		L2Character mostHated = _thisActor.getMostHated();
		if(target == null && mostHated != null || target != null && mostHated != null && !_thisActor.isConfused() && target != mostHated && _task_list.size() > 0 && _task_list.first() != null && _task_list.first().targetId != mostHated.getStoredId())
		{
			if(debug)
				_log.info("thinkAttack: " + _thisActor + " --> " + target + " change target to " + _thisActor.getMostHated());
			//clearTasks();
			//_def_think = false;
			removeTaskByTarget(target);
			addAttackDesire(mostHated, 1, DEFAULT_DESIRE);
			//addAttackDesire(mostHated, 1, _thisActor.getAggroList().get(mostHated.getObjectId()).hate);
		}

		if(Config.ALT_CHAMPION_ENABLE && _thisActor.isChampion() > 0 && _lastChampionTalk < System.currentTimeMillis() && Rnd.chance(5))
		{
			_lastChampionTalk = System.currentTimeMillis() + Rnd.get(30, 120) * 1000;
			Functions.npcSayCustom(_thisActor, Say2C.ALL, "AltChampionTalk" + Rnd.get(1, 3), null);
		}

		if(doTask())
		{
			if(target != null && !target.isMoving && Rnd.chance(30) && _thisActor.getDistance(target) < 100)
			{
				for(L2NpcInstance npc : _thisActor.getKnownNpc((int) _thisActor.getCollisionRadius()))
				{
					if(npc != null && npc.isMonster() && npc != target)
					{
						addMoveToDesire(Util.getPointInRadius(_thisActor.getLoc(), target.getLoc(), Rnd.get(30, 90) * (Rnd.chance(50) ? 1 : -1)), DEFAULT_DESIRE * 3);
						return;
					}
				}
			}
			if(debug)
				_log.info("thinkAttack: " + _thisActor + " createNewTask");
			createNewTask();
		}
	}

	@Override
	protected void onEvtReadyToAct()
	{
		onEvtThink();
	}

	@Override
	protected void onEvtArrivedTarget()
	{
		checkTerritory();
		onEvtThink();
	}

	@Override
	protected void onEvtArrived()
	{
		checkTerritory();
		if(_thisActor.isAggressive() && _intention == AI_INTENTION_ACTIVE && _globalAggro >= 0)
			findTargetForAttack();
	}

	protected boolean tryMoveToTarget(L2Character target)
	{
		return tryMoveToTarget(target, _thisActor.getPhysicalAttackRange());
	}

	protected boolean tryMoveToTarget(L2Character target, int offset)
	{
		if(debug)
			_log.info(_thisActor + " tryMoveToTarget: " + target + " offset: " + offset);
		if(!_thisActor.followToCharacter(target, offset))
		{
			if(debug)
				_log.info(_thisActor + " cannot move pf fails: " + _pathfind_fails);
			_pathfind_fails++;
		}

		if(debug)
			_log.info(_thisActor + " tryMoveToTarget attack start: " + (GameTimeController.getGameTicks() - (_thisActor.getAttackTimeout() - MAX_ATTACK_TIMEOUT)) + " tele " + TELEPORT_TIMEOUT);

		if(_pathfind_fails >= MAX_PATHFIND_FAILS && _thisActor.isInRange(target, 2000))
		{
			L2NpcInstance.AggroInfo aggro = _thisActor.getAggroList().get(target.getObjectId());
			if(aggro != null && aggro.damage > 500)
			{
				_pathfind_fails = 0;
				if(debug)
					_log.info(_thisActor + " schedule teleport to: " + target);
				ThreadPoolManager.getInstance().executeAi(new Teleport(GeoEngine.moveCheckForAI(target.getLoc(), _thisActor.getLoc(), target.getReflection())), false);
				return false;
			}

			if(debug)
				_log.info(_thisActor + " cannot move low aggro, stop hate: " + target);
			removeAttackDesire(target);
			return false;
		}

		return true;
	}

	protected boolean doTask()
	{
		if(_thisActor.isCastingNow() || _thisActor.isAttackingNow())
			return false;

		try
		{
			if(_task_list.size() == 0 || _task_list.first() == null)
				clearTasks();
		}
		catch(NoSuchElementException e)
		{
		}

		if(debug)
			_log.info(_thisActor + " doTask: def_think: " + _def_think);

		if(_task_list.size() < 1)
			return true;

		L2Character _temp_attack_target = null;

		Task currentTask = null;

		try
		{
			currentTask = _task_list.first();
		}
		catch(Exception e)
		{
		}

		if(debug)
			_log.info(_thisActor + " doTask: currentTask: " + currentTask);

		if(currentTask == null)
		{
			_def_think = false;
			return true;
		}

		if(debug)
			_log.info("doTask: " + _thisActor + " " + currentTask.type + " target " + currentTask.getTarget() + " loc " + currentTask.loc);

		switch(currentTask.type)
		{
			// Задание "прибежать в заданные координаты"
			case MOVE:
			{
				if(_actor.isMoving)
				{
					if(debug)
						_log.info("doTask: " + _thisActor + " already moving loc " + currentTask.loc);
					return false;
				}

				if(!_thisActor.isInRange(currentTask.loc, 50))
				{
					if(_actor.isMovementDisabled())
					{
						if(debug)
							_log.info("doTask: " + _thisActor + " movement disabled.");
						_def_think = false;
						return true;
					}

					if(!_thisActor.moveToLocation(currentTask.loc, 0, currentTask.usePF))
					{
						//ThreadPoolManager.getInstance().scheduleAi(new Teleport(currentTask.loc), 500, false);
						//return false;
						if(debug)
							_log.info("doTask: " + _thisActor + " cannot move to loc " + currentTask.loc + " tl size: " + _task_list.size());
						_task_list.remove(currentTask);
						if(_task_list.size() == 0)
						{
							_def_think = false;
							return true;
						}
						return false;
					}
					else if(debug)
						_log.info("doTask: " + _thisActor + " move to loc " + currentTask.loc);
				}
				else
				{
					// Следующее задание
					_task_list.remove(currentTask);
					// Если заданий больше нет - определить новое
					if(_task_list.size() == 0)
					{
						_def_think = false;
						return true;
					}
				}
			}
			break;
			case MOVE_SUPERPOINT:
			{
				if(_actor.isMoving)
				{
					if(debug)
						_log.info("doTask: " + _thisActor + " already moving loc " + currentTask.loc);
					return false;
				}

				if(_intention != AI_INTENTION_ACTIVE)
				{
					return maybeNextTask(currentTask);
				}

				if(currentTask.loc == null)
				{
					_log.warn("doTask: " + _thisActor + " MOVE_SUPERPOINT loc is null for: " + currentTask.superpoint);
					return true;
				}

				if(!_thisActor.isInRange(currentTask.loc, 50))
				{
					if(_actor.isMovementDisabled())
					{
						if(debug)
							_log.info("doTask: " + _thisActor + " movement disabled.");
						_def_think = false;
						return true;
					}

					if(!_thisActor.moveToLocation(currentTask.loc, 0, currentTask.p1 > 0))
					{
						//ThreadPoolManager.getInstance().scheduleAi(new Teleport(currentTask.loc), 500, false);
						//return false;
						if(debug)
							_log.info("doTask: " + _thisActor + " cannot move to loc " + currentTask.loc + " tl size: " + _task_list.size());
						_task_list.remove(currentTask);
						if(_task_list.size() == 0)
						{
							_def_think = false;
							return true;
						}
						return false;
					}
					else if(debug)
						_log.info("doTask: " + _thisActor + " move to loc " + currentTask.loc);
				}
				else
				{
					Location nextNode = currentTask.superpoint.getNextNode(_thisActor, currentTask.p1);
					if(currentTask.loc.equals(nextNode))
					{
						moveSuperPointFails++;
						if(moveSuperPointFails >= 10)
						{
							if(debug)
								_log.info("doTask: " + _thisActor + " node arrived, next node: " + nextNode + " move fail count: " + moveSuperPointFails + " teleport to node.");
							moveSuperPointFails = 0;
							ThreadPoolManager.getInstance().executeAi(new Teleport(nextNode), false);
						}
					}
					else
						moveSuperPointFails = 0;

					if(debug)
						_log.info("doTask: " + _thisActor + " node arrived, next node: " + nextNode);

					notifyEvent(CtrlEvent.EVT_NODE_ARRIVED, currentTask.loc);
					currentTask.loc = currentTask.superpoint.getNextNode(_thisActor, currentTask.p1);
					if(moveSuperPointFails == 0)
						doTask();
				}
			}
			break;
			case FOLLOW:
				if(_actor.isMoving)
				{
					if(debug)
						_log.info("doTask: " + _thisActor + " already moving loc " + currentTask.loc);
					return false;
				}

				_temp_attack_target = currentTask.getTarget();

				if(_temp_attack_target == null)
					return maybeNextTask(currentTask);

				if(!_thisActor.isInRange(_temp_attack_target, 100))
				{
					if(_actor.isMovementDisabled())
					{
						if(debug)
							_log.info("doTask: " + _thisActor + " movement disabled.");
						_def_think = false;
						return true;
					}

					if(!_thisActor.moveToLocation(_temp_attack_target.getLoc(), 40, true))
					{
						//ThreadPoolManager.getInstance().scheduleAi(new Teleport(currentTask.loc), 500, false);
						//return false;
						if(debug)
							_log.info("doTask: " + _thisActor + " cannot move to loc " + currentTask.loc + " tl size: " + _task_list.size());

						return true;
					}
					else if(debug)
						_log.info("doTask: " + _thisActor + " move to loc " + currentTask.loc);
				}
				else
					return maybeNextTask(currentTask);
				break;
			// Задание "добежать - ударить"
			case FOLLOW2:
				if(_actor.isMoving)
				{
					if(debug)
						_log.info("doTask: " + _thisActor + " already moving loc " + currentTask.loc);
					return false;
				}

				_temp_attack_target = currentTask.getTarget();

				if(_temp_attack_target == null)
					return maybeNextTask(currentTask);

				if(!_thisActor.isInRange(_temp_attack_target, currentTask.p2))
				{
					if(_actor.isMovementDisabled())
					{
						if(debug)
							_log.info("doTask: " + _thisActor + " movement disabled.");
						_def_think = false;
						return true;
					}

					if(!_thisActor.moveToLocation(_temp_attack_target.getLoc(), currentTask.p1, true))
					{
						//ThreadPoolManager.getInstance().scheduleAi(new Teleport(currentTask.loc), 500, false);
						//return false;
						if(debug)
							_log.info("doTask: " + _thisActor + " cannot move to loc " + currentTask.loc + " tl size: " + _task_list.size());

						return true;
					}
					else if(debug)
						_log.info("doTask: " + _thisActor + " move to loc " + currentTask.loc);
				}
				else if(_thisActor.isInRange(_temp_attack_target, currentTask.p1 / 2))
				{
					_thisActor.moveToLocation(Util.getPointInRadius(_temp_attack_target.getLoc(), Rnd.get(currentTask.p1, currentTask.p2), Rnd.get(360)), 0, false);
				}
				break;
			// Задание "добежать - ударить"
			case ATTACK:
			{
				_temp_attack_target = currentTask.getTarget();

				if(_temp_attack_target == null || _temp_attack_target.isDead())
					return maybeNextTask(currentTask);

				if(checkTarget(_temp_attack_target))
				{
					_def_think = false;
					return true;
				}

				if(debug)
					_log.info("doTask: " + _thisActor + " checkTarget " + _temp_attack_target + " current: " + getAttackTarget());

				if(_actor.isMoving && getAttackTarget() == _temp_attack_target)
				{
					if(debug)
						_log.info("doTask: already moving to target " + _thisActor + " " + _temp_attack_target);
					// Периодически прекращать преследовать и заного оценивать обстановку
					if(Rnd.chance(10))
					{
						if(debug)
							_log.info("doTask: already moving def_think to false " + _thisActor + " " + _temp_attack_target);
						_def_think = false;
						return true;
					}
					else
						tryMoveToTarget(_temp_attack_target);
					return false;
				}

				setAttackTarget(_temp_attack_target);

				if(debug)
					_log.info("doTask: " + _thisActor + " set Target " + getAttackTarget());

				// Преследовать до тех пор пока не приблизиться на расстояние атаки, потом атаковать
				if(_thisActor.getRealDistance3D(_temp_attack_target) > _thisActor.getPhysicalAttackRange() * 1.2 + _thisActor.getCollisionRadius() || !GeoEngine.canSeeTarget(_actor, _temp_attack_target))
				{
					if(debug)
						_log.info("doTask: " + _thisActor + " tryMoveToTarget " + getAttackTarget());

					if(_thisActor.isMovementDisabled())
					{
						_def_think = false;
						return true;
					}

					if(!_isMobile)
					{
						_def_think = false;
						return true;
					}

					if(!tryMoveToTarget(_temp_attack_target))
						return false;
				}
				else
				{
					_pathfind_fails = 0;

					// Обновление таймаута
					_thisActor.setAttackTimeout(MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks());

					clientStopMoving();

					if(_thisActor.isStatActive(Stats.BLOCK_PHYS_ATTACK))
					{
						_def_think = false;
						return true;
					}

					if(debug)
						_log.info("doTask: " + _thisActor + " doAttack: " + _temp_attack_target);

					// Атаковать
					_thisActor.chargeShots(false);
					_thisActor.doAttack(_temp_attack_target);
					return true;
				}
			}
			break;
			// Задание "добежать - атаковать скиллом"
			case CAST:
			{
				_temp_attack_target = currentTask.getTarget();

				if(_temp_attack_target == null || (_temp_attack_target.isDead() && currentTask.skill.getSkillTargetType() != L2Skill.TargetType.pc_body && currentTask.skill.getSkillTargetType() != L2Skill.TargetType.npc_body))
					return maybeNextTask(currentTask);

				if(_temp_attack_target == _thisActor)
				{
					_pathfind_fails = 0;

					// Обновление таймаута
					_thisActor.setAttackTimeout(MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks());

					clientStopMoving();

					// Использовать скилл на цель
					_thisActor.chargeShots(true);
					_thisActor.doCast(currentTask.skill, _temp_attack_target, null, !(_temp_attack_target instanceof L2Playable));

					// Следующее задание
					_task_list.remove(currentTask);
					// Если заданий больше нет - определить новое
					if(_task_list.size() == 0)
					{
						_def_think = false;
						return true;
					}

					break;
				}

				if(checkTarget(_temp_attack_target) && currentTask.p1 == 0)
				{
					_def_think = false;
					return maybeNextTask(currentTask);
				}

				if(_actor.isMoving && getAttackTarget() == _temp_attack_target)
				{
					// Периодически прекращать преследовать и заного оценивать обстановку
					if(Rnd.chance(10))
					{
						_def_think = false;
						return true;
					}
					else
						tryMoveToTarget(_temp_attack_target, currentTask.skill.getCastRangeForAi());

					return false;
				}

				setAttackTarget(_temp_attack_target);

				// Преследовать до тех пор пока не приблизиться на расстояние атаки, потом атаковать
				if(_thisActor.getRealDistance(_temp_attack_target) > currentTask.skill.getCastRangeForAi() + 40 || !GeoEngine.canSeeTarget(_actor, _temp_attack_target))
				{
					if(_thisActor.isMovementDisabled())
					{
						_def_think = false;
						return true;
					}

					if(!_isMobile)
					{
						_def_think = false;
						return true;
					}

					if(!tryMoveToTarget(_temp_attack_target, currentTask.skill.getCastRangeForAi()))
						return false;
				}
				else
				{
					_pathfind_fails = 0;

					// Обновление таймаута
					_thisActor.setAttackTimeout(MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks());

					clientStopMoving();

					// Использовать скилл на цель
					_thisActor.chargeShots(true);
					_thisActor.doCast(currentTask.skill, _temp_attack_target, null, !(_temp_attack_target instanceof L2Playable));

					// Следующее задание
					_task_list.remove(currentTask);
					// Если заданий больше нет - определить новое
					if(_task_list.size() == 0)
					{
						_def_think = false;
						return true;
					}
				}
			}
			break;
			// Задание "добежать - применить скилл"
			case BUFF:
			{
				_temp_attack_target = currentTask.getTarget();

				if(_temp_attack_target == null || _temp_attack_target.isDead())
					return maybeNextTask(currentTask);

				if(currentTask.skill.getAimingTarget(_thisActor, _temp_attack_target) == _thisActor)
					_temp_attack_target = _thisActor;

				setAttackTarget(_temp_attack_target);

				if(_temp_attack_target == _thisActor)
				{
					_pathfind_fails = 0;

					clientStopMoving();
					// Использовать скилл на цель
					_thisActor.doCast(currentTask.skill, _temp_attack_target, null, !(_temp_attack_target instanceof L2Playable));

					// Следующее задание
					_task_list.remove(currentTask);
					// Если заданий больше нет - определить новое
					if(_task_list.size() == 0)
					{
						_def_think = false;
						return true;
					}

					break;
				}

				if(_temp_attack_target == null || _temp_attack_target.isAlikeDead() || !_thisActor.isInRange(_temp_attack_target, 4000))
					return true;

				if(_actor.isMoving && _temp_attack_target != _thisActor)
					return false;

				// Преследовать до тех пор пока не приблизиться на расстояние атаки, потом атаковать
				if(_thisActor.getRealDistance(_temp_attack_target) > currentTask.skill.getCastRangeForAi() + 40 || !GeoEngine.canSeeTarget(_actor, _temp_attack_target))
				{
					if(_thisActor.isMovementDisabled())
					{
						_def_think = false;
						return true;
					}

					if(!_isMobile)
					{
						_def_think = false;
						return true;
					}

					if(!tryMoveToTarget(_temp_attack_target, currentTask.skill.getCastRangeForAi()))
						return false;
				}
				else
				{
					_pathfind_fails = 0;

					clientStopMoving();

					// Использовать скилл на цель
					_thisActor.doCast(currentTask.skill, _temp_attack_target, null, !(_temp_attack_target instanceof L2Playable));

					// Следующее задание
					_task_list.remove(currentTask);
					// Если заданий больше нет - определить новое
					if(_task_list.size() == 0)
					{
						_def_think = false;
						return true;
					}
				}
			}
			break;
			case FLEE:
			{
				if(_actor.isMoving)
				{
					if(debug)
						_log.info("doTask: " + _thisActor + " already moving loc " + currentTask.loc);
					return false;
				}

				_temp_attack_target = currentTask.getTarget();

				if(_temp_attack_target == null || _temp_attack_target.isDead())
					return maybeNextTask(currentTask);

				if(currentTask.loc == null)
					currentTask.loc = Util.correctCollision(_temp_attack_target.getX(), _temp_attack_target.getY(), _thisActor.getX(), _thisActor.getY(), _thisActor.getZ(), 500);

				if(!_thisActor.isInRange(currentTask.loc, 50))
				{
					if(_actor.isMovementDisabled())
					{
						if(debug)
							_log.info("doTask: " + _thisActor + " movement disabled.");
						_def_think = false;
						return true;
					}

					if(!_thisActor.moveToLocation(currentTask.loc, 0, currentTask.usePF))
					{
						//ThreadPoolManager.getInstance().scheduleAi(new Teleport(currentTask.loc), 500, false);
						//return false;
						if(debug)
							_log.info("doTask: " + _thisActor + " cannot move to loc " + currentTask.loc + " tl size: " + _task_list.size());
						_task_list.remove(currentTask);
						if(_task_list.size() == 0)
						{
							_def_think = false;
							return true;
						}
						return false;
					}
					else if(debug)
						_log.info("doTask: " + _thisActor + " move to loc " + currentTask.loc);
				}
				else
				{
					// Следующее задание
					_task_list.remove(currentTask);
					// Если заданий больше нет - определить новое
					if(_task_list.size() == 0)
					{
						_def_think = false;
						return true;
					}
				}
			}
			break;
			case EFFECT_ACTION:
				if(debug)
					_log.info("doTask: " + _thisActor + " effect action: " + currentTask.p2);

				_thisActor.broadcastPacket(new SocialAction(_thisActor.getObjectId(), currentTask.p2));
				maybeNextTask(currentTask);
				break;
		}

		return false;
	}

	protected boolean createNewTask()
	{
		return false;
	}

	@Override
	protected void onIntentionAttack(L2Character target)
	{
		// Удаляем все задания
		//clearTasks();

		setAttackTarget(target);
		changeIntention(AI_INTENTION_ATTACK, target, null);
		clientStopMoving();
		onEvtThink();
	}

	@Override
	protected void onEvtThink()
	{

		if(debug)
			_log.info("onEvtThink: " + _thisActor);

		if(_thinking || _thisActor.isActionsDisabled() || _thisActor.isAfraid() || _thisActor.isDead())
			return;

		if(_randomAnimationEnd > System.currentTimeMillis())
			return;

		_thinking = true;

		if(_thisActor.getAggroRange() > 0)
			_thisActor.lookNeighbor(_thisActor.getAggroRange());

		try
		{
			if(_intention == AI_INTENTION_ACTIVE || _intention == AI_INTENTION_IDLE)
				thinkActive();
			else if(_intention == AI_INTENTION_ATTACK)
			{
				_isMovingBack = false;
				thinkAttack();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			_thinking = false;
		}
	}

	@Override
	protected void onEvtDead(L2Character killer)
	{
		stopAITask();

		_thisActor.stopHate();

		// 10 seconds timeout of ATTACK after respawn
		setGlobalAggro(getInt("global_aggro", -10));

		_thisActor.setAttackTimeout(Integer.MAX_VALUE);

		// Удаляем все задания
		clearTasks();

		super.onEvtDead(killer);
		debug = false;
		_useUD = false;
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage, L2Skill skill)
	{
		if(attacker == null)
			return;

		if(!_thisActor.canAttackCharacter(attacker))
			return;

		int aggro = skill != null ? skill.getEffectPoint() : 0;

		L2Player _player = attacker.getPlayer();
		if(_player != null)
		{
			Quest[] quests = _thisActor.getTemplate().getEventQuests(Quest.QuestEventType.ON_ATTACKED);
			if(quests != null)
				for(Quest q : quests)
					q.notifyAttack(_thisActor, _player, skill);

			// 1 хейт добавляется хозяину суммона, чтобы после смерти суммона моб накинулся на хозяина.
			if((damage > 0 || aggro > 0) && attacker.getPlayer() != null && (attacker.isSummon() || attacker.isPet()))
			{
				if(!((_thisActor.isRaid() || _thisActor.isRaidMinion()) && attacker.getPlayer().getLevel() > _thisActor.getLevel() + Config.RAID_MAX_LEVEL_DIFF))
					_thisActor.addDamageHate(attacker.getPlayer(), 0, 1);
			}
		}

		_isMovingBack = false;
		if(debug)
			_log.info(_thisActor + " callFriends");
		_thisActor.callFriends(attacker, damage > 0 ? damage : aggro);

		if((_thisActor.isRaid() || _thisActor.isRaidMinion() || _isRbMinion) && !attacker.isDead() && attacker.getLevel() > _thisActor.getLevel() + Config.RAID_MAX_LEVEL_DIFF)
		{
			L2Skill revengeSkill = skill != null && (skill.isMagic() || skill.isSongDance()) ? SkillTable.getInstance().getInfo(L2Skill.SKILL_RAID_SILENS, 1) : SkillTable.getInstance().getInfo(L2Skill.SKILL_RAID_CURSE, 1);
			if(!_thisActor.isSkillDisabled(revengeSkill.getId()))
				_thisActor.altUseSkill(revengeSkill, attacker);

			_thisActor.addDamageHate(attacker, damage, aggro);
			return;
		}

		if(checkAttacker(_player))
			return;

		_thisActor.setAttackTimeout(MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks());
		setGlobalAggro(0);

		_thisActor.addDamageHate(attacker, damage, aggro);

		if(!_useUD && _useUDChance > 0 && !_thisActor.isSkillDisabled(_ud.getId()) && !_thisActor.isInRange(attacker, 150))
		{
			if(Rnd.chance(_useUDChance))
			{
				clearTasks();
				_def_think = false;
				_useUD = true;
			}
		}

		if(_ud != null && _thisActor.isInRange(attacker, _thisActor.getPhysicalAttackRange() + 50) && _thisActor.getEffectBySkill(_ud) != null)
			_thisActor.stopEffect(_ud.getId());

		if(!_actor.isRunning())
			startRunningTask(1000);

		if(_intention != CtrlIntention.AI_INTENTION_ATTACK)
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
	}

	@Override
	protected void onEvtClanAttacked(L2Character attacked_member, L2Character attacker, int damage)
	{
		if(_thisActor == null || !_thisActor.isInRange(attacked_member, _thisActor.getFactionRange()) || _thisActor.isChampion() > 0 && !Config.ALT_CHAMPION_SOCIAL)
			return;
		if(Math.abs(attacker.getZ() - _thisActor.getZ()) > 400)
			return;

		notifyEvent(CtrlEvent.EVT_AGGRESSION, attacker, (int) ((double) damage * 0.25 + 0.5));
	}

	@Override
	protected void onEvtAggression(L2Character attacker, int aggro, L2Skill skill)
	{
		if(attacker == null)
			return;

		L2Player _player = attacker.getPlayer();

		if(_player != null)
		{
			if(!_thisActor.canAttackCharacter(attacker))
				return;

			if((_thisActor.isRaid() || _thisActor.isRaidMinion() || _isRbMinion) && !attacker.isDead() && attacker.getLevel() > _thisActor.getLevel() + Config.RAID_MAX_LEVEL_DIFF)
			{
				L2Skill revengeSkill = skill != null && (skill.isMagic() || skill.isSongDance()) ? SkillTable.getInstance().getInfo(L2Skill.SKILL_RAID_SILENS, 1) : SkillTable.getInstance().getInfo(L2Skill.SKILL_RAID_CURSE, 1);
				if(!_thisActor.isSkillDisabled(revengeSkill.getId()))
					_thisActor.altUseSkill(revengeSkill, attacker);
				return;
			}

			if(checkAttacker(_player))
				return;
		}

		_isMovingBack = false;
		_thisActor.setAttackTimeout(MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks());
		setGlobalAggro(0);

		// 1 хейт добавляется хозяину суммона, чтобы после смерти суммона моб накинулся на хозяина.
		if(_player != null && aggro > 0 && attacker.getPlayer() != null && (attacker.isSummon() || attacker.isPet()))
			_thisActor.addDamageHate(attacker.getPlayer(), 0, 1);

		_thisActor.addDamageHate(attacker, 0, aggro);

		if(!_actor.isRunning())
			startRunningTask(1000);

		if(_intention != CtrlIntention.AI_INTENTION_ATTACK)
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
	}

	@Override
	protected void onEvtSeeSpell(L2Skill skill, L2Character caster)
	{
		if(caster != null && (!caster.isPlayer() || !caster.getPlayer().isInvisible())
				&& skill != null && !skill.isToggle() && !skill.isHandler() && !skill.isTriggered()
				&& Math.abs(caster.getZ() - _thisActor.getZ()) < _see_spell_z
				&& (_thisActor.isRaid() || _thisActor.isRaidMinion() || _isRbMinion) && caster.getLevel() > _thisActor.getLevel() + Config.RAID_MAX_LEVEL_DIFF
				&& caster.getCastingTarget() != null && _thisActor.getHate(caster.getCastingTarget()) > 0 && (skill.isMagic() || skill.isPhysic()))
		{
			L2Skill revengeSkill = skill.isMagic() ? SkillTable.getInstance().getInfo(L2Skill.SKILL_RAID_SILENS, 1) : SkillTable.getInstance().getInfo(L2Skill.SKILL_RAID_CURSE, 1);
			_thisActor.doCast(revengeSkill, caster, null, true);
		}
	}

	@Override
	protected void onEvtPartyDead(L2NpcInstance partyPrivate)
	{
		if(debug)
			_log.info(_thisActor + " onEvtPartyDead: " + partyPrivate + " leader: " + partyPrivate.getLeader());
		if(partyPrivate.getLeader() == _thisActor && partyPrivate.getMinionData().minionRespawn > 0)
		{
			if(debug)
				_log.info(_thisActor + " add respawn: " + partyPrivate + " " + partyPrivate.getMinionData().minionRespawn);
			_thisActor.respawnPrivate(partyPrivate, partyPrivate.weight_point, partyPrivate.getMinionData().minionRespawn);
		}
		else if(_thisActor.getLeader() == partyPrivate && _thisActor.isRaidMinion())
			addTimer(1004, 20000);
	}

	@Override
	protected void onEvtPartyAttacked(L2Character attacker, L2Character victim, int damage)
	{
		onEvtAggression(attacker, Rnd.get(1, 100), null);
	}

	protected boolean maybeMoveToHome()
	{
		boolean randomWalk = _actor.hasRandomWalk();
		if(_thisActor.getSpawnedLoc() == null)
			return false;

		// Random walk or not?
		if(randomWalk && (!Config.RND_WALK || !Rnd.chance(Config.RND_WALK_RATE)))
			return false;

		if(!randomWalk && _thisActor.isInRangeZ(_thisActor.getSpawnedLoc(), Config.MAX_DRIFT_RANGE))
			return false;

		int x = _thisActor.getSpawnedLoc().getX() + Rnd.get(-Config.MAX_DRIFT_RANGE, Config.MAX_DRIFT_RANGE);
		int y = _thisActor.getSpawnedLoc().getY() + Rnd.get(-Config.MAX_DRIFT_RANGE, Config.MAX_DRIFT_RANGE);
		int z = _thisActor.isFlying() ? Rnd.get(_thisActor.getZ() - Config.MAX_DRIFT_RANGE / 2, _thisActor.getZ() + Config.MAX_DRIFT_RANGE / 2) : GeoEngine.getHeight(x, y, _thisActor.getSpawnedLoc().getZ(), _thisActor.getReflection());

		if(_thisActor.getSpawnedLoc().getZ() - z > 200 && !_thisActor.isFlying())
			return false;

		L2Spawn spawn = _thisActor.getSpawn();
		if(spawn != null && spawn.getLocation() != 0 && !TerritoryTable.getInstance().getLocation(spawn.getLocation()).isInside(x, y))
			return false;

		_thisActor.setWalking();
		_thisActor.moveToLocation(x, y, z, 0, true);

		return true;
	}

	public void returnHome()
	{
		if(debug)
			_log.info("returnHome: " + _thisActor + " stopHate");
		_thisActor.stopHate();
		setIntention(AI_INTENTION_ACTIVE);
		_thisActor.setWalking();
		// Удаляем все задания
		clearTasks();

		if(debug)
			_log.info("returnHome: " + _thisActor + " add move task: " + _thisActor.getSpawnedLoc());
		// Прибежать в заданную точку и переключиться в состояние AI_INTENTION_ACTIVE
		Task task = new Task();
		task.type = TaskType.MOVE;
		task.loc = _thisActor.getSpawnedLoc();
		_task_list.add(task);
		_def_think = true;
	}

	@Override
	public void teleportHome()
	{
		if(debug)
			_log.info("teleportHome: " + _thisActor + " spawn: " + _thisActor.getSpawn() + " spawnedLoc: " + _thisActor.getSpawnedLoc());
		if(_thisActor.getSpawnedLoc() != null)
		{
			_thisActor.stopHate();
			if(debug)
				_log.info("teleportHome: " + _thisActor + " teleport to: " + _thisActor.getSpawnedLoc());
			_thisActor.teleToLocation(_thisActor.getSpawnedLoc().getX(), _thisActor.getSpawnedLoc().getY(), GeoEngine.getHeight(_thisActor.getSpawnedLoc(), _thisActor.getReflection()));
			setIntention(AI_INTENTION_ACTIVE);
			clearTasks();
		}
	}

	@Override
	public boolean isActive()
	{
		return _aiTask != null;
	}

	public void clearTasks()
	{
		if(debug)
			_log.info(_thisActor + " clearTask");

		_def_think = false;
		try
		{
			synchronized(_task_list)
			{
				_task_list.clear();
			}
		}
		catch(NullPointerException e)
		{
		}
	}

	/**
	 * Шедюлит мобу переход в режим бега через определенный интервал времени
	 *
	 * @param interval
	 */
	public void startRunningTask(int interval)
	{
		if(_runningTask == null && !_thisActor.isRunning())
			_runningTask = ThreadPoolManager.getInstance().scheduleAi(new RunningTask(), interval, false);
	}

	@Override
	public boolean isGlobalAggro()
	{
		return _globalAggro >= 0;
	}

	@Override
	public void setGlobalAggro(int value)
	{
		_globalAggro = value;
	}

	private StatsSet getAIParams()
	{
		if(_thisActor.getAIParams() != null)
			return _thisActor.getAIParams();
		return new StatsSet();
	}

	public boolean getBool(String name)
	{
		return getAIParams().getBool(name);
	}

	public boolean getBool(String name, boolean _defult)
	{
		return getAIParams().getBool(name, _defult);
	}

	public int getInt(String name)
	{
		return getAIParams().getInteger(name);
	}

	public int getInt(String name, int _defult)
	{
		return getAIParams().getInteger(name, _defult);
	}

	public long getLong(String name)
	{
		return getAIParams().getLong(name);
	}

	public long getLong(String name, long _defult)
	{
		return getAIParams().getLong(name, _defult);
	}

	public float getFloat(String name)
	{
		return getAIParams().getFloat(name);
	}

	public float getFloat(String name, float _defult)
	{
		return getAIParams().getFloat(name, _defult);
	}

	public String getString(String name)
	{
		return getAIParams().getString(name);
	}

	public String getString(String name, String _defult)
	{
		return getAIParams().getString(name, _defult);
	}

	public List<L2Skill> getEnabledSkills(L2Skill[] skills)
	{
		FastList<L2Skill> ret = new FastList<L2Skill>();
		for(L2Skill skill : skills)
		{
			if(skill != null && !_thisActor.isSkillDisabled(skill.getId()))
				ret.add(skill);
		}
		return ret;
	}

	public L2Skill getSkillByRange(L2Skill[] skills, int distance)
	{
		L2Skill skillMax = null;
		L2Skill skillOpt = null;
		int maxRange = 0;
		int minDiff = Integer.MAX_VALUE;
		for(L2Skill skill : skills)
		{
			if(skill == null)
				continue;

			if(skill.getCastRangeForAi() > maxRange || (skillMax != null && _thisActor.isSkillDisabled(skillMax.getId()) && skillMax.getCastRangeForAi() == skill.getCastRangeForAi()))
			{
				skillMax = skill;
				maxRange = skill.getCastRangeForAi();
			}

			if((skill.getCastRangeForAi() + _thisActor.getCollisionRadius() >= distance && skill.getCastRangeForAi() - distance < minDiff) || (skillOpt != null && _thisActor.isSkillDisabled(skillOpt.getId()) && skillOpt.getCastRangeForAi() == skill.getCastRangeForAi()))
			{
				skillOpt = skill;
				minDiff = skill.getCastRangeForAi() - distance;
			}
		}

		if(skillOpt == null && skillMax == null)
			return null;

		if(skillOpt == null)
			return _thisActor.isSkillDisabled(skillMax.getId()) ? null : skillMax;

		return !_thisActor.isSkillDisabled(skillOpt.getId()) ? skillOpt : skillMax != null && !_thisActor.isSkillDisabled(skillMax.getId()) ? skillMax : null;
	}

	protected boolean checkAttacker(L2Player _player)
	{
		boolean ret = false;
		if(_thisActor.isCatacombMob() && _player != null)
		{
			ArrayList<L2Player> players = new ArrayList<L2Player>();
			if(_player.getParty() != null)
				players.addAll(_player.getParty().getPartyMembers());
			else
				players.add(_player);

			for(L2Player player : players)
			{
				if(player == null)
					continue;
				if(player.getDistance(_player) >= Config.ALT_PARTY_DISTRIBUTION_RANGE)
					continue;

				if(SevenSigns.getInstance().isSealValidationPeriod())
				{
					if(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_AVARICE) == SevenSigns.CABAL_NULL ||
							SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_AVARICE) != SevenSigns.getInstance().getCabalWinner() ||
							SevenSigns.getInstance().getCabalWinner() != SevenSigns.getInstance().getPlayerCabal(player))
					{
						if(!ret)
							ret = _player == player;
						_player.teleToClosestTown();
					}
				}
				else if(SevenSigns.getInstance().getPlayerCabal(player) == SevenSigns.CABAL_NULL)
				{
					if(!ret)
						ret = _player == player;
					player.teleToClosestTown();
				}
			}
		}
		return ret;
	}

	public boolean isDebug()
	{
		return debug;
	}

	public void setDebug(boolean val)
	{
		debug = val;
	}

	public boolean findTargetForAttack()
	{
		GArray<L2Player> targets = _thisActor.getAroundPlayers(_thisActor.getAggroRange());

		int c = 0;
		if(targets.size() > 0)
			do
			{
				if(checkAggression(targets.get(Rnd.get(targets.size()))))
					return true;
				c++;
			}
			while(c < 10);

		return false;
	}

	protected L2Player getRandomTarget()
	{
		GArray<L2Player> targets = _thisActor.getAroundPlayers(_thisActor.getAggroRange());

		int c = 0;
		if(targets.size() > 0)
			do
			{
				L2Player target = targets.get(Rnd.get(targets.size()));

				if(target != null && !target.isDead() && !target.isInvul() && !target.isInvisible() && GeoEngine.canSeeTarget(_thisActor, target))
					return target;
				c++;
			}
			while(c < 10);

		return null;
	}

	@Override
	public void setAttackTarget(L2Character target)
	{
		if(debug)
			_log.info(this + " setAttackTarget: " + target);
		super.setAttackTarget(target);
		_thisActor.setTarget(target);
	}

	@Override
	protected void onEvtSpawn()
	{
		_useUDChance = getInt("ud_chance", 0);
		_inMyTerritory = true;
		if(_thisActor.isChampion() > 0)
		{
			_lastChampionTalk = System.currentTimeMillis() + Rnd.get(30, 120) * 1000;
			_useUDChance = 0;
		}

		_thisActor.spawnMinions();
	}

	protected L2Character getFriendTarget(L2Skill skill)
	{
		L2Character target = _thisActor;
		double hp = _thisActor.getCurrentHp() / _thisActor.getMaxHp();
		for(L2NpcInstance npc : _thisActor.getAroundFriends(skill.getCastRange()))
			if(!npc.isDead() && npc.getCurrentHp() / npc.getMaxHp() < hp)
				target = npc;

		return target;
	}

	@Override
	protected void onEvtOutOfMyTerritory()
	{
	}

	@Override
	public boolean inMyTerritory()
	{
		return _inMyTerritory;
	}

	@Override
	protected void onEvtTimer(int timerId, Object arg1, Object arg2)
	{
		if(debug)
			_log.info("onEvtTimer: " + _thisActor + " timer occured.");

		if(timerId == -1000) // Check territory
		{
			if(debug)
				_log.info("onEvtTimer: " + _thisActor + " check territory timer. Check state is " + check_territory_chstate + " in terr: " + _inMyTerritory + " intention: " + _intention);

			if(!_inMyTerritory && !_thisActor.isDead() && (!check_territory_chstate || _intention == AI_INTENTION_ACTIVE))
			{
				switch(check_territory_return)
				{
					case 0:
						_inMyTerritory = true;
						teleportHome();
						break;
					case 1:
						returnHome();
						_thisActor.setRunning();
						break;
				}
			}

			if(!_thisActor.isDead())
				addTimer(-1000, check_territory_time);
		}
		else if(timerId == 1004)
		{
			if(!_thisActor.isDead())
				_thisActor.onDecay();
		}
	}

	@Override
	protected void onEvtScriptEvent(int eventId, Object arg1, Object arg2)
	{
		if(eventId == 10016)
		{
			L2Character c0 = L2ObjectsStorage.getAsCharacter((Long) arg1);
			if(c0 != null && Rnd.chance(50))
			{
				_thisActor.addDamageHate(c0, 0, 200);
				if(_intention != AI_INTENTION_ATTACK)
				{
					_thisActor.setRunning();
					setIntention(AI_INTENTION_ATTACK, c0);
				}
			}
		}
	}

	protected void checkTerritory()
	{
		if(my_territory != null)
		{
			boolean inTerritory = my_territory.isInside(_thisActor.getX(), _thisActor.getY());
			if(inTerritory != _inMyTerritory)
			{
				if(_inMyTerritory)
					ThreadPoolManager.getInstance().executeAi(new L2ObjectTasks.NotifyAITask(_thisActor, CtrlEvent.EVT_OUT_OF_MY_TERRITORY, null, null), false);
				_inMyTerritory = inTerritory;
			}
		}
		else if(my_maker != null)
		{
			boolean inTerritory = my_maker.isInside(_thisActor.getX(), _thisActor.getY());
			if(inTerritory != _inMyTerritory)
			{
				if(_inMyTerritory)
					ThreadPoolManager.getInstance().executeAi(new L2ObjectTasks.NotifyAITask(_thisActor, CtrlEvent.EVT_OUT_OF_MY_TERRITORY, null, null), false);
				_inMyTerritory = inTerritory;
			}
		}
	}

	protected void randomizeTargets()
	{
		L2Character hated = _thisActor.getMostHated();

		if(hated != null)
		{
			L2NpcInstance.AggroInfo hatedAI = _thisActor.getAggroList().get(hated.getObjectId());
			List<L2Character> hateList = new FastList<L2Character>();

			for(L2NpcInstance.AggroInfo ai : _thisActor.getAggroList().values())
				if(ai != null && ai.hate > 0 && ai.getAttacker() != null)
					hateList.add(ai.getAttacker());

			hateList.remove(hated);

			if(hateList.size() > 0)
			{
				L2Character newHated = hateList.get(Rnd.get(hateList.size()));
				L2NpcInstance.AggroInfo ai = _thisActor.getAggroList().get(newHated.getObjectId());
				long hate = ai.hate;
				ai.hate = hatedAI.hate;
				hatedAI.hate = hate;
			}
		}
	}

	protected boolean maybeNextTask(Task currentTask)
	{
		if(debug)
			_log.info("doTask: maybeNextTask: remove " + currentTask);
		// Следующее задание
		_task_list.remove(currentTask);
		// Если заданий больше нет - определить новое
		return _task_list.isEmpty();
	}

	public void addUseSkillDesire(L2Character target, L2Skill skill, int p1, int p2, double desire)
	{
		addUseSkillDesire(target, skill, p1, p2, (long) desire);
	}

	public void addUseSkillDesire(L2Character target, int skillIndex, int p1, int p2, double desire)
	{
		addUseSkillDesire(target, SkillTable.getInstance().getInfo(skillIndex), p1, p2, (long) desire);
	}

	public void addUseSkillDesire(L2Character target, int skillIndex, int p1, int p2, long desire)
	{
		addUseSkillDesire(target, SkillTable.getInstance().getInfo(skillIndex), p1, p2, desire);
	}

	public void addUseSkillDesire(L2Character target, L2Skill skill, int p1, int p2, long desire)
	{
		if(debug)
			_log.info(this + " addUseSkillDesire: skill " + skill + " -> " + target);

		if(skill == null || target == null)
			return;

		Task task = new Task();
		task.type = skill.isOffensive() ? TaskType.CAST : TaskType.BUFF;
		task.targetId = target.getStoredId();
		task.skill = skill;
		task.p1 = p1;
		task.p2 = p2;
		task.weight = desire;
		_globalAggro = 0;
		if(!_task_list.add(task) && debug)
			_log.info(this + " addUseSkillDesire: skill " + skill + " -> " + target + " IGNORED");
		_def_think = true;
	}

	public void addAttackDesire(L2Character target, int p1, long desire)
	{
		if(target == null)
			return;
		Task task = new Task();
		task.type = TaskType.ATTACK;
		task.targetId = target.getStoredId();
		task.p1 = p1;
		task.weight = desire;
		_globalAggro = 0;

		if(!_thisActor.getAggroList().containsKey(target.getObjectId()))
			_thisActor.addDamageHate(target, 0, desire);

		if(debug)
			_log.info(_thisActor + " addAttackDesire: " + task);

		_def_think = true;
		_task_list.add(task);

		if(!_actor.isRunning())
			startRunningTask(1000);

		if(_intention != AI_INTENTION_ATTACK)
			setIntention(AI_INTENTION_ATTACK, target);
	}

	public void addFleeDesire(L2Character target, long desire)
	{
		if(target == null)
			return;
		Task task = new Task();
		task.type = TaskType.FLEE;
		task.targetId = target.getStoredId();
		task.weight = desire;
		_thisActor.setRunning();
		_task_list.add(task);
		_def_think = true;
	}

	public void removeAllAttackDesire()
	{
		if(debug)
			_log.info(_thisActor + " removeAllAttackDesire");
		_thisActor.stopHate();
		for(Task task : _task_list)
			if(task.type == TaskType.ATTACK || task.type == TaskType.CAST)
				_task_list.remove(task);
		setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
	}

	public void addMoveToDesire(int x, int y, int z, long desire)
	{
		addMoveToDesire(new Location(x, y, z), desire);
	}

	public void addMoveToDesire(Location loc, long desire)
	{
		Task task = new Task();
		task.type = TaskType.MOVE;
		task.loc = loc;
		task.weight = desire;
		_task_list.add(task);
		_def_think = true;
	}

	public void addMoveAroundDesire2(int p1, long desire)
	{
		if(_intention != AI_INTENTION_ACTIVE || !Rnd.chance(p1))
			return;

		Task task = new Task();
		task.type = TaskType.MOVE;
		task.p1 = p1;
		Location loc = _thisActor.getLoc();
		task.loc = GeoEngine.findPointToStay(loc.getX(), loc.getY(), loc.getZ(), 100, 200, _thisActor.getReflection());
		task.weight = desire;
		_task_list.add(task);
		_def_think = true;
	}

	public void addMoveAroundDesire(int p1, long desire)
	{
		if(_intention != AI_INTENTION_ACTIVE || !Rnd.chance(p1))
			return;

		Task task = new Task();
		task.type = TaskType.MOVE;
		task.p1 = p1;
		Location loc = _thisActor.getSpawnedLoc() == null ? _thisActor.getLoc() : _thisActor.getSpawnedLoc();
		task.loc = GeoEngine.findPointToStay(loc.getX(), loc.getY(), loc.getZ(), 50, 150, _thisActor.getReflection());
		task.weight = desire;
		_task_list.add(task);
		_def_think = true;
	}

	public void addMoveSuperPointDesire(String superPoint, int p1, long desire)
	{
		Task task = new Task();
		task.type = TaskType.MOVE_SUPERPOINT;
		task.p1 = p1;
		task.superpoint = SuperpointManager.getInstance().getSuperpointByName(superPoint);
		if(task.superpoint != null)
		{
			task.loc = task.superpoint.getNextNode(_thisActor, p1);
			task.weight = desire;
			_task_list.add(task);
			_def_think = true;
		}
	}

	public void addEffectActionDesire(int social, int p1, long desire)
	{
		Task task = new Task();
		task.type = TaskType.EFFECT_ACTION;
		task.p1 = p1;
		task.p2 = social;
		task.weight = desire;
		_task_list.add(task);
		_def_think = true;
	}

	public void addFollowDesire(L2Character target, int desire)
	{
		if(target == null)
			return;
		Task task = new Task();
		task.type = TaskType.FOLLOW;
		task.weight = desire;
		task.targetId = target.getStoredId();
		_task_list.add(task);
		_def_think = true;
	}

	public void addFollowDesire2(L2Character target, int desire, int min, int max)
	{
		if(target == null)
			return;
		Task task = new Task();
		task.type = TaskType.FOLLOW2;
		task.weight = desire;
		task.targetId = target.getStoredId();
		task.p1 = min;
		task.p2 = max;
		_task_list.add(task);
		_def_think = true;
	}

	public long getStoredIdFromCreature(L2Character creature)
	{
		if(creature == null)
			return 0;

		return creature.getStoredId();
	}

	public void removeAttackDesire(L2Character target)
	{
		if(target == null)
			return;

		if(debug)
			_log.info(_thisActor + " removeAttackDesire " + target);

		_thisActor.stopHate(target);
		for(Task task : _task_list)
			if(task.targetId == target.getStoredId() && (task.type == TaskType.ATTACK || task.type == TaskType.CAST))
				_task_list.remove(task);
	}

	public void removeTaskByTarget(L2Character target)
	{
		if(target == null)
			return;

		if(debug)
			_log.info(_thisActor + " removeTaskByTarget " + target);

		for(Task task : _task_list)
			if(task.targetId == target.getStoredId() && (task.type == TaskType.ATTACK || task.type == TaskType.CAST))
				_task_list.remove(task);
	}

	public void randomTeleportInMyTerritory()
	{
		if(_thisActor.getSpawnDefine() != null)
			_thisActor.teleToLocation(_thisActor.getSpawnDefine().getRandomPosInMyTerritory(_thisActor));
		else if(my_territory != null)
		{
			int p[] = my_territory.getRandomPoint(_thisActor.isFlying());
			_thisActor.teleToLocation(p[0], p[1], p[2]);
		}
		else
			_thisActor.teleToLocation(_thisActor.getSpawnedLoc());
	}

	@Override
	public GArray<Task> getTaskList()
	{
		GArray<Task> tasks = new GArray<>(_task_list.size());
		tasks.addAll(_task_list);
		return tasks;
	}
}