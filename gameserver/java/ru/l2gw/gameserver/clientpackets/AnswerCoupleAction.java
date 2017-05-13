package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.olympiad.Olympiad;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.util.Location;

/**
 * @author rage
 * @date 17.12.10 0:28
 */
public class AnswerCoupleAction extends L2GameClientPacket
{
	private int _charObjId;
	private int _actionId;
	private int _answer;
	
	@Override
	protected void readImpl()
	{
		_actionId = readD();
		_answer = readD();
		_charObjId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		L2Player player = getClient().getPlayer();
		L2Player target = L2ObjectsStorage.getPlayer(_charObjId);

		if(player == null || target == null)
			return;

		if(target.getTransactionRequester() != player || player.getTransactionRequester() != target || player.getTransactionType() != L2Player.TransactionType.COUPLE_ACTION || target.getTransactionType() != L2Player.TransactionType.COUPLE_ACTION)
			return;

		player.setTransactionType(L2Player.TransactionType.NONE);
		player.setTransactionRequester(null);
		target.setTransactionType(L2Player.TransactionType.NONE);
		target.setTransactionRequester(null);

		if(_answer == 0)
			target.sendPacket(Msg.THE_COUPLE_ACTION_WAS_DENIED);
		else if(_answer == 1) // approve
		{
			if(!checkCondition(player, target) || !checkCondition(target, player))
				return;

			int heading = player.calcHeading(target.getX(), target.getY());
			player.setHeading(heading);
			player.broadcastPacket(new ExRotation(player.getObjectId(), heading));
			player.block();
			heading = target.calcHeading(player.getX(), player.getY());
			target.setHeading(heading);
			target.broadcastPacket(new ExRotation(target.getObjectId(), heading));
			target.block();

			Location loc = target.applyOffset(player.getLoc(), 30);
			loc = GeoEngine.moveCheck(target.getX(), target.getY(), target.getZ(), loc.getX(), loc.getY(), target.getReflection());
			target.broadcastPacket(new MoveToPawn(target, player, 30));
			target.setXYZ(loc.getX(), loc.getY(), loc.getZ(), false);

			ThreadPoolManager.getInstance().scheduleGeneral(new SocialTask(player, target, _actionId), 400);
			player.block(2400);
			target.block(2400);
		}
		else if (_answer == -1) // refused
			target.sendPacket(new SystemMessage(SystemMessage.C1_IS_SET_TO_REFUSE_COUPLE_ACTIONS_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(player));
	}

	private static boolean checkCondition(L2Player player, L2Player pcTarget)
	{
		if(pcTarget.isDead())
		{
			player.sendPacket(new SystemMessage(SystemMessage.C1_IS_CURRENTLY_DEAD_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
			return false;
		}
		if(!player.isInRange(pcTarget, 120) || player.isInRange(pcTarget, 25) || pcTarget == player || !GeoEngine.canSeeTarget(player, pcTarget))
		{
			player.sendPacket(Msg.THE_REQUEST_CANNOT_BE_COMPLETED_BECAUSE_THE_TARGET_DOES_NOT_MEET_LOCATION_REQUIREMENTS);
			return false;
		}
		if(pcTarget.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			player.sendPacket(new SystemMessage(SystemMessage.C1_IS_IN_PRIVATE_SHOP_MODE_OR_IN_A_BATTLE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
			return false;
		}
		if(pcTarget.isFishing())
		{
			player.sendPacket(new SystemMessage(SystemMessage.C1_IS_FISHING_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
			return false;
		}
		if(pcTarget.isInCombat() || pcTarget.isCastingNow())
		{
			player.sendPacket(new SystemMessage(SystemMessage.C1_IS_IN_A_BATTLE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
			return false;
		}
		if(pcTarget.getKarma() > 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.C1_IS_IN_A_CHAOTIC_STATE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
			return false;
		}
		if(Olympiad.getRegisteredGameType(pcTarget) >= 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.C1_IS_PARTICIPATING_IN_THE_OLYMPIAD_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
			return false;
		}
		if(pcTarget.getClanId() > 0 && pcTarget.getSiegeState() > 0 && pcTarget.getSiegeId() > 0)
		{
			if(pcTarget.getSiegeId() <= 9)
				player.sendPacket(new SystemMessage(SystemMessage.C1_IS_IN_A_CASTLE_SIEGE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
			else
				player.sendPacket(new SystemMessage(SystemMessage.C1_IS_PARTICIPATING_IN_A_HIDEOUT_SIEGE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
			return false;
		}
		if(pcTarget.getMountEngine().isMounted() || pcTarget.getVehicle() != null)
		{
			player.sendPacket(new SystemMessage(SystemMessage.C1_IS_RIDING_A_SHIP_STEED_OR_STRIDER_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
			return false;
		}
		if(pcTarget.isTeleporting())
		{
			player.sendPacket(new SystemMessage(SystemMessage.C1_IS_CURRENTLY_TELEPORTING_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
			return false;
		}
		if(pcTarget.getTransformation() > 0 && player.getSkillLevel(838) <= 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.C1_IS_CURRENTLY_TRANSFORMING_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION).addCharName(pcTarget));
			return false;
		}
		return true;
	}

	private static class SocialTask implements Runnable
	{
		private L2Player _player1, _player2;
		private int _socialId;

		public SocialTask(L2Player player1, L2Player player2, int socialId)
		{
			_player1 = player1;
			_player2 = player2;
			_socialId = socialId;
		}

		public void run()
		{
			if(_player1.isOnline() && _player2.isOnline())
			{
				_player1.broadcastPacket(new StopMove(_player1));
				_player2.broadcastPacket(new StopMove(_player2));
				_player1.broadcastPacket(new SocialAction(_player1.getObjectId(), _socialId));
				_player2.broadcastPacket(new SocialAction(_player2.getObjectId(), _socialId));
			}
		}
	}
}
