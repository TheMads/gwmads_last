/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 * http://www.gnu.org/copyleft/gpl.html
 */
package ru.l2gw.loginserver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.network.MMOClient;
import ru.l2gw.commons.network.MMOConnection;
import ru.l2gw.extensions.ccpGuard.ConfigProtect;
import ru.l2gw.loginserver.crypt.LoginCrypt;
import ru.l2gw.loginserver.crypt.ScrambledKeyPair;
import ru.l2gw.loginserver.serverpackets.AccountKicked;
import ru.l2gw.loginserver.serverpackets.AccountKicked.AccountKickedReason;
import ru.l2gw.loginserver.serverpackets.L2LoginServerPacket;
import ru.l2gw.loginserver.serverpackets.LoginFail;
import ru.l2gw.loginserver.serverpackets.LoginFail.LoginFailReason;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.interfaces.RSAPrivateKey;

/**
 * Represents a client connected into the LoginServer
 *
 * @author KenM
 */
public final class L2LoginClient extends MMOClient<MMOConnection<L2LoginClient>>
{
	private static Log _log = LogFactory.getLog(L2LoginClient.class.getName());

	public static enum LoginClientState
	{
		CONNECTED,
		AUTHED_GG,
		AUTHED_LOGIN
	}

	private LoginClientState _state;
	public boolean PlayOK = false;

	// Crypt
	private LoginCrypt _loginCrypt;
	private ScrambledKeyPair _scrambledPair;
	private byte[] _blowfishKey;

	private String _account;
	private int account_id;
	private int _accessLevel;
	private int _lastServer;
	private SessionKey _sessionKey;
	private int _sessionId;
	private int _premiumExpire;

	private long _connectionStartTime;
	private boolean protect_used = false;
	private boolean _use_internalip = false;
	private String allowedIps;
	private LoginController.State _loginState;
	private String secondPass;
	private int failCount;
	private boolean secondUse;

	public L2LoginClient(MMOConnection<L2LoginClient> con)
	{
		super(con);
		_state = LoginClientState.CONNECTED;
		String ip = getIpAddress();
		protect_used = ConfigProtect.PROTECT_ENABLE;
		if(protect_used)
			protect_used = !ConfigProtect.PROTECT_UNPROTECTED_IPS.isIpInNets(ip);
		_scrambledPair = LoginController.getInstance().getScrambledRSAKeyPair();
		_blowfishKey = LoginController.getInstance().getBlowfishKey();
		_connectionStartTime = System.currentTimeMillis();
		_loginCrypt = new LoginCrypt();
		_loginCrypt.setKey(_blowfishKey, protect_used);
		//_loginCrypt.setKey(_blowfishKey, false);
		_sessionId = con.hashCode();

		if(IpManager.getInstance().CheckIp(ip))
		{
			close(new AccountKicked(AccountKickedReason.REASON_PERMANENTLY_BANNED));
			_log.warn("Drop connection from banned IP: " + ip);
		}
	}

	@Override
	public boolean decrypt(ByteBuffer buf, int size)
	{
		boolean ret;
		try
		{
			ret = _loginCrypt.decrypt(buf.array(), buf.position(), size);
		}
		catch(IOException e)
		{
			e.printStackTrace();
			closeNow(false);
			return false;
		}

		if(!ret)
		{
			byte[] dump = new byte[size];
			System.arraycopy(buf.array(), buf.position(), dump, 0, size);
			_log.warn("Wrong checksum from client: " + toString());
			closeNow(false);
		}

		return ret;
	}

	@Override
	public boolean encrypt(ByteBuffer buf, int size)
	{
		final int offset = buf.position();
		try
		{
			size = _loginCrypt.encrypt(buf.array(), offset, size);
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}

		buf.position(offset + size);
		return true;
	}

	public LoginClientState getState()
	{
		return _state;
	}

	public void setState(LoginClientState state)
	{
		_state = state;
	}

	public byte[] getBlowfishKey()
	{
		return _blowfishKey;
	}

