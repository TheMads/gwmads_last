package ru.l2gw.gameserver.cache;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2Alliance;
import ru.l2gw.gameserver.model.L2Clan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class CrestCache
{
	private static final Log _log = LogFactory.getLog(CrestCache.class.getName());

	// Требуется для получения ID значка по ID клана
	private static final TIntIntHashMap _cachePledge = new TIntIntHashMap();
	private static final TIntIntHashMap _cachePledgeLarge = new TIntIntHashMap();
	private static final TIntIntHashMap _cacheAlly = new TIntIntHashMap();

	// Integer - ID значка, byte[] - сам значек
	private static final TIntObjectHashMap<byte[]> _cachePledgeHashed = new TIntObjectHashMap<>();
	private static final TIntObjectHashMap<byte[]> _cachePledgeLargeHashed = new TIntObjectHashMap<>();
	private static final TIntObjectHashMap<byte[]> _cacheAllyHashed = new TIntObjectHashMap<>();

	private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private static final Lock readLock = lock.readLock();
	private static final Lock writeLock = lock.writeLock();

	public static void load()
	{
		int count = 0, pledgeId;
		byte[] crest;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet list = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("SELECT `clan_id`,`crest`,`largecrest` FROM `clan_data` WHERE `crest` IS NOT NULL or `largecrest` IS NOT NULL");
			list = statement.executeQuery();
			while(list.next())
			{
				pledgeId = list.getInt("clan_id");
				crest = list.getBytes("crest");
				if(crest != null)
				{
					count++;
					int hash = mhash(crest);
					_cachePledge.put(pledgeId, hash);
					_cachePledgeHashed.put(hash, crest);
				}

				crest = list.getBytes("largecrest");
				if(crest != null)
				{
					count++;
					int hash = mhash(crest);
					_cachePledgeLarge.put(pledgeId, hash);
					_cachePledgeLargeHashed.put(hash, crest);
				}
			}
			DbUtils.closeQuietly(statement, list);

			statement = con.prepareStatement("SELECT `ally_id`,`crest` FROM `ally_data` WHERE `crest` IS NOT NULL");
			list = statement.executeQuery();
			while(list.next())
			{
				crest = list.getBytes("crest");
				count++;
				int hash = mhash(crest);
				_cacheAlly.put(list.getInt("ally_id"), hash);
				_cacheAllyHashed.put(hash, crest);
			}
			DbUtils.closeQuietly(statement, list);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, list);
		}
		_log.info("CrestCache: Loaded " + count + " crests");
	}

	public static byte[] getPledgeCrest(int id)
	{
		byte[] crest = null;

		readLock.lock();
		try
		{
			crest = _cachePledgeHashed.get(id);
		}
		finally
		{
			readLock.unlock();
		}

		return crest;
	}

	public static byte[] getPledgeCrestLarge(int id)
	{
		byte[] crest = null;

		readLock.lock();
		try
		{
			crest = _cachePledgeLargeHashed.get(id);
		}
		finally
		{
			readLock.unlock();
		}

		return crest;
	}

	public static byte[] getAllyCrest(int id)
	{
		byte[] crest = null;

		readLock.lock();
		try
		{
			crest = _cacheAllyHashed.get(id);
		}
		finally
		{
			readLock.unlock();
		}

		return crest;
	}

	public static int getPledgeCrestId(int clan_id)
	{
		int crest = 0;

		readLock.lock();
		try
		{
			crest = _cachePledge.get(clan_id);
		}
		finally
		{
			readLock.unlock();
		}

		return crest;
	}

	public static int getPledgeCrestLargeId(int clan_id)
	{
		int crest = 0;

		readLock.lock();
		try
		{
			crest = _cachePledgeLarge.get(clan_id);
		}
		finally
		{
			readLock.unlock();
		}

		return crest;
	}

	public static int getAllyCrestId(int ally_id)
	{
		int crest = 0;

		readLock.lock();
		try
		{
			crest = _cacheAlly.get(ally_id);
		}
		finally
		{
			readLock.unlock();
		}

		return crest;
	}

	public static void removePledgeCrest(L2Clan clan)
	{
		clan.setCrestId(0);

		writeLock.lock();
		try
		{
			_cachePledgeHashed.remove(_cachePledge.remove(clan.getClanId()));
		}
		finally
		{
			writeLock.unlock();
		}

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET crest=? WHERE clan_id=?");
			statement.setNull(1, Types.VARBINARY);
			statement.setInt(2, clan.getClanId());
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

	public static void removePledgeCrestLarge(L2Clan clan)
	{
		clan.setCrestLargeId(0);

		writeLock.lock();
		try
		{
			_cachePledgeLargeHashed.remove(_cachePledgeLarge.remove(clan.getClanId()));
		}
		finally
		{
			writeLock.unlock();
		}

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET largecrest=? WHERE clan_id=?");
			statement.setNull(1, Types.VARBINARY);
			statement.setInt(2, clan.getClanId());
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

	public static void removeAllyCrest(L2Alliance ally)
	{
		ally.setAllyCrestId(0);

		writeLock.lock();
		try
		{
			_cacheAllyHashed.remove(_cacheAlly.remove(ally.getAllyId()));
		}
		finally
		{
			writeLock.unlock();
		}

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE ally_data SET crest=? WHERE ally_id=?");
			statement.setNull(1, Types.VARBINARY);
			statement.setInt(2, ally.getAllyId());
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

	public static void savePledgeCrest(L2Clan clan, byte[] data)
	{
		int hash = mhash(data);
		clan.setCrestId(hash);

		writeLock.lock();
		try
		{
			_cachePledgeHashed.put(hash, data);
			_cachePledge.put(clan.getClanId(), hash);
		}
		finally
		{
			writeLock.unlock();
		}

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET crest=? WHERE clan_id=?");
			statement.setBytes(1, data);
			statement.setInt(2, clan.getClanId());
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

	public static void savePledgeCrestLarge(L2Clan clan, byte[] data)
	{
		int hash = mhash(data);
		clan.setCrestLargeId(hash);

		writeLock.lock();
		try
		{
			_cachePledgeLargeHashed.put(hash, data);
			_cachePledgeLarge.put(clan.getClanId(), hash);
		}
		finally
		{
			writeLock.unlock();
		}

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET largecrest=? WHERE clan_id=?");
			statement.setBytes(1, data);
			statement.setInt(2, clan.getClanId());
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

	public static void saveAllyCrest(L2Alliance ally, byte[] data)
	{
		int hash = mhash(data);
		ally.setAllyCrestId(hash);

		writeLock.lock();
		try
		{
			_cacheAllyHashed.put(hash, data);
			_cacheAlly.put(ally.getAllyId(), hash);
		}
		finally
		{
			writeLock.unlock();
		}

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE ally_data SET crest=? WHERE ally_id=?");
			statement.setBytes(1, data);
			statement.setInt(2, ally.getAllyId());
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

	public static int mhash(byte[] data)
	{
		int ret = 0;
		if(data != null)
			for(byte element : data)
				ret = 7 * ret + element;
		return Math.abs(ret);
	}
}
