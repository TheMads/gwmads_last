package ru.l2gw.gameserver.model.instances;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.TradeController;
import ru.l2gw.gameserver.model.L2Multisell;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.NpcTradeList;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.model.zone.L2Zone.ZoneType;
import ru.l2gw.gameserver.serverpackets.ShopPreviewList;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.io.File;
import java.util.StringTokenizer;

@SuppressWarnings( { "nls", "unqualified-field-access", "boxing" })
public class L2MerchantInstance extends L2NpcInstance
{
	protected static Log _log = LogFactory.getLog(L2MerchantInstance.class.getName());

	public L2MerchantInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public String getHtmlPath(int npcId, int val, int karma)
	{
		String pom;
		if(val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		String temp = "data/html/merchant/" + pom + ".htm";
		File mainText = new File(temp);
		if(mainText.exists())
			return temp;

		if(karma > 0)
		{
			temp = "data/html/teleporter/" + pom + "-pk.htm";
			mainText = new File(temp);
			if(mainText.exists())
				return temp;
		}

		temp = "data/html/teleporter/" + pom + ".htm";
		mainText = new File(temp);
		if(mainText.exists())
			return temp;

		temp = "data/html/petmanager/" + pom + ".htm";
		mainText = new File(temp);
		if(mainText.exists())
			return temp;

		return "data/html/teleporter/" + pom + ".htm";

	}

	private void showWearWindow(L2Player player, int val)
	{
		if(AdminTemplateManager.checkBoolean("noShop", player))
			return;

		player.tempInvetoryDisable();
		if(Config.DEBUG)
			_log.debug("Showing wearlist");
		NpcTradeList list = TradeController.getInstance().getSellList(val);

		if(list != null)
			player.sendPacket(new ShopPreviewList(list, player.getAdena(), player.expertiseIndex));
		else
		{
			_log.warn("no buylist with id:" + val);
			player.sendActionFailed();
		}
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command

		if(actualCommand.equalsIgnoreCase("Buy"))
		{
			if(st.countTokens() < 1)
				return;
			int val = Integer.parseInt(st.nextToken());
			showBuyWindow(player, val);
		}
		else if(actualCommand.equalsIgnoreCase("Wear"))
		{
			if(st.countTokens() < 1)
				return;
			int val = Integer.parseInt(st.nextToken());
			showWearWindow(player, val);
		}
		else if(actualCommand.equalsIgnoreCase("Multisell"))
		{
			if(st.countTokens() < 1)
				return;
			int val = Integer.parseInt(st.nextToken());
			player.setLastMultisellNpc(player.getLastNpc());
			L2Multisell.getInstance().SeparateAndSend(val, player, (!Config.SERVICES_OFFSHORE_NO_CASTLE_TAX && !isInZone(ZoneType.offshore)) ? getCastle().getTaxRate() : 0);
		}
		else
			super.onBypassFeedback(player, command);
	}
}
