package ru.l2gw.gameserver.serverpackets;

import javolution.util.FastMap;
import ru.l2gw.gameserver.instancemanager.CastleManorManager;
import ru.l2gw.gameserver.instancemanager.CastleManorManager.CropProcure;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.entity.Castle;

/**
 * format(packet 0xFE)
 * ch dd [dddc]
 * c  - id
 * h  - sub id
 *
 * d  - crop id
 * d  - size
 *
 * [
 * d  - manor name
 * d  - buy residual
 * d  - buy price
 * c  - reward type
 * ]
 */
public class ExShowProcureCropDetail extends L2GameServerPacket
{
	private int _cropId;
	private FastMap<Integer, CropProcure> _castleCrops;

	public ExShowProcureCropDetail(int cropId)
	{
		_cropId = cropId;
		_castleCrops = new FastMap<Integer, CropProcure>();

		for(Castle c : ResidenceManager.getInstance().getCastleList())
		{
			CropProcure cropItem = c.getCrop(_cropId, CastleManorManager.PERIOD_CURRENT);
			if(cropItem != null && cropItem.getAmount() > 0)
				_castleCrops.put(c.getId(), cropItem);
		}
	}

	@Override
	public void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x79);

		writeD(_cropId); // crop id
		writeD(_castleCrops.size()); // size

		for(int manorId : _castleCrops.keySet())
		{
			CropProcure crop = _castleCrops.get(manorId);
			writeD(manorId); // manor name
			writeQ(crop.getAmount()); // buy residual
			writeQ(crop.getPrice()); // buy price
			writeC(crop.getReward()); // reward type
		}
	}
}