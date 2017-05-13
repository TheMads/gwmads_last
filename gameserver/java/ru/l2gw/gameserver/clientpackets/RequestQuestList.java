package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.serverpackets.QuestList;

public class RequestQuestList extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		sendPacket(new QuestList(getClient().getPlayer()));
	}
}