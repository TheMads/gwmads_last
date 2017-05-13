package ru.l2gw.gameserver.model.instances;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.ServerVariables;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.Warehouse.WarehouseType;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.serverpackets.WareHouseDepositList;
import ru.l2gw.gameserver.serverpackets.WareHouseWithdrawList;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.List;
import java.util.regex.Matcher;

public class L2CastleWarehouseInstance extends L2NpcInstance
{
	private static Log _log = LogFactory.getLog(L2WarehouseInstance.class.getName());

	protected static final int COND_ALL_FALSE = 0;
	protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	protected static final int COND_OWNER = 2;

	/**
	 * @param template
	 */
	public L2CastleWarehouseInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onAction(L2Player player, boolean dontMove)
	{
		player.setLastNpc(this);
		super.onAction(player, dontMove);
	}

	private void showRetrieveWindow(L2Player player)
	{
		if(AdminTemplateManager.checkBoolean("noWarehouse", player))
			return;

		player.setUsingWarehouseType(WarehouseType.PRIVATE);
		if(Config.DEBUG)
			_log.debug("Showing stored items");
		player.sendPacket(new WareHouseWithdrawList(player, WarehouseType.PRIVATE));
		player.sendActionFailed();
	}

	private void showDepositWindow(L2Player player)
	{
		if(AdminTemplateManager.checkBoolean("noWarehouse", player))
			return;

		player.setUsingWarehouseType(WarehouseType.PRIVATE);
		player.tempInvetoryDisable();
		if(Config.DEBUG)
			_log.debug("Showing items to deposit");

		player.sendPacket(new WareHouseDepositList(player, WarehouseType.PRIVATE));
		player.sendActionFailed();
	}

