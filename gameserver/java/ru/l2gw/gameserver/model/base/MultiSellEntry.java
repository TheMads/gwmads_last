package ru.l2gw.gameserver.model.base;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

public class MultiSellEntry
{
	private int _entryId;
	private GArray<MultiSellIngredient> _ingredients = new GArray<MultiSellIngredient>();
	private GArray<MultiSellIngredient> _production = new GArray<MultiSellIngredient>();

	public MultiSellEntry()
	{}

	public MultiSellEntry(int id)
	{
		_entryId = id;
	}

	public MultiSellEntry(int id, int product, int prod_count, L2ItemInstance item)
	{
		_entryId = id;
		addProduct(new MultiSellIngredient(product, prod_count, item));
	}

	/**
	 * @param entryId The entryId to set.
	 */
	public void setEntryId(int entryId)
	{
		_entryId = entryId;
	}

	/**
	 * @return Returns the entryId.
	 */
	public int getEntryId()
	{
		return _entryId;
	}

	/**
	 * @param ingredients The ingredients to set.
	 */
	public void addIngredient(MultiSellIngredient ingredient)
	{
		_ingredients.add(ingredient);
	}

	/**
	 * @return Returns the ingredients.
	 */
	public GArray<MultiSellIngredient> getIngredients()
	{
		return _ingredients;
	}

	/**
	 * @param ingredients The ingredients to set.
	 */
	public void addProduct(MultiSellIngredient ingredient)
	{
		_production.add(ingredient);
	}

	/**
	 * @return Returns the ingredients.
	 */
	public GArray<MultiSellIngredient> getProduction()
	{
		return _production;
	}

	@Override
	public int hashCode()
	{
		return _entryId;
	}
}