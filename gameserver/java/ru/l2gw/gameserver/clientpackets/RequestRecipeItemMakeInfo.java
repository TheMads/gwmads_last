package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.serverpackets.RecipeItemMakeInfo;

public class RequestRecipeItemMakeInfo extends L2GameClientPacket
{
	private int _id;

	/**
	 * packet type id 0xB7
	 * format:		cd
	 * @param decrypt
	 */
	@Override
	public void readImpl()
	{
		_id = readD();
	}

	@Override
	public void runImpl()
	{
		sendPacket(new RecipeItemMakeInfo(_id, getClient().getPlayer(), -1));
	}
}