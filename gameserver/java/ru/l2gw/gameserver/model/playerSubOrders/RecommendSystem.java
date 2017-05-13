package ru.l2gw.gameserver.model.playerSubOrders;

import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2ObjectTasks.RecommendBonusTask;
import ru.l2gw.gameserver.model.entity.RecommendBonus;
import ru.l2gw.gameserver.serverpackets.ExVoteSystemInfo;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

/**
 * @author agr0naft
 * @date 24.02.11 00:07
 */
public class RecommendSystem
{
	private L2Player _player;
	private int _recommendsHave = 0;
	private int _recommendsLeft = 20;
	private int _recommendsLeftToday = 0;
	private int _bonusTime = 3600;
	private ScheduledFuture<?> _bonusTask = null;
	private boolean _isActive = false;

	public RecommendSystem(L2Player player)
	{
		_player = player;
	}

	public void setStats(int rec_have, int rec_left, int rec_left_today, int bonus_time)
	{
		_recommendsHave = rec_have;
		_recommendsLeft = rec_left;
		_recommendsLeftToday = rec_left_today;
		_bonusTime = bonus_time;
	}

	public int getRecommendsHave()
	{
		return _recommendsHave;
	}

	public void setRecommendsHave(int value)
	{
		if(value > 255)
			_recommendsHave = 255;
		else if(value < 0)
			_recommendsHave = 0;
		else
			_recommendsHave = value;
	}

	public void addRecommendsHave(int val)
	{
		setRecommendsHave(_recommendsHave + val);
		_player.broadcastUserInfo(true);
		sendInfo();
	}

	public int getRecommendsLeft()
	{
		return _recommendsLeft;
	}

	public void setRecommendsLeft(int value)
	{
		_recommendsLeft = value;
	}

	public int addRecommendsLeft()
	{
		int recoms = 1;
		if(_recommendsLeftToday < 20)
			recoms = 10;
		_recommendsLeft += recoms;
		_recommendsLeftToday += recoms;
		_player.sendUserInfo(true);
		return recoms;
	}

	private int getRecommendsLeftToday()
	{
		return _recommendsLeftToday;
	}

	public void setRecommendLeftToday(final int value)
	{
		_recommendsLeftToday = value;
		_player.setVar("recLeftToday", String.valueOf(_recommendsLeftToday));
	}

	public void giveRecommend(L2Player target)
	{
		if(target.getRecSystem() == null)
			return;
		if(target.getRecSystem().getRecommendsHave() < 255)
			target.getRecSystem().addRecommendsHave(1);
		if(_recommendsLeft > 0)
			_recommendsLeft -= 1;
		_player.sendUserInfo(true);
		sendInfo();
	}

	public int getBonusTime()
	{
		if(_bonusTask != null)
			return (int) Math.max(0, _bonusTask.getDelay(TimeUnit.SECONDS));
		return _bonusTime;
	}

	public void setBonusTime(int val)
	{
		_bonusTime = val;
	}

	public int getBonusVal()
	{
		if(getBonusTime() > 0 || _player.isHourglassEffected())
			return RecommendBonus.getRecommendBonus(_player);
		return 0;
	}

	public double getBonusMod()
	{
		if(getBonusTime() > 0 || _player.isHourglassEffected())
			return RecommendBonus.getRecommendMultiplier(_player);
		return 1;
	}

	public void startBonusSystem()
	{
		check();
		sendInfo();
	}

	public boolean isActive()
	{
		return _isActive;
	}

	public void setActive(boolean val)
	{
		if(_isActive == val)
			return;

		_isActive = val;
		if(val)
			startBonusTask();
		else
			stopBonusTask(true);

		sendInfo();
	}

	public void startBonusTask()
	{
		if(_bonusTask == null && getBonusTime() > 0 && !_player.isHourglassEffected() && _isActive)
			_bonusTask = ThreadPoolManager.getInstance().scheduleGeneral(new RecommendBonusTask(_player), getBonusTime() * 1000);
	}

	public void stopBonusTask(boolean saveTime)
	{
		if(_bonusTask != null)
		{
			if(saveTime)
				_bonusTime = (int) Math.max(0, _bonusTask.getDelay(TimeUnit.SECONDS));
			_bonusTask.cancel(false);
			_bonusTask = null;
		}
	}

	private void check()
	{
		Calendar temp = Calendar.getInstance();
		temp.set(Calendar.HOUR_OF_DAY, 6);
		temp.set(Calendar.MINUTE, 30);
		temp.set(Calendar.SECOND, 0);
		temp.set(Calendar.MILLISECOND, 0);
		long count = Math.round((System.currentTimeMillis() / 1000 - _player.getLogoutTime()) / 86400);
		if(count == 0 && _player.getLogoutTime() < temp.getTimeInMillis() / 1000 && System.currentTimeMillis() > temp.getTimeInMillis())
			count++;

		for(int i = 1; i < count; i++)
			setRecommendsHave(_recommendsHave - 20);

		if(count > 0)
			restart();
	}

	public void restart()
	{
		_bonusTime = 3600;
		_recommendsLeft = 20;
		setRecommendLeftToday(0);
		setRecommendsHave(_recommendsHave - 20);
		stopBonusTask(false);
		startBonusTask();
		_player.sendUserInfo(true);
		sendInfo();
	}

	public void sendInfo()
	{
		_player.sendPacket(new ExVoteSystemInfo(_player));
	}
}