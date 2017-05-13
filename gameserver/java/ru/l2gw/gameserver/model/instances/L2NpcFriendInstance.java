package ru.l2gw.gameserver.model.instances;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.scripts.Events;
import ru.l2gw.gameserver.controllers.TradeController;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.NpcTradeList;
import ru.l2gw.gameserver.model.Warehouse.WarehouseType;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.StringTokenizer;

public final class L2NpcFriendInstance extends L2NpcInstance
{
	private static Log _log = LogFactory.getLog(L2NpcFriendInstance.class.getName());

	public L2NpcFriendInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	private long _lastSocialAction;

	/**
	 * this is called when a player interacts with this NPC
	 *
	 * @param player
	 */
	@Override
	@SuppressWarnings({"static-access", "cast"})
	public void onAction(L2Player player, boolean dontMove)
	{
		if(this != player.getTarget())
		{
			if(player.setTarget(this))
			{
				player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));
				if(isAttackable(player, false, false))
				{
					StatusUpdate su = new StatusUpdate(getObjectId());
					su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
					su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
					player.sendPacket(su);
				}
				player.sendPacket(new ValidateLocation(this));
				player.sendActionFailed();
			}
			return;
		}

		//player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));

		if(!dontMove && Events.onAction(player, this))
			return;
		else if(dontMove && Events.onActionShift(player, this))
			return;

		if(isAttackable(player, false, false))
		{
			player.getAI().Attack(this, false, dontMove);
			return;
		}

		if(!isInRange(player, getInteractDistance(player)))
		{
			if(player.getAI().getIntention() != CtrlIntention.AI_INTENTION_INTERACT)
				if(!dontMove)
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this, null);
				else
					player.sendActionFailed();
			return;
		}

		if(!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && player.getKarma() > 0 && !player.isGM())
		{
			player.sendActionFailed();
			return;
		}

		//С NPC нельзя разговаривать мертвым и сидя
		if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
			return;

		if(System.currentTimeMillis() - _lastSocialAction > 10000)
			broadcastPacket(new SocialAction(getObjectId(), 2));

		_lastSocialAction = System.currentTimeMillis();

		player.sendActionFailed();

		String filename = "";

		if(getNpcId() >= 31370 && getNpcId() <= 31376 && player.getVarka() > 0 || getNpcId() >= 31377 && getNpcId() < 31384 && player.getKetra() > 0)
		{
			filename = "data/html/npc_friend/" + getNpcId() + "-nofriend.htm";
			showChatWindow(player, filename);
			return;
		}

		switch(getNpcId())
		{
			case 31370:
			case 31371:
			case 31373:
			case 31377:
			case 31378:
			case 31380:
				filename = "data/html/npc_friend/" + getNpcId() + ".htm";
				break;
			case 31372:
				if(player.getKetra() > 2)
					filename = "data/html/npc_friend/" + getNpcId() + "-bufflist.htm";
				else
					filename = "data/html/npc_friend/" + getNpcId() + ".htm";
				break;
			case 31379:
				if(player.getVarka() > 2)
					filename = "data/html/npc_friend/" + getNpcId() + "-bufflist.htm";
				else
					filename = "data/html/npc_friend/" + getNpcId() + ".htm";
				break;
			case 31374:
				if(player.getKetra() > 1)
					filename = "data/html/npc_friend/" + getNpcId() + "-warehouse.htm";
				else
					filename = "data/html/npc_friend/" + getNpcId() + ".htm";
				break;
			case 31381:
				if(player.getVarka() > 1)
					filename = "data/html/npc_friend/" + getNpcId() + "-warehouse.htm";
				else
					filename = "data/html/npc_friend/" + getNpcId() + ".htm";
				break;
			case 31375:
				if(player.getKetra() == 3 || player.getKetra() == 4)
					filename = "data/html/npc_friend/" + getNpcId() + "-special1.htm";
				else if(player.getKetra() == 5)
					filename = "data/html/npc_friend/" + getNpcId() + "-special2.htm";
				else
					filename = "data/html/npc_friend/" + getNpcId() + ".htm";
				break;
			case 31382:
				if(player.getVarka() == 3 || player.getVarka() == 4)
					filename = "data/html/npc_friend/" + getNpcId() + "-special1.htm";
				else if(player.getVarka() == 5)
					filename = "data/html/npc_friend/" + getNpcId() + "-special2.htm";
				else
					filename = "data/html/npc_friend/" + getNpcId() + ".htm";
				break;
			case 31376:
				if(player.getKetra() == 4)
					filename = "data/html/npc_friend/" + getNpcId() + "-normal.htm";
				else if(player.getKetra() == 5)
					filename = "data/html/npc_friend/" + getNpcId() + "-special.htm";
				else
					filename = "data/html/npc_friend/" + getNpcId() + ".htm";
				break;
			case 31383:
				if(player.getVarka() == 4)
					filename = "data/html/npc_friend/" + getNpcId() + "-normal.htm";
				else if(player.getVarka() == 5)
					filename = "data/html/npc_friend/" + getNpcId() + "-special.htm";
				else
					filename = "data/html/npc_friend/" + getNpcId() + ".htm";
				break;
			case 31553:
			case 31554:
			case 31555:
			case 31556:
				if(player.getItemCountByItemId(7246) > 0 || player.getRam() == 1)
					filename = "data/html/npc_friend/" + getNpcId() + "-special1.htm";
				else if(player.getItemCountByItemId(7247) > 0 || player.getRam() == 2)
					filename = "data/html/npc_friend/" + getNpcId() + "-special2.htm";
				else
					filename = "data/html/npc_friend/" + getNpcId() + ".htm";
				break;
		}

		showChatWindow(player, filename);
		if (Config.NPC_DIALOG_PLAYER_DELAY > 0) {
			player.setNpcDialogEndTime((int)(System.currentTimeMillis() / 1000L) + Config.NPC_DIALOG_PLAYER_DELAY);
		}
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command

		if(actualCommand.equalsIgnoreCase("Buff"))
		{
			if(st.countTokens() < 1)
				return;
			int val = Integer.parseInt(st.nextToken());
			int item = 0;

			switch(getNpcId())
			{
				case 31372:
					item = 7186;
					break;
				case 31379:
					item = 7187;
					break;
				case 31556:
					item = 7251;
					break;
			}

			int skill = 0;
			int level = 0;
			long count = 0;

			switch(val)
			{
				case 1:
					skill = 4359;
					level = 2;
					count = 2;
					break;
				case 2:
					skill = 4360;
					level = 2;
					count = 2;
					break;
				case 3:
					skill = 4345;
					level = 3;
					count = 3;
					break;
				case 4:
					skill = 4355;
					level = 2;
					count = 3;
					break;
				case 5:
					skill = 4352;
					level = 1;
					count = 3;
					break;
				case 6:
					skill = 4354;
					level = 3;
					count = 3;
					break;
				case 7:
					skill = 4356;
					level = 1;
					count = 6;
					break;
				case 8:
					skill = 4357;
					level = 2;
					count = 6;
					break;
			}

			if(skill != 0 && player.getInventory().getItemByItemId(item) != null && item > 0 && player.getInventory().getItemByItemId(item).getCount() >= count)
			{
				if(player.getInventory().destroyItemByItemId("FriendNpc", item, count, player, this) == null)
					_log.info("L2NpcFriendInstance[274]: Item not found!!!");
				player.doCast(SkillTable.getInstance().getInfo(skill, level), player, null, true);
				if(getNpcId() == 31556) // Medic Selina
					showChatWindow(player, "data/html/npc_friend/" + getNpcId() + "-special2.htm");
			}
			else
				showChatWindow(player, "data/html/npc_friend/" + getNpcId() + "-havenotitems.htm");
		}
		else if(command.startsWith("Chat"))
		{
			int val = Integer.parseInt(command.substring(5));
			String fname = "";
			fname = "data/html/npc_friend/" + getNpcId() + "-" + val + ".htm";
			if(!fname.equals(""))
				showChatWindow(player, fname);
		}
		else if(command.startsWith("Buy"))
		{
			int val = Integer.parseInt(command.substring(4));
			showBuyWindow(player, val);
		}
		else if(command.startsWith("WithdrawP"))
		{
			int val = Integer.parseInt(command.substring(10));
			if(val == 9)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile("data/html/npc-friend/personal.htm");
				html.replace("%npcname%", getName());
				player.sendPacket(html);
			}
			else
				showRetrieveWindow(player, val);
		}
		else if(command.equals("DepositP"))
			showDepositWindow(player);
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	public void showBuyWindow(L2Player player, int val)
	{
		if(AdminTemplateManager.checkBoolean("noShop", player))
			return;

		player.tempInvetoryDisable();
		if(Config.DEBUG)
			_log.debug("Showing buylist");
		NpcTradeList list = TradeController.getInstance().getSellList(val);
		if(list != null && list.getNpcId() == getNpcId())
		{
			player.sendPacket(new ExBuyList(list, player));
			player.sendPacket(new ExSellRefundList(player));
		}
		else
			_log.warn(player.getName() + " wrong sell list: " + list + " id: " + val + " for npc: " + this + " player loc: " + player.getLoc());
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

	private void showRetrieveWindow(L2Player player, int val)
	{
		if(AdminTemplateManager.checkBoolean("noWarehouse", player))
			return;

		player.setUsingWarehouseType(WarehouseType.PRIVATE);
		if(Config.DEBUG)
			_log.debug("Showing stored items");

		player.sendPacket(new WareHouseWithdrawList(player, WarehouseType.PRIVATE));
	}

}
