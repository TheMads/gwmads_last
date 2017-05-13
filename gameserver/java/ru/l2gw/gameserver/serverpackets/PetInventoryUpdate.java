package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

public class PetInventoryUpdate extends InventoryUpdate
{
	public PetInventoryUpdate()
	{}

	public PetInventoryUpdate(GArray<L2ItemInstance> items)
	{
		for(L2ItemInstance item : items)
			_items.add(new ItemInfo(item));
	}

	@Override
	protected void writeImpl()
	{
		if(_items.size() < 1)
			return;

		writeC(0xB4);
		writeH(_items.size());

		for(ItemInfo temp : _items)
		{
			writeH(temp.getLastChange());
			writeD(temp.getObjectId());
			writeD(temp.getItemId());
			writeD(temp.getEquipSlot());
			writeQ(temp.getCount());
			writeH(temp.getType2());
			writeH(temp.getCustomType1());
			writeH(temp.isEquipped() ? 1 : 0);
			writeD(temp.getBodyPart());
			writeH(temp.getEnchantLevel());
			writeH(temp.getCustomType2());
			writeD(temp.getAugmentationId());
			writeD(temp.getShadowLifeTime());
			writeD(temp.getTemporalLifeTime());
			writeH(temp.getAttackElement()[0]);
			writeH(temp.getAttackElement()[1]);
			writeH(temp.getDefenceFire());
			writeH(temp.getDefenceWater());
			writeH(temp.getDefenceWind());
			writeH(temp.getDefenceEarth());
			writeH(temp.getDefenceHoly());
			writeH(temp.getDefenceUnholy());
			writeH(0);
			writeH(0);
			writeH(0);
		}
	}
}