package ru.l2gw.gameserver.loginservercon.lspackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.loginservercon.AttLS;
import ru.l2gw.gameserver.loginservercon.KickWaitingClientTask;
import ru.l2gw.gameserver.loginservercon.LSConnection;
import ru.l2gw.gameserver.loginservercon.SessionKey;
import ru.l2gw.gameserver.loginservercon.gspackets.PlayerInGame;
import ru.l2gw.gameserver.loginservercon.gspackets.PlayerLogout;
import ru.l2gw.gameserver.network.GameClient;
import ru.l2gw.gameserver.serverpackets.CharacterSelectionInfo;
import ru.l2gw.gameserver.serverpackets.LoginFail;

public class PlayerAuthResponse extends LoginServerBasePacket
{
	private static final Log log = LogFactory.getLog(PlayerAuthResponse.class.getName());

	public PlayerAuthResponse(byte[] decrypt, AttLS loginserver)
	{
		super(decrypt, loginserver);
	}

	@Override
	public void read()
	{
		String account = readS();
		boolean authed = readC() == 1;
		int playOkId1 = readD();
		int playOkId2 = readD();
		int loginOkId1 = readD();
		int loginOkId2 = readD();
		int premiumExpire = readD();
		String allowdIps = readS();
		int accountId = readD();
		String secondPass = readS();
		int failCount = readD();
		boolean secondUse = readC() == 0x01;
		
		GameClient client = getLoginServer().getCon().removeWaitingClient(account);

		if(client != null)
		{
			if(client.getState() != GameClient.GameClientState.CONNECTED)
			{
				log.warn("Trying to authd allready authed client.");
				client.closeNow(true);
				return;
			}

			SessionKey key = client.getSessionId();

			if(authed)
				if(getLoginServer().isLicenseShown())
					authed = key.playOkID1 == playOkId1 && key.playOkID2 == playOkId2 && key.loginOkID1 == loginOkId1 && key.loginOkID2 == loginOkId2;
				else
					authed = key.playOkID1 == playOkId1 && key.playOkID2 == playOkId2;

			if(authed)
			{
				client.setState(GameClient.GameClientState.AUTHED);
				client.setPremiumExpire(premiumExpire * 1000L);
				client.setAllowdIps(allowdIps);
				client.setAccountId(accountId);
				getLoginServer().getCon().addAccountInGame(client);
				CharacterSelectionInfo csi = new CharacterSelectionInfo(client.getLoginName(), client.getSessionId().playOkID1);
				client.sendPacket(csi);
				client.setCharSelection(csi.getCharInfo());
				client.getSecondAuthInfo().setPasswordHash(secondPass);
				client.getSecondAuthInfo().setFailCount(failCount);
				client.getSecondAuthInfo().setAuthorized(!secondUse);
				sendPacket(new PlayerInGame(client.getLoginName()));
			}
			else
			{
				if(playOkId1 != 0)
					log.warn("Cheater? SessionKey invalid! Login: " + client.getLoginName() + ", IP: " + client.getIpAddr());
				client.sendPacket(new LoginFail(LoginFail.INCORRECT_ACCOUNT_INFO_CONTACT_CUSTOMER_SUPPORT));
				ThreadPoolManager.getInstance().scheduleGeneral(new KickWaitingClientTask(client), 1000);
				LSConnection.getInstance().sendPacket(new PlayerLogout(client.getLoginName()));
				LSConnection.getInstance().removeAccount(client);
			}
		}
	}
}