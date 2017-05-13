package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.controllers.RecipeController;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.recipe.RecipeList;
import ru.l2gw.gameserver.serverpackets.RecipeBookItemList;

public class RequestRecipeItemDelete extends L2GameClientPacket
{
	// Format: cd
	private int _recipeID;

	@Override
	public void readImpl()
	{
		_recipeID = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		RecipeList rp = RecipeController.getRecipeList(_recipeID);

		if(rp == null)
		{
			player.sendActionFailed();
			return;
		}

		player.unregisterRecipe(_recipeID);

		RecipeBookItemList response = new RecipeBookItemList(rp.isCommon(), (int) player.getCurrentMp());

		response.setRecipes(player.getDwarvenRecipeBook());

		player.sendPacket(response);
	}
}