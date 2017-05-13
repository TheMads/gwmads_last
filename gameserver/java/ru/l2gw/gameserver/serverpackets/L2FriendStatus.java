package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;

public class L2FriendStatus extends L2GameServerPacket
{
	private String char_name;
	private boolean _login = false;

	public L2FriendStatus(L2Player player, boolean login)
	{
		if(player == null)
			return;
		_login = login;
		char_name = player.getName();
	}

	@Override
	protected final void writeImpl()
	{
		if(char_name == null)
			return;
		writeC(0x77);
		writeD(_login ? 1 : 0); //Logged in 1 logged off 0
		writeS(char_name);
		writeD(0); //id персонажа с базы оффа, не object_id
	}
}