package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.StringTokenizer;

/**
 * @author rage
 * @date 27.06.2009 20:05:28
 */
public class L2FortressManagerInstance extends L2ClanBaseManagerInstance
{
	private SiegeUnit _fortress;

	public L2FortressManagerInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
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
		int condition = validateCondition(player);
		if(condition <= Cond_All_False)
			return;
		else if(condition > Cond_Busy_Because_Of_Siege)
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			String actualCommand = st.nextToken(); // Get actual command
			String path = getHtmlPath();

			if(actualCommand.equalsIgnoreCase("report"))
			{
				int min = _fortress.getMinLeftForRebel();
				int h = min / 60;
				int m = min % 60;

				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				if(System.currentTimeMillis() - _fortress.getLastSiegeDate()  < 60 * 60000 || _fortress.getContractCastleId() == 0)
				{
					html.setFile(path + "report-ind.htm");
					html.replace("%siegeDate%", h + " hours " + m + " minutes");
				}
				else
				{
					html.setFile(path + "report.htm");
					html.replace("%siegeDate%", h + " hours " + m + " minutes");
					html.replace("%castle%", ResidenceManager.getInstance().getBuildingById(_fortress.getContractCastleId()).getName());
					html.replace("%status%", "active");
					html.replace("%taxamount%", String.valueOf(_fortress.getTaxAmount()));
					//min = (int)((_fortress.getLastTaxTime() + 21600000 - System.currentTimeMillis()) / 60000);
					min = _fortress.getMinLeftForTax();
					h = min / 60;
					m = min % 60;
					html.replace("%payDate%", h + " hours " + m + " minutes");
				}

				sendHtmlMessage(player, html);
			}
			else
				super.onBypassFeedback(player, command);
		}
		else
			super.onBypassFeedback(player, command);
	}

	protected String getHtmlPath()
	{
		return "data/html/fortress/manager/";
	}

	protected String getManagePath()
	{
		return "";
	}

	@Override
	protected boolean canSetFunctions(L2Player player)
	{
		return (player.getClanPrivileges() & L2Clan.CP_CS_SET_FUNCTIONS) == L2Clan.CP_CS_SET_FUNCTIONS;
	}

	@Override
	protected boolean canUseFunctions(L2Player player)
	{
		return (player.getClanPrivileges() & L2Clan.CP_CS_USE_FUNCTIONS) == L2Clan.CP_CS_USE_FUNCTIONS;
	}

	@Override
	protected boolean canUseDoors(L2Player player)
	{
		return (player.getClanPrivileges() & L2Clan.CP_CS_OPEN_DOOR) == L2Clan.CP_CS_OPEN_DOOR;
	}

	@Override
	protected boolean canDismiss(L2Player player)
	{
		return (player.getClanPrivileges() & L2Clan.CP_CS_DISMISS) == L2Clan.CP_CS_DISMISS;
	}
}
