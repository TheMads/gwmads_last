package ru.l2gw.gameserver.model.entity.recipe;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;

/**
 * @author rage
 * @date 27.05.11 11:56
 */
public class RecipeProduct
{
	private final GArray<RecipeItem> _products = new GArray<RecipeItem>(1);

	public RecipeProduct(String product)
	{
		for(String productInfo : product.split(";"))
			if(productInfo != null && !productInfo.isEmpty())
			{
				String[] info = productInfo.split(",");
				_products.add(new RecipeItem(Integer.parseInt(info[0]), Long.parseLong(info[1]), Integer.parseInt(info[2])));
			}
	}

	public RecipeItem getProductItem()
	{
		int chance = Rnd.get(100);
		for(RecipeItem ri : _products)
		{
			if(chance < ri.chance)
				return ri;
			chance -= ri.chance;
		}

		return null;
	}

	public int getProductItemId()
	{
		return _products.get(0).itemId;
	}
}
