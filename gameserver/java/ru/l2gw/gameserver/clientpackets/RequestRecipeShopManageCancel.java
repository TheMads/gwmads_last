package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.RecipeShopSellList;

// Deprecated?
public class RequestRecipeShopManageCancel extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null || player.getTarget() == null)
			return;

		if(player.isInDuel())
		{
			player.sendActionFailed();
			return;
		}

		if(player.isAlikeDead())
		{
			player.sendActionFailed();
			return;
		}

		if(!player.getTarget().isPlayer())
		{
			player.sendActionFailed();
			return;
		}

		L2Player target = (L2Player) player.getTarget();
		player.sendPacket(new RecipeShopSellList(player, target));
	}
}