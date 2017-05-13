package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.CrestCache;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestSetPledgeCrestLarge extends L2GameClientPacket
{
	private int _size;
	private byte[] _data;

	/**
	 * @param buf
	 * @param client
	 * format: chd(b)
	 */
	@Override
	public void readImpl()
	{
		_size = readD();
		if(_size > _buf.remaining() || _size > Short.MAX_VALUE || _size <= 0)
			return;
		_data = new byte[_size];
		readB(_data);
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2Clan clan = player.getClan();
		if(clan == null)
			return;

		if((player.getClanPrivileges() & L2Clan.CP_CL_REGISTER_CREST) == L2Clan.CP_CL_REGISTER_CREST)
		{
			if(!clan.getHasUnit(2) && !clan.getHasUnit(1))
			{
				player.sendPacket(new SystemMessage(SystemMessage.THE_CLANS_EMBLEM_WAS_SUCCESSFULLY_REGISTERED__ONLY_A_CLAN_THAT_OWNS_A_CLAN_HALL_OR_A_CASTLE_CAN_GET_THEIR_EMBLEM_DISPLAYED_ON_CLAN_RELATED_ITEMS));
				return;
			}

			if(clan.hasCrestLarge())
				CrestCache.removePledgeCrestLarge(clan);

			if(_data != null && _data.length <= 2176)
			{
				CrestCache.savePledgeCrestLarge(clan, _data);
				player.sendPacket(new SystemMessage(SystemMessage.THE_CLANS_EMBLEM_WAS_SUCCESSFULLY_REGISTERED__ONLY_A_CLAN_THAT_OWNS_A_CLAN_HALL_OR_A_CASTLE_CAN_GET_THEIR_EMBLEM_DISPLAYED_ON_CLAN_RELATED_ITEMS));
			}

			for(L2Player member : clan.getOnlineMembers(""))
				member.broadcastUserInfo(true);
		}
	}
}
