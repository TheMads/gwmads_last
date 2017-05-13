package ru.l2gw.gameserver.templates;

import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.extensions.scripts.Script;
import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.gameserver.model.L2Drop;
import ru.l2gw.gameserver.model.L2DropData;
import ru.l2gw.gameserver.model.L2MinionData;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.base.ClassId;
import ru.l2gw.gameserver.model.quest.Quest;
import ru.l2gw.gameserver.tables.SkillTable;

import java.util.HashMap;
import java.util.Map;

/**
 * This cl contains all generic data of a L2Spawn object.<BR><BR>
 *
 * <B><U> Data</U> :</B><BR><BR>
 * <li>npcId, type, name, sex</li>
 * <li>revardExp, revardSp</li>
 * <li>aggroRange, factionId, factionRange</li>
 * <li>rhand, lhand, armor</li>
 * <li>_drops</li>
 * <li>_minions</li>
 * <li>_teachInfo</li>
 * <li>_skills</li>
 * <li>_questsStart</li><BR><BR>
 */
public class L2NpcTemplate extends L2CharTemplate
{
	private static final Log _log = LogFactory.getLog(L2NpcTemplate.class.getName());

	public static enum ShotsType
	{
		NONE,
		SOUL,
		SPIRIT,
		BSPIRIT,
		SOUL_SPIRIT,
		SOUL_BSPIRIT
	}

	public final int npcId;
	public String type;
	public String ai_type;
	public final String name;
	public String title;
	public final String sex;
	public final byte level;
	public final int revardExp;
	public final int revardSp;
	public final short aggroRange;
	public final int rhand;
	public final int lhand;
	public final int armor;
	public final String factionId;
	public final short factionRange;
	public int displayId = 0;
	public boolean isDropHerbs = false;
	public final ShotsType shots;
	public final boolean isRaid;
	private StatsSet _AIParams = null;
	public final String ignoreClanList;
	public final int soulshotCount;
	public final int spiritshotCount;
	public final float hp_mod;
	public final int undying;
	public final int can_be_attacked;
	public final int can_move;
	public final int flying;
	public final int targetable;
	public final int show_name_tag;
	public final int unsowing;

	/** fixed skills*/
	private short race = 0;

	/** The object containing all Item that can be dropped by L2NpcInstance using this L2NpcTemplate*/
	private L2Drop _drop = null;

	/** The table containing all Minions that must be spawn with the L2NpcInstance using this L2NpcTemplate*/
	private final GArray<L2MinionData> _minions = new GArray<>(0);

	private GArray<ClassId> _teachInfo;
	protected HashMap<Integer, L2Skill> _skills;
	private HashMap<String, L2Skill[]> _skillsByType;
	private final static HashMap<Integer, L2Skill> _emptySkills = new HashMap<Integer, L2Skill>(0);
	private Map<Quest.QuestEventType, Quest[]> _questEvents;

	/**
	 * Constructor of L2Character.<BR><BR>
	 * @param set The StatsSet object to transfer data to the method
	 */
	@SuppressWarnings("unchecked")
	public L2NpcTemplate(StatsSet set, StatsSet AIParams)
	{
		super(set);
		npcId = set.getInteger("npcId");
		displayId = set.getInteger("displayId");
		type = set.getString("type");
		ai_type = set.getString("ai_type");
		name = set.getString("name");
		title = set.getString("title");
		sex = set.getString("sex");
		level = set.getByte("level");
		revardExp = set.getInteger("revardExp");
		revardSp = set.getInteger("revardSp");
		aggroRange = set.getShort("aggroRange");
		rhand = set.getInteger("rhand");
		lhand = set.getInteger("lhand");
		armor = set.getInteger("armor");
		String f = set.getString("factionId", null);
		if(f == null)
			factionId = "";
		else
			factionId = f.intern();
		factionRange = set.getShort("factionRange");
		_teachInfo = null;
		isDropHerbs = set.getBool("isDropHerbs");
		shots = set.getEnum("shots", ShotsType.class, ShotsType.NONE);
		_AIParams = AIParams;
		ignoreClanList = set.getString("ignore_clan_list", null);
		spiritshotCount = set.getInteger("spiritshot_count", 0);
		soulshotCount = set.getInteger("soulshot_count", 0);
		hp_mod = set.getInteger("hp_mod", 1);
		undying = set.getInteger("undying", 0);
		can_be_attacked  = set.getInteger("can_be_attacked", 1);
		can_move = set.getInteger("can_move", 1);
		flying = set.getInteger("flying", 0);
		targetable = set.getInteger("targetable", 1);
		show_name_tag = set.getInteger("show_name_tag", 1);
		unsowing = set.getInteger("unsowing", 0);

		Class<?> _this = null;
		try
		{
			_this = Class.forName("ru.l2gw.gameserver.model.instances." + type + "Instance");
		}
		catch(ClassNotFoundException e)
		{
			Script sc = Scripts.getInstance().getClasses().get("npc.model.instances." + type + "Instance");
			if(sc != null)
				_this = sc.getRawClass();
		}
		isRaid = _this != null && raidboss.isAssignableFrom(_this);
	}

