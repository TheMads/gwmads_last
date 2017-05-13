package ru.l2gw.gameserver.model.entity.SevenSignsFestival;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.clientpackets.Say2C;
import ru.l2gw.gameserver.model.L2Spawn;
import ru.l2gw.gameserver.model.SpawnListener;
import ru.l2gw.gameserver.model.entity.SevenSigns;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.Say2;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * @author rage
 */
public class FestivalManager implements SpawnListener
{
	private static Log _log = LogFactory.getLog("sevensigns");

	private static final String FESTIVAL_CONFIG_FILE = "./data/festival.xml";
	private Map<Integer, Festival> _festivals;
	private static FestivalManager _instance;
	private long _festivalStartTime;
	private Future<?> _competiotionTask;
	private static L2NpcInstance _dawnChatGuide;
	private static L2NpcInstance _duskChatGuide;

	private int _minPartyMemebers;
	private int _rewardCRP;
	private int _periodInit;
	private int _periodBattle;
	private Map<Integer, List<Integer>> _mobGroups;
	private Map<Integer, List<FestivalSpawnGroupTemplate>> _spawnSets;
	private boolean _isRegistrationOpen = false;
	private Map<Integer, Integer> _festivalRewardPoints;

	public static FestivalManager getInstance()
	{
		if(_instance == null)
			_instance = new FestivalManager();
		return _instance;
	}

