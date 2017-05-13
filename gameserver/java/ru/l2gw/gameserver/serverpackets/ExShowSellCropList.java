package ru.l2gw.gameserver.serverpackets;

import javolution.util.FastMap;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.instancemanager.CastleManorManager.CropProcure;
import ru.l2gw.gameserver.model.L2Manor;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

/**
 * format(packet 0xFE)
 * ch dd [ddddcdcdddc]
 * c  - id
 * h  - sub id
 *
 * d  - manor id
 * d  - size
 *
 * [
 * d  - Object id
 * d  - crop id
 * d  - seed level
 * c
 * d  - reward 1 id
 * c
 * d  - reward 2 id
 * d  - manor
 * d  - buy residual
 * d  - buy price
 * d  - reward
 * ]
 *
 */
public class ExShowSellCropList extends L2GameServerPacket
{
	private int _manorId = 1;
	private FastMap<Integer, L2ItemInstance> _cropsItems;
	private FastMap<Integer, CropProcure> _castleCrops;

	public ExShowSellCropList(L2Player player, int manorId, GArray<CropProcure> crops)
	{
		_manorId = manorId;
		_castleCrops = new FastMap<Integer, CropProcure>();
		_cropsItems = new FastMap<Integer, L2ItemInstance>();

		GArray<Integer> allCrops = L2Manor.getInstance().getAllCrops();
		for(int cropId : allCrops)
		{
			L2ItemInstance item = player.getInventory().getItemByItemId(cropId);
			if(item != null)
				_cropsItems.put(cropId, item);
		}

		for(CropProcure crop : crops)
			if(_cropsItems.containsKey(crop.getId()) && crop.getAmount() > 0)
				_castleCrops.put(crop.getId(), crop);

	}

	@Override
	public void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x2C);

		writeD(_manorId); // manor id
		writeD(_cropsItems.size()); // size

		for(L2ItemInstance item : _cropsItems.values())
		{
			writeD(item.getObjectId()); // Object id
			writeD(item.getItemId()); // crop id
			writeD(L2Manor.getInstance().getSeedLevelByCrop(item.getItemId())); // seed level
			writeC(1);
			writeD(L2Manor.getInstance().getRewardItem(item.getItemId(), 1)); // reward 1 id
			writeC(1);
			writeD(L2Manor.getInstance().getRewardItem(item.getItemId(), 2)); // reward 2 id

			if(_castleCrops.containsKey(item.getItemId()))
			{
				CropProcure crop = _castleCrops.get(item.getItemId());
				writeD(_manorId); // manor
				writeQ(crop.getAmount()); // buy residual
				writeQ(crop.getPrice()); // buy price
				writeC(crop.getReward()); // reward
			}
			else
			{
				writeD(0xFFFFFFFF); // manor
				writeQ(0); // buy residual
				writeQ(0); // buy price
				writeC(0); // reward
			}
			writeQ(item.getCount()); // my crops
		}
	}
}