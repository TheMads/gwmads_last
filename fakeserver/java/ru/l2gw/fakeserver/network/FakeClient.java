package ru.l2gw.fakeserver.network;

import ru.l2gw.commons.network.MMOClient;
import ru.l2gw.commons.network.MMOConnection;
import ru.l2gw.fakeserver.network.serverpackets.ServerPacket;

import java.nio.ByteBuffer;

/**
 * @author: rage
 * @date: 18.04.13 13:20
 */
public class FakeClient extends MMOClient<MMOConnection<FakeClient>>
{
	private final MMOConnection<FakeClient> connection;

	public FakeClient(MMOConnection<FakeClient> con)
	{
		super(con);
		connection = con;
	}

	@Override
	public boolean decrypt(ByteBuffer buf, int size)
	{
		return true;
	}

	@Override
	public boolean encrypt(ByteBuffer buf, int size)
	{
		buf.position(buf.position() + size);
		return true;
	}

	@Override
	public Enum getState()
	{
		return null;
	}

	public void sendPacket(ServerPacket gsp)
	{
		if(getConnection() == null)
			return;
		getConnection().sendPacket(gsp);
	}

	public String getIpAddr()
	{
		try
		{
			return connection.getSocket().getInetAddress().getHostAddress();
		}
		catch(NullPointerException e)
		{
			return "Disconnected";
		}
	}

	@Override
	public String toString()
	{
		return "FakeClient{IP=" + getIpAddr() + "}";
	}
}
