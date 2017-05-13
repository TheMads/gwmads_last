package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2TradeList;
import ru.l2gw.gameserver.model.TradeItem;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Item;

import java.util.concurrent.ConcurrentLinkedQueue;

public class PrivateStoreManageListSell extends AbstractItemPacket
{
	private int seller_id;
	private long seller_adena;
	private boolean _package = false;
	private ConcurrentLinkedQueue<TradeItem> _sellList;
	private ConcurrentLinkedQueue<TradeItem> _haveList;

	public PrivateStoreManageListSell(L2Player seller, boolean pkg)
	{
		seller_id = seller.getObjectId();
		seller_adena = seller.getAdena();
		_package = pkg;

		// Проверяем список вещей в инвентаре, если вещь остутствует - убираем из списка продажи
		_sellList = new ConcurrentLinkedQueue<TradeItem>();
		for(TradeItem i : seller.getSellList())
		{
			L2ItemInstance inst = seller.getInventory().getItemByObjectId(i.getObjectId());
			if(i.getCount() <= 0 || inst == null)
				continue;
			if(inst.getCount() < i.getCount())
				i.setCount(inst.getCount());
			_sellList.add(i);
		}

		L2TradeList _list = new L2TradeList(0);
		// Строим список вещей, годных для продажи имеющихся в инвентаре
		for(L2ItemInstance item : seller.getInventory().getItemsList())
			if(item.getItemId() == 5575 || !item.isEquipped() && item.getItem().getType2() != L2Item.TYPE2_QUEST && item.getItem().getType2() != L2Item.TYPE2_MONEY && item.canBeTraded(seller))
				_list.addItem(item);

		_haveList = new ConcurrentLinkedQueue<TradeItem>();

		// Делаем список для собственно передачи с учетом количества
		for(L2ItemInstance item : _list.getItems())
		{
			TradeItem ti = new TradeItem(item);
			ti.setCount(item.getCount());
			_haveList.add(ti);
		}

		//Убираем совпадения между списками, в сумме оба списка должны совпадать с содержимым инвентаря
		if(_sellList.size() > 0)
			for(TradeItem itemOnSell : _sellList)
			{
				_haveList.remove(itemOnSell);
				boolean added = false;
				for(TradeItem itemInInv : _haveList)
					if(itemInInv.getObjectId() == itemOnSell.getObjectId())
					{
						added = true;
						itemOnSell.setCount(Math.min(itemOnSell.getCount(), itemInInv.getCount()));
						if(itemOnSell.getCount() == itemInInv.getCount())
							_haveList.remove(itemInInv);
						else if(itemOnSell.getCount() > 0)
							itemInInv.setCount(itemInInv.getCount() - itemOnSell.getCount());
						else
							_sellList.remove(itemOnSell);
						break;
					}
				if(!added)
					_sellList.remove(itemOnSell);
			}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xA0);
		//section 1
		writeD(seller_id);
		writeD(_package ? 1 : 0);
		writeQ(seller_adena);

		//Список имеющихся вещей
		writeD(_haveList.size());
		for(TradeItem temp : _haveList)
		{
			L2Item tempItem = ItemTable.getInstance().getTemplate(temp.getItemId());
			writeItemInfo(temp);
			writeQ(tempItem.getReferencePrice() * 2); //store price
		}

		//Список вещей уже поставленых на продажу
		writeD(_sellList.size());
		for(TradeItem temp : _sellList)
		{
			L2Item tempItem = ItemTable.getInstance().getTemplate(temp.getItemId());
			writeItemInfo(temp);
			writeQ(temp.getOwnersPrice());
			writeQ(tempItem.getReferencePrice() * 2); //store price
		}
	}
}