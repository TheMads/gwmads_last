package ru.l2gw.commons.network;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class UDPSocket implements ISocket
{
	private final DatagramSocket _socket;

	public UDPSocket(DatagramSocket socket)
	{
		_socket = socket;
	}

	/* (non-Javadoc)
	 * @see com.l2jserver.mmocore.network.ISocket#close()
	 */
	public void close()
	{
		_socket.close();
	}

	/* (non-Javadoc)
	 * @see com.l2jserver.mmocore.network.ISocket#getReadableByteChannel()
	 */
	public ReadableByteChannel getReadableByteChannel()
	{
		return _socket.getChannel();
	}

	/* (non-Javadoc)
	 * @see com.l2jserver.mmocore.network.ISocket#getWritableByteChannel()
	 */
	public WritableByteChannel getWritableByteChannel()
	{
		return _socket.getChannel();
	}

	/* (non-Javadoc)
	 * @see org.mmocore.network.ISocket#getInetAddress()
	 */
	public InetAddress getInetAddress()
	{
		return _socket.getInetAddress();
	}

	public InetAddress getLocalAddress()
	{
		return _socket.getLocalAddress();
	}
}
