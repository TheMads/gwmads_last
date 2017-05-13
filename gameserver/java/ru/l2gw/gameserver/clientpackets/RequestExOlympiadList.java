package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.olympiad.Olympiad;
import ru.l2gw.gameserver.serverpackets.ExReceiveOlympiad;

/**
 * @author rage
 * @date 29.04.11 16:57
 */
public class RequestExOlympiadList extends L2GameClientPacket
{
	private int unk;

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player != null && (Olympiad.getRegisteredGameType(player) < 0 || player.inObserverMode()))
			player.sendPacket(new ExReceiveOlympiad());
	}

	/**
	 * format: d
	 */
	@Override
	public void readImpl()
	{
		unk = readD();
	}
}
