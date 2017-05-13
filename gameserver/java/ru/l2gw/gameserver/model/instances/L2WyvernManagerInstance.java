package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.SevenSigns;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.tables.PetDataTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.StringTokenizer;

public final class L2WyvernManagerInstance extends L2NpcInstance
{
	private String _path;

	public L2WyvernManagerInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();

		if(getBuilding(-1).isFort)
			_path = "data/html/fortress/wyvern/";
		else if(getBuilding(-1).isClanHall)
			_path = "data/html/clanhall/wyvern/";
		else
			_path = "data/html/castle/wyvern/";
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		int cond = validateCondition(player);

		if(cond == Cond_Owner || cond == Cond_Clan)
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			String actualCommand = st.nextToken(); // Get actual command

			if(actualCommand.equalsIgnoreCase("RideHelp"))
				showChatWindow(player, "help");
			else if(actualCommand.equalsIgnoreCase("RideWyvern"))
			{
				if(cond != Cond_Owner)
				{
					showChatWindow(player, "lord_only");
					return;
				}
				else if(!player.isRiding() || player.getMountEngine().getMountNpcId() == 12621)
				{
					showChatWindow(player, "not_ready");
					return;
				}
				else if(!SevenSigns.getInstance().isSealValidationPeriod() || SevenSigns.getInstance().isSealValidationPeriod() && SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
				{
					showChatWindow(player, "no_ride_dusk");
					return;
				}

				L2ItemInstance cry = player.getInventory().getItemByItemId(1460);

				if(cry == null || cry.getCount() < 25)
					showChatWindow(player, "nocry");
				else if(player.destroyItem("RideWevern", cry.getObjectId(), 25, this, true))
				{
					player.getMountEngine().setMount(PetDataTable.getInstance().getInfo(PetDataTable.WYVERN_ID, player.getMountEngine().getMountNpcLevel()), player.getMountEngine().getMountObjId());
					showChatWindow(player, "after");
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
		html.setFile(_path + "wyvern-" + prefix + ".htm");

		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.setLastNpc(this);

		player.sendPacket(html);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String filename = _path;
		int cond = validateCondition(player);

		if(cond == Cond_Clan || cond == Cond_Owner)
			filename += "wyvern.htm";
		else if(cond == Cond_Busy_Because_Of_Siege)
			filename += "wyvern-busy.htm";
		else
			filename += "wyvern-no.htm";

		NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
		player.setLastNpc(this);
		html.setFile(filename);
		html.replace("%npcname%", getName());
		html.replace("%charname%", player.getName());
		player.sendPacket(html);
	}

	@Override
	protected int validateCondition(L2Player player)
	{
		SiegeUnit unit = getBuilding(-1);

		if(unit != null && player.getClanId() != 0)
		{
			if(getBuilding(-1).isClanHall && getBuilding(0).getSiegeZone() == null)
				return Cond_All_False;
			if(getBuilding(-1).getSiege() != null && getBuilding(-1).getSiege().isInProgress())
				return Cond_Busy_Because_Of_Siege;
			if(player.isSiegeUnitLordClanMember(unit.getId()))
			{
				if(player.isClanLeader())
					return Cond_Owner; // Owner
				else
					return Cond_Clan;
			}
		}

		return Cond_All_False;
	}

}
