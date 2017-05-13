package ru.l2gw.gameserver.model.entity.SevenSignsFestival;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.clientpackets.Say2C;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Party;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Spawn;
import ru.l2gw.gameserver.model.entity.SevenSigns;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.Say2;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.util.Location;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * @author rage
 * @date 04.06.2009 10:22:08
 */
public class Festival
{
	private static Log _log = LogFactory.getLog("sevensigns");

	private int _id;
	private int _cabal;
	private int _minLevel;
	private int _witchId;
	private int _guideId;
	private int _rewardPoints;
	private long _startTime;
	private Location _startLoc;
	private Map<Integer, List<Location>> _spawnCoords;
	private Map<Integer, StatsSet> _monsters;
	private Map<Integer, Integer> _fees;
	private boolean _isStarted = false;
	private boolean _isIncreased = false;
	private L2Zone _zone;
	private FestivalParty _festivalParty;
	private L2Spawn _witchSpawn;
	private Map<Integer, List<FestivalSpawnGroup>> _spawnGroupSets;
	private Map<Integer, Future<?>> _teleportTasks;
	private static final double MAX_DIFFICULT_AT = 0.75;
	private static final int TELEPORT_BACK_DELAY = 60000;

	public void parseFest(Node f) throws Exception
	{
		_id = Integer.parseInt(f.getAttributes().getNamedItem("id").getNodeValue());
		_cabal = SevenSigns.getCabalNumber(f.getAttributes().getNamedItem("cabal").getNodeValue());

		for(Node n = f.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if("startPoint".equals(n.getNodeName()))
				_startLoc = new Location(Integer.parseInt(n.getAttributes().getNamedItem("x").getNodeValue()), Integer.parseInt(n.getAttributes().getNamedItem("y").getNodeValue()), Integer.parseInt(n.getAttributes().getNamedItem("z").getNodeValue()));
			else if("witch".equalsIgnoreCase(n.getNodeName()))
				_witchId = Integer.parseInt(n.getAttributes().getNamedItem("npcId").getNodeValue());
			else if("guide".equalsIgnoreCase(n.getNodeName()))
				_guideId = Integer.parseInt(n.getAttributes().getNamedItem("npcId").getNodeValue());
			else if("level".equalsIgnoreCase(n.getNodeName()))
				_minLevel = Integer.parseInt(n.getAttributes().getNamedItem("limit").getNodeValue());
			else if("reward".equalsIgnoreCase(n.getNodeName()))
				_rewardPoints = Integer.parseInt(n.getAttributes().getNamedItem("points").getNodeValue());
			else if("fees".equalsIgnoreCase(n.getNodeName()))
			{
				if(_fees == null)
					_fees = new FastMap<Integer, Integer>();

				for(Node fe = n.getFirstChild(); fe != null; fe = fe.getNextSibling())
				{
					if("fee".equalsIgnoreCase(fe.getNodeName()))
						_fees.put(Integer.parseInt(fe.getAttributes().getNamedItem("itemId").getNodeValue()), Integer.parseInt(fe.getAttributes().getNamedItem("itemCount").getNodeValue()));
				}
			}
			else if("monsters".equalsIgnoreCase(n.getNodeName()))
			{
				if(_monsters == null)
					_monsters = new FastMap<Integer, StatsSet>();

				for(Node m = n.getFirstChild(); m != null; m = m.getNextSibling())
				{
					if("mob".equalsIgnoreCase(m.getNodeName()))
					{
						int id = Integer.parseInt(m.getAttributes().getNamedItem("id").getNodeValue());
						int npcId = Integer.parseInt(m.getAttributes().getNamedItem("npcId").getNodeValue());
						int giveItems = Integer.parseInt(m.getAttributes().getNamedItem("giveItems").getNodeValue());
						StatsSet mob = new StatsSet();
						mob.set("npcId", npcId);
						mob.set("items", giveItems);
						mob.set("festivalId", _id);
						_monsters.put(id, mob);
					}
				}
			}
			else if("spawnCoords".equalsIgnoreCase(n.getNodeName()))
			{
				if(_spawnCoords == null)
					_spawnCoords = new FastMap<Integer, List<Location>>();

				for(Node s = n.getFirstChild(); s != null; s = s.getNextSibling())
				{
					if("spawnCoord".equalsIgnoreCase(s.getNodeName()))
					{
						int id = Integer.parseInt(s.getAttributes().getNamedItem("id").getNodeValue());
						List<Location> list = new FastList<Location>();
						for(Node l = s.getFirstChild(); l != null; l = l.getNextSibling())
							if("point".equalsIgnoreCase(l.getNodeName()))
								list.add(new Location(Integer.parseInt(l.getAttributes().getNamedItem("x").getNodeValue()), Integer.parseInt(l.getAttributes().getNamedItem("y").getNodeValue()), Integer.parseInt(l.getAttributes().getNamedItem("z").getNodeValue())));

						if(list.size() > 0)
							_spawnCoords.put(id, list);
						else
							_log.warn("Festival: parseFest no points in spawnCoords: " + id + " festId: " + _id + " cabal: " + _cabal);
					}
				}
			}
		}
	}

