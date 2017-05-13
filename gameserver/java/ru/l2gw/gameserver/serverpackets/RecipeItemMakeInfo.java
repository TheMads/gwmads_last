package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.controllers.RecipeController;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.recipe.RecipeList;

/**
 * format ddddd
 */
public class RecipeItemMakeInfo extends L2GameServerPacket
{
	private int _id;
	private int _status;
	private int _CurMP;
	private int _MaxMP;

	public RecipeItemMakeInfo(int id, L2Player pl, int status)
	{
		if(pl == null)
			return;
		_id = id;
		_status = status;
		_CurMP = (int) pl.getCurrentMp();
		_MaxMP = pl.getMaxMp();
	}

	@Override
	protected final void writeImpl()
	{
		RecipeList recipeList = RecipeController.getRecipeList(_id);
		if(recipeList == null)
			return;

		writeC(0xDD); //Точно: назначение пакета

		writeD(_id); //Точно: ID рецепта
		writeD(recipeList.isCommon());
		writeD(_CurMP); //Точно: текущее состояние полоски Creator MP
		writeD(_MaxMP); //Точно: максимальное состояние полоски Creator MP
		writeD(_status); //Точно: итог крафта; 0xFFFFFFFF нет статуса, 0 удача, 1 провал
	}
}