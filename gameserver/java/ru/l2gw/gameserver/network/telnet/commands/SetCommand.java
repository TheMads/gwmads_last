package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.commons.utils.StringUtil;
import ru.l2gw.gameserver.Config;

import java.util.StringTokenizer;

/**
 * @author: rage
 * @date: 05.03.12 16:32
 */
public class SetCommand extends TelnetCommand
{
	public SetCommand()
	{
		super("set");
	}
	
	@Override
	public String getUsage()
	{
		return "set <configField=value>";
	}
	
	@Override
	public String handle(String[] args, String ip)
	{
		if(!checkArgs(1, args))
			return null;
		
		try
		{
			StringTokenizer st = new StringTokenizer(args[0], "=");
			String fieldName = st.nextToken();
			String fieldValue = st.nextToken();
			if(args.length > 1)
				fieldValue += " " + StringUtil.joinStrings(" ", args, 1);
			Object oldValue = Config.setField(Config.class, fieldName, fieldValue);
			return "Set: " + fieldName + "=" + oldValue + " => " + fieldValue;
		}
		catch (Exception e)
		{
			return "Syntax error: " + e;
		}
	}
}
