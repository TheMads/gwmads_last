package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.Castle;
import ru.l2gw.gameserver.model.entity.Fortress;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ClanTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

/*
 * @author: rage
 */
public class L2SuspiciousMerchantInstance extends L2NpcInstance
{
	private SiegeUnit _fortress;
	private static final String _path = "data/html/fortress/merchant/";

	public L2SuspiciousMerchantInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
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
		if(command.startsWith("fortress"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			int val = 0;

			if(st.hasMoreTokens())
				val = Integer.parseInt(st.nextToken());

			switch(val)
			{
				case 1: // Register to a fortress siege
					L2Clan clan = null;
					if(player.getClanId() == 0 || (clan = player.getClan()).getLevel() < 4)
					{
						showChatWindow(player, 1, "b");
						return;
					}

					if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
					{
						showChatWindow(player, 1, "a");
						return;
					}

					if(clan.getHasUnit(2))
					{
						if(_fortress.getOwnerId() == 0)
						{
							showChatWindow(player, 1, "c");
							return;
						}

						if(ResidenceManager.getInstance().getBuildingById(clan.getHasCastle()).isParent(_fortress.getId()))
						{
							if(_fortress.getContractCastleId() == clan.getHasCastle())
							{
								showChatWindow(player, 1, "h");
								return;
							}
						}
						else
						{
							showChatWindow(player, 1, "c");
							return;
						}
					}

					if(_fortress.getOwnerId() == player.getClanId())
					{
						showChatWindow(player, 1, "d");
						return;
					}

					if(TerritoryWarManager.getWar().isInProgress() || TerritoryWarManager.getWar().getWarDate().getTimeInMillis() - 2 * 60 * 60000 < System.currentTimeMillis())
					{
						showChatWindow(player, 1, "i");
						return;
					}

					for(Fortress fort : ResidenceManager.getInstance().getFortressList())
						if(fort.getSiege().checkIsClanRegistered(player.getClanId()))
						{
							showChatWindow(player, 1, "e");
							return;
						}

					for(Castle castle : ResidenceManager.getInstance().getCastleList())
						if(castle.getSiege().checkIsClanRegistered(player.getClanId()) && castle.getSiege().getSiegeDate().get(Calendar.DAY_OF_MONTH) == Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
						{
							showChatWindow(player, 1, "e");
							return;
						}

					if(_fortress.getOwnerId() > 0)
					{
						if(_fortress.getMinLeftForRebel() <= _fortress.getRebelTime())
						{
							showChatWindow(player, 1, "g");
							return;
						}

						for(Castle castle : ResidenceManager.getInstance().getCastleList())
							if(castle.getSiege().checkIsClanRegistered(_fortress.getOwnerId()) && castle.getSiege().getSiegeDate().getTimeInMillis() - System.currentTimeMillis() < 3 * 60 * 60000)
							{
								showChatWindow(player, 1, "i");
								return;
							}
					}


					if(_fortress.getSiege().getAttackerClans().size() < 1 && !player.reduceAdena("FortressSiege", 250000, this, true))
					{
						showChatWindow(player, 1, "f");
						return;
					}

					_fortress.getSiege().getDatabase().saveSiegeClan(clan, 1, false);
					_fortress.getSiege().correctSiegeDateTime();
					_fortress.getSiege().startAutoTask();

					for(L2Player member : clan.getOnlineMembers(""))
						if(member != null)
							member.sendPacket(new SystemMessage(SystemMessage.YOUR_CLAN_HAS_BEEN_REGISTERED_TO_S1S_FORTRESS_BATTLE).addHideoutName(_fortress));

					showChatWindow(player, 1, null);
					
					break;

				case 2: // Cancel registration
					if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
					{
						showChatWindow(player, 1, "a");
						return;
					}

					if(!_fortress.getSiege().checkIsClanRegistered(player.getClanId()))
					{
						showChatWindow(player, 2, "a");
						return;
					}

					_fortress.getSiege().removeSiegeClan(player.getClanId());
					showChatWindow(player, 2, null);
					
					break;
			}
		}
		else
			super.onBypassFeedback(player, command);
	}

	private void showChatWindow(L2Player player, int val, String suffix)
	{
		showChatWindow(player, val, suffix, null);
	}

	private void showChatWindow(L2Player player, int val, String suffix, List<String> replaces)
	{
		player.setLastNpc(this);
		String filename = _path + "merchant-fort-";
		filename += suffix != null ? val + suffix + ".htm" : val + ".htm";
		NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
		html.setFile(filename);

		if(replaces != null)
			for(int i = 0; i < replaces.size(); i += 2)
				html.replace(replaces.get(i), Matcher.quoteReplacement(replaces.get(i + 1)));

		player.setLastNpc(this);
		player.sendPacket(html);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String filename = _path;

		if(val == 0)
		{
			if(_fortress.getOwnerId() > 0)
				filename += "merchant-0a.htm";
			else
				filename += "merchant-0b.htm";
		}
		else
			filename += "merchant-" + val + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);

		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));

		if(val == 0 && _fortress.getOwnerId() > 0)
			html.replace("%clanname%", ClanTable.getInstance().getClan(_fortress.getOwnerId()).getName());

		player.setLastNpc(this);
		player.sendPacket(html);
	}
}