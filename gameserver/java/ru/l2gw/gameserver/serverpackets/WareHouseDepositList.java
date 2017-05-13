package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.clientpackets.AbstractEnchantPacket;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.Warehouse.WarehouseType;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.templates.L2Item;

import java.util.ArrayList;

public class WareHouseDepositList extends AbstractItemPacket
{
	private int _whtype;
	private long char_adena;
	private ArrayList<L2ItemInstance> _itemslist = new ArrayList<L2ItemInstance>();
	private int _inventorySlots;

	public WareHouseDepositList(L2Player player, WarehouseType whtype)
	{
		_whtype = whtype.getPacketValue();
		char_adena = player.getAdena();
		AbstractEnchantPacket.checkAndCancelEnchant(player);
		for(L2ItemInstance item : player.getInventory().getItems())
			if(!item.isEquipped() && item.getItem().getType2() != L2Item.TYPE2_QUEST && !item.isActivePetControlItem(player) && item.canBeStored(player, _whtype == 1))
				_itemslist.add(item);
			
		switch(whtype)
		{
			case PRIVATE:
				_inventorySlots = player.getWarehouse().getSize();
				break;
			case FREIGHT:
				_inventorySlots = player.getFreight().getSize();
				break;
			case CLAN:
			case CASTLE:
				_inventorySlots = player.getClan().getWarehouse().getSize();
				break;
			default:
				_inventorySlots = 0;
				break;
				
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x41);
		writeH(_whtype);
		writeQ(char_adena);
		writeH(_inventorySlots);
		writeD(0x00);
		writeH(_itemslist.size());
		for(L2ItemInstance temp : _itemslist)
		{
			writeItemInfo(temp);
			writeD(temp.getObjectId()); // return value for define item (object_id)
		}
	}
}