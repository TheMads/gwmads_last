package ru.l2gw.gameserver.model.entity;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.instancemanager.CastleManorManager;
import ru.l2gw.gameserver.instancemanager.CastleManorManager.CropProcure;
import ru.l2gw.gameserver.instancemanager.CastleManorManager.SeedProduction;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.instancemanager.SiegeGuardManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Manor;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Spawn;
import ru.l2gw.gameserver.model.entity.siege.SiegeSpawn;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.entity.siege.castle.CastleSiege;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.PlaySound;
import ru.l2gw.gameserver.serverpackets.PledgeShowInfoUpdate;
import ru.l2gw.gameserver.serverpackets.SkillList;
import ru.l2gw.gameserver.tables.ClanTable;
import ru.l2gw.gameserver.templates.L2Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;

public class Castle extends SiegeUnit
{
	protected static Log logTreasure = LogFactory.getLog("treasure");

	private CastleSiege _Siege;

	private static final String CASTLE_MANOR_DELETE_PRODUCTION = "DELETE FROM castle_manor_production WHERE castle_id=?;";
	private static final String CASTLE_MANOR_DELETE_PRODUCTION_PERIOD = "DELETE FROM castle_manor_production WHERE castle_id=? AND period=?;";
	private static final String CASTLE_MANOR_DELETE_PROCURE = "DELETE FROM castle_manor_procure WHERE castle_id=?;";
	private static final String CASTLE_MANOR_DELETE_PROCURE_PERIOD = "DELETE FROM castle_manor_procure WHERE castle_id=? AND period=?;";
	private static final String CASTLE_UPDATE_CROP = "UPDATE castle_manor_procure SET can_buy=? WHERE crop_id=? AND castle_id=? AND period=?";
	private static final String CASTLE_UPDATE_SEED = "UPDATE castle_manor_production SET can_produce=? WHERE seed_id=? AND castle_id=? AND period=?";

	protected GArray<CropProcure> _procure;
	protected GArray<SeedProduction> _production;
	protected GArray<CropProcure> _procureNext;
	protected GArray<SeedProduction> _productionNext;
	protected boolean _isNextPeriodApproved;
	protected int _taxPercent;
	protected double _taxRate;

	public Castle()
	{
		super();
		_artefactSpawnList = new FastList<SiegeSpawn>();
		_controlTowerSpawnList = new FastList<SiegeSpawn>();
		_trapZones = new FastMap<Integer, L2Zone>();
		_ambassadors = new FastMap<Integer, L2Spawn>();
		_taxPercent = 0;
		_taxRate = 0;
		_treasury = 0;
		_townId = 0;
		_Siege = null;
		_procure = new GArray<CropProcure>();
		_production = new GArray<SeedProduction>();
		_procureNext = new GArray<CropProcure>();
		_productionNext = new GArray<SeedProduction>();
		_isNextPeriodApproved = false;
	}

	@Override
	public CastleSiege getSiege()
	{
		if(_Siege == null)
			_Siege = new CastleSiege(this);
		return _Siege;
	}

	// This method sets the castle owner; null here means give it back to NPC
	@Override
	public void changeOwner(int clanId)
	{
		getZone().setActive(false);
		if(getOwnerId() > 0 && (clanId == 0 || clanId != getOwnerId()))
		{
			setTaxPercent(null, 0);
			L2Clan oldOwner = ClanTable.getInstance().getClan(getOwnerId()); // Try to find clan instance
			if(oldOwner != null)
			{
				oldOwner.setHasCastle(0); // Unset has castle flag for old owner
				PledgeShowInfoUpdate pi = new PledgeShowInfoUpdate(oldOwner);
				for(L2Player player : oldOwner.getOnlineMembers(""))
				{
					if(player != null)
					{
						removeSkills(player);
						TerritoryWarManager.getTerritoryById(getId() + 80).removeSkills(player);
						player.sendPacket(new SkillList(player));
						player.sendPacket(pi);
						if(player.unEquipInappropriateItems())
							player.broadcastUserInfo();
					}
				}
			}
		}

		if(clanId != 0)
		{
			L2Clan clan = ClanTable.getInstance().getClan(clanId);
			if(clan == null)
				_log.warn("cannot set owner for castle" + getId());
			else
			{
				if(clan.getHasUnit(3))
					ResidenceManager.getInstance().getBuildingById(clan.getHasFortress()).changeOwner(0);

				clan.setHasCastle(getId()); // Set has castle flag for new owner
				for(L2Player player : clan.getOnlineMembers(""))
				{
					if(player != null)
					{
						giveSkills(player);
						player.sendPacket(new SkillList(player));
					}
				}
				getZone().setActive(true);
			}
		}

		updateOwnerInDB(clanId); // Update in database

		SiegeGuardManager.removeMercsFromDb(getId());
		removeUpgrade();

		if(clanId != 0 && getSiege().isInProgress()) // If siege in progress
			getSiege().midVictory(); // Mid victory phase of siege
	}

