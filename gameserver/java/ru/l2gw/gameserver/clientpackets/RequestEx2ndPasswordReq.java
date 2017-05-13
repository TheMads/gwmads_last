package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.loginservercon.LSConnection;
import ru.l2gw.gameserver.loginservercon.gspackets.RequestUpdateSecondAuth;
import ru.l2gw.gameserver.network.GameClient;
import ru.l2gw.gameserver.serverpackets.Ex2ndPasswordAck;
import ru.l2gw.util.Util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: rage
 * @date: 24.04.13 21:19
 */
public class RequestEx2ndPasswordReq extends L2GameClientPacket
{
	private int changePass;
	private String password, newPassword;
	private static final Pattern digits = Pattern.compile("^[0-9]+$");

	@Override
	protected void readImpl() throws Exception
	{
		changePass = readC();
		password = readS();
		if (changePass == 2)
			newPassword = readS();
	}

	@Override
	protected void runImpl() throws Exception
	{
		GameClient client = getClient();

		if(client == null || !Config.SECOND_AUTH_ENABLED)
			return;

		Matcher matcher = digits.matcher(password);
		if(!matcher.matches())
		{
			sendPacket(new Ex2ndPasswordAck(Ex2ndPasswordAck.WRONG_PATTERN));
			return;
		}

		System.out.println("set password: " + changePass + " " + password + " hash: " + getClient().getSecondAuthInfo().getPasswordHash());
		if(changePass == 0 && !client.getSecondAuthInfo().isPasswordSet())
		{
			LSConnection.getInstance().sendPacket(new RequestUpdateSecondAuth(client.getLoginName(), Util.md5(password)));
		}
		else if(changePass == 2)
		{
			client.getSecondAuthInfo().tryChangePassword(password, newPassword);
		}
	}
}
