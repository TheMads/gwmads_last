package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;


/**
 * @author rage
 * @date: 21.07.2009 18:51:26
 */
public class ExShowFortressMapInfo extends L2GameServerPacket
{
	private final SiegeUnit _fortress;

	public ExShowFortressMapInfo(SiegeUnit fortress)
	{
		_fortress = fortress;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x7E);

		writeD(_fortress.getId());
		writeD(_fortress.getSiege().isInProgress() ? 1 : 0); // fortress siege status
		writeD(_fortress.getSize()); // barracks count

		/**
		 *	1 - арчер
		 *	2 - гуард
		 *	3 - суппорт юнит
		 *	4 - электростанция
		 *	5 - оффицерс барак
		 **/

		if(_fortress.getSiege().isInProgress())
			for(int i = 1; i <= _fortress.getSize(); i++)
				writeD(_fortress.getSiege().getBarrackStateById(i));
		else
			for(int i = 1; i <= _fortress.getSize(); i++)
				writeD(0);
	}
}
