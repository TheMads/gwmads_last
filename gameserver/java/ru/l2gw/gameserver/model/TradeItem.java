package ru.l2gw.gameserver.model;

import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.templates.L2Item;

public final class TradeItem
{
	private int _objectId;
	private int _itemId;
	private long _price;
	private long _storePrice;
	private long _count;
	private long _limitCount;
	private long _limitResetTime;
	private int _enchantLevel;
	private long _tempvalue;
	private int _attackElement = -2;
	private int _attackValue;
	private int _attrDefenceFire;
	private int _attrDefenceWater;
	private int _attrDefenceWind;
	private int _attrDefenceEarth;
	private int _attrDefenceHoly;
	private int _attrDefenceUnholy;
	private int _cType2, _cType1, _type2, _bodyPart, _mana, _expireTime;
	private int[] enchantOptionId = new int[3];

	public TradeItem()
	{}

	public TradeItem(L2ItemInstance item)
	{
		_objectId = item.getObjectId();
		_itemId = item.getItemId();
		_enchantLevel = item.getEnchantLevel();
		_attackElement = item.getAttackElement()[0];
		_attackValue = item.getAttackElement()[1];
		_attrDefenceFire = item.getDefenceFire();
		_attrDefenceWater = item.getDefenceWater();
		_attrDefenceWind = item.getDefenceWind();
		_attrDefenceEarth = item.getDefenceEarth();
		_attrDefenceHoly = item.getDefenceHoly();
		_attrDefenceUnholy = item.getDefenceDark();
		_cType1 = item.getCustomType1();
		_cType2 = item.getCustomType2();
		_type2 = item.getItem().getType2();
		_bodyPart = item.getBodyPart();
		_mana = item.getMana();
		_expireTime = item.getExpireTime();
		enchantOptionId[0] = item.getEnchantOptionId(0);
		enchantOptionId[1] = item.getEnchantOptionId(1);
		enchantOptionId[2] = item.getEnchantOptionId(2);
	}

	public TradeItem(L2Item item)
	{
		_objectId = 0;
		_itemId = item.getItemId();
		_type2 = item.getType2();
		_bodyPart = item.getBodyPart();
		_mana = item.getDurability();
		_expireTime = item.getPeriod();
	}

	public void setObjectId(int id)
	{
		_objectId = id;
	}

	public int getObjectId()
	{
		return _objectId;
	}

	public void setItemId(int id)
	{
		_itemId = id;
	}

	public int getItemId()
	{
		return _itemId;
	}

	public void setOwnersPrice(long price)
	{
		_price = price;
	}

	public long getOwnersPrice()
	{
		return _price;
	}

	public void setStorePrice(long price)
	{
		_storePrice = price;
	}

	public long getStorePrice()
	{
		return _storePrice;
	}

	public void setCount(long count)
	{
		_count = count;
	}

	public long getCount()
	{
		return _count;
	}

	public void setEnchantLevel(int enchant)
	{
		_enchantLevel = enchant;
	}

	public int getEnchantLevel()
	{
		return _enchantLevel;
	}

	public void setTempValue(long tempvalue)
	{
		_tempvalue = tempvalue;
	}

	public long getTempValue()
	{
		return _tempvalue;
	}

	public int getAttackElement()
	{
		return _attackElement;
	}

	public int getAttackValue()
	{
		return _attackValue;
	}

	public int getDefenceFire()
	{
		return _attrDefenceFire;
	}

	public int getDefenceWater()
	{
		return _attrDefenceWater;
	}

	public int getDefenceWind()
	{
		return _attrDefenceWind;
	}

	public int getDefenceEarth()
	{
		return _attrDefenceEarth;
	}

	public int getDefenceHoly()
	{
		return _attrDefenceHoly;
	}

	public int getDefenceDark()
	{
		return _attrDefenceUnholy;
	}

	public void setAttackElement(int[] attackElement)
	{
		_attackElement = attackElement[0];
		_attackValue = attackElement[1];
	}

	public void setDefenceFire(int defenceFire)
	{
		_attrDefenceFire = defenceFire;
	}

	public void setDefenceWater(int defenceWater)
	{
		_attrDefenceWater = defenceWater;
	}

	public void setDefenceWind(int defenceWind)
	{
		_attrDefenceWind = defenceWind;
	}

	public void setDefenceEarth(int defenceEarth)
	{
		_attrDefenceEarth = defenceEarth;
	}

	public void setDefenceHoly(int defenceHoly)
	{
		_attrDefenceHoly = defenceHoly;
	}

	public void setDefenceUnholy(int defenceUnholy)
	{
		_attrDefenceUnholy = defenceUnholy;
	}

	public void setEnchantOptionId(int slot, int optionId)
	{
		enchantOptionId[slot] = optionId;
	}

	public void setLimitCount(long limit)
	{
		_limitCount = limit;
	}

	public void setLimitResetTime(long time)
	{
		_limitResetTime = time;
	}

	public int getCustomType1()
	{
		return _cType1;
	}
	
	public int getCustomType2()
	{
		return _cType2;
	}

	public int getType2()
	{
		return _type2;
	}

	public int getBodyPart()
	{
		return _bodyPart;
	}

	public long getLimitCount()
	{
		return _limitCount;
	}

	public long getLimitResetTime()
	{
		return _limitResetTime;
	}

	public int getMana()
	{
		return _mana;
	}

	public int getExpireTime()
	{
		return _expireTime;
	}

	public int getEnchantOptionId(int slot)
	{
		return enchantOptionId[slot];
	}

	public TradeItem clone()
	{
		TradeItem clone = new TradeItem();
		clone._objectId = _objectId;
		clone._itemId = _itemId;
		clone._price = _price;
		clone._storePrice = _storePrice;
		clone._count = _count;
		clone._enchantLevel = _enchantLevel;
		clone._tempvalue = _tempvalue;
		clone._attackElement = _attackElement;
		clone._attackValue = _attackValue;
		clone._attrDefenceFire = _attrDefenceFire;
		clone._attrDefenceWater = _attrDefenceWater;
		clone._attrDefenceWind = _attrDefenceWind;
		clone._attrDefenceEarth = _attrDefenceEarth;
		clone._attrDefenceHoly = _attrDefenceHoly;
		clone._attrDefenceUnholy = _attrDefenceUnholy;
		clone._limitCount = _limitCount;
		clone._limitResetTime = _limitResetTime;
		clone._mana = _mana;
		clone._expireTime = _expireTime;
		System.arraycopy(enchantOptionId, 0, clone.enchantOptionId, 0, enchantOptionId.length);
		return clone;
	}

	public boolean equals(L2ItemInstance item)
	{
		return item.getItemId() == _itemId && item.getEnchantLevel() == _enchantLevel &&
				item.getAttackElement()[0] == _attackElement && item.getAttackElement()[1] == _attackValue &&
				item.getDefenceFire() == _attrDefenceFire && item.getDefenceWater() == _attrDefenceWater &&
				item.getDefenceWind() == _attrDefenceWind && item.getDefenceEarth() == _attrDefenceEarth &&
				item.getDefenceHoly() == _attrDefenceHoly && item.getDefenceDark() == _attrDefenceUnholy;
	}

	@Override
	public int hashCode()
	{
		return _objectId + _itemId;
	}
}