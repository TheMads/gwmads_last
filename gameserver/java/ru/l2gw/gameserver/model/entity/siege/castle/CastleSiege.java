package ru.l2gw.gameserver.model.entity.siege.castle;

import javolution.util.FastList;
import javolution.util.FastMap;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.instancemanager.MercTicketManager;
import ru.l2gw.gameserver.instancemanager.ServerVariables;
import ru.l2gw.gameserver.instancemanager.SiegeGuardManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.SevenSigns;
import ru.l2gw.gameserver.model.entity.siege.*;
import ru.l2gw.gameserver.model.instances.L2ArtefactInstance;
import ru.l2gw.gameserver.model.instances.L2ControlTowerInstance;
import ru.l2gw.gameserver.serverpackets.CastleSiegeInfo;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ClanTable;
import ru.l2gw.gameserver.tables.DoorTable;
import ru.l2gw.gameserver.tables.MapRegionTable;

import java.util.Calendar;
import java.util.concurrent.Future;

public class CastleSiege extends Siege
{
	private FastList<L2ControlTowerInstance> _controlTowers = new FastList<L2ControlTowerInstance>();
	private FastList<L2ArtefactInstance> _artifacts = new FastList<L2ArtefactInstance>();
	private FastMap<Integer, Integer> _engrave = new FastMap<Integer, Integer>();

	private Future<?> _siegeStartTask;

	public CastleSiege(SiegeUnit castle)
	{
		super(castle);
		_database = new CastleSiegeDatabase(this);
		_siegeGuardManager = new SiegeGuardManager(getSiegeUnit());
		_changeTimeEnd = Calendar.getInstance();
	}

	@Override
	public void startSiege()
	{
		if(!isInProgress())
		{
			if(getAttackerClans().size() <= 0)
			{
				if(getSiegeUnit().getOwnerId() <= 0)
					broadcastToPlayer(new SystemMessage(SystemMessage.THE_SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST).addString(getSiegeUnit().getName()), false);
				else
					broadcastToPlayer(new SystemMessage(SystemMessage.S1S_SIEGE_WAS_CANCELED_BECAUSE_THERE_WERE_NO_CLANS_THAT_PARTICIPATED).addString(getSiegeUnit().getName()), false);

				if(getSiegeUnit().getOwnerId() > 0)
				{
					MercTicketManager.getInstance().deleteTickets(getSiegeUnit().getId());
					SiegeGuardManager.removeMercsFromDb(getSiegeUnit().getId());
					SiegeDatabase.clearSiegeClan(this); // Clear siege clan from db
					_changeTimeEnd.setTimeInMillis(System.currentTimeMillis());
					_changeTimeEnd.add(Calendar.DAY_OF_MONTH, 1);
					setChangeTimeOver(false);
					ServerVariables.set("castle_" + getSiegeUnit().getId() + "_ba", ServerVariables.getInt("castle_" + getSiegeUnit().getId() + "_ba", 0) + 1);
				}

				saveSiege(); // Save castle specific data
				return;
			}

			setIsInProgress(true); // Flag so that same siege instance cannot be started again
			_ownerBeforeStart = getSiegeUnit().getOwnerId();
			_tempAlly = true;

			startFameTask();
			SiegeDatabase.loadSiegeClan(this); // Load siege clan from db
			defendersUpdate(false); // Add defenders to list
			attackersUpdate(false); // Add attackers to list
			updatePlayerSiegeStateFlags(false);
			teleportPlayer(TeleportWhoType.Attacker, MapRegionTable.TeleportWhereType.ClosestTown); // Teleport to the closest town
			teleportPlayer(TeleportWhoType.Spectator, MapRegionTable.TeleportWhereType.ClosestTown); // Teleport to the closest town
			getSiegeUnit().reinforceDoors(true);
			respawnControlTowers(); // Respawn control towers
			getSiegeUnit().spawnDoor(); // Spawn door
			DoorTable.getInstance().notifyEvent("siegeStart_" + getSiegeUnit().getId());
			getSiegeGuardManager().spawnSiegeGuard(); // Spawn siege guard
			MercTicketManager.getInstance().deleteTickets(getSiegeUnit().getId()); // remove the tickets from the ground
			_defenderRespawnPenalty = 0; // Reset respawn delay

			getSiegeUnit().getZone().setActive(false);
			getZone().setActive(true);
			getRezidentZone().setActive(true);
			getSiegeUnit().getHQZone().setActive(true);
			// Schedule a task to prepare auto siege end
			_siegeEndDate = Calendar.getInstance();
			_siegeEndDate.add(Calendar.MINUTE, getSiegeLength());
			ThreadPoolManager.getInstance().scheduleGeneral(new SiegeEndTask(this), 1000); // Prepare auto end task

			announceToAttackers(new SystemMessage(SystemMessage.THE_TEMPORARY_ALLIANCE_OF_THE_CASTLE_ATTACKER_TEAM_IS_IN_EFFECT_IT_WILL_BE_DISSOLVED_WHEN_THE_CASTLE_LORD_IS_REPLACED));

			SystemMessage psm = new SystemMessage(SystemMessage.YOU_HAVE_PARTICIPATED_IN_THE_SIEGE_OF_S1_THIS_SIEGE_WILL_CONTINUE_FOR_2_HOURS).addHideoutName(getSiegeUnit());
			SystemMessage nsm = new SystemMessage(SystemMessage.THE_SIEGE_OF_S1_HAS_STARTED).addHideoutName(getSiegeUnit());
			for(L2Player player : L2ObjectsStorage.getAllPlayers())
				if(player != null)
					if(checkIsClanRegistered(player.getClanId()))
						player.sendPacket(psm);
					else
						player.sendPacket(nsm);
		}
	}

