package ru.l2gw.gameserver.instancemanager.boss;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Spawn;
import ru.l2gw.gameserver.model.entity.BossState;
import ru.l2gw.gameserver.model.entity.Entity;
import ru.l2gw.gameserver.model.instances.L2BossInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.serverpackets.SocialAction;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.tables.SpawnTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.util.Location;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;


public class ValakasManager extends Entity
{
	private final static Log _log = LogFactory.getLog(ValakasManager.class.getName());
	private static ValakasManager _instance = new ValakasManager();

	// location of teleport cube.
	private final int _teleportCubeId = 31759;
	private final int _teleportCubeLocation[][] =
			{
					{214880, -116144, -1644, 0},
					{213696, -116592, -1644, 0},
					{212112, -116688, -1644, 0},
					{211184, -115472, -1664, 0},
					{210336, -114592, -1644, 0},
					{211360, -113904, -1644, 0},
					{213152, -112352, -1644, 0},
					{214032, -113232, -1644, 0},
					{214752, -114592, -1644, 0},
					{209824, -115568, -1421, 0},
					{210528, -112192, -1403, 0},
					{213120, -111136, -1408, 0},
					{215184, -111504, -1392, 0},
					{215456, -117328, -1392, 0},
					{213200, -118160, -1424, 0}
			};
	protected List<L2Spawn> _teleportCubeSpawn = new FastList<L2Spawn>();
	protected List<L2NpcInstance> _teleportCube = new FastList<L2NpcInstance>();
	private long _lastAttackTime = 0;

	// list of intruders.
	protected List<L2Player> _playersInLair = new FastList<L2Player>();

	// spawn data of monsters.
	protected Map<Integer, L2Spawn> _monsterSpawn = new FastMap<Integer, L2Spawn>();

	// instance of monsters.
	protected List<L2NpcInstance> _monsters = new FastList<L2NpcInstance>();

	// tasks.
	protected Future<?> _cubeSpawnTask = null;
	protected Future<?> _monsterSpawnTask = null;
	protected Future<?> _intervalEndTask = null;
	protected Future<?> _activityTimeEndTask = null;
	protected Future<?> _onPlayersAnnihilatedTask = null;
	protected Future<?> _socialTask = null;
	protected Future<?> _mobiliseTask = null;
	protected Future<?> _moveAtRandomTask = null;
	protected Future<?> _respawnValakasTask = null;

	// status in lair.
	protected BossState _state = new BossState(29028);
	protected String _questName;

	// location of banishment
	private final int _banishmentLocation[][] =
			{
					{150604, -56283, -2980},
					{144857, -56386, -2980},
					{147696, -56845, -2780}
			};

	public ValakasManager()
	{
	}

	public static ValakasManager getInstance()
	{
		if(_instance == null) _instance = new ValakasManager();
		return _instance;
	}

