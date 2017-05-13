package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.controllers.RecipeController;
import ru.l2gw.gameserver.model.L2Player;

public class RequestRecipeBookOpen extends L2GameClientPacket
{
	private int _isCommon = 0;

	/**
	 * format:		cd
	 */
	@Override
	public void readImpl()
	{
		_isCommon = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;
		RecipeController.requestBookOpen(player, _isCommon);
	}
}