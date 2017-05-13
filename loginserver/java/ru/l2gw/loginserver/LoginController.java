package ru.l2gw.loginserver;

import javolution.util.FastCollection.Record;
import javolution.util.FastMap;
import javolution.util.FastSet;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.network.utils.NetList;
import ru.l2gw.commons.utils.Base64;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.commons.utils.StringUtil;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.loginserver.crypt.DES;
import ru.l2gw.loginserver.crypt.ScrambledKeyPair;
import ru.l2gw.loginserver.gameservercon.AttGS;
import ru.l2gw.loginserver.gameservercon.GameServerInfo;
import ru.l2gw.loginserver.serverpackets.LoginFail.LoginFailReason;

import javax.crypto.Cipher;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Random;

public class LoginController
{
	protected static org.apache.commons.logging.Log _log = LogFactory.getLog(LoginController.class.getName());
	protected static org.apache.commons.logging.Log logFails = LogFactory.getLog("loginfails");
	protected static org.apache.commons.logging.Log logLogin = LogFactory.getLog("login");

	private static LoginController _instance;

	/**
	 * Time before kicking the client if he didnt logged yet
	 */
	private final static int LOGIN_TIMEOUT = 60000;

	/**
	 * Clients that are on the LS but arent assocated with a account yet
	 */
	protected final FastSet<L2LoginClient> _clients = new FastSet<>();

	/**
	 * Authed Clients on LoginServer
	 */
	protected final FastMap<String, L2LoginClient> _loginServerClients = new FastMap<String, L2LoginClient>().shared();

	private Map<InetAddress, BanInfo> _bannedIps = new FastMap<InetAddress, BanInfo>().shared();

	private Map<InetAddress, FailedLoginAttempt> _hackProtection;

	protected ScrambledKeyPair[] _keyPairs;

	private Random _rnd = new Random();

	protected byte[][] _blowfishKeys;
	private static final int BLOWFISH_KEYS = 20;

	public static enum State
	{
		VALID,
		WRONG,
		NOT_PAID,
		BANNED,
		IN_USE,
		IP_ACCESS_DENIED,
		TEMP_PASSWORD
	}

	public class Status
	{
		public int _premiumExpire;
		public State _state;
		public String _allowIps;

		public Status setPremiumExpire(int premiumExpire)
		{
			_premiumExpire = premiumExpire;
			return this;
		}

		public Status setState(State state)
		{
			_state = state;
			return this;
		}
	}

	public static void load() throws GeneralSecurityException
	{
		if(_instance == null)
			_instance = new LoginController();
		else
			throw new IllegalStateException("LoginController can only be loaded a single time.");
	}

	public static LoginController getInstance()
	{
		return _instance;
	}

	private LoginController() throws GeneralSecurityException
	{
		_log.info("Loading LoginController...");

		_hackProtection = new FastMap<InetAddress, FailedLoginAttempt>().shared();

		_keyPairs = new ScrambledKeyPair[10];

		KeyPairGenerator keygen;

		keygen = KeyPairGenerator.getInstance("RSA");
		RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4);
		keygen.initialize(spec);

		//generate the initial set of keys
		for(int i = 0; i < 10; i++)
			_keyPairs[i] = new ScrambledKeyPair(keygen.generateKeyPair());
		_log.info("Cached 10 KeyPairs for RSA communication");

		testCipher((RSAPrivateKey) _keyPairs[0]._pair.getPrivate());