	// initialize
	public void init()
	{
		// initialize status in lair.
		_playersInLair.clear();
		_questName = "Valakas";

		// setting spawn data of monsters.
		try
		{
			L2NpcTemplate template1;
			L2Spawn tempSpawn;

			// Valakas.
			template1 = NpcTable.getTemplate(29028);
			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(212852);
			tempSpawn.setLocy(-114842);
			tempSpawn.setLocz(-1632);
			//tempSpawn.setHeading(22106);
			tempSpawn.setHeading(833);
			tempSpawn.setAmount(1);
			tempSpawn.setRespawnDelay(Config.FWV_ACTIVITYTIMEOFVALAKAS * 2);
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false, null);
			_monsterSpawn.put(29028, tempSpawn);

			// Dummy Valakas.
			template1 = NpcTable.getTemplate(32123);
			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(212852);
			tempSpawn.setLocy(-114842);
			tempSpawn.setLocz(-1632);
			//tempSpawn.setHeading(22106);
			tempSpawn.setHeading(833);
			tempSpawn.setAmount(1);
			tempSpawn.setRespawnDelay(Config.FWV_ACTIVITYTIMEOFVALAKAS * 2);
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false, null);
			_monsterSpawn.put(32123, tempSpawn);
		}
		catch(Exception e)
		{
			_log.warn(e.getMessage());
		}

		// setting spawn data of teleport cube.
		try
		{
			L2NpcTemplate Cube = NpcTable.getTemplate(_teleportCubeId);
			L2Spawn spawnDat;
			for(int[] element : _teleportCubeLocation)
			{
				spawnDat = new L2Spawn(Cube);
				spawnDat.setAmount(1);
				spawnDat.setLocx(element[0]);
				spawnDat.setLocy(element[1]);
				spawnDat.setLocz(element[2]);
				spawnDat.setHeading(element[3]);
				spawnDat.setRespawnDelay(60);
				spawnDat.setLocation(0);
				SpawnTable.getInstance().addNewSpawn(spawnDat, false, null);
				_teleportCubeSpawn.add(spawnDat);
			}
		}
		catch(Exception e)
		{
			_log.warn(e.getMessage());
		}

		if(_state.getState() == BossState.State.ALIVE)
		{
			_state.setState(BossState.State.NOTSPAWN);
			_state.update();
		}
		
		_log.info("ValakasManager: State of Valakas is " + _state.getState() + ".");
		if(_state.getState() == BossState.State.DEAD)
		{
			setInetrvalEndTask();
			Date dt = new Date(_state.getRespawnDate());
			_log.info("ValakasManager: Next spawn date of Valakas is " + dt + ".");
		}
		_log.info("ValakasManager: Init ValakasManager.");
	}

	// return Valakas state.
	public BossState.State getState()
	{
		return _state.getState();
	}

	// update list of intruders.
	public void addPlayerToLair(L2Player pc)
	{
		if(!_playersInLair.contains(pc))
			_playersInLair.add(pc);
	}

	// Whether the players was annihilated is confirmed.
	public synchronized boolean isPlayersAnnihilated()
	{
		for(L2Player pc : _playersInLair)
		{
			// player is must be alive and stay inside of lair.
			if(pc != null && !pc.isDead() && _zone != null && _zone.isCharacterInZone(pc) /*;checkIfInZone(pc)*/)
			{
				return false;
			}
		}
		return true;
	}

	// banishes players from lair.
	public void banishesPlayers()
	{
		L2Player pc;
		for(L2Character ch : _zone.getCharacters())
		{
			if(!ch.isPlayer())
				continue;
			pc = (L2Player) ch;
			if(pc.getQuestState(_questName) != null)
				pc.getQuestState(_questName).exitCurrentQuest(true);
			if(checkIfInZone(pc))
			{
				int driftX = Rnd.get(-80, 80);
				int driftY = Rnd.get(-80, 80);
				int loc = Rnd.get(3);
				pc.teleToLocation(_banishmentLocation[loc][0] + driftX, _banishmentLocation[loc][1] + driftY, _banishmentLocation[loc][2]);
			}
		}
		_playersInLair.clear();
	}

	// do spawn teleport cube.
	public void spawnCube()
	{
		for(L2Spawn spawnDat : _teleportCubeSpawn)
		{
			_teleportCube.add(spawnDat.doSpawn(true));
		}
	}

	// setting Valakas spawn task.
	public void setValakasSpawnTask()
	{
		// When someone has already invaded the lair, nothing is done.
		if(_playersInLair.size() > 0)
			return;

		if(_monsterSpawnTask == null)
		{
			long wormTime = Rnd.get(Config.FWV_APPTIMEOFVALAKAS, Config.FWV_APPTIMEOFVALAKAS * 2);
			_log.info("ValakasManager: worm time: " + wormTime / 1000 / 60 + " min.");
			_monsterSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(1, null),  wormTime);
		}
		else
			_log.info("ValakasManager: spawnValakas spawnTask already running");
	}

	// do spawn Valakas.
	private class ValakasSpawn implements Runnable
	{
		int _distance = 6502500;
		int _taskId;
		L2BossInstance _valakas = null;

		ValakasSpawn(int taskId, L2BossInstance valakas)
		{
			_taskId = taskId;
			_valakas = valakas;
		}

		public void run()
		{
			SocialAction sa;

			switch (_taskId)
			{
				case 1:
					// do spawn.
					L2Spawn valakasSpawn = _monsterSpawn.get(29028);
					_valakas = (L2BossInstance) valakasSpawn.doSpawn(true);
					_monsters.add(_valakas);
					_valakas.setImobilised(true);

					_state.setRespawnDate(Rnd.get(Config.FWV_FIXINTERVALOFVALAKAS, Config.FWV_FIXINTERVALOFVALAKAS + Config.FWV_RANDOMINTERVALOFVALAKAS) + Config.FWV_ACTIVITYTIMEOFVALAKAS);
					_state.setState(BossState.State.ALIVE);
					_state.update();

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(2, _valakas), 1600);

					break;

				case 2:
					// do social.
					sa = new SocialAction(_valakas.getObjectId(), 1);
					_valakas.broadcastPacket(sa);

					// set camera.
					for(L2Player pc : _playersInLair)
					{
						if(pc.isInRangeSq(_valakas, _distance))
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 1800, 180, -1, 1500, 15000);
						}
						else
							pc.leaveMovieMode();
					}

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(3, _valakas), 1500);

					break;

				case 3:
					// set camera.
					for(L2Player pc : _playersInLair)
					{
						if(pc.isInRangeSq(_valakas, _distance))
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 1300, 180, -5, 3000, 15000);
						}
						else
							pc.leaveMovieMode();
					}

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(4, _valakas), 3300);

					break;

				case 4:
					// set camera.
					for(L2Player pc : _playersInLair)
					{
						if(pc.isInRangeSq(_valakas, _distance))
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 500, 180, -8, 600, 15000);
						}
						else
							pc.leaveMovieMode();
					}

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(5, _valakas), 1300);

					break;

				case 5:
					// set camera.
					for(L2Player pc : _playersInLair)
					{
						if(pc.isInRangeSq(_valakas, _distance))
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 1200, 180, -5, 300, 15000);
						}
						else
							pc.leaveMovieMode();
					}

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(6, _valakas), 1600);

					break;

				case 6:
					// set camera.
					for(L2Player pc : _playersInLair)
					{
						if(pc.isInRangeSq(_valakas, _distance))
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 2800, 250, 70, 0, 15000);
						}
						else
							pc.leaveMovieMode();
					}

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(7, _valakas), 200);

					break;

				case 7:
					// set camera.
					for(L2Player pc : _playersInLair)
					{
						if(pc.isInRangeSq(_valakas, _distance))
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 2600, 30, 60, 3400, 15000);
						}
						else
							pc.leaveMovieMode();
					}

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(8, _valakas), 5700);

					break;

				case 8:
					// set camera.
					for(L2Player pc : _playersInLair)
					{
						if(pc.isInRangeSq(_valakas, _distance))
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 700, 150, -65, 0, 15000);
						}
						else
							pc.leaveMovieMode();
					}

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(9, _valakas), 1400);

					break;

				case 9:
					// set camera.
					for(L2Player pc : _playersInLair)
					{
						if(pc.isInRangeSq(_valakas, _distance))
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 1200, 150, -55, 2900, 15000);
						}
						else
							pc.leaveMovieMode();
					}

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(10, _valakas), 6700);

					break;

				case 10:
					// set camera.
					for(L2Player pc : _playersInLair)
					{
						if(pc.isInRangeSq(_valakas, _distance))
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 750, 170, -10, 1700, 5700);
						}
						else
							pc.leaveMovieMode();
					}

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(11, _valakas), 3700);

					break;

				case 11:
					// set camera.
					for(L2Player pc : _playersInLair)
					{
						if(pc.isInRangeSq(_valakas, _distance))
						{
							pc.enterMovieMode();
							pc.specialCamera(_valakas, 840, 170, -5, 1200, 2000);
						}
						else
							pc.leaveMovieMode();
					}

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new ValakasSpawn(12, _valakas), 2000);

					break;

				case 12:
					// reset camera.
					for(L2Player pc : _playersInLair)
					{
						pc.leaveMovieMode();
					}

					_mobiliseTask = ThreadPoolManager.getInstance().scheduleGeneral(new SetMobilised(_valakas), 16);

					// move at random.
					if(Config.FWV_MOVEATRANDOM)
					{
						Location pos = new Location(Rnd.get(211080, 214909), Rnd.get(-115841, -112822), -1662, 0);
						_moveAtRandomTask = ThreadPoolManager.getInstance().scheduleGeneral(new MoveAtRandom(_valakas, pos), 32);
					}

					// set delete task.
					updateLastAttack();
					_activityTimeEndTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new ActivityTimeEnd(), 1200000, 1200000);

					break;
			}
		}
	}

	// at end of activity time.
	private class ActivityTimeEnd implements Runnable
	{
		public void run()
		{
			_log.info("ValakasManager: check activity start");
			if((isPlayersAnnihilated() || System.currentTimeMillis() - _lastAttackTime > 1200000) && _state.getState() == BossState.State.ALIVE)
			{
				_log.info("ValakasManager: set unspawn state");
				_log.info("ValakasManager: last attack " + (System.currentTimeMillis() - _lastAttackTime > 1200000) + " players anihilated " + isPlayersAnnihilated());
				for(L2NpcInstance mob : _monsters)
				{
					mob.getSpawn().stopRespawn();
					mob.deleteMe();
				}
				_monsters.clear();
				banishesPlayers();
				_playersInLair.clear();

				if(_monsterSpawnTask != null)
				{
					_monsterSpawnTask.cancel(true);
					_monsterSpawnTask = null;
				}
				if(_onPlayersAnnihilatedTask != null)
				{
					_onPlayersAnnihilatedTask.cancel(true);
					_onPlayersAnnihilatedTask = null;
				}
				if(_socialTask != null)
				{
					_socialTask.cancel(true);
					_socialTask = null;
				}
				if(_mobiliseTask != null)
				{
					_mobiliseTask.cancel(true);
					_mobiliseTask = null;
				}
				if(_moveAtRandomTask != null)
				{
					_moveAtRandomTask.cancel(true);
					_moveAtRandomTask = null;
				}
				if(_respawnValakasTask != null)
				{
					_respawnValakasTask.cancel(true);
					_respawnValakasTask = null;
				}
				_state.setState(BossState.State.NOTSPAWN);
				_state.update();

				if(_activityTimeEndTask != null)
				{
					_activityTimeEndTask.cancel(true);
					_activityTimeEndTask = null;
				}
			}
		}
	}

	// clean Valakas's lair.
	public void setUnspawn()
	{
		// eliminate players.
		banishesPlayers();

		// delete monsters.
		for(L2NpcInstance mob : _monsters)
		{
			mob.getSpawn().stopRespawn();
			mob.deleteMe();
		}
		_monsters.clear();

		// delete teleport cube.
		for(L2NpcInstance cube : _teleportCube)
		{
			cube.getSpawn().stopRespawn();
			cube.deleteMe();
		}
		_teleportCube.clear();

		// not executed tasks is canceled.
		if(_cubeSpawnTask != null)
		{
			_cubeSpawnTask.cancel(true);
			_cubeSpawnTask = null;
		}
		if(_monsterSpawnTask != null)
		{
			_monsterSpawnTask.cancel(true);
			_monsterSpawnTask = null;
		}
		if(_intervalEndTask != null)
		{
			_intervalEndTask.cancel(true);
			_intervalEndTask = null;
		}
		if(_activityTimeEndTask != null)
		{
			_activityTimeEndTask.cancel(true);
			_activityTimeEndTask = null;
		}
		if(_onPlayersAnnihilatedTask != null)
		{
			_onPlayersAnnihilatedTask.cancel(true);
			_onPlayersAnnihilatedTask = null;
		}
		if(_socialTask != null)
		{
			_socialTask.cancel(true);
			_socialTask = null;
		}
		if(_mobiliseTask != null)
		{
			_mobiliseTask.cancel(true);
			_mobiliseTask = null;
		}
		if(_moveAtRandomTask != null)
		{
			_moveAtRandomTask.cancel(true);
			_moveAtRandomTask = null;
		}
		if(_respawnValakasTask != null)
		{
			_respawnValakasTask.cancel(true);
			_respawnValakasTask = null;
		}
		setInetrvalEndTask();
	}

	// start interval.
	public void setInetrvalEndTask()
	{
		//init state of Valakas's lair.
		if(!_state.getState().equals(BossState.State.DEAD))
		{
			_state.setRespawnDate(Rnd.get(Config.FWV_FIXINTERVALOFVALAKAS, Config.FWV_FIXINTERVALOFVALAKAS + Config.FWV_RANDOMINTERVALOFVALAKAS));
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
		}
	}

	// setting teleport cube spawn task.
	public void setCubeSpawn()
	{
		setInetrvalEndTask();

		if(_activityTimeEndTask != null)
			_activityTimeEndTask.cancel(true);

		_activityTimeEndTask = null;

		_cubeSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new CubeSpawn(), 21000);
	}

	// do spawn teleport cube.
	private class CubeSpawn implements Runnable
	{
		public CubeSpawn()
		{
		}

		public void run()
		{
			spawnCube();
			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				public void run()
				{
					for(L2NpcInstance cube : _teleportCube)
					{
						cube.getSpawn().stopRespawn();
						cube.deleteMe();
					}
					_teleportCube.clear();
				}
			}, 600000);
		}
	}

	// action is enabled the boss.
	private class SetMobilised implements Runnable
	{
		private L2BossInstance _boss;

		public SetMobilised(L2BossInstance boss)
		{
			_boss = boss;
		}

		public void run()
		{
			_boss.setImobilised(false);

			// When it is possible to act, a social action is canceled.
			if(_socialTask != null)
			{
				_socialTask.cancel(true);
				_socialTask = null;
			}
		}
	}

	// Move at random on after Valakas appears.
	private class MoveAtRandom implements Runnable
	{
		private L2NpcInstance _npc;
		Location _pos;

		public MoveAtRandom(L2NpcInstance npc, Location pos)
		{
			_npc = npc;
			_pos = pos;
		}

		public void run()
		{
			_npc.moveToLocation(_pos, 0, true);
		}
	}

	public void updateLastAttack()
	{
		_lastAttackTime = System.currentTimeMillis();
	}

}
