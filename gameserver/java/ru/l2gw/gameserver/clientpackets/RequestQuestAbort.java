package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.instancemanager.QuestManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.quest.QuestState;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestQuestAbort extends L2GameClientPacket
{
	private int _QuestID;

	@Override
	public void readImpl()
	{
		_QuestID = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null || QuestManager.getQuest(_QuestID) == null)
			return;
		QuestState qs = player.getQuestState(QuestManager.getQuest(_QuestID).getName());
		if(qs != null)
		{
			qs.exitCurrentQuest(true);
			if(_QuestID == 605)
				qs.getPlayer().setKetra(0);
			else if(_QuestID == 611)
				qs.getPlayer().setVarka(0);
			player.sendPacket(new SystemMessage(SystemMessage.S1_IS_ABORTED).addString(QuestManager.getQuest(_QuestID).getDescr()));
		}
	}
}