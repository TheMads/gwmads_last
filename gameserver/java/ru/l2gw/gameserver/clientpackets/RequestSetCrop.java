package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.instancemanager.CastleManorManager;
import ru.l2gw.gameserver.instancemanager.CastleManorManager.CropProcure;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.entity.Castle;

/**
 * Format: (ch) dd [dddc]
 * d - manor id
 * d - size
 * [
 * d - crop id
 * d - sales
 * d - price
 * c - reward type
 * ]
 *
 */
public class RequestSetCrop extends L2GameClientPacket
{
	private int _size;
	private int _manorId;
	private long[] _items; // _size*4

	@Override
	protected void readImpl()
	{
		_manorId = readD();
		_size = readD();
		if(_size * 21 > _buf.remaining() || _size > Short.MAX_VALUE || _size <= 0)
		{
			_size = 0;
			return;
		}
		_items = new long[_size * 4];
		for(int i = 0; i < _size; i++)
		{
			_items[i * 4 + 0] = readD();
			_items[i * 4 + 1] = readQ();
			_items[i * 4 + 2] = readQ();
			_items[i * 4 + 3] = readC();
			if(_items[i * 4 + 0] < 1 || _items[i * 4 + 1] < 0 || _items[i * 4 + 2] < 0)
			{
				_size = 0;
				return;
			}
		}
	}

	@Override
	protected void runImpl()
	{
		if(_size < 1)
			return;

		GArray<CropProcure> crops = new GArray<CropProcure>();
		for(int i = 0; i < _size; i++)
		{
			int id = (int) _items[i * 4 + 0];
			long sales = _items[i * 4 + 1];
			long price = _items[i * 4 + 2];
			int type = (int)_items[i * 4 + 3];
			if(id > 0)
			{
				CropProcure s = CastleManorManager.getInstance().getNewCropProcure(id, sales, type, price, sales);
				crops.add(s);
			}
		}
		Castle castle = ResidenceManager.getInstance().getCastleById(_manorId);
		if(castle.isCastle)
		{
			castle.setCropProcure(crops, CastleManorManager.PERIOD_NEXT);
			if(Config.MANOR_SAVE_ALL_ACTIONS)
				castle.saveCropData(CastleManorManager.PERIOD_NEXT);
		}
	}
}