	private static Class<?> raidboss = null;
	static
	{
		try
		{
			raidboss = Class.forName("ru.l2gw.gameserver.model.instances.L2RaidBossInstance");
		}
		catch(ClassNotFoundException e)
		{}
	}

	public L2NpcTemplate(StatsSet set)
	{
		this(set, null);
	}

	public void addTeachInfo(ClassId classId)
	{
		if(_teachInfo == null)
			_teachInfo = new GArray<ClassId>();
		_teachInfo.add(classId);
	}

	public GArray<ClassId> getTeachInfo()
	{
		return _teachInfo;
	}

	public boolean canTeach(ClassId classId)
	{
		return _teachInfo != null && _teachInfo.contains(classId);
	}

	public void addDropData(L2DropData drop, int groupChance, int dropType)
	{
		if(_drop == null)
			_drop = new L2Drop();
		_drop.addData(drop, groupChance, dropType);
	}

	public void addRaidData(L2MinionData minion)
	{
		_minions.add(minion);
	}

	public void addSkill(L2Skill skill, String type)
	{
		if(_skills == null)
			_skills = new HashMap<Integer, L2Skill>();

		if(_skillsByType == null)
			_skillsByType = new HashMap<String, L2Skill[]>();

		if(Config.DEBUG && skill.getId() != 5044 && skill.isActive() && (Math.abs(skill.getMagicLevel() - level) > 9 || skill.getMagicLevel() > level))
		{
			int diff = Integer.MAX_VALUE;
			int lvl = 1;
			int nlvl = 0;
			int mlvl = 0;
			L2Skill ns;
			while((ns = SkillTable.getInstance().getInfo(skill.getId(), lvl)) != null)
			{
				if(ns.getMagicLevel() > 0 && Math.abs(ns.getMagicLevel() - level) < diff && ns.getMagicLevel() <= level)
				{
					diff = Math.abs(ns.getMagicLevel() - level);
					nlvl = ns.getLevel();
					mlvl = ns.getMagicLevel();
				}
				lvl++;
			}

			if(!type.equalsIgnoreCase("L2Pet") && !type.equalsIgnoreCase("L2PetBaby") && !type.equalsIgnoreCase("L2Summon"))
				_log.warn(name + ";" + npcId + ";" + level + ";" + skill.getName() + ";" + skill.getId() + ";" + skill.getLevel() + ";" + skill.getMagicLevel() + ";" + nlvl + ";" + mlvl);

			if(!type.equalsIgnoreCase("L2Pet") && !type.equalsIgnoreCase("L2PetBaby") && !type.equalsIgnoreCase("L2Summon") && nlvl > 0 && nlvl != skill.getLevel())
			{
				String stmt = "UPDATE npcskills SET level = " + nlvl + " WHERE npcid = " + npcId + " and skillId = " + skill.getId() + "; -- old " + skill.getLevel() + "/" + skill.getMagicLevel() + " new " + nlvl + "/" + mlvl + " npc " + level;
				System.out.println(stmt);
			}
		}

		_skills.put(skill.getId(), skill);

		L2Skill[] skilllist;
		if(_skillsByType.get(type) != null)
		{
			skilllist = new L2Skill[_skillsByType.get(type).length + 1];
			System.arraycopy(_skillsByType.get(type), 0, skilllist, 0, _skillsByType.get(type).length);
		}
		else
			skilllist = new L2Skill[1];

		skilllist[skilllist.length - 1] = skill;

		if(skill.getSkillType() != L2Skill.SkillType.NOTDONE && skill.isActive())
			_skillsByType.put(type, skilllist);
	}

	public HashMap<String, L2Skill[]> getAllSkillsByType()
	{
		return _skillsByType;
	}

	public L2Skill[] getSkillsByType(String type)
	{
		if(_skillsByType == null)
			return new L2Skill[0];

		L2Skill[] ret = _skillsByType.get(type);

		return ret != null ? ret : new L2Skill[0];
	}

