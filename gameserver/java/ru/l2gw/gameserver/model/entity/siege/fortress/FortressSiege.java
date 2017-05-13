package ru.l2gw.gameserver.model.entity.siege.fortress;

import javolution.util.FastList;
import javolution.util.FastMap;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Spawn;
import ru.l2gw.gameserver.model.entity.Fortress;
import ru.l2gw.gameserver.model.entity.siege.*;
import ru.l2gw.gameserver.model.instances.L2StaticObjectInstance;
import ru.l2gw.gameserver.serverpackets.StaticObject;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ClanTable;
import ru.l2gw.gameserver.tables.MapRegionTable;
import ru.l2gw.gameserver.tables.NpcTable;

import java.util.Calendar;

public class FortressSiege extends Siege
{
	private FortressSiegeStartTask _startSiegeTask = null;

	private FortressSiegeGuardManager _fortressSiegeGuardManager;

	public FortressSiege(Fortress siegeUnit)
	{
		super(siegeUnit);
		_flagPoles = new FastList<L2StaticObjectInstance>();
		_commanders = new FastList<L2Spawn>();
		_peaceNpc = new FastList<L2Spawn>();
		_engrave = new FastMap<Integer, Integer>();
		_database = new FortressSiegeDatabase(this);
		SiegeDatabase.loadSiegeClan(this);
		_fortressSiegeGuardManager = new FortressSiegeGuardManager(_siegeUnit);
	}

	@Override
	public void startSiege()
	{
		if(!isInProgress())
		{
			getZone().setActive(true);
			if(!getRezidentZone().isActive())
				getRezidentZone().setActive(true);
			if(getSiegeUnit().getZone().isActive())
				getSiegeUnit().getZone().setActive(false);
			if(!getSiegeUnit().getHQZone().isActive())
				getSiegeUnit().getHQZone().setActive(true);
			setIsInProgress(true); // Flag so that same siege instance cannot be started again
			_ownerBeforeStart = getSiegeUnit().getOwnerId();
			announceToAttackers(new SystemMessage(SystemMessage.THE_FORTRESS_BATTLE_S1_HAS_BEGUN).addString(getSiegeUnit().getName()));
			announceToDefenders(new SystemMessage(SystemMessage.THE_FORTRESS_BATTLE_S1_HAS_BEGUN).addString(getSiegeUnit().getName()));

			//_database.loadSiegeClan(); // Load siege clan from db
			updatePlayerSiegeStateFlags(false);
			teleportPlayer(TeleportWhoType.Attacker, MapRegionTable.TeleportWhereType.ClosestTown); // Teleport to the closest town
			teleportPlayer(TeleportWhoType.Spectator, MapRegionTable.TeleportWhereType.ClosestTown); // Teleport to the closest town

			startFameTask();
			spawnCommanders(); // Spawn commander
			getSiegeUnit().reinforceDoors(true);
			getSiegeUnit().spawnDoor(); // Spawn door
			_fortressSiegeGuardManager.spawnSiegeGuard(); // Spawn siege guard
			defendersUpdate(false); // Add defenders to list
			attackersUpdate(false); // Add attackers to list
			_defenderRespawnPenalty = 0; // Reset respawn delay

			// Schedule a task to prepare auto siege end
			_siegeEndDate = Calendar.getInstance();
			_siegeEndDate.add(Calendar.MINUTE, getSiegeLength());
			ThreadPoolManager.getInstance().scheduleGeneral(new SiegeEndTask(this), 1000); // Prepare auto end task
			if(_spawnMerchantTask != null)
				_spawnMerchantTask.cancel(true);

			_database.saveSiegeDate();

			_spawnMerchantTask = ThreadPoolManager.getInstance().scheduleGeneral(new SpawnMerchantTask(), getNextSiegePeriod()); // Prepare auto end task

			for(L2StaticObjectInstance flagPole : _flagPoles)
				flagPole.broadcastPacket(new StaticObject(flagPole));
		}
	}

	@Override
	public void midVictory()
	{
	}

