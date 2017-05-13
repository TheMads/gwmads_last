package ru.l2gw.gameserver.model.playerSubOrders;

/**
 * @author rage
 * @date 07.07.2010 19:50:09
 */
public final class UserVar
{
	public String name;
	public String value;
	public long expire;

	public UserVar(String name, String value, long expire)
	{
		this.name = name;
		this.value = value;
		this.expire = expire;
	}
}

