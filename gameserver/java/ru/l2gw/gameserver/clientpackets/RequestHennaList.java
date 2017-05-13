package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.HennaEquipList;
import ru.l2gw.gameserver.tables.HennaTreeTable;

public class RequestHennaList extends L2GameClientPacket
{
	// format: cd
	// This is just a trigger packet...
	@SuppressWarnings("unused")
	private int _unknown;

	@Override
	public void readImpl()
	{
		_unknown = readD(); // ??
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;
		player.sendPacket(new HennaEquipList(player, HennaTreeTable.getInstance().getAvailableHenna(player.getClassId(), player.getSex())));
	}
}