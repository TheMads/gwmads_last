package ru.l2gw.loginserver.clientpackets;

import ru.l2gw.loginserver.Config;
import ru.l2gw.loginserver.LoginController;
import ru.l2gw.loginserver.SessionKey;
import ru.l2gw.loginserver.serverpackets.LoginFail.LoginFailReason;
import ru.l2gw.loginserver.serverpackets.PlayOk;

/**
 * Fromat is ddc
 * d: first part of session id
 * d: second part of session id
 * c: server ID
 */
public class RequestServerLogin extends L2LoginClientPacket
{
	private int _skey1;
	private int _skey2;
	private int _serverId;

	public int getSessionKey1()
	{
		return _skey1;
	}

	public int getSessionKey2()
	{
		return _skey2;
	}

	public int getServerID()
	{
		return _serverId;
	}

	@Override
	public boolean readImpl()
	{
		if(getAvaliableBytes() >= 9)
		{
			_skey1 = readD();
			_skey2 = readD();
			_serverId = readC();
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
		SessionKey sk = getClient().getSessionKey();

		// if we didnt showed the license we cant check these values
		if(!Config.SHOW_LICENCE || sk.checkLoginPair(_skey1, _skey2))
		{
			if(Config.ANTI_BRUTE_LOGIN && getClient().getLoginState() != LoginController.State.VALID)
				getClient().close(LoginFailReason.REASON_USER_OR_PASS_WRONG);
			else if(LoginController.getInstance().isLoginPossible(getClient(), _serverId))
				getClient().sendPacket(new PlayOk(sk));
			else
				getClient().close(LoginFailReason.REASON_ACCESS_FAILED);
		}
		else
			getClient().close(LoginFailReason.REASON_ACCESS_FAILED);
	}
}