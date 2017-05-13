package ru.l2gw.gameserver.model.entity.duel;

import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

/**
 * @author rage
 * @date 18.05.11 13:39
 */
public class DuelTasks
{
	public static class StartDuelTask implements Runnable
	{
		private Duel _duel;
		private int _count = 3;

		public StartDuelTask(Duel duel)
		{
			_duel = duel;
		}

		public void run()
		{
			try
			{
				Duel.DuelResult result = _duel.checkEndDuelCondition();
				if(result != Duel.DuelResult.Continue)
				{
					_duel.endDuel(result);
					return;
				}

				SystemMessage sm;
				if(_count > 0)
				{
					sm = new SystemMessage(SystemMessage.THE_DUEL_WILL_BEGIN_IN_S1_SECONDS).addNumber(_count);
					ThreadPoolManager.getInstance().scheduleGeneral(this, 1000);
				}
				else
				{
					sm = new SystemMessage(SystemMessage.LET_THE_DUEL_BEGIN);
					_duel.startDuel();
				}

				_count--;
				_duel.broadcastToTeam1(sm);
				_duel.broadcastToTeam2(sm);
			}
			catch(Throwable t)
			{
			}
		}
	}

	public static class DuelTask implements Runnable
	{
		private Duel _duel;

		public DuelTask(Duel duel)
		{
			_duel = duel;
		}

		public void run()
		{
			try
			{
				Duel.DuelResult status = _duel.checkEndDuelCondition();

				if(status != Duel.DuelResult.Continue)
					_duel.endDuel(status);
				else
					ThreadPoolManager.getInstance().scheduleGeneral(this, 1000);
			}
			catch(Throwable t)
			{
			}
		}
	}

	public static class EndDuelTask implements Runnable
	{
		private Duel _duel;
		private Duel.DuelResult _result;

		public EndDuelTask(Duel duel, Duel.DuelResult result)
		{
			_duel = duel;
			_result = result;
		}

		public void run()
		{
			_duel.finishDuel(_result);
		}
	}
}
