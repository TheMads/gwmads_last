package ru.l2gw.loginserver.gameservercon;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.NetUtil;
import ru.l2gw.loginserver.Config;
import ru.l2gw.loginserver.gameservercon.lspackets.ServerBasePacket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * @Author: Death
 * @Date: 12/11/2007
 * @Time: 17:08:29
 */
public class GSConnection extends Thread
{
	// Включение дебага: java -DenableDebugLsGs
	public static final boolean DEBUG_LS_GS = System.getProperty("enableDebugLsGs") != null;
	private static final Log log = LogFactory.getLog(GSConnection.class.getName());

	private static final GSConnection instance = new GSConnection();
	private static final FastList<AttGS> gameservers = FastList.newInstance();

	private Selector selector;
	private boolean shutdown;

	public static GSConnection getInstance()
	{
		return instance;
	}

	private GSConnection()
	{
		try
		{
			selector = Selector.open();
			ServerSocketChannel server = ServerSocketChannel.open();
			server.configureBlocking(false);

			int port = Config.GAME_SERVER_LOGIN_PORT;
			String host = Config.GAME_SERVER_LOGIN_HOST;

			InetSocketAddress address;
			if(host.equals("*"))
				address = new InetSocketAddress(port);
			else
				address = new InetSocketAddress(InetAddress.getByName(host), port);

			server.socket().bind(address);
			server.register(selector, SelectionKey.OP_ACCEPT);
		}
		catch(IOException e)
		{
			e.printStackTrace();
			System.out.println("LoginServer: Can't init GameServer Listener.");
			System.exit(0);
		}

		if(DEBUG_LS_GS)
			log.info("LS Debug: Listening for gameservers.");
	}

	@Override
	public void run()
	{
		log.info("LoginServer: GS listener started.");

		while(!isShutdown())
			try
			{
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

						if(DEBUG_LS_GS)
							log.info("LS Debug: Seletor: key selected, readyOpts: " + opts);

						switch(opts)
						{
							case SelectionKey.OP_ACCEPT:
								accept(key);
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
								log.warn("GSConnection: Unknow readyOpts: " + opts);
						}
					}
				}

				Thread.sleep(1);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.out.println("LoginServer: GameServer Listener - NIO Down... Restarting...");
				System.exit(2);
			}
	}

	public void accept(SelectionKey key)
	{
		ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
		SocketChannel sc;

		try
		{
			sc = ssc.accept();
			sc.configureBlocking(false);
			sc.register(selector, SelectionKey.OP_READ);
		}
		catch(IOException e)
		{
			close(key);
			return;
		}

		SelectionKey gsKey = sc.keyFor(selector);
		gsKey.attach(new AttGS(gsKey));

		if(DEBUG_LS_GS)
			log.info("LS Debug: key accepted.");
	}

	public void read(SelectionKey key)
	{
		SocketChannel channel = (SocketChannel) key.channel();
		AttGS att = (AttGS) key.attachment();

		ByteBuffer readBuffer = att.getReadBuffer();

		int numRead;
		try
		{
			numRead = channel.read(readBuffer);
		}
		catch(IOException e)
		{
			close(key);
			return;
		}

		if(numRead == -1)
			close(key);

		if(numRead == 0)
			return;

		att.processData();

		if(DEBUG_LS_GS)
			log.info("LS Debug: Data readed.");
	}

	public void write(SelectionKey key)
	{
		AttGS att = (AttGS) key.attachment();
		SocketChannel channel = (SocketChannel) key.channel();

		FastList<ServerBasePacket> sendQueue = att.getSendQueue();
		synchronized (sendQueue)
		{
			Iterator<ServerBasePacket> it = sendQueue.iterator();
			while(it.hasNext())
			{
				ServerBasePacket packet = it.next();
				it.remove();

				try
				{
					byte[] data = packet.getBytes();
					data = NetUtil.writeLenght(data);
					channel.write(ByteBuffer.wrap(data));

					if(DEBUG_LS_GS)
						log.info("LoginServer -> GameServer [" + att.getServerId() + "]: Sending packet: " + packet.getClass().getSimpleName());
				}
				catch(IOException e)
				{
					close(key);
					return;
				}
			}
		}

		key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);

		if(DEBUG_LS_GS)
			log.info("LS Debug: Data sended.");
	}

	public void close(SelectionKey key)
	{
		AttGS att = (AttGS) key.attachment();
		if(att != null)
			att.onClose();

		key.cancel();

		try
		{
			key.channel().close();
		}
		catch(IOException e)
		{}

		if(DEBUG_LS_GS)
			log.info("LS Debug: Closing connection with GS.");
	}

	public void addGameServer(AttGS gs)
	{
		synchronized (gameservers)
		{
			gameservers.add(gs);
		}
	}

	public void removeGameserver(AttGS gs)
	{
		synchronized (gameservers)
		{
			gameservers.remove(gs);
		}
	}

	public boolean isShutdown()
	{
		return shutdown;
	}

	public void setShutdown(boolean shutdown)
	{
		this.shutdown = shutdown;
	}

	public void broadcastPacket(ServerBasePacket packet)
	{
		synchronized (gameservers)
		{
			for(AttGS gs : gameservers)
				gs.sendPacket(packet);
		}
	}

	public AttGS getGameServerByServerId(int id)
	{
		synchronized (gameservers)
		{
			for(AttGS gs : gameservers)
				if(gs.getServerId() == id)
					return gs;
		}
		return null;
	}
}
