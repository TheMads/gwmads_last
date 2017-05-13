package ru.l2gw.gameserver.serverpackets;

/**
 * @author rage
 * @date 16.09.2010 11:45:26
 * format: ddddSSSSSS
 */
public class ExSendUIEvent extends L2GameServerPacket
{
	private final int _objectId, _stopTimer, _p2, _p3, _fString;
	private final String _asc, _sm, _ss, _em, _es;
	private final String[] _fParams;

	public ExSendUIEvent(int objectId, int p1, int p2, int p3, String s1, String s2, String s3, String s4, String s5, int fString, String... params)
	{
		_objectId = objectId;
		_stopTimer = p1;
		_p2 = p2;
		_p3 = p3;
		_asc = s1;
		_sm = s2;
		_ss = s3;
		_em = s4;
		_es = s5;
		_fString = fString;
		_fParams = params;
	}

	public ExSendUIEvent(int objectId, boolean startTimer)
	{
		this(objectId, startTimer ? 0 : 1, 0, 0, "1", "0", "0", "0", "0", -1);
	}

	public ExSendUIEvent(int objectId, boolean startTimer, boolean asc, int start, int end)
	{
		this(objectId, startTimer ? 0 : 1, 0, 0, asc ? "1" : "0", String.valueOf(start / 60), String.valueOf(start % 60), String.valueOf(end / 60), String.valueOf(end % 60), -1);
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x8F);
		writeD(_objectId);
		writeD(_stopTimer);
		writeD(_p2); // unknown
		writeD(_p3); // unknown
		writeS(_asc); // "0": count negative, "1": count positive
		writeS(_sm); // timer starting minute(s)
		writeS(_ss); // timer starting second(s)
		writeS(_em); // timer length minute(s) (timer will disappear 10 seconds before it ends)
		writeS(_es); // timer length second(s) (timer will disappear 10 seconds before it ends)
		writeD(_fString);
		for(int i = 0; i < 5; i++)
			writeS(_fParams.length > i ? _fParams[i] : "");
	}
}
