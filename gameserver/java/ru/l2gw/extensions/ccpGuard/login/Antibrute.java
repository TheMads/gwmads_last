package ru.l2gw.extensions.ccpGuard.login;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.extensions.ccpGuard.ConfigProtect;
import ru.l2gw.extensions.ccpGuard.login.crypt.ProtectionCrypt;

public class Antibrute
{
	protected static Log _log = LogFactory.getLog(Antibrute.class.getName());

	public static void init()
	{
		ConfigProtect.load();
		ProtectionCrypt.loadProtectData();
		_log.info("LoginServer: Anti brute " + (ConfigProtect.PROTECT_LOGIN_ANTIBRUTE ? "enabled" : "disabled."));
	}

	public static int getProtocol()
	{
		return ConfigProtect.PROTECT_LOGIN_PROTOCOL_VERSION;
	}

	public static byte[] cryptKey(int vector, byte[] pubKey)
	{
		byte[] result;

		if(ConfigProtect.PROTECT_LOGIN_ANTIBRUTE)
		{
			result = new byte[pubKey.length];
			ProtectionCrypt cryptor = new ProtectionCrypt();
			cryptor.setModKey(vector);
			cryptor.doCrypt(pubKey, 0, result, 0, pubKey.length);
			return result;
		}

		return pubKey;
	}
}