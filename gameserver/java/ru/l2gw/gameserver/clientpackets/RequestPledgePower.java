package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.ManagePledgePower;

public class RequestPledgePower extends L2GameClientPacket
{
	// format: cdd
	private int _rank;
	private int _action;
	private int _privs;

	@Override
	public void readImpl()
	{
		_rank = readD();
		_action = readD();
		if(_action == 2)
			_privs = readD();
		else
			_privs = 0;
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null || player.getClanId() == 0)
			return;
		if(_action == 2)
		{
			if(_rank < 0 || _rank > 9)
				return;
			if(player.getClanId() != 0 && (player.getClanPrivileges() & L2Clan.CP_CL_MANAGE_RANKS) == L2Clan.CP_CL_MANAGE_RANKS)
			{
				//if(_rank == 9)
				//	_privs = L2Clan.CP_CH_OPEN_DOOR | L2Clan.CP_CS_OPEN_DOOR;
				player.getClan().setRankPrivs(_rank, _privs);
				player.getClan().updatePrivsForRank(_rank);
			}
		}
		else
			player.sendPacket(new ManagePledgePower(player, _action, _rank));
	}
}