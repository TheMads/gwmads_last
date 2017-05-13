package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;

/**
 * Пример:
 * 08
 * a5 04 31 48 ObjectId
 * 00 00 00 7c unk
 *
 * format  d
 */
public class DeleteObject extends L2GameServerPacket
{
	private int _objectId;

	public DeleteObject(L2Object obj)
	{
		_objectId = obj.getObjectId();
	}

	@Override
	final public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player != null && player.getObjectId() == _objectId)
		{
			_log.warn("Try self.DeleteObject for " + getClient().getPlayer());
			Thread.dumpStack();
			_objectId = 0;
		}
	}

	@Override
	protected final void writeImpl()
	{
		if(_objectId == 0)
			return;

		writeC(0x08);
		writeD(_objectId);
		writeD(0x01); // Что-то странное. Если объект сидит верхом то при 0 он сперва будет ссажен, при 1 просто пропадет.
	}
}