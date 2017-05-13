package ru.l2gw.gameserver.model.playerSubOrders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.ExVitalityPointInfo;
import ru.l2gw.gameserver.skills.Stats;

import java.util.concurrent.ScheduledFuture;

/**
 * Created by IntelliJ IDEA.
 * User: rage
 * Date: 08.11.2008
 * Time: 0:00:39
 */
public class Vitality
{
	public static final double _pointsPerMin = Config.VIT_MAX_POINTS / Config.VIT_RECOVERY_TIME;
	private static final int LUCKY_SKILL = 194;
	// Vitality points 0 - 20000
	private double _vitPoints = Config.VIT_MAX_POINTS;
	private L2Player _player;

	// For future use. Maybe vip accounts etc
	private int _vitMinLevel = 0;

	private ScheduledFuture<?> _updatePointsTask;
	final static Log _log = LogFactory.getLog(Vitality.class.getName());

	/*
	 * Param: Player
	 * Param: vitPoints - restored  Vitality Points
	 */
	public Vitality(L2Player player)
	{
		_player = player;
	}

	public void setStats(int vitPoints)
	{
		_vitPoints = vitPoints > Config.VIT_MAX_POINTS ? Config.VIT_MAX_POINTS : vitPoints;
		if(Config.VIT_DEBUG)
			_log.info("Vitality[" + _player.getName() + "] resore points " + vitPoints);
		_updatePointsTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new UpdatePoints(), 60000, 60000);
	}

	public void updateOfflineTime(long logoutTime)
	{
		if(Config.VIT_DEBUG)
			_log.info("Vitality[" + _player.getName() + "] updateOffline time: " + logoutTime + " offline time " + ((System.currentTimeMillis() / 1000 - logoutTime) / 60));

		if(logoutTime > 0)
			changePoints(((System.currentTimeMillis() / 1000 - logoutTime) / 60) * _pointsPerMin);
	}

	public int getLevel()
	{
		int lvl;
		for(lvl = 0; lvl < 5; lvl++)
			if(_vitPoints <= Config.VIT_PER_LVL[lvl])
				break;

		return lvl < _vitMinLevel ? _vitMinLevel : lvl;
	}

	public double getRate()
	{
		if(_player.getHuntingBonus().isBlessingActive())
			return Config.VIT_RATE_LVL[4];
		return Config.VIT_RATE_LVL[(int) _player.calcStat(Stats.CHANGE_VP, getLevel(), null, null)];
	}

	protected synchronized void updatePoints(double points)
	{
		// Gracia Part 2 Support future use
		if(Config.VIT_MAX_PLAYER_LVL > _player.getLevel())
		{
			if(Config.VIT_CHECK_LUCKY_SKILL)
			{
				if(_player.getSkillLevel(LUCKY_SKILL) > 0)
					return;
			}
			else
				return;
		}

		int prevLvl = getLevel();
		int prevPoints = (int) _vitPoints;

		changePoints(points);
		sendChangeMessage(prevPoints, prevLvl);
	}

	private void changePoints(double points)
	{
		if(_vitPoints + points <= 0)
		{
			if(Config.VIT_DEBUG)
				_log.info("Vitality[" + _player.getName() + "] changePoints " + _vitPoints + " --> 0");

			_vitPoints = 0;
		}
		else if(_vitPoints + points >= Config.VIT_MAX_POINTS)
		{
			if(Config.VIT_DEBUG)
				_log.info("Vitality[" + _player.getName() + "] changePoints " + _vitPoints + " --> " + Config.VIT_MAX_POINTS);

			_vitPoints = Config.VIT_MAX_POINTS;
		}
		else
		{
			if(Config.VIT_DEBUG)
				_log.info("Vitality[" + _player.getName() + "] changePoints " + _vitPoints + " --> " + (_vitPoints + points));

			_vitPoints += points;
		}
	}

	private void sendChangeMessage(int prevPoint, int prevLvl)
	{
		if(prevPoint != (int) _vitPoints)
		{
			if((int) _vitPoints == 0)
				_player.sendPacket(Msg.YOUR_VITALITY_IS_FULLY_EXHAUSTED);
			else if((int) _vitPoints == Config.VIT_MAX_POINTS)
				_player.sendPacket(Msg.YOUR_VITALITY_IS_AT_MAXIMUM);
		}

		if(prevLvl != getLevel())
		{
			if(prevLvl > getLevel())
				_player.sendPacket(Msg.YOUR_VITALITY_HAS_DECREASED);
			else
				_player.sendPacket(Msg.YOUR_VITALITY_HAS_INCREASED);

			_player.sendUserInfo(true);
		}
	}

	public int getPoints()
	{
		return (int) _vitPoints;
	}

	public void updatePointsByExp(long exp, int lvl, boolean addPoint, boolean sendMessage)
	{
		// TODO: Разобратся, нельзя предметы использовать, или предметы не будут давать эффекта? 
		// (Все предметы для восполнения или поддержания энергии не действуют во время действия Нисхождения Невитта)
		if(!addPoint)
		{
			int recoveryVp = (int) _player.calcStat(Stats.RECOVERY_VP, -1, null, null);
			if(recoveryVp == 1)
				addPoint = true;
			else if(recoveryVp == 0 || _player.getHuntingBonus().isBlessingActive())
				return;
		}

		double points = exp / (double) (lvl * lvl) * 100 / 9;

		if(Config.VIT_DEBUG)
			_log.info("Vitality[" + _player.getName() + "] updatePoints exp/lvl: " + exp + "/" + lvl + " vitPoints " + points);

		updatePoints(addPoint ? points : -points);

		if(sendMessage)
			_player.sendPacket(Msg.YOU_HAVE_GAINED_VITALITY_POINTS);
	}

	public void addPoints(int points)
	{
		// TODO: Разобратся, нельзя предметы использовать, или предметы не будут давать эффекта? 
		// (Все предметы для восполнения или поддержания энергии не действуют во время действия Нисхождения Невитта)
		if(_player.getHuntingBonus().isBlessingActive() && points < 0)
			return;

		if(Config.VIT_DEBUG)
			_log.info("Vitality[" + _player.getName() + "] setPoints: vitPoints " + points);

		updatePoints(points);
	}

	public void setVitMinLvl(int value)
	{
		_vitMinLevel = value;
	}

	public void stopUpdatetask()
	{
		if(_updatePointsTask != null)
		{
			if(Config.VIT_DEBUG)
				_log.info("Vitality[" + _player.getName() + "] stop update task");
			_updatePointsTask.cancel(true);
			_updatePointsTask = null;
		}
	}

	public void setPoints(int points)
	{
		_vitPoints = points;
		_player.sendUserInfo(true);
	}

	private class UpdatePoints implements Runnable
	{
		private int _lastPonits = 0;
		public void run()
		{
			if(Config.VIT_DEBUG)
				_log.info("Vitality[" + _player.getName() + "] update task run");
			if(_player.isInZonePeace())
				updatePoints(_pointsPerMin / Config.VIT_ONLINE_RECOVERY_RATE);
			if(_lastPonits != getPoints())
			{
				_lastPonits = getPoints();
				_player.sendPacket(new ExVitalityPointInfo(_lastPonits));
			}
		}
	}
}