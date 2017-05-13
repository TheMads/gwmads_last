package ru.l2gw.gameserver.model.entity.siege.ClanHall;

import javolution.util.FastList;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.ClanHall;
import ru.l2gw.gameserver.model.entity.SevenSigns;
import ru.l2gw.gameserver.model.entity.siege.*;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.serverpackets.CastleSiegeInfo;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ClanTable;
import ru.l2gw.gameserver.tables.MapRegionTable;
import ru.l2gw.gameserver.tables.SpawnTable;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ClanHallSiege extends Siege
{
	/*Only For ClanHall Siege */
	private ConcurrentHashMap<RolePlaySiegeMember, FastList<L2Player>> attakerClans;

	public ClanHallSiege(ClanHall siegeUnit)
	{
		super(siegeUnit);
		_database = new ClanHallSiegeDatabase(this);
	}

	@Override
	public void startSiege()
	{
		if(!isInProgress())
		{
			_ownerBeforeStart = getSiegeUnit().getOwnerId();

			if(getSiegeUnit().getOwner() != null)
				getSiegeUnit().changeOwner(0);

			if(getAttackerClans().size() <= 0)
			{
				broadcastToPlayer(new SystemMessage(SystemMessage.THE_SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST).addString(getSiegeUnit().getName()), false);
				saveSiege();
				return;
			}

			broadcastToPlayer(new SystemMessage(SystemMessage.THE_SIEGE_TO_CONQUER_S1_HAS_BEGUN).addHideoutName(getSiegeUnit()), true);

			getZone().setActive(true);
			if(!getRezidentZone().isActive())
				getRezidentZone().setActive(true);
			if(!getSiegeUnit().getZone().isActive())
				getSiegeUnit().getZone().setActive(false);
			if(!getSiegeUnit().getHQZone().isActive())
				getSiegeUnit().getHQZone().setActive(true);

			setIsInProgress(true); // Flag so that same siege instance cannot be started again

			SiegeDatabase.loadSiegeClan(this); // Load siege clan from db
			teleportPlayer(TeleportWhoType.All, MapRegionTable.TeleportWhereType.ClosestTown);
			getSiegeUnit().spawnDoor();
			SpawnTable.getInstance().startEventSpawn("ch_" + getSiegeUnit().getId() + "_siege");

			attackersUpdate(false); // Add attackers to list

			_defenderRespawnPenalty = 0; // Reset respawn delay

			// Schedule a task to prepare auto siege end
			_siegeEndDate = Calendar.getInstance();
			_siegeEndDate.add(Calendar.MINUTE, getSiegeLength());
			ThreadPoolManager.getInstance().scheduleGeneral(new SiegeEndTask(this), 1000); // Prepare auto end task

			for(SiegeClan clan : getAttackerClans().values())
				clan.getClan().broadcastToOnlineMembers(new SystemMessage(SystemMessage.THE_SIEGE_OF_THE_CLAN_HALL_HAS_BEGUN));
		}
	}

	@Override
	public void midVictory()
	{
	}

	@Override
	public void reloadRegistredMembers()
	{
		ClanHallSiegeDatabase.loadSiegeClan(this);
	}

	@Override
	public void endSiege()
	{
		getZone().setActive(false);
		if(getRezidentZone().isActive())
			getRezidentZone().setActive(false);
		if(!getSiegeUnit().getZone().isActive())
			getSiegeUnit().getZone().setActive(true);
		if(getSiegeUnit().getHQZone().isActive())
			getSiegeUnit().getHQZone().setActive(false);

		if(isInProgress())
		{
			if(getSiegeUnit().getOwnerId() <= 0)
				broadcastToPlayer(new SystemMessage(SystemMessage.THE_SIEGE_OF_S1_HAS_ENDED_IN_A_DRAW).addHideoutName(getSiegeUnit()), false);
			else
			{
				L2Clan oldOwner = null;
				if(_ownerBeforeStart != 0)
					oldOwner = ClanTable.getInstance().getClan(_ownerBeforeStart);
				L2Clan newOwner = ClanTable.getInstance().getClan(getSiegeUnit().getOwnerId());

				if(oldOwner == null)
				{ // ClanHall was taken over from Npc
					broadcastToPlayer(new SystemMessage(SystemMessage.S1_CLAN_HAS_DEFEATED_S2).addString(newOwner.getName()).addHideoutName(getSiegeUnit()), true);
					if(newOwner.getLevel() >= 4)
						newOwner.broadcastToOnlineMembers(new SystemMessage(SystemMessage.YOUR_CLAN_NEWLY_ACQUIRED_CONTESTED_CLAN_HALL_HAS_ADDED_S1_POINTS_TO_YOUR_CLAN_REPUTATION_SCORE).addNumber(newOwner.incReputation(500, true, "ClanHallSiege")));
				}
				else if(newOwner.equals(oldOwner))
				{ // ClanHall was defended
					broadcastToPlayer(new SystemMessage(SystemMessage.S1_CLAN_HAS_DEFEATED_S2).addString(newOwner.getName()).addHideoutName(getSiegeUnit()), true);
					if(newOwner.getLevel() >= 5)
						newOwner.broadcastToOnlineMembers(new SystemMessage(SystemMessage.SINCE_YOUR_CLAN_EMERGED_VICTORIOUS_FROM_THE_SIEGE_S1_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_REPUTATION_SCORE).addNumber(newOwner.incReputation(500, true, "CastleSiege")));
				}
				else
				{ // ClanHall was taken over by another clan
					broadcastToPlayer(new SystemMessage(SystemMessage.S1_CLAN_HAS_DEFEATED_S2).addString(newOwner.getName()).addHideoutName(getSiegeUnit()), true);
					if(newOwner.getLevel() >= 4)
						newOwner.broadcastToOnlineMembers(new SystemMessage(SystemMessage.YOUR_CLAN_HAS_CAPTURED_YOUR_OPPONENT_CONTESTED_CLAN_HALL_S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_YOUR_OPPONENT_CLAN_REPUTATION_SCORE).addNumber(newOwner.incReputation(500, true, "ClanHallSiege")));
					if(oldOwner.getLevel() >= 4)
						oldOwner.broadcastToOnlineMembers(new SystemMessage(SystemMessage.AN_OPPOSING_CLAN_HAS_CAPTURED_YOUR_CLAN_CONTESTED_CLAN_HALL_S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_YOUR_CLAN_REPUTATION_SCORE).addNumber(-oldOwner.incReputation(-1000, true, "ClanHallSiege")));
				}
				broadcastToPlayer(new SystemMessage(SystemMessage.THE_SIEGE_OF_S1_IS_FINISHED).addHideoutName(getSiegeUnit()), true);
			}

			SpawnTable.getInstance().stopEventSpawn("ch_" + getSiegeUnit().getId() + "_siege", true);
			setIsInProgress(false); // Flag so that siege instance can be started
			saveSiege(); // Save castle specific data
			ClanHallSiegeDatabase.clearSiegeClan(this); // Clear siege clan from db
			getSiegeUnit().spawnDoor(); // Respawn door to castle
			attackersUpdate(true);
			getSiegeUnit().getZone().setActive(true);
			if(getSiegeUnit().getOwnerId() > 0)
				_database.saveSiegeClan(getSiegeUnit().getOwner(), 1, false);

		}
	}

	@Override
	public void Engrave(L2Clan clan, int objId)
	{}

	/**
	 * Start the auto tasks<BR><BR>
	 */
	@Override
	public void startAutoTask()
	{
		correctSiegeDateTime();
		_log.info("Siege of " + getSiegeUnit().getName() + ": " + _siegeDate.getTime());
		ClanHallSiegeDatabase.loadSiegeClan(this);
		// Schedule registration end
		_siegeRegistrationEndDate = Calendar.getInstance();
		_siegeRegistrationEndDate.setTimeInMillis(_siegeDate.getTimeInMillis());
		_siegeRegistrationEndDate.add(Calendar.MINUTE, -10);
		_siegeRegistrationStartDate = Calendar.getInstance();
		_siegeRegistrationStartDate.setTimeInMillis(_siegeDate.getTimeInMillis());
		_siegeRegistrationStartDate.add(Calendar.HOUR, -getSiegeUnit().getSiege().getRegestrationTime());

		if(_siegeRegistrationEndDate.getTimeInMillis() < System.currentTimeMillis())
			_isRegistrationOver = true;
		else
			_isRegistrationStarted = true;

		// Schedule siege auto start
		ThreadPoolManager.getInstance().scheduleGeneral(new SiegeStartTask(this), 1);
	}

	/** Set the date for the next siege. */
	@Override
	protected void setNextSiegeDate()
	{
		_siegeDate.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
		_siegeDate.set(Calendar.HOUR_OF_DAY, 18);
		_siegeDate.set(Calendar.MINUTE, 0);
		_siegeDate.set(Calendar.SECOND, 0);
		_siegeDate.set(Calendar.MILLISECOND, 0);

		while(_siegeDate.getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
			_siegeDate.add(Calendar.DAY_OF_MONTH, 14);

		if(!SevenSigns.getInstance().isDateInSealValidPeriod(_siegeDate))
			_siegeDate.add(Calendar.DAY_OF_MONTH, 7);

		_isRegistrationOver = false;
		_isRegistrationStarted = true;
	}

	@Override
	public void correctSiegeDateTime()
	{
		setNextSiegeDate();
	}

	/** Display list of registered clans */
	@Override
	public void listRegisterClan(L2Player player)
	{
		player.sendPacket(new CastleSiegeInfo(getSiegeUnit()));
	}

	@Override
	public void registerDefender(L2Player player, boolean force)
	{
		registerAttacker(player, force);
	}

	@Override
	public void addDefender(final SiegeClan sc)
	{
	}

	protected void BossSay(L2NpcInstance boss)
	{}

	public boolean isNpcTaken(int npcVar)
	{
		return false;
	}

	public boolean isCountFull(int clanId)
	{
		return false;
	}

	public int getNpc(int clanId)
	{
		return 0;
	}

	@Override
	public int getRegestrationTime()
	{
		return 2;
	}

	public boolean includeFlags()
	{
		return true;
	}

	/**
	 * ONLY FOR ClanHall RolePlay Siege
	 * @return HashMap index = RolePlaySiegeMember Values = forClan
	 */
	public ConcurrentHashMap<RolePlaySiegeMember, FastList<L2Player>> getAttakerClans()
	{
		if(attakerClans == null)
			attakerClans = new ConcurrentHashMap<RolePlaySiegeMember, FastList<L2Player>>();
		return attakerClans;
	}

	public final void teleportPlayer(final TeleportWhoType teleportWho, final MapRegionTable.TeleportWhereType teleportWhere, List<L2Player> players)
	{
		for(final L2Player player : players)
			if(player != null && !player.isGM())
			{
				if(teleportWho == TeleportWhoType.Owner && teleportWhere == MapRegionTable.TeleportWhereType.ClanHall)
				{
					for(final RolePlaySiegeMember rps : getAttakerClans().keySet())
						if(rps.getClanId() == player.getClanId())
							if(getAttakerClans().get(rps).contains(player))
							{
								player.teleToLocation(getSiegeUnit().getZone().getSpawn());
							}
							else
								player.teleToLocation(MapRegionTable.getInstance().getTeleToLocation(player, MapRegionTable.TeleportWhereType.ClosestTown));
					continue;
				}
				if(teleportWho == TeleportWhoType.Owner && teleportWhere == MapRegionTable.TeleportWhereType.Headquarter)
				{
					for(final RolePlaySiegeMember rps : getAttakerClans().keySet())
						if(rps.getClanId() == player.getClanId())
							if(getAttakerClans().get(rps).contains(player))
								player.teleToLocation(getZone().getSpawn());
							else
								player.teleToLocation(MapRegionTable.getInstance().getTeleToLocation(player, MapRegionTable.TeleportWhereType.ClosestTown));
					continue;
				}

				super.teleportPlayer(teleportWho, teleportWhere, players);
			}
	}

	@Override
	public void removeSiegeClan(int clanId)
	{
		_log.warn("remove from siege clanId: " + clanId);
		if(clanId <= 0  || !SiegeDatabase.checkIsRegistered(clanId, this))
			return;
		attackerClans.remove(clanId);
		SiegeDatabase.removeSiegeClan(clanId, this);
		_log.warn("remove from siege clanId: " + clanId + " removed from " + getSiegeUnit());
	}

	@Override
	public void registerAttacker(L2Player player, boolean force)
	{
		if(player.getClanId() == 0)
			return;

		if(!isRegistrationStarted())
		{
			player.sendPacket(new SystemMessage(SystemMessage.THIS_IS_NOT_THE_TIME_FOR_SIEGE_REGISTRATION_AND_SO_REGISTRATION_AND_CANCELLATION_CANNOT_BE_DONE));
			return;
		}
		else if(isRegistrationOver())
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_TOO_LATE_THE_REGISTRATION_PERIOD_IS_OVER));
			return;
		}
		else if(isInProgress())
		{
			player.sendPacket(new SystemMessage(SystemMessage.THIS_IS_NOT_THE_TIME_FOR_SIEGE_REGISTRATION_AND_SO_REGISTRATION_AND_CANCELLATION_CANNOT_BE_DONE));
			return;
		}
		else if(player.getClanId() == 0 || player.getClan().getLevel() < getSiegeClanMinLevel())
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_CLANS_WITH_LEVEL_4_AND_HIGHER_MAY_REGISTER_FOR_A_CASTLE_SIEGE));
			return;
		}

		_database.saveSiegeClan(player.getClan(), 1, false); // Save to database
	}
}