package ru.l2gw.gameserver.loginservercon;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.NetUtil;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.loginservercon.gspackets.AuthRequest;
import ru.l2gw.gameserver.loginservercon.gspackets.GameServerBasePacket;
import ru.l2gw.gameserver.loginservercon.gspackets.PlayerAuthRequest;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.network.GameClient;
import ru.l2gw.gameserver.serverpackets.LoginFail;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Iterator;

/**
 * @Author: Death
 * @Date: 12/11/2007
 * @Time: 20:29:24
 */
public class LSConnection extends Thread
{
	// Включение дебага: java -DenableDebugGsLs
	public static final boolean DEBUG_GS_LS = System.getProperty("enableDebugGsLs") != null;
	private static final Log log = LogFactory.getLog(LSConnection.class.getName());
	private static final LSConnection instance = new LSConnection();

	private Selector selector;

	private final FastMap<String, GameClient> waitingClients = FastMap.newInstance();
	private final FastMap<String, GameClient> accountsInGame = FastMap.newInstance();

	private SelectionKey key;
	private SocketChannel channel;

	private volatile boolean shutdown;
	private volatile boolean restart = true;

	public static LSConnection getInstance()
	{
		return instance;
	}

	private LSConnection()
	{
		try
		{
			selector = Selector.open();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			log.warn("LSConnection: Can't open selector, restarting.");
			System.exit(2);
		}

		if(DEBUG_GS_LS)
			log.info("GS Debug: Selector started.");
	}

	private void reconnect()
	{
		try
		{
			log.info("GameServer: Connecting to LoginServer on " + Config.GAME_SERVER_LOGIN_HOST + ":" + Config.GAME_SERVER_LOGIN_PORT);
			channel = SocketChannel.open();
			channel.configureBlocking(false);
			channel.register(selector, SelectionKey.OP_CONNECT);
			channel.connect(new InetSocketAddress(Config.GAME_SERVER_LOGIN_HOST, Config.GAME_SERVER_LOGIN_PORT));
			key = channel.keyFor(selector);
			restart = false;
		}
		catch(Exception e)
		{
			log.warn("Cant connect to server: " + e.getMessage());
		}
	}

	private void readSelected()
	{
		while(!(shutdown || restart))
			try
			{
				if(key == null || !key.isValid())
					return;

				AttLS att = (AttLS) key.attachment();
				if(att != null)
				{
					FastList<GameServerBasePacket> sendQueue = att.getSendPacketQueue();
					synchronized(sendQueue)
					{
						int sendSize = att.getSendPacketQueue().size();
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

						if(DEBUG_GS_LS)
							log.info("GS Debug: key selected, readyOpts: " + opts);

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
								log.warn("LSConnection: unknown readyOpts: " + opts);
						}
					}
				}

				Thread.sleep(1);
			}
			catch(Exception e)
			{
				System.out.println("Disconnected from LoginServer");
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
				Thread.sleep(2000);
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
		AttLS att = (AttLS) key.attachment();
		SocketChannel channel = (SocketChannel) key.channel();

		int numRead;

		try
		{
			numRead = channel.read(att.getReadBuffer());
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

		att.processData();

		if(DEBUG_GS_LS)
			log.info("GS Debug: data readed");
	}

	public void write(SelectionKey key)
	{
		AttLS att = (AttLS) key.attachment();
		SocketChannel channel = (SocketChannel) key.channel();

		FastList<GameServerBasePacket> sendPacketQueue = att.getSendPacketQueue();

		synchronized(sendPacketQueue)
		{
			try
			{
				Iterator<GameServerBasePacket> it = sendPacketQueue.iterator();
				while(it.hasNext())
				{
					GameServerBasePacket packet = it.next();
					it.remove();

					byte[] data = packet.getBytes();
					//if((data[0] & 0xFF) > 0)
					//	data = att.encrypt(data);
					data = NetUtil.writeLenght(data);
					channel.write(ByteBuffer.wrap(data));

					if(DEBUG_GS_LS)
						log.info("GameServer -> LoginServer: Sending packet: " + packet.getClass().getSimpleName());
				}
			}
			catch(Exception e)
			{
				close(key);
				return;
			}
		}

		key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);

		if(DEBUG_GS_LS)
			log.info("GS Debug: Data sended");
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

		key.attach(new AttLS(key, this));
		key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
		key.interestOps(key.interestOps() | SelectionKey.OP_READ);

		sendPacket(new AuthRequest());
		if(DEBUG_GS_LS)
			log.info("GS Debug: connection established");
	}

