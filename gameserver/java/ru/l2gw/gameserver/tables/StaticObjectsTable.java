package ru.l2gw.gameserver.tables;

import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.instances.L2StaticObjectInstance;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.util.Location;

import java.io.*;
import java.util.StringTokenizer;

public class StaticObjectsTable
{
	private static Log _log = LogFactory.getLog(StaticObjectsTable.class.getName());

	private static StaticObjectsTable _instance;
	private TIntObjectHashMap<L2StaticObjectInstance> _staticObjects;

	public static StaticObjectsTable getInstance()
	{
		if(_instance == null)
			_instance = new StaticObjectsTable();
		return _instance;
	}

	public StaticObjectsTable()
	{
		_staticObjects = new TIntObjectHashMap<>();
		parseData();
		_log.info("StaticObject: Loaded " + _staticObjects.size() + " StaticObject Templates.");
	}

	private void parseData()
	{
		LineNumberReader lnr = null;
		try
		{
			File doorData = new File("./", "data/staticobjects.csv");
			lnr = new LineNumberReader(new BufferedReader(new FileReader(doorData)));

			String line = null;
			while((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
					continue;

				L2StaticObjectInstance obj = parse(line);
				_staticObjects.put(obj.getStaticObjectId(), obj);
			}
		}
		catch(FileNotFoundException e)
		{
			_log.warn("staticobjects.csv is missing in data folder");
		}
		catch(Exception e)
		{
			_log.warn("error while creating StaticObjects table " + e);
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(lnr != null)
					lnr.close();
			}
			catch(Exception e)
			{}
		}
	}

	public static L2StaticObjectInstance parse(String line)
	{
		StringTokenizer st = new StringTokenizer(line, ";");

		st.nextToken(); // Pass over static object name (not used in server)

		int id = Integer.parseInt(st.nextToken());
		int x = Integer.parseInt(st.nextToken());
		int y = Integer.parseInt(st.nextToken());
		int z = Integer.parseInt(st.nextToken());
		int type = Integer.parseInt(st.nextToken()); // 0 arena board, 1 throne, 2 town map
		String filePath = st.nextToken();
		int mapX = Integer.parseInt(st.nextToken());
		int mapY = Integer.parseInt(st.nextToken());

		StatsSet npcDat = new StatsSet();

		npcDat.set("npcId", id);
		npcDat.set("displayId", 0);
		npcDat.set("level", 0);
		npcDat.set("jClass", "static");
		npcDat.set("baseShldDef", 0);
		npcDat.set("baseShldRate", 0);
		npcDat.set("baseCritRate", 0);
		npcDat.set("name", type == 0 ? "Arena" : "");
		npcDat.set("title", "");
		npcDat.set("collision_radius", 0);
		npcDat.set("collision_height", 0);
		npcDat.set("sex", "male");
		npcDat.set("type", "");
		npcDat.set("ai_type", "npc");
		npcDat.set("baseAtkRange", 0);
		npcDat.set("revardExp", 0);
		npcDat.set("revardSp", 0);
		npcDat.set("basePAtkSpd", 0);
		npcDat.set("baseMAtkSpd", 0);
		npcDat.set("aggroRange", 0);
		npcDat.set("rhand", 0);
		npcDat.set("lhand", 0);
		npcDat.set("armor", 0);
		npcDat.set("baseWalkSpd", 0);
		npcDat.set("baseRunSpd", 0);
		npcDat.set("baseHpReg", 0);
		npcDat.set("baseCpReg", 0);
		npcDat.set("baseMpReg", 0);
		npcDat.set("baseSTR", 0);
		npcDat.set("baseCON", 0);
		npcDat.set("baseDEX", 0);
		npcDat.set("baseINT", 0);
		npcDat.set("baseWIT", 0);
		npcDat.set("baseMEN", 0);
		npcDat.set("baseHpMax", 0);
		npcDat.set("baseCpMax", 0);
		npcDat.set("baseMpMax", 0);
		npcDat.set("basePAtk", 0);
		npcDat.set("basePDef", 0);
		npcDat.set("baseMAtk", 0);
		npcDat.set("baseMDef", 0);
		npcDat.set("factionId", "");
		npcDat.set("factionRange", 0);
		npcDat.set("isDropHerbs", false);

		L2NpcTemplate template = new L2NpcTemplate(npcDat);

		L2StaticObjectInstance obj = new L2StaticObjectInstance(IdFactory.getInstance().getNextId(), template, 0, 0, 0, 0);

		obj.setType(type);
		obj.setStaticObjectId(id);
		obj.setFilePath(filePath);
		obj.setMapX(mapX);
		obj.setMapY(mapY);
		obj.spawnMe(new Location(x, y, z));

		return obj;
	}
}