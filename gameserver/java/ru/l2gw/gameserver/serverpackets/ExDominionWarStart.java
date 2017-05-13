package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 * @author: rage
 * @date: 11.07.2010 10:17:45
 */
public class ExDominionWarStart extends L2GameServerPacket
{
	private int _objId;
	private int _terId;
	private int _disguised;
	
	public ExDominionWarStart(L2Player player)
	{
		_objId = player.getObjectId();
		_terId = player.getTerritoryId();
		_disguised = player.getVarInt("disguised");
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xA4);
		writeD(_objId);
		writeD(0x01);
		writeD(_terId);
		writeD(_disguised > 0 ? 1 : 0); // Disgaused
		writeD(_disguised); // Disgaused Territory Name
	}
}
