package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.instances.L2NpcInstance;

public class NpcSay extends L2GameServerPacket
{
	private int _objId;
	private int _type;
	private int _id;
	private int _msgId;
	private String[] _params;

	public NpcSay(L2NpcInstance npc, int chatType, String text)
	{
		_objId = npc.getObjectId();
		_type = chatType;
		if(_type < 2)
			_type += 22;
		_id = npc.getNpcId();
		_msgId = -1;
		_params = new String[] { text };
	}

	public NpcSay(L2NpcInstance npc, int chatType, int msgId, String... params)
	{
		_objId = npc.getObjectId();
		_type = chatType;
		if(_type < 2)
			_type += 22;
		_id = npc.getNpcId();
		_msgId = msgId;
		_params = params;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x30);
		writeD(_objId); // object_id отсылающего
		writeD(_type); // Тип чата, 0 = tell, 1 = shout, 2 = pm, 3 = party... Совпадает с Say2
		writeD(1000000 + _id); // npc id от кого отправлен пакет, клиент получает по нему имя.
		writeD(_msgId); // Message id
		for(int i = 0; i < 5; i++)
			writeS(i < _params.length ? _params[i] : "");
	}
}