	private FestivalManager()
	{
		_log.info("FestivalManager: initializing...");
		_festivals = new FastMap<Integer, Festival>();
		_festivalRewardPoints = new FastMap<Integer, Integer>();

		L2Spawn.addSpawnListener(this);

		try
		{
			File file = new File(FESTIVAL_CONFIG_FILE);

			if(!file.exists())
			{
				_log.info("The " + FESTIVAL_CONFIG_FILE + " file is missing.");
				return;
			}

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			Document doc = factory.newDocumentBuilder().parse(file);

			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if("festivals".equalsIgnoreCase(n.getNodeName()))
				{
					for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if("common".equalsIgnoreCase(d.getNodeName()))
							parseCommon(d);
						else if("festival".equalsIgnoreCase(d.getNodeName()))
						{
							Festival fest = new Festival();
							fest.parseFest(d);
							_festivals.put(fest.getId(), fest);
							_festivalRewardPoints.put(fest.getMinLevel(), fest.getRewardPoints());
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			_log.warn("FestivalManager: can't load festival data: " + e);
			e.printStackTrace();
		}

		_log.info("FestivalManager: loaded " + _festivals.size() + " festivals.");
	}

	public void parseCommon(Node c) throws Exception
	{
		for(Node d = c.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if("party".equalsIgnoreCase(d.getNodeName()))
				_minPartyMemebers = Integer.parseInt(d.getAttributes().getNamedItem("minMembers").getNodeValue());
			else if("reward".equalsIgnoreCase(d.getNodeName()))
				_rewardCRP = Integer.parseInt(d.getAttributes().getNamedItem("CRP").getNodeValue());
			else if("periods".equalsIgnoreCase(d.getNodeName()))
			{
				for(Node p = d.getFirstChild(); p != null; p = p.getNextSibling())
				{
					if("period".equalsIgnoreCase(p.getNodeName()))
					{
						if(p.getAttributes().getNamedItem("type").getNodeValue().equalsIgnoreCase("initial"))
						{
							_periodInit = Integer.parseInt(p.getAttributes().getNamedItem("length").getNodeValue());
							_log.info("FestivalManager: init period: " + _periodInit + " sec.");
							_periodInit *= 1000;
						}
						else if(p.getAttributes().getNamedItem("type").getNodeValue().equalsIgnoreCase("battle"))
						{
							_periodBattle = Integer.parseInt(p.getAttributes().getNamedItem("length").getNodeValue());
							_log.info("FestivalManager: battle period: " + _periodBattle + " sec.");
							_periodBattle *= 1000;
						}
					}
				}
			}
			else if("groups".equalsIgnoreCase(d.getNodeName()))
			{
				if(_mobGroups == null)
					_mobGroups = new FastMap<Integer, List<Integer>>();

				for(Node g = d.getFirstChild(); g != null; g = g.getNextSibling())
				{
					if("group".equalsIgnoreCase(g.getNodeName()))
					{
						int id = Integer.parseInt(g.getAttributes().getNamedItem("id").getNodeValue());
						List<Integer> mobs = new FastList<Integer>();
						for(Node m = g.getFirstChild(); m != null; m = m.getNextSibling())
						{
							if("mob".equalsIgnoreCase(m.getNodeName()))
								mobs.add(Integer.parseInt(m.getAttributes().getNamedItem("id").getNodeValue()));
						}
						if(mobs.size() > 0)
							_mobGroups.put(id, mobs);
						else
							_log.warn("Festival: load common no mobs in group: " + id);
					}
				}
			}
			else if("spawnSets".equalsIgnoreCase(d.getNodeName()))
			{
				if(_spawnSets == null)
					_spawnSets = new FastMap<Integer, List<FestivalSpawnGroupTemplate>>();

				for(Node s = d.getFirstChild(); s != null; s = s.getNextSibling())
				{
					if("spawnSet".equalsIgnoreCase(s.getNodeName()))
					{
						int id = Integer.parseInt(s.getAttributes().getNamedItem("id").getNodeValue());
						List<FestivalSpawnGroupTemplate> list = new FastList<FestivalSpawnGroupTemplate>();

						for(Node f = s.getFirstChild(); f != null; f = f.getNextSibling())
						{
							if("spawn".equalsIgnoreCase(f.getNodeName()))
								list.add(new FestivalSpawnGroupTemplate(f));
						}

						if(list.size() > 0)
							_spawnSets.put(id, list);
						else
							_log.warn("Festival: load common no spawns in spawnset: " + id);
					}
				}
			}
		}
	}

	public void startFestival()
	{
		if(SevenSigns.getInstance().isCompetitionPeriod())
		{
			Calendar c = Calendar.getInstance();
			c.add(Calendar.MINUTE, getTotalBattleTime() / 60000 - c.get(Calendar.MINUTE) % (getTotalBattleTime() / 60000));
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			_festivalStartTime = c.getTimeInMillis();

			_log.info("FestivalManager: festival start in " + new Date(_festivalStartTime));

			if(_competiotionTask != null)
			{
				for(Festival fest : _festivals.values())
					if(fest.isStarted())
						fest.stopFest();

				_competiotionTask.cancel(true);
			}

			_competiotionTask = ThreadPoolManager.getInstance().scheduleGeneral(new CompetitionTask(), _festivalStartTime - System.currentTimeMillis());
		}
		else
			_log.info("FestivalManager: competition period is not running.");
	}

	public void stopFestival()
	{
		if(_competiotionTask != null)
		{
			for(Festival fest : _festivals.values())
				if(fest.isStarted())
					fest.stopFest();

			_competiotionTask.cancel(true);
			_competiotionTask = null;
		}
	}

	public int getMinPartyMemebers()
	{
		return _minPartyMemebers;
	}

	public int getRewardCRP()
	{
		return _rewardCRP;
	}

	public int getTotalBattleTime()
	{
		return _periodBattle + _periodInit;
	}

	public int getBattleTime()
	{
		return _periodBattle;
	}

	public Festival getFestByNpcId(int npcId)
	{
		for(Festival fest : _festivals.values())
			if(fest.getGuideId() == npcId || fest.getWitchId() == npcId)
				return fest;

		return null;
	}

	public Festival getFestivalById(int festivalId)
	{
		return _festivals.get(festivalId);
	}

	public Map<Integer, Festival> getFestivals()
	{
		return _festivals;
	}

	public List<Integer> getFestivalLevels()
	{
		List<Integer> list = new FastList<Integer>();
		for(Festival fest : _festivals.values())
		{
			if(!list.contains(fest.getMinLevel()))
				list.add(fest.getMinLevel());
		}
		return list;
	}

	public int getFestivalIdByCabalLevel(int cabal, int level)
	{
		for(Festival fest : _festivals.values())
		{
			if(fest.getCabal() == cabal && fest.getMinLevel() == level)
				return fest.getId();
		}

		return 0;
	}

	public void addZone(L2Zone zone)
	{
		Festival fest = _festivals.get(zone.getEntityId());
		if(fest != null)
			fest.addZone(zone);
		else
			_log.warn("FestivalManager: no festrival with id: " + zone.getEntityId() + " for zone: " + zone);
	}

	public boolean isRegistrationOpen()
	{
		return _isRegistrationOpen;
	}

	public String getMinToStart()
	{
		if(SevenSigns.getInstance().isCompetitionPeriod())
			return "The next festival will begin in " + ((_festivalStartTime - System.currentTimeMillis()) / 60000) + " minute(s).";

		return "This is the Seal Validation period. Festivals will resume next week.";
	}

	public int getFestivalRewardPoints(int level)
	{
		return _festivalRewardPoints.get(level);
	}

	public void npcSpawned(L2NpcInstance npc)
	{
		if(npc == null)
			return;
		// If the spawned NPC ID matches the ones we need, assign their instances.
		if(npc.getNpcId() == 31127)
			_dawnChatGuide = npc;
		else if(npc.getNpcId() == 31137)
			_duskChatGuide = npc;
	}

	public void createSpawnSets()
	{
		for(Festival fest : _festivals.values())
			fest.createSpawnSets(_mobGroups, _spawnSets);
	}

	public void stopFestivals() throws NullPointerException
	{
		for(Festival fest : _festivals.values())
			if(fest.isStarted())
				fest.stopFest();
	}

	private class CompetitionTask implements Runnable
	{
		public synchronized void run()
		{
			_log.info("FestivalManager: CompetitionTask start");

			try
			{
				stopFestivals();
			}
			catch(NullPointerException e)
			{
				_log.warn("FestivalManager: WARNING exception in main festival thread! "+e);
				e.printStackTrace();
			}

			_festivalStartTime = (System.currentTimeMillis() + getTotalBattleTime()) / 1000 * 1000;

			if(!SevenSigns.getInstance().isCompetitionPeriod())
			{
				_log.info("FestivalManager: Seal validation period started or festival end in seal validation (" + new Date(_festivalStartTime) + "). Shutdown Festival task.");
				_competiotionTask = null;
				return;
			}

			if(_dawnChatGuide != null)
				_dawnChatGuide.broadcastPacket(new Say2(_dawnChatGuide.getObjectId(), Say2C.SHOUT, _dawnChatGuide.getName(), new CustomMessage("FestivalStart", Config.DEFAULT_LANG).toString()));
			if(_duskChatGuide != null)
				_duskChatGuide.broadcastPacket(new Say2(_duskChatGuide.getObjectId(), Say2C.SHOUT, _duskChatGuide.getName(), new CustomMessage("FestivalStart", Config.DEFAULT_LANG).toString()));

			_log.info("FestivalManager: next festival start in " + new Date(_festivalStartTime));

			_isRegistrationOpen = true;

			try
			{
				wait(_periodInit);
			}
			catch(InterruptedException e)
			{
				if(!SevenSigns.getInstance().isCompetitionPeriod())
				{
					stopFestivals();
					return;
				}
				_log.warn("FestivalManager: initial wait exception: " + e);
			}

			_isRegistrationOpen = false;

			try
			{
				for(Festival fest : _festivals.values())
					if(fest.isStarted())
						fest.startFest();
			}
			catch(NullPointerException e)
			{
				if(!SevenSigns.getInstance().isCompetitionPeriod())
					return;

				_log.warn("FestivalManager: WARNING exception in main festival thread! " + e);
			}

			if(_dawnChatGuide != null)
				_dawnChatGuide.broadcastPacket(new Say2(_dawnChatGuide.getObjectId(), Say2C.SHOUT, _dawnChatGuide.getName(), new CustomMessage("FestivalStarted", Config.DEFAULT_LANG).toString()));
			if(_duskChatGuide != null)
				_duskChatGuide.broadcastPacket(new Say2(_duskChatGuide.getObjectId(), Say2C.SHOUT, _duskChatGuide.getName(), new CustomMessage("FestivalStarted", Config.DEFAULT_LANG).toString()));

			if(_periodBattle > 5 * 60000)
			{
				try
				{
					wait(_periodBattle - 5 * 60000);
				}
				catch(InterruptedException e)
				{
					if(!SevenSigns.getInstance().isCompetitionPeriod())
					{
						stopFestivals();
						return;
					}
				}

				if(_dawnChatGuide != null)
					_dawnChatGuide.broadcastPacket(new Say2(_dawnChatGuide.getObjectId(), Say2C.SHOUT, _dawnChatGuide.getName(), new CustomMessage("Festival5minLeft", Config.DEFAULT_LANG).toString()));
				if(_duskChatGuide != null)
					_duskChatGuide.broadcastPacket(new Say2(_duskChatGuide.getObjectId(), Say2C.SHOUT, _duskChatGuide.getName(), new CustomMessage("Festival5minLeft", Config.DEFAULT_LANG).toString()));
			}

			if(_festivalStartTime - System.currentTimeMillis() > 2 * 60000)
			{
				try
				{
					wait(_festivalStartTime - System.currentTimeMillis() - 2 * 60000);
				}
				catch(Exception e)
				{
					if(!SevenSigns.getInstance().isCompetitionPeriod())
					{
						stopFestivals();
						return;
					}
				}

				if(_dawnChatGuide != null)
					_dawnChatGuide.broadcastPacket(new Say2(_dawnChatGuide.getObjectId(), Say2C.SHOUT, _dawnChatGuide.getName(), new CustomMessage("Festival2minLeft", Config.DEFAULT_LANG).toString()));
				if(_duskChatGuide != null)
					_duskChatGuide.broadcastPacket(new Say2(_duskChatGuide.getObjectId(), Say2C.SHOUT, _duskChatGuide.getName(), new CustomMessage("Festival2minLeft", Config.DEFAULT_LANG).toString()));

				for(Festival fest : _festivals.values())
					if(fest.isStarted())
						fest.witchSay(new CustomMessage("FestivalWitch2min", Config.DEFAULT_LANG).toString());
			}

			_competiotionTask = ThreadPoolManager.getInstance().scheduleGeneral(this, _festivalStartTime - System.currentTimeMillis());
		}
	}
}
