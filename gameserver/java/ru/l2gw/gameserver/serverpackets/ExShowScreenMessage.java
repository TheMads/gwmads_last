package ru.l2gw.gameserver.serverpackets;

public class ExShowScreenMessage extends L2GameServerPacket
{
	public static enum ScreenMessageAlign
	{
		TOP_LEFT,
		TOP_CENTER,
		TOP_RIGHT,
		MIDDLE_LEFT,
		MIDDLE_CENTER,
		MIDDLE_RIGHT,
		BOTTOM_CENTER,
		BOTTOM_RIGHT,
	}

	private boolean _hide;
	private int _time, _textAlign, _decoText, _bigFont, _npcMsgId;
	private int _unk1, _unk2, _unk3, _unk4;
	private String[] _params;

	public ExShowScreenMessage(int text_align, int unk1, int unk2, int bigFont, int unk3, int decoText, int time, int unk4, int npcMsgId, String... params)
	{
		_textAlign = text_align;
		_unk1 = unk1;
		_unk2 = unk2;
		_bigFont = bigFont;
		_unk3 = unk3;
		_decoText = decoText;
		_time = time;
		_unk4 = unk4;
		_npcMsgId = npcMsgId;
		_params = params;
	}

	public ExShowScreenMessage(String text, int time, int text_align, int big_font, int decoText)
	{
		this(text_align, 0, 0, big_font, 0, decoText, time, 0, -1, text);
	}

	public ExShowScreenMessage(String text, int time, ScreenMessageAlign text_align, boolean big_font, boolean decoText)
	{
		this(text_align.ordinal() + 1, 0, 0, big_font ? 1 : 0, 0, decoText ? 1 : 0, time, 0, -1, text);
	}

	public ExShowScreenMessage(int time, ScreenMessageAlign text_align, boolean big_font, boolean decoText, int stringMsgId, String... params)
	{
		this(text_align.ordinal() + 1, 0, 0, big_font ? 1 : 0, 0, decoText ? 1 : 0, time, 0, stringMsgId, params);
	}

	public ExShowScreenMessage(String text, int time, ScreenMessageAlign text_align, boolean big_font)
	{
		this(text_align.ordinal() + 1, 0, 0, big_font ? 1 : 0, 0, 0, time, 0, -1, text);
	}

	public ExShowScreenMessage(String text, int time, ScreenMessageAlign text_align)
	{
		this(text_align.ordinal() + 1, 0, 0, 1, 0, 0, time, 0, -1, text);
	}

	public ExShowScreenMessage(String text, int time)
	{
		this(5, 0, 0, 1, 0, 1, time, 0, -1, text);
	}

	@Override
	final public void runImpl()
	{}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x39);

		writeD(_hide ? 0 : 1); // показать или спрятать
		writeD(_npcMsgId);
		//if(_hide)
		//	return;
		writeD(_textAlign); // размещение текста
		writeD(_unk1);
		writeD(_bigFont); // размер текста
		writeD(_unk2);
		writeD(_unk3);
		writeD(_decoText);
		writeD(_time); // время отображения сообщения в милисекундах
		writeD(0x01); // ?
		for(int i = 0; i < 5; i++)
			writeS(i < _params.length ? _params[i] : "");
	}
}
