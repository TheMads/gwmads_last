package ru.l2gw.gameserver.clientpackets;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.extensions.listeners.PropertyCollection;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.handler.IVoicedCommandHandler;
import ru.l2gw.gameserver.handler.VoicedCommandHandler;
import ru.l2gw.gameserver.instancemanager.PartyRoomManager;
import ru.l2gw.gameserver.instancemanager.PetitionManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.PartyRoom;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.Say2;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.MapRegionTable;
import ru.l2gw.gameserver.taskmanager.ItemLinksManager;
import ru.l2gw.util.Util;

public class Say2C extends L2GameClientPacket
{
	private static final Log chatLog = LogFactory.getLog("chat");
	private static final Log banChatLog = LogFactory.getLog("banchat");

	public final static int ALL = 0;
	public final static int ALL_CHAT_RANGE = 1250; //Дальность белого чата
	public final static int SHOUT = 1; //!
	public final static int TELL = 2; //\"
	public final static int PARTY = 3; //#
	public final static int CLAN = 4; //@
	public final static int GM = 5;
	public final static int PETITION_PLAYER = 6; // used for petition
	public final static int PETITION_GM = 7; //* used for petition
	public final static int TRADE = 8; //+
	public final static int ALLIANCE = 9; //$
	public final static int ANNOUNCEMENT = 10;
	public final static int SYSTEM_SHOUT = 11;
	public final static int L2FRIEND = 12;
	public final static int MSNCHAT = 13;
	public final static int PARTYROOM_MATCHING = 14;
	public final static int COMMANDCHANNEL_ALL = 15; //`` (pink) команды лидера СС
	public final static int COMMANDCHANNEL_COMMANDER = 16; //` (yellow) команды лидеров партий в СС
	public final static int HERO_VOICE = 17; //%
	public final static int CRITICAL_ANNOUNCEMENT = 18; //dark cyan
	public final static int SCREEN_ANNOUNCE = 19;
	public final static int BATTLEFIELD = 20;
	public final static int MPCC_ROOM = 21;

	private static final String AltLetters[][] = {
			{ "h", "н" },
			{ "x", "х" },
			{ "a", "а" },
			{ "b", "ь" },
			{ "3", "з" },
			{ "c", "с" },
			{ "r", "я" },
			{ "0", "о" },
			{ "o", "о" },
			{ "m", "м" },
			{ "p", "р" },
			{ "e", "е" },
			{ "ё", "е" },
			{ "jl", "л" },
			{ "6", "б" },
			{ "y", "у" },
			{ "k", "к" },
			{ "9[^a-zA-Zа-яА-Я]", "я" },
			{ "9l*", "я" } };

	public static String[] chatNames = {
			"ALL	",
			"SHOUT",
			"TELL ",
			"PARTY",
			"CLAN",
			"GM	 ",
			"PETITION_PLAYER",
			"PETITION_GM",
			"TRADE",
			"ALLIANCE",
			"ANNOUNCEMENT",
			"",
			"",
			"",
			"PARTYROOM_MATCHING",
			"COMMANDCHANNEL_ALL",
			"COMMANDCHANNEL_COMMANDER",
			"HERO_VOICE",
			"CRITICAL_ANNOUNCEMENT",
			"UNKNOWN",
			"BATTLEFIELD"};

	private String _text;
	private int _type;
	private String _target;

	/**
	 * packet type id 0x49
	 * format:		cSd (S)
	 */
	@Override
	public void readImpl()
	{
		_text = readS();
		_text = _text.replaceAll("\\\\n", "");
		try
		{
			_type = readD();
		}
		catch(Exception e)
		{
			_type = 0;
		}
		_target = _type == TELL ? readS() : null;
	}

