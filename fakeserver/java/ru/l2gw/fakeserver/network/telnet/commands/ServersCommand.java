package ru.l2gw.fakeserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.fakeserver.FakeServer;
import ru.l2gw.fakeserver.client.ServerClient;

/**
 * @author: rage
 * @date: 18.04.13 17:31
 */
public class ServersCommand extends TelnetCommand
{
	public ServersCommand()
	{
		super("servers");
	}

	@Override
	public String getUsage()
	{
		return "type: servers";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Server list:\n");
		for(ServerClient client : FakeServer.getServerManager().getClients().values())
			sb.append("  ").append(client).append("\n");


		return sb.toString();
	}
}
