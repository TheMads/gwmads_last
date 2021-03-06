package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;

public class RecipeShopMsg extends L2GameServerPacket
{
	private int _chaObjectId;
	private String _chaStoreName;

	public RecipeShopMsg(L2Player player)
	{
		if(player.getCreateList() == null || player.getCreateList().getStoreName() == null)
			return;
		_chaObjectId = player.getObjectId();
		_chaStoreName = player.getCreateList().getStoreName();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xE1);
		writeD(_chaObjectId);
		writeS(_chaStoreName);
	}
}