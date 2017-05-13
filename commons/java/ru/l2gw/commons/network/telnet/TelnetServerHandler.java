package ru.l2gw.commons.network.telnet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.channel.*;
import ru.l2gw.commons.arrays.GCSArray;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: rage
 * @date: 03.03.12 16:55
 */
public class TelnetServerHandler extends SimpleChannelUpstreamHandler
{
	private static final Log _log = LogFactory.getLog(TelnetServerHandler.class);

	//The following regex splits a line into its parts, separated by spaces, unless there are quotes, in which case the quotes take precedence.
	private static final Pattern COMMAND_ARGS_PATTERN = Pattern.compile("\"([^\"]*)\"|([^\\s]+)");

	private Set<TelnetCommand> _commands = new LinkedHashSet<>();
	private String telnetPassword;
	private GCSArray<ChannelHandlerContext> connections;

	public TelnetServerHandler(String password)
	{
		telnetPassword = password;
		connections = new GCSArray<>();

		_commands.add(new TelnetCommand("help", "h"){
			@Override
			public String getUsage()
			{
				return "help [command]";
			}

			@Override
			public String handle(String[] args, String ip)
			{
				if(args.length == 0)
				{
					StringBuilder sb = new StringBuilder();
					sb.append("Available commands:\n");
					for(TelnetCommand cmd : _commands)
					{
						sb.append(cmd.getCommand()).append("\n");
					}

					return sb.toString();
				}
				else
				{
					TelnetCommand cmd = TelnetServerHandler.this.getCommand(args[0]);
					if(cmd == null)
						return "Unknown command.\n";

					return "usage:\n" + cmd.getUsage() + "\n";
				}
			}
		});
	}

	public void addHandler(TelnetCommand cmd)
	{
		_commands.add(cmd);
	}

	public Set<TelnetCommand> getCommands()
	{
		return _commands;
	}

	private TelnetCommand getCommand(String command)
	{
		for(TelnetCommand cmd : _commands)
			if(cmd.equals(command))
				return cmd;

		return null;
	}

	private String tryHandleCommand(String command, String[] args, String ip)
	{
		TelnetCommand cmd = getCommand(command);

		if(cmd == null)
			return "Unknown command.\nl2gw> ";

		String response = cmd.handle(args, ip);
		if(response == null)
			response = "usage:\n" + cmd.getUsage() + "\nl2gw> ";

		return response + "\nl2gw> ";
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
	{
		connections.remove(ctx);
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
	{
		// Send greeting for a new connection.
		e.getChannel().write("Welcome to telnet console.\n");
		e.getChannel().write("It is " + new Date() + " now.\n");
		if(!telnetPassword.isEmpty())
		{
			// Ask password
			e.getChannel().write("Password: ");
			ctx.setAttachment(Boolean.FALSE);
		}
		else
		{
			connections.add(ctx);
			e.getChannel().write("Type 'help' to see all available commands.\n");
			e.getChannel().write("l2gw> ");
			ctx.setAttachment(Boolean.TRUE);
		}
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
	{
		// Cast to a String first.
		// We know it is a String because we put some codec in TelnetPipelineFactory.
		String request = (String) e.getMessage();

		// Generate and write a response.
		String response = null;
		boolean close = false;

		if(Boolean.FALSE.equals(ctx.getAttachment()))
			if(telnetPassword.equals(request))
			{
				ctx.setAttachment(Boolean.TRUE);
				request = "";
			}
			else
			{
				response = "Wrong password!\n";
				close = true;
			}

		if(Boolean.TRUE.equals(ctx.getAttachment()))
			if(request.isEmpty())
				response = "l2gw> ";
			else if(request.toLowerCase().equals("exit"))
			{
				response = "Have a good day!\n";
				close = true;
			}
			else
			{
				Matcher m = COMMAND_ARGS_PATTERN.matcher(request);

				m.find();
				String command = m.group();

				List<String> args = new ArrayList<>();
				String arg;
				while(m.find())
				{
					arg = m.group(1);
					if(arg == null)
						arg = m.group(0);
					args.add(arg);
				}

				response = tryHandleCommand(command, args.toArray(new String[args.size()]), ctx.getChannel().getRemoteAddress().toString());
			}

		// We do not need to write a ChannelBuffer here.
		// We know the encoder inserted at TelnetPipelineFactory will do the conversion.
		ChannelFuture future = e.getChannel().write(response);

		// Close the connection after sending 'Have a good day!'
		// if the client has sent 'exit'.
		if(close)
			future.addListener(ChannelFutureListener.CLOSE);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
	{
		if(e.getCause() instanceof IOException)
			e.getChannel().close();
		else
			_log.error("", e.getCause());
	}

	public void writeToAllConnections(String message)
	{
		for(ChannelHandlerContext context : connections)
			context.getChannel().write(message);
	}
}
