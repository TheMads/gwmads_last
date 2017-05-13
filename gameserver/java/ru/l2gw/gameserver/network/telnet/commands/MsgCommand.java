package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.commons.utils.StringUtil;
import ru.l2gw.gameserver.clientpackets.Say2C;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.Say2;

/**
 * @author: rage
 * @date: 03.03.12 23:22
 */
public class MsgCommand extends TelnetCommand
{
	public MsgCommand()
	{
		super("msg");
	}

	@Override
	public String getUsage()
	{
		return "msg <char name> <message>";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		if(args.length < 2 || args[0].isEmpty() || args[1].isEmpty())
			return null;

		String message = StringUtil.joinStrings(" ", args, 1);

		L2Player reciever = L2ObjectsStorage.getPlayer(args[0]);
		if(reciever != null)
		{
			reciever.sendPacket(new Say2(0, Say2C.TELL, "Server Admin", message));
			return "Telnet priv-> " + args[0] + ": " + message + "\nMessage send\n";
		}

		return "Unable To Find Username: " + args[0];
	}
}