package ru.l2gw.gameserver.model.instances;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.base.ClassId;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.util.Files;
import ru.l2gw.util.Util;

import java.io.File;
import java.util.StringTokenizer;

public final class L2ClassMasterInstance extends L2MerchantInstance
{
	private static Log _log = LogFactory.getLog(L2ClassMasterInstance.class.getName());

	public L2ClassMasterInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
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
		File file = new File(Config.DATAPACK_ROOT, "data/html/custom/" + pom + ".htm");
		if(file.exists())
			return "data/html/custom/" + pom + ".htm";
		return "data/html/default/" + pom + ".htm";
	}

	private String makeMessage(L2Player player)
	{
		ClassId classId = player.getClassId();

		int jobLevel = classId.getLevel();
		int level = player.getLevel();

		StringBuilder html = new StringBuilder();
		if(Config.ALLOW_CLASS_MASTERS_LIST.isEmpty() || !Config.ALLOW_CLASS_MASTERS_LIST.contains(jobLevel))
			jobLevel = 4;
		if((level >= 20 && jobLevel == 1 || level >= 40 && jobLevel == 2 || level >= 76 && jobLevel == 3) && Config.ALLOW_CLASS_MASTERS_LIST.contains(jobLevel))
		{
			L2Item item = ItemTable.getInstance().getTemplate(Config.CLASS_MASTERS_PRICE_ITEM);
			if(Config.CLASS_MASTERS_PRICE_LIST[jobLevel] > 0)
				html.append("Price: ").append(Util.formatAdena(Config.CLASS_MASTERS_PRICE_LIST[jobLevel])).append(" ").append(item.getName()).append("<br1>");
			for(ClassId cid : ClassId.values())
			{
				// Инспектор является наследником trooper и warder, но сменить его как профессию нельзя,
				// т.к. это сабкласс. Наследуется с целью получения скилов родителей.
				if(cid == ClassId.inspector)
					continue;
				if(cid.childOf(classId) && cid.getLevel() == classId.getLevel() + 1)
					html.append("<a action=\"bypass -h npc_").append(getObjectId()).append("_change_class ").append(cid.getId()).append(" ").append(Config.CLASS_MASTERS_PRICE_LIST[jobLevel]).append("\">").append(cid.name()).append("</a><br1>");
			}
			player.sendPacket(new NpcHtmlMessage(_objectId).setHtml(html.toString()));
		}
		else
			switch(jobLevel)
			{
				case 1:
					html.append("Come back here when you reached level 20 to change your class.");
					break;
				case 2:
					html.append("Come back here when you reached level 40 to change your class.");
					break;
				case 3:
					html.append("Come back here when you reached level 76 to change your class.");
					break;
				case 4:
					html.append("There is no class changes for you any more.");
					break;
			}
		return html.toString();
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		int npcId = getTemplate().npcId;
		NpcHtmlMessage msg;
		if(Config.ALT_CLASSMASTER_INSTALLED)
		{
			val = 1;
			msg = new NpcHtmlMessage(player, this, null, val);
		}
		else
			msg = new NpcHtmlMessage(player, this, null, null);
		String filename = getHtmlPath(npcId, val, player.getKarma());
		String html = Files.read(filename, player);
		if(Config.EVENT_ClassmastersSellsSS)
			html += "<br><a action=\"bypass -h npc_%objectId%_Buy 318601\">Buy Soul/Spiritshots</a>";
		if(Config.EVENT_ClassmastersCoLShop)
			html += "<br><a action=\"bypass -h npc_%objectId%_Multisell 1\">Special shop</a>";
		if(Config.SERVICES_CHANGE_NICK_ENABLED)
			html += "<br><a action=\"bypass -h scripts_services.Rename:rename_page\">Nick change</a>";
		if(Config.SERVICES_NOBLESS_SELL_ENABLED && !player.isNoble())
			html += "<br><a action=\"bypass -h scripts_services.NoblessSell:get\">Become a Nobless</a>, Price:" + Config.SERVICES_NOBLESS_SELL_PRICE + " " + ItemTable.getInstance().getTemplate(Config.SERVICES_NOBLESS_SELL_ITEM).getName();
		if(Config.SERVICES_HOW_TO_GET_COL)
			html += "<br><a action=\"bypass -h scripts_services.RateBonus:howtogetcol\">How to get Coin of Luck</a>";

		msg.setHtml(html);
		msg.replace("%classmaster%", makeMessage(player));
		player.sendPacket(msg);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		StringTokenizer st = new StringTokenizer(command);
		String actualCommand = st.nextToken(); // Get actual command
		if(actualCommand.equals("change_class"))
		{
			short val = Short.parseShort(st.nextToken());
			int price = Integer.parseInt(st.nextToken());
			if(Config.CLASS_MASTERS_PRICE_ITEM == 57)
			{
				if(player.reduceAdena("ChangeClass", price, this, true))
					changeClass(player, val);
			}
			else if(player.destroyItemByItemId("ChangeClass", Config.CLASS_MASTERS_PRICE_ITEM, price, this, true))
				changeClass(player, val);
		}
		else if(actualCommand.equalsIgnoreCase("Wear"))
			return;
		else if(actualCommand.equalsIgnoreCase("Multisell"))
			return;
		else
			super.onBypassFeedback(player, command);
	}

	private void changeClass(L2Player player, short val)
	{
		if(Config.DEBUG)
		{
			_log.debug("Changing class to ClassId:" + val);
			_log.debug("name:" + player.getName());
			_log.debug("level:" + player.getLevel());
			_log.debug("classId:" + player.getClassId());
		}

		if(player.getClassId().getLevel() == 3)
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_COMPLETED_THE_QUEST_FOR_3RD_OCCUPATION_CHANGE_AND_MOVED_TO_ANOTHER_CLASS_CONGRATULATIONS)); // для 3 профы
		else
			player.sendPacket(new SystemMessage(SystemMessage.CONGRATULATIONS_YOU_HAVE_TRANSFERRED_TO_A_NEW_CLASS)); // для 1 и 2 профы

		player.setClassId(val, false);
		if(player.getClassId() == ClassId.cardinal)
			player.addItem("ClassChange", 15307, 1, this, true);
		else if(player.getClassId() == ClassId.evaSaint)
			player.addItem("ClassChange", 15308, 1, this, true);
		else if(player.getClassId() == ClassId.shillienSaint)
			player.addItem("ClassChange", 15309, 4, this, true);
		player.broadcastUserInfo(true);
	}
}
