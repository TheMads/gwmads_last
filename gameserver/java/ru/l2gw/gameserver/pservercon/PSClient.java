package ru.l2gw.gameserver.pservercon;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.NetUtil;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.pservercon.gspackets.GSBasePacket;
import ru.l2gw.gameserver.pservercon.pspackets.PSBasePacket;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;

/**
 * @author: rage
 * @date: 15.10.11 23:51
 */
public class PSClient
{
	private static final Log _log = LogFactory.getLog("product");

	private final FastList<GSBasePacket> sendPacketQueue = FastList.newInstance();
	private final ByteBuffer readBuffer = ByteBuffer.allocate(64 * 1024).order(ByteOrder.LITTLE_ENDIAN);

	private final SelectionKey key;
	private final PSConnection con;

	public PSClient(SelectionKey key, PSConnection con)
	{
		this.key = key;
		this.con = con;
	}

	public void sendPacket(GSBasePacket packet)
	{
		if(!key.isValid())
			return;

		synchronized (sendPacketQueue)
		{
			if(con.isShutdown())
				return;

			sendPacketQueue.addLast(packet);
		}
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

		PSBasePacket runnable = PacketHandler.handlePacket(packet, this);
		if(runnable != null)
		{
			if(Config.PRODUCT_SERVER_DEBUG)
				_log.info("PSConnection: Reading packet from premium server: " + runnable.getClass().getSimpleName());
			ThreadPoolManager.getInstance().executeLSGSPacket(runnable);
		}

		return remaining;
	}

	public void close()
	{
		FastList.recycle(sendPacketQueue);
	}

	public FastList<GSBasePacket> getSendPacketQueue()
	{
		return sendPacketQueue;
	}

	public ByteBuffer getReadBuffer()
	{
		return readBuffer;
	}

	public PSConnection getCon()
	{
		return con;
	}

	public SelectionKey getKey()
	{
		return key;
	}
}