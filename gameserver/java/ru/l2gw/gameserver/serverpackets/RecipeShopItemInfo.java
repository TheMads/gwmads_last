package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2ManufactureItem;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;

/**
 * ddddd
 */
public class RecipeShopItemInfo extends L2GameServerPacket
{
	private int _recipeId, _shopId, curMp, maxMp;
	private int _success;
	private boolean can_writeImpl = false;
	private long _price;

	public RecipeShopItemInfo(int shopId, int recipeId, int success)
	{
		_recipeId = recipeId;
		_shopId = shopId;
		_success = success;
	}

	@Override
	final public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2Object object = player.getVisibleObject(_shopId);

		if(object == null || !object.isPlayer())
			return;

		L2Player crafter = (L2Player) object;

		if(crafter.getPrivateStoreType() != L2Player.STORE_PRIVATE_MANUFACTURE || !crafter.isInRange(player, crafter.getInteractDistance(player)))
			return;

		curMp = (int) crafter.getCurrentMp();
		maxMp = crafter.getMaxMp();

		for(L2ManufactureItem i : crafter.getCreateList().getList())
			if(i.getRecipeId() == _recipeId)
			{
				can_writeImpl = true;
				_price = i.getCost();
			}
	}

	@Override
	protected final void writeImpl()
	{
		if(!can_writeImpl)
			return;

		writeC(0xE0);
		writeD(_shopId);
		writeD(_recipeId);
		writeD(curMp);
		writeD(maxMp);
		writeD(_success);
		writeQ(_price);
	}
}