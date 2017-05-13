package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.duel.Duel;

public class RequestDuelSurrender extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		Duel duel = player.getDuel();
		if(duel != null && !duel.isPartyDuel())
			duel.doSurrender(player);
	}
}