package ru.l2gw.gameserver.clientpackets;

import org.apache.commons.logging.LogFactory;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.ccpGuard.ConfigProtect;
import ru.l2gw.extensions.ccpGuard.packets.ProtectPing;
import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.extensions.scripts.Scripts.ScriptClassAndMethod;
import ru.l2gw.gameserver.Announcements;
import ru.l2gw.gameserver.controllers.GameTimeController;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.*;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.Castle;
import ru.l2gw.gameserver.model.entity.Fortress;
import ru.l2gw.gameserver.model.entity.SevenSigns;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.entity.siege.territory.TerritoryWar;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.model.mail.MailController;
import ru.l2gw.gameserver.model.quest.Quest;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.model.zone.L2Zone.ZoneType;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.tables.MapRegionTable;
import ru.l2gw.gameserver.tables.SkillTable;

import java.io.File;

import static ru.l2gw.gameserver.model.zone.L2Zone.ZoneType.no_restart;

/**
 * Enter World Packet Handler<p>
 * <p>
 * 0000: 11 <p>
 */
public class EnterWorld extends L2GameClientPacket
{
	//Format1(EnterWorldPacket): cbddddbdcccccccccccccccccccc
	//Format2(EnterWorld): cS
	private static org.apache.commons.logging.Log _log = LogFactory.getLog(EnterWorld.class.getName());

	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null)
		{
			getClient().closeNow(false);
			return;
		}

		if(!Config.SHOW_GM_LOGIN && AdminTemplateManager.checkCommandAllow("admin_invis", player) && AdminTemplateManager.checkCommand("admin_invis", player, null, null, null, null))
			player.setInvisible(true);

		//Updating Seal of Strife Buff/Debuff
		if(SevenSigns.getInstance().isSealValidationPeriod() && SevenSigns.getInstance().getPlayerCabal(player) != SevenSigns.CABAL_NULL)
		{
			if(SevenSigns.getInstance().getPlayerCabal(player) == SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
				player.addSkill(SkillTable.getInstance().getInfo(5074, 1));
			else if(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) != SevenSigns.CABAL_NULL)
				player.addSkill(SkillTable.getInstance().getInfo(5075, 1));
		}

		sendPacket(new HennaInfo(player));
		player.getInventory().sendItemList(false);
		sendPacket(new ExGetBookMarkInfo(player));
		player.getMacroses().sendUpdate();

		if(Config.DAY_STATUS_FORCE_CLIENT_UPDATE)
			if(GameTimeController.getInstance().isNowNight())
				sendPacket(Msg.SunSet);
			else
				sendPacket(Msg.SunRise);

		sendPacket(new SkillList(player));
		sendPacket(new SystemMessage(SystemMessage.WELCOME_TO_THE_WORLD_OF_LINEAGE_II));

		//add char to online characters
		player.setOnlineStatus(true);

		SevenSigns.getInstance().sendCurrentPeriodMsg(player);

		if(player.getClanId() != 0)
		{
			L2Clan clan = player.getClan();
			sendPacket(new PledgeShowMemberListAll(clan, player));
			sendPacket(new PledgeShowInfoUpdate(clan));
			sendPacket(new PledgeSkillList(player));
			player.setTerritoryId(clan.getTerritoryId());
			SiegeUnit unit = ResidenceManager.getInstance().getResidenceByOwner(player.getClanId(), true);
			if(unit != null && !unit.isPaid())
				player.sendPacket(new SystemMessage(SystemMessage.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW).addNumber(unit.getLease()));

			for(Castle castle : ResidenceManager.getInstance().getCastleList())
			{
				Siege siege = castle.getSiege();
				if(!siege.isInProgress())
					continue;
				if(siege.checkIsAttacker(player.getClanId()))
				{
					player.setSiegeState(1);
					player.setSiegeId(siege.getSiegeUnit().getId());
				}
				else if(siege.checkIsDefender(player.getClanId()))
				{
					player.setSiegeState(2);
					player.setSiegeId(siege.getSiegeUnit().getId());
				}
			}

			for(Fortress fortress : ResidenceManager.getInstance().getFortressList())
			{
				Siege siege = fortress.getSiege();
				if(!siege.isInProgress())
					continue;
				if(siege.checkIsAttacker(player.getClanId()))
				{
					player.setSiegeState(1);
					player.setSiegeId(siege.getSiegeUnit().getId());
				}
				else if(siege.checkIsDefender(player.getClanId()))
				{
					player.setSiegeState(2);
					player.setSiegeId(siege.getSiegeUnit().getId());
				}
			}
		}

		if(player.getTerritoryId() == 0)
			player.setTerritoryId(TerritoryWarManager.getMercRegisteredTerritoryId(player.getObjectId()));

		if(TerritoryWarManager.getWar().isInProgress() && player.getTerritoryId() > 0)
			player.setSiegeState(3);

		if(player.getVar("disguised") != null && !TerritoryWarManager.getWar().isFunctionsActive())
			player.unsetVar("disguised");

		// engage and notify Partner
		if(Config.ALLOW_WEDDING)
		{
			CoupleManager.getInstance().engage(player);
			CoupleManager.getInstance().notifyPartner(player);
		}

		sendPacket(new ExStorageMaxCount(player));
		sendPacket(new QuestList(player));
		sendPacket(new ExBasicActionList(player));

		// Send PCBang Points Window Info
		if(player.getPcBangPoints() > 0)
			sendPacket(new ExPCCafePointInfo(player));
		
		player.setEntering(false);
		player.sendUserInfo(true);
		player.spawnMe();
		
		player.sendPacket(new ExAdenaInvenCount(player));

		if(player.isInZoneOlympiad())
			player.teleToClosestTown();
		else if(player.isInZone(no_restart))
		{
			L2Zone zone = player.getZone(no_restart);
			long allowed_time = zone.getRestartTime();
			long last_time = player.getLastAccess();
			long curr_time = System.currentTimeMillis() / 1000;

			if(curr_time - last_time > allowed_time)
			{
				if(zone.getTypes().contains(ZoneType.instance))
					player.teleToLocation(MapRegionTable.getInstance().getTeleToLocation(player, MapRegionTable.TeleportWhereType.ClosestTown), 0);
				else
					player.teleToClosestTown();
			}
		}
		else if(!player.isGM() && player.isInSiege())
		{
			Siege siege = SiegeManager.getSiege(player.getX(), player.getY());
			if(siege != null)
			{
				if(siege.checkIsDefender(player.getClanId()))
				{
					if(siege.getSiegeUnit().isCastle)
						player.teleToCastle();
					else if(siege.getSiegeUnit().isFort)
						player.teleToFortress();
					else
						player.teleToClosestTown();
				}
				else
					player.teleToClosestTown();
			}
		}

		if(player.isDead())
			sendPacket(new Die(player));

		if(Config.VIT_DEBUG)
			_log.info("Vitality[" + player.getName() + "] on etner, peace zone: " + player.isInZonePeace());

		player.getVitality().updateOfflineTime(player.getLogoutTime());

		player.restoreEffects();
		player.restoreDisableSkills();
		sendPacket(new ShortCutInit(player));

		CursedWeaponsManager.getInstance().checkPlayer(player);

		// refresh player info
		sendPacket(new EtcStatusUpdate(player));
		player.checkHpMessages(player.getMaxHp(), player.getCurrentHp());
		player.checkDayNightMessages();

		if(Config.SHOW_HTML_WELCOME)
		{
			String welcomePath = "data/html/welcome.htm";
			File mainText = new File(Config.DATAPACK_ROOT, welcomePath); // Return the pathfile of the HTML file
			if(mainText.exists())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile(welcomePath);
				sendPacket(html);
			}
		}

		if(Config.PETITIONING_ALLOWED)
			PetitionManager.getInstance().checkPetitionMessages(player);

		if(player.isSitting())
			player.sendPacket(new ChangeWaitType(player, ChangeWaitType.WT_SITTING));
		if(player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
			if(player.getPrivateStoreType() == L2Player.STORE_PRIVATE_BUY)
				sendPacket(new PrivateStoreMsgBuy(player));
			else if(player.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL)
				sendPacket(new PrivateStoreMsgSell(player));
			else if(player.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL_PACKAGE)
				sendPacket(new ExPrivateStoreSetWholeMsg(player));
			else if(player.getPrivateStoreType() == L2Player.STORE_PRIVATE_MANUFACTURE)
				sendPacket(new RecipeShopMsg(player));

		player.unsetVar("offline");

		// на всякий случай
		player.sendActionFailed();
		player.sendPacket(new ClientSetTime());

		if(player.isGM() && Config.SAVE_GM_EFFECTS && AdminTemplateManager.checkBoolean("useCommands", player))
		{
			//silence
			if(player.getVarB("gm_silence"))
			{
				player.setMessageRefusal(true);
				player.sendPacket(new SystemMessage(SystemMessage.MESSAGE_REFUSAL_MODE));
			}
			//invul
			if(player.getVarB("gm_invul"))
			{
				player.setIsInvul(true);
				player.sendMessage(player.getName() + " is now immortal.");
			}
			//gmspeed
			try
			{
				int var_gmspeed = Integer.parseInt(player.getVar("gm_gmspeed"));
				if(var_gmspeed >= 1 && var_gmspeed <= 4)
					player.doCast(SkillTable.getInstance().getInfo(7029, var_gmspeed), player, null, true);
			}
			catch(Exception E)
			{}
		}

		PlayerMessageStack.getInstance().CheckMessages(player);
		player.sendUserInfo(false); // Отобразит права в клане

		if(getClient().getPremiumExpire() > System.currentTimeMillis() || Config.PREMIUM_MIN_CLAN_LEVEL > -1 && player.isClanLeader() && player.getClan().getLevel() >= Config.PREMIUM_MIN_CLAN_LEVEL)
			player.startPremiumTask(getClient().getPremiumExpire());
		else
			player.sendPacket(new ExBRPremiumState(player.getObjectId(), false));

		Announcements.getInstance().showAnnouncements(player);

		// Вызов всех хэндлеров, определенных в скриптах
		Object[] script_args = new Object[] { player };
		for(ScriptClassAndMethod handler : Scripts.onPlayerEnter)
			player.callScripts(handler.scriptClass, handler.method, script_args);

		L2Clan clan = player.getClan();
		if(clan != null)
			try
			{
				clan.notifyClanMembers(player, true);
			}
			catch(Exception e)
			{
			}

		player.getFriendList().sendFriendList();
		player.getFriendList().notifyFriends(true);
		player.startNonAggroTask();

		TerritoryWar.checkQuestStates(player);
		loadTutorial(player);
		
	
		//player.sendPacket(new ExSubjobInfo(activeChar.getPlayer(), false));
		//player.sendPacket(new ExTutorialList());
		player.sendPacket(new ExAcquirableSkillListByClass(player));

		int c = MailController.getInstance().getUnreadCount(player.getObjectId());
		for(int i = 0; i < c; i++)
			player.sendPacket(new ExNoticePostArrived(1));

		if(getClient()._prot_info.protect_used && ConfigProtect.PROTECT_SHOW_PING)
			sendPacket(new ProtectPing());

		if(!Config.ALT_ANNOUNCE_TEXT.isEmpty() && player.getLevel() <= Config.ALT_ANNOUNCE_MAX_LEVEL)
			player.sendPacket(new ExShowScreenMessage(Config.ALT_ANNOUNCE_TEXT, 3000 + Config.ALT_ANNOUNCE_TEXT.length() * 100, ExShowScreenMessage.ScreenMessageAlign.TOP_CENTER, Config.ALT_ANNOUNCE_TEXT.length() < 64));

		player.getRecSystem().startBonusSystem();
		player.getHuntingBonus().onEnterWorld();
		PremiumItemManager.onPlayerEnter(player);

		if(player.isInJail())
		{
			long time = player.getVarLong("jailed");
			if(time - System.currentTimeMillis() > 0)
				player.sendMessage(new CustomMessage("admin.jailed.time", player).addNumber((time - System.currentTimeMillis()) / 1000 / 60));
		}
	}

	private void loadTutorial(L2Player player)
	{
		Quest q = QuestManager.getQuest(255);
		if(q != null)
			player.processQuestEvent(q.getName(), "EW");
	}
}