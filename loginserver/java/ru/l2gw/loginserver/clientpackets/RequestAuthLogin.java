package ru.l2gw.loginserver.clientpackets;

import ru.l2gw.extensions.ccpGuard.ConfigProtect;
import ru.l2gw.loginserver.Config;
import ru.l2gw.loginserver.L2LoginClient;
import ru.l2gw.loginserver.L2LoginClient.LoginClientState;
import ru.l2gw.loginserver.LoginController;
import ru.l2gw.loginserver.LoginController.State;
import ru.l2gw.loginserver.LoginController.Status;
import ru.l2gw.loginserver.gameservercon.GameServerInfo;
import ru.l2gw.loginserver.serverpackets.AccountKicked;
import ru.l2gw.loginserver.serverpackets.AccountKicked.AccountKickedReason;
import ru.l2gw.loginserver.serverpackets.LoginFail.LoginFailReason;
import ru.l2gw.loginserver.serverpackets.LoginOk;
import ru.l2gw.loginserver.serverpackets.ServerList;

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;

/**
 * Format: x
 * 0 (a leading null)
 * x: the rsa encrypted block with the login an password
 */
public class RequestAuthLogin extends L2LoginClientPacket
{
	private byte[] _raw = new byte[128];

	private String _user;
	private String _password;
	private int _ncotp;

	public String getPassword()
	{
		return _password;
	}

	public String getUser()
	{
		return _user;
	}

	public int getOneTimePassword()
	{
		return _ncotp;
	}

	@Override
	public boolean readImpl()
	{
		if(getAvaliableBytes() >= 128)
		{
			readB(_raw);
			return true;
		}
		return false;
	}

	@Override
	public void runImpl()
	{
		L2LoginClient client = getClient();

		byte[] decrypted;
		try
		{
			Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
			rsaCipher.init(Cipher.DECRYPT_MODE, client.getRSAPrivateKey());
			decrypted = rsaCipher.doFinal(_raw, 0x00, 0x80);
		}
		catch(GeneralSecurityException e)
		{
			if(!ConfigProtect.PROTECT_LOGIN_ANTIBRUTE)
				e.printStackTrace();
			return;
		}

		_user = new String(decrypted, 0x5E, 14).trim();
		_user = _user.toLowerCase();
		_password = new String(decrypted, 0x6C, 16).trim();
		_ncotp = decrypted[0x7c];
		_ncotp |= decrypted[0x7d] << 8;
		_ncotp |= decrypted[0x7e] << 16;
		_ncotp |= decrypted[0x7f] << 24;

		LoginController lc = LoginController.getInstance();

		Status status = lc.tryAuthLogin(_user, _password, client);
		client.setLoginState(status._state);

		if(Config.ANTI_BRUTE_LOGIN && status._state == State.WRONG)
			status.setState(State.VALID);

		if(status._state == State.VALID)
		{
			client.setAccount(_user);
			client.setState(LoginClientState.AUTHED_LOGIN);
			client.setSessionKey(lc.assignSessionKeyToClient());
			// already added in lc.tryAuthLogin
			//lc.addAuthedLoginClient(_user, client);
			client.setPremiumExpire(status._premiumExpire);
			client.setAllowedIps(status._allowIps);

			if(Config.SHOW_LICENCE)
				client.sendPacket(new LoginOk(client.getSessionKey()));
			else
				client.sendPacket(new ServerList(client));
		}
		else if(status._state == State.WRONG)
			client.close(LoginFailReason.REASON_USER_OR_PASS_WRONG);
		else if(status._state == State.BANNED)
			client.close(new AccountKicked(AccountKickedReason.REASON_PERMANENTLY_BANNED));
		else if(status._state == State.IP_ACCESS_DENIED)
			client.close(LoginFailReason.REASON_ATTEMPTED_RESTRICTED_IP);
		else if(status._state == State.TEMP_PASSWORD)
			client.close(LoginFailReason.REASON_CHANGE_TEMP_PASS);
		else if(status._state == State.IN_USE)
		{
			GameServerInfo gsi = lc.getAccountOnGameServer(_user);

			// кикаем другого клиента из игры
			if(gsi != null && gsi.isAuthed())
				gsi.getGameServer().kickPlayer(_user);
			else if(lc.isAccountInLoginServer(_user))
				lc.removeAuthedLoginClient(_user).close(LoginFailReason.REASON_ACCOUNT_IN_USE);

			client.close(LoginFailReason.REASON_ACCOUNT_IN_USE);
		}
	}
}