	@Override
	public void runImpl()
	{
		if(Config.DEBUG)
			_log.info("Say type:" + _type);
		L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		if(L2ObjectsStorage.getPlayer(player.getName()) == null)
		{
			player.logout(false, false, false);
		}

		if(_type > chatNames.length || _text == null || _text.length() == 0)
		{
			player.sendActionFailed();
			return;
		}

		if(_text.startsWith("."))
		{
			String fullcmd = _text.substring(1).trim();
			String command = fullcmd.split("\\s+")[0];
			String args = fullcmd.substring(command.length()).trim();

			if(command.length() > 0)
			{
				// then check for VoicedCommands
				IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getVoicedCommandHandler(command);
				if(vch != null)
				{
					vch.useVoicedCommand(command, player, args);
					return;
				}
			}
		}

		if(_text.matches("\\s+"))
		{
			player.sendActionFailed();
			return;
		}

		if(_text != null && _text.length() > 300)
			_text = _text.substring(0, 300);

		boolean globalchat = _type != ALLIANCE && _type != CLAN && _type != PARTY;
		boolean chan_banned = false;
		for(int i = 0; i <= Config.MAT_BAN_COUNT_CHANNELS; i++)
			if(_type == Config.BAN_CHANNEL_LIST[i])
				chan_banned = true;
		boolean indecent_block = false;
		for(int i = 0; i <= Config.INDECENT_BLOCK_COUNT_CHANNELS; i++)
			if(_type == Config.BAN_CHANNEL_LIST[i])
				indecent_block = true;
		if((globalchat || chan_banned) && player.getNoChannel() != 0)
		{
			if(player.getNoChannelRemained() > 0 || player.getNoChannel() < 0)
			{
				if(player.getNoChannel() > 0)
				{
					int timeRemained = Math.round(player.getNoChannelRemained() / 60000);
					player.sendMessage(new CustomMessage("common.ChatBanned", player).addNumber(timeRemained));
				}
				else
					player.sendMessage(new CustomMessage("common.ChatBannedPermanently", player));
				player.sendActionFailed();
				return;
			}
			player.updateNoChannel(0);
		}

		String _textCheck = _text.replaceAll(" ", "").toLowerCase();
		for(String[] AltLetter : AltLetters)
			_textCheck = _textCheck.replaceAll(AltLetter[0], AltLetter[1]);

		if(chan_banned && !player.isGM())
		{
			if(Config.MAT_REPLACE && !Config.MAT_BANCHAT)
			{
				for(String pattern : Config.OBSCENE_LIST)
				{
					if(_text.matches(".*" + pattern + ".*"))
					{
						_text = Config.MAT_REPLACE_STRING;
						player.sendActionFailed();
					}
				}
			}
			else if(Config.MAT_BANCHAT)
			{
				for(String pattern : Config.OBSCENE_LIST)
				{
					if(_textCheck.matches(".*" + pattern + ".*"))
					{
						player.sendMessage("You are banned in all chats. Time to unban: " + Config.UNCHATBANTIME * 60 + "sec.");
						banChatLog.info(chatNames[_type] + " " + player + " chat banned msg{" + _text + "} " + pattern);
						player.updateNoChannel(Config.UNCHATBANTIME * 60000);
						player.sendActionFailed();
						return;
					}
				}
			}
		}
		if(indecent_block && Config.INDECENT_BLOCKCHAT && !player.isGM())
		{
			for(String pattern : Config.INDECENT_LIST)
			{
				if(_textCheck.matches(".*" + pattern + ".*"))
				{
					player.sendMessage("Your message blocked");
					banChatLog.info(player + " blocked msg{" + _text + "} " + pattern);
					player.sendActionFailed();
					return;
				}
			}
		}
		if(_type == SHOUT && Config.SHOUT_FILTER && !player.isGM())
		{
			for(String pattern : Config.SHOUT_LIST)
			{
				if(_textCheck.matches(".*" + pattern + ".*"))
				{
					player.sendMessage("Your message blocked in shout chat");
					banChatLog.info(chatNames[_type] + " " + player + " blocked msg{" + _text + "} " + pattern);
					player.sendActionFailed();
					return;
				}
			}
		}

		String logMessage = null;
		if(Config.LOG_CHAT)
		{
			logMessage = chatNames[_type];
			if(_type == CLAN)
				logMessage += "{" + player.getClanId() + "} ";
			else if(_type == PARTY)
				logMessage += "{" + Util.getPartyId(player) + "} ";
			else if(_type == ALLIANCE)
				logMessage += "{" + player.getAllyId() + "} ";
			else
				logMessage += " ";

			if(_type == TELL)
				logMessage += "[" + player.getName() + " to " + _target + "] " + _text;
			else
				logMessage += "[" + player.getName() + "] " + _text;
		}

		Say2 cs = new Say2(player.getObjectId(), _type, player.getName(), _text);
		int mapregion = MapRegionTable.getInstance().getMapRegion(player.getX(), player.getY());
		ItemLinksManager.getInstance().addItemLinks(player, _text);

		switch(_type)
		{
			case TELL:
				if(player.getLevel() < Config.MIN_LEVEL_FOR_PM)
				{
					player.sendPacket(Msg.CHAT_DISABLED);
					return;
				}
				L2Player receiver = L2ObjectsStorage.getPlayer(_target);
				if(receiver != null)
				{
					if(receiver.isInOfflineMode())
					{
						player.sendMessage("The person is in offline trade mode");
						player.sendActionFailed();
					}
					else if(receiver.isInBlockList(player) || receiver.isBlockAll())
					{
						player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_BEEN_BLOCKED_FROM_THE_CONTACT_YOU_SELECTED));
						player.sendActionFailed();
					}
					else if(receiver.getMessageRefusal())
						player.sendPacket(new SystemMessage(SystemMessage.THE_PERSON_IS_IN_A_MESSAGE_REFUSAL_MODE));
					else
					{
						for(String pattern : Config.PRIVATE_LIST)
							if(_text.toLowerCase().matches(".*" + pattern + ".*"))
							{
								if(player.getLastHWID() != null && !player.getLastHWID().equals(receiver.getLastHWID()) && !player.getNetConnection().getIpAddr().equals(receiver.getNetConnection().getIpAddr()))
								{
									banChatLog.info(chatNames[_type] + " " + player + " blocked msg{" + _text + "} " + pattern);
									player.sendPacket(new Say2(player.getObjectId(), _type, "->" + receiver.getName(), _text));
									return;
								}
								else
									banChatLog.info(chatNames[_type] + " " + player + " blocked msg{" + _text + "} " + pattern);
							}

						if(Config.LOG_PRIVATE_MESSAGE_COUNT > 0)
						{
							Object lm = player.getProperty("lastMessage");
							if(lm == null)
							{
								player.addProperty("lastMessage", _text);
								player.addProperty("lastMessageCount", 1);
							}
							else
							{
								String msg = (String) lm;
								int c = (Integer) player.getProperty("lastMessageCount");
								if(_text.equalsIgnoreCase(msg))
								{
									c++;
									if(c >= Config.LOG_PRIVATE_MESSAGE_COUNT)
									{
										c = 1;
										banChatLog.info(chatNames[_type] + " " + player.getNetConnection()._prot_info + " repeat msg{" + _text + "}");
									}
									player.addProperty("lastMessageCount", c);
								}
								else
								{
									player.addProperty("lastMessageCount", 1);
									player.addProperty("lastMessage", _text);
								}
							}
						}

						chatLog.info(logMessage);
						receiver.sendPacket(cs);
						cs = new Say2(player.getObjectId(), _type, "->" + receiver.getName(), _text);
						player.sendPacket(cs);
					}
				}
				else
				{
					player.sendPacket(new SystemMessage(SystemMessage.S1_IS_NOT_LOGGED_IN).addString(_target));
					player.sendActionFailed();
				}
				break;
			case SHOUT:
				if(player.isCursedWeaponEquipped())
				{
					player.sendPacket(new SystemMessage(SystemMessage.SHOUT_AND_TRADE_CHATING_CANNOT_BE_USED_SHILE_POSSESSING_A_CURSED_WEAPON));
					return;
				}
				if(player.inObserverMode() && player.getOlympiadGameId() < 0)
				{
					player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_CHAT_LOCALLY_WHILE_OBSERVING));
					return;
				}
				if(player.getLevel() < Config.MIN_LEVEL_FOR_SHOUT)
				{
					player.sendPacket(Msg.CHAT_DISABLED);
					return;
				}

				if(Config.SHOUT_OFF_ON_REGION == MapRegionTable.getInstance().getMapRegion(player.getX(), player.getY()) && !player.isGM())
				{
					player.sendMessage("Shout mode at this region now is turned off by GM!");
					return;
				}
				if(player.isInZone(L2Zone.ZoneType.jail) && !player.isGM())
				{
					player.sendMessage(new CustomMessage("admin.jailed.shout", player));
					return;
				}

				chatLog.info(logMessage);
				if(player.getLevel() >= Config.GLOBAL_CHAT || player.isGM())
				{
					for(L2Player player1 : L2ObjectsStorage.getAllPlayers())
						if(!player1.isInBlockList(player) && !player1.isBlockAll())
							player1.sendPacket(cs);
				}
				else
				{
					if(Config.SHOUT_CHAT_MODE == 1)
					{
						for(L2Player player1 : player.getAroundPlayers(20000))
							if(player1.getReflection() == player.getReflection() && !player1.isInBlockList(player) && !player1.isBlockAll() && player1 != player)
								player1.sendPacket(cs);
					}
					else
						for(L2Player player1 : L2ObjectsStorage.getAllPlayers())
							if(player1.getReflection() == player.getReflection() && MapRegionTable.getInstance().getMapRegion(player1.getX(), player1.getY()) == mapregion && !player1.isInBlockList(player) && !player1.isBlockAll() && player1 != player)
								player1.sendPacket(cs);
					player.sendPacket(cs);
				}
				break;
			case TRADE:
				if(player.isCursedWeaponEquipped())
				{
					player.sendPacket(new SystemMessage(SystemMessage.SHOUT_AND_TRADE_CHATING_CANNOT_BE_USED_SHILE_POSSESSING_A_CURSED_WEAPON));
					return;
				}
				if(player.inObserverMode() && player.getOlympiadGameId() < 0)
				{
					player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_CHAT_LOCALLY_WHILE_OBSERVING));
					return;
				}
				if(player.getLevel() < Config.MIN_LEVEL_FOR_TRADE)
				{
					player.sendPacket(Msg.CHAT_DISABLED);
					return;
				}
				if(player.isInZone(L2Zone.ZoneType.jail) && !player.isGM())
				{
					player.sendMessage(new CustomMessage("admin.jailed.shout", player));
					return;
				}

				chatLog.info(logMessage);
				if(player.getLevel() >= Config.GLOBAL_TRADE_CHAT || player.isGM())
				{
					for(L2Player player1 : L2ObjectsStorage.getAllPlayers())
						if(!player1.isInBlockList(player) && !player1.isBlockAll())
							player1.sendPacket(cs);
				}
				else
				{
					if(Config.TRADE_CHAT_MODE == 1)
					{
						for(L2Player player1 : player.getAroundPlayers(20000))
							if(player1.getReflection() == player.getReflection() && !player1.isInBlockList(player) && !player1.isBlockAll() && player1 != player)
								player1.sendPacket(cs);
					}
					else
						for(L2Player player1 : L2ObjectsStorage.getAllPlayers())
							if(player1.getReflection() == player.getReflection() && MapRegionTable.getInstance().getMapRegion(player1.getX(), player1.getY()) == mapregion && !player1.isInBlockList(player) && !player1.isBlockAll() && player1 != player)
								player1.sendPacket(cs);
					player.sendPacket(cs);
				}
				break;
			case ALL:
				if(player.inObserverMode() && player.getOlympiadGameId() < 0)
				{
					player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_CHAT_LOCALLY_WHILE_OBSERVING));
					return;
				}

				chatLog.info(logMessage);
				if(player.isCursedWeaponEquipped())
					cs = new Say2(player.getObjectId(), _type, player.getTransformationName(), _text);
				for(L2Player player1 : player.getAroundPlayers(ALL_CHAT_RANGE))
					if(!player1.isInBlockList(player) && !player1.isBlockAll() && player1 != player)
						player1.sendPacket(cs);
				player.sendPacket(cs);
				break;
			case CLAN:
				if(player.getClanId() != 0)
				{
					chatLog.info(logMessage);
					player.getClan().broadcastToOnlineMembers(cs);
				}
				else
					player.sendActionFailed();
				break;
			case ALLIANCE:
				if(player.getClanId() > 0 && player.getAllyId() > 0)
				{
					chatLog.info(logMessage);
					player.getAlliance().broadcastToOnlineMembers(cs);
				}
				else
					player.sendActionFailed();
				break;
			case PARTY:
				if(player.isInParty())
				{
					chatLog.info(logMessage);
					player.getParty().broadcastToPartyMembers(cs);
				}
				else
					player.sendActionFailed();
				break;
			case PARTYROOM_MATCHING:
				if(player.getPartyRoom() <= 0)
				{
					player.sendActionFailed();
					return;
				}
				PartyRoom room = PartyRoomManager.getInstance().getRooms().get(player.getPartyRoom());
				if(room == null)
				{
					player.sendActionFailed();
					return;
				}
				chatLog.info(logMessage);
				room.broadcastPacket(cs);
				break;
			case COMMANDCHANNEL_ALL:
				if(!player.isInParty() || !player.getParty().isInCommandChannel())
				{
					player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_AUTHORITY_TO_USE_THE_COMMAND_CHANNEL));
					return;
				}
				if(player.getParty().getCommandChannel().getChannelLeader() == player)
				{
					chatLog.info(logMessage);
					player.getParty().getCommandChannel().broadcastToChannelMembers(cs);
				}
				else
					player.sendPacket(new SystemMessage(SystemMessage.ONLY_CHANNEL_OPENER_CAN_GIVE_ALL_COMMAND));
				break;
			case COMMANDCHANNEL_COMMANDER:
				if(!player.isInParty() || !player.getParty().isInCommandChannel())
				{
					player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_AUTHORITY_TO_USE_THE_COMMAND_CHANNEL));
					return;
				}
				if(player.getParty().isLeader(player))
				{
					chatLog.info(logMessage);
					player.getParty().getCommandChannel().broadcastToPartyLeaders(cs);
				}
				else
					player.sendPacket(new SystemMessage(SystemMessage.ONLY_A_PARTY_LEADER_CAN_ACCESS_THE_COMMAND_CHANNEL));
				break;
			case HERO_VOICE:
				if(player.isHero() || AdminTemplateManager.checkBoolean("heroVoice", player))
				{
					// Ограничение только для героев, гм-мы пускай говорят.
					if(!AdminTemplateManager.checkBoolean("heroVoice", player))
					{
						long curTime = System.currentTimeMillis();
						Long lastHeroTime = (Long) player.getProperty(PropertyCollection.HeroChatLaunched);
						if(lastHeroTime != null && lastHeroTime + 10000L > curTime)
							return;

						player.addProperty(PropertyCollection.HeroChatLaunched, curTime);
					}

					if(player.isInZone(L2Zone.ZoneType.jail) && !player.isGM())
					{
						player.sendMessage(new CustomMessage("admin.jailed.shout", player));
						return;
					}

					chatLog.info(logMessage);
					for(L2Player player1 : L2ObjectsStorage.getAllPlayers())
						if(!player1.isInBlockList(player) && !player1.isBlockAll())
							player1.sendPacket(cs);
				}
				break;
			case PETITION_PLAYER:
			case PETITION_GM:
				if(!PetitionManager.getInstance().isPlayerInConsultation(player))
				{
					player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_CURRENTLY_NOT_IN_A_PETITION_CHAT));
					return;
				}
				chatLog.info(logMessage);
				PetitionManager.getInstance().sendActivePetitionMessage(player, _text);
				break;
			case BATTLEFIELD:
				if(TerritoryWarManager.getWar().isFunctionsActive() && player.getTerritoryId() > 0)
				{
					if(player.isInZone(L2Zone.ZoneType.jail) && !player.isGM())
					{
						player.sendMessage(new CustomMessage("admin.jailed.shout", player));
						return;
					}

					chatLog.info(logMessage);
					for(L2Player pl : L2ObjectsStorage.getAllPlayers())
						if(pl != null && pl.getTerritoryId() == player.getTerritoryId())
							pl.sendPacket(cs);
				}
				break;
			default:
				_log.warn("Character " + player.getName() + " used unknown chat type: " + _type + ". Cheater?");
		}
	}
}