package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.handler.RequestAction;
import ru.l2gw.gameserver.handler.RequestActionHandler;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.entity.olympiad.Olympiad;
import ru.l2gw.gameserver.model.entity.vehicle.L2AirShipHelm;
import ru.l2gw.gameserver.model.entity.vehicle.L2ClanAirship;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2PetInstance;
import ru.l2gw.gameserver.model.instances.L2StaticObjectInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.tables.PetDataTable;
import ru.l2gw.gameserver.templates.L2PetTemplate;
import ru.l2gw.util.Location;

import java.util.HashMap;

public class RequestActionUse extends L2GameClientPacket
{
	private int _actionId;
	private boolean _ctrlPressed;
	@SuppressWarnings("unused")
	private boolean _shiftPressed;

	/**
	 * packet type id 0x56
	 * format:		cddc
	 */
	@Override
	public void readImpl()
	{
		_actionId = readD();
		_ctrlPressed = readD() == 1;
		_shiftPressed = readC() == 1;
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		player.fireMethodInvoked(MethodCollection.onActionRequest, new Object[] { "action", _actionId });

		RequestAction ra = RequestActionHandler.getInstance().getActionById(_actionId);

		if(ra == null)
		{
			_log.warn("unhandled action type " + _actionId);
			return;
		}

		if(player.inObserverMode())
		{
			player.sendPacket(Msg.OBSERVERS_CANNOT_PARTICIPATE);
			player.sendActionFailed();
			return;
		}

		if(player.getTransformation() != 0 && !ra.allowInTransform() && player.getSkillLevel(838) < 1 || player.isBlocked())
		{
			player.sendActionFailed();
			return;
		}

		// dont do anything if player is dead or confused
		if(!ra.isServitorAction() && (player.isOutOfControl() || (player.isActionsDisabled() && !player.isCastingNow())) && !(player.isFakeDeath() && _actionId == 0))
		{
			player.sendActionFailed();
			return;
		}

		L2Object target = player.getTarget();
		L2Summon servitor = player.getPet();
		if(ra.isServitorAction() && (servitor == null || servitor.isPosessed() || servitor.isOutOfControl()))
		{
			player.sendActionFailed();
			return;
		}

		switch(ra.getActionType())
		{
			case player_sit: // Сесть/встать
				// На страйдере нельзя садиться
				if(player.getMountEngine().isMounted())
				{
					player.sendActionFailed();
					break;
				}

				if(!player.isSitting() && target instanceof L2StaticObjectInstance && ((L2StaticObjectInstance) target).getType() == 1 && player.getDistance(target) <= L2Character.INTERACTION_DISTANCE)
				{
					ChairSit cs = new ChairSit(player, ((L2StaticObjectInstance) target).getStaticObjectId());
					player.sendPacket(cs);
					player.sitDown();
					player.broadcastPacket(cs);
					break;
				}
				if(player.isFakeDeath())
				{
					player.stopEffectsByName("c_fake_death");
					player.updateEffectIcons();
				}
				else if(player.isSitting())
					player.standUp();
				else
					player.sitDown();
				break;
			case player_run: // Изменить тип передвижения, шаг/бег
				if(player.isRunning())
					player.setWalking();
				else
					player.setRunning();
				break;
			case player_next_target:
				L2Character nearest_target = null;
				for(L2Character cha : L2World.getAroundCharacters(player, 400, 200))
					if(cha != null && !cha.isAlikeDead())
						if(nearest_target == null || player.getDistance3D(cha) < player.getDistance3D(nearest_target) && cha.isAttackable(player, false, false))
							nearest_target = cha;
				if(nearest_target != null && target != nearest_target)
					nearest_target.onAction(player, true);
				break;
			case player_private_store_sell: // Запрос на создание приватного магазина продажи
				if(!checksForShop(player))
				{
					player.sendActionFailed();
					break;
				}

				player.tempWhDisable();
				AbstractEnchantPacket.checkAndCancelEnchant(player);
				if(player.isTradeInProgress())
					player.cancelActiveTrade();

				if(player.getPrivateStoreType() == L2Player.STORE_PRIVATE_NONE || player.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL)
				{
					player.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
					player.broadcastUserInfo(true);
					player.standUp();
					player.setPrivateStoreManage(true);

					if(player.getTradeList() == null)
						player.setTradeList(new L2TradeList(0));
					player.getTradeList().updateSellList(player, player.getSellList());
					player.sendPacket(new PrivateStoreManageListSell(player, false));
				}
				else
					player.sendActionFailed();
				break;
			case player_private_store_package_sell: // Запрос на создание приватного магазина продажи пакетом
				if(!checksForShop(player))
				{
					player.sendActionFailed();
					break;
				}

				AbstractEnchantPacket.checkAndCancelEnchant(player);

				if(player.getPrivateStoreType() == L2Player.STORE_PRIVATE_NONE || player.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL_PACKAGE)
				{
					player.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
					player.broadcastUserInfo(true);
					player.standUp();
					player.setPrivateStoreManage(true);

					if(player.getTradeList() == null)
						player.setTradeList(new L2TradeList(0));
					player.getTradeList().updateSellList(player, player.getSellList());
					player.sendPacket(new PrivateStoreManageListSell(player, true));
				}
				else
					player.sendActionFailed();
				break;
			case player_private_store_buy: // Запрос на создание приватного магазина покупки
				if(!checksForShop(player))
				{
					player.sendActionFailed();
					break;
				}

				AbstractEnchantPacket.checkAndCancelEnchant(player);
				if(player.isTradeInProgress())
					player.cancelActiveTrade();

				if(player.getPrivateStoreType() == L2Player.STORE_PRIVATE_NONE || player.getPrivateStoreType() == L2Player.STORE_PRIVATE_BUY)
				{
					player.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
					player.broadcastUserInfo(true);
					player.standUp();
					player.setPrivateStoreManage(true);

					if(player.getTradeList() == null)
						player.setTradeList(new L2TradeList(0));
					player.sendPacket(new PrivateStoreManageListBuy(player));
				}
				else
					player.sendActionFailed();
				break;
			case player_private_store_manufacture: // Создание магазина Dwarven Craft
				if(!checksForShop(player) || player.getSkillLevel(1321) < 1)
				{
					player.sendActionFailed();
					break;
				}

				AbstractEnchantPacket.checkAndCancelEnchant(player);
				if(player.isTradeInProgress())
					player.cancelActiveTrade();

				if(player.getPrivateStoreType() == L2Player.STORE_PRIVATE_NONE || player.getPrivateStoreType() == L2Player.STORE_PRIVATE_MANUFACTURE)
				{
					player.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
					player.standUp();
					player.broadcastUserInfo(true);
					if(player.getCreateList() == null)
						player.setCreateList(new L2ManufactureList());
					player.sendPacket(new RecipeShopManageList(player, true));
				}
				else
					player.sendActionFailed();
				break;
			case player_common_craft: // Создание магазина Common Craft
				if(!checksForShop(player) || player.getSkillLevel(1322) < 1)
				{
					player.sendActionFailed();
					break;
				}

				AbstractEnchantPacket.checkAndCancelEnchant(player);

				if(player.getPrivateStoreType() == L2Player.STORE_PRIVATE_NONE || player.getPrivateStoreType() == L2Player.STORE_PRIVATE_MANUFACTURE)
				{
					player.setPrivateStoreType(L2Player.STORE_PRIVATE_NONE);
					player.standUp();
					player.broadcastUserInfo(true);
					if(player.getCreateList() == null)
						player.setCreateList(new L2ManufactureList());
					player.sendPacket(new RecipeShopManageList(player, false));
				}
				else
					player.sendActionFailed();
				break;
			case player_social:
				if(player.getPrivateStoreType() == L2Player.STORE_PRIVATE_NONE && player.getTransactionRequester() == null && !player.isActionsDisabled() && !player.isSitting())
				{
					player.broadcastPacket(new SocialAction(player.getObjectId(), ra.getSocialId()));
					if(Config.ALT_SOCIAL_ACTION_REUSE)
					{
						player.block(2600);
					}
				}
				break;
			case player_social_couple:
				if(!(target instanceof L2Player))
				{
					player.sendActionFailed();
					return;
				}

				if(player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE || player.getTransactionRequester() != null || player.isActionsDisabled() || player.isSitting())
				{
				 	player.sendActionFailed();
					return;
				}

				L2Player pcTarget = (L2Player) target;

				if(player.isTransactionRequestInProgress() && player.getTransactionRequester() != pcTarget)
				{
					player.sendPacket(Msg.WAITING_FOR_ANOTHER_REPLY);
					return;
				}
				if(pcTarget.isInBlockList(player))
				{
					player.sendPacket(Msg.THE_COUPLE_ACTION_WAS_DENIED);
					return;
				}
				if(pcTarget.isDead())
				{
					player.sendPacket(new SystemMessage(SystemMessage.C1_IS_CURRENTLY_DEAD_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
					return;
				}
				if(!player.isInRange(pcTarget, 100) || player.isInRange(pcTarget, 25) || pcTarget == player || !GeoEngine.canSeeTarget(player, pcTarget))
				{
					player.sendPacket(Msg.THE_REQUEST_CANNOT_BE_COMPLETED_BECAUSE_THE_TARGET_DOES_NOT_MEET_LOCATION_REQUIREMENTS);
					return;
				}
				if(pcTarget.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
				{
					player.sendPacket(new SystemMessage(SystemMessage.C1_IS_IN_PRIVATE_SHOP_MODE_OR_IN_A_BATTLE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
					return;
				}
				if(pcTarget.isFishing())
				{
					player.sendPacket(new SystemMessage(SystemMessage.C1_IS_FISHING_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
					return;
				}
				if(pcTarget.isInCombat() || pcTarget.isCastingNow())
				{
					player.sendPacket(new SystemMessage(SystemMessage.C1_IS_IN_A_BATTLE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
					return;
				}
				if(pcTarget.isTransactionInProgress())
				{
					if(pcTarget.getTransactionType() == L2Player.TransactionType.COUPLE_ACTION)
						player.sendPacket(new SystemMessage(SystemMessage.C1_IS_ALREADY_PARTICIPATING_IN_A_COUPLE_ACTION_AND_CANNOT_BE_REQUESTED_FOR_ANOTHER_COUPLE_ACTION).addCharName(pcTarget));
					else
						player.sendPacket(Msg.THE_COUPLE_ACTION_WAS_DENIED);
					return;
				}
				if(pcTarget.getKarma() > 0)
				{
					player.sendPacket(new SystemMessage(SystemMessage.C1_IS_IN_A_CHAOTIC_STATE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
					return;
				}
				if(Olympiad.getRegisteredGameType(pcTarget) >= 0)
				{
					player.sendPacket(new SystemMessage(SystemMessage.C1_IS_PARTICIPATING_IN_THE_OLYMPIAD_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
					return;
				}
				if(pcTarget.getClanId() > 0 && pcTarget.getSiegeState() > 0 && pcTarget.getSiegeId() > 0)
				{
					if(pcTarget.getSiegeId() <= 9)
						player.sendPacket(new SystemMessage(SystemMessage.C1_IS_IN_A_CASTLE_SIEGE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
					else
						player.sendPacket(new SystemMessage(SystemMessage.C1_IS_PARTICIPATING_IN_A_HIDEOUT_SIEGE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
					return;
				}
				if(pcTarget.getMountEngine().isMounted() || pcTarget.getVehicle() != null)
				{
					player.sendPacket(new SystemMessage(SystemMessage.C1_IS_RIDING_A_SHIP_STEED_OR_STRIDER_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
					return;
				}
				if(pcTarget.isTeleporting())
				{
					player.sendPacket(new SystemMessage(SystemMessage.C1_IS_CURRENTLY_TELEPORTING_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
					return;
				}
				if(pcTarget.getTransformation() > 0 && player.getSkillLevel(838) <= 0)
				{
					player.sendPacket(new SystemMessage(SystemMessage.C1_IS_CURRENTLY_TRANSFORMING_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
					return;
				}

				player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_REQUESTED_A_COUPLE_ACTION_WITH_C1).addCharName(pcTarget));
				player.setTransactionRequester(pcTarget, System.currentTimeMillis() + 10000);
				player.setTransactionType(L2Player.TransactionType.COUPLE_ACTION);
				pcTarget.setTransactionRequester(player, System.currentTimeMillis() + 10000);
				pcTarget.setTransactionType(L2Player.TransactionType.COUPLE_ACTION);
				pcTarget.sendPacket(new ExAskCoupleAction(player.getObjectId(), ra.getSocialId()));
				break;
			case player_mount:
				if(!player.getMountEngine().isMounted())
				{
					if(servitor == null || player.getDistance(player.getPet()) > 300 || !servitor.isMountable() || servitor.isPosessed())
					{
						player.sendActionFailed();
						return;
					}

					if(player.isDead())
						player.sendPacket(Msg.A_STRIDER_CANNOT_BE_RIDDEN_WHEN_DEAD);
					else if(servitor.isDead())
						player.sendPacket(Msg.A_DEAD_STRIDER_CANNOT_BE_RIDDEN);
					else if(servitor.isInCombat())
						player.sendPacket(Msg.A_STRIDER_IN_BATTLE_CANNOT_BE_RIDDEN);
					else if(player.isInCombat())
						player.sendPacket(Msg.A_STRIDER_CANNOT_BE_RIDDEN_WHILE_IN_BATTLE);
					else if(player.isSitting() || player.isMoving)
						player.sendPacket(Msg.A_STRIDER_CAN_BE_RIDDEN_ONLY_WHEN_STANDING);
					else if(player.isFishing())
						player.sendPacket(Msg.YOU_CANNOT_DO_THAT_WHILE_FISHING);
					else if(player.isCursedWeaponEquipped())
						player.sendPacket(Msg.A_STRIDER_CANNOT_BE_RIDDEN_WHILE_IN_BATTLE);
					else if(servitor.isPet() && servitor.isHungry())
						player.sendPacket(Msg.A_HUNGRY_STRIDER_CANNOT_BE_MOUNTED_OR_DISMOUNTED);
					if(player.isCombatFlagEquipped())
						player.sendPacket(Msg.YOU_CANNOT_CONTROL_THE_TARGET_WHILE_HOLDING_A_FLAG);
					else if(!servitor.isDead() && !player.getMountEngine().isMounted())
					{
						player.stopEffect(5239); // Event timer
						player.getMountEngine().setMount(((L2PetInstance) servitor).getTemplate(), servitor.getObjectId());
						servitor.unSummon();
					}
				}
				else if(player.getMountEngine().isMounted())
				{
					if(player.isFlying() && !player.checkLandingState()) // Виверна
					{
						player.sendActionFailed();
						player.sendPacket(Msg.YOU_ARE_NOT_ALLOWED_TO_DISMOUNT_AT_THIS_LOCATION);
						break;
					}
					else
						player.getMountEngine().dismount();
				}
				break;
			case player_bot_report:
				L2Player bot = target != null && target.isPlayer() ? (L2Player)target : null;
				if(bot == null || bot == player)
					player.sendPacket(Msg.ActionFail);
				else if(bot.isInZonePeace() || bot.isInZoneBattle())
					player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_REPORT_A_CHARACTER_WHO_IS_IN_A_PEACE_ZONE_OR_A_BATTLEFIELD));
				else if(player.atMutualWarWith(bot, player.getClan(), bot.getClan()))
					player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_REPORT_WHEN_A_BILATERAL_CLAN_WAR_HAS_BEEN_DECLARED));
				else if(!bot.isResivedExp)
					player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_REPORT_A_CHARACTER_WHO_HAS_NOT_ACQUIRED_ANY_EXP_AFTER_CONNECTING));
				else if(bot.isGM() || player.isBotReported(bot.getObjectId()))
				    player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_REPORT_THIS_PERSON_AGAIN_AT_THIS_TIME));
				else
				{
					player.botReport(bot);
					player.sendPacket(new SystemMessage(SystemMessage.C1_WAS_REPORTED_AS_A_BOT).addCharName(bot));
				}
				break;
			case player_airship_steer:
				L2Vehicle vehicle = player.getVehicle();
				if(vehicle instanceof L2ClanAirship)
				{
					L2ClanAirship cas = (L2ClanAirship) vehicle;

					if(!(player.getTarget() instanceof L2AirShipHelm) || player.getTarget().getObjectId() != cas.getHelmId())
					{
						player.sendPacket(Msg.YOU_MUST_TARGET_THE_ONE_YOU_WISH_TO_CONTROL);
						return;
					}
					
					if(cas.getCaptainObjectId() == player.getObjectId())
						return;

					if(player.getLocInVehicle().distance(new Location(335, 0, 128)) > 100)
					{
						player.sendPacket(Msg.YOU_CANNOT_CONTROL_BECAUSE_YOU_ARE_TOO_FAR);
						return;
					}

					if(cas.getCaptainObjectId() > 0)
					{
						player.sendPacket(Msg.ANOTHER_PLAYER_IS_PROBABLY_CONTROLLING_THE_TARGET);
						return;
					}

					if(player.isParalyzed())
					{
						player.sendPacket(Msg.YOU_CANNOT_CONTROL_THE_TARGET_WHILE_YOU_ARE_PETRIFIED);
						return;
					}

					if(player.isDead() || player.isFakeDeath())
					{
						player.sendPacket(Msg.YOU_CANNOT_CONTROL_THE_TARGET_WHEN_YOU_ARE_DEAD);
						return;
					}

					if(player.isFishing())
					{
						player.sendPacket(Msg.YOU_CANNOT_CONTROL_THE_TARGET_WHILE_FISHING);
						return;
					}

					if(player.isInCombat())
					{
						player.sendPacket(Msg.YOU_CANNOT_CONTROL_THE_TARGET_WHILE_IN_A_BATTLE);
						return;
					}

					if(player.isInDuel())
					{
						player.sendPacket(Msg.YOU_CANNOT_CONTROL_THE_TARGET_WHILE_IN_A_DUEL);
						return;
					}

					if(player.isSitting())
					{
						player.sendPacket(Msg.YOU_CANNOT_CONTROL_THE_TARGET_WHILE_IN_A_SITTING_POSITION);
						return;
					}

					if(player.isCastingNow())
					{
						player.sendPacket(Msg.YOU_CANNOT_CONTROL_THE_TARGET_WHILE_USING_A_SKILL);
						return;
					}

					if(player.isCursedWeaponEquipped())
					{
						player.sendPacket(Msg.YOU_CANNOT_CONTROL_THE_TARGET_WHILE_A_CURSED_WEAPON_IS_EQUIPPED);
						return;
					}

					if(player.isCombatFlagEquipped())
					{
						player.sendPacket(Msg.YOU_CANNOT_CONTROL_THE_TARGET_WHILE_HOLDING_A_FLAG);
						return;
					}

					L2ItemInstance weapon = player.getActiveWeaponInstance();
					if(weapon != null)
					{
						player.getInventory().unEquipItemAndSendChanges(weapon);
						player.sendDisarmMessage(weapon);

						if(weapon != null)
						{
							if(weapon.getChargedSpiritshot() != L2ItemInstance.CHARGED_NONE)
								player.sendPacket(new SystemMessage(SystemMessage.POWER_OF_MANA_DISABLED));
							if(weapon.getChargedSoulshot() != L2ItemInstance.CHARGED_NONE)
								player.sendPacket(new SystemMessage(SystemMessage.POWER_OF_THE_SPIRITS_DISABLED));
							weapon.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
							weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
						}
					}

					player.sendActionFailed();
					cas.setCaptainObjectId(player.getObjectId());
					player.setLocInVehicle(new Location(348, 0, 105));
					player.broadcastPacket(new ExStopMoveInAirShip(player, cas));
					player.broadcastUserInfo(true);
					cas.broadcastUserInfo();
				}
				break;
			case player_airship_cancel_control:
				vehicle = player.getVehicle();
				if(vehicle instanceof L2ClanAirship)
				{
					player.sendActionFailed();
					L2ClanAirship cas = (L2ClanAirship) vehicle;
					if(player.getObjectId() == cas.getCaptainObjectId() && (cas.isManualControlled() || cas.getCurrentDock() != null))
					{
						if(cas.isMoving)
							if(cas.isManualControlled())
								cas.stopMove();
							else
								cas.broadcastMove();

						cas.setCaptainObjectId(0);
						player.broadcastUserInfo(true);
						cas.broadcastUserInfo();
					}
				}
				break;
			case player_airship_destination_map:
				vehicle = player.getVehicle();
				if(vehicle instanceof L2ClanAirship)
				{
					player.sendActionFailed();
					L2ClanAirship cas = (L2ClanAirship) vehicle;
					if(player.getObjectId() == cas.getCaptainObjectId() && cas.getCurrentDock() != null)
						player.sendPacket(new ExAirShipTeleportList(cas.getCurrentDock()));
				}
				break;
			case player_airship_exit:
				vehicle = player.getVehicle();
				if(vehicle instanceof L2ClanAirship)
				{
					player.sendActionFailed();
					L2ClanAirship cas = (L2ClanAirship) vehicle;
					if(player.getObjectId() == cas.getCaptainObjectId() && (cas.isManualControlled() || cas.getCurrentDock() != null))
					{
						if(cas.isMoving)
							if(cas.isManualControlled())
								cas.stopMove();
							else
								cas.broadcastMove();

						cas.setCaptainObjectId(0);
						player.broadcastUserInfo(true);
						cas.broadcastUserInfo();
						return;
					}

					if(!vehicle.isMoving && ((L2ClanAirship) vehicle).getCurrentDock() != null)
						vehicle.oustPlayer(player, vehicle.getKickPoint());
				}
				break;
			case servitor_follow:
				servitor.setFollowStatus(!servitor.getFollowStatus());
				break;
			case servitor_attack:
				if(player.isInOlympiadMode() && !player.isOlympiadStart())
				{
					player.sendActionFailed();
					return;
				}

				// Sin Eater
				if(servitor.getTemplate().getNpcId() == PetDataTable.SIN_EATER_ID || target == null)
					return;

				if(player.getLevel() + 20 <= servitor.getLevel())
				{
					player.sendPacket(Msg.THE_PET_IS_TOO_HIGH_LEVEL_TO_CONTROL);
					return;
				}

				if(servitor.isPet() && player.getDistance(player.getPet()) > 1500)//MaxCastRangeofSkills отбалды
					return;

				servitor.getAI().Attack(target, _ctrlPressed, _shiftPressed);
				break;
			case servitor_stop:
				if(servitor.getAI().getIntention() != CtrlIntention.AI_INTENTION_FOLLOW)
					servitor.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null, null);
				break;
			case servitor_unsummon:
				if(servitor.isDead())
				{
					player.sendPacket(Msg.A_DEAD_PET_CANNOT_BE_SENT_BACK);
					player.sendActionFailed();
					break;
				}

				if(servitor.isInCombat() || servitor.isPosessed())
				{
					player.sendPacket(Msg.A_PET_CANNOT_BE_SENT_BACK_DURING_BATTLE);
					player.sendActionFailed();
					break;
				}

				if(servitor.isPet() && ((L2PetTemplate) servitor.getTemplate()).food.size() > 0 && servitor.getCurrentFed() < ((L2PetTemplate) servitor.getTemplate()).hungry_limit * servitor.getMaxMeal())
				{
					player.sendPacket(Msg.YOU_CANNOT_RESTORE_HUNGRY_PETS);
					player.sendActionFailed();
					break;
				}

				servitor.unSummon();
				break;
			case servitor_move_to_target:
				if(target != null && servitor != target && !servitor.isMovementDisabled())
				{
					servitor.setFollowStatus(false);
					servitor.moveToLocation(target.getLoc(), 100, true);
				}
				break;
			case servitor_skill:
				if(player.getLevel() + 20 <= servitor.getLevel())
				{
					player.sendPacket(Msg.THE_PET_IS_TOO_HIGH_LEVEL_TO_CONTROL);
					return;
				}

				HashMap<Integer, L2Skill> _skills = servitor.getTemplate().getSkills();
				if(_skills.size() == 0)
				{
					player.sendActionFailed();
					return;
				}

				L2Skill skill = _skills.get(ra.getSkillId());
				if(skill == null)
				{
					player.sendActionFailed();
					return;
				}

				target = skill.getAimingTarget(servitor, target);

				if(target == null || !target.isCharacter() || player.getDistance(servitor) > 1500)
				{
					player.sendActionFailed();
					return;
				}
				
				servitor.setTarget(target);
				servitor.getAI().Cast(skill, skill.getAimingTarget(servitor), null, _ctrlPressed, _shiftPressed);
				break;
			case servitor_skill_by_level:
				if(target == null || !target.isCharacter() || player.getDistance(servitor) > 1500)
				{
					player.sendActionFailed();
					return;
				}

				if(player.getLevel() + 20 <= servitor.getLevel())
				{
					player.sendPacket(Msg.THE_PET_IS_TOO_HIGH_LEVEL_TO_CONTROL);
					return;
				}

				skill = ra.getSkillByLevel(servitor.getLevel());
				if(skill == null)
				{
					player.sendActionFailed();
					return;
				}

				servitor.setTarget(target);
				servitor.getAI().Cast(skill, skill.getAimingTarget(servitor), null, _ctrlPressed, _shiftPressed);
				break;
			default:
				_log.warn(getClient() + " unhandled action type: " + ra.getActionType());
				break;
		}
	}

	private static boolean checksForShop(L2Player player)
	{
		if(AdminTemplateManager.checkBoolean("noPrivateStore", player))
		{
			player.sendPacket(Msg.THIS_ACCOUNT_CANOT_USE_PRIVATE_STORES);
			return false;
		}

		if(player.isActionBlocked(L2Zone.BLOCKED_ACTION_PRIVATE_STORE) && !player.isInStoreMode())
			if(Config.SERVICES_NO_TRADE_ONLY_OFFLINE)
			{
				if(player.isInOfflineMode())
				{
					player.sendPacket(Msg.A_PRIVATE_STORE_MAY_NOT_BE_OPENED_IN_THIS_AREA);
					return false;
				}
			}
			else
			{
				player.sendPacket(Msg.A_PRIVATE_STORE_MAY_NOT_BE_OPENED_IN_THIS_AREA);
				return false;
			}

		if(player.isCastingNow())
		{
			player.sendPacket(Msg.A_PRIVATE_STORE_MAY_NOT_BE_OPENED_WHILE_USING_A_SKILL);
			return false;
		}

		if(player.isInCombat())
		{
			player.sendPacket(Msg.WHILE_YOU_ARE_ENGAGED_IN_COMBAT_YOU_CANNOT_OPERATE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			return false;
		}

		if(player.getVarInt("disguised") > 0)
		{
			player.sendPacket(Msg.WHILE_DISGUISED_YOU_CANNOT_OPERATE_A_PRIVATE_OR_MANUFACTURE_STORE);
			return false;
		}

		if(player.isOutOfControl() || player.isActionsDisabled() || player.getMountEngine().isMounted())
			return false;

		if(player.isInOlympiadMode())
			return false;

		if(player.getTransactionRequester() != null)
			return false;

		return true;
	}
}