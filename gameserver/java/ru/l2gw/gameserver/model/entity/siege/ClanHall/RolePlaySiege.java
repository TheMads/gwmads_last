package ru.l2gw.gameserver.model.entity.siege.ClanHall;

import javolution.util.FastList;
import javolution.util.FastMap;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.ClanHall;
import ru.l2gw.gameserver.model.entity.siege.SiegeClan;
import ru.l2gw.gameserver.model.entity.siege.SiegeEndTask;
import ru.l2gw.gameserver.model.entity.siege.TeleportWhoType;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.MapRegionTable;

import java.util.Calendar;

public class RolePlaySiege extends ClanHallSiege
{

	private FastMap<Integer, Integer> _engrave = new FastMap<Integer, Integer>();
	private boolean shortSiege;

	public RolePlaySiege(ClanHall siegeUnit)
	{
		super(siegeUnit);
		load();
	}

	@Override
	public void startSiege()
	{
		if(!isInProgress())
		{
			if(getAttackerClans().size() <= 0)
			{
				if(getSiegeUnit().getOwnerId() <= 0)
					broadcastToPlayer(new SystemMessage(SystemMessage.THE_SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST).addHideoutName(getSiegeUnit()), false);
				else
					broadcastToPlayer(new SystemMessage(SystemMessage.S1S_SIEGE_WAS_CANCELED_BECAUSE_THERE_WERE_NO_CLANS_THAT_PARTICIPATED).addHideoutName(getSiegeUnit()), false);
				return;
			}

			getZone().setActive(true);

			_ownerBeforeStart = getSiegeUnit().getOwnerId();
			
			setIsInProgress(true); // Flag so that same siege instance cannot be started again

			ClanHallSiegeDatabase.loadSiegeClan(this); // Load siege clan from db
			updatePlayerSiegeStateFlags(false);
			teleportPlayer(TeleportWhoType.Attacker, MapRegionTable.TeleportWhereType.ClanHall);
			getSiegeUnit().spawnDoor();
			teleportPlayer(TeleportWhoType.Owner, MapRegionTable.TeleportWhereType.ClanHall);
			teleportPlayer(TeleportWhoType.Spectator,MapRegionTable.TeleportWhereType.ClosestTown);
			//spawnBosses();//Flags
			//spawnMonsters();//NpcSpawn
			getSiegeUnit().spawnDoor();
			
			attackersUpdate(false); // Add attackers to list
			_defenderRespawnPenalty = 0; // Reset respawn delay

			// Schedule a task to prepare auto siege end
			_siegeEndDate = Calendar.getInstance();
			if(getSiegeUnit().getOwnerId() >= 0)
			{
				setShortSiege(false);
				_siegeEndDate.add(Calendar.MINUTE, getSiegeLength());
			}
			else
			{
				setShortSiege(true);
				_siegeEndDate.add(Calendar.MINUTE, getSiegeLength()/5);
			}

			ThreadPoolManager.getInstance().scheduleGeneral(new SiegeEndTask(this), 1000); // Prepare auto end task

			for(SiegeClan clan : getAttackerClans().values())
				clan.getClan().broadcastToOnlineMembers(new SystemMessage(SystemMessage.THE_SIEGE_OF_THE_CLAN_HALL_HAS_BEGUN));
		}
	}

	/**
	 * For This sieage must be called when enemy npc is die
	 *  whith parameters clan who kill, npcVar who killed
	 */
	@Override
	public void Engrave(L2Clan clan, int npcVar)
	{
		_engrave.put(npcVar, clan.getClanId());
		for(RolePlaySiegeMember rps : getAttakerClans().keySet())
			if(rps.getNpc() == npcVar)
			{
				getAttakerClans().remove(rps);
				getAttackerClans().remove(clan.getClanId());
				teleportPlayer(TeleportWhoType.Spectator,MapRegionTable.TeleportWhereType.ClosestTown);
			}
		if(_engrave.size() > 3)
		{
			if(getSiegeUnit().getOwner() != null && _engrave.size() < 5)
			{
				teleportPlayer(TeleportWhoType.Owner, MapRegionTable.TeleportWhereType.Headquarter);
				spawnForDefenders();
			}
			else
			{
				getSiegeUnit().changeOwner(clan.getClanId());
				endSiege();
			}
		}
	}

