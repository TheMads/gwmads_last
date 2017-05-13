package ru.l2gw.gameserver.model.instances;

import javolution.text.TextBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.clientpackets.Say2C;
import ru.l2gw.gameserver.model.L2Party;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.Hero;
import ru.l2gw.gameserver.model.entity.olympiad.Olympiad;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.util.Files;
import ru.l2gw.util.Util;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * Olympiad Npc's Instance
 */
public class L2OlympiadManagerInstance extends L2NpcInstance
{
	private static Log _log = LogFactory.getLog(L2OlympiadManagerInstance.class.getName());

	public L2OlympiadManagerInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(command.startsWith("OlympiadNoble"))
		{
			int val = Integer.parseInt(command.substring(14));
			NpcHtmlMessage reply;

			switch(val)
			{
				case 1:
					if(Olympiad.isRegisteredInComp(player))
					{
						if(Olympiad.getRegisteredGameType(player) == 0)
						{
							if(player.getParty() != null && player.getParty().isLeader(player))
							{
								player.sendPacket(new NpcHtmlMessage(player, this, Olympiad.OLYMPIAD_HTML_FILE + "noble_unreg.htm", 0));
								return;
							}
						}
						else
						{
							player.sendPacket(new NpcHtmlMessage(player, this, Olympiad.OLYMPIAD_HTML_FILE + "noble_unreg.htm", 0));
							return;
						}
					}
					reply = new NpcHtmlMessage(player, this, Olympiad.OLYMPIAD_HTML_FILE + "noble_reg.htm", 0);
					reply.replace("<?week?>", String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH) / 7 + 1));
					reply.replace("<?round?>", String.valueOf(Olympiad.getCurrentCycle()));
					reply.replace("<?nobles?>", String.valueOf(Olympiad.getParticipantsCount()));
					player.sendPacket(reply);
					break;
				case 2:
					if(Olympiad.getRegisteredGameType(player) == 0)
					{
						L2Party party = player.getParty();
						if(party != null && party.isLeader(player) && party.getOlympiadTeam() != null)
						{
							Olympiad.unRegisterTeam(party.getOlympiadTeam());
							party.setOlympiadTeam(null);
						}
					}
					else
						Olympiad.unRegisterNoble(player);
					break;
				case 3:
					player.sendPacket(new NpcHtmlMessage(player, this, Olympiad.OLYMPIAD_HTML_FILE + "noble_reward.htm", 0));
					break;
				case 4:
				case 5:
				case 6:
					if(player.getClassId().getLevel() < 4)
					{
						reply = new NpcHtmlMessage(player, this, Olympiad.OLYMPIAD_HTML_FILE + "noble_3dclass.htm", 0);
						player.sendPacket(reply);
						return;
					}
					if(Olympiad.isInCompPeriod() && Olympiad.getTimeToCompEnd() <= 600000)
					{
						reply = new NpcHtmlMessage(player, this, Olympiad.OLYMPIAD_HTML_FILE + "noble_notime.htm", 0);
						player.sendPacket(reply);
						return;
					}
					if(Olympiad.getNoblePoints(player.getObjectId()) < 1)
					{
						player.sendPacket(new NpcHtmlMessage(player, this, Olympiad.OLYMPIAD_HTML_FILE + "noble_regnopoints.htm", 0));
						return;
					}
					if(player.isCursedWeaponEquipped())
					{
						player.sendPacket(new NpcHtmlMessage(player, this, Olympiad.OLYMPIAD_HTML_FILE + "noble_cw.htm", 0));
						return;
					}
					if(player.isSubClassActive())
					{
						player.sendPacket(new NpcHtmlMessage(player, this, Olympiad.OLYMPIAD_HTML_FILE + "noble_sub.htm", 0));
						return;
					}
					StatsSet nobleData = Olympiad.getNoblesData(player);
					if(nobleData != null)
					{
						if(nobleData.getInteger("cb_matches", 0) + nobleData.getInteger("ncb_matches", 0) + nobleData.getInteger("team_matches", 0) >= Config.ALT_OLY_MATCH_LIMIT)
						{
							player.sendPacket(Msg.THE_MAXIMUM_MATCHES_YOU_CAN_PARTICIPATE_IN_1_WEEK_IS_70);
							return;
						}
						else if(val == 4 && nobleData.getInteger("ncb_matches", 0) >= Config.ALT_OLY_NCB_LIMIT || val == 5 && nobleData.getInteger("cb_matches", 0) >= Config.ALT_OLY_CB_LIMIT || val == 6 && nobleData.getInteger("team_matches", 0) >= Config.ALT_OLY_TEAM_LIMIT)
						{
							player.sendPacket(Msg.THE_TOTAL_NUMBER_OF_MATCHES_THAT_CAN_BE_ENTERED_IN_1_WEEK_IS_60_CLASS_IRRELEVANT_INDIVIDUAL_MATCHES_30_SPECIFIC_MATCHES_AND_10_TEAM_MATCHES);
							return;
						}
					}