	@Override
	public void midVictory()
	{
		// Если осада закончилась
		if(!isInProgress() || getSiegeUnit().getOwnerId() <= 0)
			return;

		// Если атакуется замок, принадлежащий NPC, и только 1 атакующий - закончить осаду
		if(getDefenderClans().size() == 0 && getAttackerClans().size() == 1)
		{
			SiegeClan sc_newowner = getAttackerClan(getSiegeUnit().getOwnerId());
			removeAttacker(sc_newowner);
			sc_newowner.setTypeId(SiegeClanType.OWNER);
			addDefender(sc_newowner);
			endSiege();
			return;
		}

		int allyId = ClanTable.getInstance().getClan(getSiegeUnit().getOwnerId()).getAllyId();

		// Если атакуется замок, принадлежащий NPC, и все атакующие в одном альянсе - закончить осаду
		if(allyId != 0 && getDefenderClans().size() == 0)
		{
			boolean allinsamealliance = true;
			for(SiegeClan sc : getAttackerClans().values())
				if(sc != null && sc.getClan().getAllyId() != allyId)
					allinsamealliance = false;
			if(allinsamealliance)
			{
				SiegeClan sc_newowner = getAttackerClan(getSiegeUnit().getOwnerId());
				removeAttacker(sc_newowner);
				sc_newowner.setTypeId(SiegeClanType.OWNER);
				addDefender(sc_newowner);
				endSiege();
				return;
			}
		}

		if(_tempAlly)
			announceToAttackers(new SystemMessage(SystemMessage.THE_TEMPORARY_ALLIANCE_OF_THE_CASTLE_ATTACKER_TEAM_HAS_BEEN_DISSOLVED));
		_tempAlly = false;

		// Поменять местами атакующих и защитников
		for(SiegeClan sc : getDefenderClans().values())
			if(sc != null)
			{
				removeDefender(sc);
				addAttacker(sc);
			}

		SiegeClan sc_newowner = getAttackerClan(getSiegeUnit().getOwnerId());
		removeAttacker(sc_newowner);
		removeHeadquarter(sc_newowner);
		sc_newowner.setTypeId(SiegeClanType.OWNER);
		addDefender(sc_newowner);

		// Если у нового владельца клана есть альянс, сделать их защитниками
		if(allyId != 0)
		{
			L2Clan[] clanList = ClanTable.getInstance().getClans();
			for(L2Clan clan : clanList)
				if(clan.getAllyId() == allyId)
				{
					SiegeClan sc = getAttackerClan(clan.getClanId());
					if(sc != null)
					{
						removeAttacker(sc);
						sc.setTypeId(SiegeClanType.DEFENDER);
						addDefender(sc);
					}
				}
		}

		defendersUpdate(false);
		attackersUpdate(false);
		updatePlayerSiegeStateFlags(false);
		teleportPlayer(TeleportWhoType.Attacker, MapRegionTable.TeleportWhereType.ClosestTown);
		teleportPlayer(TeleportWhoType.Spectator, MapRegionTable.TeleportWhereType.ClosestTown);
		getSiegeGuardManager().unspawnSiegeGuard(); // Remove all spawned siege guard from this castle
		getSiegeUnit().removeUpgrade(); // Remove all castle upgrade
		getSiegeUnit().spawnDoor(true); // Respawn door to castle but make them weaker (50% hp)
		respawnControlTowers();
		_killedCTCount = 0;
	}

