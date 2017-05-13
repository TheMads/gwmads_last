package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.PcInventory;
import ru.l2gw.gameserver.model.instances.L2HennaInstance;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.HennaTable;
import ru.l2gw.gameserver.tables.HennaTreeTable;
import ru.l2gw.gameserver.templates.L2Henna;

public class RequestHennaEquip extends L2GameClientPacket
{
	private int _symbolId;

	/**
	 * format:		cd
	 */
	@Override
	public void readImpl()
	{
		_symbolId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2Henna template = HennaTable.getInstance().getTemplate(_symbolId);
		if(template == null)
			return;

		L2HennaInstance temp = new L2HennaInstance(template);

		boolean cheater = true;
		for(L2HennaInstance h : HennaTreeTable.getInstance().getAvailableHenna(player.getClassId(), player.getSex()))
			if(h.getSymbolId() == temp.getSymbolId())
			{
				cheater = false;
				break;
			}

		if(cheater)
		{
			player.sendPacket(new SystemMessage(SystemMessage.THE_SYMBOL_CANNOT_BE_DRAWN));
			return;
		}

		PcInventory inventory = player.getInventory();
		L2ItemInstance item = inventory.getItemByItemId(temp.getItemIdDye());
		if(item != null && item.getCount() >= temp.getAmountDyeRequire() && player.reduceAdena("Henna", temp.getPrice(), player.getLastNpc(), true) && player.destroyItemByItemId("Henna", temp.getItemIdDye(), temp.getAmountDyeRequire(), player.getLastNpc(), true) && player.addHenna(temp))
			player.sendPacket(new SystemMessage(SystemMessage.THE_SYMBOL_HAS_BEEN_ADDED));
		else
			player.sendPacket(new SystemMessage(SystemMessage.THE_SYMBOL_CANNOT_BE_DRAWN));
	}
}
