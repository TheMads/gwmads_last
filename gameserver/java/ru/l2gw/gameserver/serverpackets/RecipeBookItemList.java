package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.entity.recipe.RecipeList;

import java.util.Collection;

public class RecipeBookItemList extends L2GameServerPacket
{
	private Collection<RecipeList> _recipes;

	private final int _isCommon;
	private final int _currentMp;

	public RecipeBookItemList(int isCommon, int CurMP)
	{
		_isCommon = isCommon;
		_currentMp = CurMP;
	}

	public void setRecipes(Collection<RecipeList> recipeBook)
	{
		_recipes = recipeBook;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xDC); //Точно: назначение пакета

		writeD(_isCommon); // Точно: 0 = Dwarven 1 = Common
		writeD(_currentMp); //Точно: текущее количество MP

		if(_recipes == null)
			writeD(0);
		else
		{
			writeD(_recipes.size()); //Точно: количество рецептов в книге
			int c = 1;
			for(RecipeList recipe : _recipes)
			{
				writeD(recipe.getId()); //Точно: ID рецепта
				writeD(c++); //Вероятно заглушка
			}
		}
	}
}