	/**
	 * Return the list of all possible drops of this L2NpcTemplate.<BR><BR>
	 */
	public L2Drop getDropData()
	{
		return _drop;
	}

	/**
	 * Обнуляет дроплист моба
	 */
	public void clearDropData()
	{
		_drop = null;
	}

	/**
	 * Return the list of all Minions that must be spawn with the L2NpcInstance using this L2NpcTemplate.<BR><BR>
	 */
	public GArray<L2MinionData> getMinionData()
	{
		return _minions;
	}

	public HashMap<Integer, L2Skill> getSkills()
	{
		return _skills == null ? _emptySkills : _skills;
	}

	public void addQuestEvent(Quest.QuestEventType EventType, Quest q)
	{
		if(_questEvents == null)
			_questEvents = new FastMap<Quest.QuestEventType, Quest[]>();

		if(_questEvents.get(EventType) == null)
			_questEvents.put(EventType, new Quest[] { q });
		else
		{
			Quest[] _quests = _questEvents.get(EventType);
			int len = _quests.length;

			// if only one registration per npc is allowed for this event type
			// then only register this NPC if not already registered for the specified event.
			// if a quest allows multiple registrations, then register regardless of count
			if(EventType.isMultipleRegistrationAllowed() || len < 1)
			{
				// be ready to add a new quest to a new copy of the list, with larger size than previously.
				Quest[] tmp = new Quest[len + 1];
				// loop through the existing quests and copy them to the new list.  While doing so, also  
				// check if this new quest happens to be just a replacement for a previously loaded quest.
				// Replace existing if the new quest is the same (reload) or a child of the existing quest.
				// Do nothing if the new quest is a superclass of an existing quest.
				// Add the new quest in the end of the list otherwise.
				for(int i = 0; i < len; i++)
				{
					if(_quests[i].getName().equals(q.getName()) || L2NpcTemplate.isAssignableTo(q, _quests[i].getClass()))
					{
						_quests[i] = q;
						return;
					}
					else if(L2NpcTemplate.isAssignableTo(_quests[i], q.getClass()))
						return;
					tmp[i] = _quests[i];
				}
				tmp[len] = q;
				_questEvents.put(EventType, tmp);
			}
			else
			{
				// if it is the same quest (i.e. reload) or the existing is a superclass of the new one, replace the existing.
				if(_quests[0].getName().equals(q.getName()) || L2NpcTemplate.isAssignableTo(q, _quests[0].getClass()))
					_quests[0] = q;
				else
					_log.warn("Quest event not allowed in multiple quests. Skipped addition of Event Type \"" + EventType + "\" for NPC \"" + name + "\" and quest \"" + q.getName() + "\".");
			}
		}
	}

	/**
	 * Checks if obj can be assigned to the Class represented by clazz.<br>
	 * This is true if, and only if, obj is the same class represented by clazz,
	 * or a subclass of it or obj implements the interface represented by clazz.
	 *
	 * @param obj
	 * @param clazz
	 * @return
	 */
	public static boolean isAssignableTo(Object obj, Class<?> clazz)
	{
		return L2NpcTemplate.isAssignableTo(obj.getClass(), clazz);
	}

	public static boolean isAssignableTo(Class<?> sub, Class<?> clazz)
	{
		// if clazz represents an interface
		if(clazz.isInterface())
		{
			// check if obj implements the clazz interface
			Class<?>[] interfaces = sub.getInterfaces();
			for(int i = 0; i < interfaces.length; i++)
			{
				if(clazz.getName().equals(interfaces[i].getName()))
				{
					return true;
				}
			}
		}
		else
		{
			do
			{
				if(sub.getName().equals(clazz.getName()))
				{
					return true;
				}

				sub = sub.getSuperclass();
			}
			while(sub != null);
		}

		return false;
	}

	public Quest[] getEventQuests(Quest.QuestEventType EventType)
	{
		if(_questEvents == null)
			return null;
		return _questEvents.get(EventType);
	}

	public short getRace()
	{
		return race;
	}

	public void setRace(short newrace)
	{
		race = newrace;
	}

	public boolean isUndead()
	{
		return race == 1;
	}

	@Override
	public String toString()
	{
		return "Npc template " + name + "[" + npcId + "]";
	}

	@Override
	public int getNpcId()
	{
		return npcId;
	}

	public final StatsSet getAIParams()
	{
		return _AIParams;
	}
}
