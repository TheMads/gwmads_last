package ru.l2gw.gameserver.tables;

import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.L2SkillLearn;
import ru.l2gw.gameserver.model.base.ClassId;
import ru.l2gw.gameserver.model.base.L2EnchantSkillLearn;
import ru.l2gw.gameserver.serverpackets.SkillList;

import java.sql.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings( { "nls", "unqualified-field-access", "boxing" })
public class SkillTreeTable
{
	public static final int NORMAL_ENCHANT_COST_MULTIPLIER = 1;
	public static final int SAFE_ENCHANT_COST_MULTIPLIER = 3;

	public static final int NORMAL_ENCHANT_BOOK = 6622;
	public static final int SAFE_ENCHANT_BOOK = 9627;
	public static final int CHANGE_ENCHANT_BOOK = 9626;
	public static final int UNTRAIN_ENCHANT_BOOK = 9625;

	public static final int SKILL_TYPE_NORMAL = 0;
	public static final int SKILL_TYPE_FISHING = 1;
	public static final int SKILL_TYPE_CLAN = 2;
	public static final int SKILL_TYPE_CLAN_SUB_PLEDGE = 3;
	public static final int SKILL_TYPE_TRANSFORM = 4;
	public static final int SKILL_TYPE_SUBCLASS = 5;
	public static final int SKILL_TYPE_COLLECTION = 6;
	public static final int SKILL_TYPE_TRANSFER = 8;

	private static final org.apache.commons.logging.Log _log = LogFactory.getLog(SkillTreeTable.class.getName());

	private static final SkillTreeTable _instance = new SkillTreeTable();

	private static Map<ClassId, GArray<L2SkillLearn>> _skillTrees;
	private static Map<ClassId, GArray<L2SkillLearn>> _skillTransferTrees;
	private static Map<Integer, GArray<L2EnchantSkillLearn>> _enchant;
	private static Map<Integer, GArray<L2SkillLearn>> _skillGroups;

	public static SkillTreeTable getInstance()
	{
		return _instance;
	}

	/**
	 * Return the minimum level needed to have this Expertise.<BR><BR>
	 *
	 * @param grade The grade level searched
	 *
	 */
	public static short getExpertiseLevel(int grade)
	{
		if(grade <= 0)
			return 0;

		for(L2SkillLearn sl : _skillTrees.get(ClassId.fighter))
			// TODO: переписать нафиг
			if(sl.getId() == 239 && sl.getLevel() == grade)
				return sl.getMinLevel();

		throw new Error("Expertise not found for grade " + grade);
	}

	public static int getMinSkillLevel(int skillID, ClassId classID, int skillLVL)
	{
		if(skillID > 0 && skillLVL > 0)
			for(L2SkillLearn sl : SkillTreeTable._skillTrees.get(classID))
				if(sl.getLevel() == skillLVL && sl.getId() == skillID)
				{
					return sl.getMinLevel();
				}

		return 0;
	}

	private SkillTreeTable()
	{
		_skillTrees = new HashMap<>();
		_skillTransferTrees = new HashMap<>();
		_skillGroups = new HashMap<>();

		int classintid = 0;
		int count = 0;

		Connection con = null;
		PreparedStatement classliststatement = null;
		PreparedStatement skilltreestatement = null;
		ResultSet classlist = null, skilltree = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			classliststatement = con.prepareStatement("SELECT * FROM class_list ORDER BY id");
			skilltreestatement = con.prepareStatement("SELECT * FROM skill_trees WHERE class_id=? AND group_id = 0 ORDER BY skill_id, level");
			classlist = classliststatement.executeQuery();
			while(classlist.next())
			{
				classintid = classlist.getInt("id");
				ClassId classId = ClassId.values()[classintid];
				GArray<L2SkillLearn> list = new GArray<L2SkillLearn>();

				skilltreestatement.setInt(1, classintid);
				skilltree = skilltreestatement.executeQuery();
				addSkills(skilltree, list);

				_skillTrees.put(ClassId.values()[classintid], list);
				count += list.size();

				ClassId secondparent = classId.getParent((byte) 1);
				if(secondparent == classId.getParent((byte) 0))
					secondparent = null;

				classId = classId.getParent((byte) 0);
				while(classId != null)
				{
					GArray<L2SkillLearn> parentList = _skillTrees.get(classId);
					list.addAll(parentList);
					classId = classId.getParent((byte) 0);
					if(classId == null && secondparent != null)
					{
						classId = secondparent;
						secondparent = secondparent.getParent((byte) 1);
					}
				}

				//_log.info("SkillTreeTable: skill tree for class " + classintid + " has " + list.size() + " skills");
			}
			DbUtils.closeQuietly(classliststatement, classlist);
			classliststatement = null;
			classlist = null;
			DbUtils.closeQuietly(skilltreestatement, skilltree);
			loadOtherSkills(con);
			loadEnchants(con);
		}
		catch(Exception e)
		{
			_log.warn("error while creating skill tree for classId " + classintid, e);
		}
		finally
		{
			DbUtils.closeQuietly(classliststatement, classlist);
			DbUtils.closeQuietly(skilltreestatement, skilltree);
			DbUtils.closeQuietly(con);
		}

