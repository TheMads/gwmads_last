package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.PcInventory;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.ExPutEnchantTargetItemResult;
import ru.l2gw.gameserver.templates.L2Item;

public class RequestExTryToPutEnchantTargetItem extends AbstractEnchantPacket
{
	private int _objectId;

	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null || _objectId == 0)
			return;

		if(player.isOutOfControl() || player.isActionsDisabled())
		{
			player.sendActionFailed();
			return;
		}

		PcInventory inventory = player.getInventory();
		L2ItemInstance itemToEnchant = inventory.getItemByObjectId(_objectId);
		L2ItemInstance scroll = player.getEnchantScroll();

		if(itemToEnchant == null || scroll == null || player.getEnchantStartTime() > 0)
		{
			System.out.println("PutTraget: " + itemToEnchant + " " + scroll + " " + player.getEnchantStartTime());
			player.cancelActiveEnchant();
			return;
		}

		if(player.isInStoreMode())
		{
			player.cancelActiveEnchant();
			player.sendPacket(new ExPutEnchantTargetItemResult(0));
			player.sendPacket(Msg.YOU_CANNOT_PRACTICE_ENCHANTING_WHILE_OPERATING_A_PRIVATE_STORE_OR_PRIVATE_MANUFACTURING_WORKSHOP);
			return;
		}

		// template for scroll
		EnchantScroll scrollTemplate = getEnchantScroll(scroll);
		if(Config.OVER_ENCHANT_ENABLED)
		{
			int maxEnchant = 0;
			if(itemToEnchant.getItem().getType2() == L2Item.TYPE2_WEAPON)
				maxEnchant = Config.ENCHANT_MAX_WEAPON;
			else if(itemToEnchant.getItem().getType2() == L2Item.TYPE2_ACCESSORY)
				maxEnchant = Config.ENCHANT_MAX_ACCESSORY;
			else if(itemToEnchant.getItem().getType2() == L2Item.TYPE1_SHIELD_ARMOR)
				maxEnchant = Config.ENCHANT_MAX_ARMOR;

			if(maxEnchant > 0 && maxEnchant <= itemToEnchant.getEnchantLevel() && scrollTemplate.isValid(itemToEnchant) && isEnchantable(itemToEnchant))
			{
				player.scriptRequest(new CustomMessage("services.overenchant.request", player).addNumber(itemToEnchant.getEnchantLevel() + Config.OVER_ENCHANT_VALUE).addItemName(itemToEnchant.getItemId()).toString(), "services.OverEnchant:onAnswer", new Integer[]{ _objectId }, 15000, "services.OverEnchant:onAnswerFail");
				return;
			}
		}

		if(!scrollTemplate.isValid(itemToEnchant) || !isEnchantable(itemToEnchant))
		{
			player.sendPacket(Msg.DOES_NOT_FIT_STRENGTHENING_CONDITIONS_OF_THE_SCROLL);
			player.cancelActiveEnchant();
			player.sendPacket(new ExPutEnchantTargetItemResult(0));
			return;
		}

		player.setEnchantStartTime(System.currentTimeMillis());
		player.sendPacket(new ExPutEnchantTargetItemResult(_objectId));
	}
}