	@Override
	public void endSiege()
	{
		if(isInProgress())
		{
			SystemMessage psm = new SystemMessage(SystemMessage.THE_SIEGE_OF_S1_IN_WHICH_YOU_ARE_PARTICIPATING_HAS_FINISHED).addHideoutName(getSiegeUnit());
			SystemMessage nsm = new SystemMessage(SystemMessage.THE_SIEGE_OF_S1_HAS_FINISHED).addHideoutName(getSiegeUnit());
			for(L2Player player : L2ObjectsStorage.getAllPlayers())
				if(player != null)
					if(checkIsClanRegistered(player.getClanId()))
						player.sendPacket(psm);
					else
						player.sendPacket(nsm);

			if(getSiegeUnit().getOwnerId() <= 0)
				broadcastToPlayer(new SystemMessage(SystemMessage.THE_SIEGE_OF_S1_HAS_ENDED_IN_A_DRAW).addHideoutName(getSiegeUnit()), false);
			else
			{
				L2Clan oldOwner = null;
				if(_ownerBeforeStart != 0)
					oldOwner = ClanTable.getInstance().getClan(_ownerBeforeStart);

				if(oldOwner != null)
					oldOwner.getLeader().unsetVar("territory_lord_" + (getSiegeUnit().getId() + 80));

				L2Clan newOwner = ClanTable.getInstance().getClan(getSiegeUnit().getOwnerId());
				TerritoryWarManager.changeTerritoryOwner(getSiegeUnit().getId() + 80, newOwner.getClanId());
				TerritoryWarManager.getTerritoryById(getSiegeUnit().getId() + 80).spawnNpc();

				if(oldOwner == null)
				{ // castle was taken over from scratch
					broadcastToPlayer(new SystemMessage(SystemMessage.CLAN_S1_IS_VICTORIOUS_OVER_S2S_CASTLE_SIEGE).addString(newOwner.getName()).addHideoutName(getSiegeUnit()), false);
					if(newOwner.getLevel() >= 5)
						newOwner.broadcastToOnlineMembers(new SystemMessage(SystemMessage.SINCE_YOUR_CLAN_EMERGED_VICTORIOUS_FROM_THE_SIEGE_S1_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_REPUTATION_SCORE).addNumber(newOwner.incReputation(1500, true, "CastleSiege")));
					ServerVariables.set("castle_" + getSiegeUnit().getId() + "_ba", 1);
				}
				else if(newOwner.equals(oldOwner))
				{ // castle was defended
					broadcastToPlayer(new SystemMessage(SystemMessage.CLAN_S1_IS_VICTORIOUS_OVER_S2S_CASTLE_SIEGE).addString(newOwner.getName()).addHideoutName(getSiegeUnit()), false);
					if(newOwner.getLevel() >= 5)
						newOwner.broadcastToOnlineMembers(new SystemMessage(SystemMessage.SINCE_YOUR_CLAN_EMERGED_VICTORIOUS_FROM_THE_SIEGE_S1_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_REPUTATION_SCORE).addNumber(newOwner.incReputation(500, true, "CastleSiege")));

					ServerVariables.set("castle_" + getSiegeUnit().getId() + "_ba", ServerVariables.getInt("castle_" + getSiegeUnit().getId() + "_ba", 0) + 1);
				}
				else
				{ // castle was taken over by another clan
					broadcastToPlayer(new SystemMessage(SystemMessage.CLAN_S1_IS_VICTORIOUS_OVER_S2S_CASTLE_SIEGE).addString(newOwner.getName()).addHideoutName(getSiegeUnit()), false);
					if(newOwner.getLevel() >= 5)
						newOwner.broadcastToOnlineMembers(new SystemMessage(SystemMessage.SINCE_YOUR_CLAN_EMERGED_VICTORIOUS_FROM_THE_SIEGE_S1_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_REPUTATION_SCORE).addNumber(newOwner.incReputation(1500, true, "CastleSiege")));
					if(oldOwner.getLevel() >= 5)
						oldOwner.broadcastToOnlineMembers(new SystemMessage(SystemMessage.YOUR_CLAN_HAS_FAILED_TO_DEFEND_THE_CASTLE_S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_YOUR_CLAN_REPUTATION_SCORE).addNumber(-oldOwner.incReputation(-3000, true, "CastleSiege")));
					ServerVariables.set("castle_" + getSiegeUnit().getId() + "_ba", 1);
					getSiegeUnit().stopFunctions();
				}
			}

			stopFameTask();
			removeHeadquarters();
			removeSiegeSummons();
			setIsInProgress(false); // Flag so that siege instance can be started
			_killedCTCount = 0;
			_tempAlly = false;
			updatePlayerSiegeStateFlags(true);
			_changeTimeEnd.setTimeInMillis(System.currentTimeMillis());
			_changeTimeEnd.add(Calendar.DAY_OF_MONTH, 1);
			setChangeTimeOver(false);
			saveSiege(); // Save castle specific data
			SiegeDatabase.clearSiegeClan(this); // Clear siege clan from db
			respawnControlTowers(); // Remove all control tower from this castle
			getSiegeGuardManager().unspawnSiegeGuard(); // Remove all spawned siege guard from this castle
			SiegeGuardManager.removeMercsFromDb(getSiegeUnit().getId());
			getSiegeUnit().spawnDoor(); // Respawn door to castle
			DoorTable.getInstance().notifyEvent("siegeEnd_" + getSiegeUnit().getId());
			_defenderRespawnPenalty = 0; // Reset respawn delay
			defendersUpdate(true);
			attackersUpdate(true);
			getSiegeUnit().removeUpgrade();
		}

		getZone().setActive(false);
		getSiegeUnit().getZone().setActive(true);
		getRezidentZone().setActive(false);
		getSiegeUnit().getHQZone().setActive(false);
	}

