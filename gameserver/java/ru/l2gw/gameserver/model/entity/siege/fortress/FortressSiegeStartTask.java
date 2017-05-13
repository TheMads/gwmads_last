package ru.l2gw.gameserver.model.entity.siege.fortress;

import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.entity.Fortress;
import ru.l2gw.gameserver.model.entity.siege.SiegeClan;
import ru.l2gw.gameserver.model.entity.siege.SiegeClanType;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.util.CountdownTimer;

import java.util.Date;

/**
 * @author rage
 * @date 18.06.2009 10:06:34
 */
public class FortressSiegeStartTask extends CountdownTimer
{
	private FortressSiege _siege;

	public FortressSiegeStartTask(FortressSiege siege)
	{
		super(siege.getCountdownTime() * 60 + ";300;120;60;30;20;10;5;4;3;2;1;0", (siege.getSiegeDate().getTimeInMillis() - System.currentTimeMillis()) / 1000);
		_siege = siege;
	}

	@Override
	public void onStart() throws Throwable
	{
		if(_siege.isInProgress())
		{
			_siege.setStartSiegeTask(null);
			abortTimer(true);
		}

		long siegeDate = _siege.getSiegeDate().getTimeInMillis();
		_log.info("FortressSiege: start fortress siege task for " + _siege.getSiegeUnit().getName() + " Date: " + new Date(siegeDate));

		if(siegeDate - System.currentTimeMillis() < _siege.getCountdownTime() * 60000)
		{
			if(_siege.getAttackerClans().size() < 1)
			{
				_log.info("FortressSiege: siege aborted " + _siege.getSiegeUnit().getName());
				if(_siege.getSiegeUnit().getOwnerId() > 0)
					_siege.removeSiegeClan(_siege.getSiegeUnit().getOwnerId());
				_siege.setStartSiegeTask(null);
				abortTimer(true);
				return;
			}

			if(_siege.getSiegeUnit().getMerchantSpawn() != null)
				_siege.getSiegeUnit().getMerchantSpawn().despawnAll();

			if(_siege.getSiegeUnit().getOwnerId() > 0 && _siege.getDefenderClans().size() < 1)
			{
				_siege.addDefender(new SiegeClan(_siege.getSiegeUnit().getOwnerId(), SiegeClanType.OWNER));

				for(Fortress fort : ResidenceManager.getInstance().getFortressList())
					if(fort.getId() != _siege.getSiegeUnit().getId() && fort.getSiege().checkIsClanRegistered(_siege.getSiegeUnit().getOwnerId()))
					{
						if(fort.getSiege().isInProgress())
							fort.getSiege().updateRemovedClan(_siege.getSiegeUnit().getOwnerId());

						fort.getSiege().removeSiegeClan(_siege.getSiegeUnit().getOwnerId());
					}
			}
		}
	}

	@Override
	public void onCheckpoint(long sec) throws Throwable
	{
		if(sec == _siege.getCountdownTime() * 60)
		{
			if(_siege.getAttackerClans().size() < 1)
			{
				_log.info("FortressSiege: siege aborted " + _siege.getSiegeUnit().getName());
				if(_siege.getSiegeUnit().getOwnerId() > 0)
					_siege.removeSiegeClan(_siege.getSiegeUnit().getOwnerId());
				_siege.setStartSiegeTask(null);
				abortTimer(true);
				return;
			}

			if(_siege.getSiegeUnit().getMerchantSpawn() != null)
				_siege.getSiegeUnit().getMerchantSpawn().despawnAll();

			if(_siege.getSiegeUnit().getOwnerId() > 0 && _siege.getDefenderClans().size() < 1)
			{
				_siege.addDefender(new SiegeClan(_siege.getSiegeUnit().getOwnerId(), SiegeClanType.OWNER));

				for(Fortress fort : ResidenceManager.getInstance().getFortressList())
					if(fort.getId() != _siege.getSiegeUnit().getId() && fort.getSiege().checkIsClanRegistered(_siege.getSiegeUnit().getOwnerId()))
					{
						if(fort.getSiege().isInProgress())
							fort.getSiege().updateRemovedClan(_siege.getSiegeUnit().getOwnerId());

						fort.getSiege().removeSiegeClan(_siege.getSiegeUnit().getOwnerId());
					}
			}
		}

		if(sec >= 60)
		{
			_siege.announceToAttackers(new SystemMessage(SystemMessage.S1_MINUTES_UNTIL_THE_FORTRESS_BATTLE_STARTS).addNumber(sec / 60));
			_siege.announceToDefenders(new SystemMessage(SystemMessage.S1_MINUTES_UNTIL_THE_FORTRESS_BATTLE_STARTS).addNumber(sec / 60));
		}
		else
		{
			_siege.announceToAttackers(new SystemMessage(SystemMessage.S1_SECONDS_UNTIL_THE_FORTRESS_BATTLE_STARTS).addNumber(sec));
			_siege.announceToDefenders(new SystemMessage(SystemMessage.S1_SECONDS_UNTIL_THE_FORTRESS_BATTLE_STARTS).addNumber(sec));
		}
	}

	@Override
	public void onFinish() throws Throwable
	{
		_siege.setStartSiegeTask(null);
		_siege.startSiege();
	}

	@Override
	public void onTimerAborted() throws Throwable
	{
		_log.warn("FortressSiege: interrupted " + _siege.getSiegeUnit().getName());
		_siege.setStartSiegeTask(null);
	}
}
