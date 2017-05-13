package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.HennaEquipList;
import ru.l2gw.gameserver.serverpackets.HennaRemoveList;
import ru.l2gw.gameserver.tables.HennaTreeTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

/**
 * This class ...
 *
 * @version $Revision$ $Date$
 */
public class L2SymbolMakerInstance extends L2NpcInstance
{
	public L2SymbolMakerInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(command.equals("Draw"))
		{
			L2HennaInstance[] henna = HennaTreeTable.getInstance().getAvailableHenna(player.getClassId(), player.getSex());
			player.sendPacket(new HennaEquipList(player, henna));
		}
		else if(command.equals("RemoveList"))
			player.sendPacket(new HennaRemoveList(player));
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	public String getHtmlPath(int npcId, int val, int karma)
	{
		String pom;
		if(val == 0)
			pom = "SymbolMaker";
		else
			pom = "SymbolMaker-" + val;

		return "data/html/symbolmaker/" + pom + ".htm";
	}
}