	@Override
	public void endSiege()
	{
		_log.info("endSiege: " + getSiegeUnit());

		getZone().setActive(false);
		if(getRezidentZone().isActive())
			getRezidentZone().setActive(false);
		if(!getSiegeUnit().getZone().isActive())
			getSiegeUnit().getZone().setActive(true);
		if(getSiegeUnit().getHQZone().isActive())
			getSiegeUnit().getHQZone().setActive(false);

		if(isInProgress())
		{
			announceToAttackers(new SystemMessage(SystemMessage.THE_FORTRESS_BATTLE_OF_S1_HAS_FINISHED).addHideoutName(getSiegeUnit()));
			announceToDefenders(new SystemMessage(SystemMessage.THE_FORTRESS_BATTLE_OF_S1_HAS_FINISHED).addHideoutName(getSiegeUnit()));

			if(getSiegeUnit().getOwnerId() > 0)
			{
				L2Clan oldOwner = null;
				if(_ownerBeforeStart != 0)
					oldOwner = ClanTable.getInstance().getClan(_ownerBeforeStart);
				L2Clan newOwner = ClanTable.getInstance().getClan(getSiegeUnit().getOwnerId());

				if(oldOwner != newOwner)
				{
					if(newOwner.getLevel() >= 5)
						newOwner.broadcastToOnlineMembers(new SystemMessage(SystemMessage.SINCE_YOUR_CLAN_EMERGED_VICTORIOUS_FROM_THE_SIEGE_S1_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_REPUTATION_SCORE).addNumber(newOwner.incReputation(_crpWin, true, "FortressSiege")));

					_siegeUnit.setLastSiegeDate((int)(System.currentTimeMillis() / 1000));
					_database.saveLastSiegeDate();
					_siegeUnit.startHoldTask();
					_siegeUnit.requestAmbassadors();
					_siegeUnit.setContractCastle(0);
					_siegeUnit.stopFunctions();
					_siegeUnit.setSupplyLevel(0);
					_siegeUnit.setRewardLevel(0);
					SystemMessage sm = new SystemMessage(SystemMessage.S1_CLAN_IS_VICTORIOUS_IN_THE_FORTRESS_BATTLE_OF_S2).addString(newOwner.getName()).addHideoutName(_siegeUnit.getId());
					announceToAttackers(sm);
					announceToDefenders(sm);
				}
			}

			stopFameTask();
			removeHeadquarters();
			unSpawnFlags();
			unspawnCommanders(); // Remove commander from this fort
			_fortressSiegeGuardManager.unspawnSiegeGuard(); // Remove all spawned siege guard from this castle
			removeSiegeSummons();
			operateCommandCenterDoors(false);
			getSiegeUnit().spawnDoor(); // Respawn door to castle
			setIsInProgress(false); // Flag so that siege instance can be started
			updatePlayerSiegeStateFlags(true);
			SiegeDatabase.clearSiegeClan(this); // Clear siege clan from db
			_siegeUnit.removeUpgrade();
			_defenderRespawnPenalty = 0; // Reset respawn delay
			defendersUpdate(true);
			attackersUpdate(true);
			getSiegeUnit().getZone().setActive(true);
			for(L2StaticObjectInstance flagPole : _flagPoles)
				flagPole.broadcastPacket(new StaticObject(flagPole));
		}
	}

	@Override
	public void Engrave(L2Clan clan, int objId)
	{
		_engrave.put(objId, clan.getClanId());
		if(_engrave.size() >= _flagPoles.size())
		{
			boolean rst = true;
			for(int id : _engrave.values())
				if(id != clan.getClanId())
					rst = false;
			if(rst)
			{
				_engrave.clear();
				getSiegeUnit().changeOwner(clan.getClanId());
			}
		}
	}

	/**
	 * Start the auto tasks<BR><BR>
	 */
	@Override
	public void startAutoTask()
	{
		if(getAttackerClans().size() > 0 && _startSiegeTask == null)
		{
			if(_siegeDate.getTimeInMillis() > System.currentTimeMillis())
			{
				_siegeRegistrationEndDate = Calendar.getInstance();
				_siegeRegistrationEndDate.setTimeInMillis(_siegeDate.getTimeInMillis());
				_siegeRegistrationEndDate.add(Calendar.MINUTE, -_countdownTime);
				_siegeRegistrationStartDate = Calendar.getInstance();

				_startSiegeTask = new FortressSiegeStartTask(this);
				_startSiegeTask.startTimer();
			}
			else
				getAttackerClans().clear();
		}
	}

	public void setStartSiegeTask(FortressSiegeStartTask task)
	{
		_startSiegeTask = task;
	}

	/** Set the date for the next siege. */
	@Override
	protected void setNextSiegeDate()
	{
		if(_siegeDate.getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
		{
			_siegeDate.setTimeInMillis(System.currentTimeMillis());
			_siegeDate.add(Calendar.MINUTE, _registrationTime + _countdownTime);
			_isRegistrationOver = false; // Allow registration for next siege
			_isRegistrationStarted = true;
		}
		_siegeDate.set(Calendar.SECOND, 0);
	}

	@Override
	public void correctSiegeDateTime()
	{
		if(_siegeDate.getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
		{
			setNextSiegeDate();
			_database.saveSiegeDate();
		}
	}

	/** Display list of registered clans */
	@Override
	public void listRegisterClan(L2Player player)
	{}

	private void spawnCommanders()
	{
		for(L2Spawn spawn : _peaceNpc)
		{
			spawn.stopRespawn();
			spawn.despawnAll();
		}

		if(_commanders.isEmpty())
		{
			for(SiegeSpawn sp : _siegeUnit.getCommanderSpawns())
			{
				try
				{
					L2Spawn spawn = new L2Spawn(NpcTable.getTemplate(sp.getNpcId()));
					spawn.setRespawnDelay(600);
					spawn.setAmount(1);
					spawn.setLoc(sp.getLoc());
					_commanders.add(spawn);
				}
				catch(ClassNotFoundException e)
				{
				}
			}
		}

		for(L2Spawn spawn : _commanders)
		{
			spawn.startRespawn();
			spawn.doSpawn(true);
		}
	}

	private void unSpawnFlags()
	{
		for(CombatFlag cf : _siegeUnit.getFlagList())
			if(cf != null)
				cf.unSpawnMe();
	}

	public boolean includeFlags()
	{
		return true;
	}
}