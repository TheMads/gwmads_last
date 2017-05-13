package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;

/**
 * @author: rage
 * @date: 21.07.2009 19:27:15
 */
public class ExShowFortressSiegeInfo extends L2GameServerPacket
{
    private int _fortId;
	private int _size;
	private SiegeUnit _fortress;

	public ExShowFortressSiegeInfo(SiegeUnit fort)
	{
		_fortress = fort;
		_fortId = fort.getId();
		_size = fort.getSize();
	}

    @Override
    protected void writeImpl()
    {
        writeC(EXTENDED_PACKET);
        writeH(0x17);
        
        writeD(_fortId); // Fortress Id
        writeD(_size); // Total Barracks Count
		int c = 0;
		for(int i=1;i<=_size;i++)
			if(_fortress.getSiege().getBarrackStateById(i) == 1)
				c++;
		writeD(c);
    }
    
}
