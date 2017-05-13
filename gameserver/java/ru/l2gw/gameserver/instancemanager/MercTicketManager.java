package ru.l2gw.gameserver.instancemanager;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2World;
import ru.l2gw.gameserver.model.entity.Castle;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.util.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class MercTicketManager
{
	protected static Log _log = LogFactory.getLog(MercTicketManager.class.getName());

	private static MercTicketManager _instance;

	public static MercTicketManager getInstance()
	{
		if(_instance == null)
		{
			_log.info("MercTicketManager: Initializing");
			_instance = new MercTicketManager();
			_instance.load();
		}
		return _instance;
	}

	private List<L2ItemInstance> _droppedTickets; // to keep track of items on the ground

	private final int[] _maxMercPerType = { 10, // Gludio
			15, // Dion
			10, // Giran
			10, // Oren
			20, // Aden
			20, // Innadril
			20, // Goddard
			20, // Rune
			20, // Schuttgart
	};
	private final int[] _maxMercPerCastle = { 50, // Gludio
			75, // Dion
			100, // Giran
			150, // Oren
			200, // Aden
			200, // Innadril
			200, // Goddard
			200, // Rune
			200 // Schuttgart
	};

	private final short[][] _itemIds = {
	// Gludio
			{ 3960, 3961, 3962, 3963, 3964, 3965, 3966, 3967, 3968, 3969, // Mercenary
					3970,
					3971,
					3972,
					0,
					0,
					0,
					0,
					0,
					0,
					0, // Teleporter
					6038,
					6039,
					6040,
					6041,
					6042,
					6043,
					6044,
					6045,
					6046,
					6047, // Greater Mercenary
					6115,
					6116,
					6117,
					6118,
					6119,
					6120,
					6121,
					6122,
					6123,
					6124, // Dawn Mercenary
					6175,
					6176,
					6177,
					6178,
					6179,
					6180,
					6181,
					6182,
					6183,
					6184, // Greater Recruit
					6235,
					6236,
					6237,
					6238,
					6239,
					6240,
					6241,
					6242,
					6243,
					6244, // Recruit
					6295,
					6296 // Nephilim
			},

			// Dion
			{ 3973, 3974, 3975, 3976, 3977, 3978, 3979, 3980, 3981, 3982, // Mercenary
					3983,
					3984,
					3985,
					0,
					0,
					0,
					0,
					0,
					0,
					0, // Teleporter
					6051,
					6052,
					6053,
					6054,
					6055,
					6056,
					6057,
					6058,
					6059,
					6060, // Greater Mercenary
					6125,
					6126,
					6127,
					6128,
					6129,
					6130,
					6131,
					6132,
					6133,
					6134, // Dawn Mercenary
					6185,
					6186,
					6187,
					6188,
					6189,
					6190,
					6191,
					6192,
					6193,
					6194, // Greater Recruit
					6245,
					6246,
					6247,
					6248,
					6249,
					6250,
					6251,
					6252,
					6253,
					6254, // Recruit
					6297,
					6298 // Nephilim
			},

			// Giran
			{ 3986, 3987, 3988, 3989, 3990, 3991, 3992, 3993, 3994, 3995, // Mercenary
					3996,
					3997,
					3998,
					0,
					0,
					0,
					0,
					0,
					0,
					0, // Teleporter
					6064,
					6065,
					6066,
					6067,
					6068,
					6069,
					6070,
					6071,
					6072,
					6073, // Greater Mercenary
					6135,
					6136,
					6137,
					6138,
					6139,
					6140,
					6141,
					6142,
					6143,
					6144, // Dawn Mercenary
					6195,
					6196,
					6197,
					6198,
					6199,
					6200,
					6201,
					6202,
					6203,
					6204, // Greater Recruit
					6255,
					6256,
					6257,
					6258,
					6259,
					6260,
					6261,
					6262,
					6263,
					6264, // Recruit
					6299,
					6300 // Nephilim
			},

			// Oren
			{ 3999, 4000, 4001, 4002, 4003, 4004, 4005, 4006, 4007, 4008, // Mercenary
					4009,
					4010,
					4011,
					0,
					0,
					0,
					0,
					0,
					0,
					0, // Teleporter
					6077,
					6078,
					6079,
					6080,
					6081,
					6082,
					6083,
					6084,
					6085,
					6086, // Greater Mercenary
					6145,
					6146,
					6147,
					6148,
					6149,
					6150,
					6151,
					6152,
					6153,
					6154, // Dawn Mercenary
					6205,
					6206,
					6207,
					6208,
					6209,
					6210,
					6211,
					6212,
					6213,
					6214, // Greater Recruit
					6265,
					6266,
					6267,
					6268,
					6269,
					6270,
					6271,
					6272,
					6273,
					6274, // Recruit
					6301,
					6302 //Nephilim
			},

			// Aden
			{ 4012, 4013, 4014, 4015, 4016, 4017, 4018, 4019, 4020, 4021, // Mercenary
					4022,
					4023,
					4024,
					4025,
					4026,
					0,
					0,
					0,
					0,
					0, // Teleporter
					6090,
					6091,
					6092,
					6093,
					6094,
					6095,
					6096,
					6097,
					6098,
					6099, // Greater Mercenary
					6155,
					6156,
					6157,
					6158,
					6159,
					6160,
					6161,
					6162,
					6163,
					6164, // Dawn Mercenary
					6215,
					6216,
					6217,
					6218,
					6219,
					6220,
					6221,
					6222,
					6223,
					6224, // Greater Recruit
					6275,
					6276,
					6277,
					6278,
					6279,
					6280,
					6281,
					6282,
					6283,
					6284, // Recruit
					6303,
					6304 // Nephilim
			},

			// Innadril
			{ 5205, 5206, 5207, 5208, 5209, 5210, 5211, 5212, 5213, 5214, // Mercenary
					5215,
					0,
					0,
					0,
					0,
					0,
					0,
					0,
					0,
					0, // Teleporter
					6105,
					6106,
					6107,
					6108,
					6109,
					6110,
					6111,
					6112,
					6113,
					6114,// Greater Mercenary
					6165,
					6166,
					6167,
					6168,
					6169,
					6170,
					6171,
					6172,
					6173,
					6174, // Dawn Mercenary
					6225,
					6226,
					6227,
					6228,
					6229,
					6230,
					6231,
					6232,
					6233,
					6234, // Greater Recruit
					6285,
					6286,
					6287,
					6288,
					6289,
					6290,
					6291,
					6292,
					6293,
					6294, // Recruit
					6305,
					6306 // Nephilim
			},

			// Goddard
			{ 6779, 6780, 6781, 6782, 6783, 6784, 6785, 6786, 6787, 6788, // Mercenary
					6789,
					6790,
					6791,
					0,
					0,
					0,
					0,
					0,
					0,
					0, // Teleporter
					6792,
					6793,
					6794,
					6795,
					6796,
					6797,
					6798,
					6799,
					6800,
					6801, // Greater Mercenary
					6802,
					6803,
					6804,
					6805,
					6806,
					6807,
					6808,
					6809,
					6810,
					6811, // Dawn Mercenary
					6812,
					6813,
					6814,
					6815,
					6816,
					6817,
					6818,
					6819,
					6820,
					6821, // Greater Recruit
					6822,
					6823,
					6824,
					6825,
					6826,
					6827,
					6828,
					6829,
					6830,
					6831, // Recruit
					6832,
					6833 // Nephilim
			},

			// Rune
			{ 7973, 7974, 7975, 7976, 7977, 7978, 7979, 7980, 7981, 7982, // Mercenary
					7983,
					7984,
					7985,
					7986,
					7987,
					0,
					0,
					0,
					0,
					0, // Teleporter
					7988,
					7989,
					7990,
					7991,
					7992,
					7993,
					7994,
					7995,
					7996,
					7997, // Greater Mercenary
					7998,
					7999,
					8000,
					8001,
					8002,
					8003,
					8004,
					8005,
					8006,
					8007, // Dawn Mercenary
					8008,
					8009,
					8010,
					8011,
					8012,
					8013,
					8014,
					8015,
					8016,
					8017, // Greater Recruit
					8018,
					8019,
					8020,
					8021,
					8022,
					8023,
					8024,
					8025,
					8026,
					8027, // Recruit
					8028,
					8029 // Nephilim
			},

			// Schuttgart
			{ 7918, 7919, 7920, 7921, 7922, 7923, 7924, 7925, 7926, 7927, // Mercenary
					7928,
					7929,
					7930,
					0,
					0,
					0,
					0,
					0,
					0,
					0, // Teleporter
					7931,
					7932,
					7933,
					7934,
					7935,
					7936,
					7937,
					7938,
					7939,
					7940, // Greater Mercenary
					7941,
					7942,
					7943,
					7944,
					7945,
					7946,
					7947,
					7948,
					7949,
					7950, // Dawn Mercenary
					7951,
					7952,
					7953,
					7954,
					7955,
					7956,
					7957,
					7958,
					7959,
					7960, // Greater Recruit
					7961,
					7962,
					7963,
					7964,
					7965,
					7966,
					7967,
					7968,
					7969,
					7970, // Recruit
					7971,
					7972 // Nephilim
			} };

	private final int[] _NpcIds = { 35010, 35011, 35012, 35013, 35014, 35015, 35016, 35017, 35018, 35019, // Mercenary
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0,
			0, // TODO: Teleporter
			35030,
			35031,
			35032,
			35033,
			35034,
			35035,
			35036,
			35037,
			35038,
			35039, // Elite Mercenary
			35020,
			35021,
			35022,
			35023,
			35024,
			35025,
			35026,
			35027,
			35028,
			35029, // Mercenary of Dawn
			35040,
			35041,
			35042,
			35043,
			35044,
			35045,
			35046,
			35047,
			35048,
			35049, // Greater Recruit
			35050,
			35051,
			35052,
			35053,
			35054,
			35055,
			35056,
			35057,
			35058,
			35059, // Recruit
			35060,
			35060, // Nephilim
	};

	private void load()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;

		// load merc tickets into the world
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM siege_guards Where isHired = 1");
			rset = statement.executeQuery();

			int npcId;
			short itemId;
			int x, y, z;

			while(rset.next())
			{
				npcId = rset.getInt("npcId");
				x = rset.getInt("x");
				y = rset.getInt("y");
				z = rset.getInt("z");
				SiegeUnit siegeUnit = SiegeManager.getSiegeUnitByCoord(x, y);

				// find the FIRST ticket itemId with spawns the saved NPC in the saved location
				for(int i = 0; i < _NpcIds.length; i++)
					if(_NpcIds[i] == npcId && _NpcIds[i] != 0) // Find the index of the item used
					{
						if(siegeUnit != null && !siegeUnit.getSiege().isInProgress())
						{
							// TODO переделать для фортов
							itemId = _itemIds[siegeUnit.getId() - 1][i];
							if(itemId == 0)
								break;

							L2ItemInstance dropticket = new L2ItemInstance(IdFactory.getInstance().getNextId(), itemId);
							dropticket.setLocation(L2ItemInstance.ItemLocation.INVENTORY);
							dropticket.dropMe(null, new Location(x, y, z));
							dropticket.setDropTime(0); // avoids it from beeing removed by the auto item destroyer
							getDroppedTickets().add(dropticket);
						}
						break;
					}
			}
			_log.info("MercTicketManager: Loaded " + getDroppedTickets().size() + " Mercenary Tickets");
		}
		catch(Exception e)
		{
			System.out.println("Exception: loadMercenaryData(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	/**
	 * Checks if the passed item has reached the limit of number of dropped
	 * tickets that this SPECIFIC item may have in its castle
	 */
	public boolean isAtTypeLimit(int itemId)
	{
		int castleId = getTicketCastleId(itemId);
		if(castleId <= 0)
			return true;

		int limit = _maxMercPerType[castleId - 1];
		if(limit <= 0)
			return true;

		int count = 0;
		for(L2ItemInstance ticket : getDroppedTickets())
			if(ticket != null && ticket.getItemId() == itemId)
				count++;
		if(count >= limit)
			return true;
		return false;
	}

	/**
	 * Checks if the passed item belongs to a castle which has reached its limit
	 * of number of dropped tickets.
	 */
	public boolean isAtCasleLimit(int itemId)
	{
		int castleId = getTicketCastleId(itemId);
		if(castleId <= 0)
			return true;
		int limit = _maxMercPerCastle[castleId - 1];
		if(limit <= 0)
			return true;

		int count = 0;
		for(L2ItemInstance ticket : getDroppedTickets())
			if(ticket != null && getTicketCastleId(ticket.getItemId()) == castleId)
				count++;
		if(count >= limit)
			return true;
		return false;
	}

	public void addTicket(int itemId, L2Player player)
	{
		Location loc = new Location(player.getX(), player.getY(), player.getZ(), player.getHeading());
		Castle castle = ResidenceManager.getInstance().getCastleByObjectInSiegeZone(player);
		if(castle == null)
			return;

		int npcId = getNpcId(itemId);
		if(npcId <= 0)
			return;

		L2ItemInstance dropticket = new L2ItemInstance(IdFactory.getInstance().getNextId(), itemId);
		dropticket.setLocation(L2ItemInstance.ItemLocation.INVENTORY);

		dropticket.dropMe(null, loc);
		castle.getSiege().getSiegeGuardManager().hireMerc(dropticket.getLoc(), npcId);

		dropticket.setDropTime(0); // avoids it from beeing removed by the auto item destroyer
		getDroppedTickets().add(dropticket);
	}

	/**
	 * Delete all tickets from a castle;
	 * remove the items from the world and remove references to them from this class
	 */
	public void deleteTickets(int castleId)
	{
		int i = 0;
		while(i < getDroppedTickets().size())
		{
			L2ItemInstance item = getDroppedTickets().get(i);
			if(item != null && getTicketCastleId(item.getItemId()) == castleId)
			{
				item.decayMe();
				L2World.removeObject(item);

				// remove from the list
				getDroppedTickets().remove(i);
			}
			else
				i++;
		}
	}

	public void removeTicket(L2ItemInstance item)
	{
		int itemId = item.getItemId();
		int npcId = getNpcId(itemId);

		// find the castle where this item is
		SiegeUnit castle = ResidenceManager.getInstance().getBuildingById(getTicketCastleId(itemId));

		if(npcId > 0 && castle != null)
			castle.getSiege().getSiegeGuardManager().removeMerc(npcId, item.getLoc());

		getDroppedTickets().remove(item);
	}

	public short[][] getItemIds()
	{
		return _itemIds;
	}

	public final List<L2ItemInstance> getDroppedTickets()
	{
		if(_droppedTickets == null)
			_droppedTickets = new FastList<L2ItemInstance>();
		return _droppedTickets;
	}

	public int getNpcId(int itemId)
	{
		for(short[] element : _itemIds)
			for(int j = 0; j < element.length; j++)
				if(element[j] == itemId && _NpcIds[j] != 0)
					return _NpcIds[j];
		return -1;
	}

	/** returns the castleId for the passed ticket item id */
	public int getTicketCastleId(int itemId)
	{
		for(int i = 0; i < _itemIds.length; i++)
			for(int j = 0; j < _itemIds[i].length; j++)
				if(itemId == _itemIds[i][j])
					return i + 1;
		return -1;
	}
}