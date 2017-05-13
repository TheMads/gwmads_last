package ru.l2gw.gameserver.tables;

import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.skills.SkillsEngine;

@SuppressWarnings({"nls", "unqualified-field-access", "boxing"})
public class SkillTable
{
	private static SkillTable _instance = new SkillTable();
	private static final Log _log = LogFactory.getLog(SkillTable.class);

	private TIntObjectHashMap<L2Skill> _skills;
	private boolean _initialized = true;

	private static L2Skill[] _nobleSkills = {
			SkillTable.getInstance().getInfo(1323, 1),
			SkillTable.getInstance().getInfo(325, 1),
			SkillTable.getInstance().getInfo(326, 1),
			SkillTable.getInstance().getInfo(327, 1),
			SkillTable.getInstance().getInfo(1324, 1),
			SkillTable.getInstance().getInfo(1325, 1),
			SkillTable.getInstance().getInfo(1326, 1),
			SkillTable.getInstance().getInfo(1327, 1)
	};

	public static SkillTable getInstance()
	{
		return _instance;
	}

	private SkillTable()
	{
		_skills = new TIntObjectHashMap<>();
		SkillsEngine.getInstance().loadAllSkills(_skills);
	}

	public void reload()
	{
		_instance = new SkillTable();
		SkillTreeTable.getInstance().reloadEnchant();
	}

	public boolean isInitialized()
	{
		return _initialized;
	}

	public L2Skill getInfo(int skillId, int level)
	{
		return _skills.get(getSkillIndex(skillId, level));
	}

	public L2Skill getInfo(int skillIndex)
	{
		return _skills.get(skillIndex);
	}

	public int getMaxLevel(int magicId, int level)
	{
		while(level < 700)
			if(_skills.get(getSkillIndex(magicId, ++level)) == null)
				return level - 1;

		return level;
	}

	/**
	 * Centralized method for easier change of the hashing sys
	 *
	 * @param skillId	The Skill Id
	 * @param skillLevel The Skill Level
	 * @return The Skill hash number
	 */
	public static int getSkillIndex(int skillId, int skillLevel)
	{
		return (skillId << 16) | skillLevel;
	}

	public static void giveNobleSkills(L2Player player)
	{
		for(L2Skill skill : _nobleSkills)
			player.addSkill(skill);
	}

	public static L2Skill parseSkillInfo(String skillInfo)
	{
		String[] skillStr = skillInfo.split("-");
		if(skillStr != null && skillStr.length == 2)
		{
			int skillId = Integer.parseInt(skillStr[0]);
			int skillLvl = Integer.parseInt(skillStr[1]);
			if(skillId > 0 && skillLvl > 0)
				return SkillTable.getInstance().getInfo(skillId, skillLvl);
		}

		return null;
	}

	public static int getAbnormalLevel(L2Character target, int skillIndex)
	{
		return getAbnormalLevel(target, getInstance().getInfo(skillIndex));
	}

	public static int getAbnormalLevel(L2Character target, L2Skill skill)
	{
		if(target == null || skill == null)
			return -1;

		for(String abnormal : skill.getAbnormalTypes())
		{
			L2Effect effect = target.getEffectByAbnormalType(abnormal);
			if(effect != null && effect.getSkill().getAbnormalTypes().contains(abnormal))
				return effect.getAbnormalLevel();
		}

		return -1;
	}

	public static boolean isAbnormalTypeMatch(L2Skill skill1, int skillIndex)
	{
		return isAbnormalTypeMatch(skill1, getInstance().getInfo(skillIndex));
	}

	public static boolean isAbnormalTypeMatch(L2Skill skill1, L2Skill skill2)
	{
		if(skill1 == null || skill2 == null)
			return false;

		for(String at : skill2.getAbnormalTypes())
		{
			if(skill1.getAbnormalTypes().contains(at))
				return true;
		}

		return false;
	}

	public static int isMagic(int skillIndex)
	{
		return isMagic(getInstance().getInfo(skillIndex));
	}

	public static int isMagic(L2Skill skill)
	{
		if(skill == null)
			return -1;

		return skill.getMagicType();
	}

	public static int mpConsume(int skillIndex)
	{
		L2Skill skill = getInstance().getInfo(skillIndex);
		if(skill == null)
			return 0;

		return skill.getMpConsume();
	}

	public static int hpConsume(int skillIndex)
	{
		L2Skill skill = getInstance().getInfo(skillIndex);
		if(skill == null)
			return 0;

		return skill.getHpConsume();
	}

	public static boolean inReuseDelay(L2Character cha, int skillIndex)
	{
		if(cha == null)
			return false;

		L2Skill skill = SkillTable.getInstance().getInfo(skillIndex);
		return skill != null && cha.isSkillDisabled(skill.getId());
	}
}