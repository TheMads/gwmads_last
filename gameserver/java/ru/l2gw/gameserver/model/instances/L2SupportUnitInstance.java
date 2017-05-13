package ru.l2gw.gameserver.model.instances;

import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.StringTokenizer;

/**
 * @author rage
 * @date 01.07.2009 13:54:56
 */
public class L2SupportUnitInstance extends L2NpcInstance
{
	private SiegeUnit _fortress;
	private static final int KNIGHT_EPOLET = 9912;
	
	public L2SupportUnitInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();

		_fortress = getBuilding(1);
		if(_fortress == null)
			_log.warn("Warning: " + this + " has no fortress!");
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		int cond = validateCondition(player);
		if(cond == Cond_Owner)
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			String actualCommand = st.nextToken();

			if(actualCommand.equalsIgnoreCase("talisman"))
			{
				if(!player.isQuestContinuationPossible(false))
				{
					player.sendPacket(new SystemMessage(SystemMessage.YOUR_INVENTORY_IS_FULL));
					return;
				}

				L2ItemInstance epolets = player.getInventory().getItemByItemId(KNIGHT_EPOLET);

				if(epolets == null || epolets.getCount() < 10)
				{
					showChatWindow(player, 3);
					return;
				}

				if(player.destroyItemByItemId("talisman", KNIGHT_EPOLET, 10, this, true))
				{
					int cat = Rnd.get(6);
					int talismanId;

					if(cat == 0)
						talismanId = Rnd.get(9914, 9922);
					else if(cat == 1)
						talismanId = Rnd.get(9924, 9949);
					else if(cat == 2)
						talismanId = Rnd.get(9952, 9966);
					else if(cat == 3)
						talismanId = Rnd.get(10416, 10424);
					else if(cat == 4)
						talismanId = Rnd.get(10518, 10519);
					else
						talismanId = Rnd.get(10533, 10543);

					player.addItem("talisman", talismanId, 1, this, true);
					showChatWindow(player, 4);
				}
			}
			else if(command.equalsIgnoreCase("subPledgeSkills"))
			{
				if(player.isClanLeader())
					showClanSubPledgeSkillList(player);
				else
					showChatWindow(player, 2);
			}
			else
				super.onBypassFeedback(player, command);
		}
		else
			showChatWindow(player, 0);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String filename;

		if(val == 0)
		{
			int cond = validateCondition(player);
			if(cond == Cond_Busy_Because_Of_Siege)
				filename = "data/html/fortress/supportunit/supportunit-busy.htm";
			else if(cond == Cond_Owner)
				filename = "data/html/fortress/supportunit/supportunit.htm";
			else
				filename = "data/html/fortress/supportunit/supportunit-no.htm";
		}
		else
			filename = "data/html/fortress/supportunit/supportunit-" + val + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);

		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		player.setLastNpc(this);
		
		player.sendPacket(html);
	}
}
