package ru.l2gw.gameserver.loginservercon.gspackets;

import javolution.util.FastList;
import ru.l2gw.gameserver.loginservercon.Attribute;

public class ServerStatus extends GameServerBasePacket
{

	public ServerStatus(FastList<Attribute> attributes)
	{
		writeC(0x06);
		writeD(attributes.size());
		for(Attribute temp : attributes)
		{
			writeD(temp.id);
			writeD(temp.value);
		}

		FastList.recycle(attributes);
	}
}