package ru.l2gw.gameserver.serverpackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.network.SendablePacket;
import ru.l2gw.gameserver.network.GameClient;

public abstract class L2GameServerPacket extends SendablePacket<GameClient>
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

	protected final static int EXTENDED_PACKET = 0xFE;

	protected void writeEx(int opcode)
	{
		writeC(EXTENDED_PACKET);
		writeH(opcode);
	}

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