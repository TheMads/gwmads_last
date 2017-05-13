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
import java.util.concurrent.ScheduledFuture;

public class AntharasManager extends Entity
{
	private final static Log _log = LogFactory.getLog(AntharasManager.class.getName());
	private static AntharasManager _instance = new AntharasManager();

	// location of teleport cube.
	private final int _teleportCubeId = 31859;
	private final int _teleportCubeLocation[][] = {{177615, 114941, -7709, 0}};
	protected List<L2Spawn> _teleportCubeSpawn = new FastList<L2Spawn>();
	protected List<L2NpcInstance> _teleportCube = new FastList<L2NpcInstance>();

	// list of intruders.
	protected List<L2Player> _playersInLair = new FastList<L2Player>();

	// spawn data of monsters.
	protected Map<Integer, L2Spawn> _monsterSpawn = new FastMap<Integer, L2Spawn>();


	// instance of monsters.
	protected List<L2NpcInstance> _monsters = new FastList<L2NpcInstance>();

	// tasks.
	protected ScheduledFuture<?> _cubeSpawnTask = null;
	protected ScheduledFuture<?> _monsterSpawnTask = null;
	protected ScheduledFuture<?> _intervalEndTask = null;
	protected ScheduledFuture<?> _activityTimeEndTask = null;
	protected ScheduledFuture<?> _onPlayersAnnihilatedTask = null;
	protected ScheduledFuture<?> _socialTask = null;
	protected ScheduledFuture<?> _mobiliseTask = null;
	protected ScheduledFuture<?> _behemothSpawnTask = null;
	protected ScheduledFuture<?> _bomberSpawnTask = null;
	protected ScheduledFuture<?> _moveAtRandomTask = null;
	protected ScheduledFuture<?> _movieTask = null;
	private long _lastAttackTime = 0;

	// status in lair.
	protected BossState _state = new BossState(29019);
	protected String _questName;

	// location of banishment
	private final int _banishmentLocation[][] =
			{
					{79959, 151774, -3532},
					{81398, 148055, -3468},
					{82286, 149113, -3468},
					{84264, 147427, -3404}
			};

	public static AntharasManager getInstance()
	{
		if(_instance == null)
			_instance = new AntharasManager();

		return _instance;
	}

	// initialize
	public void init()
	{
		// initialize status in lair.
		_playersInLair.clear();
		_questName = "Antharas";

		// setting spawn data of monsters.
		try
		{
			L2NpcTemplate template1;
			L2Spawn tempSpawn;

			// old Antharas.
			template1 = NpcTable.getTemplate(29019);
			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(181323);
			tempSpawn.setLocy(114850);
			tempSpawn.setLocz(-7623);
			tempSpawn.setHeading(32542);
			tempSpawn.setAmount(1);
			tempSpawn.setRespawnDelay(Config.FWA_ACTIVITYTIMEOFANTHARAS * 2);
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false, null);
			_monsterSpawn.put(29019, tempSpawn);

			// weak Antharas.
			template1 = NpcTable.getTemplate(29066);
			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(181323);
			tempSpawn.setLocy(114850);
			tempSpawn.setLocz(-7623);
			tempSpawn.setHeading(32542);
			tempSpawn.setAmount(1);
			tempSpawn.setRespawnDelay(Config.FWA_ACTIVITYTIMEOFANTHARAS * 2);
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false, null);
			_monsterSpawn.put(29066, tempSpawn);

