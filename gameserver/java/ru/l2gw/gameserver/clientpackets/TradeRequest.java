package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Player.TransactionType;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.serverpackets.SendTradeRequest;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class TradeRequest extends L2GameClientPacket
{
	//Format: cd
	private int _objectId;

	@Override
	public void readImpl()
	{
		_objectId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(AdminTemplateManager.checkBoolean("noTrade", player) || player.isInJail())
		{
			player.sendPacket(Msg.THIS_ACCOUNT_CANOT_TRADE_ITEMS);
			player.sendActionFailed();
			return;
		}

		if(player.isDead())
		{
			player.sendActionFailed();
			return;
		}

		L2Object target = player.getVisibleObject(_objectId);

		if(target == null || !target.isPlayer() || target.getObjectId() == player.getObjectId())
		{
			player.sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
			return;
		}

		if(!player.knowsObject(target) || !GeoEngine.canSeeTarget(player, target))
		{
			player.sendPacket(Msg.CANNOT_SEE_TARGET);
			return;
		}

		L2Player pcTarget = (L2Player) target;

		if(player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE || pcTarget.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			player.sendPacket(Msg.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
			return;
		}

		if(AdminTemplateManager.checkBoolean("noTrade", pcTarget) || pcTarget.isInJail())
		{
			player.sendPacket(Msg.THIS_ACCOUNT_CANOT_TRADE_ITEMS);
			player.sendActionFailed();
			return;
		}

		if(pcTarget.getTeam() != 0)
		{
			player.sendActionFailed();
			return;
		}

		if(pcTarget.isInOlympiadMode() || player.isInOlympiadMode())
		{
			player.sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
			return;
		}

		if(pcTarget.getTradeRefusal() || pcTarget.isInBlockList(player) || pcTarget.isBlockAll())
		{
			player.sendPacket(Msg.YOU_HAVE_BEEN_BLOCKED_FROM_THE_CONTACT_YOU_SELECTED);
			return;
		}

		if(player.isTransactionInProgress())
		{
			player.sendPacket(Msg.ALREADY_TRADING);
			return;
		}

		if(pcTarget.isTransactionInProgress())
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER).addString(pcTarget.getName()));
			return;
		}

		if(player.isFishing())
		{
			player.sendPacket(Msg.YOU_CANNOT_DO_THAT_WHILE_FISHING);
			return;
		}

		AbstractEnchantPacket.checkAndCancelEnchant(player);

		pcTarget.setTransactionRequester(player, System.currentTimeMillis() + 10000);
		pcTarget.setTransactionType(TransactionType.TRADE);
		player.setTransactionRequester(pcTarget, System.currentTimeMillis() + 10000);
		player.setTransactionType(TransactionType.TRADE);
		pcTarget.sendPacket(new SendTradeRequest(player.getObjectId()));
		player.sendPacket(new SystemMessage(SystemMessage.REQUEST_S1_FOR_TRADE).addString(pcTarget.getName()));
	}
}
