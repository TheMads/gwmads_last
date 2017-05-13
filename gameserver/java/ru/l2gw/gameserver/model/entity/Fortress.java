package ru.l2gw.gameserver.model.entity;

import javolution.util.FastList;
import javolution.util.FastMap;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.instancemanager.PlayerMessageStack;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Spawn;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.entity.siege.SiegeSpawn;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.entity.siege.fortress.CombatFlag;
import ru.l2gw.gameserver.model.entity.siege.fortress.FortressSiege;
import ru.l2gw.gameserver.model.entity.siege.fortress.FortressSiegeDatabase;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.PlaySound;
import ru.l2gw.gameserver.serverpackets.PledgeShowInfoUpdate;
import ru.l2gw.gameserver.serverpackets.SkillList;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ClanTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Fortress extends SiegeUnit
{
	protected ScheduledFuture<TaxTask> _castleTaxTask;

	public static enum FortressType
	{
		BORDER,
		TERRITORY
	}

	public Fortress()
	{
		super();
		_commanderSpawns = new FastList<SiegeSpawn>();
		_peaceSpawns = new FastList<SiegeSpawn>();
		_flagList = new FastList<CombatFlag>();
		_flagPoleSpawns = new FastList<SiegeSpawn>();
		_commandCenterDoors = new FastList<Integer>();
		_castles = new FastList<Integer>();
		_supplyBoxes = new FastMap<Integer, L2Spawn>();
		_doorControllers = new FastList<Integer>();
		_mainControllers = new FastList<Integer>();
		_controlDoors = new FastList<Integer>();
	}

	@Override
	public Siege getSiege()
	{
		if(_siege == null)
			_siege = new FortressSiege(this);
		return _siege;
	}

	@Override
	public void changeOwner(int clanId)
	{
		getZone().setActive(false);
		if(getOwner() != null)
		{
			getOwner().setHasFortress(0);
			PledgeShowInfoUpdate pi = new PledgeShowInfoUpdate(getOwner());
			for(L2Player player : getOwner().getOnlineMembers(""))
				if(player != null)
				{
					removeSkills(player);
					player.sendPacket(new SkillList(player));
					player.sendPacket(pi);
					if(player.unEquipInappropriateItems())
						player.broadcastUserInfo();
				}
		}

		if(clanId != 0)
		{
			L2Clan clan = ClanTable.getInstance().getClan(clanId);
			if(clan == null)
				_log.warn("cannot set owner for fortress" + getId());
			else
			{
				if(clan.getHasUnit(2))
					clanId = 0;	
				//	ResidenceManager.getInstance().getBuildingById(clan.getHasCastle()).changeOwner(0);

				if(clan.getHasUnit(3) && clan.getHasFortress() != getId())
					ResidenceManager.getInstance().getBuildingById(clan.getHasFortress()).changeOwner(0);

				clan.setHasFortress(getId()); // Set has fortress flag for new owner
				PledgeShowInfoUpdate pi = new PledgeShowInfoUpdate(clan);
				for(L2Player player : clan.getOnlineMembers(""))
					if(player != null)
					{
						giveSkills(player);
						player.sendPacket(new SkillList(player));
						player.sendPacket(pi);
					}
				getZone().setActive(true);
			}
		}

		updateOwnerInDB(clanId); // Update in database

		if(getSiege().isInProgress()) // If siege in progress
			getSiege().endSiege();

		if(clanId == 0)
		{
			stopFunctions();
			setContractCastle(0);
			setSupplyLevel(0);
			setRewardLevel(0);
			stopHoldTask();
		}
	}


	private void updateOwnerInDB(int clanId)
	{
		setOwnerId(clanId); // Update owner id property

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE clan_data SET hasFortress=0 WHERE hasFortress=? LIMIT 1");
			statement.setInt(1, getId());
			statement.execute();
			DbUtils.closeQuietly(statement);
			statement = null;

			statement = con.prepareStatement("DELETE FROM clanhall_functions WHERE hall_id=?");
			statement.setInt(1, getId());
			statement.execute();

			if(clanId != 0)
			{
				statement = con.prepareStatement("UPDATE clan_data SET hasFortress=? WHERE clan_id=? LIMIT 1");
				statement.setInt(1, getId());
				statement.setInt(2, getOwnerId());
				statement.execute();

				L2Clan clan = ClanTable.getInstance().getClan(clanId);
				if(clan == null)
					_log.warn("cannot set owner for fortress" + getId());
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

	public void startAutoTask()
	{
		if(_castleTaxTask != null)
			_castleTaxTask.cancel(false);

		_castleTaxTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new TaxTask(), _lastTax + 21600000 - System.currentTimeMillis(), 21600000);
		((FortressSiegeDatabase) _siege.getDatabase()).saveLastTaxTime();
	}

	private class TaxTask implements Runnable
	{
		public void run()
		{
			if(getOwner() != null)
			{
				_log.info(getName() + " tax task run, owner: " + getOwner() + " contract: " + _contractCastle);
				if(_contractCastle > 0)
				{
					L2ItemInstance adena = getOwner().getWarehouse().getItemByItemId(57);
					if(adena != null && adena.getCount() >= _taxAmount && ResidenceManager.getInstance().getBuildingById(_contractCastle).getOwner() != null)
					{
						getOwner().getWarehouse().destroyItemByItemId("FortressTax", 57, _taxAmount, null, null);
						ResidenceManager.getInstance().getCastleById(_contractCastle).addToTreasuryNoTax(_taxAmount, false, false, "FORTRESS_TAX");
						if(_supplyLevel < 6)
						{
							if(ResidenceManager.getInstance().getBuildingById(_contractCastle).getOwner().getReputationScore() >= 2)
							{
								ResidenceManager.getInstance().getBuildingById(_contractCastle).getOwner().incReputation(-2, false, "FortSupply");
								setSupplyLevel(_supplyLevel + 1);
								_log.info(getName() + " tax task run, owner: " + getOwner() + " set supply level: " + (_supplyLevel + 1));
							}
							else
							{
								PlayerMessageStack.getInstance().mailto(getOwner().getLeaderId(), new SystemMessage(SystemMessage.THE_SUPPLY_ITEMS_HAVE_NOT_NOT_BEEN_PROVIDED_BECAUSE_THE_HIGHER_CASTLE_IN_CONTRACT_DOESNT_HAVE_ENOUGH_CLAN_REPUTATION_SCORE));
								_log.info(getName() + " tax task run, owner: " + getOwner() + " no reputation: " + ResidenceManager.getInstance().getBuildingById(_contractCastle).getOwner() + " for supply." );
							}
						}
						else
							_log.info(getName() + " tax task run, owner: " + getOwner() + " max supply level: " + _supplyLevel);
					}
					else
					{
						_log.info(getName() + " tax task run, owner: " + getOwner() + " no adena for tax, cancel contract with: " + _contractCastle);
						setContractCastle(0);
					}
				}

				_lastTax = System.currentTimeMillis();
				((FortressSiegeDatabase) _siege.getDatabase()).saveLastTaxTime();
				_log.info(getName() + " tax task run, owner: " + getOwner() + " set reward level: " + (_rewardLevel + 1));
				setRewardLevel(_rewardLevel + 1);
			}
		}
	}

	public int getMinLeftForTax()
	{
		if(_castleTaxTask != null)
			return (int)_castleTaxTask.getDelay(TimeUnit.MINUTES);
		return 0;
	}

	public L2Spawn getSupplySpawn()
	{
		if(!_supplyBoxes.containsKey(_supplyLevel))
			return null;
		return _supplyBoxes.get(_supplyLevel);
	}

	@Override
	public String toString()
	{
		return "Fortress[id=" + getId() + ";name=" + getName() + "]";
	}


	public Map<Integer, L2Zone> getTrapZones()
	{
		return _trapZones;
	}

}