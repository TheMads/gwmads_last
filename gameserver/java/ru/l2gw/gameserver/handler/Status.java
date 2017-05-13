package ru.l2gw.gameserver.handler;

import javolution.text.TextBuilder;
import ru.l2gw.extensions.scripts.Functions;
import ru.l2gw.gameserver.GameServer;
import ru.l2gw.gameserver.Shutdown;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.tables.FakePlayersTable;

public class Status extends Functions implements IVoicedCommandHandler
{
	private final String[] _commandList = new String[] { "status" };

	public String[] getVoicedCommandList()
	{
		return _commandList;
	}

	public boolean useVoicedCommand(String command, L2Player player, String target)
	{
		if(command.equals("status") && player.isGM())
		{
			TextBuilder ret = TextBuilder.newInstance();
			boolean en = player.getVar("lang@").equalsIgnoreCase("en");
			if(en)
			{
				ret.append("<center><font color=\"LEVEL\">Server status:</font></center>");
				ret.append("<br1>Version: ").append(GameServer.getVersion().getRevisionNumber());
				ret.append("<br>Total online:  ");
			}
			else
			{
				ret.append("<center><font color=\"LEVEL\">Статус сервера:</font></center>");
				ret.append("<br1>Версия: ").append(GameServer.getVersion().getRevisionNumber());
				ret.append("<br>Онлайн сервера:  ");
			}
			ret.append(L2ObjectsStorage.getAllPlayersCount() + FakePlayersTable.getFakePlayersCount());
			ret.append("<br1>Memory usage: ").append((100 - Runtime.getRuntime().freeMemory() * 100 / Runtime.getRuntime().maxMemory())).append("%");
			int mtc = Shutdown.getInstance().getSeconds();
			if(mtc > 0)
			{
				if(en)
					ret.append("<br1>Time to restart: ");
				else
					ret.append("<br1>До рестарта: ");
				int numDays = mtc / 86400;
				mtc -= numDays * 86400;
				int numHours = mtc / 3600;
				mtc -= numHours * 3600;
				int numMins = mtc / 60;
				mtc -= numMins * 60;
				int numSeconds = mtc;
				if(numDays > 0)
					ret.append(numDays + "d ");
				if(numHours > 0)
					ret.append(numHours + "h ");
				if(numMins > 0)
					ret.append(numMins + "m ");
				if(numSeconds > 0)
					ret.append(numSeconds + "s");
			}
			else
				ret.append("<br1>Restart task not launched.");

			ret.append("<br><center><button value=\"");
			if(en)
				ret.append("Refresh");
			else
				ret.append("Обновить");
			ret.append("\" action=\"bypass -h user_status\" width=100 height=15 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\" /></center>");

			show(ret.toString(), player);
			TextBuilder.recycle(ret);
			return true;
		}
		return false;
	}
}
