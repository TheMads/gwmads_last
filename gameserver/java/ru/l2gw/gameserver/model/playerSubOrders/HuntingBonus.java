package ru.l2gw.gameserver.model.playerSubOrders;

import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.skills.AbnormalEffect;
import ru.l2gw.gameserver.serverpackets.ExNavitAdventEffect;
import ru.l2gw.gameserver.serverpackets.ExNavitAdventPointInfo;
import ru.l2gw.gameserver.serverpackets.ExNavitAdventTimeChange;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

/**
 * @author rage edited by agr0naft
 * @date 14.02.11 14:05
 */
public class HuntingBonus
{
	private static final int MAX_POINTS = 7200;
	private static final int ADVENT_TIME = 14400; // 240 минут длится период постоянного начисления очков.
	private static final int BONUS_EFFECT_TIME = 180; // 180 секунд длится эффект бонуса нэвита.

	private L2Player _player;
	private int _points = 0;
	private int _time = ADVENT_TIME;
	private ScheduledFuture<?> _adventTask = null;
	private ScheduledFuture<?> _nevitEffectTask = null;
	private int _percent = 0;
	private boolean _active = false;

	public HuntingBonus(L2Player player)
	{
		_player = player;
	}

	public void setStats(int points, int time)
	{
		_points = points;
		_percent = getPercent(_points);

		Calendar temp = Calendar.getInstance();
		temp.set(Calendar.HOUR_OF_DAY, 6);
		temp.set(Calendar.MINUTE, 30);
		temp.set(Calendar.SECOND, 0);
		temp.set(Calendar.MILLISECOND, 0);
		if(_player.getLogoutTime() < temp.getTimeInMillis() / 1000L && System.currentTimeMillis() > temp.getTimeInMillis())
			_time = ADVENT_TIME;
		else
			_time = time;
	}

	public void restartSystem()
	{
		_time = ADVENT_TIME;
		_player.sendPacket(new ExNavitAdventTimeChange(_active, _time));
	}

	public void onEnterWorld()
	{
		_player.sendPacket(new ExNavitAdventPointInfo(_points));
		_player.sendPacket(new ExNavitAdventTimeChange(_active, _time));
		startNevitEffect(_player.getVarInt("nevit"));
		if(_percent >= 45 && _percent < 50)
			_player.sendPacket(Msg.YOU_ARE_STARTING_TO_FEEL_THE_EFFECTS_OF_NEVITS_BLESSING);
		else if(_percent >= 50 && _percent < 75)
			_player.sendPacket(Msg.YOU_ARE_FURTHER_INFUSED_WITH_THE_BLESSINGS_OF_NEVIT_CONTINUE_TO_BATTLE_EVIL_WHEREVER_IT_MAY_LURK);
		else if(_percent >= 75)
			_player.sendPacket(Msg.NEVITS_BLESSING_SHINES_STRONGLY_FROM_ABOVE_YOU_CAN_ALMOST_SEE_HIS_DIVINE_AURA);
	}

	public void startAdventTask()
	{
		if(!_active)
		{
			_active = true;
			if(_time > 0 && _adventTask == null)
				_adventTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AdventTask(), 30000L, 30000L);

			_player.sendPacket(new ExNavitAdventTimeChange(_active, _time));
		}
	}

	private void startNevitEffect(int time)
	{
		if(time > 0)
		{
			_player.setVar("nevit", time);
			_player.sendPacket(new ExNavitAdventEffect(time));
			_player.sendPacket(Msg.THE_ANGEL_NEVIT_HAS_BLESSED_YOU_FROM_ABOVE_YOU_ARE_IMBUED_WITH_FULL_VITALITY_AS_WELL_AS_A_VITALITY_REPLENISHING_EFFECT);
			_player.startAbnormalEffect(L2Skill.AbnormalVisualEffect.vitality);
			_nevitEffectTask = ThreadPoolManager.getInstance().scheduleGeneral(new NevitEffectEnd(), time * 1000L);
		}
	}

	public void stopTasksOnLogout()
	{
		stopNevitEffectTask(true);
		stopAdventTask(false);
	}

	public void stopAdventTask(boolean sendPacket)
	{
		if(_adventTask != null)
		{
			_adventTask.cancel(true);
			_adventTask = null;
		}
		_active = false;
		if(sendPacket)
			_player.sendPacket(new ExNavitAdventTimeChange(_active, _time));
	}

	private void stopNevitEffectTask(boolean saveTime)
	{
		try
		{
			if(_nevitEffectTask != null)
			{
				if(saveTime)
				{
					int time = getEffectTime();
					if(time > 0)
						_player.setVar("nevit", time);
					else
						_player.unsetVar("nevit");
				}
				_nevitEffectTask.cancel(true);
				_nevitEffectTask = null;
			}
		}
		catch(NullPointerException e)
		{
		}
	}

	public boolean isActive()
	{
		return _active;
	}

	public int getTime()
	{
		return _time;
	}

	public int getPoints()
	{
		return _points;
	}

	public void addPoints(int val)
	{
		_points += val;
		int percent = getPercent(_points);
		if(_percent != percent)
		{
			_percent = percent;
			if(_percent == 45)
				_player.sendPacket(Msg.YOU_ARE_STARTING_TO_FEEL_THE_EFFECTS_OF_NEVITS_BLESSING);
			else if(_percent == 50)
				_player.sendPacket(Msg.YOU_ARE_FURTHER_INFUSED_WITH_THE_BLESSINGS_OF_NEVIT_CONTINUE_TO_BATTLE_EVIL_WHEREVER_IT_MAY_LURK);
			else if(_percent == 75)
				_player.sendPacket(Msg.NEVITS_BLESSING_SHINES_STRONGLY_FROM_ABOVE_YOU_CAN_ALMOST_SEE_HIS_DIVINE_AURA);
		}
		if(_points > MAX_POINTS)
		{
			_percent = 0;
			_points = 0;
			startNevitEffect(BONUS_EFFECT_TIME);
		}
		_player.sendPacket(new ExNavitAdventPointInfo(_points));
	}

	public int getPercent(int points)
	{
		return (int) (100.0D / MAX_POINTS * points);
	}

	public void setTime(int time)
	{
		_time = time;
	}

	public boolean isBlessingActive()
	{
		return getEffectTime() > 0;
	}

	private int getEffectTime()
	{
		if(_nevitEffectTask == null)
			return 0;
		return (int) Math.max(0, _nevitEffectTask.getDelay(TimeUnit.SECONDS));
	}

	private class AdventTask implements Runnable
	{
		public void run()
		{
			_time -= 30;
			if(_time <= 0)
			{
				_time = 0;
				stopAdventTask(true);
				return;
			}
			else
			{
				addPoints(72);
				if((_time % 60) == 0)
					_player.sendPacket(new ExNavitAdventTimeChange(true, _time));
			}
		}
	}

	private class NevitEffectEnd implements Runnable
	{
		public void run()
		{
			_player.sendPacket(new ExNavitAdventEffect(0));
			_player.sendPacket(new ExNavitAdventPointInfo(_points));
			_player.sendPacket(Msg.NEVITS_BLESSING_HAS_ENDED_CONTINUE_YOUR_JOURNEY_AND_YOU_WILL_SURELY_MEET_HIS_FAVOR_AGAIN_SOMETIME_SOON);
			//_player.stopAbnormalEffect(L2Skill.AbnormalVisualEffect.av2_nevit);
			_player.stopAbnormalEffect(L2Skill.AbnormalVisualEffect.vitality);
			_player.unsetVar("nevit");
			stopNevitEffectTask(false);
		}
	}
}