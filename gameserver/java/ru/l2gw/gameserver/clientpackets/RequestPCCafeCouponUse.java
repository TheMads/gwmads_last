package ru.l2gw.gameserver.clientpackets;

/**
 * User: Death
 * Date: 23 груд 2007
 * Time: 19:24:13
 * format: chS
 */
public class RequestPCCafeCouponUse extends L2GameClientPacket
{
	// format: (ch)S
	@SuppressWarnings("unused")
	private String _unknown;

	@Override
	public void readImpl()
	{
		_unknown = readS();
	}

	@Override
	public void runImpl()
	{
		System.out.println("Unfinished packet: " + getType());
		System.out.println("  S: " + _unknown);
	}
}