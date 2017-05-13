package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.extensions.ccpGuard.Protection;

public class ReplyGameGuardQuery extends L2GameClientPacket
{
	// Format: cdddd
	private byte[] _data = new byte[72];
	private boolean _readOk;

	@Override
	public void readImpl()
	{
		_readOk = Protection.doReadReplyGameGuard(getClient(), _buf, _data);
	}

	@Override
	public void runImpl()
	{
		if(getClient() != null && _readOk)
		{
			getClient().setGameGuardOk(true);
			Protection.doReplyGameGuard(_client, _data);
		}
	}
}
