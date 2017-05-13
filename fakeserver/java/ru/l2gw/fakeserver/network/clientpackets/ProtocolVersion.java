package ru.l2gw.fakeserver.network.clientpackets;

import ru.l2gw.fakeserver.network.serverpackets.SendStatus;

/**
 * @author: rage
 * @date: 18.04.13 13:42
 */
public class ProtocolVersion extends ClientPacket
{
	@Override
	protected void readImpl() throws Exception
	{
		int version;
		if(_buf.remaining() < 4)
			version = (byte) readC();
		else
			version = readD();

		if((version == -3  || version == -2 ))
		{
			_client.close(new SendStatus(version));
			return;
		}

		_client.closeNow(false);
	}

	@Override
	protected void runImpl() throws Exception
	{
	}
}
