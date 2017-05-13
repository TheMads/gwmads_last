package ru.l2gw.commons.network;

import java.nio.ByteBuffer;

@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class MMOClient<T extends MMOConnection>
{
	private T _connection;

	@SuppressWarnings("unchecked")
	public MMOClient(T con)
	{
		this.setConnection(con);
		con.setClient(this);
	}

	public void setConnection(T con)
	{
		_connection = con;
	}

	public T getConnection()
	{
		return _connection;
	}

	public void closeNow(boolean error)
	{
		if(this.getConnection() != null)
			this.getConnection().closeNow(error);
	}

	@SuppressWarnings("unchecked")
	public void close(SendablePacket packet)
	{
		if(this.getConnection() != null)
			this.getConnection().close(packet);
	}

	public void closeLater()
	{
		this.getConnection().closeLater();
	}

	public abstract boolean decrypt(ByteBuffer buf, int size);

	public abstract boolean encrypt(ByteBuffer buf, int size);

	public abstract Enum getState();

	protected void onDisconnection()
	{}

	protected void onForcedDisconnection()
	{}
}
