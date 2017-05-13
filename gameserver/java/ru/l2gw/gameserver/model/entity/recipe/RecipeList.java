package ru.l2gw.gameserver.model.entity.recipe;

import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 27.05.11 11:56
 */
public class RecipeList
{
	private final GArray<RecipeItem> _materialItems;
	private final RecipeProduct _product;
	private final int _recipeId;
	private final int _level;
	private final int _recipeItemId;
	private final int _mpConsume;
	private final int _successRate;
	private final int _isCommon;

	public RecipeList(int recipeId, int level, int recipeItemId, int mpConsume, int successRate, int isCommon, String product)
	{
		_materialItems = new GArray<RecipeItem>(1);
		_product = new RecipeProduct(product);
		_recipeId = recipeId;
		_level = level;
		_recipeItemId = recipeItemId;
		_mpConsume = mpConsume;
		_successRate = successRate;
		_isCommon = isCommon;
	}

	public void addMaterial(RecipeItem recipeItem)
	{
		_materialItems.add(recipeItem);
	}

	public int getId()
	{
		return _recipeId;
	}

	public int getLevel()
	{
		return _level;
	}

	public int getRecipeItemId()
	{
		return _recipeItemId;
	}

	public int getSuccessRate()
	{
		return _successRate;
	}

	public int getMpConsume()
	{
		return _mpConsume;
	}

	public GArray<RecipeItem> getMaterials()
	{
		return _materialItems;
	}

	public RecipeItem getProductItem()
	{
		return _product.getProductItem();
	}

	public int getProductItemId()
	{
		return _product.getProductItemId();
	}

	public int isCommon()
	{
		return _isCommon;
	}
}