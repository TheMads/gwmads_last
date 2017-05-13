package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.serverpackets.SendStatus;

public class RequestStatus extends L2GameClientPacket
{
	private byte _opcode;

	@Override
	public void readImpl()
	{
		_opcode = (byte) readC();
	}

	@Override
	public void runImpl()
	{
		getClient().close(new SendStatus(_opcode));
	}
}
