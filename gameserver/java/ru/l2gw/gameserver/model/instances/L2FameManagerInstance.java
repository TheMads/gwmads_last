package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: rage
 * Date: 21.05.2009
 * Time: 17:09:45
 */
public class L2FameManagerInstance extends L2NpcInstance
{
	public L2FameManagerInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(!isInRange(player, getInteractDistance(player)))
		{
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			player.sendActionFailed();
			return;
		}

		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken();
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

		if(actualCommand.equalsIgnoreCase("PK_Count"))
		{
			if(player.getFame() >= 5000)
			{
				if(player.getPkKills() > 0)
				{
					player.addFame(-5000);
					player.setPkKills(player.getPkKills() - 1);
					player.sendUserInfo(true);
					html.setFile("data/html/default/" + getNpcId() + "-3.htm");
				}
				else
					html.setFile("data/html/default/" + getNpcId() + "-4.htm");
			}
			else
				html.setFile("data/html/default/" + getNpcId() + "-lowfame.htm");

			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("CRP"))
		{
			if(player.getFame() >= 1000 && player.getClassId().level() >= 2 && player.getClanId() != 0 && player.getClan().getLevel() >= 5)
			{
				player.addFame(-1000);
				player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_ACQUIRED_50_CLANS_FAME_POINTS).addNumber(50));
				player.getClan().incReputation(50, false, "FameToCRP");

				html.setFile("data/html/default/" + getNpcId() + "-5.htm");
			}
			else
				html.setFile("data/html/default/" + getNpcId() + "-lowfame.htm");

			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
		}
		else
			super.onBypassFeedback(player, command);
	}
}
