package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.ExRpItemLink;
import ru.l2gw.gameserver.taskmanager.ItemLinksManager;

public class RequestExRqItemLink extends L2GameClientPacket
{
	// format: (ch)d
	int _item;

	@Override
	public void readImpl()
	{
		_item = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player != null)
		{
			L2ItemInstance item = ItemLinksManager.getInstance().getItem(_item);
			if(item != null)
				player.sendPacket(new ExRpItemLink(item));
			else
				player.sendActionFailed();
		}
	}
}