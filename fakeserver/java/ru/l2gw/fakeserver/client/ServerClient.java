package ru.l2gw.fakeserver.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author: rage
 * @date: 18.04.13 16:12
 */
public class ServerClient
{
	private final String host;
	private final int port;
	private int max, online1, online2, store;
	private ByteBuffer readBuffer;
	private ByteBuffer writeBuffer;
	private boolean lastRequest;

	public ServerClient(String host, int port)
	{
		this.host = host;
		this.port = port;
	}

	public boolean readOnline()
	{
		try
		{
			max = online1 = online2 = store = 0;
			Socket socket = new Socket(host, port);
			DataOutputStream os = new DataOutputStream(socket.getOutputStream());
			DataInputStream is = new DataInputStream(socket.getInputStream());
			writeBuffer = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN);
			writeBuffer.putShort((short) 7);
			writeBuffer.put((byte) 0x0e);
			writeBuffer.putInt(-3);

			os.write(writeBuffer.array());
			os.flush();

			byte[] buff = new byte[23];
			is.read(buff, 0, buff.length);

			readBuffer = ByteBuffer.wrap(buff);
			readBuffer = readBuffer.order(ByteOrder.LITTLE_ENDIAN);

			readBuffer.position(2 + 1 + 4);
			max = readBuffer.getInt();
			online1 = readBuffer.getInt();
			online2 = readBuffer.getInt();
			store = readBuffer.getInt();

			os.close();
			is.close();
			socket.close();

			lastRequest = true;

			return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		lastRequest = false;

		return false;
	}

	public int getMax()
	{
		return max;
	}

	public int getOnline1()
	{
		return online1;
	}

	public int getOnline2()
	{
		return online2;
	}

	public int getStore()
	{
		return store;
	}

	@Override
	public String toString()
	{
		return host + ":" + port + "{" + max + ";" + online1 + ";" + online2 + ";" + store + "} last request: " + (lastRequest ? "success" : "fail");
	}
}
