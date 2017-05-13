package ru.l2gw.gameserver.handler;

import ru.l2gw.gameserver.model.L2Player;

public interface IUserCommandHandler
{
	/**
	 * this is the worker method that is called when someone uses an admin command.
	 * @param player
	 * @param command
	 * @return command success
	 */
	public boolean useUserCommand(int id, L2Player player);

	/**
	 * this method is called at initialization to register all the item ids automatically
	 * @return all known itemIds
	 */
	public int[] getUserCommandList();
}
