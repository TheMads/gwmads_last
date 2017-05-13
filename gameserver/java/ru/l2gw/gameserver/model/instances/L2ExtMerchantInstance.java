package ru.l2gw.gameserver.model.instances;

import javolution.text.TextBuilder;
import ru.l2gw.gameserver.Config;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.base.Race;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ColorNameTable;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.StringTokenizer;

public final class L2ExtMerchantInstance extends L2MerchantInstance
{
	private static final String UPDATE_CHAR_SEX = "UPDATE characters SET sex = ? WHERE obj_id = ?";

	public L2ExtMerchantInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	public void onBypassFeedback(L2Player player, String command)
	{

		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command

		if(actualCommand.equalsIgnoreCase("ChangeNameColor"))
		{
			if(st.countTokens() < 1)
			{
				ShowChangeNameColor(player);
			}
			else
			{
				int val = Integer.parseInt(st.nextToken());

				if(ColorNameTable.getInstance().getColorById(val) == null)
				{
					_log.warn("ChangeNameColor: no color found id: " + val + " " + player);
					player.sendPacket(Msg.ActionFail);
					return;
				}

				if(player.destroyItemByItemId("ChangeColorName", ColorNameTable.getInstance().getColorById(val).getItemId(), ColorNameTable.getInstance().getColorById(val).getCount(), null, true))
				{
					player.setNameColor(ColorNameTable.getInstance().getColorById(val).getColor());
					player.setVar("namecolor", Integer.toHexString(ColorNameTable.getInstance().getColorById(val).getColor()));
					player.broadcastUserInfo();
				}
				else player.sendPacket(Msg.ActionFail);

				ShowChangeNameColor(player);
			}
		}
		if(actualCommand.equalsIgnoreCase("ChangeTitleColor"))
		{
			if(st.countTokens() < 1)
			{
				ShowChaneTitleColor(player);
			}
			else
			{
				int val = Integer.parseInt(st.nextToken());

				if(ColorNameTable.getInstance().getTitleColorById(val) == null)
				{
					_log.warn("ChangeTitleColor: no color found id: " + val + " " + player);
					player.sendPacket(Msg.ActionFail);
					return;
				}

				if(player.destroyItemByItemId("ChangeColorTitle", ColorNameTable.getInstance().getTitleColorById(val).getItemId(), ColorNameTable.getInstance().getTitleColorById(val).getCount(), null, true))
				{
					player.setTitleColor(ColorNameTable.getInstance().getTitleColorById(val).getColor());
					player.setVar("titlecolor", Integer.toHexString(ColorNameTable.getInstance().getTitleColorById(val).getColor()));
					player.broadcastUserInfo();
				}
				else player.sendPacket(Msg.ActionFail);

				ShowChaneTitleColor(player);
			}
		}
		if(actualCommand.equalsIgnoreCase("ChangeSex"))
		{
			if(player.getRace() == Race.kamael)
			{
				player.sendPacket(Msg.ActionFail);
				showChatWindow(player, 0);
				return;
			}

			if(player.destroyItemByItemId("ChangeSex", 4037, Config.GWS_SEX_CHANGE_COST, null, true))
			{
				Connection con = null;
				try
				{
					con = DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement(UPDATE_CHAR_SEX);
					statement.setInt(1, player.getSex() == 0 ? 1 : 0);
					statement.setInt(2, player.getObjectId());
					statement.execute();
					statement.close();
				}
				catch(Exception e)
				{
					_log.warn("ChangeNameColor: " + e);
				}
				finally
				{
					try
					{
						con.close();
						player.logout(false, false, true);
					}
					catch(Exception e)
					{
					}
				}
			}
			else
			{
				player.sendPacket(Msg.ActionFail);
				showChatWindow(player, 0);
			}
		}
		else if(actualCommand.equalsIgnoreCase("RecMe"))
		{
			if(player.destroyItemByItemId("RecByAkim", 4037, Config.GWS_REC_COST, null, true))
			{
				player.getRecSystem().setRecommendsHave(player.getRecSystem().getRecommendsHave() + Config.GWS_REC_AMOUNT);
				player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_BEEN_RECOMMENDED).addString(getName()));
				player.broadcastUserInfo();
			}
			else
			{
				player.sendPacket(Msg.ActionFail);
			}
			showChatWindow(player, 0);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}


	private void ShowChangeNameColor(L2Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

		html.setFile("data/html/merchant/namecolor.htm");

		TextBuilder html1 = new TextBuilder("<table width=270 border=0>");
		html1.append("<tr><td>Цвет</td><td><center>Стоймость</center></td></tr>");
		for(Integer i : ColorNameTable.getInstance().getColorsIds())
		{
			String colorStr = ColorNameTable.getInstance().getColorById(i).getColorStr();
			String rgb = colorStr.substring(4, 6) + colorStr.substring(2, 4) + colorStr.substring(0, 2);
			html1.append("<tr>");
			html1.append("<td><a action=\"bypass -h npc_" + getObjectId() + "_ChangeNameColor " + i + "\"><font color=\"" + rgb + "\">" + ColorNameTable.getInstance().getColorById(i).getName() + "</font></a></td>");
			html1.append("<td><center>" + ColorNameTable.getInstance().getColorById(i).getCount() + " (" + ItemTable.getInstance().getTemplate(ColorNameTable.getInstance().getColorById(i).getItemId()).getName() + ")</center></td>");
			html1.append("</tr>");
		}
		html1.append("</table>");

		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%name%", getName());
		html.replace("%colortable%", html1.toString());

		player.sendPacket(html);
	}

	private void ShowChaneTitleColor(L2Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

		html.setFile("data/html/merchant/titlecolor.htm");

		TextBuilder html1 = new TextBuilder("<table width=270 border=0>");
		html1.append("<tr><td>Цвет</td><td><center>Стоймость</center></td></tr>");
		for(Integer i : ColorNameTable.getInstance().getTitleColorsIds())
		{
			String colorStr = ColorNameTable.getInstance().getTitleColorById(i).getColorStr();
			String rgb = colorStr.substring(4, 6) + colorStr.substring(2, 4) + colorStr.substring(0, 2);
			html1.append("<tr>");
			html1.append("<td><a action=\"bypass -h npc_" + getObjectId() + "_ChangeTitleColor " + i + "\"><font color=\"" + rgb + "\">" + ColorNameTable.getInstance().getTitleColorById(i).getName() + "</font></a></td>");
			html1.append("<td><center>" + ColorNameTable.getInstance().getTitleColorById(i).getCount() + " (" + ItemTable.getInstance().getTemplate(ColorNameTable.getInstance().getTitleColorById(i).getItemId()).getName() + ")</center></td>");
			html1.append("</tr>");
		}
		html1.append("</table>");

		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%name%", getName());
		html.replace("%colortable%", html1.toString());

		player.sendPacket(html);
	}

}