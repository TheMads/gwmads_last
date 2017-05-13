package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.RecipeShopItemInfo;

/**
 * cdd
 */
public class RequestRecipeShopMakeInfo extends L2GameClientPacket
{
	private int _playerObjectId;
	private int _recipeId;

	@Override
	public void readImpl()
	{
		_playerObjectId = readD();
		_recipeId = readD();
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

		player.sendPacket(new RecipeShopItemInfo(_playerObjectId, _recipeId, -1));
	}
}