package ru.l2gw.gameserver.skills;

public class TimeStamp
{
	private int skillId;
	private long reuse;
	private long endTime;

	public TimeStamp(int skillId, long reuse)
	{
		this(skillId, reuse, System.currentTimeMillis() + reuse);
	}

	public TimeStamp(int skillId, long reuse, long endTime)
	{
		this.skillId = skillId;
		this.reuse = reuse;
		this.endTime = endTime;
	}

	public int getSkillId()
	{
		return skillId;
	}

	public long getReuseTotal()
	{
		if(reuse == 0)
			return getReuseCurrent();
		return reuse;
	}

	public long getReuseCurrent()
	{
		long rt = endTime - System.currentTimeMillis();
		if(rt > 0)
			return rt;
		return 0;
	}

	public long getEndTime()
	{
		return endTime;
	}

	public boolean hasNotPassed()
	{
		return System.currentTimeMillis() < endTime;
	}
}