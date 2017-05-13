package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.serverpackets.PackageSendableList;

/**
 * Format: cd
 *
 * Пример пакета с оффа:
 * 9E D0 33 08 00
 *
 * @author SYS
 */
public class RequestPackageSendableItemList extends L2GameClientPacket
{
	private int _characterObjectId;

	@Override
	public void readImpl()
	{
		_characterObjectId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null || AdminTemplateManager.checkBoolean("noWarehouse", player))
			return;

		player.tempInvetoryDisable();

		if(Config.DEBUG)
			_log.info("Showing items to freight");

		player.sendPacket(new PackageSendableList(player, _characterObjectId));
	}
}
