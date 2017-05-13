package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.controllers.RecipeController;
import ru.l2gw.gameserver.model.L2ManufactureItem;
import ru.l2gw.gameserver.model.L2Player;

public class RequestRecipeShopMakeDo extends L2GameClientPacket
{
	private int _id;
	private int _recipeId;
	@SuppressWarnings("unused")
	private long _price;

	/**
	 * format:		cddd
	 */
	@Override
	public void readImpl()
	{
		_id = readD();
		_recipeId = readD();
		_price = readQ();
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

		L2Player manufacturer = (L2Player) player.getVisibleObject(_id);
		if(manufacturer == null || manufacturer.getPrivateStoreType() != L2Player.STORE_PRIVATE_MANUFACTURE || !manufacturer.isInRange(player, manufacturer.getInteractDistance(player)))
			return;

		for(L2ManufactureItem i : manufacturer.getCreateList().getList())
			if(i.getRecipeId() == _recipeId)
				if(_price != i.getCost())
				{
					player.sendActionFailed();
					return;
				}
				else
					break;

		RecipeController.requestManufactureItem(manufacturer, player, _recipeId);
	}
}