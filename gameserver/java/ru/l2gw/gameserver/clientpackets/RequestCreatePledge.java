package ru.l2gw.gameserver.clientpackets;

public class RequestCreatePledge extends L2GameClientPacket
{
	//Format: cS
	private String _pledgename;

	@Override
	public void readImpl()
	{
		_pledgename = readS();
	}

	@Override
	public void runImpl()
	{
		System.out.println("Unfinished packet: " + getType());
		System.out.println("  S: " + _pledgename);
	}
}