	public byte[] getScrambledModulus()
	{
		if(_scrambledPair == null || _scrambledPair._scrambledModulus == null)
		{
			closeNow(true);
			return null;
		}

		return _scrambledPair._scrambledModulus;
	}

	public RSAPrivateKey getRSAPrivateKey()
	{
		return (RSAPrivateKey) _scrambledPair._pair.getPrivate();
	}

	public String getAccount()
	{
		return _account;
	}

	public void setAccount(String account)
	{
		_account = account;
	}

	public void setAccountId(int id)
	{
		account_id = id;
	}

	public int getAccountId()
	{
		return account_id;
	}

	public void setAccessLevel(int accessLevel)
	{
		_accessLevel = accessLevel;
	}

	public int getAccessLevel()
	{
		return _accessLevel;
	}

	public void setLastServer(int lastServer)
	{
		_lastServer = lastServer;
	}

	public int getLastServer()
	{
		return _lastServer;
	}

	public void setSessionId(int val)
	{
		_sessionId = val;
	}

	public int getSessionId()
	{
		return _sessionId;
	}

	public void setSessionKey(SessionKey sessionKey)
	{
		_sessionKey = sessionKey;
	}

	public SessionKey getSessionKey()
	{
		return _sessionKey;
	}

	public long getConnectionStartTime()
	{
		return _connectionStartTime;
	}

	public void sendPacket(L2LoginServerPacket lsp)
	{
		getConnection().sendPacket(lsp);
	}

	public void close(LoginFailReason reason)
	{
		getConnection().close(new LoginFail(reason));
	}

	public void close(L2LoginServerPacket lsp)
	{
		getConnection().close(lsp);
	}

	@Override
	public void onDisconnection()
	{
		if(Config.LOGIN_DEBUG)
			_log.info("DISCONNECTED: " + toString());

		LoginController lc = LoginController.getInstance();
		if(getState() != LoginClientState.AUTHED_LOGIN)
			lc.removeLoginClient(this);
		else if(!PlayOK && _account != null && lc.isAccountInLoginServer(_account))
			lc.removeAuthedLoginClient(_account);
	}

	@Override
	public String toString()
	{
		InetAddress address = getConnection().getSocket().getInetAddress();
		if(getState() == LoginClientState.AUTHED_LOGIN)
			return "[" + getAccount() + " (" + (address == null ? "disconnected" : address.getHostAddress()) + ")]";
		return "[" + (address == null ? "disconnected" : address.getHostAddress()) + "]";
	}

	public void addBannedIP(String ip, int incorrectCount)
	{
		int bantime = incorrectCount * incorrectCount;
		IpManager.getInstance().BanIp(ip, "Loginserver", bantime, "LoginTryBeforeBan=" + Config.LOGIN_TRY_BEFORE_BAN + " Count=" + incorrectCount);
	}

	public void setPremiumExpire(int premiumExpire)
	{
		_premiumExpire = premiumExpire;
	}

	public int getPremiumExpire()
	{
		return _premiumExpire;
	}

	public void setAllowedIps(String ips)
	{
		allowedIps = ips;
	}

	public String getAllowdIps()
	{
		return allowedIps;
	}

	public String getIpAddress()
	{
		try
		{
			return getConnection().getSocket().getInetAddress().getHostAddress();
		}
		catch(Exception e)
		{
			return "Null IP";
		}
	}

	public void setUseInternalIp(boolean use)
	{
		_use_internalip = use;
	}

	public boolean isUseInternalIp()
	{
		return _use_internalip;
	}

	public void setLoginState(LoginController.State state)
	{
		_loginState = state;
	}

	public LoginController.State getLoginState()
	{
		return _loginState;
	}

	public String getSecondPass()
	{
		return secondPass;
	}

	public int getFailCount()
	{
		return failCount;
	}

	public void setSecondPass(String secondPass)
	{
		this.secondPass = secondPass;
	}

	public void setFailCount(int failCount)
	{
		this.failCount = failCount;
	}

	public boolean isSecondUse()
	{
		return secondUse;
	}

	public void setSecondUse(boolean secondUse)
	{
		this.secondUse = secondUse;
	}
}