	public void createSpawnSets(Map<Integer, List<Integer>> mobGroups, Map<Integer, List<FestivalSpawnGroupTemplate>> spawnSets)
	{
		Map<Integer, List<StatsSet>> _mobGoups = new FastMap<Integer, List<StatsSet>>();

		for(Integer groupId : mobGroups.keySet())
		{
			List<StatsSet> mobs = new FastList<StatsSet>();
			for(Integer mobId : mobGroups.get(groupId))
			{
				if(_monsters.containsKey(mobId))
					mobs.add(_monsters.get(mobId));
				else
					_log.warn(this + "has no mobId: " + mobId + " in group id: " + groupId);
			}
			_mobGoups.put(groupId, mobs);
		}

		_spawnGroupSets = new FastMap<Integer, List<FestivalSpawnGroup>>();
		
		for(Integer setId : spawnSets.keySet())
		{
			List<FestivalSpawnGroup> spawnGroups = new FastList<FestivalSpawnGroup>();
			for(FestivalSpawnGroupTemplate fsgt : spawnSets.get(setId))
			{
				if(!_mobGoups.containsKey(fsgt.groupId))
					_log.warn(this + " has no mob group id: "+fsgt.groupId);
				else if(!_spawnCoords.containsKey(fsgt.coordSpawnId))
					_log.warn(this + "has no spawn coords for id: "+fsgt.coordSpawnId);
				else
				{
					FestivalSpawnGroup fsg = new FestivalSpawnGroup(this, fsgt, _mobGoups.get(fsgt.groupId), _spawnCoords.get(fsgt.coordSpawnId));
					spawnGroups.add(fsg);
				}
			}
			_spawnGroupSets.put(setId, spawnGroups);
		}
	}

	public int getId()
	{
		return _id;
	}

	public int getCabal()
	{
		return _cabal;
	}

	public int getMinLevel()
	{
		return _minLevel;
	}

	public int getWitchId()
	{
		return _witchId;
	}

	public int getGuideId()
	{
		return _guideId;
	}

	public int getRewardPoints()
	{
		return _rewardPoints;
	}

	public Location getStartLoc()
	{
		return _startLoc;
	}

	public int getCostByStoneId(int stoneId)
	{
		return _fees.get(stoneId);
	}

	public boolean isStarted()
	{
		return _isStarted;
	}

	public boolean isIncreased()
	{
		return _isIncreased;
	}

	public void setIncreased(boolean val)
	{
		_isIncreased = val;
	}

	public void initFest(L2Party party)
	{
		if(_witchSpawn == null)
		{
			try
			{
				L2NpcTemplate tpl = NpcTable.getTemplate(_witchId);
				_witchSpawn = new L2Spawn(tpl);
				_witchSpawn.setRespawnDelay(60);
				_witchSpawn.setLoc(_startLoc);
				_witchSpawn.doSpawn(true);
			}
			catch(ClassNotFoundException e)
			{
				_log.info(this + "no witch template found: " + _witchId);
			}
		}
		else
			_witchSpawn.doSpawn(true);

		_festivalParty = new FestivalParty(party);
		_festivalParty.setFestivalLevel(_minLevel);
		_log.info(this + "Initialize festival for party: " + _festivalParty);
		_isStarted = true;
	}

