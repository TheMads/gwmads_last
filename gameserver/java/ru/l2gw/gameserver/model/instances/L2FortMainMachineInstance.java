package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.concurrent.ScheduledFuture;

/**
 * @author rage
 * @date 15.07.2009 11:41:23
 */
public class L2FortMainMachineInstance extends L2NpcInstance
{
	private SiegeUnit _fortress;
	private static final String _path = "data/html/fortress/powercontrol/";
	private long _enableTime = 0;
	private ScheduledFuture<?> _resetTask = null;

	public L2FortMainMachineInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();

		_fortress = getBuilding(1);
		if(_fortress == null)
			_log.warn("Warning: " + this + " has no fortress!");
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(command.startsWith("switch"))
		{
			boolean disabled = true;

			for(Integer controlId : _fortress.getMainControllers())
			{
				for(L2NpcInstance npc : getKnownNpc(1500))
					if(npc.getNpcId() == controlId && npc instanceof L2FortPowerControllerInstance && !((L2FortPowerControllerInstance)npc).isDisabled())
					{
						disabled = false;
						break;
					}

				if(!disabled)
					break;
			}

			if(disabled)
			{
				if(_enableTime < System.currentTimeMillis())
				{
					_enableTime = System.currentTimeMillis() + 600000;
					_fortress.powerOff();
					if(_resetTask != null)
						_resetTask.cancel(true);

					_resetTask = ThreadPoolManager.getInstance().scheduleGeneral(new ResetTask(), 600000);
				}
				showChatWindow(player, 3);
			}
			else
				showChatWindow(player, 2);
		}
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		if(!_fortress.getSiege().checkIsAttacker(player.getClanId()))
		{
			player.sendActionFailed();
			return;
		}
		
		String filename = _path;

		if(val == 0)
			filename += "mainpower.htm";
		else
			filename += "mainpower-" + val + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);

		player.setLastNpc(this);
		player.sendPacket(html);
	}

	@Override
	public void deleteMe()
	{
		if(_resetTask != null)
		{
			_resetTask.cancel(true);
			_resetTask = null;
		}
		super.deleteMe();
	}

	@Override
	public boolean isInvul()
	{
		return true;
	}

	private class ResetTask implements Runnable
	{
		public void run()
		{
			for(Integer controlId : _fortress.getMainControllers())
				for(L2NpcInstance npc : getKnownNpc(1500))
					if(npc.getNpcId() == controlId && npc instanceof L2FortPowerControllerInstance)
						((L2FortPowerControllerInstance)npc).reset();

			_resetTask = null;
		}
	}
}
