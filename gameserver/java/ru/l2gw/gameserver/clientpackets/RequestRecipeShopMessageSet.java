package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestRecipeShopMessageSet extends L2GameClientPacket
{
	// format: cS
	private String _name;

	@Override
	public void readImpl()
	{
		_name = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(player.isInDuel())
		{
			player.sendActionFailed();
			return;
		}

		if(AdminTemplateManager.checkBoolean("noPrivateStore", player))
		{
			player.sendPacket(new SystemMessage(SystemMessage.THIS_ACCOUNT_CANOT_USE_PRIVATE_STORES));
			return;
		}

		if(player.getCreateList() != null)
			player.getCreateList().setStoreName(_name);
	}
}