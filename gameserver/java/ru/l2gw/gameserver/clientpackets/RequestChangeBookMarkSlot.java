package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;

public class RequestChangeBookMarkSlot extends L2GameClientPacket
{
	private int slot_old, slot_new;

	@Override
	public void readImpl()
	{
		slot_old = readD();
		slot_new = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getPlayer();
		System.out.println(getType() + "@" + activeChar + "::" + slot_old + "::" + slot_new);
	}
}