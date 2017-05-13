package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.serverpackets.SetSummonRemainTime;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.taskmanager.DecayTaskManager;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.concurrent.ScheduledFuture;

public class L2SummonInstance extends L2Summon
{
	public final int cyrcle = 5000; // in millis
	private float expPenalty = 0;
	private int itemConsumeIdInTime;
	private int itemConsumeCountInTime;
	private int itemConsumeDelay;
	private int consumeCountdown;

	private ScheduledFuture<?> disappearTask;

	private int aliveTime;
	private int maxTime;

	public L2SummonInstance(int objectId, L2NpcTemplate template, L2Player owner, int lifetime, int consumeid, int consumecount, int consumedelay)
	{
		super(objectId, template, owner);
		setName(template.name);
		aliveTime = maxTime = lifetime;
		itemConsumeIdInTime = consumeid;
		itemConsumeCountInTime = consumecount;
		itemConsumeDelay = consumedelay;
		disappearTask = ThreadPoolManager.getInstance().scheduleGeneral(new Lifetime(this), cyrcle);
		consumeCountdown = itemConsumeDelay;
		restoreSummonEffects();
	}

	@Override
	public final byte getLevel()
	{
		return getTemplate() != null ? getTemplate().level : 0;
	}

	@Override
	public int getSummonType()
	{
		return 1;
	}

	@Override
	public int getCurrentFed()
	{
		return aliveTime;
	}

	@Override
	public int getMaxMeal()
	{
		return maxTime;
	}

	public void setLifeTime(int lifeTime)
	{
		aliveTime = lifeTime;
	}

	public void setExpPenalty(float expPenalty)
	{
		this.expPenalty = expPenalty;
	}

	public float getExpPenalty()
	{
		return expPenalty;
	}

	public void reduceHp(double damage, L2Character attacker, boolean directHp, boolean reflect)
	{
		if(attacker instanceof L2Playable && isInZoneBattle() != attacker.isInZoneBattle())
		{
			attacker.getPlayer().sendPacket(Msg.INVALID_TARGET);
			return;
		}

		super.reduceHp(damage, attacker, directHp, reflect);

		if(getPlayer() == null)
			return;

		SystemMessage sm = new SystemMessage(SystemMessage.THE_SUMMONED_MONSTER_RECEIVED_DAMAGE_OF_S2_CAUSED_BY_S1).addCharName(attacker);
		getPlayer().sendPacket(sm.addNumber((int) damage));
	}

	private class Lifetime implements Runnable
	{
		private final L2SummonInstance summon;

		Lifetime(final L2SummonInstance summon)
		{
			this.summon = summon;
		}

		public void run()
		{
			L2Player owner = getPlayer();
			if(owner == null)
			{
				disappearTask = null;
				unSummon();
				return;
			}

			int usedtime = summon.isInCombat() ? cyrcle : cyrcle / 4;
			aliveTime -= usedtime;

			if(aliveTime <= 0)
			{
				owner.sendPacket(new SystemMessage(SystemMessage.SERVITOR_DISAPPEASR_BECAUSE_THE_SUMMONING_TIME_IS_OVER));
				disappearTask = null;
				unSummon();
				return;
			}

			consumeCountdown -= usedtime;
			if(itemConsumeIdInTime > 0 && itemConsumeCountInTime > 0 && consumeCountdown <= 0)
			{
				L2ItemInstance item = owner.getInventory().getItemByItemId(summon.getItemConsumeIdInTime());
				if(item != null && item.getCount() >= summon.getItemConsumeCountInTime())
				{
					consumeCountdown = itemConsumeDelay;
					L2ItemInstance dest = getPlayer().getInventory().destroyItemByItemId("Consume", summon.getItemConsumeIdInTime(), summon.getItemConsumeCountInTime(), getPlayer(), summon);
					owner.sendPacket(new SystemMessage(SystemMessage.A_SUMMONED_MONSTER_USES_S1).addItemName(dest.getItemId()));
				}
				else
				{
					owner.sendPacket(new SystemMessage(SystemMessage.SINCE_YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_MAINTAIN_THE_SERVITORS_STAY_THE_SERVITOR_WILL_DISAPPEAR));
					summon.unSummon();
				}
			}

			owner.sendPacket(new SetSummonRemainTime(summon));

			disappearTask = ThreadPoolManager.getInstance().scheduleEffect(new Lifetime(summon), cyrcle);
		}
	}

