package ru.l2gw.gameserver.instancemanager.boss;

import javolution.util.FastList;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Spawn;
import ru.l2gw.gameserver.model.entity.BossState;
import ru.l2gw.gameserver.model.entity.Entity;
import ru.l2gw.gameserver.model.instances.L2BossInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.serverpackets.Earthquake;
import ru.l2gw.gameserver.serverpackets.SocialAction;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.util.Location;
import ru.l2gw.commons.math.Rnd;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by IntelliJ IDEA.
 * User: admin
 * Date: 09.02.2009
 * Time: 18:51:56
 */
public class BaiumManager extends Entity
{
	private static BaiumManager _instance;
	// status in lair.
	protected BossState _state = new BossState(29020);

	private int _minionsSpawn[][] =
			{{114501, 15921, 10082},
			{114353, 16795, 10082},
			{115315, 17437, 10082},
			{116037, 16712, 10082},
			{115306, 15963, 10082}};

	private long _lastAttackTime = 0;
	private final int STONE_BAIUM = 29025;
	private final int LIVE_BAIUM = 29020;
	private final int MINIONS = 29021;
	private final int CHECK_INTERVAL = 1200000;
	private final Location spawnPoint = new Location(116026, 17426, 10106, 37604);
	// list of intruders.
	protected List<L2Player> _playersInLair = new FastList<L2Player>();
	// instance of monsters.
	protected List<L2NpcInstance> _minions = new FastList<L2NpcInstance>();
	private L2BossInstance _baium = null;

	protected Future<?> _intervalEndTask = null;
	protected Future<?> _minionSpawnTask = null;
	protected Future<?> _activityCheckTask = null;

	protected String _questName;

	public static BaiumManager getInstance()
	{
		if(_instance == null)
			_instance = new BaiumManager();
		return _instance;
	}

	// initialize
	public void init()
	{
		_playersInLair.clear();
		_questName = "Baium";

		if(_state.getState() == BossState.State.ALIVE)
		{
			_state.setState(BossState.State.NOTSPAWN);
			_state.update();
		}

		_log.info("BaiumManager: State of Baium is " + _state.getState() + ".");

		if(_state.getState() == BossState.State.DEAD)
		{
			deadEndTask();
			Date dt = new Date(_state.getRespawnDate());
			_log.info("BaiumManager: Next spawn date of Baium is " + dt + ".");
		}
		else if(_state.getState() == BossState.State.NOTSPAWN)
			spawnStoneBaium();

		_log.info("BaiumManager: Init BaiumManager.");
		
	}

	private void spawnStoneBaium()
	{
		L2NpcTemplate template = NpcTable.getTemplate(STONE_BAIUM);
		L2Spawn spawn;

		if(_baium != null)
		{
			_baium.deleteMe();
			_baium = null;
		}

		try
		{
			if(template != null)
			{
				spawn = new L2Spawn(template);
				spawn.setLoc(spawnPoint);
				spawn.stopRespawn();
				spawn.spawnOne();
			}
		}
		catch(Exception e)
		{
			_log.warn("BaiumManager: Stone Baium spawn error! "+e);
		}
	}

	private void deadEndTask()
	{
		//init state of Antharas's lair.
		_log.info("BaiumManager: start Dead task");
		if(!_state.getState().equals(BossState.State.DEAD))
		{
			_state.setRespawnDate(Rnd.get(Config.FWB_FIXINTERVALOFBAIUM, Config.FWB_FIXINTERVALOFBAIUM + Config.FWB_RANDOMINTERVALOFBAIUM));
			_state.setState(BossState.State.DEAD);
			_state.update();
		}

		_intervalEndTask = ThreadPoolManager.getInstance().scheduleGeneral(new IntervalEnd(), _state.getInterval());
	}

	// at end of interval.
	private class IntervalEnd implements Runnable
	{
		public IntervalEnd()
		{
		}

		public void run()
		{
			_playersInLair.clear();
			_state.setState(BossState.State.NOTSPAWN);
			_state.update();
			spawnStoneBaium();
		}
	}

