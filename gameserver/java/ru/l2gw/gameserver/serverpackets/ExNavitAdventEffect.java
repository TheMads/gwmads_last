package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 04.02.11 0:27
 */
public class ExNavitAdventEffect extends L2GameServerPacket
{
	private final int _time;

	public ExNavitAdventEffect(int time)
	{
		_time = time;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xE4);
		writeD(_time); // Время в секундах сколько будет мегять полоска виталити на оффе (3 минуты)
	}
}
