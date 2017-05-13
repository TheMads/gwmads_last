package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 * @author rage
 * @date 04.11.2010 14:50:50
 */
public class ExBrExtraUserInfo extends L2GameServerPacket
{
	private int _objectId;

	public ExBrExtraUserInfo(L2Player player)
	{
		_objectId = player.getObjectId();
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xDA);
		writeD(_objectId);
		writeD(0);
		//writeC(0x00);		// Event flag, added only if event is active
	}
}
