package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 16.12.10 16:47
 */
public class ExEventMatchMessage extends L2GameServerPacket
{
	private int _type;
	private String _message;

	/**
	 * Create an event match message.
	 * @param type 0 - gm, 1 - finish, 2 - start, 3 - game over, 4 - 1, 5 - 2, 6 - 3, 7 - 4, 8 - 5
	 * @param message message to show, only when type is 0 - gm
	 */
	public ExEventMatchMessage(int type, String message)
	{
		_type = type;
		_message = message;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x0f);
		writeC(_type);
		writeS(_message);
	}
}
