package ru.l2gw.commons.network;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SelectorThread<T extends MMOClient<?>> extends Thread
{
	/*
	class TimeoutChecker implements Runnable
	{
		@Override
		public void run()
		{
			long time = System.currentTimeMillis() - Config.TIMEOUT_CHECKER_CLIENT;
			for(Entry<MMOClient, Long> e : _unauthedClients.entrySet())
				if(e.getValue() < time)
				{
					if(e.getKey().getState() == L2GameClient.GameClientState.CONNECTED)
						e.getKey().closeNow(false);
					_unauthedClients.remove(e.getKey());
				}
		}
	}
	*/

	//private RunnableScheduledFuture<TimeoutChecker> timeoutChecker;

	private Selector _selector;

	// Implementations
	private final IPacketHandler<T> _packetHandler;
	private final IPacketHandler<T> _udpPacketHandler;
	private IMMOExecutor<T> _executor;
	private IClientFactory<T> _clientFactory;
	private IAcceptFilter _acceptFilter;
	private final UDPHeaderHandler<T> _udpHeaderHandler;
	private final TCPHeaderHandler<T> _tcpHeaderHandler;

	private boolean _shutdown;

	// Pending Close
	private List<MMOConnection<T>> _pendingClose = new CopyOnWriteArrayList<>();

	// Configs
	private final int HELPER_BUFFER_SIZE;
	private final int HELPER_BUFFER_COUNT;
	private final int MAX_SEND_PER_PASS;
	private int HEADER_SIZE = 2;
	private final ByteOrder BYTE_ORDER;
	private final long SLEEP_TIME;

	// MAIN BUFFERS
	private final ByteBuffer DIRECT_WRITE_BUFFER;
	private final ByteBuffer WRITE_BUFFER;
	private final ByteBuffer READ_BUFFER;

	// ByteBuffers General Purpose Pool
	private final Queue<ByteBuffer> _bufferPool = new ArrayDeque<ByteBuffer>();

	private static int MAX_UNHANDLED_SOCKETS_PER_IP = 5;
	private static int UNHANDLED_SOCKET_TTL = 5000;
	private boolean enableAntiflood = false;
	private final Object antifloodLock = new Object();
	private final Map<String, FloodInfo> _unhandledChannels = new ConcurrentHashMap<>();
	private static final Map<MMOClient<?>, Long> _unauthedClients = new ConcurrentHashMap<>();

	public SelectorThread(SelectorConfig<T> sc, IPacketHandler<T> udpPacketHandler, IPacketHandler<T> packetHandler, IMMOExecutor<T> executor, IClientFactory<T> clientFactory, IAcceptFilter acceptFilter) throws IOException
	{
		HELPER_BUFFER_SIZE = sc.getHelperBufferSize();
		HELPER_BUFFER_COUNT = sc.getHelperBufferCount();
		MAX_SEND_PER_PASS = sc.getMaxSendPerPass();
		BYTE_ORDER = sc.getByteOrder();
		SLEEP_TIME = sc.getSelectorSleepTime();

		DIRECT_WRITE_BUFFER = ByteBuffer.allocateDirect(sc.getWriteBufferSize()).order(BYTE_ORDER);
		WRITE_BUFFER = ByteBuffer.wrap(new byte[sc.getWriteBufferSize()]).order(BYTE_ORDER);
		READ_BUFFER = ByteBuffer.wrap(new byte[sc.getReadBufferSize()]).order(BYTE_ORDER);

		_udpHeaderHandler = sc.getUDPHeaderHandler();
		_tcpHeaderHandler = sc.getTCPHeaderHandler();
		this.initBufferPool();
		_acceptFilter = acceptFilter;
		_packetHandler = packetHandler;
		_udpPacketHandler = udpPacketHandler;
		_clientFactory = clientFactory;
		this.setExecutor(executor);
		this.initializeSelector();
		//if(Config.TIMEOUT_CHECKER_CLIENT > 0)
		//	timeoutChecker = (RunnableScheduledFuture<TimeoutChecker>) ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new TimeoutChecker(), 10, 1000);
	}

	protected void initBufferPool()
	{
		for(int i = 0; i < HELPER_BUFFER_COUNT; i++)
			this.getFreeBuffers().add(ByteBuffer.wrap(new byte[HELPER_BUFFER_SIZE]).order(BYTE_ORDER));
	}

	public void openServerSocket(InetAddress address, int tcpPort) throws IOException
	{
		ServerSocketChannel selectable = ServerSocketChannel.open();
		selectable.configureBlocking(false);

		ServerSocket ss = selectable.socket();
		address = address == null ? MMOSocket.getInstance(true) : address;
		if(address == null)
			ss.bind(new InetSocketAddress(tcpPort));
		else
			ss.bind(new InetSocketAddress(address, tcpPort));
		selectable.register(this.getSelector(), SelectionKey.OP_ACCEPT);
	}

	public void openDatagramSocket(InetAddress address, int udpPort) throws IOException
	{
		DatagramChannel selectable = DatagramChannel.open();
		selectable.configureBlocking(false);

		DatagramSocket ss = selectable.socket();
		if(address == null)
			ss.bind(new InetSocketAddress(udpPort));
		else
			ss.bind(new InetSocketAddress(address, udpPort));
		selectable.register(this.getSelector(), SelectionKey.OP_READ);
	}

	protected void initializeSelector() throws IOException
	{
		setName("SelectorThread-" + getId());
		this.setSelector(Selector.open());
	}

	protected ByteBuffer getPooledBuffer()
	{
		if(this.getFreeBuffers().isEmpty())
			return ByteBuffer.wrap(new byte[HELPER_BUFFER_SIZE]).order(BYTE_ORDER);
		return this.getFreeBuffers().poll();
	}

	public void recycleBuffer(ByteBuffer buf)
	{
		if(this.getFreeBuffers().size() < HELPER_BUFFER_COUNT)
		{
			buf.clear();
			this.getFreeBuffers().add(buf);
		}
	}

	public Queue<ByteBuffer> getFreeBuffers()
	{
		return _bufferPool;
	}

	public SelectionKey registerClientSocket(SelectableChannel sc, int interestOps) throws ClosedChannelException
	{
		//		SelectionKey sk = sc.register(this.getSelector(), interestOps);
		//		//sk.attach(ob)
		//		return sk;
		return sc.register(this.getSelector(), interestOps);
	}

	@Override
	public void run()
	{
		//System.out.println("Selector Started");
		int totalKeys = 0;
		Iterator<SelectionKey> iter;
		SelectionKey key;

		// main loop
		for(;;)
		{
			// check for shutdown
			if(this.isShuttingDown())
			{
				this.closeSelectorThread();
				break;
			}

			try
			{
				totalKeys = this.getSelector().selectNow();
			}
			catch(IOException e)
			{
				//TODO logging
				e.printStackTrace();
			}
			//System.out.println("Selector Selected "+totalKeys);

			if(totalKeys > 0)
			{
				Set<SelectionKey> keys = this.getSelector().selectedKeys();
				iter = keys.iterator();

				while(iter.hasNext())
				{
					key = iter.next();
					iter.remove();

					if(!key.isValid())
						continue;
					switch(key.readyOps())
					{
						case SelectionKey.OP_CONNECT:
							this.finishConnection(key);
							break;
						case SelectionKey.OP_ACCEPT:
							this.acceptConnection(key);
							break;
						case SelectionKey.OP_READ:
							this.readPacket(key);
							break;
						case SelectionKey.OP_WRITE:
							this.writePacket2(key);
							break;
						case SelectionKey.OP_READ | SelectionKey.OP_WRITE:
							this.writePacket2(key);
							// key might have been invalidated on writePacket
							if(key.isValid())
								this.readPacket(key);
							break;
					}

				}
			}

			// process pending close
			synchronized (this.getPendingClose())
			{
				for(MMOConnection<T> con : getPendingClose())
				{
					if(con != null && con.getSendQueue() != null && con.getSendQueue().isEmpty())
					{
						getPendingClose().remove(con);
						closeConnectionImpl(con);
					}
				}
			}

			try
			{
				Thread.sleep(SLEEP_TIME);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}

	}

	@SuppressWarnings("unchecked")
	protected void finishConnection(SelectionKey key)
	{
		try
		{
			((SocketChannel) key.channel()).finishConnect();
		}
		catch(IOException e)
		{
			MMOConnection<T> con = (MMOConnection<T>) key.attachment();
			T client = con.getClient();
			client.getConnection().onForcedDisconnection();
			this.closeConnectionImpl(client.getConnection());
		}

		// key might have been invalidated on finishConnect()
		if(key.isValid())
		{
			key.interestOps(key.interestOps() | SelectionKey.OP_READ);
			key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
		}
	}

	protected void acceptConnection(SelectionKey key)
	{
		SocketChannel sc;
		try
		{
			while((sc = ((ServerSocketChannel) key.channel()).accept()) != null)
			{
				if(enableAntiflood)
					synchronized (antifloodLock)
					{
						floodCloseOld();
						if(!floodAccept(sc.socket()))
						{
							sc.socket().close();
							continue;
						}
					}
				if(this.getAcceptFilter() == null || this.getAcceptFilter().accept(sc))
				{
					sc.configureBlocking(false);
					SelectionKey clientKey = sc.register(this.getSelector(), SelectionKey.OP_READ /*| SelectionKey.OP_WRITE*/);

					MMOConnection<T> con = new MMOConnection<T>(this, new TCPSocket(sc.socket()), clientKey);
					MMOClient<?> client = this.getClientFactory().create(con);
					//if(Config.TIMEOUT_CHECKER_CLIENT > 0 && client instanceof L2GameClient)
					//	_unauthedClients.put((L2GameClient) client, System.currentTimeMillis());
					clientKey.attach(con);
				}
				else
					sc.socket().close();
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	protected void readPacket(SelectionKey key)
	{
		if(key.channel() instanceof SocketChannel)
			this.readTCPPacket(key);
		else
			this.readUDPPacket(key);
	}

	@SuppressWarnings("unchecked")
	protected void readTCPPacket(SelectionKey key)
	{
		MMOConnection<T> con = (MMOConnection<T>) key.attachment();
		T client = con.getClient();

		ByteBuffer buf;
		if((buf = con.getReadBuffer()) == null)
			buf = READ_BUFFER;
		int result = -2;

		// if we try to to do a read with no space in the buffer it will read 0 bytes
		// going into infinite loop
		if(buf.position() == buf.limit())
		{
			// should never happen
			System.out.println("POS ANTES SC.READ(): " + buf.position() + " limit: " + buf.limit());
			System.out.println("NOOBISH ERROR " + (buf == READ_BUFFER ? "READ_BUFFER" : "temp"));
			closeConnectionImpl(con);
			return;
		}

		//System.out.println("POS ANTES SC.READ(): "+buf.position()+" limit: "+buf.limit()+" - buf: "+(buf == READ_BUFFER ? "READ_BUFFER" : "TEMP"));

		try
		{
			result = con.getReadableByteChannel().read(buf);
		}
		catch(IOException e)
		{
			//error handling goes bellow
		}

		//System.out.println("LEU: "+result+" pos: "+buf.position());
		if(result > 0)
		{
			// TODO this should be done vefore even reading
			if(!con.isClosed())
			{
				buf.flip();
				// try to read as many packets as possible
				while(this.tryReadPacket2(key, client, buf))
				{}
			}
			else if(buf == READ_BUFFER)
				READ_BUFFER.clear();
		}
		else if(result == 0)
		{
			// read interest but nothing to read? wtf?
			System.out.println("NOOBISH ERROR 2 THE MISSION");
			closeConnectionImpl(con);
		}
		else if(result == -1)
			closeConnectionImpl(con);
		else
		{
			con.onForcedDisconnection();
			closeConnectionImpl(con);
		}
	}

	@SuppressWarnings("unchecked")
	protected void readUDPPacket(SelectionKey key)
	{
		int result = -2;
		ByteBuffer buf = READ_BUFFER;

		DatagramChannel dc = (DatagramChannel) key.channel();
		if(!dc.isConnected())
		{
			try
			{
				dc.configureBlocking(false);
				SocketAddress address = dc.receive(buf);
				buf.flip();
				this._udpHeaderHandler.onUDPConnection(this, dc, address, buf);
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			buf.clear();
		}
		else
		{
			//System.err.println("UDP CONN "+buf.remaining());
			try
			{
				result = dc.read(buf);
			}
			catch(IOException e)
			{
				//error handling goes bellow
				//System.err.println("UDP ERR: "+e.getMessage());
			}

			//System.out.println("LEU: "+result+" pos: "+buf.position());
			if(result > 0)
			{
				buf.flip();
				// try to read as many packets as possible
				while(this.tryReadUDPPacket(key, buf))
				{}
			}
			else if(result == 0)
			{
				// read interest but nothing to read? wtf?
				System.out.println("CRITICAL ERROR ON SELECTOR");
				System.exit(0);
			}
			else
			{
				// TODO kill and cleanup this UDP connection
				//System.err.println("UDP ERROR: "+result);
			}
		}
	}

	/*protected boolean tryReadPacket(T client, ByteBuffer buf)
		{
			MMOConnection con = client.getConnection();
			//System.out.println("BUFF POS ANTES DE LER: "+buf.position()+" - REMAINING: "+buf.remaining());

			if (buf.hasRemaining())
			{
				int result = buf.remaining();

				// then check if there are enough bytes for the header
				if (result >= HEADER_SIZE)
				{
					// then read header and check if we have the whole packet
					int size = this.getHeaderValue(buf);
					//System.out.println("IF: ("+size+" <= "+result+") => (size <= result)");
					if (size <= result)
					{
						//System.out.println("BOA");

						// avoid parsing dummy packets (packets without body)
						if (size > HEADER_SIZE)
						{
							this.parseClientPacket(this.getPacketHandler(), buf, size, client);
						}

						// if we are done with this buffer
						if (!buf.hasRemaining())
						{
							//System.out.println("BOA 2");
							if (buf != READ_BUFFER)
							{
								con.setReadBuffer(null);
								this.recycleBuffer(buf);
							}
							else
							{
								READ_BUFFER.clear();
							}

							return false;
						}
						else
						{
							// nothing
						}

						return true;
					}
					else
					{
						//System.out.println("ENABLEI");
						client.getConnection().enableReadInterest();

						//System.out.println("LIMIT "+buf.limit());
						if (buf == READ_BUFFER)
						{
							buf.position(buf.position() - HEADER_SIZE);
							this.allocateReadBuffer(con);
						}
						else
						{
							buf.position(buf.position() - HEADER_SIZE);
							buf.compact();
						}
						return false;
					}
				}
				else
				{
					if (buf == READ_BUFFER)
					{
						this.allocateReadBuffer(con);
					}
					else
					{
						buf.compact();
					}
					return false;
				}
			}
			else
			{
				//con.disableReadInterest();
				return false; //empty buffer
			}
		}*/

	@SuppressWarnings("unchecked")
	protected boolean tryReadPacket2(SelectionKey key, T client, ByteBuffer buf)
	{
		MMOConnection<T> con = client.getConnection();
		//System.out.println("BUFF POS ANTES DE LER: "+buf.position()+" - REMAINING: "+buf.remaining());

		if(buf.hasRemaining())
		{
			TCPHeaderHandler<T> handler = _tcpHeaderHandler;
			// parse all jeaders
			HeaderInfo<T> ret;
			while(!handler.isChildHeaderHandler())
			{
				handler.handleHeader(key, buf);
				handler = handler.getSubHeaderHandler();
			}
			// last header
			ret = handler.handleHeader(key, buf);

			if(ret != null)
			{
				int result = buf.remaining();

				// then check if header was processed
				if(ret.headerFinished())
				{
					// get expected packet size
					int size = ret.getDataPending();

					//System.out.println("IF: ("+size+" <= "+result+") => (size <= result)");
					// do we got enough bytes for the packet?
					if(size <= result)
					{
						// avoid parsing dummy packets (packets without body)
						if(size > 0)
						{
							int pos = buf.position();
							this.parseClientPacket(this.getPacketHandler(), buf, size, client);
							buf.position(pos + size);
						}

						// if we are done with this buffer
						if(!buf.hasRemaining())
						{
							//System.out.println("BOA 2");
							if(buf != READ_BUFFER)
							{
								con.setReadBuffer(null);
								this.recycleBuffer(buf);
							}
							else
								READ_BUFFER.clear();

							return false;
						}

						return true;
					}
					// we dont have enough bytes for the dataPacket so we need to read
					client.getConnection().enableReadInterest();

					//System.out.println("LIMIT "+buf.limit());
					if(buf == READ_BUFFER)
					{
						buf.position(buf.position() - HEADER_SIZE);
						this.allocateReadBuffer(con);
					}
					else
					{
						buf.position(buf.position() - HEADER_SIZE);
						buf.compact();
					}
					return false;
				}
				// we dont have enough data for header so we need to read
				client.getConnection().enableReadInterest();

				if(buf == READ_BUFFER)
					this.allocateReadBuffer(con);
				else
					buf.compact();
				return false;
			}
			// null ret means critical error
			// kill the connection
			this.closeConnectionImpl(con);
			return false;
		}
		//con.disableReadInterest();
		return false; //empty buffer
	}

	@SuppressWarnings("unchecked")
	protected boolean tryReadUDPPacket(@SuppressWarnings("unused") SelectionKey key, ByteBuffer buf)
	{
		if(buf.hasRemaining())
		{
			UDPHeaderHandler<T> handler = _udpHeaderHandler;
			// parse all jeaders
			HeaderInfo<T> ret;
			while(!handler.isChildHeaderHandler())
			{
				handler.handleHeader(buf);
				handler = handler.getSubHeaderHandler();
			}
			// last header
			ret = handler.handleHeader(buf);

			if(ret != null)
			{
				int result = buf.remaining();

				// then check if header was processed
				if(ret.headerFinished())
				{
					T client = ret.getClient();
					MMOConnection<T> con = client.getConnection();

					// get expected packet size
					int size = ret.getDataPending();

					//System.out.println("IF: ("+size+" <= "+result+") => (size <= result)");
					// do we got enough bytes for the packet?
					if(size <= result)
					{
						if(ret.isMultiPacket())
							while(buf.hasRemaining())
								this.parseClientPacket(_udpPacketHandler, buf, buf.remaining(), client);
						else // avoid parsing dummy packets (packets without body)
						if(size > 0)
						{
							int pos = buf.position();
							this.parseClientPacket(_udpPacketHandler, buf, size, client);
							buf.position(pos + size);
						}

						// if we are done with this buffer
						if(!buf.hasRemaining())
						{
							//System.out.println("BOA 2");
							if(buf != READ_BUFFER)
							{
								con.setReadBuffer(null);
								this.recycleBuffer(buf);
							}
							else
								READ_BUFFER.clear();

							return false;
						}

						return true;
					}
					// we dont have enough bytes for the dataPacket so we need to read
					client.getConnection().enableReadInterest();

					//System.out.println("LIMIT "+buf.limit());
					if(buf == READ_BUFFER)
					{
						buf.position(buf.position() - HEADER_SIZE);
						this.allocateReadBuffer(con);
					}
					else
					{
						buf.position(buf.position() - HEADER_SIZE);
						buf.compact();
					}
					return false;
				}
				buf.clear(); // READ_BUFFER
				return false;
			}
			buf.clear(); // READ_BUFFER
			return false;
		}
		//con.disableReadInterest();
		buf.clear();
		return false; //empty buffer
	}

	protected void allocateReadBuffer(MMOConnection<T> con)
	{
		//System.out.println("con: "+Integer.toHexString(con.hashCode()));
		//Util.printHexDump(READ_BUFFER);
		con.setReadBuffer(this.getPooledBuffer().put(READ_BUFFER));
		READ_BUFFER.clear();
	}

	protected void parseClientPacket(IPacketHandler<T> handler, ByteBuffer buf, int dataSize, T client)
	{
		if(enableAntiflood)
			synchronized (antifloodLock)
			{
				floodClose(((TCPSocket) client.getConnection().getSocket()).getSocket());
			}
		int pos = buf.position();

		boolean ret = client.decrypt(buf, dataSize);

		buf.position(pos);

		//System.out.println("pCP -> BUF: POS: "+buf.position()+" - LIMIT: "+buf.limit()+" == Packet: SIZE: "+dataSize);

		if(buf.hasRemaining() && ret)
		{
			//  apply limit
			int limit = buf.limit();
			buf.limit(pos + dataSize);
			//System.out.println("pCP2 -> BUF: POS: "+buf.position()+" - LIMIT: "+buf.limit()+" == Packet: SIZE: "+size);
			ReceivablePacket<T> cp = handler.handlePacket(buf, client);

			if(cp != null)
			{
				cp.setByteBuffer(buf);
				cp.setClient(client);

				if(cp.read())
				{
					this.getExecutor().execute(cp);
				}
			}
			buf.limit(limit);
		}
	}

	@SuppressWarnings("unchecked")
	/*protected void writePacket(SelectionKey key)
		{
			T client = ((T) key.attachment());
			MMOConnection<T> con = client.getConnection();

			ByteBuffer buf;
			boolean sharedBuffer = false;
			if ((buf = con.getWriteBuffer()) == null)
			{
				SendablePacket<T> sp = null;
				synchronized (con.getSendQueue())
				{
					if (!con.getSendQueue().isEmpty())
					{
						sp = con.getSendQueue().removeFirst();
					}
				}
				if (sp == null)
				{
					System.out.println("OMG WRITE BUT NO WRITE");
					return;
				}
				//System.out.println("WRITING: "+sp.getClass().getSimpleName());
				this.prepareWriteBuffer(client, sp);
				buf = sp.getByteBuffer();
				//System.out.println("WRITED:: "+sp.getClass().getSimpleName());
				//System.out.println("wp:" +buf.position());
				buf.flip();

				sharedBuffer = true;
			}

			int size = buf.remaining();
			int result = -1;

			try
			{
				result = con.getSocketChannel().write(buf);
			}
			catch (IOException e)
			{
				// error handling goes on the if bellow
			}

			// check if no error happened
			if (result > 0)
			{
				// check if we writed everything
				if (result == size)
				{
					//System.out.println("WRITEI COMPLETO");
					synchronized (con.getSendQueue())
					{
						if (con.getSendQueue().isEmpty())
						{
							con.disableWriteInterest();
						}
					}

					// if it was a pooled buffer we can recycle
					if (!sharedBuffer)
					{
						this.recycleBuffer(buf);
						con.setWriteBuffer(null);
					}
				}
				else //incomplete write
				{
					//System.out.println("WRITEI INCOMPLETO");
					// if its the main buffer allocate a pool one
					if (sharedBuffer)
					{
						con.setWriteBuffer(this.getPooledBuffer().put(buf));
					}
					else
					{
						con.setWriteBuffer(buf);
					}
				}
			}
			else
			{
				con.onForcedDisconnection();
				this.closeConnectionImpl(con);
			}
		}*/
	protected void prepareWriteBuffer(T client, SendablePacket<T> sp)
	{
		WRITE_BUFFER.clear();

		//set the write buffer
		sp.setByteBuffer(WRITE_BUFFER);

		// reserve space for the size
		int headerPos = sp.getByteBuffer().position();
		int headerSize = sp.getHeaderSize();
		sp.getByteBuffer().position(headerPos + headerSize);

		// write contents
		sp.write();

		int dataSize = sp.getByteBuffer().position() - headerPos - headerSize;
		sp.getByteBuffer().position(headerPos + headerSize);
		client.encrypt(sp.getByteBuffer(), dataSize);

		// write size
		sp.writeHeader(dataSize);
		//sp.writeHeader(HEADER_TYPE, headerPos);
	}

	@SuppressWarnings("unchecked")
	protected void writePacket2(SelectionKey key)
	{
		MMOConnection<T> con = (MMOConnection<T>) key.attachment();

		this.prepareWriteBuffer2(con);
		DIRECT_WRITE_BUFFER.flip();

		int size = DIRECT_WRITE_BUFFER.remaining();

		//System.err.println("WRITE SIZE: "+size);
		int result = -1;

		try
		{
			result = con.getWritableChannel().write(DIRECT_WRITE_BUFFER);
		}
		catch(IOException e)
		{
			// error handling goes on the if bellow
			//System.err.println("IOError: " + e.getMessage());
		}

		// check if no error happened
		if(result >= 0)
		{
			// check if we writed everything
			if(result == size)
				synchronized (con.getSendQueue())
				{
					if(con.getSendQueue().isEmpty() && !con.hasPendingWriteBuffer())
						con.disableWriteInterest();
				}
			else
				con.createWriteBuffer(DIRECT_WRITE_BUFFER);
			//System.err.println("DEBUG: INCOMPLETE WRITE - write size: "+size);
			//System.err.flush();

			if(result == 0)
			{
				//System.err.println("DEBUG: write result: 0 - write size: "+size+" - DWB rem: "+DIRECT_WRITE_BUFFER.remaining());
				//System.err.flush();
			}

		}
		else
		{
			//System.err.println("IOError: "+result);
			//System.err.flush();
			con.onForcedDisconnection();
			this.closeConnectionImpl(con);
		}
	}

	@SuppressWarnings("unchecked")
	protected void prepareWriteBuffer2(MMOConnection<T> con)
	{
		DIRECT_WRITE_BUFFER.clear();

		// if theres pending content add it
		if(con.hasPendingWriteBuffer())
			con.movePendingWriteBufferTo(DIRECT_WRITE_BUFFER);
		//System.err.println("ADDED PENDING TO DIRECT "+DIRECT_WRITE_BUFFER.position());

		if(DIRECT_WRITE_BUFFER.remaining() > 1 && !con.hasPendingWriteBuffer())
		{

			int i = 0;

			//FastList<SendablePacket<T>> sendQueue = con.getSendQueue();
			//Node<SendablePacket<T>> n, temp, end;

			do
			{
				SendablePacket<T> sp;
				synchronized(con.getSendQueue())
				{
					if(!con.getSendQueue().isEmpty())
					{
						sp = con.getSendQueue().poll();
						i++;
					}
					else
						break;
				}

				putPacketIntoWriteBuffer(con.getClient(), sp);

				WRITE_BUFFER.flip();
				if(DIRECT_WRITE_BUFFER.remaining() >= WRITE_BUFFER.limit())
					DIRECT_WRITE_BUFFER.put(WRITE_BUFFER);
				else
				{
					// there is no more space in the direct buffer
					//con.addWriteBuffer(this.getPooledBuffer().put(WRITE_BUFFER));
					con.createWriteBuffer(WRITE_BUFFER);
					break;
				}
			}
			while(i < MAX_SEND_PER_PASS);
/*
			synchronized (sendQueue)
			{
				for(n = sendQueue.head(), end = sendQueue.tail(); (n = n.getNext()) != end && i++ < MAX_SEND_PER_PASS;)
				{
					sp = n.getValue();
					// put into WriteBuffer
					putPacketIntoWriteBuffer(con.getClient(), sp);

					// delete packet from queue
					temp = n.getPrevious();
					sendQueue.delete(n);
					n = temp;

					WRITE_BUFFER.flip();
					if(DIRECT_WRITE_BUFFER.remaining() >= WRITE_BUFFER.limit())
						DIRECT_WRITE_BUFFER.put(WRITE_BUFFER);
					else
					{
						// there is no more space in the direct buffer
						//con.addWriteBuffer(this.getPooledBuffer().put(WRITE_BUFFER));
						con.createWriteBuffer(WRITE_BUFFER);
						break;
					}
				}
			}
*/
		}
	}

	protected final void putPacketIntoWriteBuffer(T client, SendablePacket<T> sp)
	{
		WRITE_BUFFER.clear();

		// set the write buffer
		sp.setByteBuffer(WRITE_BUFFER);

		// reserve space for the size
		int headerPos = sp.getByteBuffer().position();
		int headerSize = sp.getHeaderSize();
		sp.getByteBuffer().position(headerPos + headerSize);

		// write content to buffer
		sp.write();

		// size (incl header)
		int dataSize = sp.getByteBuffer().position() - headerPos - headerSize;
		if(dataSize == 0)
		{
			sp.getByteBuffer().position(headerPos);
			//System.out.println("Warning! Empty packet [" + sp.toString() + "]");
			return;
		}
		sp.getByteBuffer().position(headerPos + headerSize);
		client.encrypt(sp.getByteBuffer(), dataSize);

		// recalculate size after encryption
		dataSize = sp.getByteBuffer().position() - headerPos - headerSize;

		// prepend header
		//this.prependHeader(headerPos, size);
		sp.getByteBuffer().position(headerPos);
		sp.writeHeader(dataSize);
		sp.getByteBuffer().position(headerPos + headerSize + dataSize);
	}

	/*protected void prependHeader(int pos, int size)
		{
			switch (HEADER_TYPE)
			{
				case BYTE_HEADER:
					WRITE_BUFFER.put(pos, (byte) size);
					break;
				case SHORT_HEADER:
					WRITE_BUFFER.putShort(pos, (short) size);
					break;
				case INT_HEADER:
					WRITE_BUFFER.putInt(pos, size);
					break;
			}
		}*/

	/*protected int getHeaderValue(ByteBuffer buf)
		{
			switch (HEADER_TYPE)
			{
				case BYTE_HEADER:
					return buf.get() & 0xFF;
				case SHORT_HEADER:
					return buf.getShort() & 0xFFFF;
				case INT_HEADER:
					return buf.getInt();
			}
			return -1; // O.o
		}*/

	protected void setSelector(Selector selector)
	{
		_selector = selector;
	}

	public Selector getSelector()
	{
		return _selector;
	}

	protected void setExecutor(IMMOExecutor<T> executor)
	{
		_executor = executor;
	}

	protected IMMOExecutor<T> getExecutor()
	{
		return _executor;
	}

	public IPacketHandler<T> getPacketHandler()
	{
		return _packetHandler;
	}

	protected void setClientFactory(IClientFactory<T> clientFactory)
	{
		_clientFactory = clientFactory;
	}

	public IClientFactory<T> getClientFactory()
	{
		return _clientFactory;
	}

	public void setAcceptFilter(IAcceptFilter acceptFilter)
	{
		_acceptFilter = acceptFilter;
	}

	public IAcceptFilter getAcceptFilter()
	{
		return _acceptFilter;
	}

	public void closeConnection(MMOConnection<T> con)
	{
		getPendingClose().add(con);
	}

	protected void closeConnectionImpl(MMOConnection<T> con)
	{
		try
		{
			if(enableAntiflood)
				synchronized (antifloodLock)
				{
					floodClose(((TCPSocket) con.getSocket()).getSocket());
				}
			// notify connection
			con.onDisconnection();
		}
		finally
		{
			try
			{
				// close socket and the SocketChannel
				con.getSocket().close();
			}
			catch(IOException e)
			{
				// ignore, we are closing anyway
			}
			finally
			{
				con.releaseBuffers();
				// clear attachment
				con.getSelectionKey().attach(null);
				// cancel key
				con.getSelectionKey().cancel();
			}
		}
	}

	protected List<MMOConnection<T>> getPendingClose()
	{
		return _pendingClose;
	}

	public void shutdown()
	{
		_shutdown = true;
	}

	public boolean isShuttingDown()
	{
		return _shutdown;
	}

	protected void closeAllChannels()
	{
		Set<SelectionKey> keys = this.getSelector().keys();
		for(SelectionKey key : keys)
			try
			{
				key.channel().close();
			}
			catch(IOException e)
			{
				// ignore
			}
	}

	protected void closeSelectorThread()
	{
		this.closeAllChannels();
		//if(timeoutChecker != null)
		//	timeoutChecker.cancel(false);
		try
		{
			this.getSelector().close();
		}
		catch(IOException e)
		{
			// Ignore
		}
	}

	protected boolean floodAccept(Socket sc)
	{
		String ip = sc.getInetAddress().getHostAddress();
		FloodInfo fi = _unhandledChannels.get(ip);

		if(fi == null)
		{
			fi = new FloodInfo(ip);
			fi.sockets.put(sc, System.currentTimeMillis());
			_unhandledChannels.put(ip, fi);
			return true;
		}

		if(fi.sockets.size() < MAX_UNHANDLED_SOCKETS_PER_IP)
		{
			fi.sockets.put(sc, System.currentTimeMillis());
			return true;
		}

		return false;
	}

	protected void floodClose(Socket sc)
	{
		if(sc == null || sc.getInetAddress() == null)
			return;

		String ip = sc.getInetAddress().getHostAddress();

		FloodInfo fi = _unhandledChannels.get(ip);

		if(fi != null)
		{
			fi.sockets.remove(sc);
			if(fi.sockets.size() < 1)
				_unhandledChannels.remove(ip);
		}
	}

	protected void floodCloseOld()
	{
		long now_time = System.currentTimeMillis();

		for(FloodInfo fi : _unhandledChannels.values())
			for(Socket sc : fi.sockets.keySet())
				if(now_time - fi.sockets.get(sc) >= UNHANDLED_SOCKET_TTL)
				{
					fi.sockets.remove(sc);
					if(fi.sockets.size() < 1)
						_unhandledChannels.remove(fi.ip);
					try
					{
						sc.close();
					}
					catch(IOException e)
					{
						e.printStackTrace();
					}
				}
	}

	public void setAntiFlood(boolean _enableAntiflood)
	{
		enableAntiflood = _enableAntiflood;
	}

	public void setAntiFloodSocketsConf(int MaxUnhandledSocketsPerIP, int UnhandledSocketsMinTTL)
	{
		MAX_UNHANDLED_SOCKETS_PER_IP = MaxUnhandledSocketsPerIP;
		UNHANDLED_SOCKET_TTL = UnhandledSocketsMinTTL;
	}
}
