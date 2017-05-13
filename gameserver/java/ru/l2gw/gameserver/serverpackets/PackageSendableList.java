package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.templates.L2Item;

/**
 * Fotmat:
 * d char object id
 * d количество адены
 * d количество предметов
 * [hdddhhdhhhddddddddd]
 *
 * Пример с оффа(828 протокол):
 * 0000: d2 97 e7 07 00 41 06 00 00 04 00 00 00 04 00 70    .....A.........p
 * 0010: 28 54 41 39 00 00 00 41 06 00 00 04 00 00 00 00    (TA9...A........
 * 0020: 00 00 00 00 00 00 00 00 00 f1 ca f8 06 fe ff ff    ................
 * 0030: ff 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00    ................
 * 0040: 00 00 00 00 00 00 00 00 00 00 00 00 00 04 00 b2    ................
 * 0050: 78 63 41 b4 21 00 00 01 00 00 00 05 00 00 00 00    xcA.!...........
 * 0060: 00 00 00 00 00 00 00 00 00 d9 fa e2 06 fe ff ff    ................
 * 0070: ff 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00    ................
 * 0080: 00 00 00 00 00 00 00 00 00 00 00 00 00 04 00 94    ................
 * 0090: 80 59 41 af 21 00 00 01 00 00 00 05 00 00 00 00    .YA.!...........
 * 00a0: 00 00 00 00 00 00 00 00 00 d8 fa e2 06 fe ff ff    ................
 * 00b0: ff 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00    ................
 * 00c0: 00 00 00 00 00 00 00 00 00 00 00 00 00 04 00 53    ...............S
 * 00d0: 8d 25 41 ae 21 00 00 02 00 00 00 05 00 00 00 00    .%A.!...........
 * 00e0: 00 00 00 00 00 00 00 00 00 88 b7 e2 06 fe ff ff    ................
 * 00f0: ff 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00    ................
 * 0100: 00 00 00 00 00 00 00 00 00 00 00 00 00             .............
 */
public class PackageSendableList extends AbstractItemPacket
{
	private int player_obj_id;
	private long char_adena;
	private GArray<L2ItemInstance> _itemslist = new GArray<L2ItemInstance>();

	public PackageSendableList(L2Player player, int playerObjId)
	{
		player_obj_id = playerObjId;
		char_adena = player.getAdena();
		for(L2ItemInstance item : player.getInventory().getItems())
			if(!item.isEquipped() && item.getItem().getType2() != L2Item.TYPE2_QUEST && item.isFreightPossible(player))
				_itemslist.add(item);
	}

	@Override
	protected final void writeImpl()
	{
		if(player_obj_id == 0)
			return;

		writeC(0xD2);
		writeD(player_obj_id);
		writeQ(char_adena);
		writeD(_itemslist.size());
		for(L2ItemInstance temp : _itemslist)
		{
			writeItemInfo(temp);
			writeD(temp.getObjectId());
		}
	}
}