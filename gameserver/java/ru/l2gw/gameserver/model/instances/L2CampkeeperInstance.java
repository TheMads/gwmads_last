package ru.l2gw.gameserver.model.instances;

import javolution.util.FastList;
import ru.l2gw.gameserver.instancemanager.InstanceManager;
import ru.l2gw.gameserver.instancemanager.ServerVariables;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.instance.Instance;
import ru.l2gw.gameserver.model.entity.instance.InstanceTemplate;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.List;
import java.util.StringTokenizer;

/**
 * @author rage
 * @date 02.07.2009 16:00:14
 */
public class L2CampkeeperInstance extends L2NpcInstance
{
	private SiegeUnit _fortress;
	private SiegeUnit _castle;
	private String _path;

	public L2CampkeeperInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();

		_fortress = getBuilding(1);
		if(_fortress != null)
			_path = "data/html/fortress/campkeeper/";
		else
		{
			_castle = getBuilding(2);
			_path = "data/html/castle/campkeeper/";
		}
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		int cond = validateCondition(player);

		if(cond == Cond_Owner)
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			String actualCommand = st.nextToken();

			if(actualCommand.equalsIgnoreCase("dungeon"))
			{
				int instId = Integer.parseInt(st.nextToken());
				InstanceTemplate it = InstanceManager.getInstance().getInstanceTemplateById(instId);

				if(it == null)
				{
					_log.warn(this + " has no instance template: " + instId);
					return;
				}

				long lastTime = ServerVariables.getInt("camp-" + (_fortress != null ? _fortress.getId() : _castle.getId()), 0) * 1000L;

				if(lastTime > System.currentTimeMillis())
				{
					if(player.getParty() != null)
					{
						if(InstanceManager.getInstance().getInstanceByPlayer(player) == null)
						{
							showChatWindow(player, "4hrs");
							return;
						}
						else
							for(L2Player member : player.getParty().getPartyMembers())
								if(member != null && InstanceManager.getInstance().getInstanceByPlayer(member) == null)
								{
									showChatWindow(player, "noparty");
									return;
								}
					}	
					else
					{
						showChatWindow(player, "noparty");
						return;
					}
				}
				else if(player.getParty() == null)
				{
					showChatWindow(player, "party");
					return;
				}
				else if(!player.getParty().isLeader(player))
				{
					showChatWindow(player, "notpl");
					return;
				}

				if(_fortress != null)
				{
					if(System.currentTimeMillis() - _fortress.getLastSiegeDate() < 60 * 60000)
					{
						showChatWindow(player, "notdecided");
						return;
					}

					if(_fortress.getContractCastleId() > 0)
					{
						showChatWindow(player, "castle");
						return;
					}
				}
				else if(_castle.getContractedFortressId() == 0)
				{
					showChatWindow(player, "fortress");
					return;
				}

				Instance inst = InstanceManager.getInstance().getInstanceByPlayer(player);

				if(inst != null)
				{
					if(inst.getTemplate().getId() != it.getId())
					{
						player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_ENTERED_ANOTHER_INSTANCE_ZONE_THEREFORE_YOU_CANNOT_ENTER_CORRESPONDING_DUNGEON));
						return;
					}
					player.setStablePoint(player.getLoc());
					player.teleToLocation(inst.getStartLoc(), inst.getReflection());
				}
				else
				{
					List<L2Player> party = new FastList<L2Player>();
					boolean ok = true;
					for(L2Player member : player.getParty().getPartyMembers())
						if(!isInRange(member, 300))
						{
							player.getParty().broadcastToPartyMembers(new SystemMessage(SystemMessage.C1_IS_IN_A_LOCATION_WHICH_CANNOT_BE_ENTERED_THEREFORE_IT_CANNOT_BE_PROCESSED).addCharName(member));
							ok = false;
						}
						else if(validateCondition(member) == Cond_Owner)
							party.add(member);
						else
						{
							player.getParty().broadcastToPartyMembers(new SystemMessage(SystemMessage.C1S_QUEST_REQUIREMENT_IS_NOT_SUFFICIENT_AND_CANNOT_BE_ENTERED).addCharName(member));
							ok = false;
						}

					if(!ok)
						return;

					inst = InstanceManager.getInstance().createNewInstance(instId, party);

					if(inst != null)
					{
						ServerVariables.set("camp-" + (_fortress != null ? _fortress.getId() : _castle.getId()), System.currentTimeMillis() / 1000 + 4 * 3600);
						for(L2Player member : party)
						{
							member.setStablePoint(member.getLoc());
							member.teleToLocation(inst.getStartLoc(), inst.getReflection());
						}
					}
				}
			}
			else
				super.onBypassFeedback(player, command);
		}
		else
			showChatWindow(player, 0);
	}


	public void showChatWindow(L2Player player, String prefix)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(_path + "campkeeper-" + prefix + ".htm");

		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.setLastNpc(this);

		player.sendPacket(html);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String filename = _path;

		if(val == 0)
		{
			int cond = validateCondition(player);
			if(cond == Cond_Busy_Because_Of_Siege)
				filename += "campkeeper-busy.htm";
			else if(cond == Cond_Owner)
				filename += "campkeeper.htm";
			else
				filename += "campkeeper-no.htm";
		}
		else
			filename += "campkeeper-" + val + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);

		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.setLastNpc(this);

		player.sendPacket(html);
	}

	
}
