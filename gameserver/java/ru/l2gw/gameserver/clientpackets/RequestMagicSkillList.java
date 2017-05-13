package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SkillList;

public class RequestMagicSkillList extends L2GameClientPacket
{
	/**
	 * packet type id 0x38
	 * format:		c
	 * @param rawPacket
	 */

	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		sendPacket(new SkillList(player));
	}
}