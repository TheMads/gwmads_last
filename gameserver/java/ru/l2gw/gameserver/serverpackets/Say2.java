package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.clientpackets.Say2C;
import ru.l2gw.gameserver.model.L2Player;

public class Say2 extends L2GameServerPacket
{
	private int _objectId, _textType, _stringId, _messageId, _stringMsgId;
	private String _charName;
	private String[] _params;

	public Say2(int objectId, int messageType, String charName, String text)
	{
		_objectId = objectId;
		_textType = messageType;
		_charName = charName;
		_params = new String[] { text };
		_stringMsgId = -1;
	}

	public Say2(int objectId, int messageType, String charName, int stringMsgId, String... params)
	{
		_objectId = objectId;
		_textType = messageType;
		_charName = charName;
		_params = params;
		_stringMsgId = stringMsgId;
		_messageId = -1;
	}

	public Say2(int messageType, int stringId, int messageId)
	{
		_textType = messageType;
		_stringId = stringId;
		_messageId = messageId;
		_objectId = 0;
		_stringMsgId = -1;
	}

	@Override
	final public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		player.broadcastSnoop(_textType, _charName, _stringMsgId, _params);
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x4A);
		writeD(_objectId);
		writeD(_textType);
		if(_textType == Say2C.SYSTEM_SHOUT)
		{
			writeD(_stringId);
			writeD(_messageId);
		}
		else
		{
			writeS(_charName);
			writeD(_stringMsgId); //npc string id
			for(String param : _params)
				writeS(param); // параметр для вставки в сообщение.
		}
	}
}