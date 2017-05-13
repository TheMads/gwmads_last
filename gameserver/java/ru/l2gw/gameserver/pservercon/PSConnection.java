package ru.l2gw.gameserver.pservercon;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.NetUtil;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.pservercon.gspackets.GSBasePacket;
import ru.l2gw.gameserver.pservercon.gspackets.ServerInfoPacket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * @author: rage
 * @date: 15.10.11 23:44
 */
public class PSConnection extends Thread
{
	private static final Log _log = LogFactory.getLog("product");
	private static final Log _logStd = LogFactory.getLog(PSConnection.class);
	private static PSConnection instance = null;

	private Selector selector;

	private SelectionKey key;
	private SocketChannel channel;

	private volatile boolean shutdown;
	private volatile boolean restart = true;

	public static PSConnection getInstance()
	{
		if(instance == null)
			instance = new PSConnection();

		return instance;
	}

	private PSConnection()
	{
		try
		{
			selector = Selector.open();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			_log.warn("PSConnection: Can't open selector.");
		}
	}

	private void reconnect()
	{
		try
		{
			_logStd.info("GameServer: Connecting to PremiumServer on " + Config.PRODUCT_SERVER_HOST + ":" + Config.PRODUCT_SERVER_PORT);
			channel = SocketChannel.open();
			channel.configureBlocking(false);
			channel.register(selector, SelectionKey.OP_CONNECT);
			channel.connect(new InetSocketAddress(Config.PRODUCT_SERVER_HOST, Config.PRODUCT_SERVER_PORT));
			key = channel.keyFor(selector);
			restart = false;
		}
		catch(Exception e)
		{
			_logStd.warn("GameServer: Can't connect to PremiumServer: " + e.getMessage());
		}
	}

	private void readSelected()
	{
		while(!(shutdown || restart))
			try
			{
				if(key == null || !key.isValid())
					return;

				PSClient client = (PSClient) key.attachment();
				if(client != null)
				{
					FastList<GSBasePacket> sendQueue = client.getSendPacketQueue();
					synchronized(sendQueue)
					{
						int sendSize = client.getSendPacketQueue().size();
						if(sendSize > 0)
							key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
						else
						{
							key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
							key.interestOps(key.interestOps() | SelectionKey.OP_READ);
						}
					}
				}

				int keyNum = selector.selectNow();

				if(keyNum > 0)
				{
					Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
					while(keys.hasNext())
					{
						SelectionKey key = keys.next();
						keys.remove();

						if(!key.isValid())
						{
							close(key);
							continue;
						}

						int opts = key.readyOps();

						switch(opts)
						{
							case SelectionKey.OP_CONNECT:
								connect(key);
								break;
							case SelectionKey.OP_WRITE:
								write(key);
								break;
							case SelectionKey.OP_READ:
								read(key);
								break;
							case SelectionKey.OP_READ | SelectionKey.OP_WRITE:
								write(key);
								read(key);
								break;
							default:
								_log.warn("PSConnection: unknown readyOpts: " + opts);
						}
					}
				}

				Thread.sleep(1);
			}
			catch(Exception e)
			{
				_log.info("Disconnected from Premium Server.");
				e.printStackTrace();
				close(key);
				break;
			}
	}

	@Override
	public void run()
	{
		while(restart)
		{
			reconnect();
			readSelected();
			close(null);
			try
			{
				Thread.sleep(10000);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}

			if(shutdown)
				break;
		}
	}

	public void read(SelectionKey key)
	{
		PSClient client = (PSClient) key.attachment();
		SocketChannel channel = (SocketChannel) key.channel();

		int numRead;

		try
		{
			numRead = channel.read(client.getReadBuffer());
		}
		catch(IOException e)
		{
			close(key);
			return;
		}

		if(numRead == -1)
		{
			close(key);
			return;
		}

		if(numRead == 0)
			return;

		client.processData();
	}

	public void write(SelectionKey key)
	{
		PSClient client = (PSClient) key.attachment();
		SocketChannel channel = (SocketChannel) key.channel();

		FastList<GSBasePacket> sendPacketQueue = client.getSendPacketQueue();

		synchronized(sendPacketQueue)
		{
			try
			{
				Iterator<GSBasePacket> it = sendPacketQueue.iterator();
				while(it.hasNext())
				{
					GSBasePacket packet = it.next();
					it.remove();

					byte[] data = packet.getBytes();
					data = NetUtil.writeLenght(data);
					channel.write(ByteBuffer.wrap(data));

					if(Config.PRODUCT_SERVER_DEBUG)
						_log.info("PSConnection: Sending packet: " + packet.getClass().getSimpleName());
				}
			}
			catch(Exception e)
			{
				close(key);
				return;
			}
		}

		key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);

		if(Config.PRODUCT_SERVER_DEBUG)
			_log.info("PSConnection: Data sended");
	}

	public void connect(SelectionKey key)
	{
		SocketChannel channel = (SocketChannel) key.channel();

		try
		{
			channel.finishConnect();
		}
		catch(IOException e)
		{
			close(key);
			return;
		}

		PSClient client = new PSClient(key, this);
		key.attach(client);
		key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
		key.interestOps(key.interestOps() | SelectionKey.OP_READ);
		client.sendPacket(new ServerInfoPacket());
		if(Config.PRODUCT_SERVER_DEBUG)
			_log.info("PSConnection: connection to premium server established.");
	}

	public void close(SelectionKey key)
	{
		if(Config.PRODUCT_SERVER_DEBUG)
			_log.info("PSConnection: closing connection to premium server.");

		if(key == null)
			key = this.key;

		if(key == null && channel != null)
			key = channel.keyFor(selector);

		if(key != null)
		{
			key.cancel();

			PSClient client = (PSClient) key.attachment();
			if(client != null)
				client.close();
		}

		try
		{
			if(channel != null)
				channel.close();
		}
		catch(IOException e)
		{
		}

		this.key = null;
		channel = null;

		if(shutdown)
			return;

		restart = true;
	}

	public void sendPacket(GSBasePacket packet)
	{
		if(shutdown || key == null || key.attachment() == null)
			return;

		PSClient client = (PSClient) key.attachment();
		client.sendPacket(packet);
	}

	public void shutdown()
	{
		shutdown = true;
	}

	public boolean isShutdown()
	{
		return shutdown;
	}

	public void restart()
	{
		restart = true;
	}
}