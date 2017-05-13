package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.clientpackets.AbstractEnchantPacket;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.Warehouse.WarehouseType;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WareHouseWithdrawList extends AbstractItemPacket
{
	public static final int PRIVATE = 1;
	public static final int CLAN = 2;
	public static final int CASTLE = 3;
	public static final int FREIGHT = 4;

	private long _money;
	private ConcurrentLinkedQueue<L2ItemInstance> _items;
	private int _type;
	private int _inventorySlots;
	private boolean can_writeImpl = false;

	public WareHouseWithdrawList(L2Player player, WarehouseType type)
	{
		if(player == null)
			return;

		_money = player.getAdena();
		_type = type.getPacketValue();
		switch(type)
		{
			case PRIVATE:
				_items = player.getWarehouse().getItemsList();
				break;
			case CLAN:
			case CASTLE:
				_items = player.getClan().getWarehouse().getItemsList();
				break;
			/*
			 case CASTLE:
			 items = _player.getClan().getCastleWarehouse().listItems();
			 break;
			 */
			case FREIGHT:
				_items = player.getFreight().getItemsList();
				break;
			default:
				throw new NoSuchElementException("Invalid value of 'type' argument");
		}

		AbstractEnchantPacket.checkAndCancelEnchant(player);

		if(_items.size() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_NOT_DEPOSITED_ANY_ITEMS_IN_YOUR_WAREHOUSE));
			return;
		}
		
		_inventorySlots = player.getInventory().getAllSize();

		can_writeImpl = true;
	}

	@Override
	protected final void writeImpl()
	{
		if(!can_writeImpl)
			return;

		writeC(0x42);
		writeH(_type);
		writeQ(_money);
		writeH(_items.size());
		writeH(0x00);
		if(_type == 1 || _type == 2)
			writeD(_inventorySlots);
		for(L2ItemInstance temp : _items)
		{
			writeItemInfo(temp);
			writeD(temp.getObjectId()); // return value for define item (object_id)
			writeD(0x00); // wtf???
			writeD(0x00); // wtf???
		}
	}
}