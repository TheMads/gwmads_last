package ru.l2gw.gameserver.instancemanager.CommunityBoard;

import ru.l2gw.gameserver.model.L2Player;

/**
 * @author rage
 * @date 25.02.2010 17:27:14
 */
public interface ICommunityBoardHandler
{
	public String[] getBypassCommands();

	public void onBypassCommand(L2Player player, String bypass);

	public void onWriteCommand(L2Player player, String bypass, String arg1, String arg2, String arg3, String arg4, String arg5);
}
