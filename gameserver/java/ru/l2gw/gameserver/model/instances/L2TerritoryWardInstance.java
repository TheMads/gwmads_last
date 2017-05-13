package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.territory.TerritoryWar;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

/**
 * @author rage
 * @date 09.07.2010 12:42:21
 */
public class L2TerritoryWardInstance extends L2TerritoryGuardInstance
{
	private int _ownerId;
	private int _currentTerritoryId;

	public L2TerritoryWardInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
		hasChatWindow = false;
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_log.info(this + " onSpawn");
		_currentTerritoryId = TerritoryWarManager.getTerritoryByWardId(_territoryId).getId();
		TerritoryWarManager.getWar().addSpawnedWard(this);
		_ownerId = 0;
	}

	@Override
	public boolean isAttackable(L2Character attacker, boolean forceUse, boolean sendMessage)
	{
		return TerritoryWarManager.getWar().isInProgress() && attacker.getPlayer() != null && attacker.getPlayer().getTerritoryId() > 0 && _currentTerritoryId != attacker.getPlayer().getTerritoryId() && attacker.getPlayer().getClan() != null && attacker.getPlayer().getClan().getHasCastle() > 0;
	}

	@Override
	public void doDie(L2Character killer)
	{
		super.doDie(killer);
		L2Player player = killer.getPlayer();
		if(player != null)
		{
			if(player.getItemCountByItemId(_territoryId + 13479) > 0)
			{
				ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
				{
					@Override
					public void run()
					{
						if(TerritoryWarManager.getWar().isInProgress())
							getSpawn().respawnNpc(L2TerritoryWardInstance.this);
					}
				}, 5000);
			}
			else
			{
				player.addItem("TerritoryWard", _territoryId + 13479, 1, this, true);
				player.sendPacket(Msg.YOU_VE_ACQUIRED_THE_WARD_MOVE_QUICKLY_TO_YOUR_FORCES_OUTPOST);
				TerritoryWar.broadcastToPlayers(new SystemMessage(SystemMessage.THE_S1_WARD_HAS_BEEN_DESTROYED_C2_NOW_HAS_THE_TERRITORY_WARD).addHideoutName(_territoryId).addCharName(killer.getPlayer()));
				_ownerId = player.getObjectId();
			}
		}
	}

	@Override
	public void decreaseHp(double damage, L2Character attacker, boolean directHp, boolean reflect)
	{
		if(!isAttackable(attacker, false, false))
			return;

		L2Player player = attacker.getPlayer();
		if(player != null && damage >= getCurrentHp() && player.getItemCountByItemId(_territoryId + 13479) > 0)
			return;

		super.decreaseHp(damage, attacker, directHp, reflect);
	}


	@Override
	public boolean isMovementDisabled()
	{
		return true;
	}

	@Override
	public L2Player getPlayer()
	{
		return L2ObjectsStorage.getPlayer(_ownerId);
	}

	public int getOwnerId()
	{
		return _ownerId;
	}

	public void setOwnerId(int ownerId)
	{
		_ownerId = ownerId;	
	}

	public void setCurrentTerritoryId(int terrId)
	{
		_currentTerritoryId = terrId;
	}

	public int getCurrentTerritoryId()
	{
		return _currentTerritoryId;
	}

	@Override
	public String toString()
	{
		return "Ward[territoryId=" + _territoryId + ";currentTerritory=" + _currentTerritoryId + ";ownerId=" + _ownerId + "]";
	}
}
