package ru.l2gw.gameserver.loginservercon.lspackets;

import org.apache.commons.lang3.StringUtils;
import ru.l2gw.gameserver.loginservercon.AttLS;
import ru.l2gw.gameserver.network.GameClient;
import ru.l2gw.gameserver.serverpackets.Ex2ndPasswordAck;

/**
 * @author: rage
 * @date: 25.04.13 0:39
 */
public class ResponseUpdateSecondAuth extends LoginServerBasePacket
{
	public ResponseUpdateSecondAuth(byte[] decrypt, AttLS loginServer)
	{
		super(decrypt, loginServer);
	}

	@Override
	public void read()
	{
		String account = readS();
		String hash = readS();
		GameClient client = getLoginServer().getCon().getAccountInGame(account);
		if(client != null && client.getState() == GameClient.GameClientState.AUTHED)
		{
			if(StringUtils.isNotBlank(hash))
			{
				client.getSecondAuthInfo().setPasswordHash(hash);
				client.sendPacket(new Ex2ndPasswordAck(Ex2ndPasswordAck.SUCCESS));
			}
			else
				client.sendPacket(new Ex2ndPasswordAck(Ex2ndPasswordAck.WRONG_PASSWORD));
		}
	}
}
