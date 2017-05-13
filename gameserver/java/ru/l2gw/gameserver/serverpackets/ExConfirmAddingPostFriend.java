package ru.l2gw.gameserver.serverpackets;

/**
 * @author admin
 * @date 03.02.11 12:53
 */
public class ExConfirmAddingPostFriend extends L2GameServerPacket
{
	private String _name;
	private int _state;
	public ExConfirmAddingPostFriend(String name, int state)
	{
		_name = name;
		_state = state;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xD3);
		writeS(_name);
		writeD(_state);
	}
}