					if(val == 6)
					{
						L2Party party = player.getParty();
						if(party == null || party.getMemberCount() != 3)
						{
							player.sendPacket(Msg.THE_REQUEST_CANNOT_BE_MADE_BECAUSE_THE_REQUIREMENTS_HAVE_NOT_BEEN_MET_TO_PARTICIPATE_IN_A_TEAM_MATCH_YOU_MUST_FIRST_FORM_A_3_MEMBER_PARTY);
							return;
						}
						if(!party.isLeader(player))
						{
							player.sendPacket(Msg.ONLY_A_PARTY_LEADER_CAN_REQUEST_A_TEAM_MATCH);
							return;
						}
						for(L2Player member : party.getPartyMembers())
						{
							if(member.isSubClassActive())
							{
								party.broadcastToPartyMembers(new SystemMessage(SystemMessage.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_A_SUBCLASS_CHARACTER_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD).addCharName(member));
								return;
							}
							if(!member.isNoble())
							{
								party.broadcastToPartyMembers(new SystemMessage(SystemMessage.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_ONLY_NOBLESSE_CHARACTERS_CAN_PARTICIPATE_IN_THE_OLYMPIAD).addCharName(member));
								return;
							}
							int gt;
							if((gt = Olympiad.getRegisteredGameType(player)) >= 0)
							{
								if(gt == 0)
									party.broadcastToPartyMembers(new SystemMessage(SystemMessage.C1_IS_ALREADY_REGISTERED_ON_THE_WAITING_LIST_FOR_THE_3_VS_3_CLASS_IRRELEVANT_TEAM_MATCH).addCharName(member));
								else if(gt == 1)
									party.broadcastToPartyMembers(new SystemMessage(SystemMessage.C1_IS_ALREADY_REGISTERED_ON_THE_WAITING_LIST_FOR_THE_NON_CLASS_LIMITED_INDIVIDUAL_MATCH_EVENT).addCharName(member));
								else
									party.broadcastToPartyMembers(new SystemMessage(SystemMessage.C1_IS_ALREADY_REGISTERED_ON_THE_CLASS_MATCH_WAITING_LIST).addCharName(member));
								return;
							}
							if(Olympiad.getNoblePoints(member.getObjectId()) < 1)
							{
								party.broadcastToPartyMembers(Msg.THE_REQUEST_CANNOT_BE_COMPLETED_BECAUSE_THE_REQUIREMENTS_ARE_NOT_MET_IN_ORDER_TO_PARTICIPATE_IN_A_TEAM_MATCH_ALL_TEAM_MEMBERS_MUST_HAVE_AN_OLYMPIAD_SCORE_OF_1_OR_MORE);
								return;
							}
							if(!member.isQuestContinuationPossible(false))
							{
								party.broadcastToPartyMembers(new SystemMessage(SystemMessage.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD_BECAUSE_YOUR_INVENTORY_SLOT_EXCEEDS_80).addCharName(member));
								return;
							}
							if(member.isCursedWeaponEquipped())
							{
								party.broadcastToPartyMembers(new SystemMessage(SystemMessage.C1_IS_THE_OWNER_OF_S2_AND_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD).addCharName(member).addItemName(player.getCursedWeaponEquippedId()));
								return;
							}
							if(member.isDead())
							{
								party.broadcastToPartyMembers(new SystemMessage(SystemMessage.C1_IS_CURRENTLY_DEAD_AND_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD).addCharName(member));
								return;
							}
							nobleData = Olympiad.getNoblesData(member);
							if(nobleData != null)
							{
								if(nobleData.getInteger("cb_matches", 0) + nobleData.getInteger("ncb_matches", 0) + nobleData.getInteger("team_matches", 0) >= Config.ALT_OLY_MATCH_LIMIT)
								{
									player.sendPacket(Msg.THE_MAXIMUM_MATCHES_YOU_CAN_PARTICIPATE_IN_1_WEEK_IS_70);
									return;
								}
								else if(nobleData.getInteger("team_matches", 0) >= Config.ALT_OLY_TEAM_LIMIT)
								{
									party.broadcastToPartyMembers(Msg.THE_TOTAL_NUMBER_OF_MATCHES_THAT_CAN_BE_ENTERED_IN_1_WEEK_IS_60_CLASS_IRRELEVANT_INDIVIDUAL_MATCHES_30_SPECIFIC_MATCHES_AND_10_TEAM_MATCHES);
									return;
								}
							}
							if(Config.ALT_OLY_ENABLE_HWID_CHECK && Olympiad.checkHWID(member.getLastHWID()))
							{
								party.broadcastMessageToPartyMembers(new CustomMessage("olympiad.hwid.check", member).addCharName(member).toString());
								return;
							}
						}
						Olympiad.registerParty(party);
						return;
					}

