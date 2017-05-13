package ru.l2gw.gameserver.model.instances;

import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.VehicleManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2World;
import ru.l2gw.gameserver.model.entity.vehicle.L2AirShipDock;
import ru.l2gw.gameserver.model.entity.vehicle.L2ClanAirship;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.gameserver.serverpackets.ExGetOnAirShip;
import ru.l2gw.gameserver.serverpackets.StopMove;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.util.Location;

/**
 * @author rage
 * @date 25.06.2010 11:46:32
 */
public class L2AirShipControllerInstance extends L2NpcInstance
{
	private L2AirShipDock _dock = null;
	private static final int AIRSHIP_SUMMON_LICENSE = 13559;
	private static final int ENERGY_STAR_STONE = 13277;

	public L2AirShipControllerInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
		int dockId = getAIParams() != null ? getAIParams().getInteger("dock_id", 0) : 0;
		if(dockId > 0)
			_dock = VehicleManager.getInstance().getDockById(dockId);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(!isInRange(player, getInteractDistance(player)))
		{
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			player.sendActionFailed();
		}
		else
		{
			if(command.equalsIgnoreCase("getOnAirShip"))
			{
				if(!checkBoardCondition(player))
					return;

				L2Vehicle airship = null;
				int dist = Integer.MAX_VALUE;
				for(L2Vehicle vehicle : L2World.getAroundTransport(getLoc(), 1000, 0))
					if(vehicle.isAirShip() && dist > vehicle.getDistance(this))
					{
						dist = (int) vehicle.getDistance(this);
						airship = vehicle;
					}

				if(airship != null && !airship.isMoving)
				{
					player.broadcastPacket(new StopMove(player));
					airship.addPlayerOnBoard(player);
					player.setVehicle(airship);
					player.setLocInVehicle(new Location(0, 0, 0));
					player.setXYZ(airship.getX(), airship.getY(), airship.getZ(), false);
					player.broadcastPacket(new ExGetOnAirShip(player, airship, airship.getLoc()));
					player.sendActionFailed();
				}
			}
			else if(command.equals("summonClanAirShip"))
			{
				if(_dock == null)
					return;

				if(player.getClan() == null || player.getClan().getLevel() < 5)
				{
					player.sendPacket(Msg.IN_ORDER_TO_ACQUIRE_AN_AIRSHIP_THE_CLAN_S_LEVEL_MUST_BE_LEVEL_5_OR_HIGHER);
					return;
				}

				if(!isHaveRigths(player, L2Clan.CP_CL_SUMMON_AIRSHIP))
				{
					player.sendPacket(Msg.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
					return;
				}

				if(!player.getClan().isAirshipEnabled())
				{
					player.sendPacket(Msg.AN_AIRSHIP_CANNOT_BE_SUMMONED_BECAUSE_EITHER_YOU_HAVE_NOT_REGISTERED_YOUR_AIRSHIP_LICENSE_OR_THE);
					return;
				}

				L2ClanAirship clanAirship = player.getClan().getAirship();

				if(clanAirship != null)
				{
					if(clanAirship == _dock.getDockedShip())
						player.sendPacket(Msg.THE_CLAN_OWNED_AIRSHIP_ALREADY_EXISTS);
					else
						player.sendPacket(Msg.THE_AIRSHIP_OWNED_BY_THE_CLAN_IS_ALREADY_BEING_USED_BY_ANOTHER_CLAN_MEMBER);
					return;
				}

				if(_dock.getDockedShip() != null)
				{
					player.sendPacket(Msg.ANOTHER_AIRSHIP_HAS_ALREADY_BEEN_SUMMONED_AT_THE_WHARF_PLEASE_TRY_AGAIN_LATER);
					return;
				}

				if(player.getItemCountByItemId(ENERGY_STAR_STONE) < 5)
				{
					player.sendMessage(new CustomMessage("ClanAirshipNoEnargeyStones", player).toString());
					return;
				}

				if(player.destroyItemByItemId("SummonAirship", ENERGY_STAR_STONE, 5, this, true))
					_dock.summonClanAirShip(player.getClan());
			}
			else if(command.equalsIgnoreCase("register"))
			{
				if(player.getClan() == null)
				{
					player.sendPacket(Msg.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
					return;
				}

				if(player.getClan().isAirshipEnabled())
				{
					player.sendMessage(new CustomMessage("ClanAirshipHasLicense", player).toString()); // На оффе шлется именно этот месадж оО
					return;
				}

				if(player.getItemCountByItemId(AIRSHIP_SUMMON_LICENSE) < 1)
				{
					player.sendMessage(new CustomMessage("ClanAirshipNoLicense", player).toString());
					return;
				}

				if(player.destroyItemByItemId("AirshipRegister", AIRSHIP_SUMMON_LICENSE, 1, this, true))
				{
					player.getClan().registerAirshipLicense();
					player.sendPacket(Msg.THE_AIRSHIP_SUMMON_LICENSE_HAS_BEEN_ENTERED_YOUR_CLAN_CAN_NOW_SUMMON_THE_AIRSHIP);
				}
			}
			else
				super.onBypassFeedback(player, command);
		}
	}

	public static boolean checkBoardCondition(L2Player player)
	{
		if(player.getTransformation() != 0)
		{
			player.sendPacket(Msg.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_TRANSFORMED);
			return false;
		}
		if(player.isParalyzed())
		{
			player.sendPacket(Msg.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_PETRIFIED);
			return false;
		}
		if(player.isAlikeDead())
		{
			player.sendPacket(Msg.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_DEAD);
			return false;
		}
		if(player.isFishing())
		{
			player.sendPacket(Msg.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_FISHING);
			return false;
		}
		if(player.isInCombat())
		{
			player.sendPacket(Msg.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_IN_BATTLE);
			return false;
		}
		if(player.isInDuel())
		{
			player.sendPacket(Msg.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_IN_A_DUEL);
			return false;
		}
		if(player.isSitting())
		{
			player.sendPacket(Msg.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_SITTING);
			return false;
		}
		if(player.isCastingNow())
		{
			player.sendPacket(Msg.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_SKILL_CASTING);
			return false;
		}
		if(player.isCursedWeaponEquipped())
		{
			player.sendPacket(Msg.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_A_CURSED_WEAPON_IS_EQUIPPED);
			return false;
		}
		if(player.isCombatFlagEquipped())
		{
			player.sendPacket(Msg.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_HOLDING_A_FLAG);
			return false;
		}
		if(player.getPet() != null || player.getMountEngine().isMounted())
		{
			player.sendPacket(Msg.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_A_PET_OR_A_SERVITOR_IS_SUMMONED);
			return false;
		}
		if(player.isInBoat())
		{
			player.sendPacket(Msg.YOU_HAVE_ALREADY_BOARDED_ANOTHER_AIRSHIP);
			return false;
		}
		return true;
	}

	public L2AirShipDock getDock()
	{
		return _dock;
	}
}
