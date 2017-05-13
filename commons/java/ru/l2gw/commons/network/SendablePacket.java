package ru.l2gw.commons.network;

public abstract class SendablePacket<T extends MMOClient<?>> extends AbstractPacket<T>
{
	protected void putShort(int value)
	{
		getByteBuffer().putShort((short) value);
	}

	protected void putInt(int value)
	{
		getByteBuffer().putInt(value);
	}

	protected void putDouble(double value)
	{
		getByteBuffer().putDouble(value);
	}

	protected void putFloat(float value)
	{
		getByteBuffer().putFloat(value);
	}

	protected void writeC(int data)
	{
		getByteBuffer().put((byte) data);
	}

	protected void writeF(double value)
	{
		getByteBuffer().putDouble(value);
	}

	protected void writeH(int value)
	{
		getByteBuffer().putShort((short) value);
	}

	protected void writeD(int value)
	{
		getByteBuffer().putInt(value);
	}
	
	protected void writeD(long value)
	{
		int v = value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
		getByteBuffer().putInt(v);
	}
	
	protected void writeQ(long value)
	{
		getByteBuffer().putLong(value);
	}

	protected void writeB(byte[] data)
	{
		getByteBuffer().put(data);
	}

	protected void writeS(CharSequence charSequence)
	{
		if(charSequence == null)
			charSequence = "";

		int length = charSequence.length();
		for(int i = 0; i < length; i++)
			getByteBuffer().putChar(charSequence.charAt(i));
		getByteBuffer().putChar('\000');
	}

	protected abstract void write();

	protected abstract int getHeaderSize();

	protected abstract void writeHeader(int dataSize);

	public void runImpl()
	{}
}
