package ru.l2gw.extensions.ccpGuard;

import ru.l2gw.extensions.ccpGuard.crypt.ProtectionCrypt;
import ru.l2gw.extensions.ccpGuard.packets.ProtectOnline;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.network.GameClient;
import ru.l2gw.util.Util;

import java.util.concurrent.ScheduledFuture;

public class ProtectInfo
{
	public static enum ProtectionClientState
	{
		ENABLED, DISABLED, LIMITED
	}

	private ProtectionClientState protectState;
	private GameClient client;
	private int _protectPenalty = 0;
	private String _loginName = "";
	private String _playerName = "";
	private int _playerId = 0;

	private String HWID = "";
	private String IP = "";
	private ScheduledFuture<?> _onlinerTask = null;
	public final ProtectionCrypt GGdec = new ProtectionCrypt();
	public boolean GGReaded = false;
	public boolean protect_used = false;

	public ProtectInfo(GameClient client, String ip, boolean offline)
	{
		protect_used = ConfigProtect.PROTECT_ENABLE;

		byte[] newKey = new byte[24];
		for(int i = 0; i < 6; i++)
		{
			int val = ProtectionCrypt.getValue(i);
			Util.intToBytes(val, newKey, i * 4);
		}
		GGdec.setKey(newKey);

		if(offline)
			protectState = ProtectionClientState.ENABLED;
		else
			protectState = ProtectionClientState.DISABLED;

		IP = ip;
		if(protect_used)
		{
			protect_used = !ConfigProtect.PROTECT_UNPROTECTED_IPS.isIpInNets(ip);
			this.client = client;
		}
	}

	public final GameClient getClient()
	{
		return client;
	}

	public final String getHWID()
	{
		return HWID;
	}

	public void setHWID(final String hwid)
	{
		HWID = hwid;
	}

	public int getProtectPenalty()
	{
		return _protectPenalty;
	}

	public void setProtectPenalty(int protectPenalty)
	{
		_protectPenalty = protectPenalty;
	}

	public void addProtectPenalty(int protectPenalty)
	{
		_protectPenalty = _protectPenalty + protectPenalty;
	}

	public ProtectionClientState getProtectState()
	{
		return protectState;
	}

	public void setProtectState(ProtectionClientState protectState)
	{
		this.protectState = protectState;
	}

	public final String getLoginName()
	{
		return _loginName;
	}

	public void setLoginName(final String name)
	{
		_loginName = name;
	}

	public final String getPlayerName()
	{
		return _playerName;
	}

	public void setPlayerName(final String name)
	{
		_playerName = name;
	}

	public int getPlayerId()
	{
		return _playerId;
	}

	public void setPlayerId(int plId)
	{
		_playerId = plId;
	}

	public String getIpAddr()
	{
		return IP;
	}

	@Override
	public String toString()
	{
		return "pi[login=" + _loginName + (_playerName.isEmpty() ? "" : ";player=" + _playerName + ";obj_id=" + _playerId) + ";HWID=" + HWID + ";IP=" + IP + "]";
	}

	public void startOnlinerTask()
	{
		stopOnlinerTask(true);
		if(ConfigProtect.PROTECT_ONLINE_PACKET_TIME > 0)
			_onlinerTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Onliner(client), 5000, ConfigProtect.PROTECT_ONLINE_PACKET_TIME);
	}

	public void stopOnlinerTask(final boolean mayInterruptIfRunning)
	{
		if(_onlinerTask != null)
		{
			try
			{
				_onlinerTask.cancel(mayInterruptIfRunning);
			}
			catch(Exception e)
			{
			}
			_onlinerTask = null;
		}
	}

	private class Onliner implements Runnable
	{
		private final GameClient targetClient;

		public Onliner(GameClient client)
		{
			this.targetClient = client;
		}

		public void run()
		{
			try
			{
				if(targetClient._prot_info.protect_used)
					targetClient.sendPacket(new ProtectOnline());
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}


}
