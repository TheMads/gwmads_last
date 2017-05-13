package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.CrestCache;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.PledgeShowInfoUpdate;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestSetPledgeCrest extends L2GameClientPacket
{
	// Format: cdb
	private int _length;
	private byte[] _data;

	@Override
	public void readImpl()
	{
		_length = readD();
		if(_length == 0)
			return; // удаление значка
		if(_length > _buf.remaining() || _length != 256)
		{
			_log.warn("Possibly server crushing packet: " + getType() + " with length " + _length);
			_buf.clear();
			return;
		}
		_data = new byte[_length];
		readB(_data);
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2Clan clan = player.getClan();
		if((player.getClanPrivileges() & L2Clan.CP_CL_REGISTER_CREST) == L2Clan.CP_CL_REGISTER_CREST)
		{
			if(clan.getLevel() < 3)
			{
				player.sendPacket(new SystemMessage(SystemMessage.CLAN_CREST_REGISTRATION_IS_ONLY_POSSIBLE_WHEN_CLANS_SKILL_LEVELS_ARE_ABOVE_3));
				return;
			}

			if(clan.hasCrest())
				CrestCache.removePledgeCrest(clan);

			if(_data != null && _length != 0)
				CrestCache.savePledgeCrest(clan, _data);

			PledgeShowInfoUpdate pi = new PledgeShowInfoUpdate(clan);
			for(L2Player member : clan.getOnlineMembers(""))
			{
				member.sendPacket(pi);
				member.broadcastUserInfo(true);
			}

			if(clan.getHasCastle() > 0 && TerritoryWarManager.getTerritoryById(clan.getHasCastle() + 80).hasLord())
				TerritoryWarManager.changeTerritoryLord(TerritoryWarManager.getTerritoryById(clan.getHasCastle() + 80));
		}
	}
}
