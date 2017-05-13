package ru.l2gw.gameserver.model.instances;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.instancemanager.AuctionManager;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.instancemanager.TownManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.Auction;
import ru.l2gw.gameserver.model.entity.ClanHall;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.MapRegionTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.StringTokenizer;

public final class L2AuctioneerInstance extends L2NpcInstance
{
	private static Log _logClanHall = LogFactory.getLog("clanhall");
	private static SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy");

	public L2AuctioneerInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(!isInRange(player, getInteractDistance(player)))
			return;

		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command

		String val = "";
		if(st.countTokens() >= 1)
			val = st.nextToken();

		if(actualCommand.equalsIgnoreCase("list"))
		{
			String items = "<table width=270 border=0>";
			FastList<Auction> auctions = AuctionManager.getInstance().getAuctions();
			int page = val.length() > 0 ? Integer.parseInt(val) : 1;
			int nPg = 0;

			if(auctions.size() > 0)
			{
				nPg = page + 1;
				int s = auctions.size();
				int end = page * 10;

				if(end >= s)
				{
					nPg = page;
					end = s;
				}

				int start = (page - 1) * 10;

				for(int i=start; i < end; i++)
				{
					Auction a = auctions.get(i);

					items += "<tr><td width=50 align=left>" + a.getClanHall().getLocation() + "</td>" +
							"<td width=100 align=left><a action=\"bypass -h npc_" + getObjectId() + "_agitinfo " + a.getClanHallId() + " " + page + "\">" + a.getClanName() + "</a></td>" +
							"<td width=50 align=left>" + format.format(a.getEndDate().getTimeInMillis()) + "</td>" +
							"<td width=70 align=right>" + a.getMaxBid() + "</td></tr>";
				}
			}

			if(page > 1)
				items += "<tr><td width=50 align=left><a action=\"bypass -h npc_" + getObjectId() + "_list " + (page - 1) +"\">&$1037;</a></td><td></td><td></td>";
			else
				items += "<tr><td width=50 align=left></td><td></td><td></td>";

			if(nPg != page)
				items += "<td width=70 align=right><a action=\"bypass -h npc_" + getObjectId() + "_list " + (page + 1) + "\">&$1038;</a></td></tr></table>";
			else
				items += "<td></td></tr></table>";


			String filename = "data/html/auction/auction-list.htm";

			NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
			html.setFile(filename);
			html.replace("%AGIT_LIST%", items);
			html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_Chat 0");
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("agitinfo"))
		{
			if(val.equals(""))
				return;

			try
			{
				int auctionId = Integer.parseInt(val);

				Auction a = AuctionManager.getInstance().getAuction(auctionId);

				if(a == null)
				{
					player.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_NO_OFFERINGS_I_OWN_OR_I_MADE_A_BID_FOR));
					return;
				}

				String filename = "data/html/auction/";
				L2Clan clan = player.getClan();
				int t = 0;
				if(clan != null && clan.getAuctionBiddedAt() == auctionId && a.isBidder(clan.getClanId()))
				{
					t = 1;
					filename += "auction-bidinfo.htm";
				}
				else if(clan != null && clan.getHasHideout() == auctionId)
				{
					t = 2;
					filename += "auction-saleinfo.htm";
				}
				else
					filename += "auction-info.htm";

				ClanHall ch = a.getClanHall();
				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile(filename);
				html.replace("%AGIT_NAME%", ch.getName());
				html.replace("%OWNER_PLEDGE_NAME%", a.getClanName());
				html.replace("%OWNER_PLEDGE_MASTER%", a.getClanLeaderName());
				html.replace("%AGIT_SIZE%", ch.getGrade() + "0 ");
				html.replace("%AGIT_LEASE%", String.valueOf(ch.getLease()));
				html.replace("%AGIT_LOCATION%", ch.getLocation());
				html.replace("%AGIT_AUCTION_END_YY%", String.valueOf(a.getEndDate().get(Calendar.YEAR)));
				html.replace("%AGIT_AUCTION_END_MM%", String.format("%02d", a.getEndDate().get(Calendar.MONTH) + 1));
				html.replace("%AGIT_AUCTION_END_DD%", String.valueOf(a.getEndDate().get(Calendar.DAY_OF_MONTH)));
				html.replace("%AGIT_AUCTION_END_HH%", String.valueOf(a.getEndDate().get(Calendar.HOUR_OF_DAY)));
				html.replace("%AGIT_AUCTION_REMAIN%", String.valueOf((a.getEndDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / 3600000) + " hours " + String.valueOf(((a.getEndDate().getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / 60000 % 60)) + " minutes");
				html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getMaxBid()));
				html.replace("%AGIT_AUCTION_COUNT%", String.valueOf(a.getBidders().size()));
				html.replace("%AGIT_AUCTION_DESC%", a.getDescription());

