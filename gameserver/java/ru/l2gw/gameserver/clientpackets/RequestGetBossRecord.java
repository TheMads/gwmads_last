package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.instancemanager.RaidBossSpawnManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.ExGetBossRecord;

/**
 * Format: (ch) d
 * @author  -Wooden-
 *
 */
public class RequestGetBossRecord extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private int _bossID;

	@Override
	public void readImpl()
	{
		_bossID = readD(); // always 0?
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		player.sendPacket(new ExGetBossRecord(RaidBossSpawnManager.getInstance().getPointsByOwnerId(player.getObjectId())));
	}
}