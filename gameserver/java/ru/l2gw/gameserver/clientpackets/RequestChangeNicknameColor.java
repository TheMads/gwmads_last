package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

public class RequestChangeNicknameColor extends L2GameClientPacket
{
	private static final int COLORS[] =
			{
					0x9393FF,	// Pink
					0x7C49FC,	// Rose Pink
					0x97F8FC,	// Lemon Yellow
					0xFA9AEE,	// Lilac
					0xFF5D93,	// Cobalt Violet
					0x00FCA0,	// Mint Green
					0xA0A601,	// Peacock Green
					0x7898AF,	// Yellow Ochre
					0x486295,	// Chocolate
					0x999999	// Silver
			};

	private int _colorNum, _itemObjectId;
	private String _title;

	@Override
	protected void readImpl()
	{
		_colorNum = readD();
		_title = readS(16);
		_itemObjectId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(_colorNum < 0 || _colorNum >= COLORS.length)
			return;

		final L2ItemInstance item = player.getInventory().getItemByObjectId(_itemObjectId);
		if(item == null || item.getItemId() != 13021 && item.getItemId() != 13307)
			return;

		if(player.destroyItem("Consume", _itemObjectId, 1, null, true))
		{
			player.setTitle(_title);
			player.setTitleColor(COLORS[_colorNum]);
			player.setVar("titlecolor", Integer.toHexString(player.getTitleColor()));
			player.broadcastUserInfo(true);
		}
	}
}