		int transCount = 0;
		for(GArray<L2SkillLearn> list : _skillTransferTrees.values())
			transCount += list.size();

		_log.info("SkillTreeTable: Loaded " + count + " skills.");
		_log.info("SkillTreeTable: Loaded " + (_skillGroups.get(SKILL_TYPE_FISHING) == null ? 0 : _skillGroups.get(SKILL_TYPE_FISHING).size()) + " fishing skills.");
		_log.info("SkillTreeTable: Loaded " + (_skillGroups.get(SKILL_TYPE_SUBCLASS) == null ? 0 : _skillGroups.get(SKILL_TYPE_SUBCLASS).size()) + " subclass skills.");
		_log.info("SkillTreeTable: Loaded " + (_skillGroups.get(SKILL_TYPE_TRANSFORM) == null ? 0 : _skillGroups.get(SKILL_TYPE_TRANSFORM).size()) + " transformation skills.");
		_log.info("SkillTreeTable: Loaded " + (_skillGroups.get(SKILL_TYPE_CLAN) == null ? 0 : _skillGroups.get(SKILL_TYPE_CLAN).size()) + " clan skills.");
		_log.info("SkillTreeTable: Loaded " + (_skillGroups.get(SKILL_TYPE_CLAN_SUB_PLEDGE) == null ? 0 : _skillGroups.get(SKILL_TYPE_CLAN_SUB_PLEDGE).size()) + " clan sub pledge skills.");
		_log.info("SkillTreeTable: Loaded " + (_skillGroups.get(SKILL_TYPE_COLLECTION) == null ? 0 : _skillGroups.get(SKILL_TYPE_COLLECTION).size()) + " collection skills.");
		_log.info("SkillTreeTable: Loaded " + transCount + " transfer skills for " + _skillTransferTrees.size() + " classes.");
		_log.info("SkillTreeTable: Loaded " + _enchant.size() + " enchanted skills.");
	}

	private void loadOtherSkills(Connection con) throws SQLException
	{
		PreparedStatement statement = null;
		ResultSet skilltree = null;
		try
		{
			statement = con.prepareStatement("SELECT * FROM skill_trees WHERE group_id > 0 ORDER BY group_id, skill_id, level");
			skilltree = statement.executeQuery();
			addSkills(skilltree, null);
		}
		finally
		{
			DbUtils.closeQuietly(statement, skilltree);
		}
	}

	private void addSkills(ResultSet skilltree, GArray<L2SkillLearn> dest) throws SQLException
	{
		while(skilltree.next())
		{
			int classId = skilltree.getInt("class_id");
			int groupId = skilltree.getInt("group_id");
			short id = skilltree.getShort("skill_id");
			byte lvl = skilltree.getByte("level");
			String name = skilltree.getString("name");

			if(lvl == 1)
			{
				L2Skill s = SkillTable.getInstance().getInfo(id, 1);
			}

			byte minLvl = skilltree.getByte("min_level");
			byte raceId = skilltree.getByte("race_id");
			short itemId = skilltree.getShort("item_id");
			int itemCount = skilltree.getInt("item_count");
			int cost = groupId == 2 || groupId == 3 ? skilltree.getInt("rep") : skilltree.getInt("sp");

			L2SkillLearn skl = new L2SkillLearn(id, lvl, minLvl, name, cost, itemId, itemCount, raceId, groupId);

			if(dest != null)
				dest.add(skl);
			else
			{
				switch(groupId)
				{
					case SKILL_TYPE_TRANSFER:
						GArray<L2SkillLearn> list = _skillTransferTrees.get(ClassId.values()[classId]);
						if(list == null)
						{
							list = new GArray<L2SkillLearn>();
							_skillTransferTrees.put(ClassId.values()[classId], list);
						}
						list.add(skl);
						break;
					default:
						list = _skillGroups.get(groupId);
						if(list == null)
						{
							list = new GArray<L2SkillLearn>();
							_skillGroups.put(groupId, list);
						}
						list.add(skl);
						break;
				}
			}
		}
	}

	private void loadEnchants(Connection con) throws SQLException
	{
		_enchant = new HashMap<>();
		Statement statement = null;
		ResultSet skilltree = null;
		try
		{
			statement = con.createStatement();
			skilltree = statement.executeQuery("SELECT * FROM enchant_skill_trees ORDER BY skill_id, level");
			int cnt = 0;
			while(skilltree.next())
			{
				int id = skilltree.getInt("skill_id");
				int level = skilltree.getInt("level");
				String name = skilltree.getString("name");
				int base = skilltree.getInt("base_lvl");
				String type = skilltree.getString("enchant_type");
				int min = skilltree.getInt("min_skill_lvl");
				L2EnchantSkillLearn e = new L2EnchantSkillLearn(id, level, name, type, min, base);

				GArray<L2EnchantSkillLearn> t = _enchant.get(id);
				if(t == null)
					t = new GArray<L2EnchantSkillLearn>();
				t.add(e);
				_enchant.put(id, t);
				cnt++;
			}

			for(Integer skillId : _enchant.keySet())
			{
				int maxEnchantLevel = getMaxEnchantLevel(skillId);
				for(L2EnchantSkillLearn sl : _enchant.get(skillId))
				{
					sl.setMaxEnchantLevel(maxEnchantLevel);
					try
					{
						SkillTable.getInstance().getInfo(skillId, convertEnchantLevel(sl.getBaseLevel(), sl.getLevel(), maxEnchantLevel)).setDisplayLevel((short) sl.getLevel());
						// 101-130 = base+1-base+30
						// 201-230 = base+31-base+60
						// 301-330 = base+61-base+90
						// 401-430 = base+91-base+120
						// 501-530 = base+121-base+150
						// 601-630 = base+151-base+180
					}
					catch(NullPointerException x)
					{}
				}
			}
			_log.info("SkillTreeTable: Loaded " + cnt + " skills enchants.");
		}
		finally
		{
			DbUtils.closeQuietly(statement, skilltree);
		}
	}

	private int getMaxEnchantLevel(int skillId)
	{
		int maxEnchant = 0;
		for(L2EnchantSkillLearn sl : _enchant.get(skillId))
		{
			if(sl.getLevel() % 100 > maxEnchant)
				maxEnchant = sl.getLevel() % 100;
			else
				break;
		}
		return maxEnchant;
	}

	public void reloadEnchant()
	{
		for(GArray<L2EnchantSkillLearn> t : _enchant.values())
			if(t != null)
				for(L2EnchantSkillLearn e : t)
					try
					{
						SkillTable.getInstance().getInfo(e.getId(), e.getBaseLevel() + e.getLevel() - 100).setDisplayLevel((short) e.getLevel());
					}
					catch(NullPointerException x)
					{}
	}

	public GArray<L2SkillLearn> getAvailableSkills(L2Player player, ClassId classId)
	{
		GArray<L2SkillLearn> result = new GArray<L2SkillLearn>();
		GArray<L2SkillLearn> skills = _skillTrees.get(classId);
		if(skills == null)
		{
			// the skilltree for this class is undefined, so we give an empty list
			_log.warn("Skilltree for class " + classId + " is not defined !");
			return new GArray<L2SkillLearn>(0);
		}

		Collection<L2Skill> oldSkills = player.getAllSkills();
		for(L2SkillLearn temp : skills)
			if(temp.getMinLevel() <= player.getLevel())
			{
				boolean knownSkill = false;
				for(L2Skill s : oldSkills)
				{
					if(knownSkill)
						break;
					if(s.getId() == temp.getId())
					{
						knownSkill = true;
						if(s.getLevel() == temp.getLevel() - 1)
							// this is the next level of a skill that we know
							result.add(temp);
					}
				}
				if(!knownSkill && temp.getLevel() == 1)
					// this is a new skill
					result.add(temp);
			}
		return result;
	}

	public GArray<L2SkillLearn> getAvailableClanSkills(L2Clan clan)
	{
		GArray<L2SkillLearn> result = new GArray<L2SkillLearn>();
		GArray<L2SkillLearn> skills = _skillGroups.get(SKILL_TYPE_CLAN);

		if(skills == null)
			return new GArray<L2SkillLearn>(0);

		L2Skill[] oldSkills = clan.getAllSkills();

		for(L2SkillLearn temp : skills)
			if(temp.getMinLevel() <= clan.getLevel())
			{
				boolean knownSkill = false;

				for(int j = 0; j < oldSkills.length && !knownSkill; j++)
					if(oldSkills[j].getId() == temp.getId())
					{
						knownSkill = true;

						if(oldSkills[j].getLevel() == temp.getLevel() - 1)
							// this is the next level of a skill that we know
							result.add(temp);
					}

				if(!knownSkill && temp.getLevel() == 1)
					// this is a new skill
					result.add(temp);
			}

		return result;
	}

	public GArray<L2SkillLearn> getAvailableSubPledgeSkills(L2Player player)
	{
		GArray<L2SkillLearn> res = new GArray<L2SkillLearn>();
		if(player.getClanId() != 0)
		{
			L2Clan clan = player.getClan();
			GArray<Integer> subPledges = new GArray<Integer>();
			subPledges.add(0);

			for(L2Clan.SubPledge subPledge : clan.getAllSubPledges())
				if(subPledge.getType() != L2Clan.SUBUNIT_ACADEMY)
					subPledges.add(subPledge.getType());

			for(L2SkillLearn sl : _skillGroups.get(SKILL_TYPE_CLAN_SUB_PLEDGE))
				if(sl.getMinLevel() <= clan.getLevel())
					for(int subType : subPledges)
					{
						boolean knownSkill = false;
						GArray<L2Skill> skills = clan.getSubPledgeSkills(subType);
						if(skills != null)
							for(L2Skill skill : skills)
								if(skill.getId() == sl.getId())
								{
									knownSkill = true;
									if(skill.getLevel() + 1 == sl.getLevel() && !res.contains(sl))
									{
										res.add(sl);
										break;
									}
								}

						if(!knownSkill && sl.getLevel() == 1 && !res.contains(sl))
							res.add(sl);
					}
		}
		return res;
	}

	public GArray<L2SkillLearn> getAvailableTransferSkills(L2Player player)
	{
		return _skillTransferTrees.get(player.getClassId());
	}

	public boolean isSubPledgeSkill(int skillId)
	{
		for(L2SkillLearn sl : _skillGroups.get(SKILL_TYPE_CLAN_SUB_PLEDGE))
			if(sl.getId() == skillId)
				return true;

		return false;
	}

	// Определяем минимальный уровень скила который можно точить
	public int getMinSkillLevelToEnchant(int id)
	{
		int minLvl = 0;
		for(L2EnchantSkillLearn e : _enchant.get(id))
		{
			if(minLvl == 0 || minLvl > e.getMinSkillLevel())
				minLvl = e.getMinSkillLevel();
		}
		return minLvl;
	}

	public static GArray<L2EnchantSkillLearn> getFirstEnchantsForSkill(int skillid)
	{
		GArray<L2EnchantSkillLearn> result = new GArray<L2EnchantSkillLearn>();

		GArray<L2EnchantSkillLearn> enchants = _enchant.get(skillid);
		if(enchants == null)
			return result;

		for(L2EnchantSkillLearn e : enchants)
			if(e.getLevel() % 100 == 1)
				result.add(e);

		return result;
	}

	public static int isEnchantable(L2Skill skill)
	{
		GArray<L2EnchantSkillLearn> enchants = _enchant.get(skill.getId());
		if(enchants == null)
			return 0;

		for(L2EnchantSkillLearn e : enchants)
			if(e.getBaseLevel() <= skill.getLevel())
				return 1;

		return 0;
	}

	public static GArray<L2EnchantSkillLearn> getEnchantsForChange(int skillid, int level)
	{
		GArray<L2EnchantSkillLearn> result = new GArray<L2EnchantSkillLearn>();

		GArray<L2EnchantSkillLearn> enchants = _enchant.get(skillid);
		if(enchants == null)
			return result;

		for(L2EnchantSkillLearn e : enchants)
			if(e.getLevel() % 100 == level % 100)
				result.add(e);

		return result;
	}

	public static L2EnchantSkillLearn getSkillEnchant(int skillid, int level)
	{
		GArray<L2EnchantSkillLearn> enchants = _enchant.get(skillid);
		if(enchants == null)
			return null;

		for(L2EnchantSkillLearn e : enchants)
			if(e.getLevel() == level)
				return e;
		return null;
	}

	/**
	 * Преобразует уровень скила из клиентского представления в серверное
	 * @param baseLevel базовый уровень скила - максимально возможный без заточки
	 * @param level - текущий уровень скила
	 * @return уровень скила
	 */
	public static int convertEnchantLevel(int baseLevel, int level, int maxEnchant)
	{
		if(level < 100)
			return level;
		return baseLevel + ((level - level % 100) / 100 - 1) * maxEnchant + level % 100;
	}

	public static L2SkillLearn getSkillLearn(int skillid, short level, ClassId classid, L2Clan clan)
	{
		return getSkillLearn(skillid, level, classid, clan, null);
	}
	
	public static L2SkillLearn getSkillLearn(int skillid, short level, ClassId classid, L2Clan clan, L2Player player)
	{
		if(clan != null)
		{
			GArray<L2SkillLearn> clskills = getInstance().getAvailableClanSkills(clan);
			for(L2SkillLearn tmp : clskills)
				if(tmp.getId() == skillid && tmp.getLevel() == level)
					return tmp;

			if(player != null)
				for(L2SkillLearn sl : getInstance().getAvailableSubPledgeSkills(player))
					if(sl.getId() == skillid && sl.getLevel() == level)
						return sl;
		}
		else
		{
			if(_skillGroups.get(SKILL_TYPE_FISHING) != null)
				for(L2SkillLearn tmp : _skillGroups.get(SKILL_TYPE_FISHING))
					if(tmp.getId() == skillid && tmp.getLevel() == level)
						return tmp;

			if(_skillGroups.get(SKILL_TYPE_TRANSFORM) != null)
				for(L2SkillLearn tmp : _skillGroups.get(SKILL_TYPE_TRANSFORM))
					if(tmp.getId() == skillid && tmp.getLevel() == level)
						return tmp;

			if(_skillGroups.get(SKILL_TYPE_SUBCLASS) != null)
				for(L2SkillLearn tmp : _skillGroups.get(SKILL_TYPE_SUBCLASS))
					if(tmp.getId() == skillid && tmp.getLevel() == level)
						return tmp;

			if(_skillGroups.get(SKILL_TYPE_CLAN_SUB_PLEDGE) != null)
				for(L2SkillLearn tmp : _skillGroups.get(SKILL_TYPE_CLAN_SUB_PLEDGE))
					if(tmp.getId() == skillid && tmp.getLevel() == level)
						return tmp;

			if(_skillGroups.get(SKILL_TYPE_COLLECTION) != null)
				for(L2SkillLearn tmp : _skillGroups.get(SKILL_TYPE_COLLECTION))
					if(tmp.getId() == skillid && tmp.getLevel() == level)
						return tmp;

			GArray<L2SkillLearn> list = _skillTransferTrees.get(classid);
			if(list != null)
				for(L2SkillLearn sl : list)
					if(sl.getId() == skillid && sl.getLevel() == level)
						return sl;

			//		for(ArrayList<L2SkillLearn> tmp1 : _skillTrees.get(classid))
			for(L2SkillLearn tmp : _skillTrees.get(classid))
				if(tmp.getId() == skillid && tmp.getLevel() == level)
					return tmp;
		}
		return null;
	}

	public GArray<L2SkillLearn> getAvailableSubclassSkills(L2Player player)
	{
		GArray<L2SkillLearn> result = new GArray<L2SkillLearn>();
		if(_skillGroups.get(SKILL_TYPE_SUBCLASS) == null)
		{
			_log.warn("Subclass skills not defined!");
			return result;
		}

		Collection<L2Skill> oldSkills = player.getAllSkills();

		for(L2SkillLearn temp : _skillGroups.get(SKILL_TYPE_SUBCLASS))
			if(temp.getMinLevel() <= player.getLevel())
			{
				boolean knownSkill = false;
				for(L2Skill s : oldSkills)
				{
					if(knownSkill)
						break;
					if(s.getId() == temp.getId())
					{
						knownSkill = true;
						if(s.getLevel() == temp.getLevel() - 1)
							result.add(temp);
					}
				}

				if(!knownSkill && temp.getLevel() == 1)
					result.add(temp);
			}
		return result;
	}

	public GArray<L2SkillLearn> getAvailableTransformationSkills(L2Player player)
	{
		GArray<L2SkillLearn> result = new GArray<L2SkillLearn>();
		if(_skillGroups.get(SKILL_TYPE_TRANSFORM) == null)
		{
			_log.warn("Transformation skills not defined!");
			return result;
		}

		Collection<L2Skill> oldSkills = player.getAllSkills();

		for(L2SkillLearn temp : _skillGroups.get(SKILL_TYPE_TRANSFORM))
			if(temp.getMinLevel() <= player.getLevel())
			{
				if(temp.getRaceId() >= 0 && temp.getRaceId() != player.getRace().ordinal())
					continue;

				boolean knownSkill = false;
				for(L2Skill s : oldSkills)
				{
					if(knownSkill)
						break;
					if(s.getId() == temp.getId())
					{
						knownSkill = true;
						if(s.getLevel() == temp.getLevel() - 1)
							result.add(temp);
					}
				}

				if(!knownSkill && temp.getLevel() == 1)
					result.add(temp);
			}
		return result;
	}

	public GArray<L2SkillLearn> getAvailableFishingSkills(L2Player player)
	{
		GArray<L2SkillLearn> result = new GArray<L2SkillLearn>();
		if(_skillGroups.get(SKILL_TYPE_FISHING) == null)
		{
			_log.warn("Fishing skills not defined!");
			return result;
		}

		Collection<L2Skill> oldSkills = player.getAllSkills();

		for(L2SkillLearn temp : _skillGroups.get(SKILL_TYPE_FISHING))
			if(temp.getMinLevel() <= player.getLevel())
			{
				boolean knownSkill = false;
				for(L2Skill s : oldSkills)
				{
					if(knownSkill)
						break;
					if(s.getId() == temp.getId())
					{
						knownSkill = true;
						if(s.getLevel() == temp.getLevel() - 1)
							result.add(temp);
					}
				}

				if(!knownSkill && temp.getLevel() == 1)
					result.add(temp);
			}
		return result;
	}

	public GArray<L2SkillLearn> getAvailableCollectionSkills(L2Player player)
	{
		GArray<L2SkillLearn> result = new GArray<L2SkillLearn>();
		if(_skillGroups.get(SKILL_TYPE_COLLECTION) == null)
		{
			_log.warn("Collection skills not defined!");
			return result;
		}

		Collection<L2Skill> oldSkills = player.getAllSkills();

		for(L2SkillLearn temp : _skillGroups.get(SKILL_TYPE_COLLECTION))
			if(temp.getMinLevel() <= player.getLevel())
			{
				boolean knownSkill = false;
				for(L2Skill s : oldSkills)
				{
					if(knownSkill)
						break;
					if(s.getId() == temp.getId())
					{
						knownSkill = true;
						if(s.getLevel() == temp.getLevel() - 1)
							result.add(temp);
					}
				}

				if(!knownSkill && temp.getLevel() == 1)
					result.add(temp);
			}
		return result;
	}

	public Collection<L2SkillLearn> getAcquirableSkillListByClass(L2Player player)
	{
		Map<Short, L2SkillLearn> result = new HashMap<>();
		Collection<L2Skill> skills = player.getAllSkills();
		GArray<L2SkillLearn> skillsTree = _skillTrees.get(player.getClassId());
		
		boolean find;
		for(L2SkillLearn temp : skillsTree) //Открываем цикл по всем скилам для данного класса
		{
			if(temp.getMinLevel() <= player.getLevel()) //если уровень необходимый для изучения скила ниже или равен уровню игрока
			{
				if(result.get(temp.getId()) == null) //если скила небыло в result
				{
					find = false;
					for(L2Skill s : skills) //открываем цикл по всем скилам игрока
					{
						if(s == null)
						{
							_log.warn(player + " WTF!! has null skill!");
							continue;
						}
						if(temp.getId() == s.getId()) // если скилл который можно изучить уже есть у игрока
						{
							find = true;
							if(temp.getLevel() > s.getLevel()) //если уровень скила для изучения выше чем у игрока
							{
								if(!result.containsKey(temp.getId())) {
									result.put(temp.getId(), temp);
								}
							}
						}
					}
					if(!find) //если этого скила небыло у игрока в изученых - то просто добавляем его
						result.put(temp.getId(), temp);
				}
				//если скил с этим skillId уже есть в списке
				//else if(result.get(temp.getId()).getLevel() < temp.getLevel()) // проверяем не нужно ли изменить ему уровень
				//	result.put(temp.getId(), temp); //обновляем запись
			}
			
			if(temp.getMinLevel() > player.getLevel())
			{
					if(!result.containsKey(temp.getId())) {
						result.put(temp.getId(), temp);
					}
			}
		}
		
		
		//if(skillsTree == null)
		//{
		//	// the skilltree for this class is undefined, so we give an empty list
		//	_log.warn("Skilltree for class " + player.getClassId() + " is not defined !");
		//	return result.values();
		//}
        //
		//for(L2SkillLearn temp : skillsTree) //Открываем цикл по всем скилам для данного класса
		//{
		//	if(temp.getMinLevel() == level) //если уровень необходимый для изучения скила ниже или равен уровню игрока
		//		if(result.get(temp.getId()) == null) //если скила небыло в result
		//			result.put(temp.getId(), temp);
		//}
		
		//if(skillsTree == null)
		//{
		//	// the skilltree for this class is undefined, so we give an empty list
		//	_log.warn("Skilltree for class " + player.getClassId() + " is not defined !");
		//	result = new HashMap<>();
		//	return result.values();
		//}
		//boolean find = false;
		//for(L2SkillLearn temp : skillsTree) //Открываем цикл по всем скилам для данного класса
		//{
		//	if(temp.getMinLevel() <= player.getLevel()) //если уровень необходимый для изучения скила ниже или равен уровню игрока
		//	{
		//			for(L2Skill s : skills) //открываем цикл по всем скилам игрока
		//			{
		//				if(temp.getId() == s.getId()) // если скилл который можно изучить уже есть у игрока
		//				{
		//					find = true;
		//					if(temp.getLevel() > s.getLevel()) //если уровень скила для изучения выше чем у игрока
		//					{
		//						result.put(temp.getId(), temp);
		//					}
		//				}
		//			}
		//			if(!find) //если этого скила небыло у игрока в изученых - то просто добавляем его
		//				result.put(temp.getId(), temp);
		//		//если скил с этим skillId уже есть в списке
		//		//if(result.get(temp.getId()).getLevel() < temp.getLevel()) // проверяем не нужно ли изменить ему уровень
		//		//	result.put(temp.getId(), temp); //обновляем запись
		//	}
		//	else
		//	{
		//		if(result.get(temp.getId()) == null) //если скила небыло в result
		//		{
		//			//if(temp.getLevel() > s.getLevel()) //если уровень скила для изучения выше чем у игрока
		//			//{
		//				//result.put(temp.getId(), temp);
		//			//}
		//		}
		//		//если скил с этим skillId уже есть в списке
		//		//if(result.get(temp.getId()).getLevel() < temp.getLevel()) // проверяем не нужно ли изменить ему уровень
		//		//	result.put(temp.getId(), temp); //обновляем запись
		//	}
		//}
		return result.values();
	}
	
	public Map<Short, L2SkillLearn> getMaxEnableLevelsForSkillsAtLevel(L2Player player, ClassId classId)
	{
		Map<Short, L2SkillLearn> result = new HashMap<>();
		Collection<L2Skill> skills = player.getAllSkills();
		GArray<L2SkillLearn> skillsTree = _skillTrees.get(classId);

		boolean find;
		for(L2SkillLearn temp : skillsTree) //Открываем цикл по всем скилам для данного класса
		{
			if(temp.getMinLevel() <= player.getLevel()) //если уровень необходимый для изучения скила ниже или равен уровню игрока
			{
				if(result.get(temp.getId()) == null) //если скила небыло в result
				{
					find = false;
					for(L2Skill s : skills) //открываем цикл по всем скилам игрока
					{
						if(s == null)
						{
							_log.warn(player + " WTF!! has null skill!");
							continue;
						}
						if(temp.getId() == s.getId()) // если скилл который можно изучить уже есть у игрока
						{
							find = true;
							if(temp.getLevel() > s.getLevel()) //если уровень скила для изучения выше чем у игрока
							{
								result.put(temp.getId(), temp);
							}
						}
					}
					if(!find) //если этого скила небыло у игрока в изученых - то просто добавляем его
						result.put(temp.getId(), temp);
				}
				//если скил с этим skillId уже есть в списке
				else if(result.get(temp.getId()).getLevel() < temp.getLevel()) // проверяем не нужно ли изменить ему уровень
					result.put(temp.getId(), temp); //обновляем запись
			}
		}
		
		return result;
	}

	public Map<Short, L2SkillLearn> getSkillsAtCertainLevel(ClassId classId, int level)
	{
		Map<Short, L2SkillLearn> result = new HashMap<>();
		GArray<L2SkillLearn> skillsTree = _skillTrees.get(classId);

		if(skillsTree == null)
		{
			// the skilltree for this class is undefined, so we give an empty list
			_log.warn("Skilltree for class " + classId + " is not defined !");
			return new HashMap<>(0);
		}

		for(L2SkillLearn temp : skillsTree) //Открываем цикл по всем скилам для данного класса
		{
			if(temp.getMinLevel() == level) //если уровень необходимый для изучения скила ниже или равен уровню игрока
				if(result.get(temp.getId()) == null) //если скила небыло в result
					result.put(temp.getId(), temp);
		}
		return result;
	}

	public byte getMinLevelForNewSkill(L2Player player, ClassId classId)
	{
		byte minlevel = 0;
		GArray<L2SkillLearn> skills = _skillTrees.get(classId);
		if(skills == null)
		{
			// the skilltree for this class is undefined, so we give an empty list
			_log.warn("Skilltree for class " + classId + " is not defined !");
			return minlevel;
		}

		for(L2SkillLearn temp : skills)
			if(temp.getMinLevel() > player.getLevel())
				if(minlevel == 0 || temp.getMinLevel() < minlevel)
					minlevel = temp.getMinLevel();
		return minlevel;
	}

	/**
	 * Возвращает true если скилл может быть изучен данным классом
	 * @param player
	 * @param skillid
	 * @param level
	 * @return true/false
	 */
	public boolean isSkillPossible(L2Player player, int skillid, int level)
	{
		for(L2SkillLearn tmp : _skillGroups.get(SKILL_TYPE_CLAN))
			if(tmp.getId() == skillid && tmp.getLevel() <= level)
				return true;

		for(L2SkillLearn tmp : _skillGroups.get(SKILL_TYPE_CLAN_SUB_PLEDGE))
			if(tmp.getId() == skillid && tmp.getLevel() <= level)
				return true;

		GArray<L2SkillLearn> skills = _skillTrees.get(ClassId.values()[player.getActiveClass()]);
		if(skills == null)
		{
			// the skilltree for this class is undefined, so we give an empty list!
			_log.warn("Skilltree for class " + player.getActiveClass() + " is not defined ! [isSkillPossible]");
			return false;
		}
		for(L2SkillLearn skillLearn : skills)
			if(skillLearn.getId() == skillid && skillLearn.getLevel() <= level)
				return true;

		skills = _skillTransferTrees.get(ClassId.values()[player.getActiveClass()]);
		if(skills != null)
			for(L2SkillLearn skillLearn : skills)
				if(skillLearn.getId() == skillid && skillLearn.getLevel() <= level)
					return true;

		return false;
	}

	public void deleteSubclassSkills(L2Player player)
	{
		GArray<Integer> skills = new GArray<Integer>();

		for(L2SkillLearn sl : _skillGroups.get(SKILL_TYPE_SUBCLASS))
			if(!skills.contains((int)sl.getId()))
				skills.add((int)sl.getId());

		for(Integer skillId : skills)
			player.removeSkill(SkillTable.getInstance().getInfo(skillId, 1), true);

		player.sendPacket(new SkillList(player));
	}
}