package ru.l2gw.gameserver.tables;

import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.database.mysql;
import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.gameserver.instancemanager.DayNightSpawnManager;
import ru.l2gw.gameserver.instancemanager.RaidBossSpawnManager;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.entity.instance.Instance;
import ru.l2gw.gameserver.model.npcmaker.DefaultMaker;
import ru.l2gw.gameserver.model.npcmaker.RespawnData;
import ru.l2gw.gameserver.model.npcmaker.SpawnDefine;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.util.Location;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"nls", "unqualified-field-access", "boxing"})
public class SpawnTable
{
	private static final Log _log = LogFactory.getLog(SpawnTable.class.getName());

	private static SpawnTable _instance;

	private FastMap<Integer, L2Spawn> _spawntable;
	private FastMap<Integer, GArray<L2Spawn>> _spawnsByNpcId;
	private FastMap<String, GArray<L2Spawn>> _eventSpawns;
	private FastMap<String, DefaultMaker> _npcMakers;
	private FastMap<String, RespawnData> _respawnData;
	private int _npcSpawnCount;
	private int _spawnCount;

	private int _highestId;

	public static SpawnTable getInstance()
	{
		if(_instance == null)
			new SpawnTable();
		return _instance;
	}

	private SpawnTable()
	{
		_instance = this;
		NpcTable.getInstance().applyServerSideTitle();
		if(!Config.DONTLOADSPAWN)
			fillSpawnTable(true);
		else
		{
			_log.info("Spawn Correctly Disabled");
			Scripts.getInstance().callOnLoad();
		}
	}

	public FastMap<Integer, L2Spawn> getSpawnTable()
	{
		return _spawntable;
	}

