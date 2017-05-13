package ru.l2gw.extensions.ccpGuard.managers;

public class L2HwidBan
{
	private final String HWID;

	public L2HwidBan(String hwid)
	{
		HWID = hwid;
	}

	public String getHwid()
	{
		return HWID;
	}
}