				String p = st.hasMoreTokens() ? " " + st.nextToken() : "";

				if(t == 1)
				{
					html.replace("%AGIT_AUCTION_MYBID%", String.valueOf(a.getBidder(clan.getClanId()).getBid()));
					html.replace("%AGIT_LINK_CANCELBID%", "bypass -h npc_" + getObjectId() + "_cancelBid " + p);
				}
				else if(t == 2)
					html.replace("%AGIT_LINK_CANCEL%", "bypass -h npc_" + getObjectId() + "_cancelAuction " + p);

				html.replace("%AGIT_LINK_BIDLIST%", "bypass -h npc_" + getObjectId() + "_bidlist " + a.getClanHallId() + p);
				html.replace("%AGIT_LINK_RE%", "bypass -h npc_" + getObjectId() + "_bid1 " + a.getClanHallId() + p);

				if(!p.isEmpty())
					html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_list" + p);
				else
					html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_Chat 0");

				player.sendPacket(html);
			}
			catch(Exception e)
			{
				player.sendMessage("Invalid auction!");
				e.printStackTrace();
			}
		}
		else if(actualCommand.equalsIgnoreCase("clanhall"))
		{
			L2Clan clan = player.getClan();
			if(clan != null && clan.getHasUnit(1))
			{
				String filename = "data/html/auction/auction-agitinfo.htm";
				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile(filename);
				ClanHall ch = ResidenceManager.getInstance().getClanHallById(clan.getHasHideout());
				html.replace("%AGIT_NAME%", ch.getName());
				html.replace("%OWNER_PLEDGE_NAME%", clan.getName());
				html.replace("%OWNER_PLEDGE_MASTER%", clan.getLeaderName());
				html.replace("%AGIT_SIZE%", ch.getGrade() + "0 ");
				html.replace("%AGIT_LEASE%", String.valueOf(ch.getLease()));
				html.replace("%AGIT_LOCATION%", ch.getLocation());
				player.sendPacket(html);
			}
		}
		else if(actualCommand.equalsIgnoreCase("location"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
			html.setFile("data/html/auction/location.htm");
			html.replace("%location%", TownManager.getInstance().getClosestTownName(player));
			html.replace("%LOCATION%", getPictureName(player));
			html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_Chat 0");
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("bidlist"))
		{
			int auctionId = Integer.parseInt(val);
			Auction a = AuctionManager.getInstance().getAuction(auctionId);

			if(a == null)
			{
				player.sendPacket(new SystemMessage(SystemMessage.THE_CLAN_DOES_NOT_OWN_A_CLAN_HALL));
				return;
			}

			String bidders = "";

			for(Auction.Bidder b : a.getBidders())
				bidders += "<tr><td>" + a.getClanName() + "</td><td>" + b.getClanName() + "</td><td>" + format.format(b.getBidTime()) + "</td></tr>";

			String filename = "data/html/auction/auction-bidders.htm";
			NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
			html.setFile(filename);
			html.replace("%AGIT_LIST%", bidders);
			html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_agitinfo " + auctionId + (st.hasMoreTokens() ? " " + st.nextToken() : ""));
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("bid1"))
		{
			if(val.equals(""))
				return;

			L2Clan clan = player.getClan();

			if(clan == null || clan.getLevel() < 2 || !isHaveRigths(player, L2Clan.CP_CH_AUCTION))
			{
				player.sendPacket(new SystemMessage(SystemMessage.ONLY_A_CLAN_LEADER_WHOSE_CLAN_IS_OF_LEVEL_2_OR_HIGHER_IS_ALLOWED_TO_PARTICIPATE_IN_A_CLAN_HALL_AUCTION));
				return;
			}

			if(clan.getHasHideout() > 0)
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_PARTICIPATE_IN_AN_AUCTION));
				return;
			}

			Auction a = AuctionManager.getInstance().getAuction(Integer.parseInt(val));

			if(a == null)
			{
				player.sendPacket(new SystemMessage(SystemMessage.THE_CLAN_DOES_NOT_OWN_A_CLAN_HALL));
				return;
			}

			if(clan.getAuctionBiddedAt() > 0 && a.getClanHallId() != clan.getAuctionBiddedAt())
			{
				player.sendPacket(new SystemMessage(SystemMessage.SINCE_YOU_HAVE_ALREADY_SUBMITTED_A_BID_YOU_ARE_NOT_ALLOWED_TO_PARTICIPATE_IN_ANOTHER_AUCTION_AT_THIS_TIME));
				return;
			}

			try
			{
				L2ItemInstance adena = clan.getWarehouse().getItemByItemId(57);

				String filename = "data/html/auction/auction-bid1.htm";
				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile(filename);
				String back = st.hasMoreTokens() ? " " + st.nextToken() : "";
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_agitinfo " + val + back);
				html.replace("%PLEDGE_ADENA%", String.valueOf(adena == null ? 0 : adena.getCount()));
				//if(clan.getAuctionBiddedAt() > 0)
				//	html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getBidder(clan.getClanId()).getBid()));
				//else
				html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(a.getMaxBid()));
				html.replace("%AGIT_ID%", val);
				html.replace("%BACK_ID%", back);
				player.sendPacket(html);
			}
			catch(Exception e)
			{
				player.sendMessage("Invalid auction!");
				e.printStackTrace();
			}
		}
		else if(actualCommand.equalsIgnoreCase("bid2"))
		{
			if(val.equals(""))
				return;

			L2Clan clan = player.getClan();

			if(clan == null || clan.getLevel() < 2 || !isHaveRigths(player, L2Clan.CP_CH_AUCTION))
			{
				player.sendPacket(new SystemMessage(SystemMessage.ONLY_A_CLAN_LEADER_WHOSE_CLAN_IS_OF_LEVEL_2_OR_HIGHER_IS_ALLOWED_TO_PARTICIPATE_IN_A_CLAN_HALL_AUCTION));
				return;
			}

			if(clan.getHasHideout() > 0)
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_PARTICIPATE_IN_AN_AUCTION));
				return;
			}

			Auction a = AuctionManager.getInstance().getAuction(Integer.parseInt(val));

			if(a == null)
			{
				player.sendPacket(new SystemMessage(SystemMessage.THE_CLAN_DOES_NOT_OWN_A_CLAN_HALL));
				return;
			}

			if(clan.getAuctionBiddedAt() > 0 && a.getClanHallId() != clan.getAuctionBiddedAt())
			{
				player.sendPacket(new SystemMessage(SystemMessage.SINCE_YOU_HAVE_ALREADY_SUBMITTED_A_BID_YOU_ARE_NOT_ALLOWED_TO_PARTICIPATE_IN_ANOTHER_AUCTION_AT_THIS_TIME));
				return;
			}

			try
			{
				long minimumBid = clan.getAuctionBiddedAt() > 0 ? a.getBidder(clan.getClanId()).getBid() : a.getStartBid();
				long bid = 0;

				try
				{
					if(st.hasMoreTokens())
						bid = Long.parseLong(st.nextToken());
				}
				catch(NumberFormatException e)
				{
				}

				if(bid < minimumBid)
				{
					player.sendPacket(new SystemMessage(SystemMessage.YOUR_BID_PRICE_MUST_BE_HIGHER_THAN_THE_MINIMUM_PRICE_THAT_CAN_BE_BID));
					return;
				}

				String filename = "data/html/auction/auction-bid2.htm";
				String back = st.hasMoreTokens() ? " " + st.nextToken() : "";

				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile(filename);
				html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(minimumBid));
				html.replace("%AGIT_AUCTION_BID%", String.valueOf(bid));
				html.replace("%AGIT_AUCTION_END_YY%", String.valueOf(a.getEndDate().get(Calendar.YEAR)));
				html.replace("%AGIT_AUCTION_END_MM%", String.format("%02d", a.getEndDate().get(Calendar.MONTH) + 1));
				html.replace("%AGIT_AUCTION_END_DD%", String.valueOf(a.getEndDate().get(Calendar.DAY_OF_MONTH)));
				html.replace("%AGIT_AUCTION_END_HH%", String.valueOf(a.getEndDate().get(Calendar.HOUR_OF_DAY)));
				html.replace("%AGIT_ID%", val);
				html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_bid1 " + val + back);
				player.sendPacket(html);
			}
			catch(Exception e)
			{
				player.sendMessage("Invalid auction!");
				e.printStackTrace();
			}
		}
		else if(actualCommand.equals("bid3"))
		{
			if(val.equals(""))
				return;

			L2Clan clan = player.getClan();

			if(clan == null || clan.getLevel() < 2 || !isHaveRigths(player, L2Clan.CP_CH_AUCTION))
			{
				player.sendPacket(new SystemMessage(SystemMessage.ONLY_A_CLAN_LEADER_WHOSE_CLAN_IS_OF_LEVEL_2_OR_HIGHER_IS_ALLOWED_TO_PARTICIPATE_IN_A_CLAN_HALL_AUCTION));
				return;
			}

			if(clan.getHasHideout() > 0)
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_PARTICIPATE_IN_AN_AUCTION));
				return;
			}

			Auction a = AuctionManager.getInstance().getAuction(Integer.parseInt(val));

			if(a == null)
			{
				player.sendPacket(new SystemMessage(SystemMessage.THE_CLAN_DOES_NOT_OWN_A_CLAN_HALL));
				return;
			}

			if(clan.getAuctionBiddedAt() > 0 && a.getClanHallId() != clan.getAuctionBiddedAt())
			{
				player.sendPacket(new SystemMessage(SystemMessage.SINCE_YOU_HAVE_ALREADY_SUBMITTED_A_BID_YOU_ARE_NOT_ALLOWED_TO_PARTICIPATE_IN_ANOTHER_AUCTION_AT_THIS_TIME));
				return;
			}

			try
			{
				long bid = 0;
				long adena = clan.getWarehouse().getItemByItemId(57) == null ? 0 : clan.getWarehouse().getItemByItemId(57).getCount();

				try
				{
					if(st.hasMoreTokens())
						bid = Long.parseLong(st.nextToken());
				}
				catch(NumberFormatException e)
				{
				}

				long minimumBid = clan.getAuctionBiddedAt() > 0 ? a.getBidder(clan.getClanId()).getBid() : a.getStartBid();

				if(bid <= minimumBid)
				{
					player.sendPacket(new SystemMessage(SystemMessage.YOUR_BID_PRICE_MUST_BE_HIGHER_THAN_THE_MINIMUM_PRICE_THAT_CAN_BE_BID));
					return;
				}

				long needAdena = clan.getAuctionBiddedAt() > 0 ? bid - minimumBid : bid;

				if(adena < needAdena)
				{
					player.sendPacket(new SystemMessage(SystemMessage.THERE_IS_NOT_ENOUGH_ADENA_IN_THE_CLAN_HALL_WAREHOUSE));
					return;
				}

				Auction.Bidder b = a.getBidder(clan.getClanId());

				if(b != null)
				{
					clan.getWarehouse().destroyItemByItemId("Rebid", 57, needAdena, player, this);
					b.updateBid(needAdena);
					b.store();
					_logClanHall.info(a.getClanHall() + ": " + player + " clan: " + clan.getName() + " rebid: " + bid);
				}
				else
				{
					clan.getWarehouse().destroyItemByItemId("SetBid", 57, bid, player, this);
					a.addBidder(clan, bid);
					_logClanHall.info(a.getClanHall() + ": " + player + " clan: " + clan.getName() + " bid: " + bid);
				}
				player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_SUBMITTED_A_BID_IN_THE_AUCTION_OF_S1).addHideoutName(a.getClanHallId()));
			}
			catch(Exception e)
			{
				player.sendMessage("Invalid auction!");
				e.printStackTrace();
			}
		}
		else if(actualCommand.equalsIgnoreCase("cancelBid"))
		{
			L2Clan clan = player.getClan();
			if(clan == null || clan.getLevel() < 2 || !isHaveRigths(player, L2Clan.CP_CH_AUCTION))
			{
				player.sendPacket(new SystemMessage(SystemMessage.ONLY_A_CLAN_LEADER_WHOSE_CLAN_IS_OF_LEVEL_2_OR_HIGHER_IS_ALLOWED_TO_PARTICIPATE_IN_A_CLAN_HALL_AUCTION));
				return;
			}

			Auction a = AuctionManager.getInstance().getAuction(clan.getAuctionBiddedAt());

			if(a == null || a.getEndDate().getTimeInMillis() < System.currentTimeMillis())
			{
				player.sendPacket(new SystemMessage(SystemMessage.THE_CLAN_DOES_NOT_OWN_A_CLAN_HALL));
				return;
			}

			Auction.Bidder b = a.getBidder(clan.getClanId());

			if(b == null)
				return;

			NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
			html.setFile("data/html/auction/auction-bidcancel.htm");
			html.replace("%AGIT_BID%", String.valueOf(b.getBid()));
			html.replace("%AGIT_BID_REMAIN%", String.valueOf((long) (b.getBid() * 0.90)));
			html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_agitinfo " + a.getClanHallId() + (st.hasMoreTokens() ? " " + st.nextToken() : ""));
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("doCancelBid"))
		{
			L2Clan clan = player.getClan();
			if(clan == null || clan.getLevel() < 2 || !isHaveRigths(player, L2Clan.CP_CH_AUCTION))
			{
				player.sendPacket(new SystemMessage(SystemMessage.ONLY_A_CLAN_LEADER_WHOSE_CLAN_IS_OF_LEVEL_2_OR_HIGHER_IS_ALLOWED_TO_PARTICIPATE_IN_A_CLAN_HALL_AUCTION));
				return;
			}

			Auction a = AuctionManager.getInstance().getAuction(clan.getAuctionBiddedAt());

			if(a == null || a.getEndDate().getTimeInMillis() < System.currentTimeMillis() || !a.isBidder(clan.getClanId()))
			{
				player.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_NO_OFFERINGS_I_OWN_OR_I_MADE_A_BID_FOR));
				return;
			}

			Auction.Bidder b = a.getBidder(clan.getClanId());
			if(b != null)
			{
				long retBid = (long) (b.getBid() * 0.90);
				_logClanHall.info(a.getClanHall() + ": " + player + " clan: " + clan.getName() + " cancel bid, return bid to CWH: " + retBid);
				a.removeBidder(clan.getClanId());
				clan.setAuctionBiddedAt(0, System.currentTimeMillis());
				clan.getWarehouse().addItem("OutBid", 57, retBid, player, this);
				player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_CANCELED_YOUR_BID));
			}
		}
		else if(actualCommand.equalsIgnoreCase("sale1"))
		{
			L2Clan clan = player.getClan();
			if(clan == null || clan.getLevel() < 2 || !isHaveRigths(player, L2Clan.CP_CH_AUCTION))
			{
				player.sendPacket(new SystemMessage(SystemMessage.ONLY_A_CLAN_LEADER_WHOSE_CLAN_IS_OF_LEVEL_2_OR_HIGHER_IS_ALLOWED_TO_PARTICIPATE_IN_A_CLAN_HALL_AUCTION));
				return;
			}

			if(!clan.getHasUnit(1))
			{
				player.sendPacket(new SystemMessage(SystemMessage.THE_CLAN_DOES_NOT_OWN_A_CLAN_HALL));
				return;
			}

			if(clan.getAuctionCancelTime() + Config.CH_AUCTION_CANCEL_PENALTY > System.currentTimeMillis())
			{
				player.sendPacket(new SystemMessage(SystemMessage.IT_HAS_NOT_YET_BEEN_SEVEN_DAYS_SINCE_CANCELING_AN_AUCTION));
				return;
			}

			ClanHall ch = ResidenceManager.getInstance().getClanHallById(clan.getHasHideout());

			if(AuctionManager.getInstance().getAuction(clan.getHasHideout()) != null || ch == null)
			{
				showChatWindow(player, 0);
				return;
			}

			NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
			html.setFile("data/html/auction/auction-sale1.htm");
			html.replace("%AGIT_DEPOSIT%", String.valueOf((int) (ch.getPrice() * 0.50)));
			html.replace("%AGIT_PLEDGE_ADENA%", String.valueOf(clan.getWarehouse().getItemByItemId(57) == null ? 0 : clan.getWarehouse().getItemByItemId(57).getCount()));
			html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_clanhall");
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("sale2"))
		{
			L2Clan clan = player.getClan();
			if(clan == null || clan.getLevel() < 2 || !isHaveRigths(player, L2Clan.CP_CH_AUCTION))
			{
				player.sendPacket(new SystemMessage(SystemMessage.ONLY_A_CLAN_LEADER_WHOSE_CLAN_IS_OF_LEVEL_2_OR_HIGHER_IS_ALLOWED_TO_PARTICIPATE_IN_A_CLAN_HALL_AUCTION));
				return;
			}

			if(!clan.getHasUnit(1))
			{
				player.sendPacket(new SystemMessage(SystemMessage.THE_CLAN_DOES_NOT_OWN_A_CLAN_HALL));
				return;
			}

			if(clan.getAuctionCancelTime() + Config.CH_AUCTION_CANCEL_PENALTY > System.currentTimeMillis())
			{
				player.sendPacket(new SystemMessage(SystemMessage.IT_HAS_NOT_YET_BEEN_SEVEN_DAYS_SINCE_CANCELING_AN_AUCTION));
				return;
			}

			ClanHall ch = ResidenceManager.getInstance().getClanHallById(clan.getHasHideout());

			if(AuctionManager.getInstance().getAuction(clan.getHasHideout()) != null || ch == null)
			{
				showChatWindow(player, 0);
				return;
			}

			long deposit = (long)(ch.getPrice() * 0.50);
			long adena = clan.getWarehouse().getItemByItemId(57) == null ? 0 : clan.getWarehouse().getItemByItemId(57).getCount();

			if(adena < deposit)
			{
				player.sendPacket(new SystemMessage(SystemMessage.THERE_IS_NOT_ENOUGH_ADENA_IN_THE_CLAN_HALL_WAREHOUSE));
				return;
			}

			NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
			html.setFile("data/html/auction/auction-sale2.htm");
			html.replace("%AGIT_LAST_PRICE%", String.valueOf(ch.getLastPrice()));
			html.replace("%AGIT_AUCTION_MIN%", String.valueOf(ch.getPrice()));
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("sale3"))
		{
			L2Clan clan = player.getClan();
			if(clan == null || clan.getLevel() < 2 || !isHaveRigths(player, L2Clan.CP_CH_AUCTION))
			{
				player.sendPacket(new SystemMessage(SystemMessage.ONLY_A_CLAN_LEADER_WHOSE_CLAN_IS_OF_LEVEL_2_OR_HIGHER_IS_ALLOWED_TO_PARTICIPATE_IN_A_CLAN_HALL_AUCTION));
				return;
			}

			if(!clan.getHasUnit(1))
			{
				player.sendPacket(new SystemMessage(SystemMessage.THE_CLAN_DOES_NOT_OWN_A_CLAN_HALL));
				return;
			}

			if(clan.getAuctionCancelTime() + Config.CH_AUCTION_CANCEL_PENALTY > System.currentTimeMillis())
			{
				player.sendPacket(new SystemMessage(SystemMessage.IT_HAS_NOT_YET_BEEN_SEVEN_DAYS_SINCE_CANCELING_AN_AUCTION));
				return;
			}

			ClanHall ch = ResidenceManager.getInstance().getClanHallById(clan.getHasHideout());

			if(AuctionManager.getInstance().getAuction(clan.getHasHideout()) != null || ch == null)
			{
				showChatWindow(player, 0);
				return;
			}

			long deposit = (long)(ch.getPrice() * 0.50);
			long adena = clan.getWarehouse().getItemByItemId(57) == null ? 0 : clan.getWarehouse().getItemByItemId(57).getCount();
			int days = 0;
			long price = 0;
			try
			{
				days = Integer.parseInt(val);
				st.nextToken();
				if(st.hasMoreTokens())
					price = Long.parseLong(st.nextToken());
			}
			catch(NumberFormatException e)
			{
			}

			if(days < 1 || days > 7)
			{
				showChatWindow(player, 0);
				return;
			}

			String desc = "";
			while(st.hasMoreTokens())
				desc += st.nextToken() + " ";

			if(!desc.isEmpty())
			{
				desc = desc.replace("&", "");
				desc = desc.replace("<", "");
				desc = desc.replace(">", "");
				desc = desc.replace("\\", "");
				desc = desc.replace("'", "");
				desc = desc.replace("\"", "");
			}

			if(adena < deposit)
			{
				player.sendPacket(new SystemMessage(SystemMessage.THERE_IS_NOT_ENOUGH_ADENA_IN_THE_CLAN_HALL_WAREHOUSE));
				return;
			}

			if(price < deposit)
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOUR_BID_PRICE_MUST_BE_HIGHER_THAN_THE_MINIMUM_PRICE_THAT_CAN_BE_BID));
				return;
			}

			Calendar endDate = Calendar.getInstance();
			endDate.add(Calendar.DAY_OF_MONTH, days);

			NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
			html.setFile("data/html/auction/auction-sale3.htm");
			html.replace("%AGIT_AUCTION_MIN%", String.valueOf(ch.getPrice()));
			html.replace("%AGIT_AUCTION_MINBID%", String.valueOf(price));
			html.replace("%x%", String.valueOf(days));
			html.replace("%AGIT_AUCTION_END_YY%", String.valueOf(endDate.get(Calendar.YEAR)));
			html.replace("%AGIT_AUCTION_END_MM%", String.format("%02d", endDate.get(Calendar.MONTH) + 1));
			html.replace("%AGIT_AUCTION_END_DD%", String.valueOf(endDate.get(Calendar.DAY_OF_MONTH)));
			html.replace("%AGIT_AUCTION_END_HH%", String.valueOf(endDate.get(Calendar.HOUR_OF_DAY)));
			html.replace("%AGIT_AUCTION_DESC%", desc);
			html.replace("%AGIT_LINK_PARAM%", days + " " + price + " " + desc);

			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("doSale"))
		{
			L2Clan clan = player.getClan();
			if(clan == null || clan.getLevel() < 2 || !isHaveRigths(player, L2Clan.CP_CH_AUCTION))
			{
				player.sendPacket(new SystemMessage(SystemMessage.ONLY_A_CLAN_LEADER_WHOSE_CLAN_IS_OF_LEVEL_2_OR_HIGHER_IS_ALLOWED_TO_PARTICIPATE_IN_A_CLAN_HALL_AUCTION));
				return;
			}

			if(!clan.getHasUnit(1))
			{
				player.sendPacket(new SystemMessage(SystemMessage.THE_CLAN_DOES_NOT_OWN_A_CLAN_HALL));
				return;
			}

			if(clan.getAuctionCancelTime() + Config.CH_AUCTION_CANCEL_PENALTY > System.currentTimeMillis())
			{
				player.sendPacket(new SystemMessage(SystemMessage.IT_HAS_NOT_YET_BEEN_SEVEN_DAYS_SINCE_CANCELING_AN_AUCTION));
				return;
			}

			ClanHall ch = ResidenceManager.getInstance().getClanHallById(clan.getHasHideout());

			if(AuctionManager.getInstance().getAuction(clan.getHasHideout()) != null || ch == null)
			{
				showChatWindow(player, 0);
				return;
			}

			long deposit = (long)(ch.getPrice() * 0.50);
			long adena = clan.getWarehouse().getItemByItemId(57) == null ? 0 : clan.getWarehouse().getItemByItemId(57).getCount();
			int days = 0;
			long price = 0;
			try
			{
				days = Integer.parseInt(val);
				if(st.hasMoreTokens())
					price = Long.parseLong(st.nextToken());
			}
			catch(NumberFormatException e)
			{
			}

			if(days < 1 || days > 7)
			{
				showChatWindow(player, 0);
				return;
			}

			String desc = "";
			while(st.hasMoreTokens())
				desc += st.nextToken() + " ";

			if(!desc.isEmpty())
			{
				desc = desc.replace("&", "");
				desc = desc.replace("<", "");
				desc = desc.replace(">", "");
				desc = desc.replace("\\", "");
				desc = desc.replace("'", "");
				desc = desc.replace("\"", "");
			}

			if(adena < deposit)
			{
				player.sendPacket(new SystemMessage(SystemMessage.THERE_IS_NOT_ENOUGH_ADENA_IN_THE_CLAN_HALL_WAREHOUSE));
				return;
			}

			if(price < ch.getPrice())
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOUR_BID_PRICE_MUST_BE_HIGHER_THAN_THE_MINIMUM_PRICE_THAT_CAN_BE_BID));
				return;
			}

			clan.getWarehouse().destroyItemByItemId("AuctionDeposit", 57, deposit, player, this);
			_logClanHall.info(ch + ": " + player + " create auction, price: " + price + " days: " + days + " deposit: " + deposit);
			AuctionManager.getInstance().addToAuction(ch, clan, price, days, desc);
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_REGISTERED_FOR_A_CLAN_HALL_AUCTION));
		}
		else if(actualCommand.equalsIgnoreCase("cancelAuction"))
		{
			L2Clan clan = player.getClan();
			if(clan == null || clan.getLevel() < 2 || !isHaveRigths(player, L2Clan.CP_CH_AUCTION))
			{
				player.sendPacket(new SystemMessage(SystemMessage.ONLY_A_CLAN_LEADER_WHOSE_CLAN_IS_OF_LEVEL_2_OR_HIGHER_IS_ALLOWED_TO_PARTICIPATE_IN_A_CLAN_HALL_AUCTION));
				return;
			}

			if(!clan.getHasUnit(1))
			{
				player.sendPacket(new SystemMessage(SystemMessage.THE_CLAN_DOES_NOT_OWN_A_CLAN_HALL));
				return;
			}

			Auction a = AuctionManager.getInstance().getAuction(clan.getHasHideout());
			if(a == null)
			{
				player.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_NO_CLAN_HALLS_UP_FOR_AUCTION));
				return;
			}

			NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
			html.setFile("data/html/auction/auction-salecancel.htm");
			html.replace("%AGIT_DEPOSIT%", String.valueOf(a.getDeposit()));
			html.replace("%AGIT_LINK_BACK%", "bypass -h npc_" + getObjectId() + "_agitinfo " + a.getClanHallId() + (st.hasMoreTokens() ? " " + st.nextToken() : ""));
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("doCancelAuction"))
		{
			L2Clan clan = player.getClan();
			if(clan == null || clan.getLevel() < 2 || !isHaveRigths(player, L2Clan.CP_CH_AUCTION))
			{
				player.sendPacket(new SystemMessage(SystemMessage.ONLY_A_CLAN_LEADER_WHOSE_CLAN_IS_OF_LEVEL_2_OR_HIGHER_IS_ALLOWED_TO_PARTICIPATE_IN_A_CLAN_HALL_AUCTION));
				return;
			}

			if(!clan.getHasUnit(1))
			{
				player.sendPacket(new SystemMessage(SystemMessage.THE_CLAN_DOES_NOT_OWN_A_CLAN_HALL));
				return;
			}

			Auction a = AuctionManager.getInstance().getAuction(clan.getHasHideout());
			if(a == null)
			{
				player.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_NO_CLAN_HALLS_UP_FOR_AUCTION));
				return;
			}

			_logClanHall.info(a + ": " + player + " canceled auction.");
			clan.setAuctionBiddedAt(0, System.currentTimeMillis());
			AuctionManager.getInstance().removeAuction(a.getClanHall());
		}
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String filename;
		if(val == 0)
			filename = "data/html/auction/auction.htm";
		else
			filename = "data/html/auction/auction-" + val + ".htm";
		NpcHtmlMessage html = new NpcHtmlMessage(player, this, filename, val);

		L2Clan clan = player.getClan();

		if(clan == null || (clan.getAuctionBiddedAt() == 0 && !clan.getHasUnit(1)))
			html.replace("%AGIT_SELECTED_LINK%", "");
		else if(player.getClan().getAuctionBiddedAt() > 0)
		{
			if(AuctionManager.getInstance().getAuction(clan.getAuctionBiddedAt()) != null && AuctionManager.getInstance().getAuction(clan.getAuctionBiddedAt()).isBidder(clan.getClanId()))
				html.replace("%AGIT_SELECTED_LINK%", "bypass -h npc_" + getObjectId() + "_agitinfo " + clan.getAuctionBiddedAt());
			else
			{
				player.getClan().setAuctionBiddedAt(0, 0);
				html.replace("%AGIT_SELECTED_LINK%", "");
			}
		}
		else if(clan.getHasUnit(1))
		{
			if(AuctionManager.getInstance().getAuction(clan.getHasHideout()) != null)
				html.replace("%AGIT_SELECTED_LINK%", "bypass -h npc_" + getObjectId() + "_agitinfo " + clan.getHasHideout());
			else
				html.replace("%AGIT_SELECTED_LINK%", "bypass -h npc_" + getObjectId() + "_clanhall");
		}

		player.sendPacket(html);
	}

	private String getPictureName(L2Player player)
	{
		int nearestTownId = MapRegionTable.getInstance().getMapRegion(player.getX(), player.getY());
		String nearestTown;

		switch(nearestTownId)
		{
			case 6:
				nearestTown = "GLUDIO";
				break;
			case 7:
				nearestTown = "GLUDIN";
				break;
			case 8:
				nearestTown = "DION";
				break;
			case 9:
				nearestTown = "GIRAN";
				break;
			case 14:
				nearestTown = "RUNE";
				break;
			case 15:
				nearestTown = "GODARD";
				break;
			case 16:
				nearestTown = "SCHUTTGART";
				break;
			default:
				nearestTown = "ADEN";
				break;
		}
		return nearestTown;
	}
}