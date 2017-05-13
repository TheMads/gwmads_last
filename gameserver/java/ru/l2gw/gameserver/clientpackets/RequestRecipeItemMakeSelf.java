package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.controllers.RecipeController;
import ru.l2gw.gameserver.model.L2Player;

public class RequestRecipeItemMakeSelf extends L2GameClientPacket
{
	private int _id;

	/**
	 * packet type id 0xB8
	 * format:		cd
	 * @param decrypt
	 */
	@Override
	public void readImpl()
	{
		_id = readD();
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

		RecipeController.requestMakeItem(player, _id);
	}
}