	public void close(SelectionKey key)
	{
		if(DEBUG_GS_LS)
			log.info("GS Debug: closing connection");

		if(key == null)
			key = this.key;

		if(key == null && channel != null)
			key = channel.keyFor(selector);

		if(key != null)
		{
			key.cancel();

			AttLS att = (AttLS) key.attachment();
			if(att != null)
				att.close();
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

		synchronized(waitingClients)
		{
			Collection<GameClient> wc = waitingClients.values();

			for(GameClient c : wc)
			{
				c.sendPacket(new LoginFail(LoginFail.SYSTEM_ERROR_LOGIN_LATER));
				ThreadPoolManager.getInstance().scheduleGeneral(new KickWaitingClientTask(c), 1000);
			}

			waitingClients.clear();
		}

		synchronized(accountsInGame)
		{
			Collection<GameClient> aig = accountsInGame.values();

			for(GameClient client : aig)
				if(client.getPlayer() == null)
					client.closeNow(false);

			accountsInGame.clear();
		}
	}

	public void sendPacket(GameServerBasePacket packet)
	{
		if(shutdown || key == null || key.attachment() == null)
			return;

		AttLS att = (AttLS) key.attachment();
		att.sendPacket(packet);
	}

	public void addWaitingClient(GameClient client)
	{
		synchronized(waitingClients)
		{
			// Если идет процесс выключения даного трида, то не позволяем сюда логинится.
			if(shutdown || key == null || key.attachment() == null)
			{
				client.sendPacket(new LoginFail(LoginFail.SYSTEM_ERROR_LOGIN_LATER));
				ThreadPoolManager.getInstance().scheduleGeneral(new KickWaitingClientTask(client), 1000);
				return;
			}

			GameClient sameClient = waitingClients.remove(client.getLoginName());

			if(sameClient != null)
			{
				sameClient.sendPacket(new LoginFail(LoginFail.ACOUNT_ALREADY_IN_USE));
				ThreadPoolManager.getInstance().scheduleGeneral(new KickWaitingClientTask(sameClient), 1000);
			}

			waitingClients.put(client.getLoginName(), client);
			sendPacket(new PlayerAuthRequest(client));
		}

		if(DEBUG_GS_LS)
			log.info("GameServer: Adding client to waiting list: " + client.getLoginName());
	}

	public GameClient removeWaitingClient(String account)
	{
		GameClient client;
		synchronized(waitingClients)
		{
			client = waitingClients.remove(account);
		}
		return client;
	}

	public void addAccountInGame(GameClient client)
	{
		if(client == null)
			return;

		synchronized(accountsInGame)
		{
			// Если идет процесс выключения даного трида, то не позволяем сюда логинится.
			if(shutdown || key.attachment() == null)
			{
				client.sendPacket(new LoginFail(LoginFail.SYSTEM_ERROR_LOGIN_LATER));
				ThreadPoolManager.getInstance().scheduleGeneral(new KickWaitingClientTask(client), 1000);
				return;
			}

			GameClient oldClient = null;

			if(client.getLoginName() != null)
				oldClient = accountsInGame.remove(client.getLoginName());

			if(oldClient != null)
			{
				L2Player player = oldClient.getPlayer();
				if(player != null)
				{
					player.sendPacket(new SystemMessage(SystemMessage.ANOTHER_PERSON_HAS_LOGGED_IN_WITH_THE_SAME_ACCOUNT));
					player.setOfflineMode(false);
				}
				else
					oldClient.sendPacket(Msg.ServerClose);

				ThreadPoolManager.getInstance().scheduleGeneral(new KickPlayerInGameTask(oldClient), 1000);
			}

			if(client.getLoginName() != null)
				accountsInGame.put(client.getLoginName(), client);
		}
	}

	public void addOfflineAccount(GameClient client)
	{
		if(client == null)
			return;

		if(accountsInGame.containsKey(client.getLoginName()))
		{
			log.info("Warning! try to double add offline account! " + client.getLoginName());
			return;
		}

		accountsInGame.put(client.getLoginName(), client);
	}

	public void removeAccountInGame(GameClient client)
	{
		synchronized(accountsInGame)
		{
			String loginName = client.getLoginName();
			GameClient oldClient = accountsInGame.get(loginName);

			if(client.equals(oldClient))
				accountsInGame.remove(loginName);
		}
	}

	public GameClient getAccountInGame(String account)
	{
		synchronized(accountsInGame)
		{
			return accountsInGame.get(account);
		}
	}

	public void kickAccountInGame(String account)
	{
		synchronized(accountsInGame)
		{
			GameClient client = accountsInGame.get(account);

			if(client != null)
			{
				L2Player player = client.getPlayer();
				if(player != null)
				{
					player.sendPacket(new SystemMessage(SystemMessage.ANOTHER_PERSON_HAS_LOGGED_IN_WITH_THE_SAME_ACCOUNT));
					player.setOfflineMode(false);
				}
				else
					client.sendPacket(Msg.ServerClose);

				ThreadPoolManager.getInstance().scheduleGeneral(new KickPlayerInGameTask(client), 1000);
			}
		}
	}

	public void removeAccount(GameClient client)
	{
		if(client.getState() == GameClient.GameClientState.CONNECTED)
			removeWaitingClient(client.getLoginName());
		else
			removeAccountInGame(client);
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