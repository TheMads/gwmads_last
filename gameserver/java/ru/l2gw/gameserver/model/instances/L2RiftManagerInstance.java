package ru.l2gw.gameserver.model.instances;

import javolution.util.FastList;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.instancemanager.DimensionalRiftManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.DimensionalRift.DimensionalRift;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

/**
 * @author rage
 * @date 03.09.2009 10:22:55
 */
public class L2RiftManagerInstance extends L2NpcInstance
{
	private final static String _path = "data/html/seven_signs/rift/";
	private final static int DIMENSIONAL_FRAGMENT_ITEM_ID = 7079;
	private byte _riftLevel;
	private byte _riftRoom;

	public L2RiftManagerInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();

		if(getAIParams() != null && getAIParams().getByte("rift_level", (byte) 0) > 0)
			_riftLevel = getAIParams().getByte("rift_level", (byte) 1);
		else
			_log.warn(this + " has not reft_level ai_param!");

		if(getAIParams() != null)
			_riftRoom = getAIParams().getByte("room_index", (byte) 0);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(command.startsWith("EnterRift"))
		{
			if(player.getParty() == null)
			{
				showChatWindow(player, "alone", null);
				return;
			}
			else if(!player.getParty().isLeader(player))
			{
				List<String> replace = new FastList<String>();
				replace.add("%partyleader%");
				replace.add(player.getParty().getPartyLeader().getName());
				showChatWindow(player, "notleader", replace);
				return;
			}
			else if(DimensionalRiftManager.getInstance().isPartyInRift(player.getParty()))
			{
				showChatWindow(player, "partyinrift", null);
				return;
			}
			else
			{
				for(L2Player member : player.getParty().getPartyMembers())
				{
					if(member.getQuestState("_634_InSearchofDimensionalFragments") == null || member.getQuestState("_635_InTheDimensionalRift") == null || !isInRange(member, 300))
					{
						List<String> replace = new FastList<String>();
						replace.add("%noquestname%");
						replace.add(member.getName());
						showChatWindow(player, "noquest", replace);
						return;
					}
					else if(member.getItemCountByItemId(DIMENSIONAL_FRAGMENT_ITEM_ID) < getNeededItems(_riftLevel))
					{
						List<String> replace = new FastList<String>();
						replace.add("%nofragsname%");
						replace.add(member.getName());
						showChatWindow(player, "nofragments", replace);
						return;
					}
				}
			}

			if(DimensionalRiftManager.getInstance().getFreeRooms(_riftLevel, false).size() < 3)
			{
				showChatWindow(player, "nospace", null);
				return;
			}

			for(L2Player member : player.getParty().getPartyMembers())
				if(!member.destroyItemByItemId("RiftEntrance", DIMENSIONAL_FRAGMENT_ITEM_ID, getNeededItems(_riftLevel), this, true))
					return;

			showChatWindow(player, "success", null);
			DimensionalRiftManager.getInstance().start(player, _riftLevel);
		}
		else if(command.startsWith("ExitPage"))
			showChatWindow(player, "giveup", null);
		else if(command.startsWith("ChangeRiftRoom"))
		{
			if(player.getParty() == null || !player.getParty().isLeader(player))
			{
				showChatWindow(player, "notleader", null);
				return;
			}

			DimensionalRift rift = player.getParty().getDimensionalRift();

			if(rift == null)
			{
				_log.warn(this + ": can't jump! No rift for party!");
				return;
			}
			else if(rift.isJumped())
			{
				showChatWindow(player, "nomorechance", null);
				return;
			}

			rift.manualTeleport();
		}
		else if(command.startsWith("ExitRift"))
		{
			if(player.getParty() == null || !player.getParty().isLeader(player))
			{
				showChatWindow(player, "notleader", null);
				return;
			}

			DimensionalRift rift = player.getParty().getDimensionalRift();

			if(rift == null)
			{
				_log.warn(this + ": can't exit! No rift for party!");
				return;
			}

			rift.manualExit();
		}
		else if(command.startsWith("rift"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();

			int val = Integer.parseInt(st.nextToken());
			showChatWindow(player, val);
		}
		else
			super.onBypassFeedback(player, command);
	}

	private void showChatWindow(L2Player player, String suffix, List<String> replaces)
	{
		String filename = _path + (_riftRoom > 0 ? "teleporter-" : "rift-") + suffix + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
		html.setFile(filename);

		if(replaces != null)
			for(int i = 0; i < replaces.size(); i += 2)
				html.replace(replaces.get(i), Matcher.quoteReplacement(replaces.get(i + 1)));

		player.setLastNpc(this);
		player.sendPacket(html);
		player.sendActionFailed();
	}

	public void showChatWindow(L2Player player, int val)
	{
		String filename = _path + (_riftRoom > 0 ? "teleporter-" + _riftRoom : "rift");

		if(val == 0)
			filename += ".htm";
		else
			filename += "-" + val + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);

		player.setLastNpc(this);
		player.sendPacket(html);
	}

	private int getNeededItems(byte type)
	{
		switch(type)
		{
			case 1:
				return Config.RIFT_ENTER_COST_RECRUIT;
			case 2:
				return Config.RIFT_ENTER_COST_SOLDIER;
			case 3:
				return Config.RIFT_ENTER_COST_OFFICER;
			case 4:
				return Config.RIFT_ENTER_COST_CAPTAIN;
			case 5:
				return Config.RIFT_ENTER_COST_COMMANDER;
			case 6:
				return Config.RIFT_ENTER_COST_HERO;
			default:
				return 999999;
		}
	}
}
