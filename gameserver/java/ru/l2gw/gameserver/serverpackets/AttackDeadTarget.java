package ru.l2gw.gameserver.serverpackets;

public class AttackDeadTarget extends L2GameServerPacket
{
	@Override
	protected void writeImpl()
	{
		writeC(0x04);
		// just trigger - без аргументов
	}
}