	public BossState.State getState()
	{
		return _state.getState();
	}

	public void wakeUp()
	{
		L2NpcTemplate template = NpcTable.getTemplate(LIVE_BAIUM);
		L2Spawn spawn;
		try{
			spawn = new L2Spawn(template);
			spawn.setLoc(spawnPoint);
			spawn.stopRespawn();
			_baium = (L2BossInstance)spawn.spawnOne();
			_state.setState(BossState.State.ALIVE);
			_state.update();
			_baium.broadcastPacket(new SocialAction(_baium.getObjectId(), 2));
			_baium.setImobilised(true);
			_baium.setIsInvul(true);
			_minionSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new MinionSpawnTask(), 15000);
			_activityCheckTask = ThreadPoolManager.getInstance().scheduleGeneral(new ActivityCheckTask(), CHECK_INTERVAL);
			_lastAttackTime = System.currentTimeMillis();
			_log.info("BaiumManager: spawn Baium");
		}
		catch(Exception e)
		{
			_log.warn("BaiumManager: wakeUp error: "+e);
		}
	}

	private class MinionSpawnTask implements Runnable
	{
		public void run()
		{
			try{
				_baium.broadcastPacket(new SocialAction(_baium.getObjectId(), 1));
				_baium.broadcastPacket(new Earthquake(_baium.getLoc(), 40, 5));
				_baium.setImobilised(false);
				_baium.setIsInvul(false);
				_minions.clear();
				L2NpcTemplate template = NpcTable.getTemplate(MINIONS);
				for(int[] sp : _minionsSpawn)
				{
					L2Spawn spawn = new L2Spawn(template);
					spawn.setLocx(sp[0]);
					spawn.setLocy(sp[1]);
					spawn.setLocz(sp[2]);
					spawn.stopRespawn();
					_minions.add(spawn.doSpawn(true));
					//TODO Написать АИ миникам чтобы они атаковали баюма
				}
				_minionSpawnTask = null;
			}
			catch(Exception e)
			{
				_log.warn("BaiumMnagaer: minions spawn error! "+e);
			}
		}
	}

	private class ActivityCheckTask implements Runnable
	{
		public void run()
		{
			if(_lastAttackTime + CHECK_INTERVAL < System.currentTimeMillis())
			{
				_log.info("BaiumManager: activity check, sleep baium");
				if(_state.getState().equals(BossState.State.ALIVE))
				{
					for(L2NpcInstance minion : _minions)
					{
						if(minion != null)
							minion.deleteMe();
					}
					_minions.clear();
					banishesPlayers();
					_state.setState(BossState.State.NOTSPAWN);
					_state.update();
					spawnStoneBaium();
				}
			}
			else
				_activityCheckTask = ThreadPoolManager.getInstance().scheduleGeneral(new ActivityCheckTask(), CHECK_INTERVAL);
		}
	}

	public void updateLastAttack()
	{
		_lastAttackTime = System.currentTimeMillis();
	}

	public void spawnCube()
	{
		_log.info("BaiumManager: Baium killed spawn Cube");
		deadEndTask();
		if(_activityCheckTask != null)
			_activityCheckTask.cancel(true);

		_activityCheckTask = null;

		for(L2NpcInstance minion : _minions)
		{
			if(minion != null)
				minion.deleteMe();
		}
		_minions.clear();
	}

	// banishes players from lair.
	public void banishesPlayers()
	{
		L2Player pc;
		for(L2Character ch : _zone.getCharacters())
		{
			if(ch.isPlayer())
				pc = (L2Player) ch;
			else
				continue;

			if(pc.getQuestState(_questName) != null)
				pc.getQuestState(_questName).exitCurrentQuest(true);

			if(checkIfInZone(pc))
				pc.teleToLocation(Location.coordsRandomize(new Location(116862, 16070, -5126), 200));
		}
		_playersInLair.clear();
	}

	public void addPlayer(L2Player player)
	{
		_playersInLair.add(player);
	}
}
