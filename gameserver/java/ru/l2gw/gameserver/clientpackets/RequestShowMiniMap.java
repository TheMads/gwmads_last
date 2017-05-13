package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.serverpackets.ShowMiniMap;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.zone.L2Zone;

public class RequestShowMiniMap extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		// TODO: Разобраться почему игровое время в клиенте расходится с сервером
		// Временный фикс, посылаем клиенту время во время открытия карты.
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(player.isActionBlocked(L2Zone.BLOCKED_MINI_MAP))
		{
			sendPacket(Msg.THIS_IS_AN_AREA_WHERE_YOU_CANNOT_USE_THE_MINI_MAP_THE_MINI_MAP_WILL_NOT_BE_OPENED);
			return;
		}
		sendPacket(Msg.ClientSetTime);
		sendPacket(new ShowMiniMap(1665));
	}
}