		// Store keys for blowfish communication
		generateBlowFishKeys();
	}

	/**
	 * This is mostly to force the initialization of the Crypto Implementation, avoiding it being done on runtime when its first needed.<BR>
	 * In short it avoids the worst-case execution time on runtime by doing it on loading.
	 *
	 * @param key Any private RSA Key just for testing purposes.
	 * @throws GeneralSecurityException if a underlying exception was thrown by the Cipher
	 */
	private void testCipher(RSAPrivateKey key) throws GeneralSecurityException
	{
		// avoid worst-case execution, KenM
		Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
		rsaCipher.init(Cipher.DECRYPT_MODE, key);
	}

	private void generateBlowFishKeys()
	{
		_blowfishKeys = new byte[BLOWFISH_KEYS][16];

		for(int i = 0; i < BLOWFISH_KEYS; i++)
			for(int j = 0; j < _blowfishKeys[i].length; j++)
				_blowfishKeys[i][j] = (byte) (_rnd.nextInt(255) + 1);
		_log.info("Stored " + _blowfishKeys.length + " keys for Blowfish communication");
	}

	/**
	 * @return Returns a random key
	 */
	public byte[] getBlowfishKey()
	{
		return _blowfishKeys[(int) (Math.random() * BLOWFISH_KEYS)];
	}

	public void removeLoginClient(L2LoginClient client)
	{
		synchronized(_clients)
		{
			_clients.remove(client);
		}
	}

	public SessionKey assignSessionKeyToClient()
	{
		return new SessionKey(_rnd.nextInt(), _rnd.nextInt(), _rnd.nextInt(), _rnd.nextInt());
	}

	public void addAuthedLoginClient(String account, L2LoginClient client)
	{
		synchronized(_loginServerClients)
		{
			_loginServerClients.put(account, client);
		}
	}

	public L2LoginClient removeAuthedLoginClient(String account)
	{
		synchronized(_loginServerClients)
		{
			return _loginServerClients.remove(account);
		}
	}

	public boolean isAccountInLoginServer(String account)
	{
		synchronized(_loginServerClients)
		{
			return _loginServerClients.containsKey(account);
		}
	}

	public L2LoginClient getAuthedClient(String account)
	{
		synchronized(_loginServerClients)
		{
			return _loginServerClients.get(account);
		}
	}

	public Status tryAuthLogin(String account, String password, L2LoginClient client)
	{
		Status ret = loginValid(account, password, client);

		if(ret._state != State.VALID)
			return ret;

		if(!isAccountInLoginServer(account) && !isAccountInAnyGameServer(account))
		{
			// dont allow 2 simultaneous login
			synchronized(_loginServerClients)
			{
				if(!_loginServerClients.containsKey(account))
					addAuthedLoginClient(account, client);
				else
					ret._state = State.IN_USE;
			}

			// was login successful?
			if(ret._state == State.VALID)
				// remove him from the non-authed list
				removeLoginClient(client);
		}
		else
			ret._state = State.IN_USE;

		return ret;
	}

	/**
	 * Adds the address to the ban list of the login server, with the given duration.
	 *
	 * @param address  The Address to be banned.
	 * @param duration is miliseconds
	 */
	public void addBanForAddress(InetAddress address, long duration)
	{
		_bannedIps.put(address, new BanInfo(address, System.currentTimeMillis() + duration));
	}

	public boolean isBannedAddress(InetAddress address)
	{
		BanInfo bi = _bannedIps.get(address);
		if(bi != null)
		{
			if(bi.hasExpired())
			{
				_bannedIps.remove(address);
				return false;
			}
			logFails.info((address != null ? address.getHostAddress() : "") + " banned, expire time: " + new Date(bi._expiration));
			return true;
		}
		return false;
	}

	/**
	 * Remove the specified address from the ban list
	 *
	 * @param address The address to be removed from the ban list
	 * @return true if the ban was removed, false if there was no ban for this ip
	 */
	public boolean removeBanForAddress(InetAddress address)
	{
		return _bannedIps.remove(address) != null;
	}

	public boolean isAccountInAnyGameServer(String account)
	{
		Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();
		for(GameServerInfo gsi : serverList)
		{
			AttGS gst = gsi.getGameServer();
			if(gst != null && gst.isAccountInGameServer(account))
				return true;
		}
		return false;
	}

	public GameServerInfo getAccountOnGameServer(String account)
	{
		Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();
		for(GameServerInfo gsi : serverList)
		{
			AttGS gst = gsi.getGameServer();
			if(gst != null && gst.isAccountInGameServer(account))
				return gsi;
		}
		return null;
	}

	public boolean isLoginPossible(L2LoginClient client, int serverId)
	{
		GameServerInfo gsi = GameServerTable.getInstance().getRegisteredGameServerById(serverId);
		int access = client.getAccessLevel();
		boolean loginOk = gsi != null && gsi.isAuthed() && (gsi.getCurrentPlayerCount() < gsi.getMaxPlayers() || access >= 50);
		if(loginOk && client.getLastServer() != serverId)
		{
			Connection con = null;
			Statement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.createStatement();
				statement.executeUpdate("UPDATE accounts SET lastServer = " + serverId + " WHERE login = '" + client.getAccount() + "'");
			}
			catch(Exception e)
			{
				_log.warn("Could not execute update: " + e);
				e.printStackTrace();
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
		return loginOk;
	}

	public void setAccountAccessLevel(String user, int banLevel, String comments, int banTime)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			String stmt = "UPDATE accounts SET access_level = ?, comments = ?, banExpires = ? WHERE login=?";
			statement = con.prepareStatement(stmt);
			statement.setInt(1, banLevel);
			statement.setString(2, comments);
			statement.setInt(3, banTime);
			statement.setString(4, user);
			statement.executeUpdate();
		}
		catch(Exception e)
		{
			_log.warn("Could not set accessLevel: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public boolean isGM(String user)
	{
		boolean ok = false;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT access_level FROM accounts WHERE login=?");
			statement.setString(1, user);
			rset = statement.executeQuery();
			if(rset.next())
			{
				int accessLevel = rset.getInt(1);
				if(accessLevel >= 100)
					ok = true;
			}
		}
		catch(Exception e)
		{
			//_log.warn("could not check gm state:"+e);
			ok = false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return ok;
	}

	/**
	 * <p>This method returns one of the cached {@link ScrambledKeyPair ScrambledKeyPairs} for communication with Login Clients.</p>
	 *
	 * @return a scrambled keypair
	 */
	public ScrambledKeyPair getScrambledRSAKeyPair()
	{
		return _keyPairs[_rnd.nextInt(10)];
	}

	public Status loginValid(String user, String password, L2LoginClient client)// throws HackingException
	{
		Status ok = new Status().setState(State.WRONG);

		if(!StringUtil.isMatchingRegexp(user, Config.ANAME_TEMPLATE))
		{
			logFails.info("'" + user + "' wrong login template " + client.getIpAddress());
			return ok;
		}
		if(!StringUtil.isMatchingRegexp(password, Config.APASSWD_TEMPLATE))
		{
			logFails.info("'" + user + "' wrong password template " + client.getIpAddress());
			return ok;
		}

		InetAddress address = client.getConnection().getSocket().getInetAddress();
		logLogin.info("'" + (user == null ? "null" : user) + "' " + (address == null ? "null" : address.getHostAddress()));

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			MessageDigest md = MessageDigest.getInstance("SHA");
			byte[] raw = password.getBytes("UTF-8");
			byte[] hash = md.digest(raw);

			byte[] expected = null;
			boolean paid = false;
			boolean banned = false;
			String phash = "";

			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM accounts WHERE login=?");
			statement.setString(1, user);
			rset = statement.executeQuery();
			if(rset.next())
			{
				if(rset.getBoolean("temp_password") || (Config.PASSWORD_CHANGE_TIME > 0 && rset.getInt("update_password") * 1000L + Config.PASSWORD_CHANGE_TIME < System.currentTimeMillis()))
					return new Status().setState(State.TEMP_PASSWORD);

				String allowedIps = rset.getString("AllowIPs");
				if(allowedIps != null && !allowedIps.isEmpty() && !allowedIps.equals("*"))
				{
					NetList allowedList = new NetList();
					allowedList.LoadFromString(allowedIps, ",");
					if(!allowedList.isIpInNets(client.getIpAddress()))
						return new Status().setState(State.IP_ACCESS_DENIED);
				}
				client.setLastServer(Math.max(rset.getInt("lastServer"), 1));

				phash = rset.getString("password");
				if(phash.equals(""))
					return new Status().setState(State.WRONG);

				expected = Base64.decode(phash);
				paid = rset.getInt("pay_stat") == 1;
				banned = rset.getInt("access_level") < 0;
				long banTime = rset.getLong("banExpires");
				ok.setPremiumExpire(rset.getInt("premiumEndDate"));
				ok._allowIps = allowedIps;
				if(banTime == -1)
					banned = true;
				else if(banTime > 0)
					if(banTime < System.currentTimeMillis() / 1000)
						unBanAcc(user);
					else
						banned = true;

				client.setUseInternalIp(rset.getInt("altIp") > 0);
				client.setAccountId(rset.getInt("account_id"));
				client.setAccessLevel(rset.getInt("access_level"));
				client.setSecondUse(rset.getBoolean("second_use"));
				client.setSecondPass(rset.getString("second_password"));
				client.setFailCount(rset.getInt("second_fail"));

				if(Config.LOGIN_DEBUG)
					_log.debug("account exists");
			}
			DbUtils.closeQuietly(statement, rset);
			if(expected == null)
			{
				if(Config.AUTO_CREATE_ACCOUNTS)
				{
					if(user != null && user.length() >= 2 && user.length() <= 14)
					{
						statement = con.prepareStatement("INSERT INTO accounts (login,password,lastactive,access_level,lastIP,comments) values(?,?,?,?,?,?)");
						statement.setString(1, user);
						statement.setString(2, Base64.encodeBytes(hash));
						statement.setLong(3, System.currentTimeMillis() / 1000);
						statement.setInt(4, 0);
						statement.setString(5, address != null ? address.getHostAddress() : "");
						statement.setString(6, "");
						statement.execute();
						DbUtils.closeQuietly(statement);
						if(Config.LOGIN_DEBUG)
							_log.debug("created new account for " + user);
						return new Status().setState(State.VALID);

					}
					if(Config.LOGIN_DEBUG)
						_log.debug("Invalid username creation/use attempt: " + user);
					return new Status().setState(State.WRONG);
				}
				if(Config.LOGIN_DEBUG)
					_log.debug("account missing for user " + user);

				increaseFiledLoginCount(address, password, user);
				return new Status().setState(State.WRONG);
			}

			ok.setState(State.VALID);
			if(phash.startsWith("0x") && DES.encrypt(password).equalsIgnoreCase(phash))
			{
				statement = con.prepareStatement("UPDATE accounts SET password=? WHERE login=?");
				statement.setString(1, Base64.encodeBytes(hash));
				statement.setString(2, user);
				statement.execute();
				DbUtils.closeQuietly(statement);
			}
			else
				for(int i = 0; i < expected.length; i++)
					if(hash[i] != expected[i])
					{
						ok.setState(State.WRONG);
						break;
					}
			if(!paid)
				return new Status().setState(State.NOT_PAID);
			if(banned)
				return new Status().setState(State.BANNED);
			if(ok._state == State.VALID)
			{
				statement = con.prepareStatement("UPDATE accounts SET lastactive=?, lastIP=? WHERE login=?");
				statement.setLong(1, System.currentTimeMillis() / 1000);
				statement.setString(2, address != null ? address.getHostAddress() : "");
				statement.setString(3, user);
				statement.execute();
			}
		}
		catch(Exception e)
		{
			_log.warn("Could not check password:" + e);
			e.printStackTrace();
			ok.setState(State.WRONG);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		if(ok._state != State.VALID)
			increaseFiledLoginCount(address, password, user);

		return ok;
	}

	private void increaseFiledLoginCount(InetAddress address, String password, String user)
	{
		if(address != null)
		{
			FailedLoginAttempt failedAttempt = _hackProtection.get(address);
			int failedCount;
			if(failedAttempt == null)
			{
				_hackProtection.put(address, new FailedLoginAttempt(address, password));
				failedCount = 1;
			}
			else
			{
				failedAttempt.increaseCounter(password);
				failedCount = failedAttempt.getCount();
			}

			logFails.info("'" + user + "':'" + password + "' " + address.getHostAddress() + " failed count: " + failedCount + "/" + Config.LOGIN_TRY_BEFORE_BAN);

			if(failedCount >= Config.LOGIN_TRY_BEFORE_BAN)
			{
				addBanForAddress(address, Config.LOGIN_BAN_TIME * 60000);
				logFails.info("'" + user + "' " + address.getHostAddress() + " banned for " + Config.LOGIN_BAN_TIME + " min, try limit exceeded");
				_hackProtection.remove(address);
			}
		}
	}

	public void unBanAcc(String name)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE accounts SET access_level = ?, banExpires = ? WHERE login = ?");
			statement.setInt(1, 0);
			statement.setInt(2, 0);
			statement.setString(3, name);
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warn("Cant unban acc " + name + ", " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	class FailedLoginAttempt
	{
		//private InetAddress _ipAddress;
		private int _count;
		private long _lastAttempTime;
		private String _lastPassword;

		public FailedLoginAttempt(@SuppressWarnings("unused") InetAddress address, String lastPassword)
		{
			//_ipAddress = address;
			_count = 1;
			_lastAttempTime = System.currentTimeMillis();
			_lastPassword = lastPassword;
		}

		public void increaseCounter(String password)
		{
			if(!_lastPassword.equals(password))
			{
				// check if theres a long time since last wrong try
				if(System.currentTimeMillis() - _lastAttempTime < Config.LOGIN_CLEAR_FILED_COUNT_TIME)
					_count++;
				else
					// restart the status
					_count = 1;
				_lastPassword = password;
				_lastAttempTime = System.currentTimeMillis();
			}
			else
				_lastAttempTime = System.currentTimeMillis();
		}

		public int getCount()
		{
			return _count;
		}
	}

	class BanInfo
	{
		private InetAddress _ipAddress;
		// Expiration
		private long _expiration;

		public BanInfo(InetAddress ipAddress, long expiration)
		{
			_ipAddress = ipAddress;
			_expiration = expiration;
		}

		public InetAddress getAddress()
		{
			return _ipAddress;
		}

		public boolean hasExpired()
		{
			return System.currentTimeMillis() > _expiration;
		}
	}

	class PurgeThread extends Thread
	{
		@Override
		public void run()
		{
			while(true)
			{
				synchronized(_clients)
				{
					for(Record e = _clients.head(), end = _clients.tail(); (e = e.getNext()) != end;)
					{
						L2LoginClient client = _clients.valueOf(e);
						if(client.getConnectionStartTime() + LOGIN_TIMEOUT >= System.currentTimeMillis())
							client.close(LoginFailReason.REASON_ACCESS_FAILED);
					}
				}

				synchronized(_loginServerClients)
				{
					for(FastMap.Entry<String, L2LoginClient> e = _loginServerClients.head(), end = _loginServerClients.tail(); (e = e.getNext()) != end;)
					{
						L2LoginClient client = e.getValue();
						if(client.getConnectionStartTime() + LOGIN_TIMEOUT >= System.currentTimeMillis())
							client.close(LoginFailReason.REASON_ACCESS_FAILED);
					}
				}

				try
				{
					Thread.sleep(2 * LOGIN_TIMEOUT);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	public boolean ipBlocked(String ipAddress)
	{
		int tries = 0;
		InetAddress ia;
		try
		{
			ia = InetAddress.getByName(ipAddress);
		}
		catch(UnknownHostException e)
		{
			return false;
		}

		if(_hackProtection.containsKey(ia))
			tries = _hackProtection.get(ia).getCount();

		if(tries > Config.LOGIN_TRY_BEFORE_BAN)
		{
			_hackProtection.remove(ia);
			_log.warn("Removed host from hacklist! IP number: " + ipAddress);
			return true;
		}
		return false;
	}

	public boolean setPassword(String account, String password)
	{
		boolean updated = true;
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			MessageDigest md = MessageDigest.getInstance("SHA");
			byte[] raw = password.getBytes("UTF-8");
			byte[] hash = md.digest(raw);
			statement = con.prepareStatement("UPDATE accounts SET password=? WHERE login=?");
			statement.setString(1, Base64.encodeBytes(hash));
			statement.setString(2, account);
			statement.execute();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			updated = false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		return updated;
	}

	public void changePremium(String account, int premiumEndDate)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE accounts SET premiumEndDate=? WHERE login=?");
			statement.setInt(1, premiumEndDate);
			statement.setString(2, account);
			statement.execute();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public boolean updateSecondAuth(String account, String hash)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE accounts SET second_password=? WHERE login=?");
			statement.setString(1, hash);
			statement.setString(2, account);
			statement.execute();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}

		return true;
	}

	public boolean updateSecondFail(String account, int failCount)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE accounts SET second_fail=? WHERE login=?");
			statement.setInt(1, failCount);
			statement.setString(2, account);
			statement.execute();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}

		return true;
	}
}
