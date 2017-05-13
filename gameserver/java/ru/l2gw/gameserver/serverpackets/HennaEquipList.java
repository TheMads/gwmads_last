package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2HennaInstance;

import java.util.HashSet;

public class HennaEquipList extends L2GameServerPacket
{
	private int HennaEmptySlots;
	private long char_adena;
	private HashSet<L2HennaInstance> availHenna = new HashSet<L2HennaInstance>();

	public HennaEquipList(L2Player player, L2HennaInstance[] hennaEquipList)
	{
		char_adena = player.getAdena();
		HennaEmptySlots = player.getHennaEmptySlots();
		for(L2HennaInstance element : hennaEquipList)
			if(player.getInventory().getItemByItemId(element.getItemIdDye()) != null)
				availHenna.add(element);
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xEE);
		writeQ(char_adena);
		writeD(HennaEmptySlots);
		if(availHenna.size() != 0)
		{
			writeD(availHenna.size());
			for(L2HennaInstance henna : availHenna)
			{
				writeD(henna.getSymbolId()); //symbolid
				writeD(henna.getItemIdDye()); //itemid of dye
				writeQ(henna.getAmountDyeRequire()); //amount of dye require
				writeQ(henna.getPrice()); //amount of aden require
				writeD(1); //meet the requirement or not
			}
		}
		else
		{
			writeD(0x01);
			writeD(0x00);
			writeD(0x00);
			writeQ(0x00);
			writeQ(0x00);
			writeD(0x00);
		}
	}
}