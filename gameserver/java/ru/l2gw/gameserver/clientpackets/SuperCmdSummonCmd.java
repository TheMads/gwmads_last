package ru.l2gw.gameserver.clientpackets;

/**
 * Format chS
 * c: (id) 0x39
 * h: (subid) 0x01
 * S: the summon name (or maybe cmd string ?)
 * @author -Wooden-
 *
 */
class SuperCmdSummonCmd extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private String _summonName;

	/**
	 * @param buf
	 * @param client
	 */
	@Override
	public void readImpl()
	{
		_summonName = readS();
	}

	@Override
	public void runImpl()
	{}
}