	@Override
	public void doDie(L2Character killer)
	{
		super.doDie(killer);

		if(disappearTask != null)
		{
			disappearTask.cancel(false);
			disappearTask = null;
		}
		storeSummonEffects();
		DecayTaskManager.getInstance().addDecayTask(this);
	}

	public int getItemConsumeIdInTime()
	{
		return itemConsumeIdInTime;
	}

	public int getItemConsumeCountInTime()
	{
		return itemConsumeCountInTime;
	}

	public int getItemConsumeDelay()
	{
		return itemConsumeDelay;
	}

	protected synchronized void stopDisappear()
	{
		if(disappearTask != null)
		{
			disappearTask.cancel(false);
			disappearTask = null;
		}
	}

	@Override
	public synchronized void unSummon()
	{
		stopDisappear();
		storeSummonEffects();
		super.unSummon();
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
				getPlayer().sendPacket(Msg.SUMMONED_MONSTERS_CRITICAL_HIT);

			getPlayer().sendPacket(new SystemMessage(SystemMessage.THE_SUMMONED_MONSTER_GAVE_DAMAGE_OF_S1).addNumber(damage));
		}
	}

	@Override
	public boolean consumeItem(final int itemConsumeId, final int itemCount, boolean sendMessage)
	{
		return getPlayer() != null && getPlayer().destroyItemByItemId("Consume", itemConsumeId, itemCount, null, sendMessage);
	}

	@Override
	public int getPAtk(L2Character target)
	{
		L2Player owner = getPlayer();
		return super.getPAtk(target) + (owner == null ? 0 : (int) (owner.getPAtk(target) * owner.calcStat(Stats.SERVITOR_TRANSFER_PATK, 0, target, null) * 0.01));
	}

	@Override
	public int getPAtkSpd()
	{
		L2Player owner = getPlayer();
		return super.getPAtkSpd() + (owner == null ? 0 : (int) (owner.getPAtkSpd() * owner.calcStat(Stats.SERVITOR_TRANSFER_P_ATK_SPD, 0, null, null) * 0.01));
	}

	@Override
	public int getPDef(L2Character target)
	{
		L2Player owner = getPlayer();
		return super.getPDef(target) + (owner == null ? 0 : (int) (owner.getPDef(target) * owner.calcStat(Stats.SERVITOR_TRANSFER_PDEF, 0, target, null) * 0.01));
	}

	@Override
	public int getMAtk(L2Character target, L2Skill skill)
	{
		L2Player owner = getPlayer();
		return super.getMAtk(target, skill) + (owner == null ? 0 : (int) (owner.getMAtk(target, skill) * owner.calcStat(Stats.SERVITOR_TRANSFER_MATK, 0, target, skill) * 0.01));
	}

	@Override
	public int getMAtkSpd()
	{
		L2Player owner = getPlayer();
		return super.getMAtkSpd() + (owner == null ? 0 : (int) (owner.getMAtkSpd() * owner.calcStat(Stats.SERVITOR_TRANSFER_M_ATK_SPD, 0, null, null) * 0.01));
	}

	@Override
	public int getMDef(L2Character target, L2Skill skill)
	{
		L2Player owner = getPlayer();
		return super.getMDef(target, skill) + (owner == null ? 0 : (int) (owner.getMDef(target, skill) * owner.calcStat(Stats.SERVITOR_TRANSFER_MDEF, 0, target, skill) * 0.01));
	}

	@Override
	public int getMaxHp()
	{
		L2Player owner = getPlayer();
		return super.getMaxHp() + (owner == null ? 0 : (int) (owner.getMaxHp() * owner.calcStat(Stats.SERVITOR_TRANSFER_MAX_HP, 0, null, null) * 0.01));
	}

	@Override
	public int getMaxMp()
	{
		L2Player owner = getPlayer();
		return super.getMaxMp() + (owner == null ? 0 : (int) (owner.getMaxMp() * owner.calcStat(Stats.SERVITOR_TRANSFER_MAX_MP, 0, null, null) * 0.01));
	}

	@Override
	public int getCriticalHit(L2Character target, L2Skill skill)
	{
		L2Player owner = getPlayer();
		return  super.getCriticalHit(target, skill) + (owner == null ? 0 : (int) (owner.getCriticalHit(target, skill) * owner.calcStat(Stats.SERVITOR_TRANSFER_C_RATE, 0, target, skill) * 0.01));
	}

	@Override
	public String toString()
	{
		return "Summon " + getTemplate().name + "(" + getNpcId() + ") owner " + getPlayer();
	}

	@Override
	public void sendPetInfo()
	{
		// do nothing but may be in future ?
	}
}