package ru.l2gw.gameserver.serverpackets;

public class GameGuardQuery extends L2GameServerPacket
{
	@Override
	final public void runImpl()
	{
		getClient().setGameGuardOk(false);
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x74);
		writeD(0x00); // ? - Меняется при каждом перезаходе.
		writeD(0x00); // ? - Меняется при каждом перезаходе.
		writeD(0x00); // ? - Меняется при каждом перезаходе.
		writeD(0x00); // ? - Меняется при каждом перезаходе.
	}
}