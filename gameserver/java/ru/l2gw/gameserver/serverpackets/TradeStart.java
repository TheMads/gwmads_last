package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

import java.util.ArrayList;

//0x2e TradeStart   d h (h dddhh dhhh)
public class TradeStart extends AbstractItemPacket
{
	private ArrayList<L2ItemInstance> _tradelist = new ArrayList<L2ItemInstance>();
	private boolean can_writeImpl = false;
	private int requester_obj_id;

	public TradeStart(L2Player me)
	{
		if(me == null)
			return;

		if(me.getTransactionRequester() == null)
			return;

		requester_obj_id = me.getTransactionRequester().getObjectId();

		L2ItemInstance[] inventory = me.getInventory().getItems();
		for(L2ItemInstance item : inventory)
			if(!item.isEquipped() && item.getItem().getType2() != 3 && item.canBeTraded(me))
				_tradelist.add(item);

		can_writeImpl = true;
	}

	@Override
	protected final void writeImpl()
	{
		if(!can_writeImpl)
			return;

		writeC(0x14);
		writeD(requester_obj_id);
		writeH(_tradelist.size());//count??
		for(L2ItemInstance temp : _tradelist)
			writeItemInfo(temp);
	}
}