	private void showDepositWindowClan(L2Player player)
	{
		if(AdminTemplateManager.checkBoolean("noClanWarehouse", player))
			return;

		if(player.getClanId() == 0)
		{
			player.sendActionFailed();
			return;
		}

		if(player.getClan().getLevel() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_CLANS_OF_CLAN_LEVEL_1_OR_HIGHER_CAN_USE_A_CLAN_WAREHOUSE));
			player.sendActionFailed();
			return;
		}

		player.setUsingWarehouseType(WarehouseType.CLAN);
		player.tempInvetoryDisable();

		if(Config.DEBUG)
			_log.debug("Showing items to deposit - clan");

		if(!(player.isClanLeader() || Config.ALT_ALLOW_OTHERS_WITHDRAW_FROM_CLAN_WAREHOUSE && (player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) == L2Clan.CP_CL_VIEW_WAREHOUSE))
			player.sendPacket(Msg.ITEMS_LEFT_AT_THE_CLAN_HALL_WAREHOUSE_CAN_ONLY_BE_RETRIEVED_BY_THE_CLAN_LEADER_DO_YOU_WANT_TO_CONTINUE);

		player.sendPacket(new WareHouseDepositList(player, WarehouseType.CLAN));
	}

	private void showWithdrawWindowClan(L2Player player, int val)
	{
		if(AdminTemplateManager.checkBoolean("noClanWarehouse", player))
			return;

		if(player.getClanId() == 0)
		{
			player.sendActionFailed();
			return;
		}

		if(player.getClan().getLevel() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_CLANS_OF_CLAN_LEVEL_1_OR_HIGHER_CAN_USE_A_CLAN_WAREHOUSE));
			player.sendActionFailed();
			return;
		}

		if(/*Config.ALT_ALLOW_OTHERS_WITHDRAW_FROM_CLAN_WAREHOUSE&&*/(player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) == L2Clan.CP_CL_VIEW_WAREHOUSE)
		{
			player.setUsingWarehouseType(WarehouseType.CLAN);
			player.tempInvetoryDisable();
			player.sendPacket(new WareHouseWithdrawList(player, WarehouseType.CLAN));
		}
		else
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_THE_CLAN_WAREHOUSE));
			player.sendActionFailed();
		}
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if((player.getClanPrivileges() & L2Clan.CP_CS_USE_FUNCTIONS) != L2Clan.CP_CS_USE_FUNCTIONS)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
			return;
		}

		if(player.getEnchantScroll() != null)
		{
			_log.info("Player " + player.getName() + " trying to use enchant exploit, ban this player!");
			player.closeNetConnection();
			return;
		}

		if(command.startsWith("WithdrawP"))
		{
			int val = Integer.parseInt(command.substring(10));
			if(val == 9)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile("data/html/warehouse/personal.htm");
				html.replace("%npcname%", getName());
				player.sendPacket(html);
			}
			else
				showRetrieveWindow(player);
		}
		else if(command.equals("DepositP"))
			showDepositWindow(player);
		else if(command.startsWith("WithdrawC"))
		{
			int val = Integer.parseInt(command.substring(10));
			if(val == 9)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile("data/html/warehouse/clan.htm");
				html.replace("%npcname%", getName());
				player.sendPacket(html);
			}
			else
				showWithdrawWindowClan(player, val);
		}
		else if(command.equals("DepositC"))
			showDepositWindowClan(player);
		else if(command.startsWith("Chat"))
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
			showChatWindow(player, val);
		}
		else if(command.equalsIgnoreCase("HonorItem"))
		{
			if(!player.isClanLeader())
			{
			 	showChatWindow(player, "no", null);
				return;
			}

			List<String> replaces = new FastList<String>();
			replaces.add("%medal_level%");
			replaces.add(String.valueOf(ServerVariables.getInt("castle_" + getBuilding(2).getId() + "_ba", 0)));
			showChatWindow(player, "item", replaces);
		}
		else if(command.equalsIgnoreCase("Receive"))
		{
			if(!player.isClanLeader())
			{
			 	showChatWindow(player, "no", null);
				return;
			}

			int count = ServerVariables.getInt("castle_" + getBuilding(2).getId() + "_ba", 0);
			if(count > 0)
			{
				player.addItem("Castle", 9911, count, this, true);
				ServerVariables.set("castle_" + getBuilding(2).getId() + "_ba", 0);
				showChatWindow(player, "received", null);
			}
			else
				showChatWindow(player, "notreceived", null);
		}
		else if(command.equalsIgnoreCase("Exchange"))
		{
			if(!player.isClanLeader())
			{
			 	showChatWindow(player, "no", null);
				return;
			}

			long count = player.getItemCountByItemId(9911);
			if(count > 0)
			{
				player.destroyItemByItemId("Castle", 9911, 1, this, true);
				player.addItem("Castle", 9910, 30, this, true);
				showChatWindow(player, "exchanged", null);
			}
			else
				showChatWindow(player, "notexchanged", null);
		}
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		player.sendPacket(Msg.ActionFail);
		String filename = "data/html/castle/warehouse/castlewarehouse-no.htm";

		int condition = validateCondition(player);
		if(condition > COND_ALL_FALSE)
			if(condition == COND_BUSY_BECAUSE_OF_SIEGE)
				filename = "data/html/castle/warehouse/castlewarehouse-busy.htm"; // Busy because of siege
			else if(condition == COND_OWNER)
				if(val == 0)
					filename = "data/html/castle/warehouse/castlewarehouse.htm";
				else
					filename = "data/html/castle/warehouse/castlewarehouse-" + val + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}

	public void showChatWindow(L2Player player, String prefix, List<String> replaces)
	{
		String filename = "data/html/castle/warehouse/castlewarehouse-no.htm";

		int condition = validateCondition(player);
		if(condition > COND_ALL_FALSE)
			if(condition == COND_BUSY_BECAUSE_OF_SIEGE)
				filename = "data/html/castle/warehouse/castlewarehouse-busy.htm"; // Busy because of siege
			else if(condition == COND_OWNER)
				filename = "data/html/castle/warehouse/castlewarehouse-" + prefix + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());

		if(replaces != null)
			for(int i = 0; i < replaces.size(); i += 2)
				html.replace(replaces.get(i), Matcher.quoteReplacement(replaces.get(i + 1)));

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
				else if(getBuilding(2).getOwnerId() == player.getClanId()) // Clan owns castle
					return COND_OWNER;
		return COND_ALL_FALSE;
	}
}