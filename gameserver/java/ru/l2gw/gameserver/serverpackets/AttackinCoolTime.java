package ru.l2gw.gameserver.serverpackets;

public class AttackinCoolTime extends L2GameServerPacket
{
	@Override
	protected void writeImpl()
	{
		writeC(0x03);
		// just trigger - без аргументов
	}
}