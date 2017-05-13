package ru.l2gw.gameserver.model.entity.siege;

import ru.l2gw.gameserver.model.L2Character;

/**
 * @author rage
 * @date 30.06.2009 16:05:05
 */
public class SiegeFameTask implements Runnable
{
	private Siege _siege;

	public SiegeFameTask(Siege siege)
	{
		_siege = siege;
	}

	public void run()
	{
		if(_siege.isInProgress())
		{
			long currTime = System.currentTimeMillis();
			for(L2Character cha : _siege.getZone().getCharacters())
				if(cha.isPlayer() && cha.getPlayer().getSiegeId() == _siege.getSiegeUnit().getId() && cha.getPlayer().getLastFameUpdate() + 5 * 60000 < currTime)
				{
					cha.getPlayer().addFame(_siege.getFamePoints());
					cha.getPlayer().updateFameTime();
				}
		}
	}
}
