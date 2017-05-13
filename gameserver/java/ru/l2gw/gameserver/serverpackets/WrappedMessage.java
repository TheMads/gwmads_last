package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.commons.network.MMOConnection;
import ru.l2gw.gameserver.clientpackets.L2GameClientPacket;
import ru.l2gw.gameserver.network.GameClient;

public class WrappedMessage extends L2GameServerPacket
{
	final byte[] data;

	@SuppressWarnings("unchecked")
	public WrappedMessage(byte[] data, @SuppressWarnings("unused") MMOConnection<GameClient> con)
	{
		this.data = data;
	}

	public int size()
	{
		return data.length + 2;
	}

	public byte[] getData()
	{
		return data;
	}

	public L2GameClientPacket getClientMsg()
	{
		return null;
	}

	@Override
	protected final void writeImpl()
	{
		writeB(data);
	}
}
