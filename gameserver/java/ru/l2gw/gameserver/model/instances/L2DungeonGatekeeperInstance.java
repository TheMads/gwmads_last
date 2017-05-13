package ru.l2gw.gameserver.model.instances;

import javolution.util.FastList;
import ru.l2gw.gameserver.instancemanager.InstanceManager;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.instance.Instance;
import ru.l2gw.gameserver.model.entity.instance.InstanceTemplate;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.MapRegionTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.List;
import java.util.StringTokenizer;

/**
 * @author: rage
 * @date: 26.07.2009 18:28:26
 */
public class L2DungeonGatekeeperInstance extends L2NpcInstance
{
	private static final String _path = "data/html/instances/";

	public L2DungeonGatekeeperInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}


	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command

		if(actualCommand.equalsIgnoreCase("instance"))
		{
			int instId = Integer.parseInt(st.nextToken());
			InstanceTemplate it = InstanceManager.getInstance().getInstanceTemplateById(instId);

			if(it == null)
			{
				_log.warn(this + " try to enter instance id: " + instId + " but no instance template!");
				return;
			}

			Instance inst = InstanceManager.getInstance().getInstanceByPlayer(player);
			List<L2Player> party = new FastList<L2Player>();

			if(inst != null)
			{
				if(inst.getTemplate().getId() != instId)
				{
					player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_ENTERED_ANOTHER_INSTANCE_ZONE_THEREFORE_YOU_CANNOT_ENTER_CORRESPONDING_DUNGEON));
					return;
				}
				if(player.getLevel() < it.getMinLevel() || player.getLevel() > it.getMaxLevel())
				{
					player.sendPacket(new SystemMessage(SystemMessage.C1S_LEVEL_REQUIREMENT_IS_NOT_SUFFICIENT_AND_CANNOT_BE_ENTERED).addCharName(player));
					return;
				}

				if(it.isDispelBuff())
					for(L2Effect e : player.getAllEffects())
					{
						if(e.getNext() != null && e.getNext().isInUse() && e.getNext().getSkill().getBuffProtectLevel() < 1)
							e.getNext().exit();

						if(e.getSkill().getBuffProtectLevel() < 1)
							e.exit();
					}

				player.setStablePoint(player.getLoc());
				player.teleToLocation(inst.getStartLoc(), inst.getReflection());
				return;
			}

			if(it.getMaxCount() > 0 && InstanceManager.getInstance().getInstanceCount(instId) >= it.getMaxCount())
			{
				player.sendPacket(new SystemMessage(SystemMessage.THE_MAXIMUM_NUMBER_OF_INSTANCE_ZONES_HAS_BEEN_EXCEEDED_YOU_CANNOT_ENTER));
				return;
			}

			if(it.getMinParty() > 1)
			{
				if(player.getParty() == null)
				{
					player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_CURRENTLY_IN_A_PARTY_SO_YOU_CANNOT_ENTER));
					return;
				}
				else if(!player.getParty().isLeader(player))
				{
					player.getParty().broadcastToPartyMembers(new SystemMessage(SystemMessage.ONLY_A_PARTY_LEADER_CAN_TRY_TO_ENTER));
					return;
				}
				else if(player.getParty().getMemberCount() > it.getMaxParty() || player.getParty().getMemberCount() < it.getMinParty())
				{
					player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_ENTER_DUE_TO_THE_PARTY_HAVING_EXCEEDED_THE_LIMIT));
					return;
				}

				boolean ok = true;
				for(L2Player member : player.getParty().getPartyMembers())
					if(member.getLevel() < it.getMinLevel() || member.getLevel() > it.getMaxLevel())
					{
						player.getParty().broadcastToPartyMembers(new SystemMessage(SystemMessage.C1S_LEVEL_REQUIREMENT_IS_NOT_SUFFICIENT_AND_CANNOT_BE_ENTERED).addCharName(member));
						ok = false;
					}
					else if(member.getVar("instance-" + it.getType()) != null || InstanceManager.getInstance().getInstanceByPlayer(member) != null)
					{
						player.getParty().broadcastToPartyMembers(new SystemMessage(SystemMessage.C1_MAY_NOT_RE_ENTER_YET).addCharName(member));
						ok = false;
					}
					else if(!isInRange(member, 300))
					{
						player.getParty().broadcastToPartyMembers(new SystemMessage(SystemMessage.C1_IS_IN_A_LOCATION_WHICH_CANNOT_BE_ENTERED_THEREFORE_IT_CANNOT_BE_PROCESSED).addCharName(member));
						ok = false;
					}

				if(!ok)
					return;

				party.addAll(player.getParty().getPartyMembers());
			}
			else
			{
				if(player.getLevel() < it.getMinLevel() || player.getLevel() > it.getMaxLevel())
				{
					player.sendPacket(new SystemMessage(SystemMessage.C1S_LEVEL_REQUIREMENT_IS_NOT_SUFFICIENT_AND_CANNOT_BE_ENTERED).addCharName(player));
					return;
				}
				else if(player.getVar("instance-" + it.getType()) != null)
				{
					player.sendPacket(new SystemMessage(SystemMessage.C1_MAY_NOT_RE_ENTER_YET).addCharName(player));
					return;
				}

				party.add(player);
			}

			inst = InstanceManager.getInstance().createNewInstance(instId, party);
			if(inst != null)
				for(L2Player member : party)
					if(member != null)
					{
						if(it.isDispelBuff())
							for(L2Effect e : member.getAllEffects())
							{
								if(e.getNext() != null && e.getNext().isInUse() && e.getNext().getSkill().getBuffProtectLevel() < 1)
									e.getNext().exit();

								if(e.getSkill().getBuffProtectLevel() < 1)
									e.exit();
							}
						member.setStablePoint(member.getLoc());
						member.teleToLocation(inst.getStartLoc(), inst.getReflection());
					}
		}
		else if(actualCommand.equalsIgnoreCase("exitInstance"))
		{
			if(player.getParty() == null)
			{
				player.sendPacket(new SystemMessage(SystemMessage.ONLY_A_PARTY_LEADER_CAN_TRY_TO_ENTER));
				return;
			}
			else if(!player.getParty().isLeader(player))
			{
				player.getParty().broadcastToPartyMembers(new SystemMessage(SystemMessage.ONLY_A_PARTY_LEADER_CAN_TRY_TO_ENTER));
				return;
			}

			Instance inst = InstanceManager.getInstance().getInstanceByPlayer(player);

			if(inst == null)
			{
				_log.warn(this + " try to exit from instance but no instance! " + player + " reflection: " + player.getReflection());
				return;
			}

			for(L2Player member : player.getParty().getPartyMembers())
				if(isInRange(member, 300))
					member.teleToLocation(MapRegionTable.getInstance().getTeleToLocation(member, MapRegionTable.TeleportWhereType.ClosestTown), 0);

			inst.rescheduleEndTask(600);
		}
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String filename = _path;

		if((getNpcId() == 32496 || getNpcId() == 32664) && (player.getParty() == null || !player.getParty().isLeader(player)))
			filename += getNpcId() + "-notpl.htm";
		else if(val == 0)
			filename += getNpcId() + ".htm";
		else
			filename += getNpcId() + "-" + val + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);

		Instance inst = InstanceManager.getInstance().getInstanceByPlayer(player);
		html.replace("%instancename%", inst != null ? inst.getTemplate().getName() : "");

		player.setLastNpc(this);
		player.sendPacket(html);
	}
}
