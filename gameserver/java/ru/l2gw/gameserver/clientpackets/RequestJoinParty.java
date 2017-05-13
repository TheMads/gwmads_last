package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.InstanceManager;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Party;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Player.TransactionType;
import ru.l2gw.gameserver.model.entity.instance.Instance;
import ru.l2gw.gameserver.model.entity.olympiad.Olympiad;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.serverpackets.AskJoinParty;
import ru.l2gw.gameserver.serverpackets.JoinParty;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

/**
 *  sample
 *  29
 *  42 00 00 10
 *  01 00 00 00
 *
 *  format  cdd
 */
public class RequestJoinParty extends L2GameClientPacket
{
	//Format: cSd
	private String _name;
	private int _itemDistribution;

	@Override
	public void readImpl()
	{
		_name = readS();
		_itemDistribution = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2Player target = L2ObjectsStorage.getPlayer(_name);

		player.fireMethodInvoked(MethodCollection.onActionRequest, new Object[] { "party", target });
		
		if(target == null || target == player || player.getSessionVar("event_team_pvp") != null && player.getTeam() != target.getTeam())
		{
			player.sendPacket(new JoinParty(0));
			player.sendPacket(Msg.YOU_HAVE_INVITED_WRONG_TARGET);
			return;
		}

		if(player.inObserverMode())
		{
			player.sendPacket(Msg.OBSERVERS_CANNOT_PARTICIPATE);
			player.sendActionFailed();
			return;
		}

		if(target.isInBlockList(player))
		{
			player.sendPacket(new JoinParty(0));
			return;
		}
		
		if(target.getReflection() == -3)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOUR_TARGET_IS_OUT_OF_RANGE));
			return;
		}

		if(target.getTeam() != 0 && player.getSessionVar("event_team_pvp") == null)
		{
			player.sendActionFailed();
			return;
		}

		if(target.isCursedWeaponEquipped() || player.isCursedWeaponEquipped())
		{
			player.sendPacket(Msg.INVALID_TARGET);
			return;
		}

		if(AdminTemplateManager.checkBoolean("noParty", player) || player.isInOlympiadMode())
		{
			player.sendActionFailed();
			return;
		}

		if(AdminTemplateManager.checkBoolean("noParty", player) || target.isInOlympiadMode())
		{
			player.sendActionFailed();
			return;
		}

		if(player.isTransactionInProgress())
		{
			player.sendPacket(Msg.WAITING_FOR_ANOTHER_REPLY);
			return;
		}

		if(Olympiad.isRegisteredInComp(player) || player.getOlympiadGameId() >= 0)
		{
			player.sendPacket(Msg.A_USER_CURRENTLY_PARTICIPATING_IN_THE_OLYMPIAD_CANNOT_SEND_PARTY_AND_FRIEND_INVITATIONS);
			return;
		}

		if(target.isInParty())
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_IS_A_MEMBER_OF_ANOTHER_PARTY_AND_CANNOT_BE_INVITED).addString(target.getName()));
			return;
		}

		if(!player.isInParty())
			createNewParty(_itemDistribution, target, player);
		else
			addTargetToParty(_itemDistribution, target, player);
	}

	/**
	 * @param itemDistribution
	 * @param target
	 * @param player
	 */
	private void addTargetToParty(int itemDistribution, L2Player target, L2Player player)
	{
		if(player.getParty().getMemberCount() >= 9)
		{
			player.sendPacket(new SystemMessage(SystemMessage.PARTY_IS_FULL));
			return;
		}
		// Только Party Leader может приглашать новых членов
		if(Config.PARTY_LEADER_ONLY_CAN_INVITE && !player.getParty().isLeader(player))
		{
			player.sendPacket(Msg.ONLY_THE_LEADER_CAN_GIVE_OUT_INVITATIONS);
			return;
		}

		Instance inst = InstanceManager.getInstance().getInstanceByPlayer(player);
		if((inst != null && !inst.isInside(target.getObjectId())) || player.getParty().isInDimensionalRift())
		{
			player.sendPacket(Msg.UNABLE_TO_INVITE_BECAUSE_THE_PARTY_IS_LOCKED);
			return;
		}

		if(!target.isTransactionInProgress())
		{
			target.setTransactionRequester(player, System.currentTimeMillis() + 10000);
			target.setTransactionType(TransactionType.PARTY);
			player.setTransactionRequester(target, System.currentTimeMillis() + 10000);
			player.setTransactionType(TransactionType.PARTY);

			target.sendPacket(new AskJoinParty(player.getName(), itemDistribution));
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_INVITED_S1_TO_YOUR_PARTY).addString(target.getName()));
		}
		else
			player.sendPacket(new SystemMessage(SystemMessage.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER).addString(target.getName()));

	}

	/**
	 * @param itemDistribution
	 * @param target
	 * @param requestor
	 */
	private void createNewParty(int itemDistribution, L2Player target, L2Player requestor)
	{
		if(!target.isTransactionInProgress())
		{
			Instance inst = InstanceManager.getInstance().getInstanceByPlayer(requestor);
			if(inst != null && !inst.isInside(target.getObjectId()))
			{
				requestor.sendPacket(Msg.UNABLE_TO_INVITE_BECAUSE_THE_PARTY_IS_LOCKED);
				return;
			}

			requestor.setParty(new L2Party(requestor, itemDistribution));
			target.setTransactionRequester(requestor, System.currentTimeMillis() + 10000);
			target.setTransactionType(TransactionType.PARTY);
			requestor.setTransactionRequester(target, System.currentTimeMillis() + 10000);
			requestor.setTransactionType(TransactionType.PARTY);
			target.sendPacket(new AskJoinParty(requestor.getName(), itemDistribution));
			requestor.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_INVITED_S1_TO_YOUR_PARTY).addString(target.getName()));
		}
		else
			requestor.sendPacket(new SystemMessage(SystemMessage.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER).addString(target.getName()));
	}
}