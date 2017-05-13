package ru.l2gw.gameserver.clientpackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.handler.AdminCommandHandler;
import ru.l2gw.gameserver.handler.IVoicedCommandHandler;
import ru.l2gw.gameserver.handler.VoicedCommandHandler;
import ru.l2gw.gameserver.instancemanager.QuestManager;
import ru.l2gw.gameserver.model.BypassManager.DecodedBypass;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.Warehouse;
import ru.l2gw.gameserver.model.entity.Hero;
import ru.l2gw.gameserver.model.entity.olympiad.Olympiad;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.playerSubOrders.BypassEngine;
import ru.l2gw.gameserver.model.quest.Quest;
import ru.l2gw.gameserver.network.GameClient;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.serverpackets.PackageToList;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.serverpackets.WareHouseWithdrawList;

import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestBypassToServer extends L2GameClientPacket
{
	//Format: cS
	private static Log _log = LogFactory.getLog(RequestBypassToServer.class.getName());
	//private int command_success;
	private DecodedBypass bp = null;
	private static final Pattern questPattern = Pattern.compile("ask=(\\-?\\d+) *&reply= *(\\-?\\d+)");
	private static final Pattern questChoice = Pattern.compile("choice=(\\-?\\d+)&option=(\\-?\\d+)");
	private static final Pattern questStart = Pattern.compile("quest_start\\?quest_id=(\\d+)");
	private static final Pattern classChange = Pattern.compile("class_name=(\\d+)");

	@Override
	public void readImpl()
	{
		String bypass = readS();
		if(bypass != null && !bypass.isEmpty() && getClient() != null && getClient().getPlayer() != null && !getClient().getPlayer().isEntering() && !getClient().getPlayer().isLogoutStarted())
			bp = BypassEngine.decodeBypass(bypass, getClient().getPlayer());
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		try
		{
			if(bp.bypass.startsWith("admin_"))
				AdminCommandHandler.getInstance().useAdminCommandHandler(player, bp.bypass);
			else if(bp.bypass.equals("come_here") && player.isGM())
				comeHere(getClient());
			else if(bp.bypass.startsWith("player_help "))
				playerHelp(player, bp.bypass.substring(12));
			else if(bp.bypass.startsWith("scripts_"))
			{
				if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
				{
					player.sendActionFailed();
					return;
				}

				if(!(player.isGM() || AdminTemplateManager.checkBoolean("useCommands", player)) && (player.getLastNpc() == null || player.getLastNpc() != player.getTarget()))
				{
					player.sendActionFailed();
					return;
				}

				//System.out.println(bp.bypass);
				String command = bp.bypass.substring(8).trim();
				String[] word = command.split("\\s+");
				String[] args = command.substring(word[0].length()).trim().split("\\s+");
				String[] path = word[0].split(":");
				if(path.length != 2)
				{
					_log.warn("Bad Script bypass!");
					return;
				}

				HashMap<String, Object> variables = new HashMap<String, Object>();

				if(player.getTarget() instanceof L2NpcInstance)
					variables.put("npc", player.getTarget());
				else
					variables.put("npc", null);

				if(word.length == 1)
					player.callScripts(path[0], path[1], new Object[]{}, variables);
				else
					player.callScripts(path[0], path[1], new Object[]{args}, variables);
			}
			else if(bp.bypass.startsWith("user_"))
			{
				String command = bp.bypass.substring(5).trim();
				String word = (command.split("\\s+"))[0];
				String args = command.substring(word.length()).trim();
				IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getVoicedCommandHandler(word);

				if(vch != null)
					vch.useVoicedCommand(word, player, args);
				else
					_log.warn("Unknow voiced command '" + word + "'");
			}
			else if(bp.bypass.startsWith("npc_"))
			{
				if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
				{
					player.sendActionFailed();
					return;
				}

				int endOfId = bp.bypass.indexOf('_', 5);
				String id;
				if(endOfId > 0)
					id = bp.bypass.substring(4, endOfId);
				else
					id = bp.bypass.substring(4);
				L2Object object = player.getVisibleObject(Integer.parseInt(id));
				if(object != null && object instanceof L2NpcInstance && endOfId > 0 && player.isInRange(object, player.getInteractDistance(object)))
					((L2NpcInstance) object).onBypassFeedback(player, bp.bypass.substring(endOfId + 1));
			}
			// Navigate throught Manor windows
			else if(bp.bypass.startsWith("manor_menu_select?"))
			{
				if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
				{
					player.sendActionFailed();
					return;
				}

				L2Object object = player.getTarget();
				if(object instanceof L2NpcInstance)
					((L2NpcInstance) object).onBypassFeedback(player, bp.bypass);
			}
/*
			else if(bp.bypass.startsWith("bbs_"))
				CommunityBoard.getInstance().handleCommands(getClient(), bp.bypass);
			else if(bp.bypass.startsWith("_bbs"))
				CommunityBoard.getInstance().handleCommands(getClient(), bp.bypass);
*/
			else if(bp.bypass.startsWith("Quest "))
			{
				if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
				{
					player.sendActionFailed();
					return;
				}

				if(player.getLastNpc() != null && player.isInRange(player.getLastNpc(), player.getInteractDistance(player.getLastNpc())))
				{
					String p = bp.bypass.substring(6).trim();
					int idx = p.indexOf(' ');
					if(idx < 0)
						player.processQuestEvent(p, "");
					else
						player.processQuestEvent(p.substring(0, idx), p.substring(idx).trim());
				}
				else
					player.sendActionFailed();
			}
			else if(bp.bypass.startsWith("quest_select?"))
			{
				Matcher m = questPattern.matcher(bp.bypass);
				if(m.find())
				{
					int ask = Integer.parseInt(m.group(1));
					int reply = Integer.parseInt(m.group(2));

					if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
					{
						player.sendActionFailed();
						return;
					}

					if(player.getLastNpc() != null && player.isInRange(player.getLastNpc(), player.getInteractDistance(player.getLastNpc())))
					{
						Quest q = QuestManager.getQuest(ask);
						if(q != null)
							q.onQuestSelect(reply, player);
					}
				}
				player.sendActionFailed();
			}
			else if(bp.bypass.startsWith("talk_select"))
			{
				if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
				{
					player.sendActionFailed();
					return;
				}

				L2NpcInstance npc = player.getLastNpc();
				if(npc != null && player.isInRange(npc, player.getInteractDistance(npc)))
					npc.getAI().onTalkSelected(player, 0, false);
			}
			else if(bp.bypass.startsWith("quest_choice?"))
			{
				if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
				{
					player.sendActionFailed();
					return;
				}

				L2NpcInstance npc = player.getLastNpc();
				if(npc != null && player.isInRange(npc, player.getInteractDistance(npc)))
				{
					Matcher m = questChoice.matcher(bp.bypass);
					if(m.find())
					{
						int code = Integer.parseInt(m.group(1).trim());
						npc.getAI().onTalkSelected(player, code, true);
					}
				}
			}
			else if(bp.bypass.startsWith("quest_start?"))
			{
				Matcher m = questStart.matcher(bp.bypass);
				if(m.find())
				{
					int questId = Integer.parseInt(m.group(1).trim());
					Quest quest = QuestManager.getQuest(questId);
					if(quest != null)
					{
						quest.onQuestStart(player);
					}
				}
			}
			else if(bp.bypass.startsWith("menu_select?"))
			{
				if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
				{
					player.sendActionFailed();
					return;
				}

				L2NpcInstance npc = player.getLastNpc();
				if(npc != null && player.isInRange(npc, player.getInteractDistance(npc)))
				{
					Matcher m = questPattern.matcher(bp.bypass);
					if(m.find())
					{
						int ask = Integer.parseInt(m.group(1).trim());
						int reply = Integer.parseInt(m.group(2).trim());

						npc.getAI().onMenuSelected(player, ask, reply);
					}
				}
			}
			else if(bp.bypass.startsWith("class_change?"))
			{
				if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
				{
					player.sendActionFailed();
					return;
				}

				L2NpcInstance npc = player.getLastNpc();
				if(npc != null && player.isInRange(npc, player.getInteractDistance(npc)))
				{
					Matcher m = classChange.matcher(bp.bypass);
					if(m.find())
					{
						int occupation_name_id = Integer.parseInt(m.group(1).trim());
						npc.getAI().classChangeRequested(player, occupation_name_id);
					}
				}
			}
			else if(bp.bypass.equals("teleport_request"))
			{
				if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
				{
					player.sendActionFailed();
					return;
				}

				L2NpcInstance npc = player.getLastNpc();
				if(npc != null && player.isInRange(npc, player.getInteractDistance(npc)))
				{
					npc.getAI().onTeleportRequested(player);
				}
			}
			else if(bp.bypass.equals("package_deposit"))
			{
				if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
				{
					player.sendActionFailed();
					return;
				}

				L2NpcInstance npc = player.getLastNpc();
				if(npc != null && player.isInRange(npc, player.getInteractDistance(npc)))
				{
					player.setUsingWarehouseType(Warehouse.WarehouseType.FREIGHT);
					player.sendPacket(new PackageToList());
				}
			}
			else if(bp.bypass.equals("package_withdraw"))
			{
				if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
				{
					player.sendActionFailed();
					return;
				}

				L2NpcInstance npc = player.getLastNpc();
				if(npc != null && player.isInRange(npc, player.getInteractDistance(npc)))
				{
					player.setUsingWarehouseType(Warehouse.WarehouseType.FREIGHT);
					if(Config.DEBUG)
						_log.debug("Showing freightened items");

					Warehouse list = player.getFreight();

					if(list != null)
						player.sendPacket(new WareHouseWithdrawList(player, Warehouse.WarehouseType.FREIGHT));
					player.sendActionFailed();
				}
			}
			else if(bp.bypass.startsWith("teleport_"))
			{
				if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
				{
					player.sendActionFailed();
					return;
				}

				L2NpcInstance npc = player.getLastNpc();
				if(npc != null && player.isInRange(npc, player.getInteractDistance(npc)))
				{
					StringTokenizer st = new StringTokenizer(bp.bypass, "_");
					st.nextToken();
					if(st.countTokens() == 4)
					{
						try
						{
							npc.getAI().onTeleport(player, Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()));
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
					}
				}
			}
			else if(bp.bypass.startsWith("_olympiad?command=move_op_field&field="))
			{
				int arenaId = -1;
				try
				{
					arenaId = Integer.parseInt(bp.bypass.substring(38)) - 1;
				}
				catch(Exception e)
				{
					return;
				}

				if(arenaId < 0)
					return;

				if(player.isInCombat())
				{
					player.sendPacket(Msg.YOU_CANNOT_OBSERVE_WHILE_YOU_ARE_IN_COMBAT);
					return;
				}

				if(Olympiad.getRegisteredGameType(player) >= 0 && !player.inObserverMode())
				{
					player.sendPacket(Msg.WHILE_YOU_ARE_ON_THE_WAITING_LIST_YOU_ARE_NOT_ALLOWED_TO_WATCH_THE_GAME);
					return;
				}

				if(!Olympiad.isInCompPeriod())
					return;

				if(player.inObserverMode() && player.getOlympiadGameId() >= 0)
					player.moveToArena(arenaId);
				else if(player.getReflection() == 0 && player.getLastNpc() != null && player.getLastNpc().getNpcId() == 31688 && player.isInRange(player.getLastNpc(), 150))
					player.enterOlympiadObserverMode(arenaId);
			}
			/*
			else if(bp.bypass.startsWith("move_to_arena "))
			{
				if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
				{
					player.sendActionFailed();
					return;
				}

				if(player == null || !player.inObserverMode() || player.getOlympiadGameId() == -1)
					return;

				int arenaId = -1;
				try
				{
					arenaId = Integer.parseInt(bp.bypass.substring(14));
				}
				catch(Exception e)
				{
				}
				if(arenaId != -1)
					player.moveToArena(arenaId);
			}
			*/
			else if(bp.bypass.startsWith("_diary"))
			{
				if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
				{
					player.sendActionFailed();
					return;
				}

				if(player == null)
					return;
				Hero.onViewHeroHistory(player, bp.bypass);
			}
			else if(bp.bypass.startsWith("_match"))
			{
				if(!Config.ALLOW_TALK_WHILE_SITTING && player.isSitting() || player.isAlikeDead())
				{
					player.sendActionFailed();
					return;
				}

				if(player == null) return;
				Hero.onViewHeroMatchHistory(player, bp.bypass);
			}
			else if(bp.handler != null)
			{
				if(!Config.COMMUNITYBOARD_ENABLED)
					player.sendPacket(new SystemMessage(SystemMessage.THE_COMMUNITY_SERVER_IS_CURRENTLY_OFFLINE));
				else
					bp.handler.onBypassCommand(player, bp.bypass);
			}
		}
		catch(Exception e)
		{
			String st = "Bad RequestBypassToServer: " + (bp != null ? bp.bypass : "bp is null");
			if(player.getTarget() instanceof L2NpcInstance)
				st += " via NPC #" + ((L2NpcInstance) player.getTarget()).getNpcId();
			_log.warn(st);
		}
	}

	/**
	 * @param client
	 */
	private void comeHere(GameClient client)
	{
		L2Object obj = client.getPlayer().getTarget();
		if(obj instanceof L2NpcInstance)
		{
			L2NpcInstance temp = (L2NpcInstance) obj;
			L2Player player = client.getPlayer();
			temp.setTarget(player);
			temp.moveToLocation(player.getLoc(), 0, true);
		}
	}

	private void playerHelp(L2Player player, String path)
	{
		String filename = "data/html/" + path.replace("..", "").replace("//", "").replace("\\\\", "");
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile(filename);
		player.sendPacket(html);
	}
}