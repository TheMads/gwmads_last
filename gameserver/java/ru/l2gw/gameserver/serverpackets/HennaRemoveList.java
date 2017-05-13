package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2HennaInstance;

/**
 * @author rage
 * @date 16.12.10 18:20
 */
public class HennaRemoveList extends L2GameServerPacket
{
	private L2Player _player;
	
	public HennaRemoveList(L2Player player)
	{
		_player = player;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xe6);
		writeQ(_player.getAdena());
		writeD(0x00);
		writeD(Math.max(0, 3 - _player.getHennaEmptySlots()));
		
		for (int i = 1; i <= 3; i++)
		{
			L2HennaInstance henna = _player.getHenna(i);
			if (henna != null)
			{
				writeD(henna.getSymbolId());
				writeD(henna.getItemIdDye());
				writeD(henna.getAmountDyeRequire() / 2);
				writeD(0x00);
				writeD(henna.getPrice() / 5);
				writeD(0x00);
				writeD(0x01);
			}
		}
	}
}