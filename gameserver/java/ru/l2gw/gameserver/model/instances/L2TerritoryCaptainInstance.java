package ru.l2gw.gameserver.model.instances;

import ru.l2gw.extensions.scripts.Functions;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.clientpackets.Say2C;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.serverpackets.ExShowDominionRegistry;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.commons.math.Rnd;

import java.util.StringTokenizer;
import java.util.concurrent.ScheduledFuture;

/**
 * @author rage
 * @date 06.07.2010 15:40:02
 */
public class L2TerritoryCaptainInstance extends L2NpcInstance
{
	private static final String _path = "data/html/territory/";
	private static final int[] MERC_CERTIFICATE_ID = { 13767, 13768 };
	private static final int[] STRIDERS_ID = { 4422, 4423, 4424, 14819 };
	private final int _badgeId;
	private ScheduledFuture<TalkTask> _talkTask;

	public L2TerritoryCaptainInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
		_badgeId = TerritoryWarManager.badgesId.get(_territoryId);
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		if(_talkTask != null)
			_talkTask.cancel(true);
		_talkTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new TalkTask(), Rnd.get(60) * 60000, 60 * 60000);
	}

	@Override
	public void deleteMe()
	{
		super.deleteMe();
		if(_talkTask != null)
			_talkTask.cancel(true);
		_talkTask = null;
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken();
		if(actualCommand.equalsIgnoreCase("TWRegistration"))
		{
			player.setLastNpc(this);
			player.sendPacket(new ExShowDominionRegistry(player, TerritoryWarManager.getTerritoryById(_territoryId)));
		}
		else if(actualCommand.equalsIgnoreCase("BuyMercItems"))
		{
			if(st.countTokens() < 2)
				return;

			int itemId = Integer.parseInt(st.nextToken());

			if(player.getItemCountByItemId(MERC_CERTIFICATE_ID[itemId]) < 1)
			{
				showChatFile(player, "captain-nocert.htm");
				return;
			}

			super.onBypassFeedback(player, "Multisell " + st.nextToken());
		}
		else if(actualCommand.equalsIgnoreCase("BuyStrider"))
		{
			if(!st.hasMoreTokens())
				return;

			int striderId = Integer.parseInt(st.nextToken());

			if(striderId >= STRIDERS_ID.length)
				return;

			int price = striderId < 3 ? 50 : 80;

			if(player.getItemCountByItemId(_badgeId) < price)
			{
				showChatFile(player, "captain-nobadges.htm");
				return;
			}

			if(player.destroyItemByItemId("BuyStrider", _badgeId, price, this, true))
			{
				player.addItem("BuyStrider", STRIDERS_ID[striderId], 1, this, true);
				showChatFile(player, "captain-ok.htm");
			}
			else
				showChatFile(player, "captain-nobadges.htm");
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
		html.replace("%quest_name%", player.getItemCountByItemId(MERC_CERTIFICATE_ID[0]) > 0 ? "_148_PathtoBecominganExaltedMercenary" : "_147_PathtoBecominganEliteMercenary");
		player.setLastNpc(this);

		player.sendPacket(html);
	}

	public void showChatWindow(L2Player player, int val)
	{
		String filename = _path;
		if(TerritoryWarManager.getWar().isInProgress())
			filename += "captain-busy.htm";
		else if(player.getLevel() < 40 || player.getClassId().getLevel() < 3)
			filename += "captain-lowlevel.htm";
		else
			filename += getNpcId() + (val != 0 ? "-" + val : "") + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);

		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		html.replace("%quest_name%", player.getItemCountByItemId(MERC_CERTIFICATE_ID[0]) > 0 ? "_148_PathtoBecominganExaltedMercenary" : "_147_PathtoBecominganEliteMercenary");
		player.setLastNpc(this);

		player.sendPacket(html);
	}

	private class TalkTask implements Runnable
	{
		public void run()
		{
			if(TerritoryWarManager.getWar().isInProgress())
				Functions.npcSay(L2TerritoryCaptainInstance.this, Say2C.SHOUT, 1300165);
			else if(Rnd.chance(50))
				Functions.npcSay(L2TerritoryCaptainInstance.this, Say2C.SHOUT, 1300163);
			else
				Functions.npcSay(L2TerritoryCaptainInstance.this, Say2C.SHOUT, 1300164);
		}
	}

	@Override
	public L2Skill.TargetType getTargetRelation(L2Character target, boolean offensive)
	{
		if(!offensive)
			return L2Skill.TargetType.invalid;

		return super.getTargetRelation(target, offensive);
	}
}
