package ru.l2gw.gameserver.model.instances;

import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.tables.ClanTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.util.Location;

public class L2CastleMagicianInstance extends L2NpcInstance
{
	protected static final int COND_ALL_FALSE = 0;
	protected static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	protected static final int COND_OWNER = 2;
	private static final int KNIGHT_EPOLET = 9912;

	public L2CastleMagicianInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		player.sendActionFailed();
		String filename = "data/html/castle/magician/magician-no.htm";

		int condition = validateCondition(player);
		if(condition > COND_ALL_FALSE)
		{
			if(condition == COND_BUSY_BECAUSE_OF_SIEGE)
				filename = "data/html/castle/magician/magician-busy.htm"; // Busy because of siege
			else if(condition == COND_OWNER)									// Clan owns castle
			{
				if(val == 0)
					filename = "data/html/castle/magician/" + getNpcId() + ".htm";
				else
					filename = "data/html/castle/magician/" + getNpcId() + "-" + val + ".htm";
			}
		}

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", String.valueOf(getName() + " " + getTitle()));
		player.setLastNpc(this);

		player.sendPacket(html);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(command.equalsIgnoreCase("ClanGate"))
		{
			L2Clan clan = ClanTable.getInstance().getClan(getBuilding(-1).getOwnerId());
			if(clan != null)
			{
				L2Player cl = clan.getLeader().getPlayer();
				if(cl != null && cl.isStatActive(Stats.CLAN_GATE))
				{
					Location dst = Location.coordsRandomize(cl, 50);
					dst = GeoEngine.moveCheck(cl.getX(), cl.getY(), cl.getZ(), dst.getX(), dst.getY(), cl.getReflection());
					player.teleToLocation(dst);
				}
				else
				{
					if(cl == null)
						_log.warn("ClanGate: clan leader is null " + getBuilding(-1).getName());

					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile("data/html/castle/magician/magician-nocg.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%npcname%", String.valueOf(getName() + " " + getTitle()));
					player.sendPacket(html);
				}
			}
			else
				_log.warn("ClanGate: no clan " + getBuilding(-1).getName() + " player " + player);
		}
		if(command.equalsIgnoreCase("talisman"))
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
				int cat = Rnd.get(5);
				int talismanId = 0;

				if(cat == 0)
					talismanId = Rnd.get(9914, 9922);
				else if(cat == 1)
					talismanId = Rnd.get(9924, 9965);
				else if(cat == 2)
					talismanId = Rnd.get(10416, 10424);
				else if(cat == 3)
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

	protected int validateCondition(L2Player player)
	{
		if(getBuilding(2).getId() > 0)
		{
			if(player.getClanId() != 0)
			{
				if(getBuilding(2).getSiege().isInProgress())
					return COND_BUSY_BECAUSE_OF_SIEGE;				   // Busy because of siege
				else if(getBuilding(2).getOwnerId() == player.getClanId()) // Clan owns castle
					return COND_OWNER;
			}
		}
		return COND_ALL_FALSE;
	}
}