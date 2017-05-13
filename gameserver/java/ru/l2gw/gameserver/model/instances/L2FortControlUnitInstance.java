package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.tables.DoorTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

/**
 * @author rage
 * @date 15.07.2009 11:40:42
 */
public class L2FortControlUnitInstance extends L2NpcInstance
{
	private SiegeUnit _fortress;
	private static final String _path = "data/html/fortress/powercontrol/";
	private static final int CONTROL_CARD_ID = 10014;

	public L2FortControlUnitInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
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
		if(command.startsWith("open"))
		{
			if(player.getItemCountByItemId(CONTROL_CARD_ID) < 1)
			{
				showChatWindow(player, 2);
				return;
			}

			boolean disabled = true;
			for(Integer controlId : _fortress.getDoorControllers())
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
				if(!player.destroyItemByItemId("FortSiege", CONTROL_CARD_ID, 1, this, true))
				{
					showChatWindow(player, 2);
					return;
				}

				for(Integer doorId : _fortress.getControlDoors())
				{
					L2DoorInstance door = DoorTable.getInstance().getDoor(doorId);
					if(door != null && !door.isOpen())
						door.openMe();
				}
			}
			else
				showChatWindow(player, 3);
		}
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	public boolean isInvul()
	{
		return true;
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
			filename += "controller.htm";
		else
			filename += "controller-" + val + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);

		player.setLastNpc(this);
		player.sendPacket(html);
	}
}
