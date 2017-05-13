package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.quest.QuestState;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.serverpackets.PlaySound;
import ru.l2gw.gameserver.serverpackets.SkillList;
import ru.l2gw.gameserver.serverpackets.SocialAction;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.StringTokenizer;

/**
 * @author rage
 * @date 06.07.2010 14:30:09
 */
public class L2TerritoryManagerInstance extends L2NpcInstance
{
	private static final String _path = "data/html/territory/";
	private final int _badgeId;

	public L2TerritoryManagerInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
		_badgeId = TerritoryWarManager.badgesId.get(_territoryId);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken();
		if(actualCommand.equalsIgnoreCase("BuySpecial"))
		{
			if(!st.hasMoreTokens())
				return;

			if(player.getItemCountByItemId(_badgeId) < 1)
			{
				showChatFile(player, "nobadges.htm");
				return;
			}
			super.onBypassFeedback(player, "Multisell " + st.nextToken());
		}
		else if(actualCommand.equalsIgnoreCase("BecomeNoblesse"))
		{
			if(player.getItemCountByItemId(_badgeId) < 1)
			{
				showChatFile(player, "nobadges.htm");
				return;
			}

			if(player.isNoble())
			{
				showChatFile(player, "noblesse-no.htm");
				return;
			}

			if(player.getLevel() < 75)
			{
				showChatFile(player, "noblesse-lowlevel.htm");
				return;
			}

			if(player.destroyItemByItemId("NoblessBuy", _badgeId, 100, this, true))
			{
				for(QuestState qs : player.getAllQuestsStates())
					if(qs.getQuest().getQuestIntId() == 241 || qs.getQuest().getQuestIntId() == 242 || qs.getQuest().getQuestIntId() == 246 || qs.getQuest().getQuestIntId() == 247)
						qs.exitCurrentQuest(false);

				SkillTable.giveNobleSkills(player);
				player.sendPacket(new SkillList(player));
				player.addItem("NoblessBuy", 7694, 1, this, true);
				player.setNoble(true);
				player.sendPacket(new PlaySound("ItemSound.quest_finish"));
				player.broadcastPacket(new SocialAction(player.getObjectId(), SocialAction.SocialType.VICTORY));
				altUseSkill(SkillTable.getInstance().getInfo(4339, 1), player, null);
				showChatFile(player, "noblesse-ok.htm");
			}
			else
				showChatFile(player, "nobadges.htm");
		}
		else if(actualCommand.equalsIgnoreCase("CalcQuest"))
		{
			if(!checkCondition(player))
				return;

			int badges =  (int) player.getVarFloat("tw_badges");
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(_path + "reward.htm");
			html.replace("%objectId%", String.valueOf(getObjectId()));
			html.replace("%npcId%", String.valueOf(getNpcId()));
			html.replace("%territory%", ResidenceManager.getInstance().getCastleById(_territoryId - 80).getName());
			html.replace("%badge%", String.valueOf(badges));
			html.replace("%adena%", String.valueOf(badges / 2 * 100000));
			player.setLastNpc(this);
			player.sendPacket(html);
		}
		else if(actualCommand.equalsIgnoreCase("ReceiveRewards"))
		{
			if(!checkCondition(player))
				return;

			if(TerritoryWarManager.getWar().isInProgress())
				return;

			int badges =  (int) player.getVarFloat("tw_badges");
			player.setBadges(0);
			player.setVar("tw_badges", 0);
			player.addItem("RecieveBadges", _badgeId, badges, this, true);
			player.addAdena("RecieveBadges", badges / 2 * 100000, this, true);
			showChatFile(player, "reward-ok.htm");
		}
		else
			super.onBypassFeedback(player, command);
	}

	private void showChatFile(L2Player player, String file)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(_path + file);

		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		player.setLastNpc(this);

		player.sendPacket(html);
	}

	public void showChatWindow(L2Player player, int val)
	{
		String filename = _path;
		if(player.getLevel() < 40 || player.getClassId().getLevel() < 3)
			filename += "lowlevel.htm";
		else
			filename += getNpcId() + (val != 0 ? "-" + val : "") + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);

		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		player.setLastNpc(this);

		player.sendPacket(html);
	}

	private boolean checkCondition(L2Player player)
	{
		int lastTerrId = player.getVarInt("tw_last");
		if(lastTerrId == 0)
		{
			showChatFile(player, getNpcId() + "-new.htm");
			return false;
		}

		if(TerritoryWarManager.getWar().isInProgress())
		{
			if(player.getTerritoryId() == _territoryId)
				showChatFile(player, getNpcId() + "-tw.htm");
			else
				showChatFile(player, "manager-twenemy.htm");
			return false;
		}

		if(lastTerrId != _territoryId)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			html.setFile(_path + "wrong-territory.htm");
			html.replace("%objectId%", String.valueOf(getObjectId()));
			html.replace("%npcId%", String.valueOf(getNpcId()));
			html.replace("%territory%", ResidenceManager.getInstance().getCastleById(lastTerrId - 80).getName());
			player.setLastNpc(this);
			player.sendPacket(html);
			return false;
		}

		int badges =  (int) player.getVarFloat("tw_badges");
		if(badges < 1)
		{
			showChatFile(player, "badges-given.htm");
			return false;
		}

		return true;
	}
}
