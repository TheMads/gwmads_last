package ru.l2gw.gameserver.model.entity.vehicle.actions;

import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.clientpackets.Say2C;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.serverpackets.NpcSay;
import ru.l2gw.gameserver.serverpackets.Say2;

/**
 * @author rage
 * @date 07.05.2010 10:30:45
 */
public class messageAction extends StationAction
{
	private int _stringId;
	private int _messageId;
	private int _npcId;
	private String _message, _custom;
	private long _npcStorageId;

	public void parseAction(Node an) throws Exception
	{
		Node attr = an.getAttributes().getNamedItem("stringId");
		_stringId = attr == null ? 0 : Integer.parseInt(attr.getNodeValue());
		attr = an.getAttributes().getNamedItem("messageId");
		_messageId = attr == null ? 0 : Integer.parseInt(attr.getNodeValue());
		attr = an.getAttributes().getNamedItem("npcId");
		_npcId = attr == null ? 0 : Integer.parseInt(attr.getNodeValue());
		attr = an.getAttributes().getNamedItem("message");
		_message = attr == null ? null : attr.getNodeValue();
		attr = an.getAttributes().getNamedItem("custom");
		_custom = attr == null ? null : attr.getNodeValue();
		super.parseAction(an);
	}

	public void doAction(L2Vehicle vehicle)
	{
		if(_npcId > 0 && _npcStorageId == 0)
		{
			L2NpcInstance npc = L2ObjectsStorage.getByNpcId(_npcId);
			if(npc != null)
				_npcStorageId = npc.getStoredId();
		}

		if(_stringId > 0)
			vehicle.broadcastPacketToPoints(new Say2(Say2C.SYSTEM_SHOUT, _stringId, _messageId));
		else if(_npcStorageId > 0 && (_message != null || _custom != null))
		{
			L2NpcInstance npc = L2ObjectsStorage.getAsNpc(_npcStorageId);
			if(npc != null)
				npc.broadcastPacket(new NpcSay(npc, Say2C.SHOUT, _message == null ? new CustomMessage(_custom, Config.DEFAULT_LANG).toString() : _message));
		}
	}
}
