package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;

/**
 * @author: rage
 * @date: 24.04.13 21:21
 */
public class RequestEx2ndPasswordVerify extends L2GameClientPacket
{
	private String _password;

	@Override
	protected void readImpl() throws Exception
	{
		_password = readS();
	}

	@Override
	protected void runImpl() throws Exception
	{
		if(!Config.SECOND_AUTH_ENABLED)
			return;

		getClient().getSecondAuthInfo().tryAuth(_password);
	}
}
