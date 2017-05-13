package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.EnchantResult;

public class RequestExCancelEnchantItem extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{
	// nothing (trigger)
	}

	@Override
	protected void runImpl()
	{
		L2Player player = this.getClient().getPlayer();
		if(player != null)
		{
			player.sendPacket(new EnchantResult(2));
			player.setEnchantScroll(null);
			player.setEnchantSupportItem(null);
			player.setEnchantStartTime(0);
		}
	}
}
