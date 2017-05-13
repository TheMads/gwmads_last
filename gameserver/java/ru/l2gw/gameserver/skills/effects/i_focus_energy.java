package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 31.12.2009 12:42:32
 */
public class i_focus_energy extends i_effect
{
	private final int maxCharge;
	public i_focus_energy(EffectTemplate template)
	{
		super(template);
		maxCharge = _template._attrs.getInteger("chargeLevel", 0);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(!env.target.isPlayer())
				continue;

			L2Player player = env.target.getPlayer();

			if(player.getIncreasedForce() < maxCharge)
			{
				player.setIncreasedForce(player.getIncreasedForce() + 1);
				createRunnable(player);
			}
			else
				player.sendPacket(new SystemMessage(SystemMessage.YOUR_FORCE_HAS_REACHED_MAXIMUM_CAPACITY));
		}
	}

	public void createRunnable(L2Player player)
	{
		if(player._lastChargeRunnable == null)
		{
			player._lastChargeRunnable = new ChargeTimer(player);
			ThreadPoolManager.getInstance().scheduleGeneral(player._lastChargeRunnable, 600000);
		}
	}

	public class ChargeTimer implements Runnable
	{
		private L2Player _player;

		public ChargeTimer(L2Player player)
		{
			_player = player;
		}

		public void run()
		{
			if(_player._lastChargeRunnable != this)
				return;

			_player.setIncreasedForce(0);
			_player._lastChargeRunnable = null;
		}
	}
}
