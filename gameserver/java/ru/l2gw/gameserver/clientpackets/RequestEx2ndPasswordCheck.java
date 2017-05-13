package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.serverpackets.Ex2ndPasswordCheck;

/**
 * @author: rage
 * @date: 24.04.13 21:14
 */
public class RequestEx2ndPasswordCheck extends L2GameClientPacket
{
	@Override
	protected void readImpl() throws Exception
	{
	}

	@Override
	protected void runImpl() throws Exception
	{

		if(!Config.SECOND_AUTH_ENABLED || getClient().getSecondAuthInfo().isAuthorized())
		{
			sendPacket(new Ex2ndPasswordCheck(Ex2ndPasswordCheck.PASSWORD_OK));
			return;
		}

		if(getClient().getSecondAuthInfo().isPasswordSet())
			sendPacket(new Ex2ndPasswordCheck(Ex2ndPasswordCheck.PASSWORD_PROMPT));
		else
			sendPacket(new Ex2ndPasswordCheck(Ex2ndPasswordCheck.PASSWORD_NEW));
	}
}