	/**
	 * Выбирает свободного нпц и спавнит его для овнеров
	 */
	public void spawnForDefenders()
	{
		/*
		boolean found = false;
		for(int i = 0;i < 5;i++)
		{
			for(RolePlaySiegeMember rps : getAttakerClans().keySet())
				if(rps.getNpc() == i)
					found = true;
			if(!found)
			{
				SiegeSpawn sSp = ClanHallSiegeManager.getBossSpawnList(getSiegeUnit().getId()).get(i);
				try
				{
					L2NpcTemplate template = NpcTable.getTemplate(sSp.getNpcId());
					L2Spawn sp = new L2Spawn(template);
					sp.setLocx(sSp.getLoc().getX());
					sp.setLocy(sSp.getLoc().getY());
					sp.setLocz(sSp.getLoc().getZ());
					sp.setHeading(sSp.getHeading());
					sp.setSiegeId(getSiegeUnit().getId());
					sp.doSpawn(true);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		*/
	}
	@Override
	public void addMember(L2Player player,boolean save)
	{
		if(!getAttakerClans().isEmpty())
			for(RolePlaySiegeMember rps : getAttakerClans().keySet())
				if(rps.getClanId() == player.getClanId())
				{
					getAttakerClans().get(rps).add(player);
					if(save)
						_database.saveSettings(player.getClanId(), player.getObjectId(), rps.getNpc());
				}
	}

	public boolean isCountFull(int clanId)
	{
		return getAttakerClans().get(clanId).size() > 18;
	}

	@Override
	public final void addDefender(final SiegeClan sc)
	{
		getAttakerClans().put(new RolePlaySiegeMember(sc.getClanId(), 0), new FastList<L2Player>());
		getDefenderClans().put(sc.getClanId(), sc);
	}

	@Override
	public void setNpc(int clanId, int npcVar,boolean save)
	{
		if(!getAttakerClans().isEmpty())
			for(RolePlaySiegeMember rps : getAttakerClans().keySet())
				if(rps.getClanId() == clanId)
				{
					rps.setNpc(npcVar);
					FastList<L2Player> tpm = getAttakerClans().get(clanId);
					if(save)
						for(L2Player player : tpm)
							_database.saveSettings(clanId, player.getObjectId(), npcVar);

					getAttakerClans().remove(clanId);
					getAttakerClans().put(new RolePlaySiegeMember(clanId, npcVar), tpm);
				}
	}

	public boolean isNpcTaken(int npcVar)
	{
		for(RolePlaySiegeMember rps : getAttakerClans().keySet())
			if(rps.getNpc() == npcVar)
				return true;

		return false;
	}

	public int getNpc(int clanId)
	{
		for(RolePlaySiegeMember rps : getAttakerClans().keySet())
			if(rps.getClanId() == clanId)
				return rps.getNpc();
		return 0;
	}

	@Override
	public void removeSiegeClan(int clanId)
	{
		if(clanId <= 0 || clanId == getSiegeUnit().getOwnerId())
			return;

		super.removeSiegeClan(clanId);

		for(RolePlaySiegeMember rps : getAttakerClans().keySet())
			if(rps.getClanId() == clanId)
				getAttakerClans().remove(rps);
	}

	public void load()
	{
	}

	public void setShortSiege(boolean shortSiege)
	{
		this.shortSiege = shortSiege;
	}

	public boolean isShortSiege()
	{
		return shortSiege;
	}

	public boolean includeFlags()
	{
		return false;
	}

	@Override
	public final void addAttacker(final SiegeClan sc)
	{
		getAttakerClans().put(new RolePlaySiegeMember(sc.getClanId(), 0), new FastList<L2Player>());
		super.addAttacker(sc);
	}
}
