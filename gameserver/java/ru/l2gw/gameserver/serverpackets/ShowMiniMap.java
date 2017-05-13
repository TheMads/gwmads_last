package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.entity.SevenSigns;

/**
 * sample
 * format
 * d
 */
public class ShowMiniMap extends L2GameServerPacket
{
	private int _mapId, _period;

	public ShowMiniMap(int mapId)
	{
		_mapId = mapId;
		_period = SevenSigns.getInstance().getCurrentPeriod();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xA3);
		writeD(_mapId);
		writeC(_period);
	}
}