package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.ai.CtrlEvent;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.L2World;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

public final class L2TrapInstance extends L2NpcInstance
{
	private L2Character owner;
	private long _detectTime = 0;
	private long _lifeTime = 0;
	private boolean _isActive;
	private boolean _worldTrapActive;

	public L2TrapInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
		_isActive = getAIParams() == null || getAIParams().getBool("trap_active", true);
	}

	public static L2TrapInstance createTrap(L2Character owner, int npcId)
	{
		L2TrapInstance trap = new L2TrapInstance(IdFactory.getInstance().getNextId(), NpcTable.getTemplate(npcId), 0L, 0L, 0L, 0L);
		trap.setOwner(owner);
		trap.setTitle(owner.getName());
		return trap;
	}

	public void setLifeTime(int sec)
	{
		_lifeTime = System.currentTimeMillis() + sec * 1000;
	}

	public long getTrapLifeTime()
	{
		return _lifeTime;
	}

	
	/**
	 * Return  player instance owner, if he is a player or  players summon
	 * else return null 
	 */
	@Override
	public L2Player getPlayer()
	{
		return getTrapOwner() != null ? getTrapOwner().getPlayer() : null;
	}

	/**
	 * Return owner instance of L2Character if any
	 */
	public L2Character getTrapOwner()
	{
		return owner;
	}

	public void setOwner(L2Character owner)
	{
		this.owner = owner;
		setReflection(owner.getReflection());
	}

	@Override
	public boolean isAttackable(L2Character attacker, boolean forceUse, boolean sendMessage)
	{
		return !(getTrapOwner() != null && getTrapOwner().isPlayer());
	}

	@Override
	public void doDie(L2Character killer)
	{
		if(getPlayer() != null)
			getPlayer().setLastTrap(null);

		super.doDie(killer);
	}

	@Override
	public boolean isInvul()
	{
		return false;
	}

	@Override
	public void decreaseHp(double i, L2Character attacker, boolean directHp, boolean reflect)
	{
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
	public void showChatWindow(L2Player player, int val)
	{}

	@Override
	public void showChatWindow(L2Player player, String filename)
	{}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{}

	@Override
	public int getPvpFlag()
	{
		return getPlayer() != null ? getPlayer().getPvpFlag() : 0;
	}

	public void setDetected(int sec)
	{
		if(!isDetected())
		{
			_detectTime = System.currentTimeMillis() + sec * 1000;

			for(L2Player player : L2World.getAroundPlayers(this))
				if(getPlayer() == null || getPlayer() != player)
					player.addVisibleObject(this, null);
		}
		else
			_detectTime = System.currentTimeMillis() + sec * 1000;
	}

	public boolean isDetected()
	{
		return _detectTime > System.currentTimeMillis();
	}

	public boolean isActive()
	{
		return _isActive;
	}

	public void setActive(boolean active)
	{
		_isActive = active;
	}

	public void sendPacket(L2GameServerPacket mov)
	{
		if(mov instanceof SystemMessage && getPlayer() != null && ((SystemMessage) mov).getMessageId() != SystemMessage.YOU_USE_S1)
			getPlayer().sendPacket(mov);
	}

	@Override
	public void sendDamageMessage(final L2Character target, final int damage, final boolean miss, final boolean pcrit, final boolean block)
	{
		if(getPlayer() != null)
		{
			if(block)
			{
				getPlayer().sendPacket(Msg.THE_ATTACK_HAS_BEEN_BLOCKED);
				return;
			}
			else if(miss)
			{
				getPlayer().sendPacket(new SystemMessage(SystemMessage.S1S_ATTACK_WENT_ASTRAY).addCharName(this));
				return;
			}

			if(pcrit)
				getPlayer().sendPacket(new SystemMessage(SystemMessage.S1_HAD_A_CRITICAL_HIT).addCharName(this));

			getPlayer().sendPacket(new SystemMessage(SystemMessage.S1_HAS_GIVEN_S2_DAMAGE_OF_S3).addCharName(this).addCharName(target).addNumber(damage));
		}
	}

	@Override
	public L2Skill.TargetType getTargetRelation(L2Character target, boolean offensive)
	{
		if(target == this)
			return L2Skill.TargetType.self;

		if(target.isPlayer() && getPlayer() == target || (getPlayer() != null && getPlayer().getParty() != null && getPlayer().getParty().containsMember(target)))
			return L2Skill.TargetType.target;

		return L2Skill.TargetType.enemy;
	}

	public boolean isWorldTrapActive()
	{
		return _worldTrapActive;
	}

	public void activateWorldTrap()
	{
		_worldTrapActive = true;
		notifyAiEvent(this, CtrlEvent.EVT_TRAP_ACTIVATED, null, null, null);
	}

	public void defuseWorldTrap()
	{
		_worldTrapActive = false;
	}
}