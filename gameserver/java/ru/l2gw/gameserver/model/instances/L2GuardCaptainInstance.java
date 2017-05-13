package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.entity.siege.reinforce.DoorReinforce;
import ru.l2gw.gameserver.model.entity.siege.reinforce.GuardReinforce;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.StringTokenizer;

/**
 * @author rage
 * @date 30.06.2009 10:33:28
 */
public class L2GuardCaptainInstance extends L2NpcInstance
{
	private SiegeUnit _fortress;
	private static final String _path = "data/html/fortress/garrison/";

	public L2GuardCaptainInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
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
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command

		if(actualCommand.equalsIgnoreCase("garrison"))
		{
			actualCommand = st.nextToken();

			if(actualCommand.equalsIgnoreCase("guards"))
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(_path + "notauthorized.htm");
					sendHtmlMessage(player, html);
					return;
				}

				GuardReinforce guards = (GuardReinforce) _fortress.getReinforceById(1);
				GuardReinforce balista = (GuardReinforce) _fortress.getReinforceById(3);

				if(guards == null || balista == null)
				{
					_log.info(_fortress + " has no Guard reinforces!");
					return;
				}

				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile(_path + "garrison-guards.htm");
				html.replace("%guards_level%", String.valueOf(guards.getLevel()));
				html.replace("%photocannon%", String.valueOf(balista.getLevel()));
				html.replace("%nextLvl%", String.valueOf(balista.getLevel() < balista.getMaxLevel() ? balista.getLevel() + 1 : balista.getMaxLevel()));
				sendHtmlMessage(player, html);
			}
			else if(actualCommand.equalsIgnoreCase("reinforce"))
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(_path + "notauthorized.htm");
					sendHtmlMessage(player, html);
					return;
				}

				int rId = Integer.parseInt(st.nextToken());
				int rLvl = Integer.parseInt(st.nextToken());

				GuardReinforce gr = (GuardReinforce) _fortress.getReinforceById(rId);

				if(gr == null)
				{
					_log.info(_fortress + " has no reinforce id: " + rId);
					return;
				}

				if(gr.getLevel() == rLvl)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(_path + "garrison-used.htm");
					sendHtmlMessage(player, html);
					return;
				}

				if(gr.getLevel() + 1 != rLvl)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(_path + "garrison-no1lvl.htm");
					sendHtmlMessage(player, html);
					return;
				}

				int price = gr.getPrice(rLvl);

				if(player.getAdena() < price)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(_path + "low_adena.htm");
					sendHtmlMessage(player, html);
					return;
				}

				if(player.reduceAdena("FortressReinforce", price, this, true))
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(_path + "garrison-enabled.htm");
					sendHtmlMessage(player, html);
					gr.setLevel(rLvl);
					gr.store();
				}
			}
			else if(actualCommand.equalsIgnoreCase("scouts"))
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(_path + "notauthorized.htm");
					sendHtmlMessage(player, html);
					return;
				}

				int rId = Integer.parseInt(st.nextToken());

				GuardReinforce gr = (GuardReinforce) _fortress.getReinforceById(rId);

				if(gr == null)
				{
					_log.info(_fortress + " has no reinforce id: " + rId);
					return;
				}

				if(gr.getLevel() == gr.getMaxLevel())
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(_path + "garrison-used.htm");
					sendHtmlMessage(player, html);
					return;
				}

				int price = gr.getPrice(gr.getMaxLevel());

				if(player.getAdena() < price)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(_path + "low_adena.htm");
					sendHtmlMessage(player, html);
					return;
				}

				if(player.reduceAdena("FortressReinforce", price, this, true))
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(_path + "garrison-enabled.htm");
					sendHtmlMessage(player, html);
					gr.setLevel(gr.getMaxLevel());
					gr.store();
				}
			}
			else if(actualCommand.equalsIgnoreCase("gates"))
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(_path + "notauthorized.htm");
					sendHtmlMessage(player, html);
					return;
				}

				int rId = Integer.parseInt(st.nextToken());

				DoorReinforce dr = (DoorReinforce) _fortress.getReinforceById(rId);

				if(dr == null)
				{
					_log.info(_fortress + " has no reinforce id: " + rId);
					return;
				}

				if(dr.getLevel() == dr.getMaxLevel())
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(_path + "garrison-used.htm");
					sendHtmlMessage(player, html);
					return;
				}

				int price = dr.getPrice(dr.getMaxLevel());

				if(player.getAdena() < price)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(_path + "low_adena.htm");
					sendHtmlMessage(player, html);
					return;
				}

				if(player.reduceAdena("FortressReinforce", price, this, true))
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(_path + "garrison-enabled.htm");
					sendHtmlMessage(player, html);
					dr.setLevel(dr.getMaxLevel());
					dr.store();
				}
			}
		}
		else
			super.onBypassFeedback(player, command);

	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String filename = _path;

	    if(player.getClanId() == 0 || player.getClanId() != _fortress.getOwnerId())
			filename += "garrison-no.htm";
		else if(_fortress.getSiege().isInProgress())
			filename += "garrison-busy.htm";
		else if(System.currentTimeMillis() - _fortress.getLastSiegeDate()  < 60 * 60000 || _fortress.getContractCastleId() == 0)
			filename += "garrison-ind.htm";
		else
			filename += "garrison.htm";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);

		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		html.replace("%fortId%", String.valueOf(_fortress.getId()));
		player.setLastNpc(this);

		player.sendPacket(html);
	}

	protected void sendHtmlMessage(L2Player player, NpcHtmlMessage html)
	{
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
}
