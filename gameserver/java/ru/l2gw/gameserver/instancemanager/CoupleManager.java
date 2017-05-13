package ru.l2gw.gameserver.instancemanager;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.Couple;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.List;

public class CoupleManager
{
	protected static Log _log = LogFactory.getLog(CoupleManager.class.getName());

	private static CoupleManager _instance;

	private List<Couple> _couples;
	private volatile List<Couple> _deletedCouples;

	public static CoupleManager getInstance()
	{
		if(_instance == null)
			new CoupleManager();
		return _instance;
	}

	public CoupleManager()
	{
		_instance = this;
		_log.info("Initializing CoupleManager");
		_instance.load();
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new StoreTask(), 10 * 60 * 1000, 10 * 60 * 1000);
	}

	private void load()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM couples ORDER BY id");
			rs = statement.executeQuery();
			while(rs.next())
			{
				Couple c = new Couple(rs.getInt("id"));
				c.setPlayer1Id(rs.getInt("player1Id"));
				c.setPlayer2Id(rs.getInt("player2Id"));
				c.setMaried(rs.getBoolean("maried"));
				c.setAffiancedDate(rs.getLong("affiancedDate"));
				c.setWeddingDate(rs.getLong("weddingDate"));
				getCouples().add(c);
			}
			_log.info("Loaded: " + getCouples().size() + " couples(s)");
		}
		catch(Exception e)
		{
			_log.warn("Exception: CoupleManager.load(): " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rs);
		}
	}

	public final Couple getCouple(int coupleId)
	{
		for(Couple c : getCouples())
			if(c.getId() == coupleId)
				return c;
		return null;
	}

	/**
	 * Вызывается при каждом входе персонажа в мир
	 * @param cha
	 */
	public void engage(L2Player player)
	{
		int chaId = player.getObjectId();

		for(Couple cl : getCouples())
			if(cl.getPlayer1Id() == chaId || cl.getPlayer2Id() == chaId)
			{
				if(cl.getMaried())
					player.setMaried(true);

				player.setCoupleId(cl.getId());

				if(cl.getPlayer1Id() == chaId)
					player.setPartnerId(cl.getPlayer2Id());
				else
					player.setPartnerId(cl.getPlayer1Id());
			}
	}

	/**
	 * Уведомляет партнера персонажа о его входе в мир.
	 * @param cha
	 */
	public void notifyPartner(L2Player player)
	{
		if(player.getPartnerId() != 0)
		{
			L2Player partner = L2ObjectsStorage.getPlayer(player.getPartnerId());
			if(partner != null)
				partner.sendMessage(new CustomMessage("ru.l2gw.gameserver.instancemanager.CoupleManager.PartnerEntered", partner));
			else if(Config.DEBUG)
				_log.info(player + " partner not in world.");
		}
	}

	public void createCouple(L2Player player1, L2Player player2)
	{
		if(player1 != null && player2 != null)
			if(player1.getPartnerId() == 0 && player2.getPartnerId() == 0)
				getCouples().add(new Couple(player1, player2));
	}

	public final List<Couple> getCouples()
	{
		if(_couples == null)
			_couples = new FastList<Couple>();
		return _couples;
	}

	public List<Couple> getDeletedCouples()
	{
		if(_deletedCouples == null)
			_deletedCouples = new FastList<Couple>();
		return _deletedCouples;
	}

	/**
	 * Вызывется при шатдауне
	 * Сначала очищаем таблицу от ненужных свадеб, потом загоняем в нее все нужные.
	 * Обращение происходит только при загрузке/шатдауне сервера, ну или по запросу
	 */
	public void store()
	{
		Connection con = null;

		try
		{
			if(_deletedCouples != null && !_deletedCouples.isEmpty())
			{
				con = DatabaseFactory.getInstance().getConnection();
				for(Couple c : _deletedCouples)
				{
					PreparedStatement statement = con.prepareStatement("DELETE FROM couples WHERE id = ?");
					statement.setInt(1, c.getId());
					statement.execute();
					statement.close();
				}
				_deletedCouples.clear();
			}

			if(_couples != null && !_couples.isEmpty())
				for(Couple c : _couples)
					if(c.isChanged())
					{
						if(con == null)
							con = DatabaseFactory.getInstance().getConnection();

						c.store(con);
						c.setChanged(false);
					}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con);
		}
	}

	private class StoreTask implements Runnable
	{
		private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

		public void run()
		{
			store();
			_log.debug("Scheduled couple DB storing finished at: " + formatter.format(System.currentTimeMillis()));
		}
	}
}