package ru.l2gw.gameserver.serverpackets;

/**
 * Asks the player to join a Command Channel
 */
public class ExAskJoinMPCC extends L2GameServerPacket
{
	private String _requestorName;

	public ExAskJoinMPCC(String requestorName)
	{
		_requestorName = requestorName;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x1a);
		writeS(_requestorName); // лидер CC
		writeD(0x00);
	}
}