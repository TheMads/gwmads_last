package ru.l2gw.gameserver.model.instances;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.Warehouse;
import ru.l2gw.gameserver.model.Warehouse.WarehouseType;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

public final class L2WarehouseInstance extends L2NpcInstance
{
	private static Log _log = LogFactory.getLog(L2WarehouseInstance.class.getName());

	/**
	 * @param template
	 */
	public L2WarehouseInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public String getHtmlPath(int npcId, int val, int karma)
	{
		String pom = "";
		if(val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;
		return "data/html/warehouse/" + pom + ".htm";
	}

	private void showRetrieveWindow(L2Player player, int val)
	{
		if(AdminTemplateManager.checkBoolean("noWarehouse", player))
			return;

		player.setUsingWarehouseType(WarehouseType.PRIVATE);
		if(Config.DEBUG)
			_log.debug("Showing stored items");
		player.sendPacket(new WareHouseWithdrawList(player, WarehouseType.PRIVATE));
	}

	private void showDepositWindow(L2Player player)
	{
		if(AdminTemplateManager.checkBoolean("noWarehouse", player))
			return;

		if (player.isWhDisabled())
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
			player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_THE_CLAN_WAREHOUSE));
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
			player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_THE_CLAN_WAREHOUSE));
			player.sendActionFailed();
			return;
		}

		L2Clan _clan = player.getClan();

		if(_clan.getLevel() == 0)
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

	private void showWithdrawWindowFreight(L2Player player)
	{
		if(AdminTemplateManager.checkBoolean("noWarehouse", player))
			return;

		player.setUsingWarehouseType(WarehouseType.FREIGHT);
		if(Config.DEBUG)
			_log.debug("Showing freightened items");

		Warehouse list = player.getFreight();

		if(list != null)
			player.sendPacket(new WareHouseWithdrawList(player, WarehouseType.FREIGHT));
		else if(Config.DEBUG)
			_log.debug("no items freightened");

		player.sendActionFailed();
	}

	private void showDepositWindowFreight(L2Player player)
	{
		if(AdminTemplateManager.checkBoolean("noWarehouse", player))
			return;

		player.setUsingWarehouseType(WarehouseType.FREIGHT);

		if(Config.DEBUG)
			_log.debug("Showing destination chars to freight - char src: " + player.getName());

		player.sendPacket(new PackageToList());
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		// lil check to prevent enchant exploit
		if(player.getEnchantScroll() != null)
		{
			_log.info("Player " + player.getName() + " trying to use enchant exploit, ban this player!");
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
				showRetrieveWindow(player, val);
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
		else if(command.startsWith("WithdrawF"))
		{
			if(Config.ALLOW_FREIGHT)
				showWithdrawWindowFreight(player);
		}
		else if(command.startsWith("DepositF"))
		{
			if(Config.ALLOW_FREIGHT)
				showDepositWindowFreight(player);
		}
		else
			super.onBypassFeedback(player, command);
	}
}
