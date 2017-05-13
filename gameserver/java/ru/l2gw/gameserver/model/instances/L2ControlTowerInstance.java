package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.Castle;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.entity.siege.reinforce.Reinforce;
import ru.l2gw.gameserver.model.entity.siege.reinforce.TrapReinforce;
import ru.l2gw.gameserver.serverpackets.EventTrigger;
import ru.l2gw.gameserver.serverpackets.MyTargetSelected;
import ru.l2gw.gameserver.serverpackets.StatusUpdate;
import ru.l2gw.gameserver.serverpackets.ValidateLocation;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

public class L2ControlTowerInstance extends L2NpcInstance
{
	private Siege _siege;
	private L2FakeTowerInstance _fakeTower;
	private int _controlTrapId = 0;
	private int _maxHp = 0;

	public L2ControlTowerInstance(int objectId, L2NpcTemplate template, Siege siege)
	{
		super(objectId, template, 0L, 0L, 0L, 0L);
		_siege = siege;
	}

	@Override
	public boolean isAttackable(L2Character attacker, boolean forceUse, boolean sendMessage)
	{
		if(attacker == null)
			return false;
		L2Player player = attacker.getPlayer();
		return player != null && _siege != null && _siege.isInProgress();
	}

	@Override
	public void onForcedAttack(L2Player player, boolean dontMove)
	{
		if(player.setTarget(this))
		{
			player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
			StatusUpdate su = new StatusUpdate(getObjectId());
			su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
			su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
			player.sendPacket(su);
			player.sendPacket(new ValidateLocation(this));
			if(Math.abs(player.getZ() - getZ()) < 200)
				player.getAI().Attack(this, true, dontMove);
			else
				player.sendActionFailed();
		}
	}

	@Override
	public void onAction(L2Player player, boolean dontMove)
	{
		if(this != player.getTarget())
		{
			if(player.setTarget(this))
			{
				player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
				StatusUpdate su = new StatusUpdate(getObjectId());
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
				player.sendPacket(new ValidateLocation(this));
			}
		}
		else
		{
			//player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
			if(Math.abs(player.getZ() - getZ()) < 200)
				player.getAI().Attack(this, false, dontMove);
			else
				player.sendActionFailed();
		}
	}

	/**
	 * Вызывает обработку смерти у вышек.
	 * @param killer убийца
	 */
	@Override
	public void doDie(L2Character killer)
	{
		onDeath();
		super.doDie(killer);
	}

	/**
	 * Спавнит фэйковую вышку на месте умершей
	 */
	@Override
	public void onDecay()
	{
		super.onDecay();
		spawnFakeTower();
	}

	/**
	 * Убирает фэйковую вышку на месте новорожденной
	 */
	@Override
	public void spawnMe()
	{
		if(_siege.isInProgress())
		{
			unSpawnFakeTower();
			if(_controlTrapId > 0)
			{
				Reinforce tr = _siege.getSiegeUnit().getReinforceById(_controlTrapId);
				if(tr != null && tr.getLevel() > 0)
				{
					tr.setActive(true);
					super.spawnMe();
					_siege.getZone().broadcastPacket(new EventTrigger(((TrapReinforce)tr).getEventId(), false));
				}
				else
					spawnFakeTower();
			}
			else
				super.spawnMe();
		}
		else
			spawnFakeTower();
	}

	/**
	 * Обработка умирания вышки
	 */
	public void onDeath()
	{
		if(_controlTrapId > 0)
		{
			Reinforce tr = _siege.getSiegeUnit().getReinforceById(_controlTrapId);
			if(tr != null && tr.getLevel() > 0)
				tr.setActive(false);
		}
		else
			_siege.killedCT();
	}

	/**
	 * Спавнит фэйковую вышку на месте умершей настоящей.
	 * Создается новый инстанс, и привязывается к текущему инстансу.
	 */
	public void spawnFakeTower()
	{
		if(_fakeTower == null)
		{
			L2FakeTowerInstance tower = new L2FakeTowerInstance(IdFactory.getInstance().getNextId(), NpcTable.getTemplate(getFakeTowerNpcId()), _siege);
			tower.setControlTrapId(_controlTrapId);
			tower.spawnMe(getLoc());
			_fakeTower = tower;
		}
		else
		{
			_fakeTower.decayMe();
			_fakeTower.spawnMe();
		}
	}

	/**
	 * Убирает с мира фэйковую вышку которая относится к данному инстансу.
	 * Ссылка на обьект не обнуляется, т.к. он еше будет использован в перспективе
	 */
	public void unSpawnFakeTower()
	{
		if(_fakeTower == null)
			return;

		_fakeTower.decayMe();
	}

	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}

	/**
	 * Осадные вышки должны быть уязвимы во время осады, во время осады включается осадная зона
	 * Вывод - если не в осадной зоне, то неуязвимая
	 * @return уязвимая ли вышка
	 */
	@Override
	public boolean isInvul()
	{
		return _siege == null || !_siege.isInProgress();
	}

	@Override
	public boolean isFearImmune()
	{
		return true;
	}

	/**
	 * Возвращает ID Фэйковой вышки которая спавнится после смерти настоящей.
	 * Для Life Control Tower это 13003
	 * Для Flame Control Tower это 13005
	 * @return Fake Tower NPC ID
	 */
	private int getFakeTowerNpcId()
	{
		return getNpcId() + 1;
	}

	public void setControlTrapId(int controlTrapId)
	{
		_controlTrapId = controlTrapId;
	}

	public int getControlTrapId()
	{
		return _controlTrapId;
	}

	public int getControlEventId()
	{
		if(_controlTrapId > 0)
		{
			Reinforce tr = ((Castle)_siege.getSiegeUnit()).getReinforceById(_controlTrapId);
			if(tr != null && tr.getLevel() > 0)
				return ((TrapReinforce)tr).getEventId();
		}
		return 0;
	}

	public boolean isTrapActive()
	{
		if(_controlTrapId > 0)
		{
			Reinforce tr = ((Castle)_siege.getSiegeUnit()).getReinforceById(_controlTrapId);
			if(tr != null && tr.getLevel() > 0)
				return tr.isActive();
		}
		return false;
	}

	public void setMaxHp(int maxHp)
	{
		_maxHp = maxHp;
	}

	@Override
	public int getMaxHp()
	{
		if(_maxHp > 0)
			return _maxHp;
		return super.getMaxHp();
	}

}