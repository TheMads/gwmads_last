package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.CastleManorManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.MyTargetSelected;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.serverpackets.ValidateLocation;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

public class L2CastleBlacksmithInstance extends L2NpcInstance
{
	//private static Log _log = LogFactory.getLog(L2CastleChamberlainInstance.class.getName());

	protected static final int COND_ALL_FALSE = 0;
	protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	protected static final int COND_OWNER = 2;

	public L2CastleBlacksmithInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onAction(L2Player player, boolean dontMove)
	{
		player.setLastNpc(this);
		if(this != player.getTarget())
		{
			if(player.setTarget(this))
			{
				player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
				player.sendPacket(new ValidateLocation(this));
			}
		}
		else
		{
			//player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
			if(!isInRange(player, getInteractDistance(player)))
			{
				if(!dontMove)
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				player.sendActionFailed();
			}
			else
			{
				if(CastleManorManager.getInstance().isDisabled())
				{
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/npcdefault.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%npcname%", getName());
					player.sendPacket(html);
				}
				else
					showMessageWindow(player, 0);
				player.sendActionFailed();
			}
		}
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		player.sendPacket(Msg.ActionFail);

		if(CastleManorManager.getInstance().isDisabled())
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile("data/html/npcdefault.htm");
			html.replace("%objectId%", String.valueOf(getObjectId()));
			html.replace("%npcname%", getName());
			player.sendPacket(html);
			return;
		}

		int condition = validateCondition(player);
		if(condition <= COND_ALL_FALSE)
			return;

		if(condition == COND_BUSY_BECAUSE_OF_SIEGE)
			return;

		if(condition == COND_OWNER)
			if(command.startsWith("Chat"))
			{
				int val = 0;
				try
				{
					val = Integer.parseInt(command.substring(5));
				}
				catch(IndexOutOfBoundsException ioobe)
				{}
				catch(NumberFormatException nfe)
				{}
				showMessageWindow(player, val);
			}
			else
				super.onBypassFeedback(player, command);
	}

	private void showMessageWindow(L2Player player, int val)
	{
		player.sendPacket(Msg.ActionFail);
		String filename = "data/html/castle/blacksmith/castleblacksmith-no.htm";

		int condition = validateCondition(player);
		if(condition > COND_ALL_FALSE)
			if(condition == COND_BUSY_BECAUSE_OF_SIEGE)
				filename = "data/html/castle/blacksmith/castleblacksmith-busy.htm"; // Busy because of siege
			else if(condition == COND_OWNER)
				if(val == 0)
					filename = "data/html/castle/blacksmith/castleblacksmith.htm";
				else
					filename = "data/html/castle/blacksmith/castleblacksmith-" + val + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		html.replace("%castleid%", Integer.toString(getBuilding(2).getId()));
		player.sendPacket(html);
	}

	protected int validateCondition(L2Player player)
	{
		if(player.isGM())
			return COND_OWNER;
		if(getBuilding(2).getId() > 0)
			if(player.getClanId() != 0)
				if(getBuilding(2).getSiege().isInProgress())
					return COND_BUSY_BECAUSE_OF_SIEGE; // Busy because of siege
				else if(getBuilding(2).getOwnerId() == player.getClanId() // Clan owns castle
						&& (player.getClanPrivileges() & L2Clan.CP_CS_MANOR_ADMIN) == L2Clan.CP_CS_MANOR_ADMIN) // has manor rights
					return COND_OWNER; // Owner
		return COND_ALL_FALSE;
	}
}