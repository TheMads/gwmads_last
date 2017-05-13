package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.network.GameClient;
import ru.l2gw.gameserver.serverpackets.SkillCoolTime;

/**
 * @author: Death
 * @date: 16/2/2007
 * @time: 21:16:06
 */
public class RequestSkillCoolTime extends L2GameClientPacket
{
	GameClient _client;

	@Override
	public void readImpl()
	{
		_client = getClient();
	}

	@Override
	public void runImpl()
	{
		L2Player pl = _client.getPlayer();
		if(pl != null)
			pl.sendPacket(new SkillCoolTime(pl));
	}
}