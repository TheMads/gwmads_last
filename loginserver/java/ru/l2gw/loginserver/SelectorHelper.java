package ru.l2gw.loginserver;

import ru.l2gw.loginserver.serverpackets.Init;
import ru.l2gw.commons.network.*;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class is used for Login Server.
 */
public class SelectorHelper extends TCPHeaderHandler<L2LoginClient> implements IMMOExecutor<L2LoginClient>, IClientFactory<L2LoginClient>, IAcceptFilter
{
	private ExecutorService _generalPacketsThreadPool;

	public SelectorHelper()
	{
		super(null);
		//		_generalPacketsThreadPool = new ThreadPoolExecutor(4, 6, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		_generalPacketsThreadPool = Executors.newSingleThreadExecutor();
	}

	public void execute(ReceivablePacket<L2LoginClient> packet)
	{
		_generalPacketsThreadPool.execute(packet);
	}

	public L2LoginClient create(MMOConnection<L2LoginClient> con)
	{
		L2LoginClient client = new L2LoginClient(con);
		client.sendPacket(new Init(client));
		return client;
	}

	public boolean accept(SocketChannel sc)
	{
		return !LoginController.getInstance().isBannedAddress(sc.socket().getInetAddress());
	}

	@SuppressWarnings("unchecked")
	@Override
	public HeaderInfo<L2LoginClient> handleHeader(SelectionKey key, ByteBuffer buf)
	{
		if(buf.remaining() >= 2)
		{
			int dataPending = (buf.getShort() & 0xffff) - 2;
			L2LoginClient client = ((MMOConnection<L2LoginClient>) key.attachment()).getClient();
			return getHeaderInfoReturn().set(0, dataPending, false, client);
		}
		L2LoginClient client = ((MMOConnection<L2LoginClient>) key.attachment()).getClient();
		return getHeaderInfoReturn().set(2 - buf.remaining(), 0, false, client);
	}

}
