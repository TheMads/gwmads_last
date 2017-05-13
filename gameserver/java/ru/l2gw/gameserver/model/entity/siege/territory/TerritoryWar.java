package ru.l2gw.gameserver.model.entity.siege.territory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Announcements;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.QuestManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2World;
import ru.l2gw.gameserver.model.entity.SevenSigns;
import ru.l2gw.gameserver.model.entity.Territory;
import ru.l2gw.gameserver.model.entity.siege.territory.TerritoryWarTasks.DisableWarFunctions;
import ru.l2gw.gameserver.model.entity.siege.territory.TerritoryWarTasks.TerritoryWarEndTask;
import ru.l2gw.gameserver.model.entity.siege.territory.TerritoryWarTasks.TerritoryWarStartTask;
import ru.l2gw.gameserver.model.entity.siege.territory.TerritoryWarTasks.WarFameTask;
import ru.l2gw.gameserver.model.instances.L2TerritoryWardInstance;
import ru.l2gw.gameserver.model.quest.Quest;
import ru.l2gw.gameserver.model.quest.QuestState;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.ExDominionWarEnd;
import ru.l2gw.gameserver.serverpackets.ExDominionWarStart;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;
import ru.l2gw.gameserver.serverpackets.SkillList;
import ru.l2gw.gameserver.tables.ClanTable;
import ru.l2gw.gameserver.tables.SpawnTable;
import ru.l2gw.commons.arrays.GArray;

import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;

/**
 * @author rage
 * @date 06.07.2010 11:38:18
 */
public class TerritoryWar
{
	private static final Log _log = LogFactory.getLog("territory");

	private final Calendar _warDate = Calendar.getInstance();
	private ScheduledFuture<TerritoryWarStartTask> _startTask;
	private ScheduledFuture<TerritoryWarEndTask> _endTask;
	private ScheduledFuture<DisableWarFunctions> _disableTask;
	private ScheduledFuture<WarFameTask> _fameTask;
	private boolean _functionsActive = false;
	private boolean _inProgress = false;
	private GArray<L2TerritoryWardInstance> _wards = new GArray<L2TerritoryWardInstance>(9);
	private long _warEndDate;

	public static final String[] _questNames = {
			"_729_ProtectTheTerritoryCatapult",
			"_717_ForTheSakeOfTheTerritoryGludio",
			"_718_ForTheSakeOfTheTerritoryDion",
			"_719_ForTheSakeOfTheTerritoryGiran",
			"_720_ForTheSakeOfTheTerritoryOren",
			"_721_ForTheSakeOfTheTerritoryAden",
			"_722_ForTheSakeOfTheTerritoryInnadril",
			"_723_ForTheSakeOfTheTerritoryGoddard",
			"_724_ForTheSakeOfTheTerritoryRune",
			"_725_ForTheSakeOfTheTerritorySchuttgart",
			"_730_ProtectTheSuppliesSafe",
			"_731_ProtectTheMilitaryAssociationLeader",
			"_732_ProtectTheReligiousAssociationLeader",
			"_733_ProtectTheEconomicAssociationLeader",
			"_734_PierceThroughAShield",
			"_735_MakeSpearsDull",
			"_736_WeakenMagic",
			"_737_DenyBlessings",
			"_738_DestroyKeyTargets",
			"TerritoryWarQuest"
	};

