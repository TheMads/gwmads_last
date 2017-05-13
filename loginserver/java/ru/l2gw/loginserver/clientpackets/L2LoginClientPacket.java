package ru.l2gw.loginserver.clientpackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.network.ReceivablePacket;
import ru.l2gw.loginserver.L2LoginClient;

/**
 *
 * @author KenM
 */
public abstract class L2LoginClientPacket extends ReceivablePacket<L2LoginClient>
{
	protected static Log _log = LogFactory.getLog(L2LoginClientPacket.class.getName());

	/**
	 * @see ru.l2gw.extensions.network.ReceivablePacket#read()
	 */
	@Override
	protected final boolean read()
	{
		try
		{
			return readImpl();
		}
		catch(Exception e)
		{
			_log.warn("ERROR READING: " + this.getClass().getSimpleName());
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void run()
	{
		try
		{
			runImpl();
		}
		catch(Exception e)
		{
			_log.warn("runImpl error: Client: " + getClient().toString());
			e.printStackTrace();
		}
	}

	protected abstract boolean readImpl();

	protected abstract void runImpl() throws Exception;
}
