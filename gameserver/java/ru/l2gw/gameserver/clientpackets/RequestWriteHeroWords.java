package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.entity.Hero;
import ru.l2gw.gameserver.model.L2Player;

public class RequestWriteHeroWords extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private String _heroWords;

	@Override
	public void readImpl()
	{
		_heroWords = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if (player == null || !player.isHero())
			return;
		Hero.setHeroMessage(player.getObjectId(), _heroWords);
	}
}