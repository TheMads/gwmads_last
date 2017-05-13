package ru.l2gw.extensions.ccpGuard.managers;

public class HwidInfo
{
	private String HWID;
	private int count;
	private int playerID;
	private String login;
	private LockType lockType;

	public static enum LockType
	{
		PLAYER_LOCK,
		ACCOUNT_LOCK,
		NONE
	}

	public HwidInfo(String hwid)
	{
		HWID = hwid;
		count = 1;
	}

	public void setHwids(String hwid)
	{
		HWID = hwid;
		count = 1;
	}

	public String getHWID()
	{
		return HWID;
	}

	public void setHWID(String HWID)
	{
		this.HWID = HWID;
	}

	public int getPlayerID()
	{
		return playerID;
	}

	public void setPlayerID(int playerID)
	{
		this.playerID = playerID;
	}

	public String getLogin()
	{
		return login;
	}

	public void setLogin(String login)
	{
		this.login = login;
	}

	public LockType getLockType()
	{
		return lockType;
	}

	public void setLockType(LockType lockType)
	{
		this.lockType = lockType;
	}

	public int getCount()
	{
		return count;
	}

	public void setCount(int count)
	{
		this.count = count;
	}
}