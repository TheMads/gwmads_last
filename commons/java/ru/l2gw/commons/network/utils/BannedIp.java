package ru.l2gw.commons.network.utils;

public class BannedIp
{
	public String ip;
	public String admin;
	public int expireTime = -1;

	@Override
	public boolean equals(Object obj)
	{
		if(!(obj instanceof BannedIp))
			return false;

		BannedIp b = (BannedIp) obj;

		return ip.equals(b.ip) && admin.equals(b.admin);
	}
}
