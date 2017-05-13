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

import javolution.io.UTF8StreamReader;
import javolution.util.FastMap;
import javolution.xml.stream.XMLStreamConstants;
import javolution.xml.stream.XMLStreamException;
import javolution.xml.stream.XMLStreamReaderImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.network.utils.AdvIP;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.loginserver.gameservercon.GameServerInfo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.RSAKeyGenParameterSpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

/**
 * @author KenM
 */
public class GameServerTable
{
	private static Log _log = LogFactory.getLog(GameServerTable.class.getName());
	private static GameServerTable _instance;

	// Server Names Config
	static FastMap<Integer, String> _serverNames = new FastMap<Integer, String>();

	// Game Server Table
	private final Map<Integer, GameServerInfo> _gameServerTable = new FastMap<Integer, GameServerInfo>().shared();

	// RSA Config
	private static final int KEYS_SIZE = 10;
	private KeyPair[] _keyPairs;
	private Random _rnd = new Random();

	public static void load() throws SQLException, GeneralSecurityException
	{
		if(_instance == null)
			_instance = new GameServerTable();
		else
			throw new IllegalStateException("Load can only be invoked a single time.");
	}

	public static GameServerTable getInstance()
	{
		return _instance;
	}

	public GameServerTable() throws SQLException, NoSuchAlgorithmException, InvalidAlgorithmParameterException
	{
		loadServerNames();
		_log.info("Loaded " + _serverNames.size() + " server names");

		loadRegisteredGameServers();
		_log.info("Loaded " + _gameServerTable.size() + " registered Game Servers");

		loadRSAKeys();
		_log.info("Cached " + _keyPairs.length + " RSA keys for Game Server communication.");
	}

	private void loadRSAKeys() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException
	{
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(512, RSAKeyGenParameterSpec.F4);
		keyGen.initialize(spec);

		_keyPairs = new KeyPair[KEYS_SIZE];
		for(int i = 0; i < KEYS_SIZE; i++)
			_keyPairs[i] = keyGen.genKeyPair();
	}

	private void loadServerNames()
	{
		InputStream in = null;
		try
		{
			in = new FileInputStream("servername.xml");
			XMLStreamReaderImpl xpp = new XMLStreamReaderImpl();
			xpp.setInput(new UTF8StreamReader().setInput(in));
			for(int e = xpp.getEventType(); e != XMLStreamConstants.END_DOCUMENT; e = xpp.next())
				if(e == XMLStreamConstants.START_ELEMENT)
					if(xpp.getLocalName().toString().equals("server"))
					{
						Integer id = new Integer(xpp.getAttributeValue(null, "id").toString());
						String name = xpp.getAttributeValue(null, "name").toString();
						_serverNames.put(id, name);
					}
		}
		catch(FileNotFoundException e)
		{
			_log.warn("servername.xml could not be loaded: file not found");
		}
		catch(XMLStreamException xppe)
		{
			xppe.printStackTrace();
		}
		finally
		{
			try
			{
				if(in != null)
					in.close();
			}
			catch(Exception e)
			{}
		}
	}

	private void loadRegisteredGameServers() throws SQLException
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		int id;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM gameservers");
			rset = statement.executeQuery();
			GameServerInfo gsi;
			while(rset.next())
			{
				id = rset.getInt("server_id");
				gsi = new GameServerInfo(id, stringToHex(rset.getString("hexid")));
				_gameServerTable.put(id, gsi);
			}
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public Map<Integer, GameServerInfo> getRegisteredGameServers()
	{
		return _gameServerTable;
	}

	public GameServerInfo getRegisteredGameServerById(int id)
	{
		return _gameServerTable.get(id);
	}

	public boolean hasRegisteredGameServerOnId(int id)
	{
		return _gameServerTable.containsKey(id);
	}

	public boolean registerWithFirstAvailableId(GameServerInfo gsi)
	{
		// avoid two servers registering with the same "free" id
		synchronized (_gameServerTable)
		{
			for(Entry<Integer, String> entry : _serverNames.entrySet())
				if(!_gameServerTable.containsKey(entry.getKey()))
				{
					_gameServerTable.put(entry.getKey(), gsi);
					gsi.setId(entry.getKey());
					return true;
				}
		}
		return false;
	}

	public boolean register(int id, GameServerInfo gsi)
	{
		// avoid two servers registering with the same id
		synchronized (_gameServerTable)
		{
			if(!_gameServerTable.containsKey(id))
			{
				_gameServerTable.put(id, gsi);
				gsi.setId(id);
				return true;
			}
		}
		return false;
	}

	public void registerServerOnDB(GameServerInfo gsi)
	{
		this.registerServerOnDB(gsi.getHexId(), gsi.getId(), gsi.getExternalHost());
	}

	public void registerServerOnDB(byte[] hexId, int id, String externalHost)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("INSERT INTO gameservers (hexid,server_id,host) values (?,?,?)");
			statement.setString(1, hexToString(hexId));
			statement.setInt(2, id);
			statement.setString(3, externalHost);
			statement.executeUpdate();
		}
		catch(SQLException e)
		{
			_log.warn("SQL error while saving gameserver: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public String getServerNameById(int id)
	{
		return getServerNames().get(id);
	}

	public Map<Integer, String> getServerNames()
	{
		return _serverNames;
	}

	public KeyPair getKeyPair()
	{
		return _keyPairs[_rnd.nextInt(10)];
	}

	private byte[] stringToHex(String string)
	{
		return new BigInteger(string, 16).toByteArray();
	}

	private String hexToString(byte[] hex)
	{
		if(hex == null)
			return "null";
		return new BigInteger(hex).toString(16);
	}

	public List<String> status()
	{
		List<String> str = new ArrayList<String>();
		str.add("There are " + _gameServerTable.size() + " GameServers");
		for(GameServerInfo gsi : _gameServerTable.values())
			str.add(gsi.toString());
		return str;
	}

	public Boolean CheckSubNet(String ip, AdvIP advip)
	{
		String[] temp = ip.split("\\.");
		String[] temp2 = advip.bitmask.split("\\.");
		String result = "";
		for(int i = 0; i < temp.length; i++)
			result += (Integer.valueOf(temp[i]) & Integer.valueOf(temp2[i])) + ".";
		return result.equals(advip.ipmask.replace("\\.", "") + ".");
	}
}
