package ru.l2gw.gameserver.clientpackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.network.ReceivablePacket;
import ru.l2gw.extensions.ccpGuard.ConfigProtect;
import ru.l2gw.gameserver.GameServer;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.network.GameClient;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;

/**
 * Packets received by the game server from clients
 */
public abstract class L2GameClientPacket extends ReceivablePacket<GameClient>
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
			_log.fatal("Client: " + getClient().toString() + " from IP: " + getClient().getIpAddr() + " - Failed reading: " + getType() + "(" + getClass().getName() + ") - Server Version: " + GameServer.getVersion().getRevisionNumber());
			if(ConfigProtect.PROTECT_ENABLE && !getClient().getHWID().isEmpty())
				_log.fatal("Client HWID: " + getClient().getHWID());
			e.printStackTrace();

			handleIncompletePacket();
		}
		return false;
	}

	protected abstract void readImpl() throws Exception;

	@Override
	public void run()
	{
		GameClient client = getClient();
		try
		{
			runImpl();
		}
		catch(Exception e)
		{
			_log.fatal("Client: " + client.toString() + " from IP: " + client.getIpAddr() + " - Failed running: " + getType() + " - Server Version: " + GameServer.getVersion().getRevisionNumber());
			if(ConfigProtect.PROTECT_ENABLE && !getClient().getHWID().isEmpty())
				_log.fatal("Client HWID: " + getClient().getHWID());
			e.printStackTrace();

			handleIncompletePacket();
		}
	}

	protected abstract void runImpl() throws Exception;

	protected void sendPacket(L2GameServerPacket gsp)
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
		GameClient client = getClient();
		_log.warn("Packet not completed. Maybe cheater. IP:" + client.getIpAddr());
		if(ConfigProtect.PROTECT_ENABLE && !getClient().getHWID().isEmpty())
			_log.warn("Client HWID: " + client.getHWID());
		if(client.getUPTryes() > 4)
		{
			L2Player player = client.getPlayer();
			if(player == null)
			{
				_log.warn("Too many incomplete packets, connection closed. IP: " + client.getIpAddr() + ", account:" + client.getLoginName());
				client.closeNow(true);
				return;
			}
			_log.warn("Too many incomplete packets, connection closed. IP: " + client.getIpAddr() + ", account:" + client.getLoginName() + ", character:" + player.getName());
			player.logout(false, false, true);
		}
		else
			client.addUPTryes();
	}

	/**
	 * @return A String with this packet name for debuging purposes
	 */
	public String getType()
	{
		return getClass().getSimpleName();
	}
}
