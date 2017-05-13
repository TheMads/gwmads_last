package ru.l2gw.gameserver.model.quest;

import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;

import java.util.concurrent.ScheduledFuture;

public class QuestTimer
{
	private class ScheduleTimerTask implements Runnable
	{
		public void run()
		{
			if(!isActive())
				return;
			cancel();
			if(_player != null)
				_player.processQuestEvent(_quest.getName(), _name);
		}
	}

	private class ScheduleGlobalTimerTask implements Runnable
	{
		public void run()
		{
			if(!isActive())
				return;
			cancel();
			getQuest().notifyEvent(_name, _npc, _player);
		}
	}

	private boolean _isActive = true;
	private String _name;
	private L2NpcInstance _npc;
	private L2Player _player;
	private Quest _quest;
	private ScheduledFuture<?> _schedular;

	public QuestTimer(Quest quest, String name, long time, L2NpcInstance npc, L2Player player, boolean isGlobal)
	{
		_name = name;
		_quest = quest;
		_player = player;
		_npc = npc;
		if(isGlobal)
			_schedular = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleGlobalTimerTask(), time); // Prepare auto end task
		else
			_schedular = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTimerTask(), time); // Prepare auto end task
	}

	public void cancel()
	{
		_isActive = false;
		if(_schedular != null)
			_schedular.cancel(false);
		getQuest().removeQuestTimer(this);
	}

	public final boolean isActive()
	{
		return _isActive;
	}

	public final String getName()
	{
		return _name;
	}

	public final L2NpcInstance getNpc()
	{
		return _npc;
	}

	public final L2Player getPlayer()
	{
		return _player;
	}

	public final Quest getQuest()
	{
		return _quest;
	}

	// public method to compare if this timer matches with the key attributes passed.
	// a quest and a name are required.
	// null npc or player act as wildcards for the match
	public boolean isMatch(Quest quest, String name, L2NpcInstance npc, L2Player player)
	{
		return quest == _quest && name.equalsIgnoreCase(_name) && (npc == null || getNpc() == null || npc == getNpc()) && (player == null || getPlayer() == null || player == getPlayer());
	}

	@Override
	public final String toString()
	{
		return _name;
	}
}
