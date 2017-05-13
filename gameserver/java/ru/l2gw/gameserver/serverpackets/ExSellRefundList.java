package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.controllers.TradeController;
import ru.l2gw.gameserver.model.Inventory;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author rage
 * @date 16.06.2010 11:02:36
 */
public class ExSellRefundList extends AbstractItemPacket
{
	private int _done;
	private final GArray<L2ItemInstance> _sellList;
	private final ConcurrentLinkedQueue<L2ItemInstance> _refundList;

	public ExSellRefundList(L2Player player)
	{
		_refundList = player.getInventory().getRefundItemsList();
		_sellList = new GArray<L2ItemInstance>();

		for(L2ItemInstance item : player.getInventory().getItemsList())
			if(!item.isEquipped() && item.canBeSelled(player))
				_sellList.add(item);

		L2ItemInstance[] sorted = new L2ItemInstance[_sellList.size()];
		_sellList.toArray(sorted);
		Arrays.sort(sorted, Inventory.OrderComparator);
		_sellList.clear();
		_sellList.addAll(Arrays.asList(sorted));
	}

	public ExSellRefundList done()
	{
		_done = 1;
		return this;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xB8);
		writeD(0x01);
		writeD(0x00); //TODO [Bonux] Awakening

		if(_sellList == null)
			writeH(0);
		else
		{
			writeH(_sellList.size());
			for(L2ItemInstance item : _sellList)
			{
				writeItemInfo(item);
				long price = TradeController.getInstance().getBuyPrice(item.getItemId());
				writeQ(price >= 0 ? price : item.getReferencePrice() / 2);
			}
		}

		if(_refundList == null)
			writeH(0);
		else
		{
			writeH(_refundList.size());
			int index = 0;
			//hx[ddQhhhhQhhhhhhhh h]
			for(L2ItemInstance item : _refundList)
			{
				writeItemInfo(item);
				writeD(index++);
				long price = TradeController.getInstance().getBuyPrice(item.getItemId());
				writeQ(item.getCount() * (price >= 0 ? price : item.getReferencePrice() / 2));
			}
		}

		writeC(_done);
	}
}
