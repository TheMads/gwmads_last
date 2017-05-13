package ru.l2gw.gameserver.model.instances;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.SevenSigns;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.StringTokenizer;

/**
 * Dawn/Dusk Seven Signs Priest Instance
 *
 * @author Tempy
 */
public class L2SignsPriestInstance extends L2NpcInstance
{
	private static Log _log = LogFactory.getLog(L2SignsPriestInstance.class.getName());

	public L2SignsPriestInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	private void showChatWindow(L2Player player, int val, String suffix, boolean isDescription)
	{
		player.setLastNpc(this);
		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;
		filename += isDescription ? "desc_" + val : "signs_" + val;
		filename += suffix != null ? "_" + suffix + ".htm" : ".htm";
		showChatWindow(player, filename);
	}

	private boolean getPlayerAllyHasCastle(L2Player player)
	{
		L2Clan playerClan = player.getClan();

		if(playerClan == null)
			return false;

		if(playerClan.getHasUnit(2))
			return true;
		else if(player.getAllyId() > 0)
		{
			for(L2Clan clan : playerClan.getAlliance().getMembers())
				if(clan.getHasUnit(2))
					return true;
		}

		return false;
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(getNpcId() == 31113 || getNpcId() == 31126)
			if(SevenSigns.getInstance().getPlayerCabal(player) == SevenSigns.CABAL_NULL)
				return;

		// first do the common stuff
		// and handle the commands that all NPC classes know
		super.onBypassFeedback(player, command);

		if(command.startsWith("SevenSignsDesc"))
		{
			int val = Integer.parseInt(command.substring(15));

			showChatWindow(player, val, null, true);
		}
		else if(command.startsWith("SevenSigns"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			int val = 0;
			int subVal = 0;

			try
			{
				if(st.hasMoreTokens())
					val = Integer.parseInt(st.nextToken());

				if(st.hasMoreTokens())
					subVal = Integer.parseInt(st.nextToken());
			}
			catch(NumberFormatException e)
			{
			}

			SystemMessage sm;
			int stoneType;

			L2ItemInstance ancientAdena = player.getInventory().getItemByItemId(SevenSigns.ANCIENT_ADENA_ID);
			long ancientAdenaAmount = ancientAdena == null ? 0 : ancientAdena.getCount();
			int priestCabal = SevenSigns.getCabalNumber(getPriestCabal());

			switch(val)
			{
				case 1:
					showChatWindow(player, val, getPriestCabal(), false);
					break;
				case 2: // Purchase Record of the Seven Signs
					if(!player.getInventory().validateCapacity(1))
					{
						player.sendPacket(Msg.YOUR_INVENTORY_IS_FULL);
						showChatWindow(player, 1, getPriestCabal(), false);
						return;
					}

					if(player.getAdena() >= SevenSigns.RECORD_SEVEN_SIGNS_COST && player.reduceAdena("Buy", SevenSigns.RECORD_SEVEN_SIGNS_COST, this, true))
					{
						player.addItem("Buy", SevenSigns.RECORD_SEVEN_SIGNS_ID, 1, this, true);
						showChatWindow(player, 2, "success", false);
					}
					else
						showChatWindow(player, 2, "noadena", false);
					break;
				case 3: // Join Cabal Intro 1
				case 8: // Festival of Darkness Intro - SevenSigns x [0]1
					showChatWindow(player, val, getPriestCabal(), false);
					break;
				case 10: // Teleport Locations List
					String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;
					if(SevenSigns.getInstance().isSealValidationPeriod())
					{
						if(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_GNOSIS) != SevenSigns.CABAL_NULL)
							filename += getNpcId() + "_gnosis.htm";
						else
							filename += getNpcId() + ".htm";
					}
					else
					{
						if(SevenSigns.getInstance().getPlayerCabal(player) == SevenSigns.CABAL_NULL)
							filename += getPriestCabal() + "_priest_tp_no.htm";
						else
							filename += getNpcId() + ".htm";
					}
					showChatWindow(player, filename);
					break;
				case 4: // Join a Cabal - SevenSigns 4 [0]1 x
					int oldCabal = SevenSigns.getInstance().getPlayerCabal(player);

					if(oldCabal != SevenSigns.CABAL_NULL)
					{
						showChatWindow(player, 4, getPriestCabal() + "_already", false);
						return;
					}
					if(player.getClassId().level() == 0)
					{
						showChatWindow(player, 4, getPriestCabal() + "_newbie", false);
						return;
					}

					if(priestCabal == SevenSigns.CABAL_DUSK && player.getClan() != null && player.getClan().getHasUnit(2)) // reg for dusk with castle
					{
						showChatWindow(player, 4, getPriestCabal() + "_hascastle", false);
						return;
					}

					if(priestCabal == SevenSigns.CABAL_DAWN && player.getClassId().level() >= 2 && !getPlayerAllyHasCastle(player))
					{
						L2ItemInstance temp = player.getInventory().getItemByItemId(SevenSigns.CERTIFICATE_OF_APPROVAL_ID);
						if(!((temp != null && temp.getCount() > 0 && player.destroyItemByItemId("JoinSS", SevenSigns.CERTIFICATE_OF_APPROVAL_ID, 1, this, true)) || player.reduceAdena("JoinSS", SevenSigns.ADENA_JOIN_DAWN_COST, this, true)))
						{
							// no certificate or adena
							showChatWindow(player, 4, "no_adena", false);
							return;
						}
					}

					SevenSigns.getInstance().setPlayerInfo(player, priestCabal, subVal);

					if(priestCabal == SevenSigns.CABAL_DAWN)
						player.sendPacket(new SystemMessage(SystemMessage.YOU_WILL_PARTICIPATE_IN_THE_SEVEN_SIGNS_AS_A_MEMBER_OF_THE_LORDS_OF_DAWN)); // Joined Dawn
					else
						player.sendPacket(new SystemMessage(SystemMessage.YOU_WILL_PARTICIPATE_IN_THE_SEVEN_SIGNS_AS_A_MEMBER_OF_THE_REVOLUTIONARIES_OF_DUSK)); // Joined Dusk

					//Show a confirmation message to the user, indicating which seal they chose.
					switch(subVal)
					{
						case SevenSigns.SEAL_AVARICE:
							player.sendPacket(new SystemMessage(SystemMessage.YOUVE_CHOSEN_TO_FIGHT_FOR_THE_SEAL_OF_AVARICE_DURING_THIS_QUEST_EVENT_PERIOD));
							break;
						case SevenSigns.SEAL_GNOSIS:
							player.sendPacket(new SystemMessage(SystemMessage.YOUVE_CHOSEN_TO_FIGHT_FOR_THE_SEAL_OF_GNOSIS_DURING_THIS_QUEST_EVENT_PERIOD));
							break;
						case SevenSigns.SEAL_STRIFE:
							player.sendPacket(new SystemMessage(SystemMessage.YOUVE_CHOSEN_TO_FIGHT_FOR_THE_SEAL_OF_STRIFE_DURING_THIS_QUEST_EVENT_PERIOD));
							break;
					}

					showChatWindow(player, 4, SevenSigns.getCabalShortName(priestCabal), false);
					break;
				case 5:
					filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;
					if(SevenSigns.getInstance().getPlayerCabal(player) == SevenSigns.CABAL_NULL)
					{
						filename += getPriestCabal() + "_priest_sealstones_no.htm";
						showChatWindow(player, filename);
					}
					else
						showChatWindow(player, val, null, false);
					break;
				case 6: // Contribute Seal Stones - SevenSigns 6 x
					long redStoneCount = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_RED_ID) == null ? 0 : player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_RED_ID).getCount();
					long greenStoneCount = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_GREEN_ID) == null ? 0 : player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_GREEN_ID).getCount();
					long blueStoneCount = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_BLUE_ID) == null ? 0 : player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_BLUE_ID).getCount();
					long contribScore = SevenSigns.getInstance().getPlayerContribScore(player);

					if(contribScore >= SevenSigns.MAXIMUM_PLAYER_CONTRIB)
						player.sendPacket(new SystemMessage(SystemMessage.CONTRIBUTION_LEVEL_HAS_EXCEEDED_THE_LIMIT_YOU_MAY_NOT_CONTINUE));
					else if(subVal == 4)
					{
						long redContribCount;
						long greenContribCount;
						long blueContribCount;
						long tempContribScore = contribScore;

						redContribCount = (SevenSigns.MAXIMUM_PLAYER_CONTRIB - tempContribScore) / SevenSigns.RED_CONTRIB_POINTS;
						if(redContribCount > redStoneCount)
							redContribCount = redStoneCount;
						tempContribScore += redContribCount * SevenSigns.RED_CONTRIB_POINTS;

						greenContribCount = (SevenSigns.MAXIMUM_PLAYER_CONTRIB - tempContribScore);
						if(greenContribCount > greenStoneCount)
							greenContribCount = greenStoneCount;
						tempContribScore += greenContribCount;

						blueContribCount = (SevenSigns.MAXIMUM_PLAYER_CONTRIB - tempContribScore);
						if(blueContribCount > blueStoneCount)
							blueContribCount = blueStoneCount;

						contribScore = SevenSigns.getInstance().addPlayerStoneContrib(player, blueContribCount, greenContribCount, redContribCount);
						if(contribScore == -1)
						{
							player.sendPacket(new SystemMessage(SystemMessage.CONTRIBUTION_LEVEL_HAS_EXCEEDED_THE_LIMIT_YOU_MAY_NOT_CONTINUE));
							return;
						}
						if(redContribCount > 0)
						{
							L2ItemInstance temp = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_RED_ID);
							if(temp != null && temp.getCount() >= redContribCount)
								player.destroyItemByItemId("SevenSigns", SevenSigns.SEAL_STONE_RED_ID, redContribCount, this, true);
						}
						if(greenContribCount > 0)
						{
							L2ItemInstance temp = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_GREEN_ID);
							if(temp != null && temp.getCount() >= greenContribCount)
								player.destroyItemByItemId("SevenSigns", SevenSigns.SEAL_STONE_GREEN_ID, greenContribCount, this, true);
						}
						if(blueContribCount > 0)
						{
							L2ItemInstance temp = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_BLUE_ID);
							if(temp != null && temp.getCount() >= blueContribCount)
								player.destroyItemByItemId("SevenSigns", SevenSigns.SEAL_STONE_BLUE_ID, blueContribCount, this, true);
						}

						if(redContribCount > 0 || greenContribCount > 0 || blueContribCount > 0)
						{

							sm = new SystemMessage(SystemMessage.YOUR_CONTRIBUTION_SCORE_IS_INCREASED_BY_S1);
							sm.addNumber(contribScore);
							player.sendPacket(sm);

							showChatWindow(player, 27, "success", false);
						}
						else
							showChatWindow(player, 27, "no_stones", false);
					}
					else if(subVal >= 1 && subVal <= 3)
					{
						long stoneCount = player.getInventory().getItemByItemId(SevenSigns.getStoneIdByType(subVal)) != null ? player.getInventory().getItemByItemId(SevenSigns.getStoneIdByType(subVal)).getCount() : 0;
						NpcHtmlMessage html = new NpcHtmlMessage(player, this, SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_6_ss.htm", 0);
						html.replace("%stoneCount%", String.valueOf(stoneCount));
						html.replace("%stoneType%", String.valueOf(subVal));
						html.replace("%stoneName%", ItemTable.getInstance().createDummyItem(SevenSigns.getStoneIdByType(subVal)).getItem().getName());
						player.sendPacket(html);
					}
					break;
				case 7: // Exchange Ancient Adena for Adena - SevenSigns 7 xxxxxxx
					if(ancientAdenaAmount < subVal || subVal < 1)
					{
						player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
						return;
					}

					player.destroyItemByItemId("ExchangeAA", SevenSigns.ANCIENT_ADENA_ID, subVal, this, true);
					player.addAdena("ExchangeAA", subVal, this, true);
					break;
				case 9: // Receive Contribution Rewards
					int playerCabal = SevenSigns.getInstance().getPlayerCabal(player);
					int winningCabal = SevenSigns.getInstance().getCabalWinner();

					if(SevenSigns.getInstance().isSealValidationPeriod() && playerCabal == winningCabal)
					{
						int ancientAdenaReward = SevenSigns.getInstance().getAncientAdenaReward(player, true);
						if(SevenSigns.getInstance().getPlayerStoneContrib(player) > 0 && ancientAdenaReward > 0)
						{
							player.addItem("RevardAA", SevenSigns.ANCIENT_ADENA_ID, ancientAdenaReward, this, true);
							SevenSigns.getInstance().clearPlayerStoneContrib(player);
							showChatWindow(player, 9, "a", false);
						}
						else
							showChatWindow(player, 9, "b", false);
					}
					break;
				case 11: // Teleport to Hunting Grounds
					try
					{
						String portInfo = command.substring(14).trim();

						st = new StringTokenizer(portInfo);
						int x = Integer.parseInt(st.nextToken());
						int y = Integer.parseInt(st.nextToken());
						int z = Integer.parseInt(st.nextToken());
						long ancientAdenaCost = Long.parseLong(st.nextToken());

						if(ancientAdenaCost > 0)
						{
							L2ItemInstance temp = player.getInventory().getItemByItemId(SevenSigns.ANCIENT_ADENA_ID);
							if(temp == null || ancientAdenaCost > temp.getCount())
							{
								player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
								return;
							}
							player.destroyItemByItemId("PristTeleport", SevenSigns.ANCIENT_ADENA_ID, ancientAdenaCost, this, true);
						}
						player.teleToLocation(x, y, z);
					}
					catch(Exception e)
					{
						_log.warn("SevenSigns: Error occurred while teleporting player: " + e);
					}
					break;
				case 17: // Exchange Seal Stones for Ancient Adena (Type Choice) - SevenSigns 17 x
					stoneType = subVal;

					if(stoneType == 4)
					{
						L2ItemInstance BlueStoneInstance = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_BLUE_ID);
						long bcount = BlueStoneInstance != null ? BlueStoneInstance.getCount() : 0;
						L2ItemInstance GreenStoneInstance = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_GREEN_ID);
						long gcount = GreenStoneInstance != null ? GreenStoneInstance.getCount() : 0;
						L2ItemInstance RedStoneInstance = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_RED_ID);
						long rcount = RedStoneInstance != null ? RedStoneInstance.getCount() : 0;
						long ancientAdenaReward = SevenSigns.calcAncientAdenaReward(bcount, gcount, rcount);
						if(ancientAdenaReward > 0)
						{
							if(BlueStoneInstance != null)
								player.destroyItem("ExchangeStones", BlueStoneInstance.getObjectId(), bcount, this, true);
							if(GreenStoneInstance != null)
								player.destroyItem("ExchangeStones", GreenStoneInstance.getObjectId(), gcount, this, true);
							if(RedStoneInstance != null)
								player.destroyItem("ExchangeStones", RedStoneInstance.getObjectId(), rcount, this, true);

							player.addItem("ExchangeStones", SevenSigns.ANCIENT_ADENA_ID, ancientAdenaReward, this, true);
							showChatWindow(player, 18, "success", false);
						}
						else
							showChatWindow(player, 18, "no_stones", false);
						break;
					}
					else if(stoneType >= 1 && stoneType <= 3)
					{
						int stoneValue[] = {SevenSigns.BLUE_CONTRIB_POINTS, SevenSigns.GREEN_CONTRIB_POINTS, SevenSigns.RED_CONTRIB_POINTS};
						long stoneCount = player.getInventory().getItemByItemId(SevenSigns.getStoneIdByType(subVal)) != null ? player.getInventory().getItemByItemId(SevenSigns.getStoneIdByType(subVal)).getCount() : 0;
						NpcHtmlMessage html = new NpcHtmlMessage(player, this, SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_17.htm", 0);
						html.replace("%stoneCount%", String.valueOf(stoneCount));
						html.replace("%stoneType%", String.valueOf(subVal));
						html.replace("%stoneValue%", String.valueOf(stoneValue[subVal - 1]));
						html.replace("%stoneName%", ItemTable.getInstance().createDummyItem(SevenSigns.getStoneIdByType(subVal)).getItem().getName());
						player.sendPacket(html);
					}
					break;
				case 18:
					if(subVal >= 1 && subVal <= 3)
					{
						int convertCount = 0;
						try
						{
							convertCount = Integer.parseInt(command.substring(16).trim());
						}
						catch(Exception e)
						{
						}

						long stoneCount = player.getInventory().getItemByItemId(SevenSigns.getStoneIdByType(subVal)) != null ? player.getInventory().getItemByItemId(SevenSigns.getStoneIdByType(subVal)).getCount() : 0;

						if(convertCount <= 0)
							showChatWindow(player, 18, "no_input", false);
						else if(stoneCount < convertCount)
							showChatWindow(player, 18, "toomany", false);
						else
						{
							if(player.destroyItemByItemId("SevenSigns", SevenSigns.getStoneIdByType(subVal), convertCount, this, true))
							{
								long aa = 0;
								switch(subVal)
								{
									case 1:
										aa = SevenSigns.calcAncientAdenaReward(convertCount, 0, 0);
										break;
									case 2:
										aa = SevenSigns.calcAncientAdenaReward(0, convertCount, 0);
										break;
									case 3:
										aa = SevenSigns.calcAncientAdenaReward(0, 0, convertCount);
										break;
								}
								player.addItem("SevenSigns", SevenSigns.ANCIENT_ADENA_ID, aa, this, true);
								showChatWindow(player, 18, "success", false);
							}
							else
								showChatWindow(player, 18, "no_input", false);
						}
					}
					break;
				case 19: // Seal Information (for when joining a cabal)
					String fileSuffix = SevenSigns.getSealName(subVal, true) + "_" + SevenSigns.getCabalShortName(priestCabal);

					showChatWindow(player, val, fileSuffix, false);
					break;
				case 20: // Seal Status (for when joining a cabal)
					StringBuffer contentBuffer = new StringBuffer("<html><body><font color=\"LEVEL\">[Seal Status]</font><br>");

					for(int i = 1; i < 4; i++)
					{
						int sealOwner = SevenSigns.getInstance().getSealOwner(i);
						if(sealOwner != SevenSigns.CABAL_NULL)
							contentBuffer.append("[" + SevenSigns.getSealName(i, false) + ": " + SevenSigns.getCabalName(sealOwner) + "]<br>");
						else
							contentBuffer.append("[" + SevenSigns.getSealName(i, false) + ": Nothingness]<br>");
					}

					contentBuffer.append("<a action=\"bypass -h npc_" + getObjectId() + "_SevenSigns 3\">Go back.</a></body></html>");

					NpcHtmlMessage html2 = new NpcHtmlMessage(_objectId);
					html2.setHtml(contentBuffer.toString());
					player.sendPacket(html2);
					break;
				case 25:
					if(SevenSigns.getInstance().getPlayerCabal(player) != SevenSigns.CABAL_NULL)
						showChatWindow(player, 25, getPriestCabal() + "_already", false);
					else if(!getPlayerAllyHasCastle(player) && getPriestCabal().equalsIgnoreCase("dusk"))
						showChatWindow(player, 26, "dusk", false);
					else if((getPlayerAllyHasCastle(player) || player.getClassId().level() < 2) && getPriestCabal().equalsIgnoreCase("dawn"))
						showChatWindow(player, 26, "dawn", false);
					else
						showChatWindow(player, 25, getPriestCabal(), false);
					break;
				case 26:
					showChatWindow(player, 26, getPriestCabal(), false);
					break;
				case 27: // Contribute seal stones SevenSigns 27 x xxxxxx
					if(subVal >= 1 && subVal <= 3)
					{
						int convertCount = 0;
						try
						{
							convertCount = Integer.parseInt(command.substring(16).trim());
						}
						catch(Exception e)
						{
						}

						long stoneCount = player.getInventory().getItemByItemId(SevenSigns.getStoneIdByType(subVal)) != null ? player.getInventory().getItemByItemId(SevenSigns.getStoneIdByType(subVal)).getCount() : 0;

						if(convertCount <= 0)
							showChatWindow(player, 27, "no_input", false);
						else if(stoneCount < convertCount)
							showChatWindow(player, 27, "toomany", false);
						else
						{
							contribScore = SevenSigns.getInstance().getPlayerContribScore(player);
							if(contribScore == -1)
							{
								player.sendPacket(new SystemMessage(SystemMessage.CONTRIBUTION_LEVEL_HAS_EXCEEDED_THE_LIMIT_YOU_MAY_NOT_CONTINUE));
								return;
							}

							long score = 0;
							switch(subVal)
							{
								case 1:
									score = SevenSigns.getInstance().addPlayerStoneContrib(player, convertCount, 0, 0);
									break;
								case 2:
									score = SevenSigns.getInstance().addPlayerStoneContrib(player, 0, convertCount, 0);
									break;
								case 3:
									score = SevenSigns.getInstance().addPlayerStoneContrib(player, 0, 0, convertCount);
									break;
							}
							if(score == -1)
							{
								player.sendPacket(new SystemMessage(SystemMessage.CONTRIBUTION_LEVEL_HAS_EXCEEDED_THE_LIMIT_YOU_MAY_NOT_CONTINUE));
								return;
							}

							if(player.destroyItemByItemId("SevenSigns", SevenSigns.getStoneIdByType(subVal), convertCount, this, true))
							{
								player.sendPacket(new SystemMessage(SystemMessage.YOUR_CONTRIBUTION_SCORE_IS_INCREASED_BY_S1).addNumber(score));
								showChatWindow(player, 27, "success", false);
							}
							else
								showChatWindow(player, 27, "no_input", false);
						}
					}
					break;
				case 28:
					showChatWindow(player, val, getPriestCabal(), false);
					break;
				default:
					showChatWindow(player, val, null, false);
					break;
			}
		}
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		player.setLastNpc(this);
		int npcId = getTemplate().npcId;

		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;

		int sealAvariceOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_AVARICE);
		int sealGnosisOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_GNOSIS);
		int playerCabal = SevenSigns.getInstance().getPlayerCabal(player);
		boolean isSealValidationPeriod = SevenSigns.getInstance().isSealValidationPeriod();
		int compWinner = SevenSigns.getInstance().getCabalWinner();
		int priestCabal = SevenSigns.getCabalNumber(getPriestCabal());

		switch(npcId)
		{
			case 31078: // Dawn Priests
			case 31079:
			case 31080:
			case 31081:
			case 31082:
			case 31083:
			case 31084:
			case 31168:
			case 31692:
			case 31694:
			case 31997:
			case 31085: // Dusk Priest
			case 31086:
			case 31087:
			case 31088:
			case 31089:
			case 31090:
			case 31091:
			case 31169:
			case 31693:
			case 31695:
			case 31998:
				if(player.getKarma() > 0)
					filename += getPriestCabal() + "_priest_pk.htm";
				else if(SevenSigns.getInstance().getCurrentPeriod() == SevenSigns.PERIOD_COMP_RECRUITING || SevenSigns.getInstance().getCurrentPeriod() == SevenSigns.PERIOD_COMP_RESULTS)
					filename += getPriestCabal() + "_priest_blocked.htm";
				else if(playerCabal == SevenSigns.CABAL_NULL)
				{
					if(player.getClassId().level() == 0)
						filename += getPriestCabal() + "_priest_noob.htm";
					else if(isSealValidationPeriod)
					{
						if(compWinner == priestCabal)
							filename += getPriestCabal() + "_priest_4.htm";
						else
							filename += getPriestCabal() + "_priest_2b.htm";
					}
					else
						filename += getPriestCabal() + "_priest_1a.htm";
				}
				else if(playerCabal == priestCabal)
				{
					if(isSealValidationPeriod)
					{
						if(compWinner == priestCabal)
							filename += getPriestCabal() + "_priest_2a.htm";
						else
							filename += getPriestCabal() + "_priest_2b.htm";
					}
					else
						filename += getPriestCabal() + "_priest_1b.htm";
				}
				else if(isSealValidationPeriod)
					filename += getPriestCabal() + "_priest_3b.htm";
				else
					filename += getPriestCabal() + "_priest_3a.htm";
				break;
			case 31092: // Black Marketeer of Mammon
				filename += "blkmrkt_1.htm";
				break;
			case 31113: // Merchant of Mammon
				switch(compWinner)
				{
					case SevenSigns.CABAL_DAWN:
						if(playerCabal != compWinner || playerCabal != sealAvariceOwner)
						{
							player.sendPacket(new NpcHtmlMessage(player, this, filename + "mammmerch_2.htm", val));
							return;
						}
						break;
					case SevenSigns.CABAL_DUSK:
						if(playerCabal != compWinner || playerCabal != sealAvariceOwner)
						{
							player.sendPacket(new NpcHtmlMessage(player, this, filename + "mammmerch_2.htm", val));
							return;
						}
						break;
				}
				filename += "mammmerch_1.htm";
				break;
			case 31126: // Blacksmith of Mammon
				switch(compWinner)
				{
					case SevenSigns.CABAL_DAWN:
						if(playerCabal != compWinner || playerCabal != sealGnosisOwner)
						{
							player.sendPacket(new NpcHtmlMessage(player, this, filename + "mammblack_2.htm", val));
							return;
						}
						break;
					case SevenSigns.CABAL_DUSK:
						if(playerCabal != compWinner || playerCabal != sealGnosisOwner)
						{
							player.sendPacket(new NpcHtmlMessage(player, this, filename + "mammblack_2.htm", val));
							return;
						}
						break;
				}
				filename += "mammblack_1.htm";
				break;
			default:
				filename = getHtmlPath(npcId, val, player.getKarma());
		}

		player.sendPacket(new NpcHtmlMessage(player, this, filename, val));
	}

	private String getPriestCabal()
	{
		return SevenSigns.getCabalShortName(SevenSigns.getInstance().getPriestCabal(getNpcId()));
	}
}
