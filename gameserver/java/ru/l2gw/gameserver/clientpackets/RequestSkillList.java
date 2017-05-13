package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.ExAcquirableSkillListByClass;
import ru.l2gw.gameserver.serverpackets.SkillList;

public final class RequestSkillList extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{
	// this is just a trigger packet. it has no content
	}

	@Override
	protected void runImpl()
	{
		L2Player cha = getClient().getPlayer();

		if(cha != null)
			cha.sendPacket(new SkillList(cha));
			cha.sendPacket(new ExAcquirableSkillListByClass(cha));
	}
}
