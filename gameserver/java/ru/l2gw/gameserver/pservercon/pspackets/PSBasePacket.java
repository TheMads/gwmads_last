package ru.l2gw.gameserver.pservercon.pspackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.pservercon.PSClient;
import ru.l2gw.gameserver.pservercon.gspackets.GSBasePacket;

/**
 * @author: rage
 * @date: 16.10.11 0:02
 */
public abstract class PSBasePacket implements Runnable
{
	protected static final Log _log = LogFactory.getLog("product");
	private int offset;
	private final PSClient client;
	private byte[] data;

	public PSBasePacket(byte[] decrypt, PSClient client)
	{
		data = decrypt;
		offset = 2; // skip packet type id
		this.client = client;
	}

	public int readD()
	{
		int result = data[offset++] & 0xff;
		result |= data[offset++] << 8 & 0xff00;
		result |= data[offset++] << 0x10 & 0xff0000;
		result |= data[offset++] << 0x18 & 0xff000000;
		return result;
	}

	public long readQ()
	{
		long result = data[offset++] & 0xff;
		result |= data[offset++] << 8 & 0xff00;
		result |= data[offset++] << 0x10 & 0xff0000;
		result |= data[offset++] << 0x18 & 0xff000000;
		result |= (long) data[offset++] << 0x20 & 0xff00000000L;
		result |= (long) data[offset++] << 0x28 & 0xff0000000000L;
		result |= (long) data[offset++] << 0x30 & 0xff000000000000L;
		result |= (long) data[offset++] << 0x38 & 0xff00000000000000L;
		return result;
	}

	public int readC()
	{
		return data[offset++] & 0xff;
	}

	public int readH()
	{
		int result = data[offset++] & 0xff;
		result |= data[offset++] << 8 & 0xff00;
		return result;
	}

	public double readF()
	{
		long result = data[offset++] & 0xff;
		result |= data[offset++] << 8 & 0xff00;
		result |= data[offset++] << 0x10 & 0xff0000;
		result |= data[offset++] << 0x18 & 0xff000000;
		result |= data[offset++] << 0x20 & 0xff00000000l;
		result |= data[offset++] << 0x28 & 0xff0000000000l;
		result |= data[offset++] << 0x30 & 0xff000000000000l;
		result |= data[offset++] << 0x38 & 0xff00000000000000l;
		return Double.longBitsToDouble(result);
	}

	public String readS()
	{
		String result = null;
		try
		{
			result = new String(data, offset, data.length - offset, "UTF-16LE");
			result = result.substring(0, result.indexOf(0x00));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		if(result != null)
			offset += result.length() * 2 + 2;
		return result;
	}

	public final byte[] readB(int length)
	{
		byte[] result = new byte[length];
		System.arraycopy(data, offset, result, 0, length);
		offset += length;
		return result;
	}

	public void run()
	{
		try
		{
			read();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public abstract void read();

	public PSClient getClient()
	{
		return client;
	}

	public void sendPacket(GSBasePacket packet)
	{
		client.sendPacket(packet);
	}
}
