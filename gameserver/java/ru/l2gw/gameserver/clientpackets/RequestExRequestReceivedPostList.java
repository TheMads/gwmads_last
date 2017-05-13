package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.ExShowReceivedPostList;

/**
 * Отсылается при нажатии на кнопку "почта", "received mail" или уведомление от {@link ExNoticePostArrived}, запрос входящих писем.
 * В ответ шлется {@link ExShowReceivedPostList}
 */
public class RequestExRequestReceivedPostList extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{
	//just a trigger
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player != null)
			player.sendPacket(new ExShowReceivedPostList(player));
	}
}