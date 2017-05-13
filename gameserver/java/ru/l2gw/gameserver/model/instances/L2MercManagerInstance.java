package ru.l2gw.gameserver.model.instances;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.TradeController;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.NpcTradeList;
import ru.l2gw.gameserver.model.entity.SevenSigns;
import ru.l2gw.gameserver.serverpackets.ExBuyList;
import ru.l2gw.gameserver.serverpackets.ExSellRefundList;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.StringTokenizer;

public final class L2MercManagerInstance extends L2NpcInstance
{
	private static Log _log = LogFactory.getLog(L2MercManagerInstance.class.getName());

	private static int Cond_All_False = 0;
	private static int Cond_Busy_Because_Of_Siege = 1;
	private static int Cond_Owner = 2;

	public L2MercManagerInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		player.sendActionFailed();

		if(!isInRange(player, getInteractDistance(player)))
			return;

		int condition = validateCondition(player);
		if(condition <= Cond_All_False || condition == Cond_Busy_Because_Of_Siege)
			return;

		if(condition == Cond_Owner)
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			String actualCommand = st.nextToken(); // Get actual command

			String val = "";
			if(st.countTokens() >= 1)
				val = st.nextToken();

			if(actualCommand.equalsIgnoreCase("hire"))
			{
				if(val.equals(""))
					return;

				showBuyWindow(player, Integer.parseInt(val));
			}
			else
				super.onBypassFeedback(player, command);
		}
	}

	@Override
	public void showBuyWindow(L2Player player, int val)
	{
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

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String filename = "data/html/mercmanager/mercmanager-no.htm";
		int condition = validateCondition(player);
		if(condition == Cond_Busy_Because_Of_Siege)
			filename = "data/html/mercmanager/mercmanager-busy.htm"; // Busy because of siege
		else if(condition == Cond_Owner)
			if(SevenSigns.getInstance().getCurrentPeriod() == SevenSigns.PERIOD_SEAL_VALIDATION)
			{
				if(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN)
					filename = "data/html/mercmanager/mercmanager_dawn.htm";
				else if(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
					filename = "data/html/mercmanager/mercmanager_dusk.htm";
				else
					filename = "data/html/mercmanager/mercmanager.htm";
			}
			else
				filename = "data/html/mercmanager/mercmanager_nohire.htm";
		player.sendPacket(new NpcHtmlMessage(player, this, filename, val));
	}

	@Override
	protected int validateCondition(L2Player player)
	{
		if(getBuilding(2).getId() > 0)
			if(player.getClanId() != 0)
				if(getBuilding(2).getSiege().isInProgress())
					return Cond_Busy_Because_Of_Siege; // Busy because of siege
				else if(getBuilding(2).getOwnerId() == player.getClanId() // Clan owns castle
						&& (player.getClanPrivileges() & L2Clan.CP_CS_MERCENARIES) == L2Clan.CP_CS_MERCENARIES) // has merc rights
					return Cond_Owner; // Owner

		return Cond_All_False;
	}
}