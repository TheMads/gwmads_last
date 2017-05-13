package ru.l2gw.gameserver.loginservercon;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.NetUtil;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.loginservercon.gspackets.GameServerBasePacket;
import ru.l2gw.gameserver.loginservercon.lspackets.LoginServerBasePacket;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;

/**
 * @Author: Death
 * @Date: 13/11/2007
 * @Time: 16:42:49
 */
public class AttLS
{
	private static final Log log = LogFactory.getLog(AttLS.class.getName());

	private final FastList<GameServerBasePacket> sendPacketQueue = FastList.newInstance();
	private final ByteBuffer readBuffer = ByteBuffer.allocate(64 * 1024).order(ByteOrder.LITTLE_ENDIAN);

	private final SelectionKey key;
	private final LSConnection con;
	private boolean licenseShown = true;

	public AttLS(SelectionKey key, LSConnection con)
	{
		this.key = key;
		this.con = con;
	}

	public void sendPacket(GameServerBasePacket packet)
	{

		if(LSConnection.DEBUG_GS_LS)
			log.info("GS Debug: Trying to add packet to sendQueue");

		if(!key.isValid())
			return;

		synchronized(sendPacketQueue)
		{
			if(con.isShutdown())
				return;

			sendPacketQueue.addLast(packet);
		}

		if(LSConnection.DEBUG_GS_LS)
			log.info("GS Debug: packet added.");
	}

	public void processData()
	{
		ByteBuffer buf = readBuffer;

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

		LoginServerBasePacket runnable = PacketHandler.handlePacket(packet, this);
		if(runnable != null)
		{
			if(LSConnection.DEBUG_GS_LS)
				log.info("GameServer: Reading packet from login: " + runnable.getClass().getSimpleName());
			ThreadPoolManager.getInstance().executeLSGSPacket(runnable);
		}

		return remaining;
	}

	public void close()
	{
		FastList.recycle(sendPacketQueue);
	}

	public FastList<GameServerBasePacket> getSendPacketQueue()
	{
		return sendPacketQueue;
	}

	public ByteBuffer getReadBuffer()
	{
		return readBuffer;
	}

	public LSConnection getCon()
	{
		return con;
	}

	public SelectionKey getKey()
	{
		return key;
	}

	public boolean isLicenseShown()
	{
		return licenseShown;
	}

	public void setLicenseShown(boolean licenseShown)
	{
		this.licenseShown = licenseShown;
	}
}