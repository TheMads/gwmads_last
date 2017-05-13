package ru.l2gw.gameserver.tables;

import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.extensions.scripts.Script;
import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.gameserver.ai.L2CharacterAI;
import ru.l2gw.gameserver.cache.InfoCache;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.base.ClassId;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.skills.Formulas;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.gameserver.templates.L2Weapon;
import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.util.Files;
import ru.l2gw.util.Util;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class NpcTable
{
	private static final org.apache.commons.logging.Log _log = LogFactory.getLog(NpcTable.class.getName());

	private static NpcTable _instance;

	private static TIntObjectHashMap<L2NpcTemplate> _npcs;
	private static TIntObjectHashMap<StatsSet> ai_params;
	private static GArray<L2NpcTemplate>[] _npcsByLevel;
	private static HashMap<String, L2NpcTemplate> _npcsNames;
	private static boolean _initialized = false;
	private static final DropComparator dropComparator = new DropComparator();
	private static final DropGroupComparator dropGroupComparator = new DropGroupComparator();

	public static NpcTable getInstance()
	{
		if(_instance == null)
			_instance = new NpcTable();

		return _instance;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private NpcTable()
	{
		_npcs = new TIntObjectHashMap<>();
		_npcsByLevel = new GArray[100];
		_npcsNames = new HashMap<>();
		ai_params = new TIntObjectHashMap<>();
		RestoreNpcData();
	}

	private void RestoreNpcData()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			try
			{
				statement = con.prepareStatement("SELECT * FROM ai_params");
				rs = statement.executeQuery();
				LoadAIParams(rs);
			}
			catch(Exception e)
			{}
			finally
			{
				DbUtils.closeQuietly(statement, rs);
			}

			try
			{
				statement = con.prepareStatement("SELECT * FROM npc WHERE ai_type IS NOT NULL");
				rs = statement.executeQuery();
				fillNpcTable(rs);
			}
			catch(Exception e)
			{
				_log.warn("NpcTable: Error while creating npc table ", e);
			}
			finally
			{
				DbUtils.closeQuietly(statement, rs);
			}

			try
			{
				statement = con.prepareStatement("SELECT npcid, skillid, level, stype FROM npcskills");
				rs = statement.executeQuery();
				L2NpcTemplate npcDat;
				L2Skill npcSkill;
				int counter = 0;
				while(rs.next())
				{
					int mobId = rs.getInt("npcid");
					npcDat = _npcs.get(mobId);
					if(npcDat == null)
						continue;
					short skillId = rs.getShort("skillid");
					byte level = rs.getByte("level");

					// Для определения расы используется скилл 4416
					if(skillId == 4416)
						npcDat.setRace(level);
					// Скилы 4290 - 4302 не используются
					else if(skillId >= 4290 && skillId <= 4302)
					{
						_log.info("Warning! Skill " + skillId + " not used, use 4416 instead.");
						continue;
					}

					npcSkill = SkillTable.getInstance().getInfo(skillId, level);

					if(npcSkill == null)
						continue;

					npcDat.addSkill(npcSkill, rs.getString("stype"));
					counter++;
				}
				_log.info("Loaded " + counter + " npc skills.");
			}
			catch(Exception e)
			{
				_log.warn("error while reading npcskills table ", e);
			}
			finally
			{
				DbUtils.closeQuietly(statement, rs);
			}

			try
			{
				statement = con.prepareStatement("SELECT * FROM droplist ORDER BY mobId, drop_type, gid");
				rs = statement.executeQuery();
				L2NpcTemplate npcDat;

				while(rs.next())
				{
					int mobId = rs.getInt("mobId");
					npcDat = _npcs.get(mobId);
					if(npcDat != null)
					{
						L2Item item = ItemTable.getInstance().getTemplate(rs.getShort("itemId"));
						if(item == null)
						{
							_log.warn("No item template id: " + rs.getShort("itemId"));
							continue;
						}
						npcDat.addDropData(new L2DropData(item, rs.getInt("min"), rs.getInt("max"), (int) (rs.getFloat("chance") * 10000), rs.getShort("gid"), npcDat.isRaid), (int) (rs.getFloat("gchance") * 10000), rs.getInt("drop_type"));
					}
				}

				if(Config.ALT_GAME_SHOW_DROPLIST && !Config.ALT_GAME_GEN_DROPLIST_ON_DEMAND)
					FillDropList();
				else
					_log.info("Players droplist load skipped");
			}
			catch(Exception e)
			{
				_log.warn("error reading npc drops ", e);
			}
			finally
			{
				DbUtils.closeQuietly(statement, rs);
			}

			try
			{
				statement = con.prepareStatement("SELECT * FROM minions");
				rs = statement.executeQuery();
				L2MinionData minionDat;
				L2NpcTemplate npcDat;
				int cnt = 0;

				while(rs.next())
				{
					int raidId = rs.getInt("boss_id");
					npcDat = _npcs.get(raidId);
					minionDat = new L2MinionData(rs.getInt("minion_id"), null, rs.getInt("amount"), rs.getInt("respawn"), 0);
					npcDat.addRaidData(minionDat);
					cnt++;
				}

				_log.info("NpcTable: Loaded " + cnt + " Minions.");
			}
			catch(Exception e)
			{
				_log.warn("error loading minions", e);
			}
			finally
			{
				DbUtils.closeQuietly(statement, rs);
			}

			try
			{
				statement = con.prepareStatement("SELECT npc_id, class_id FROM skill_learn");
				rs = statement.executeQuery();
				L2NpcTemplate npcDat = null;
				int cnt = 0;

				while(rs.next())
				{
					npcDat = _npcs.get(rs.getInt(1));
					npcDat.addTeachInfo(ClassId.values()[rs.getInt(2)]);
					cnt++;
				}

				_log.info("NpcTable: Loaded " + cnt + " SkillLearn entrys.");
			}
			catch(Exception e)
			{
				_log.warn("error loading minions", e);
			}
			finally
			{
				DbUtils.closeQuietly(statement, rs);
			}
		}
		catch(Exception e)
		{
			_log.warn("Cannot find connection to database");
		}
		finally
		{
			DbUtils.closeQuietly(con);
		}

		_initialized = true;

		Scripts.getInstance();
	}

	private static void LoadAIParams(ResultSet AIData) throws Exception
	{
		int ai_params_counter = 0;
		StatsSet _set = null;
		int npc_id;
		String param, value;
		while(AIData.next())
		{
			npc_id = AIData.getInt("npc_id");
			param = AIData.getString("param");
			value = AIData.getString("value");
			if(ai_params.containsKey(npc_id))
				_set = ai_params.get(npc_id);
			else
			{
				_set = new StatsSet();
				ai_params.put(npc_id, _set);
			}
			_set.set(param, value);
			ai_params_counter++;
		}
		_log.info("NpcTable: Loaded " + ai_params_counter + " AI params for " + ai_params.size() + " NPCs.");
	}

	private static StatsSet fillNpcTable(ResultSet NpcData) throws Exception
	{
		StatsSet npcDat = null;
		while(NpcData.next())
		{
			npcDat = new StatsSet();
			int id = NpcData.getInt("id");
			int level = NpcData.getByte("level");

			npcDat.set("npcId", id);
			npcDat.set("displayId", NpcData.getInt("displayId"));
			npcDat.set("level", level);

			npcDat.set("baseShldDef", NpcData.getInt("shield_defense"));
			npcDat.set("baseShldRate", NpcData.getInt("shield_defense_rate"));
			npcDat.set("baseCritRate", Math.max(1, NpcData.getInt("base_critical")));

			npcDat.set("name", NpcData.getString("name"));
			npcDat.set("title", NpcData.getString("title"));
			npcDat.set("collision_radius", NpcData.getDouble("collision_radius"));
			npcDat.set("collision_height", NpcData.getDouble("collision_height"));
			npcDat.set("sex", NpcData.getString("sex"));
			npcDat.set("type", NpcData.getString("type"));
			npcDat.set("ai_type", NpcData.getString("ai_type"));
			npcDat.set("baseAtkRange", NpcData.getInt("attackrange"));
			npcDat.set("revardExp", NpcData.getInt("exp"));
			npcDat.set("revardSp", NpcData.getInt("sp"));
			npcDat.set("basePAtkSpd", Formulas.getPAtkSpdFromBase(NpcData.getInt("atkspd"), NpcData.getInt("dex")));
			npcDat.set("baseMAtkSpd", Formulas.getMAtkSpdFromBase(NpcData.getInt("matkspd"), NpcData.getInt("wit")));
			npcDat.set("aggroRange", NpcData.getShort("aggro"));
			npcDat.set("rhand", NpcData.getInt("rhand"));
			npcDat.set("lhand", NpcData.getInt("lhand"));
			npcDat.set("armor", NpcData.getInt("armor"));
			npcDat.set("baseWalkSpd", NpcData.getInt("walkspd"));
			npcDat.set("baseRunSpd", NpcData.getInt("runspd"));

			npcDat.set("baseHpReg", NpcData.getDouble("base_hp_regen"));
			npcDat.set("baseCpReg", 0);
			npcDat.set("baseMpReg", NpcData.getDouble("base_mp_regen"));

			npcDat.set("baseSTR", NpcData.getInt("str"));
			npcDat.set("baseCON", NpcData.getInt("con"));
			npcDat.set("baseDEX", NpcData.getInt("dex"));
			npcDat.set("baseINT", NpcData.getInt("int"));
			npcDat.set("baseWIT", NpcData.getInt("wit"));
			npcDat.set("baseMEN", NpcData.getInt("men"));

			npcDat.set("baseHpMax", Formulas.getMaxHpFromBase(NpcData.getFloat("hp"), NpcData.getInt("con")));
			npcDat.set("baseCpMax", 0);
			npcDat.set("baseMpMax", Formulas.getMaxMpFromBase(NpcData.getFloat("mp"),  NpcData.getInt("men")));
			int pAtk = 0;
			int mAtk = 0;
			if(NpcData.getInt("rhand") > 0)
			{
				L2Item weapon = ItemTable.getInstance().getTemplate(NpcData.getInt("rhand"));
				if(weapon instanceof L2Weapon)
				{
					pAtk = ((L2Weapon) weapon).physical_damage;
					mAtk = ((L2Weapon) weapon).magic_damage;
				}
			}
			npcDat.set("basePAtk", Formulas.getPAtkFromBase((int) NpcData.getFloat("patk") + pAtk, NpcData.getInt("str"), level));
			npcDat.set("baseMAtk", Formulas.getMAtkFromBase((int) NpcData.getFloat("matk") + mAtk, NpcData.getInt("int"), level));
			npcDat.set("basePDef", Formulas.getPDefFromBase((int) NpcData.getFloat("pdef"), level));
			npcDat.set("baseMDef", Formulas.getMDefFromBase((int) NpcData.getFloat("mdef"), NpcData.getInt("men"), level));

			npcDat.set("factionId", NpcData.getString("faction_id"));
			npcDat.set("factionRange", NpcData.getShort("faction_range"));

			npcDat.set("isDropHerbs", NpcData.getBoolean("isDropHerbs"));

			npcDat.set("shots", NpcData.getString("shots"));
			npcDat.set("corpse_time", NpcData.getInt("corpse_time"));
			npcDat.set("ignore_clan_list", NpcData.getString("ignore_clan_list"));

			npcDat.set("physical_hit_modify", NpcData.getFloat("physical_hit_modify"));
			npcDat.set("physical_avoid_modify", NpcData.getFloat("physical_avoid_modify"));
			npcDat.set("soulshot_count", NpcData.getInt("soulshot_count"));
			npcDat.set("spiritshot_count", NpcData.getInt("spiritshot_count"));
			npcDat.set("hp_mod", NpcData.getFloat("hp_mod"));
			npcDat.set("base_rand_dam", NpcData.getFloat("base_rand_dam"));

			npcDat.set("base_attr_attack", NpcData.getInt("base_attr_attack"));
			npcDat.set("base_attr_attack_value", NpcData.getInt("base_attr_attack_value"));
			npcDat.set("base_attr_def_fire", NpcData.getInt("base_attr_def_fire"));
			npcDat.set("base_attr_def_water", NpcData.getInt("base_attr_def_water"));
			npcDat.set("base_attr_def_wind", NpcData.getInt("base_attr_def_wind"));
			npcDat.set("base_attr_def_earth", NpcData.getInt("base_attr_def_earth"));
			npcDat.set("base_attr_def_holy", NpcData.getInt("base_attr_def_holy"));
			npcDat.set("base_attr_def_dark", NpcData.getInt("base_attr_def_dark"));
			npcDat.set("undying", NpcData.getInt("undying"));
			npcDat.set("can_be_attacked", NpcData.getInt("can_be_attacked"));
			npcDat.set("can_move", NpcData.getInt("can_move"));
			npcDat.set("flying", NpcData.getInt("flying"));
			npcDat.set("targetable", NpcData.getInt("targetable"));
			npcDat.set("show_name_tag", NpcData.getInt("show_name_tag"));
			npcDat.set("unsowing", NpcData.getInt("unsowing"));

			L2NpcTemplate template = new L2NpcTemplate(npcDat, ai_params.containsKey(id) ? ai_params.get(id) : null);
			_npcs.put(id, template);
			if(_npcsByLevel[level] == null)
				_npcsByLevel[level] = new GArray<L2NpcTemplate>();
			_npcsByLevel[level].add(template);
			_npcsNames.put(NpcData.getString("name").toLowerCase(), template);
		}
		_log.info("NpcTable: Loaded " + _npcs.size() + " Npc Templates.");

		return npcDat;
	}

	public static void reloadNpc(int id)
	{
		Connection con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			// save a copy of the old data
			L2NpcTemplate old = getTemplate(id);
			HashMap<String, L2Skill[]> skills = new HashMap<String, L2Skill[]>();
			if(old.getSkills() != null)
				skills.putAll(old.getAllSkillsByType());

			GArray<ClassId> classIds = null;
			if(old.getTeachInfo() != null)
			{
				classIds = new GArray<ClassId>(old.getTeachInfo().size());
				classIds.addAll(old.getTeachInfo());
			}
			ArrayList<L2MinionData> minions = new ArrayList<L2MinionData>();
			if(old.getMinionData() != null)
				minions.addAll(old.getMinionData());

			// reload the NPC base data
			con = DatabaseFactory.getInstance().getConnection();
			st = con.prepareStatement("SELECT * FROM npc WHERE id=?");
			st.setInt(1, id);
			rs = st.executeQuery();
			fillNpcTable(rs);

			// restore additional data from saved copy
			L2NpcTemplate created = getTemplate(id);
			for(String type : skills.keySet())
			{
				L2Skill[] skillList = skills.get(type);
				for(L2Skill skill : skillList)
					created.addSkill(skill, type);
			}
			/*
			 for(L2DropData drop : drops)
			 created.addDropData(drop);
			 */
			if(classIds != null)
				for(ClassId classId : classIds)
					created.addTeachInfo(classId);
			for(L2MinionData minion : minions)
				created.addRaidData(minion);
		}
		catch(Exception e)
		{
			_log.warn("cannot reload npc " + id + ": " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, st, rs);
		}
	}

	public static StatsSet getNpcStatsSet(int id)
	{
		StatsSet dat = null;

		Connection con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			st = con.prepareStatement("SELECT * FROM npc WHERE id=?");
			st.setInt(1, id);
			rs = st.executeQuery();
			dat = fillNpcTable(rs);
		}
		catch(Exception e)
		{
			_log.warn("cannot load npc stats for " + id + ": " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, st, rs);
		}

		return dat;
	}

	// just wrapper
	public void reloadAllNpc()
	{
		RestoreNpcData();
	}

	public void saveNpc(StatsSet npc, L2Player editor)
	{
		Connection con = null;
		PreparedStatement statement = null;
		String query = "";
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			HashMap<String, Object> set = npc.getSet();
			String name = "";
			String values = "";
			for(Object obj : set.keySet())
			{
				name = (String) obj;
				if(!name.equalsIgnoreCase("npcId"))
				{
					if(!values.equals(""))
						values += ", ";
					values += name + " = '" + set.get(name) + "'";
				}
			}
			if(!values.equals(""))
				values += ", last_editor = '" + editor.getName() + "'";
			query = "UPDATE npc SET " + values + " WHERE id = ?";
			statement = con.prepareStatement(query);
			statement.setInt(1, npc.getInteger("npcId"));
			statement.execute();
		}
		catch(Exception e1)
		{
			// problem with storing spawn
			_log.warn("npc data couldnt be stored in db, query is :" + query + " : " + e1);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public static boolean isInitialized()
	{
		return _initialized;
	}

	public static void replaceTemplate(L2NpcTemplate npc)
	{
		_npcs.put(npc.npcId, npc);
		_npcsNames.put(npc.name.toLowerCase(), npc);
	}

	public static L2NpcTemplate getTemplate(int id)
	{
		return _npcs.get(id);
	}

	public static L2NpcTemplate getTemplateByName(String name)
	{
		return _npcsNames.get(name.toLowerCase());
	}

	public static GArray<L2NpcTemplate> getAllOfLevel(int lvl)
	{
		return _npcsByLevel[lvl];
	}

	public static L2NpcTemplate[] getAll()
	{
		return _npcs.valueCollection().toArray(new L2NpcTemplate[_npcs.size()]);
	}

	public void FillDropList()
	{
		for(L2NpcTemplate npc : _npcs.valueCollection())
			InfoCache.addToDroplistCache(npc.npcId, generateDroplist(npc));
		_log.info("Players droplist was cached");
	}

	public static String generateDroplist(L2NpcTemplate npc)
	{
		HashMap<Integer, String> tpls = Util.parseTemplate(Files.read("data/html/show_drop.htm", Config.DEFAULT_LANG, false));
		String html = tpls.get(0);
		html = html.replace("<?npc_name?>", npc.name);
		html = html.replace("<?npc_id?>", String.valueOf(npc.getNpcId()));
		if(npc.getDropData() == null)
		{
			html = html.replace("<?drop_herbs?>", "");
			html = html.replace("<?drop_list?>", "No drop");
			return html;
		}

		html = html.replace("<?drop_herbs?>", npc.getDropData().getExDrop().isEmpty() ? "" : tpls.get(1));

		StringBuilder dropList = new StringBuilder();
		if(!npc.getDropData().getNormal().isEmpty())
		{
			for(L2DropGroup g : npc.getDropData().getNormal())
			{
				StringBuilder drop = new StringBuilder();
				L2DropData[] sorted = new L2DropData[g.getDropItems(false).size()];
				g.getDropItems(false).toArray(sorted);
				Arrays.sort(sorted, dropComparator);
				for(L2DropData d : sorted)
					drop.append(tpls.get(3).replace("<?item_name?>", compact(d.getItem().getName())).replace("<?item_count?>", formatCount(d.getBalancedMin(), d.getBalancedMax())).replace("<?item_chance?>", formatChance(d.getBalancedChance())));

				dropList.append(tpls.get(2).replace("<?drop_row?>", drop).replace("<?group_id?>", String.valueOf(g.getId() + 1)).replace("<?group_chance?>", formatChance(g.getRatedGroupChance())));
			}
		}

		if(!npc.getDropData().getAdditional().isEmpty())
		{
			StringBuilder drop = new StringBuilder();
			L2DropGroup[] sorted = new L2DropGroup[npc.getDropData().getAdditional().size()];
			npc.getDropData().getAdditional().toArray(sorted);
			Arrays.sort(sorted, dropGroupComparator);
			for(L2DropGroup g : sorted)
				for(L2DropData d : g.getDropItems(false))
					drop.append(tpls.get(3).replace("<?item_name?>", compact(d.getItem().getName())).replace("<?item_count?>", formatCount(d.getBalancedMin(), d.getBalancedMax())).replace("<?item_chance?>", formatChance(d.getBalancedChance())));

			dropList.append(tpls.get(4).replace("<?drop_row?>", drop));
		}

		if(!npc.getDropData().getSpoil().isEmpty())
		{
			StringBuilder drop = new StringBuilder();
			L2DropGroup[] sorted = new L2DropGroup[npc.getDropData().getSpoil().size()];
			npc.getDropData().getSpoil().toArray(sorted);
			Arrays.sort(sorted, dropGroupComparator);
			for(L2DropGroup g : sorted)
				for(L2DropData d : g.getDropItems(false))
					drop.append(tpls.get(3).replace("<?item_name?>", compact(d.getItem().getName())).replace("<?item_count?>", formatCount(d.getBalancedMin(), d.getBalancedMax())).replace("<?item_chance?>", formatChance(d.getBalancedChance())));

			dropList.append(tpls.get(5).replace("<?drop_row?>", drop));
		}

		return html.replace("<?drop_list?>", dropList);
	}

	public static GArray<String> generateDroplistString(int npcId)
	{
		L2NpcTemplate npc = getTemplate(npcId);
		GArray<String> ret = new GArray<>();
		if(npc == null)
		{
			ret.add("No npc with id " + npcId + " found!");
			return ret;
		}
		ret.add(npc.name + ", Id: " + npc.getNpcId() + (npc.isDropHerbs ? "herbs" : ""));

		if(npc.getDropData() != null)
		{
			for(L2DropGroup g : npc.getDropData().getNormal())
			{
				ret.add("Group: " +  g.getId() + " chance: " + g.getRatedGroupChance());
				for(L2DropData d : g.getDropItems(false))
					ret.add(compact(d.getItem().getName()) + "\t\t" + d.getBalancedMin() + "-" + d.getBalancedMax() + "\t" + formatChance(d.getBalancedChance()));
			}

			if(!npc.getDropData().getAdditional().isEmpty())
			{
				ret.add("Additional: ");
				for(L2DropGroup g : npc.getDropData().getAdditional())
					for(L2DropData d : g.getDropItems(false))
						ret.add(compact(d.getItem().getName()) + "\t\t" + d.getBalancedMin() + "-" + d.getBalancedMax() + "\t" + formatChance(d.getBalancedChance()));
			}

			if(!npc.getDropData().getExDrop().isEmpty())
			{
				ret.add("Ex drop list:");
				for(L2DropGroup g : npc.getDropData().getExDrop())
				{
					ret.add("Ex group: " + g.getId());
					for(L2DropData d : g.getDropItems(false))
						ret.add(compact(d.getItem().getName()) + "\t\t" + d.getBalancedMin() + "-" + d.getBalancedMax() + "\t" + formatChance(d.getBalancedChance()));
				}
			}

			if(npc.getDropData().getSpoil().size() > 0)
			{
				ret.add("Spoil:");
				for(L2DropGroup g : npc.getDropData().getSpoil())
					for(L2DropData d : g.getDropItems(false))
						ret.add(compact(d.getItem().getName()) + "\t\t" + d.getBalancedMin() + "-" + d.getBalancedMax() + "\t" + formatChance(d.getBalancedChance()));
			}
		}
		
		return ret;
	}

	public static String compact(String s)
	{
		return s.replaceFirst("Recipe:", "R:").replaceAll("Recipe -", "R:").replaceAll("Life Stone -", "LS:").replace("Mid-Grade", "Mid").replace("High-Grade", "High").replace("Top-Grade", "Top").replaceFirst("Common Item - ", "Common ").replaceFirst("Scroll: Enchant", "Enchant").replaceFirst("Compressed Package", "CP");
	}

	public static String formatChance(int c)
	{
		float chance = c / 10000f;

		if (chance >= 20)
			return String.format("%.2f", chance) + "%";
		else
			return "1/" + Math.round(1 / chance * 100);
	}

	public static String formatCount(int min, int max)
	{
		if(min == max)
			return String.valueOf(max);

		if(min >= 1000)
		{
			String k = "k";
			while((min /= 1000) >= 1000)
				k += "k";

			String km = "k";
			while((max /= 1000) >= 1000)
				km += "k";

			if(min == max)
				return min + k;

			return min + k + "-" + max + km;
		}
		return min + "-" + max;
	}

	@SuppressWarnings("unchecked")
	public void applyServerSideTitle()
	{
		Class<?> L2MonsterInstance;
		if(Config.SERVER_SIDE_NPC_TITLE_WITH_LVL)
			try
			{
				L2MonsterInstance = Class.forName("ru.l2gw.gameserver.model.instances.L2MonsterInstance");
				for(L2NpcTemplate npc : _npcs.valueCollection())
				{
					String title = "";
					Class<?> _this = null;
					try
					{
						_this = Class.forName("ru.l2gw.gameserver.model.instances." + npc.type + "Instance");
					}
					catch(ClassNotFoundException e)
					{
						Script sc = Scripts.getInstance().getClasses().get("npc.model.instances." + npc.type + "Instance");
						if(sc != null)
							_this = sc.getRawClass();
						else
							continue;
					}
					if(L2MonsterInstance.isAssignableFrom(_this) && !npc.type.equalsIgnoreCase("L2TamedBeast"))
					{
						title = "L" + npc.level;
						if(npc.aggroRange != 0 || npc.factionRange != 0)
							title += " " + (npc.aggroRange != 0 ? "A" : "") + (npc.factionRange != 0 ? "S" : "");
						title += " ";
					}
					npc.title = title + npc.title;
				}
			}
			catch(ClassNotFoundException e1)
			{
				e1.printStackTrace();
			}
	}

	public static void setAIParams(L2NpcInstance npc, L2CharacterAI ai)
	{
		StatsSet aiParams = npc.getAIParams();
		if(aiParams != null && ai != null)
		{
			Class<?> aiClass = ai.getClass();
			Field field;
			for(Map.Entry<String, Object> entry : aiParams.getSet().entrySet())
			{
				String key = entry.getKey();
				if(key.startsWith("[") && key.endsWith("]"))
				{
					key = key.replace("[", "").replace("]", "");
					try
					{
						field = aiClass.getField(key);
					}
					catch(NoSuchFieldException e)
					{
						if(Config.DEBUG_AI)
							_log.warn(npc + " ai=[" + (npc.getAIConstructor() != null ? npc.getAIConstructor().getName() : npc.getTemplate().ai_type) + "] has no public field: " + key);
						continue;
					}

					try
					{
						if(field.getType().getSimpleName().equalsIgnoreCase("boolean"))
							field.setBoolean(ai, Boolean.parseBoolean(entry.getValue().toString()));
						else if(field.getType().getSimpleName().equalsIgnoreCase("int"))
							field.setInt(ai, Integer.valueOf(entry.getValue().toString()));
						else if(field.getType().getSimpleName().equalsIgnoreCase("long"))
							field.setLong(ai, Long.valueOf(entry.getValue().toString()));
						else if(field.getType().getSimpleName().equalsIgnoreCase("double"))
							field.setDouble(ai, Double.valueOf(entry.getValue().toString()));
						else if(field.getType().getSimpleName().equalsIgnoreCase("float"))
							field.setFloat(ai, Float.valueOf(entry.getValue().toString()));
						else if(field.getType().getSimpleName().equalsIgnoreCase("string"))
							field.set(ai, entry.getValue().toString());
						else if(field.getType().getSimpleName().equalsIgnoreCase("L2Skill"))
						{
							String skill = entry.getValue().toString();
							if(skill.contains("-"))
								field.set(ai, SkillTable.parseSkillInfo(skill));
							else
								field.set(ai, SkillTable.getInstance().getInfo(Integer.parseInt(skill)));
						}
						else
							_log.warn(npc + " ai=[" + npc.getTemplate().ai_type + "] field: " + key + " unsupported type: " + field.getType().getSimpleName());
					}
					catch(IllegalAccessException e)
					{
						_log.warn(npc + " ai=[" + npc.getTemplate().ai_type + "] field: " + key + " illegal access: " + e);
					}
				}
			}
		}
	}

	private static class DropComparator implements Comparator<L2DropData>
	{
		@Override
		public int compare(L2DropData o1, L2DropData o2)
		{
			if(o1 == null || o2 == null)
				return 0;

			return o2.getBalancedChance() - o1.getBalancedChance();
		}
	}

	private static class DropGroupComparator implements Comparator<L2DropGroup>
	{
		@Override
		public int compare(L2DropGroup o1, L2DropGroup o2)
		{
			if(o1 == null || o2 == null)
				return 0;

			return o2.getDropItems(false).get(0).getBalancedChance() - o1.getDropItems(false).get(0).getBalancedChance();
		}
	}
}
