package ru.l2gw.gameserver.serverpackets;

/**
 * sample
 * 0000: 84 6d 06 00 00 36 05 00 00 42 4d 36 05 00 00 00    .m...6...BM6....
 * 0010: 00 00 00 36 04 00 00 28 00 00 00 10 00 00 00 10    ...6...(........
 * 0020: 00 00 00 01 00 08 00 00 00 00 00 00 01 00 00 c4    ................
 * 0030: ...
 * 0530: 10 91 00 00 00 60 9b d1 01 e4 6e ee 52 97 dd       .....`....n.R..
 *
 * format   dd x...x
 */
public class PledgeCrest extends L2GameServerPacket
{
	private int _crestId;
	private int _crestSize;
	private byte[] _data;

	public PledgeCrest(int crestId, byte[] data)
	{
		_crestId = crestId;
		_data = data;
		_crestSize = _data.length;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x6A);
		writeD(_crestId);
		writeD(_crestSize);
		writeB(_data);
	}
}