package ru.l2gw.gameserver.handler;

import ru.l2gw.gameserver.model.L2Player;

/**
 * This class ...
 *
 * @version $Revision: 1.1.4.2 $ $Date: 2005/03/27 15:30:09 $
 */
public interface IVoicedCommandHandler
{
	/**
	 * this is the worker method that is called when someone uses an admin command.
	 * @param player
	 * @param command
	 * @return command success
	 */
	public boolean useVoicedCommand(String command, L2Player player, String target);

	/**
	 * this method is called at initialization to register all the item ids automatically
	 * @return all known itemIds
	 */
	public String[] getVoicedCommandList();
}
