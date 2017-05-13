package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.extensions.ccpGuard.ConfigProtect;
import ru.l2gw.extensions.ccpGuard.Protection;
import ru.l2gw.gameserver.loginservercon.LSConnection;
import ru.l2gw.gameserver.loginservercon.SessionKey;
import ru.l2gw.gameserver.network.GameClient;

/**
 * This class ...
 *
 * @version $Revision: 1.9.2.3.2.4 $ $Date: 2005/03/27 15:29:30 $
 */
public class AuthLogin extends L2GameClientPacket
{
	//Format: cSddddd

	// loginName + keys must match what the loginserver used.
	private String _loginName;
	private int _playKey1;
	private int _playKey2;
	private int _loginKey1;
	private int _loginKey2;
	private byte[] _data;

	@Override
	public void readImpl()
	{
		_loginName = readS().toLowerCase();
		_playKey2 = readD();
		_playKey1 = readD();
		_loginKey1 = readD();
		_loginKey2 = readD();

		if(ConfigProtect.PROTECT_ENABLE && _client._prot_info.protect_used)
			try
			{
				_data = new byte[48];
				_client.setLoginName(_loginName);
				Protection.doReadAuthLogin(_client, _buf, _data);
			}
			catch(Exception e)
			{
				_log.info("Filed read AuthLogin! May be BOT or unprotected client! Client IP: " + _client.getIpAddr());
				_client.closeNow(true);
				return;
			}
		// ignore the rest
		_buf.clear();
	}

	@Override
	public void runImpl()
	{
		if(ConfigProtect.PROTECT_ENABLE && _client._prot_info.protect_used && !Protection.doAuthLogin(getClient(), _data, _loginName))
			return;

		SessionKey key = new SessionKey(_loginKey1, _loginKey2, _playKey1, _playKey2);

		final GameClient client = getClient();
		client.setSessionId(key);
		client.setLoginName(_loginName);
		LSConnection.getInstance().addWaitingClient(client);
	}
}