	private void updateOwnerInDB(int clanId)
	{
		setOwnerId(clanId); // Update owner id property

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET hasCastle=0 WHERE hasCastle=? LIMIT 1");
			statement.setInt(1, getId());
			statement.execute();
			DbUtils.closeQuietly(statement);
			statement = null;

			if(clanId != 0)
			{
				statement = con.prepareStatement("UPDATE clan_data SET hasCastle=? WHERE clan_id=? LIMIT 1");
				statement.setInt(1, getId());
				statement.setInt(2, getOwnerId());
				statement.execute();

				L2Clan clan = ClanTable.getInstance().getClan(clanId);
				if(clan == null)
					_log.warn("cannot set owner for castle" + getId());
				else
				{
					clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
					clan.broadcastToOnlineMembers(new PlaySound("Siege_Victory"));
				}
			}
		}
		catch(Exception e)
		{
			_log.warn("Exception: updateOwnerInDB(L2Clan clan): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	/**
	 * Возвращает ID города, за которым закреплен замок
	 *
	 * @return идентификатор города
	 */
	public int getTown()
	{
		return _townId;
	}

	@Override
	public String toString()
	{
		return "Castle[id=" + getId() + ";name=" + getName() + "]";
	}

	@Override
	public void startAutoTask()
	{}

	@Override
	public int getMinLeftForTax()
	{
		return 0;
	}

	public Map<Integer, L2Zone> getTrapZones()
	{
		return _trapZones;
	}

	public int getCropRewardType(int crop)
	{
		int rw = 0;
		for(CropProcure cp : _procure)
			if(cp.getId() == crop)
				rw = cp.getReward();
		return rw;
	}

	public GArray<SeedProduction> getSeedProduction(int period)
	{
		return period == CastleManorManager.PERIOD_CURRENT ? _production : _productionNext;
	}

	public GArray<CropProcure> getCropProcure(int period)
	{
		return period == CastleManorManager.PERIOD_CURRENT ? _procure : _procureNext;
	}

	public void setSeedProduction(GArray<SeedProduction> seed, int period)
	{
		if(period == CastleManorManager.PERIOD_CURRENT)
			_production = seed;
		else
			_productionNext = seed;
	}

	public void setCropProcure(GArray<CropProcure> crop, int period)
	{
		if(period == CastleManorManager.PERIOD_CURRENT)
			_procure = crop;
		else
			_procureNext = crop;
	}

	public synchronized SeedProduction getSeed(int seedId, int period)
	{
		for(SeedProduction seed : getSeedProduction(period))
			if(seed.getId() == seedId)
				return seed;
		return null;
	}

	public synchronized CropProcure getCrop(int cropId, int period)
	{
		for(CropProcure crop : getCropProcure(period))
			if(crop.getId() == cropId)
				return crop;
		return null;
	}

	public int getManorCost(int period)
	{
		GArray<CropProcure> procure;
		GArray<SeedProduction> production;

		if(period == CastleManorManager.PERIOD_CURRENT)
		{
			procure = _procure;
			production = _production;
		}
		else
		{
			procure = _procureNext;
			production = _productionNext;
		}

		int total = 0;
		if(production != null)
			for(SeedProduction seed : production)
				total += L2Manor.getInstance().getSeedBuyPrice(seed.getId()) * seed.getStartProduce();
		if(procure != null)
			for(CropProcure crop : procure)
				total += crop.getPrice() * crop.getStartAmount();
		return total;
	}

	// Save manor production data
	public void saveSeedData()
	{
		Connection con = null;
		PreparedStatement statement = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement(CASTLE_MANOR_DELETE_PRODUCTION);
			statement.setInt(1, getId());

			statement.execute();
			statement.close();

			if(_production != null)
			{
				int count = 0;
				String query = "INSERT INTO castle_manor_production VALUES ";
				String values[] = new String[_production.size()];
				for(SeedProduction s : _production)
				{
					values[count] = "(" + getId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + CastleManorManager.PERIOD_CURRENT + ")";
					count++;
				}
				if(values.length > 0)
				{
					query += values[0];
					for(int i = 1; i < values.length; i++)
						query += "," + values[i];
					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
				}
			}

			if(_productionNext != null)
			{
				int count = 0;
				String query = "INSERT INTO castle_manor_production VALUES ";
				String values[] = new String[_productionNext.size()];
				for(SeedProduction s : _productionNext)
				{
					values[count] = "(" + getId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + CastleManorManager.PERIOD_NEXT + ")";
					count++;
				}
				if(values.length > 0)
				{
					query += values[0];
					for(int i = 1; i < values.length; i++)
						query += "," + values[i];
					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
				}
			}

			DbUtils.closeQuietly(con, statement);
		}
		catch(Exception e)
		{
			_log.info("Error adding seed production data for castle " + getName() + ": " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	// Save manor production data for specified period
	public void saveSeedData(int period)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement(CASTLE_MANOR_DELETE_PRODUCTION_PERIOD);
			statement.setInt(1, getId());
			statement.setInt(2, period);
			statement.execute();
			statement.close();

			GArray<SeedProduction> prod = getSeedProduction(period);

			if(prod != null)
			{
				int count = 0;
				String query = "INSERT INTO castle_manor_production VALUES ";
				String values[] = new String[prod.size()];
				for(SeedProduction s : prod)
				{
					values[count] = "(" + getId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + period + ")";
					count++;
				}
				if(values.length > 0)
				{
					query += values[0];
					for(int i = 1; i < values.length; i++)
						query += "," + values[i];
					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
				}
			}

			DbUtils.closeQuietly(con, statement);
		}
		catch(Exception e)
		{
			_log.info("Error adding seed production data for castle " + getName() + ": " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	// Save crop procure data
	public void saveCropData()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement(CASTLE_MANOR_DELETE_PROCURE);
			statement.setInt(1, getId());
			statement.execute();
			statement.close();
			if(_procure != null)
			{
				int count = 0;
				String query = "INSERT INTO castle_manor_procure VALUES ";
				String values[] = new String[_procure.size()];
				for(CropProcure cp : _procure)
				{
					values[count] = "(" + getId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + CastleManorManager.PERIOD_CURRENT + ")";
					count++;
				}
				if(values.length > 0)
				{
					query += values[0];
					for(int i = 1; i < values.length; i++)
						query += "," + values[i];
					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
				}
			}
			if(_procureNext != null)
			{
				int count = 0;
				String query = "INSERT INTO castle_manor_procure VALUES ";
				String values[] = new String[_procureNext.size()];
				for(CropProcure cp : _procureNext)
				{
					values[count] = "(" + getId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + CastleManorManager.PERIOD_NEXT + ")";
					count++;
				}
				if(values.length > 0)
				{
					query += values[0];
					for(int i = 1; i < values.length; i++)
						query += "," + values[i];
					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
				}
			}

			DbUtils.closeQuietly(con, statement);
		}
		catch(Exception e)
		{
			_log.info("Error adding crop data for castle " + getName() + ": " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	// Save crop procure data for specified period
	public void saveCropData(int period)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement(CASTLE_MANOR_DELETE_PROCURE_PERIOD);
			statement.setInt(1, getId());
			statement.setInt(2, period);
			statement.execute();
			statement.close();

			GArray<CropProcure> proc = getCropProcure(period);

			if(proc != null)
			{
				int count = 0;
				String query = "INSERT INTO castle_manor_procure VALUES ";
				String values[] = new String[proc.size()];

				for(CropProcure cp : proc)
				{
					values[count] = "(" + getId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + period + ")";
					count++;
				}
				if(values.length > 0)
				{
					query += values[0];
					for(int i = 1; i < values.length; i++)
						query += "," + values[i];
					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
				}
			}

			DbUtils.closeQuietly(con, statement);
		}
		catch(Exception e)
		{
			_log.info("Error adding crop data for castle " + getName() + ": " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void updateCrop(int cropId, long amount, int period)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement(CASTLE_UPDATE_CROP);
			statement.setLong(1, amount);
			statement.setInt(2, cropId);
			statement.setInt(3, getId());
			statement.setInt(4, period);
			statement.execute();
			statement.close();
		}
		catch(Exception e)
		{
			_log.info("Error adding crop data for castle " + getName() + ": " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void updateSeed(int seedId, long amount, long period)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement(CASTLE_UPDATE_SEED);
			statement.setLong(1, amount);
			statement.setInt(2, seedId);
			statement.setInt(3, getId());
			statement.setLong(4, period);
			statement.execute();
			statement.close();
		}
		catch(Exception e)
		{
			_log.info("Error adding seed production data for castle " + getName() + ": " + e.getMessage());
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public boolean isNextPeriodApproved()
	{
		return _isNextPeriodApproved;
	}

	public void setNextPeriodApproved(boolean val)
	{
		_isNextPeriodApproved = val;
	}

	public double getTaxRate()
	{
		// Если печатью SEAL_STRIFE владеют DUSK то налог можно выставлять не более 5%
		if(_taxRate > 0.05 && SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
			_taxRate = 0.05;
		return _taxRate;
	}
	
	public long getTreasury()
	{
		return _treasury;
	}

	public void loadTreasury(long treasury)
	{
		_treasury = treasury;
	}

	/**
	 * Only Castle
	 */
	public void loadTaxPercent(int taxPercent)
	{
		_taxPercent = taxPercent;
		_taxRate = _taxPercent / 100.0;
	}

	public int getTaxPercent()
	{
		// Если печатью SEAL_STRIFE владеют DUSK то налог можно выставлять не более 5%
		if(_taxPercent > 5 && SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
			_taxPercent = 5;
		return _taxPercent;
	}

	// This method add to the treasury
	/** Add amount to castle instance's treasury (warehouse). */
	public void addToTreasury(long amount, boolean shop, boolean seed, String process)
	{
		if(getOwnerId() <= 0)
			return;

		if(amount == 0)
			return;

		if(amount > 1 && (getId() == 7 || getId() == 9)) // If current castle instance is not Rune
		{
			Castle rune = ResidenceManager.getInstance().getCastleById(8);
			if(rune != null)
			{
				int runeTax = (int) (amount * rune.getTaxRate()); // Find out what Rune gets from the current castle instance's income
				if(rune.getOwnerId() > 0)
				{
					rune.addToTreasury(runeTax, shop, seed, process); // Only bother to really add the tax to the treasury if not npc owned
				}

				amount -= runeTax; // Subtract Rune's income from current castle instance's income
			}
		}
		else if(amount > 1 && getId() != 5 && getId() != 8 && getId() != 7 && getId() != 9)
		{
			// If current castle instance is not Aden

			Castle aden = ResidenceManager.getInstance().getCastleById(5);
			if(aden != null)
			{
				int adenTax = (int) (amount * aden.getTaxRate()); // Find out what Aden gets from the current castle instance's income
				if(aden.getOwnerId() > 0)
				{
					aden.addToTreasury(adenTax, shop, seed, process); // Only bother to really add the tax to the treasury if not npc owned
				}

				amount -= adenTax; // Subtract Aden's income from current castle instance's income
			}
		}

		addToTreasuryNoTax(amount, shop, seed, process);
	}

	// This method updates the castle tax rate
	public void setTaxPercent(L2Player player, int taxPercent)
	{
		_taxPercent = taxPercent;
		_taxRate = _taxPercent / 100.0;

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE residence SET taxPercent = ? WHERE id = ? LIMIT 1");
			statement.setInt(1, taxPercent);
			statement.setInt(2, getId());
			statement.execute();
		}
		catch(Exception e)
		{
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}

		if(player != null)
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.entity.Castle.OutOfControl.CastleTaxChangetTo", player).addString(getName()).addNumber(taxPercent));
	}

	/**
	 * Add amount to castle instance's treasury (warehouse), no tax paying.
	 */
	public void addToTreasuryNoTax(long amount, boolean shop, boolean seed, String process)
	{
		if(getOwnerId() <= 0)
			return;

		if(amount == 0)
			return;

		// Add to the current treasury total.  Use "-" to substract from treasury
		// TODO Add Limit 99,999,999,999
		if(_treasury + amount > L2Item.MAX_COUNT)
			_treasury = L2Item.MAX_COUNT;
		else
			_treasury += amount;

		if(shop)
			_collectedShops += amount;

		if(seed)
			_collectedSeed += amount;

		logTreasure.info(getName() + "(" + getId() + ") " + process + ": " + amount);
		Connection con = null;
		PreparedStatement statement = null;

		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("Update residence set treasury = ? where id = ?");
			statement.setLong(1, getTreasury());
			statement.setInt(2, getId());
			statement.execute();
			statement.close();

			DbUtils.closeQuietly(con, statement);
		}
		catch(Exception e)
		{
			_log.warn("Exception: addToTreasuryNoTax(): " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
}