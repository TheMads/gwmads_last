package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 18.06.2010 15:15:53
 */
public class ExChangePostState extends L2GameServerPacket
{
	private int _listId;
	private int _messages[];

	public ExChangePostState(int listId, int[] messages)
	{
		_listId = listId;
		_messages = messages;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xB4);
		writeD(_listId);
		writeD(_messages.length / 2);
		for(int i = 0; i < _messages.length; i += 2)
		{
			writeD(_messages[i]); // message id
			writeD(_messages[i + 1]); // message state 0 - delete, 1 - read;
		}
	}
}

