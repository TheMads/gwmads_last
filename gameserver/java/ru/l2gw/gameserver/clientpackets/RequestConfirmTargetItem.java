package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.ExPutItemResultForVariationMake;

public class RequestConfirmTargetItem extends AbstractRefinePacket
{
	// format: (ch)d
	private int _itemObjId;

	@Override
	public void readImpl()
	{
		_itemObjId = readD(); // object_id шмотки
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		L2ItemInstance item = player.getInventory().getItemByObjectId(_itemObjId);

		if(!isValid(player, item, true))
			return;

		player.sendPacket(new ExPutItemResultForVariationMake(item.getItemId(), _itemObjId));
		player.sendPacket(Msg.SELECT_THE_CATALYST_FOR_AUGMENTATION);
	}
}
