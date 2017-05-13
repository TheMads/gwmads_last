package ru.l2gw.fakeserver.network;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.network.*;
import ru.l2gw.fakeserver.network.clientpackets.ClientPacket;
import ru.l2gw.fakeserver.network.clientpackets.ProtocolVersion;
import ru.l2gw.fakeserver.threading.ThreadPoolManager;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * @author: rage
 * @date: 18.04.13 13:25
 */
public class PacketHandler extends TCPHeaderHandler<FakeClient> implements IPacketHandler<FakeClient>, IClientFactory<FakeClient>, IMMOExecutor<FakeClient>
{
	protected static final Log _log = LogFactory.getLog("network");

	public PacketHandler()
	{
		super(null);
	}

	@Override
	public FakeClient create(MMOConnection<FakeClient> con)
	{
		return new FakeClient(con);
	}

	@Override
	public void execute(ReceivablePacket<FakeClient> packet)
	{
		ThreadPoolManager.getInstance().executeGameClientPacket((ClientPacket) packet);
	}

	@Override
	public ReceivablePacket<FakeClient> handlePacket(ByteBuffer data, FakeClient client)
	{
		int id = data.get() & 0xFF;

		_log.info("Incoming packet: " + id + " from " + client);

		if(client == null)
			return null;


		if(id == 0x0e)
			return new ProtocolVersion();

		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public HeaderInfo<FakeClient> handleHeader(SelectionKey key, ByteBuffer buf)
	{
		if(buf.remaining() >= 2)
		{
			int dataPending = (buf.getShort() & 0xffff) - 2;
			FakeClient client = ((MMOConnection<FakeClient>) key.attachment()).getClient();
			return getHeaderInfoReturn().set(0, dataPending, false, client);
		}
		FakeClient client = ((MMOConnection<FakeClient>) key.attachment()).getClient();
		return getHeaderInfoReturn().set(2 - buf.remaining(), 0, false, client);
	}
}