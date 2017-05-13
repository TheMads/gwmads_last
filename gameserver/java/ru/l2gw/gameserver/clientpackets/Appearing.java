package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.olympiad.Olympiad;

/**
 * Appearing Packet Handler<p>
 * <p>
 * 0000: 30 <p>
 * <p>
 */
public class Appearing extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		final L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		if(player.inObserverMode() && player.getOlympiadGameId() == -1)
		{
			player.appearObserverMode();
			return;
		}

		if(!player.isTeleporting() && L2ObjectsStorage.getAsPlayer(player.getStoredId()) != null)
		{
			player.sendActionFailed();
			return;
		}

		// 15 секунд после телепорта на персонажа не агрятся мобы
		player.setNonAggroTime(System.currentTimeMillis() + 15000);
		player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null, null);

		// Персонаж появляется только после полной прогрузки
		if(player.isTeleporting())
			player.onTeleported();
		else
			player.spawnMe(player.getLoc());

		player.sendUserInfo(true);

		if(player.inObserverMode() && player.getOlympiadGameId() >= 0)
			Olympiad.broadcastPlayersState(player.getOlympiadGameId());
	}
}