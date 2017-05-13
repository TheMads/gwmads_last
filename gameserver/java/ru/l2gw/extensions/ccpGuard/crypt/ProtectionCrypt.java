package ru.l2gw.extensions.ccpGuard.crypt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.extensions.ccpGuard.ConfigProtect;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.StringTokenizer;

public final class ProtectionCrypt
{
	private static final Log _log = LogFactory.getLog(ProtectionCrypt.class.getSimpleName());
	private static int[] KeyData = new int[256];

	int x;
	int y;
	byte[] state = new byte[256];
	boolean _inited = false;

	final int arcfour_byte()
	{
		int x;
		int y;
		int sx, sy;

		x = (this.x + 1) & 0xff;
		sx = (int) state[x];
		y = (sx + this.y) & 0xff;
		sy = (int) state[y];
		this.x = x;
		this.y = y;
		state[y] = (byte) (sx & 0xff);
		state[x] = (byte) (sy & 0xff);
		return (int) state[((sx + sy) & 0xff)];
	}

	public synchronized void encrypt(byte[] src, int srcOff, byte[] dest, int destOff, int len)
	{
		if(!_inited)
			return;
		int end = srcOff + len;
		for(int si = srcOff, di = destOff; si < end; si++, di++)
			dest[di] = (byte) (((int) src[si] ^ arcfour_byte()) & 0xff);
	}

	public void decrypt(byte[] src, int srcOff, byte[] dest, int destOff, int len)
	{
		encrypt(src, srcOff, dest, destOff, len);
	}

	public void setKey(byte[] key)
	{
		int t, u;
		int counter;
		this.x = 0;
		this.y = 0;

		for(counter = 0; counter < 256; counter++)
			state[counter] = (byte) counter;

		int keyindex = 0;
		int stateindex = 0;
		for(counter = 0; counter < 256; counter++)
		{
			t = (int) state[counter];
			stateindex = (stateindex + key[keyindex] + t) & 0xff;
			u = (int) state[stateindex];
			state[stateindex] = (byte) (t & 0xff);
			state[counter] = (byte) (u & 0xff);
			if(++keyindex >= key.length)
				keyindex = 0;
		}
		this._inited = true;
	}

	public boolean isInited()
	{
		return this._inited;
	}

	public static int getValue(final int index)
	{
		return KeyData[index];
	}

	public static void loadProtectData()
	{
		if(!ConfigProtect.PROTECT_ENABLE)
			return;

		LineNumberReader reader = null;
		try
		{
			File file = new File("config/protectdata.key");
			if(!file.exists())
			{
				_log.warn("ProtectManager: file config/protectdata.key not found!");
				return;
			}

			reader = new LineNumberReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			String line;
			int i = 0;
			while((line = reader.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(line);
				while(st.hasMoreTokens())
				{
					long l = Long.decode(st.nextToken());
					KeyData[i] = (int) l;
					i++;
				}
			}
		}
		catch(Exception e)
		{
			_log.warn("ProtectManager: error while reading config/protectdata.key " + e);
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(reader != null)
					reader.close();
			}
			catch(Exception e2)
			{
				// nothing
			}
		}
	}
}

