package ru.l2gw.gameserver.model.entity.recipe;

/**
 * @author rage
 * @date 27.05.11 11:56
 */
public class RecipeItem
{
	public final int itemId;
	public final int chance;
	public final long quantity;

	public RecipeItem(int itemId, long quantity)
	{
		this.itemId = itemId;
		this.quantity = quantity;
		chance = 0;
	}

	public RecipeItem(int itemId, long quantity, int chance)
	{
		this.itemId = itemId;
		this.quantity = quantity;
		this.chance = chance;
	}
}