	private void fillSpawnTable(boolean scripts)
	{
		_spawntable = new FastMap<Integer, L2Spawn>().shared();
		_spawnsByNpcId = new FastMap<Integer, GArray<L2Spawn>>().shared();
		_eventSpawns = new FastMap<String, GArray<L2Spawn>>().shared();
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT s.spawn_id, count, s.npc_templateid, locx, locy, locz, heading, randomx, randomy, ai, ai_parameters, respawn_delay, respawn_random, loc_id, ks.respawn_time, periodOfDay, event_name, spawn_time FROM `spawnlist` s LEFT JOIN `kill_status` AS ks ON ( s.spawn_id = ks.spawn_id ) ORDER BY npc_templateid");
			rset = statement.executeQuery();

			L2Spawn spawnDat;
			L2NpcTemplate template1;
			_npcSpawnCount = 0;
			_spawnCount = 0;

			while(rset.next())
			{
				template1 = NpcTable.getTemplate(rset.getInt("npc_templateid"));
				if(template1 != null)
				{
					if(rset.getString("event_name") != null)
					{
						spawnDat = new L2Spawn(template1);
						spawnDat.setId(rset.getInt("spawn_id"));
						spawnDat.setAmount(rset.getInt("count"));
						spawnDat.setLocx(rset.getInt("locx"));
						spawnDat.setLocy(rset.getInt("locy"));
						spawnDat.setLocz(rset.getInt("locz"));
						spawnDat.setHeading(rset.getInt("heading"));
						spawnDat.setRandomX(rset.getInt("randomx"));
						spawnDat.setRandomY(rset.getInt("randomy"));
						spawnDat.setAIType(rset.getString("ai"));
						spawnDat.setAIParameters(rset.getString("ai_parameters"));
						spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
						spawnDat.setRespawnRandom(rset.getInt("respawn_random"));
						spawnDat.setEventName(rset.getString("event_name"));
						if(rset.getString("respawn_time") == null)
							spawnDat.setRespawnTime(0);
						else
							spawnDat.setRespawnTime(rset.getInt("respawn_time"));
						spawnDat.setLocation(rset.getInt("loc_id"));
						GArray<L2Spawn> spawns = _eventSpawns.get(rset.getString("event_name"));
						if(spawns == null)
						{
							spawns = new GArray<L2Spawn>();
							_eventSpawns.put(rset.getString("event_name"), spawns);
						}
						spawns.add(spawnDat);
						if(rset.getBoolean("spawn_time"))
							_npcSpawnCount += spawnDat.init();
					}
					else if(template1.type.equalsIgnoreCase("L2SiegeGuard") || template1.type.equalsIgnoreCase("L2RaidBoss") || template1.type.equalsIgnoreCase("L2Boss") || template1.type.equalsIgnoreCase("QueenAnt"))
					{
						// Don't spawn
					}
					else if(Config.ALLOW_CLASS_MASTERS_LIST.isEmpty() && template1.type.equals("L2ClassMaster"))
					{
						// Dont' spawn class masters
					}
					else
					{
						spawnDat = new L2Spawn(template1);
						spawnDat.setId(rset.getInt("spawn_id"));
						spawnDat.setAmount(rset.getInt("count") * (Config.ALT_DOUBLE_SPAWN ? 2 : 1));
						spawnDat.setLocx(rset.getInt("locx"));
						spawnDat.setLocy(rset.getInt("locy"));
						spawnDat.setLocz(rset.getInt("locz"));
						spawnDat.setHeading(rset.getInt("heading"));
						spawnDat.setAIType(rset.getString("ai"));
						spawnDat.setAIParameters(rset.getString("ai_parameters"));
						spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));
						spawnDat.setRespawnRandom(rset.getInt("respawn_random"));
						if(rset.getString("respawn_time") == null)
							spawnDat.setRespawnTime(0);
						else
							spawnDat.setRespawnTime(rset.getInt("respawn_time"));
						spawnDat.setLocation(rset.getInt("loc_id"));

						switch(rset.getInt("periodOfDay"))
						{
							case 0: // default
								_npcSpawnCount += spawnDat.init();
								_spawntable.put(spawnDat.getId(), spawnDat);
								GArray<L2Spawn> s = _spawnsByNpcId.get(spawnDat.getNpcId());
								if(s == null)
								{
									s = new GArray<L2Spawn>();
									_spawnsByNpcId.put(spawnDat.getNpcId(), s);
								}
								s.add(spawnDat);
								break;
							case 1: // Day
								DayNightSpawnManager.getInstance().addDayMob(spawnDat);
								break;
							case 2: // Night
								DayNightSpawnManager.getInstance().addNightMob(spawnDat);
								break;
						}
						_spawnCount++;

						if(_npcSpawnCount % 1000 == 0)
							_log.info("Spawned " + _npcSpawnCount + " npc");

						if(spawnDat.getId() > _highestId)
							_highestId = spawnDat.getId();
					}
				}
				else
					_log.warn("mob data for id:" + rset.getInt("npc_templateid") + " missing in npc table");
			}
			DayNightSpawnManager.getInstance().notifyChangeMode();
		}
		catch(Exception e1)
		{
			// problem with initializing spawn, go to next one
			_log.warn("spawn couldnt be initialized:" + e1);
			e1.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		_log.info("SpawnTable: Loaded " + _spawnCount + " Npc Spawn Locations. Total NPCs: " + _npcSpawnCount);
		if(Config.DEBUG)
			_log.info("Spawning completed, total number of NPCs in the world: " + _npcSpawnCount);

		loadRespawnData();
		loadNpcMakers();

		if(scripts)
			Scripts.getInstance().callOnLoad();
	}

	private void loadRespawnData()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		_respawnData = new FastMap<String, RespawnData>().shared();

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM `respawn`");
			rset = statement.executeQuery();

			while(rset.next())
			{
				if(rset.getLong("respawn_time") > 0 && rset.getLong("respawn_time") < System.currentTimeMillis())
					continue;

				_respawnData.put(rset.getString("dbname"), new RespawnData(rset.getString("dbname"), rset.getLong("respawn_time"), rset.getInt("hp"), rset.getInt("mp"), rset.getInt("x"), rset.getInt("y"), rset.getInt("z")));
			}

			mysql.set("DELETE FROM respawn WHERE respawn_time < " + System.currentTimeMillis());
		}
		catch(Exception e)
		{
			_log.warn("SpawnTable: can't load respawn data: " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		_log.info("SpawnTable: Loaded " + _respawnData.size() + " scheduled respawns.");
	}

	public void saveRespawn(String dbname, long respawnTime, int hp, int mp, Location pos)
	{
		mysql.set("REPLACE INTO respawn VALUES('" + dbname + "'," + respawnTime + "," + hp + "," + mp + "," + pos.getX() + "," + pos.getY() + "," + pos.getZ() + ")");
	}

	public void removeRespawnData(RespawnData rd)
	{
		_respawnData.remove(rd.dbname);
	}

	public static Pattern tp = Pattern.compile("\\{(\\-?\\d+);(\\-?\\d+);(\\-?\\d+);(\\-?\\d+);?(.+?\\%)?\\}");

	private void loadNpcMakers()
	{
		_npcMakers = new FastMap<>();
		File file = new File(Config.NPCPOS_FILE);

		if(!file.exists())
		{
			_log.info("The " + Config.NPCPOS_FILE + " file is missing.");
			return;
		}

		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			Document doc = factory.newDocumentBuilder().parse(file);

			for(Node l = doc.getFirstChild(); l != null; l = l.getNextSibling())
			{
				try
				{
					if("list".equalsIgnoreCase(l.getNodeName()))
						for(Node n = l.getFirstChild(); n != null; n = n.getNextSibling())
							if("territory".equalsIgnoreCase(n.getNodeName()))
							{
								String name = n.getAttributes().getNamedItem("name").getNodeValue();
								String points = n.getAttributes().getNamedItem("points").getNodeValue();
								Matcher m = tp.matcher(points);
								L2Territory terr = new L2Territory(name);

								while(m.find())
									terr.add(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)));

								if(terr.getCoords().size() < 3)
									_log.warn("SpawnTable: can't parse territory " + name + " points=" + points);

								TerritoryTable.getInstance().getLocations().put(name, terr);
							}
							else if("npcmaker".equalsIgnoreCase(n.getNodeName()))
							{
								NamedNodeMap attr = n.getAttributes();
								String name = getStringFromNode(attr.getNamedItem("name"), null);
								String ai = getStringFromNode(attr.getNamedItem("ai"), "DefaultMaker");
								String ai_params = getStringFromNode(attr.getNamedItem("ai_parameters"), null);
								String terr = getStringFromNode(attr.getNamedItem("territory"), "");
								String ban_terr = getStringFromNode(attr.getNamedItem("banned_territory"), "");
								int maximum_npc = Integer.parseInt(getStringFromNode(attr.getNamedItem("maximum_npc"), "1"));

								Constructor<?> constructor = null;
								try
								{
									if(!ai.equals("DefaultMaker"))
										constructor = Scripts.getInstance().getClasses().get("npc.maker." + ai).getRawClass().getConstructors()[0];
								}
								catch(Exception e)
								{
									_log.warn("SpawnTable: can't find npcmaker ai: " + ai + " use DefaultMake. " + e);
								}

								DefaultMaker defaultMaker = null;

								if(constructor != null)
									try
									{
										defaultMaker = (DefaultMaker) constructor.newInstance(maximum_npc, name);
									}
									catch(Exception e)
									{
										_log.warn("SpawnTable: can't create npcmaker ai: " + ai + " " + e);
										e.printStackTrace();
									}

								if(defaultMaker == null)
									defaultMaker = new DefaultMaker(maximum_npc, name);

								if(ai_params != null)
								{
									Class<?> dmClass = defaultMaker.getClass();
									Field field;
									for(String key_val : ai_params.split(";"))
										if(!key_val.isEmpty())
										{
											String key = key_val.split("=")[0];
											String val = key_val.split("=")[1];

											key = key.replace("[", "").replace("]", "");

											try
											{
												field = dmClass.getField(key);
											}
											catch(NoSuchFieldException e)
											{
												_log.warn("SpawnTable: maker " + ai + " has no public field: " + key);
												continue;
											}

											try
											{
												if(field.getType().getSimpleName().equalsIgnoreCase("boolean"))
													field.setBoolean(defaultMaker, Boolean.parseBoolean(val));
												else if(field.getType().getSimpleName().equalsIgnoreCase("int"))
													field.setInt(defaultMaker, Integer.valueOf(val));
												else if(field.getType().getSimpleName().equalsIgnoreCase("long"))
													field.setLong(defaultMaker, Long.valueOf(val));
												else if(field.getType().getSimpleName().equalsIgnoreCase("double"))
													field.setDouble(defaultMaker, Double.valueOf(val));
												else if(field.getType().getSimpleName().equalsIgnoreCase("string"))
													field.set(defaultMaker, val);
												else if(field.getType().getSimpleName().equalsIgnoreCase("L2Skill"))
												{
													if(val.contains("-"))
														field.set(defaultMaker, SkillTable.parseSkillInfo(val));
													else
														field.set(defaultMaker, SkillTable.getInstance().getInfo(Integer.parseInt(val)));
												}
												else
													_log.warn("SpawnTable: maker " + ai + " field: " + key + " unsupported type: " + field.getType().getSimpleName());
											}
											catch(IllegalAccessException e)
											{
												_log.warn("SpawnTable: maker " + ai + " field: " + key + " illegal access: " + e);
											}
										}
								}

								if(!terr.isEmpty())
									for(String terr_name : terr.split(";"))
										if(!terr_name.isEmpty())
										{
											L2Territory territory = TerritoryTable.getInstance().getLocations().get(terr_name);
											if(territory == null)
												_log.warn("SpawnTable: maker " + ai + " territory: " + terr_name + " not found.");
											else
												defaultMaker.addTerritory(territory);
										}

								if(!ban_terr.isEmpty())
									for(String terr_name : ban_terr.split(";"))
										if(!terr_name.isEmpty())
										{
											L2Territory territory = TerritoryTable.getInstance().getLocations().get(terr_name);
											if(territory == null)
												_log.warn("SpawnTable: maker " + ai + " banned territory: " + terr_name + " not found.");
											else
												defaultMaker.addBannedTerritory(territory);
										}

								for(Node s = n.getFirstChild(); s != null; s = s.getNextSibling())
									if("npc".equalsIgnoreCase(s.getNodeName()))
									{
										attr = s.getAttributes();
										int npcId = Integer.parseInt(getStringFromNode(attr.getNamedItem("id"), "0"));
										int total = Integer.parseInt(getStringFromNode(attr.getNamedItem("total"), "1"));
										int respawn = getSecFromString(getStringFromNode(attr.getNamedItem("respawn"), "no"));
										int respawn_rand = getSecFromString(getStringFromNode(attr.getNamedItem("respawn_rand"), "0"));
										int is_chase_pc = Integer.parseInt(getStringFromNode(attr.getNamedItem("is_chase_pc"), "0"));

										String pos = getStringFromNode(attr.getNamedItem("pos"), "anywhere");
										String npc_ai = getStringFromNode(attr.getNamedItem("ai"), null);
										String npc_ai_params = getStringFromNode(attr.getNamedItem("ai_parameters"), null);
										String privates = getStringFromNode(attr.getNamedItem("Privates"), null);
										String dbname = getStringFromNode(attr.getNamedItem("dbname"), null);
										String dbsaving = getStringFromNode(attr.getNamedItem("dbsaving"), "");
										boolean rbSpawnSet = getStringFromNode(attr.getNamedItem("boss_respawn_set"), "no").equalsIgnoreCase("yes");
										L2NpcTemplate template;

										if(npcId > 0 && (template = NpcTable.getTemplate(npcId)) != null)
										{
											SpawnDefine spawnDefine = new SpawnDefine(template, total, respawn, respawn_rand, npc_ai, npc_ai_params, privates, dbname, dbsaving, defaultMaker, rbSpawnSet);
											if(is_chase_pc > 0)
												spawnDefine.setChasePc(is_chase_pc);

											if(!"anywhere".equals(pos))
											{
												Matcher m = tp.matcher(pos);

												while(m.find())
													if(m.group(5) != null)
														spawnDefine.addPosition(new Location(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4))), Integer.parseInt(m.group(5).replace("%", "")));
													else
														spawnDefine.addPosition(new Location(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4))), 100);
											}

											defaultMaker.addSpawnDefine(spawnDefine);
										}
										else
											_log.warn("SpawnTable: npc template id: " + npcId + " not found!");

										_npcMakers.put(name, defaultMaker);
									}
							}
				}
				catch(Exception e)
				{
					_log.warn("SpawnTable: can't load npcpos data " + e);
					e.printStackTrace();
				}
			}
		}
		catch(Exception e)
		{
			_log.warn("SpawnTable: error while loading " + Config.NPCPOS_FILE + " " + e);
			e.printStackTrace();
		}

		_log.info("SpawnTable: loaded " + _npcMakers.size() + " npc makers.");

		for(DefaultMaker maker : _npcMakers.values())
			maker.onStart();
	}

	public DefaultMaker getNpcMaker(String name)
	{
		return _npcMakers.get(name);
	}

	public RespawnData getRespawnData(String name)
	{
		return _respawnData.get(name);
	}

	private static String getStringFromNode(Node n, String def)
	{
		return n == null ? def : n.getNodeValue();
	}

	public static int getSecFromString(String time)
	{
		if(time.endsWith("sec"))
			return Integer.parseInt(time.replace("sec", ""));
		if(time.endsWith("min"))
			return Integer.parseInt(time.replace("min", "")) * 60;
		if(time.endsWith("hour"))
			return Integer.parseInt(time.replace("hour", "")) * 3600;
		if(time.equalsIgnoreCase("no"))
			return 0;

		return Integer.parseInt(time);
	}

	public L2Spawn getTemplate(int id)
	{
		return _spawntable.get(id);
	}

	public GArray<L2Spawn> getSpawnsByNpcId(int id)
	{
		return _spawnsByNpcId.get(id);
	}

	public void addNewSpawn(L2Spawn spawn, boolean storeInDb, L2Player editor)
	{
		if(Config.DONTLOADSPAWN)
			return;

		if(spawn.getId() == 0)
		{
			_highestId++;
			spawn.setId(_highestId);
		}

		_spawntable.put(spawn.getId(), spawn);

		GArray<L2Spawn> s = _spawnsByNpcId.get(spawn.getNpcId());
		if(s == null)
		{
			s = new GArray<L2Spawn>();
			_spawnsByNpcId.put(spawn.getId(), s);
		}
		s.add(spawn);

		if(!storeInDb)
			return;

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			if(editor == null)
				statement = con.prepareStatement("INSERT INTO `spawnlist` (location,count,npc_templateid,locx,locy,locz,heading,respawn_delay,loc_id) values(?,?,?,?,?,?,?,?,?)");
			else
			{
				statement = con.prepareStatement("INSERT INTO `spawnlist` (location,count,npc_templateid,locx,locy,locz,heading,respawn_delay,loc_id,last_editor) values(?,?,?,?,?,?,?,?,?,?)");
				statement.setString(10, editor.getName());
			}

			statement.setString(1, "");// spawn.getLocation());
			statement.setInt(2, spawn.getAmount());
			statement.setInt(3, spawn.getNpcId());
			statement.setInt(4, spawn.getLocx());
			statement.setInt(5, spawn.getLocy());
			statement.setInt(6, spawn.getLocz());
			statement.setInt(7, spawn.getHeading());
			statement.setInt(8, spawn.getRespawnDelay());
			statement.setInt(9, spawn.getLocation());
			statement.execute();
		}
		catch(Exception e1)
		{
			// problem with storing spawn
			_log.warn("spawn couldnt be stored in db:" + e1);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void deleteSpawn(L2Spawn spawn, boolean updateDb)
	{
		GArray<L2Spawn> s = _spawnsByNpcId.get(spawn.getNpcId());
		if(s != null)
			s.remove(spawn);

		if(_spawntable.remove(new Integer(spawn.getId())) != null && updateDb)
		{
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("DELETE FROM `spawnlist` WHERE `npc_templateid`=? AND `locx`=? AND `locy`=? AND `locz`=? AND `loc_id`=?");
				statement.setInt(1, spawn.getNpcId());
				statement.setInt(2, spawn.getLocx());
				statement.setInt(3, spawn.getLocy());
				statement.setInt(4, spawn.getLocz());
				statement.setInt(5, spawn.getLocation());
				statement.execute();
			}
			catch(Exception e1)
			{
				// problem with deleting spawn
				_log.warn("spawn couldnt be deleted in db:" + e1);
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}

	public void startEventSpawn(String event)
	{
		if(_eventSpawns.containsKey(event))
		{
			for(L2Spawn spawn : _eventSpawns.get(event))
			{
				if(spawn.getNpcTemplate().type.equals("L2RaidBoss") || spawn.getNpcTemplate().type.equals("L2Boss") || spawn.getNpcTemplate().type.equals("QueenAnt"))
					RaidBossSpawnManager.getInstance().addNewSpawn(spawn, false, null);
				else
					spawn.init();
			}
		}
		else if(_npcMakers.containsKey(event))
		{
			DefaultMaker dm = _npcMakers.get(event);
			dm.onScriptEvent(1001, 0, 0);
		}
		else
		{
			_log.warn("SpawnTable: no spawn list for event: " + event);
		}
	}

	public GArray<L2Spawn> getEventSpawn(String event, Instance inst)
	{
		if(!_eventSpawns.containsKey(event))
		{
			_log.warn("SpawnTable: no spawn list for event: " + event);
			return null;
		}

		GArray<L2Spawn> res = new GArray<L2Spawn>(_eventSpawns.get(event).size());
		for(L2Spawn spawn : _eventSpawns.get(event))
		{
			L2Spawn instSpawn = spawn.copy();
			if(inst != null)
			{
				instSpawn.setInstance(inst);
				instSpawn.setReflection(inst.getReflection());
			}
			res.add(instSpawn);
		}
		return res;
	}

	public void stopEventSpawn(String event, boolean despawn)
	{
		if(_eventSpawns.containsKey(event))
		{
			for(L2Spawn spawn : _eventSpawns.get(event))
			{
				if(spawn.getNpcTemplate().type.equals("L2RaidBoss") || spawn.getNpcTemplate().type.equals("L2Boss") || spawn.getNpcTemplate().type.equals("QueenAnt"))
					RaidBossSpawnManager.getInstance().deleteSpawn(spawn, false);
				else
					spawn.stopRespawn();
				if(despawn)
					spawn.despawnAll();
			}
		}
		else if(_npcMakers.containsKey(event))
		{
			DefaultMaker dm = _npcMakers.get(event);
			dm.despawn();
		}
		else
		{
			_log.warn("SpawnTable: no spawn list for event: " + event);
		}
	}

	public L2GroupSpawn getEventGroupSpawn(String event, Instance inst)
	{
		if(!_eventSpawns.containsKey(event))
		{
			_log.warn("SpawnTable: no spawn list for event: " + event);
			return null;
		}

		L2GroupSpawn group = new L2GroupSpawn();
		group.setEventName(event);

		if(inst != null)
		{
			group.setReflection(inst.getReflection());
			group.setInstance(inst);
		}

		for(L2Spawn spawn : _eventSpawns.get(event))
			group.addSpawn(spawn.copy());

		return group;
	}

	public GArray<L2Spawn> getEventSpawns(String event)
	{
		return _eventSpawns.get(event);
	}

	//just wrapper
	public void reloadAll()
	{
		L2World.deleteVisibleNpcSpawns();
		for(DefaultMaker defaultMaker : _npcMakers.values())
		{
			defaultMaker.save();
			defaultMaker.despawn();
			defaultMaker.stopTimers();
		}
		DayNightSpawnManager.getInstance().cleanUp();
		fillSpawnTable(false);
		RaidBossSpawnManager.getInstance().reloadBosses();
	}

	public void reloadNpcMakers()
	{
		for(DefaultMaker defaultMaker : _npcMakers.values())
		{
			defaultMaker.save();
			defaultMaker.despawn();
			defaultMaker.stopTimers();
		}

		loadNpcMakers();
	}
}
