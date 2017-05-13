package ru.l2gw.gameserver.clientpackets;

import org.apache.commons.lang3.StringUtils;
import ru.l2gw.extensions.ccpGuard.ConfigProtect;
import ru.l2gw.extensions.ccpGuard.Protection;
import ru.l2gw.extensions.ccpGuard.managers.ProtectManager;
import ru.l2gw.extensions.ccpGuard.packets.L2ExtHost;
import ru.l2gw.extensions.ccpGuard.packets.ProtectTitle;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.network.GameClient;
import ru.l2gw.gameserver.network.GameClient.GameClientState;
import ru.l2gw.gameserver.serverpackets.CharSelected;
import ru.l2gw.gameserver.serverpackets.Ex2ndPasswordCheck;
import ru.l2gw.gameserver.serverpackets.SSQInfo;
import ru.l2gw.util.AutoBan;

public class CharacterSelected extends L2GameClientPacket
{
	private int _charSlot, f, x, y, z;

	/**
	 * @param decrypt
	 * Format: cdhddd
	 */
	@Override
	public void readImpl()
	{
		_charSlot = readD();
	}

	@Override
	public void runImpl()
	{
		GameClient client = getClient();

		if(client.charLoaded || client.getPlayer() != null)
			return;

		L2Player player = client.loadCharFromDisk(_charSlot);
		if(player == null)
			return;

		if(Config.SECOND_AUTH_ENABLED && !client.getSecondAuthInfo().isAuthorized())
		{
			if(StringUtils.isBlank(getClient().getSecondAuthInfo().getPasswordHash()))
				sendPacket(new Ex2ndPasswordCheck(Ex2ndPasswordCheck.PASSWORD_NEW));
			else
				sendPacket(new Ex2ndPasswordCheck(Ex2ndPasswordCheck.PASSWORD_PROMPT));

			return;
		}

		if(f > 0 && player.isGM())
			player.setXYZInvisible(x, y, z);

		if(AutoBan.isBanned(player.getObjectId()))
			return;

		if(!Protection.checkPlayerWithHWID(client, player.getObjectId(), player.getName()))
			return;

		if(player.getAccessLevel() < 0)
			player.setAccessLevel(0);

		client.setState(GameClientState.IN_GAME);

		sendPacket(new SSQInfo());

		sendPacket(new CharSelected(player, client.getSessionId().playOkID1));

		if(client._prot_info.protect_used)
		{
			if(!ConfigProtect.PROTECT_SERVER_TITLE.isEmpty())
				sendPacket(new ProtectTitle());
			sendPacket(new L2ExtHost(ProtectManager.getInstance().getL2ExtIp(), 0));
		}
	}
}
