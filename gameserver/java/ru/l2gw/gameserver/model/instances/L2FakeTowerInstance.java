package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.entity.siege.reinforce.Reinforce;
import ru.l2gw.gameserver.model.entity.siege.reinforce.TrapReinforce;
import ru.l2gw.gameserver.model.entity.Castle;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.gameserver.serverpackets.MyTargetSelected;
import ru.l2gw.gameserver.serverpackets.ValidateLocation;

/**
 * @Author: Death
 * @Date: 17/9/2007
 * @Time: 19:11:50
 *
 * Этот инстанс просто для отрисовки умершей вышки на месте оригинальной на осаде
 * Фэйковый инстанс неуязвим.
 * @see ru.l2gw.gameserver.model.instances.L2ControlTowerInstance#spawnMe()
 * @see ru.l2gw.gameserver.model.instances.L2ControlTowerInstance#onDecay()
 */
public class L2FakeTowerInstance extends L2NpcInstance
{
	private Siege _siege;
	private int _controlTrapId = 0;

	public L2FakeTowerInstance(int objectId, L2NpcTemplate template, Siege siege)
	{
		super(objectId, template, 0L, 0L, 0L, 0L);
		_siege = siege;
	}

	/**
	 * Фэйковые вышки нельзя атаковать
	 */
	@Override
	public boolean isAttackable(L2Character cha, boolean forceUse, boolean sendMessage)
	{
		return false;
	}

	@Override
	public void onAction(L2Player player, boolean dontMove)
	{
		if(this != player.getTarget())
		{
			if(player.setTarget(this))
			{
				player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
				player.sendPacket(new ValidateLocation(this));
			}	
		}
		else
			player.sendActionFailed();
	}

	/**
	 * Вышки не умеют говорить
	 */
	@Override
	public void showChatWindow(L2Player player, int val)
	{}

	/**
	 * Вышки не умеют говорить
	 */
	@Override
	public void showChatWindow(L2Player player, String filename)
	{}

	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}

	/**
	 * Фэйковые вышки неуязвимы
	 * @return true
	 */
	@Override
	public boolean isInvul()
	{
		return true;
	}

	@Override
	public boolean isFearImmune()
	{
		return true;
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
}