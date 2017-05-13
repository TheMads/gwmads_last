package ru.l2gw.gameserver.network;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.loginservercon.LSConnection;
import ru.l2gw.gameserver.loginservercon.gspackets.ChangeAccessLevel;
import ru.l2gw.gameserver.loginservercon.gspackets.RequestUpdateSecondAuth;
import ru.l2gw.gameserver.loginservercon.gspackets.RequestUpdateSecondFail;
import ru.l2gw.gameserver.serverpackets.Ex2ndPasswordVerify;
import ru.l2gw.util.Util;

import java.util.Date;

/**
 * @author: rage
 * @date: 24.04.13 22:21
 */
public class SecondAuthInfo
{
	private static final Log log = LogFactory.getLog("service");
	private boolean authorized;
	private int failCount;
	private String passwordHash;
	private final GameClient gameClient;

	public SecondAuthInfo(GameClient gameClient)
	{
		this.gameClient = gameClient;
	}

	public void tryAuth(String password)
	{
		if(checkPassword(password))
		{
			authorized = true;
			failCount = 0;
			LSConnection.getInstance().sendPacket(new RequestUpdateSecondFail(gameClient.getLoginName(), failCount));
			gameClient.sendPacket(new Ex2ndPasswordVerify(Ex2ndPasswordVerify.PASSWORD_OK, failCount));
		}
		else
		{
			failCount++;
			LSConnection.getInstance().sendPacket(new RequestUpdateSecondFail(gameClient.getLoginName(), failCount));
			log.info("Secondary auth: " + gameClient.getLoginName() + " fail " + failCount + "/" + Config.SECOND_AUTH_LOGIN_TRIES);
			if(failCount >= Config.SECOND_AUTH_LOGIN_TRIES)
			{
				log.info("Secondary auth: " + gameClient.getLoginName() + " banned. Unban time: " + new Date(System.currentTimeMillis() + Config.SECOND_AUTH_BAN_TIME * 1000L));
				LSConnection.getInstance().sendPacket(new ChangeAccessLevel(gameClient.getLoginName(), 0, "Secoundary auth", (int) (System.currentTimeMillis() / 1000) + Config.SECOND_AUTH_BAN_TIME));
				gameClient.sendPacket(new Ex2ndPasswordVerify(Ex2ndPasswordVerify.PASSWORD_BAN, failCount));
			}
			else
				gameClient.sendPacket(new Ex2ndPasswordVerify(Ex2ndPasswordVerify.PASSWORD_WRONG, failCount));
		}
	}

	public void tryChangePassword(String oldPassword, String newPassword)
	{
		if(checkPassword(oldPassword))
		{
			failCount = 0;
			LSConnection.getInstance().sendPacket(new RequestUpdateSecondFail(gameClient.getLoginName(), failCount));
			LSConnection.getInstance().sendPacket(new RequestUpdateSecondAuth(gameClient.getLoginName(), Util.md5(newPassword)));
		}
		else
		{
			failCount++;
			LSConnection.getInstance().sendPacket(new RequestUpdateSecondFail(gameClient.getLoginName(), failCount));
			log.info("Secondary auth: " + gameClient.getLoginName() + " fail " + failCount + "/" + Config.SECOND_AUTH_LOGIN_TRIES);
			if(failCount >= Config.SECOND_AUTH_LOGIN_TRIES)
			{
				log.info("Secondary auth: " + gameClient.getLoginName() + " banned. Unban time: " + new Date(System.currentTimeMillis() + Config.SECOND_AUTH_BAN_TIME * 1000L));
				LSConnection.getInstance().sendPacket(new ChangeAccessLevel(gameClient.getLoginName(), 0, "Secoundary auth", (int) (System.currentTimeMillis() / 1000) + Config.SECOND_AUTH_BAN_TIME));
				gameClient.sendPacket(new Ex2ndPasswordVerify(Ex2ndPasswordVerify.PASSWORD_BAN, failCount));
			}
			else
				gameClient.sendPacket(new Ex2ndPasswordVerify(Ex2ndPasswordVerify.PASSWORD_WRONG, failCount));
		}
	}

	public boolean checkPassword(String password)
	{
		String hash = Util.md5(password);
		return hash != null && hash.equals(passwordHash);
	}

	public boolean isAuthorized()
	{
		return authorized;
	}

	public void setFailCount(int failCount)
	{
		this.failCount = failCount;
	}

	public String getPasswordHash()
	{
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash)
	{
		this.passwordHash = passwordHash;
	}

	public boolean isPasswordSet()
	{
		return StringUtils.isNotBlank(passwordHash);
	}

	public void setAuthorized(boolean authorized)
	{
		this.authorized = authorized;
	}
}
