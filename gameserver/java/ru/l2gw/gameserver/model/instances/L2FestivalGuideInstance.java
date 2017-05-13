package ru.l2gw.gameserver.model.instances;

import javolution.util.FastList;
import ru.l2gw.gameserver.model.L2Party;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.entity.SevenSigns;
import ru.l2gw.gameserver.model.entity.SevenSignsFestival.Festival;
import ru.l2gw.gameserver.model.entity.SevenSignsFestival.FestivalManager;
import ru.l2gw.gameserver.model.entity.SevenSignsFestival.FestivalParty;
import ru.l2gw.gameserver.model.entity.SevenSignsFestival.SevenSignsFestival;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.SkillTreeTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.gameserver.templates.StatsSet;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

/**
 * Festival of Darkness Guide (Seven Signs)
 *
 * @author rage
 */
public final class L2FestivalGuideInstance extends L2NpcInstance
{
	private Festival _festival;
	/**
	 * @param template
	 */
	public L2FestivalGuideInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
		_festival = FestivalManager.getInstance().getFestByNpcId(getNpcId());
		if(_festival == null)
			_log.warn("L2FestivalGuide: has no festival defined! " + this);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		List<String> replace;

		if(command.startsWith("FestivalDesc"))
		{
			int val = Integer.parseInt(command.substring(13));
			if(val == 4 && SevenSigns.getInstance().isSealValidationPeriod())
				showChatWindow(player, val, "a", true);
			else
				showChatWindow(player, val, null, true);
		}
		else if(command.startsWith("Festival"))
		{
			L2Party playerParty = player.getParty();
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			int val = 0;
			int subVal = 0;

			if(st.hasMoreTokens())
				val = Integer.parseInt(st.nextToken());
			if(st.hasMoreTokens())
				subVal = Integer.parseInt(st.nextToken());

			if(SevenSigns.getInstance().getPlayerCabal(player) != _festival.getCabal() && val != 7)
			{
				showChatWindow(player, 2, "k", false);
				return;
			}

			switch(val)
			{
				case 1: // Become a Participant
					replace = new FastList<String>();
					replace.add("%bssfee%");
					replace.add(Integer.toString(_festival.getCostByStoneId(6360)));
					replace.add("%gssfee%");
					replace.add(Integer.toString(_festival.getCostByStoneId(6361)));
					replace.add("%rssfee%");
					replace.add(Integer.toString(_festival.getCostByStoneId(6362)));

					showChatWindow(player, 1, null, false, replace);
					break;
				case 2: // Festival 2 xxxx
					// Check if the festival period is active, if not then don't allow registration.
					if(!SevenSigns.getInstance().isCompetitionPeriod())
					{
						showChatWindow(player, 2, "a", false);
						return;
					}

					if(SevenSigns.getInstance().getPlayerCabal(player) != _festival.getCabal())
					{
						showChatWindow(player, 2, "k", false);
						return;
					}

					// Check if the player is in a formed party already.
					if(playerParty == null || playerParty.getMemberCount() < FestivalManager.getInstance().getMinPartyMemebers())
					{
						showChatWindow(player, 2, "b", false);
						return;
					}

					// Check if the player is the party leader.
					if(!playerParty.isLeader(player))
					{
						showChatWindow(player, 2, "c", false);
						return;
					}

					// Check if all the party members are in the required level range.
					String members = "";
					for(L2Player member : playerParty.getPartyMembers())
					{
						if(member.getLevel() > _festival.getMinLevel())
							members += member.getName() + ", ";
					}

					if(!members.isEmpty())
					{
						replace = new FastList<String>();
						replace.add("%festivalType%");
						replace.add(_festival.getMinLevel() + " or below");
						replace.add("%names%");
						replace.add(members);
						replace.add("%limit%");
						replace.add(Integer.toString(_festival.getMinLevel()));
						showChatWindow(player, 2, "d", false, replace);
						return;
					}

					if(!FestivalManager.getInstance().isRegistrationOpen())
					{
						showChatWindow(player, 2, "f", false);
						return;
					}

					for(L2Player member : playerParty.getPartyMembers())
					{
						for(L2Skill skill : member.getAllSkills())
						{
							if(!skill.isCommon() && SkillTreeTable.getMinSkillLevel(skill.getId(), player.getClassId(), skill.getLevel()) > _festival.getMinLevel())
							{
								members += member.getName() + ", ";
							}
						}
					}

					if(!members.isEmpty())
					{
						replace = new FastList<String>();
						replace.add("%names%");
						replace.add(members);
						showChatWindow(player, 2, "g", false, replace);
						return;
					}

					int stoneType = subVal;
					int stonesNeeded = _festival.getCostByStoneId(stoneType);

					L2ItemInstance sealStoneInst = player.getInventory().getItemByItemId(stoneType);

					if(sealStoneInst == null || sealStoneInst.getCount() < stonesNeeded)
					{
						showChatWindow(player, 2, "h", false);
						return;
					}

					for(L2Player member : playerParty.getPartyMembers())
					{
						if(SevenSigns.getInstance().getPlayerCabal(member) != _festival.getCabal())
								members += member.getName() + ", ";
					}

					if(!members.isEmpty())
					{
						replace = new FastList<String>();
						replace.add("%names%");
						replace.add(members);
						showChatWindow(player, 2, "i", false, replace);
						return;
					}

					replace = new FastList<String>();
					replace.add("%stoneType%");
					replace.add(Integer.toString(stoneType));

					showChatWindow(player, 2, "e", false, replace);
					break;
				case 3: // Score Registration
					// Check if the festival period is active, if not then don't register the score.
					if(!SevenSigns.getInstance().isCompetitionPeriod())
					{
						showChatWindow(player, 3, "a", false);
						return;
					}

					L2ItemInstance bloodOfferings = player.getInventory().getItemByItemId(SevenSignsFestival.FESTIVAL_OFFERING_ID);

					// Check if the player collected any blood offerings during the festival.
					if(bloodOfferings == null)
					{
						showChatWindow(player, 3, "g", false);
						return;
					}

					FestivalParty festParty = null;

					// Check if the player is in a party.
					for(FestivalParty fp : SevenSignsFestival.getInstance().getContrubutePartys())
					{
						if(fp.getPartyLeaderObjId() == player.getObjectId())
						{
							if(fp.getValidTime() < System.currentTimeMillis())
							{
								SevenSignsFestival.getInstance().getContrubutePartys().remove(fp);

								showChatWindow(player, 3, "f", false);
								return;
							}
							festParty = fp;
							break;
						}
					}

					if(festParty == null)
					{
						showChatWindow(player, 3, "b", false);
						return;
					}

					if(festParty.getFestivalLevel() != _festival.getMinLevel())
					{
						showChatWindow(player, 3, "e", false);
						return;
					}

					long offeringCount = bloodOfferings.getCount();

					if(player.destroyItem("Festival", bloodOfferings.getObjectId(), offeringCount, this, true))
					{
						SevenSignsFestival.getInstance().getContrubutePartys().remove(festParty);

						player.sendPacket(new SystemMessage(SystemMessage.YOUR_CONTRIBUTION_SCORE_IS_INCREASED_BY_S1).addNumber(offeringCount));
						FestivalParty currentTopParty = SevenSignsFestival.getInstance().getCurrentTopParty(_festival.getId());
						if(currentTopParty == null || currentTopParty.getScore() < offeringCount)
						{
							festParty.setScore(offeringCount);
							SevenSignsFestival.getInstance().setCurrentTopParty(_festival.getId(), festParty);

							FestivalParty topParty = SevenSignsFestival.getInstance().getTopParty(_festival.getMinLevel());
							if(topParty == null || topParty.getScore() < festParty.getScore())
								SevenSignsFestival.getInstance().setTopParty(_festival.getMinLevel(), festParty);

							SevenSigns.getInstance().updateFestivalScore();
							SevenSignsFestival.getInstance().saveFestivalData();
							showChatWindow(player, 3, "c", false);
						}
						else
							showChatWindow(player, 3, "d", false);
					}
					else
						showChatWindow(player, 3, "g", false);
					break;
				case 4: // Current High Scores
					int dawnFestId = _festival.getCabal() == SevenSigns.CABAL_DAWN ? _festival.getId() : FestivalManager.getInstance().getFestivalIdByCabalLevel(SevenSigns.CABAL_DAWN, _festival.getMinLevel());
					int duskFestId = _festival.getCabal() == SevenSigns.CABAL_DUSK ? _festival.getId() : FestivalManager.getInstance().getFestivalIdByCabalLevel(SevenSigns.CABAL_DUSK, _festival.getMinLevel());
					replace = new FastList<String>();

					FestivalParty dawnParty = SevenSignsFestival.getInstance().getCurrentTopParty(dawnFestId);
					FestivalParty duskParty = SevenSignsFestival.getInstance().getCurrentTopParty(duskFestId);

					FestivalParty topParty = SevenSignsFestival.getInstance().getTopParty(_festival.getMinLevel());

					SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");

					if(dawnParty != null)
					{
						replace.add("%dawnDate%");
						replace.add(format.format(dawnParty.getDate()) + ", ");
						replace.add("%dawnScore%");
						replace.add(String.valueOf(dawnParty.getScore()));
						replace.add("%dawnParty%");
						replace.add(dawnParty.getMembersString());
					}
					else
					{
						replace.add("%dawnDate%");
						replace.add("No record exists.");
						replace.add("%dawnScore%");
						replace.add("0");
						replace.add("%dawnParty%");
						replace.add("");
					}

					if(duskParty != null)
					{
						replace.add("%duskDate%");
						replace.add(format.format(duskParty.getDate()) + ", ");
						replace.add("%duskScore%");
						replace.add(String.valueOf(duskParty.getScore()));
						replace.add("%duskParty%");
						replace.add(duskParty.getMembersString());
					}
					else
					{
						replace.add("%duskDate%");
						replace.add("No record exists.");
						replace.add("%duskScore%");
						replace.add("0");
						replace.add("%duskParty%");
						replace.add("");
					}

					if(topParty != null)
					{
						replace.add("%topDate%");
						replace.add(format.format(topParty.getDate()) + ", ");
						replace.add("%topScore%");
						replace.add(String.valueOf(topParty.getScore()));
						replace.add("%topParty%");
						replace.add(topParty.getMembersString());
						replace.add("%topCabal%");
						String cabal = SevenSigns.getCabalShortName(topParty.getCabal());
						replace.add(cabal.substring(0, 1).toUpperCase() + cabal.substring(1));
					}
					else
					{
						replace.add("%topDate%");
						replace.add("No record exists.");
						replace.add("%topScore%");
						replace.add("0");
						replace.add("%topParty%");
						replace.add("");
						replace.add("%topCabal%");
						replace.add("No Cabal");
					}

					showChatWindow(player, 4, null, false, replace);
					break;
				case 5:
					replace = new FastList<String>();

					for(Festival fest : FestivalManager.getInstance().getFestivals().values())
					{
						replace.add("%" + SevenSigns.getCabalShortName(fest.getCabal()) + "Score" + fest.getMinLevel() + "%");
						FestivalParty fp = SevenSignsFestival.getInstance().getCurrentTopParty(fest.getId());
						replace.add(fp == null ? "0" : String.valueOf(fp.getScore()));
					}

					for(Integer level : FestivalManager.getInstance().getFestivalLevels())
					{
						replace.add("%winCabal" + level + "%");
						String cabal = SevenSigns.getCabalShortName(SevenSignsFestival.getInstance().getWiningCabalForLevel(level));
						replace.add(cabal.substring(0, 1).toUpperCase() + cabal.substring(1));
					}
					showChatWindow(player, 5, null, false, replace);
					break;
				case 6:
					replace = new FastList<String>();

					for(Integer level : FestivalManager.getInstance().getFestivalLevels())
					{
						replace.add("%amount" + level + "%");
						replace.add(String.valueOf(SevenSignsFestival.getInstance().getAccumulatedBonus(level)));
					}

					showChatWindow(player, 6, null, false, replace);
					break;
				case 8: // Increase the Festival Challenge
					if(playerParty == null || !playerParty.isLeader(player))
					{
						showChatWindow(player, 8, "a", false);
						return;
					}

					if(_festival.isIncreased())
					{
						showChatWindow(player, 8, "c", false);
						return;
					}

					_festival.setIncreased(true);

					if(!FestivalManager.getInstance().isRegistrationOpen())
						_festival.increaseSpawn();
					
					showChatWindow(player, 8, "b", false);
					break;
				case 9: // Enter the Festival
					if(!SevenSigns.getInstance().isCompetitionPeriod())
					{
						showChatWindow(player, 2, "a", false);
						return;
					}

					if(SevenSigns.getInstance().getPlayerCabal(player) != _festival.getCabal())
					{
						showChatWindow(player, 2, "k", false);
						return;
					}

					// Check if the player is in a formed party already.
					if(playerParty == null || playerParty.getMemberCount() < FestivalManager.getInstance().getMinPartyMemebers())
					{
						showChatWindow(player, 2, "b", false);
						return;
					}

					// Check if the player is the party leader.
					if(!playerParty.isLeader(player))
					{
						showChatWindow(player, 2, "c", false);
						return;
					}

					// Check if all the party members are in the required level range.
					members = "";
					for(L2Player member : playerParty.getPartyMembers())
					{
						if(member.getLevel() > _festival.getMinLevel())
							members += member.getName() + ", ";
					}

					if(!members.isEmpty())
					{
						replace = new FastList<String>();
						replace.add("%festivalType%");
						replace.add(_festival.getMinLevel() + " or below");
						replace.add("%names%");
						replace.add(members);
						replace.add("%limit%");
						replace.add(Integer.toString(_festival.getMinLevel()));
						showChatWindow(player, 2, "d", false, replace);
						return;
					}

					if(!FestivalManager.getInstance().isRegistrationOpen())
					{
						showChatWindow(player, 2, "f", false);
						return;
					}

					for(L2Player member : playerParty.getPartyMembers())
					{
						for(L2Skill skill : member.getAllSkills())
						{
							if(!skill.isCommon() && SkillTreeTable.getMinSkillLevel(skill.getId(), player.getClassId(), skill.getLevel()) > _festival.getMinLevel())
							{
								members += member.getName() + ", ";
							}
						}
					}

					if(!members.isEmpty())
					{
						replace = new FastList<String>();
						replace.add("%names%");
						replace.add(members);
						showChatWindow(player, 2, "g", false, replace);
						return;
					}

					for(L2Player member : playerParty.getPartyMembers())
					{
						if(SevenSigns.getInstance().getPlayerCabal(member) != _festival.getCabal())
								members += member.getName() + ", ";
					}

					if(!members.isEmpty())
					{
						replace = new FastList<String>();
						replace.add("%names%");
						replace.add(members);
						showChatWindow(player, 2, "i", false, replace);
						return;
					}

					if(_festival.isStarted())
					{
						showChatWindow(player, 2, "j", false);
						return;
					}

					for(L2Player member : playerParty.getPartyMembers())
					{
						if(member.getPet() != null)
						{
							showChatWindow(player, 9, "a", false);
							return;
						}
					}

					stoneType = subVal;
					stonesNeeded = _festival.getCostByStoneId(stoneType);

					if(!player.destroyItemByItemId("FestivalReg", stoneType, stonesNeeded, this, true))
					{
						showChatWindow(player, 2, "h", false);
						return;
					}

					SevenSignsFestival.getInstance().addAccumulatedBonus(_festival.getMinLevel(), stoneType, stonesNeeded);
					SevenSignsFestival.getInstance().saveFestivalData();

					_festival.initFest(playerParty);

					for(L2Player member : playerParty.getPartyMembers())
						if(isInRange(member, 300))
						{
							member.stopAllEffects();
							L2ItemInstance blood = member.getInventory().getItemByItemId(SevenSignsFestival.FESTIVAL_OFFERING_ID);
							if(blood != null)
								member.destroyItem("FestivalTeleport", blood.getObjectId(), blood.getCount(), this, true);
							member.teleToLocation(_festival.getStartLoc());
						}

					break;
				case 10:
					if(!SevenSigns.getInstance().isSealValidationPeriod())
					{
						showChatWindow(player, 10, "c", false);
						return;
					}

					FestivalParty fp = SevenSignsFestival.getInstance().getCurrentTopParty(_festival.getId());
					StatsSet member;
					if(fp == null || (member = fp.getMember(player.getObjectId())) == null || fp.isAborted())
					{
						showChatWindow(player, 10, "a", false);
						return;
					}

					if(member.getBool("bonus"))
					{
						showChatWindow(player, 10, "b", false);
						return;
					}

					int bonus = SevenSignsFestival.getInstance().getAccumulatedBonusByLevel(_festival.getMinLevel()) / fp.getMembers().size();

					if(bonus > 0)
					{
						member.set("bonus", true);
						player.addItem("FestivalBonus", SevenSigns.ANCIENT_ADENA_ID, bonus, this, true);
						SevenSignsFestival.getInstance().saveFestivalData();
						showChatWindow(player, 10, null, false);
					}
					else
						showChatWindow(player, 10, "a", false);
					break;
				case 12:
					_festival.teleportBack(player);
					showChatWindow(player, 12, null, false);
					break;
				default:
					showChatWindow(player, val, null, false);
			}
		}
		else
			// this class dont know any other commands, let forward
			// the command to the parent class
			super.onBypassFeedback(player, command);
	}

	private void showChatWindow(L2Player player, int val, String suffix, boolean isDescription)
	{
		showChatWindow(player, val, suffix, isDescription, null);
	}

	private void showChatWindow(L2Player player, int val, String suffix, boolean isDescription, List<String> replaces)
	{
		player.setLastNpc(this);
		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH + "festival/";
		filename += isDescription ? "desc_" : "festival_";
		filename += suffix != null ? val + suffix + ".htm" : val + ".htm";
		NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
		html.setFile(filename);

		if(replaces != null)
			for(int i = 0; i < replaces.size(); i += 2)
				html.replace(replaces.get(i), Matcher.quoteReplacement(replaces.get(i + 1)));

		player.sendPacket(html);
		player.sendActionFailed();
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;

		switch(getNpcId())
		{
			// Dawn Festival Guides
			case 31127:
			case 31128:
			case 31129:
			case 31130:
			case 31131:
				filename += "festival/dawn_guide.htm";
				break;
			// Dusk Festival Guides
			case 31137:
			case 31138:
			case 31139:
			case 31140:
			case 31141:
				filename += "festival/dusk_guide.htm";
				break;
			// Festival Witches
			case 31132:
			case 31133:
			case 31134:
			case 31135:
			case 31136:
			case 31142:
			case 31143:
			case 31144:
			case 31145:
			case 31146:
				filename += "festival/festival_witch.htm";
				break;
			default:
				filename = getHtmlPath(getNpcId(), val, player.getKarma());
		}

		NpcHtmlMessage html = new NpcHtmlMessage(player, this, filename, val);
		html.replace("%festivalMins%", FestivalManager.getInstance().getMinToStart());
		player.sendPacket(html);
	}
}
