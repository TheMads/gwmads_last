package ru.l2gw.gameserver.serverpackets;

public class Snoop extends L2GameServerPacket
{
	private int _convoID;
	private String _name;
	private int _type;
	private int _msgId;
	private String _speaker;
	private String[] _params;

	public Snoop(int id, String name, int type, String speaker, String text)
	{
		_convoID = id;
		_name = name;
		_type = type;
		_speaker = speaker;
		_params = new String[] { text };
		_msgId = -1;
	}

	public Snoop(int id, String name, int type, String speaker, int msgId, String... params)
	{
		_convoID = id;
		_name = name;
		_type = type;
		_speaker = speaker;
		_params = params;
		_msgId = msgId;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xDB);

		writeD(_convoID);
		writeS(_name);
		writeD(0x00); // ??
		writeD(_type);
		writeS(_speaker);
		writeD(_msgId);
		if(_params != null)
			for(String param : _params)
				writeS(param); // параметр для вставки в сообщение.

	}
}