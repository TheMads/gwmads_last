package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;

public class ExAdenaInvenCount extends L2GameServerPacket
{
	private final long _adena;
	private final int _useInventorySlots;

	public ExAdenaInvenCount(L2Player player)
	{
		_adena = player.getAdena();
		_useInventorySlots = player.getInventory().getSize();
	}

	@Override
	protected void writeImpl()
	{
		writeEx(0x148);
		writeQ(_adena);
		writeD(_useInventorySlots);
	}
}