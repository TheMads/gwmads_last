package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.TradeItem;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.tables.ItemTable;

/**
 * @author: rage
 * @date: 28.06.2010 19:06:46
 */
public class AbstractItemPacket extends L2GameServerPacket
{

	protected void writeItemInfo(L2ItemInstance item)
	{
		writeItemInfo(item, item.getCount());
	}

	protected void writeItemInfo(L2ItemInstance item, long count)
	{
		writeD(item.getObjectId());
		writeD(item.getItemId());
		writeD(item.getEquipSlot());
		writeQ(count);
		writeH(item.getItem().getType2());
		writeH(item.getCustomType1());
		writeH(item.isEquipped() ? 1 : 0);
		writeD(item.getBodyPart());
		writeH(item.getEnchantLevel());
		writeH(item.getCustomType2());
		//if(item.isAugmented())
		//	writeD(item.getAugmentationId());
		//else
        //    writeD(0x00);
		writeH(item.getAugmentationId());
		writeH(0x00);
		writeD(item.getMana());
		writeD(item.getExpireTime());
		writeH(0x01); // блокировать ли вещь( 01 нет, 00 да)
		writeH(item.getAttackElement()[0]);
		writeH(item.getAttackElement()[1]);
		writeH(item.getDefenceFire());
		writeH(item.getDefenceWater());
		writeH(item.getDefenceWind());
		writeH(item.getDefenceEarth());
		writeH(item.getDefenceHoly());
		writeH(item.getDefenceDark());
		// Enchent Effects
		writeH(item.getEnchantOptionId(0));
		writeH(item.getEnchantOptionId(1));
		writeH(item.getEnchantOptionId(2));
		writeD(item.getVisualId()); // ИД внешнего вида оружия.
	}
	
	protected void writeItemInfo(TradeItem item)
	{
		writeD(item.getObjectId());
		writeD(item.getItemId());
		writeD(0x00);
		writeQ(item.getCount());
		writeH(ItemTable.getInstance().getTemplate(item.getItemId()).getType2());
		writeH(item.getCustomType1());
		writeH(0x00);
		writeD(item.getBodyPart());
		writeH(item.getEnchantLevel());
		writeH(item.getCustomType2());
		writeH(0x00);
		writeH(0x00);
		writeD(item.getMana());
		writeD(item.getExpireTime());
		writeH(0x01); // блокировать ли вещь( 01 нет, 00 да)
		writeH(item.getAttackElement());
		writeH(item.getAttackValue());
		writeH(item.getDefenceFire());
		writeH(item.getDefenceWater());
		writeH(item.getDefenceWind());
		writeH(item.getDefenceEarth());
		writeH(item.getDefenceHoly());
		writeH(item.getDefenceDark());
		writeH(item.getEnchantOptionId(0));
		writeH(item.getEnchantOptionId(1));
		writeH(item.getEnchantOptionId(2));
		writeD(0x00); // ИД внешнего вида оружия.
	}

	@Override
	protected void writeImpl()
	{
	}
}
