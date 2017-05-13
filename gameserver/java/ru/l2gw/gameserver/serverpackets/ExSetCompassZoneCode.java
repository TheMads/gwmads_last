package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 * http://forum.l2jserver.com/thread.php?threadid=22736
 */
public class ExSetCompassZoneCode extends L2GameServerPacket
{
	public static int ZONE_ALTERED = 8; // 9, 10 - Danger Area???
	public static int ZONE_SIEGE = 11;
	public static int ZONE_PEACE = 12;
	public static int ZONE_SS = 13;
	public static int ZONE_PVP = 14; // 1, 2, 3, 4, 5, 6, 7
	public static int ZONE_GENERAL_FIELD = 15; //0 и > 15

	private int _zone = -1;

	public ExSetCompassZoneCode(L2Player player)
	{
		_zone = player.getLastCompassZone();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x33);
		writeD(_zone);
	}
}
