package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.quest.QuestState;
import ru.l2gw.commons.arrays.GArray;

/**
 * sample forrev 377:
 * <p/>
 * 98
 * 05 00 		number of quests
 * ff 00 00 00
 * 0a 01 00 00
 * 39 01 00 00
 * 04 01 00 00
 * a2 00 00 00
 * <p/>
 * 04 00 		number of quest items
 * <p/>
 * 85 45 13 40 	item obj id
 * 36 05 00 00 	item id
 * 02 00 00 00 	count
 * 00 00 		?? bodyslot
 * <p/>
 * 23 bd 12 40
 * 86 04 00 00
 * 0a 00 00 00
 * 00 00
 * <p/>
 * 1f bd 12 40
 * 5a 04 00 00
 * 09 00 00 00
 * 00 00
 * <p/>
 * 1b bd 12 40
 * 5b 04 00 00
 * 39 00 00 00
 * 00 00                                                 .
 * <p/>
 * format h (d) h (dddh)   rev 377
 * format h (dd) h (dddd)  rev 417
 * This text was wrote by XaKa
 * QuestList packet structure:
 * {
 * 1 byte - 0x80
 * 2 byte - Number of Quests
 * for Quest in AvailibleQuests
 * {
 * 4 byte - Quest.ID
 * 4 byte - 1
 * }
 * 2 byte - Number of All Quests Item
 * for Item in AllQuestsItem
 * {
 * 4 byte - Item.ObjID
 * 4 byte - Item.ID
 * 4 byte - Item.Count
 * 4 byte - 5
 * }
 */
public class QuestList extends L2GameServerPacket
{
	private GArray<QuestState> _quests;
	private static final byte[] _unk = new byte[128];

	public QuestList(L2Player player)
	{
		_quests = player.getAllActiveQuests();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x86);
		writeH(_quests.size());
		for(QuestState qs : _quests)
		{
			writeD(qs.getQuest().getQuestIntId());
			writeD(qs.getCond()); // stage of quest progress
		}
		writeB(_unk);
	}
}