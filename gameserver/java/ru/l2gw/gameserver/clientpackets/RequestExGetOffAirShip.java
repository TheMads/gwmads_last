package ru.l2gw.gameserver.clientpackets;

public class RequestExGetOffAirShip extends L2GameClientPacket
{
	private int _x;
	private int _y;
	private int _z;
	private int _shipId;

	@Override
	protected void readImpl()
	{
		_x = readD();
		_y = readD();
		_z = readD();
		_shipId = readD();
	}

	@Override
	protected void runImpl()
	{
		System.out.println("[T1:RequestExGetOffAirShip] x: " + _x);
		System.out.println("[T1:RequestExGetOffAirShip] y: " + _y);
		System.out.println("[T1:RequestExGetOffAirShip] z: " + _z);
		System.out.println("[T1:RequestExGetOffAirShip] ship ID: " + _shipId);
	}
}