package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;

public class RequestItemList extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null || AdminTemplateManager.checkBoolean("noInventory", player) || player.isInventoryDisabled())
			return;
		player.getInventory().sendItemList(true);
		player.sendUserInfo(true);
	}
}