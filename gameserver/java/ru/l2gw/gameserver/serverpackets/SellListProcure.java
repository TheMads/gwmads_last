package ru.l2gw.gameserver.serverpackets;

import javolution.util.FastMap;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.instancemanager.CastleManorManager.CropProcure;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

import java.util.Map;

@SuppressWarnings("unused")
public class SellListProcure extends L2GameServerPacket
{
	private long _money;
	private Map<L2ItemInstance, Long> _sellList = new FastMap<L2ItemInstance, Long>();
	private GArray<CropProcure> _procureList = new GArray<CropProcure>();
	private int _castle;

	public SellListProcure(L2Player player, int castleId)
	{
		_money = player.getAdena();
		_castle = castleId;
		_procureList = ResidenceManager.getInstance().getCastleById(_castle).getCropProcure(0);
		for(CropProcure c : _procureList)
		{
			L2ItemInstance item = player.getInventory().getItemByItemId(c.getId());
			if(item != null && c.getAmount() > 0)
				_sellList.put(item, c.getAmount());
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xEF);
		writeQ(_money); // money
		writeD(0x00); // lease ?
		writeH(_sellList.size()); // list size

		for(L2ItemInstance item : _sellList.keySet())
		{
			writeH(item.getItem().getType1());
			writeD(item.getObjectId());
			writeD(item.getItemId());
			writeQ(_sellList.get(item)); // count
			writeH(item.getItem().getType2());
			writeH(0); // unknown
			writeQ(0); // price, u shouldnt get any adena for crops, only raw materials
		}
	}
}
