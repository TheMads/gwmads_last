package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 16.12.10 15:14
 */
public class ExAskModifyPartyLooting extends L2GameServerPacket
{
	private String _requestor;
	private byte _mode;

	public ExAskModifyPartyLooting(String name, byte mode)
	{
		_requestor = name;
		_mode = mode;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0xC0);
		writeS(_requestor);
		writeD(_mode);
	}
}
