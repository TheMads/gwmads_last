package ru.l2gw.extensions.listeners.L2Zone;

import ru.l2gw.extensions.listeners.L2ZoneEnterLeaveListener;
import ru.l2gw.gameserver.ai.CtrlEvent;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.instances.L2TrapInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;

/**
 * @author: rage
 * @date: 27.02.12 18:47
 */
public class TrapArea extends L2ZoneEnterLeaveListener
{
	@Override
	public void objectEntered(L2Zone zone, L2Character object)
	{
		if(zone.getEntityId() > 0)
		{
			for(L2NpcInstance npc : L2ObjectsStorage.getAllByNpcId(zone.getEntityId(), true))
			{
				if(npc.isTrap() && ((L2TrapInstance) npc).isWorldTrapActive())
					npc.notifyAiEvent(npc, CtrlEvent.EVT_TRAP_STEP_IN, object, null, null);
			}
		}
	}

	@Override
	public void objectLeaved(L2Zone zone, L2Character object)
	{
		if(zone.getEntityId() > 0)
		{
			for(L2NpcInstance npc : L2ObjectsStorage.getAllByNpcId(zone.getEntityId(), true))
			{
				if(npc.isTrap() && ((L2TrapInstance) npc).isWorldTrapActive())
					npc.notifyAiEvent(npc, CtrlEvent.EVT_TRAP_STEP_OUT, object, null, null);
			}
		}
	}

	@Override
	public void sendZoneStatus(L2Zone zone, L2Player player)
	{
	}
}