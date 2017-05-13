package ru.l2gw.gameserver.model.instances;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.tables.ClanTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.StringTokenizer;

public class L2DoormenInstance extends L2NpcInstance
{

	private static int Cond_All_False = 0;
	private static int Cond_Busy_Because_Of_Siege = 1;
	private static int Cond_Owner = 2;
	private String _path;

	protected static Log _log = LogFactory.getLog(L2DoormenInstance.class.getName());

	public L2DoormenInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();

		if(getBuilding(-1).isClanHall)
			_path = "data/html/doormen/clanhall/";
		else if(getBuilding(-1).isFort)
			_path = "data/html/doormen/fortress/";
		else
			_path = "data/html/doormen/";
	}

	protected int validateCondition(L2Player player)
	{
		SiegeUnit su = getBuilding(-1);
		if(su == null)
			return Cond_All_False;

		if(su.isCastle && TerritoryWarManager.getWar().isInProgress() && player.getTerritoryId() == su.getId() + 80)
			return Cond_Busy_Because_Of_Siege;

		if(player.getClanId() != 0)
		{
			if((su.getSiege() != null && su.getSiege().isInProgress()))
			{
				if(su.getOwnerId() == player.getClanId() || su.getSiege().checkIsDefender(player.getClanId()))
					return Cond_Busy_Because_Of_Siege;
			}
			else if(su.getOwnerId() == player.getClanId())
				return Cond_Owner;
		}

		return Cond_All_False;
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		int condition = validateCondition(player);
		SiegeUnit su = getBuilding(-1);
		String filename = _path + (su.isClanHall ? "doormen" : getTemplate().npcId) + "-no.htm";
		NpcHtmlMessage html;
		if(condition > Cond_All_False)
		{
			if(condition == Cond_Busy_Because_Of_Siege)
				filename = _path + (su.isClanHall ? "doormen" : getTemplate().npcId) + "-busy.htm";
			else
			{
				if(su.isClanHall)
				{
					switch(su.getId())
					{
						case 34:
						case 36:
						case 37:
						case 38:
						case 39:
						case 40:
						case 41:
						case 51:
						case 52:
						case 53:
						case 54:
						case 55:
						case 56:
						case 57:
						case 63:
						case 64:
							filename = _path + "doormen-elite.htm";
							break;
						default:
							filename = _path + "doormen.htm";
							break;
					}
				}
				else
					filename = _path + getTemplate().npcId + ".htm";
			}

			html = new NpcHtmlMessage(player, this, filename, val);
			if(su.isClanHall && condition == Cond_Owner) // Clan owns CH
				html.replace("%clanname%", player.getClanId() != 0 ? player.getClan().getName() : "GM Access");
			player.sendPacket(html);
		}
		else
		{
			if(su.isClanHall)
			{
				final L2Clan clanowner = ClanTable.getInstance().getClan(su.getOwnerId());
				if(clanowner == null)
					filename = _path + "doormen-auc.htm";

				html = new NpcHtmlMessage(player, this, filename, val);

				if(clanowner != null)
				{
					html.replace("%clanname%", clanowner.getName());
					html.replace("%clanlidername%", clanowner.getLeaderName());
				}
				player.sendPacket(html);
				return;
			}

			html = new NpcHtmlMessage(player, this, filename, val);
			player.sendPacket(html);
		}
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		player.sendPacket(Msg.ActionFail);
		int condition = validateCondition(player);
		if(condition <= Cond_All_False)
			return;

		NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
		StringTokenizer st;

		SiegeUnit su = getBuilding(-1);
		if(command.startsWith("open_doors"))
		{
			if(su.getSiege() != null && su.getSiege().isInProgress() || (su.isCastle && TerritoryWarManager.getWar().isInProgress() || su.isFort && TerritoryWarManager.isFortInWar(su)))
			{
				html.setFile(_path + (su.isClanHall ? "doormen" : getTemplate().npcId) + "-busy.htm");
				player.sendPacket(html);
				return;
			}

			if(su.isClanHall)
			{
				if(!isHaveRigths(player, L2Clan.CP_CH_OPEN_DOOR))
				{
					html.setFile(_path + "notauthorized.htm");
					player.sendPacket(html);
					return;
				}

				for(L2DoorInstance door : su.getDoors())
					su.openCloseDoor(player, door.getDoorId(), true);

				html.setFile(_path + "AfterDoorOpen.htm");
				player.sendPacket(html);
			}
			else
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_OPEN_DOOR))
				{
					html.setFile(_path + "notauthorized.htm");
					player.sendPacket(html);
					return;
				}

				st = new StringTokenizer(command.substring(10), ", ");
				st.nextToken(); // Bypass first value since its castleid/hallid/fortid
				while(st.hasMoreTokens())
				{
					int tk = Integer.parseInt(st.nextToken());
					if(su.isCastle)
					{
						if(su.getDoor(tk) == null)
						{
							_log.warn("L2DoormenInstance: no door found id: " + tk + " castle: " + su.getName());
							continue;
						}

						if(!su.getDoor(tk).isOpen())
							su.openDoor(player, tk);
					}
					else if(su.isFort)
					{
						if(su.getDoor(tk) == null)
						{
							_log.warn("L2DoormenInstance: no door found id: " + tk + " fortress: " + su.getName());
							continue;
						}

						if(!su.getDoor(tk).isOpen())
							su.openDoor(player, tk);
					}
				}
			}
		}
		else if(command.startsWith("close_doors"))
		{
			if(su.isClanHall)
			{

				if(!isHaveRigths(player, L2Clan.CP_CH_OPEN_DOOR))
				{
					html.setFile(_path + "notauthorized.htm");
					player.sendPacket(html);
					return;
				}

				for(L2DoorInstance door : su.getDoors())
					su.openCloseDoor(player, door.getDoorId(), false);

				html.setFile(_path + "AfterDoorClose.htm");
				player.sendPacket(html);
			}
			else
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_OPEN_DOOR))
				{
					html.setFile(_path + "notauthorized.htm");
					player.sendPacket(html);
					return;
				}

				st = new StringTokenizer(command.substring(11), ", ");
				st.nextToken(); // Bypass first value since its castleid/hallid/fortid
				while(st.hasMoreTokens())
				{
					int tk = Integer.parseInt(st.nextToken());
					if(su.isCastle)
					{
						if(su.getDoor(tk) == null)
						{
							_log.warn("L2DoormenInstance: no door found id: " + tk + " castle: " + su.getName());
							continue;
						}

						if(su.getDoor(tk).isOpen())
							su.closeDoor(player, tk);
					}
					else if(su.isFort)
					{
						if(su.getDoor(tk) == null)
						{
							_log.info("cannot find door for fortress for open it door no found  need check dp for debug:");
							_log.info("building is clanhall: " + su.isClanHall + "building is castle: " + su.isCastle + "building is fortress(must be true): " + su.isFort);
							_log.info("FortName is" + su.getName() + "NPCID is:" + getNpcId() + "dooris is" + tk);
							continue;
						}
						if(su.getDoor(tk).isOpen())
							su.closeDoor(player, tk);
					}
				}
			}
		}
		else if(command.equalsIgnoreCase("door"))
		{
			if((su.isClanHall && isHaveRigths(player, L2Clan.CP_CH_OPEN_DOOR)) || ((su.isFort || su.isCastle) && isHaveRigths(player, L2Clan.CP_CS_OPEN_DOOR)))
			{
				if(su.isCastle)
					html.setFile(_path + "door.htm");
				else
				{
					showChatWindow(player, 0);
					return;
				}
			}	
			else
				html.setFile(_path + "notauthorized.htm");

			player.sendPacket(html);
		}
		else if(command.equalsIgnoreCase("banish_foreigner"))
		{
			if((su.isClanHall && !isHaveRigths(player, L2Clan.CP_CH_DISMISS)) || ((su.isCastle || su.isFort) && !isHaveRigths(player, L2Clan.CP_CS_DISMISS)))
			{
				html.setFile(_path + "notauthorized.htm");
				player.sendPacket(html);
				return;
			}

			su.banishForeigner();
			if(su.isClanHall)
			{
				html.setFile(_path + "afterbanish.htm");
				player.sendPacket(html);
			}
		}
		else if(command.startsWith("teleTo"))
		{
			if(su.getSiege() != null && su.getSiege().isInProgress() && !su.getSiege().checkIsDefender(player.getClanId()))
			{
				html.setFile(_path + "notauthorized.htm");
				player.sendPacket(html);
				return;
			}

			st = new StringTokenizer(command.substring(7), " ");
			try{
				String tmp = st.nextToken();
				int x = Integer.parseInt(tmp);
				tmp = st.nextToken();
				int y = Integer.parseInt(tmp);
				tmp = st.nextToken();
				int z = Integer.parseInt(tmp);
				player.teleToLocation(x, y, z);
			}
			catch(Exception e)
			{
				_log.warn("L2Doormen: can't parse command: " + command);
			}
		}
		else
			super.onBypassFeedback(player, command);
	}
}
