package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.CrestCache;
import ru.l2gw.gameserver.serverpackets.ExPledgeCrestLarge;

public class RequestPledgeCrestLarge extends L2GameClientPacket
{
	// format: chd
	private int _crestId;

	@Override
	public void readImpl()
	{
		_crestId = readD();
	}

	@Override
	public void runImpl()
	{
		if(_crestId == 0)
			return;

		byte[] data = CrestCache.getPledgeCrestLarge(_crestId);
		if(data != null)
		{
			sendPacket(new ExPledgeCrestLarge(_crestId, data));
		}
	}
}
