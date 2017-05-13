package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.QuestManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.quest.Quest;

public class RequestTutorialLinkHtml extends L2GameClientPacket
{
	// format: cS
	String _bypass;

	@Override
	public void readImpl()
	{
		_bypass = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		Quest q = QuestManager.getQuest(255);
		if(q != null && _bypass.startsWith("tutorial_close_"))
		{
			player.sendPacket(Msg.TutorialCloseHtml);
			player.processQuestEvent(q.getName(), "TE-" + Integer.parseInt(_bypass.substring(15)));
		}
	}
}