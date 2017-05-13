package ru.l2gw.fakeserver.network.clientpackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.network.ReceivablePacket;
import ru.l2gw.fakeserver.network.FakeClient;
import ru.l2gw.fakeserver.network.serverpackets.ServerPacket;

/**
 * @author: rage
 * @date: 18.04.13 13:30
 */
public abstract class ClientPacket extends ReceivablePacket<FakeClient>
{
	protected static Log _log = LogFactory.getLog("network");

	@Override
	protected boolean read()
	{
		try
		{
			readImpl();
			return true;
		}
		catch(Exception e)
		{
			_log.fatal("Client: " + getClient().toString() + " from IP: " + getClient().getIpAddr() + " - Failed reading: " + getType() + "(" + getClass().getName() + ")");
			e.printStackTrace();

			handleIncompletePacket();
		}
		return false;
	}

	protected abstract void readImpl() throws Exception;

	@Override
	public void run()
	{
		FakeClient client = getClient();
		try
		{
			runImpl();
		}
		catch(Exception e)
		{
			_log.fatal("Client: " + client.toString() + " from IP: " + client.getIpAddr() + " - Failed running: " + getType());
			e.printStackTrace();

			handleIncompletePacket();
		}
	}

	protected abstract void runImpl() throws Exception;

	protected void sendPacket(ServerPacket gsp)
	{
		getClient().sendPacket(gsp);
	}

	public boolean checkReadArray(int expected_elements, int element_size, boolean _debug)
	{
		int expected_size = expected_elements * element_size;
		boolean result = expected_size < 0 ? false : _buf.remaining() >= expected_size;
		if(!result && _debug)
			_log.error("Buffer Underflow Risk in [" + getType() + "], Client: " + getClient().toString() + " from IP: " + getClient().getIpAddr() + " - Buffer Size: " + _buf.remaining() + " / Expected Size: " + expected_size);
		return result;
	}

	public boolean checkReadArray(int expected_elements, int element_size)
	{
		return checkReadArray(expected_elements, element_size, true);
	}

	public void handleIncompletePacket()
	{
		FakeClient client = getClient();
		_log.warn("Packet not completed. Maybe cheater. IP:" + client.getIpAddr());
	}

	public final String getType()
	{
		return getClass().getSimpleName();
	}
}