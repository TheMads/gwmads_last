package ru.l2gw.commons.network.telnet;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author: rage
 * @date: 03.03.12 16:56
 */
public abstract class TelnetCommand implements Comparable<TelnetCommand>
{
	protected static final Log log = LogFactory.getLog(TelnetCommand.class);
	private final String command;
	private final String[] acronyms;

	public TelnetCommand(String command)
	{
		this(command, ArrayUtils.EMPTY_STRING_ARRAY);
	}

	public TelnetCommand(String command, String... acronyms)
	{
		this.command = command;
		this.acronyms = acronyms;
	}

	public String getCommand()
	{
		return command;
	}

	public String[] getAcronyms()
	{
		return acronyms;
	}

	public abstract String getUsage();

	/**
	 * Handle command and return result
	 * @param args arguments
	 * @return result for output
	 */
	public abstract String handle(String[] args, String ip);

	public boolean equals(String command)
	{
		for(String acronym : acronyms)
			if(command.equals(acronym))
				return true;
		return this.command.equalsIgnoreCase(command);
	}

	protected boolean checkArgs(int num, String[] args)
	{
		if(args.length < num)
			return false;

		for(int i = 0; i < num; i++)
			if(args[i].isEmpty())
				return false;

		return true;
	}

	@Override
	public String toString()
	{
		return command;
	}

	@Override
	public boolean equals(Object o)
	{
		if(o == this)
			return true;
		if(o == null)
			return true;
		if(o instanceof TelnetCommand)
			return command.equals(((TelnetCommand) o).command);
		return false;
	}

	@Override
	public int compareTo(TelnetCommand o)
	{
		return command.compareTo(o.command);
	}
}