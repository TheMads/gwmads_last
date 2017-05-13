package ru.l2gw.loginserver.gameservercon;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.NetUtil;
import ru.l2gw.loginserver.GameServerTable;
import ru.l2gw.loginserver.LoginController;
import ru.l2gw.loginserver.ThreadPoolManager;
import ru.l2gw.loginserver.gameservercon.gspackets.ClientBasePacket;
import ru.l2gw.loginserver.gameservercon.lspackets.KickPlayer;
import ru.l2gw.loginserver.gameservercon.lspackets.ServerBasePacket;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @Author: Death
 * @Date: 12/11/2007
 * @Time: 17:52:34
 */
public class AttGS
{
	private static final Log log = LogFactory.getLog(AttGS.class.getName());

	private final ByteBuffer readBuffer = ByteBuffer.allocate(64 * 1024).order(ByteOrder.LITTLE_ENDIAN);
	private final FastList<ServerBasePacket> sendQueue = FastList.newInstance();
	private final FastList<String> accountsInGameServer = FastList.newInstance();

	private final SelectionKey key;
	private int serverId = -1;
	private boolean _isAuthed;
	private GameServerInfo gameServerInfo;
	private int fakePlayersCount = 0;

	public AttGS(SelectionKey sc)
	{
		key = sc;

		if(GSConnection.DEBUG_LS_GS)
			log.info("LS Debug: RSAKey task started");
	}

	public void sendPacket(ServerBasePacket packet)
	{
		if(!key.isValid())
			return;

		if(GSConnection.DEBUG_LS_GS)
			log.info("LS Debug: adding packet to sendQueue: " + packet.getClass().getName());

		synchronized (sendQueue)
		{
			sendQueue.addLast(packet);
			key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
		}

		if(GSConnection.DEBUG_LS_GS)
			log.info("LS Debug: Packet added");
	}

	public void onClose()
	{
		if(isAuthed())
		{
			setAuthed(false);
			log.info("LoginServer: Connection with gameserver " + getServerId() + " [" + getName() + "] lost.");
		}

		GSConnection.getInstance().removeGameserver(this);
		FastList.recycle(sendQueue);
		if(gameServerInfo != null)
		{
			for(String account : accountsInGameServer)
			{
				LoginController.getInstance().removeAuthedLoginClient(account);
			}
			gameServerInfo.setDown();
		}
		gameServerInfo = null;
	}

	public ByteBuffer getReadBuffer()
	{
		return readBuffer;
	}

	public FastList<ServerBasePacket> getSendQueue()
	{
		return sendQueue;
	}

	public void processData()
	{
		ByteBuffer buf = getReadBuffer();

		int position = buf.position();
		if(position < 2) // У нас недостаточно данных для получения длинны пакета
			return;

		// Получаем длинну пакета
		int lenght = NetUtil.getPacketLength(buf.get(0), buf.get(1));

		// Пакетик не дошел целиком, ждем дальше
		if(lenght > position)
			return;

		byte[] data = new byte[position];
		for(int i = 0; i < position; i++)
			data[i] = buf.get(i);

		buf.clear();

		while((lenght = NetUtil.getPacketLength(data[0], data[1])) <= data.length)
		{
			data = processPacket(data, lenght);
			if(data.length < 2)
				break;
		}

		buf.put(data);
	}

	private byte[] processPacket(byte[] data, int lenght)
	{
		byte[] remaining = new byte[data.length - lenght];
		byte[] packet = new byte[lenght - 2];

		System.arraycopy(data, 2, packet, 0, lenght - 2);
		System.arraycopy(data, lenght, remaining, 0, remaining.length);

		ClientBasePacket runnable = PacketHandler.handlePacket(packet, this);
		if(runnable != null)
		{
			if(GSConnection.DEBUG_LS_GS)
				log.info("LoginServer: Reading packet from GS [" + getServerId() + "]: " + runnable.getClass().getSimpleName());
			ThreadPoolManager.getInstance().execute(runnable);
		}

		return remaining;
	}

	public int getServerId()
	{
		return serverId;
	}

	public void setServerId(int serverId)
	{
		this.serverId = serverId;
	}

	public boolean isAuthed()
	{
		return _isAuthed;
	}

	public void setAuthed(boolean authed)
	{
		_isAuthed = authed;
	}

	public void addAccountInGameServer(String account)
	{
		synchronized (accountsInGameServer)
		{
			if(accountsInGameServer.contains(account))
			{
				log.warn("[CRITICAL] attemp of account double add, account: " + account);
				return;
			}
			accountsInGameServer.add(account);
		}
	}

	public void removeAccountFromGameServer(String account)
	{
		synchronized (accountsInGameServer)
		{
			accountsInGameServer.remove(account);
		}
	}

	public boolean isAccountInGameServer(String account)
	{
		synchronized (accountsInGameServer)
		{
			return accountsInGameServer.contains(account);
		}
	}

	public int getPlayerCount()
	{
		synchronized (accountsInGameServer)
		{
			return accountsInGameServer.size();
		}
	}

	public int getFakePlayerCount()
	{
		return fakePlayersCount;
	}

	public GameServerInfo getGameServerInfo()
	{
		return gameServerInfo;
	}

	public void setGameServerInfo(GameServerInfo gameServerInfo)
	{
		this.gameServerInfo = gameServerInfo;
	}

	public String getName()
	{
		return GameServerTable.getInstance().getServerNames().get(getServerId());
	}

	public String getConnectionIpAddress()
	{
		SocketChannel channel = (SocketChannel) key.channel();
		return channel.socket().getInetAddress().getHostAddress();
	}

	public void kickPlayer(String account)
	{
		sendPacket(new KickPlayer(account));
		removeAccountFromGameServer(account);
		LoginController.getInstance().removeAuthedLoginClient(account);
	}

	public SelectionKey getSelectionKey()
	{
		return key;
	}

	public void setFakePlayersCount(int count)
	{
		fakePlayersCount = count;
	}
}
