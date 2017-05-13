package ru.l2gw.loginserver.clientpackets;

import ru.l2gw.loginserver.serverpackets.ServerList;
import ru.l2gw.loginserver.serverpackets.LoginFail.LoginFailReason;

/**
 * Format: ddc
 * d: fist part of session id
 * d: second part of session id
 * c: ?
 */
public class RequestServerList extends L2LoginClientPacket
{
	private int _skey1;
	private int _skey2;
	private int _data3;

	public int getSessionKey1()
	{
		return _skey1;
	}

	public int getSessionKey2()
	{
		return _skey2;
	}

	public int getData3()
	{
		return _data3;
	}

	@Override
	public boolean readImpl()
	{
		if(getAvaliableBytes() >= 8)
		{
			_skey1 = readD(); // loginOk 1
			_skey2 = readD(); // loginOk 2
			return true;
		}
		return false;
	}

	/**
	 * @see ru.l2gw.extensions.network.ReceivablePacket#run()
	 */
	@Override
	public void runImpl()
	{
		if(getClient().getSessionKey().checkLoginPair(_skey1, _skey2))
			getClient().sendPacket(new ServerList(getClient()));
		else
			getClient().close(LoginFailReason.REASON_ACCESS_FAILED);
	}
}