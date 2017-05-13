package ru.l2gw.commons.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class TCPSocket implements ISocket
{
	private final Socket _socket;

	public TCPSocket(Socket socket)
	{
		_socket = socket;
	}

	/* (non-Javadoc)
	 * @see com.l2jserver.mmocore.network.ISocket#close()
	 */
	public void close() throws IOException
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

	public Socket getSocket()
	{
		return _socket;
	}
}
