package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: rage
 * @date: 07.09.11 19:20
 */
public class RequestLinkHtml extends L2GameClientPacket
{
	private String link;
	private static final Pattern p = Pattern.compile("#(\\d+)$");

	//Format: cS
	@Override
	public void readImpl()
	{
		link = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null || link == null || link.isEmpty())
			return;

		try
		{
			int index = Integer.parseInt(link.substring(1), 16);
			if(player.getStoredLinks().size() > index)
			{
				link = player.getStoredLinks().get(index).replace("..", "").replace("//", "").replace("\\\\", "").replace(" ", "");
				Matcher m = p.matcher(link);
				if(m.find())
				{
					link = link.replace("#" + m.group(1), "");
					int itemId = Integer.parseInt(m.group(1));
					if(player.getItemCountByItemId(itemId) > 0)
					{
						link = "data/html/default/" + link;
						NpcHtmlMessage html = new NpcHtmlMessage(0);
						html.setFile(link);
						player.sendPacket(html);
					}

					return;
				}
				else if(link.contains("/"))
					link = "data/html/" + link;
				else
					link = "data/html/default/" + link;

				if(player.getLastNpc() == null || !player.isInRange(player.getLastNpc(), player.getInteractDistance(player.getLastNpc())))
					return;

				NpcHtmlMessage html = new NpcHtmlMessage(player, player.getLastNpc());
				html.setFile(link);
				player.sendPacket(html);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}