	public void correctWarDate()
	{
		_warDate.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
		_warDate.set(Calendar.HOUR_OF_DAY, 20);
		_warDate.set(Calendar.MINUTE, 0);
		_warDate.set(Calendar.SECOND, 0);
		_warDate.set(Calendar.MILLISECOND, 0);

		while(_warDate.getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
		{
			// Set next siege date if siege has passed
			if(_warDate.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY)
				_warDate.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
			// set the next siege day to the next weekend
			_warDate.add(Calendar.DAY_OF_MONTH, 7);
		}

		if(!SevenSigns.getInstance().isDateInSealValidPeriod(_warDate))
			_warDate.add(Calendar.DAY_OF_MONTH, 7);

		_log.info("TerritoryWarManager: Next territory war " + _warDate.getTime());
	}

	public void startAutoTask()
	{
		long delay = (_warDate.getTimeInMillis() - System.currentTimeMillis()) / 1000;
		if(delay > 2 * 60 * 60)
		{
			_log.info("TerritoryWarManager: start task delay: " + 2 * 60 * 60);
			_startTask = ThreadPoolManager.getInstance().scheduleGeneral(new TerritoryWarStartTask(this, 2 * 60 * 60), _warDate.getTimeInMillis() - System.currentTimeMillis() - 2 * 60 * 60000);
		}
		else if(delay > 20 * 60)
		{
			_log.info("TerritoryWarManager: start task delay: " + 100 * 60);
			_startTask = ThreadPoolManager.getInstance().scheduleGeneral(new TerritoryWarStartTask(this, 100 * 60), _warDate.getTimeInMillis() - System.currentTimeMillis() - 20 * 60000);
		}
		else if(delay > 10 * 60)
		{
			_log.info("TerritoryWarManager: start task delay: " + 10 * 60);
			_functionsActive = true;
			_startTask = ThreadPoolManager.getInstance().scheduleGeneral(new TerritoryWarStartTask(this, 10 * 60), _warDate.getTimeInMillis() - System.currentTimeMillis() - 10 * 60000);
		}
		else if(delay > 5 * 60)
		{
			_log.info("TerritoryWarManager: start task delay: " + 5 * 60);
			_functionsActive = true;
			_startTask = ThreadPoolManager.getInstance().scheduleGeneral(new TerritoryWarStartTask(this, 5 * 60), _warDate.getTimeInMillis() - System.currentTimeMillis() - 5 * 60000);
		}
		else if(delay > 60)
		{
			_log.info("TerritoryWarManager: start task delay: " + 4 * 60);
			_functionsActive = true;
			_startTask = ThreadPoolManager.getInstance().scheduleGeneral(new TerritoryWarStartTask(this, 4 * 60), _warDate.getTimeInMillis() - System.currentTimeMillis() - 60000);
		}
		else
		{
			_log.info("TerritoryWarManager: start task delay: 0");
			_functionsActive = true;
			_startTask = ThreadPoolManager.getInstance().scheduleGeneral(new TerritoryWarStartTask(this, 0), _warDate.getTimeInMillis() - System.currentTimeMillis());
		}
	}

	public Calendar getWarDate()
	{
		return _warDate;
	}

	public boolean isInProgress()
	{
		return _inProgress;
	}

	public boolean isRegistrationOver()
	{
		return System.currentTimeMillis() > _warDate.getTimeInMillis() - 2 * 60 * 60000;
	}

	public void setFunctionsActive(boolean functionsActive)
	{
		_functionsActive = functionsActive;
	}

	public boolean isFunctionsActive()
	{
		return _functionsActive;
	}

	public void setStartTask(ScheduledFuture<TerritoryWarStartTask> startTask)
	{
		_startTask = startTask;
	}

	public void setEndTask(ScheduledFuture<TerritoryWarEndTask> endTask)
	{
		_endTask = endTask;
	}

	public void stopStartTask()
	{
		if(_startTask != null)
			_startTask.cancel(true);
		_startTask = null;
	}

	public void stopEndTask()
	{
		if(_endTask != null)
			_endTask.cancel(true);
		_endTask = null;
	}

	private void startFameTask()
	{
		if(_fameTask != null)
			_fameTask.cancel(true);

		_fameTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new WarFameTask(), 5 * 60000, 60000);
	}

	private void stopFameTask()
	{
		if(_fameTask != null)
		{
			_fameTask.cancel(true);
			_fameTask = null;
		}
	}

	public void startWar()
	{
	 	_log.info("TerritoryWar: started.");
		broadcastToPlayers(Msg.TERRITORY_WAR_HAS_BEGUN);
		_inProgress = true;
		_functionsActive = true;
		_warEndDate = System.currentTimeMillis() + 2 * 60 * 60000L;
		_endTask = ThreadPoolManager.getInstance().scheduleGeneral(new TerritoryWarEndTask(this, 60 * 60), 60 * 60000);
		if(_disableTask != null)
		{
			_disableTask.cancel(true);
			_disableTask = null;
		}
		startFameTask();
		for(Territory terr : TerritoryWarManager.getTerritories())
			if(terr.getOwner() != null)
			{
				terr.spawnWard();
				terr.getCastle().spawnDoor();
				terr.getCastle().getSiegeZone().setActive(true);
				terr.getCastle().getRezidentZone().setActive(true);
				terr.getCastle().getHQZone().setActive(true);
				terr.getFort().spawnDoor();
				terr.getFort().getSiegeZone().setActive(true);
				terr.getFort().getRezidentZone().setActive(true);
				terr.getFort().getHQZone().setActive(true);
				terr.setCatapultState(false);
				terr.setLeadersState(false);
				terr.setSuppliesState(false);
				teleportFromZone(terr.getCastle().getSiegeZone());
				teleportFromZone(terr.getFort().getSiegeZone());
				SpawnTable.getInstance().startEventSpawn("territory_c_" + terr.getId());
				SpawnTable.getInstance().startEventSpawn("territory_" + terr.getId());
				Quest.giveQuestForTerritory(terr.getId(), _questNames[0], null);
				Quest.giveQuestForTerritory(terr.getId(), _questNames[terr.getId() - 80], null);
				Quest.giveQuestForTerritory(terr.getId(), _questNames[_questNames.length - 1], null);
			}

		updatePlayers(true);
		for(L2Player player : L2ObjectsStorage.getAllPlayers())
			player.sendPacket(new ExDominionWarStart(player));
	}

