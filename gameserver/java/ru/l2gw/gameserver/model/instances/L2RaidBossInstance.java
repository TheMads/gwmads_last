package ru.l2gw.gameserver.model.instances;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.RaidBossSpawnManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2CommandChannel;
import ru.l2gw.gameserver.model.L2Playable;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.ExMultiPartyCommandChannelInfo;
import ru.l2gw.gameserver.serverpackets.ExShowScreenMessage;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.lang.ref.WeakReference;
import java.util.concurrent.ScheduledFuture;

/**
 * This class manages all RaidBoss.
 * In a group mob, there are one master called RaidBoss and several slaves called Minions.
 */
public class L2RaidBossInstance extends L2MonsterInstance
{
	protected static Log _log = LogFactory.getLog(L2RaidBossInstance.class.getName());
	final static Log _logBoss = LogFactory.getLog("boss");

	private static final int RAIDBOSS_MAINTENANCE_INTERVAL = 5000;
	private static final int MINION_UNSPAWN_INTERVAL = 5000; //time to unspawn minions when boss is dead, msec

	private RaidBossSpawnManager.StatusEnum _raidStatus;
	private int _minCCMemebers;
	private WeakReference<L2CommandChannel> _channel;
	private long _lastAttacked;
	private long _lastCCMessage = 0;
	private ScheduledFuture<?> _lootingCheckTask;

	/**
	 * Constructor of L2RaidBossInstance (use L2Character and L2NpcInstance constructor).<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Call the L2Character constructor to set the _template of the L2RaidBossInstance (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR) </li>
	 * <li>Set the name of the L2RaidBossInstance</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it </li><BR><BR>
	 *
	 * @param objectId Identifier of the object to initialized
	 * @param template Template to apply to the NPC
	 */
	public L2RaidBossInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
		_minCCMemebers = getAIParams() != null ? getAIParams().getInteger("looting_rights", 18) : 18;
		_channel = null;
	}

	@Override
	protected int getMaintenanceInterval()
	{
		return RAIDBOSS_MAINTENANCE_INTERVAL;
	}

	protected int getMinionUnspawnInterval()
	{
		return MINION_UNSPAWN_INTERVAL;
	}

	@Override
	public void decreaseHp(double i, L2Character attacker, boolean directHp, boolean reflect)
	{
		if(attacker == null || attacker.getPlayer() == null || (attacker == this) && !attacker.getPlayer().isGM())
			return;

		if(attacker.getPlayer() != null)
		{
			_logBoss.info(attacker + "(" + attacker.getObjectId() + ") "
					+ attacker.getLevel() + " lvl did damage " + i + " at ("
					+ attacker.getX() + "," + attacker.getY() + "," + attacker.getZ() + ") to Boss " + getName() + " at (" + getX() + "," + getY() + "," + getZ() + ")");
		}

		_lastAttacked = System.currentTimeMillis();

		if(attacker.getPlayer().getParty() != null && attacker.getPlayer().getParty().isInCommandChannel() && attacker.getPlayer().getParty().getCommandChannel().getMemberCount() >= _minCCMemebers)
		{
			if(_channel == null || _channel.get() == null)
			{
				_channel = new WeakReference<L2CommandChannel>(attacker.getPlayer().getParty().getCommandChannel());
				getCommandChannel().setLootingRights(true);
				getCommandChannel().broadcastToChannelMembers(new SystemMessage(SystemMessage.S1_HAS_GRANTED_THE_CHANNELS_MASTER_PARTY_THE_PRIVILEGE_OF_ITEM_LOOTING).addCharName(attacker.getPlayer().getParty().getPartyLeader()));
				getCommandChannel().broadcastToChannelMembers(new ExMultiPartyCommandChannelInfo(getCommandChannel()));
				_lootingCheckTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new LootingRightsCheck(), 60000, 60000);

				for(L2Player playerAround : getAroundPlayers(1500))
					if(playerAround != null)
						playerAround.sendPacket(new ExShowScreenMessage(new CustomMessage("lootingRightsOn", Config.DEFAULT_LANG).addString(attacker.getPlayer().getParty().getCommandChannel().getChannelLeader().getName()).toString(), 15000, ExShowScreenMessage.ScreenMessageAlign.TOP_CENTER, true, true));
			}
			else if(getCommandChannel() != attacker.getPlayer().getParty().getCommandChannel() && _lastCCMessage < System.currentTimeMillis())
			{
				_lastCCMessage = System.currentTimeMillis() + 300000;
				attacker.getPlayer().getParty().getCommandChannel().broadcastToChannelMembers(new SystemMessage(SystemMessage.A_COMMAND_CHANNEL_WITH_THE_ITEM_LOOTING_PRIVILEGE_ALREADY_EXISTS));
			}
		}

		super.decreaseHp(i, attacker, directHp, reflect);
	}

	@Override
	public void doDie(L2Character killer)
	{
		if(killer != null && killer instanceof L2Playable)
		{
			for(L2Player playerAround : getAroundPlayers(1500))
			{
				if(playerAround != null)
				{
					playerAround.sendPacket(Msg.CONGRATULATIONS_YOUR_RAID_WAS_SUCCESSFUL);
					if(playerAround.isHero())
						playerAround.updateHeroHistory("&@" + getNpcId() + "; " + new CustomMessage("HeroRaidKill", Config.DEFAULT_LANG).toString());
				}
			}
		}

		if(!(getAIParams() != null && getAIParams().getBool("no_rb_spawn_update", false)))
			RaidBossSpawnManager.getInstance().updateStatus(this, true);

		super.doDie(killer);
	}

	@Override
	public void onSpawn()
	{
		RaidBossSpawnManager.getInstance().updateStatus(this, false);
		super.onSpawn();

		if(getSpawn() != null)
			getSpawn().stopRespawn();
	}

	public void setRaidStatus(RaidBossSpawnManager.StatusEnum status)
	{
		_raidStatus = status;
	}

	public RaidBossSpawnManager.StatusEnum getRaidStatus()
	{
		return _raidStatus;
	}

	@Override
	public boolean isFearImmune()
	{
		return true;
	}

	@Override
	public boolean isLethalImmune()
	{
		return true;
	}

	@Override
	public boolean hasRandomWalk()
	{
		return false;
	}

	public L2CommandChannel getCommandChannel()
	{
		if(_channel == null)
			return null;

		return _channel.get();
	}

	public L2Player getLootOwner()
	{
		return getCommandChannel() != null ? getCommandChannel().getChannelLeader() : null;
	}

	private class LootingRightsCheck implements Runnable
	{
		public void run()
		{
			if(_lastAttacked + 300000 < System.currentTimeMillis())
			{
				L2CommandChannel channel = getCommandChannel();

				if(channel != null && channel.getChannelLeader() != null)
				{
					channel.setLootingRights(false);
					channel.broadcastToChannelMembers(new ExMultiPartyCommandChannelInfo(channel));
				}

				_channel = null;
				_lastCCMessage = 0;

				for(L2Player playerAround : getAroundPlayers(1500))
					if(playerAround != null)
						playerAround.sendPacket(new ExShowScreenMessage(new CustomMessage("lootingRightsOff", Config.DEFAULT_LANG).toString(), 15000, ExShowScreenMessage.ScreenMessageAlign.TOP_CENTER, true, true));

				if(_lootingCheckTask != null)
					_lootingCheckTask.cancel(true);

				_lootingCheckTask = null;
			}
		}
	}
}