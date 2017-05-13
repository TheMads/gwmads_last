package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Spawn;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.entity.siege.reinforce.GuardPowerReinforce;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.util.Location;

import java.util.StringTokenizer;

/**
 * @author rage
 * @date 01.07.2009 17:24:59
 */
public class L2LogisticOfficerInstance extends L2NpcInstance
{
	private SiegeUnit _fortress;
	private static final String _path = "data/html/fortress/logistic/";

	public L2LogisticOfficerInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
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
			NpcHtmlMessage html;

			if(actualCommand.equalsIgnoreCase("logistic"))
			{
				actualCommand = st.nextToken();
				if(actualCommand.equalsIgnoreCase("security"))
				{
					if(System.currentTimeMillis() - _fortress.getLastSiegeDate()  < 60 * 60000 || _fortress.getContractCastleId() == 0)
					{
						showChatWindow(player, "ind");
						return;
					}

					html = new NpcHtmlMessage(_objectId);
					html.setFile(_path + "logistic-guards.htm");
					html.replace("%guardpower%", String.valueOf(_fortress.getGuardPowerReinforce().getLevel()));

					player.sendPacket(html);
				}
				else if(actualCommand.equalsIgnoreCase("reinforce"))
				{
					if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
					{
						html = new NpcHtmlMessage(_objectId);
						html.setFile(_path + "notauthorized.htm");
						player.sendPacket(html);
						return;
					}

					int rId = Integer.parseInt(st.nextToken());
					int rLvl = Integer.parseInt(st.nextToken());

					GuardPowerReinforce gr = (GuardPowerReinforce) _fortress.getReinforceById(rId);

					if(gr == null)
					{
						_log.info(_fortress + " has no reinforce id: " + rId);
						return;
					}

					if(gr.getLevel() == rLvl)
					{
						showChatWindow(player, "used");
						return;
					}

					if(gr.getLevel() + 1 != rLvl)
					{
						showChatWindow(player, "no1lvl");
						return;
					}

					int price = gr.getPrice(rLvl);

					if(player.getAdena() < price)
					{
						html = new NpcHtmlMessage(_objectId);
						html.setFile(_path + "low_adena.htm");
						player.sendPacket(html);
						return;
					}

					if(player.reduceAdena("FortressReinforce", price, this, true))
					{
						showChatWindow(player, "enabled");
						gr.setLevel(rLvl);
						gr.store();
					}
				}
				else if(actualCommand.equalsIgnoreCase("supplies"))
				{
					if(!player.isClanLeader())
					{
						html = new NpcHtmlMessage(_objectId);
						html.setFile(_path + "notauthorized.htm");
						player.sendPacket(html);
						return;
					}

					if(System.currentTimeMillis() - _fortress.getLastSiegeDate()  < 60 * 60000 || _fortress.getContractCastleId() == 0)
					{
						showChatWindow(player, "ind");
						return;
					}

					html = new NpcHtmlMessage(_objectId);
					html.setFile(_path + "logistic-supply.htm");
					html.replace("%supplylevel%", String.valueOf(_fortress.getSupplyLevel()));
					player.sendPacket(html);
				}
				else if(actualCommand.equals("getsupplies"))
				{
					if(!player.isClanLeader())
					{
						html = new NpcHtmlMessage(_objectId);
						html.setFile(_path + "notauthorized.htm");
						player.sendPacket(html);
						return;
					}

					if(System.currentTimeMillis() - _fortress.getLastSiegeDate()  < 60 * 60000 || _fortress.getContractCastleId() == 0)
					{
						showChatWindow(player, "ind");
						return;
					}

					if(_fortress.getSupplyLevel() == 0)
					{
						showChatWindow(player, "nosupplies");
						return;
					}

					if(player.getAdena() < 25000)
					{
						html = new NpcHtmlMessage(_objectId);
						html.setFile(_path + "low_adena.htm");
						player.sendPacket(html);
						return;
					}

					if(player.reduceAdena("FortSupply", 25000, this, true))
					{
						L2Spawn spawn = _fortress.getSupplySpawn();
						if(spawn == null)
						{
							_log.warn(_fortress + " has no supply spawn for level: " + _fortress.getSupplyLevel());
							return;
						}

						spawn.setLoc(Location.coordsRandomize(this, 50));
						spawn.spawnOne();
						_fortress.setSupplyLevel(0);
						showChatWindow(player, "gotsupplies");
					}
				}
				else if(actualCommand.equalsIgnoreCase("rewards"))
				{
					if(!player.isClanLeader())
					{
						html = new NpcHtmlMessage(_objectId);
						html.setFile(_path + "notauthorized.htm");
						player.sendPacket(html);
						return;
					}

					if(System.currentTimeMillis() - _fortress.getLastSiegeDate()  < 60 * 60000)
					{
						showChatWindow(player, "ind");
						return;
					}

					html = new NpcHtmlMessage(_objectId);
					html.setFile(_path + "logistic-rewards.htm");
					html.replace("%rewardlevel%", String.valueOf(_fortress.getRewardLevel()));
					player.sendPacket(html);
				}
				else if(actualCommand.equalsIgnoreCase("getreward"))
				{
					if(!player.isClanLeader())
					{
						html = new NpcHtmlMessage(_objectId);
						html.setFile(_path + "notauthorized.htm");
						player.sendPacket(html);
						return;
					}

					if(System.currentTimeMillis() - _fortress.getLastSiegeDate()  < 60 * 60000)
					{
						showChatWindow(player, "ind");
						return;
					}

					if(_fortress.getRewardLevel() == 0)
					{
						showChatWindow(player, "noreward");
						return;
					}

					player.addItem("FortReward", 9910, _fortress.getRewardLevel(), this, true);
					_fortress.setRewardLevel(0);

					showChatWindow(player, "gotreward");
				}
			}
			else
				super.onBypassFeedback(player, command);
		}
		else
			showChatWindow(player, 0);
	}

	public void showChatWindow(L2Player player, String prefix)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(_path + "logistic-" + prefix + ".htm");

		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		player.setLastNpc(this);

		player.sendPacket(html);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String filename = _path;

		if(val == 0)
		{
			int cond = validateCondition(player);
			if(cond == Cond_Busy_Because_Of_Siege)
				filename += "logistic-busy.htm";
			else if(cond == Cond_Owner)
				filename += "logistic.htm";
			else
				filename += "logistic-no.htm";
		}
		else
			filename += "logistic-" + val + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);

		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		player.setLastNpc(this);

		player.sendPacket(html);
	}
}