	public void startFest()
	{
		List<FestivalSpawnGroup> list = _spawnGroupSets.get(1);
		_startTime = System.currentTimeMillis();
		_log.info(this + "Starting festival for party: " + _festivalParty);

		for(FestivalSpawnGroup fsg : list)
			fsg.startRespawn();

		if(_isIncreased)
			increaseSpawn();
	}

	public void increaseSpawn()
	{
		_log.info(this + "Increase mob spawn, party: " + _festivalParty);
		List<FestivalSpawnGroup> list = _spawnGroupSets.get(2);
		for(FestivalSpawnGroup fsg : list)
			fsg.startRespawn();
	}

	public void stopFest()
	{
		if(_witchSpawn != null)
			_witchSpawn.despawnAll();

		if(_festivalParty != null)
		{
			boolean aborted = true;
			if(_zone != null)
				for(L2Character cha : _zone.getCharacters())
					if(cha != null && cha.isPlayer())
					{
						if(!cha.isDead())
							aborted = false;
						cha.teleToClosestTown();
					}

			_festivalParty.setAborted(aborted);
			SevenSignsFestival.getInstance().addContributeParty(_festivalParty);
		}

		_log.info(this + "Festival ended, party: " + _festivalParty);

		if(_teleportTasks != null)
			for(Future<?> task : _teleportTasks.values())
				if(task != null)
					task.cancel(true);

		_teleportTasks = null;

		List<FestivalSpawnGroup> list = _spawnGroupSets.get(1);

		for(FestivalSpawnGroup fsg : list)
			fsg.stopRespawn();

		if(_isIncreased)
			for(FestivalSpawnGroup fsg : _spawnGroupSets.get(2))
				fsg.stopRespawn();

		_festivalParty = null;
		_isStarted = false;
		_isIncreased = false;
	}

	public void teleportBack(L2Player player)
	{
		if(_startTime + FestivalManager.getInstance().getTotalBattleTime() - System.currentTimeMillis() > TELEPORT_BACK_DELAY)
		{
			if(_teleportTasks == null)
				_teleportTasks = new FastMap<Integer, Future<?>>();

			if(player.getParty() != null && player.getParty().isLeader(player))
			{
				_festivalParty.setAborted(true);
				SevenSignsFestival.getInstance().addContributeParty(_festivalParty);

				for(L2Player member : player.getParty().getPartyMembers())
					if(member != null && !_teleportTasks.containsKey(member.getObjectId()) && _zone.isCharacterInZone(member))
						_teleportTasks.put(member.getObjectId(), ThreadPoolManager.getInstance().scheduleGeneral(new TeleportBackTask(member), TELEPORT_BACK_DELAY));
				_log.info(this + "Festival aborted by party request: " + _festivalParty);
				_festivalParty = null;
			}
			else if(!_teleportTasks.containsKey(player.getObjectId()))
				_teleportTasks.put(player.getObjectId(), ThreadPoolManager.getInstance().scheduleGeneral(new TeleportBackTask(player), TELEPORT_BACK_DELAY));
		}
	}

	public double getFestivalProgress()
	{
		return (double)(System.currentTimeMillis() - _startTime) / (FestivalManager.getInstance().getBattleTime() * MAX_DIFFICULT_AT);
	}

	public void addZone(L2Zone zone)
	{
		_zone = zone;
	}

	private class TeleportBackTask implements Runnable
	{
		private L2Player _player;

		public TeleportBackTask(L2Player player)
		{
			_player = player;
		}

		public void run()
		{
			if(!Festival.this.isStarted() || _player == null || !_zone.isCharacterInZone(_player))
				return;

			_player.teleToClosestTown();
		}
	}

	public void witchSay(String message)
	{
		if(_witchSpawn != null && _witchSpawn.getLastSpawn() != null)
			_witchSpawn.getLastSpawn().broadcastPacket(new Say2(_witchSpawn.getLastSpawn().getObjectId(), Say2C.SHOUT, _witchSpawn.getLastSpawn().getName(), message));
	}

	@Override
	public String toString()
	{
		return "Festival[id: " + _id + "; cabal: " + SevenSigns.getCabalName(_cabal) + "]: ";
	}
}
