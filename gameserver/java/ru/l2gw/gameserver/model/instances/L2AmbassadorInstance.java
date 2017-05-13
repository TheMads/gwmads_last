package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.Fortress;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.StringTokenizer;

/**
 * @author rage
 * @date 25.06.2009 17:24:14
 */
public class L2AmbassadorInstance extends L2NpcInstance
{
	private int _answer = 0;
	private SiegeUnit _fortress;
	private static final String _path = "data/html/fortress/ambassador/";

	public L2AmbassadorInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_answer = 0;

		_fortress = getBuilding(1);

		if(_fortress == null)
			_log.warn("Warning: " + this + " has no fortress!");
	}


	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(command.startsWith("contractCastle"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			int val = -1;

			if(st.hasMoreTokens())
				val = Integer.parseInt(st.nextToken());

			if(_fortress.getOwnerId() != player.getClanId() || !player.isClanLeader())
			{
				showChatWindow(player, 3);
				return;
			}

			if(val > 0)
			{
				if(_answer == 2)
				{
					showChatWindow(player, 5);
					return;
				}
				else if(_answer == 1)
				{
					showChatWindow(player, 7);
					return;
				}

				_answer = 2;
				_fortress.setContractCastle(val);
				if(ResidenceManager.getInstance().getBuildingById(val).getSiege().checkIsClanRegistered(player.getClanId()))
				{
					ResidenceManager.getInstance().getBuildingById(val).getSiege().removeSiegeClan(player.getClanId());
					if(ResidenceManager.getInstance().getBuildingById(val).getSiege().checkIsAttacker(player.getClanId()))
						ResidenceManager.getInstance().getBuildingById(val).getSiege().getAttackerClans().remove(player.getClanId());
					else if(ResidenceManager.getInstance().getBuildingById(val).getSiege().checkIsDefender(player.getClanId()))
						ResidenceManager.getInstance().getBuildingById(val).getSiege().getDefenderClans().remove(player.getClanId());
					else if(ResidenceManager.getInstance().getBuildingById(val).getSiege().checkIsDefenderWaiting(player.getClanId()))
						ResidenceManager.getInstance().getBuildingById(val).getSiege().getDefenderWaitingClans().remove(player.getClanId());
				}

				showChatWindow(player, 4);
			}
			else if(val == 0)
			{
				if(_answer == 1)
				{
					showChatWindow(player, 6);
					return;
				}
				else if(_answer == 2)
				{
					showChatWindow(player, 8);
					return;
				}

				_answer = 1;
				showChatWindow(player, 2);
			}
		}
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String filename = _path;

		if(val < 5)
		{
			if(val == 0)
			{
				if(_answer == 0 && _fortress.getFortressType() == Fortress.FortressType.TERRITORY)
					filename += getNpcId() + ".htm";
				else
					filename += getNpcId() + "-1.htm";
			}
			else
				filename += getNpcId() + "-" + val + ".htm";
		}
		else
			filename += "err-" + val + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);

		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));

		player.sendPacket(html);
	}
}
