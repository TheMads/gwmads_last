package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.ExPutEnchantSupportItemResult;

/**
 * @author viRUS
 */
public class RequestExTryToPutEnchantSupportItem extends AbstractEnchantPacket
{
	private int _supportObjectId;
	private int _enchantObjectId;

	@Override
	public void readImpl()
	{
		_supportObjectId = readD();
		_enchantObjectId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null || player.getEnchantStartTime() == 0)
			return;

		L2ItemInstance item = player.getInventory().getItemByObjectId(_enchantObjectId);
		L2ItemInstance support = player.getInventory().getItemByObjectId(_supportObjectId);

		if(item == null || support == null)
			return;

		EnchantItem supportTemplate = getSupportItem(support);

		if(supportTemplate == null || !supportTemplate.isValid(item))
		{
			player.sendPacket(Msg.THIS_IS_AN_INCORRECT_SUPPORT_ENHANCEMENT_SPELLBOOK);
			player.cancelActiveEnchant();
			player.sendPacket(new ExPutEnchantSupportItemResult(0));
			return;
		}
		player.setEnchantSupportItem(support);
		player.sendPacket(new ExPutEnchantSupportItemResult(_supportObjectId));
	}
}