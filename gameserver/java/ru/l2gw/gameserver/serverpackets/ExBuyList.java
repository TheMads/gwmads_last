package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.NpcTradeList;
import ru.l2gw.gameserver.model.TradeItem;

public class ExBuyList extends AbstractItemPacket
{
	private int _listId;
	private GArray<TradeItem> _list;
	private long _money;
	private double _TaxRate = 0;

	public ExBuyList(NpcTradeList list, L2Player player)
	{
		_listId = list.getListId();
		_list = list.getTradeItems();
		_money = player.getAdena();
		player.setBuyListId(_listId);
	}

	public ExBuyList(NpcTradeList list, L2Player player, double taxRate)
	{
		_listId = list.getListId();
		_list = list.getTradeItems();
		_money = player.getAdena();
		_TaxRate = taxRate;
		player.setBuyListId(_listId);
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xB8);
		writeD(0x00);
		writeQ(_money); // current money
		writeD(_listId);
		writeD(0x00); //TODO [Bonux] Awakening

		writeH(_list.size());

		for(TradeItem item : _list)
		{
			if(item.getLimitCount() > 0)
			{
				if(item.getLimitResetTime() > 0 && item.getLimitResetTime() < System.currentTimeMillis())
				{
					item.setLimitResetTime(0);
					item.setCount(item.getLimitCount());
				}
				else if(item.getCount() < 1)
					continue;
			}

			writeItemInfo(item);
			writeQ((long) (item.getOwnersPrice() * (1 + _TaxRate)));
		}
	}
}