package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.Stats;

public class ExStorageMaxCount extends L2GameServerPacket
{
	private int _inventory;
	private int _warehouse;
	private int _freight;
	private int _privateSell;
	private int _privateBuy;
	private int _recipeDwarven;
	private int _recipeCommon;
	private int _inventoryExtraSlots;

	public ExStorageMaxCount(L2Player player)
	{
		_inventory = player.getInventoryLimit();
		_warehouse = player.getWarehouseLimit();
		_freight = player.getFreightLimit();
		_privateBuy = _privateSell = player.getTradeLimit();
		_recipeDwarven = player.getDwarvenRecipeLimit();
		_recipeCommon = player.getCommonRecipeLimit();
		_inventoryExtraSlots = (int) player.calcStat(Stats.INVENTORY_LIMIT, 0, null, null);
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x2F);

		writeD(_inventory);
		writeD(_warehouse);
		writeD(_freight);
		writeD(_privateSell);
		writeD(_privateBuy);
		writeD(_recipeDwarven);
		writeD(_recipeCommon);
		writeD(_inventoryExtraSlots); // belt inventory slots increase count
		writeD(Config.INVENTORY_MAXIMUM_QUEST);
		writeD(40); // ??? 40 slots
		writeD(40); // ??? 40 slots
	}
}