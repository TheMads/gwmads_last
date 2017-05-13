package ru.l2gw.gameserver.network.telnet.commands;

import ru.l2gw.commons.network.telnet.TelnetCommand;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.L2World;

/**
 * @author: rage
 * @date: 03.03.12 23:04
 */
public class DatabaseCommand extends TelnetCommand
{
	public DatabaseCommand()
	{
		super("database");
	}

	@Override
	public String getUsage()
	{
		return "database";
	}

	@Override
	public String handle(String[] args, String ip)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Database Usage Status: ").append("\n");
		sb.append("+... Players operation: ").append("\n");
		sb.append("-->  Update characters: ").append(L2World.getUpdatePlayerBase()).append("\n");
		sb.append("+..... Items operation: ").append("\n");
		sb.append("-->      Insert: ").append(L2World.getInsertItemCount()).append("\n");
		sb.append("-->      Delete: ").append(L2World.getDeleteItemCount()).append("\n");
		sb.append("-->      Update: ").append(L2World.getUpdateItemCount()).append("\n");
		sb.append("--> Lazy Update: ").append(L2World.getLazyUpdateItem()).append("\n");
		sb.append("+... Lazy items update: ").append(Config.LAZY_ITEM_UPDATE).append("\n");
		sb.append("+... Released ObjectId: ").append(IdFactory.getInstance().getReleasedCount()).append("\n");
		return sb.toString();
	}
}