	public void endWar()
	{
		_log.info("TerritoryWar: ended.");
		broadcastToPlayers(Msg.TERRITORY_WAR_HAS_ENDED);
		_inProgress = false;
		stopFameTask();
		removeWards();
		updatePlayers(false);
		for(Territory terr : TerritoryWarManager.getTerritories())
			if(terr.getOwner() != null)
			{
				terr.getCastle().spawnDoor();
				terr.getCastle().getSiegeZone().setActive(false);
				terr.getCastle().getRezidentZone().setActive(false);
				terr.getCastle().getHQZone().setActive(false);
				terr.getFort().spawnDoor();
				terr.getFort().getSiegeZone().setActive(false);
				terr.getFort().getRezidentZone().setActive(false);
				terr.getFort().getHQZone().setActive(false);
				TerritoryWarManager.clearRegistration(terr.getId());
				SpawnTable.getInstance().stopEventSpawn("territory_c_" + terr.getId(), true);
				SpawnTable.getInstance().stopEventSpawn("territory_d_" + terr.getId(), true);
				SpawnTable.getInstance().stopEventSpawn("territory_" + terr.getId(), true);
			}

		TerritoryWarManager.removeCamps();
		correctWarDate();
		startAutoTask();
		_disableTask = ThreadPoolManager.getInstance().scheduleGeneral(new DisableWarFunctions(), 600000);
		Announcements.getInstance().announceToAll(new ExDominionWarEnd());
	}

	private void updatePlayers(boolean start)
	{
		GArray<L2Player> players = new GArray<L2Player>();
		int m = 0, c = 0;
		for(Territory terr : TerritoryWarManager.getTerritories())
			if(terr.getOwner() != null)
			{
				GArray<Integer> mercs = TerritoryWarManager.getRegisteredMerc(terr.getId());
				for(int objectId : mercs)
				{
					L2Player player = L2ObjectsStorage.getPlayer(objectId);
					if(player != null)
					{
						player.setTerritoryId(terr.getId());
						m++;
						players.add(player);
					}
				}

				GArray<Integer> clans = TerritoryWarManager.getRegisteredClans(terr.getId());
				for(int clanId : clans)
				{
					L2Clan clan = ClanTable.getInstance().getClan(clanId);
					if(clan == null)
						continue;
					c++;
					players.addAll(clan.getOnlineMembers(""));
				}
			}

		_log.info("TerritoryWar: updated " + m + " mercenaries and " + c + " clans.");

		for(L2Player player : players)
			if(!start)
			{
				player.setSiegeState(0);
				if(player.getTerritoryId() > 0)
					notifyQuests("warEnd", player);
				for(int i = 717; i < 739; i++)
					player.unsetVar("twq_" + i);
			}
			else
				player.setSiegeState(3);

		for(L2Player player : players)
		{
			if(player.getClanId() > 0 && player.getClan().getHasCastle() > 0)
			{
				if(start)
				{
					player.sendPacket(Msg.THE_EFFECT_OF_TERRITORY_WARD_IS_DISAPPEARING);
					TerritoryWarManager.getTerritoryById(player.getClan().getHasCastle() + 80).removeSkills(player);
					player.sendPacket(new SkillList(player));
				}
				else
				{
					TerritoryWarManager.getTerritoryById(player.getClan().getHasCastle() + 80).giveSkills(player);
					player.sendPacket(new SkillList(player));
				}
			}

			player.broadcastUserInfo(true);
			for(L2Player pl : L2World.getAroundPlayers(player))
				player.sendRelation(pl);
		}
	}