			// normal Antharas.
			template1 = NpcTable.getTemplate(29067);
			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(181323);
			tempSpawn.setLocy(114850);
			tempSpawn.setLocz(-7623);
			tempSpawn.setHeading(32542);
			tempSpawn.setAmount(1);
			tempSpawn.setRespawnDelay(Config.FWA_ACTIVITYTIMEOFANTHARAS * 2);
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false, null);
			_monsterSpawn.put(29067, tempSpawn);

			// strong Antharas.
			template1 = NpcTable.getTemplate(29068);
			tempSpawn = new L2Spawn(template1);
			tempSpawn.setLocx(181323);
			tempSpawn.setLocy(114850);
			tempSpawn.setLocz(-7623);
			tempSpawn.setHeading(32542);
			tempSpawn.setAmount(1);
			tempSpawn.setRespawnDelay(Config.FWA_ACTIVITYTIMEOFANTHARAS * 2);
			SpawnTable.getInstance().addNewSpawn(tempSpawn, false, null);
			_monsterSpawn.put(29068, tempSpawn);
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

		_log.info("AntharasManager: State of Antharas is " + _state.getState() + ".");

		if(_state.getState() == BossState.State.DEAD)
		{
			deadEndTask();
			Date dt = new Date(_state.getRespawnDate());
			_log.info("AntharasManager: Next spawn date of Antharas is " + dt + ".");
		}
		_log.info("AntharasManager: Init AntharasManager.");
	}

	// return Antaras state.
	public BossState.State getState()
	{
		return _state.getState();
	}

	// return list of intruders.
	public List<L2Player> getPlayersInLair()
	{
		return _playersInLair;
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
				_log.warn("AntharasManager: isPlayersAnnihilated(): " + pc + " x " + pc.getX() + " y " + pc.getY() + " z " + pc.getZ());
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
			if(ch instanceof L2Player) pc = (L2Player) ch;
			else continue;
			if(pc.getQuestState(_questName) != null)
				pc.getQuestState(_questName).exitCurrentQuest(true);

			if(checkIfInZone(pc))
			{
				int driftX = Rnd.get(-80, 80);
				int driftY = Rnd.get(-80, 80);
				int loc = Rnd.get(4);
				pc.teleToLocation(_banishmentLocation[loc][0] + driftX, _banishmentLocation[loc][1] + driftY, _banishmentLocation[loc][2]);
			}
		}
		_playersInLair.clear();
	}

	// do spawn teleport cube.
	public void spawnCube()
	{
		if(_behemothSpawnTask != null)
		{
			_behemothSpawnTask.cancel(true);
			_behemothSpawnTask = null;
		}
		if(_bomberSpawnTask != null)
		{
			_bomberSpawnTask.cancel(true);
			_bomberSpawnTask = null;
		}
		for(L2Spawn spawnDat : _teleportCubeSpawn)
		{
			_teleportCube.add(spawnDat.doSpawn(true));
		}
	}

	// When the party is annihilated, they are banished.
	public void checkAnnihilated()
	{
		if(isPlayersAnnihilated())
			_onPlayersAnnihilatedTask =	ThreadPoolManager.getInstance().scheduleGeneral(new OnPlayersAnnihilatedTask(), 5000);
	}

	// When the party is annihilated, they are banished.
	private class OnPlayersAnnihilatedTask implements Runnable
	{
		public void run()
		{
			// banishes players from lair.
			banishesPlayers();
		}
	}

	// setting Antharas spawn task.
	public void setAntharasSpawnTask()
	{
		// When someone has already invaded the lair, nothing is done.
		if(_playersInLair.size() > 0) return;

		if(_monsterSpawnTask == null)
		{
			long wormTime = Rnd.get(Config.FWA_APPTIMEOFANTHARAS, Config.FWA_APPTIMEOFANTHARAS * 2);
			_log.info("AntharasManager: worm time: " + wormTime / 1000 / 60 + " min.");
			_monsterSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(1, null), wormTime);
		}
	}

	// do spawn Antharas.
	private class AntharasSpawn implements Runnable
	{
		int _distance = 25000000;
		int _taskId = 0;
		L2BossInstance _antharas = null;

		AntharasSpawn(int taskId, L2BossInstance antharas)
		{
			_taskId = taskId;
			_antharas = antharas;
		}

		public void run()
		{
			int npcId;
			L2Spawn antharasSpawn = null;
			SocialAction sa = null;

			switch(_taskId)
			{
				case 1: // spawn.
					// Strength of Antharas is decided by the number of players that
					// invaded the lair.
					//if(Config.FWA_OLDANTHARAS)
					//	npcId = 29019; // old
					//else if(_playersInLair.size() <= Config.FWA_LIMITOFWEAK)
					//	npcId = 29066; // weak
					//else if(_playersInLair.size() >= Config.FWA_LIMITOFNORMAL)
					npcId = 29068; // strong
					//else
					//	npcId = 29067; // normal

					// do spawn.
					antharasSpawn = _monsterSpawn.get(npcId);
					_antharas = (L2BossInstance)antharasSpawn.doSpawn(true);
					_monsters.add(_antharas);
					_antharas.setImobilised(true);

					_state.setRespawnDate(Rnd.get(Config.FWA_FIXINTERVALOFANTHARAS, Config.FWA_FIXINTERVALOFANTHARAS + Config.FWA_RANDOMINTERVALOFANTHARAS) + Config.FWA_ACTIVITYTIMEOFANTHARAS);
					_state.setState(BossState.State.ALIVE);
					_state.update();

					// setting 1st time of minions spawn task.
					if(!Config.FWA_OLDANTHARAS)
					{
						int intervalOfBehemoth;
						int intervalOfBomber;

						// Interval of minions is decided by the number of players
						// that invaded the lair.
						if(_playersInLair.size() <= Config.FWA_LIMITOFWEAK) // weak
						{
							intervalOfBehemoth = Config.FWA_INTERVALOFBEHEMOTHONWEAK;
							intervalOfBomber = Config.FWA_INTERVALOFBOMBERONWEAK;
						}
						else if(_playersInLair.size() >= Config.FWA_LIMITOFNORMAL) // strong
						{
							intervalOfBehemoth = Config.FWA_INTERVALOFBEHEMOTHONSTRONG;
							intervalOfBomber = Config.FWA_INTERVALOFBOMBERONSTRONG;
						}
						else
						// normal
						{
							intervalOfBehemoth = Config.FWA_INTERVALOFBEHEMOTHONNORMAL;
							intervalOfBomber = Config.FWA_INTERVALOFBOMBERONNORMAL;
						}

						// spawn Behemoth.
						_behemothSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new BehemothSpawn(intervalOfBehemoth), 30000);

						// spawn Bomber.
						_bomberSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new BomberSpawn(intervalOfBomber), 30000);
					}

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(2, _antharas), 16);

					break;

				case 2:
					// set camera.
					for(L2Player pc : _playersInLair)
					{
						if(pc.isInRangeSq(_antharas, _distance))
						{
							pc.enterMovieMode();
							pc.specialCamera(_antharas, 700, 13, -19, 0, 10000);
						}
						else
						{
							pc.leaveMovieMode();
						}
					}

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(3, _antharas), 3000);

					break;

				case 3:
					// do social.
					sa = new SocialAction(_antharas.getObjectId(), 1);
					_antharas.broadcastPacket(sa);

					// set camera.
					for(L2Player pc : _playersInLair)
					{
						if(pc.isInRangeSq(_antharas,_distance))
						{
							pc.enterMovieMode();
							pc.specialCamera(_antharas, 700, 13, 0, 6000, 10000);
						}
						else
						{
							pc.leaveMovieMode();
						}
					}

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(4, _antharas), 10000);

					break;

				case 4:
					// set camera.
					for(L2Player pc : _playersInLair)
					{
						if(pc.isInRangeSq(_antharas,_distance))
						{
							pc.enterMovieMode();
							pc.specialCamera(_antharas, 3800, 0, -3, 0, 10000);
						}
						else
						{
							pc.leaveMovieMode();
						}
					}

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(5, _antharas), 200);

					break;

				case 5:
					// do social.
					sa = new SocialAction(_antharas.getObjectId(), 2);
					_antharas.broadcastPacket(sa);

					// set camera.
					for(L2Player pc : _playersInLair)
					{
						if(pc.isInRangeSq(_antharas, _distance))
						{
							pc.enterMovieMode();
							pc.specialCamera(_antharas, 1200, 0, -3, 22000, 11000);
						}
						else
						{
							pc.leaveMovieMode();
						}
					}

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(6, _antharas), 10800);

					break;

				case 6:
					// set camera.
					for(L2Player pc : _playersInLair)
					{
						if(pc.isInRangeSq(_antharas, _distance))
						{
							pc.enterMovieMode();
							pc.specialCamera(_antharas, 1200, 0, -3, 300, 2000);
						}
						else
						{
							pc.leaveMovieMode();
						}
					}

					// set next task.
					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					_socialTask = ThreadPoolManager.getInstance().scheduleGeneral(new AntharasSpawn(7, _antharas), 1900);

					break;

				case 7:
					_antharas.abortCast();
					// reset camera.
					for(L2Player pc : _playersInLair)
					{
						pc.leaveMovieMode();
					}

					_mobiliseTask = ThreadPoolManager.getInstance().scheduleGeneral(new SetMobilised(_antharas), 16);

					// move at random.
					if(Config.FWA_MOVEATRANDOM)
					{
						Location pos = Location.coordsRandomize(new Location(178400,114940,-7700), 500);
						_moveAtRandomTask = ThreadPoolManager.getInstance().scheduleGeneral(new MoveAtRandom(_antharas, pos), 32);
					}

					updateLastAttack();
					// set delete task.
					_activityTimeEndTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new ActivityTimeEnd(), 1200000, 1200000);

					if(_socialTask != null)
					{
						_socialTask.cancel(true);
						_socialTask = null;
					}
					break;
			}
		}
	}

	// do spawn Behemoth.
	private class BehemothSpawn implements Runnable
	{
		private int _interval;

		public BehemothSpawn(int interval)
		{
			_interval = interval;
		}

		public void run()
		{
			L2NpcTemplate template1;
			L2Spawn tempSpawn;

			try
			{
				if(_monsters.size() < 30)
				{
					// set spawn.
					template1 = NpcTable.getTemplate(29069);
					tempSpawn = new L2Spawn(template1);
					// allocates it at random in the lair of Antharas.
					tempSpawn.setLocx(Rnd.get(175000, 179900));
					tempSpawn.setLocy(Rnd.get(112400, 116000));
					tempSpawn.setLocz(-7709);
					tempSpawn.setHeading(0);
					tempSpawn.setAmount(1);
					tempSpawn.setRespawnDelay(Config.FWA_ACTIVITYTIMEOFANTHARAS * 2);
					SpawnTable.getInstance().addNewSpawn(tempSpawn, false, null);

					// do spawn.
					_monsters.add(tempSpawn.doSpawn(true));
				}
			}
			catch(Exception e)
			{
				_log.warn(e.getMessage());
			}

			if(_behemothSpawnTask != null)
			{
				_behemothSpawnTask.cancel(true);
				_behemothSpawnTask = null;
			}

			// repeat.
			_behemothSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(
					new BehemothSpawn(_interval), _interval);

		}
	}

	// do spawn Bomber.
	private class BomberSpawn implements Runnable
	{
		private int _interval;

		public BomberSpawn(int interval)
		{
			_interval = interval;
		}

		public void run()
		{
			int npcId = Rnd.get(29070, 29076);
			L2NpcTemplate template1;
			L2Spawn tempSpawn;
			L2NpcInstance bomber = null;

			try
			{
				if(_monsters.size() < 30)
				{
					// set spawn.
					template1 = NpcTable.getTemplate(npcId);
					tempSpawn = new L2Spawn(template1);
					// allocates it at random in the lair of Antharas.
					tempSpawn.setLocx(Rnd.get(175000, 179900));
					tempSpawn.setLocy(Rnd.get(112400, 116000));
					tempSpawn.setLocz(-7709);
					tempSpawn.setHeading(0);
					tempSpawn.setAmount(1);
					tempSpawn.setRespawnDelay(Config.FWA_ACTIVITYTIMEOFANTHARAS * 2);
					SpawnTable.getInstance().addNewSpawn(tempSpawn, false, null);

					// do spawn.
					bomber = tempSpawn.doSpawn(true);
					_monsters.add(bomber);
				}
			}
			catch(Exception e)
			{
				_log.warn(e.getMessage());
			}

			if(_bomberSpawnTask != null)
			{
				_bomberSpawnTask.cancel(true);
				_bomberSpawnTask = null;
			}

			// repeat.
			_bomberSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(
					new BomberSpawn(_interval), _interval);

		}
	}

	// at end of activitiy time.
	private class ActivityTimeEnd implements Runnable
	{
		public ActivityTimeEnd()
		{
		}

		public void run()
		{
			_log.info("AntharasManager: check activity start isPlayersAnnihilated(): " + isPlayersAnnihilated() + " state: " + _state.getState());
			if((isPlayersAnnihilated() || System.currentTimeMillis() - _lastAttackTime > 1200000) && (_state.getState() == BossState.State.ALIVE || _state.getState() == BossState.State.NOTSPAWN))
			{
				_log.info("AntharasManager: set unspawn state");
				_log.info("AntharasManager: last attack: " + (System.currentTimeMillis() - _lastAttackTime > 1200000) + " players anihilated " + isPlayersAnnihilated());
				// delete monsters.
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
				if(_behemothSpawnTask != null)
				{
					_behemothSpawnTask.cancel(true);
					_behemothSpawnTask = null;
				}
				if(_bomberSpawnTask != null)
				{
					_bomberSpawnTask.cancel(true);
					_bomberSpawnTask = null;
				}
				if(_moveAtRandomTask != null)
				{
					_moveAtRandomTask.cancel(true);
					_moveAtRandomTask = null;
				}
				if(_monsterSpawnTask != null)
				{
					_monsterSpawnTask.cancel(true);
					_monsterSpawnTask = null;
				}
				_state.setState(BossState.State.NOTSPAWN);
				_state.update();

				_activityTimeEndTask.cancel(false);
				_activityTimeEndTask = null;
			}
		}
	}

	public void clearLair()
	{
		// delete monsters.
		for(L2NpcInstance mob : _monsters)
		{
			mob.getSpawn().stopRespawn();
			mob.deleteMe();
		}
		_monsters.clear();

		if(_monsterSpawnTask != null)
		{
			_monsterSpawnTask.cancel(true);
			_monsterSpawnTask = null;
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

		if(_behemothSpawnTask != null)
		{
			_behemothSpawnTask.cancel(true);
			_behemothSpawnTask = null;
		}

		if(_bomberSpawnTask != null)
		{
			_bomberSpawnTask.cancel(true);
			_bomberSpawnTask = null;
		}

		if(_moveAtRandomTask != null)
		{
			_moveAtRandomTask.cancel(true);
			_moveAtRandomTask = null;
		}
	}

	// clean Antharas's lair.
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
		if(_behemothSpawnTask != null)
		{
			_behemothSpawnTask.cancel(true);
			_behemothSpawnTask = null;
		}
		if(_bomberSpawnTask != null)
		{
			_bomberSpawnTask.cancel(true);
			_bomberSpawnTask = null;
		}
		if(_moveAtRandomTask != null)
		{
			_moveAtRandomTask.cancel(true);
			_moveAtRandomTask = null;
		}
		_state.setState(BossState.State.NOTSPAWN);
		_state.update();
	}

	public void deadEndTask()
	{
		//init state of Antharas's lair.
		_log.info("AntharasManager: start Dead task");
		if(!_state.getState().equals(BossState.State.DEAD))
		{
			_state.setRespawnDate(Rnd.get(Config.FWA_FIXINTERVALOFANTHARAS, Config.FWA_FIXINTERVALOFANTHARAS + Config.FWA_RANDOMINTERVALOFANTHARAS));
			_state.setState(BossState.State.DEAD);
			_state.update();
		}

		_intervalEndTask = ThreadPoolManager.getInstance().scheduleGeneral(
				new IntervalEnd(), _state.getInterval());
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
		_log.info("AntharasManager: Antharas killed spawn Cube");
		deadEndTask();
		if(_activityTimeEndTask != null)
			_activityTimeEndTask.cancel(true);

		_activityTimeEndTask = null;

		_cubeSpawnTask = ThreadPoolManager.getInstance().scheduleGeneral(new CubeSpawn(), 14000);
	}

	// do spawn teleport cube.
	private class CubeSpawn implements Runnable
	{
		public CubeSpawn()
		{
		}

		public void run()
		{
			clearLair();
			spawnCube();
			ThreadPoolManager.getInstance().scheduleGeneral(new
					Runnable()
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

	// Move at random on after Antharas appears.
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
			_npc.setRunning();
			_npc.moveToLocation(_pos, 0, true);
		}
	}

	public void updateLastAttack()
	{
		_lastAttackTime = System.currentTimeMillis();
	}

}
