package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.controllers.TradeController;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.CastleManorManager;
import ru.l2gw.gameserver.instancemanager.CastleManorManager.SeedProduction;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2TradeList;
import ru.l2gw.gameserver.model.NpcTradeList;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.commons.arrays.GArray;

import java.util.StringTokenizer;

public class L2ManorManagerInstance extends L2MerchantInstance
{
	public L2ManorManagerInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
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
			//MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
			//player.sendPacket(my);
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
				else if(!player.isGM() // Player is not GM
						&& player.getClanId() != 0 // Player have clan
						&& getBuilding(2).getOwnerId() == player.getClanId() // Player's clan owning the castle
						&& player.isClanLeader() // Player is clan leader of clan (then he is the lord)
				)
					showMessageWindow(player, "manager-lord.htm");
				else
					showMessageWindow(player, "manager.htm");
				player.sendActionFailed();
			}
		}
	}

	@Override
	public void showBuyWindow(L2Player player, int val)
	{
		double taxRate = 0;
		player.tempInvetoryDisable();
		NpcTradeList list = TradeController.getInstance().getSellList(val);
		if(list != null)
		{
			list.getTradeItems().get(0).setCount(1);
			player.sendPacket(new ExBuyList(list, player, taxRate));
			player.sendPacket(new ExSellRefundList(player));
		}

		player.sendActionFailed();
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(player.isActionsDisabled() || player.isSitting() || player.getLastNpc().getDistance(player) > 300)
			return;

		if(command.startsWith("manor_menu_select"))
		{ // input string format:
			// manor_menu_select?ask=X&state=Y&time=X
			if(CastleManorManager.getInstance().isUnderMaintenance())
			{
				player.sendActionFailed();
				player.sendPacket(Msg.THE_MANOR_SYSTEM_IS_CURRENTLY_UNDER_MAINTENANCE);
				return;
			}

			String params = command.substring(command.indexOf("?") + 1);
			StringTokenizer st = new StringTokenizer(params, "&");
			int ask = Integer.parseInt(st.nextToken().split("=")[1]);
			int state = Integer.parseInt(st.nextToken().split("=")[1]);
			int time = Integer.parseInt(st.nextToken().split("=")[1]);

			int castleId;
			if(state == -1) // info for current manor
				castleId = getBuilding(2).getId();
			else
				// info for requested manor
				castleId = state;

			switch(ask)
			{ // Main action
				case 1: // Seed purchase
					if(castleId != getBuilding(2).getId())
						player.sendPacket(new SystemMessage(SystemMessage._HERE_YOU_CAN_BUY_ONLY_SEEDS_OF_S1_MANOR));
					else
					{
						L2TradeList tradeList = new L2TradeList(0);
						GArray<SeedProduction> seeds = getCastle().getSeedProduction(CastleManorManager.PERIOD_CURRENT);

						for(SeedProduction s : seeds)
						{
							L2ItemInstance item = ItemTable.getInstance().createDummyItem(s.getId());
							item.setPriceToSell(s.getPrice());
							item.setCount(s.getCanProduce());
							if(item.getCount() > 0 && item.getPriceToSell() > 0)
								tradeList.addItem(item);
						}

						BuyListSeed bl = new BuyListSeed(tradeList, castleId, player.getAdena());
						player.sendPacket(bl);
					}
					break;
				case 2: // Crop sales
					player.sendPacket(new ExShowSellCropList(player, castleId, getCastle().getCropProcure(CastleManorManager.PERIOD_CURRENT)));
					break;
				case 3: // Current seeds (Manor info)
					if(time == 1 && !ResidenceManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
						player.sendPacket(new ExShowSeedInfo(castleId, null));
					else
						player.sendPacket(new ExShowSeedInfo(castleId, ResidenceManager.getInstance().getCastleById(castleId).getSeedProduction(time)));
					break;
				case 4: // Current crops (Manor info)
					if(time == 1 && !ResidenceManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
						player.sendPacket(new ExShowCropInfo(castleId, null));
					else
						player.sendPacket(new ExShowCropInfo(castleId, ResidenceManager.getInstance().getCastleById(castleId).getCropProcure(time)));
					break;
				case 5: // Basic info (Manor info)
					player.sendPacket(new ExShowManorDefaultInfo());
					break;
				case 6: // Buy harvester
					showBuyWindow(player, Integer.parseInt("3" + getNpcId()));
					break;
				case 9: // Edit sales (Crop sales)
					player.sendPacket(new ExShowProcureCropDetail(state));
					break;
			}
		}
		else if(command.startsWith("help"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // discard first
			String filename = "manor_client_help00" + st.nextToken() + ".htm";
			showMessageWindow(player, filename);
		}
		else
			super.onBypassFeedback(player, command);
	}

	public String getHtmlPath()
	{
		return "data/html/manormanager/";
	}

	@Override
	public String getHtmlPath(int npcId, int val, int karma)
	{
		return "data/html/manormanager/manager.htm"; // Used only in parent method
		// to return from "Territory status"
		// to initial screen.
	}

	private void showMessageWindow(L2Player player, String filename)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(getHtmlPath() + filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
}
