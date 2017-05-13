package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.ExGoodsInventoryResult;

/**
 * @author VISTALL
 * @date 23:33/23.03.2011
 */
public class RequestGoodsInventoryInfo extends L2GameClientPacket
{
	@Override
	protected void readImpl() throws Exception
	{

	}

	@Override
	protected void runImpl() throws Exception
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
		{
			return;
		}
		player.sendPacket(new ExGoodsInventoryResult(-6));
		// player.sendPacket(new ExGoodsInventoryInfo(player));
	}
}
