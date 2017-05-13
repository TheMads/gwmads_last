package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.CrestCache;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Alliance;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.PledgeShowInfoUpdate;

public class RequestSetAllyCrest extends L2GameClientPacket
{
	// format: cdb
	private int _length;
	private byte[] _data;

	@Override
	public void readImpl()
	{
		_length = readD();
		if(_length > _buf.remaining() || _length > Short.MAX_VALUE || _length < 0)
		{
			_log.warn("Possibly server crushing packet: [C] 91 RequestSetAllyCrest with length " + _length);
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
		if(player == null || _data == null)
			return;

		L2Alliance ally = player.getAlliance();
		if(ally != null && player.isAllyLeader())
		{
			if(ally.hasAllyCrest())
				CrestCache.removeAllyCrest(ally);

			if(_data.length != 0)
				CrestCache.saveAllyCrest(ally, _data);

			for(L2Clan temp : ally.getMembers())
			{
				PledgeShowInfoUpdate pi = new PledgeShowInfoUpdate(temp);
				for(L2Player member : temp.getOnlineMembers(""))
				{
					member.sendPacket(pi);
					member.broadcastUserInfo(true);
				}
				if(temp.getHasCastle() > 0 && TerritoryWarManager.getTerritoryById(temp.getHasCastle() + 80).hasLord())
					TerritoryWarManager.changeTerritoryLord(TerritoryWarManager.getTerritoryById(temp.getHasCastle() + 80));
			}
		}
	}
}