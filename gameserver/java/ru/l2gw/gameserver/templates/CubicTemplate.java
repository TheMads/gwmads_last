package ru.l2gw.gameserver.templates;

import javolution.util.FastList;
import ru.l2gw.gameserver.model.L2Skill;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: rage
 * Date: 18.05.2009
 * Time: 15:38:49
 */
public class CubicTemplate
{
	private final int _id;
	private final int _level;
	private final int _liveTime;
	private final int _power;
	private final int _attackCount;
	private final int _reuseDelay;
	private final int _activateRate;
	private List<L2Skill> _skills;

	public CubicTemplate(int id, int level, int liveTime, int power, int attackCount, int reuseDelay, int activateRate)
	{
		_id = id;
		_level = level;
		_liveTime = liveTime * 1000;
		_power = power;
		_attackCount = attackCount;
		_reuseDelay = reuseDelay * 1000;
		_activateRate = activateRate;
		_skills = new FastList<L2Skill>();
	}

	public int getId()
	{
		return _id;
	}

	public int getLevel()
	{
		return _level;
	}

	public int getLiveTime()
	{
		return _liveTime;
	}

	public int getPower()
	{
		return _power;
	}

	public int getAttackCount()
	{
		return _attackCount;
	}

	public int getReuseDelay()
	{
		return _reuseDelay;
	}

	public int getActivateRate()
	{
		return _activateRate;
	}

	public List<L2Skill> getSkills()
	{
		return _skills;
	}

	public void addSkill(L2Skill skill)
	{
		_skills.add(skill);
	}

	@Override
	public final int hashCode()
	{
		return _id * 300 + _level;
	}
	
	@Override
	public String toString()
	{
		return "CubicTemplate[id=" + _id + ";level=" + _level + ";power=" + _power + "]";
	}
}
