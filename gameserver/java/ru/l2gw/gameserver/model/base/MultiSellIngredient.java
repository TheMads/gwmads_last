package ru.l2gw.gameserver.model.base;

import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Armor;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.gameserver.templates.L2Weapon;

public class MultiSellIngredient implements Cloneable
{
	private int _itemId;
	private long _itemCount;
	private int _itemEnchant;
	private int _attackElement = -2;
	private int _attackValue;
	private int _attrFire;
	private int _attrWater;
	private int _attrWind;
	private int _attrEarth;
	private int _attrHoly;
	private int _attrDark;

	public MultiSellIngredient(int itemId, long itemCount, L2ItemInstance item)
	{
		setItemId(itemId);
		setItemCount(itemCount);
		if(item != null)
		{
			setItemEnchant(item.getEnchantLevel());
			if(item.getItem() instanceof L2Weapon)
			{
				_attackElement = item.getAttackElement()[0];
				_attackValue = item.getAttackElement()[1];
			}
			else if(item.getItem() instanceof L2Armor)
			{
				_attrFire = item.getAttributeElementValue(0);
				_attrWater = item.getAttributeElementValue(1);
				_attrWind = item.getAttributeElementValue(2);
				_attrEarth = item.getAttributeElementValue(3);
				_attrHoly = item.getAttributeElementValue(4);
				_attrDark = item.getAttributeElementValue(5);
			}
		}
	}

	public MultiSellIngredient(int itemId, long itemCount)
	{
		setItemId(itemId);
		setItemCount(itemCount);
		setItemEnchant(0);
	}

	@Override
	public MultiSellIngredient clone()
	{
		MultiSellIngredient mi = new MultiSellIngredient(_itemId, _itemCount);
		mi._itemEnchant = _itemEnchant;
		mi._attackElement = _attackElement;
		mi._attackValue = _attackValue;
		mi._attrFire = _attrFire;
		mi._attrWater = _attrWater;
		mi._attrWind = _attrWind;
		mi._attrEarth = _attrEarth;
		mi._attrHoly = _attrHoly;
		mi._attrDark = _attrDark;
		return mi;
	}

	/**
	 * @param itemId The itemId to set.
	 */
	public void setItemId(int itemId)
	{
		_itemId = itemId;
	}

	/**
	 * @return Returns the itemId.
	 */
	public int getItemId()
	{
		return _itemId;
	}

	/**
	 * @param itemCount The itemCount to set.
	 */
	public void setItemCount(long itemCount)
	{
		_itemCount = itemCount;
	}

	/**
	 * @return Returns the itemCount.
	 */
	public long getItemCount()
	{
		return _itemCount;
	}

	/**
	 * Returns if item is stackable
	 * @return boolean
	 */
	public boolean isStackable()
	{
		return _itemId == L2Item.ITEM_ID_FAME_POINTS || ItemTable.getInstance().getTemplate(_itemId).isStackable();
	}

	/**
	 * @param itemEnchant The itemEnchant to set.
	 */
	public void setItemEnchant(int itemEnchant)
	{
		_itemEnchant = itemEnchant;
	}

	/**
	 * @return Returns the itemEnchant.
	 */
	public int getItemEnchant()
	{
		return _itemEnchant;
	}

	public int getAttackElement()
	{
		return _attackElement;
	}

	public int getAttackValue()
	{
		return _attackValue;
	}

	public int getAttrFire()
	{
		return _attrFire;
	}

	public int getAttrWater()
	{
		return _attrWater;
	}

	public int getAttrWind()
	{
		return _attrWind;
	}

	public int getAttrEarth()
	{
		return _attrEarth;
	}

	public int getAttrHoly()
	{
		return _attrHoly;
	}

	public int getAttrDark()
	{
		return _attrDark;
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + (int) (_itemCount ^ (_itemCount >>> 32));
		result = PRIME * result + _itemEnchant;
		result = PRIME * result + _itemId;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		final MultiSellIngredient other = (MultiSellIngredient) obj;
		if(_itemCount != other._itemCount)
			return false;
		if(_itemEnchant != other._itemEnchant)
			return false;
		if(_itemId != other._itemId)
			return false;
		if(_attackElement != other._attackElement || _attackValue != other._attackValue || _attrFire != other._attrFire || _attrWater != other._attrWater || _attrWind != other._attrWind || _attrEarth != other._attrEarth || _attrHoly != other._attrHoly || _attrDark != other._attrDark)
			return false;
		return true;
	}
}