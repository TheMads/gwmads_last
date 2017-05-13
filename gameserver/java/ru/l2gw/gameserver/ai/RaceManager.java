package ru.l2gw.gameserver.ai;

import javolution.util.FastList;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2RaceManagerInstance;
import ru.l2gw.gameserver.serverpackets.MonRaceInfo;

public class RaceManager extends DefaultAI
{
	private Boolean thinking = false; // to prevent recursive thinking
	FastList<L2Player> _knownPlayers = new FastList<L2Player>();

	public RaceManager(L2Character actor)
	{
		super(actor);
		AI_TASK_DELAY = 5000;
	}

	@Override
	public void run()
	{
		onEvtThink();
	}

	@Override
	protected void onEvtThink()
	{
		L2RaceManagerInstance actor = (L2RaceManagerInstance) _actor;
		MonRaceInfo packet = actor.getPacket();

		if(packet == null)
			return;

		synchronized (thinking)
		{
			if(thinking)
				return;
			thinking = true;
		}

		try
		{
			FastList<L2Player> newPlayers = new FastList<L2Player>();

			for(L2Player player : _actor.getAroundPlayers(1200))
			{
				newPlayers.add(player);
				if(!_knownPlayers.contains(player))
					player.sendPacket(packet);
				_knownPlayers.remove(player);
			}

			for(L2Player player : _knownPlayers)
				actor.removeKnownPlayer(player);

			_knownPlayers = newPlayers;

		}
		finally
		{
			// Stop thinking action
			thinking = false;
		}
	}
}