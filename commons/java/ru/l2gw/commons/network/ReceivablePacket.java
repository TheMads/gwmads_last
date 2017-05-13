package ru.l2gw.commons.network;

public abstract class ReceivablePacket<T extends MMOClient<?>> extends AbstractPacket<T> implements Runnable
{
	protected ReceivablePacket()
	{

	}

	protected int getAvaliableBytes()
	{
		return getByteBuffer().remaining();
	}

	protected abstract boolean read();

	public abstract void run();

	protected void readB(byte[] dst)
	{
		try
		{
			getByteBuffer().get(dst);
		}
		catch(Exception e)
		{ }
	}

	protected void readB(byte[] dst, int offset, int len)
	{
		try
		{
			getByteBuffer().get(dst, offset, len);
		}
		catch(Exception e)
		{ }
	}

	protected int readC()
	{
		try
		{
			return getByteBuffer().get() & 0xFF;
		}
		catch(Exception e)
		{
			return 0;
		}
	}

	protected int readH()
	{
		try
		{
			return getByteBuffer().getShort() & 0xFFFF;
		}
		catch(Exception e)
		{
			return 0;
		}
	}

	protected int readD()
	{
		try
		{
			return getByteBuffer().getInt();
		}
		catch(Exception e)
		{
			return 0;
		}
	}

	protected long readQ()
	{
		try{
			return getByteBuffer().getLong();
		}
		catch(Exception e)
		{
			return 0;
		}
	}

	protected double readF()
	{
		try
		{
			return getByteBuffer().getDouble();
		}
		catch(Exception e)
		{
			return 0;
		}
	}

	protected String readS()
	{
		try
		{
			StringBuilder sb = new StringBuilder();
			char ch;
			while((ch = getByteBuffer().getChar()) != 0)
				sb.append(ch);
			return sb.toString();
		}
		catch(Exception e)
		{
			return "";
		}
	}

	protected String readS(int Maxlen)
	{
		String ret = readS();
		return ret.length() > Maxlen ? ret.substring(0, Maxlen) : ret;
	}
}
