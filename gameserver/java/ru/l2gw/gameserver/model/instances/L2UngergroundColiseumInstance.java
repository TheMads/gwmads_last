package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2Multisell;
import ru.l2gw.gameserver.model.L2Party;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.Coliseum;
import ru.l2gw.gameserver.model.zone.L2Zone.ZoneType;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.StringTokenizer;

/**
 * This class need for Teleporter and Registrator of Ungerground Coliseum
 *@author FlareDrakon l2f
 */
public class L2UngergroundColiseumInstance extends L2NpcInstance
{

	public L2UngergroundColiseumInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
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

		return "data/html/Coliseum/" + pom + ".htm";
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		player.sendActionFailed();

		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command
		String filename = getHtmlPath(getNpcId(), 1, player.getKarma());

		super.onBypassFeedback(player, command);

		if(actualCommand.startsWith("register"))
		{
			//Eсли группы нет неможем региться
			if(player.getParty() == null)
				filename = getHtmlPath(getNpcId(), 2, player.getKarma());
			//Eсли мы не лидер то тож неможем
			else if(!player.getParty().isLeader(player))
				filename = getHtmlPath(getNpcId(), 3, player.getKarma());
			//Eсли наша группа меньше 7ми человек неможем
			else if(player.getParty().getMemberCount() < 7)
				filename = getHtmlPath(getNpcId(), 4, player.getKarma());
			else
			{
				//зарегить нас в очередь
				Coliseum.register(player);
				return;
			}
		}
		if(actualCommand.startsWith("teleport"))
		{
			if(!Coliseum.getOponentList().contains(player))
				//НЕможем телепортировать потому что незарегестрированы
				filename = getHtmlPath(getNpcId(), 2, player.getKarma());
			if(Coliseum.getPartysList().size() < 1)
				filename = getHtmlPath(getNpcId(), 1, player.getKarma());
			else if(Coliseum.getOponentList().size() > 0)
				order: for(L2Player pl : Coliseum.getOponentList())
				{
					for(L2Party p : Coliseum.getPartysList())
					{
						if(p.containsMember(player) && p.containsMember(pl))
							continue order;
					}
					if(pl.getLevel() < 50 && player.getLevel() < 50)
						//Coliseum.teleportPlayers(player.getParty(),pl.getParty(),Coliseum.getColiseumByPlayerLevel());
						return;
					else if(pl.getLevel() < 60 && player.getLevel() < 60)
						//Coliseum.teleportPlayers(player.getParty(),pl.getParty(),Coliseum.getColiseumByPlayerLevel());
						return;
					else if(pl.getLevel() < 70 && player.getLevel() < 70)
						//Coliseum.teleportPlayers(player.getParty(),pl.getParty(),Coliseum.getColiseumByPlayerLevel());
						return;
					else if(pl.getLevel() >= 70 && player.getLevel() >= 70)
						//Coliseum.teleportPlayers(player.getParty(),pl.getParty(),Coliseum.getColiseumByPlayerLevel());
						return;
					else
						//НЕможем телепортироваться так как в очереди нет группы с подходящим лвл
						filename = getHtmlPath(getNpcId(), 2, player.getKarma());
				}
		}
		else if(command.startsWith("Multisell") || command.startsWith("multisell"))
		{
			String listId = command.substring(9).trim();
			player.setLastMultisellNpc(player.getLastNpc());
			L2Multisell.getInstance().SeparateAndSend(Integer.parseInt(listId), player, (!Config.SERVICES_OFFSHORE_NO_CASTLE_TAX && !isInZone(ZoneType.offshore)) ? getCastle().getTaxRate() : 0);
		}
		showChatWindow(player, filename); // выдать html-ку с ответом
	}

}
