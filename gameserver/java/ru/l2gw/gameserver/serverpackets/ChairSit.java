package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 * sample
 * format
 * d
 */
public class ChairSit extends L2GameServerPacket
{
	private L2Player _player;
	private int _staticObjectId;

	public ChairSit(L2Player player, int staticObjectId)
	{
		_player = player;
		_staticObjectId = staticObjectId;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xed);
		writeD(_player.getObjectId());
		writeD(_staticObjectId);
	}
}