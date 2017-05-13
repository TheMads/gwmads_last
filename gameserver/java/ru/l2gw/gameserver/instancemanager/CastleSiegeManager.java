package ru.l2gw.gameserver.instancemanager;

import javolution.util.FastList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.entity.Castle;
import ru.l2gw.gameserver.model.entity.siege.SiegeSpawn;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.instances.L2ArtefactInstance;
import ru.l2gw.gameserver.model.instances.L2ControlTowerInstance;
import ru.l2gw.gameserver.tables.NpcTable;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public final class CastleSiegeManager extends SiegeManager
{
	private static int _crpWin;
	private static int _crpLoos;
	private static CastleSiegeManager _instance;

	public static CastleSiegeManager getInstance()
	{
		if(_instance == null)
			_instance = new CastleSiegeManager();

		return _instance;
	}

	public CastleSiegeManager()
	{
		load();
	}

	public static void reload()
	{
		load();
	}

	public static void load()
	{
		_log.info("CastleSiegeManager: loading castle data...");
		try
		{
			// Siege Time Change
			Config.SIEGE_HOUR_LIST = new FastList<Integer>();
			Config.SIEGE_HOUR_LIST.add(16);
			Config.SIEGE_HOUR_LIST.add(20);

			int siegeLength = 120;
			int siegeClanMinLevel = 4;
			int defenderRespawnDelay = 30000;

			File file = new File(Config.CASTLE_DATA_FILE);

			if (!file.exists()) {
				if (Config.DEBUG)
					_log.info("The " + Config.CASTLE_DATA_FILE + " file is missing.");
				return;
			}

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			Document doc = factory.newDocumentBuilder().parse(file);
			int famePoints = 150;

			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				try
				{
					if("castles".equalsIgnoreCase(n.getNodeName()))
					{
						for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if("common".equalsIgnoreCase(d.getNodeName()))
							{
								for(Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
								{
									if("SiegeLength".equalsIgnoreCase(c.getNodeName()))
										siegeLength = Integer.parseInt(c.getAttributes().getNamedItem("time").getNodeValue());
									else if("SiegeClanMinLevel".equalsIgnoreCase(c.getNodeName()))
										siegeClanMinLevel = Integer.parseInt(c.getAttributes().getNamedItem("val").getNodeValue());
									else if("DefenderRespawn".equalsIgnoreCase(c.getNodeName()))
										defenderRespawnDelay = Integer.parseInt(c.getAttributes().getNamedItem("time").getNodeValue());
									else if("DawnGatesPdefMult".equalsIgnoreCase(c.getNodeName()))
										Config.SIEGE_DAWN_GATES_MDEF_MULT = Double.parseDouble(c.getAttributes().getNamedItem("val").getNodeValue());
									else if("DuskGatesPdefMult".equalsIgnoreCase(c.getNodeName()))
										Config.SIEGE_DUSK_GATES_PDEF_MULT = Double.parseDouble(c.getAttributes().getNamedItem("val").getNodeValue());
									else if("DawnGatesMdefMult".equalsIgnoreCase(c.getNodeName()))
										Config.SIEGE_DAWN_GATES_MDEF_MULT = Double.parseDouble(c.getAttributes().getNamedItem("val").getNodeValue());
									else if("DuskGatesMdefMult".equalsIgnoreCase(c.getNodeName()))
										Config.SIEGE_DUSK_GATES_MDEF_MULT = Double.parseDouble(c.getAttributes().getNamedItem("val").getNodeValue());
									else if("CRPWin".equalsIgnoreCase(c.getNodeName()))
										_crpWin = Integer.parseInt(c.getAttributes().getNamedItem("val").getNodeValue());
									else if("CRPLose".equalsIgnoreCase(c.getNodeName()))
										_crpLoos = Integer.parseInt(c.getAttributes().getNamedItem("val").getNodeValue());
									else if("FamePoints".equalsIgnoreCase(c.getNodeName()))
										famePoints = Integer.parseInt(c.getAttributes().getNamedItem("val").getNodeValue());
								}
							}
							else if("castle".equalsIgnoreCase(d.getNodeName()))
							{
								int castleId = (d != null) ? Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue()) : 0;
								if(castleId != 0)
								{
									SiegeUnit castle = ResidenceManager.getInstance().getBuildingById(castleId);
									if(castle == null)
										_log.warn("cannot find castle whith id:" + castleId);
									else
									{
										castle.parseCastle(d);
										castle.loadReinforces();
									}
								}
							}
						}
					}
				}
				catch(Exception e)
				{
					_log.warn("CastleSiegeManager: can't load castle data" + e);
					e.printStackTrace();
				}
			}

			for(Castle castle : ResidenceManager.getInstance().getCastleList())
			{
				castle.getSiege().setFamePoints(famePoints);
				castle.getSiege().setDefenderRespawnDelay(defenderRespawnDelay);
				castle.getSiege().setSiegeClanMinLevel(siegeClanMinLevel);
				castle.getSiege().setSiegeLength(siegeLength);

				castle.getSiege().startAutoTask();
				spawnArtifacts(castle);
				spawnControlTowers(castle);

				castle.getSiege().getZone().setActive(false);
			}
		}
		catch(Exception e)
		{
			System.err.println("Error while loading siege data.");
			e.printStackTrace();
		}
	}

	public static void spawnArtifacts(SiegeUnit castle)
	{
		for(SiegeSpawn _sp : castle.getArtifcatSpawns())
		{
			L2ArtefactInstance art = new L2ArtefactInstance(IdFactory.getInstance().getNextId(), NpcTable.getTemplate(_sp.getNpcId()), 0, 0, 0, 0);
			art.setCurrentHpMp(art.getMaxHp(), art.getMaxMp());
			art.setHeading(_sp.getLoc().getHeading());
			art.spawnMe(_sp.getLoc().changeZ(50));
			castle.getSiege().addArtifact(art);
		}
	}

	public static int getCRPWin()
	{
		return _crpWin;
	}

	public static int getCRPLoos()
	{
		return _crpLoos;
	}

	/**
	 * Spawn control tower.
	 * @param Сastle
	 */
	public static void spawnControlTowers(SiegeUnit castle)
	{
		for(SiegeSpawn _sp : castle.getControlTowerSpawns())
		{
			L2ControlTowerInstance tower = new L2ControlTowerInstance(IdFactory.getInstance().getNextId(), NpcTable.getTemplate(_sp.getNpcId()), castle.getSiege());
			tower.setMaxHp(_sp.getHp());
			tower.setCurrentHpMp(tower.getMaxHp(), tower.getMaxMp());
			tower.setHeading(_sp.getLoc().getHeading());
			tower.setControlTrapId(_sp.getControlId());
			tower.spawnMe(_sp.getLoc());
			castle.getSiege().addControlTower(tower);
		}
	}
}