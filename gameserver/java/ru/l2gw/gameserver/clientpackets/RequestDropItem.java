package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.InventoryUpdate;
import ru.l2gw.gameserver.tables.GmListTable;
import ru.l2gw.gameserver.tables.PetDataTable;
import ru.l2gw.gameserver.templates.L2EtcItem.EtcItemType;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.util.Location;

public class RequestDropItem extends L2GameClientPacket
{
	private int _objectId;
	private long _count;
	private Location _loc;

	/**
	 * format:		cdd ddd
	 */
	@Override
	public void readImpl()
	{
		_objectId = readD();
		_count = readQ();
		_loc = new Location(readD(), readD(), readD());
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null || player.isDead())
			return;

		if(player.inObserverMode())
		{
			player.sendPacket(Msg.OBSERVERS_CANNOT_PARTICIPATE);
			player.sendActionFailed();
			return;
		}
 
		if(_count < 1 || _loc.getX() == 0 || _loc.getY() == 0 || _loc.getZ() == 0)
		{
			player.sendActionFailed();
			return;
		}

		if(!Config.ALLOW_DISCARDITEM || AdminTemplateManager.checkBoolean("noDropItems", player))
		{
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestDropItem.Disallowed", player));
			return;
		}

		if(AdminTemplateManager.checkBoolean("noInventory", player))
		{
			player.sendActionFailed();
			return;
		}

		if(player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			player.sendPacket(Msg.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
			return;
		}

		if(player.isTransactionInProgress() || player.isActionBlocked(L2Zone.BLOCKED_ITEM_DROP))
		{
			sendPacket(Msg.NOTHING_HAPPENED);
			return;
		}

		L2ItemInstance oldItem = player.getInventory().getItemByObjectId(_objectId);
		if(oldItem == null)
		{
			_log.warn(player.getName() + ":tried to drop an item that is not in the inventory ?!?:" + _objectId);
			return;
		}

		if(player.isFishing())
		{
			player.sendPacket(Msg.YOU_CANNOT_DO_THAT_WHILE_FISHING);
			return;
		}

		if(player.isActionsBlocked() || player.isStunned() || player.isSleeping() || player.isParalyzed() || player.isInBoat() || player.isAlikeDead() || player.isDropDisabled())
		{
			player.sendActionFailed();
			return;
		}

		if(oldItem.isActivePetControlItem(player))
		{
			player.sendPacket(Msg.THE_PET_HAS_BEEN_SUMMONED_AND_CANNOT_BE_LET_GO);
			return;
		}

		if(player.isCastingNow() && PetDataTable.isPetControlItem(oldItem))
		{
			player.sendActionFailed();
			return;
		}

		if((oldItem.getCustomFlags() & L2ItemInstance.FLAG_PET_INVENTORY) == L2ItemInstance.FLAG_PET_INVENTORY)
		{
			player.sendPacket(Msg.THERE_ARE_ITEMS_IN_YOUR_PET_INVENTORY_RENDERING_YOU_UNABLE_TO_SELL_TRADE_DROP_PET_SUMMONING_ITEM_PLEASE_EMPTY_YOUR_PET_INVENTORY);
			return;
		}

		if(oldItem.getItemType() == EtcItemType.QUEST || !oldItem.canBeDropped(player))
		{
			player.sendPacket(Msg.THAT_ITEM_CANNOT_BE_DISCARDED);
			return;
		}

		long oldCount = oldItem.getCount();
		if(Config.DEBUG)
			_log.warn("requested drop item " + _objectId + "(" + oldCount + ") at " + _loc.toString());

		if(oldCount < _count)
		{
			player.sendActionFailed();
			if(Config.DEBUG)
				_log.warn(player.getObjectId() + ":player tried to drop more items than he has");
			return;
		}

		if(!player.isInRangeSq(_loc, 22500) || Math.abs(_loc.getZ() - player.getZ()) > 50 || !GeoEngine.canMoveToCoord(player.getX(), player.getY(), player.getZ(), _loc.getX(), _loc.getY(), player.getZ(), player.getReflection()))
		{
			if(Config.DEBUG)
				_log.warn(player.getObjectId() + ": trying to drop too far away");
			player.sendPacket(Msg.TOO_FAR_TO_DISCARD);
			return;
		}

		if(player.getEnchantScroll() != null)
		{
			player.sendActionFailed();
			if(Config.DEBUG)
				_log.warn(player.getObjectId() + ":player tried to drop item while enchant in progress.");
			return;
		}

		if(oldItem.isEquipped())
		{
			L2ItemInstance weapon = player.getActiveWeaponInstance();
			GArray<L2ItemInstance> unequipped = player.getInventory().unEquipItemAndRecord(oldItem);

			for(L2ItemInstance uneq : unequipped)
			{
				if(uneq == null)
					continue;

				player.sendDisarmMessage(oldItem);

				if(weapon != null && uneq == weapon)
				{
					uneq.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
					uneq.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
				}
			}
			player.sendPacket(new InventoryUpdate(unequipped));
			player.refreshExpertisePenalty();
			player.broadcastUserInfo();
		}

		L2ItemInstance dropedItem = player.getInventory().dropItem("Drop", _objectId, _count, player, null);

		if(dropedItem == null)
		{
			player.sendActionFailed();
			return;
		}

		dropedItem.dropToTheGround(player, _loc);

		player.disableDrop(1000);

		if(dropedItem.getItemId() == 57 && dropedItem.getCount() >= 1000000)
		{
			String msg = "Character (" + player.getName() + ") has dropped (" + dropedItem.getCount() + ") adena at " + _loc.toString();
			_log.warn(msg);
			GmListTable.broadcastMessageToGMs(msg);
		}

		player.updateStats();
	}
}