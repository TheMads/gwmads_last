package ru.l2gw.gameserver.model.playerSubOrders;

import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Player;

import java.util.concurrent.ScheduledFuture;

/**
 * @author rage
 * @date 14.04.11 12:21
 */
public class FloatingRate
{
	public double rateEXPSP = 1;
	public double rateADENA = 1;
	public double rateDROP = 1;
	public double rateSPOIL = 1;

	public int pointEXPSP;
	public int pointADENA;
	public int pointDROP;
	public int pointSPOIL;
	public int extraPoints;

	private double rate;
	private final L2Player player;
	private ScheduledFuture<?> extraEndTask;

	public FloatingRate(L2Player player)
	{
		rate = 1. / Config.ALT_FLOATING_RATE_MIN;
		this.player = player;
	}

	public void setExtraPoints(int points, long expire)
	{
		extraPoints = points;
		if(extraEndTask != null)
			extraEndTask.cancel(true);
		extraEndTask = ThreadPoolManager.getInstance().scheduleGeneral(new ExtraEndTask(), expire);
	}

	public void setPointExpSp(int point)
	{
		if(point >= 0 && getPointsSum() - pointEXPSP + point <= Config.ALT_FLOATING_RATE_POINTS + extraPoints && point <= Config.ALT_FLOATING_RATE_MAX - Config.ALT_FLOATING_RATE_MIN)
		{
			pointEXPSP = point;
			rateEXPSP = 1 + rate * point;
			player.setVar("fr_expsp", point);
			player.setSessionVar("nrate_expsp", null);
		}
	}

	public void setPointAdena(int point)
	{
		if(point >= 0 && getPointsSum() - pointADENA + point <= Config.ALT_FLOATING_RATE_POINTS + extraPoints && point <= Config.ALT_FLOATING_RATE_MAX - Config.ALT_FLOATING_RATE_MIN)
		{
			pointADENA = point;
			rateADENA = 1 + rate * point;
			player.setVar("fr_adena", point);
			player.setSessionVar("nrate_adena", null);
		}
	}

	public void setPointDrop(int point)
	{
		if(point >= 0 && getPointsSum() - pointDROP + point <= Config.ALT_FLOATING_RATE_POINTS + extraPoints && point <= Config.ALT_FLOATING_RATE_MAX - Config.ALT_FLOATING_RATE_MIN)
		{
			pointDROP = point;
			rateDROP = 1 + rate * point;
			player.setVar("fr_drop", point);
			player.setSessionVar("nrate_drop", null);
		}
	}

	public void setPointSpoil(int point)
	{
		if(point >= 0 && getPointsSum() - pointSPOIL + point <= Config.ALT_FLOATING_RATE_POINTS + extraPoints && point <= Config.ALT_FLOATING_RATE_MAX - Config.ALT_FLOATING_RATE_MIN)
		{
			pointSPOIL = point;
			rateSPOIL = 1 + rate * point;
			player.setVar("fr_spoil", point);
			player.setSessionVar("nrate_spoil", null);
		}
	}

	public int getPointsSum()
	{
		return pointEXPSP + pointADENA + pointDROP + pointSPOIL;
	}

	public void recalcRates()
	{
		while(getPointsSum() > Config.ALT_FLOATING_RATE_POINTS + extraPoints)
		{
			if(pointSPOIL > 0)
				pointSPOIL--;
			if(getPointsSum() > Config.ALT_FLOATING_RATE_POINTS + extraPoints && pointDROP > 0)
				pointDROP--;
			if(getPointsSum() > Config.ALT_FLOATING_RATE_POINTS + extraPoints && pointADENA > 0)
				pointADENA--;
			if(getPointsSum() > Config.ALT_FLOATING_RATE_POINTS + extraPoints && pointEXPSP > 0)
				pointEXPSP--;
		}
		setPointExpSp(pointEXPSP);
		setPointAdena(pointADENA);
		setPointDrop(pointDROP);
		setPointSpoil(pointSPOIL);
	}

	public void stopExtraTask()
	{
		if(extraEndTask != null)
			extraEndTask.cancel(true);
		extraEndTask = null;
	}

	private class ExtraEndTask implements Runnable
	{
		public void run()
		{
			extraPoints = 0;
			recalcRates();
			player.sendMessage(new CustomMessage("floatingRate.expired", player));
			extraEndTask = null;
		}
	}
}
