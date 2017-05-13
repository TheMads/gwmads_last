package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.quest.QuestState;

public class GMViewQuestInfo extends L2GameServerPacket
{
	private final String _playerName;
	private final GArray<QuestState> _quests;
	private static final byte[] _unk = new byte[128];

	public GMViewQuestInfo(L2Player player)
	{
		_playerName = player.getName();
		_quests = player.getAllActiveQuests();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x99);
		writeS(_playerName);
		writeH(_quests.size());
		for(QuestState qs : _quests)
		{
			writeD(qs.getQuest().getQuestIntId());
			writeD(qs.getCond());
		}
		writeB(_unk);
	}
}