	public void deactivateFunctions()
	{
		_log.info("TerritoryWarManager: disable war functions.");
		Announcements.getInstance().announceToAll(Msg.THE_TERRITORY_WAR_CHANNEL_AND_FUNCTIONS_WILL_NOW_BE_DEACTIVATED);
		_functionsActive = false;
		_disableTask = null;
		for(L2Clan clan : ClanTable.getInstance().getClans())
			if(clan.getHasCastle() == 0 && clan.getTerritoryId() > 0 && TerritoryWarManager.getClanRegisteredTerritoryId(clan.getClanId()) == 0)
				clan.setTerritoryId(0);
		for(L2Player player : L2ObjectsStorage.getAllPlayers())
		{
			if(player.getClanId() == 0 && player.getTerritoryId() > 0 && TerritoryWarManager.getMercRegisteredTerritoryId(player.getObjectId()) == 0)
				player.setTerritoryId(0);
			if(player.getVarInt("disguised") > 0)
			{
				player.unsetVar("disguised");
				player.broadcastUserInfo(true);
			}
		}
	}

	public static void broadcastToPlayers(L2GameServerPacket gsp)
	{
		for(L2Player player : L2ObjectsStorage.getAllPlayers())
			if(player != null)
				player.sendPacket(gsp);
	}

	private static void teleportFromZone(L2Zone zone)
	{
		for(L2Player player : zone.getPlayers())
			if(player.getTerritoryId() == 0)
				player.teleToClosestTown();
	}

	public void addSpawnedWard(L2TerritoryWardInstance ward)
	{
		if(!_wards.contains(ward))
		{
			_log.info("TerritoryWar: spawn " + ward);
			_wards.add(ward);
		}
	}

	public void removeSpawnedWard(L2TerritoryWardInstance ward)
	{
		_wards.remove(ward);
	}

	public GArray<L2TerritoryWardInstance> getWards()
	{
		return _wards;
	}

	public void removeWards()
	{
		_log.info("TerritoryWar: end remove wards");
		for(L2TerritoryWardInstance ward : _wards)
		{
			L2Player owner = ward.getPlayer();
			_log.info("TerritoryWar: ward: " + ward.getTerritoryId() + " " + owner + " itemId: " + (ward.getTerritoryId() + 13479));
			SpawnTable.getInstance().stopEventSpawn("territory_ward_" + ward.getTerritoryId() + "_" + ward.getCurrentTerritoryId(), true);
			if(owner != null)
			{
				owner.destroyItemByItemId("TWEnd", ward.getTerritoryId() + 13479, 1, ward, true);
				owner.setCombatFlagEquipped(false);
			}
		}
		_wards.clear();
	}

	public L2TerritoryWardInstance getWardByTerritoryId(int terrId)
	{
		for(L2TerritoryWardInstance ward : _wards)
			if(ward.getTerritoryId() == terrId)
				return ward;

		return null;
	}

	public long getWardEndDate()
	{
		return _warEndDate;
	}

	public static void notifyQuests(String event, L2Player player)
	{
		for(String questName : _questNames)
		{
			QuestState qs = player.getQuestState(questName);
			if(qs != null)
				qs.getQuest().notifyEvent(event, qs);
		}
	}

	public static void checkQuestStates(L2Player player)
	{
		boolean tw = TerritoryWarManager.getWar().isInProgress();
		for(String questName : _questNames)
		{
			QuestState qs = player.getQuestState(questName);
			if(tw)
			{
				if(qs != null && !player.getVarB("twq_" + qs.getQuest().getQuestIntId()))
					qs.exitCurrentQuest(true);
			}
			else if(qs != null)
				qs.exitCurrentQuest(true);
		}

		if(!tw && player.getVarFloat("tw_badges") < 0)
			player.setVar("tw_badges", 0);

		if(tw && player.getTerritoryId() > 0)
		{
			QuestState qs = player.getQuestState(_questNames[0]);
			if(qs == null && !TerritoryWarManager.getTerritoryById(player.getTerritoryId()).isCatapultKilled() && !player.getVarB("twq_729"))
			{
				Quest q = QuestManager.getQuest(_questNames[0]);
				if(q != null)
				{
					qs = q.newQuestState(player);
					qs.set("cond", "1");
					qs.setState(Quest.STARTED);
				}
			}

			qs = player.getQuestState(_questNames[player.getTerritoryId() - 80]);
			if(qs == null && !player.getVarB("twq_" + (636 + player.getTerritoryId())))
			{
				Quest q = QuestManager.getQuest(_questNames[player.getTerritoryId() - 80]);
				if(q != null)
				{
					qs = q.newQuestState(player);
					qs.set("cond", "1");
					qs.setState(Quest.STARTED);
				}
			}

			qs = player.getQuestState(_questNames[_questNames.length - 1]);
			if(qs == null)
			{
				Quest q = QuestManager.getQuest(_questNames[_questNames.length - 1]);
				if(q != null)
				{
					qs = q.newQuestState(player);
					qs.set("cond", "1");
					qs.setState(Quest.STARTED);
				}
			}
		}
	}
}
