package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.handler.IUserCommandHandler;
import ru.l2gw.gameserver.handler.UserCommandHandler;
import ru.l2gw.gameserver.model.L2Player;

/**
 * Пример пакета по команде /loc:
 * AA 00 00 00 00
 */
public class BypassUserCmd extends L2GameClientPacket
{
	private int _command;

	/**
	 * packet type id 0xB3
	 * format:  cd
	 */
	@Override
	public void readImpl()
	{
		_command = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		IUserCommandHandler handler = UserCommandHandler.getInstance().getUserCommandHandler(_command);

		if(handler == null)
			player.sendMessage(new CustomMessage("common.S1NotImplemented", player).addString(String.valueOf(_command)));
		else
			handler.useUserCommand(_command, player);
	}
}