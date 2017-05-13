package ru.l2gw.gameserver.serverpackets;

/**
 * Format: ch S
 */
public class ExAskJoinPartyRoom extends L2GameServerPacket
{
	private String _charName;

	// Need Room Name
	public ExAskJoinPartyRoom(String charName)
	{
		_charName = charName;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x35);
		writeS(_charName);
	}
}