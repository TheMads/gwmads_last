package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.ZoneManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.StringTokenizer;

import static ru.l2gw.gameserver.model.zone.L2Zone.ZoneType.siege;

public final class L2ObservationInstance extends L2NpcInstance
{
	//private static Log _log = LogFactory.getLog(L2ObservationInstance.class.getName());

	/**
	 * @param template
	 */
	public L2ObservationInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		// first do the common stuff
		// and handle the commands that all NPC classes know
		super.onBypassFeedback(player, command);

		if(command.startsWith("observeSiege"))
		{
			String val = command.substring(13);
			StringTokenizer st = new StringTokenizer(val);
			st.nextToken(); // Bypass cost

			if(ZoneManager.getInstance().isInsideZone(siege, Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken())) != null)
				doObserve(player, val);
			else
				player.sendPacket(new SystemMessage(SystemMessage.OBSERVATION_IS_ONLY_POSSIBLE_DURING_A_SIEGE));
		}
		else if(command.startsWith("observe"))
			doObserve(player, command.substring(8));
	}

	@Override
	public String getHtmlPath(int npcId, int val, int karma)
	{
		String pom = "";
		if(val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		return "data/html/observation/" + pom + ".htm";
	}

	private void doObserve(L2Player player, String val)
	{
		StringTokenizer st = new StringTokenizer(val);
		int cost = Integer.parseInt(st.nextToken());
		int x = Integer.parseInt(st.nextToken());
		int y = Integer.parseInt(st.nextToken());
		int z = Integer.parseInt(st.nextToken());
		if(player.getAdena() < cost)
			player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
		else if(player.enterObserverMode(x, y, z))
			player.reduceAdena("Observ", cost, this, true);

		player.sendActionFailed();
	}
}