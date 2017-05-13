package ru.l2gw.gameserver.tables;

import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ru.l2gw.commons.arrays.ArrayUtils;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.commons.utils.XmlUtil;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.base.ClassId;
import ru.l2gw.gameserver.templates.L2PlayerTemplate;
import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.util.Location;

import javax.xml.parsers.DocumentBuilderFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CharTemplateTable
{
	private static final Log _log = LogFactory.getLog(CharTemplateTable.class.getName());

	private static CharTemplateTable _instance;

	private TIntObjectHashMap<L2PlayerTemplate> _templates;
	private static Map<ClassId, GArray<Location>> initialStartPoints;
	private static Map<ClassId, GArray<StatsSet>> initialEquipment;
	private static Map<ClassId, GArray<StatsSet>> initialCustomEquipment;

	public static final String[] charClasses = {
			"Human Fighter",
			"Warrior",
			"Gladiator",
			"Warlord",
			"Human Knight",
			"Paladin",
			"Dark Avenger",
			"Rogue",
			"Treasure Hunter",
			"Hawkeye",
			"Human Mystic",
			"Human Wizard",
			"Sorceror",
			"Necromancer",
			"Warlock",
			"Cleric",
			"Bishop",
			"Prophet",
			"Elven Fighter",
			"Elven Knight",
			"Temple Knight",
			"Swordsinger",
			"Elven Scout",
			"Plainswalker",
			"Silver Ranger",
			"Elven Mystic",
			"Elven Wizard",
			"Spellsinger",
			"Elemental Summoner",
			"Elven Oracle",
			"Elven Elder",
			"Dark Fighter",
			"Palus Knight",
			"Shillien Knight",
			"Bladedancer",
			"Assassin",
			"Abyss Walker",
			"Phantom Ranger",
			"Dark Elven Mystic",
			"Dark Elven Wizard",
			"Spellhowler",
			"Phantom Summoner",
			"Shillien Oracle",
			"Shillien Elder",
			"Orc Fighter",
			"Orc Raider",
			"Destroyer",
			"Orc Monk",
			"Tyrant",
			"Orc Mystic",
			"Orc Shaman",
			"Overlord",
			"Warcryer",
			"Dwarven Fighter",
			"Dwarven Scavenger",
			"Bounty Hunter",
			"Dwarven Artisan",
			"Warsmith",
			"HumanShaman",
			"HumanOverlord",
			"HumanDominator",
			"OrcWizard",
			"orcNecromancer",
			"orcSoultaker",
			"darkKnight",
			"DdarkAvenger",
			"DhellKnight",
			"dwarvenCleric",
			"dwarvenbishop",
			"dwarvencardinal",
			"dummyEntry13",
			"dummyEntry14",
			"dummyEntry15",
			"dummyEntry16",
			"dummyEntry17",
			"dummyEntry18",
			"dummyEntry19",
			"dummyEntry20",
			"dummyEntry21",
			"dummyEntry22",
			"dummyEntry23",
			"dummyEntry24",
			"dummyEntry25",
			"dummyEntry26",
			"dummyEntry27",
			"dummyEntry28",
			"dummyEntry29",
			"dummyEntry30",
			"Duelist",
			"DreadNought",
			"Phoenix Knight",
			"Hell Knight",
			"Sagittarius",
			"Adventurer",
			"Archmage",
			"Soultaker",
			"Arcana Lord",
			"Cardinal",
			"Hierophant",
			"Eva Templar",
			"Sword Muse",
			"Wind Rider",
			"Moonlight Sentinel",
			"Mystic Muse",
			"Elemental Master",
			"Eva's Saint",
			"Shillien Templar",
			"Spectral Dancer",
			"Ghost Hunter",
			"Ghost Sentinel",
			"Storm Screamer",
			"Spectral Master",
			"Shillien Saint",
			"Titan",
			"Grand Khauatari",
			"Dominator",
			"Doomcryer",
			"Fortune Seeker",
			"Maestro",
			"dummyEntry31",
			"dummyEntry32",
			"dummyEntry33",
			"dummyEntry34",
			"Male Soldier",
			"Female Soldier",
			"Trooper",
			"Warder",
			"Berserker",
			"Male Soulbreaker",
			"Female Soulbreaker",
			"Arbalester",
			"Doombringer",
			"Male Soulhound",
			"Female Soulhound",
			"Trickster",
			"Inspector",
			"Judicator" };

	public static CharTemplateTable getInstance()
	{
		if(_instance == null)
			_instance = new CharTemplateTable();
		return _instance;
	}

	private CharTemplateTable()
	{
		_templates = new TIntObjectHashMap<>();
		Connection con = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		Connection con2 = null;
		PreparedStatement stmt2 = null;
		ResultSet rset2 = null;
		int male = 0;
		int female = 0;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("SELECT * FROM class_list, char_templates WHERE class_list.id = char_templates.classId ORDER BY class_list.id");
			rset = stmt.executeQuery();

			while(rset.next())
			{
				StatsSet set = new StatsSet();
				ClassId classId = ClassId.values()[rset.getInt("class_list.id")];
				set.set("classId", rset.getInt("class_list.id"));
				set.set("className", rset.getString("char_templates.className"));
				set.set("raceId", rset.getByte("char_templates.RaceId"));
				set.set("baseSTR", rset.getByte("char_templates.STR"));
				set.set("baseCON", rset.getByte("char_templates.CON"));
				set.set("baseDEX", rset.getByte("char_templates.DEX"));
				set.set("baseINT", rset.getByte("char_templates.INT"));
				set.set("baseWIT", rset.getByte("char_templates.WIT"));
				set.set("baseMEN", rset.getByte("char_templates.MEN"));
				set.set("baseHpReg", 0.01);
				set.set("baseCpReg", 0.01);
				set.set("baseMpReg", 0.01);
				set.set("basePAtk", rset.getInt("char_templates.p_atk"));
				set.set("basePDef", /* classId.isMage()? 77 : 129 */rset.getInt("char_templates.p_def"));
				set.set("baseMAtk", rset.getInt("char_templates.m_atk"));
				set.set("baseMDef", 41 /* rset.getInt("char_templates.m_def") */);
				set.set("basePAtkSpd", rset.getInt("char_templates.p_spd"));
				set.set("baseMAtkSpd", classId.isMage() ? 166 : 333 /* rset.getInt("char_templates.m_spd") */);
				set.set("baseCritRate", rset.getInt("char_templates.critical"));
				set.set("baseWalkSpd", rset.getInt("char_templates.walk_spd"));
				set.set("baseRunSpd", rset.getInt("char_templates.run_spd"));
				set.set("baseShldDef", 0);
				set.set("baseShldRate", 0);
				set.set("baseAtkRange", 40);
				set.set("safeFall", rset.getInt("char_templates.safeFall"));

				L2PlayerTemplate ct;

				boolean sex = rset.getBoolean("sex");
				set.set("isMale", !sex);
				// set.setMUnk1(rset.getDouble(27));
				// set.setMUnk2(rset.getDouble(28));
				set.set("collision_radius", rset.getDouble("char_templates.col_r"));
				set.set("collision_height", rset.getDouble("char_templates.col_h"));
				ct = new L2PlayerTemplate(set);

				con2 = DatabaseFactory.getInstance().getConnection();
				stmt2 = con2.prepareStatement("SELECT * FROM pc_parameter WHERE class_id = " + rset.getInt("class_list.id") + " ORDER BY 2");
				rset2 = stmt2.executeQuery();
				while(rset2.next())
				{
					ct.baseHp[rset2.getInt("level")] = rset2.getFloat("baseHp");
					ct.baseMp[rset2.getInt("level")] = rset2.getFloat("baseMp");
					ct.baseCp[rset2.getInt("level")] = rset2.getFloat("baseCp");
				}
				DbUtils.closeQuietly(con2, stmt2, rset2);

				_templates.put(sex ? ct.classId.getId() | 0x100 : ct.classId.getId(), ct);

				if(sex)
					female++;
				else
					male++;
			}
		}
		catch(SQLException e)
		{
			_log.warn("error while loading char templates " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, stmt, rset);
		}

		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			Document doc = factory.newDocumentBuilder().parse(Config.SETTINGS_FILE);

			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if("settings".equalsIgnoreCase(n.getNodeName()))
				{
					for(Node s = n.getFirstChild(); s != null; s = s.getNextSibling())
					{
						if("initial_start_points".equals(s.getNodeName()))
						{
							Map<ClassId, GArray<Location>> startPoints = new HashMap<>();
							for(Node ps = s.getFirstChild(); ps != null; ps = ps.getNextSibling())
							{
								if("points".equals(ps.getNodeName()))
								{
									String[] classes = ArrayUtils.toStringArray(XmlUtil.getAttribute(ps, "class"));
									GArray<Location> points = new GArray<>();
									for(Node p = ps.getFirstChild(); p != null; p = p.getNextSibling())
									{
										if("point".equals(p.getNodeName()))
										{
											points.add(Location.parseLoc(XmlUtil.getNodeValue(p)));
										}
									}

									for(String className : classes)
									{
										ClassId classId = ClassId.valueOf(className);
										startPoints.put(classId, points);
									}
								}
							}
							setInitialStartPoints(startPoints);
						}
						else if("initial_equipment".equals(s.getNodeName()) || "initial_custom_equipment".equals(s.getNodeName()))
						{
							Map<ClassId, GArray<StatsSet>> startItems = new HashMap<>();
							for(Node c = s.getFirstChild(); c != null; c = c.getNextSibling())
							{
								if(c.getNodeType() == Node.ELEMENT_NODE)
								{
									ClassId classId = ClassId.valueOf(c.getNodeName());
									GArray<StatsSet> items = new GArray<>();
									int[] itemsArray = ArrayUtils.toIntArray(XmlUtil.getNodeValue(c));
									for(int i = 0; i < itemsArray.length; i += 2)
									{
										StatsSet item = new StatsSet();
										item.set("itemId", itemsArray[i]);
										item.set("count", itemsArray[i + 1]);
										items.add(item);
									}
									startItems.put(classId, items);
								}
							}
							if("initial_equipment".equals(s.getNodeName()))
								setInitialEquipment(startItems);
							else
								setInitialCustomEquipment(startItems);
						}
					}
				}
			}
			_log.info("Data loader: settings loaded.");
		}
		catch(Exception e)
		{
			_log.warn("Data loader: Error while loading data.");
			e.printStackTrace();
		}

		_log.info("CharTemplateTable: Loaded " + male + "/" + female + " Male/Female Character Templates.");
	}

	public L2PlayerTemplate getTemplate(ClassId classId, boolean female)
	{
		return getTemplate(classId.getId(), female);
	}

	public L2PlayerTemplate getTemplate(int classId, boolean female)
	{
		int key = classId;
		if(female)
			key |= 0x100;
		return _templates.get(key);
	}

	public static String getClassNameById(int classId)
	{
		if(classId == 128 || classId == 129)
			return "Soulbreaker";
		else if(classId == 132 || classId == 133)
			return "Soulhound";

		return charClasses[classId];
	}

	public static int getClassIdByName(String className)
	{
		int currId = 1;

		for(String name : charClasses)
		{
			if(name.equalsIgnoreCase(className))
				break;

			currId++;
		}

		return currId;
	}

	public static Location getInitialStartPoint(ClassId classId)
	{
		GArray<Location> points = initialStartPoints.get(classId);
		if(points == null)
		{
			_log.info("PlayerTemplateTable: no initial start points for class: " + classId);
			return null;
		}

		return points.get(Rnd.get(points.size()));
	}

	public static GArray<StatsSet> getInitialEquipment(ClassId classId)
	{
		return initialEquipment.get(classId);
	}

	public static GArray<StatsSet> getInitialCustomEquipment(ClassId classId)
	{
		return initialCustomEquipment.get(classId);
	}

	public static void setInitialStartPoints(Map<ClassId, GArray<Location>> initialStartPoints)
	{
		CharTemplateTable.initialStartPoints = initialStartPoints;
	}

	public static void setInitialEquipment(Map<ClassId, GArray<StatsSet>> initialEquipment)
	{
		CharTemplateTable.initialEquipment = initialEquipment;
	}

	public static void setInitialCustomEquipment(Map<ClassId, GArray<StatsSet>> initialCustomEquipment)
	{
		CharTemplateTable.initialCustomEquipment = initialCustomEquipment;
	}
}
