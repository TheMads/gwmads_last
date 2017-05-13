package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 18.06.2010 11:33:32
 */
public class ExReplyWritePost extends L2GameServerPacket
{
	private int _reply;

	/**
	 * @param i если 1 окно создания письма закрывается
	 */
	public ExReplyWritePost(int i)
	{
		_reply = i;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xB5);
		writeD(_reply); // 1 - закрыть окно письма, иное - не закрывать
	}
}
