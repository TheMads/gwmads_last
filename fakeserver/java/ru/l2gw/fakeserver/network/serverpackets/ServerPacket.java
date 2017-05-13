package ru.l2gw.fakeserver.network.serverpackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.network.SendablePacket;
import ru.l2gw.fakeserver.network.FakeClient;

/**
 * @author: rage
 * @date: 18.04.13 13:35
 */
public abstract class ServerPacket extends SendablePacket<FakeClient>
{
	protected static Log _log = LogFactory.getLog("network");

	@Override
	protected void write()
	{
		try
		{
			writeImpl();
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
	}

	@Override
	public void runImpl()
	{}

	protected abstract void writeImpl();

	@Override
	protected int getHeaderSize()
	{
		return 2;
	}

	@Override
	protected void writeHeader(int dataSize)
	{
		writeH(dataSize + getHeaderSize());
	}
}