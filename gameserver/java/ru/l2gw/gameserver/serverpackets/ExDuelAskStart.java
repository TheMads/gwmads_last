package ru.l2gw.gameserver.serverpackets;

public class ExDuelAskStart extends L2GameServerPacket
{
	private String _requestor;
	private int _isPartyDuel;

	public ExDuelAskStart(String requestor, int isPartyDuel)
	{
		_requestor = requestor;
		_isPartyDuel = isPartyDuel;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x4D);
		writeS(_requestor);
		writeD(_isPartyDuel);
	}
}