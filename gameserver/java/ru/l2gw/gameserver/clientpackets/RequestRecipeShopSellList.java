package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.serverpackets.RecipeShopSellList;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

/**
 * Возврат к списку из информации о рецепте
 */
public class RequestRecipeShopSellList extends L2GameClientPacket
{
	int _objectId;

	@Override
	public void readImpl()
	{
		_objectId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		L2Player trader = L2ObjectsStorage.getPlayer(_objectId);

		if(player == null || trader == null)
			return;

		if(AdminTemplateManager.checkBoolean("noPrivateStore", player))
		{
			player.sendPacket(new SystemMessage(SystemMessage.THIS_ACCOUNT_CANOT_USE_PRIVATE_STORES));
			return;
		}

		if(trader.isPlayer())
			player.sendPacket(new RecipeShopSellList(player, trader));
		else
			player.sendActionFailed();
	}
}