package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.CrestCache;
import ru.l2gw.gameserver.serverpackets.AllianceCrest;

public class RequestAllyCrest extends L2GameClientPacket
{
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

		byte[] data = CrestCache.getAllyCrest(_crestId);
		if(data != null)
		{
			sendPacket(new AllianceCrest(_crestId, data));
		}
	}
}