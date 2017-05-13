package ru.l2gw.gameserver.serverpackets;

public class RestartResponse extends L2GameServerPacket
{
	private final int _status;
	public final static RestartResponse OK = new RestartResponse(1);
	public final static RestartResponse FAIL = new RestartResponse(0);

	public RestartResponse(int ok)
	{
		_status = ok;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x71);
		writeD(_status); //01-ok
	}
}