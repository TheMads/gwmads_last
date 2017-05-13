package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

/**
 * sample
 *
 * 21			// packet type
 * 01 00			// item count
 *
 * 03 00			// update type   01-added?? 02-modified 03-removed
 * 04 00			// itemType1  0-weapon/ring/earring/necklace  1-armor/shield  4-item/questitem/adena
 * c6 37 50 40	// objectId
 * cd 09 00 00	// itemId
 * 05 00 00 00	// count
 * 05 00			// itemType2  0-weapon  1-shield/armor  2-ring/earring/necklace  3-questitem  4-adena  5-item
 * 00 00			// always 0 ??
 * 00 00			// equipped 1-yes
 * 00 00 00 00	// slot  0006-lr.ear  0008-neck  0030-lr.finger  0040-head  0080-??  0100-l.hand  0200-gloves  0400-chest  0800-pants  1000-feet  2000-??  4000-r.hand  8000-r.hand
 * 00 00			// enchant level
 * 00 00			// always 0 ??
 * 00 00 00 00	// augmentation id
 * ff ff ff ff	// shadow weapon time remaining
 *
 * format   h (hh dddhhhh hh)	revision 377
 * format   h (hh dddhhhd hh)   revision 415
 *
 *
 * format   h (hh dddhhhd hh dd) revision 740
 */
public class InventoryUpdate extends AbstractItemPacket
{
	protected final GArray<ItemInfo> _items = new GArray<ItemInfo>();

	public InventoryUpdate()
	{}

	/**
	 * @param items list of items
	 */
	public InventoryUpdate(GArray<L2ItemInstance> items)
	{
		for(L2ItemInstance item : items)
			_items.add(new ItemInfo(item));
	}

	public InventoryUpdate addNewItem(L2ItemInstance item)
	{
		item.setLastChange(L2ItemInstance.ADDED);
		_items.add(new ItemInfo(item));
		return this;
	}

	public InventoryUpdate addModifiedItem(L2ItemInstance item)
	{
		item.setLastChange(L2ItemInstance.MODIFIED);
		_items.add(new ItemInfo(item));
		return this;
	}

	public InventoryUpdate addRemovedItem(L2ItemInstance item)
	{
		item.setLastChange(L2ItemInstance.REMOVED);
		_items.add(new ItemInfo(item));
		return this;
	}

	public InventoryUpdate addItem(L2ItemInstance item)
	{
		if(item == null)
			return null;

		switch(item.getLastChange())
		{
			case L2ItemInstance.ADDED:
			{
				addNewItem(item);
				break;
			}
			case L2ItemInstance.MODIFIED:
			{
				addModifiedItem(item);
				break;
			}
			case L2ItemInstance.REMOVED:
			{
				addRemovedItem(item);
			}
		}
		return this;
	}

	@Override
	protected void writeImpl()
	{
		if(_items.size() < 1)
			return;

		writeC(0x21);
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
			writeH(temp.getAugmentationId());
			writeH(0x00);
			writeD(temp.getShadowLifeTime());
			writeD(temp.getTemporalLifeTime());
			writeH(0x01); // блокировать ли вещь( 01 нет, 00 да)
			writeH(temp.getAttackElement()[0]);
			writeH(temp.getAttackElement()[1]);
			writeH(temp.getDefenceFire());
			writeH(temp.getDefenceWater());
			writeH(temp.getDefenceWind());
			writeH(temp.getDefenceEarth());
			writeH(temp.getDefenceHoly());
			writeH(temp.getDefenceUnholy());
			writeH(temp.getEnchantOption()[0]);
			writeH(temp.getEnchantOption()[1]);
			writeH(temp.getEnchantOption()[2]);
			writeD(temp.getVisualId());
		}
	}

	public class ItemInfo
	{
		private final short lastChange;
		private final short type1;
		private final int objectId;
		private final int itemId;
		private final long count;
		private final short type2;
		private final short customType1;
		private final boolean isEquipped;
		private final int bodyPart;
		private final short enchantLevel;
		private final short customType2;
		private final int augmentationId;
		private final int shadowLifeTime;
		private final int[] attackElement;
		private final int defenceFire;
		private final int defenceWater;
		private final int defenceWind;
		private final int defenceEarth;
		private final int defenceHoly;
		private final int defenceUnholy;
		private final short equipSlot;
		private final int temporalLifeTime;
		private final int[] enchantOption;
		private final int visualId;

		protected ItemInfo(L2ItemInstance item)
		{
			lastChange = (short) item.getLastChange();
			type1 = (short) item.getItem().getType1();
			objectId = item.getObjectId();
			itemId = item.getItemId();
			count = item.getCount();
			type2 = (short) item.getItem().getType2();
			customType1 = (short) item.getCustomType1();
			isEquipped = item.isEquipped();
			bodyPart = item.getItem().getBodyPart();
			enchantLevel = (short) item.getEnchantLevel();
			customType2 = (short) item.getCustomType2();
			augmentationId = item.getAugmentationId();
			shadowLifeTime = item.getMana();
			attackElement = item.getAttackElement();
			defenceFire = item.getDefenceFire();
			defenceWater = item.getDefenceWater();
			defenceWind = item.getDefenceWind();
			defenceEarth = item.getDefenceEarth();
			defenceHoly = item.getDefenceHoly();
			defenceUnholy = item.getDefenceDark();
			equipSlot = item.getEquipSlot();
			temporalLifeTime = item.getExpireTime();
			enchantOption = new int[3];
			enchantOption[0] = item.getEnchantOptionId(0);
			enchantOption[1] = item.getEnchantOptionId(1);
			enchantOption[2] = item.getEnchantOptionId(2);
			visualId = item.getVisualId();
		}

		public short getLastChange()
		{
			return lastChange;
		}

		public int getVisualId()
		{
			return visualId;
		}

		public short getType1()
		{
			return type1;
		}

		public int getObjectId()
		{
			return objectId;
		}

		public int getItemId()
		{
			return itemId;
		}

		public long getCount()
		{
			return count;
		}

		public short getType2()
		{
			return type2;
		}

		public short getCustomType1()
		{
			return customType1;
		}

		public boolean isEquipped()
		{
			return isEquipped;
		}

		public int getBodyPart()
		{
			return bodyPart;
		}

		public short getEnchantLevel()
		{
			return enchantLevel;
		}

		public int getAugmentationId()
		{
			return augmentationId;
		}

		public int getShadowLifeTime()
		{
			return shadowLifeTime;
		}

		public short getCustomType2()
		{
			return customType2;
		}

		public int[] getAttackElement()
		{
			return attackElement;
		}

		public int getDefenceFire()
		{
			return defenceFire;
		}

		public int getDefenceWater()
		{
			return defenceWater;
		}

		public int getDefenceWind()
		{
			return defenceWind;
		}

		public int getDefenceEarth()
		{
			return defenceEarth;
		}

		public int getDefenceHoly()
		{
			return defenceHoly;
		}

		public int getDefenceUnholy()
		{
			return defenceUnholy;
		}

		public short getEquipSlot()
		{
			return equipSlot;
		}
	
		public int getTemporalLifeTime()
		{
			return temporalLifeTime;
		}

		public int[] getEnchantOption()
		{
			return enchantOption;
		}
	}
}