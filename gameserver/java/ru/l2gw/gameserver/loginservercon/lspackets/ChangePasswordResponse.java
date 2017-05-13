package ru.l2gw.gameserver.loginservercon.lspackets;

import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.extensions.scripts.Functions;
import ru.l2gw.gameserver.loginservercon.AttLS;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.network.GameClient;

/**
 * @Author: Death
 * @Date: 8/2/2007
 * @Time: 14:39:46
 */
public class ChangePasswordResponse extends LoginServerBasePacket
{
	public ChangePasswordResponse(byte[] decrypt, AttLS loginServer)
	{
		super(decrypt, loginServer);
	}

	@Override
	public void read()
	{
		String account = readS();
		boolean changed = readD() == 1;

		GameClient client = getLoginServer().getCon().getAccountInGame(account);

		if(client == null)
			return;

		L2Player player = client.getPlayer();

		if(player == null)
			return;

		if(changed)
			Functions.show(new CustomMessage("scripts.commands.user.password.ResultTrue", player), player);
		else
			Functions.show(new CustomMessage("scripts.commands.user.password.ResultFalse", player), player);
	}
}