	@Override
	public void Engrave(L2Clan clan, int objId)
	{
		_engrave.put(objId, clan.getClanId());
		if(_engrave.size() >= _artifacts.size())
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
		correctSiegeDateTime();
		_log.info("Siege of " + getSiegeUnit().getName() + ": " + _siegeDate.getTime());
		SiegeDatabase.loadSiegeClan(this);
		// Schedule registration end
		_siegeRegistrationEndDate = Calendar.getInstance();
		_siegeRegistrationEndDate.setTimeInMillis(_siegeDate.getTimeInMillis());
		_siegeRegistrationEndDate.add(Calendar.DAY_OF_MONTH, -1);
		_siegeRegistrationStartDate = Calendar.getInstance();
		_siegeRegistrationStartDate.setTimeInMillis(Calendar.getInstance().getTimeInMillis());

		if(_siegeRegistrationEndDate.getTimeInMillis() < System.currentTimeMillis())
			_isRegistrationOver = true;
		else
			_isRegistrationStarted = true;

		// Schedule siege auto start
		if(_siegeStartTask != null)
			_siegeStartTask.cancel(true);

		_siegeStartTask = ThreadPoolManager.getInstance().scheduleGeneral(new SiegeStartTask(this), 1000);
	}

	/**
	 * Set the date for the next siege.
	 */
	@Override
	public void setNextSiegeDate()
	{
		while(_siegeDate.getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
		{
			// Set next siege date if siege has passed
			if(_siegeDate.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && _siegeDate.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY)
				_siegeDate.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
			// set the next siege day to the next weekend
			_siegeDate.add(Calendar.DAY_OF_MONTH, 7);
		}
		if(!SevenSigns.getInstance().isDateInSealValidPeriod(_siegeDate))
			_siegeDate.add(Calendar.DAY_OF_MONTH, 7);

		_isRegistrationOver = false; // Allow registration for next siege
		_isRegistrationStarted = true;
	}

	@Override
	public void correctSiegeDateTime()
	{
		boolean corrected = false;
		if(_siegeDate.getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
		{
			// Since siege has past reschedule it to the next one (14 days)
			// This is usually caused by server being down
			corrected = true;
			setNextSiegeDate();
		}

		if(!SevenSigns.getInstance().isDateInSealValidPeriod(_siegeDate))
		{
			// no sieges in Quest period! reschedule it to the next SealValidationPeriod
			// This is usually caused by server being down
			corrected = true;
			setNextSiegeDate();
		}

		if(_siegeDate.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY || (_siegeDate.get(Calendar.HOUR_OF_DAY) != 16 && _siegeDate.get(Calendar.HOUR_OF_DAY) != 20))
		{
			corrected = true;
			_siegeDate.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
			if(_siegeDate.get(Calendar.HOUR_OF_DAY) != 16 && _siegeDate.get(Calendar.HOUR_OF_DAY) != 20)
				_siegeDate.set(Calendar.HOUR_OF_DAY, 20);
		}

		_siegeDate.set(Calendar.MINUTE, 0);
		_siegeDate.set(Calendar.SECOND, 0);
		_siegeDate.set(Calendar.MILLISECOND, 0);

		if(corrected)
			_database.saveSiegeDate();
	}

	/**
	 * Display list of registered clans
	 */
	@Override
	public void listRegisterClan(L2Player player)
	{
		player.sendPacket(new CastleSiegeInfo(getSiegeUnit()));
	}

	/**
	 * Remove all control tower spawned.
	 */
	private void respawnControlTowers()
	{
		// Remove all instance of control tower for this castle
		for(L2ControlTowerInstance ct : _controlTowers)
			if(ct != null)
			{
				ct.decayMe();
				ct.spawnMe();
			}
	}

	public void addControlTower(L2ControlTowerInstance tower)
	{
		_controlTowers.add(tower);
	}

	public void addArtifact(L2ArtefactInstance art)
	{
		_artifacts.add(art);
	}

	@Override
	public void updateSiegeTime()
	{
		setChangeTimeOver(true);
		getDatabase().saveSiegeDate();

		// Корректируем время регистрации на осаду
		_siegeRegistrationEndDate = Calendar.getInstance();
		_siegeRegistrationEndDate.setTimeInMillis(_siegeDate.getTimeInMillis());
		_siegeRegistrationEndDate.add(Calendar.DAY_OF_MONTH, -1);
		_log.info("Siege of " + getSiegeUnit().getName() + " changed to: " + _siegeDate.getTime());
		// Перезапускаем такс на старт осады с новой датой
		if(_siegeStartTask != null)
			_siegeStartTask.cancel(true);

		_siegeStartTask = ThreadPoolManager.getInstance().scheduleGeneral(new SiegeStartTask(this), 1000);
	}

	public boolean includeFlags()
	{
		return true;
	}
}