package ru.l2gw.gameserver.instancemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.quest.Quest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class QuestManager
{
	private static Map<String, Quest> _questsByName = new HashMap<>();
	private static Map<Integer, Quest> _questsById = new HashMap<>();
	private static final Log _log = LogFactory.getLog("quest");

	public static Quest getQuest(String name)
	{
		return _questsByName.get(name);
	}

	public static Quest getQuest(int questId)
	{
		return _questsById.get(questId);
	}

	public static void addQuest(Quest quest)
	{
		_questsByName.put(quest.getName(), quest);
		_questsById.put(quest.getQuestIntId(), quest);

			if(Config.ALT_SHOW_QUEST_LOAD)
			_log.info("Loaded Quest: " + quest.getQuestIntId() + ": " + quest.getDescr());
	}

	public static Collection<Quest> getQuests()
	{
		return _questsByName.values();
	}
}