package ru.l2gw.gameserver.network;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.text.MessageFormat;

import ru.l2gw.gameserver.model.L2Player;

public class PacketLogger
{
	private ArrayList<ByteBuffer> pktqueue = new ArrayList<ByteBuffer>();

	private int pktqueue_byteslen = 0;

	private int AUTOFLUSH_SIZE;

	private FileOutputStream fs = null;

	private L2Player player = null;

	private GameClient _client;

	private static String LOG_DIR = "./log/packets";

	private Date _createdDate = new Date();

	private Object in_lock = new Object();

	private Object out_lock = new Object();

	public PacketLogger(GameClient client, int FLUSH_SIZE)
	{
		_client = client;
		AUTOFLUSH_SIZE = FLUSH_SIZE;
	}

	public void log_packet(byte direction, ByteBuffer buf, int size)
	{
		Object lock = direction == 0 ? in_lock : out_lock;
		synchronized (lock)
		{
			ByteBuffer pkt = PacketInfo(direction, buf, size, false);
			if(pktqueue != null)
				pktqueue.add(pkt);
			flush(false);
		}
	}

	public boolean assign()
	{
		return assign("%DATE%_%TIME%_%IP%_%LOGIN%_%CHAR%");
	}

	public boolean assign(String name_mask)
	{
		synchronized (_client)
		{
			if(assigned())
				return false;
			String DATE = new SimpleDateFormat("yyyy-MM-dd").format(_createdDate);
			String TIME = new SimpleDateFormat("HH-mm-ss.SSS").format(_createdDate);
			String IP = _client.getIpAddr() == null ? "null" : _client.getIpAddr();
			String LOGIN = _client.getLoginName() == null ? "null" : _client.getLoginName();
			TryGetplayer();
			String CHAR = player == null ? "null" : player.getName() == null ? "null" : player.getName();
			Object[] arguments = { DATE, TIME, IP, LOGIN, CHAR };
			String fn = MessageFormat.format("{0}_{1}_{2}_{3}_{4}", arguments);
			//String fn = name_mask.replaceAll("%DATE%", DATE).replaceAll("%TIME%", TIME).replaceAll("%IP%", IP).replaceAll("%LOGIN%", LOGIN).replaceAll("%CHAR%", CHAR);

			new File(LOG_DIR).mkdirs();
			int n = 0;
			File f;
			for(;;)
			{
				String full_fn = LOG_DIR + "/" + fn;
				if(n > 0)
					full_fn += "_" + n;
				full_fn += ".rpl";
				f = new File(full_fn);
				if(!f.exists())
					break;
				n++;
			}
			try
			{
				f.createNewFile();
				fs = new FileOutputStream(f);
			}
			catch(Exception e)
			{
				fs = null;
				e.printStackTrace();
				return false;
			}
			flush(true);
		}
		return true;
	}

	public boolean assigned()
	{
		return fs != null;
	}

	public void close()
	{
		synchronized (_client)
		{
			if(assigned())
			{
				flush(true);
				try
				{
					fs.close();
				}
				catch(IOException e)
				{}
				fs = null;
			}
			else
				pktqueue.clear();
		}
	}

	private void TryGetplayer()
	{
		if(player == null)
			player = _client.getPlayer();
	}

	private void flush(boolean force)
	{
		if(!assigned())
			return;

		if(!force && pktqueue_byteslen < AUTOFLUSH_SIZE)
			return;

		for(int i = 0; i < pktqueue.size(); i++)
		{
			ByteBuffer pkt = pktqueue.get(i);
			try
			{
				fs.write(pkt.array());
			}
			catch(IOException e)
			{}
			pkt = null;
		}
		pktqueue.clear();
		pktqueue_byteslen = 0;
	}

	private ByteBuffer PacketInfo(byte direction, ByteBuffer buf, int size, boolean logStats)
	{
		String pktTime = new SimpleDateFormat("HHmmssSSS").format(new Date());
		int buf_size = 7 + size;
		ByteBuffer pktBuffer = ByteBuffer.allocate(buf_size).order(ByteOrder.LITTLE_ENDIAN);
		pktqueue_byteslen += buf_size;

		if(logStats)
			direction += 2;
		pktBuffer.put(direction);
		pktBuffer.putInt(Integer.valueOf(pktTime));
		pktBuffer.putShort((short) size);
		int savepos = buf.position();
		int savelimit = buf.limit();
		buf.limit(savepos + size);
		pktBuffer.put(buf.slice());
		buf.position(savepos);
		buf.limit(savelimit);
		pktBuffer.clear();
		return pktBuffer;
	}
}