					Olympiad.registerNoble(player, val == 5);
					break;
				case 7:
					if(Olympiad.isCalculatePeriod())
					{
						player.sendPacket(new SystemMessage(SystemMessage.THIS_IS_A_PERIOD_OF_CALCULATIING_STATISTICS_IN_THE_SERVER));
						return;
					}

					if(Olympiad.getPreviousPoints(player.getObjectId()) > 0)
						player.sendPacket(new NpcHtmlMessage(player, this, Olympiad.OLYMPIAD_HTML_FILE + "noble_points.htm", 0));
					else
						player.sendPacket(new NpcHtmlMessage(player, this, Olympiad.OLYMPIAD_HTML_FILE + "noble_nopoints.htm", 0));
					break;
				case 8:
					if(Olympiad.isCalculatePeriod())
					{
						player.sendPacket(new SystemMessage(SystemMessage.THIS_IS_A_PERIOD_OF_CALCULATIING_STATISTICS_IN_THE_SERVER));
						return;
					}

					long tokensCount = Olympiad.getOlympiadTokensCount(player.getObjectId());
					if(tokensCount > 0)
						player.addItem("Olympiad", Olympiad.OLYMPIAD_TOKENS_ID, tokensCount, this, true);
					else
					{
						reply = new NpcHtmlMessage(player, this, Olympiad.OLYMPIAD_HTML_FILE + "noble_nopoints.htm", 0);
						player.sendPacket(reply);
					}
					break;
				default:
					_log.warn("Olympiad System: Couldnt send packet for request " + val);
					break;
			}
		}
		else if(command.startsWith("Olympiad"))
		{
			int val = Integer.parseInt(command.substring(9, 10));

			NpcHtmlMessage reply = new NpcHtmlMessage(getObjectId());

			switch(val)
			{
				case 1:
					if(!Olympiad.isInCompPeriod())
						player.sendPacket(Msg.THE_OLYMPIAD_GAME_IS_NOT_CURRENTLY_IN_PROGRESS);
					else
						player.sendPacket(new ExReceiveOlympiad());
					break;
				case 2:
					// for example >> Olympiad 1_88
					int classId = Integer.parseInt(command.substring(11));
					if(classId >= 88)
					{
						HashMap<Integer, String> tpls = Util.parseTemplate(Files.read(Olympiad.OLYMPIAD_HTML_FILE + "olymp_stat.htm", player, false));
						String tpl = tpls.get(1);
						String tmp;
						TextBuilder table = new TextBuilder();
						List<String> names = Olympiad.getClassLeaderBoard(classId);
						for(int i = 0; i < 15; i++)
						{
							String name = "";
							if(i < names.size())
							{
								name = names.get(i);
								tmp = tpl.replace("<?index?>", String.valueOf(i + 1));
							}
							else
								tmp = tpl.replace("<?index?>", "");

							tmp = tmp.replace("<?name?>", name);
							table.append(tmp);
						}

						reply = new NpcHtmlMessage(player, this);
						reply.setHtml(tpls.get(0).replace("<?table?>", table.toString()));
						player.sendPacket(reply);
					}
					break;
				case 3:
					if(Olympiad.isCalculatePeriod())
					{
						reply.setFile(Olympiad.OLYMPIAD_HTML_FILE + "monument-calc.htm");
						player.sendPacket(reply);
						return;
					}

					player.sendPacket(new ExHeroList());
					break;
				case 4:
					if(Olympiad.isCalculatePeriod())
					{
						reply.setFile(Olympiad.OLYMPIAD_HTML_FILE + "monument-calc.htm");
						player.sendPacket(reply);
					}
					else if(player.isHero())
					{
						reply.setFile(Olympiad.OLYMPIAD_HTML_FILE + "monument-hero-taken.htm");
						player.sendPacket(reply);
					}
					else if(player.canBeAHero())
					{
						reply.setFile(Olympiad.OLYMPIAD_HTML_FILE + "monument-q.htm");
						player.sendPacket(reply);
					}
					else
					{
						reply.setFile(Olympiad.OLYMPIAD_HTML_FILE + "monument-notq.htm");
						player.sendPacket(reply);
					}
					break;
				case 5:
					if(Olympiad.isCalculatePeriod())
					{
						reply.setFile(Olympiad.OLYMPIAD_HTML_FILE + "monument-calc.htm");
						player.sendPacket(reply);
					}
					else if(player.canBeAHero())
					{
						Hero.giveHeroBonuses(player);
						broadcastPacket(new Say2(player.getLastNpc().getObjectId(), Say2C.SHOUT, player.getLastNpc().getName(), new CustomMessage("OlympiadHero_" + player.getBaseClass(), Config.DEFAULT_LANG).addCharName(player).toString()));
					}
					else
					{
						reply.setFile(Olympiad.OLYMPIAD_HTML_FILE + "monument-notq.htm");
						player.sendPacket(reply);
					}
					break;
				default:
					_log.warn("Olympiad System: Couldnt send packet for request " + val);
					break;
			}
		}
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		if(player.isCursedWeaponEquipped())
		{
			player.sendPacket(new NpcHtmlMessage(player, this, Olympiad.OLYMPIAD_HTML_FILE + "noble_cw.htm", val));
			return;
		}
		if(player.isCombatFlagEquipped())
		{
			player.sendPacket(new NpcHtmlMessage(player, this, "data/html/default/flagmen.htm", val));
			return;
		}
		String filename;

		switch(getNpcId())
		{
			case 31688:
				if(player.isNoble() && val == 0)
					filename = Olympiad.OLYMPIAD_HTML_FILE + "noble_main.htm";
				else
					filename = getHtmlPath(getNpcId(), val, player.getKarma());
				break;
			case 31690:
			case 31769:
			case 31770: // Monument of Heroes
			case 31771:
			case 31772:
				if(player.isNoble())
					filename = Olympiad.OLYMPIAD_HTML_FILE + "monument-nobl.htm";
				else
					filename = Olympiad.OLYMPIAD_HTML_FILE + "monument.htm";
				break;
			default:
				filename = getHtmlPath(getNpcId(), val, player.getKarma());
				break;
		}
		player.sendPacket(new NpcHtmlMessage(player, this, filename, val));
	}
}
