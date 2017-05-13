package ru.l2gw.gameserver.model.entity.siege;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.FortressSiegeManager;
import ru.l2gw.gameserver.instancemanager.SiegeGuardManager;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.entity.siege.fortress.CombatFlag;
import ru.l2gw.gameserver.model.instances.*;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.ExBrExtraUserInfo;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.serverpackets.UserInfo;
import ru.l2gw.gameserver.tables.ClanTable;
import ru.l2gw.gameserver.tables.MapRegionTable;
import ru.l2gw.gameserver.tables.NpcTable;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public abstract class Siege
{

	protected FastList<L2StaticObjectInstance> _flagPoles = null;
	protected List<L2Spawn> _commanders = null;
	protected List<L2Spawn> _peaceNpc = null;
	protected FastMap<Integer, Integer> _engrave = null;
	protected Future<?> _spawnMerchantTask = null;
	protected boolean _tempAlly = false;

	public class RolePlaySiegeMember
	{
		int clanId;
		int npcVar;

		public RolePlaySiegeMember(int clanId, int npcVar)
		{
			this.clanId = clanId;
			this.npcVar = npcVar;
		}

		public void setNpc(int npcVar)
		{
			this.npcVar = npcVar;
		}

		public int getNpc()
		{
			return npcVar;
		}

		public int getClanId()
		{
			return clanId;
		}
	}

	protected static Log _log = LogFactory.getLog("siege");

	private int _defenderRespawnDelay = 10000;
	private int _siegeClanMinLevel = 4;
	private int _siegeLength = 120;
	private int _nextSiegePeriod = 14;

	protected ConcurrentHashMap<Integer, SiegeClan> attackerClans = new ConcurrentHashMap<Integer, SiegeClan>();
	protected ConcurrentHashMap<Integer, SiegeClan> defenderClans = new ConcurrentHashMap<Integer, SiegeClan>();
	protected ConcurrentHashMap<Integer, SiegeClan> defenderWaitingClans = new ConcurrentHashMap<Integer, SiegeClan>();

	protected SiegeUnit _siegeUnit;
	protected SiegeDatabase _database;
	protected SiegeGuardManager _siegeGuardManager;

	protected boolean _isInProgress = false;
	protected boolean _isRegistrationOver = false;
	protected boolean _isChangeTimeOver = false;
	protected boolean _isRegistrationStarted = false;
	protected int _registrationTime;
	protected int _countdownTime;
	protected int _crpWin;
	protected int _ownerBeforeStart;
	protected int _defenderRespawnPenalty;
	protected int _killedCTCount = 0;

	// Время осады
	protected Calendar _siegeDate;
	// Время конца осады
	protected Calendar _siegeEndDate;
	// Время конца регистрации на осаду
	protected Calendar _siegeRegistrationEndDate;
	// Время конца смены времени осады
	protected Calendar _changeTimeEnd;

	protected Calendar _siegeRegistrationStartDate;

	// Fame System
	private int _famePoints;
	private Future<?> _fameTask;

	public Siege(SiegeUnit siegeUnit)
	{
		_siegeUnit = siegeUnit;
		_siegeDate = Calendar.getInstance();
	}

	public L2Zone getZone()
	{
		return getSiegeUnit().getSiegeZone();
	}

	public L2Zone getRezidentZone()
	{
		return getSiegeUnit().getRezidentZone();
	}

	/**
	 * When siege starts<BR><BR>
	 */
	public abstract void startSiege();

	/**
	 * When control of castle changed during siege<BR><BR>
	 */
	public abstract void midVictory();

	/** Display list of registered clans */
	public abstract void listRegisterClan(L2Player player);

	/**
	 * When siege ends<BR><BR>
	 */
	public abstract void endSiege();

	public abstract void Engrave(L2Clan clan, int objId);

	/**
	 * Start the auto tasks<BR><BR>
	 */
	public abstract void startAutoTask();

	protected abstract void setNextSiegeDate();

	public abstract void correctSiegeDateTime();

	public SiegeUnit getSiegeUnit()
	{
		return _siegeUnit;
	}

	public SiegeGuardManager getSiegeGuardManager()
	{
		return _siegeGuardManager;
	}

	public SiegeDatabase getDatabase()
	{
		return _database;
	}

	/** Return true if object is inside the zone */
	public boolean checkIfInZone(int x, int y)
	{
		return isInProgress() && (getSiegeUnit().checkIfInSiegeZone(x, y));
	}

	public void reloadRegistredMembers()
	{
		SiegeDatabase.loadSiegeClan(this);
	}

	/**
	 * Announce to player.<BR><BR>
	 * @param gsp The String of the message to send to player
	 * @param participants The boolean flag to show message to players in area only.
	 */
	public void broadcastToPlayer(L2GameServerPacket gsp, boolean participants)
	{
		for(L2Player player : L2ObjectsStorage.getAllPlayers())
			if(player != null && (!participants || checkIsClanRegistered(player.getClanId())))
				player.sendPacket(gsp);
	}

	public void updatePlayerSiegeStateFlags(boolean clear)
	{
		L2Clan clan;
		for(SiegeClan siegeClan : getAttackerClans().values())
		{
			clan = ClanTable.getInstance().getClan(siegeClan.getClanId());
			for(L2Player member : clan.getOnlineMembers(""))
			{
				if(clear)
				{
					member.setSiegeState(0);
					member.setSiegeId(0);
					L2ItemInstance weapon = member.getActiveWeaponInstance();
					if(weapon != null && weapon.getItemId() == 9819)
					{
						member.getInventory().unEquipItemAndSendChanges(weapon);
						member.destroyItemByItemId("Siege", weapon.getItemId(), 1, null, true);
					}
				}
				else
				{
					member.setSiegeState(1);
					member.setSiegeId(getSiegeUnit().getId());
				}
				member.sendUserInfo(true);
			}
		}

		for(SiegeClan siegeclan : getDefenderClans().values())
		{
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			for(L2Player member : clan.getOnlineMembers(""))
			{
				if(clear)
				{
					member.setSiegeState(0);
					member.setSiegeId(0);
					if(member.isHero())
						member.updateHeroHistory(new CustomMessage("HeroCastleTaken", Config.DEFAULT_LANG).addString(getSiegeUnit().getName()).toString());
				}
				else
				{
					member.setSiegeState(2);
					member.setSiegeId(getSiegeUnit().getId());
				}
				member.sendPacket(new UserInfo(member));
				member.sendPacket(new ExBrExtraUserInfo(member));
			}
		}

		for(SiegeClan siegeclan : getAttackerClans().values())
		{
			 clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			 for (L2Player member : clan.getOnlineMembers(""))
			 {
				for(L2Player pl : L2World.getAroundPlayers(member))
					member.sendRelation(pl);
			 }
		}

		for(SiegeClan siegeclan : getDefenderClans().values())
		{
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			for (L2Player member : clan.getOnlineMembers(""))
			{
				for(L2Player pl : L2World.getAroundPlayers(member))
					member.sendRelation(pl);
			}
		}
	}

	/** Return list of L2Player in the zone. */
	public List<L2Player> getPlayersInZone()
	{
		List<L2Player> players = new FastList<L2Player>();
		for(L2Object object : getZone().getCharacters())
			if(object.isPlayer())
				players.add((L2Player) object);
		return players;
	}

	/**
	 * Teleport players
	 */
	public final void teleportPlayer(final TeleportWhoType teleportWho, final MapRegionTable.TeleportWhereType teleportWhere)
	{
		List<L2Player> players = new FastList<L2Player>();
		final int ownerId = getSiegeUnit().getOwnerId();
		switch(teleportWho)
		{
			case Owner:
				if(ownerId > 0)
					for(final L2Player player : getPlayersInZone())
						if(player.getClanId() != 0 && player.getClanId() == ownerId)
							players.add(player);
				break;
			case Attacker:
				for(final L2Player player : getPlayersInZone())
					if(player.getClanId() != 0 && getAttackerClans().get(player.getClanId()) != null)
						if(!getSiegeUnit().isClanHall || getSiegeUnit().isClanHall && getSiegeUnit().getOwnerId() != player.getClanId())
							players.add(player);
				break;
			case Defender:
				for(final L2Player player : getPlayersInZone())
					if(player.getClanId() != 0 && player.getClanId() != ownerId && getDefenderClans().get(player.getClanId()) != null)
						players.add(player);
				break;
			case Spectator:
				for(final L2Player player : getPlayersInZone())
					if(player.getClanId() == 0 || getAttackerClans().get(player.getClanId()) == null && getDefenderClan(player.getClanId()) == null)
						players.add(player);
				break;
			default:
				players = getPlayersInZone();
		}
		teleportPlayer(teleportWho, teleportWhere, players);
	}

	public void teleportPlayer(final TeleportWhoType teleportWho, final MapRegionTable.TeleportWhereType teleportWhere, List<L2Player> players)
	{
		for(final L2Player player : players)
			if(player != null && !player.isGM())
			{
				if(player.getCastingSkill() != null && player.getCastingSkill().getId() == 246)
					player.abortCast();
				if(teleportWho == TeleportWhoType.Defender && teleportWhere == MapRegionTable.TeleportWhereType.Castle)
				{
					player.teleToLocation(getRezidentZone().getSpawn());
					continue;
				}
				if(teleportWho == TeleportWhoType.Attacker && teleportWhere == MapRegionTable.TeleportWhereType.ClanHall)
				{
					player.teleToLocation(getZone().getSpawn());
					continue;
				}
				player.teleToLocation(MapRegionTable.getInstance().getTeleToLocation(player, teleportWhere));
			}
	}

	/**
	 * Set siege date time<BR><BR>
	 * @param siegeDateTime The long of date time in millisecond
	 */
	public void setSiegeDateTime(long siegeDateTime)
	{
		_siegeDate.setTimeInMillis(siegeDateTime); // Set siege date
	}

	/** Save siege related to database. */
	protected void saveSiege()
	{
		// Выставляем дату прошедшей осады
		getSiegeUnit().setLastSiegeDate((int) (getSiegeDate().getTimeInMillis() / 1000));
		// Сохраняем дату прошедшей осады
		_database.saveLastSiegeDate();
		// Выставляем дату следующей осады
		setNextSiegeDate();
		// Сохраняем дату следующей осады
		_database.saveSiegeDate();
		// Запускаем таск для следующей осады
		startAutoTask();
	}

	/**
	 * Return true if the player can register.<BR><BR>
	 * @param player The L2Player of the player trying to register
	 * @return true if the player can register.
	 */
	protected boolean checkIfCanRegister(L2Player player)
	{
		if(!isRegistrationStarted())
			player.sendPacket(new SystemMessage(SystemMessage.THIS_IS_NOT_THE_TIME_FOR_SIEGE_REGISTRATION_AND_SO_REGISTRATION_AND_CANCELLATION_CANNOT_BE_DONE));
		else if(isRegistrationOver())
			player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_TOO_LATE_THE_REGISTRATION_PERIOD_IS_OVER));
		else if(isInProgress())
			player.sendPacket(new SystemMessage(SystemMessage.THIS_IS_NOT_THE_TIME_FOR_SIEGE_REGISTRATION_AND_SO_REGISTRATION_AND_CANCELLATION_CANNOT_BE_DONE));
		else if(player.getClanId() == 0 || player.getClan().getLevel() < getSiegeClanMinLevel())
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_CLANS_WITH_LEVEL_4_AND_HIGHER_MAY_REGISTER_FOR_A_CASTLE_SIEGE));
		else if(player.getClan().getHasUnit(2))
			player.sendPacket(new SystemMessage(SystemMessage.A_CLAN_THAT_OWNS_A_CASTLE_CANNOT_PARTICIPATE_IN_ANOTHER_SIEGE));
		else if(player.getClanId() == getSiegeUnit().getOwnerId())
			player.sendPacket(new SystemMessage(SystemMessage.THE_CLAN_THAT_OWNS_THE_CASTLE_IS_AUTOMATICALLY_REGISTERED_ON_THE_DEFENDING_SIDE));
		else if(SiegeDatabase.checkIsRegistered(player.getClanId(), this))
			player.sendPacket(new SystemMessage(SystemMessage.YOUR_APPLICATION_HAS_BEEN_DENIED_BECAUSE_YOU_HAVE_ALREADY_SUBMITTED_A_REQUEST_FOR_ANOTHER_SIEGE_BATTLE));
		else
			return true;

		return false;
	}

	public void registerAttacker(L2Player player, boolean force)
	{
		if(player.getClanId() == 0)
			return;
		int allyId = 0;
		if(getSiegeUnit().getOwnerId() != 0)
		{
			L2Clan castleClan = ClanTable.getInstance().getClan(getSiegeUnit().getOwnerId());
			if(castleClan != null)
				allyId = castleClan.getAllyId();
		}
		if(allyId != 0)
			if(player.getAllyId() == allyId && !force)
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_REGISTER_ON_THE_ATTACKING_SIDE_BECAUSE_YOU_ARE_PART_OF_AN_ALLIANCE_WITH_THE_CLAN_THAT_OWNS_THE_CASTLE));
				return;
			}
		if(force || checkIfCanRegister(player))
			_database.saveSiegeClan(player.getClan(), 1, false); // Save to database
	}

	public void registerDefender(L2Player player, boolean force)
	{
		if(getSiegeUnit().getOwnerId() <= 0)
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.entity.siege.Siege.OwnedByNPC", player).addString(getSiegeUnit().getName()));
		else if(force || checkIfCanRegister(player))
			_database.saveSiegeClan(player.getClan(), 2, false); // Save to database
	}

	public void addDefender(SiegeClan sc)
	{
		getDefenderClans().put(sc.getClanId(), sc);
	}

	protected void addDefenderWaiting(SiegeClan sc)
	{
		getDefenderWaitingClans().put(sc.getClanId(), sc);
	}

	protected void addAttacker(SiegeClan sc)
	{
		sc.setTypeId(SiegeClanType.ATTACKER);
		getAttackerClans().put(sc.getClanId(), sc);
	}

	protected void removeDefender(SiegeClan sc)
	{
		if(sc != null)
			getDefenderClans().remove(sc.getClanId());
	}

	protected void removeAttacker(SiegeClan sc)
	{
		if(sc != null)
			getAttackerClans().remove(sc.getClanId());
	}

	public SiegeClan getAttackerClan(int clanId)
	{
		if(clanId <= 0)
			return null;
		return getAttackerClans().get(clanId);
	}

	public SiegeClan getDefenderClan(int clanId)
	{
		if(clanId <= 0)
			return null;
		return getDefenderClans().get(clanId);
	}

	public SiegeClan getDefenderWaitingClan(int clanId)
	{
		if(clanId <= 0)
			return null;
		return getDefenderWaitingClans().get(clanId);
	}

	/**
	 * Approve clan as defender for siege<BR><BR>
	 * @param clanId The int of player's clan id
	 */
	public void approveSiegeDefenderClan(int clanId)
	{
		if(clanId <= 0)
			return;
		_database.saveSiegeClan(ClanTable.getInstance().getClan(clanId), 0, true);
		SiegeDatabase.loadSiegeClan(this);
	}

	protected void defendersUpdate(boolean end)
	{
		for(SiegeClan clan : getDefenderClans().values())
			if(end)
			{
				clan.getClan().setSiege(null);
				clan.getClan().setDefender(false);
				clan.getClan().setAttacker(false);
			}
			else
			{
				clan.getClan().setSiege(this);
				clan.getClan().setDefender(true);
				clan.getClan().setAttacker(false);
			}
	}

	protected void attackersUpdate(boolean end)
	{
		for(SiegeClan clan : getAttackerClans().values())
		{
			if(clan != null)
			{
				if(end)
				{
					clan.getClan().setSiege(null);
					clan.getClan().setDefender(false);
					clan.getClan().setAttacker(false);
				}
				else
				{
					clan.getClan().setSiege(this);
					clan.getClan().setDefender(false);
					clan.getClan().setAttacker(true);
				}
			}
		}
	}

	/**
	 * Return true if clan is attacker<BR><BR>
	 * @param clanId The L2Clan of the player
	 */
	public boolean checkIsAttacker(int clanId)
	{
		return getAttackerClan(clanId) != null;
	}

	/**
	 * Return true if clan is defender<BR><BR>
	 * @param clanId The L2Clan of the player
	 */
	public boolean checkIsDefender(int clanId)
	{
		return getDefenderClan(clanId) != null;
	}

	/**
	 * Return true if clan is defender waiting approval<BR><BR>
	 * @param clanId The L2Clan of the player
	 */
	public boolean checkIsDefenderWaiting(int clanId)
	{
		return getDefenderWaitingClan(clanId) != null;
	}

	/**
	 * Return true if clan is registered to the siege<BR><BR>
	 * @param clanId The clan id of the player
	 */
	public boolean checkIsClanRegistered(int clanId)
	{
		return getAttackerClan(clanId) != null || getDefenderClan(clanId) != null || getDefenderWaitingClan(clanId) != null;
	}

	public int getDefenderRespawnTotal()
	{
		return _defenderRespawnDelay + _defenderRespawnPenalty;
	}

	public boolean isInProgress()
	{
		return _isInProgress;
	}

	public void setIsInProgress(boolean isInProgress)
	{
		_isInProgress = isInProgress;
	}

	public boolean isRegistrationOver()
	{
		return _isRegistrationOver;
	}

	public boolean isRegistrationStarted()
	{
		return _isRegistrationStarted;
	}

	public void setRegistrationStarted(boolean value)
	{
		_isRegistrationStarted = value;
	}

	public void setRegistrationOver(boolean value)
	{
		_isRegistrationOver = value;
	}

	public Calendar getSiegeDate()
	{
		return _siegeDate;
	}

	public Calendar getSiegeEndDate()
	{
		return _siegeEndDate;
	}

	public long getTimeRemaining()
	{
		return getSiegeDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
	}

	private static final short[] SIEGE_SUMMONS = {
			1459,
			14768,
			14769,
			14770,
			14771,
			14772,
			14773,
			14774,
			14775,
			14776,
			14777,
			14778,
			14779,
			14780,
			14781,
			14782,
			14783,
			14784,
			14785,
			14786,
			14787,
			14788,
			14789,
			14790,
			14791,
			14792,
			14793,
			14794,
			14795,
			14796,
			14797,
			14798,
			14839 };

	protected void removeSiegeSummons()
	{
		for(L2Player player : getPlayersInZone())
			for(short id : SIEGE_SUMMONS)
				if(player.isPetSummoned() && id == player.getPet().getNpcId())
					player.getPet().unSummon();
	}

	/** Remove all Headquarters */
	protected void removeHeadquarters()
	{
		for(final SiegeClan sc : getAttackerClans().values())
			if(sc != null)
				sc.getClan().removeCamp();
		for(final SiegeClan sc : getDefenderClans().values())
			if(sc != null)
				sc.getClan().removeCamp();
	}

	protected void removeHeadquarter(SiegeClan clan)
	{
		clan.getClan().removeCamp();
	}

	public L2NpcInstance getHeadquarter(int clanId)
	{
		if(clanId > 0)
		{
			SiegeClan sc = getAttackerClan(clanId);
			if(sc != null)
				return sc.getClan().getCamp();
		}
		return null;
	}

	/**
	 * Control Tower was killed
	 * Add respawn penalty to defenders for each control tower lose
	 */
	public void killedCT()
	{
		_defenderRespawnPenalty += _siegeUnit.getControlTowerLosePenalty();
		_killedCTCount++;
	}

	public int getKilledCtCount()
	{
		return _killedCTCount;
	}

	public void sendTrapStatus(@SuppressWarnings("unused") L2Player player, @SuppressWarnings("unused") boolean enter)
	{}

	public Calendar getSiegeRegistrationEndDate()
	{
		return _siegeRegistrationEndDate;
	}

	public Calendar getSiegeRegistrationStartDate()
	{
		return _siegeRegistrationStartDate;
	}

	public int getSiegeClanMinLevel()
	{
		return _siegeClanMinLevel;
	}

	public void setSiegeClanMinLevel(int siegeClanMinLevel)
	{
		_siegeClanMinLevel = siegeClanMinLevel;
	}

	public int getSiegeLength()
	{
		return _siegeLength;
	}

	public void setSiegeLength(int siegeLength)
	{
		_siegeLength = siegeLength;
	}

	public void setNextSiegePeriod(int nextSiegePeriod)
	{
		_nextSiegePeriod = nextSiegePeriod;
	}

	public int getNextSiegePeriod()
	{
		return _nextSiegePeriod;
	}

	public int getDefenderRespawnDelay()
	{
		return _defenderRespawnDelay;
	}

	public void setDefenderRespawnDelay(int respawnDelay)
	{
		_defenderRespawnDelay = respawnDelay;
	}

	public void spawnMessenger()
	{}

	public Calendar getChangeTimeEnd()
	{
		return _changeTimeEnd;
	}

	public boolean isChangeTimeOver()
	{
		return _isChangeTimeOver;
	}

	public void setChangeTimeOver(boolean changeTimeOver)
	{
		_isChangeTimeOver = changeTimeOver;
	}

	public void updateSiegeTime()
	{}

	public int getRegestrationTime()
	{
		return 0;
	}

	/**
	 * ONLY FOR ClanHall RolePlay Siege
	 */
	public void addMember(L2Player player, boolean save)
	{
	}

	/**
	 * ONLY FOR ClanHall RolePlay Siege
	 */
	public void setNpc(int clanId, int npcVar, boolean save)
	{
	}

	public void setFamePoints(int points)
	{
		_famePoints = points;
	}

	public int getFamePoints()
	{
		return _famePoints;
	}

	protected void startFameTask()
	{
		if(_fameTask != null)
			_fameTask.cancel(true);

		_fameTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new SiegeFameTask(this), 5 * 60000, 60000);
	}

	protected void stopFameTask()
	{
		if(_fameTask != null)
		{
			_fameTask.cancel(true);
			_fameTask = null;
		}
	}

	public abstract boolean includeFlags();

	/**
	 * Only Fort
	 */
	public void unspawnCommanders()
	{
		for(L2Spawn spawn : _commanders)
			if(spawn != null)
			{
				spawn.stopRespawn();
				spawn.despawnAll();
			}

		if(_peaceNpc.isEmpty())
		{
			for(SiegeSpawn sp : _siegeUnit.getPeaceNpcList())
			{
				try
				{
					L2Spawn spawn = new L2Spawn(NpcTable.getTemplate(sp.getNpcId()));
					spawn.setLoc(sp.getLoc());
					spawn.setRespawnDelay(60);
					spawn.setAmount(1);
					_peaceNpc.add(spawn);
				}
				catch(ClassNotFoundException e)
				{
				}
			}
		}

		for(L2Spawn spawn : _peaceNpc)
		{
			spawn.startRespawn();
			spawn.doSpawn(true);
		}
	}

	/**
	 * Only Fort
	 */
	public void spawnMerchant()
	{
		if(getSiegeDate().getTimeInMillis() + getNextSiegePeriod() < System.currentTimeMillis())
		{
			if(_siegeUnit.getMerchantSpawn() != null)
			{
				_log.info("Last siege of " + _siegeUnit.getName() + " " + new Date(getSiegeDate().getTimeInMillis()));
				_siegeUnit.getMerchantSpawn().startRespawn();
				_siegeUnit.getMerchantSpawn().doSpawn(true);
			}
		}
		else
		{
			if(_siegeUnit.getMerchantSpawn() != null)
			{
				_log.info("Merchant spawn of " + _siegeUnit.getName() + " " + new Date(getSiegeDate().getTimeInMillis() + getNextSiegePeriod()));
				_spawnMerchantTask = ThreadPoolManager.getInstance().scheduleGeneral(new SpawnMerchantTask(), (getSiegeDate().getTimeInMillis() + getNextSiegePeriod()) - System.currentTimeMillis());
			}
		}
	}

	/**
	 * Only Fort
	 */
	public class SpawnMerchantTask implements Runnable
	{
		public void run()
		{
			if(_siegeUnit.getMerchantSpawn() != null)
			{
				_log.info("FortressSiege: spawn merchant for: " + _siegeUnit);
				_siegeUnit.getMerchantSpawn().startRespawn();
				_siegeUnit.getMerchantSpawn().doSpawn(true);
			}
			_spawnMerchantTask = null;
		}
	}

	/**
	 * Only Fort
	 */
	/** Один из командиров убит */
	public void killedCommander()
	{
		announceToDefenders(Msg.THE_BARRACKS_HAVE_BEEN_SEIZED);
		announceToAttackers(Msg.THE_BARRACKS_HAVE_BEEN_SEIZED);

		for(L2Spawn spawn : _commanders)
			if(!spawn.getLastSpawn().isDead())
				return;

		if(_siegeUnit.getMainControllers().size() > 0 && _siegeUnit.getPowerOnTime() < System.currentTimeMillis())
			return;

		for(L2Spawn spawn : _commanders)
			spawn.stopRespawn();

		if(_siegeUnit.getMainControllers().size() > 0)
			_siegeUnit.setPowerOnTime(_siegeEndDate.getTimeInMillis());

		spawnFlags(); // Spawn flags
		operateCommandCenterDoors(true);
		announceToDefenders(Msg.ALL_BARRACKS_ARE_OCCUPIED);
		announceToAttackers(Msg.ALL_BARRACKS_ARE_OCCUPIED);
	}

	/**
	 * Only Fort
	 */
	private void spawnFlags()
	{
		for(CombatFlag cf : _siegeUnit.getFlagList())
			if(cf != null)
				cf.spawnMe();
	}

	/**
	 * Only Fort
	 */
	public void operateCommandCenterDoors(boolean open)
	{
		for(Integer doorId : _siegeUnit.getCommandCenterDoors())
		{
			L2DoorInstance door = getSiegeUnit().getDoor(doorId);
			if(door != null)
				if(open)
					door.openMe();
				else
					door.closeMe();
		}
	}

	/**
	 * Only Fort
	 */
	public void updateRemovedClan(int clanId)
	{
		for(L2Player member : ClanTable.getInstance().getClan(clanId).getOnlineMembers(""))
			if(member != null)
			{
				if(member.isCastingNow() && member.getTarget() instanceof L2StaticObjectInstance)
					member.abortCast();

				if(member.isCombatFlagEquipped())
					FortressSiegeManager.getInstance().dropCombatFlag(member);

				member.setSiegeState(0);
				member.setSiegeId(0);
				member.sendUserInfo(true);
			}

		L2Clan clan;
		for(SiegeClan siegeclan : getAttackerClans().values())
		{
			 clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			 for (L2Player member : clan.getOnlineMembers(""))
			 {
				 for(L2Player pl : L2World.getAroundPlayers(member))
					 member.sendRelation(pl);
			 }
		}

		for(SiegeClan siegeclan : getDefenderClans().values())
		{
			clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			for (L2Player member : clan.getOnlineMembers(""))
			{
				for(L2Player pl : L2World.getAroundPlayers(member))
					member.sendRelation(pl);
			}
		}
	}


	public void removeSiegeClan(int clanId)
	{
		if(clanId <= 0  || clanId == getSiegeUnit().getOwnerId() || !SiegeDatabase.checkIsRegistered(clanId, this))
			return;
		attackerClans.remove(clanId);
		SiegeDatabase.removeSiegeClan(clanId, this);
	}

	public void setRegistrationTime(int min)
	{
		_registrationTime = min;
	}

	public void setCountdownTime(int min)
	{
		_countdownTime = min;
	}

	public int getCountdownTime()
	{
		return _countdownTime;
	}

	public void setCRPWin(int crp)
	{
		_crpWin = crp;
	}

	/**
	 * Only Fort
	 */
	public int getBarrackStateById(int barrackId)
	{
		if(_commanders.size() < 1)
			return 0;

		if(barrackId == 4)
			return _siegeUnit.getPowerOnTime() < System.currentTimeMillis() ? 0 : 1;

		for(L2Spawn spawn : _commanders)
			if(spawn.getLastSpawn() != null && spawn.getLastSpawn().getAIParams() != null && spawn.getLastSpawn().getAIParams().getInteger("barrack_id", 0) == barrackId)
				return spawn.getLastSpawn().isDead() ? 1 : 0;

		return 0;
	}

	public void addFlagPole(L2StaticObjectInstance art)
	{
		_flagPoles.add(art);
	}

	public void addControlTower(L2ControlTowerInstance tower)
	{}

	public void addArtifact(L2ArtefactInstance art)
	{}

	public void announceToAttackers(L2GameServerPacket sp)
	{
		for(SiegeClan clan : getAttackerClans().values())
		{
			for(L2Player member : clan.getClan().getOnlineMembers(""))
				if(member != null)
					member.sendPacket(sp);
		}
	}

	public void announceToDefenders(L2GameServerPacket sp)
	{
		for(SiegeClan clan : getDefenderClans().values())
		{
			for(L2Player member : clan.getClan().getOnlineMembers(""))
				if(member != null)
					member.sendPacket(sp);
		}
	}


	public ConcurrentHashMap<Integer, SiegeClan> getDefenderClans()
	{
		return defenderClans;
	}

	public ConcurrentHashMap<Integer, SiegeClan> getDefenderWaitingClans()
	{
		return defenderWaitingClans;
	}

	public ConcurrentHashMap<Integer, SiegeClan> getAttackerClans()
	{
		return attackerClans;
	}

	public boolean isTempAllyActive()
	{
		return _tempAlly;
	}
}