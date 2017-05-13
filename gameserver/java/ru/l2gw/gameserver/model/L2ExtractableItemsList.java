package ru.l2gw.gameserver.model;

public class L2ExtractableItemsList
{
	public int _productId;
	public int _chance;
	public int _count;

	public void setProductId(int productId)
	{
		_productId = productId;
	}

	public void setChance(int chance)
	{
		_chance = chance;
	}

	public void setCount(int count)
	{
		_count = count;
	}

	public int getProductId()
	{
		return _productId;
	}

	public int getChance()
	{
		return _chance;
	}

	public int getCount()
	{
		return _count;
	}

}
