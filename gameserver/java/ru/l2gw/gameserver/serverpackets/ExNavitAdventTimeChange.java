package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 04.02.11 0:30
 */
public class ExNavitAdventTimeChange extends L2GameServerPacket
{
	private final int _active, _time;

	public ExNavitAdventTimeChange(boolean active, int time)
	{
		_active = active ? 1 : 0;
		_time = 14400 - time;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xE5);
		writeC(_active); // 1 - показывает время, 0 пишет Maintenace.
		writeD(_time); // Оставшееся время. Макс. = 14400
	}
}
