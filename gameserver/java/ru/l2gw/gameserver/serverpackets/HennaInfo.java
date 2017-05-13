package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2HennaInstance;

public class HennaInfo extends L2GameServerPacket
{
	private final L2HennaInstance[] _hennas = new L2HennaInstance[3];
	private int _count, _str, _con, _dex, _int, _wit, _men;

	public HennaInfo(L2Player player)
	{
		int j = 0;
		for(int i = 0; i < 3; i++)
		{
			L2HennaInstance h = player.getHenna(i + 1);
			if(h != null)
				_hennas[j++] = h;
		}
		_count = j;

		_str = player.getHennaStatSTR();
		_con = player.getHennaStatCON();
		_dex = player.getHennaStatDEX();
		_int = player.getHennaStatINT();
		_wit = player.getHennaStatWIT();
		_men = player.getHennaStatMEN();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xE5);
		writeC(_int); //equip INT
		writeC(_str); //equip STR
		writeC(_con); //equip CON
		writeC(_men); //equip MEM
		writeC(_dex); //equip DEX
		writeC(_wit); //equip WIT
		writeD(3); //interlude, slots?
		writeD(_count);
		for(int i = 0; i < _count; i++)
			if(_hennas[i] != null)
			{
				writeD(_hennas[i].getSymbolId());
				writeD(_hennas[i].getSymbolId());
			}
	}
}